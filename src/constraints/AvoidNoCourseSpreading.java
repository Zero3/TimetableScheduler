package constraints;

import gurobi.GRBLinExpr;
import gurobi.GRBVar;
import java.util.List;
import model.Day;
import model.Session;
import model.Model;
import model.Person;
import org.javatuples.Triplet;
import solvers.GurobiSolver;
import util.GeputHashMap;
import util.Tools;

// NOTE: This constraint only affects the case in which the number of sessions for a given user for a given course is <= DESIRED_WEEK_LENGTH.
// To support the case with more sessions, another constraint to limit the number of daily sessions for the same course could be implemented.
public class AvoidNoCourseSpreading extends Constraint
{
	private static final int DESIRED_WEEK_LENGTH = 5;	// Base break calculations on a desired week this long
	
	private GRBVar[] spreadPenalties;
	private final int maxPenalty;

	public AvoidNoCourseSpreading(GurobiSolver solver, Model model, int maxPenalty)
	{
		super(solver, model);
		
		this.maxPenalty = maxPenalty;
	}

	@Override
	public void addVariables()
	{
		// Allocate session penalty variables for each session for each day. These variables are to be minimized in the objective and represent the active penalties for scheduling sessions separated by too short breaks
		spreadPenalties = new GRBVar[model.sessions().size()];
		
		for (Session session : model.sessions())
		{
			spreadPenalties[model.indexOf(session)] = solver.addLinearVar(0, null, Tools.nameConcat(this, session));
		}
	}

	@Override
	public void addConstraints()
	{
		// Create a session penalty map representing the penalty for scheduling specific sessions near each other
		GeputHashMap<Triplet<Session, Session, Integer>, Double> penaltyMap = new GeputHashMap<>();	// <session 1>, <session 2>, <break length in days>, <penalty>
		
		for (Person person : model.persons())
		{
			for (List<Session> sessionList : person.studentSessionsByCourse())
			{
				if (sessionList.size() == 1 || sessionList.size() > DESIRED_WEEK_LENGTH)
				{
					continue;
				}
				
				// Determine desired length of breaks between sessions (in days).
				// Gives the following breaks for a standard 5-day week (keep in mind that 0 day long breaks are not useless because several sessions during the same day always will be taken into account)
				// 5 sessions: 4 breaks of 0 days
				// 4 sessions: 3 breaks of 0 days
				// 3 sessions: 2 breaks of 1 day
				// 2 sessions: 1 break of 2 days
				int breakDays = Math.max(0, DESIRED_WEEK_LENGTH - sessionList.size());
				int breaks = sessionList.size() - 1;
				int breakLength = Math.min(2, breakDays / breaks);									// It makes little sense to schedule more than 2 days break on a 7-day week, because any weekend break is only 2 days anyway
				
				// Distribute penalty linearly over the breaks
				double penaltyPerDay = (double) (person.weight * maxPenalty) / (breakLength + 1);		// + 1 to include the day a session is scheduled as well
				
				// We then loop over every combination (not permutation) of two sessions for this course
				for (int i = 0; i < sessionList.size() - 1; i++)
				{
					for (int j = i + 1; j < sessionList.size(); j++)
					{
						for (int b = 0; b <= breakLength; b++)
						{
							Triplet<Session, Session, Integer> mapKey = new Triplet<>(sessionList.get(i), sessionList.get(j), b);
							double penalty = penaltyMap.geput(mapKey, 0D);
							penalty += penaltyPerDay * ((breakLength + 1) - b);
							penaltyMap.put(mapKey, penalty);
						}
					}
				}
			}
		}

		// Setup penalty trigger constraints for each session and each day. The variable corresponding to the day a session is scheduled will receive a huge "trigger value",
		// causing the penalties added to the contraint to overflow into the penalty variables. We set up this relationship later.
		for (Day day : model.days())
		{
			for (Session session : model.sessions())
			{
				// Setup a penalty trigger expression. It will contain the penalties for scheduling other sessions in the days following this one.
				// This is implemented (below) by multiplying penalties with corresponding assignment variables. In order to assure that the penalties only
				// are active when the session is scheduled at this day, a large trigger factor is added to the assignment variables of the session for this day
				GRBLinExpr penaltyTrigger = new GRBLinExpr();
				
				// Setup penalties
				Double totalPenalty = 0D;
				
				for (Session otherSession : model.sessions())
				{
					for (Day otherDay : model.days())
					{
						Double penalty = penaltyMap.get(new Triplet<>(session, otherSession, model.indexOf(otherDay) - model.indexOf(day)));
						
						if (penalty != null)		// If there is no penalty for this combination then skip it
						{
							totalPenalty += penalty;
							
							// Add trigger weight
							for (GRBVar var : solver.assignVars(day, null, session))
							{
								penaltyTrigger.addTerm(penalty, var);
							}
							
							// Add penalty
							for (GRBVar var : solver.assignVars(otherDay, null, otherSession))
							{
								penaltyTrigger.addTerm(penalty, var);
							}
						}
					}
				}

				if (totalPenalty > 0)				// Don't add constraint if there are no penalties anyway
				{
					// We now setup the constraint such that penalties are "pushed" from the penalty expression to the corresponding penalty variable when
					// session are scheduled with noo little spreading
					GRBLinExpr rhs = new GRBLinExpr();
					rhs.addConstant(totalPenalty);
					rhs.addTerm(1, spreadPenalties[model.indexOf(session)]);

					solver.addLessOrEqualsConstr(penaltyTrigger, rhs, Tools.nameConcat(this, session.course, session, day));
				}
			}
		}
	}

	@Override
	public void addObjectives()
	{
		for (Session session : model.sessions())
		{
			solver.addObjective(1, spreadPenalties[model.indexOf(session)], Tools.nameConcat(this, session));
		}
	}
}