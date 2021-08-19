package Eledge;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageExam extends HttpServlet {

  // in this servlet, the index i is used to indicate the Assignment Number,
  // and normally ranges from 0 to numberOfAssignments-1
  // conversely, the index j runs over the sections of the course, 
  // normally from 1 to numberOfSections.  The difference is due
  // to the fact that the auto_increment feature of mysql starts numbering 
  // the SectionID variable at 1 not 0.  Therefore, the first
  // and default sectionID number is 1.  
  Logger log = new Logger();
  RBStore res=EledgeResources.getManageExamBundle(); //i18n

  public String getServletInfo() {
    return "This Eledge servlet allows the instructor to edit and configure exams.";
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    log.paranoid("getting/setting session information.","ManageExam:doGet");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageExam");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageExam",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+err.toString(),student));
        return;
      }
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }
    // from here on, user is assumed to be the instructor
    log.paranoid("Writing deadlinesForm page.","ManageExam:doGet");
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+deadlinesForm(student),student));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Getting/setting session information.","ManageExam:doPost");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageExam");
      return;
    }

    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageExam",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+err.toString(),student));
        return;
      }
    }

    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }
    // from here on, user is assumed to be the instructor
    StringBuffer error = new StringBuffer(" ");

    String userRequest = request.getParameter("UserRequest");
    log.paranoid("userRequest is: " + userRequest, "ManageExam:doPost");
    if (userRequest == null) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+deadlinesForm(student),student));
      return;
    }

    if (userRequest.equals("Update")) {
      updateDeadlines(request,error,student);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+error + deadlinesForm(student),student));
      return;
    }
    else if (userRequest.equals("AddAnExam")) {
      addAnExam(student);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+deadlinesForm(student),student));
      return;
    }

    int assignmentNumber = 0;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> Error</em><br><br>"+res.getString("str_must_select_exam"),student));
      return;
    }

    if (userRequest.equals("DeleteExam")) {
      deleteExam(assignmentNumber);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> Administrador de Examenes</em><br><br>"+deadlinesForm(student),student));
      return;
    }
    else if (userRequest.equals("EditForm")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Editar o Agregar Preguntas</em><br><br>"+editQuestionsForm(assignmentNumber),student));
    }
    else if (userRequest.equals("AddQuestionForm")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Agregar Preguntas</em><br><br>"+addQuestionForm(assignmentNumber),student));
    }
    else if (userRequest.equals("AddQuestion")) {
      if (questionAddedSuccessfully(request,out)) {
	log.paranoid("Question was added successfully.","ManageExam:doPost");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Agregar Preguntas</em><br><br>"+res.getString("str_question_added").concat("<br>").concat(addQuestionForm(assignmentNumber)),student));
      } else {
	log.paranoid("Missing or bad information for question.","ManageExam:doPost");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" Agregar Preguntas Error</em><br><br>"+res.getString("str_bad_information"),student));
      }
    }
    else if (userRequest.equals("Edit")) {
      if (questionEditedSuccessfully(request,out)) {
	log.paranoid("Question changed successfully.","ManageExam:doPost");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Editar o Agregar Preguntas</em><br><br>"+res.getString("str_question_changed").concat("<br>").concat(editQuestionsForm(assignmentNumber)),student));
      }
      else
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Editar o Agregar Preguntas</em><br><br>"+res.getString("str_bad_information"),student));
    }
    else if (userRequest.equals("DeleteQuestion")) {
      deleteQuestion(request.getParameter("QuestionID"));
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+" >> Editar o Agregar Preguntas</em><br><br>"+editQuestionsForm(assignmentNumber),student));
    }
    else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Exam'>Examenes</a> >> <a href='"+Course.name+".ManageExam'>Administrador de Examenes</a> >> "+getExamTitle(assignmentNumber)+"  nose</em><br><br>"+deadlinesForm(student),student));
  }

  String deadlinesForm(Student student) {
    int numberOfSections = 1;
    boolean useSectionDeadlines = false;
    //need this for future reference for whether or not to print the
    //"date available" field.
    boolean allowWorkAhead = false;
    String[] sectionNames = null;
    StringBuffer buf = new StringBuffer();
    Date now = new Date();
    
    buf.append("<h2>".concat(res.getString("str_exam_info_header")).concat("</h2>"));
    buf.append("<FORM METHOD=POST>");
    buf.append("<input type=hidden name=UserRequest value=Update>");
    int numberOfAssignments = 0;
    log.paranoid("Beginning try block.","ManageExam:deadlinesForm");   
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Getting rsNSections.","ManageExam:deadlinesForm");
      //ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      //if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      //if (numberOfSections == 1) 
      //useSectionDeadlines = false;
      //log.paranoid("numberOfSections: " + Integer.toString(numberOfSections),"ManageExam:deadlinesForm");
      ResultSet rsParams = stmt.executeQuery("SELECT * FROM ExamParameters WHERE id_course='"+student.getCourse_id()+"'");
      if (rsParams.next()) useSectionDeadlines = rsParams.getBoolean("UseSectionDeadlines");
      log.paranoid("UseSectionDeadlines: " + useSectionDeadlines,"ManageExam:deadlinesForm");
      allowWorkAhead=rsParams.getBoolean("AllowWorkAhead");
      log.paranoid("allowWorkAhead: " + allowWorkAhead,"ManageExam:deadlinesForm");
      buf.append("\n<h3>".concat(res.getString("str_exam_param_header")).concat("</h3><table>")
      + "\n<tr><td><b>".concat(res.getString("str_param_field_param")).concat("</b></td><td><b>").concat(res.getString("str_param_field_value")).concat("</b></td><td><b>".concat(res.getString("str_param_field_descrip")).concat("</b></td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_time_limit")).concat("</td> <td><INPUT SIZE=3 NAME=TimeLimit VALUE=") + rsParams.getInt("TimeLimit") + "></td><td>".concat(res.getString("str_param_time_limit2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_wait_time")).concat("</td>  <td><INPUT SIZE=3 NAME=WaitForNewDownload VALUE=") + rsParams.getInt("WaitForNewDownload") + "></td><td>".concat(res.getString("str_param_wait_time2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_deadlines")).concat("</td>  <td><INPUT TYPE=CHECKBOX VALUE=true NAME=EnforceDeadlines").concat((rsParams.getBoolean("EnforceDeadlines"))?" CHECKED":"").concat("></td><td>").concat(res.getString("str_param_deadlines2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_tryagain")).concat("</td>  <td><INPUT TYPE=CHECKBOX VALUE=true NAME=AllowMultipleTries") + (rsParams.getBoolean("AllowMultipleTries")?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_tryagain2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_scramble")).concat("</td>   <td><INPUT TYPE=CHECKBOX VALUE=true NAME=ScrambleQuestions") + (rsParams.getBoolean("ScrambleQuestions")?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_scramble2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_work_ahead")).concat("</td> <td><INPUT TYPE=CHECKBOX VALUE=true NAME=AllowWorkAhead") + (allowWorkAhead?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_work_ahead2")).concat("</td></tr>")
      + "\n<tr><td>".concat(res.getString("str_param_show_missed")).concat("</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=ShowMissedQuestions") + (rsParams.getBoolean("ShowMissedQuestions")?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_show_missed2")).concat("</td></tr>")
      + (numberOfSections>1?"\n<tr><td>".concat(res.getString("str_param_sections")).concat("</td><td><INPUT TYPE=CHECKBOX VALUE=true NAME=UseSectionDeadlines") + (useSectionDeadlines?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_sections2")).concat("</td></tr>"):"")));
      
      try{
         boolean track=rsParams.getBoolean("TrackAnswers");
         buf.append("\n<tr><td>".concat(res.getString("str_param_track_answers")).concat("</td> <td><INPUT TYPE=CHECKBOX VALUE=true NAME=TrackAnswers") + (track?" CHECKED":"") + "></td><td>".concat(res.getString("str_param_track_answers2")).concat("</td></tr>"));
      }catch(Exception e){
         if(addParam()) {
	   log.paranoid("Track answers error caught and handled with addParam.","ManageExam:deadlinesForm");
           return(res.getString("str_param_track_answers_added"));
	 }
	 else {
	   log.normal("Unhandled exception in answer tracking caught.","ManageExam:deadlinesForm");
	   return e.getMessage();
	 }
      }
      buf.append("\n</table>");

      //numberOfSections = 1;
      //sectionNames = new String[numberOfSections+1];
      log.paranoid("Selecting SectionNames from CourseSections","ManageExam:deadlinesForm");
      //ResultSet rsSectionNames = stmt.executeQuery("SELECT SectionName FROM CourseSections ORDER BY SectionID");
      //for (int j=1;j<=numberOfSections;j++) 
      //  if (rsSectionNames.next()) sectionNames[j] = rsSectionNames.getString("SectionName");

      // print a header row to the exam info table
      buf.append("\n<h3>".concat(res.getString("str_specific_exam_info_header")).concat("</h3>"));
      buf.append("\n<table border=1 cellspacing=0><tr><td>".concat(res.getString("str_select")).concat("</td><td>").concat(res.getString("str_field_exam_code")).concat("</td>"));  // print a header row to the table of exam deadlines
      log.paranoid("Running for loop for deadlines/date avaliables","ManageExam:deadlinesForm");
      //for (int j=1;j<=numberOfSections;j++) {
      //  if (useSectionDeadlines) buf.append((allowWorkAhead?"":"<td>".concat(res.getString("str_field_exam_dateavail")).concat(" ") + sectionNames[j] + "</td>") + "<td>".concat(res.getString("str_field_exam_deadline")).concat(" ") + sectionNames[j] + "</td>");
	//else  buf.append((allowWorkAhead?"<td>".concat(res.getString("str_field_exam_deadline")).concat("</td>"):"<td>".concat(res.getString("str_field_exam_dateavail")).concat("</td><td>").concat(res.getString("str_field_exam_deadline")).concat("</td>")));
      //}
      buf.append("<td>".concat(res.getString("str_field_exam_title")).concat("</td><td>").concat(res.getString("str_field_exam_dateavail")).concat("</td><td>").concat(res.getString("str_field_exam_deadline")).concat("</td><td>").concat(res.getString("str_field_exam_questions_per_sa")).concat("</td></tr>"));

      // get the exam information and the database table stucture
      log.paranoid("Getting exam info and dbase table structure.","ManageExam:deadlinesForm");
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ExamInfo WHERE id_course='"+student.getCourse_id()+"' ORDER BY AssignmentNumber");
      ResultSetMetaData rsmd = rsInfo.getMetaData();
       
      String assignmentNumber;
      int i=0;
      
      while (rsInfo.next()) {  // print one row for each exam
        
        String iStr = Integer.toString(i);
        assignmentNumber = rsInfo.getString("AssignmentNumber");
        buf.append("\n<tr><td><INPUT TYPE=RADIO NAME=AssignmentNumber VALUE='" + assignmentNumber + "'></td>");
        buf.append("<td><INPUT SIZE=5 NAME=" + iStr + ":AssignmentNumber VALUE='"
        + assignmentNumber + "'></td>");
        
        // print 1 column for each section; use values for section 1 if necessary; flag if expansion required
        //for (int j=1;j<=numberOfSections;j++) {
          //if no work ahead, put in the date available field.
	  //log.paranoid("For loop for date availables and deadlines;real info","ManageExam:deadlinesForm");
          /*if (!allowWorkAhead) {*/
          
        buf.append("<td><INPUT NAME=" + iStr + ":Title VALUE='" + rsInfo.getString("Title") + "'></td>");
	    buf.append("<td><INPUT NAME=" + iStr + ":Available" + Integer.toString(1) + " VALUE='");
	    
	    
	    try {
	      buf.append(rsInfo.getString("Available" + Integer.toString(1)));
	    } catch(Exception e) {
	    	
	      buf.append(rsInfo.getString("Available1"));
	    }
	    buf.append("' SIZE=20></td>");
	  //}
          
          
          buf.append("<td><INPUT NAME=" + iStr + ":Deadline" + Integer.toString(1) + " VALUE='");
          try {
          	buf.append(rsInfo.getString("Deadline" + Integer.toString(1)));
          }
          catch (Exception e) {
            log.normal("Exception: " + e.getMessage() + " caught. Using 'Deadline1'","ManageExam:deadlinesForm");
            buf.append(rsInfo.getString("Deadline1"));
          }
          buf.append("' SIZE=20></td>"); 
        //}
        
        //buf.append("<td><INPUT NAME=" + iStr + ":NSubjectAreas SIZE=4 VALUE='" + (rsInfo.getString("NSubjectAreas")==null?"1":rsInfo.getString("NSubjectAreas")) + "'></td>");
        buf.append("<td><INPUT NAME=" + iStr + ":NQuestionsPerSubjectArea SIZE=4 VALUE='" + (rsInfo.getString("NQuestionsPerSubjectArea")==null?"10":rsInfo.getString("NQuestionsPerSubjectArea")) + "'></td>");
        i++;
        
        
      }
      
      numberOfAssignments = i;
    }
    catch (Exception e) {
      if (Exam.addAvailableField()){
	log.normal("Caught exception " + e.getMessage() + " with Exam.addAvailableField","ManageExam:deadlinesForm");
        return deadlinesForm(student);
      }
      else
        log.sparse("Unhandled exception " + e.getMessage() + " not handled by Exam.addAvilableField.","ManageExam:deadlinesForm");
    }
    buf.append("\n</table><br>");

    buf.append("\n<INPUT TYPE=HIDDEN NAME=NumberOfAssignments VALUE=" + numberOfAssignments + ">");
    buf.append("\n<input type=submit value='".concat(res.getString("str_update_exam_info")).concat("'>&nbsp;"));
    buf.append("<input type=reset value='".concat(res.getString("str_restore_exam_values")).concat("'><br>"));
    buf.append("<input type=submit value='".concat(res.getString("str_add_exam")).concat("' onClick=this.form.elements.UserRequest.value='AddAnExam'>&nbsp;"));
    buf.append("<input type=submit value='".concat(res.getString("str_delete_exam")).concat("' onClick=this.form.elements.UserRequest.value='DeleteExam'><br>"));
    buf.append("<input type=submit value='".concat(res.getString("str_edit_exam_questions")).concat("' onClick=this.form.elements.UserRequest.value='EditForm'>"));
    buf.append("</FORM>");

    return buf.toString();
  }

  void updateDeadlines(HttpServletRequest request, StringBuffer error, Student student) {
    int numberOfSections = 1;
    boolean useSectionDeadlines = request.getParameter("UseSectionDeadlines")==null?false:true;
    StringBuffer sqlUpdate = new StringBuffer("UPDATE ExamParameters SET ");
    log.paranoid("Beginning of try statement.","ManageExam:updateDeadlines");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      log.paranoid("Getting parameters for updating.","ManageExam:updateDeadlines");
      sqlUpdate.append("TimeLimit='" + request.getParameter("TimeLimit") + "',"
      + "WaitForNewDownload='" + request.getParameter("WaitForNewDownload") + "',"
      + "EnforceDeadlines=" + (request.getParameter("EnforceDeadlines")==null?"'false',":"'true',")
      + "AllowMultipleTries=" + (request.getParameter("AllowMultipleTries")==null?"'false',":"'true',")
      + "ScrambleQuestions=" + (request.getParameter("ScrambleQuestions")==null?"'false',":"'true',")
      + "AllowWorkAhead=" + (request.getParameter("AllowWorkAhead")==null?"'false',":"'true',")
      + "ShowMissedQuestions=" + (request.getParameter("ShowMissedQuestions")==null?"'false',":"'true',")
      + "UseSectionDeadlines=" + (useSectionDeadlines?"'true',":"'false',")
      + "TrackAnswers=" + (request.getParameter("TrackAnswers")==null?"'false'":"'true'") + "WHERE id_course = '"+student.getCourse_id()+"'");
      log.paranoid("Running sqlUpdate: " + sqlUpdate.toString(),"ManageExam:updateDeadlines");
      stmt.executeUpdate(sqlUpdate.toString());      
      log.paranoid("sqlUpdate done.","ManageExam:updateDeadlines");
      //ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      //if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      //rsNSections.close();
//since we're adding an "Available" column, we need to divide the column count by 2. 
      int nSectionsInfo =  (stmt.executeQuery("SELECT * FROM ExamInfo WHERE id_course='"+student.getCourse_id()+"' ORDER BY AssignmentNumber").getMetaData().getColumnCount() - 5)/2;
      log.paranoid("nSectionInfo: " + Integer.toString(nSectionsInfo),"ManageExam:updateDeadlines");
      String now = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
      /*for (int j=nSectionsInfo+1;j<=numberOfSections;j++) {
        log.paranoid("Updating ExamInfo table with new deadline and available columns.","ManageExam:updateDeadlines");
        stmt.executeUpdate("ALTER TABLE ExamInfo ADD COLUMN Available" + Integer.toString(j) + " DATETIME DEFAULT '" + now + "'");
        stmt.executeUpdate("ALTER TABLE ExamInfo ADD COLUMN Deadline" + Integer.toString(j) + " DATETIME DEFAULT '" + now + "'");
        //add an "available" column as well.
      }*/
      
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ExamInfo WHERE id_course='"+student.getCourse_id()+"' ORDER BY AssignmentNumber");
  
      int i=0;
      while (rsInfo.next()) {
        sqlUpdate = new StringBuffer("UPDATE ExamInfo SET ");
        sqlUpdate.append("AssignmentNumber='" + request.getParameter(Integer.toString(i) + ":AssignmentNumber") + "',");
        //for (int j=1;j<=numberOfSections;j++) {
        //  log.paranoid("updating deadlines and availables.","ManageExam:updateDeadlines");
        
        String deadline = request.getParameter(Integer.toString(i) + ":Deadline" + Integer.toString(1));
        if (deadline != null) sqlUpdate.append("Deadline" + Integer.toString(1) + "='" + deadline + "',");
	    
	    String available = request.getParameter(Integer.toString(i) + ":Available" + Integer.toString(1));
	    if (available != null) sqlUpdate.append("Available" + Integer.toString(1) + "='" + available + "',");
        
        //}
        sqlUpdate.append("Title='" + request.getParameter(Integer.toString(i) + ":Title") + "',"
        //+ "NSubjectAreas='" + request.getParameter(Integer.toString(i) + ":NSubjectAreas") +  "',"
        + "NQuestionsPerSubjectArea='" + request.getParameter(Integer.toString(i) + ":NQuestionsPerSubjectArea") + "' "
	+ "WHERE (AssignmentNumber='" + rsInfo.getInt("AssignmentNumber") + "')");
        stmt.executeUpdate(sqlUpdate.toString());      
        i++;
      }
      rsInfo.close();
    }
    catch (Exception e) {
      error.append(e.getMessage());
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","ManageExam:updateDeadlines");
    }
  }

  void addAnExam(Student student) {
    Date now = new Date();
    log.paranoid("Beginning of try block.","ManageExam:addAnExam");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      StringBuffer sqlInsert = new StringBuffer("INSERT INTO "
      + "ExamInfo (id_course , Title , SubjectAreas , NQuestionsPerSubjectArea , Deadline1 , Available1 ) "
      + "VALUES ("+student.getCourse_id()+", 'New Exam', 1, 10, now()," + getPreviousDeadline() + ")");
      //log.paranoid("sqlInsert: " + sqlInsert.toString(),"ManageExam:addAnExam");
      
      stmt.executeUpdate(sqlInsert.toString());
      //log.paranoid("Inserted.","ManageExam:addAnExam");
    }
    catch (Exception e) {
      if (addAvailableField()) {
        addAnExam(student);
        log.sparse("Added Available field to the ExamInfo table","ManageExam:addAnExam");
      }
      else log.sparse("Unhandled exception: " + e.getMessage(),"ManageExam:addAnExam");
    }
  }

  //This method checks for a previous exam, and if there is one, returns the 
  //deadline of the previous exam. Called from addExam in order to determine
  //an appropriate date available. Use previous deadline as default in order
  //to simulate previous behaviour of AllowWorkAhead.
  String getPreviousDeadline() {
    StringBuffer buf = new StringBuffer("now()");
    log.paranoid("Beginning try block.","ManageExam:getPreviousDeadline");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //pull the previous deadline out, if one exists.
      ResultSet rsPrev = stmt.executeQuery("SELECT Deadline1 FROM ExamInfo ORDER BY AssignmentNumber DESC LIMIT 1");
      if (!rsPrev.next()) { //no previous exams . . . this is the first one. 
	log.normal("No previous exams.","ManageExam:getPreviousDeadline");
        return buf.toString();
      }
      else return "'" + rsPrev.getString("Deadline1") + "'";
    } catch (Exception e) {
      log.normal("Unhandled exception " + e.getMessage() + " caught.","ManageExam:getPreviousDeadline"); 
    }
    return buf.toString();
  }

  void deleteExam(int assignmentNumber) {
    log.paranoid("Beginning try block.","ManageExam:deleteExam");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM ExamInfo WHERE AssignmentNumber=" + assignmentNumber);
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","ManageExam:deleteExam");
    }
  }

  String addQuestionForm(int assignmentNumber) {
    StringBuffer buf = new StringBuffer();
    JSmethods jsm = new JSmethods(JSmethods.EXAM);
    log.paranoid("Beginning method.","ManageExam:addQuestionForm");
    jsm.appendJSCheckHtml(buf);
    buf.append("<FORM METHOD=POST><input type=hidden name=UserRequest value=EditForm>"
    + "<input type=hidden name=AssignmentNumber value=" + assignmentNumber + ">"
    + "<input type=submit value='".concat(res.getString("str_return_edit_page")).concat("'></FORM>"));

    buf.append("<H2>".concat(res.getString("str_add_question")).concat("</h2>")
    + res.getString("str_add_question_help").concat("<br>")
    + "<TABLE>"
    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='MULTIPLE_CHOICE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><H3>".concat(res.getString("str_multiple_choice_question")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>".concat(res.getString("str_select_best_answer")).concat("</FONT></td></tr>")
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
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_multiple_choice")).concat("' onClick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='TRUE_FALSE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>".concat(res.getString("str_true_false_question")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>".concat(res.getString("str_select_true_false")).concat("</FONT></td></tr>")
    + "<tr><td><input type=radio name='Answer' value='true'></td><td>True</td></tr>"
    + "<tr><td><input type=radio name='Answer' value='false'></td><td>False</td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_true_false")).concat("' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='SELECT_MULTIPLE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>".concat(res.getString("str_checkbox_question")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>".concat(res.getString("str_select_checkbox")).concat("</FONT></td></tr>")
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
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_checkbox")).concat("' onClick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='FILL_IN_WORD'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>".concat(res.getString("str_fillintheblank_question")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>"
    + res.getString("str_fill_in_blank").concat("</FONT></td></tr>")
    + "<tr><td>".concat(res.getString("str_answer_text")).concat(":</td><td><input type=text size=23 name='CorrectAnswer1'>")
    + " &nbsp;or&nbsp;<input type=text size=23 name='CorrectAnswer2'></td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='QuestionTag' ROWS=3 COLS=47 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_fillintheblank")).concat("' onClick=\"if (parse_for_error(this.form.elements.Question.value) && parse_for_error(this.form.elements.CorrectAnswer1.value) && parse_for_error(this.form.elements.CorrectAnswer2.value) && parse_for_error(this.form.elements.QuestionTag.value)){this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='NUMERIC'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>".concat(res.getString("str_numeric_question")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td COLSPAN=2>".concat(res.getString("str_question_text")).concat(":")
    + "<TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT>"
    + "</TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>".concat(res.getString("str_type_numeric"))
    + "</FONT></td></tr>"
    + "<tr><td>".concat(res.getString("str_answer_text")).concat(":<input type=text size=20 name='Answer'></td>")
    + "<td>Units:<input type=text size=15 name='Units'></td></tr>"
    + "<tr><td COLSPAN=2>".concat(res.getString("str_required_precision")).concat(" ")
    + "<input type=text size=6 name='RequiredPrecision' value='0.5'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_numeric")).concat("' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"
    + "</td></FORM></tr>"
    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='ESSAY'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>".concat(res.getString("str_essay")).concat("</H3><TABLE class=row1>")
    + "<tr><td COLSPAN=2>".concat(res.getString("str_field_exam_code")).concat(" <input type=text size=2 name='AssignmentNumber' value='") + assignmentNumber + "'>"
    //+ "&nbsp;".concat(res.getString("str_field_exam_subject_area")).concat(" <input type=text size=2 name='SubjectArea' value=0>")
    //+ "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>".concat(res.getString("str_question_text")).concat(":</td>")
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>"
    + res.getString("str_write_your_answer").concat("</FONT></td></tr>")
    + "<tr><td COLSPAN=2><input type=button value='".concat(res.getString("str_send_essay")).concat("' onClick=\"if (parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>")
    + "</TABLE>"
    + "</td></FORM></tr>"
    + "</TABLE>");
    log.paranoid("End of method.","ManageExam:addQuestionsForm");
    return buf.toString();
  }

  void deleteQuestion(String questionID) {
    log.paranoid("Beginning of try block.","ManageExam:deleteQuestion");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM ExamQuestions WHERE QuestionID='" + questionID + "'");
      stmt.close();
      conn.close();
    }
    catch (Exception e) {
      log.normal("Unhandled exception " + e.getMessage() + " caught.","ManageExam:deleteQuestion");
    }
  }

  boolean questionAddedSuccessfully(HttpServletRequest request, PrintWriter out) {
    log.paranoid("Beginning method.","ManageExam:questionAddedSuccessfully");
    String sqlInsertString = "INSERT INTO ExamQuestions ";
    // get all of the relevant parameters via the POST method:
    String assignmentNumber = request.getParameter("AssignmentNumber");
    //String subjectArea = request.getParameter("SubjectArea");
    int pointValue = 1;
    String questionType = request.getParameter("QuestionType");
    String question = CharHider.quot2literal(request.getParameter("Question"));
    //String section = request.getParameter("Section");
    int numberOfChoices = 0;
    
    if (questionType.equals("MULTIPLE_CHOICE")) {
        sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue) ";
      sqlInsertString += "VALUES ('" + assignmentNumber + "','";
      //sqlInsertString += subjectArea + "','";
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
      sqlInsertString += pointValue + ")";
    }
    else if (questionType.equals("TRUE_FALSE")) {
        sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,CorrectAnswer1,PointValue) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      //sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "TRUE_FALSE" + "',";
      sqlInsertString += "2" + ",'";
      sqlInsertString += "True" + "','";
      sqlInsertString += "False" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Answer")) + "',";
      sqlInsertString += pointValue + ")";
    }
    else if (questionType.equals("SELECT_MULTIPLE")) {
        sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      //sqlInsertString += subjectArea + "','";
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
      sqlInsertString += pointValue + ")";
    }
    else if (questionType.equals("FILL_IN_WORD")) {
      sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,CorrectAnswer1,CorrectAnswer2,QuestionTag,PointValue) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      //sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "FILL_IN_WORD" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      // the following line inserts NULL into the database if 2nd answer doesn't exist, so that blank answer is not counted as correct:
      sqlInsertString += request.getParameter("CorrectAnswer2").equals("")?"NULL,'":"'" + CharHider.quot2literal(request.getParameter("CorrectAnswer2")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("QuestionTag")) + "',";
      sqlInsertString += pointValue + ")";
    }
    else if (questionType.equals("NUMERIC")) {
      sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,CorrectAnswer1,QuestionTag,RequiredPrecision,PointValue) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      //sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "NUMERIC" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Answer")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Units")) + "','";
      sqlInsertString += request.getParameter("RequiredPrecision") + "',";
      sqlInsertString += pointValue + ")";
    }
    else if (questionType.equals("ESSAY")) {
      sqlInsertString += "(AssignmentNumber,QuestionText,QuestionType,PointValue) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      //sqlInsertString += subjectArea +"','";
      sqlInsertString += question + "','";
      sqlInsertString += "ESSAY" + "',";
      sqlInsertString += pointValue + ")";
    }
    else return false;
    
      log.paranoid("sqlInsertString: " + sqlInsertString,"ManageExam:questionAddedSuccessfully.");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sqlInsertString);  
      stmt.close();
      conn.close();
    }
    catch(Exception e) {
      /*if (addSectionField()){
	log.normal("Exception caught and handled by addSectionField","ManageExam:questionAddedSuccessfully.");
        out.println("Section field added. Please go back and try again.");
        return false;
      }*/
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","ManageExam:questionAddedSuccessfully.");
      out.println(e.getMessage());
      return false;
    }
    return true;  
  }

  boolean questionEditedSuccessfully(HttpServletRequest request, PrintWriter out) {
    log.paranoid("Begin method.","ManageExam:questionEditedSuccessfully.");
    String sqlUpdateString = "UPDATE ExamQuestions SET ";
    // get all of the relevant parameters via the POST method:
    String questionType = request.getParameter("QuestionType");
    //sqlUpdateString += "Section='" + request.getParameter("Section") + "',";
    if (questionType.equals("MULTIPLE_CHOICE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
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
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
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
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
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
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
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
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "QuestionTag='" + CharHider.quot2literal(request.getParameter("QuestionTag")) + "',"; // to specify units of answer
      sqlUpdateString += "RequiredPrecision='" + request.getParameter("RequiredPrecision") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }
    else if (questionType.equals("ESSAY")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      //sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }
    else return false;
    log.paranoid("sqlUpdateString: " + sqlUpdateString,"ManageExam:questionEditedSuccessfully.");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sqlUpdateString);
      stmt.close();
      conn.close();
    } catch(Exception e) {
        /*if (addSectionField()){
	  log.normal("Caught exception handled by addSectionField","ManageExam:questionEditedSuccessfully.");
          out.println("Section field added. Please refresh your browser."); 
          return false;
        }*/
	log.sparse("Unhanlded exception " + e.getMessage() + " caught.","ManageExam:questionEditedSuccessfully.");
        out.println(e.getMessage());
      return false;
    }
    return true;
  }

  String editQuestionsForm(int assignmentNumber){
    JSmethods jsm = new JSmethods(JSmethods.EXAM);
    log.paranoid("Beginning Method.","ManageExam:editQuestionsForm");
    StringBuffer buf = new StringBuffer();
    jsm.appendJSCheckHtml(buf);
    //buf.append((res.getString("str_field_exam_code")).concat(" ") + assignmentNumber + "  "+getExamTitle(assignmentNumber)+"<br>");
    buf.append("<TABLE BORDER=0 CELLSPACING=0><TR>"
    + "<TD><FORM METHOD=POST><input type=hidden name=UserRequest value='AddQuestionForm'>"
    + "<input type=hidden name=AssignmentNumber value='" + assignmentNumber + "'>"
    + "<input type=submit value='".concat(res.getString("str_add_question")).concat("'></FORM></TD>")
    + "<TD><FORM METHOD=POST><input type=submit value='".concat(res.getString("str_return_exam_page")).concat("'></FORM></TD>")
    + "</TR></TABLE>");

    buf.append("<TABLE BORDER=1 CELLSPACING=0>");
    log.paranoid("Beginning try block.","ManageExam:editQuestionsForm");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ExamQuestions WHERE AssignmentNumber=" + assignmentNumber 
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
    } catch(Exception e) {
      /*if (addSectionField()){
	log.normal("addSectionField method handled caught exception.","ManageExam:editQuestionsForm");
        return (res.getString("str_section_field_added"));
      }*/
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","ManageExam: editQuestionsForm");
      return e.getMessage();
    }
    buf.append("\n</TABLE>");
    return buf.toString();
  }

  /*String displaySectionInfo(){
    StringBuffer buf = new StringBuffer(res.getString("str_course_section").concat(": <SELECT NAME='Section'><OPTION>" + res.getString("str_all")));
    log.paranoid("Beginning try block.","ManageExam:displaySectionInfo");
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
      log.normal("Caught exception " + e.getMessage(),"ManageExam:editQuestionsForm");
      return "<INPUT TYPE=HIDDEN NAME=Section value='All'>";
    }
    return buf.toString();
  }*/
 
  /*boolean addSectionField(){
    log.paranoid("Beginning try block.","ManageExam:addSectionField");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Adding Section field.","ManageExam:addSectionField");
      stmt.executeUpdate("ALTER TABLE ExamQuestions ADD (Section VARCHAR(3) DEFAULT 'All' )");
    }catch(Exception e){
      log.normal("Caught Exception " + e.getMessage() + " (table doesn't exist, maybe?","ManageExam:addSectionField");
      return false;
    }
    return true;
  }*/

  boolean addParam(){
    log.paranoid("Beginning try block.","ManageExam:addParam");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Adding TrackAnswers field to ExamParameters","ManageExam:addParam");
      stmt.executeUpdate("ALTER TABLE ExamParameters ADD TrackAnswers VARCHAR(5)");
      log.paranoid("Setting TrackAnswers to false.","ManageExam:addParam");
      stmt.executeUpdate("UPDATE ExamParameters SET TrackAnswers='false'");
    }catch(Exception e){
       log.normal("Caught exception " + e.getMessage() + " (Table doesn't exist, maybe?)","ManageExam:addParam");
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
  
  

}
