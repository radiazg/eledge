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

public class Homework extends HttpServlet {
  // parameters that determine the properties of the homework program:
  
  int nSubjectAreas = 1;               // default number of subject areas for homework overridden by values read from HomeworkInfo database
  int nQuestionsPerSubjectArea = 100;   // number of questions presented in each area also overridden in method printHomework()
  int timeLimit = 0;                   // minutes; set to zero for no time limit to complete the homework
  int waitForNewDownload = 0;          // minutes; set to zero for unlimited rate of homework downloads
  boolean enforceDeadlines = true;     // true means that homework score is not recorded after the deadline
  boolean allowMultipleTries = true;   // false allows only one attempt at each homework; true is recommended
  boolean scrambleQuestions = true;    // false presents the same questions each attempt; true is recommended
  boolean allowWorkAhead = false;      // true makes every homework available; false makes available after the deadline for the previous homework expires
  boolean showMissedQuestions = true;  // true reveals questions that were answered incorrectly as part of the grading process
  boolean useSectionDeadlines = false; // true uses default deadlines for all sections of the course
  int numberOfSections = 1;
  boolean trackAnswers= false; //true keeps track of missed questions for students.... warning: setting this to true has the potential of racking up hd space in a hurry. Use at your own discression. RDZ.
  private RBStore res = EledgeResources.getHomeworkBundle();  
  private Logger log = new Logger();

  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Homework");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Homework",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }

    StringBuffer error = new StringBuffer();
    getHomeworkParameters();
    if (timeLimit>0 && session.getMaxInactiveInterval()<(timeLimit*60 + 300))
      session.setMaxInactiveInterval(timeLimit*60 + 300);
    out.println(Page.create(homeworkSelectForm(student)));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Homework");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Homework",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }

    getHomeworkParameters();

    // find out what the user wants
    String userRequest = request.getParameter("UserRequest");

    if (userRequest==null) {  // Print a form for the student to select a homework:
      out.println(Page.create(homeworkSelectForm(student)));
      return;
    }
    
    int assignmentNumber=-1;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      out.println(Page.create(res.getString("str_must_select_valid")));
      return;
    }

    if (userRequest.equals("NewHomework")) {
      if (okTimeForNewHomework(student,assignmentNumber,out))
      out.println(Page.create(printHomework(student,assignmentNumber,request.getRemoteAddr())));
      return;
    }
  
    if (userRequest.equals("GradeHomework")) {
      int code = Integer.parseInt(request.getParameter("Code"));
      int possibleScore = Integer.parseInt(request.getParameter("PossibleScore"));
      out.println(Page.create(gradeHomework(student,assignmentNumber,possibleScore,code,request)));
      return;
    }

    out.println(Page.create(res.getString("str_no_understand")));
  }

  String homeworkSelectForm(Student student) {
    StringBuffer buf = new StringBuffer();
    int sectionID = student.sectionID;
    buf.append("<b>" + student.getFullName() + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
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
      if (useSectionDeadlines) buf.append("<br>Section " + sectionName);
      else sectionID=1;
      
      Timestamp deadline;
      SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a");

      buf.append("<form method=POST");
      if (!allowMultipleTries){
        buf.append(" onSubmit=\"return confirm('" + res.getString("str_confirm_submit") + "')\"");
      }
      buf.append(">");
      buf.append(res.getString("str_select_hw") + "<br>");
      buf.append("<table cellspacing=0 border=1>");
      buf.append("<tr><td></td><td><b>" + res.getString("str_field_hw")
      + "</b></td><td><b>" + res.getString("str_field_title") + "</b></td><td><b>"
      + res.getString("str_field_deadline") + "</b></td></tr>");

      int assignmentNumber;
      boolean previousHomeworkExpired = true;
        ResultSet rs;
      try {  // get homework information for student's section, if it exists
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline" + sectionID + " AS Deadline FROM HomeworkInfo ORDER BY DEADLINE");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        rs = stmt.executeQuery("SELECT AssignmentNumber,Title,Deadline1 AS Deadline FROM HomeworkInfo ORDER BY DEADLINE");
      }
      while (rs.next()) {
        assignmentNumber = rs.getInt("AssignmentNumber");
        deadline = rs.getTimestamp("Deadline");
        if (deadline.before(now))  // this is an expired homework
          buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        else if (previousHomeworkExpired) { // this is the current homework
          // no highlighting: buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
          buf.append("<tr BGCOLOR=FFFF00><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + " CHECKED></td>");
          previousHomeworkExpired = false;
        }
        else if (allowWorkAhead || student.getIsInstructor()) { // future homework is made available
            buf.append("<tr><td><input type=radio name='AssignmentNumber' value=" + assignmentNumber + "></td>");
        }
        else { // this homework is not currently available but is listed to show the deadline information
            buf.append("<tr><td align=center><font color=FF0000 size=-2>n/a</font></td>"); // homework not available yet
        }
        buf.append("<td ALIGN=CENTER>" + assignmentNumber + "</td>");
        buf.append("<td>" + rs.getString("Title") + "</td>");
        buf.append("<td>" +  df.format(deadline) + "</td>");
        buf.append("</tr>");
      }
    } 
    catch (Exception e) {
      return createHomeworkTables(student);
    }
    buf.append("</table>");
    buf.append("<input type=hidden name='UserRequest' value='NewHomework'>");
    buf.append("<input type=submit value='" + res.getString("str_btn_display_hw") + "'>");

    buf.append("</form>");

    if (student.getIsInstructor()) {
      buf.append("<FORM ACTION=" + Course.name + ".ManageHomework>"
      + "<b>" + res.getString("str_teach_only") 
      + "</b><input type=submit value='" + res.getString("str_btn_manage_hw")
      + "'></FORM>");
    }

    buf.append(homeworkRules());
    return buf.toString();
  }

  String homeworkRules () {
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat("str_rule_deadline");
    Object[] args = {
      new Integer(timeLimit)
    };
    buf.append("<h2>" + res.getString("str_title_rules") + "</h2>");
    buf.append("<ul>");
    if (enforceDeadlines)
      buf.append("<li>" + res.getString("str_rule_deadline") + "</li>");
    if (allowMultipleTries)
      buf.append("<li>" + res.getString("str_rule_allow_multiple") + "</li>");
    else
      buf.append("<li>" + res.getString("str_rule_deny_multiple1") + "<i>"
        + res.getString("str_rule_deny_multiple2") + "</i></li>");
    if (timeLimit != 0)
      buf.append("<li>" + mf.format(args) + "</li>");
    mf.applyPattern(res.getString("str_rule_dlrate"));
    args[0] = new Integer(waitForNewDownload);
    if (waitForNewDownload > 0)
      buf.append("<li>" + mf.format(args) + "</li>");
    buf.append("</ul>");
    
    return buf.toString();  
  }

  boolean okTimeForNewHomework(Student student, int assignmentNumber, PrintWriter out) {
    if (waitForNewDownload==0) return true;
    if (student.getIsInstructor()) return true;
    ResultSet rsTimestamp;
    Statement stmt;
    boolean returnValue;
    MessageFormat mf = new MessageFormat(res.getString("str_must_wait"));
    Object[] args = {new Integer(waitForNewDownload)};
    Calendar now = Calendar.getInstance();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      stmt = conn.createStatement();
      rsTimestamp = stmt.executeQuery("SELECT MAX(Date-ElapsedMinutes*100) AS StartTime FROM HomeworkTransactions "
       + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND AssignmentNumber='" + assignmentNumber + "' GROUP BY StudentIDNumber");
      if (rsTimestamp.next()) {
        Calendar then = Calendar.getInstance();
        then.setTime((Date)rsTimestamp.getTimestamp("StartTime"));
        then.add(Calendar.MINUTE,waitForNewDownload);
        if (!now.after(then)) {  // not yet eligible for new homework
          String ret = mf.format(args) + "<br>";
          mf.applyPattern(res.getString("str_curr_time"));
          args[0] = now.getTime();
          ret+=mf.format(args) + "<br>";
          mf.applyPattern(res.getString("str_next_avail"));
          args[0] = then.getTime();
          ret+=mf.format(args) + "<br>" + res.getString("str_try_reloading");
    	  out.println(Page.create(ret));
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

  String printHomework(Student student,int assignmentNumber,String ipNumber) {
    int nRows = 0;
    int possibleScore = 0;
    int code;
    String sqlInsert;
    String title = "";
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_title_assignment"));
    Object[] args = new Object[2]; 
    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    Random rand = new Random();

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM HomeworkInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      if (rsInfo.next()) {
        nSubjectAreas = rsInfo.getInt("NSubjectAreas");
        nQuestionsPerSubjectArea = rsInfo.getInt("NQuestionsPerSubjectArea");
    	title = rsInfo.getString("Title");
      }
      args[0] = new Integer(assignmentNumber);
      args[1] = title;
      buf.append("<h3>" + mf.format(args) + "</h3>");
      buf.append("<form METHOD=POST onSubmit=\"return confirm('" + res.getString("str_confirm_grade") + "?')\">");
      buf.append("<OL>");
      rsInfo.close();

      for (int area=0;area<nSubjectAreas;area++) {
        Vector questions = new Vector();
        String sqlQueryString = "SELECT * FROM HomeworkQuestions WHERE AssignmentNumber='" + assignmentNumber + "' AND SubjectArea='" + area + "' AND (Section='All' OR Section='" + student.sectionID + "') ORDER BY QuestionID";
        ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
        while (rsQuestions.next()) {
          Question question = new Question();
          question.loadQuestionData(rsQuestions);
          questions.addElement(question);
        }
        rsQuestions.close();

        int nQuestions = nQuestionsPerSubjectArea<questions.size()?nQuestionsPerSubjectArea:questions.size();
        for (int i = 0; i < nQuestions; i++){
          int q = scrambleQuestions?rand.nextInt(questions.size()):0;
          Question selected = (Question)questions.remove(q);
          if (selected.getQuestionType()==6)//essay questions.
          {
            //if it picks an essay question, skip it. Essay
            //questions are handled separately.
            if (nQuestionsPerSubjectArea < questions.size()){
              i--;
            }
            continue;
          }
          possibleScore += selected.getPointValue();
          //because rand.nextInto(int) returns a value between 0 inclussive and
          //(int), can set the currently unkown "code" to -1, and then
          //update the -1 entries to the correct code once the code is known.
          //First, though, make sure the class admin -wants- to track the questions.
          if (trackAnswers){
            String mySqlInsertString = "INSERT INTO HomeworkAssignedQuestions VALUES('-1'"
             +",'" + selected.getID() + "','" + selected.getQuestionGraded() + "','null')";
            stmt.executeUpdate(mySqlInsertString);
          }
          buf.append("\n<li>" + selected.print() + "</li>");
        }
      }
//order by keeps things consistent so that essay question "1" is always question
//1. (In this case, for the essay grading, it's necessary that the order be
//the same.
      String sqlQueryString="Select * from HomeworkQuestions WHERE AssignmentNumber='" + assignmentNumber + "' AND QuestionType='ESSAY' AND (Section='All' OR Section='" + student.sectionID + "') ORDER BY QuestionID";
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
      while(rsQuestions.next()){
        Statement stmt2=conn.createStatement();
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        sqlQueryString="SELECT * FROM Essays WHERE StudentIDNumber='" + student.getIDNumber() + "' AND TestType='Homework' AND QuestionID='" + question.getID() + "'";
        ResultSet rsHasAnswered=stmt2.executeQuery(sqlQueryString);        if (!rsHasAnswered.next()){
         buf.append("\n<li>" + question.print() + "</li>");        }
        rsHasAnswered.close();
        stmt2.close();
      }
      rsQuestions.close();
//moved the </ol> tag outside of the for loop. RDZ. 10/7/02
      buf.append("</ol>");
      stmt.close();
      conn.close();
    } catch(Exception e) {
       if (addSectionField())
         return res.getString("str_tbl_updated");
       if (addAQTable())
         return res.getString("str_tbl_updated");
       else if (addEssayTable())
         return res.getString("str_essaytbl_added");
       return e.getMessage();
    }

    do { // try to record the transaction until a unique random code works
      code = rand.nextInt(99999999);
      sqlInsert = "INSERT INTO HomeworkTransactions"
      + " (StudentIDNumber,LastName,FirstName,AssignmentNumber,Code,IPNumber)"
      + " VALUES ('"+student.getIDNumber() + "','" + converter(student.lastName)
      + "','" + converter(student.firstName) + "','" + assignmentNumber + "','" 
      + code + "','" + ipNumber + "')";
    } while (dbSQLRequest(sqlInsert) != 1);  // in case of accidental repeat of a code value

//change the -1 code to the correct code if trackAnswers enabled...
    if (trackAnswers){
        dbSQLRequest("UPDATE HomeworkAssignedQuestions SET Code='"+code+"' WHERE Code='-1'");
    }

    buf.append("<input type=hidden name='Code' value=" + code + ">");
    buf.append("<input type=hidden name='PossibleScore' value=" + possibleScore + ">");
    buf.append("<input type=hidden name='AssignmentNumber' value=" + assignmentNumber + ">");
    buf.append("<input type=hidden name='UserRequest' value='GradeHomework'>");
    buf.append("<input type=submit value='" + res.getString("str_btn_compute") + "'>");
    buf.append("</form>");

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

  String gradeHomework(Student student, int assignmentNumber, int possibleScore, int code, HttpServletRequest request) {
    boolean hasEssayQuestion=false;
    int studentScore = 0; // returned value
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_grade_title"));
    Object[] args = { new Integer(assignmentNumber), null };
    buf.append("<b>" + student.firstName + " " + student.lastName + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    buf.append("<h3>" + mf.format(args) + "</h3>");
    
    try {
      boolean someAnswersWrong = false;
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM HomeworkQuestions WHERE AssignmentNumber='" + assignmentNumber + "' ORDER BY SubjectArea,QuestionID";
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);
      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        String studentAnswer[] = request.getParameterValues(question.getID());
        if (studentAnswer != null) {
          for (int i = 1; i < studentAnswer.length; i++)
            studentAnswer[0] += studentAnswer[i];
          int pts = question.getPointValue();
          if (question.getQuestionType()==6){
            if(!(studentAnswer[0].equals(""))){
//if !hasScore guards against students attempting to resubmit a previously
//submitted quiz.
              if (!hasScore(code)){
                hasEssayQuestion=true;
                recordEssay(student.getIDNumber(), question.getID(), studentAnswer[0],code);
              }
            }
            continue;
          }
          if (question.isCorrect(studentAnswer[0])) studentScore += pts;
          else if (showMissedQuestions) {  // print a list of questions answered incorrectly:
            if (!someAnswersWrong) { 
              buf.append("<FONT COLOR=#FF0000><h4>" + res.getString("str_title_incorrect") + "</h4>"
               + res.getString("str_title_left_blank") + "</FONT><UL>");
              someAnswersWrong = true;
            }
            buf.append("<LI>" + question.print());
          }
          //It's been graded; say so and update student answer if tracking enabled.
          if (trackAnswers && !hasScore(code)){
            stmt.executeUpdate("UPDATE HomeworkAssignedQuestions SET Graded='"
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
      mf.applyPattern(res.getString("str_hw_score"));
      args[0] = new Integer(studentScore);
      args[1] = new Integer(possibleScore);
      buf.append(mf.format(args));
    } catch(Exception e) {
      buf.append(res.getString("str_err") + e.getMessage());
    }
    if (eligibleForGrading(student,assignmentNumber,code,buf)) {
      if (possibleScore > 0 && studentScore == possibleScore) buf.append("<h2>" + res.getString("str_perfect_score") + "</h2>");
      buf.append(recordScore(code,assignmentNumber,studentScore,possibleScore,student,request.getRemoteAddr()));
      if (hasEssayQuestion){
        buf.append(recordEssayScore(code,assignmentNumber,student,request.getRemoteAddr()));
      }
    }
    else {
      mf.applyPattern(res.getString("str_score_not_recorded"));
      args[0]="<b>";
      args[1]="</b>";
      buf.append("<BR>" + mf.format(args));
//if the score isn't recorded, the teacher will never see the score; Hence, there's really
//no point in keeping those answers around. RDZ
      if (trackAnswers && !hasScore(code)){
        dbSQLRequest("DELETE FROM HomeworkAssignedQuestions WHERE Code='"+code+"'");
      }
      if (hasEssayQuestion && !hasScore(code)){
        dbSQLRequest("DELETE FROM Essays WHERE Code='"+code+"' AND TestType='Homework'");
      }
    }
    return buf.toString();
  }

//added 10/8/02 to protect against scores being overwritten by students
//attempting to resubmit an instance of a homework.
  boolean hasScore(int code){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsResult = stmt.executeQuery("SELECT * FROM Scores WHERE Code='"+code+"' AND TestType='Homework'");
      if (rsResult.next())
        return true;
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      return false;
    }
    return false;
  }

  int getEssayPoints(Student student, int assignmentNumber) {
    int essayPoints=0;
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
      ResultSet rsCodes=stmt.executeQuery("SELECT Code FROM HomeworkTransactions "
      + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND "
      + "AssignmentNumber=" + assignmentNumber + " AND StudentScore IS NOT NULL");
      //if there are no codes, this is the first attempt at the assignment anyway, and there won't be any essays to speak of.
      if (!rsCodes.next())
        return 0;
      String sqlQueryString="SELECT Score FROM Essays WHERE StudentIDNumber='"
      + student.getIDNumber() + "' AND TestType='Homework' AND Graded='true' AND (";
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
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      while (rsEssays.next()){
        essayPoints+=rsEssays.getInt("Score");
      }
    }catch(Exception e){
      return 0;
    }
    return essayPoints;
  }

  String recordScore(int code, int assignmentNumber, int studentScore, int possibleScore,Student student,String ipAddress) {
    studentScore+=getEssayPoints(student,assignmentNumber);
    String sqlUpdateString = "UPDATE HomeworkTransactions SET StudentScore='" + studentScore
    + "',PossibleScore='" + possibleScore + "',ElapsedMinutes='" + elapsedMinutes(code) + "'"
    + " WHERE ((Code="+code+") AND (StudentScore IS NULL))";
    dbSQLRequest(sqlUpdateString);
    String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress, Code, TestType) Values ('" + student.getIDNumber() 
    + "','HW" 
    + (assignmentNumber<10?"0":"")  // inserts leading zero HW01 - HW09 for correct ordering in gradebook
    + assignmentNumber + "','" + studentScore + "','" + ipAddress + "','"
    + code + "','Homework')";
    if (dbSQLRequest(sqlInsertString)==1) //successful
      return "<BR>" + res.getString("str_score_recorded");
//if error, maybe from old version of Eledge that doesn't have Code and Type
//in Scores table. So, after adding Code and Type, try request again.
//if -still- no good; unexpected error(note, since update already done above, shouldn't do here.
    if (addCodeType()){
     if(dbSQLRequest(sqlInsertString)==1)
       return "<BR>" + res.getString("str_score_recorded");
    }

    return assignmentNumber + " " + studentScore + res.getString("str_score_record_err");
    }

    int dbSQLRequest(String sqlString) {
    int result=0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      result = stmt.executeUpdate(sqlString);
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
    }
    return result;
  }

  void recordEssay(String studentID, String questionID, String studentAnswer, int code) {
//so, this, eventually, will record the student's essay in the essays table.
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlUpdateString="INSERT INTO Essays VALUES (" + code + ",'";
      sqlUpdateString+=questionID + "','false','";
      sqlUpdateString+=CharHider.quot2literal(studentAnswer) + "','" + studentID + "','Homework','0')";
      stmt.executeUpdate(sqlUpdateString);
      stmt.close();
      conn.close();
    }catch(Exception e){
      addEssayTable();
    }
  }

  String recordEssayScore(int code, int assignmentNumber, Student student, String ipAddress){
    int nofquestions=0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCount=stmt.executeQuery("SELECT COUNT(*) FROM HomeworkQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int questionID[];
      if (rsCount.next())
         questionID=new int[rsCount.getInt("Count(*)")];
      else return "Error: no essay questions for for this assignment.";
      rsCount.close();
      ResultSet rsQuestionID=stmt.executeQuery("SELECT QuestionID from HomeworkQuestions WHERE QuestionType='ESSAY' AND AssignmentNumber='" + assignmentNumber + "'");
      int i=0;
      while (rsQuestionID.next()){
        questionID[i++]=rsQuestionID.getInt("QuestionID");
      }
      rsQuestionID.close();
      ResultSet rsEssayQ=stmt.executeQuery("SELECT * from Essays WHERE Code='" + code + "' AND TestType='Homework' ORDER BY QuestionID");
      while (rsEssayQ.next()){
        int id=-1;
        for(int j=0; j<i;j++){
          if (questionID[j]==rsEssayQ.getInt("QuestionID")){
             id=j;
             break;
          }
        }
        String sqlInsertString = "INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType) VALUES ('" + student.getIDNumber()
        + "','Homework" + (assignmentNumber<10?"0":"")
        + assignmentNumber + "Essay" + (id<10?"0":"") + (id+1) + "','-"
        + questionID[id] + "','" + ipAddress + "','" + code + "','Homework')";
        stmt.executeUpdate(sqlInsertString);
        nofquestions++;
      }
    }catch(Exception e){
      return "<BR>" + res.getString("str_record_essay_err");
    }
    
    return "<BR>" + (nofquestions>1?res.getString("str_record_essays"):res.getString("str_record_essay"));
  }

  boolean eligibleForGrading(Student student, int assignmentNumber, int code, StringBuffer buf) {
    boolean eligible = true;  // initialize
    MessageFormat mf = new MessageFormat(res.getString("str_too_long"));
    Object[] args = {
      null,
      new Integer(timeLimit)
    };
    if (!allowMultipleTries) {
      if (nTries(student,assignmentNumber) > 1) {
        eligible = false;
        buf.append("<br>" + res.getString("str_oneattempt"));
      }
    }
    if (timeLimit != 0) {
      int minutes = elapsedMinutes(code);
      args[0] = new Integer(minutes);
      if (minutes > timeLimit) {
        eligible = false;
        buf.append("<br>" + mf.format(args));
      }
    }
    if (enforceDeadlines) {
      if (deadlinePassed(student,assignmentNumber)) {
        eligible = false;
        buf.append("<br>" + res.getString("str_passed_deadline"));
      }
    }
/************** This section disabled for Homework servlet **********************
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM HomeworkTransactions WHERE Code='" + code + "' AND StudentScore IS NULL";
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (!rs.next()) {
        eligible = false;
        buf.append("<br>You previously submitted this particular homework for grading.  To receive credit you must download a fresh homework and try again.");
      }
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      eligible = false;
    }
********************************************************************************/
    return eligible;
  }

  int nTries(Student student, int assignmentNumber) {
    int n = 0;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM HomeworkTransactions WHERE (AssignmentNumber='" + assignmentNumber + "' AND StudentIDNumber='" + student.getIDNumber() + "')";
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      while (rs.next()) {
        n++;
      }
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
    }
    return n;
  }

  int elapsedMinutes(int code){
    log.paranoid("Begin Method.","Exam:elapsedMinutes");
    Timestamp dateTaken=null;
    log.paranoid("dateTaken (should be null): " + dateTaken,"Homework:elapsedMinutes");
    int ret;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM HomeworkTransactions WHERE Code=" + code;
      log.paranoid("executing: " + sqlQueryString,"Homework:elapsedMinutes");
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (rs.next()) dateTaken=rs.getTimestamp("Date");
      rs.close();
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
    }
    log.paranoid("dateTaken: " + dateTaken,"Homework:elapsedMinutes");
    log.paranoid("dT in millis: " + dateTaken.getTime(),"Homework:elapsedMinutes");
    Date now = new Date();
    log.paranoid("now: " + now,"Homework:elapsedMinutes");
    log.paranoid("now in millis: " + now.getTime(),"Homework:elapsedMinutes");
    long elapsedMilliseconds = now.getTime() - dateTaken.getTime();
    log.paranoid("elapsedMillis: " + elapsedMilliseconds,"Homework:elapsedMinutes");
    ret = (int)(elapsedMilliseconds/60000);
    log.paranoid("returning: " + ret,"Homework:elapsedMinutes");
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
      if (!useSectionDeadlines) sectionID=1;
      
      ResultSet rsDeadline;
      try {  // get homework informatino for student's section, if it exists
        rsDeadline = stmt.executeQuery("SELECT Deadline" + sectionID + " AS Deadline FROM HomeworkInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      catch (Exception e) {  // otherwiese, get the info for section 1
        rsDeadline = stmt.executeQuery("SELECT Deadline1 AS Deadline FROM HomeworkInfo WHERE AssignmentNumber='" + assignmentNumber + "'");
      }
      if (rsDeadline.next()) deadline = rsDeadline.getTimestamp("Deadline");
    }
    catch(Exception e) {
      return true; // if database error occurs, assume deadline has passed
    }
    Date now = new Date();
    return deadline.before(now);  // returns true if deadline has passed
  }
  
  void getHomeworkParameters() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT * FROM HomeworkParameters");
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
      stmt.executeUpdate("ALTER TABLE HomeworkParameters ADD TrackAnswers VARCHAR(5)");
      stmt.executeUpdate("UPDATE HomeworkParameters SET TrackAnswers='false'");
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
      stmt.executeUpdate("ALTER TABLE HomeworkQuestions ADD (Section VARCHAR(3) DEFAULT 'All')");
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
      stmt.executeUpdate("CREATE TABLE HomeworkAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
    }catch(Exception e){
      return false;
    }
    return true;
  }
 
  boolean addEssayTable(){
    try {
      Class.forName(Course.jdbcDriver).newInstance();      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT)");
    }catch(Exception e){
      return false;
    }
    return true;  }

  String createHomeworkTables(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE HomeworkInfo (AssignmentNumber INT PRIMARY KEY AUTO_INCREMENT,"
      + "Title TEXT,NSubjectAreas INT,NQuestionsPerSubjectArea INT,Deadline1 DATETIME)");
      stmt.executeUpdate("CREATE TABLE HomeworkQuestions (QuestionID INT PRIMARY KEY AUTO_INCREMENT,"
      + "AssignmentNumber INT,SubjectArea INT,QuestionText TEXT,QuestionType TEXT,NumberOfChoices INT,"
      + "ChoiceAText TEXT,ChoiceBText TEXT,ChoiceCText TEXT,ChoiceDText TEXT,ChoiceEText TEXT,"
      + "RequiredPrecision DOUBLE,CorrectAnswer1 TEXT,CorrectAnswer2 TEXT,QuestionTag TEXT,PointValue INT,Section VARCHAR(3) DEFAULT 'All')");
      stmt.executeUpdate("CREATE TABLE HomeworkTransactions (StudentIDNumber VARCHAR(50),LastName TEXT,"
      + "FirstName TEXT,AssignmentNumber INT,Date TIMESTAMP,Code INT,StudentScore INT,PossibleScore INT,"
      + "ElapsedMinutes INT,IPNumber VARCHAR(15))");
      stmt.executeUpdate("CREATE TABLE HomeworkParameters (TimeLimit INT,WaitForNewDownload INT,"
      + "EnforceDeadlines VARCHAR(5),AllowMultipleTries VARCHAR(5),ScrambleQuestions VARCHAR(5),"
      + "AllowWorkAhead VARCHAR(5),ShowMissedQuestions VARCHAR(5),UseSectionDeadlines VARCHAR(5), TrackAnswers VARCHAR(5))");
//new table for keeping track of student's missed questions.
      stmt.executeUpdate("CREATE TABLE HomeworkAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
//new table for essay questions. Probably not many of these, so we only need one
//table for all test types. So put in try/catch for case of already created.
      try{
        stmt.executeUpdate("CREATE TABLE Essays (Code INT, QuestionID INT, Graded VARCHAR(5), Answer TEXT, StudentIDNumber VARCHAR(50), TestType VARCHAR(8), Score INT)");
      }catch(Exception e){
      }
      stmt.executeUpdate("INSERT INTO HomeworkParameters VALUES (" + timeLimit + "," + waitForNewDownload 
      + ",'" + enforceDeadlines + "','" + allowMultipleTries + "','" + scrambleQuestions + "','" 
      + allowWorkAhead + "','" + showMissedQuestions + "','" + useSectionDeadlines + "','"
      + trackAnswers + "')");
      return homeworkSelectForm(student);
    }
    catch (Exception e2) {
      return e2.getMessage();
    }
  }
  protected static synchronized String getUnsubmittedAssignments(String studentIDNumber) {
    StringBuffer buf = new StringBuffer("");
    boolean hasUnsubmitted=false;
    RBStore r2 = EledgeResources.getHomeworkBundle();
    try {
      buf.append("<table border=1 cellspacing=0><thead><b>" + r2.getString("str_field_hw") + "</b></thead>");
      buf.append("<TR><TH>" + r2.getString("str_field_assignment") 
      + "</TH><TH>" + r2.getString("str_field_status") + "</TH></TR>");
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM HomeworkInfo");
      if (!rsInfo.isBeforeFirst()){
        return "";
      }
      while (rsInfo.next()) {
        String assignmentName = "HW" + ((rsInfo.getInt("AssignmentNumber")<10)?"0":"") + rsInfo.getString("AssignmentNumber");
        String sqlQueryString = "SELECT * FROM Scores WHERE StudentIDNumber='" + studentIDNumber + "' AND Assignment='" + assignmentName + "'";
        ResultSet rsAssignment=stmt2.executeQuery(sqlQueryString);
        if (!rsAssignment.next()){
          if (!hasUnsubmitted) hasUnsubmitted=true;
          buf.append("\n<TR><TD>" + assignmentName + "</TD><TD>"
          + r2.getString("str_status_unsubmitted") + "</TD></TR>");
        }
      }
    }catch(Exception e){
      return "";
    }
    if (!hasUnsubmitted)
      buf.append("</tr><td colspan=2>" + r2.getString("str_hw_done") + "</td></tr>");
    buf.append("</TABLE>");
    return buf.toString();
  }
}
