package model;

public class Room
{
	public final String name;

	protected Room(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}