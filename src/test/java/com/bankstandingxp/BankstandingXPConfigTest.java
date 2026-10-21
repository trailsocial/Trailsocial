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
