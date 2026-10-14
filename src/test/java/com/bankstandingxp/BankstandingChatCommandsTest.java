package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
	public void hoursAndGnomesCommands()
	{
		assertEquals(BankstandingChatCommands.Command.HOURS, BankstandingChatCommands.parse("!hours"));
		assertEquals(BankstandingChatCommands.Command.HOURS, BankstandingChatCommands.parse("!HOURS "));
		assertEquals(BankstandingChatCommands.Command.GNOMES, BankstandingChatCommands.parse("!gnomes"));
		assertEquals(BankstandingChatCommands.Command.GNOMES, BankstandingChatCommands.parse(" !Gnomes"));
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
