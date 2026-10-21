package com.bankstandingxp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BankstandingXPConfig.GROUP)
public interface BankstandingXPConfig extends Config
{
	String GROUP = "bankstandingxp";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show the Bankstanding XP box overlay in-game",
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyLevelUp",
		name = "Notify on level up",
		description = "Show a game message and tray notification when your Bankstanding level increases",
		position = 5
	)
	default boolean notifyLevelUp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showXpDrops",
		name = "Show XP drops",
		description = "Show a fake XP drop popup (like a real skill) whenever you earn Bankstanding XP",
		position = 6
	)
	default boolean showXpDrops()
	{
		return true;
	}

	@Range(min = 25, max = 400)
	@ConfigItem(
		keyName = "dropSize",
		name = "XP drop size",
		description = "Size of the XP drop popup, as a percentage of normal (100 = normal size)",
		position = 7
	)
	default int dropSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "chatCommands",
		name = "Chat commands",
		description = "Replace !lvl bankstanding, !hours and !gnomes in chat with that player's Bankstanding stats. "
			+ "Other players' stats are looked up on the public TrailSocial leaderboard.",
		position = 8
	)
	default boolean chatCommands()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clanLevelUps",
		name = "Clan level-up messages",
		description = "While you are in the TrailSocial clan chat, show a clan message in your chat whenever another "
			+ "clan member reaches a new Bankstanding level. Uses the same public leaderboard download as the chat commands.",
		position = 9
	)
	default boolean clanLevelUps()
	{
		return true;
	}

	@ConfigSection(
		name = "Leaderboard",
		description = "Sync your Bankstanding XP and stats to the TrailSocial leaderboard",
		position = 10
	)
	String leaderboardSection = "leaderboard";

	@ConfigItem(
		keyName = "leaderboardCode",
		name = "Leaderboard link code",
		description = "Paste the code from your trailsocial.net profile (Leaderboard Sync). While it is set, your in-game name, "
			+ "Bankstanding XP and the stats shown in the sidebar are sent to trailsocial.net every few minutes. Clear it to stop.",
		section = leaderboardSection,
		secret = true,
		position = 11
	)
	default String leaderboardCode()
	{
		return "";
	}

	@ConfigSection(
		name = "In-game stats",
		description = "Choose which stats the in-game boxes show. Each box still only appears in its own situation: "
			+ "Bankstanding near a bank, the Clan Hall box in the Clan Hall, deaths while bossing.",
		position = 12
	)
	String statsSection = "inGameStats";

	@ConfigItem(
		keyName = "showClanHallOverlay",
		name = "Clan Hall overlay",
		description = "Show the Clan Hall stats box while you are inside the Clan Hall",
		section = statsSection,
		position = 13
	)
	default boolean showClanHallOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBossOverlay",
		name = "Deaths overlay",
		description = "Show the deaths box while you are fighting a boss or inside a raid",
		section = statsSection,
		position = 14
	)
	default boolean showBossOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsLevel",
		name = "Bankstanding: level",
		description = "Show your Bankstanding level in the overlay (the overlay itself only shows near a bank)",
		section = statsSection,
		position = 15
	)
	default boolean bsLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsNextLevel",
		name = "Bankstanding: next level",
		description = "Show the XP to your next Bankstanding level",
		section = statsSection,
		position = 16
	)
	default boolean bsNextLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsTotalXp",
		name = "Bankstanding: total XP",
		description = "Show your total Bankstanding XP",
		section = statsSection,
		position = 17
	)
	default boolean bsTotalXp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsStatus",
		name = "Bankstanding: status",
		description = "Show whether you are currently bankstanding",
		section = statsSection,
		position = 18
	)
	default boolean bsStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsProgressBar",
		name = "Bankstanding: progress bar",
		description = "Show the progress bar to the next level",
		section = statsSection,
		position = 19
	)
	default boolean bsProgressBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsHours",
		name = "Bankstanding: time spent",
		description = "Show your total time spent bankstanding",
		section = statsSection,
		position = 20
	)
	default boolean bsHours()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeIn",
		name = "Bankstanding: time in the GE",
		description = "Show your total time spent in the Grand Exchange",
		section = statsSection,
		position = 21
	)
	default boolean bsGeIn()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeOut",
		name = "Bankstanding: time outside the GE",
		description = "Show your total time spent outside the Grand Exchange",
		section = statsSection,
		position = 22
	)
	default boolean bsGeOut()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chWealth",
		name = "Clan Hall: wealth",
		description = "Show Clan Hall wealth picked up in the Clan Hall box",
		section = statsSection,
		position = 23
	)
	default boolean chWealth()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chBalloons",
		name = "Clan Hall: balloons",
		description = "Show balloons burst in the Clan Hall box",
		section = statsSection,
		position = 24
	)
	default boolean chBalloons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chChains",
		name = "Clan Hall: gilded chains",
		description = "Show gilded chains picked up in the Clan Hall box",
		section = statsSection,
		position = 25
	)
	default boolean chChains()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chSitting",
		name = "Clan Hall: time sitting",
		description = "Show time spent sitting in the Clan Hall box",
		section = statsSection,
		position = 26
	)
	default boolean chSitting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDeathsStat",
		name = "Deaths",
		description = "Show your death count in the deaths box",
		section = statsSection,
		position = 27
	)
	default boolean showDeathsStat()
	{
		return true;
	}
}
