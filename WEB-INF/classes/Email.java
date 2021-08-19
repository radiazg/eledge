package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.sql.*;
import java.io.*;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class Email extends HttpServlet {
  RBStore res =  EledgeResources.getEmailBundle();
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
      response.sendRedirect(Course.secureLoginURL + "Email");
      return;
    }
    // from here on, student id is assumed to be valid
	
    if (student.getIsVisitor())
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+res.getString("str_no_visitor"),student));
    else if (student.getIsFrozen())
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+res.getString("str_act_frozen"),student));
    else  // Print the class email directory:
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+emailPage(student),student));    
  }
  
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Email");
      return;
    }
    // from here on, student id is assumed to be valid
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      TA ta=null;
      
      //String sql = "SELECT * FROM Students WHERE (Status!='Frozen' AND Status!='Visitor')";
      //actualizado 24/sept/2004
      String sql = "SELECT  * FROM course_to_student, courses, students"
      		  + " WHERE courses.sectionid=idcourses and studentIDNumber=idstudent and idcourses ='"+student.getCourse_id()+"' AND Status!='Frozen' AND Status!='Visitor' ORDER BY LastName ASC";
                  
      if (student.getIsTA()) {
        ta = TAS.getTA(student.getIDNumber());
        if (ta.getPermission("Email_Send").getPermissionLevel().equals(TAPermission.PERM_NONE)) {
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+Permissions.getPermission("Email_Send").getDenyMsg(),student));
          return;
        }
        if (ta.getPermission("Email_Send").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
          
          //actualizado 24/sept/2004
      		sql = "SELECT  * FROM course_to_student, courses, students"
      		    + " WHERE courses.sectionid=idcourses and studentIDNumber=idstudent" 
      		    + " and idcourses ='"+student.getCourse_id()+"' AND Status!='Frozen'"
      		    + " AND Status!='Visitor' ORDER BY LastName ASC";
      
          /*sql = "SELECT * FROM Students LEFT JOIN TAAssignments ON (TAAssignments.Value=Students.StudentIDNumber) "
            + "WHERE TAAssignments.StudentIDNumber='" + student.getIDNumber() + "'";
        */}
      }
      
      ResultSet rsStudents = stmt.executeQuery(sql);
      String recipients = "";
      String subject = request.getParameter("Subject");
      String text = request.getParameter("Text");
      String recipientType = request.getParameter("RecipientType");
      Message.RecipientType type = Message.RecipientType.BCC;
      if (request.getParameter("RecipientType").equals("To"))
        type = Message.RecipientType.TO;
      if (request.getParameter("Recipients").equals("All")) {
        while (rsStudents.next()) {
          recipients += rsStudents.getString("Email") + ",";
        }
      }
      
      else if (request.getParameter("Recipients").equals("Selected")) {
        String[] recArray = request.getParameterValues("recipient");
        if (recArray==null) out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+res.getString("str_must_select")
          + emailPage(student),student));
        for (int i=0;i<recArray.length;i++) {
	  if (ta != null) {
            if (studentMatches(recArray[i], ta.getAssignedStudents())) {
              log.paranoid("Student " + recArray[i] + " matched. adding to recipients.","Email:doPost");
              recipients += recArray[i];
	    } 
          } else {
	    recipients += recArray[i];
	  }
        }
      } 
      
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Email</em><br><br>"+sendMessage(student,type,
        recipients,subject,text) + emailPage(student),student));
    }
    catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Email:doPost",e);
      out.println(e.getMessage());
    }
  }

  String emailPage(Student student){
    StringBuffer buf = new StringBuffer();
    String permission = TAPermission.PERM_STUDENT;
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      permission = ta.getPermission("Email").getPermissionLevel();
      if (permission.equals(TAPermission.PERM_NONE))
        return Permissions.getPermission("Email").getDenyMsg();
      if (!permission.equals(TAPermission.PERM_STUDENT))
        student.setIsInstructor(true);
    }
    boolean isInstructor = student.getIsInstructor();
    buf.append("<h3>" + res.getString("str_title_email") + "</h3><FORM METHOD=POST>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      String sqlStr1 = "SELECT * FROM Students WHERE Status='Instructor' ORDER BY LastName";
      ResultSet rs1 = stmt.executeQuery(sqlStr1);
      buf.append("<b>" + res.getString("str_field_instructor") + "</b><br>");
      while (rs1.next()) {
        if (isInstructor) 
          buf.append("<input type=checkbox name=recipient value='" + rs1.getString("Email") + ",'>");
        buf.append(rs1.getString("FirstName") + "&nbsp;" + rs1.getString("LastName") + "&nbsp;"
        + "<a href='mailto:" + rs1.getString("Email") + "'>" + rs1.getString("Email") + "</a><br>"); 
      }
      
      String sqlStr3; 
      
      if (!permission.equals(TAPermission.PERM_CONDITIONAL)) {
        
        
        String sqlStr2 = "SELECT  * FROM course_to_teacher, courses, students"
        				+" WHERE courses.sectionid=idcourses and studentIDNumber=idstudent and idcourses ='"+student.getCourse_id()+"' ORDER BY FirstName ASC";
        
        //String sqlStr2 = "SELECT * FROM Students WHERE Status='TA' ORDER BY LastName";
        
        ResultSet rs2 = stmt.executeQuery(sqlStr2);
        if (rs2.isBeforeFirst()) {
          buf.append("<br><b>" + res.getString("str_field_ta") + "</b><br>");
          while (rs2.next()) {
            if (isInstructor)
              buf.append("<input type=checkbox name=recipient value='" + rs2.getString("Email") + ",'>");
            buf.append(rs2.getString("FirstName") + "&nbsp;" + rs2.getString("LastName") + "&nbsp;"
            + "<a href='mailto:" + rs2.getString("Email") + "'>" + rs2.getString("Email") + "</a><br>"); 
          }
        }      
      
      //actualizado sept/23/2004
      sqlStr3 = "SELECT  * FROM course_to_student, courses, students"
      		  + " WHERE courses.sectionid=idcourses and studentIDNumber=idstudent and idcourses ='"+student.getCourse_id()+"' AND Status!='Frozen' AND Status!='Visitor' ORDER BY LastName ASC";
      
      
      /*sqlStr3 = "SELECT * from Students WHERE Status='Current' "
	      + (isInstructor?"":"AND SectionID='" + student.sectionID + "' ")
	      + "ORDER BY LastName";
      */
      
      } else {
       sqlStr3 = "SELECT * FROM Students LEFT JOIN TAAssignments ON "
          + "(TAAssignments.Value=Students.StudentIDNumber AND TAAssignments.StudentIDNumber='"
          + student.getIDNumber() + "') WHERE TAAssignments.Type='Student' AND Students.Status='Current'";
      }
      
      log.paranoid("Executing: " + sqlStr3,"Email:emailPage");
      
      ResultSet rs3 = stmt.executeQuery(sqlStr3);
      buf.append("<p><TABLE><TR VALIGN=TOP><TD>");
      buf.append("<table border=1 cellspacing=0>"
      + "<tr><td><b>" + res.getString("str_field_student") 
      + "</b></td><td><b>" + res.getString("str_field_email") + "</b></td></tr>");
      while (rs3.next()) {
        buf.append("<tr><td>");  
        
        if(student.isRegisteredCourse(rs3.getString("StudentIDNumber"), Integer.parseInt(student.getCourse_id()))) {
        
        	if (isInstructor) 
          		buf.append("<input type=checkbox name=recipient value='" + rs3.getString("Email") + ",'>");
          
          	buf.append(rs3.getString("FirstName") + "&nbsp;" + rs3.getString("LastName") + "</td>"
        	+ "<td>"
        	+ (rs3.getString("ShareInfo").equals("true")||isInstructor?
          	"<a href='mailto:" + rs3.getString("Email") + "'>" + rs3.getString("Email") 
          	+ "</a>":"")
        	+ "</td></tr>"); 
        	}
      	}
      
      buf.append("</table></TD>");
      if (isInstructor) buf.append("<TD>" + emailForm(student) + "</TD>");
      buf.append("</FORM></TR></TABLE>");
    if (student.getIsTA())
      student.setIsInstructor(false);
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Email:emailPage",e);
      return e.toString();
    }
    return buf.toString();
  }
  
  String emailForm(Student student) {
    StringBuffer buf = new StringBuffer();
    InternetAddress fromAddress = null;
    try {
      fromAddress = new InternetAddress(student.email,student.getFullName());
    }
    catch (Exception e) {
      return e.getMessage();
    }
    buf.append("<p><TABLE BORDER=1 CELLSPACING=0>"
    + "<TR><TD ALIGN=RIGHT><b>" + res.getString("str_field_from") + "</b></TD><TD>" 
    + fromAddress.getPersonal() + " &lt;" + fromAddress.getAddress() + "&gt;</TD></TR>"
    + "<TR><TD ALIGN=RIGHT><b>" + res.getString("str_field_to") 
    + "<INPUT TYPE=RADIO NAME=RecipientType VALUE=To><br>"
    + res.getString("str_field_bcc") + "<INPUT TYPE=RADIO NAME=RecipientType VALUE=Bcc CHECKED></b></TD><TD>"
    + "<INPUT TYPE=RADIO NAME=Recipients VALUE=All>" + res.getString("str_to_all") 
    + "<br><INPUT TYPE=RADIO NAME=Recipients VALUE=Selected CHECKED>"
    + res.getString("str_to_selected") + "</TD></TR>"
    + "<TR><TD ALIGN=RIGHT><b>" + res.getString("str_field_subject") + "</b></TD><TD>"
    + "<INPUT SIZE=40 NAME=Subject VALUE='[" + Course.name + "]'></TD></TR>"
    + "<TR><TD COLSPAN=2><TEXTAREA NAME=Text ROWS=15 COLS=50></TEXTAREA></TD></TR>");
    buf.append("</TABLE><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_send") + "'>");
    return buf.toString();
  }
  
  String sendMessage(Student student, Message.RecipientType type, String recipients, 
  String subject, String text) {
    StringBuffer err = new StringBuffer();
    try {
      Properties props = System.getProperties();
      props.put("mail.smtp.host",Course.outgoingMailServer);
      Session session = Session.getDefaultInstance(props,null);
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(student.email,student.getFullName()));
      message.addRecipients(type, recipients);
      message.setSubject(subject);
      message.setText(text);
      Transport.send(message);
      return res.getString("str_msg_sent_ok");
    }
    catch (Exception e) {
      return res.getString("str_send_bad") + err + e.getMessage();
    }
  }

  boolean studentMatches(String recipient, Student[] students) {
    for (int i=0;i<students.length;i++) {
      log.paranoid("students[i]: " + students[i].getIDNumber(),"Email:studentMatches");
      log.paranoid("recipient: " + recipient,"Email:studentMatches");
      if (recipient.equals((students[i].getEmail() + ","))) {
        log.paranoid("recipient and student matched.","Email:studentMatches");
        return true;
      }
    }
    return false;
  }
}
