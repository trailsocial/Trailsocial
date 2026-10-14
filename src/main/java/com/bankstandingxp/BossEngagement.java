package com.bankstandingxp;

/**
 * Remembers whether the player was in combat with a boss a moment ago. Checking only the exact tick of
 * death misses most boss deaths: fights constantly flip the player's target to a minion, or the player
 * steps away or eats, so "interacting with a boss right now" is often false when they actually die.
 */
final class BossEngagement
{
	/** About 7 seconds of game time. */
	static final int WINDOW_TICKS = 12;

	private int ticksSinceEngaged = Integer.MAX_VALUE;

	/** Call once per game tick. */
	void tick(boolean engagedNow)
	{
		if (engagedNow)
		{
			ticksSinceEngaged = 0;
		}
		else if (ticksSinceEngaged < Integer.MAX_VALUE)
		{
			ticksSinceEngaged++;
		}
	}

	boolean recentlyEngaged()
	{
		return ticksSinceEngaged <= WINDOW_TICKS;
	}

	void reset()
	{
		ticksSinceEngaged = Integer.MAX_VALUE;
	}
}
