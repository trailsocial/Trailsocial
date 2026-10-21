package com.bankstandingxp;

/** Shared formatting for the in-game stat boxes. */
final class StatFormat
{
	private StatFormat()
	{
	}

	/** 3725 -> "1h 02m 05s"; under an hour "2m 05s"; under a minute "5s". */
	static String duration(long totalSeconds)
	{
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format("%dh %02dm %02ds", hours, minutes, seconds);
		}
		if (minutes > 0)
		{
			return String.format("%dm %02ds", minutes, seconds);
		}
		return seconds + "s";
	}
}
