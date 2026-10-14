package com.bankstandingxp;

import java.util.Locale;

/**
 * The chat commands the plugin understands. Any player running the plugin who sees one of these in chat
 * has it replaced on their own screen with that player's stats, the same way RuneLite's built-in
 * "!lvl attack" works. These are matched by the plugin itself rather than registered with RuneLite's
 * command manager, because "!lvl" is already owned by the built-in Chat Commands plugin.
 */
final class BankstandingChatCommands
{
	enum Command
	{
		/** !lvl bankstanding */
		LEVEL,
		/** !hours */
		HOURS,
		/** !gnomes */
		GNOMES
	}

	private BankstandingChatCommands()
	{
	}

	/** @return the command a chat message is, or null if it isn't one of ours (matching ignores case and extra spaces) */
	static Command parse(String message)
	{
		if (message == null)
		{
			return null;
		}
		switch (message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))
		{
			case "!lvl bankstanding":
				return Command.LEVEL;
			case "!hours":
				return Command.HOURS;
			case "!gnomes":
				return Command.GNOMES;
			default:
				return null;
		}
	}

	/** Seconds as hours with one decimal, e.g. 148500 -> "41.3". */
	static String formatHours(long seconds)
	{
		return String.format(Locale.US, "%.1f", seconds / 3600.0);
	}

	/** 62303 -> "62,303". */
	static String formatNumber(long value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	/** Everything a command can show about one player. {@code rank} is 0 when unknown. */
	static final class Stats
	{
		final String name;
		final int level;
		final long xp;
		final int rank;
		final int total;
		final long bankstandingSeconds;
		final long gnomesKilled;

		Stats(String name, int level, long xp, int rank, int total, long bankstandingSeconds, long gnomesKilled)
		{
			this.name = name;
			this.level = level;
			this.xp = xp;
			this.rank = rank;
			this.total = total;
			this.bankstandingSeconds = bankstandingSeconds;
			this.gnomesKilled = gnomesKilled;
		}
	}
}
