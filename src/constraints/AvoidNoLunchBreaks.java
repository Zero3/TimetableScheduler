package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import java.util.LinkedList;
import java.util.List;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import model.Person;
import solvers.GurobiSolver;
import util.Tools;

public class AvoidNoLunchBreaks extends Constraint
{
	private GRBVar[][][] busySlots;			// Indicator of whether a person has a session scheduled on a specific day in a specific lunch break slot
	private GRBVar[][] lunchBreakDenials;	// Indicator of whether all the person's lunch break slots are occupied during a given day
	private final List<Slot> lunchBreakSlots = new LinkedList<>();
	private final int penalty;

	public AvoidNoLunchBreaks(GurobiSolver solver, Model model, List<Slot> lunchBreakSlots, int penaltyPerDenial)
	{
		super(solver, model);
		
		this.lunchBreakSlots.addAll(lunchBreakSlots);
		this.penalty = penaltyPerDenial;
	}
	
	@Override
	public void addVariables()
	{
		busySlots = new GRBVar[model.persons().size()][model.days().size()][lunchBreakSlots.size()];

		for (Person person : model.persons())
		{
			for (Day day : model.days())
			{
				for (Slot slot : lunchBreakSlots)
				{
					busySlots[model.persons().indexOf(person)][model.indexOf(day)][lunchBreakSlots.indexOf(slot)] = solver.addBinaryVar(Tools.nameConcat(this, "BUSY", person, day, slot));
				}
			}
		}
		
		lunchBreakDenials = new GRBVar[model.persons().size()][model.days().size()];

		for (Person person : model.persons())
		{
			for (Day day : model.days())
			{
				lunchBreakDenials[model.persons().indexOf(person)][model.indexOf(day)] = solver.addBinaryVar(Tools.nameConcat(this, person, day));
			}
		}
	}

	@Override
	public void addConstraints()
	{
		for (Person person : model.persons())
		{
			for (Day day : model.days())
			{
				// If a given lunch break slot is occupied by a session, we force the corresponding "busy" variable to be 1.
				// We need to have a separate variable for each slot because a shared would fail in case the person has overlap in some of the slots and has a hole in one of the others.
				for (Slot slot : lunchBreakSlots)
				{
					GRBLinExpr lhs = new GRBLinExpr();

					for (Session session : person.sessions())
					{
						for (GRBVar var : solver.assignVars(day, slot, session))
						{
							lhs.addTerm(1, var);
						}
					}

					GRBLinExpr rhs = new GRBLinExpr();
					rhs.addTerm(person.sessions().size(), busySlots[model.persons().indexOf(person)][model.indexOf(day)][lunchBreakSlots.indexOf(slot)]);
					
					solver.addLessOrEqualsConstr(lhs, rhs, Tools.nameConcat(this, "Busy", person, day, slot));
				}
				
				// Now force the daily denial variables to be 1 in case all the busy variables are set to 1
				GRBLinExpr lhs = new GRBLinExpr();
				
				for (Slot slot : lunchBreakSlots)
				{
					lhs.addTerm(1, busySlots[model.persons().indexOf(person)][model.indexOf(day)][lunchBreakSlots.indexOf(slot)]);
				}
				
				GRBLinExpr rhs = new GRBLinExpr();
				rhs.addConstant(lunchBreakSlots.size() - 1);
				rhs.addTerm(1, lunchBreakDenials[model.persons().indexOf(person)][model.indexOf(day)]);

				solver.addLessOrEqualsConstr(lhs, rhs, Tools.nameConcat(this, "Denial", person, day));
			}
		}
	}

	@Override
	public void addObjectives()
	{
		for (Person person : model.persons())
		{
			for (Day day : model.days())
			{
				solver.addObjective(person.weight * penalty, lunchBreakDenials[model.persons().indexOf(person)][model.indexOf(day)], Tools.nameConcat(this, person, day));
			}
		}
	}
}