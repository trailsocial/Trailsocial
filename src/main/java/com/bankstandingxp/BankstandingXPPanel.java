package com.bankstandingxp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
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

	BankstandingXPPanel(BankstandingXPConfig config, int bankRadius, int idleThresholdSeconds,
		java.util.function.Consumer<Boolean> onOverlayToggle, Runnable onReset)
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Bankstanding XP");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(title);
		header.add(Box.createVerticalStrut(8));

		levelLabel.setFont(new Font("Arial", Font.BOLD, 28));
		levelLabel.setForeground(Color.ORANGE);
		levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(levelLabel);

		nextLevelLabel.setFont(FontManager.getRunescapeSmallFont());
		nextLevelLabel.setForeground(Color.LIGHT_GRAY);
		nextLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(nextLevelLabel);

		totalXpLabel.setFont(FontManager.getRunescapeSmallFont());
		totalXpLabel.setForeground(Color.GRAY);
		totalXpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(totalXpLabel);
		header.add(Box.createVerticalStrut(6));

		progressBar.setPreferredSize(new Dimension(0, 12));
		progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
		progressBar.setForeground(ColorScheme.BRAND_ORANGE);
		progressBar.setBackground(ColorScheme.DARK_GRAY_COLOR);
		progressBar.setStringPainted(false);
		header.add(progressBar);
		header.add(Box.createVerticalStrut(10));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(Color.YELLOW);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		header.add(statusLabel);
		header.add(Box.createVerticalStrut(10));

		overlayToggle.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overlayToggle.setForeground(Color.WHITE);
		overlayToggle.setFont(FontManager.getRunescapeSmallFont());
		overlayToggle.setAlignmentX(Component.CENTER_ALIGNMENT);
		overlayToggle.setSelected(config.showOverlay());
		overlayToggle.addActionListener(e -> onOverlayToggle.accept(overlayToggle.isSelected()));
		header.add(overlayToggle);
		header.add(Box.createVerticalStrut(6));

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
		header.add(resetButton);

		JPanel infoPanel = new JPanel(new GridLayout(0, 1, 0, 4));
		infoPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JLabel hint = new JLabel("<html>Stand within " + bankRadius + " tiles of a bank and don't move for "
			+ idleThresholdSeconds + "s to start earning Bankstanding XP.</html>");
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(Color.GRAY);
		infoPanel.add(hint);

		add(header, BorderLayout.NORTH);
		add(infoPanel, BorderLayout.CENTER);

		update(0L, BankstandingStatus.NOT_LOGGED_IN);
	}

	void setOverlaySelected(boolean selected)
	{
		overlayToggle.setSelected(selected);
	}

	void update(long xp, BankstandingStatus status)
	{
		int level = BankstandingXP.getLevelForXp(xp);
		long xpToNext = BankstandingXP.getXpToNextLevel(xp);
		levelLabel.setText("Level " + level);
		nextLevelLabel.setText(level >= BankstandingXP.MAX_LEVEL
			? "Next level: -"
			: String.format("Next level: %,d xp", xpToNext));
		totalXpLabel.setText(String.format("Total: %,d xp", xp));
		statusLabel.setText(status.getLabel());
		statusLabel.setForeground(status.getColor());
		progressBar.setValue((int) Math.round(BankstandingXP.getProgressToNextLevel(xp) * 100));
	}
}
