package callbacks.gurobi;

import exporters.Exporter;
import gurobi.GRB;
import model.Model;
import solvers.GurobiSolver;

public class IntermediateResultExporter implements GurobiCallback
{
	private static final int EXPORT_INTERVAL = 1000;
	private final Model model;
	private final Exporter exporter;
	private Long lastExport = null;
	
	public IntermediateResultExporter(Model model, Exporter exporter)
	{
		this.model = model;
		this.exporter = exporter;
	}
	
	@Override
	public void callback(GurobiSolver solver, GurobiCallbackCoordinator callbackCoordinator)
	{
		try
		{
			if (callbackCoordinator.getWhere() == GRB.Callback.MIPSOL)
			{
				if (lastExport == null || (System.currentTimeMillis() - lastExport > EXPORT_INTERVAL))
				{
					exporter.export(model, solver, false);
					lastExport = System.currentTimeMillis();
				}
			}
		}
		catch (RuntimeException ex)
		{
			ex.printStackTrace();	// Exceptions will be eaten raw and silently by Gurobi, so print them instead
		}
	}
}