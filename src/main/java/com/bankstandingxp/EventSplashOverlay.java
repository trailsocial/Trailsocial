package com.bankstandingxp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * A brief, prominent banner shown once at login if there are new
 * trailsocial.net events to announce. Auto-hides itself after a fixed
 * lifetime with no fade animation needed - it's a "you have mail" splash,
 * not a persistent HUD element.
 */
public class EventSplashOverlay extends OverlayPanel
{
	private static final long LIFETIME_MS = 12_000;

	private List<TrailSocialEvent> events = List.of();
	private long shownAtMs = 0;

	@Inject
	private EventSplashOverlay()
	{
		setPosition(OverlayPosition.TOP_CENTER);
		setPreferredSize(new Dimension(260, 0));
	}

	void show(List<TrailSocialEvent> newEvents)
	{
		this.events = newEvents;
		this.shownAtMs = System.currentTimeMillis();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (events.isEmpty() || System.currentTimeMillis() - shownAtMs > LIFETIME_MS)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(events.size() == 1 ? "New TrailSocial Event!" : events.size() + " New TrailSocial Events!")
			.color(Color.CYAN)
			.build());

		int shown = 0;
		for (TrailSocialEvent event : events)
		{
			if (shown >= 3)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("+" + (events.size() - shown) + " more...")
					.build());
				break;
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left(truncate(event.title, 28))
				.leftColor(Color.WHITE)
				.build());
			if (event.date != null && !event.date.isEmpty())
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(event.date)
					.leftColor(Color.LIGHT_GRAY)
					.build());
			}
			shown++;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("See trailsocial.net/events")
			.leftColor(Color.GRAY)
			.build());

		return super.render(graphics);
	}

	private static String truncate(String text, int maxLen)
	{
		if (text == null)
		{
			return "";
		}
		return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
	}
}
