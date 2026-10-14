package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import com.google.gson.Gson;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import org.junit.Test;

/**
 * Talks to a real TrailSocial server, so it only runs when pointed at one:
 *
 *   TS_LEADERBOARD_URL=http://127.0.0.1:18931/.netlify/functions/bankstanding
 *   TS_LEADERBOARD_CODE=tsb_...   (a live link code)
 *   TS_LEADERBOARD_NAME="Noble Mobile"   (a RuneScape name claimed by that code's account)
 */
public class LeaderboardSyncIntegrationTest
{
	private static LeaderboardSync.Result send(String url, String code, LeaderboardSync.Snapshot snapshot) throws InterruptedException
	{
		LeaderboardSync sync = new LeaderboardSync(new OkHttpClient(), new Gson(), url);
		AtomicReference<LeaderboardSync.Result> out = new AtomicReference<>();
		CountDownLatch done = new CountDownLatch(1);
		sync.send(code, snapshot, result ->
		{
			out.set(result);
			done.countDown();
		});
		assertTrue("no answer within 15s", done.await(15, TimeUnit.SECONDS));
		return out.get();
	}

	private static LeaderboardSync.Snapshot snapshot(String name, long xp)
	{
		return new LeaderboardSync.Snapshot(name, xp, 9000, 5_000_000, 12, 1, 1800, 2, 40, 7200, 600, 3000);
	}

	/** Needs only TS_LEADERBOARD_URL: proves the real client can reach the server and is turned away cleanly without a valid code. */
	@Test
	public void serverIsReachableAndRejectsAnUnknownCode() throws Exception
	{
		String url = System.getenv("TS_LEADERBOARD_URL");
		assumeNotNull(url);
		assertEquals(LeaderboardSync.Kind.BAD_CODE, send(url, "tsb_not-a-real-code-at-all-0000", snapshot("Zezima", 1)).kind);
	}

	/** A fresh install reports almost nothing; the server must keep the saved row and hand it back instead. */
	@Test
	public void aFreshInstallGetsItsSavedRowBackInsteadOfOverwritingIt() throws Exception
	{
		String url = System.getenv("TS_LEADERBOARD_URL");
		String code = System.getenv("TS_LEADERBOARD_CODE");
		String name = System.getenv("TS_LEADERBOARD_NAME");
		assumeNotNull(url, code, name);

		LeaderboardSync.Snapshot saved = snapshot(name, 12_400);
		send(url, code, saved); // may be throttled if sent a moment ago; the stored row is the same either way

		LeaderboardSync.Snapshot fresh = new LeaderboardSync.Snapshot(name, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 5);
		LeaderboardSync.Result result = send(url, code, fresh);
		assertEquals(LeaderboardSync.Kind.SYNCED, result.kind);
		assertTrue("the server should hand the saved row back", result.restore != null);
		assertEquals(saved.xp, result.restore.xp);
		assertEquals(saved.clanHallWealth, result.restore.clanHallWealth);
		assertEquals(saved.geSecondsIn, result.restore.geSecondsIn);
		assertEquals(saved.geSecondsOut, result.restore.geSecondsOut);

		// the merge the plugin performs puts a fresh install back to exactly the saved row
		LeaderboardSync.Snapshot merged = fresh.mergedWith(result.restore);
		assertTrue(merged.covers(saved) && saved.covers(merged));

		// and a client that is not behind is not given anything to restore
		LeaderboardSync.Result normal = send(url, code, snapshot(name, 12_400));
		assertTrue(normal.restore == null);
	}

	@Test
	public void realClientAgainstRealServer() throws Exception
	{
		String url = System.getenv("TS_LEADERBOARD_URL");
		String code = System.getenv("TS_LEADERBOARD_CODE");
		String name = System.getenv("TS_LEADERBOARD_NAME");
		assumeNotNull(url, code, name);

		// Another test may have synced this same row moments ago, in which case the server throttles this one.
		LeaderboardSync.Result first = send(url, code, snapshot(name, 12_400));
		assertTrue("unexpected " + first.kind, first.kind == LeaderboardSync.Kind.SYNCED || first.kind == LeaderboardSync.Kind.THROTTLED);
		if (first.kind == LeaderboardSync.Kind.SYNCED)
		{
			assertTrue("rank " + first.rank, first.rank >= 1);
			assertTrue("total " + first.total, first.total >= first.rank);
			System.out.println("first sync: " + first.describe(name));
		}

		LeaderboardSync.Result again = send(url, code, snapshot(name, 12_450));
		assertEquals("a second report right away is throttled", LeaderboardSync.Kind.THROTTLED, again.kind);

		assertEquals(LeaderboardSync.Kind.BAD_CODE, send(url, "tsb_not-a-real-code-at-all-0000", snapshot(name, 1)).kind);
		assertEquals(LeaderboardSync.Kind.NAME_NOT_LINKED, send(url, code, snapshot("Hey Jase", 1)).kind);
		assertEquals(LeaderboardSync.Kind.NETWORK_ERROR, send("http://127.0.0.1:1/nothing-here", code, snapshot(name, 1)).kind);
	}
}
