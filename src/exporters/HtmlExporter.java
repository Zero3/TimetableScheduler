package exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;
import model.Course;
import model.Day;
import model.Session;
import model.Slot;
import model.Model;
import model.Person;
import org.javatuples.Pair;
import solvers.Solver;
import util.Tools;

public class HtmlExporter implements Exporter
{
	private static final String FILENAME_SANITIZER = "[^a-zA-Z0-9æøåÆØÅ\\-_]+";
	private static final int NONFINAL_SCHEDULE_REFRESH_INTERVAL = 2;
	
	private final String dataFolder;
	private final String outputFolder;
	private boolean outputFolderCleaned = false;

	public HtmlExporter(String dataFolder, String outputFolder)
	{
		this.dataFolder = dataFolder;
		this.outputFolder = outputFolder;
	}
	
	@Override
	public void export(Model model, Solver solver, boolean finalExport)
	{
		// Delete old output and create new empty output dir
		if (!outputFolderCleaned)
		{
			File folder = new File(outputFolder + "/");

			if (folder.exists())
			{
				// Make a small safety check before deleting the directory. This is by no means comprehensive, but should be better than nothing...
				if (!deleteSafetyCheck(folder))
				{
					throw new IllegalStateException("Safety check failed when trying to clean up export directory '" + folder + "': Directory contains files not ending in .htm or .css");
				}
				
				try
				{
					Tools.deleteRecursive(folder, false);
				}
				catch (FileNotFoundException ex)
				{
					throw new RuntimeException(ex);
				}
			}
			else if (!folder.mkdir())
			{
				throw new AccessControlException("Could not create export directory " + folder);
			}
			
			try
			{
				Files.copy(new File(dataFolder + "/style.css").toPath(), new File(outputFolder + "/style.css").toPath());
			}
			catch (IOException ex)
			{
				throw new AccessControlException("Could not copy style.css from data folder '" + dataFolder + "' to output folder '" + outputFolder + "'");
			}
			
			outputFolderCleaned = true;
		}
		
		// Create folder for this model
		String sanitizedModelName = model.modelName.replaceAll(FILENAME_SANITIZER, "");
		File modelFolder = new File(outputFolder + "/" + sanitizedModelName + "/");
		
		if (!modelFolder.exists() && !modelFolder.mkdir())
		{
			throw new AccessControlException("Could not create export directory " + modelFolder);
		}

		// Generate timetable for each student
		for (Person person : model.persons())
		{
			Map<Pair<Day, Slot>, String> tdMap = new HashMap<>();
			
			for (Day day : model.days())
			{
				for (Slot slot : model.slots())
				{
					StringBuilder content = new StringBuilder();
					boolean overlap = false;
					
					for (Session session : person.sessions())
					{
						buildSessionString(content, model, solver, day, slot, session, person);
					}
					
					String td = "<td" + (overlap ? " class=\"overlap\"" : "") + ">" + content + "</td>";
					tdMap.put(new Pair<>(day, slot), td);
				}
			}
			
			// Now print the timetable
			String sanitizedPersonName = person.name.replaceAll(FILENAME_SANITIZER, "");
			File file = new File(modelFolder + "/" + sanitizedModelName + "_Person_" + sanitizedPersonName + ".htm");
			writeTimetable(model, file, person.name, tdMap, finalExport);
		}
		
		// Generate timetable for each course
		for (Course course : model.courses())
		{
			Map<Pair<Day, Slot>, String> tdMap = new HashMap<>();
			
			for (Day day : model.days())
			{
				for (Slot slot : model.slots())
				{
					StringBuilder content = new StringBuilder();
					
					for (Session session : course.sessions(null))
					{
						buildSessionString(content, model, solver, day, slot, session, null);
					}
					
					String td = "<td>" + content + "</td>";
					tdMap.put(new Pair<>(day, slot), td);
				}
			}
			
			// Now print the timetable
			String sanitizedCourseName = course.name.replaceAll(FILENAME_SANITIZER, "");
			File file = new File(modelFolder + "/" + sanitizedModelName + "_Course_" + sanitizedCourseName + ".htm");
			writeTimetable(model, file, course.name, tdMap, finalExport);
		}
	}
	
	private void buildSessionString(StringBuilder content, Model model, Solver solver, Day day, Slot slot, Session session, Person person)
	{
		if (solver.isScheduledDuring(day, slot, session))
		{
			if (model.previous(slot) != null && solver.isScheduledDuring(day, model.previous(slot), session))
			{
				content.insert(0, "<div class=\"continue\">&#8226;<br>&#8226;<br>&#8226;</div>");
			}
			else
			{
				content.append("<div>");

				content.append(session.course.toString());
				content.append(" ").append(session.groups);

				if (session.type != null)
				{
					content.append(" (").append(session.type).append(")");
				}

				if (person != null && session.staffs.contains(person))
				{
					content.append(" [STAFF]");
				}

				if (session.title != null)
				{
					content.append("<br>");
					content.append(session.title);
				}	

				if (session.room != null)
				{
					content.append("<br>");
					content.append(session.room.toString());
				}

				content.append("</div>");
			}
		}
	}

	private void writeTimetable(Model model, File file, String timetableName, Map<Pair<Day, Slot>, String> tdMap, boolean finalExport)
	{
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file)))
		{
			out.write("<html>");
				out.write("<head>");
					out.write("<meta charset=\"UTF-8\">");
					out.write("<title>");
						out.write("Schedule for person " + timetableName.replaceAll(FILENAME_SANITIZER, ""));
					out.write("</title>");
					if (!finalExport)
					{
						out.write("<meta http-equiv=\"refresh\" content=\"" + NONFINAL_SCHEDULE_REFRESH_INTERVAL + "\">");
					}
					out.write("<link rel=\"stylesheet\" href=\"../style.css\">");
				out.write("</head>");
				out.write("<body>");
					out.write("<table>");
						out.write("<tr>");
							out.write("<td class=\"personheader\">" + model.modelName.replaceAll(FILENAME_SANITIZER, "") + "<br>Timetable for:<br>" + timetableName + "</td>");
							for (Day day : model.days())
							{
								out.write("<td class=\"day\">" + day + "</td>");
							}
						out.write("</tr>");
						for (Slot slot : model.slots())
						{
							out.write("<tr>");
								out.write("<td class=\"time\">" + slot + "</td>");
								for (Day day : model.days())
								{
									out.write(tdMap.get(new Pair<>(day, slot)));
								}
							out.write("</tr>");
						}
					out.write("</table>");
				out.write("</body>");
			out.write("</html>");
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	private boolean deleteSafetyCheck(File folder)
	{
		for (File file : folder.listFiles())
		{
			if (file.isDirectory())
			{
				if (!deleteSafetyCheck(file))
				{
					return false;
				}
			}
			else if (!file.getName().endsWith(".htm") && !file.getName().endsWith(".css"))
			{
				return false;
			}
		}
		
		return true;
	}
}