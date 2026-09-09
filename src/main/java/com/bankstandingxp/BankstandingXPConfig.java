package com.bankstandingxp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
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
}
