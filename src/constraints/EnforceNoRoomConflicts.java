package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Day;
import model.Room;
import model.Session;
import model.Slot;
import model.Model;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceNoRoomConflicts extends Constraint
{
	public EnforceNoRoomConflicts(GurobiSolver solver, Model model)
	{
		super(solver, model);
	}
	
	@Override
	public void addConstraints()
	{
		for (Day day : model.days())
		{
			for (Slot slot : model.slots())
			{
				for (Room room : model.rooms())
				{
					GRBLinExpr lhs = new GRBLinExpr();
					
					for (Session session : model.sessions())
					{
						if (session.room == room)
						{
							for (GRBVar var : solver.assignVars(day, slot, session))
							{
								lhs.addTerm(1, var);
							}
						}
					}

					if (lhs.size() > 1)
					{
						solver.addLessOrEqualsConstr(lhs, 1, Tools.nameConcat(this, day, slot, room));
					}
				}
			}
		}
	}
}