package com.bankstandingxp;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.runelite.api.Skill;

/**
 * Recognises real in-game xp drops from StatChanged events. The client also fires StatChanged for every
 * skill while logging in and whenever a boost wears off, so a stat change only counts as a drop when the
 * skill's xp went up from a baseline taken after login.
 */
final class XpDropTracker
{
	static final int PENALTY_PER_DROP = 1;
	static final int AGILITY_PENALTY_PER_DROP = 5;

	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private boolean ready;

	/** Bankstanding xp lost for one xp drop in the given skill: agility hurts the most. */
	static int penaltyFor(Skill skill)
	{
		return skill == Skill.AGILITY ? AGILITY_PENALTY_PER_DROP : PENALTY_PER_DROP;
	}

	boolean isReady()
	{
		return ready;
	}

	/** Forget everything; call on logout and world hops, then {@link #baseline} again once logged in. */
	void reset()
	{
		lastXp.clear();
		ready = false;
	}

	void baseline(ToIntFunction<Skill> currentXp)
	{
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.OVERALL)
			{
				lastXp.put(skill, currentXp.applyAsInt(skill));
			}
		}
		ready = true;
	}

	/** @return true if this change is a genuine xp gain in a real skill. */
	boolean isDrop(Skill skill, int xp)
	{
		if (!ready || skill == Skill.OVERALL)
		{
			return false;
		}
		Integer previous = lastXp.put(skill, xp);
		return previous != null && xp > previous;
	}
}
