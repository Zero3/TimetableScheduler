package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import org.javatuples.Pair;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceSessionTimeBlacklist extends Constraint
{
	public EnforceSessionTimeBlacklist(GurobiSolver solver, Model model)
	{
		super(solver, model);
	}

	@Override
	public void addConstraints()
	{
		GRBLinExpr lhs = new GRBLinExpr();
		
		for (Session session : model.sessions())
		{
			for (Pair<Day, Slot> time : session.blacklistedTimes)
			{
				for (GRBVar var : solver.assignVars(time.getValue0(), time.getValue1(), session))
				{
					lhs.addTerm(1, var);
				}
			}
		}
		
		if (lhs.size() > 0)
		{
			solver.addEqualsConstr(lhs, 0, Tools.nameConcat(this));
		}
	}
}