package callbacks.gurobi;

import gurobi.GRBCallback;
import gurobi.GRBException;
import gurobi.GRBVar;
import java.util.LinkedList;
import java.util.List;
import solvers.GurobiSolver;

// We wrap callbacks in an instance of this coordinator class because Gurobi only accepts a single callback
public class GurobiCallbackCoordinator extends GRBCallback
{
	public final GurobiSolver solver;
	public final List<GurobiCallback> callbacks = new LinkedList<>();

	public GurobiCallbackCoordinator(GurobiSolver solver)
	{
		this.solver = solver;
	}
	
	@Override
	protected void callback()
	{
		for (GurobiCallback callback : callbacks)
		{
			callback.callback(solver, this);
		}
	}
	
	protected int getWhere()
	{
		return where;
	}
	
	@Override
	protected double getDoubleInfo(int infoID)
	{
		try
		{
			return super.getDoubleInfo(infoID);
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	protected void abort()
	{
		super.abort();
	}

	@Override
	public double getSolution(GRBVar v)
	{
		try
		{
			return super.getSolution(v);
		}
		catch (GRBException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}