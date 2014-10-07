package callbacks.gurobi;

import gurobi.GRB;
import solvers.GurobiSolver;

public class GapLogger implements GurobiCallback
{
	private final int reportInterval;
	private double lastReportTime = 0;
	private int bestDist = Integer.MAX_VALUE;

	public GapLogger(int reportInterval)
	{
		super();
		
		this.reportInterval = reportInterval;
	}
	
	@Override
	public void callback(GurobiSolver solver, GurobiCallbackCoordinator callbackCoordinator)
	{
		if (callbackCoordinator.getWhere() == GRB.Callback.MIP)
		{
			double currentTime = callbackCoordinator.getDoubleInfo(GRB.Callback.RUNTIME);

			if ((lastReportTime + reportInterval) < currentTime)
			{
				int currentResult = (int) Math.ceil(callbackCoordinator.getDoubleInfo(GRB.Callback.MIP_OBJBST));	// Ceil for pessimism
				int currentBound = (int) Math.floor(callbackCoordinator.getDoubleInfo(GRB.Callback.MIP_OBJBND));	// Floor for pessimism
				int distToOpt = currentResult - currentBound;
				
				if (distToOpt < bestDist)
				{
					System.out.println("Status: Current solution is no more than " + distToOpt + " points from optimality (" + Math.round(currentTime) + "s used)");
					bestDist = distToOpt;
				}
				
				lastReportTime = currentTime;
			}
		}
	}
}