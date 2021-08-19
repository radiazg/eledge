package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.MessageFormat;

public class Exam extends HttpServlet {
  // parameters that determine the properties of the exam program:
  
  int nSubjectAreas = 1;               // default number of subject areas for exam overridden by values read from ExamInfo database
  int nQuestionsPerSubjectArea = 10;   // number of questions presented in each area also overridden in method printExam()
  int timeLimit = 60;                  // minutes; set to zero for no time limit to complete the exam
  int waitForNewDownload = 0;          // minutes; set to zero for unlimited rate of exam downloads
  boolean enforceDeadlines = true;     // true means that exam score is not recorded after the deadline
  boolean allowMultipleTries = false;  // false allows only one attempt at each exam; true is recommended
  boolean scrambleQuestions = true;    // false presents the same questions each attempt; true is recommended
  boolean allowWorkAhead = false;      // true makes every exam available; false makes available after the deadline for the previous exam expires. 
  //Hm. But, what about the case where there's only 1, final exam? Even if you 
  //don't allow work ahead, the "final" exam is available from day 1 (or, from
  //whatever day it's created. Hence, Let's make this a little more detailed.
  //We're going to allow teachers to define a "date available" day for tests. 
  //So, what about the behaviour of this field? If we have a date available, 
  //do we need a "allowWorkAhead" for? Allow workahead will toggle the presence/
  //absence of the "date available" fields. If it's false, there's a date 
  //available field, which will default to the deadline of the previous exam,
  //if one exists, to mimic the previous behaviour. If no previous exam exists,
  //the date available will default to the same date as the current test deadline.
  //oi. this is gonna take some work. ;) In fact, it's going to suck. =/
  boolean showMissedQuestions = false; // true reveals questions that were answered incorrectly as part of the grading process
  boolean useSectionDeadlines = false; // true uses default deadlines for all sections of the course
  int numberOfSections = 1;
  boolean trackAnswers= false; //true keeps track of missed questions for students.... warning: setting this to true has the potential of racking up hd space in a hurry. Use at your own discretion. RDZ.
  RBStore res=EledgeResources.getExamBundle(); //i18n

  Logger log = new Logger();
  public String getServletInfo() {
    return "This Eledge servlet is used to administer examinations.";
  }
      
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Exam");
    
    if (student.getIsFrozen()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Examenes</em><br><br>"+res.getString("str_account_frozen").concat(res.getString("str_contact_instructor")),student));
      return;
    }
    
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Exam",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Examenes</em><br><br>"+err.toString(),student));
        return;
      }
    }

    StringBuffer error = new StringBuffer();
    getExamParameters(student);
    
    if (timeLimit>0 && session.getMaxInactiveInterval()<(timeLimit*60 + 300))
      session.setMaxInactiveInterval(timeLimit*60 + 300);
      
    makeExamparametersToCourse(student);
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Examenes</em><br><br>"+examSelectForm(student),student));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Exam");
    if (student.getIsFrozen()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Examenes</em><br><br>"+res.getString("str_account_frozen").concat(res.getString("str_contact_instructor")),student));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Exam",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Examen Error</em><br><br>"+err.toString(),student));
        return;
      }
    }

    getExamParameters(student);

    // find out what the user wants
    String userRequest = request.getParameter("UserRequest");

    if (userRequest==null) {  // Print a form for the student to select an exam:
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Examenes</em><br><br>"+examSelectForm(student),student));
      return;
    }
    
    int assignmentNumber=-1;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> No Examen</em><br><br>"+res.getString("str_select_valid_exam"),student));
      return;
    }

    if (userRequest.equals("NewExam")) {
      if (okTimeForNewExam(student,assignmentNumber,out))
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> "+getExamTitle(assignmentNumber)+"</em><br><br>"+printExam(student,assignmentNumber,request.getRemoteAddr()),student));
      return;
    }
  
    if (userRequest.equals("GradeExam")) {
      int code = Integer.parseInt(request.getParameter("Code"));
      int possibleScore = Integer.parseInt(request.getParameter("PossibleScore"));
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> "+getExamTitle(assignmentNumber)+"</em><br><br>"+gradeExam(student,assignmentNumber,possibleScore,code,request),student));
      return;
    }

    out.println("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Examen Error</em><br><br>"+Page.create(res.getString("str_dont_understand"),student));
  }

  String examSelectForm(Student student) {
    StringBuffer buf = new StringBuffer();
    int sectionID = student.sectionID;
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a"); //i18n sensible?
    buf.append("<b>" + student.getFullName() + "</b>");
    Date now = new Date();
    buf.append("<br>" + df.format(now));
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //ResultSet rsParams = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      //if (rsParams.next()) numberOfSections = rsParams.getInt("Value");
      //String sectionName;
      //ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + sectionID + "'");
      //if (rsSections.next())
      //  sectionName = rsSections.getString("SectionName");
      //else {
      //  sectionID=1;
      //  sectionName = "";        
      //}
      //if (useSectionDeadlines) buf.append("<br>".concat(res.getString("str_section")).concat(sectionName));
      //else sectionID=1;
      
      Timestamp deadline;
      MessageFormat mf = new MessageFormat(res.getString("str_timed_interval"));
      Object[] mfArgs = {
          new Integer(timeLimit)
      };
      buf.append("<form method=POST");
      if (!allowMultipleTries){
        buf.append(" onSubmit=\"return confirm('" + res.getString("str_warning_one_chance")
        + (timeLimit>0?mf.format(mfArgs):"")
        + (enforceDeadlines?res.getString("str_deadline_warning"):"")
        + res.getString("str_sure_to_take_exam") + "')\"");
      }
      buf.append(">");
      buf.append("\n".concat(res.getString("str_select_exam")).concat("<br>"));
      buf.append("<table cellspacing=0 border=1>");
      //buf.append("<tr><td></td><td><b>".concat(res.getString("str_field_exam_code")).concat("</b></td><td><b>").concat(res.getString("str_field_exam_title")).concat("</b></td><td><b>").concat(res.getString("str_field_exam_dateavail")).concat(!allowWorkAhead?res.getString("str_enforced"):"").concat("</b></td><td><b>").concat(res.getString("str_field_exam_deadline")).concat(enforceDeadlines?res.getString("str_enforced"):"").concat("</b></td><td><b>").concat(res.getString("str_field_exam_previous_exams")).concat(enforcePreviousExams?res.getString("str_enforced"):"").concat("</b></td><td><b>").concat(res.getString("str_field_exam_score")).concat("</b></td></tr>"));
      buf.append("<tr><td></td><td><b>".concat(res.getString("str_field_exam_code")).concat("</b></td><td><b>").concat(res.getString("str_field_exam_title")).concat("</b></td><td><b>").concat(res.getString("str_field_exam_deadline")).concat(enforceDeadlines?res.getString("str_enforced"):"").concat("</b></td></tr>"));
      int assignmentNumber;
      boolean previousExamExpired = true;
        ResultSet rs;
      try {  // get exam informatino for student's section, if it exists
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline" + sectionID + " AS Deadline,Available" + sectionID + " AS Available FROM ExamInfo WHERE id_course='"+student.getCourse_id()+"' "+"ORDER BY DEADLINE");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline1 AS Deadline FROM ExamInfo WHERE id_course='"+student.getCourse_id()+"' "+"ORDER BY DEADLINE");
      }
      while (rs.next()) {
        assignmentNumber = rs.getInt("AssignmentNumber");
        deadline = rs.getTimestamp("Deadline");
        if (deadline.before(now))  // this is an expired exam
          buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        else if (previousExamExpired) { // this is the current exam
          // Need to check for the dateavailable, (as long as allow workahead is false.)

          Timestamp available = rs.getTimestamp("Available");
          //use "shortcut" or here. If we allow workahead, then the available
          //time doesn't matter, so short cirucuit it and print the Exam.
          //otherwise, if we dont' allow workahead, check to make sure that
          //the available time is before now. If so, print the Exam.
          //Otherwise, print "Unavailable". Note: chose to use Unavailable vs.
          //n/a in order to differenciate this as the "current" test. It -is-
          //the current one, you just can't take it yet. ;) The rest are n/a
          //Also, if it's an instructor, allow clicking.
          if (allowWorkAhead || available.before(now) || student.getIsInstructor()) {
            
           //no highlighting: buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
              buf.append("\n<tr BGCOLOR=FFFF00><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + " CHECKED></td>");
          } else 
            buf.append("\n<tr><td align=center>Unavailable</td>");
          previousExamExpired = false;
        }
        else if (allowWorkAhead || student.getIsInstructor()) { // future exam is made available
            //don't need to worry about it here, because if they're the instructor, they -should- be able to see this. And if we allow work ahead, we should be able to see this, as well.
            buf.append("\n<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        }
        else { // this exam is not currently available but is listed to show the deadline information
            buf.append("\n<tr><td align=center><font color=FF0000 size=-2>n/a</font></td>"); // exam not available yet
        }
        //may need to add a new column for date available.... consider it later.
        buf.append("\n<td ALIGN=CENTER>" + assignmentNumber + "</td>");
        buf.append("\n<td>" + rs.getString("Title") + "</td>");
        buf.append("\n<td>" +  df.format(deadline) + "</td>");
        buf.append("\n</tr>");
      }
    } 
    catch (Exception e) {
      if (addAvailableField())
        return examSelectForm(student);
      return createExamTables(student);
    }
    buf.append("</table>");
    buf.append("\n<input type=hidden name='UserRequest' value='NewExam'>");
    buf.append("\n<input type=submit value='".concat(res.getString("str_button_display_exam")).concat("'>"));

    buf.append("</form>");

    if (student.getIsInstructor()) buf.append("\n<FORM ACTION=" + Course.name + ".ManageExam>"
      + "<b>".concat(res.getString("str_instructor_only")).concat(":</b><input type=submit value='").concat(res.getString("str_button_manage_exam")).concat("'></FORM>"));

    buf.append(examRules());
    return buf.toString();
  }

  String examRules () {
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_exam_rule_elapsedTime1"));    
    Object[] mfArgs = {
      new Integer(timeLimit)
    };
    buf.append("\n<h2>".concat(res.getString("str_exam_rules_title")).concat("</h2>"));
    buf.append("\n<ul>");
    if (enforceDeadlines)
      buf.append("\n<li>".concat(res.getString("str_exam_rule_deadline")).concat("</li>"));
    if (allowMultipleTries)
      buf.append("\n<li>".concat(res.getString("str_exam_rule_ntries1")).concat("</li>"));
    else
      buf.append("\n<li>".concat(res.getString("str_exam_rule_ntries2")).concat("</li>"));
    if (timeLimit != 0) {
      buf.append("\n<li>" + mf.format(mfArgs) + "</li>");
      buf.append("\n<li>".concat(res.getString("str_exam_rule_elapsedTime2")).concat("</li>"));
    }
    //if (waitForNewDownload > 0)
    //  buf.append("\n".concat(res.getString("str_exam_rule_dlRate")).concat(String.valueOf(waitForNewDownload)).concat(res.getString("str_time_unit")));
    buf.append("\n</ul>");
    
    return buf.toString();  
  }

  boolean okTimeForNewExam(Student student, int assignmentNumber, PrintWriter out) {
    if (waitForNewDownload==0) return true;
    if (student.getIsInstructor()) return true;
    ResultSet rsTimestamp;
    Statement stmt;
    boolean returnValue;
    MessageFormat mf = new MessageFormat(res.getString("str_must_wait"));
    Object[] mfArgs = {
      new Integer(waitForNewDownload)
    };
    Calendar now = Calendar.getInstance();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      stmt = conn.createStatement();
      rsTimestamp = stmt.executeQuery("SELECT MAX(Date-ElapsedMinutes*100) AS StartTime FROM ExamTransactions "
       + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND AssignmentNumber='" + assignmentNumber + "' GROUP BY StudentIDNumber");
      if (rsTimestamp.next()) {
        Calendar then = Calendar.getInstance();
        then.setTime((Date)rsTimestamp.getTimestamp("StartTime"));
        then.add(Calendar.MINUTE,waitForNewDownload);
        if (!now.after(then)) {  // not yet eligible for new exam
          out.println(Page.create(mf.format(mfArgs).concat(res.getString("str_exam_dlInterval")).concat("<br>").concat(res.getString("str_current_time")).concat(now.getTime().toString()).concat("<br>").concat(res.getString("str_next_exam_availability")).concat(then.getTime().toString()).concat("<hr>").concat(res.getString("str_exam_dlTip")),student));
        }
        returnValue = now.after(then);
      }
      else {
        returnValue = true;
      }
      rsTimestamp.close();
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      returnValue = false;
    }
    return returnValue;
  }

  String printExam(Student student,int assignmentNumber,String ipNumber) {
    int nRows = 0;
    int possibleScore = 0;
    int code = 0;
    String sqlInsert;
    String title = "";
    StringBuffer buf = new StringBuffer();
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a"); //i18n sensible?
    String courseTitle=getCourseTitle();

    buf.append("<h3>".concat(courseTitle).concat("</h3>"));
    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + df.format(now));
    Random rand = new Random();
    log.paranoid("Begin try. Student:"+student.getIDNumber(),"printExam");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ExamInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      if (rsInfo.next()) {
        //nSubjectAreas = rsInfo.getInt("NSubjectAreas");
        //nQuestionsPerSubjectArea = rsInfo.getInt("NQuestionsPerSubjectArea");
        title = rsInfo.getString("Title");
      }
      buf.append("<h3>".concat(res.getString("str_field_exam_code")).concat(String.valueOf(assignmentNumber)).concat(" - ").concat(title).concat("</h3>"));
      //buf.append("<h3>".concat(res.getString("str_field_exam_code")).concat(" - ").concat(title).concat("</h3>"));
      buf.append("<form name=examForm METHOD=POST onSubmit=\"return confirm('".concat(res.getString("str_grading_confirmation")).concat("')\">"));
      buf.append("<OL>");
      rsInfo.close();
      log.paranoid("Begin question writing for loop for " + student.getIDNumber() + ".","Exam:printExam");
    // get the code number if one has been assigned previously, and use it to generate the same exam
      ResultSet rsCode = stmt.executeQuery("SELECT Code FROM ExamTransactions WHERE "
      + "StudentIDNumber='" + student.getIDNumber() + "' AND AssignmentNumber='" 
      + assignmentNumber + "' AND StudentScore IS NULL ORDER BY Date");
      if (rsCode.last()) // unfinished exam exists
        code = rsCode.getInt("Code");
      else { // this is a new exam
        log.paranoid("Attempting to insert randomd transaction.","Exam:printExam");
        do { // try to record the transaction until a unique random code works
          code = rand.nextInt(99999999);
          sqlInsert = "INSERT INTO ExamTransactions"
          + " (StudentIDNumber,LastName,FirstName,AssignmentNumber,Code,IPNumber)"
          + " VALUES ('"+student.getIDNumber() + "','" + converter(student.lastName)
          + "','" + converter(student.firstName) + "','" + assignmentNumber + "','" 
          + code + "','" + ipNumber + "')";
        } while (dbSQLRequest(sqlInsert) != 1);  // in case of accidental repeat of a code value
        log.paranoid("Inserted.","Exam:printExam");
      }
      rand.setSeed((long)code); // seeds the random int generator to give a predictable question set based on code
      
      //for (int area=0;area<nSubjectAreas;area++) {
        Vector questions = new Vector();
        String sqlQueryString = "SELECT * FROM ExamQuestions WHERE AssignmentNumber='" + assignmentNumber + "'";
        ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
        log.paranoid("Begin while loop to load questions into vector for " + student.getIDNumber() + ".","Exam:printExam");
        while (rsQuestions.next()) {
          Question question = new Question();
          question.loadQuestionData(rsQuestions);
          questions.addElement(question);
        }
        log.paranoid("End While. " + student.getIDNumber(),"Exam:printExam");
        rsQuestions.close();

        int nQuestions = nQuestionsPerSubjectArea<questions.size()?nQuestionsPerSubjectArea:questions.size();
        log.paranoid("Begin inner for." + student.getIDNumber(),"Exam:printExam");
        for (int i = 0; i < nQuestions; i++){
          int q = scrambleQuestions?rand.nextInt(questions.size()):0;
          Question selected = (Question)questions.remove(q);
          if (selected.getQuestionType()==6)//essay questions.
          {
            //if it picks an essay question, skip it. Essay
            //questions are handled separately.
            log.paranoid("Essay question selected." + student.getIDNumber(),"Exam:printExam");
            if (nQuestionsPerSubjectArea < questions.size()){
              i--;
            }
            continue;
          }
          possibleScore += selected.getPointValue();
          if (trackAnswers){ // record the exam questions in the database
            log.paranoid("Track Answers True. " + student.getIDNumber(),"Exam:printExam");
            String mySqlInsertString = "INSERT INTO ExamAssignedQuestions VALUES('" + code 
            + "','" + selected.getID() + "','" + selected.getQuestionGraded() + "','null')";
            stmt.executeUpdate(mySqlInsertString);
          }
          buf.append("\n<li>" + selected.print() + "</li>");
        }
      //}   
      
      //!!!OJO lo de arriba lo borre porque utiliza Subjet Area
      
//order by keeps things consistent so that essay question "1" is always question
//1. (In this case, for the essay grading, it's necessary that the order be
//the same.
      sqlQueryString="Select * from ExamQuestions WHERE AssignmentNumber='" + assignmentNumber + "' AND QuestionType='ESSAY' ORDER BY QuestionID";
      log.paranoid("Executing query: " + sqlQueryString + " for:" + student.getIDNumber(),"Exam:printExam");
      rsQuestions = stmt.executeQuery(sqlQueryString);
      log.paranoid("Begin Essay Question while loop for " + student.getIDNumber(),"Exam:printExam");
      while(rsQuestions.next()){
        Statement stmt2=conn.createStatement();
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        sqlQueryString="SELECT * FROM Essays WHERE StudentIDNumber='" + student.getIDNumber() + "' AND TestType='Exam' AND QuestionID='" + question.getID() + "'";
        log.paranoid("Executing: " + sqlQueryString + " within while for " + student.getIDNumber(),"Exam:printExam");
        ResultSet rsHasAnswered=stmt2.executeQuery(sqlQueryString);
        if (!rsHasAnswered.next()){
         buf.append("\n<li>" + question.print() + "</li>");
        }
        rsHasAnswered.close();
        stmt2.close();
      } 
      log.paranoid("End while loop for " + student.getIDNumber(),"Exam:printExam");
      rsQuestions.close();
      buf.append("</ol>");
      stmt.close();
      conn.close();
    } catch(Exception e) {
       if (addSectionField()) {
         log.normal("added Section Field; updating.","Exam:printExam");
         return (res.getString("str_table_updated").concat(res.getString("str_refresh_browser")));
       }
       else if (addAQTable()) {
         log.normal("addAQTable true.","Exam:printExam");
         return (res.getString("str_table_updated").concat(res.getString("str_refresh_browser")));
       }
       else if (addEssayTable()) {
         log.normal("addEssayTable true.","Exam:printExam");
         return (res.getString("str_essay_table_added").concat(res.getString("str_refresh_browser")));
       }
       log.sparse("Caught exception " + e.getMessage() + " while " + student.getIDNumber() + " was working","Exam:printExam");
       return e.getMessage();
    }
    buf.append("<input type=hidden name='Code' value=" + code + ">");
    buf.append("<input type=hidden name='PossibleScore' value=" + possibleScore + ">");
    buf.append("<input type=hidden name='AssignmentNumber' value=" + assignmentNumber + ">");
    buf.append("<input type=hidden name='UserRequest' value='GradeExam'>");
    buf.append("<input type=submit value='".concat(res.getString("str_button_compute_exam_score")).concat("'>"));
    buf.append("</form>");
    log.paranoid("Ending Method for " + student.getIDNumber(),"Exam:printExam");
    return buf.toString();
  }

  String converter(String oldString) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',0);
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }

  String converter(String oldString, int fromIndex) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }

  String gradeExam(Student student, int assignmentNumber, int possibleScore, int code, HttpServletRequest request) {
    log.paranoid("Beginning method for " + student.getIDNumber(),"Exam:gradeExam");
    boolean hasEssayQuestion=false;
    int studentScore = 0; // returned value
    StringBuffer buf = new StringBuffer();
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a"); //i18n sensible?

    buf.append("<h3>".concat(getCourseTitle()).concat("</h3>"));
    buf.append("<b>".concat(res.getString("str_grade_page_title")).concat("</b>"));
    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + df.format(now));
      buf.append("<h3>".concat(res.getString("str_field_exam_code")).concat(" ").concat(String.valueOf(assignmentNumber)).concat("</h3>"));
      buf.append("<h3>".concat(res.getString("str_field_exam_code")).concat(" - ").concat(getExamTitle(assignmentNumber)).concat("</h3>"));
    
    log.paranoid("Beginning try for " + student.getIDNumber(),"Exam:gradeExam");
    try {
      boolean someAnswersWrong = false;
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ExamQuestions WHERE AssignmentNumber='" + assignmentNumber + "'";
      log.paranoid("Executing query: " + sqlQueryString + " for " + student.getIDNumber(),"Exam:gradeExam");
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
      log.paranoid("Begin main while loop for " + student.getIDNumber(),"Exam:gradeExam");
      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        String studentAnswer[] = request.getParameterValues(question.getID());
        if (studentAnswer != null) {
          log.paranoid("studentAnswer wasn't null:" + question.getID() + ":" + student.getIDNumber(),"Exam:gradeExam");
          for (int i = 1; i < studentAnswer.length; i++)
            studentAnswer[0] += studentAnswer[i];
          int pts = question.getPointValue();
          if (question.getQuestionType()==6){
          log.paranoid("Essay question for " + student.getIDNumber(),"Exam:gradeExam");
            if(!(studentAnswer[0].equals(""))){
              log.paranoid("Answer not blank for " + student.getIDNumber(),"Exam:gradeExam");
//if !hasScore guards against students attempting to resubmit a previously 
//submitted exam.
              if (!hasScore(code)){
                log.paranoid(student.getIDNumber() + " didn't have a score.","Exam:gradeExam");
                hasEssayQuestion=true;
                log.paranoid("Recording essay for " + student.getIDNumber(),"Exam:gradeExam");
                recordEssay(student.getIDNumber(), question.getID(), studentAnswer[0],code,student.getCourse_id());
              }
              
            }
            continue;
          }
          if (question.isCorrect(studentAnswer[0])) studentScore += pts;
          else if (showMissedQuestions) {  // print a list of questions answered incorrectly:
            log.paranoid(student.getIDNumber() + " answered " + question.getID() + " incorrectly.","Exam:gradeExam");
            if (!someAnswersWrong) { 
              log.paranoid("First wrong answer for " + student.getIDNumber(),"Exam:gradeExam");
              buf.append("<FONT COLOR=#FF0000><h4>".concat(res.getString("str_incorrect_answered_questions")).concat("</h4>").concat(res.getString("str_incorrect_answered_questions2")).concat("</FONT><UL>"));
              someAnswersWrong = true;
            }
            buf.append("<LI>" + question.print() + "</LI>");
          }
          //It's been graded; say so and update student answer if tracking enabled.
          if (trackAnswers && !hasScore(code)){
            log.paranoid("trackAnswers-true; didn't have score:" + student.getIDNumber(),"Exam:gradeExam");
            stmt.executeUpdate("UPDATE ExamAssignedQuestions SET Graded='"
            + question.getQuestionGraded() + "', StudentAnswer='"
            + converter(studentAnswer[0]) + "' WHERE Code='" + code
            + "' AND QuestionID='" + question.getID() + "'");
          }
        }
      }
      rsQuestions.close();
      stmt.close();
      conn.close();
      if (someAnswersWrong) buf.append("</UL><hr>");  // finish unordered list of questions answered incorrectly
      buf.append(res.getString("str_exam_score").concat(String.valueOf(studentScore)).concat(res.getString("str_exam_score2")).concat(String.valueOf(possibleScore)).concat(res.getString("str_score_units")));
    } catch(Exception e) {
      log.sparse("Exception: " + e.getMessage() + " caught for " + student.getIDNumber(),"Exam:gradeExam");
      buf.append("Error Message: " + e.getMessage());
    }
    if (eligibleForGrading(student,assignmentNumber,code,buf)) {
      log.paranoid(student.getIDNumber() + " was eligible for grading.","Exam,gradeExam");
      if (possibleScore > 0 && studentScore == possibleScore) buf.append("<h2>".concat(res.getString("str_exam_score_congrat")).concat("</h2>"));
      log.paranoid("Attempting to record score for " + student.getIDNumber(),"Exam:gradeExam");
      buf.append(recordScore(code,assignmentNumber,studentScore,possibleScore,student,request.getRemoteAddr()));
      if (hasEssayQuestion){
        log.paranoid(student.getIDNumber() + " has an essay question.","Exam:gradeExam");
        buf.append(recordEssayScore(code,assignmentNumber,student,request.getRemoteAddr()));
      }
    }
    else {
      log.paranoid(student.getIDNumber() + "'s score was -NOT- recorded in the class database.","Exam:gradeExam");
      buf.append("<BR>".concat(res.getString("str_score_not_recorded")));
//if the score isn't recorded, the teacher will never see the score; Hence, there's really
//no point in keeping those answers around.
      if (trackAnswers && !hasScore(code)){
        log.paranoid("Didn't record " + student.getIDNumber() + "'s score, so deleting from ExamAssignedQuestions where code=" + code,"Exam:gradeExam");
        dbSQLRequest("DELETE FROM ExamAssignedQuestions WHERE Code='"+code+"'");
      }
      if (hasEssayQuestion && !hasScore(code)){
        log.paranoid("Didn't record " + student.getIDNumber() + "'s score, so deleting from Essays WHERE code=" + code,"Exam:gradeExam");
        dbSQLRequest("DELETE FROM Essays WHERE Code='"+code+"' AND TestType='Exam'");
      }
    }
    log.paranoid("End method for " + student.getIDNumber(),"Exam:gradeExam");
    return buf.toString();
  }

//added 10/8/02 to protect against scores being overwritten by students
//attempting to resubmit an instance of an exam.
  boolean hasScore(int code){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsResult = stmt.executeQuery("SELECT * FROM Scores WHERE Code='"+code+"' AND TestType='Exam'");
      if (rsResult.next()) {
        log.paranoid("Score exists.","Exam:hasScore");
        return true;
      }
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      log.normal("Caught exception " + e.getMessage(),"Exam:hasScore");
      return false;
      
    }
    return false;
  }
  
  void TableScore(){
  	
    int result=0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      result = stmt.executeUpdate("CREATE TABLE Scores (ScoreID INT PRIMARY KEY AUTO_INCREMENT,"
		    + "StudentIDNumber VARCHAR(50),Assignment TEXT,Score INT,Timestamp TIMESTAMP,"
        + "IPAddress VARCHAR(15), Code INT, TestType VARCHAR(8), id_course INT)");
        
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
          }
    
 }
  
  
//added 03/03/2003 to print course title in exams
   String getCourseTitle(){  
    log.paranoid("Beginning try block.","Exam:getCourseTitle");
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsTitle = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='Title'");
      if (!rsTitle.next() || rsTitle.getString("Value")==null ||  rsTitle.getString("Value")=="")
        return "Course Title not assigned";
      String title=rsTitle.getString("Value");
      return title;
    }catch(Exception e){
      log.normal("Caught exception " + e.getMessage(),"Exam:getCourseTitle");
      return "Course Title not assigned";
    }
  }
//added 03/03/2003 to print exam title in exams printing and grading
   String getExamTitle(int assignmentNumber){
    log.paranoid("Beginning try block.","Exam:getExamTitle");
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsTitle = stmt.executeQuery("SELECT Title from ExamInfo WHERE AssignmentNumber='".concat(String.valueOf(assignmentNumber)).concat("'"));
      if (!rsTitle.next() || rsTitle.getString("Title")==null ||  rsTitle.getString("Title")=="")
        return "Exam Title not assigned";
      String title=rsTitle.getString("Title");
      return title;
    }catch(Exception e){
      log.normal("Caught exception " + e.getMessage(),"Exam:getExamTitle");
      return "Exam Title not assigned";
    }
  } 

  int getEssayPoints(Student student, int assignmentNumber) {
    int essayPoints=0;
    log.paranoid("Begin Try for " + student.getIDNumber(),"Exam:getEssayPoints");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //rather than adding the "assignmentnumber" column (which would, be
      //a viable solution... from now on, but leaves those using essay q's
      //up 'til now, well, it leaves them out in the dark. ;) Instead,
      //since there is a code for each question, we'll select the codes
      //from the tables of transactions, and look for essay questions
      //w/ the appropriate testtype and code.
      //the "is not null" keeps the current exam attempt from being included.
      ResultSet rsCodes=stmt.executeQuery("SELECT Code FROM ExamTransactions "
      + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND "
      + "AssignmentNumber=" + assignmentNumber + " AND StudentScore IS NOT NULL");
      //if there are no codes, this is the first attempt at the assignment anyway, and there won't be any essays to speak of.
      if (!rsCodes.next()) {
        log.paranoid("No essay points for " + student.getIDNumber() + ".","Exam:getEssayPoints");
        return 0;
      }
      String sqlQueryString="SELECT Score FROM Essays WHERE StudentIDNumber='"
      + student.getIDNumber() + "' AND TestType='Exam' AND Graded='true' AND (";
      boolean done=false;
      while (!done) {
        sqlQueryString+="Code=" + rsCodes.getInt("Code");
        if (rsCodes.next())
          sqlQueryString+=" OR ";
        else {
          sqlQueryString+=")";
          done=true;
        }
      }
      rsCodes.close();
      log.paranoid("Executing query: " + sqlQueryString,"Exam:getEssayPoints");
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      while (rsEssays.next()){
        essayPoints+=rsEssays.getInt("Score");
      }
    }catch(Exception e){
      log.sparse("Caught exception " + e.getMessage(),"Exam:getEssayPoints");
      return 0;
    }
    return essayPoints;
  }

  String recordScore(int code, int assignmentNumber, int studentScore, int possibleScore,Student student,String ipAddress) {
    log.paranoid("Begin method for " + student.getIDNumber(),"Exam:recordScore");
    
    studentScore+=getEssayPoints(student,assignmentNumber);
    
    
    String sqlUpdateString1 = "Select * From Scores";
     //crea la tabla Scores si aun no se ha creado
    if (dbSQLRequest(sqlUpdateString1)!=1) {
    	TableScore();
    }
    
    String sqlUpdateString = "UPDATE ExamTransactions SET StudentScore='" + studentScore
    + "',PossibleScore='" + possibleScore + "',ElapsedMinutes='" + elapsedMinutes(code) + "'"
    + " WHERE ((Code="+code+") AND (StudentScore IS NULL))";
    
    String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType, id_course) Values ('" + student.getIDNumber() 
    + "','Exam" 
    + (assignmentNumber<10?"0":"")  // inserts leading zero Exam01 - Exam09 for correct ordering in gradebook
    + assignmentNumber + "','" + studentScore + "','" + ipAddress + "','"
    + code + "','Exam', '"+student.getCourse_id()+"')"; 
    log.paranoid("Executing update: " + sqlUpdateString + " and Insert:" + sqlInsertString + " for " + student.getIDNumber(),"Exam:recordScore");
    
    if (dbSQLRequest(sqlUpdateString)==1 && dbSQLRequest(sqlInsertString)==1) { //successful
      log.paranoid("Update and insert for " + student.getIDNumber() + " successful . . . supposedly.","Exam:recordScore");
      return ("<BR>".concat(res.getString("str_score_recorded")));
    }
//if error, maybe from old version of Eledge that doesn't have Code and Type
//in Scores table. So, after adding Code and Type, try request again.
//if -still- no good; unexpected error(note, since update already done above, shouldn't do here.
    if (addCodeType()){
      if(dbSQLRequest(sqlInsertString)==1) {
        log.paranoid("Student score recorded after addCodeType for " + student.getIDNumber(),"Exam:recordScore");
        return ("<BR>".concat(res.getString("str_score_recorded")));
      }
    }
    log.paranoid("Student: " + student.getIDNumber() + "'s score -NOT- recorded, for \"unexpected reasons.\".","Exam:recordScore");
    return (res.getString("str_score_not_recorded_unexpected").concat(String.valueOf(assignmentNumber)).concat(" ").concat(String.valueOf(studentScore)));
    }

  int dbSQLRequest(String sqlString) {
    log.paranoid("Begin method.","Exam:dbSQLRequest");
    int result=0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Executing: " + sqlString,"Exam:dbSQLRequest");
      result = stmt.executeUpdate(sqlString);
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage() + " while executing: " + sqlString,"Exam:dbSQLRequest");
    }
    log.paranoid("End method.","Exam:dbSQLRequest");
    return result;
  }


  void recordEssay(String studentID, String questionID, String studentAnswer, int code, String id_course) {
//so, this, eventually, will record the student's essay in the essays table.
    log.paranoid("Recording essay for " + studentID,"Exam:recordEssasy");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlUpdateString="INSERT INTO Essays VALUES (" + code + ",'";
      sqlUpdateString+=questionID + "','false','";
      sqlUpdateString+=CharHider.quot2literal(studentAnswer) + "','" + studentID + "','Exam','0','"+id_course+"')";
      stmt.executeUpdate(sqlUpdateString);
      stmt.close();
      conn.close();
    }
    catch(Exception e){
      log.paranoid("Caught: " + e.getMessage() + " trying addEssayTable","Exam:recordEssasy");
      addEssayTable();
    }
    log.paranoid("End method","Exam:recordEssay");
  }

  String recordEssayScore(int code, int assignmentNumber, Student student, String ipAddress){
    int nofquestions=0;
    log.paranoid("Begin try for " + student.getIDNumber(),"Exam:recordEssayScore");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCount=stmt.executeQuery("SELECT COUNT(*) FROM ExamQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int questionID[];
      if (rsCount.next())
         questionID=new int[rsCount.getInt("Count(*)")];
      else return "Error: no essay questions for for this assignment.";
      rsCount.close();
      ResultSet rsQuestionID=stmt.executeQuery("SELECT QuestionID from ExamQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int i=0;
      while (rsQuestionID.next()){
        questionID[i++]=rsQuestionID.getInt("QuestionID");
      }
      rsQuestionID.close();
      ResultSet rsEssayQ=stmt.executeQuery("SELECT * from Essays WHERE Code='" + code + "' AND TestType='Exam' ORDER BY QuestionID");
      while (rsEssayQ.next()){
        int id=-1;
        for(int j=0; j<i;j++){
          if (questionID[j]==rsEssayQ.getInt("QuestionID")){
             id=j;
             break;
          }
        }
        String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType, id_course) VALUES ('" + student.getIDNumber()
        + "','Exam" + (assignmentNumber<10?"0":"")
        + assignmentNumber + "Essay" + (id<10?"0":"") + (id+1) + "','-"
        + questionID[id] + "','" + ipAddress + "','" + code + "','Exam', '"+student.getCourse_id()+"')";
        stmt.executeUpdate(sqlInsertString);
        nofquestions++;
      }
    }catch(Exception e){
      log.sparse("Caught: " + e.getMessage() + " while " + student.getIDNumber() + " working","Exam:recordEssayScore");
      return ("<BR>".concat(res.getString("str_essay_processing_error")));
    }
    log.paranoid("Normal termination of method.","Exam:recordEssayScore");
    return ("<BR>".concat(res.getString("str_your_essay")).concat((nofquestions>1?"s ":res.getString("str_essay_later_processing"))));
  }

  boolean eligibleForGrading(Student student, int assignmentNumber, int code, StringBuffer buf) {
    boolean eligible = true;  // initialize
    if (!allowMultipleTries) {
      if (nTries(student,assignmentNumber) > 1) {
        eligible = false;
        log.paranoid("Too many attempts at exam" + assignmentNumber + " for " + student.getIDNumber() + "; not eligible for grading!","Exam:eligibleForGrading");
        buf.append("<br>".concat(res.getString("str_only_one_attempt")));
      }
    }
    if (timeLimit != 0) {
      int minutes = elapsedMinutes(code);
      log.paranoid("elapsedMinutes returned: " + minutes,"Exam:eligibleForGrading");
      if (minutes > timeLimit) {
        eligible = false;
        log.paranoid(student.getIDNumber() + " took too long! Can't record score.","Exam:eligibleForGrading");
        buf.append("<br>".concat(res.getString("str_elapsed_time_exceeded")).concat(String.valueOf(minutes)).concat(res.getString("str_elapsed_time_exceeded1")).concat(String.valueOf(timeLimit)).concat(res.getString("str_elapsed_time_exceeded2")));
      }
    }
    if (enforceDeadlines) {
      if (deadlinePassed(student,assignmentNumber)) {
        eligible = false;
        log.paranoid("Whoops! Deadline passed. Can't record " + student.getIDNumber() + "'s score.","Exam:elibigleForGrading");
        buf.append("<br>".concat(res.getString("str_deadline_passed")));
      }
    }
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ExamTransactions WHERE Code='" + code + "' AND StudentScore IS NULL";
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (!rs.next()) {
        eligible = false;
        log.paranoid("Uh-oh! " + student.getIDNumber() + " tried the ol' go back, change answers, and resubmit, it would seem! (They've already submitted this instance of this exam for grading.","Exam:eligibleForGrading");
        buf.append("<br>".concat(res.getString("str_exam_previously_submitted")));
      }
      else // there is an unscored exam; if ineligible to score it, set the score to zero
        if (!eligible) 
          stmt.executeUpdate("UPDATE ExamTransactions SET StudentScore=0 WHERE Code='" + code + "'");
    }
    catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage() + " for " + student.getIDNumber() + "; might want to look into that.","Exam:eligibleForGrading");
      eligible = false;
    }
    log.paranoid("returning: " + eligible + " for " + student.getIDNumber(),"Exam:eligibleForGrading");
    return eligible;
  }

  int nTries(Student student, int assignmentNumber) {
    int n = 0;
    log.paranoid("Begin try; " + student.getIDNumber(),"Exam:nTries");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ExamTransactions WHERE (AssignmentNumber='" + assignmentNumber + "' AND StudentIDNumber='" + student.getIDNumber() + "')";
      log.paranoid("Executing: " + sqlQueryString,"Exam:nTries");
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      while (rs.next()) {
        n++;
        log.paranoid("In while loop. N: " + n + " for " + student.getIDNumber(),"Exam:nTries");
      }
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      log.sparse("Caught exception " + e.getMessage() + " for " + student.getIDNumber(),"Exam:nTries");
    }
    log.paranoid("returning n = " + n + " for " + student.getIDNumber(),"Exam:nTries");
    return n;
  }

  int elapsedMinutes(int code){
    log.paranoid("Begin Method.","Exam:elapsedMinutes");
    Timestamp dateTaken=null;
    log.paranoid("dateTaken (should be null): " + dateTaken,"Exam:elapsedMinutes");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ExamTransactions WHERE Code=" + code;
      log.paranoid("Executing; " + sqlQueryString,"Exam:elapsedMinutes");
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (rs.next()) dateTaken=rs.getTimestamp("Date");
      log.paranoid("After sql, dateTaken is: " + dateTaken,"Exam:elapsedMinutes");
      log.paranoid("In milliseconds, dateTaken is: " + dateTaken.getTime(),"Exam:elapsedMinutes");
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      log.sparse("Caught exception " + e.getMessage(),"Exam:elapsedMinutes");
    }
    Date now = new Date();
    log.paranoid("Ok, almost done....","Exam:elapsedMinutes");
    log.paranoid("now.getTime() yields: " + now.getTime(),"Exam:elapsedMinutes");
    log.paranoid("dateTaken.getTime() yields: " + dateTaken.getTime(),"Exam:elapsedMinutes");
    long elapsedMilliseconds = now.getTime() - dateTaken.getTime();
    log.paranoid("elapsedMilliseconds: " + elapsedMilliseconds,"Exam:elapsedMinutes");
    int ret = (int)(elapsedMilliseconds/60000);
    log.paranoid("ret value (after division...):" + ret,"Exam:elapsedMinutes");
    return ret;
  }

  boolean deadlinePassed(Student student,int assignmentNumber) {
    Timestamp deadline = null;
    int sectionID = student.sectionID;

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      /*ResultSet rsParams = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsParams.next()) numberOfSections = rsParams.getInt("Value");
      String sectionName;
      ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + sectionID + "'");
      if (rsSections.next())
        sectionName = rsSections.getString("SectionName");
      else {
        sectionID=1;
        sectionName = "";        
      }*/
      
      ResultSet rsDeadline;
      try {  // get exam information for student's section, if it exists
        rsDeadline = stmt.executeQuery("SELECT Deadline" + sectionID + " AS Deadline FROM ExamInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        log.paranoid("Caught exception " + e.getMessage(),"Exam:deadlinePassed");
        rsDeadline = stmt.executeQuery("SELECT Deadline1 AS Deadline FROM ExamInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      if (rsDeadline.next()) deadline = rsDeadline.getTimestamp("Deadline");
    }
    catch(Exception e) {
      log.paranoid("Caught exception " + e.getMessage(),"Exam:deadlinePassed");
      return true; // if database error occurs, assume deadline has passed
    }
    Date now = new Date();
    return deadline.before(now);  // returns true if deadline has passed
  }
  
  void getExamParameters(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT * FROM ExamParameters WHERE id_course='"+student.getCourse_id()+"'");
      rsParams.first();
      timeLimit = rsParams.getInt("TimeLimit");
      waitForNewDownload = rsParams.getInt("WaitForNewDownload");
      enforceDeadlines = rsParams.getBoolean("EnforceDeadlines");
      allowMultipleTries = rsParams.getBoolean("AllowMultipleTries");
      scrambleQuestions = rsParams.getBoolean("ScrambleQuestions");
      allowWorkAhead = rsParams.getBoolean("AllowWorkAhead");
      showMissedQuestions = rsParams.getBoolean("ShowMissedQuestions");
      useSectionDeadlines = rsParams.getBoolean("UseSectionDeadlines");
      trackAnswers= rsParams.getBoolean("TrackAnswers");
    }
    catch (Exception e) {
      log.normal("Caught exception " + e.getMessage() + " tryig addParam()","Exam:getParams");
      addParam();
    }  
  }
  
  boolean addCodeType(){
    try{
       Class.forName(Course.jdbcDriver).newInstance();
       Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
       Statement stmt = conn.createStatement();
       stmt.executeUpdate("ALTER TABLE Scores ADD COLUMN Code INT");
       stmt.executeUpdate("ALTER TABLE Scores ADD COLUMN TestType VARCHAR(8)");
    }catch(Exception e){
       return false;
    }
    return true;
  }

  boolean addParam(){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE ExamParameters ADD TrackAnswers VARCHAR(5)");
      stmt.executeUpdate("UPDATE ExamParameters SET TrackAnswers='false'");
    }catch(Exception e){
      return false;
    }
    return true;
  }

  boolean addSectionField(){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE ExamQuestions ADD (Section VARCHAR(3) DEFAULT 'All')");
      }catch(Exception e){
        return false;
      }
      return true;
  }

  boolean addAQTable(){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE ExamAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
    }catch(Exception e){
      return false;
    }
    return true;
  }

  boolean addEssayTable(){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT, id_course INT)");
    }catch(Exception e){
      return false;
    }
    return true;
  }

  static protected boolean addAvailableField() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //get the nSectionsInfo. This tells us how many "SectionDeadlines" there
      //are. Have to do this before adding available1. Use this information to
      //determine how many "Available" fields to add. 
      int nSectionsInfo = (stmt.executeQuery("SELECT * FROM ExamInfo ORDER BY AssignmentNumber").getMetaData().getColumnCount()-4);
      stmt.executeUpdate("ALTER TABLE ExamInfo ADD COLUMN Available1 DATETIME");
      for (int j=2;j<=nSectionsInfo;j++){
        stmt.executeUpdate("ALTER TABLE ExamInfo ADD COLUMN Available" + Integer.toString(j) + " DATETIME DEFAULT '2002-01-15 23:59:59'");
      }
      //If we're needing the addAVailableField method, and it's not going to 
      //error, (the examinfo table actually exists), then there are probably
      //tests already. If there are tests already, we need valid dates for 
      //the date available.
      ResultSet rsExams = stmt.executeQuery("SELECT * FROM ExamInfo ORDER BY AssignmentNumber");
      //loop through the exams and assign the previous exam deadline as the 
      //current exam available time. Set the first exam available time to 
      //the same as it's deadline.
      String previousDate="2002-01-15 23:59:59";
      boolean first = true;
      while (rsExams.next()) {
        if (first) {
          previousDate=rsExams.getString("Deadline1");
          rsExams.updateString("Available1",previousDate);
          rsExams.updateRow();
          first=false;
        } else {
          rsExams.updateString("Available1",previousDate);
          previousDate=rsExams.getString("Deadline1");
          rsExams.updateRow();
        }
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  String createExamTables(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      stmt.executeUpdate("CREATE TABLE examinfo (AssignmentNumber int(11) NOT NULL auto_increment, id_course int(11) NOT NULL default '0',Title text,"
  	  +"SubjectAreas int(11) default NULL, NQuestionsPerSubjectArea int(11) default NULL, Deadline1 datetime default NULL,"
  	  +"Available1 datetime default NULL, PRIMARY KEY  (AssignmentNumber))");
      stmt.executeUpdate("CREATE TABLE ExamQuestions (QuestionID INT PRIMARY KEY AUTO_INCREMENT,"
      + "AssignmentNumber INT,SubjectArea INT,QuestionText TEXT,QuestionType TEXT,NumberOfChoices INT,"
      + "ChoiceAText TEXT,ChoiceBText TEXT,ChoiceCText TEXT,ChoiceDText TEXT,ChoiceEText TEXT,"
      + "RequiredPrecision DOUBLE,CorrectAnswer1 TEXT,CorrectAnswer2 TEXT,QuestionTag TEXT,PointValue INT, Section VARCHAR(3) DEFAULT 'All')");
      stmt.executeUpdate("CREATE TABLE ExamTransactions (StudentIDNumber VARCHAR(50),LastName TEXT,"
      + "FirstName TEXT,AssignmentNumber INT,Date TIMESTAMP,Code INT,StudentScore INT,PossibleScore INT,"
      + "ElapsedMinutes INT,IPNumber VARCHAR(15))");
      stmt.executeUpdate("CREATE TABLE examparameters (id_course int(11) NOT NULL default '0', TimeLimit int(11) default NULL, WaitForNewDownload int(11) default NULL,"
  	  +"EnforceDeadlines varchar(5) default NULL, AllowMultipleTries varchar(5) default NULL, ScrambleQuestions varchar(5) default NULL, AllowWorkAhead varchar(5) default NULL,"
  	  +"ShowMissedQuestions varchar(5) default NULL, UseSectionDeadlines varchar(5) default NULL, TrackAnswers varchar(5) default NULL)");
//new table for keeping track of student's missed questions.
      stmt.executeUpdate("CREATE TABLE ExamAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
//new table for essay questions. Probably not many of these, so we only need one//table for all test types. So put in try/catch for case of already created.
      try{
        stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT, id_course INT)");
      }catch(Exception e){
      }
      stmt.executeUpdate("INSERT INTO ExamParameters VALUES ("+ student.getCourse_id() + "," + timeLimit + "," + waitForNewDownload 
      + ",'" + enforceDeadlines + "','" + allowMultipleTries + "','" + scrambleQuestions + "','" 
      + allowWorkAhead + "','" + showMissedQuestions + "','" + useSectionDeadlines + "','"
      + trackAnswers + "')");
      return examSelectForm(student);
    }
    catch (Exception e2) {
      return e2.getMessage();
    }
  }

  protected static String getUnsubmittedAssignments(String studentIDNumber, String id_course) {
    StringBuffer buf = new StringBuffer("");
    boolean hasUnsubmitted=false;
    try {
      buf.append("<table border=1 cellspacing=0><thead><b>Exams</b></thead>");
      buf.append("<TR><TH>Assignment</TH><TH>Status</TH></TR>");
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ExamInfo WHERE id_course='"+id_course+"'");
      if (!rsInfo.isBeforeFirst()){
        return "";
      }
      while (rsInfo.next()) {
        String assignmentName = "Exam" + ((rsInfo.getInt("AssignmentNumber")<10)?"0":"") + rsInfo.getString("AssignmentNumber");
        String sqlQueryString = "SELECT * FROM Scores WHERE StudentIDNumber='" + studentIDNumber + "' AND Assignment='" + assignmentName + "'";
        ResultSet rsAssignment=stmt2.executeQuery(sqlQueryString);
        if (!rsAssignment.next()){
          if (!hasUnsubmitted) hasUnsubmitted=true;
          buf.append("\n<TR><TD>" + assignmentName + "</TD><TD>Unsubmitted</TD></TR>");
        }
      }
    }catch(Exception e){
      return "";
    }
    if (!hasUnsubmitted)
      buf.append("</tr><td colspan=2>All exams complete</td></tr>");
    buf.append("</TABLE>");
    return buf.toString();
  }
  
  //crea nuevos registros en la tabla examparameters, para nuevos cursos que requieran el examen activo.
  public boolean makeExamparametersToCourse(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      ResultSet rsCount = stmt.executeQuery("SELECT COUNT(id_course) FROM examparameters WHERE id_course = '"+student.getCourse_id()+"'");       
      
      rsCount.next();
      
          
      if (Integer.parseInt(rsCount.getString("COUNT(id_course)"))<=0) {
            
      stmt.executeUpdate("INSERT INTO ExamParameters VALUES ("+ student.getCourse_id() + "," + timeLimit + "," + waitForNewDownload 
      + ",'" + enforceDeadlines + "','" + allowMultipleTries + "','" + scrambleQuestions + "','" 
      + allowWorkAhead + "','" + showMissedQuestions + "','" + useSectionDeadlines + "','"
      + trackAnswers + "')");
      
      return true;
      }
      
      return false;
    }
    catch (Exception e) {
      return false;
    }
  }
  
}
