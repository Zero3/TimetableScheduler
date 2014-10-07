package constraints;

import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import solvers.GurobiSolver;
import util.Tools;

public class AvoidUnalignedAllocation extends Constraint
{
	private final int penalty;
	
	public AvoidUnalignedAllocation(GurobiSolver solver, Model model, int penalty)
	{
		super(solver, model);
		
		this.penalty = penalty;
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
					if
					(
						session.length == 2 && model.indexOf(slot) % 2 != 0
						|| session.length == 3 && model.indexOf(slot) % 3 != 0
					)
					{
						solver.addObjective(penalty, solver.startVar(day, slot, session), Tools.nameConcat(this, day, slot, session));
					}
				}
			}
		}
	}
}