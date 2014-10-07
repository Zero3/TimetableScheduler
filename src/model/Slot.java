package model;

import util.Tools;

public class Slot
{
	public final int startHour;
	public final int endHour;

	protected Slot(int startHour, int endHour)
	{
		this.startHour = startHour;
		this.endHour = endHour;
	}

	@Override
	public String toString()
	{
		return Tools.doubleZeroPad(startHour) + "-" + Tools.doubleZeroPad(endHour);
	}
}