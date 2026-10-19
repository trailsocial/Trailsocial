package com.bankstandingxp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The chat commands the plugin understands. Any player running the plugin who sees one of these in chat
 * has it replaced on their own screen with that player's stats, the same way RuneLite's built-in
 * "!lvl attack" works. These are matched by the plugin itself rather than registered with RuneLite's
 * command manager, because "!lvl" is already owned by the built-in Chat Commands plugin.
 */
final class BankstandingChatCommands
{
	enum Command
	{
		/** !lvl bankstanding */
		LEVEL("!lvl bankstanding", null),
		/** !hours */
		HOURS("!hours", "Hours bankstanding"),
		/** !gnomes */
		GNOMES("!gnomes", "Gnomes killed"),
		/** !deaths: boss and raid deaths */
		DEATHS("!deaths", "Boss deaths"),
		/** !wealth: wealth picked up in the Clan Hall */
		WEALTH("!wealth", "Clan Hall wealth"),
		/** !balloons */
		BALLOONS("!balloons", "Balloons burst"),
		/** !chains: gilded chains picked up */
		CHAINS("!chains", "Gilded chains"),
		/** !chair: hours spent sitting (not "!sitting", which the game itself treats as an emote) */
		SITTING("!chair", "Hours sitting"),
		/** !cane: hours the Trailblazer cane has been held */
		CANE("!cane", "Hours holding the cane"),
		/** !ge: hours in the Grand Exchange */
		GE("!ge", "Hours in the GE"),
		/** !geout: hours outside the Grand Exchange */
		GE_OUT("!geout", "Hours outside the GE"),
		/** !events: the next TrailSocial events; about no player, so it never needs the leaderboard */
		EVENTS("!events", null);

		final String text;
		/** What is said before the number, or null for the commands that have their own layout. */
		final String label;

		Command(String text, String label)
		{
			this.text = text;
			this.label = label;
		}

		/** The number this command reports for a player, already formatted. Not used by {@link #LEVEL} or {@link #EVENTS}. */
		String value(LeaderboardSync.Snapshot stats)
		{
			switch (this)
			{
				case HOURS:
					return formatHours(stats.bankstandingSeconds);
				case GNOMES:
					return formatNumber(stats.gnomesKilled);
				case DEATHS:
					return formatNumber(stats.bossDeaths);
				case WEALTH:
					return formatNumber(stats.clanHallWealth) + " gp";
				case BALLOONS:
					return formatNumber(stats.balloons);
				case CHAINS:
					return formatNumber(stats.gildedChains);
				case SITTING:
					return formatHours(stats.sittingSeconds);
				case CANE:
					return formatHours(stats.caneHeldSeconds);
				case GE:
					return formatHours(stats.geSecondsIn);
				case GE_OUT:
					return formatHours(stats.geSecondsOut);
				default:
					throw new IllegalStateException(this + " has no single value");
			}
		}
	}

	private BankstandingChatCommands()
	{
	}

	/** @return the command a chat message is, or null if it isn't one of ours (matching ignores case and extra spaces) */
	static Command parse(String message)
	{
		if (message == null)
		{
			return null;
		}
		String normalized = message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
		for (Command command : Command.values())
		{
			if (command.text.equals(normalized))
			{
				return command;
			}
		}
		return null;
	}

	private static final int MAX_EVENTS_SHOWN = 3;

	/**
	 * What {@code !events} says: the next few events that haven't ended, soonest first, each as "TITLE (mm/dd)"
	 * (or "mm/dd-mm/dd" when it runs over several days). Events with no usable date go last.
	 */
	static String formatEvents(List<TrailSocialEvent> events, LocalDate today)
	{
		List<TrailSocialEvent> upcoming = new ArrayList<>();
		for (TrailSocialEvent event : events)
		{
			if (event == null || event.title == null || event.title.trim().isEmpty())
			{
				continue;
			}
			LocalDate last = parseDay(event.endDate != null && !event.endDate.isEmpty() ? event.endDate : event.date);
			if (last == null || !last.isBefore(today)) // no date at all never counts as past
			{
				upcoming.add(event);
			}
		}
		// the server already sorts soonest first, but don't depend on it: undated events go to the end
		upcoming.sort((a, b) ->
		{
			LocalDate x = parseDay(a.date);
			LocalDate y = parseDay(b.date);
			if (x == null || y == null)
			{
				return x == y ? 0 : x == null ? 1 : -1;
			}
			return x.compareTo(y);
		});

		if (upcoming.isEmpty())
		{
			return "No upcoming TrailSocial events";
		}
		StringBuilder text = new StringBuilder("Upcoming events: ");
		for (int i = 0; i < Math.min(upcoming.size(), MAX_EVENTS_SHOWN); i++)
		{
			TrailSocialEvent event = upcoming.get(i);
			text.append(i == 0 ? "" : ", ").append(event.title.trim());
			String days = formatDays(event);
			if (!days.isEmpty())
			{
				text.append(" (").append(days).append(')');
			}
		}
		if (upcoming.size() > MAX_EVENTS_SHOWN)
		{
			text.append(" +").append(upcoming.size() - MAX_EVENTS_SHOWN).append(" more");
		}
		return text.toString();
	}

	private static String formatDays(TrailSocialEvent event)
	{
		LocalDate start = parseDay(event.date);
		LocalDate end = parseDay(event.endDate);
		if (start == null)
		{
			return "";
		}
		String first = String.format(Locale.US, "%02d/%02d", start.getMonthValue(), start.getDayOfMonth());
		if (end == null || end.equals(start))
		{
			return first;
		}
		return first + "-" + String.format(Locale.US, "%02d/%02d", end.getMonthValue(), end.getDayOfMonth());
	}

	/** "2026-09-01" -> that day; anything blank or malformed -> null. */
	private static LocalDate parseDay(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return null;
		}
		try
		{
			return LocalDate.parse(value.trim());
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}

	/** Seconds as hours with one decimal, e.g. 148500 -> "41.3". */
	static String formatHours(long seconds)
	{
		return String.format(Locale.US, "%.1f", seconds / 3600.0);
	}

	/** 62303 -> "62,303". */
	static String formatNumber(long value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	/** Everything a command can show about one player. {@code rank} is 0 when unknown. */
	static final class Stats
	{
		final String name;
		final int level;
		final long xp;
		final int rank;
		final int total;
		final LeaderboardSync.Snapshot stats;

		Stats(String name, int level, long xp, int rank, int total, LeaderboardSync.Snapshot stats)
		{
			this.name = name;
			this.level = level;
			this.xp = xp;
			this.rank = rank;
			this.total = total;
			this.stats = stats;
		}
	}
}
