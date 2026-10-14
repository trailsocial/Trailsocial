package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class TrailSocialOutfitTest
{
	private static Set<Integer> wearing(int... ids)
	{
		Set<Integer> set = new HashSet<>();
		for (int id : ids)
		{
			set.add(id);
		}
		return set;
	}

	@Test
	public void theFullOutfitCounts()
	{
		assertTrue(TrailSocialOutfit.isComplete(wearing(TrailSocialOutfit.PIECES)::contains));
	}

	@Test
	public void sevenPiecesAndNoDuplicates()
	{
		assertEquals(7, TrailSocialOutfit.PIECES.length);
		assertEquals(7, Arrays.stream(TrailSocialOutfit.PIECES).distinct().count());
	}

	@Test
	public void missingAnyOnePieceMeansNoBonus()
	{
		for (int missing : TrailSocialOutfit.PIECES)
		{
			Set<Integer> worn = wearing(TrailSocialOutfit.PIECES);
			worn.remove(missing);
			assertFalse("item " + missing + " is required", TrailSocialOutfit.isComplete(worn::contains));
		}
	}

	@Test
	public void otherSlotsDoNotMatter()
	{
		Set<Integer> worn = wearing(TrailSocialOutfit.PIECES);
		worn.add(24411);  // Twisted boots (t1)
		worn.add(4151);   // abyssal whip, a random extra
		worn.add(1);
		assertTrue(TrailSocialOutfit.isComplete(worn::contains));
	}

	@Test
	public void onlyTheNamedVersionsCount()
	{
		// the t2 coat and the plain top hat are different items from the ones the outfit asks for
		Set<Integer> worn = wearing(TrailSocialOutfit.PIECES);
		worn.remove(TrailSocialOutfit.TWISTED_COAT_T1);
		worn.add(24399);
		assertFalse(TrailSocialOutfit.isComplete(worn::contains));

		worn = wearing(TrailSocialOutfit.PIECES);
		worn.remove(TrailSocialOutfit.TOP_HAT_AND_MONOCLE);
		worn.add(12432);
		assertFalse(TrailSocialOutfit.isComplete(worn::contains));
	}

	@Test
	public void wearingNothingIsNotComplete()
	{
		assertFalse(TrailSocialOutfit.isComplete(id -> false));
	}

	@Test
	public void theCaneMatchesTheOneTheHeldTimeTrackerUses()
	{
		assertEquals(25013, TrailSocialOutfit.TRAILBLAZER_CANE);
	}
}
