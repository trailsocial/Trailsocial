package com.bankstandingxp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BossAndRaidDataTest
{
	@Test
	public void sarachnisIsABossWhateverTheCase()
	{
		assertTrue(BossAndRaidData.isBossName("Sarachnis"));
		assertTrue(BossAndRaidData.isBossName("SARACHNIS"));
		assertTrue(BossAndRaidData.isBossName("  sarachnis "));
	}

	@Test
	public void commonBossesAreRecognised()
	{
		for (String name : new String[] {"Vorkath", "Zulrah", "General Graardor", "Kalphite Queen", "Alchemical Hydra",
			"The Nightmare", "Phosani's Nightmare", "Duke Sucellus", "Tztok-Jad", "TzKal-Zuk", "Cerberus", "Dusk", "Dawn"})
		{
			assertTrue(name, BossAndRaidData.isBossName(name));
		}
	}

	@Test
	public void theFracturedArchiveBossesAreRecognised()
	{
		for (String name : new String[] {"Archive Guardian", "Ezekiel", "Moia", "Dreadnoth the Jellied", "Xarth", "Dagon'hai Elder", "Ocellus"})
		{
			assertTrue(name, BossAndRaidData.isBossName(name));
		}
	}

	@Test
	public void ordinaryMonstersAndNothingAreNotBosses()
	{
		assertFalse(BossAndRaidData.isBossName("Gnome"));
		assertFalse(BossAndRaidData.isBossName("Spawn of Sarachnis"));
		assertFalse(BossAndRaidData.isBossName(""));
		assertFalse(BossAndRaidData.isBossName(null));
	}
}
