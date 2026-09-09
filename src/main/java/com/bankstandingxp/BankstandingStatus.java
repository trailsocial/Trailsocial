package com.bankstandingxp;

import java.awt.Color;

public enum BankstandingStatus
{
	NOT_LOGGED_IN("Not logged in", Color.GRAY),
	NOT_NEAR_BANK("Not near a bank", Color.LIGHT_GRAY),
	MOVING("Near bank, moving", Color.WHITE),
	WAITING("Standing still...", Color.YELLOW),
	GAINING_XP("Bankstanding", Color.GREEN),
	MAX_LEVEL("Max level!", Color.CYAN);

	private final String label;
	private final Color color;

	BankstandingStatus(String label, Color color)
	{
		this.label = label;
		this.color = color;
	}

	public String getLabel()
	{
		return label;
	}

	public Color getColor()
	{
		return color;
	}
}
