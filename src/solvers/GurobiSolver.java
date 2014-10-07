package solvers;

import callbacks.gurobi.GurobiCallback;
import callbacks.gurobi.GurobiCallbackCoordinator;
import constraints.Constraint;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import model.Day;
import model.Model;
import model.Session;
import model.Slot;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import util.AsciiTable;
import util.GeputHashMap;
import util.Tools;

public class GurobiSolver implements Solver, AutoCloseable
{
	private static final double DOUBLE_ZERO_THRESHOLD = 0.01;	// Consider values closer than this to zero as zero for various purposes
	private static final DecimalFormat OBJECTIVE_VALUE_FORMAT = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	
	private final Model model;
		
	private final List<Constraint> constraints = new LinkedList<>();
	private final List<Triplet<Double, GRBVar, String>> objectives = new LinkedList<>();
	
	private final GurobiCallbackCoordinator callbackCoordinator = new GurobiCallbackCoordinator(this);
	
	private final GRBModel mipModel;
	private GRBVar[][][] assignVars = null;
	private boolean solved = false;

	public GurobiSolver(Model model)
	{
		try
		{
			this.model = model;
			mipModel = new GRBModel(new GRBEnv());
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	public void addCallback(GurobiCallback callback)
	{
		callbackCoordinator.callbacks.add(callback);
	}
	
	@Override
	public void addConstraint(Constraint constraint)
	{
		constraints.add(constraint);
	}
	
	@Override
	public boolean solve(boolean logSolverOutput)
	{
		try
		{
			if (solved)
			{
				throw new IllegalStateException("This solver has already been used to solve its model. Create a new solver to solve another.");
			}
			
			// Ensure timetabling model is valid
			model.assertValid();

			// Setup assignment variables
			assignVars = new GRBVar[model.days().size()][model.sessions().size()][];

			for (Session session : model.sessions())
			{
				int slotCount = model.slots().size() - session.length + 1;	// Only create assignment variables for time slots in which the session will actually fit (a session of 2 hours cannot fit in the last slot of the day, for example)

				for (Day day : model.days())
				{				
					assignVars[model.indexOf(day)][model.indexOf(session)] = new GRBVar[slotCount];

					for (Slot slot : model.slots().subList(0, slotCount))
					{
						assignVars[model.indexOf(day)][model.indexOf(session)][model.indexOf(slot)] = addBinaryVar(Tools.nameConcat("ASSIGNMENT", day, slot, session.course, session));
					}
				}
			}

			mipModel.update();
			int sessionAssignmentVarCount = mipModel.getVars().length;

			// Setup constraint variables
			GeputHashMap<String, Integer> constraintVariableCounts = new GeputHashMap<>();

			for (Constraint constraint : constraints)
			{
				int prevCount = mipModel.getVars().length;

				constraint.addVariables();
				mipModel.update();

				int constraintVariableCount = constraintVariableCounts.geput(constraint.toString(), 0);
				constraintVariableCounts.put(constraint.toString(), (constraintVariableCount + (mipModel.getVars().length - prevCount)));
			}		

			// Setup constraint constraints
			GeputHashMap<String, Integer> constraintCounts = new GeputHashMap<>();
			GeputHashMap<String, Integer> constraintTermCounts = new GeputHashMap<>();
			int constraintTermsTotal = 0;

			for (Constraint constraint : constraints)
			{
				int prevConstraintCount = (mipModel.getConstrs() == null ? 0 : mipModel.getConstrs().length);
				int prevConstraintTermCount = mipModel.get(GRB.IntAttr.NumNZs);

				constraint.addConstraints();
				mipModel.update();

				int currentConstraintCount = (mipModel.getConstrs() == null ? 0 : mipModel.getConstrs().length);

				int constraintCount = constraintCounts.geput(constraint.toString(), 0);
				constraintCounts.put(constraint.toString(), constraintCount + (currentConstraintCount - prevConstraintCount));

				int constraintTermCount = constraintTermCounts.geput(constraint.toString(), 0);
				int constraintTermsAdded = (mipModel.get(GRB.IntAttr.NumNZs) - prevConstraintTermCount);
				constraintTermsTotal += constraintTermsAdded;
				constraintTermCounts.put(constraint.toString(), constraintTermCount + constraintTermsAdded);
			}

			// Setup constraint objectives
			GeputHashMap<String, Integer> constraintObjectiveTermCounts = new GeputHashMap<>();
			GRBLinExpr completeObjective = new GRBLinExpr();
			
			for (Constraint constraint : constraints)
			{
				int prevCount = objectives.size();

				constraint.addObjectives();

				int constraintObjectiveTermCount = constraintObjectiveTermCounts.geput(constraint.toString(), 0);
				constraintObjectiveTermCounts.put(constraint.toString(), constraintObjectiveTermCount + (objectives.size() - prevCount));
			}
			
			for (Triplet<Double, GRBVar, String> objective : objectives)
			{
				completeObjective.addTerm(objective.getValue0(), objective.getValue1());
			}
			
			mipModel.update();
			mipModel.setObjective(completeObjective, GRB.MINIMIZE);

			// Time to output a stats table of variables, constraints, constraint terms and objective terms
			AsciiTable statsTable = new AsciiTable(true, false, false, false, false);
			
			// Add header row
			statsTable.addRow((model.modelName + " (" + model.sessions().size() + " sessions)"), "Variables", "Constraints", "Constraint terms", "Objective terms");
			statsTable.addDelimiter();
			
			// Add row for session assignment variables
			statsTable.addRow("<Session assignment>", sessionAssignmentVarCount, "0", "0", "0");

			// Add constraint rows
			for (String constraintName : new TreeSet<>(constraintVariableCounts.keySet()))		// Using the key set from any of our maps will work. Wrap in TreeSet for sorting.
			{
				statsTable.addRow(constraintName, constraintVariableCounts.get(constraintName), constraintCounts.get(constraintName), constraintTermCounts.get(constraintName), constraintObjectiveTermCounts.get(constraintName));
			}

			// Add totals row
			statsTable.addDelimiter();
			statsTable.addRow("Total", mipModel.getVars().length, mipModel.getConstrs().length, constraintTermsTotal, objectives.size());

			// Print stats table
			System.out.println(statsTable);

			// Enough of the fancy stats stuff. Let's finalize our model by setting up various advanced settings
			mipModel.setCallback(callbackCoordinator);
			mipModel.getEnv().set(GRB.IntParam.Presolve, 2);													// 2 = Extra presolve (seems to give a significant boost to solve times)
			mipModel.getEnv().set(GRB.IntParam.Seed, new Random().nextInt(Integer.MAX_VALUE));					// Randomize the seed for each run to avoid repeating uncommonly fast/slow solves
			mipModel.getEnv().set(GRB.IntParam.Threads, Runtime.getRuntime().availableProcessors() - 1);		// All but one core
			mipModel.getEnv().set(GRB.IntParam.LogToConsole, (logSolverOutput ? 1 : 0));

			// Now do the magic
			System.out.println("--- Solving started ---");
			mipModel.optimize();
			System.out.println("--- Solving finished ---");
			solved = true;

			// Handle result
			int status = mipModel.get(GRB.IntAttr.Status);

			if (status == GRB.Status.INFEASIBLE)
			{
				// Find violating constraint(s)
				System.out.println("Problem is unsolvable. Computing conflicting constraints...");
				mipModel.computeIIS();
				System.out.println("\nThe following constraints conflict with each other:");

				for (GRBConstr constraint : mipModel.getConstrs())
				{
					if (constraint.get(GRB.IntAttr.IISConstr) == 1)
					{
						System.out.println(" * " + constraint.get(GRB.StringAttr.ConstrName));
					}
				}
				
				return false;
			}
			else if (status == GRB.Status.OPTIMAL || status == GRB.Status.INTERRUPTED)
			{
				double penaltyTotal = 0;
				double bonusTotal = 0;
				
				for (Triplet<Double, GRBVar, String> objective : objectives)
				{
					double objectiveResult = objective.getValue0() * varValue(objective.getValue1());
					
					if (objectiveResult >= DOUBLE_ZERO_THRESHOLD)
					{
						penaltyTotal += objectiveResult;
						System.out.println("Penalty: " + objective.getValue2() + ". Cost: " + OBJECTIVE_VALUE_FORMAT.format(objectiveResult));
					}
					else if (objectiveResult <= -DOUBLE_ZERO_THRESHOLD)
					{
						bonusTotal += -objectiveResult;
					}
				}

				// Print results table
				AsciiTable resultsTable = new AsciiTable(true, false, false, false, false, false);
				resultsTable.addRow("Problem", "Solve time", "Stability bonus", "Penalties", "Total quality", "Dist. to optimal");
				resultsTable.addDelimiter();
				resultsTable.addRow
				(
					model.modelName,
					Tools.timeString((long) Math.ceil(mipModel.get(GRB.DoubleAttr.Runtime))),
					OBJECTIVE_VALUE_FORMAT.format(bonusTotal),
					OBJECTIVE_VALUE_FORMAT.format(-penaltyTotal),
					OBJECTIVE_VALUE_FORMAT.format(-mipModel.get(GRB.DoubleAttr.ObjVal)),
					((status == GRB.Status.OPTIMAL) ? "" : "~") + (int) Math.round(mipModel.get(GRB.DoubleAttr.ObjVal) - mipModel.get(GRB.DoubleAttr.ObjBound))
				);
				System.out.println(resultsTable);
				
				return true;
			}
			else
			{
				throw new IllegalStateException("Unknown Gurobi result status: " + status);
			}
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public GRBVar addBinaryVar(String name)
	{
		return addVar(0, 1, GRB.BINARY, name);
	}
	
	@Override
	public GRBVar addIntegerVar(double minValue, Double maxValue, String name)
	{
		return addVar(minValue, (maxValue == null ? GRB.INFINITY : maxValue), GRB.INTEGER, name);
	}
	
	@Override
	public GRBVar addLinearVar(double minValue, Double maxValue, String name)
	{
		return addVar(minValue, (maxValue == null ? GRB.INFINITY : maxValue), GRB.CONTINUOUS, name);
	}
	
	private GRBVar addVar(double minValue, double maxValue, char type, String name)
	{
		try
		{
			return mipModel.addVar(minValue, maxValue, 0, type, name);
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	// Method for returning the assignment variable for a session corresponding to it being scheduled to start at a specific time
	@Override
	public GRBVar startVar(Day day, Slot slot, Session session)
	{
		if (!model.sessionFits(session, slot))
		{
			throw new IllegalArgumentException("Session " + session + " does not fit into slot " + slot);
		}

		return assignVars[model.indexOf(day)][model.indexOf(session)][model.indexOf(slot)];
	}
	
	// Method for retuning all assignment variables whose scheduling covers a specific time
	@Override
	public GRBVar[] assignVars(Day day, Slot slot, Session session)
	{
		List<GRBVar> vars = new LinkedList<>();
		
		for (Day currentDay : (day == null ? model.days() : Arrays.asList(day)))
		{
			for (Session currentSession : (session == null ? model.sessions() : Arrays.asList(session)))
			{
				// Find the relevant slots
				int slotStartIndex = 0;
				int slotEndIndex = model.slots().size() - currentSession.length;

				if (slot != null)
				{
					slotStartIndex = Math.max(slotStartIndex, model.indexOf(slot) - currentSession.length + 1);
					slotEndIndex = Math.min(slotEndIndex, model.indexOf(slot));
				}

				for (Slot currentSlot : model.slots().subList(slotStartIndex, slotEndIndex + 1))
				{
					vars.add(assignVars[model.indexOf(currentDay)][model.indexOf(currentSession)][model.indexOf(currentSlot)]);
				}
			}
		}
		
		return vars.toArray(new GRBVar[vars.size()]);
	}
	
	public boolean isScheduledAt(Day day, Slot slot, Session session)
	{
		return varValue(startVar(day, slot, session)) == 1;
	}
	
	@Override
	public boolean isScheduledDuring(Day day, Slot slot, Session session)
	{
		for (GRBVar var : assignVars(day, slot, session))
		{
			if (varValue(var) == 1)
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public int varValue(GRBVar var)
	{
		// The rounding used in this function is very important, as the solver might return non-integral values very close to the integers they represent
		if (!solved)
		{
			return (int) Math.round(callbackCoordinator.getSolution(var));
		}
		else
		{
			try
			{
				return (int) Math.round(var.get(GRB.DoubleAttr.X));
			}
			catch(GRBException ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}
	
	@Override
	public GRBConstr addEqualsConstr(GRBLinExpr lhs, double rhs, String name)
	{
		return addConstr(lhs, GRB.EQUAL, rhs, name);
	}
	
	@Override
	public GRBConstr addLessOrEqualsConstr(GRBLinExpr lhs, double rhs, String name)
	{
		return addConstr(lhs, GRB.LESS_EQUAL, rhs, name);
	}
	
	private GRBConstr addConstr(GRBLinExpr expr, char type, double rhs, String name)
	{
		try
		{
			GRBConstr constraint = mipModel.addConstr(expr, type, rhs, name);
			return constraint;
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public GRBConstr addEqualsConstr(GRBLinExpr lhs, GRBLinExpr rhs, String name)
	{
		return addConstr(lhs, GRB.EQUAL, rhs, name);
	}
	
	@Override
	public GRBConstr addLessOrEqualsConstr(GRBLinExpr lhs, GRBLinExpr rhs, String name)
	{
		return addConstr(lhs, GRB.LESS_EQUAL, rhs, name);
	}
	
	private GRBConstr addConstr(GRBLinExpr lhs, char type, GRBLinExpr rhs, String name)
	{
		try
		{
			GRBConstr constraint = mipModel.addConstr(lhs, type, rhs, name);
			return constraint;
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void addObjective(double weight, GRBVar var, String name)
	{
		objectives.add(new Triplet<>(weight, var, name));
	}
	
	public Map<Session, Pair<Day, Slot>> schedulingMap()
	{
		Map<Session, Pair<Day, Slot>> schedulingMap = new HashMap<>();
		
		for (Session session : model.sessions())
		{
			for (Day day : model.days())
			{
				for (Slot slot : model.slots())
				{
					if (model.sessionFits(session, slot) && isScheduledAt(day, slot, session))
					{
						schedulingMap.put(session, new Pair<>(day, slot));
					}
				}
			}
		}
		
		return schedulingMap;
	}
	
	@Override
	public void close()
	{
		try
		{
			GRBEnv grbEnv = mipModel.getEnv();
			mipModel.dispose();
			grbEnv.dispose();
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}