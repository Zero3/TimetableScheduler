package exporters;

import model.Model;
import solvers.Solver;

public interface Exporter
{
	void export(Model model, Solver solver, boolean finalExport);
}
