package com.bankstandingxp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notices when a player on the leaderboard reaches a new Bankstanding level, by comparing each download of the
 * public board with the one before it. Only new highs count, so someone whose level wobbles around a boundary
 * (xp lost to skilling, then regained) is announced once, not every time.
 */
final class LevelUpWatcher
{
	static final class LevelUp
	{
		final String name;
		final int level;

		LevelUp(String name, int level)
		{
			this.name = name;
			this.level = level;
		}
	}

	private final Map<String, Integer> highest = new HashMap<>();
	private boolean seeded;

	/**
	 * @param self the logged-in player's own name, who already gets their own level-up message; may be null
	 * @return the level-ups since the last call. The first board after {@link #reset} only records everyone's
	 *         current level, so logging in never announces old levels, and a player who first appears on the
	 *         board later is recorded without an announcement.
	 */
	List<LevelUp> update(LeaderboardSync.Board board, String self)
	{
		List<LevelUp> found = new ArrayList<>();
		String ownKey = self == null ? null : LeaderboardSync.normalizeName(self);
		for (LeaderboardSync.BoardRow row : board.rows())
		{
			String key = LeaderboardSync.normalizeName(row.name);
			Integer previous = highest.get(key);
			if (previous == null || row.level > previous)
			{
				highest.put(key, row.level);
				if (seeded && previous != null && !key.equals(ownKey))
				{
					found.add(new LevelUp(row.name, row.level));
				}
			}
		}
		seeded = true;
		found.sort(Comparator.comparing((LevelUp l) -> l.name.toLowerCase()));
		return found;
	}

	/** Forget everything; the next board only re-records current levels. */
	void reset()
	{
		highest.clear();
		seeded = false;
	}
}
