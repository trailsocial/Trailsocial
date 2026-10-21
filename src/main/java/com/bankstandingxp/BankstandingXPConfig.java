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

	@ConfigSection(
		name = "Interface",
		description = "What shows on your screen. The Bankstanding box normally appears near a bank, the Clan Hall box in "
			+ "the Clan Hall and the deaths box while bossing; turn on \"always show\" under a stat to keep it up everywhere.",
		position = 1
	)
	String interfaceSection = "interface";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show the Bankstanding XP box overlay in-game (near a bank, unless a stat below is set to always show)",
		section = interfaceSection,
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClanHallOverlay",
		name = "Clan Hall overlay",
		description = "Show the Clan Hall stats box (inside the Clan Hall, unless a stat below is set to always show)",
		section = interfaceSection,
		position = 2
	)
	default boolean showClanHallOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBossOverlay",
		name = "Deaths overlay",
		description = "Show the deaths box (while fighting a boss or in a raid, unless the stat below is set to always show)",
		section = interfaceSection,
		position = 3
	)
	default boolean showBossOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyLevelUp",
		name = "Notify on level up",
		description = "Show a game message and tray notification when your Bankstanding level increases",
		section = interfaceSection,
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
		section = interfaceSection,
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
		section = interfaceSection,
		position = 7
	)
	default int dropSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "bsLevel",
		name = "Bankstanding: level",
		description = "Show your Bankstanding level",
		section = interfaceSection,
		position = 20
	)
	default boolean bsLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsLevelAlways",
		name = "Bankstanding: level: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 21
	)
	default boolean bsLevelAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsNextLevel",
		name = "Bankstanding: next level",
		description = "Show the XP to your next Bankstanding level",
		section = interfaceSection,
		position = 22
	)
	default boolean bsNextLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsNextLevelAlways",
		name = "Bankstanding: next level: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 23
	)
	default boolean bsNextLevelAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsTotalXp",
		name = "Bankstanding: total XP",
		description = "Show your total Bankstanding XP",
		section = interfaceSection,
		position = 24
	)
	default boolean bsTotalXp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsTotalXpAlways",
		name = "Bankstanding: total XP: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 25
	)
	default boolean bsTotalXpAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsStatus",
		name = "Bankstanding: status",
		description = "Show whether you are currently bankstanding",
		section = interfaceSection,
		position = 26
	)
	default boolean bsStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsStatusAlways",
		name = "Bankstanding: status: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 27
	)
	default boolean bsStatusAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsProgressBar",
		name = "Bankstanding: progress bar",
		description = "Show the progress bar to the next level",
		section = interfaceSection,
		position = 28
	)
	default boolean bsProgressBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bsProgressBarAlways",
		name = "Bankstanding: progress bar: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 29
	)
	default boolean bsProgressBarAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsHours",
		name = "Bankstanding: time spent",
		description = "Show your total time spent bankstanding",
		section = interfaceSection,
		position = 30
	)
	default boolean bsHours()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsHoursAlways",
		name = "Bankstanding: time spent: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 31
	)
	default boolean bsHoursAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeIn",
		name = "Bankstanding: time in the GE",
		description = "Show your total time spent in the Grand Exchange",
		section = interfaceSection,
		position = 32
	)
	default boolean bsGeIn()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeInAlways",
		name = "Bankstanding: time in the GE: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 33
	)
	default boolean bsGeInAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeOut",
		name = "Bankstanding: time outside the GE",
		description = "Show your total time spent outside the Grand Exchange",
		section = interfaceSection,
		position = 34
	)
	default boolean bsGeOut()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bsGeOutAlways",
		name = "Bankstanding: time outside the GE: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 35
	)
	default boolean bsGeOutAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chWealth",
		name = "Clan Hall: wealth",
		description = "Show Clan Hall wealth picked up",
		section = interfaceSection,
		position = 36
	)
	default boolean chWealth()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chWealthAlways",
		name = "Clan Hall: wealth: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 37
	)
	default boolean chWealthAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chBalloons",
		name = "Clan Hall: balloons",
		description = "Show balloons burst",
		section = interfaceSection,
		position = 38
	)
	default boolean chBalloons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chBalloonsAlways",
		name = "Clan Hall: balloons: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 39
	)
	default boolean chBalloonsAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chChains",
		name = "Clan Hall: gilded chains",
		description = "Show gilded chains picked up",
		section = interfaceSection,
		position = 40
	)
	default boolean chChains()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chChainsAlways",
		name = "Clan Hall: gilded chains: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 41
	)
	default boolean chChainsAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chSitting",
		name = "Clan Hall: time sitting",
		description = "Show time spent sitting",
		section = interfaceSection,
		position = 42
	)
	default boolean chSitting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chSittingAlways",
		name = "Clan Hall: time sitting: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 43
	)
	default boolean chSittingAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDeathsStat",
		name = "Deaths",
		description = "Show your death count",
		section = interfaceSection,
		position = 44
	)
	default boolean showDeathsStat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDeathsStatAlways",
		name = "Deaths: always show",
		description = "Keep this stat on screen all the time, not just in its usual situation",
		section = interfaceSection,
		position = 45
	)
	default boolean showDeathsStatAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatCommands",
		name = "Chat commands",
		description = "Replace commands like !lvl bankstanding, !hours, !deaths and !events in chat with that player's Bankstanding "
			+ "stats. Other players' stats are looked up on the public TrailSocial leaderboard.",
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
}
