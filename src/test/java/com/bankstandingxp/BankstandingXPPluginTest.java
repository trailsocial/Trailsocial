package com.bankstandingxp;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Run this class's main method (module classpath: test) to launch a full
 * RuneLite client with the Bankstanding XP plugin sideloaded, for local testing.
 */
public class BankstandingXPPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BankstandingXPPlugin.class);
		RuneLite.main(args);
	}
}
