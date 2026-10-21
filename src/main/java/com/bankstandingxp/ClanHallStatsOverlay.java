package com.bankstandingxp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Shows a handful of joke stats (balloons burst, gilded chains picked up,
 * time spent sitting) but only while the player is physically in the Clan Hall.
 */
public class ClanHallStatsOverlay extends OverlayPanel
{
	private final BankstandingXPPlugin plugin;
	private final BankstandingXPConfig config;

	@Inject
	private ClanHallStatsOverlay(BankstandingXPPlugin plugin, BankstandingXPConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPreferredSize(new Dimension(200, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showClanHallOverlay() || !plugin.isInClanHall())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Clan Hall")
			.color(Color.CYAN)
			.build());

		int lines = 0;
		if (config.chWealth())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Wealth:").right(String.format("%,d gp", plugin.getClanHallWealthPickedUp())).build());
			lines++;
		}
		if (config.chBalloons())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Balloons burst:").right(String.format("%,d", plugin.getBalloonsPopped())).build());
			lines++;
		}
		if (config.chChains())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Gilded chains:").right(String.format("%,d", plugin.getGildedChainsPickedUp())).build());
			lines++;
		}
		if (config.chSitting())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Time sitting:").right(StatFormat.duration(plugin.getSecondsSpentSitting())).build());
			lines++;
		}
		if (lines == 0)
		{
			return null;
		}

		return super.render(graphics);
	}
}
