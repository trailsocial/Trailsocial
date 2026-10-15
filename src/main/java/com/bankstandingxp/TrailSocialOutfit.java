package com.bankstandingxp;

import java.util.function.IntPredicate;

/**
 * The TrailSocial clan outfit. Wearing every piece multiplies Bankstanding XP by {@link #XP_MULTIPLIER};
 * what is in any other slot (boots, gloves, ammo, ring...) does not matter. Ids are the OSRS item ids,
 * checked against the OSRS Wiki, and only the exact items named here count (for example a Twisted
 * coat (t2) does not).
 */
final class TrailSocialOutfit
{
	/**
	 * Bankstanding XP gain while wearing the full outfit. The leaderboard server caps xp per hour
	 * (MAX_XP_PER_HOUR in bankstanding.js) with slack above 300 * this, so raise that too if this goes up.
	 */
	static final int XP_MULTIPLIER = 5;


	static final int TOP_HAT_AND_MONOCLE = 12434;
	static final int TRAILBLAZER_CANE = 25013;
	static final int BRIEFCASE = 12335;
	static final int TWISTED_COAT_T1 = 24407;
	static final int TWISTED_TROUSERS_T1 = 24409;
	static final int CLUE_HUNTER_CLOAK = 19697;
	static final int CAT_TRAINING_MEDAL = 1575;

	/** Ring of visibility. On its own it gives the same boost as the full outfit; the two do not stack. */
	static final int RING_OF_VISIBILITY = 4657;

	static final int[] PIECES = {
		TOP_HAT_AND_MONOCLE,
		TRAILBLAZER_CANE,
		BRIEFCASE,
		TWISTED_COAT_T1,
		TWISTED_TROUSERS_T1,
		CLUE_HUNTER_CLOAK,
		CAT_TRAINING_MEDAL,
	};

	private TrailSocialOutfit()
	{
	}

	/** The Bankstanding XP multiplier for what is worn: 5x for the full outfit or the ring, never more. */
	static int xpMultiplier(IntPredicate wearing)
	{
		return isComplete(wearing) || wearing.test(RING_OF_VISIBILITY) ? XP_MULTIPLIER : 1;
	}

	/** @param wearing whether an item id is currently equipped */
	static boolean isComplete(IntPredicate wearing)
	{
		for (int id : PIECES)
		{
			if (!wearing.test(id))
			{
				return false;
			}
		}
		return true;
	}
}
