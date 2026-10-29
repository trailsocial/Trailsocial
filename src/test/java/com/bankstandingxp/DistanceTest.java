package com.bankstandingxp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DistanceTest
{
	@Test
	public void walkingAndRunningCountTilesAndStandingStillCountsNothing()
	{
		assertEquals(0, Distance.tilesBetween(3200, 3200, 0, 3200, 3200, 0));
		assertEquals(1, Distance.tilesBetween(3200, 3200, 0, 3201, 3200, 0));
		assertEquals(2, Distance.tilesBetween(3200, 3200, 0, 3200, 3198, 0));
	}

	@Test
	public void aDiagonalStepIsOneTile()
	{
		assertEquals(1, Distance.tilesBetween(3200, 3200, 0, 3201, 3201, 0));
		assertEquals(2, Distance.tilesBetween(3200, 3200, 0, 3202, 3201, 0));
	}

	@Test
	public void teleportsAndPlaneChangesAreNotWalking()
	{
		assertEquals("teleport", 0, Distance.tilesBetween(3200, 3200, 0, 3222, 3200, 0));
		assertEquals("just over a run", 0, Distance.tilesBetween(3200, 3200, 0, 3203, 3200, 0));
		assertEquals("stairs", 0, Distance.tilesBetween(3200, 3200, 0, 3200, 3201, 1));
	}

	@Test
	public void tilesBecomeMilesAndKilometres()
	{
		// 1 tile = 1 metre
		assertEquals("1.61 km (1,609 tiles)", Distance.format(1609, DistanceUnit.KILOMETRES));
		assertEquals("1.00 mi (1,609 tiles)", Distance.format(1609, DistanceUnit.MILES));
		assertEquals("12.35 km", Distance.formatShort(12_345, DistanceUnit.KILOMETRES));
		assertEquals("7.67 mi", Distance.formatShort(12_345, DistanceUnit.MILES));
		assertEquals("0.00 mi (0 tiles)", Distance.format(0, DistanceUnit.MILES));
		assertEquals("1,242.74 mi", Distance.formatShort(2_000_000, DistanceUnit.MILES));
	}

	@Test
	public void theUnitsHaveFriendlyNames()
	{
		assertEquals("Miles", DistanceUnit.MILES.toString());
		assertEquals("Kilometres", DistanceUnit.KILOMETRES.toString());
	}
}
