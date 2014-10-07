package model;

public class Day
{
	public final String name;

	protected Day(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}