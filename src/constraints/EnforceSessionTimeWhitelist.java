package constraints;

import gurobi.GRBLinExpr;
import java.util.Arrays;
import java.util.List;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import org.javatuples.Pair;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceSessionTimeWhitelist extends Constraint
{
	public EnforceSessionTimeWhitelist(GurobiSolver solver, Model model)
	{
		super(solver, model);
	}

	@Override
	public void addConstraints()
	{
		for (Session session : model.sessions())
		{
			if (!session.whitelistedTimes.isEmpty())
			{
				GRBLinExpr lhs = new GRBLinExpr();

				for (Pair<Day, Slot> time : session.whitelistedTimes)
				{
					List<Day> days = (time.getValue0() != null ? Arrays.asList(time.getValue0()) : model.days());
					List<Slot> slots = (time.getValue1() != null ? Arrays.asList(time.getValue1()) : model.slots());

					for (Day day : days)
					{
						for (Slot slot : slots)
						{
							if (model.sessionFits(session, slot))
							{
								lhs.addTerm(1, solver.startVar(day, slot, session));
							}
						}
					}
				}

				solver.addEqualsConstr(lhs, 1, Tools.nameConcat(this, session));
			}
		}
	}
}