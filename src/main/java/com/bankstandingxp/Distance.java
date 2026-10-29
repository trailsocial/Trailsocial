package com.bankstandingxp;

import java.util.Locale;

/** Counting tiles walked and turning them into miles or kilometres. */
final class Distance
{
	/** A player can run two tiles per game tick; anything further in one tick is a teleport or other jump. */
	static final int MAX_TILES_PER_TICK = 2;

	/** How far one tile is taken to be in the real world. */
	static final double METRES_PER_TILE = 1.0;

	private static final double METRES_PER_MILE = 1609.344;

	private Distance()
	{
	}

	/**
	 * The tiles walked between two game ticks. A diagonal step counts as one tile, like in the game. Moving to
	 * another plane or further than a run can cover in a tick (a teleport, a fairy ring, a loading screen) counts
	 * as nothing, and so does standing still.
	 */
	static int tilesBetween(int x1, int y1, int plane1, int x2, int y2, int plane2)
	{
		if (plane1 != plane2)
		{
			return 0;
		}
		int tiles = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
		return tiles > MAX_TILES_PER_TICK ? 0 : tiles;
	}

	static double metres(long tiles)
	{
		return tiles * METRES_PER_TILE;
	}

	/** 12,345 tiles in miles -> "7.67 mi (12,345 tiles)". */
	static String format(long tiles, DistanceUnit unit)
	{
		double metres = metres(tiles);
		String distance = unit == DistanceUnit.KILOMETRES
			? String.format(Locale.US, "%,.2f km", metres / 1000.0)
			: String.format(Locale.US, "%,.2f mi", metres / METRES_PER_MILE);
		return distance + String.format(Locale.US, " (%,d tiles)", tiles);
	}

	/** Just the distance, for the narrow in-game box: 12,345 tiles in km -> "12.35 km". */
	static String formatShort(long tiles, DistanceUnit unit)
	{
		double metres = metres(tiles);
		return unit == DistanceUnit.KILOMETRES
			? String.format(Locale.US, "%,.2f km", metres / 1000.0)
			: String.format(Locale.US, "%,.2f mi", metres / METRES_PER_MILE);
	}
}
