package constraints;

import model.Model;
import solvers.GurobiSolver;
import solvers.Solver;

public abstract class Constraint
{
	protected final Solver solver;
	protected final Model model;
	
	protected Constraint(GurobiSolver solver, Model model)
	{
		this.solver = solver;
		this.model = model;
	}
 
	public void addVariables()
	{
		// Default: Do nothing
	}

	public void addConstraints()
	{
		// Default: Do nothing
	}

	public void addObjectives()
	{
		// Default: Do nothing
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}
}