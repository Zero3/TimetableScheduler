package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Person
{
	public final String name;
	public double weight = 1;	// Reasonable default
		
	private final Set<Session> allSessions = new HashSet<>();
	private final Map<Course, List<Session>> allSessionsByCourse = new HashMap<>();
	private final Set<Session> staffSessions = new HashSet<>();
	
	public Person(String name)
	{
		this.name = name;
	}
	
	// This method is called by a session when a person is added to it
	protected void addSession(Session session)
	{
		List<Session> sessionList = allSessionsByCourse.get(session.course);
		
		if (sessionList == null)
		{
			sessionList = new LinkedList<>();
			allSessionsByCourse.put(session.course, sessionList);
		}
		
		sessionList.add(session);
		allSessions.add(session);
		
		if (session.staffs.contains(this))
		{
			staffSessions.add(session);
		}
	}
	
	public Set<Session> sessions()
	{
		return Collections.unmodifiableSet(allSessions);
	}
	
	public Set<Course> courses()
	{
		return Collections.unmodifiableSet(allSessionsByCourse.keySet());
	}
	
	public Set<Session> staffSessions()
	{
		return Collections.unmodifiableSet(staffSessions);
	}
	
	public List<Session> sessionsByCourse(Course course)
	{
		List<Session> sessions = allSessionsByCourse.get(course);
		return Collections.unmodifiableList(sessions != null ? sessions : new LinkedList<Session>());
	}
	
	public Set<List<Session>> studentSessionsByCourse()
	{
		Set<List<Session>> resultOuterSet = new HashSet<>();
		
		for (List<Session> sessionSet : allSessionsByCourse.values())
		{
			List<Session> resultInnerSet = new ArrayList<>();
			
			for (Session session : sessionSet)
			{
				if (session.students.contains(this))
				{
					resultInnerSet.add(session);
				}
			}
			
			resultOuterSet.add(Collections.unmodifiableList(resultInnerSet));
		}
		
		return Collections.unmodifiableSet(resultOuterSet);
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}