package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LevelUpWatcherTest
{
	/** A board from name/level pairs, e.g. board("Noble Mobile", 29, "Megh", 17). */
	private static LeaderboardSync.Board board(Object... nameAndLevel)
	{
		Map<String, LeaderboardSync.BoardRow> rows = new HashMap<>();
		for (int i = 0; i < nameAndLevel.length; i += 2)
		{
			String name = (String) nameAndLevel[i];
			int level = (Integer) nameAndLevel[i + 1];
			LeaderboardSync.Snapshot stats = new LeaderboardSync.Snapshot(name, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			rows.put(LeaderboardSync.normalizeName(name), new LeaderboardSync.BoardRow(name, level, 0, i / 2 + 1, stats));
		}
		return new LeaderboardSync.Board(rows, rows.size());
	}

	@Test
	public void theFirstBoardOnlyRecordsLevelsSoLoggingInAnnouncesNothing()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		assertTrue(watcher.update(board("Noble Mobile", 29, "Megh", 17), "Me").isEmpty());
	}

	@Test
	public void aHigherLevelIsAnnouncedOnce()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Noble Mobile", 29, "Megh", 17), null);
		List<LevelUpWatcher.LevelUp> ups = watcher.update(board("Noble Mobile", 30, "Megh", 17), null);
		assertEquals(1, ups.size());
		assertEquals("Noble Mobile", ups.get(0).name);
		assertEquals(30, ups.get(0).level);
		assertTrue("the same board again is not news", watcher.update(board("Noble Mobile", 30, "Megh", 17), null).isEmpty());
	}

	@Test
	public void skippingSeveralLevelsAnnouncesTheNewOne()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Megh", 17), null);
		List<LevelUpWatcher.LevelUp> ups = watcher.update(board("Megh", 20), null);
		assertEquals(1, ups.size());
		assertEquals(20, ups.get(0).level);
	}

	@Test
	public void aLevelThatDropsAndComesBackIsNotAnnouncedAgain()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Megh", 17), null);
		assertEquals(1, watcher.update(board("Megh", 18), null).size());
		assertTrue("xp lost to skilling", watcher.update(board("Megh", 17), null).isEmpty());
		assertTrue("back where they were", watcher.update(board("Megh", 18), null).isEmpty());
		assertEquals("a genuinely new high", 1, watcher.update(board("Megh", 19), null).size());
	}

	@Test
	public void yourOwnLevelUpIsLeftToTheMessageYouAlreadyGet()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Noble Mobile", 29, "Megh", 17), "noble_mobile");
		List<LevelUpWatcher.LevelUp> ups = watcher.update(board("Noble Mobile", 30, "Megh", 18), "NOBLE MOBILE");
		assertEquals(1, ups.size());
		assertEquals("Megh", ups.get(0).name);
	}

	@Test
	public void aPlayerWhoJoinsTheBoardLaterIsRecordedNotAnnounced()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Megh", 17), null);
		assertTrue(watcher.update(board("Megh", 17, "Newbie", 12), null).isEmpty());
		assertEquals(1, watcher.update(board("Megh", 17, "Newbie", 13), null).size());
	}

	@Test
	public void severalAtOnceComeBackInNameOrder()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Zed", 5, "Alpha", 6, "Mid", 7), null);
		List<LevelUpWatcher.LevelUp> ups = watcher.update(board("Zed", 6, "Alpha", 7, "Mid", 7), null);
		assertEquals(2, ups.size());
		assertEquals("Alpha", ups.get(0).name);
		assertEquals("Zed", ups.get(1).name);
	}

	@Test
	public void afterAResetTheNextBoardIsOnlyRecorded()
	{
		LevelUpWatcher watcher = new LevelUpWatcher();
		watcher.update(board("Megh", 17), null);
		watcher.reset(); // left the clan chat, logged out, or the setting was turned off
		assertTrue("levels gained meanwhile are not a backlog of announcements", watcher.update(board("Megh", 25), null).isEmpty());
		assertEquals(1, watcher.update(board("Megh", 26), null).size());
	}
}
