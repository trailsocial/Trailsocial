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

	@Test
	public void realClientAgainstRealServer() throws Exception
	{
		String url = System.getenv("TS_LEADERBOARD_URL");
		String code = System.getenv("TS_LEADERBOARD_CODE");
		String name = System.getenv("TS_LEADERBOARD_NAME");
		assumeNotNull(url, code, name);

		LeaderboardSync.Result first = send(url, code, snapshot(name, 12_400));
		assertEquals(LeaderboardSync.Kind.SYNCED, first.kind);
		assertTrue("rank " + first.rank, first.rank >= 1);
		assertTrue("total " + first.total, first.total >= first.rank);
		System.out.println("first sync: " + first.describe(name));

		LeaderboardSync.Result again = send(url, code, snapshot(name, 12_450));
		assertEquals("a second report right away is throttled", LeaderboardSync.Kind.THROTTLED, again.kind);

		assertEquals(LeaderboardSync.Kind.BAD_CODE, send(url, "tsb_not-a-real-code-at-all-0000", snapshot(name, 1)).kind);
		assertEquals(LeaderboardSync.Kind.NAME_NOT_LINKED, send(url, code, snapshot("Hey Jase", 1)).kind);
		assertEquals(LeaderboardSync.Kind.NETWORK_ERROR, send("http://127.0.0.1:1/nothing-here", code, snapshot(name, 1)).kind);
	}
}
