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
 * Shows lifetime "deaths while bossing" whenever the player is fighting a
 * recognised boss NPC or is inside any raid instance (CoX, ToB, or ToA).
 */
public class BossRaidStatsOverlay extends OverlayPanel
{
	private final BankstandingXPPlugin plugin;

	@Inject
	private BossRaidStatsOverlay(BankstandingXPPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
		setPreferredSize(new Dimension(200, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.isInRaid() && !plugin.isFightingBoss())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Bossing")
			.color(Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Deaths while bossing:")
			.right(String.format("%,d", plugin.getBossDeaths()))
			.build());

		return super.render(graphics);
	}
}
