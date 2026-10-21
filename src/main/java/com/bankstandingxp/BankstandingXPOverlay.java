package com.bankstandingxp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class BankstandingXPOverlay extends OverlayPanel
{
	private final BankstandingXPPlugin plugin;
	private final BankstandingXPConfig config;

	@Inject
	private BankstandingXPOverlay(BankstandingXPPlugin plugin, BankstandingXPConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPreferredSize(new Dimension(200, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || !plugin.isNearBank())
		{
			return null;
		}

		long xp = plugin.getXp();
		int level = BankstandingXP.getLevelForXp(xp);
		long xpToNext = BankstandingXP.getXpToNextLevel(xp);

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Bankstanding")
			.color(Color.ORANGE)
			.build());

		int lines = 0;
		if (config.bsLevel())
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Level:").right(String.valueOf(level)).build());
			lines++;
		}
		if (config.bsNextLevel())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Next level:")
				.right(level >= BankstandingXP.MAX_LEVEL ? "-" : String.format("%,d xp", xpToNext))
				.build());
			lines++;
		}
		if (config.bsTotalXp())
		{
			panelComponent.getChildren().add(LineComponent.builder().left("Total XP:").right(String.format("%,d", xp)).build());
			lines++;
		}
		if (config.bsStatus())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Status:")
				.right(plugin.getStatus().getLabel())
				.rightColor(plugin.getStatus().getColor())
				.build());
			lines++;
		}
		if (config.bsHours())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Time spent:").right(StatFormat.duration(plugin.getBankstandingSeconds())).build());
			lines++;
		}
		if (config.bsGeIn())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Time in GE:").right(StatFormat.duration(plugin.getSecondsInGe())).build());
			lines++;
		}
		if (config.bsGeOut())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Time outside GE:").right(StatFormat.duration(plugin.getSecondsOutsideGe())).build());
			lines++;
		}
		if (config.bsProgressBar())
		{
			ProgressBarComponent progressBar = new ProgressBarComponent();
			progressBar.setValue(BankstandingXP.getProgressToNextLevel(xp) * 100);
			progressBar.setBackgroundColor(Color.DARK_GRAY);
			progressBar.setForegroundColor(Color.ORANGE);
			panelComponent.getChildren().add(progressBar);
			lines++;
		}
		if (lines == 0)
		{
			return null; // every line is switched off, so don't draw an empty box
		}

		return super.render(graphics);
	}
}
