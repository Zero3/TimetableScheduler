package constraints;

import java.util.List;
import model.Day;
import model.Model;
import model.Person;
import model.Session;
import model.Slot;
import model.Solution;
import solvers.GurobiSolver;
import util.Tools;

// Assign objective function bonuses each time we are able to schedule sessions such that they are "stable" with respect to the solutions of earlier weeks
public class AvoidTimetableInstability extends Constraint
{
	private static final double SESSION_TYPE_MISMATCH_FACTOR = 0.75;
	private static final double MINIMUM_ACCEPTED_BONUS = 0.01;
	
	private final List<Solution> solutions;
	private final int baseBonus;
	
	public AvoidTimetableInstability(GurobiSolver solver, Model model, List<Solution> solutions, int bonus)
	{
		super(solver, model);
		
		this.solutions = solutions;
		this.baseBonus = bonus;
	}
	
	@Override
	public void addObjectives()
	{
		for (Session session : model.sessions())
		{
			for (Day day : model.days())
			{
				for (Slot slot : model.slots(session))
				{
					double factor = 0;

					for (Person person : session.persons)
					{
						factor += baseBonus * person.weight * bonusFactor(session, day, slot, person);
					}

					if (factor > MINIMUM_ACCEPTED_BONUS)
					{
						solver.addObjective(-factor, solver.startVar(day, slot, session), Tools.nameConcat(this, session, day, slot));	// Negative because this is a bonus and not a penalty
					}
				}
			}
		}
	}

	private double bonusFactor(Session session, Day day, Slot slot, Person person)
	{
		int scheduleCount = 0;		// Number of schedules the given person attends the course of the given session
		double totalWeight = 0;		// The total weight over these schedules
		
		for (Solution solution : solutions)
		{
			if (solution.attends(person.name, session.course.name))
			{
				scheduleCount++;
			
				// Now find the types of all sessions the given person had for the same course at the given time (most likely just one, assuming a constraint to avoid overlaps was used) and keep the best type factor
				List<String> sessionTypesScheduled = solution.sessionTypesScheduled(person.name, session.course.name, day.name, slot.startHour);

				// Calculate weight for this schedule. The best weight, in case of overlap.
				double weight = 0;

				for (String sessionType : sessionTypesScheduled)
				{
					// We use a lower weight if the type of the previously scheduled session is different. This is done to avoid lecture and exercise sessions switching places uncontrollably.
					weight = Math.max(weight, sessionType.equals(session.type) ? 1 : SESSION_TYPE_MISMATCH_FACTOR);
				}

				totalWeight += weight;
			}
		}

		return ((scheduleCount == 0) ? 0 : (totalWeight / scheduleCount));
	}
}