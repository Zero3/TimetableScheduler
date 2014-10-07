package constraints;

import gurobi.GRBLinExpr;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import solvers.GurobiSolver;
import util.Tools;

public class EnforceRoomBreaks extends Constraint
{
	public EnforceRoomBreaks(GurobiSolver solver, Model model)
	{
		super(solver, model);
	}
	
	@Override
	public void addConstraints()
	{
		for (Session session : model.sessions())
		{
			if (session.roomBreak > 0)
			{
				if (session.room == null)
				{
					throw new IllegalStateException("This constraint does not support enforcing room breaks for sessions without a specific room assigned");
				}
				
				for (Day day : model.days())
				{
					for (Slot slot : model.slots(session))
					{
						// If session starts here
						GRBLinExpr lhs = new GRBLinExpr();
						lhs.addTerm(1, solver.startVar(day, slot, session));
						int breakSlotsDone = 0;

						for (Slot breakSlot = model.next(slot, session.length); breakSlot != null; breakSlot = model.next(breakSlot))
						{
							if (!model.sessionFits(session, breakSlot) || breakSlotsDone == session.roomBreak)
							{
								break;
							}

							// For every break slot after the session
							for (Session otherSession : model.sessions())
							{
								if (otherSession.room == session.room)
								{
									lhs.addTerm(1, solver.startVar(day, breakSlot, otherSession));
								}
							}

							breakSlotsDone++;
						}

						if (lhs.size() > 1)
						{
							solver.addLessOrEqualsConstr(lhs, 1, Tools.nameConcat(this, session, day, slot));
						}
					}
				}
			}
		}
	}
}