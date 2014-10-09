TimetableScheduler
==================

TimetableScheduler is a an individual study activity I did during my computer science master's degree. It solves the university timetabling problem at Faculty of Science at University of Southern Denmark using [integer programming](https://en.wikipedia.org/wiki/Integer_programming). This project is currently neither active nor maintained, but merely available here for its conceptual value and any curious eyes.

Running TimetableScheduler requires the [Gurobi Optimizer](http://www.gurobi.com/) to be installed.

Individual study activity description
-----
The university timetabling problem considers how to schedule university courses in such a way that various
constraints with regard to students, teachers, time and other factors are respected. The Faculty of Science
at University of Southern Denmark currently assembles these timetables manually, only assisted by tools
to visualize the process and detect conflicts in the proposed timetables. This approach is inadequate with
regard to timetable quality and time investment. The goal of this individual study activity is to generate
timetables with mathematical guarantees with regard to the specified goals by developing an automatic system
capable of transforming the concrete problem to an instance of an integer programming problem and solving
it. The automatic system should take input in a human readable data format and produce output in a human
readable output format, allowing usage and integration by users with little to no knowledge of computational
optimization.

More concretely, in this activity we will:

* Formalize the timetabling problem at the Faculty of Science
* Model the problem as an integer linear programming problem
* Develop an automatic system capable of solving the model
* Collect, organize and import test data from the faculty
* Produce test results using the automatic system with the test data
* Discuss the test results
