package com.bankstandingxp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

public class BankstandingXPConfigTest
{
	private final BankstandingXPConfig config = new BankstandingXPConfig()
	{
	};

	@Test
	public void theOriginalBoxesLookTheSameUntilSomeoneChangesTheToggles()
	{
		assertTrue(config.showOverlay() && config.showClanHallOverlay() && config.showBossOverlay());
		assertTrue(config.bsLevel() && config.bsNextLevel() && config.bsTotalXp() && config.bsStatus() && config.bsProgressBar());
		assertTrue(config.chBalloons() && config.chChains() && config.chSitting());
		assertTrue(config.showDeathsStat());
	}

	@Test
	public void everyStatHasAnAlwaysShowSwitchThatStartsOffInTheInterfaceSection() throws Exception
	{
		int stats = 0;
		for (Method method : BankstandingXPConfig.class.getDeclaredMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item == null || !item.keyName().endsWith("Always"))
			{
				continue;
			}
			stats++;
			assertEquals("interface", item.section());
			assertFalse(item.keyName() + " must start off so boxes keep their usual situation", (Boolean) method.invoke(config));
			String statKey = item.keyName().substring(0, item.keyName().length() - "Always".length());
			assertTrue(statKey + " should be a stat switch", BankstandingXPConfig.class.getMethod(statKey).getAnnotation(ConfigItem.class) != null);
		}
		assertEquals("one for each of the 13 stat lines", 13, stats);
	}

	@Test
	public void everyInterfaceOptionLivesInTheInterfaceSection()
	{
		for (String key : new String[] {"showOverlay", "showClanHallOverlay", "showBossOverlay", "notifyLevelUp", "showXpDrops", "dropSize",
			"bsLevel", "chBalloons", "showDeathsStat"})
		{
			boolean found = false;
			for (Method method : BankstandingXPConfig.class.getDeclaredMethods())
			{
				ConfigItem item = method.getAnnotation(ConfigItem.class);
				if (item != null && item.keyName().equals(key))
				{
					found = true;
					assertEquals(key, "interface", item.section());
				}
			}
			assertTrue(key + " exists", found);
		}
	}

	@Test
	public void theNewOptionalLinesStartHidden()
	{
		assertFalse(config.bsHours());
		assertFalse(config.bsGeIn());
		assertFalse(config.bsGeOut());
		assertFalse(config.chWealth());
	}

	@Test
	public void everyConfigKeyIsUniqueAndNoneClashWithTheStoredCounters()
	{
		Set<String> keys = new HashSet<>();
		int items = 0;
		for (Method method : BankstandingXPConfig.class.getDeclaredMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				items++;
				assertTrue("duplicate key " + item.keyName(), keys.add(item.keyName()));
				// the plugin stores its per-account counters in the same config group as <name>_<account hash>
				assertFalse(item.keyName() + " looks like a stored counter", item.keyName().matches("[A-Za-z]+_-?\\d+"));
			}
		}
		assertEquals("every option has a key", items, keys.size());
		assertTrue(items >= 20);
	}
}
