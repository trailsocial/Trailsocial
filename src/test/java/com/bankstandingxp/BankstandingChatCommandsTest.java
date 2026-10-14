package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BankstandingChatCommandsTest
{
	@Test
	public void theLevelCommandMatchesWhateverTheCaseAndSpacing()
	{
		assertEquals(BankstandingChatCommands.Command.LEVEL, BankstandingChatCommands.parse("!lvl bankstanding"));
		assertEquals(BankstandingChatCommands.Command.LEVEL, BankstandingChatCommands.parse("!Lvl bankstanding"));
		assertEquals(BankstandingChatCommands.Command.LEVEL, BankstandingChatCommands.parse("  !LVL    BANKSTANDING  "));
	}

	@Test
	public void everyStatHasItsOwnCommand()
	{
		String[] texts = {"!hours", "!gnomes", "!deaths", "!wealth", "!balloons", "!chains", "!sitting", "!cane", "!ge", "!geout"};
		BankstandingChatCommands.Command[] expected = {
			BankstandingChatCommands.Command.HOURS, BankstandingChatCommands.Command.GNOMES,
			BankstandingChatCommands.Command.DEATHS, BankstandingChatCommands.Command.WEALTH,
			BankstandingChatCommands.Command.BALLOONS, BankstandingChatCommands.Command.CHAINS,
			BankstandingChatCommands.Command.SITTING, BankstandingChatCommands.Command.CANE,
			BankstandingChatCommands.Command.GE, BankstandingChatCommands.Command.GE_OUT,
		};
		for (int i = 0; i < texts.length; i++)
		{
			assertEquals(expected[i], BankstandingChatCommands.parse(texts[i]));
			assertEquals(expected[i], BankstandingChatCommands.parse("  " + texts[i].toUpperCase() + " "));
		}
		assertEquals("every command is covered above", texts.length + 1, BankstandingChatCommands.Command.values().length);
	}

	@Test
	public void noTwoCommandsShareText()
	{
		java.util.Set<String> seen = new java.util.HashSet<>();
		for (BankstandingChatCommands.Command command : BankstandingChatCommands.Command.values())
		{
			assertTrue(command + " reuses text", seen.add(command.text));
		}
	}

	@Test
	public void eachCommandReportsItsOwnStat()
	{
		// distinct value in every field so a command reading the wrong one is caught
		LeaderboardSync.Snapshot s = new LeaderboardSync.Snapshot("A", 100,
			3600 * 2, 21_067_424_759L, 11, 491, 1800, 7, 50, 3600 * 3, 3600 * 4, 3600 * 5);
		assertEquals("2.0", BankstandingChatCommands.Command.HOURS.value(s));
		assertEquals("50", BankstandingChatCommands.Command.GNOMES.value(s));
		assertEquals("7", BankstandingChatCommands.Command.DEATHS.value(s));
		assertEquals("21,067,424,759 gp", BankstandingChatCommands.Command.WEALTH.value(s));
		assertEquals("11", BankstandingChatCommands.Command.BALLOONS.value(s));
		assertEquals("491", BankstandingChatCommands.Command.CHAINS.value(s));
		assertEquals("0.5", BankstandingChatCommands.Command.SITTING.value(s));
		assertEquals("3.0", BankstandingChatCommands.Command.CANE.value(s));
		assertEquals("4.0", BankstandingChatCommands.Command.GE.value(s));
		assertEquals("5.0", BankstandingChatCommands.Command.GE_OUT.value(s));
		for (BankstandingChatCommands.Command command : BankstandingChatCommands.Command.values())
		{
			if (command != BankstandingChatCommands.Command.LEVEL)
			{
				assertTrue(command + " needs a label", command.label != null && !command.label.isEmpty());
			}
		}
	}

	@Test
	public void otherMessagesAreNeverTouched()
	{
		// real skills belong to RuneLite's own !lvl command
		assertNull(BankstandingChatCommands.parse("!lvl attack"));
		assertNull(BankstandingChatCommands.parse("!lvl"));
		assertNull(BankstandingChatCommands.parse("!level bankstanding"));
		assertNull(BankstandingChatCommands.parse("!bankstanding"));
		assertNull(BankstandingChatCommands.parse("!hours please"));
		assertNull(BankstandingChatCommands.parse("check !hours"));
		assertNull(BankstandingChatCommands.parse("!gnome"));
		assertNull(BankstandingChatCommands.parse("!death"));
		assertNull(BankstandingChatCommands.parse("!ge now"));
		assertNull(BankstandingChatCommands.parse("!lvl ge"));
		assertNull(BankstandingChatCommands.parse("hello"));
		assertNull(BankstandingChatCommands.parse(""));
		assertNull(BankstandingChatCommands.parse(null));
	}

	@Test
	public void hoursAreShownWithOneDecimal()
	{
		assertEquals("41.3", BankstandingChatCommands.formatHours(148_500));
		assertEquals("0.0", BankstandingChatCommands.formatHours(0));
		assertEquals("1.0", BankstandingChatCommands.formatHours(3600));
		assertEquals("0.5", BankstandingChatCommands.formatHours(1800));
	}

	@Test
	public void numbersGetThousandsSeparators()
	{
		assertEquals("62,303", BankstandingChatCommands.formatNumber(62_303));
		assertEquals("0", BankstandingChatCommands.formatNumber(0));
		assertEquals("1,320", BankstandingChatCommands.formatNumber(1320));
		assertEquals("200,000,000", BankstandingChatCommands.formatNumber(200_000_000));
	}
}
