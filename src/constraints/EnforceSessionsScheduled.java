package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Session;
import model.Model;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceSessionsScheduled extends Constraint
{
	public EnforceSessionsScheduled(GurobiSolver solver, Model model)
	{
		super(solver, model);
	}
	
	@Override
	public void addConstraints()
	{
		for (Session session : model.sessions())
		{
			GRBLinExpr lhs = new GRBLinExpr();
			
			for (GRBVar var : solver.assignVars(null, null, session))
			{
				lhs.addTerm(1, var);
			}
			
			solver.addEqualsConstr(lhs, 1, Tools.nameConcat(this, session.course, session, "SCHEDULED"));
		}
	}
}