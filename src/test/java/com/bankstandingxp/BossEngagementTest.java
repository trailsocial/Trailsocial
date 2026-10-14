package com.bankstandingxp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BossEngagementTest
{
	@Test
	public void neverEngagedMeansNoBossDeath()
	{
		BossEngagement e = new BossEngagement();
		assertFalse(e.recentlyEngaged());
		for (int i = 0; i < 100_000; i++)
		{
			e.tick(false);
		}
		assertFalse("idle ticks must never wrap around into 'engaged'", e.recentlyEngaged());
	}

	@Test
	public void engagedThisTickCounts()
	{
		BossEngagement e = new BossEngagement();
		e.tick(true);
		assertTrue(e.recentlyEngaged());
	}

	@Test
	public void staysEngagedForTheWholeWindowThenExpires()
	{
		BossEngagement e = new BossEngagement();
		e.tick(true);
		for (int i = 1; i <= BossEngagement.WINDOW_TICKS; i++)
		{
			e.tick(false);
			assertTrue("tick " + i + " after the fight is still inside the window", e.recentlyEngaged());
		}
		e.tick(false);
		assertFalse("one tick past the window is not a boss death", e.recentlyEngaged());
	}

	@Test
	public void aTargetSwitchToAMinionMidFightDoesNotLoseTheFight()
	{
		// boss, boss, then the player targets a spawn (not engaged) for a few ticks, then dies
		BossEngagement e = new BossEngagement();
		e.tick(true);
		e.tick(true);
		e.tick(false);
		e.tick(false);
		e.tick(false);
		assertTrue(e.recentlyEngaged());
	}

	@Test
	public void engagingAgainRestartsTheWindow()
	{
		BossEngagement e = new BossEngagement();
		e.tick(true);
		for (int i = 0; i < BossEngagement.WINDOW_TICKS; i++)
		{
			e.tick(false);
		}
		e.tick(true);
		for (int i = 0; i < BossEngagement.WINDOW_TICKS; i++)
		{
			e.tick(false);
		}
		assertTrue(e.recentlyEngaged());
	}

	@Test
	public void resetForgetsTheFight()
	{
		BossEngagement e = new BossEngagement();
		e.tick(true);
		e.reset();
		assertFalse(e.recentlyEngaged());
	}
}
