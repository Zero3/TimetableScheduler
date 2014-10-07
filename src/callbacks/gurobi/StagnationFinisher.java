package callbacks.gurobi;

import gurobi.GRB;
import solvers.GurobiSolver;

public class StagnationFinisher implements GurobiCallback
{
	private final int timeLimit;
	private double lastResultTime = 0;
	private Double lastResult = Double.MAX_VALUE;

	public StagnationFinisher(int secondsLimit)
	{
		super();
		
		timeLimit = secondsLimit;
	}
	
	@Override
	public void callback(GurobiSolver solver, GurobiCallbackCoordinator callbackCoordinator)
	{
		if (callbackCoordinator.getWhere() == GRB.Callback.MIP)
		{
			double currentResult = callbackCoordinator.getDoubleInfo(GRB.Callback.MIP_OBJBST);
			double currentTime = callbackCoordinator.getDoubleInfo(GRB.Callback.RUNTIME);

			if (currentResult < lastResult)
			{								
				lastResult = currentResult;
				lastResultTime = currentTime;
			}

			if ((lastResultTime + timeLimit) < currentTime)
			{
				System.out.println(getClass().getSimpleName() + ": No better solution found during the last " + timeLimit + " seconds. Stopping...");
				callbackCoordinator.abort();
			}
		}
	}
}