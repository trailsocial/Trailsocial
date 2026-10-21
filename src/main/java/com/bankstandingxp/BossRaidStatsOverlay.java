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
	private final BankstandingXPConfig config;

	@Inject
	private BossRaidStatsOverlay(BankstandingXPPlugin plugin, BankstandingXPConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPreferredSize(new Dimension(200, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showBossOverlay() || !config.showDeathsStat()
			|| (!plugin.isInRaid() && !plugin.isFightingBoss() && !config.showDeathsStatAlways()))
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Bossing")
			.color(Color.RED)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Deaths:")
			.right(String.format("%,d", plugin.getBossDeaths()))
			.build());

		return super.render(graphics);
	}
}
