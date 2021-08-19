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

public class Usuarios extends HttpServlet {
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
      response.sendRedirect(Course.secureLoginURL + "Login");
      return;
    }
    
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Usuarios",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+err.toString(),student));
        return;
      }
    }
    if (!student.getIsInstructor()) { 
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }

    // from here on, user is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    if (userRequest!=null){
      if (userRequest.equals("HelpTextFile"))
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+helpTextFile(),student));
      return;
    }
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+classUsers(student),student));
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
      response.sendRedirect(Course.secureLoginURL + "Login");
      return;
    }

    if (student.getIsTA()) {
      ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Usuarios",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> <a href='"+Course.name+".Usuarios?'>Usuarios</a> >> Error</em><br><br>"+err.toString(),student));
        return;
      }
    }
    if (!student.getIsInstructor()) { 
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> <a href='"+Course.name+".Usuarios?'>Usuarios</a> >> Restricción</em><br><br>"+res.getString("str_instructor_only_page"),student));
      return;
    }

    // from here on, user is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    if (userRequest == null) {  // first entry; print gradebook
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios </em><br><br>"+classUsers(student),student));
      return;
    }
    
    String id = request.getParameter("StudentIDNumber");    
    if (id == null) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> <a href='"+Course.name+".Usuarios?'>Usuarios</a> >> Error Studiante</em><br><br>"+res.getString("str_must_select_student"),student));
      return;
    }
    
    if (userRequest.equals("DeleteStudent")) {
      if (student.idNumberLooksValid(id)) deleteStudent(id);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+classUsers(student), student));
      return;
    }
	
    if (userRequest.equals("UpdateStudent")) {
      if (student.idNumberLooksValid(id)) updateStudent(id,request,ta);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+classUsers(student), student));
      return;
    }

    if (userRequest.equals("ResetPassword")){
      if (student.idNumberLooksValid(id))
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >> Usuarios</em><br><br>"+resetStudentPasswordForm(id),student));
    }
    
    if (userRequest.equals("ResetStudentPassword")){
      if (!(student.getPassword().equals(request.getParameter("TeacherPass")))){
        out.println(res.getString("str_teacher_passwd_invalid"));
        return;
      }
      String studentPass=request.getParameter("StudentPass");
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>MyEledge</a> >>Usuarios</em><br><br>"+student.resetPassword(studentPass, id) + classUsers(student),student));
      return;
    }
  }

  String classUsers(Student student) {
    int numberOfSections = 1;
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_td_title"));
    Date now = new Date();
        
    buf.append("<h3> Administración de Usuarios </h3>"
    + now
    + "<p>" + "Aquí se visualizan todos los estudiantes del LMS.");

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

           
     //default... if you're a teacher and you like that sorta thing.... 
     
      String sqlStr1 = "SELECT CONCAT(Students.FirstName,'&nbsp;',Students.LastName)  "
      +"AS Name, Students.StudentIDNumber AS ID, "
      +"Students.Status AS STATUS , Students.Email "
      +"AS Email FROM "
      +"Students "
      +"GROUP  BY ID ORDER  BY  STATUS , Students.LastName, Students.FirstName";

      

      ResultSet rs1 = stmt.executeQuery(sqlStr1);
      
      buf.append("<FORM NAME=UpdateStudent METHOD=POST>");
      buf.append("\n<input type=hidden name=UserRequest>");
      buf.append("<input type=SUBMIT value='" + res.getString("str_reset_passwd") + "' onClick=this.form.elements.UserRequest.value='ResetPassword';><BR>");
      buf.append("<table border=1 cellspacing=0>"
      + "<tr><td></td><td><b>" + res.getString("str_field_name") + "</b></td><td><b>" + res.getString("str_field_id") + "</b></td>");

      buf.append("<td><b>" + res.getString("str_field_status") + "</b></td>");

      
      buf.append("</tr>");

      
            
      while (rs1.next()) {
        String id = rs1.getString("ID");
        String name = fixName(rs1.getString("Name"));
        buf.append("\n<tr>"
        + "<td><input type=radio name=StudentIDNumber value='" + rs1.getString("ID") + "'></td>"
        + "<td><a href=mailto:" + rs1.getString("Students.Email") + ">" 
        + rs1.getString("Name") + "</a></td>" + "<td>" + rs1.getString("ID") + "</td>");
        buf.append("<td>" + statusSelectionBox(id,rs1.getString("Status")) + "</td>");

        buf.append("</tr>");
      }

      buf.append("\n</table>");
      
      buf.append("\n<input type=SUBMIT onClick=this.form.elements.UserRequest.value='UpdateStudent';"
      + " VALUE='" + res.getString("str_update_student") + "'>&nbsp;");
      buf.append("\n<input type=BUTTON "
      + "onClick=\"if (confirm('" + res.getString("str_warning_delete_student") + "')) { "
      + "UserRequest.value='DeleteStudent'; UpdateStudent.submit();}\" VALUE='" + res.getString("str_delete_student") + "'>&nbsp;");
      
      buf.append("\n</FORM>");      
      
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
  
  String statusSelectionBox(String studentIDNumber,String status) {
    StringBuffer buf = new StringBuffer("\n<SELECT NAME=Status" + studentIDNumber + ">");
    buf.append("<OPTION VALUE=Current" + (status.equals("Current")?" SELECTED>":">") + res.getString("str_status_current") + "</OPTION>");
    buf.append("<OPTION VALUE=Activo" + (status.equals("Activo")?" SELECTED>":">") + res.getString("str_status_activo") + "</OPTION>");
    buf.append("<OPTION VALUE=Visitor" + (status.equals("Visitor")?" SELECTED>":">") + res.getString("str_status_visitor") + "</OPTION>");
    buf.append("<OPTION VALUE=Frozen" + (status.equals("Frozen")?" SELECTED>":">") + res.getString("str_status_frozen") + "</OPTION>");
    buf.append("<OPTION VALUE=Instructor" + (status.equals("Instructor")?" SELECTED>":">") + res.getString("str_status_instructor") + "</OPTION>");
    buf.append("<OPTION VALUE=TA" + (status.equals("TA")?" SELECTED>":">") +  res.getString("str_status_ta") + "</OPTION>");
    buf.append("</SELECT>");
    return buf.toString();
  }
  
 
      
  String converter(String oldString, int fromIndex) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }
    
  void deleteStudent(String id) {
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
  }

  void updateStudent(String id,HttpServletRequest request, TA ta) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      String newStatus=request.getParameter("Status" + id);
      String newSection="1";
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
 
  String resetStudentPasswordForm(String studentID){
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
  }
  
 
}
