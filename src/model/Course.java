package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Course
{
	public final String name;
	protected final List<Session> sessions = new ArrayList<>();

	protected Course(String name)
	{
		this.name = name;
	}
	
	public List<Session> sessions(String group)
	{
		List<Session> results = new ArrayList<>();
		
		for (Session session : sessions)
		{
			if (group == null || session.groups.contains(group))
			{
				results.add(session);
			}
		}
		
		return Collections.unmodifiableList(results);
	}

	@Override
	public String toString()
	{
		return name;
	}
}