package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import java.util.LinkedList;
import java.util.List;
import model.Day;
import model.Model;
import solvers.GurobiSolver;
import util.Tools;

// TODO: Actually use this constraint!
public class EnforceDayBlacklisting extends Constraint
{
	private final List<Day> blacklistedDays = new LinkedList<>();

	public EnforceDayBlacklisting(GurobiSolver solver, Model model, List<Day> blacklistedDays)
	{
		super(solver, model);
		
		this.blacklistedDays.addAll(blacklistedDays);
	}

	@Override
	public void addConstraints()
	{
		for (Day day : blacklistedDays)
		{
			GRBLinExpr lhs = new GRBLinExpr();
			
			for (GRBVar var : solver.assignVars(day, null, null))
			{
				lhs.addTerm(1, var);
			}

			solver.addLessOrEqualsConstr(lhs, 0, Tools.nameConcat(this, day));
		}
	}
}