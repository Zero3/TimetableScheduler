/*
	TimetableScheduler

	This program is part of the individual study activity "Solving a university
	timetabling problem by integer programming" by Christian Funder Sommerlund
	(zero3@zero3.dk / chsom09@student.sdu.dk)

	Description from ISA abstract:

	"The university timetabling problem considers how to schedule university
	courses in such a way that various constraints with regards to students,
	teachers, time and other factors are respected. The Faculty of Science at
	University of Southern Denmark currently assembles these timetables
	manually, only assisted by tools to visualize the process and detect
	conflicts in the proposed timetables. This approach is inadequate with
	regards to timetable quality and time investment. The goal of this
	individual study activity is to generate timetables with mathematical
	guarantees with regard to the specified goals by developing an automatic
	system capable of transforming the concrete problem to an instance of an
	integer programming problem and solving it. The automatic system should
	take input from a human readable data format and produce output in a human
	readable output format, allowing usage and integration by persons with little
	to no knowledge of computational optimization."
 */
package problems;

import callbacks.gurobi.GapLogger;
import callbacks.gurobi.IntermediateResultExporter;
import callbacks.gurobi.StagnationFinisher;
import constraints.AvoidNoCourseSpreading;
import constraints.AvoidNoLunchBreaks;
import constraints.AvoidPersonConflicts;
import constraints.AvoidTimetableInstability;
import constraints.AvoidUnalignedAllocation;
import constraints.AvoidUndesiredTimes;
import constraints.EnforceNoRoomConflicts;
import constraints.EnforceNoStaffConflicts;
import constraints.EnforceRoomBreaks;
import constraints.EnforceSessionTimeBlacklist;
import constraints.EnforceSessionTimeWhitelist;
import constraints.EnforceSessionsScheduled;
import exporters.Exporter;
import exporters.HtmlExporter;
import importers.Importer;
import importers.JsonImporter;
import java.util.Arrays;
import java.util.LinkedList;
import model.Day;
import model.Model;
import model.Slot;
import model.Solution;
import org.javatuples.Triplet;
import solvers.GurobiSolver;

// The following list of TODOs are basically a wishlist of nice-to-have things that could be implemented.
//  TODO: Consider changing some variables to be continous. Some solvers may run faster this way, others not.
//	TODO: Color code I/TE/TL in HTML output?
//	TODO: Support session dependencies like first TE session of first week should be before first I session. Marco wrote an example of such constraint for me.
//	TODO: Minimize breaks during a day. Could be done by either directly implementing a constraint to penalize these or indirectly by adapting the AvoidUndesiredTimes constraint to take number of hours to be scheduled into account (such that students with few hours have penalties for 9-10 and 15-16 for example)
//	TODO: Consider moving some of the options to the JSON input file
//	TODO: Blacklist specific times at specific days both global (campus events and such) and per student (classes might have other teaching at another faculty or such) (see EnforceDayBlacklisting.java)
//	TODO: Further room restrictions? Max. number of rooms of some sizes at the same time perhaps?
public class NatProblem
{
	private static final String DEFAULT_DATA_FOLDER = "data";
	private static final String DEFAULT_OUTPUT_FOLDER = "schedules";
	
	public static void main(String[] args)
	{
		System.out.println("TimetableScheduler 1.0 by Christian Funder Sommerlund (zero3@zero3.dk)");
		
		if (args.length < 1 || args.length > 3)
		{
			System.out.println("Usage: <input file> [output folder] [data folder]");
			System.out.println("Defaults: <none> '" + DEFAULT_OUTPUT_FOLDER + "' '" + DEFAULT_DATA_FOLDER + "'");
			return;
		}
		
		// Setup importers and exporters
		System.out.println("Importing timetabling problem from file '" + args[0] + "'");
		Importer importer = new JsonImporter(args[0]);
		
		String exportFolder = (args.length >= 2 ? args[1] : DEFAULT_OUTPUT_FOLDER);
		System.out.println("Exporting timetables to folder '" + exportFolder + "'");
		Exporter exporter = new HtmlExporter((args.length >= 3 ? args[2] : DEFAULT_DATA_FOLDER), exportFolder);
		
		LinkedList<Solution> schedules = new LinkedList<>();
		
		// Now schedule every week found in input file
		for (int week = 0; week <= 53; week++)
		{
			// Setup our timetabling model
			Model model = new Model(week, 8);
			
			model.addDays("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");		// Add days
			model.addSlots(10);																		// Add a number of time slots. They start from the hour specified to the constructor of Model and is 1 hour long each.
			model.addRooms("Lab 3 og 4", "Lab 5 og 6", "IMADAs terminalrum", "Fysik øvelseslab");	// Add special rooms. Everything else will be scheduled without a room.
			
			importer.importProblem(model);															// Import courses, sessions and students from input file
			
			// Check if we actually have anything to schedule this week. If not, skip it
			if (model.sessions().isEmpty())
			{
				continue;
			}
			
			// Solve using MIP solver
			try (GurobiSolver solver = new GurobiSolver(model))
			{
				// Setup hard constraints
				solver.addConstraint(new EnforceSessionsScheduled(solver, model));
				solver.addConstraint(new EnforceSessionTimeWhitelist(solver, model));
				solver.addConstraint(new EnforceSessionTimeBlacklist(solver, model));
				
				solver.addConstraint(new EnforceNoStaffConflicts(solver, model));
				solver.addConstraint(new EnforceNoRoomConflicts(solver, model));
				solver.addConstraint(new EnforceRoomBreaks(solver, model));

				// Setup soft constraints. Last constructor argument is usually the penalty per violation.
				solver.addConstraint(new AvoidPersonConflicts(solver, model, 32));
				solver.addConstraint(new AvoidNoCourseSpreading(solver, model, 4));
				solver.addConstraint(new AvoidUnalignedAllocation(solver, model, 16));
				solver.addConstraint(new AvoidTimetableInstability(solver, model, schedules, 1));
				solver.addConstraint(new AvoidNoLunchBreaks(solver, model, Arrays.asList(model.findSlot(11), model.findSlot(12), model.findSlot(13)), 2));

				// Setup soft time constraints. Specifying null as day or slot means any day or slot respectively. Last argument is the penalty.
				solver.addConstraint(new AvoidUndesiredTimes(solver, model, Arrays.asList
				(
					new Triplet<>((Day) null,					model.findSlot(8),	2),
					new Triplet<>((Day) null,					model.findSlot(16),	1),
					new Triplet<>((Day) null,					model.findSlot(17),	2),
					new Triplet<>(model.findDay("Friday"),		model.findSlot(14),	1),
					new Triplet<>(model.findDay("Friday"),		model.findSlot(15),	1),
					new Triplet<>(model.findDay("Friday"),		model.findSlot(16),	2),
					new Triplet<>(model.findDay("Friday"),		model.findSlot(17),	2),
					new Triplet<>(model.findDay("Saturday"),	(Slot) null,		8)
				)));

				// Setup callbacks
				//solver.addCallback(new IntermediateResultExporter(model, exporter));	// Export solutions as they are found during the solve (and not just the final one)
				solver.addCallback(new StagnationFinisher(60 * 5));						// Ends the solve early if no better solution is found for the specified amount of seconds
				solver.addCallback(new GapLogger(5));									// Log progress to finding optimal solution in a nice way. Preferred to raw solver output.

				// Go! Go! Go!
				if (solver.solve(false))					// The boolean argument is whether to print raw solver output
				{
					schedules.add(new Solution(model, solver.schedulingMap()));
					exporter.export(model, solver, true);	// Success! Export solution
				}
				else
				{
					break;
				}
			}
		}
	}
}