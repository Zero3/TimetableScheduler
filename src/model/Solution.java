package model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;

// This class allows for storage of a solution to a model and contains a schedule lookup method based on string identifiers.
// This approach is intentional, as we want to prevent direct access to the underlying model in order to avoid confusion
// about whether the scope of courses, sessions, ... are problem or model specific.
public class Solution
{
	private final Model model;
	private final Map<Session, Pair<Day, Slot>> schedule;
	
	public Solution(Model model, Map<Session, Pair<Day, Slot>> schedule)
	{
		this.model = model;
		this.schedule = schedule;
	}
	
	public boolean attends(String personName, String courseName)
	{
		// Look up arguments in model
		Person person = model.findPerson(personName);
		Course course = model.findCourse(courseName);
		
		if (course == null || person == null)
		{
			return false;
		}
		
		return person.sessionsByCourse(course).size() > 0;
	}
	
	public List<String> sessionTypesScheduled(String personName, String courseName, String dayName, int startHour)
	{
		List<String> sessionSchedules = new LinkedList<>();
		
		// Look up arguments in model
		Person person = model.findPerson(personName);
		Course course = model.findCourse(courseName);
		Day day = model.findDay(dayName);
		Slot slot = model.findSlot(startHour);
		
		if (course == null || person == null || day == null || slot == null)
		{
			return sessionSchedules;
		}
		
		// Generate and return list of session schedules
		List<Session> sessions = person.sessionsByCourse(course);
		
		for (Session session : sessions)
		{
			if (schedule.get(session).getValue0().equals(day) && schedule.get(session).getValue1().equals(slot))
			{
				sessionSchedules.add(session.type);
			}
		}
		
		return sessionSchedules;
	}
}