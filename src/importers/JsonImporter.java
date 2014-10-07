package importers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import model.Course;
import model.Day;
import model.Room;
import model.Session;
import model.Slot;
import model.Model;
import org.javatuples.Pair;

public class JsonImporter implements Importer
{
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	static
	{
		jsonMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
	}
	
	private final String filePath;

	public JsonImporter(String filePath)
	{
		this.filePath = filePath;
	}

	@Override
	public int importProblem(Model model)
	{
		try
		{
			JsonNode jsonRoot = jsonMapper.readTree(new File(filePath));
			
			int importedSessions = importCourses(model, jsonRoot);
			importStudents(model, jsonRoot);
			
			return importedSessions;
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	private static int importCourses(Model model, JsonNode jsonRoot)
	{
		// Parse courses
		Path coursesPath = new Path("courses");
		int importedSessions = 0;

		for (Iterator<Map.Entry<String, JsonNode>> iter = assertObject(jsonRoot.get("courses"), coursesPath).fields(); iter.hasNext(); )
		{
			Map.Entry<String, JsonNode> courseEntry = iter.next();
			String courseName = courseEntry.getKey();
			Path coursePath = new Path(coursesPath, courseName);
			JsonNode courseSpecs = assertArray(courseEntry.getValue(), coursePath);

			for (int specIndex = 0; specIndex < courseSpecs.size(); specIndex++)
			{
				// For every scheduling line for this course
				Path specPath = new Path(coursePath, specIndex);
				JsonNode spec = assertObject(courseSpecs.get(specIndex), specPath);

				for (JsonNode weekSpec : listifyNodeContents(spec.get("weeks")))
				{
					// Loop over each week and only continue parsing the specification for the week for which we are scheduling (if it exists at all)
					if (assertInteger(weekSpec, new Path(specPath, "weeks")) != model.week)
					{
						continue;
					}

					if (spec.has("shared") && assertBoolean(spec.get("shared"), new Path(specPath, "shared")))
					{
						// Create a shared session
						Set<String> groupStrings = new HashSet<>();
						
						for (JsonNode group : listifyNodeContents(spec.get("groups")))
						{
							groupStrings.add(assertString(group, new Path(specPath, "groups")));
						}
						
						Session session = parseSession(model, spec, specPath, model.findAddCourse(courseName), groupStrings);
						importedSessions++;
						
						// Assign staffs to session (if any)
						if (spec.has("staff"))
						{
							List<JsonNode> staffs = listifyNodeContents(spec.get("staff"));
							
							for (JsonNode staff : staffs)
							{
								session.addStaff(model.findAddPerson(assertString(staff, new Path(specPath, "staff"))));
							}
						}
					}
					else
					{
						// Create separate session for every group
						List<JsonNode> groups = listifyNodeContents(spec.get("groups"));
						
						for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++)
						{
							Session session = parseSession(model, spec, specPath, model.findAddCourse(courseName), assertString(groups.get(groupIndex), new Path(specPath, "groups")));
							importedSessions++;
							
							// Assign staffs to session (if any)
							if (spec.has("staff"))
							{
								List<JsonNode> staffs = listifyNodeContents(spec.get("staff"));

								if (staffs.size() == 1)
								{
									session.addStaff(model.findAddPerson(assertString(staffs.get(0), new Path(specPath, "staff"))));
								}
								else
								{
									if (staffs.size() != groups.size())
									{
										throw new IllegalStateException("Error while parsing input file: Staff list at " + new Path(specPath, "staff") + " must be one of the following: (1) Non-existing (2) A single staff to be used for all groups (3) A list of a staff, one staff per group (4) A list of lists of staffs, one list of staff per group");
									}

									for (JsonNode staff : listifyNodeContents(staffs.get(groupIndex)))
									{
										session.addStaff(model.findAddPerson(assertString(staff, new Path(specPath, "staff"))));
									}
								}
							}
						}
					}
				}
			}
		}
		
		return importedSessions;
	}
	
	private static Session parseSession(Model model, JsonNode spec, Path specPath, Course course, String group)
	{
		return parseSession(model, spec, specPath, course, new HashSet<>(Arrays.asList(group)));
	}
		
	private static Session parseSession(Model model, JsonNode spec, Path specPath, Course course, Set<String> groups)
	{
		// Fetch session details
		String roomString = assertStringOrNull(spec.get("room"), new Path(specPath, "room"));
		Room room = (roomString == null ? null : model.findRoom(roomString));
		
		String type = assertStringOrNull(spec.get("type"), new Path(specPath, "type"));
		String title = assertStringOrNull(spec.get("title"), new Path(specPath, "title"));
		int length = assertInteger(spec.get("length"), new Path(specPath, "length"));
		
		// Create session
		int roomBreak = !spec.has("roombreak") ? 0 : assertInteger(spec.get("roombreak"), new Path(specPath, "roombreak"));
		Session session = model.addSession(course, type, title, groups, length, room, roomBreak);

		// Handle forced times (whitelisting)
		if (spec.has("times"))
		{
			addSessionTimes(model, session, true, spec.get("times"), new Path(specPath, "times"));
		}

		// Handle denied times (blacklisting)
		if (spec.has("denytimes"))
		{
			addSessionTimes(model, session, false, spec.get("denytimes"), new Path(specPath, "denytimes"));
		}
		
		return session;
	}
	
	private static void addSessionTimes(Model model, Session session, boolean whiteListing, JsonNode timeSpecs, Path timeSpecPath)
	{
		for (JsonNode timeSpec : listifyNodeContents(timeSpecs))
		{
			assertObject(timeSpec, timeSpecPath);

			// Combine all specified days and hours
			for (JsonNode daySpec : listifyNodeContents(timeSpec.get("days")))
			{
				for (JsonNode hourSpec : listifyNodeContents(timeSpec.get("hours")))
				{
					Day day = model.findDay(assertStringOrNull(daySpec, new Path(timeSpecPath, "days")));
					Slot slot = model.findSlot(assertIntegerOrNull(hourSpec, new Path(timeSpecPath, "hours")));
					
					if (whiteListing)
					{
						session.whitelistTime(new Pair<>(day, slot));
					}
					else
					{
						session.blacklistTime(new Pair<>(day, slot));
					}
				}
			}
		}
	}
	
	private static void importStudents(Model model, JsonNode jsonRoot)
	{
		// Parse students
		Path studentsPath = new Path("students");

		for (Iterator<Map.Entry<String, JsonNode>> iter = assertObject(jsonRoot.get("students"), studentsPath).fields(); iter.hasNext(); )
		{
			Map.Entry<String, JsonNode> studentEntry = iter.next();
			String studentName = studentEntry.getKey();
			Path studentPath = new Path(studentsPath, studentName);
			
			// Parse attendance specs
			Path attendancePath = new Path(studentPath, "attendance");
			JsonNode studentAttendance = assertArray(studentEntry.getValue().get("attendance"), attendancePath);
			
			for (int specIndex = 0; specIndex < studentAttendance.size(); specIndex++)
			{
				// For every course enrollment specified for this student
				Path specPath = new Path(attendancePath, specIndex);
				JsonNode spec = assertObject(studentAttendance.get(specIndex), specPath);
				String courseName = assertString(spec.get("course"), new Path(specPath, "course"));
				
				if (!jsonRoot.get("courses").has(courseName))
				{
					throw new IllegalStateException("Error in input file: Could not find specifications for course '" + spec.get("course").textValue() + "' referenced at " + new Path(specPath, "course"));
				}
				
				// If the course doesn't exist in this model (read: has no sessions scheduled for this week), we just ignore the spec
				Course course =  model.findCourse(courseName);
				
				if (course != null)
				{
					// Add student to sessions for the groups to which he is enrolled
					for (JsonNode group : listifyNodeContents(spec.get("groups")))
					{
						for (Session session :course.sessions(assertString(group, new Path(specPath, "groups"))))
						{
							session.addStudent(model.findAddPerson(studentName));
						}
					}
				}
			}
			
			// Parse student weight (if any)
			if (studentEntry.getValue().has("weight"))
			{
				model.findAddPerson(studentName).weight = assertDouble(studentEntry.getValue().get("weight"), new Path(studentPath, "weight"));
			}
		}
		
		// Double-check that all sessions have students enrolled to them
		for (Session session : model.sessions())
		{
			if (session.students.isEmpty())
			{
				throw new IllegalStateException("Error in input file: No students are registered for session " + session);
			}
		}
	}
	
	private static List<JsonNode> listifyNodeContents(JsonNode node)
	{
		List<JsonNode> list = new LinkedList<>();

		if (node == null || node.isValueNode() || node.isObject())
		{
			list.add(node);	// We treat both null, values and objects as "a single item"
		}
		else if (node.isArray())
		{
			for (JsonNode arrayEntry : node)
			{
				list.add(arrayEntry);
			}
		}
		
		return list;
	}
	
	private static void assertType(JsonNode node, Path path, JsonNodeType type)
	{
		if (node == null)
		{
			throw new IllegalStateException("Error while parsing input file: The field '" + path + "' does not exist as expected.");
		}
		if (!node.getNodeType().equals(type))
		{
			throw new IllegalStateException("Error while parsing input file: The field (or element of) '" + path + "' is of type " + node.getNodeType() + " unlike " + type + " as expected.");
		}
	}
	
	private static void assertNonEmpty(JsonNode node, Path path)
	{
		if (node.size() == 0)
		{
			throw new IllegalStateException("Error while parsing input file: The field '" + path + "' is empty.");
		}
	}
	
	private static boolean assertBoolean(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.BOOLEAN);
		return node.asBoolean();
	}
	
	private static int assertInteger(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.NUMBER);
		
		if (!node.isInt())
		{
			throw new IllegalStateException("Error while parsing input file: The field (or element of) '" + path + "' is of type " + node.getNodeType() + " but not an integer as expected.");
		}
		
		return node.intValue();
	}
	
	private static double assertDouble(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.NUMBER);
		return node.doubleValue();
	}
	
	private static Integer assertIntegerOrNull(JsonNode node, Path path)
	{
		if (node != null)
		{
			assertType(node, path, JsonNodeType.NUMBER);
			return node.intValue();
		}
		
		return null;
	}

	private static String assertString(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.STRING);
		return node.textValue();
	}
	
	private static String assertStringOrNull(JsonNode node, Path path)
	{
		if (node != null)
		{
			assertType(node, path, JsonNodeType.STRING);
			return node.textValue();
		}
		
		return null;
	}

	private static JsonNode assertArray(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.ARRAY);
		assertNonEmpty(node, path);
		return node;
	}
	
	private static JsonNode assertObject(JsonNode node, Path path)
	{
		assertType(node, path, JsonNodeType.OBJECT);
		assertNonEmpty(node, path);
		return node;
	}
	
	private static class Path
	{
		private final List<String> elements = new LinkedList<>();
		
		public Path(String element)
		{
			elements.add(element);
		}
		
		public Path(Path oldPath, String element)
		{
			elements.addAll(oldPath.elements);
			elements.add(element);
		}
		
		public Path(Path oldPath, int elementIndex)
		{
			elements.addAll(oldPath.elements);
			elements.add("[" + elementIndex + "]");
		}

		@Override
		public String toString()
		{
			StringBuilder path = new StringBuilder(elements.get(0));

			for (int i = 1; i < elements.size(); i++)
			{
				path.append("->").append(elements.get(i));
			}

			return path.toString();
		}
	}
}