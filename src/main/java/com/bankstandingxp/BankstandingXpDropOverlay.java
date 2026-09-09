package com.bankstandingxp;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

/**
 * Mimics the vanilla XP drop popup for the fake Bankstanding skill, since
 * the real xp-drop widget only knows about the 23 real in-game skills.
 * Laid out the same way as a real drop: number first, skill icon after.
 */
public class BankstandingXpDropOverlay extends Overlay
{
	private static final long LIFETIME_MS = 3000;
	private static final long FADE_MS = 700;
	private static final int MAX_STACK = 6;
	private static final int BASE_ICON_SIZE = 17;

	private final BankstandingXPConfig config;
	private final Deque<Drop> drops = new ArrayDeque<>();
	private final BufferedImage icon;

	@Inject
	private BankstandingXpDropOverlay(BankstandingXPConfig config)
	{
		this.config = config;
		this.icon = ImageUtil.loadImageResource(getClass(), "bank_icon.png");
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	void addDrop(long amount)
	{
		if (amount <= 0)
		{
			return;
		}
		drops.addLast(new Drop(amount, System.currentTimeMillis()));
		while (drops.size() > MAX_STACK)
		{
			drops.removeFirst();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showXpDrops())
		{
			return null;
		}

		long now = System.currentTimeMillis();
		drops.removeIf(d -> now - d.timestamp > LIFETIME_MS);

		if (drops.isEmpty())
		{
			return null;
		}

		float scale = Math.max(0.25f, config.dropSize() / 100f);
		int iconSize = Math.round(BASE_ICON_SIZE * scale);

		Font baseFont = FontManager.getRunescapeSmallFont();
		Font font = baseFont.deriveFont(baseFont.getSize2D() * scale);
		graphics.setFont(font);
		FontMetrics fm = graphics.getFontMetrics();
		int rowHeight = Math.max(iconSize, fm.getHeight()) + Math.round(3 * scale);

		int maxWidth = 0;
		int y = 0;
		for (Drop drop : drops)
		{
			String text = String.valueOf(drop.amount);
			long age = now - drop.timestamp;
			float alpha = age > LIFETIME_MS - FADE_MS
				? Math.max(0f, 1f - (age - (LIFETIME_MS - FADE_MS)) / (float) FADE_MS)
				: 1f;

			int textWidth = fm.stringWidth(text);
			int textX = 0;
			int textY = y + (rowHeight + fm.getAscent()) / 2 - 2;
			int iconX = textWidth + Math.round(4 * scale);

			graphics.setColor(new Color(0, 0, 0, (int) (200 * alpha)));
			graphics.drawString(text, textX + 1, textY + 1);

			graphics.setColor(new Color(255, 255, 0, (int) (255 * alpha)));
			graphics.drawString(text, textX, textY);

			Composite original = graphics.getComposite();
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			graphics.drawImage(icon, iconX, y + (rowHeight - iconSize) / 2, iconSize, iconSize, null);
			graphics.setComposite(original);

			maxWidth = Math.max(maxWidth, iconX + iconSize);
			y += rowHeight;
		}

		return new Dimension(maxWidth, y);
	}

	private static final class Drop
	{
		private final long amount;
		private final long timestamp;

		private Drop(long amount, long timestamp)
		{
			this.amount = amount;
			this.timestamp = timestamp;
		}
	}
}
