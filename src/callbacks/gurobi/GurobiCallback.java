package callbacks.gurobi;

import solvers.GurobiSolver;

public interface GurobiCallback
{
	void callback(GurobiSolver solver, GurobiCallbackCoordinator callbackCoordinator);
}