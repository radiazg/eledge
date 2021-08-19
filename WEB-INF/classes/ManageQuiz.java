package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageQuiz extends HttpServlet {

  // in this servlet, the index i is used to indicate the Assignment Number, 
  // and normally ranges from 0 to numberOfAssignments-1
  // conversely, the index j runs over the sections of the course, 
  // normally from 1 to numberOfSections.  The difference is due
  // to the fact that the auto_increment feature of mysql starts numbering 
  // the SectionID variable at 1 not 0.  Therefore, the first
  // and default sectionID number is 1.  

  RBStore res = EledgeResources.getManageQuizBundle();
  Logger log = new Logger();
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

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageQuiz");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageQuiz",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_instructor")));
      return;
    }
    // from here on, user is assumed to be the instructor
    out.println(Page.create(deadlinesForm()));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageQuiz");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageQuiz",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_instructor")));
      return;
    }
    // from here on, user is assumed to be the instructor
    StringBuffer error = new StringBuffer(" ");

    String userRequest = request.getParameter("UserRequest");
    if (userRequest == null) {
      out.println(Page.create(deadlinesForm()));
      return;
    }

    if (userRequest.equals("Update")) {
      updateDeadlines(request,error);
      out.println(Page.create(error + deadlinesForm()));
      return;
    }
    else if (userRequest.equals("AddAQuiz")) {
      addAQuiz();
      out.println(Page.create(deadlinesForm()));
      return;
    }

    int assignmentNumber = 0;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      out.println(Page.create(res.getString("str_must_select")));
      return;
    }

    if (userRequest.equals("DeleteQuiz")) {
      deleteQuiz(assignmentNumber);
      out.println(Page.create(deadlinesForm()));
      return;
    }
    else if (userRequest.equals("EditForm")) out.println(Page.create(editQuestionsForm(assignmentNumber)));
    else if (userRequest.equals("AddQuestionForm")) {
      out.println(Page.create(addQuestionForm(assignmentNumber)));
    }
    else if (userRequest.equals("AddQuestion")) {
      if (questionAddedSuccessfully(request,out))
        out.println(Page.create(res.getString("str_question_added_ok") + "<br>" + addQuestionForm(assignmentNumber)));
      else
        out.println(Page.create(res.getString("str_bad_info")));
    }
    else if (userRequest.equals("Edit")) {
      if (questionEditedSuccessfully(request,out))
        out.println(Page.create(res.getString("str_question_changed_ok") + "<br>" + editQuestionsForm(assignmentNumber)));
      else
        out.println(Page.create(res.getString("str_bad_info")));
    }
    else if (userRequest.equals("DeleteQuestion")) {
      deleteQuestion(request.getParameter("QuestionID"));
      out.println(Page.create(editQuestionsForm(assignmentNumber)));
    }
    else out.println(Page.create(deadlinesForm()));
  }

  String deadlinesForm() {
    int numberOfSections = 1;
    boolean useSectionDeadlines = false;
    String[] sectionNames = null;
    StringBuffer buf = new StringBuffer();
    Date now = new Date();

    buf.append("<h2>" + res.getString("str_title_info") + "</h2>");
    buf.append("<FORM METHOD=POST>");
    buf.append("<input type=hidden name=UserRequest value=Update>");
    int numberOfAssignments = 0;
      
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      if (numberOfSections == 1) useSectionDeadlines = false;

      ResultSet rsParams = stmt.executeQuery("SELECT * FROM QuizParameters");
      if (rsParams.next()) useSectionDeadlines = rsParams.getBoolean("UseSectionDeadlines");

      buf.append("\n<h3>" + res.getString("str_params") + "</h3><table>"
      + "\n<tr><td><b>" + res.getString("str_param") + "</b></td><td><b>"
      + res.getString("str_val") + "</b></td><td><b>" + res.getString("str_desc") + "</b></td></tr>"
      + "\n<tr><td>" + res.getString("str_time_limit") + "</td> <td><INPUT SIZE=3 NAME=TimeLimit VALUE=" 
      + rsParams.getInt("TimeLimit") + "></td><td>" + res.getString("str_explain_time") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_wait_time") +"</td><td><INPUT SIZE=3 NAME=WaitForNewDownload VALUE=" 
      + rsParams.getInt("WaitForNewDownload") + "></td><td>" + res.getString("str_explain_wait") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_deadline") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=EnforceDeadlines" 
      + (rsParams.getBoolean("EnforceDeadlines")?" CHECKED":"") + "></td><td>" + res.getString("str_explain_deadline") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_try_again") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=AllowMultipleTries" 
      + (rsParams.getBoolean("AllowMultipleTries")?" CHECKED":"") + "></td><td>" + res.getString("str_explain_again") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_scramble") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=ScrambleQuestions" 
      + (rsParams.getBoolean("ScrambleQuestions")?" CHECKED":"") + "></td><td>" + res.getString("str_explain_scramble") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_work_ahead") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=AllowWorkAhead" 
      + (rsParams.getBoolean("AllowWorkAhead")?" CHECKED":"") + "></td><td>" + res.getString("str_explain_ahead") + "</td></tr>"
      + "\n<tr><td>" + res.getString("str_show_missed") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=ShowMissedQuestions" 
      + (rsParams.getBoolean("ShowMissedQuestions")?" CHECKED":"") + "></td><td>" + res.getString("str_explain_show") + "</td></tr>"
      + (numberOfSections>1?"\n<tr><td>" + res.getString("str_sections") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=UseSectionDeadlines" 
      + (useSectionDeadlines?" CHECKED":"") + "></td><td>" + res.getString("str_explain_sections") + "</td></tr>":""));
      try{
         boolean track=rsParams.getBoolean("TrackAnswers");
         buf.append("\n<tr><td>" + res.getString("str_track") + "</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=TrackAnswers" 
         + (track?" CHECKED":"") + "></td><td>" + res.getString("str_explain_track") + "</td></tr>");
      }catch(Exception e){
         log.normal("Caught: " + e.getMessage(),"Quiz:deadlinesForm");
         if(addParam())
           return(res.getString("str_ta_added"));
         else return e.getMessage();
      }
      buf.append("\n</table>");

      numberOfSections = useSectionDeadlines?numberOfSections:1;
      sectionNames = new String[numberOfSections+1];
      ResultSet rsSectionNames = stmt.executeQuery("SELECT SectionName FROM CourseSections ORDER BY SectionID");
      for (int j=1;j<=numberOfSections;j++) 
        if (rsSectionNames.next()) sectionNames[j] = rsSectionNames.getString("SectionName");

      // print a header row to the quiz info table
      buf.append("\n<h3>" + res.getString("str_title_deadlines") + "</h3>");
      buf.append("\n<table border=1 cellspacing=0><tr><td>Select</td><td>Quiz</td>");  // print a header row to the table of quiz deadlines
      for (int j=1;j<=numberOfSections;j++) {
        if (useSectionDeadlines) buf.append("<td>" + res.getString("str_field_deadline") + "&nbsp;" + sectionNames[j] + "</td>");
        else buf.append("<td>" + res.getString("str_field_deadline") + "</td>");
      }
      buf.append("<td>" + res.getString("str_title") + "</td><td>" 
      + res.getString("str_subject_area") + "</td><td>" + res.getString("str_questions_per_subj") + "</td></tr>");

      // get the quiz information and the database table stucture
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM QuizInfo ORDER BY AssignmentNumber");
      ResultSetMetaData rsmd = rsInfo.getMetaData();
       
      String assignmentNumber;
      int i=0;
      while (rsInfo.next()) {  // print one row for each quiz
        String iStr = Integer.toString(i);
        assignmentNumber = rsInfo.getString("AssignmentNumber");
        buf.append("\n<tr><td><INPUT TYPE=RADIO NAME=AssignmentNumber VALUE='" + assignmentNumber + "'></td>");
        buf.append("<td><INPUT SIZE=5 NAME=" + iStr + ":AssignmentNumber VALUE='"
        + assignmentNumber + "'></td>");
        // print 1 column for each section; use values for section 1 if necessary; flag if expansion required
        for (int j=1;j<=numberOfSections;j++) {
          buf.append("<td><INPUT NAME=" + iStr + ":Deadline" + Integer.toString(j) + " VALUE='");
          try {
            buf.append(rsInfo.getString("Deadline" + Integer.toString(j)));
          }
          catch (Exception e) {
            buf.append(rsInfo.getString("Deadline1"));
          }
          buf.append("' SIZE=20></td>"); 
        }
        buf.append("<td><INPUT NAME=" + iStr + ":Title VALUE='" + rsInfo.getString("Title") + "'></td>");
        buf.append("<td><INPUT NAME=" + iStr + ":NSubjectAreas SIZE=4 VALUE='" + (rsInfo.getString("NSubjectAreas")==null?"1":rsInfo.getString("NSubjectAreas")) + "'></td>");
        buf.append("<td><INPUT NAME=" + iStr + ":NQuestionsPerSubjectArea SIZE=4 VALUE='" + (rsInfo.getString("NQuestionsPerSubjectArea")==null?"10":rsInfo.getString("NQuestionsPerSubjectArea")) + "'></td>");
        i++;
      }
      numberOfAssignments = i;
    }
    catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Quiz:deadlinesForm");
    }
    buf.append("\n</table><br>");
    buf.append("\n<INPUT TYPE=HIDDEN NAME=NumberOfAssignments VALUE=" + numberOfAssignments + ">");
    buf.append("\n<input type=submit value='" + res.getString("str_btn_update") + "'>&nbsp;");
    buf.append("<input type=reset value='" + res.getString("str_btn_reset") + "'><br>");
    buf.append("<input type=submit value='" + res.getString("str_btn_add") + "' "
    + "onClick=this.form.elements.UserRequest.value='AddAQuiz'>&nbsp;");
    buf.append("<input type=submit value='" + res.getString("str_btn_del") + "' "
    + "onClick=this.form.elements.UserRequest.value='DeleteQuiz'><br>");
    buf.append("<input type=submit value='" + res.getString("str_btn_questions") + "' "
    + "onClick=this.form.elements.UserRequest.value='EditForm'>");
    buf.append("</FORM>");

    return buf.toString();
  }

  void updateDeadlines(HttpServletRequest request, StringBuffer error) {
    int numberOfSections = 1;
    boolean useSectionDeadlines = request.getParameter("UseSectionDeadlines")==null?false:true;
    StringBuffer sqlUpdate = new StringBuffer("UPDATE QuizParameters SET ");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      sqlUpdate.append("TimeLimit='" + request.getParameter("TimeLimit") + "',"
      + "WaitForNewDownload='" + request.getParameter("WaitForNewDownload") + "',"
      + "EnforceDeadlines=" + (request.getParameter("EnforceDeadlines")==null?"'false',":"'true',")
      + "AllowMultipleTries=" + (request.getParameter("AllowMultipleTries")==null?"'false',":"'true',")
      + "ScrambleQuestions=" + (request.getParameter("ScrambleQuestions")==null?"'false',":"'true',")
      + "AllowWorkAhead=" + (request.getParameter("AllowWorkAhead")==null?"'false',":"'true',")
      + "ShowMissedQuestions=" + (request.getParameter("ShowMissedQuestions")==null?"'false',":"'true',")
      + "UseSectionDeadlines=" + (useSectionDeadlines?"'true',":"'false',")
      + "TrackAnswers=" + (request.getParameter("TrackAnswers")==null?"'false'":"'true'"));
      stmt.executeUpdate(sqlUpdate.toString());      

      ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      rsNSections.close();

      int nSectionsInfo =  stmt.executeQuery("SELECT * FROM QuizInfo ORDER BY AssignmentNumber").getMetaData().getColumnCount() - 4;
      String now = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
      for (int j=nSectionsInfo+1;j<=numberOfSections;j++) {
        stmt.executeUpdate("ALTER TABLE QuizInfo ADD COLUMN Deadline" + Integer.toString(j) + " DATETIME DEFAULT '" + now + "'");
      }
      
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM QuizInfo ORDER BY AssignmentNumber");
  
      int i=0;
      while (rsInfo.next()) {
        rsInfo.updateString("AssignmentNumber",request.getParameter(Integer.toString(i) + ":AssignmentNumber"));
        for (int j=1;j<=numberOfSections;j++) {
          String deadline = request.getParameter(Integer.toString(i) + ":Deadline" + Integer.toString(j));
          if (deadline != null) rsInfo.updateString("Deadline" + Integer.toString(j),deadline);
        }
        rsInfo.updateString("Title",request.getParameter(Integer.toString(i) + ":Title"));
        rsInfo.updateString("NSubjectAreas",request.getParameter(Integer.toString(i) + ":NSubjectAreas"));
        rsInfo.updateString("NQuestionsPerSubjectArea",request.getParameter(Integer.toString(i) + ":NQuestionsPerSubjectArea"));
        rsInfo.updateRow();
        i++;
      }
      rsInfo.close();
    }
    catch (Exception e) {
      error.append(e.getMessage());
    }
  }

  void addAQuiz() {
    Date now = new Date();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      StringBuffer sqlInsert = new StringBuffer("INSERT INTO "
      + "QuizInfo (Title,Deadline1,NSubjectAreas,NQuestionsPerSubjectArea) "
      + "VALUES ('New Quiz',now(),1,10)");
      stmt.executeUpdate(sqlInsert.toString());
    }
    catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"ManageQuiz:addAQuiz");
    }
  }

  void deleteQuiz(int assignmentNumber) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM QuizInfo WHERE AssignmentNumber=" + assignmentNumber);
    }
    catch (Exception e) {
    }
  }

  String addQuestionForm(int assignmentNumber) {
    StringBuffer buf = new StringBuffer();
    JSmethods jsm = new JSmethods(JSmethods.QUIZ);
    jsm.appendJSCheckHtml(buf);
    buf.append("<FORM METHOD=POST><input type=hidden name=UserRequest value=EditForm>"
    + "<input type=hidden name=AssignmentNumber value=" + assignmentNumber + ">"
    + "<input type=submit value='" + res.getString("str_btn_ret_edit") + "'></FORM>");

    buf.append("<H2>" + res.getString("str_title_add_q") + "</h2>"
    + res.getString("str_explain_add1") + "<br>"
    + res.getString("str_explain_add2") + "<br>"
    + "<TABLE>"
    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='MULTIPLE_CHOICE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><H3>" + res.getString("str_mc") + "</H3><TABLE BGCOLOR=66FFFF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>&nbsp;" 
    + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_q") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>[" + res.getString("str_select_best") + "]</FONT></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='a'>A:</td>"
    + "<td><input type=text size=50 name='ChoiceAText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='b'>B:</td>"
    + "<td><input type=text size=50 name='ChoiceBText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='c'>C:</td>"
    + "<td><input type=text size=50 name='ChoiceCText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='d'>D:</td>"
    + "<td><input type=text size=50 name='ChoiceDText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='e'>E:</td>"
    + "<td><input type=text size=50 name='ChoiceEText'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_mc") 
    + "' onClick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='TRUE_FALSE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_tf") + "</H3><TABLE BGCOLOR=FF66FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>&nbsp;"
    + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>&nbsp;" 
    + displaySectionInfo() + "</td></tr>\n<tr><td>" + res.getString("str_q") 
    + "</td><td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>[" + res.getString("str_select_tf") + "]</FONT></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='true'></td><td>" + res.getString("str_true") + "</td></tr>"
    + "<tr><td><input type=radio name='Answer' value='false'></td><td>" + res.getString("str_false") + "</td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_tf") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='SELECT_MULTIPLE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_cb") + "</H3><TABLE BGCOLOR=FFFF66>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>&nbsp;"
    + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>&nbsp;" 
    + displaySectionInfo() + "</td></tr>\n<tr><td>" + res.getString("str_q") 
    + "</td><td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>[" + res.getString("str_select_cb") + "]</FONT></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='a'>A:</td>"
    + "<td><input type=text size=50 name='ChoiceAText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='b'>B:</td>"
    + "<td><input type=text size=50 name='ChoiceBText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='c'>C:</td>"
    + "<td><input type=text size=50 name='ChoiceCText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='d'>D:</td>"
    + "<td><input type=text size=50 name='ChoiceDText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='e'>E:</td>"
    + "<td><input type=text size=50 name='ChoiceEText'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_cb") 
    + "' onClick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='FILL_IN_WORD'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_fib") + "</H3><TABLE BGCOLOR=9999FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>&nbsp;"
    + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>&nbsp;" 
    + displaySectionInfo() + "</td></tr>\n<tr><td>" + res.getString("str_q") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>[" + res.getString("str_answer_fib") + "]</FONT></td></tr>"
    + "<tr><td>" + res.getString("str_answer") + "</td><td><input type=text size=23 name='CorrectAnswer1'>"
    + " &nbsp;" + res.getString("str_or") + "&nbsp;<input type=text size=23 name='CorrectAnswer2'></td></tr>"
    + "<tr><td>" + res.getString("str_q") + "</td>"
    + "<td><TEXTAREA NAME='QuestionTag' ROWS=3 COLS=47 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_fib") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value) && parse_for_error(this.form.elements.CorrectAnswer1.value) && parse_for_error(this.form.elements.CorrectAnswer2.value) && parse_for_error(this.form.elements.QuestionTag.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='NUMERIC'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_num") + "</H3><TABLE BGCOLOR=FF9999>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td COLSPAN=2>" + res.getString("str_q") 
    + "<TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT>"
    + "</TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>["+ res.getString("str_answer_num") + "]</FONT></td></tr>"
    + "<tr><td>" + res.getString("str_answer") + "<input type=text size=20 name='Answer'></td>"
    + "<td>" + res.getString("str_units") + "<input type=text size=15 name='Units'></td></tr>"
    + "<tr><td COLSPAN=2>" + res.getString("str_prec") 
    + "<input type=text size=6 name='RequiredPrecision' value='0.5'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_num") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"
    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='ESSAY'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_btn_essay") + "</H3><TABLE BGCOLOR=0099FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_qn") + " <input type=text size=2 name='AssignmentNumber' value='" + assignmentNumber + "'>&nbsp;"
    + res.getString("str_subj") + " <input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_q") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>[" + res.getString("str_answer_essay") + "]</FONT></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_essay") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "</TABLE>");

    return buf.toString();
  }

  void deleteQuestion(String questionID) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM QuizQuestions WHERE QuestionID='" + questionID + "'");
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
    }
  }

  boolean questionAddedSuccessfully(HttpServletRequest request, PrintWriter out) {
    String sqlInsertString = "INSERT INTO QuizQuestions ";
    // get all of the relevant parameters via the POST method:
    String assignmentNumber = request.getParameter("AssignmentNumber");
    String subjectArea = request.getParameter("SubjectArea");
    int pointValue = 1;
    String questionType = request.getParameter("QuestionType");
    String question = CharHider.quot2literal(request.getParameter("Question"));
    String section = request.getParameter("Section");
    int numberOfChoices = 0;
    
    if (questionType.equals("MULTIPLE_CHOICE")) {
        sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue, Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "MULTIPLE_CHOICE" + "',";
      for (char choice='A';choice<='E';choice++)
        if (request.getParameter("Choice" + choice + "Text").length()>0) numberOfChoices++;
      sqlInsertString += numberOfChoices + ",'";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceAText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceBText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceCText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceDText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceEText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Answer")) + "',";
      sqlInsertString += pointValue + ",'"; 
      sqlInsertString += section +  "')";
    }
    else if (questionType.equals("TRUE_FALSE")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,CorrectAnswer1,PointValue, Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "TRUE_FALSE" + "',";
      sqlInsertString += "2" + ",'";
      sqlInsertString += "True" + "','";
      sqlInsertString += "False" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Answer")) + "',";
      sqlInsertString += pointValue + ",'";
      sqlInsertString += section + "')";
    }
    else if (questionType.equals("SELECT_MULTIPLE")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "SELECT_MULTIPLE" + "',";
      for (char choice='A';choice<='E';choice++)
        if (request.getParameter("Choice" + choice + "Text").length()>0) numberOfChoices++;
      sqlInsertString += numberOfChoices + ",'";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceAText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceBText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceCText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceDText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceEText")) + "','";
      String correctAnswer[] = request.getParameterValues("Answer");
      if (correctAnswer != null) {
        for (int i = 1; i < correctAnswer.length; i++)   // concatenate correct answers into one string
          correctAnswer[0] += correctAnswer[i];
        sqlInsertString += correctAnswer[0] + "',";
      }
      else sqlInsertString += "',";
      sqlInsertString += pointValue + ",'";
      sqlInsertString += section + "')";
    }
    else if (questionType.equals("FILL_IN_WORD")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,CorrectAnswer1,CorrectAnswer2,QuestionTag,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "FILL_IN_WORD" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      // the following line inserts NULL into the database if 2nd answer doesn't exist, so that blank answer is not counted as correct:
      sqlInsertString += request.getParameter("CorrectAnswer2").equals("")?"NULL,'":"'" + CharHider.quot2literal(request.getParameter("CorrectAnswer2")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("QuestionTag")) + "',";
      sqlInsertString += pointValue + ",'";
      sqlInsertString += section + "')";
    }
    else if (questionType.equals("NUMERIC")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,CorrectAnswer1,QuestionTag,RequiredPrecision,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "NUMERIC" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Answer")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Units")) + "','";
      sqlInsertString += request.getParameter("RequiredPrecision") + "',";
      sqlInsertString += pointValue + ",'";
      sqlInsertString += section + "')";
    }
    else if (questionType.equals("ESSAY")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea +"','";
      sqlInsertString += question + "','";
      sqlInsertString += "ESSAY" + "',";
      sqlInsertString += pointValue + ",'";
      sqlInsertString += section + "')"; 
    }  
    else return false;
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sqlInsertString);  
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      if (addSectionField()){
        out.println(res.getString("str_section_added"));
        return false;
      }
      out.println(e.getMessage());
      return false;
    }
    return true;  
  }

  boolean questionEditedSuccessfully(HttpServletRequest request, PrintWriter out) {
    String sqlUpdateString = "UPDATE QuizQuestions SET ";
    // get all of the relevant parameters via the POST method:
    String questionType = request.getParameter("QuestionType");
    //all of the question types will need/use the section/coursesection info
    //additionally, it doesn't matter what order "set" statements are in. Hence,
    //put the set statmenet outside of the if's.
    sqlUpdateString += "Section='" + request.getParameter("Section") + "',";
    if (questionType.equals("MULTIPLE_CHOICE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices='" + request.getParameter("NumberOfChoices") + "',";
      sqlUpdateString += "ChoiceAText='" + CharHider.quot2literal(request.getParameter("ChoiceAText")) + "',";
      sqlUpdateString += "ChoiceBText='" + CharHider.quot2literal(request.getParameter("ChoiceBText")) + "',";
      sqlUpdateString += "ChoiceCText='" + CharHider.quot2literal(request.getParameter("ChoiceCText")) + "',";
      sqlUpdateString += "ChoiceDText='" + CharHider.quot2literal(request.getParameter("ChoiceDText")) + "',";
      sqlUpdateString += "ChoiceEText='" + CharHider.quot2literal(request.getParameter("ChoiceEText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + request.getParameter("CorrectAnswer1") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("TRUE_FALSE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices=2,";
      sqlUpdateString += "ChoiceAText='true',";
      sqlUpdateString += "ChoiceBText='false',";
      sqlUpdateString += "CorrectAnswer1='" + request.getParameter("CorrectAnswer1") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("SELECT_MULTIPLE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices='" + request.getParameter("NumberOfChoices") + "',";
      sqlUpdateString += "ChoiceAText='" + CharHider.quot2literal(request.getParameter("ChoiceAText")) + "',";
      sqlUpdateString += "ChoiceBText='" + CharHider.quot2literal(request.getParameter("ChoiceBText")) + "',";
      sqlUpdateString += "ChoiceCText='" + CharHider.quot2literal(request.getParameter("ChoiceCText")) + "',";
      sqlUpdateString += "ChoiceDText='" + CharHider.quot2literal(request.getParameter("ChoiceDText")) + "',";
      sqlUpdateString += "ChoiceEText='" + CharHider.quot2literal(request.getParameter("ChoiceEText")) + "',";
      String correctAnswer[] = request.getParameterValues("CorrectAnswer1");
      if (correctAnswer != null) {
        for (int i = 1; i < correctAnswer.length; i++)   // concatenate correct answers into one string
          correctAnswer[0] += correctAnswer[i];
        sqlUpdateString += "CorrectAnswer1='" + correctAnswer[0] + "',";
      }
      else sqlUpdateString += "CorrectAnswer1='',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("FILL_IN_WORD")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "CorrectAnswer2=" // inserts NULL if 2nd answer is blank
      + (request.getParameter("CorrectAnswer2").equals("") ? "NULL," : ("'" + CharHider.quot2literal(request.getParameter("CorrectAnswer2")) + "',"));
      sqlUpdateString += "QuestionTag='" + CharHider.quot2literal(request.getParameter("QuestionTag")) + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("NUMERIC")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "QuestionTag='" + CharHider.quot2literal(request.getParameter("QuestionTag")) + "',"; // units
      sqlUpdateString += "RequiredPrecision='" + request.getParameter("RequiredPrecision") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }
    else if (questionType.equals("ESSAY")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }  
    else return false;

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sqlUpdateString);
      stmt.close();
      conn.close();
    } 
    catch(Exception e) {
      if (addSectionField()){
        out.println(res.getString("str_section_added"));
        return false;
      }
      out.println(e.getMessage());
      return false;
    }
    return true;
  }

  String editQuestionsForm(int assignmentNumber){
    JSmethods jsm = new JSmethods(JSmethods.QUIZ);
    StringBuffer buf = new StringBuffer();
    jsm.appendJSCheckHtml(buf);
    buf.append(res.getString("str_quiz") + "&nbsp;" + assignmentNumber + "<br>");
    buf.append("<TABLE BORDER=0 CELLSPACING=0><TR>"
    + "<TD><FORM METHOD=POST><input type=hidden name=UserRequest value='AddQuestionForm'>"
    + "<input type=hidden name=AssignmentNumber value='" + assignmentNumber + "'>"
    + "<input type=submit value='" + res.getString("str_btn_add_q") + "'></FORM></TD>"
    + "<TD><FORM METHOD=POST><input type=submit value='" + res.getString("str_btn_ret_info") + "'></FORM></TD>"
    + "</TR></TABLE>");

    buf.append("<TABLE BORDER=1 CELLSPACING=0>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM QuizQuestions WHERE AssignmentNumber=" + assignmentNumber 
      + " ORDER BY SubjectArea,QuestionID";
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);

      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        buf.append("\n<TR>");
        buf.append("\n<td><FORM>" + question.print() + "</FORM></td>");
        buf.append("\n<td><FORM METHOD=POST>" + question.edit() + "</FORM></td>");
        buf.append("\n</TR>");
      }
    }
    catch (Exception e) {
      if (addSectionField()){
        return res.getString("str_section_added");
      }
      return e.getMessage();
    }
    buf.append("\n</TABLE>");
    return buf.toString();
  }

  String displaySectionInfo(){
      StringBuffer buf = new StringBuffer("Course Section: <SELECT NAME='Section'><OPTION VALUE='All'>" + res.getString("str_all"));      
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
        return "<INPUT TYPE=HIDDEN Name=Section value='All'>";
      int nSections=rsSections.getInt("Value");
      //sections start with 1, so, so does i.
      for (int i=1; i<=nSections; i++){
        buf.append("<OPTION>"+i);    
      }
      buf.append("</SELECT>");
    }catch(Exception e){
      return "<INPUT TYPE=HIDDEN NAME=Section value='All'>";
    }
    return buf.toString();
  }

  boolean addSectionField(){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE QuizQuestions ADD (Section VARCHAR(3) DEFAULT 'All' )");
    }catch(Exception e){
      return false;
    }
    return true;
  }

  boolean addParam(){
    try {
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
}
