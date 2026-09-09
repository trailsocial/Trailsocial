package com.bankstandingxp;

/**
 * XP-to-level table for the Bankstanding "skill", using the exact same
 * formula as real OSRS skills - level 99 lands at 13,034,431 xp. Unlike a
 * real skill, xp keeps accumulating past that (level display stays frozen
 * at 99) up to the game's real absolute per-skill xp ceiling of 200M.
 */
public final class BankstandingXP
{
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 99;
	public static final long MAX_XP = 200_000_000L;

	private static final long[] XP_FOR_LEVEL = new long[MAX_LEVEL + 1];

	static
	{
		double points = 0;
		for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++)
		{
			XP_FOR_LEVEL[level] = (long) Math.floor(points / 4);
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
		}
	}

	private BankstandingXP()
	{
	}

	public static int getLevelForXp(long xp)
	{
		xp = clampXp(xp);
		for (int level = MAX_LEVEL; level >= MIN_LEVEL; level--)
		{
			if (xp >= XP_FOR_LEVEL[level])
			{
				return level;
			}
		}
		return MIN_LEVEL;
	}

	public static long getXpForLevel(int level)
	{
		level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
		return XP_FOR_LEVEL[level];
	}

	public static long clampXp(long xp)
	{
		if (xp < 0)
		{
			return 0;
		}
		return Math.min(xp, MAX_XP);
	}

	/**
	 * @return xp still needed to reach the next level, or 0 once level 99 is reached.
	 */
	public static long getXpToNextLevel(long xp)
	{
		int level = getLevelForXp(xp);
		if (level >= MAX_LEVEL)
		{
			return 0;
		}
		return getXpForLevel(level + 1) - clampXp(xp);
	}

	/**
	 * @return progress toward the next level as a value from 0.0 to 1.0. Once
	 * level 99 is reached this tracks progress toward the absolute xp cap instead.
	 */
	public static double getProgressToNextLevel(long xp)
	{
		long clamped = clampXp(xp);
		int level = getLevelForXp(clamped);
		if (level >= MAX_LEVEL)
		{
			long floor = getXpForLevel(MAX_LEVEL);
			long ceil = MAX_XP;
			if (ceil <= floor)
			{
				return 1.0;
			}
			return (clamped - floor) / (double) (ceil - floor);
		}
		long floor = getXpForLevel(level);
		long ceil = getXpForLevel(level + 1);
		if (ceil <= floor)
		{
			return 1.0;
		}
		return (clamped - floor) / (double) (ceil - floor);
	}
}
