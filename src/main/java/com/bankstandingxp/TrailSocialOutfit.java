package com.bankstandingxp;

import java.util.function.IntPredicate;

/**
 * The TrailSocial clan outfit. Wearing every piece doubles Bankstanding XP; what is in any other
 * slot (boots, gloves, ammo, ring...) does not matter. Ids are the OSRS item ids, checked against
 * the OSRS Wiki, and only the exact items named here count (for example a Twisted coat (t2) does not).
 */
final class TrailSocialOutfit
{
	static final int TOP_HAT_AND_MONOCLE = 12434;
	static final int TRAILBLAZER_CANE = 25013;
	static final int BRIEFCASE = 12335;
	static final int TWISTED_COAT_T1 = 24407;
	static final int TWISTED_TROUSERS_T1 = 24409;
	static final int CLUE_HUNTER_CLOAK = 19697;
	static final int CAT_TRAINING_MEDAL = 1575;

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
