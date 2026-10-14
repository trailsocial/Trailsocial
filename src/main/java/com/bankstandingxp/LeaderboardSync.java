package com.bankstandingxp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Reports one RuneScape account's Bankstanding XP and stats to the leaderboard on trailsocial.net.
 * Only used while the player has pasted a link code into the plugin settings; the code (not the
 * player name) is what ties the report to a TrailSocial login.
 */
final class LeaderboardSync
{
	static final String ENDPOINT = "https://trailsocial.net/.netlify/functions/bankstanding";
	static final int PROTOCOL_VERSION = 1;

	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final long PAUSE_AFTER_REJECTION_MS = 30 * 60_000L;
	private static final long PAUSE_AFTER_RATE_LIMIT_MS = 10 * 60_000L;
	private static final long MAX_RESTORED_VALUE = 10_000_000_000_000L;

	private final OkHttpClient http;
	private final Gson gson;
	private final String url;

	LeaderboardSync(OkHttpClient http, Gson gson, String url)
	{
		this.http = http;
		this.gson = gson;
		this.url = url;
	}

	/** Everything that is sent about one account. Immutable so it can be handed between threads. */
	static final class Snapshot
	{
		final String name;
		final long xp;
		final long bankstandingSeconds;
		final long clanHallWealth;
		final long balloons;
		final long gildedChains;
		final long sittingSeconds;
		final long bossDeaths;
		final long gnomesKilled;
		final long caneHeldSeconds;
		final long geSecondsIn;
		final long geSecondsOut;

		Snapshot(String name, long xp, long bankstandingSeconds, long clanHallWealth, long balloons, long gildedChains,
			long sittingSeconds, long bossDeaths, long gnomesKilled, long caneHeldSeconds, long geSecondsIn, long geSecondsOut)
		{
			this.name = name;
			this.xp = xp;
			this.bankstandingSeconds = bankstandingSeconds;
			this.clanHallWealth = clanHallWealth;
			this.balloons = balloons;
			this.gildedChains = gildedChains;
			this.sittingSeconds = sittingSeconds;
			this.bossDeaths = bossDeaths;
			this.gnomesKilled = gnomesKilled;
			this.caneHeldSeconds = caneHeldSeconds;
			this.geSecondsIn = geSecondsIn;
			this.geSecondsOut = geSecondsOut;
		}

		/** Every field is the larger of the two, so restoring from the site can never lose local progress. */
		Snapshot mergedWith(Snapshot saved)
		{
			return new Snapshot(name,
				Math.max(xp, saved.xp),
				Math.max(bankstandingSeconds, saved.bankstandingSeconds),
				Math.max(clanHallWealth, saved.clanHallWealth),
				Math.max(balloons, saved.balloons),
				Math.max(gildedChains, saved.gildedChains),
				Math.max(sittingSeconds, saved.sittingSeconds),
				Math.max(bossDeaths, saved.bossDeaths),
				Math.max(gnomesKilled, saved.gnomesKilled),
				Math.max(caneHeldSeconds, saved.caneHeldSeconds),
				Math.max(geSecondsIn, saved.geSecondsIn),
				Math.max(geSecondsOut, saved.geSecondsOut));
		}

		/** True if every field of this snapshot is already at least what {@code other} has. */
		boolean covers(Snapshot other)
		{
			return xp >= other.xp && bankstandingSeconds >= other.bankstandingSeconds
				&& clanHallWealth >= other.clanHallWealth && balloons >= other.balloons
				&& gildedChains >= other.gildedChains && sittingSeconds >= other.sittingSeconds
				&& bossDeaths >= other.bossDeaths && gnomesKilled >= other.gnomesKilled
				&& caneHeldSeconds >= other.caneHeldSeconds && geSecondsIn >= other.geSecondsIn
				&& geSecondsOut >= other.geSecondsOut;
		}

		/** A brand-new account with nothing recorded yet is not worth a leaderboard row. */
		boolean hasProgress()
		{
			return xp > 0 || bankstandingSeconds > 0 || clanHallWealth > 0 || balloons > 0 || gildedChains > 0
				|| sittingSeconds > 0 || bossDeaths > 0 || gnomesKilled > 0 || caneHeldSeconds > 0
				|| geSecondsIn > 0 || geSecondsOut > 0;
		}
	}

	enum Kind
	{
		SYNCED, THROTTLED, BAD_CODE, NAME_NOT_LINKED, PLUGIN_OUTDATED, RATE_LIMITED, SERVER_ERROR, NETWORK_ERROR
	}

	static final class Result
	{
		final Kind kind;
		final int rank;
		final int total;
		/** Set when the site says this install is behind the saved row: the saved xp and stats to restore from. */
		final Snapshot restore;

		Result(Kind kind, int rank, int total)
		{
			this(kind, rank, total, null);
		}

		Result(Kind kind, int rank, int total, Snapshot restore)
		{
			this.kind = kind;
			this.rank = rank;
			this.total = total;
			this.restore = restore;
		}

		/** True for outcomes the player can do something about (as opposed to success or a plain retry). */
		boolean needsAttention()
		{
			return kind == Kind.BAD_CODE || kind == Kind.NAME_NOT_LINKED || kind == Kind.PLUGIN_OUTDATED;
		}

		boolean isError()
		{
			return kind != Kind.SYNCED && kind != Kind.THROTTLED;
		}

		/** How long to stop trying after this result; retrying a rejected code every few minutes only adds noise. */
		long pauseMillis()
		{
			if (needsAttention())
			{
				return PAUSE_AFTER_REJECTION_MS;
			}
			return kind == Kind.RATE_LIMITED ? PAUSE_AFTER_RATE_LIMIT_MS : 0;
		}

		/** The line shown in the plugin's sidebar panel. */
		String describe(String playerName)
		{
			switch (kind)
			{
				case SYNCED:
					if (restore != null)
					{
						return "Leaderboard: restoring your XP from trailsocial.net";
					}
					return rank > 0 && total > 0 ? "Leaderboard: synced, rank #" + rank + " of " + total : "Leaderboard: synced";
				case THROTTLED:
					return "Leaderboard: synced";
				case BAD_CODE:
					return "Leaderboard: link code not recognised. Generate a new one on your trailsocial.net profile";
				case NAME_NOT_LINKED:
					return "Leaderboard: " + (playerName == null ? "this account" : playerName)
						+ " isn't linked to your TrailSocial account. Add it under Leaderboard Sync on your profile";
				case PLUGIN_OUTDATED:
					return "Leaderboard: the plugin needs an update to keep syncing";
				case RATE_LIMITED:
					return "Leaderboard: sending too often, paused for a while";
				case SERVER_ERROR:
					return "Leaderboard: trailsocial.net had a problem, will retry";
				default:
					return "Leaderboard: can't reach trailsocial.net, will retry";
			}
		}
	}

	static String buildPayload(String code, Snapshot s)
	{
		JsonObject stats = new JsonObject();
		stats.addProperty("bankstandingSeconds", s.bankstandingSeconds);
		stats.addProperty("clanHallWealth", s.clanHallWealth);
		stats.addProperty("balloons", s.balloons);
		stats.addProperty("gildedChains", s.gildedChains);
		stats.addProperty("sittingSeconds", s.sittingSeconds);
		stats.addProperty("bossDeaths", s.bossDeaths);
		stats.addProperty("gnomesKilled", s.gnomesKilled);
		stats.addProperty("caneHeldSeconds", s.caneHeldSeconds);
		stats.addProperty("geSecondsIn", s.geSecondsIn);
		stats.addProperty("geSecondsOut", s.geSecondsOut);

		JsonObject payload = new JsonObject();
		payload.addProperty("v", PROTOCOL_VERSION);
		payload.addProperty("token", code);
		payload.addProperty("name", s.name);
		payload.addProperty("xp", s.xp);
		payload.add("stats", stats);

		JsonObject body = new JsonObject();
		body.addProperty("action", "sync");
		body.add("payload", payload);
		return body.toString();
	}

	static Result parseResponse(Gson gson, int httpCode, String body)
	{
		JsonObject json = null;
		try
		{
			json = gson.fromJson(body, JsonObject.class);
		}
		catch (RuntimeException e)
		{
			// not JSON (e.g. an error page from a proxy); the status code below decides
		}
		String error = stringOr(json, "error");

		if (httpCode >= 200 && httpCode < 300 && json != null && json.has("ok"))
		{
			if (booleanOr(json, "throttled"))
			{
				return new Result(Kind.THROTTLED, 0, 0);
			}
			Snapshot restore = booleanOr(json, "behind") ? parseRestore(json) : null;
			return new Result(Kind.SYNCED, intOr(json, "rank"), intOr(json, "total"), restore);
		}

		switch (httpCode)
		{
			case 401:
				return new Result(Kind.BAD_CODE, 0, 0);
			case 403:
				return new Result("name_not_linked".equals(error) ? Kind.NAME_NOT_LINKED : Kind.BAD_CODE, 0, 0);
			case 400:
				if ("unsupported_version".equals(error))
				{
					return new Result(Kind.PLUGIN_OUTDATED, 0, 0);
				}
				return new Result("invalid_name".equals(error) ? Kind.NAME_NOT_LINKED : Kind.SERVER_ERROR, 0, 0);
			case 429:
				return new Result(Kind.RATE_LIMITED, 0, 0);
			default:
				return new Result(Kind.SERVER_ERROR, 0, 0);
		}
	}

	/** One player's row on the public leaderboard, as the chat commands need it. */
	static final class BoardRow
	{
		final String name;
		final int level;
		final long xp;
		final int rank;
		final Snapshot stats;

		BoardRow(String name, int level, long xp, int rank, Snapshot stats)
		{
			this.name = name;
			this.level = level;
			this.xp = xp;
			this.rank = rank;
			this.stats = stats;
		}
	}

	/** The public leaderboard, searchable by player name. */
	static final class Board
	{
		private final Map<String, BoardRow> rows;
		final int total;

		Board(Map<String, BoardRow> rows, int total)
		{
			this.rows = rows;
			this.total = total;
		}

		/** The row for a player, whatever the capitalisation or the kind of space/underscore in the name. */
		BoardRow find(String playerName)
		{
			return rows.get(normalizeName(playerName));
		}
	}

	/** OSRS treats spaces, underscores, hyphens and non-breaking spaces as the same character, and names ignore case. */
	static String normalizeName(String name)
	{
		return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[\\u00a0_\\-\\s]+", " ").trim();
	}

	/** @return the board, or null if the reply isn't a usable leaderboard */
	static Board parseBoard(Gson gson, String body)
	{
		JsonObject json = null;
		try
		{
			json = gson.fromJson(body, JsonObject.class);
		}
		catch (RuntimeException e)
		{
			// not JSON
		}
		if (json == null || !booleanOr(json, "ok"))
		{
			return null;
		}
		JsonElement entries = json.get("entries");
		if (entries == null || !entries.isJsonArray())
		{
			return null;
		}

		Map<String, BoardRow> rows = new HashMap<>();
		for (JsonElement element : entries.getAsJsonArray())
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			JsonObject row = element.getAsJsonObject();
			String name = stringOr(row, "name");
			if (name == null)
			{
				continue;
			}
			JsonElement statsElement = row.get("stats");
			JsonObject stats = statsElement != null && statsElement.isJsonObject() ? statsElement.getAsJsonObject() : new JsonObject();
			rows.put(normalizeName(name), new BoardRow(name, intOr(row, "level"), longOr(row, "xp"), intOr(row, "rank"),
				new Snapshot(name, longOr(row, "xp"),
					longOr(stats, "bankstandingSeconds"), longOr(stats, "clanHallWealth"), longOr(stats, "balloons"),
					longOr(stats, "gildedChains"), longOr(stats, "sittingSeconds"), longOr(stats, "bossDeaths"),
					longOr(stats, "gnomesKilled"), longOr(stats, "caneHeldSeconds"), longOr(stats, "geSecondsIn"),
					longOr(stats, "geSecondsOut"))));
		}
		return new Board(rows, rows.size());
	}

	/** Downloads the public leaderboard (no player names are sent). {@code done} gets null if that fails. */
	void fetchBoard(Consumer<Board> done)
	{
		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, "{\"action\":\"board\"}"))
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				done.accept(null);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				Board board = null;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						board = parseBoard(gson, body.string());
					}
				}
				catch (IOException e)
				{
					// leave board null
				}
				done.accept(board);
			}
		});
	}

	void send(String code, Snapshot snapshot, Consumer<Result> done)
	{
		Request request = new Request.Builder()
			.url(url)
			.post(RequestBody.create(JSON, buildPayload(code, snapshot)))
			.build();

		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				done.accept(new Result(Kind.NETWORK_ERROR, 0, 0));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				Result result;
				try (ResponseBody body = response.body())
				{
					result = parseResponse(gson, response.code(), body == null ? "" : body.string());
				}
				catch (IOException e)
				{
					result = new Result(Kind.NETWORK_ERROR, 0, 0);
				}
				done.accept(result);
			}
		});
	}

	private static String stringOr(JsonObject json, String key)
	{
		JsonElement e = json == null ? null : json.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
	}

	/** The saved row the site hands back to a client that is behind it, or null if it can't be read. */
	private static Snapshot parseRestore(JsonObject json)
	{
		JsonElement saved = json.get("restore");
		if (saved == null || !saved.isJsonObject())
		{
			return null;
		}
		JsonObject row = saved.getAsJsonObject();
		JsonElement statsElement = row.get("stats");
		JsonObject stats = statsElement != null && statsElement.isJsonObject() ? statsElement.getAsJsonObject() : new JsonObject();
		return new Snapshot(
			stringOr(json, "name"),
			Math.min(longOr(row, "xp"), BankstandingXP.MAX_XP),
			longOr(stats, "bankstandingSeconds"),
			longOr(stats, "clanHallWealth"),
			longOr(stats, "balloons"),
			longOr(stats, "gildedChains"),
			longOr(stats, "sittingSeconds"),
			longOr(stats, "bossDeaths"),
			longOr(stats, "gnomesKilled"),
			longOr(stats, "caneHeldSeconds"),
			longOr(stats, "geSecondsIn"),
			longOr(stats, "geSecondsOut"));
	}

	/** A number from the site, never negative and never absurd; anything unreadable counts as 0. */
	private static long longOr(JsonObject json, String key)
	{
		JsonElement e = json.get(key);
		try
		{
			long v = e != null && e.isJsonPrimitive() ? e.getAsLong() : 0;
			return Math.max(0, Math.min(v, MAX_RESTORED_VALUE));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static int intOr(JsonObject json, String key)
	{
		JsonElement e = json.get(key);
		try
		{
			return e != null && e.isJsonPrimitive() ? e.getAsInt() : 0;
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static boolean booleanOr(JsonObject json, String key)
	{
		JsonElement e = json.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsBoolean();
	}
}
