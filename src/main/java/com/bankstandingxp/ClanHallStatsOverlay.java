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

	@Inject
	private ClanHallStatsOverlay(BankstandingXPPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setPreferredSize(new Dimension(200, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInClanHall())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Clan Hall")
			.color(Color.CYAN)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Balloons burst:")
			.right(String.format("%,d", plugin.getBalloonsPopped()))
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Gilded chains:")
			.right(String.format("%,d", plugin.getGildedChainsPickedUp()))
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Time sitting:")
			.right(formatDuration(plugin.getSecondsSpentSitting()))
			.build());

		return super.render(graphics);
	}

	private static String formatDuration(long totalSeconds)
	{
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format("%dh %02dm %02ds", hours, minutes, seconds);
		}
		if (minutes > 0)
		{
			return String.format("%dm %02ds", minutes, seconds);
		}
		return seconds + "s";
	}
}
