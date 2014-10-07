package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Model
{
	public final int week;
	public final String modelName;
	public final int startHour;
	
	private final List<Day> days = new ArrayList<>();
	private final List<Slot> slots = new ArrayList<>();
	private final List<Room> rooms = new ArrayList<>();
	private final List<Course> courses = new ArrayList<>();
	private final List<Session> sessions = new ArrayList<>();
	private final List<Person> persons = new ArrayList<>();
	
	public Model(int week, int startHour)
	{
		this.week = week;
		this.modelName = "Week " + week;
		this.startHour = startHour;
	}

	public List<Day> days()
	{
		return Collections.unmodifiableList(days);
	}
	
	public void addDay(String name)
	{
		days.add(new Day(name));
	}

	public void addDays(String... names)
	{
		for (String name : names)
		{
			addDay(name);
		}
	}

	public Day findDay(String name)
	{
		for (Day day : days)
		{
			if (day.name.equalsIgnoreCase(name))
			{
				return day;
			}
		}
		
		return null;
	}
	
	public int indexOf(Day day)
	{
		return days.indexOf(day);
	}
	
	public Day previous(Day day)
	{
		return (indexOf(day) == 0 ? null : days.get(indexOf(day) - 1));
	}
	
	public Day next(Day day)
	{
		return (indexOf(day) == (days.size() - 1) ? null : days.get(indexOf(day) + 1));
	}
	
	public List<Slot> slots()
	{
		return Collections.unmodifiableList(slots);
	}
	
	public List<Slot> slots(Session session)
	{
		return slots().subList(0, slots.size() - session.length + 1);
	}

	public void addSlot()
	{
		Slot lastExistingSlot = (slots.isEmpty() ? null : slots.get(slots.size() - 1));
		int newStartHour = (lastExistingSlot == null ? startHour : lastExistingSlot.endHour);
		slots.add(new Slot(newStartHour, newStartHour + 1));
	}
	
	public void addSlots(int numSlots)
	{
		for (int i = 0; i < numSlots; i++)
		{
			addSlot();
		}
	}
	
	public Slot findSlot(Integer startHour)
	{
		for (Slot slot : slots)
		{
			if (((Integer) slot.startHour).equals(startHour))	// startHour might be null, so we compare using the Integer class
			{
				return slot;
			}
		}
		
		return null;
	}
	
	public int indexOf(Slot slot)
	{
		return slots.indexOf(slot);
	}
	
	public Slot previous(Slot slot)
	{
		return (indexOf(slot) == 0 ? null : slots.get(indexOf(slot) - 1));
	}
	
	public Slot next(Slot slot)
	{
		return (indexOf(slot) == (slots.size() - 1) ? null : slots.get(indexOf(slot) + 1));
	}
	
	public Slot next(Slot slot, int num)
	{
		if (num == 0)
		{
			return slot;
		}
		else if (next(slot) == null)
		{
			return null;
		}
		else
		{
			return next(next(slot), num - 1);
		}
	}

	public List<Room> rooms()
	{
		return Collections.unmodifiableList(rooms);
	}
	
	public void addRoom(String name)
	{
		rooms.add(new Room(name));
	}
	
	public void addRooms(String... names)
	{
		for (String name : names)
		{
			addRoom(name);
		}
	}
	
	public Room findRoom(String name)
	{
		for (Room room : rooms)
		{
			if (room.name.equalsIgnoreCase(name))
			{
				return room;
			}
		}
		
		throw new IllegalArgumentException("Unknown room '" + name + "' specified");
	}

	public List<Course> courses()
	{
		return Collections.unmodifiableList(courses);
	}
	
	public Course addCourse(String name)
	{
		Course course = new Course(name);
		courses.add(course);
		return course;
	}
	
	public Course findCourse(String name)
	{
		for (Course course : courses)
		{
			if (course.name.equalsIgnoreCase(name))
			{
				return course;
			}
		}
		
		return null;
	}
	
	public Course findAddCourse(String name)
	{
		for (Course course : courses)
		{
			if (course.name.equalsIgnoreCase(name))
			{
				return course;
			}
		}
		
		return addCourse(name);
	}
	
	public List<Person> persons()
	{
		return Collections.unmodifiableList(persons);
	}
	
	public Person addPerson(String name)
	{
		Person person = new Person(name);
		persons.add(person);
		return person;
	}
	
	public Person findPerson(String name)
	{
		for (Person person : persons)
		{
			if (person.name.equalsIgnoreCase(name))
			{
				return person;
			}
		}
		
		return null;
	}
	
	public Person findAddPerson(String name)
	{
		for (Person person : persons)
		{
			if (person.name.equalsIgnoreCase(name))
			{
				return person;
			}
		}
		
		return addPerson(name);
	}
	
	public final List<Session> sessions()
	{
		return Collections.unmodifiableList(sessions);
	}

	public Session addSession(Course course, String type, String name, Set<String> groups, int length, Room room, int roomBreak)
	{
		Session session = new Session(course, type, name, groups, length, room, roomBreak);
		course.sessions.add(session);
		sessions.add(session);
		return session;
	}
	
	public int indexOf(Session session)
	{
		return sessions.indexOf(session);
	}

	public boolean sessionFits(Session session, Slot startSlot)
	{		
		return indexOf(startSlot) <= (slots.size() - session.length);
	}
	
	public void assertValid()
	{
		if (days.isEmpty() || slots.isEmpty() || courses.isEmpty() || sessions.isEmpty() || persons.isEmpty())
		{
			throw new IllegalStateException("Error: Model is invalid. It is missing either days, slots, courses, sessions or persons.");
		}
	}
}