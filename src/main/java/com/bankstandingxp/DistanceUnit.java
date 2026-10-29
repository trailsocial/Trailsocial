package com.bankstandingxp;

/** How the distance walked is shown. Public because the settings interface exposes it. */
public enum DistanceUnit
{
	MILES("Miles"),
	KILOMETRES("Kilometres");

	private final String label;

	DistanceUnit(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
