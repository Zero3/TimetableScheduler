package constraints;

import gurobi.GRBVar;
import java.util.Arrays;
import java.util.List;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import solvers.GurobiSolver;
import util.GeputHashMap;
import util.Tools;

// Assigns penalties for each session each person has scheduled at undesired times
public class AvoidUndesiredTimes extends Constraint
{
	private final GeputHashMap<Pair<Day, Slot>, Integer> penaltyMap = new GeputHashMap<>();
	
	public AvoidUndesiredTimes(GurobiSolver solver, Model model, List<Triplet<Day, Slot, Integer>> times)
	{
		super(solver, model);
		
		for (Triplet<Day, Slot, Integer> time : times)
		{
			List<Day> days = (time.getValue0() != null ? Arrays.asList(time.getValue0()) : model.days());
			List<Slot> slots = (time.getValue1() != null ? Arrays.asList(time.getValue1()) : model.slots());
		
			for (Day day : days)
			{
				for (Slot slot : slots)
				{
					Integer currentPenalty = penaltyMap.geput(new Pair<>(day, slot), 0);
					penaltyMap.put(new Pair<>(day, slot), currentPenalty += time.getValue2());
				}
			}
		}
	}
	
	@Override
	public void addObjectives()
	{
		for (Pair<Day, Slot> entry : penaltyMap.keySet())
		{
			for (Session session : model.sessions())
			{
				for (GRBVar var : solver.assignVars(entry.getValue0(), entry.getValue1(), session))
				{
					solver.addObjective(penaltyMap.get(entry) * session.weightedPersonCount(), var, Tools.nameConcat(this, entry.getValue0(), entry.getValue1(), session));
				}
			}
		}
	}
}