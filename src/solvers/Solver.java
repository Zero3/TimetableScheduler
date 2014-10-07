package solvers;

import constraints.Constraint;
import gurobi.GRBConstr;
import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import model.Day;
import model.Session;
import model.Slot;

// TODO: Refactor out Gurobi-specific things by introducing generic GRBVar, gRBLinExpr and GRBConstr classes. This would allow one to change the solver used easily.
public interface Solver
{
	// Methods used by the problem when setting up the model
	void addConstraint(Constraint constraint);
	
	boolean solve(boolean logSolverOutput);
	
	boolean isScheduledDuring(Day day, Slot slot, Session session);
	int varValue(GRBVar var);
	
	// Methods used by constraints when called upon to update the model
	GRBVar startVar(Day day, Slot slot, Session session);		// Method for returning the assignment variable for a session corresponding to it being scheduled to start at a specific time
	GRBVar[] assignVars(Day day, Slot slot, Session session);	// Method for retuning all assignment variables whose scheduling covers a specific time
	
	GRBVar addLinearVar(double minValue, Double maxValue, String name);
	GRBVar addIntegerVar(double minValue, Double maxValue, String name);
	GRBVar addBinaryVar(String name);

	GRBConstr addEqualsConstr(GRBLinExpr lhs, double rhs, String name);
	GRBConstr addLessOrEqualsConstr(GRBLinExpr lhs, double rhs, String name);
	GRBConstr addEqualsConstr(GRBLinExpr lhs, GRBLinExpr rhs, String name);
	GRBConstr addLessOrEqualsConstr(GRBLinExpr lhs, GRBLinExpr rhs, String name);
	
	void addObjective(double weight, GRBVar var, String name);
}
