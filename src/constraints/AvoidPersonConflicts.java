package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import model.Person;
import solvers.GurobiSolver;
import util.Tools;

// Note:	This is not a hard constraint because a stupid student could sign up for more courses
//			than can be fit into his schedule or perhaps just enough to screw up all the other objectives
// Note:	This constraint includes both staff and non-staff sessions for each person on purpose.
//			This is because we want to avoid overlap between a student session and staff session
//			for the same person. Hard no-staff-conflicts constraint is handled elsewhere.
public class AvoidPersonConflicts extends Constraint
{
	private GRBVar[][][] overlaps;
	private final int penalty;

	public AvoidPersonConflicts(GurobiSolver solver, Model model, int penaltyPerConflict)
	{
		super(solver, model);
		
		this.penalty = penaltyPerConflict;
	}

	@Override
	public void addVariables()
	{
		overlaps = new GRBVar[model.persons().size()][model.days().size()][model.slots().size()];

		for (Person person : model.persons())
		{
			for (Day day : model.days())
			{
				for (Slot slot : model.slots())
				{
					overlaps[model.persons().indexOf(person)][model.indexOf(day)][model.indexOf(slot)] = solver.addIntegerVar(0, null, Tools.nameConcat(this, person, day, slot));
				}	
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
				for (Slot slot : model.slots())
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
					rhs.addConstant(1);
					rhs.addTerm(1, overlaps[model.persons().indexOf(person)][model.indexOf(day)][model.indexOf(slot)]);

					solver.addLessOrEqualsConstr(lhs, rhs, Tools.nameConcat(this, person, day, slot));
				}
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
				for (Slot slot : model.slots())
				{
					solver.addObjective(person.weight * penalty, overlaps[model.persons().indexOf(person)][model.indexOf(day)][model.indexOf(slot)], Tools.nameConcat(this, person, day, slot));
				}
			}
		}
	}
}