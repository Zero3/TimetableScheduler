package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.javatuples.Pair;

public class Session
{
	public final Course course;
	public final String type;
	public final Set<String> groups;
	public final String title;
	public final int length;
	public final Room room;
	public final int roomBreak;

	private final Set<Person> personsInternal = new HashSet<>();
	public final Set<Person> persons = Collections.unmodifiableSet(personsInternal);
	
	private final Set<Person> studentsInternal = new HashSet<>();
	public final Set<Person> students = Collections.unmodifiableSet(studentsInternal);
	
	private final Set<Person> staffInternal = new HashSet<>();
	public final Set<Person> staffs = Collections.unmodifiableSet(staffInternal);
	
	private final Set<Pair<Day, Slot>> whitelistedTimesInternal = new HashSet<>();
	public final Set<Pair<Day, Slot>> whitelistedTimes = Collections.unmodifiableSet(whitelistedTimesInternal);
	
	private final Set<Pair<Day, Slot>> blacklistedTimesInternal = new HashSet<>();
	public final Set<Pair<Day, Slot>> blacklistedTimes = Collections.unmodifiableSet(blacklistedTimesInternal);
	
	protected Session(Course course, String type, String title, Set<String> groups, int length, Room room, int roomBreak)
	{
		this.course = course;
		this.type = type;
		this.groups = Collections.unmodifiableSet(new TreeSet<>(groups));
		this.title = title;
		this.length = length;
		this.room = room;
		this.roomBreak = roomBreak;
	}

	public void addStaff(Person person)
	{
		personsInternal.add(person);
		staffInternal.add(person);
		person.addSession(this);
	}
	
	public void addStudent(Person person)
	{
		personsInternal.add(person);
		studentsInternal.add(person);
		person.addSession(this);
	}
	
	public double weightedPersonCount()
	{
		double totalWeight = 0;
		
		for (Person person : persons)
		{
			totalWeight += person.weight;
		}
		
		return totalWeight;
	}
	
	public void whitelistTime(Pair<Day, Slot> time)
	{
		if (!blacklistedTimes.isEmpty())
		{
			throw new IllegalStateException("Cannot whitelist time " + time + " for session " + this + " when it already has blacklisted times");
		}
		
		whitelistedTimesInternal.add(time);
	}

	public void blacklistTime(Pair<Day, Slot> time)
	{
		if (!whitelistedTimes.isEmpty())
		{
			throw new IllegalStateException("Cannot blacklist time " + time + " for session " + this + " when it already has whitelisted times");
		}
		
		blacklistedTimesInternal.add(time);
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "-" + course + "-" + type + "-" + groups;
	}
}