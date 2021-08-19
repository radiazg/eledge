package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import javax.servlet.http.*;
import javax.servlet.*;

public class Quiz extends HttpServlet {
  // parameters that determine the properties of the quiz program:
  
  int nSubjectAreas = 1;               // default number of subject areas for quiz overridden by values read from QuizInfo database
  int nQuestionsPerSubjectArea = 10;   // number of questions presented in each area also overridden in method printQuiz()
  int timeLimit = 15;                  // minutes; set to zero for no time limit to complete the quiz
  int waitForNewDownload = 0;          // minutes; set to zero for unlimited rate of quiz downloads
  boolean enforceDeadlines = true;     // true means that quiz score is not recorded after the deadline
  boolean allowMultipleTries = true;   // false allows only one attempt at each quiz; true is recommended
  boolean scrambleQuestions = true;    // false presents the same questions each attempt; true is recommended
  boolean allowWorkAhead = false;      // true makes every quiz available; false makes available after the deadline for the previous quiz expires
  boolean showMissedQuestions = true;  // true reveals questions that were answered incorrectly as part of the grading process
  boolean useSectionDeadlines = false; // true uses default deadlines for all sections of the course
  int numberOfSections = 1;
  boolean trackAnswers= false; //true keeps track of missed questions for students.... warning: setting this to true has the potential of racking up hd space in a hurry. Use at your own discretion. RDZ.
  RBStore res = EledgeResources.getQuizBundle();    
  Logger log = new Logger();
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Getting Session.","Quiz:doGet");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    log.paranoid("Getting Student.","Quiz:doGet");
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Quiz");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Quiz",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }

    log.paranoid("Student is: " + student.getIDNumber(),"Quiz:doGet");
    StringBuffer error = new StringBuffer();
    getQuizParameters();
    if (timeLimit>0 && session.getMaxInactiveInterval()<(timeLimit*60 + 300))
      session.setMaxInactiveInterval(timeLimit*60 + 300);
    out.println(Page.create(quizSelectForm(student)));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Getting Session.","Quiz:doPost");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    log.paranoid("Getting Student.","Quiz:doPost");
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Quiz");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Quiz",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }

    log.paranoid("Student is: " + student.getIDNumber(),"Quiz:doPost");
    getQuizParameters();

    // find out what the user wants
    String userRequest = request.getParameter("UserRequest");

    if (userRequest==null) {  // Print a form for the student to select a quiz:
      out.println(Page.create(quizSelectForm(student)));
      return;
    }
    
    log.paranoid("User request not null, but: " + userRequest,"Quiz:doPost");
    int assignmentNumber=-1;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      log.normal("Invalid Quiz Number selected by " + student.getIDNumber(),"Quiz:doPost");
      out.println(Page.create(res.getString("str_select_valid")));
      return;
    }

    if (userRequest.equals("NewQuiz")) {
      if (okTimeForNewQuiz(student,assignmentNumber,out)) {
        out.println(Page.create(printQuiz(student,assignmentNumber,request.getRemoteAddr())));
        log.paranoid("Ok time for new quiz for " + student.getIDNumber(),"Quiz:doPost");
      }
      return;
    }
  
    if (userRequest.equals("GradeQuiz")) {
      log.paranoid("User Request for " + student.getIDNumber() + " was gradequiz.","Quiz:doPost");
      int code = Integer.parseInt(request.getParameter("Code"));
      log.paranoid("Student - " + student.getIDNumber() + ";Code: " + Integer.toString(code),"Quiz:doPost");
      int possibleScore = Integer.parseInt(request.getParameter("PossibleScore"));
      log.paranoid("Student - " + student.getIDNumber() + ";possibleScore:" + possibleScore,"Quiz:doPost");
      log.paranoid("Creating page 'grade Quiz' for " + student.getIDNumber(),"Quiz:doPost");
      out.println(Page.create(gradeQuiz(student,assignmentNumber,possibleScore,code,request)));
      log.paranoid("'grade Quiz' page created for " + student.getIDNumber(),"Quiz:doPost");
      return;
    }

    out.println(Page.create(res.getString("str_unkown_request")));
    log.paranoid("Request '" + userRequest + "' not understood. Made by " + student.getIDNumber(),"Quiz:doPost");
  }

  String quizSelectForm(Student student) {
    StringBuffer buf = new StringBuffer();
    int sectionID = student.sectionID;
    buf.append("<b>" + student.getFullName() + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    log.paranoid("Begin try for " + student.getIDNumber(),"Quiz:quizSelectForm");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsParams.next()) numberOfSections = rsParams.getInt("Value");
      String sectionName;
      ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + sectionID + "'");
      if (rsSections.next())
        sectionName = rsSections.getString("SectionName");
      else {
        sectionID=1;
        sectionName = "";        
      };
      if (useSectionDeadlines) buf.append("<br>" + res.getString("str_section") + " " + sectionName);
      else sectionID=1;
      
      Timestamp deadline;
      SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a");

      buf.append("<form method=POST");
      if (!allowMultipleTries){
        buf.append(" onSubmit=\"return confirm('" + res.getString("str_conf_continue") + "')\"");
      }
      buf.append(">"); 
      buf.append(res.getString("str_select_quiz") + "<br>");
      buf.append("<table cellspacing=0 border=1>");
      buf.append("<tr><td></td><td><b>" + res.getString("str_quiz") + "</b></td><td><b>"
      + res.getString("str_title") + "</b></td><td><b>" + res.getString("str_deadline") + "</b></td></tr>");

      int assignmentNumber;
      boolean previousQuizExpired = true;
        ResultSet rs;
      try {  // get quiz information for student's section, if it exists
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline" + sectionID + " AS Deadline FROM QuizInfo ORDER BY DEADLINE");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline1 AS Deadline FROM QuizInfo ORDER BY DEADLINE");
      }
      while (rs.next()) {
        assignmentNumber = rs.getInt("AssignmentNumber");
        deadline = rs.getTimestamp("Deadline");
        if (deadline.before(now))  // this is an expired quiz
          buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        else if (previousQuizExpired) { // this is the current quiz
          // no highlighting: buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
          buf.append("<tr BGCOLOR=FFFF00><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + " CHECKED></td>");
          previousQuizExpired = false;
        }
        else if (allowWorkAhead || student.getIsInstructor()) { // future quiz is made available
            buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        }
        else { // this quiz is not currently available but is listed to show the deadline information
            buf.append("<tr><td align=center><font color=FF0000 size=-2>" + res.getString("str_na") + "</font></td>"); // quiz not available yet
        }
        buf.append("<td ALIGN=CENTER>" + assignmentNumber + "</td>");
        buf.append("<td>" + rs.getString("Title") + "</td>");
        buf.append("<td>" +  df.format(deadline) + "</td>");
        buf.append("</tr>");
      }
    } 
    catch (Exception e) {
      log.normal("Caught Exception: " + e.getMessage() + " while " + student.getIDNumber() + " was working.","Quiz:quizSelectForm");
      return createQuizTables(student);
    }
    buf.append("</table>");
    buf.append("<input type=hidden name='UserRequest' value='NewQuiz'>");
    buf.append("<input type=submit value='" + res.getString("str_display_quiz") + "'>");

    buf.append("</form>");

    if (student.getIsInstructor()) buf.append("<FORM ACTION=" + Course.name + ".ManageQuiz>"
      + "<b>" + res.getString("str_teach_only") + "</b><input type=submit value='" 
      + res.getString("str_manage_quiz") + "'></FORM>");

    buf.append(quizRules());
    log.paranoid("End method" + student.getIDNumber(),"Quiz:quizSelectForm");
    return buf.toString();
  }

  String quizRules () {
    MessageFormat mf = new MessageFormat(res.getString("str_rule_no_multiple"));
    Object[] args = {
      "<i>",
      "</i>"
    };
    StringBuffer buf = new StringBuffer();
    log.paranoid("Begin method.","Quiz:quizRules"); 
    buf.append("<h2>" + res.getString("str_quiz_rules") + "</h2>");
    buf.append("<ul>");
    if (enforceDeadlines)
      buf.append("<li>" + res.getString("str_rule_deadlines") + "</li>");
    if (allowMultipleTries)
      buf.append("<li>" + res.getString("str_rule_multiple") + "</li>");
    else 
      buf.append("<li>" + mf.format(args) + "</li>");
    if (timeLimit != 0) {
      mf.applyPattern(res.getString("str_rule_timelimit"));
      args[0] = new Integer(timeLimit);
      buf.append("<li>" + mf.format(args) + "</li>");
    }
    if (waitForNewDownload > 0) {
      mf.applyPattern(res.getString("str_rule_dl_wait"));
      args[0] = new Integer(waitForNewDownload);
      buf.append("<li>" + mf.format(args) + "</li>");
    }
    buf.append("</ul>");
    log.paranoid("End method","Quiz:quizRules"); 
    return buf.toString();  
  }

  boolean okTimeForNewQuiz(Student student, int assignmentNumber, PrintWriter out) {
    if (waitForNewDownload==0) return true;
    if (student.getIsInstructor()) return true;
    ResultSet rsTimestamp;
    Statement stmt;
    boolean returnValue;
    MessageFormat mf = new MessageFormat(res.getString("str_must_wait"));
    Calendar now = Calendar.getInstance();
    Object[] args = {
      "<br>",
      new Integer(waitForNewDownload), 
      now.getTime(),
      null
    };
    log.paranoid("Begin try. Student:" + student.getIDNumber(),"Quiz:okTimeForNewQuiz");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      stmt = conn.createStatement();
      rsTimestamp = stmt.executeQuery("SELECT MAX(Date-ElapsedMinutes*100) AS StartTime FROM QuizTransactions "
       + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND AssignmentNumber='" + assignmentNumber + "' GROUP BY StudentIDNumber");
      if (rsTimestamp.next()) {
        Calendar then = Calendar.getInstance();
        then.setTime((Date)rsTimestamp.getTimestamp("StartTime"));
        then.add(Calendar.MINUTE,waitForNewDownload);
        if (!now.after(then)) {  // not yet eligible for new quiz
          args[3] = then.getTime();
    	  out.println(Page.create(mf.format(args) + "<hr>" + res.getString("str_relaunch_browser")));
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
      log.paranoid("Caught exception: " + e.getMessage() + " while " + student.getIDNumber() + " was working","okTimeForNewQuiz:");
      returnValue = false;
    }
    log.paranoid("Ending Method.","Quiz:okTimeForNewQuiz");
    return returnValue;
  }

  String printQuiz(Student student,int assignmentNumber,String ipNumber) {
    int nRows = 0;
    int possibleScore = 0;
    int code = 0;
    String sqlInsert;
    String title = "";
    StringBuffer buf = new StringBuffer();

    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    Random rand = new Random();
    log.paranoid("Begin try. Student:"+student.getIDNumber(),"printQuiz");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM QuizInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      if (rsInfo.next()) {
        nSubjectAreas = rsInfo.getInt("NSubjectAreas");
        nQuestionsPerSubjectArea = rsInfo.getInt("NQuestionsPerSubjectArea");
    	title = rsInfo.getString("Title");
      }
      buf.append("<h3>" + res.getString("str_quiz") + " " + assignmentNumber + " - " + title + "</h3>");
      buf.append("<form METHOD=POST onSubmit=\"return confirm('" + res.getString("str_confirm_grade") + "')\">");
      buf.append("<OL>");
      rsInfo.close();
      log.paranoid("Begin question writing for loop for " + student.getIDNumber() + ".","Quiz:printQuiz");
    // get the code number if one has been assigned previously, and use it to generate the same quiz
      ResultSet rsCode = stmt.executeQuery("SELECT Code FROM QuizTransactions WHERE "
      + "StudentIDNumber='" + student.getIDNumber() + "' AND AssignmentNumber='" 
      + assignmentNumber + "' AND StudentScore IS NULL ORDER BY Date");
      if (rsCode.last()) // unfinished quiz exists
        code = rsCode.getInt("Code");
      else { // this is a new quiz
        log.paranoid("Attempting to insert randomd transaction.","Quiz:printQuiz");
        do { // try to record the transaction until a unique random code works
          code = rand.nextInt(99999999);
          sqlInsert = "INSERT INTO QuizTransactions"
          + " (StudentIDNumber,LastName,FirstName,AssignmentNumber,Code,IPNumber)"
          + " VALUES ('"+student.getIDNumber() + "','" + converter(student.lastName)
          + "','" + converter(student.firstName) + "','" + assignmentNumber + "','" 
          + code + "','" + ipNumber + "')";
        } while (dbSQLRequest(sqlInsert) != 1);  // in case of accidental repeat of a code value
        log.paranoid("Inserted.","Quiz:printQuiz");
      }
      rand.setSeed((long)code); // seeds the random int generator to give a predictable question set based on code
      for (int area=0;area<nSubjectAreas;area++) {
        Vector questions = new Vector();
        String sqlQueryString = "SELECT * FROM QuizQuestions WHERE AssignmentNumber='" + assignmentNumber + "' AND SubjectArea=" + area + " AND (Section='All' OR Section='" + student.sectionID + "')";
        ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
        log.paranoid("Begin while loop to load questions into vector for " + student.getIDNumber() + ".","Quiz:printQuiz");
        while (rsQuestions.next()) {
          Question question = new Question();
          question.loadQuestionData(rsQuestions);
          questions.addElement(question);
        }
        log.paranoid("End While. " + student.getIDNumber(),"Quiz:printQuiz");
        rsQuestions.close();

        int nQuestions = nQuestionsPerSubjectArea<questions.size()?nQuestionsPerSubjectArea:questions.size();
        log.paranoid("Begin inner for." + student.getIDNumber(),"Quiz:printQuiz");
        for (int i = 0; i < nQuestions; i++){
          int q = scrambleQuestions?rand.nextInt(questions.size()):0;
          Question selected = (Question)questions.remove(q);
          if (selected.getQuestionType()==6)//essay questions.
          {
            //if it picks an essay question, skip it. Essay
            //questions are handled separately. 
            log.paranoid("Essay question selected." + student.getIDNumber(),"Quiz:printQuiz");
            if (nQuestionsPerSubjectArea < questions.size()){
              i--;
            } 
            continue;
          }
          possibleScore += selected.getPointValue();
          if (trackAnswers){ // record the quiz questions in the database
            log.paranoid("Track Answers True. " + student.getIDNumber(),"Quiz:printQuiz");
            String mySqlInsertString = "INSERT INTO QuizAssignedQuestions VALUES('" + code 
            + "','" + selected.getID() + "','" + selected.getQuestionGraded() + "','null')";
            stmt.executeUpdate(mySqlInsertString);
          }
          buf.append("\n<li>" + selected.print() + "</li>");
        }
      }
//order by keeps things consistent so that essay question "1" is always question
//1. (In this case, for the essay grading, it's necessary that the order be 
//the same.
      String sqlQueryString="Select * from QuizQuestions WHERE AssignmentNumber='" + assignmentNumber + "' AND QuestionType='ESSAY' AND (Section='" + student.sectionID + "' OR Section='All') ORDER BY QuestionID";
      log.paranoid("Executing query: " + sqlQueryString + " for:" + student.getIDNumber(),"Quiz:printQuiz");
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
      log.paranoid("Begin Essay Question while loop for " + student.getIDNumber(),"Quiz:printQuiz");
      while(rsQuestions.next()){
        Statement stmt2=conn.createStatement();
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        sqlQueryString="SELECT * FROM Essays WHERE StudentIDNumber='" + student.getIDNumber() + "' AND TestType='Quiz' AND QuestionID='" + question.getID() + "'";
        log.paranoid("Executing: " + sqlQueryString + " within while for " + student.getIDNumber(),"Quiz:printQuiz");
        ResultSet rsHasAnswered=stmt2.executeQuery(sqlQueryString); 
        if (!rsHasAnswered.next()){
         buf.append("\n<li>" + question.print() + "</li>");
        }
        rsHasAnswered.close();
        stmt2.close(); 
      }
      log.paranoid("End while loop for " + student.getIDNumber(),"Quiz:printQuiz");
      rsQuestions.close();
      buf.append("</ol>");
      stmt.close();
      conn.close();
    } catch(Exception e) {
       if (addSectionField()) {
         log.normal("added Section Field; updating.","Quiz:printQuiz");
         return res.getString("str_tbl_updated");
       }
       else if (addAQTable()) {
         log.normal("addAQTable true.","Quiz:printQuiz");
         return res.getString("str_tbl_updated");
       }
       else if (addEssayTable()) {
         log.normal("addEssayTable true.","Quiz:printQuiz");
         return res.getString("str_tbl_updated");
       }
       log.sparse("Caught exception " + e.getMessage() + " while " + student.getIDNumber() + " was working","Quiz:printQuiz");
       return e.getMessage();
    }
    buf.append("<input type=hidden name='Code' value=" + code + ">");
    buf.append("<input type=hidden name='PossibleScore' value=" + possibleScore + ">");
    buf.append("<input type=hidden name='AssignmentNumber' value=" + assignmentNumber + ">");
    buf.append("<input type=hidden name='UserRequest' value='GradeQuiz'>");
    buf.append("<input type=submit value='" + res.getString("str_compute_score") + "'>");
    buf.append("</form>");
    log.paranoid("Ending Method for " + student.getIDNumber(),"Quiz:printQuiz");
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

  String gradeQuiz(Student student, int assignmentNumber, int possibleScore, int code, HttpServletRequest request) {
    log.paranoid("Beginning method for " + student.getIDNumber(),"Quiz:gradeQuiz");
    boolean hasEssayQuestion=false;
    int studentScore = 0; // returned value
    StringBuffer buf = new StringBuffer();

    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    buf.append("<h3>" + res.getString("str_quiz") + " " + assignmentNumber + "</h3>");
    
    log.paranoid("Beginning try for " + student.getIDNumber(),"Quiz:gradeQuiz");
    try {
      boolean someAnswersWrong = false;
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM QuizQuestions WHERE AssignmentNumber='" + assignmentNumber + "'";
      log.paranoid("Executing query: " + sqlQueryString + " for " + student.getIDNumber(),"Quiz:gradeQuiz");
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
      log.paranoid("Begin main while loop for " + student.getIDNumber(),"Quiz:gradeQuiz");
      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        String studentAnswer[] = request.getParameterValues(question.getID());
        if (studentAnswer != null) {
          log.paranoid("studentAnswer wasn't null:" + question.getID() + ":" + student.getIDNumber(),"Quiz:gradeQuiz");
          for (int i = 1; i < studentAnswer.length; i++)
            studentAnswer[0] += studentAnswer[i];
          int pts = question.getPointValue();
          if (question.getQuestionType()==6){ 
          log.paranoid("Essay question for " + student.getIDNumber(),"Quiz:gradeQuiz");
            if(!(studentAnswer[0].equals(""))){
              log.paranoid("Answer not blank for " + student.getIDNumber(),"Quiz:gradeQuiz");
//if !hasScore guards against students attempting to resubmit a previously 
//submitted quiz.
              if (!hasScore(code)){
                log.paranoid(student.getIDNumber() + " didn't have a score.","Quiz:gradeQuiz");
                hasEssayQuestion=true;
                log.paranoid("Recording essay for " + student.getIDNumber(),"Quiz:gradeQuiz");
                recordEssay(student.getIDNumber(), question.getID(), studentAnswer[0],code); 
              }
            }
            continue;
          }
          if (question.isCorrect(studentAnswer[0])) studentScore += pts;
          else if (showMissedQuestions) {  // print a list of questions answered incorrectly:
            log.paranoid(student.getIDNumber() + " answered " + question.getID() + " incorrectly.","Quiz:gradeQuiz");
            if (!someAnswersWrong) { 
              log.paranoid("First wrong answer for " + student.getIDNumber(),"Quiz:gradeQuiz");
              buf.append("<FONT COLOR=#FF0000><h4>" + res.getString("str_answered_incorrect") + "</h4>("
               + res.getString("str_adtl_questions") + ")</FONT><UL>");
              someAnswersWrong = true;
            }
            buf.append("<LI>" + question.print());
          }
          //It's been graded; say so and update student answer if tracking enabled.
          if (trackAnswers && !hasScore(code)){
            log.paranoid("trackAnswers-true; didn't have score:" + student.getIDNumber(),"Quiz:gradeQuiz");
            stmt.executeUpdate("UPDATE QuizAssignedQuestions SET Graded='"
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
      MessageFormat mf = new MessageFormat(res.getString("str_final_score"));
      Object[] args = {
        new Integer(studentScore),
        new Integer(possibleScore)
      };
      buf.append(mf.format(args));
    } catch(Exception e) {
      log.sparse("Exception: " + e.getMessage() + " caught for " + student.getIDNumber(),"Quiz:gradeQuiz");
      buf.append(res.getString("str_err_msg") + " " + e.getMessage());
    }
    log.paranoid(student.getIDNumber() + "earned " + studentScore + " out of " 
      + possibleScore + " points on " + assignmentNumber,"Quiz:gradeQuiz");
    if (eligibleForGrading(student,assignmentNumber,code,buf)) {
      log.paranoid(student.getIDNumber() + " was eligible for grading.","Quiz,gradeQuiz");
      if (possibleScore > 0 && studentScore == possibleScore) buf.append("<h2>" + res.getString("str_grat_perfect") + "</h2>");
      log.paranoid("Attempting to record score for " + student.getIDNumber(),"Quiz:gradeQuiz");
      buf.append(recordScore(code,assignmentNumber,studentScore,possibleScore,student,request.getRemoteAddr()));
      if (hasEssayQuestion){
        log.paranoid(student.getIDNumber() + " has an essay question.","Quiz:gradeQuiz");
        buf.append(recordEssayScore(code,assignmentNumber,student,request.getRemoteAddr()));
      }
    }
    else {
      log.paranoid(student.getIDNumber() + "'s score was -NOT- recorded in the class database.","Quiz:gradeQuiz");
      buf.append("<BR>" + res.getString("str_score_not_recorded"));
//if the score isn't recorded, the teacher will never see the score; Hence, there's really
//no point in keeping those answers around.  
      if (trackAnswers && !hasScore(code)){
        log.paranoid("Didn't record " + student.getIDNumber() + "'s score, so deleting from QuizAssignedQuestions where code=" + code,"Quiz:gradeQuiz");
        dbSQLRequest("DELETE FROM QuizAssignedQuestions WHERE Code='"+code+"'");
      }
      if (hasEssayQuestion && !hasScore(code)){
        log.paranoid("Didn't record " + student.getIDNumber() + "'s score, so deleting from Essays WHERE code=" + code,"Quiz:gradeQuiz");
        dbSQLRequest("DELETE FROM Essays WHERE Code='"+code+"' AND TestType='Quiz'");
      }
    }
    log.paranoid("End method for " + student.getIDNumber(),"Quiz:gradeQuiz");
    return buf.toString();
  }
  
//added 10/8/02 to protect against scores being overwritten by students
//attempting to resubmit an instance of a quiz.
  boolean hasScore(int code){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsResult = stmt.executeQuery("SELECT * FROM Scores WHERE Code='"+code+"' AND TestType='Quiz'");
      if (rsResult.next()) {
        log.paranoid("Score exists.","Quiz:hasScore");
        return true; 
      }
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      log.normal("Caught exception " + e.getMessage(),"Quiz:hasScore");
      return false;
    }
    return false;
  }

  int getEssayPoints(Student student, int assignmentNumber) {
    int essayPoints=0;
    log.paranoid("Begin Try for " + student.getIDNumber(),"Quiz:getEssayPoints");
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
      //the "is not null" keeps the current quiz attempt from being included.
      ResultSet rsCodes=stmt.executeQuery("SELECT Code FROM QuizTransactions "
      + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND "
      + "AssignmentNumber=" + assignmentNumber + " AND StudentScore IS NOT NULL");
      //if there are no codes, this is the first attempt at the assignment anyway, and there won't be any essays to speak of.
      if (!rsCodes.next()) {
        log.paranoid("No essay points for " + student.getIDNumber() + ".","Quiz:getEssayPoints");
        return 0;
      }
      String sqlQueryString="SELECT Score FROM Essays WHERE StudentIDNumber='"
      + student.getIDNumber() + "' AND TestType='Quiz' AND Graded='true' AND (";
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
      log.paranoid("Executing query: " + sqlQueryString,"Quiz:getEssayPoints");
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      while (rsEssays.next()){
        essayPoints+=rsEssays.getInt("Score");
      }
    }catch(Exception e){
      log.sparse("Caught exception " + e.getMessage(),"Quiz:getEssayPoints");
      return 0;
    }
    return essayPoints; 
  }

  String recordScore(int code, int assignmentNumber, int studentScore, int possibleScore,Student student,String ipAddress) {
    log.paranoid("Begin method for " + student.getIDNumber(),"Quiz:recordScore");
    studentScore+=getEssayPoints(student,assignmentNumber);
    String sqlUpdateString = "UPDATE QuizTransactions SET StudentScore='" + studentScore
    + "',PossibleScore='" + possibleScore + "',ElapsedMinutes='" + elapsedMinutes(code) + "'"
    + " WHERE ((Code="+code+") AND (StudentScore IS NULL))";
    
    String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType) Values ('" + student.getIDNumber() 
    + "','Quiz" 
    + (assignmentNumber<10?"0":"")  // inserts leading zero Quiz01 - Quiz09 for correct ordering in gradebook
    + assignmentNumber + "','" + studentScore + "','" + ipAddress + "','" 
    + code + "','Quiz')";
    log.paranoid("Executing update: " + sqlUpdateString + " and Insert:" + sqlInsertString + " for " + student.getIDNumber(),"Quiz:recordScore");
    if (dbSQLRequest(sqlUpdateString)==1 && dbSQLRequest(sqlInsertString)==1) {//successful
      log.paranoid("Update and insert for " + student.getIDNumber() + " successful . . . supposedly.","Quiz:recordScore");
      return "<BR>" + res.getString("str_score_recorded");
    }
//if error, maybe from old version of Eledge that doesn't have Code and Type
//in Scores table. So, after adding Code and Type, try request again.
//if -still- no good; unexpected error(note, since update already done above, shouldn't do here.
    if (addCodeType()){
      if(dbSQLRequest(sqlInsertString)==1) {
        log.paranoid("Student score recorded after addCodeType for " + student.getIDNumber(),"Quiz:recordScore");
        return "<BR>" + res.getString("str_score_recorded");
      }
    }  
    log.paranoid("Student: " + student.getIDNumber() + "'s score -NOT- recorded, for \"unexpected reasons.\".","Quiz:recordScore");
    return assignmentNumber + " " + studentScore + res.getString("str_err_score_not_recorded");
    }

    int dbSQLRequest(String sqlString) {
    log.paranoid("Begin method.","Quiz:dbSQLRequest");
    int result=0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Executing: " + sqlString,"Quiz:dbSQLRequest");
      result = stmt.executeUpdate(sqlString);
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage() + " while executing: " + sqlString,"Quiz:dbSQLRequest");
    }
    log.paranoid("End method.","Quiz:dbSQLRequest");
    return result;
  }

  void recordEssay(String studentID, String questionID, String studentAnswer, int code) {
//so, this, eventually, will record the student's essay in the essays table.   
    log.paranoid("Recording essay for " + studentID,"Quiz:recordEssasy");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlUpdateString="INSERT INTO Essays VALUES (" + code + ",'";
      sqlUpdateString+=questionID + "','false','";
      sqlUpdateString+=CharHider.quot2literal(studentAnswer) + "','" + studentID + "','Quiz','0')";
      stmt.executeUpdate(sqlUpdateString);
      stmt.close();
      conn.close();
    }
    catch(Exception e){
      log.paranoid("Caught: " + e.getMessage() + " trying addEssayTable","Quiz:recordEssasy");
      addEssayTable();
    }
    log.paranoid("End method","Quiz:recordEssay");
  }

  String recordEssayScore(int code, int assignmentNumber, Student student, String ipAddress){
    int nofquestions=0;
    log.paranoid("Begin try for " + student.getIDNumber(),"Quiz:recordEssayScore");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCount=stmt.executeQuery("SELECT COUNT(*) FROM QuizQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int questionID[];
      if (rsCount.next())
         questionID=new int[rsCount.getInt("Count(*)")];
      else return res.getString("str_err_no_essay");
      rsCount.close();
      ResultSet rsQuestionID=stmt.executeQuery("SELECT QuestionID from QuizQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int i=0;
      while (rsQuestionID.next()){
        questionID[i++]=rsQuestionID.getInt("QuestionID"); 
      }
      rsQuestionID.close();
      ResultSet rsEssayQ=stmt.executeQuery("SELECT * from Essays WHERE Code='" + code + "' AND TestType='Quiz' ORDER BY QuestionID");
      while (rsEssayQ.next()){
        int id=-1;
        for(int j=0; j<i;j++){
          if (questionID[j]==rsEssayQ.getInt("QuestionID")){
             id=j;
             break;
          }
        }
        String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType) VALUES ('" + student.getIDNumber()
        + "','Quiz" + (assignmentNumber<10?"0":"")
        + assignmentNumber + "Essay" + (id<10?"0":"") + (id+1) + "','-" 
        + questionID[id] + "','" + ipAddress + "','" + code + "','Quiz')";
        stmt.executeUpdate(sqlInsertString); 
        nofquestions++; 
      }  
    }catch(Exception e){
      log.sparse("Caught: " + e.getMessage() + " while " + student.getIDNumber() + " working","Quiz:recordEssayScore");
      return "<BR>" + res.getString("str_essay_record_error");
    }
    log.paranoid("Normal termination of method.","Quiz:recordEssayScore");
    return "<BR>" + (nofquestions>1?res.getString("str_plural_essay_processed"):res.getString("str_single_essay_processed"));
  }

  boolean eligibleForGrading(Student student, int assignmentNumber, int code, StringBuffer buf) {
    boolean eligible = true;  // initialize
    if (!allowMultipleTries) {
      if (nTries(student,assignmentNumber) > 1) {
        eligible = false;
        log.paranoid("Too many attempts at quiz" + assignmentNumber + " for " + student.getIDNumber() + "; not eligible for grading!","Quiz:eligibleForGrading");
        buf.append("<br>" + res.getString("str_one_attempt_only"));
      }
    }
    if (timeLimit != 0) {
      int minutes = elapsedMinutes(code);
      if (minutes > timeLimit) {
        eligible = false;
        log.paranoid(student.getIDNumber() + " took too long! Can't record score.","Quiz:eligibleForGrading");
        MessageFormat mf = new MessageFormat(res.getString("str_bad_timelimit"));
        Object[] args = {
          new Integer(minutes),
          new Integer(timeLimit),
        };
        buf.append("<br>" + mf.format(args));
      }
    }
    if (enforceDeadlines) {
      if (deadlinePassed(student,assignmentNumber)) {
        eligible = false;
        log.paranoid("Whoops! Deadline passed. Can't record " + student.getIDNumber() + "'s score.","Quiz:elibigleForGrading");
        buf.append("<br>" + res.getString("str_deadline_passed"));
      }
    }
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM QuizTransactions WHERE Code='" + code + "' AND StudentScore IS NULL";
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (!rs.next()) {
        eligible = false;
        log.paranoid("Uh-oh! " + student.getIDNumber() + " tried the ol' go back, change answers, and resubmit, it would seem! (They've already submitted this instance of this quiz for grading.","Quiz:eligibleForGrading");
        buf.append("<br>" + res.getString("str_previously_submitted")); 
      }
      else // there is an unscored quiz; if ineligible to score it, set the score to zero
        if (!eligible) 
          stmt.executeUpdate("UPDATE QuizTransactions SET StudentScore=0 WHERE Code='" + code + "'");
    }
    catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage() + " for " + student.getIDNumber() + "; might want to look into that.","Quiz:eligibleForGrading");
      eligible = false;
    }
    log.paranoid("returning: " + eligible + " for " + student.getIDNumber(),"Quiz:eligibleForGrading");
    return eligible;
  }

  int nTries(Student student, int assignmentNumber) {
    int n = 0;
    log.paranoid("Begin try; " + student.getIDNumber(),"Quiz:nTries");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM QuizTransactions WHERE (AssignmentNumber='" + assignmentNumber + "' AND StudentIDNumber='" + student.getIDNumber() + "')";
      log.paranoid("Executing: " + sqlQueryString,"Quiz:nTries");
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      while (rs.next()) {
        n++;
        log.paranoid("In while loop. N: " + n + " for " + student.getIDNumber(),"Quiz:nTries");
      }
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      log.sparse("Caught exception " + e.getMessage() + " for " + student.getIDNumber(),"Quiz:nTries");
    }
    log.paranoid("returning n = " + n + " for " + student.getIDNumber(),"Quiz:nTries");
    return n;
  }

  int elapsedMinutes(int code){
    Timestamp dateTaken=null;
    int ret;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM QuizTransactions WHERE Code=" + code;
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (rs.next()) dateTaken=rs.getTimestamp("Date");
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      log.sparse("Caught exception " + e.getMessage(),"Quiz:elapsedMinutes");
    }
    log.paranoid("dateTaken: " + dateTaken.getTime(),"Quiz:elapsedMinutes");
    Date now = new Date();
    log.paranoid("now: " + now.getTime(),"Quiz:elapsedMinutes");
    long elapsedMilliseconds = now.getTime() - dateTaken.getTime();
    log.paranoid("elapsedMilliseconds: " + elapsedMilliseconds,"Quiz:elapsedMinutes");
    ret = (int)(elapsedMilliseconds/60000); 
    log.paranoid("Returning: " + ret,"Quiz:elapsedMinutes");
    return ret;
  }

  boolean deadlinePassed(Student student,int assignmentNumber) {
    Timestamp deadline = null;
    int sectionID = student.sectionID;

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsParams.next()) numberOfSections = rsParams.getInt("Value");
      String sectionName;
      ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + sectionID + "'");
      if (rsSections.next())
        sectionName = rsSections.getString("SectionName");
      else {
        sectionID=1;
        sectionName = "";        
      }
      
      ResultSet rsDeadline;
      try {  // get quiz information for student's section, if it exists
        rsDeadline = stmt.executeQuery("SELECT Deadline" + sectionID + " AS Deadline FROM QuizInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        log.paranoid("Caught exception " + e.getMessage(),"Quiz:deadlinePassed");
        rsDeadline = stmt.executeQuery("SELECT Deadline1 AS Deadline FROM QuizInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      if (rsDeadline.next()) deadline = rsDeadline.getTimestamp("Deadline");
    }
    catch(Exception e) {
      log.paranoid("Caught exception " + e.getMessage(),"Quiz:deadlinePassed");
      return true; // if database error occurs, assume deadline has passed
    }
    Date now = new Date();
    return deadline.before(now);  // returns true if deadline has passed
  }
  
  void getQuizParameters() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT * FROM QuizParameters");
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
      log.normal("Caught exception " + e.getMessage() + " tryig addParam()","Quiz:getParams");
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
      stmt.executeUpdate("ALTER TABLE QuizParameters ADD TrackAnswers VARCHAR(5)");
      stmt.executeUpdate("UPDATE QuizParameters SET TrackAnswers='false'");
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
      stmt.executeUpdate("ALTER TABLE QuizQuestions ADD (Section VARCHAR(3) DEFAULT 'All')");
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
      stmt.executeUpdate("CREATE TABLE QuizAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
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
      stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT)");
    }catch(Exception e){
      return false;
    }
    return true;
  }

  String createQuizTables(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE QuizInfo (AssignmentNumber INT PRIMARY KEY AUTO_INCREMENT,"
      + "Title TEXT,NSubjectAreas INT,NQuestionsPerSubjectArea INT,Deadline1 DATETIME)");
      stmt.executeUpdate("CREATE TABLE QuizQuestions (QuestionID INT PRIMARY KEY AUTO_INCREMENT,"
      + "AssignmentNumber INT,SubjectArea INT,QuestionText TEXT,QuestionType TEXT,NumberOfChoices INT,"
      + "ChoiceAText TEXT,ChoiceBText TEXT,ChoiceCText TEXT,ChoiceDText TEXT,ChoiceEText TEXT,"
      + "RequiredPrecision DOUBLE,CorrectAnswer1 TEXT,CorrectAnswer2 TEXT,QuestionTag TEXT,PointValue INT, Section VARCHAR(3) DEFAULT 'All')");
      stmt.executeUpdate("CREATE TABLE QuizTransactions (StudentIDNumber VARCHAR(50),LastName TEXT,"
      + "FirstName TEXT,AssignmentNumber INT,Date TIMESTAMP,Code INT,StudentScore INT,PossibleScore INT,"
      + "ElapsedMinutes INT,IPNumber VARCHAR(15))");
      stmt.executeUpdate("CREATE TABLE QuizParameters (TimeLimit INT,WaitForNewDownload INT,"
      + "EnforceDeadlines VARCHAR(5),AllowMultipleTries VARCHAR(5),ScrambleQuestions VARCHAR(5),"
      + "AllowWorkAhead VARCHAR(5),ShowMissedQuestions VARCHAR(5),UseSectionDeadlines VARCHAR(5),TrackAnswers VARCHAR(5))");
//new table for keeping track of student's missed questions.
      stmt.executeUpdate("CREATE TABLE QuizAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
//new table for essay questions. Probably not many of these, so we only need one
//table for all test types. So put in try/catch for case of already created.
      try{
        stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT)");
      }catch(Exception e){
      }
      stmt.executeUpdate("INSERT INTO QuizParameters VALUES (" + timeLimit + "," + waitForNewDownload 
      + ",'" + enforceDeadlines + "','" + allowMultipleTries + "','" + scrambleQuestions + "','" 
      + allowWorkAhead + "','" + showMissedQuestions + "','" + useSectionDeadlines + "','"       + trackAnswers + "')");
      return quizSelectForm(student);
    }
    catch (Exception e2) {
      return e2.getMessage();
    }
  }

  protected static String getUnsubmittedAssignments(String studentIDNumber) {
    StringBuffer buf = new StringBuffer("");
    boolean hasUnsubmitted=false;
    RBStore r = EledgeResources.getQuizBundle();
    try {
      buf.append("<table border=1 cellspacing=0><thead><b>" + r.getString("str_quizzes") + "</b></thead>");
      buf.append("<TR><TH>" + r.getString("str_assignment") + "</th><th>" 
      + r.getString("str_status") + "</th></tr>");
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM QuizInfo");
      if (!rsInfo.isBeforeFirst()){
        return "";
      }
      while (rsInfo.next()) {
        String assignmentName = "Quiz" + ((rsInfo.getInt("AssignmentNumber")<10)?"0":"") + rsInfo.getString("AssignmentNumber");
        String sqlQueryString = "SELECT * FROM Scores WHERE StudentIDNumber='" + studentIDNumber + "' AND Assignment='" + assignmentName + "'";
        ResultSet rsAssignment=stmt2.executeQuery(sqlQueryString);
        if (!rsAssignment.next()){
          if (!hasUnsubmitted) hasUnsubmitted=true;
          buf.append("\n<TR><TD>" + assignmentName + "</TD><TD>" + r.getString("str_unsubmitted") + "</TD></TR>");
        }
      }
    }catch(Exception e){
      return "";
    }
    if (!hasUnsubmitted)
      buf.append("</tr><td colspan=2>" + r.getString("str_quizzes_complete") + "</td></tr>");
    buf.append("</TABLE>");
    return buf.toString();
  }
}
