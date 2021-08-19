package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.text.MessageFormat;

public class Gradebook extends HttpServlet {
  RBStore res=EledgeResources.getGradebookBundle(); //i18n


  public String getServletInfo() {
    return "This Eledge servlet module displays a summary of student grades for the Instructor.";  
  }

  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Gradebook");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Gradebook",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+err.toString(),student));
        return;
      }
    }
    if (!student.getIsInstructor()) { 
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }

    // from here on, user is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    if (userRequest!=null){
      if (userRequest.equals("HelpTextFile"))
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+helpTextFile(),student));
      return;
    }
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+classScores(student),student));
	}

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    TA ta = null;
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Gradebook");
      return;
    }

    if (student.getIsTA()) {
      ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Gradebook",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Error</em><br><br>"+err.toString(),student));
        return;
      }
    }
    if (!student.getIsInstructor()) { 
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Restricción</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }

    // from here on, user is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    if (userRequest == null) {  // first entry; print gradebook
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> jajaj</em><br><br>"+classScores(student),student));
      return;
    }
    
    /*if (userRequest.equals("CreateTextFile")) {
      out.println(createTextFile(student));
      return;
    }*/

	  String id = request.getParameter("StudentIDNumber");    
    if (id == null) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Error Studiante</em><br><br>"+res.getString("str_must_select_student"),student));
      return;
    }
    
    /*if (userRequest.equals("DeleteStudent")) {
      if (student.idNumberLooksValid(id)) deleteStudent(id);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+classScores(student), student));
      return;
    }
	
    if (userRequest.equals("UpdateStudent")) {
      if (student.idNumberLooksValid(id)) updateStudent(id,request,ta);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+classScores(student), student));
      return;
    }*/

    if (userRequest.equals("ScoresDetail")) {
      if (student.idNumberLooksValid(id)) out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+scoresDetail(id, student),student));
      return;
    }

    if (userRequest.equals("DeleteScore")) {
      String scoreID = request.getParameter("ScoreID");
      if (scoreID != null) {
        deleteScore(scoreID);
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+scoresDetail(id, student),student));
        return;
      }
      else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas >> Error</em><br><br>"+res.getString("str_must_select_score"),student));
      return;
    }
  
    if (userRequest.equals("AddScore")) {
      String assignment = request.getParameter("Assignment");
      String ipNumber = request.getRemoteAddr();
      int score = 0;
      try {
        score = Integer.parseInt(request.getParameter("Score"));
      }
      catch (NumberFormatException e) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_must_enter_int_score"),student));
        return;
      }
      if (assignment != null) {
        addScore(id,assignment,score,ipNumber, student);
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+scoresDetail(id, student),student));
      }
      else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_must_enter_assign"),student));
      return;
    }

    if (userRequest.equals("ShowAnswers"))
    {

      String scoreID = request.getParameter("ScoreID");
      if (scoreID != null) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+showAnswers(id, scoreID),student));
        return;
      }
      else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_must_select_score"),student));
      return;
    }  
    if (userRequest.equals("GradeEssays")){
      if (student.idNumberLooksValid(id)) out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+gradeEssayForm(id, student),student));
    }
    if (userRequest.equals("UpdateEssay")){
      int score=0;
      try {
        score = Integer.parseInt(request.getParameter("Score"));
      }
      catch (NumberFormatException e) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_must_enter_int_score"),student));
        return;
      }
      String assignment=request.getParameter("Assignment");
      String questionID=request.getParameter("QuestionID");
      String testType=request.getParameter("TestType");
      String gradedAnswer=request.getParameter("Answer");
      boolean email=(request.getParameter("Email")==null?false:true);
      if (questionID != null && testType != null && gradedAnswer != null && assignment != null){
       if((updateEssay(student,id,questionID,testType,gradedAnswer,score,assignment,email)))
         out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+gradeEssayForm(id, student),student));
       else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_error_answer_not_updated"),student));
      }else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_error_essay_page"),student));
    }

    if (userRequest.equals("DeleteEssay")){
      String questionID=request.getParameter("QuestionID");
      String testType=request.getParameter("TestType");
      if (questionID != null && testType != null){
        if(deleteEssay(id,questionID,testType))
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+gradeEssayForm(id, student),student));
        else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_error_answer_not_deleted"),student));
      }else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+res.getString("str_error_essay_page"),student));
    }

    if (userRequest.equals("ShowGradedEssays")){
      if (student.idNumberLooksValid(id)){
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Gradebook?'>Lista de Notas</a> >> Detalle Notas</em><br><br>"+gradedEssays(id, student),student));
      }
    }

    /*if (userRequest.equals("ResetPassword")){
      if (student.idNumberLooksValid(id))
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+resetStudentPasswordForm(id)));
    }*/
    
    /*if (userRequest.equals("ResetStudentPassword")){
      if (!(student.getPassword().equals(request.getParameter("TeacherPass")))){
        out.println(res.getString("str_teacher_passwd_invalid"));
        return;
      }
      String studentPass=request.getParameter("StudentPass");
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Lista de Notas</em><br><br>"+student.resetPassword(studentPass, id) + classScores(student)));
      return;
    }*/
  }

  String classScores(Student student) {
    int numberOfSections = 1;
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_td_title"));
    Date now = new Date();
        
    buf.append("<h3>" + res.getString("str_scores_page_title") + "</h3>"
    + now
    + "<p>" + res.getString("str_scores_explain1") 
    + res.getString("str_scores_explain2"));

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      //ResultSet rs = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      //if (rs.next()) numberOfSections = rs.getInt("Value");
      //String[] sectionNames = new String[numberOfSections+1];
      //ResultSet rsSectionNames = stmt.executeQuery("SELECT SectionName FROM CourseSections");
      //for (int j=1;j<=numberOfSections;j++)
        //if (rsSectionNames.next()) sectionNames[j] = rsSectionNames.getString("SectionName");
      
      String sqlStr2 = "SELECT Assignment FROM Scores WHERE id_course="+student.getCourse_id()+" GROUP BY Assignment ORDER BY Assignment";
      ResultSet rs2 = stmt.executeQuery(sqlStr2);

      Vector names = new Vector();
      while (rs2.next()) names.addElement(rs2.getString("Assignment"));
      String[] assignmentName = new String[names.size()];
      for (int i=0;i<names.size();i++) assignmentName[i] = (String)names.get(i);
      
     //default... if you're a teacher and you like that sorta thing.... 
      String sqlStr1 = "SELECT CONCAT(Students.FirstName,'&nbsp;',Students.LastName)  "
      +"AS Name, Students.StudentIDNumber AS ID, Scores.Assignment AS Assignment, "
      +"MAX( Scores.Score )  AS Score, Students.Status AS STATUS , Students.Email "
      +"AS Email, course_to_student.idcourses "
      +"FROM Students, course_to_student LEFT JOIN Scores ON Scores.StudentIDNumBer= course_to_student.idstudent "
      +"and Scores.id_course=course_to_student.idcourses where course_to_student.idstudent=Students.StudentIDNumber "
      +"and course_to_student.idcourses="+student.getCourse_id()+" GROUP  BY Assignment, ID ORDER  BY  STATUS, " 
      +"Students.LastName, Students.FirstName, Assignment";

;

      if (student.getIsTA()) {
	TA ta = TAS.getTA(student.getIDNumber());
	TAPermission tap = ta.getPermission("Gradebook");
	if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)
	  || tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT))
	  return Permissions.getPermission("Gradebook").getDenyMsg();
	
	/*if (tap.getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
	  //this monstrosity pulls out the student information, like the normal sql string, but.....
	  //this one only pulls out students who are assigned to this ta. Yay. =)
	  sqlStr1 = "SELECT CONCAT(Students.Firstname,'&nbsp;',Students.LastName) "
	    + "AS Name, Students.StudentIDNumber AS ID, Students.SectionID AS "
	    + "SectionID, Scores.Assignment AS Assignment, MAX(Scores.Score) AS "
	    + "Score, Students.Status AS Status, Students.Email AS Email, "
	    + "TAAssignments.StudentIDNumber AS TAID, TAAssignments.Type, "
	    + "TAAssignments.Value FROM Students LEFT OUTER JOIN Scores USING "
	    + "(StudentIDNumber) LEFT OUTER JOIN TAAssignments ON (TAAssignments.StudentIDNumber='" 
	    + student.getIDNumber() 
	    + "' AND TAAssignments.Type='Student' AND TAAssignments.Value=Students.StudentIDNumber) "
	    + "WHERE TAAssignments.Value IS NOT NULL GROUP BY Assignment,ID "
	    + "ORDER BY Status, SectionID, Students.LastName, Students.FirstName, Assignment";
	}*/
      }

      ResultSet rs1 = stmt.executeQuery(sqlStr1);
      
      buf.append("<FORM NAME=UpdateStudent METHOD=POST>");
      buf.append("\n<input type=hidden name=UserRequest>");
      //buf.append("<input type=SUBMIT value='" + res.getString("str_reset_passwd") + "' onClick=this.form.elements.UserRequest.value='ResetPassword';><BR>");
      buf.append("<table border=1 cellspacing=0>"
      + "<tr><td></td><td><b>" + res.getString("str_field_name") + "</b></td><td><b>" + res.getString("str_field_id") + "</b></td>");

      //if (numberOfSections > 1) buf.append("<td><b>" + res.getString("str_field_section") + "</b></td>");

      buf.append("<td><b>" + res.getString("str_field_status") + "</b></td>");

      for (int i=0;i<assignmentName.length;i++) {
        buf.append("<td><b>" + assignmentName[i] + "</b></td>");
      }
      buf.append("</tr>");

      boolean more = rs1.next();
            
      while (more) {
        String id = rs1.getString("ID");
        String name = fixName(rs1.getString("Name"));
        buf.append("\n<tr>"
        + "<td><input type=radio name=StudentIDNumber value='" + rs1.getString("ID") + "'></td>"
        + "<td><a href=mailto:" + rs1.getString("Students.Email") + ">" 
        + rs1.getString("Name") + "</a></td>" + "<td>" + rs1.getString("ID") + "</td>");

        //if (numberOfSections > 1)
         // buf.append("<td>" + sectionSelectBox(rs1.getString("ID"),rs1.getInt("SectionID"),numberOfSections,sectionNames) + "</td>");

        buf.append("<td>" + rs1.getString("Status") + "</td>");

        if (rs1.getString("Assignment") == null) { // no assignments done yet for this student
          for(int i=0;i<assignmentName.length;i++) {
            Object[] args = { assignmentName[i],name };
            buf.append("<td title='"
              + mf.format(args) + "'>&nbsp;</td>");
          }
         more = rs1.next();
        }
        else { // complete the row of scores
          for(int i=0;i<assignmentName.length;i++) {
            Object[] args = {assignmentName[i], name };
            if (!more) buf.append("<td title='" + mf.format(args) 
                + "'>&nbsp;</td>");
    	    else if (rs1.getString("Assignment") == null) buf.append("<td "
                + "title='" + mf.format(args) + "'>&nbsp;</td>");
            else {
              if (rs1.getString("ID").equals(id) & rs1.getString("Assignment").toUpperCase().equals(assignmentName[i].toUpperCase())) {
                buf.append("<td ALIGN=CENTER title='"
                  + mf.format(args) + "'>" + (rs1.getInt("Score")<0?res.getString("str_not_graded"):rs1.getString("Score")) + "</td>");
                more = rs1.next();
              }
              else buf.append("<td title='" + mf.format(args) + "'>&nbsp;</td>");
            }
          }
        }
        buf.append("</tr>");
      }

      buf.append("\n</table>");
      
      //buf.append("\n<input type=SUBMIT onClick=this.form.elements.UserRequest.value='UpdateStudent';"
      //+ " VALUE='" + res.getString("str_update_student") + "'>&nbsp;");
      //buf.append("\n<input type=BUTTON "
      //+ "onClick=\"if (confirm('" + res.getString("str_warning_delete_student") + "')) { "
      //+ "UserRequest.value='DeleteStudent'; UpdateStudent.submit();}\" VALUE='" + res.getString("str_delete_student") + "'>&nbsp;");
      buf.append("\n<input type=SUBMIT onClick=this.form.elements.UserRequest.value='ScoresDetail';"
      + " VALUE='" + res.getString("str_scores_detail") + "'>");
      if(hasUngradedEssays(student)){
        buf.append("\n<input type=SUBMIT onClick=this.form.elements.UserRequest.value='GradeEssays'; VALUE='" + res.getString("str_grade_essays") + "'>");
      }
      buf.append("\n</FORM>");
      
      //buf.append("<FORM METHOD=POST><INPUT TYPE=HIDDEN NAME=UserRequest VALUE=CreateTextFile>"
      //+ "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_export_scores") + "'> "
      //+ "<a href=" + Course.name + ".Gradebook?UserRequest=HelpTextFile>" + res.getString("str_explain_export") + "</a></FORM>");
      
      /*if (!student.getIsTA()) {
        buf.append("<BR><hr width=40%><FORM METHOD=GET ACTION=\"" + Course.name 
            + ".ManageTAPermissions\"><INPUT TYPE=SUBMIT VALUE='" 
            + res.getString("str_manage_ta") + "'></FORM>");
      }*/
    } catch (Exception e) {
      return e.getMessage() + buf.toString();
    }
    
    return buf.toString();
  }  

  String fixName(String oldName) {
    StringBuffer on = new StringBuffer(oldName);
    on.replace(oldName.indexOf('&'),(oldName.indexOf(';')+1)," ");
    return on.toString();
  }

  /*String createTextFile(Student student) {
    int numberOfSections = 1;
    StringBuffer buf = new StringBuffer();
    Date now = new Date();
        
    buf.append(now + "\n");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rs.next()) numberOfSections = rs.getInt("Value");
      String[] sectionNames = new String[numberOfSections+1];
      ResultSet rsSectionNames = stmt.executeQuery("SELECT SectionName FROM CourseSections");
      for (int j=1;j<=numberOfSections;j++)
        if (rsSectionNames.next()) sectionNames[j] = rsSectionNames.getString("SectionName");      
      String sqlStr2 = "SELECT Assignment FROM Scores GROUP BY Assignment ORDER BY Assignment";
      ResultSet rs2 = stmt.executeQuery(sqlStr2);
      Vector names = new Vector();
      while (rs2.next()) names.addElement(rs2.getString("Assignment"));
      String[] assignmentName = new String[names.size()];
      for (int i=0;i<names.size();i++) assignmentName[i] = (String)names.get(i);      
      String sqlStr1 = "SELECT CONCAT(Students.FirstName,'&nbsp;',Students.LastName)  "
      +"AS Name, Students.StudentIDNumber AS ID, Scores.Assignment AS Assignment, "
      +"MAX( Scores.Score )  AS Score, Students.Status AS STATUS , Students.Email "
      +"AS Email, course_to_student.idcourses, course_to_student.idstudent FROM "
      +"course_to_student, Students LEFT JOIN Scores USING ( StudentIDNumber ) "
      +"WHERE course_to_student.idcourses='"+student.getCourse_id()+"' and course_to_student.idstudent=Students.StudentIDNumber "
      +"GROUP  BY Assignment, ID ORDER  BY  STATUS , Students.LastName, Students.FirstName, Assignment";

      if (student.getIsTA()) {
	TA ta = TAS.getTA(student.getIDNumber());
	TAPermission tap = ta.getPermission("Gradebook");
	if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)
	  || tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT))
	  return Permissions.getPermission("Gradebook").getDenyMsg();
	
	if (tap.getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
	  //this monstrosity pulls out the student information, like the normal sql string, but.....
	  //this one only pulls out students who are assigned to this ta. Yay. =)
	  sqlStr1 = "SELECT CONCAT(Students.Firstname,'&nbsp;',Students.LastName) "
	    + "AS Name, Students.StudentIDNumber AS ID, Students.SectionID AS "
	    + "SectionID, Scores.Assignment AS Assignment, MAX(Scores.Score) AS "
	    + "Score, Students.Status AS Status, Students.Email AS Email, "
	    + "TAAssignments.StudentIDNumber AS TAID, TAAssignments.Type, "
	    + "TAAssignments.Value FROM Students LEFT OUTER JOIN Scores USING "
	    + "(StudentIDNumber) LEFT OUTER JOIN TAAssignments ON (TAAssignments.StudentIDNumber='" 
	    + student.getIDNumber() 
	    + "' AND TAAssignments.Type='Student' AND TAAssignments.Value=Students.StudentIDNumber) "
	    + "WHERE TAAssignments.Value IS NOT NULL GROUP BY Assignment,ID "
	    + "ORDER BY Status, SectionID, Students.LastName, Students.FirstName, Assignment";
	}
      }

      ResultSet rs1 = stmt.executeQuery(sqlStr1);
      buf.append(res.getString("str_field_name") + "\t" + res.getString("str_field_id") + "\t");
      if (numberOfSections > 1) buf.append(res.getString("str_field_section") +"\t");
      buf.append(res.getString("str_field_status") + "\t");
      for (int i=0;i<assignmentName.length;i++) buf.append(assignmentName[i] + "\t");
      buf.append("\n");
      boolean more = rs1.next();            
      while (more) {
        String id = rs1.getString("ID");
        buf.append(rs1.getString("Name") + "\t\"" + rs1.getString("ID") + "\"\t");
        if (numberOfSections > 1)
          buf.append(rs1.getInt("SectionID") + "\t");
        buf.append(rs1.getString("Status") + "\t");
        if (rs1.getString("Assignment") == null) { // no assignments done yet for this student
          for(int i=0;i<assignmentName.length;i++) buf.append("\t");
          more = rs1.next();
        }
        else { // complete the row of scores
          for(int i=0;i<assignmentName.length;i++) {
            if (!more) buf.append("\t");
    	      else if (rs1.getString("Assignment") == null) buf.append("\t");
            else {
              if (rs1.getString("ID").equals(id) & rs1.getString("Assignment").toUpperCase().equals(assignmentName[i].toUpperCase())) {
                buf.append(rs1.getString("Score") + "\t");
                more = rs1.next();
              }
              else buf.append("\t");
            }
          }
        }
        buf.append("\n");
      }
    } catch (Exception e) {
      return e.getMessage() + buf.toString();
    }    
    return buf.toString();
  }*/

  String helpTextFile() {
    StringBuffer buf = new StringBuffer();
    buf.append("<h3>" + res.getString("str_export_title") + "</h3>");
    buf.append(res.getString("str_explain_export2"));
    return buf.toString();
  }
    
  /*String sectionSelectBox(String studentIDNumber,int sectionID,int numberOfSections,String[] sectionNames) {
    StringBuffer buf = new StringBuffer("\n<SELECT NAME=Section" + studentIDNumber + ">");
    for (int j=1;j<=numberOfSections;j++) {
      buf.append("<OPTION VALUE=" + j + (j==sectionID?" SELECTED>":">") + sectionNames[j] + "</OPTION>");
    }
    buf.append("</SELECT>");
    return buf.toString();
  }*/
  
  /*String statusSelectionBox(String studentIDNumber,String status) {
    StringBuffer buf = new StringBuffer("\n<SELECT NAME=Status" + studentIDNumber + ">");
    buf.append("<OPTION VALUE=Current" + (status.equals("Current")?" SELECTED>":">") + res.getString("str_status_current") + "</OPTION>");
    buf.append("<OPTION VALUE=Visitor" + (status.equals("Visitor")?" SELECTED>":">") + res.getString("str_status_visitor") + "</OPTION>");
    buf.append("<OPTION VALUE=Frozen" + (status.equals("Frozen")?" SELECTED>":">") + res.getString("str_status_frozen") + "</OPTION>");
    buf.append("<OPTION VALUE=Instructor" + (status.equals("Instructor")?" SELECTED>":">") + res.getString("str_status_instructor") + "</OPTION>");
    buf.append("<OPTION VALUE=TA" + (status.equals("TA")?" SELECTED>":">") +  res.getString("str_status_ta") + "</OPTION>");
    buf.append("</SELECT>");
    return buf.toString();
  }*/
  
  void deleteScore(String scoreID) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsCodeType = stmt.executeQuery("SELECT Code, TestType FROM Scores WHERE " + "ScoreID='" + scoreID + "'");
      String sqlUpdateString = "DELETE FROM Scores WHERE ScoreID='" + scoreID + "'";
      if (stmt.executeUpdate(sqlUpdateString) != 1) return;
      if (!rsCodeType.next())
         return;
      else
      {
         int code = rsCodeType.getInt("Code");
         String testType = rsCodeType.getString("TestType"); 
         if (code == -1 || testType.equals("void"))
            return;
         String mysqlString = "DELETE FROM " + testType + "AssignedQuestions WHERE " + "Code='" + code + "'";
          if (stmt.executeUpdate(mysqlString) != 1) return;
      }
       
      stmt.close();
      conn.close();      
    } catch (Exception e) {
    }    
    return;
  }
      
  void addScore(String studentIDNumber, String assignment, int score, String ipNumber, Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      String sqlUpdateString = "INSERT INTO Scores "
      + "(StudentIDNumber,Assignment,Score,IPAddress,Code,TestType,id_course) "
      + "VALUES ('" + studentIDNumber + "','" + converter(assignment,0)
      + "'," + score + ",'" + ipNumber + "','-1','void','"+student.getCourse_id()+"')";
      
      if (stmt.executeUpdate(sqlUpdateString) != 1) return;
      stmt.close();
      conn.close();      
    } catch (Exception e) {
    }    
    return;
  }
      
  String converter(String oldString, int fromIndex) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }
    
  /*void deleteStudent(String id) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      stmt.executeUpdate("DELETE FROM Students WHERE StudentIDNumber='" + id + "'");
      stmt.executeUpdate("DELETE FROM Scores WHERE StudentIDNumber='" + id + "'");
      stmt.close();
      conn.close();      
    } catch (Exception e) {
    }    
    return;
  }*/

  /*void updateStudent(String id,HttpServletRequest request, TA ta) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      String newStatus=request.getParameter("Status" + id);
      String newSection=request.getParameter("Section" + id);
      String sqlUpdateString = "UPDATE Students "
      + "SET SectionID='" + newSection + "',"
      + " Status='" + newStatus  + "' "
      + "WHERE StudentIDNumber='" + id + "'";
      if (ta != null) {//if not null, then stud is a ta
        //only modify stuff if stud is a ta and permission is conditional.
        //theoretically... ;) If the student got this far as a TA, their
        //permission level -MUST- be higher than none and student.
        if (ta.getPermission("Gradebook_UpdateStudent").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
          //don't need to worry about checking for the assignment here,
          //because "hasPermission" already check to make sure this student is
          //assigned to this ta... so, we'll just pull out the student. =)
          //note that we double check the student assignment with the
          //stud==null below... ;)
          Student stud = (Student)(ta.getAssignment(TA.ASSIGNMENT_STUDENT,id));
          //conditional ta's not alowed to mess with other TA's...
          if (stud == null || stud.getIsTA()) 
            return;
          sqlUpdateString = "UPDATE Students SET ";
          if (ta.isAssigned(TA.ASSIGNMENT_SECTION,newSection)) {
            sqlUpdateString+="SectionID='" + newSection + "', ";
          }  else {
            sqlUpdateString+="SectionID=SectionID, ";
          }

          //coniditional ta's also not allowed to set someone to the status of TA.
          if (newStatus.equals("TA"))  {
            sqlUpdateString+="Status=Status";
          } else {
            sqlUpdateString+="Status='" + newStatus + "'";
          }

          sqlUpdateString+=" WHERE StudentIDNumber='" + id + "'";
        }
      }

      if (newStatus.equals("TA")) {
        if (ta==null || ta.getPermission("Gradebook_UpdateStudent").getPermissionLevel().equals(TAPermission.PERM_ALL)) {
          TA newTa = new TA(id,false,true);
          newTa.setupNewTA();
        }
      }

      if (stmt.executeUpdate(sqlUpdateString) != 1) throw new Exception("Unknown database error.");

    } catch (Exception e) {
      return;
    }    
    return;
  }*/
  
  String showAnswers(String studentID, String scoreID)
  {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();

//get the Code and the type of test from the Score table.
      ResultSet rsCodeType = stmt.executeQuery("SELECT Code, Assignment, TestType FROM Scores WHERE" + " ScoreID='" + scoreID + "'");
      if (!rsCodeType.next() || rsCodeType.getString("Code")==null 
          || rsCodeType.getString("TestType")==null)
         return res.getString("str_invalid_score");
      if (rsCodeType.getString("TestType").equals("Review")) {
	      return res.getString("str_is_review");
      }
      int code = rsCodeType.getInt("Code");
      String testType = rsCodeType.getString("TestType"); 
      String assignment = rsCodeType.getString("Assignment"); 
      rsCodeType.close();
//Check to make sure this is not a teacher inputted score.
      if (code == -1 || testType.equals("void")) return res.getString("str_score_by_teacher");

//Check to make sure tracking is enabled for this type of test.
      try{
        ResultSet rsTrackAnswers = stmt.executeQuery("SELECT TrackAnswers FROM " + testType + "Parameters");
        if (rsTrackAnswers.next() && !rsTrackAnswers.getBoolean("TrackAnswers"))
        {
          buf.append(res.getString("str_tracking_disabled"));
          if (testType.equals("Quiz"))
            buf.append(res.getString("str_quizzes"));
          else if (testType.equals("Homework"))
            buf.append(res.getString("str_homework"));
          else
            buf.append(res.getString("str_exams"));
          return buf.toString();
        }

      }catch(Exception e){
        if(addParam(testType))
           return (res.getString("str_param_track_answers_added1") + testType + res.getString("str_param_track_answers_added2"));
      }

      ResultSet rsStudentName = stmt.executeQuery("SELECT CONCAT(FirstName,' ',LastName)"
        + " AS Name " + "FROM Students WHERE StudentIDNumber LIKE '" + studentID + "'");

      String mySqlQuery1 = "SELECT * FROM " + testType + "AssignedQuestions WHERE "
                           + "Code='" + code + "'";
      buf.append("<h3>".concat(assignment).concat("</h3>"));
      buf.append("<div align=center><h2>"); 
//Check to make sure student is valid
      if (rsStudentName.next()){
        buf.append(rsStudentName.getString("Name"));
      }
      else return res.getString("str_invalid_student");
      rsStudentName.close();
      buf.append( res.getString("str_answers") + " </h2></div>\n<FORM METHOD=POST>\n"
      + "<INPUT type=hidden name=StudentIDNumber value='" + studentID + "'>\n<ol>");
      ResultSet rs1= stmt.executeQuery(mySqlQuery1);
      
      if (rs1.next()){
        do{ 
          String mySqlQuery2 = "SELECT * FROM " + testType + "Questions WHERE QuestionID='"
                      + rs1.getString("QuestionID") + "'";
          ResultSet rs2 = stmt2.executeQuery(mySqlQuery2);
          if (!rs2.next()){
            buf.append("<li>" + res.getString("str_invalid_question"));
            continue;
          }
          Question question = new Question();
          question.loadQuestionData(rs2);
          if (rs1.getBoolean("Graded")){
            buf.append("<li>" + question.printCorrection(rs1.getString("StudentAnswer")));
          }
          else buf.append("<li>" + question.print());
          //buf.append("<br><i>Correct answer: " + question.getCorrectAnswer()
          //           + "</i><br><br>");
          rs2.close();
        }while(rs1.next());
      }
      else buf.append(res.getString("str_not_avail_answers"));
      buf.append("</ol><INPUT type=hidden name=UserRequest value='ScoresDetail'>"
        + "<INPUT type=SUBMIT value='" + res.getString("str_return_to_score") + "'>\n</FORM>");
  
      stmt.close();
      stmt2.close();
      conn.close();
    } catch (Exception e) {
      //if (createAQTable())
        // return res.getString("str_tables_created");
     // else
      // return (e.getMessage());
    } 
    return buf.toString();
  }
  
  String scoresDetail(String id, Student student) {
    StringBuffer buf = new StringBuffer();
    SimpleDateFormat dfDateTime = new SimpleDateFormat("MM-dd-yyyy h:mm a");
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
            
      String sqlStr1 = "SELECT CONCAT(FirstName,' ',LastName) AS Name "
      + "FROM Students WHERE StudentIDNumber LIKE '" + id + "'";      
      ResultSet rs1 = stmt.executeQuery(sqlStr1);
      
      if (!rs1.next()) return res.getString("str_student_not_found");
      
      buf.append("<h4>" +  res.getString("str_scdetail_for") + rs1.getString("Name") + "</h4>");

      
      String sqlStr2 = "SELECT ScoreID,Assignment,Score,Timestamp,IPAddress FROM Scores "
      + "WHERE StudentIDNumber LIKE '" + id + "' AND id_course='"+student.getCourse_id()+"' ORDER BY ScoreID DESC";
      ResultSet rs2 = stmt.executeQuery(sqlStr2);

      // print a button to return to the Gradebook page
      buf.append("<FORM><INPUT TYPE=SUBMIT VALUE='" +  res.getString("str_return_gradebook") + "'></FORM><HR>");

      // print a form for adding a new score to the table
      buf.append("<FORM METHOD=POST>");
      buf.append("<table border=1 cellspacing=0>"
      + "<tr><td><b>" +  res.getString("str_assignment") + "</b></td><td><b>" +  res.getString("str_score_input") + "</b></td></tr>"
      + "<tr><td><input name=Assignment></td><td align=center><input size=3 name=Score></td></tr>"
      + "</table>"
      + "<input type=hidden name=StudentIDNumber value='" + id + "'>"
      + "<input type=hidden name=UserRequest value=AddScore>"
      + "<input type=submit value='" +  res.getString("str_add_score") + "'>"
      + "</FORM>");

      // print scores in a form/table
      buf.append("\n<HR><FORM METHOD=POST>");
      buf.append("\n<input type=submit onClick=this.form.elements.UserRequest.value='DeleteScore'; value='" +  res.getString("str_delete_score") + "'>");
      buf.append("<input type=submit onClick=this.form.elements.UserRequest.value='ShowAnswers'; value='" +  res.getString("str_show_answers") + "'>");
      buf.append("\n<table cellspacing=0 border=1>"
      + "\n<tr><td></td>"
      + "<td><b>" +  res.getString("str_field_assignment") + "</b></td><td><b>" +  res.getString("str_field_score") + "</b></td>"
      + "<td><b>" +  res.getString("str_field_timestamp") + "</b></td><td><b>" +  res.getString("str_field_ip") + "</b></td></tr>");
      
      while (rs2.next())
        buf.append("\n<tr><td><input type=radio name=ScoreID value='"
        + rs2.getString("ScoreID") + "'></td>"
        + "<td>" + rs2.getString("Assignment") + "</td>"
        + "<td ALIGN=CENTER>" + (rs2.getInt("Score")<0?res.getString("str_not_graded"):rs2.getString("Score"))  + "</td>"
        + "<td>" + dfDateTime.format(rs2.getTimestamp("Timestamp")) + "</td>"
        + "<td>" + rs2.getString("IPAddress") + "</td></tr>");

      buf.append("\n</table>");
      buf.append("\n<input type=hidden name=StudentIDNumber value='" + id + "'>");
      buf.append("\n<input type=hidden name=UserRequest>");
      buf.append("\n<input type=submit onClick=this.form.elements.UserRequest.value='DeleteScore'; value='" +  res.getString("str_delete_score") + "'>"); 
      buf.append("<input type=submit onClick=this.form.elements.UserRequest.value='ShowAnswers'; value='" +  res.getString("str_show_answers") + "'>");
      if(hasGradedEssays(id, student))
        buf.append("<br><input type=submit onClick=this.form.elements.UserRequest.value='ShowGradedEssays' value='" +  res.getString("str_review_essays") + "'>");
      buf.append("\n</FORM>");
      
      // print a button to return to the Gradebook page
      buf.append("<hr><FORM><INPUT TYPE=SUBMIT VALUE='" +  res.getString("str_return_gradebook") + "'></FORM>");
    } 
    catch (Exception e) {
      if (addCodeType())
        return res.getString("str_fields_created");
      else
        return(e.getMessage());
    }
    
    return buf.toString();  // send buffer to html output  
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
  
  boolean addParam(String testType){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE "+testType+"Parameters ADD TrackAnswers VARCHAR(5)");
      stmt.executeUpdate("UPDATE "+testType+"Parameters SET TrackAnswers='false'");
     }catch(Exception e){
        return false;
     }
     return true;
  }
    
  /*boolean createAQTable(){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //stmt.executeUpdate("CREATE TABLE QuizAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
      stmt.executeUpdate("CREATE TABLE ExamAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)"); 
      //stmt.executeUpdate("CREATE TABLE HomeworkAssignedQuestions (Code INT, QuestionID INT, Graded VARCHAR(5), StudentAnswer TEXT)");
    }catch(Exception e){
      return false;
    }
    return true;
  }*/

  boolean hasUngradedEssays(Student student){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE Graded='false' AND id_course ='"+student.getCourse_id()+"'";
      ResultSet rsGraded=stmt.executeQuery(sqlQueryString);
      if (rsGraded.next()) return true;
    }catch (Exception e){
    }
    return false;
  }

  boolean hasGradedEssays(String id, Student student){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE Graded='true' AND StudentIDNumber='" + id + "' AND id_course='"+student.getCourse_id()+"'";
      ResultSet rsGraded=stmt.executeQuery(sqlQueryString);
      if (rsGraded.next()) return true;
    }catch (Exception e){
    }
    return false;
  }

  String gradeEssayForm(String studentIDNumber, Student student){
    StringBuffer buf=new StringBuffer();
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE StudentIDNumber='";
      sqlQueryString+=studentIDNumber+"' AND Graded='false' AND id_course='"+student.getCourse_id()+"'";
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      if (!rsEssays.next()){
        return res.getString("str_no_ungraded_essays") + "<form><input type=submit value='" + res.getString("str_return_gradebook") + ".'></form>";
      }
      buf.append("<h3>" + res.getString("str_ungrade_essays") + studentIDNumber + "</h3>");
      do{
        buf.append("\n<FORM METHOD=POST><input type=hidden name='UserRequest'><table border=0>");
         ResultSet rsQuestion=stmt2.executeQuery("Select * from " + rsEssays.getString("TestType") + "Questions WHERE QuestionID='" + rsEssays.getString("QuestionID") + "'");
        if (!rsQuestion.next()){
          return res.getString("str_error_question");
        }
        buf.append("\n<tr><td><b>" +  res.getString("str_field_question") + ":</b></td><td width=70%><pre>"+rsQuestion.getString("QuestionText")+"</pre></td></tr>");
        buf.append("<INPUT TYPE=HIDDEN NAME='Assignment' VALUE='"+(rsEssays.getString("TestType").equals("Homework")?"HW":rsEssays.getString("TestType"))+(rsQuestion.getInt("AssignmentNumber")<10?"0":"")+rsQuestion.getString("AssignmentNumber")+"'>");

        buf.append("\n<tr><td valign=top><b>" +  res.getString("str_field_answer") + ":</b></td><td width=70%><TEXTAREA ROWS=10 COLS=50 WRAP=SOFT NAME=Answer>" + rsEssays.getString("Answer")+"</TEXTAREA></td></tr>");
        buf.append("\n<tr><td><b>" +  res.getString("str_field_score") + ":</b> <input type=text name='Score' size=3></td>");
        buf.append("\n<td><table width=100%><tr><td align=left width=50%><INPUT type='submit' value='" +  res.getString("str_submit_grade") + "' onClick=this.form.elements.UserRequest.value='UpdateEssay';></td>");
        buf.append("<td width=50% align=left><INPUT TYPE=SUBMIT onClick=\"if (confirm('" +  res.getString("str_delete_warning") + "')) {");
        buf.append(" UserRequest.value='DeleteEssay' } else { UserRequest.value='GradeEssays' }\"; value='" +  res.getString("str_delete_answer") + "'></td></tr></table>");
        buf.append("</td></tr>");
        buf.append("<tr><td colspan=2 valign=center height=30><input type=checkbox name=Email value=true>" +  res.getString("str_send_grade") + "</td></tr>");
        buf.append("\n<input type='hidden' name='QuestionID' value='"+rsEssays.getString("QuestionID")+"'>");
        buf.append("\n<input type='hidden' name='StudentIDNumber' value='"+studentIDNumber+"'>");
        buf.append("\n<input type='hidden' name='TestType' value='"+rsEssays.getString("TestType")+"'>");
        buf.append("<tr><td colspan=2 height=20><hr></td></tr></table>");
        buf.append("</FORM>");
      }while (rsEssays.next());

      buf.append("\n<FORM><INPUT TYPE=SUBMIT VALUE='" +  res.getString("str_return_gradebook") + "'></FORM>");
    }catch(Exception e){
      return e.getMessage();
    }
    return buf.toString();
  }

  boolean updateEssay(Student student, String studentIDNumber, String questionID, String testType, String gradedAnswer, int score, String assignment, boolean email){
//note: try setting scores for ungraded essay questiosn to -questionID, and in gradebook and scores classes, rather than checking for -1 score, check for score<0.
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      String sqlUpdateString="Update Essays SET Graded='true', Answer='"+CharHider.quot2literal(gradedAnswer)+"', Score='"+score+"' WHERE Graded='false' AND StudentIDNumber='"+studentIDNumber+"' AND QuestionID='"+questionID+"' AND TestType='"+testType+"'";
      stmt.executeUpdate(sqlUpdateString);

//students won't ever have more than one score per Essay question in the Score table, so, not worried about the test code in this delete.
      sqlUpdateString="DELETE From Scores WHERE StudentIDNumber='"+studentIDNumber+"' AND TestType='"+testType+"' AND Score='-"+questionID+"'";
      stmt.executeUpdate(sqlUpdateString);

//since students only ever see the max score anyway, and, since adding a set score to all score values will result in the max score being the same one as before, add the essay grade to all scores of corresponding assignment.
      sqlUpdateString="Update Scores Set Score=Score+" +score +" WHERE StudentIDNumber='" + studentIDNumber + "' AND Assignment='" + assignment + "'";
      stmt.executeUpdate(sqlUpdateString);
      if (email){
        sendMessage(student, studentIDNumber, Message.RecipientType.TO, (gradedAnswer+ "\n\rFinal Score: " + score));
      }
    }catch(Exception e){
      return false;
    }
    return true;
  }

  boolean deleteEssay(String studentIDNumber, String questionID, String testType){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM Essays WHERE StudentIDNumber='"+studentIDNumber+"' AND QuestionID='"+questionID+"' AND TestType='"+testType+"'");
      stmt.executeUpdate("DELETE FROM Scores WHERE StudentIDNumber='"+studentIDNumber+"' AND TestType='"+testType+"' AND Score='-"+questionID+"'");
    }catch(Exception e){
      return false;
    }
    return true; 
  }

  String gradedEssays(String id, Student student){
    StringBuffer buf=new StringBuffer();
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE StudentIDNumber='";
      sqlQueryString += id + "' AND Graded='true' AND id_course='"+student.getCourse_id()+"'";
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      if (!rsEssays.next()) return res.getString("str_no_graded_essays");
      buf.append("<h3>" +  res.getString("str_graded_essays") + id);
      buf.append("<table width=100%>");
      do{
        ResultSet rsQuestion=stmt2.executeQuery("Select * from " + rsEssays.getString("TestType") + "Questions WHERE QuestionID='" + rsEssays.getString("QuestionID") + "'");
        if (rsQuestion.next()){
          buf.append("<tr><td valign=top><b>");
          buf.append("\n" +  res.getString("str_field_question") + ":</b></td><td width=70%>" + rsQuestion.getString("QuestionText") + "</td></tr>");
        }
        buf.append("\n<tr><td><b>" +  res.getString("str_field_score") + ":</b></td><td width=70%> "+rsEssays.getString("Score")+"</td></tr>");
        buf.append("<tr><td valign=top><b>" +  res.getString("str_field_graded_answer") + ":</b></td><td width=70%>" + rsEssays.getString("Answer")+"</td></tr>\n");
       buf.append("<tr><td height=15 colspan=2><hr></td></tr>");

      }while(rsEssays.next());

      buf.append("\n</table>");
      buf.append("<FORM METHOD=POST><INPUT TYPE=hidden name='StudentIDNumber' value='"+id+"'><INPUT TYPE=hidden name='UserRequest' VALUE='ScoresDetail'><INPUT TYPE=SUBMIT VALUE='" +  res.getString("str_return_to_score") + "'></FORM>");
    }catch(Exception e){
      return e.getMessage();
    } 
    return buf.toString();

  }

  String sendMessage(Student student, String id, Message.RecipientType type, String text) {
    StringBuffer err = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsRecipient=stmt.executeQuery("SELECT Email FROM Students WHERE StudentIDNumber='" + id + "'");
      if (!rsRecipient.next()) return res.getString("str_student_not_found");
      Properties props = System.getProperties();
      props.put("mail.smtp.host",Course.outgoingMailServer);
      Session session = Session.getDefaultInstance(props,null);
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(student.email,student.getFullName()));
      message.addRecipients(type, rsRecipient.getString("Email"));
      message.setSubject(res.getString("str_mail_subject"));
      message.setText(text);
      Transport.send(message);
      return res.getString("str_mail_sent");
    }
    catch (Exception e) {
      return "sendMessage error: " + err + e.getMessage();
    }
  }
 
  /*String resetStudentPasswordForm(String studentID){
    StringBuffer buf = new StringBuffer();
    buf.append("<h3>" +  res.getString("str_change_passwd") + studentID + "</h3>");
    buf.append("<SCRIPT><!--");
    buf.append("\nfunction verifypass(){"
    + "\n if(document.passform.StudentPass.value.length < 6 || document.passform.StudentPass.value.length > 12){"
    + "\n  alert('" +  res.getString("str_invalid_passwd") + "');\n  return false;\n }\n return true;\n}"
    + "\n --> </SCRIPT>");
    buf.append("<FORM METHOD=POST NAME=passform><TABLE BORDER=0><TR><TD>" +  res.getString("str_new_passwd") + ":</TD>");
    buf.append("<TD><INPUT TYPE=PASSWORD NAME='StudentPass'></TD></TR>");
    buf.append("<TR><TD>" +  res.getString("str_teach_passwd") + ":</TD>");
    buf.append("<TD><INPUT TYPE=PASSWORD NAME='TeacherPass'></TD></TR></TABLE>");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest Value='ResetStudentPassword'>"
    + "<INPUT TYPE=HIDDEN NAME=StudentIDNumber VALUE='" + studentID + "'>"
    + "<INPUT TYPE=BUTTON VALUE='" +  res.getString("str_set_passwd") + "' onClick=\"if (verifypass()){document.passform.submit()}\"></FORM>"); 
    return buf.toString();
  }*/
  
 
}
