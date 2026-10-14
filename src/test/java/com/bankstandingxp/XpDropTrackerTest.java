package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class XpDropTrackerTest
{
	private static XpDropTracker readyTracker(Map<Skill, Integer> xp)
	{
		XpDropTracker t = new XpDropTracker();
		t.baseline(skill -> xp.getOrDefault(skill, 0));
		return t;
	}

	@Test
	public void nothingCountsBeforeTheBaselineExists()
	{
		XpDropTracker t = new XpDropTracker();
		assertFalse(t.isReady());
		assertFalse("login floods StatChanged for every skill", t.isDrop(Skill.ATTACK, 5_000_000));
		assertFalse(t.isReady());
	}

	@Test
	public void aGainAfterTheBaselineIsADrop()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.WOODCUTTING, 1000);
		XpDropTracker t = readyTracker(xp);
		assertTrue(t.isReady());
		assertTrue(t.isDrop(Skill.WOODCUTTING, 1025));
		assertTrue("each further gain is its own drop", t.isDrop(Skill.WOODCUTTING, 1050));
	}

	@Test
	public void sameXpIsNotADrop()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.PRAYER, 400);
		XpDropTracker t = readyTracker(xp);
		assertFalse("a boost wearing off re-fires StatChanged with the same xp", t.isDrop(Skill.PRAYER, 400));
		assertFalse(t.isDrop(Skill.PRAYER, 400));
	}

	@Test
	public void eachSkillIsTrackedSeparately()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.ATTACK, 100);
		xp.put(Skill.HITPOINTS, 1154);
		XpDropTracker t = readyTracker(xp);
		assertTrue(t.isDrop(Skill.ATTACK, 104));
		assertTrue("one hit gives attack and hitpoints drops", t.isDrop(Skill.HITPOINTS, 1156));
		assertFalse(t.isDrop(Skill.STRENGTH, 0));
		assertTrue(t.isDrop(Skill.STRENGTH, 4));
	}

	@Test
	public void combatIsFreeAgilityCostsFiveAndEverythingElseCostsOne()
	{
		assertEquals(5, XpDropTracker.penaltyFor(Skill.AGILITY));
		assertEquals("slayer is not free", 1, XpDropTracker.penaltyFor(Skill.SLAYER));
		Skill[] combat = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS, Skill.PRAYER};
		for (Skill skill : combat)
		{
			assertEquals(skill + " is combat and should be free", 0, XpDropTracker.penaltyFor(skill));
		}
		for (Skill skill : Skill.values())
		{
			if (skill != Skill.AGILITY && !java.util.Arrays.asList(combat).contains(skill))
			{
				assertEquals(skill + " should cost 1", 1, XpDropTracker.penaltyFor(skill));
			}
		}
	}

	@Test
	public void nothingIsLostInTheGrandExchange()
	{
		for (Skill skill : Skill.values())
		{
			assertEquals(skill + " is free in the GE", 0, XpDropTracker.penaltyFor(skill, true));
			assertEquals(skill + " is unchanged elsewhere", XpDropTracker.penaltyFor(skill), XpDropTracker.penaltyFor(skill, false));
		}
		assertEquals(5, XpDropTracker.penaltyFor(Skill.AGILITY, false));
		assertEquals(1, XpDropTracker.penaltyFor(Skill.COOKING, false));
	}

	@Test
	public void aCombatDropStillMovesTheBaseline()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.STRENGTH, 100);
		XpDropTracker t = readyTracker(xp);
		assertTrue("still a real drop, it just costs nothing", t.isDrop(Skill.STRENGTH, 140));
		assertFalse(t.isDrop(Skill.STRENGTH, 140));
	}

	@Test
	public void overallIsNeverADrop()
	{
		XpDropTracker t = readyTracker(new HashMap<>());
		assertFalse(t.isDrop(Skill.OVERALL, 1_000_000));
	}

	@Test
	public void resetRequiresANewBaseline()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.MINING, 50);
		XpDropTracker t = readyTracker(xp);
		t.reset();
		assertFalse(t.isReady());
		assertFalse("logging back in re-fires every skill", t.isDrop(Skill.MINING, 9_999));

		xp.put(Skill.MINING, 9_999);
		t.baseline(skill -> xp.getOrDefault(skill, 0));
		assertFalse("the new baseline absorbs the login values", t.isDrop(Skill.MINING, 9_999));
		assertTrue(t.isDrop(Skill.MINING, 10_020));
	}

	@Test
	public void aDropInXpIsNotAGainAndLowersTheBaseline()
	{
		Map<Skill, Integer> xp = new HashMap<>();
		xp.put(Skill.SLAYER, 500);
		XpDropTracker t = readyTracker(xp);
		assertFalse(t.isDrop(Skill.SLAYER, 400));
		assertTrue("gains are measured from the latest value", t.isDrop(Skill.SLAYER, 401));
	}

	@Test
	public void baselineCoversEveryRealSkillAndNotOverall()
	{
		int[] asked = {0};
		XpDropTracker t = new XpDropTracker();
		t.baseline(skill ->
		{
			asked[0]++;
			assertFalse(skill == Skill.OVERALL);
			return 0;
		});
		// Skill.OVERALL is a separate constant that is not part of Skill.values().
		assertEquals(Skill.values().length, asked[0]);
	}
}
