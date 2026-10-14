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
		LEVEL("!lvl bankstanding", null),
		/** !hours */
		HOURS("!hours", "Hours bankstanding"),
		/** !gnomes */
		GNOMES("!gnomes", "Gnomes killed"),
		/** !deaths: boss and raid deaths */
		DEATHS("!deaths", "Boss deaths"),
		/** !wealth: wealth picked up in the Clan Hall */
		WEALTH("!wealth", "Clan Hall wealth"),
		/** !balloons */
		BALLOONS("!balloons", "Balloons burst"),
		/** !chains: gilded chains picked up */
		CHAINS("!chains", "Gilded chains"),
		/** !sitting: hours spent sitting */
		SITTING("!sitting", "Hours sitting"),
		/** !cane: hours the Trailblazer cane has been held */
		CANE("!cane", "Hours holding the cane"),
		/** !ge: hours in the Grand Exchange */
		GE("!ge", "Hours in the GE"),
		/** !geout: hours outside the Grand Exchange */
		GE_OUT("!geout", "Hours outside the GE");

		final String text;
		/** What is said before the number, or null for the level command, which has its own layout. */
		final String label;

		Command(String text, String label)
		{
			this.text = text;
			this.label = label;
		}

		/** The number this command reports for a player, already formatted. Not used by {@link #LEVEL}. */
		String value(LeaderboardSync.Snapshot stats)
		{
			switch (this)
			{
				case HOURS:
					return formatHours(stats.bankstandingSeconds);
				case GNOMES:
					return formatNumber(stats.gnomesKilled);
				case DEATHS:
					return formatNumber(stats.bossDeaths);
				case WEALTH:
					return formatNumber(stats.clanHallWealth) + " gp";
				case BALLOONS:
					return formatNumber(stats.balloons);
				case CHAINS:
					return formatNumber(stats.gildedChains);
				case SITTING:
					return formatHours(stats.sittingSeconds);
				case CANE:
					return formatHours(stats.caneHeldSeconds);
				case GE:
					return formatHours(stats.geSecondsIn);
				case GE_OUT:
					return formatHours(stats.geSecondsOut);
				default:
					throw new IllegalStateException(this + " has no single value");
			}
		}
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
		String normalized = message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
		for (Command command : Command.values())
		{
			if (command.text.equals(normalized))
			{
				return command;
			}
		}
		return null;
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
		final LeaderboardSync.Snapshot stats;

		Stats(String name, int level, long xp, int rank, int total, LeaderboardSync.Snapshot stats)
		{
			this.name = name;
			this.level = level;
			this.xp = xp;
			this.rank = rank;
			this.total = total;
			this.stats = stats;
		}
	}
}
