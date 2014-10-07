package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import java.util.Set;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import model.Person;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceNoStaffConflicts extends Constraint
{
	public EnforceNoStaffConflicts(GurobiSolver solver, Model model)
	{
		super(solver, model);
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
					Set<Session> personStaffSessions = person.staffSessions();
					
					if (personStaffSessions.size() > 1)
					{
						GRBLinExpr lhs = new GRBLinExpr();

						for (Session session : personStaffSessions)
						{
							// Only the sessions in which this person is staff should conflict with each other
							for (GRBVar var : solver.assignVars(day, slot, session))
							{
								lhs.addTerm(1, var);
							}
						}

						solver.addLessOrEqualsConstr(lhs, 1, Tools.nameConcat(this, person, day, slot));
					}
				}
			}
		}
	}
}