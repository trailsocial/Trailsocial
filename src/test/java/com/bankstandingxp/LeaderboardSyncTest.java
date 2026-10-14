package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

public class LeaderboardSyncTest
{
	private static final Gson GSON = new Gson();

	private static LeaderboardSync.Snapshot snapshot(String name)
	{
		return new LeaderboardSync.Snapshot(name, 12345, 111, 222, 3, 4, 555, 6, 7, 888, 999, 1010);
	}

	private static LeaderboardSync.Kind kind(int http, String body)
	{
		return LeaderboardSync.parseResponse(GSON, http, body).kind;
	}

	@Test
	public void payloadHasTheShapeTheServerExpects()
	{
		JsonObject body = GSON.fromJson(LeaderboardSync.buildPayload("tsb_code", snapshot("Noble Mobile")), JsonObject.class);
		assertEquals("sync", body.get("action").getAsString());

		JsonObject payload = body.getAsJsonObject("payload");
		assertEquals(LeaderboardSync.PROTOCOL_VERSION, payload.get("v").getAsInt());
		assertEquals("tsb_code", payload.get("token").getAsString());
		assertEquals("Noble Mobile", payload.get("name").getAsString());
		assertEquals(12345L, payload.get("xp").getAsLong());

		JsonObject stats = payload.getAsJsonObject("stats");
		assertEquals(111L, stats.get("bankstandingSeconds").getAsLong());
		assertEquals(222L, stats.get("clanHallWealth").getAsLong());
		assertEquals(3L, stats.get("balloons").getAsLong());
		assertEquals(4L, stats.get("gildedChains").getAsLong());
		assertEquals(555L, stats.get("sittingSeconds").getAsLong());
		assertEquals(6L, stats.get("bossDeaths").getAsLong());
		assertEquals(7L, stats.get("gnomesKilled").getAsLong());
		assertEquals(888L, stats.get("caneHeldSeconds").getAsLong());
		assertEquals(999L, stats.get("geSecondsIn").getAsLong());
		assertEquals(1010L, stats.get("geSecondsOut").getAsLong());
		assertEquals("every stat the server knows about, and nothing else", 10, stats.size());
	}

	@Test
	public void payloadEscapesAwkwardText()
	{
		String name = "Iron \"Man\"\\\n";
		JsonObject body = GSON.fromJson(LeaderboardSync.buildPayload("a\"b", snapshot(name)), JsonObject.class);
		assertEquals(name, body.getAsJsonObject("payload").get("name").getAsString());
		assertEquals("a\"b", body.getAsJsonObject("payload").get("token").getAsString());
	}

	@Test
	public void successCarriesRankAndTotal()
	{
		LeaderboardSync.Result r = LeaderboardSync.parseResponse(GSON, 200,
			"{\"ok\":true,\"name\":\"Zezima\",\"xp\":10,\"level\":1,\"rank\":3,\"total\":12,\"clamped\":false,\"nextSyncSeconds\":300}");
		assertEquals(LeaderboardSync.Kind.SYNCED, r.kind);
		assertEquals(3, r.rank);
		assertEquals(12, r.total);
		assertFalse(r.isError());
		assertEquals(0, r.pauseMillis());
		assertEquals("Leaderboard: synced, rank #3 of 12", r.describe("Zezima"));
	}

	@Test
	public void throttledIsQuietSuccess()
	{
		LeaderboardSync.Result r = LeaderboardSync.parseResponse(GSON, 200, "{\"ok\":true,\"throttled\":true,\"retryInSeconds\":40}");
		assertEquals(LeaderboardSync.Kind.THROTTLED, r.kind);
		assertFalse(r.isError());
		assertFalse(r.needsAttention());
	}

	@Test
	public void everyServerReplyMapsToTheRightOutcome()
	{
		assertEquals(LeaderboardSync.Kind.BAD_CODE, kind(401, "{\"error\":\"invalid_token\"}"));
		assertEquals(LeaderboardSync.Kind.NAME_NOT_LINKED, kind(403, "{\"error\":\"name_not_linked\",\"name\":\"X\"}"));
		assertEquals(LeaderboardSync.Kind.BAD_CODE, kind(403, "{\"error\":\"something_else\"}"));
		assertEquals(LeaderboardSync.Kind.PLUGIN_OUTDATED, kind(400, "{\"error\":\"unsupported_version\",\"supported\":2}"));
		assertEquals(LeaderboardSync.Kind.NAME_NOT_LINKED, kind(400, "{\"error\":\"invalid_name\"}"));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(400, "{\"error\":\"Invalid JSON body\"}"));
		assertEquals(LeaderboardSync.Kind.RATE_LIMITED, kind(429, "{\"error\":\"rate_limited\"}"));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(500, "{\"error\":\"Internal error\"}"));
	}

	@Test
	public void garbageFromAProxyIsAServerErrorNotACrash()
	{
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(502, "<html>Bad gateway</html>"));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(503, ""));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(200, "not json at all"));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(200, "{}"));
		assertEquals(LeaderboardSync.Kind.SERVER_ERROR, kind(200, "[1,2,3]"));
	}

	@Test
	public void rejectedCodesPauseSyncingButTransientFailuresDoNot()
	{
		long half = 30 * 60_000L;
		assertEquals(half, new LeaderboardSync.Result(LeaderboardSync.Kind.BAD_CODE, 0, 0).pauseMillis());
		assertEquals(half, new LeaderboardSync.Result(LeaderboardSync.Kind.NAME_NOT_LINKED, 0, 0).pauseMillis());
		assertEquals(half, new LeaderboardSync.Result(LeaderboardSync.Kind.PLUGIN_OUTDATED, 0, 0).pauseMillis());
		assertEquals(10 * 60_000L, new LeaderboardSync.Result(LeaderboardSync.Kind.RATE_LIMITED, 0, 0).pauseMillis());
		assertEquals(0, new LeaderboardSync.Result(LeaderboardSync.Kind.NETWORK_ERROR, 0, 0).pauseMillis());
		assertEquals(0, new LeaderboardSync.Result(LeaderboardSync.Kind.SERVER_ERROR, 0, 0).pauseMillis());
		assertTrue(new LeaderboardSync.Result(LeaderboardSync.Kind.BAD_CODE, 0, 0).needsAttention());
		assertFalse(new LeaderboardSync.Result(LeaderboardSync.Kind.NETWORK_ERROR, 0, 0).needsAttention());
	}

	@Test
	public void statusLinesTellThePlayerWhatToDo()
	{
		assertTrue(new LeaderboardSync.Result(LeaderboardSync.Kind.NAME_NOT_LINKED, 0, 0).describe("Noble Mobile").contains("Noble Mobile"));
		assertTrue(new LeaderboardSync.Result(LeaderboardSync.Kind.BAD_CODE, 0, 0).describe("x").contains("Generate a new one"));
		assertTrue(new LeaderboardSync.Result(LeaderboardSync.Kind.NETWORK_ERROR, 0, 0).describe("x").contains("will retry"));
		assertEquals("Leaderboard: synced", new LeaderboardSync.Result(LeaderboardSync.Kind.SYNCED, 0, 0).describe("x"));
	}

	private static final String BEHIND_BODY = "{\"ok\":true,\"behind\":true,\"name\":\"Noble Mobile\",\"xp\":9000,\"level\":37,\"rank\":2,\"total\":5,"
		+ "\"restore\":{\"xp\":9000,\"stats\":{\"bankstandingSeconds\":7000,\"clanHallWealth\":48200000,\"balloons\":40,\"gildedChains\":3,"
		+ "\"sittingSeconds\":5400,\"bossDeaths\":17,\"gnomesKilled\":1320,\"caneHeldSeconds\":86400,\"geSecondsIn\":1000,\"geSecondsOut\":20000}}}";

	@Test
	public void aBehindReplyCarriesTheSavedRowToRestoreFrom()
	{
		LeaderboardSync.Result r = LeaderboardSync.parseResponse(GSON, 200, BEHIND_BODY);
		assertEquals(LeaderboardSync.Kind.SYNCED, r.kind);
		assertEquals(2, r.rank);
		assertEquals(5, r.total);
		LeaderboardSync.Snapshot s = r.restore;
		assertEquals("Noble Mobile", s.name);
		assertEquals(9000L, s.xp);
		assertEquals(7000L, s.bankstandingSeconds);
		assertEquals(48_200_000L, s.clanHallWealth);
		assertEquals(40L, s.balloons);
		assertEquals(3L, s.gildedChains);
		assertEquals(5400L, s.sittingSeconds);
		assertEquals(17L, s.bossDeaths);
		assertEquals(1320L, s.gnomesKilled);
		assertEquals(86400L, s.caneHeldSeconds);
		assertEquals(1000L, s.geSecondsIn);
		assertEquals(20000L, s.geSecondsOut);
		assertEquals("Leaderboard: restoring your XP from trailsocial.net", r.describe("Noble Mobile"));
	}

	@Test
	public void aNormalReplyHasNothingToRestore()
	{
		assertEquals(null, LeaderboardSync.parseResponse(GSON, 200, "{\"ok\":true,\"rank\":1,\"total\":1}").restore);
		// restore data without the behind flag must be ignored
		assertEquals(null, LeaderboardSync.parseResponse(GSON, 200, BEHIND_BODY.replace("\"behind\":true,", "")).restore);
	}

	@Test
	public void unreadableRestoreDataIsIgnoredNotTrusted()
	{
		assertEquals(null, LeaderboardSync.parseResponse(GSON, 200, "{\"ok\":true,\"behind\":true}").restore);
		assertEquals(null, LeaderboardSync.parseResponse(GSON, 200, "{\"ok\":true,\"behind\":true,\"restore\":\"nope\"}").restore);
		LeaderboardSync.Snapshot s = LeaderboardSync.parseResponse(GSON, 200,
			"{\"ok\":true,\"behind\":true,\"restore\":{\"xp\":\"lots\",\"stats\":{\"balloons\":-5,\"bossDeaths\":99999999999999999}}}").restore;
		assertEquals("garbage numbers become 0", 0L, s.xp);
		assertEquals("negatives become 0", 0L, s.balloons);
		assertTrue("absurd numbers are capped", s.bossDeaths <= 10_000_000_000_000L);
	}

	@Test
	public void restoredXpIsCappedAtTheGameMaximum()
	{
		LeaderboardSync.Snapshot s = LeaderboardSync.parseResponse(GSON, 200,
			"{\"ok\":true,\"behind\":true,\"restore\":{\"xp\":9999999999,\"stats\":{}}}").restore;
		assertEquals(BankstandingXP.MAX_XP, s.xp);
	}

	@Test
	public void mergingKeepsTheLargerOfEveryValue()
	{
		LeaderboardSync.Snapshot local = new LeaderboardSync.Snapshot("A", 50, 10, 900, 1, 0, 5, 0, 7, 100, 20, 30);
		LeaderboardSync.Snapshot saved = new LeaderboardSync.Snapshot("B", 9000, 7000, 100, 40, 3, 5400, 17, 2, 86400, 1000, 20000);
		LeaderboardSync.Snapshot m = local.mergedWith(saved);
		assertEquals("A", m.name);
		assertEquals(9000L, m.xp);
		assertEquals(7000L, m.bankstandingSeconds);
		assertEquals("local progress is never lost", 900L, m.clanHallWealth);
		assertEquals(40L, m.balloons);
		assertEquals(7L, m.gnomesKilled);
		assertEquals(20000L, m.geSecondsOut);
		assertTrue(m.covers(local));
		assertTrue(m.covers(saved));
	}

	@Test
	public void coversMeansNothingWouldChange()
	{
		LeaderboardSync.Snapshot a = new LeaderboardSync.Snapshot("A", 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10);
		assertTrue(a.covers(a));
		assertFalse("one field behind is enough", a.covers(new LeaderboardSync.Snapshot("A", 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11)));
	}

	private static final String BOARD_BODY = "{\"ok\":true,\"generatedAt\":1,\"mine\":[],\"entries\":["
		+ "{\"rank\":1,\"key\":\"bulkies\",\"name\":\"Bulkies\",\"level\":43,\"xp\":54321,\"stats\":{\"bankstandingSeconds\":651852,\"gnomesKilled\":5432}},"
		+ "{\"rank\":2,\"key\":\"noble_mobile\",\"name\":\"Noble Mobile\",\"level\":29,\"xp\":12345,\"stats\":{\"bankstandingSeconds\":148500,\"gnomesKilled\":1320,\"bossDeaths\":7,\"clanHallWealth\":21067424759,\"balloons\":11,\"gildedChains\":491,\"sittingSeconds\":2754,\"caneHeldSeconds\":756410,\"geSecondsIn\":359510,\"geSecondsOut\":15212}},"
		+ "{\"rank\":3,\"key\":\"megh\",\"name\":\"Megh\",\"level\":17,\"xp\":3210}]}";

	@Test
	public void namesAreMatchedTheWayOldSchoolDoes()
	{
		assertEquals("noble mobile", LeaderboardSync.normalizeName("Noble Mobile"));
		assertEquals("noble mobile", LeaderboardSync.normalizeName("NOBLE_MOBILE"));
		assertEquals("noble mobile", LeaderboardSync.normalizeName("noble-mobile"));
		assertEquals("noble mobile", LeaderboardSync.normalizeName("Noble Mobile"));
		assertEquals("noble mobile", LeaderboardSync.normalizeName("  noble   mobile "));
		assertEquals("", LeaderboardSync.normalizeName(null));
	}

	@Test
	public void theBoardIsSearchableByPlayerName()
	{
		LeaderboardSync.Board board = LeaderboardSync.parseBoard(GSON, BOARD_BODY);
		assertEquals(3, board.total);
		LeaderboardSync.BoardRow row = board.find("Noble Mobile");
		assertEquals("Noble Mobile", row.name);
		assertEquals(29, row.level);
		assertEquals(12345L, row.xp);
		assertEquals(2, row.rank);
		assertEquals(148_500L, row.stats.bankstandingSeconds);
		assertEquals(1320L, row.stats.gnomesKilled);
		assertEquals(7L, row.stats.bossDeaths);
		assertEquals(21_067_424_759L, row.stats.clanHallWealth);
		assertEquals(11L, row.stats.balloons);
		assertEquals(491L, row.stats.gildedChains);
		assertEquals(2754L, row.stats.sittingSeconds);
		assertEquals(756_410L, row.stats.caneHeldSeconds);
		assertEquals(359_510L, row.stats.geSecondsIn);
		assertEquals(15_212L, row.stats.geSecondsOut);
		assertEquals(12_345L, row.stats.xp);
		assertTrue("underscore, case and nbsp all find the same player",
			board.find("NOBLE_MOBILE") == row && board.find("noble mobile") == row);
	}

	@Test
	public void aPlayerNotOnTheBoardIsNotFound()
	{
		LeaderboardSync.Board board = LeaderboardSync.parseBoard(GSON, BOARD_BODY);
		assertEquals(null, board.find("Zezima"));
		assertEquals(null, board.find(""));
		assertEquals(null, board.find(null));
	}

	@Test
	public void missingStatsDefaultToZero()
	{
		LeaderboardSync.BoardRow row = LeaderboardSync.parseBoard(GSON, BOARD_BODY).find("Megh");
		assertEquals(0L, row.stats.bankstandingSeconds);
		assertEquals(0L, row.stats.gnomesKilled);
		assertEquals(3, row.rank);
	}

	@Test
	public void anUnusableBoardReplyIsNull()
	{
		assertEquals(null, LeaderboardSync.parseBoard(GSON, ""));
		assertEquals(null, LeaderboardSync.parseBoard(GSON, "<html>502</html>"));
		assertEquals(null, LeaderboardSync.parseBoard(GSON, "{}"));
		assertEquals(null, LeaderboardSync.parseBoard(GSON, "{\"ok\":false,\"entries\":[]}"));
		assertEquals(null, LeaderboardSync.parseBoard(GSON, "{\"ok\":true}"));
		assertEquals(null, LeaderboardSync.parseBoard(GSON, "{\"ok\":true,\"entries\":\"nope\"}"));
	}

	@Test
	public void anEmptyBoardIsStillABoard()
	{
		LeaderboardSync.Board board = LeaderboardSync.parseBoard(GSON, "{\"ok\":true,\"entries\":[]}");
		assertEquals(0, board.total);
		assertEquals(null, board.find("Anyone"));
	}

	@Test
	public void junkRowsAreSkippedNotFatal()
	{
		LeaderboardSync.Board board = LeaderboardSync.parseBoard(GSON,
			"{\"ok\":true,\"entries\":[5,\"x\",{\"level\":3},{\"name\":\"Good One\",\"level\":9,\"xp\":99,\"rank\":1}]}");
		assertEquals(1, board.total);
		assertEquals(9, board.find("good one").level);
	}

	@Test
	public void emptyAccountsAreNotWorthReporting()
	{
		assertFalse(new LeaderboardSync.Snapshot("A", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).hasProgress());
		assertTrue(new LeaderboardSync.Snapshot("A", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).hasProgress());
		assertTrue(new LeaderboardSync.Snapshot("A", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5).hasProgress());
	}
}
