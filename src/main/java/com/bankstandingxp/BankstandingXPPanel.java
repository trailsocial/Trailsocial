package com.bankstandingxp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class BankstandingXPPanel extends PluginPanel
{
	private final JLabel levelLabel = new JLabel();
	private final JLabel nextLevelLabel = new JLabel();
	private final JLabel totalXpLabel = new JLabel();
	private final JLabel statusLabel = new JLabel();
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JCheckBox overlayToggle = new JCheckBox("Show in-game overlay");
	private final JButton resetButton = new JButton("Reset XP");

	private final JLabel bankstandingTimeValue = new JLabel();
	private final JLabel clanHallWealthValue = new JLabel();
	private final JLabel balloonsValue = new JLabel();
	private final JLabel gildedChainsValue = new JLabel();
	private final JLabel sittingTimeValue = new JLabel();
	private final JLabel bossDeathsValue = new JLabel();
	private final JLabel gnomesKilledValue = new JLabel();
	private final JLabel caneHeldValue = new JLabel();
	private final JLabel geTimeInValue = new JLabel();
	private final JLabel geTimeOutValue = new JLabel();
	private final JPanel eventsContainer = new JPanel();

	BankstandingXPPanel(BankstandingXPConfig config, BufferedImage logo, int bankRadius, int idleThresholdSeconds,
		java.util.function.Consumer<Boolean> onOverlayToggle, Runnable onReset)
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new ScrollableContentPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel logoPanel = new JPanel();
		logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
		logoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		logoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		logoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel logoLabel = new JLabel(new ImageIcon(logo));
		logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		logoPanel.add(logoLabel);

		JPanel bankstandingPanel = new JPanel();
		bankstandingPanel.setLayout(new BoxLayout(bankstandingPanel, BoxLayout.Y_AXIS));
		bankstandingPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bankstandingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		bankstandingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel bankstandingTitle = new JLabel("Bankstanding XP");
		bankstandingTitle.setFont(FontManager.getRunescapeBoldFont());
		bankstandingTitle.setForeground(Color.WHITE);
		bankstandingTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		bankstandingPanel.add(bankstandingTitle);
		bankstandingPanel.add(Box.createVerticalStrut(8));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(Color.YELLOW);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		bankstandingPanel.add(statusLabel);
		bankstandingPanel.add(Box.createVerticalStrut(10));

		overlayToggle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overlayToggle.setForeground(Color.WHITE);
		overlayToggle.setFont(FontManager.getRunescapeSmallFont());
		overlayToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
		overlayToggle.setSelected(config.showOverlay());
		overlayToggle.addActionListener(e -> onOverlayToggle.accept(overlayToggle.isSelected()));
		bankstandingPanel.add(overlayToggle);
		bankstandingPanel.add(Box.createVerticalStrut(6));

		resetButton.setFont(FontManager.getRunescapeSmallFont());
		resetButton.setFocusPainted(false);
		resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		resetButton.addActionListener(e -> {
			int choice = JOptionPane.showConfirmDialog(
				this,
				"Reset your Bankstanding level back to 1 (0 xp)? This can't be undone.",
				"Reset Bankstanding XP",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
			if (choice == JOptionPane.YES_OPTION)
			{
				onReset.run();
			}
		});
		bankstandingPanel.add(resetButton);

		JPanel statsPanel = new JPanel();
		statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
		statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel statsTitle = new JLabel("Lifetime Stats");
		statsTitle.setFont(FontManager.getRunescapeBoldFont());
		statsTitle.setForeground(Color.WHITE);
		statsPanel.add(statsTitle);
		statsPanel.add(Box.createVerticalStrut(6));

		statsPanel.add(statRow(levelLabel));
		statsPanel.add(statRow(nextLevelLabel));
		statsPanel.add(statRow(totalXpLabel));

		progressBar.setPreferredSize(new Dimension(0, 10));
		progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
		progressBar.setForeground(ColorScheme.BRAND_ORANGE);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progressBar.setStringPainted(false);
		progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		statsPanel.add(progressBar);
		statsPanel.add(Box.createVerticalStrut(6));

		statsPanel.add(statRow(bankstandingTimeValue));
		statsPanel.add(statRow(clanHallWealthValue));
		statsPanel.add(statRow(balloonsValue));
		statsPanel.add(statRow(gildedChainsValue));
		statsPanel.add(statRow(sittingTimeValue));
		statsPanel.add(statRow(bossDeathsValue));
		statsPanel.add(statRow(gnomesKilledValue));
		statsPanel.add(statRow(caneHeldValue));
		statsPanel.add(statRow(geTimeInValue));
		statsPanel.add(statRow(geTimeOutValue));

		JPanel eventsPanel = new JPanel();
		eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));
		eventsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		eventsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		eventsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel eventsTitle = new JLabel("TrailSocial Events");
		eventsTitle.setFont(FontManager.getRunescapeBoldFont());
		eventsTitle.setForeground(Color.WHITE);
		eventsPanel.add(eventsTitle);
		eventsPanel.add(Box.createVerticalStrut(6));

		eventsContainer.setLayout(new BoxLayout(eventsContainer, BoxLayout.Y_AXIS));
		eventsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		eventsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
		eventsPanel.add(eventsContainer);

		JPanel hintPanel = new JPanel();
		hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
		hintPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hintPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		hintPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel hint = new JLabel("<html>Stand within " + bankRadius + " tiles of a bank and don't move for "
			+ idleThresholdSeconds + "s to start earning Bankstanding XP.</html>");
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(Color.GRAY);
		hintPanel.add(hint);

		content.add(logoPanel);
		content.add(Box.createVerticalStrut(8));
		content.add(bankstandingPanel);
		content.add(Box.createVerticalStrut(8));
		content.add(statsPanel);
		content.add(Box.createVerticalStrut(8));
		content.add(eventsPanel);
		content.add(Box.createVerticalStrut(8));
		content.add(hintPanel);

		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		add(scrollPane, BorderLayout.CENTER);

		update(0L, BankstandingStatus.NOT_LOGGED_IN);
		updateStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
		updateEvents(List.of());
	}

	private static JLabel statRow(JLabel line)
	{
		line.setFont(FontManager.getRunescapeSmallFont());
		line.setForeground(Color.WHITE);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
	}

	void setOverlaySelected(boolean selected)
	{
		overlayToggle.setSelected(selected);
	}

	void update(long xp, BankstandingStatus status)
	{
		int level = BankstandingXP.getLevelForXp(xp);
		long xpToNext = BankstandingXP.getXpToNextLevel(xp);
		levelLabel.setText("Level: " + level);
		nextLevelLabel.setText(level >= BankstandingXP.MAX_LEVEL
			? "Next level: -"
			: String.format("Next level: %,d xp", xpToNext));
		totalXpLabel.setText(String.format("Total: %,d xp", xp));
		statusLabel.setText(status.getLabel());
		statusLabel.setForeground(status.getColor());
		progressBar.setValue((int) Math.round(BankstandingXP.getProgressToNextLevel(xp) * 100));
	}

	void updateStats(long bankstandingSeconds, long clanHallWealth, long balloonsPopped,
		long gildedChainsPickedUp, long sittingSeconds, long bossDeaths, long gnomesKilled, long caneHeldSeconds,
		long secondsInGe, long secondsOutsideGe)
	{
		bankstandingTimeValue.setText("Bankstanding time: " + formatDuration(bankstandingSeconds));
		clanHallWealthValue.setText("Clan Hall wealth: " + String.format("%,d gp", clanHallWealth));
		balloonsValue.setText("Balloons burst: " + String.format("%,d", balloonsPopped));
		gildedChainsValue.setText("Gilded chains: " + String.format("%,d", gildedChainsPickedUp));
		sittingTimeValue.setText("Time sitting: " + formatDuration(sittingSeconds));
		bossDeathsValue.setText("Deaths bossing: " + String.format("%,d", bossDeaths));
		gnomesKilledValue.setText("Gnomes killed: " + String.format("%,d", gnomesKilled));
		caneHeldValue.setText("Cane held for: " + formatDuration(caneHeldSeconds));
		geTimeInValue.setText("Time in GE: " + formatDuration(secondsInGe));
		geTimeOutValue.setText("Time outside GE: " + formatDuration(secondsOutsideGe));
	}

	void updateEvents(List<TrailSocialEvent> events)
	{
		eventsContainer.removeAll();

		if (events.isEmpty())
		{
			JLabel none = new JLabel("No events found.");
			none.setFont(FontManager.getRunescapeSmallFont());
			none.setForeground(Color.GRAY);
			none.setAlignmentX(Component.LEFT_ALIGNMENT);
			eventsContainer.add(none);
		}
		else
		{
			int shown = 0;
			for (TrailSocialEvent event : events)
			{
				if (shown >= 5)
				{
					JLabel more = new JLabel("+" + (events.size() - shown) + " more on trailsocial.net/events");
					more.setFont(FontManager.getRunescapeSmallFont());
					more.setForeground(Color.GRAY);
					more.setAlignmentX(Component.LEFT_ALIGNMENT);
					eventsContainer.add(more);
					break;
				}

				JLabel titleLabel = new JLabel(wrap(event.title == null ? "(untitled)" : event.title));
				titleLabel.setFont(FontManager.getRunescapeSmallFont());
				titleLabel.setForeground(Color.WHITE);
				titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				eventsContainer.add(titleLabel);

				String dateLocation = (event.date == null ? "" : event.date)
					+ (event.location != null && !event.location.isEmpty() ? "  " + event.location : "");
				if (!dateLocation.isEmpty())
				{
					JLabel detail = new JLabel(wrap(dateLocation));
					detail.setFont(FontManager.getRunescapeSmallFont());
					detail.setForeground(Color.LIGHT_GRAY);
					detail.setAlignmentX(Component.LEFT_ALIGNMENT);
					eventsContainer.add(detail);
				}

				eventsContainer.add(Box.createVerticalStrut(6));
				shown++;
			}
		}

		eventsContainer.revalidate();
		eventsContainer.repaint();
	}

	private static String wrap(String text)
	{
		return "<html><div style='width:170px'>" + text + "</div></html>";
	}

	private static String formatDuration(long totalSeconds)
	{
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format("%dh %02dm", hours, minutes);
		}
		if (minutes > 0)
		{
			return String.format("%dm %02ds", minutes, seconds);
		}
		return seconds + "s";
	}

	/**
	 * A JPanel that forces its width to always match the JScrollPane
	 * viewport's width, so content never silently overflows/clips
	 * sideways (the viewport only ever scrolls vertically here).
	 */
	private static class ScrollableContentPanel extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 64;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
