package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;

public class Scores extends HttpServlet {
  // parameters that determine the properties of the quiz program:
  RBStore res = EledgeResources.getScoresBundle();  
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
      response.sendRedirect(Course.secureLoginURL + "Scores");
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Scores",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+err.toString(),student));
        return;
      }
    }

    if (student.getIsFrozen())
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+res.getString("str_act_frozen"),student));
    else
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+studentScores(student),student));
  }
  
  String studentScores(Student student) {
    StringBuffer buf = new StringBuffer();
    Date now = new Date();
    
    buf.append("<b>" + student.getFullName() + "</b>"
    + "<br>" + now
    + "<h3>" + res.getString("str_curr_scores") + "</h3>"
    + res.getString("str_only_max_shown"));
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
            
      String sqlQueryString = "SELECT Assignment,MAX(Score) AS Score FROM Scores "
      + "WHERE StudentIDNumber='" + student.getIDNumber() + "' AND id_course='"+student.getCourse_id()+"' GROUP BY Assignment";
            
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      
      // print scores in a table
      
      buf.append("<table cellspacing=0 border=1>"
      + "<tr><td><b>" + res.getString("str_assignment") + "</b></td><td><b>"
      + res.getString("str_score") + "</b></td></tr>");
      
      while (rs.next()){
        buf.append("<tr><td>" + rs.getString("Assignment") + "</td>"
        + "<td ALIGN=CENTER>" + (rs.getInt("Score")<0?"ungraded":rs.getString("Score")) + "</td></tr>");
      }
      buf.append("</table>");
      //buf.append("<table border=0><tr><td height=25>&nbsp;</td></tr></table>");
      //buf.append(Homework.getUnsubmittedAssignments(student.getIDNumber()));
      //buf.append("<table border=0><tr><td height=25>&nbsp;</td></tr></table>");
      //buf.append(Quiz.getUnsubmittedAssignments(student.getIDNumber()));
      //buf.append("<table border=0><tr><td height=25>&nbsp;</td></tr></table>");
      //buf.append(Report.getUnsubmittedAssignments(student.getIDNumber()));
      buf.append("<table border=0><tr><td height=25>&nbsp;</td></tr></table>");
      buf.append(Exam.getUnsubmittedAssignments(student.getIDNumber(),student.getCourse_id()));
     
    } 
    catch (Exception e) {
	  try {  // create the missing database table (at time of course creation)
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE Scores (ScoreID INT PRIMARY KEY AUTO_INCREMENT,"
		    + "StudentIDNumber VARCHAR(50),Assignment TEXT,Score INT,Timestamp TIMESTAMP,"
        + "IPAddress VARCHAR(15), Code INT, TestType VARCHAR(8), id_course INT)");
        return studentScores(student);
      }
      catch (Exception e2) {
      }
      return(e.getMessage());
    }

    if (hasGradedEssays(student)){
          buf.append("<FORM METHOD=POST><input type='hidden' name='UserRequest' value='viewGradedEssays'><input type=submit value='" + res.getString("str_view_essays") + "'></FORM>");
    }
    /*if (student.getIsInstructor()) {
	  buf.append("<FORM ACTION=" + Course.name + ".Gradebook>"
	  + "<p><b>" + res.getString("str_teach_only") + " </b><INPUT TYPE=SUBMIT VALUE='"
          + res.getString("str_gradebook") + "'></FORM>");
	}*/ 

    return buf.toString();  // send buffer to html output
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
  
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Scores");
    if (student.getIsFrozen()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+res.getString("str_act_frozen"),student));
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Scores",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+err.toString(),student));
        return;
      }
    }


    //find out what the user wants.

    String userRequest = request.getParameter("UserRequest");
   
    if (userRequest==null || userRequest.equals("main")) 
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Puntuaciones</em><br><br>"+studentScores(student),student));
    if (userRequest.equals("viewGradedEssays"))
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Scores'>Puntuaciones</a> >> Resultados Preguntas de Ensayo</em><br><br>"+studentEssays(student),student));
  }

  boolean hasGradedEssays(Student student){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE StudentIDNumber='";
      sqlQueryString+=student.getIDNumber() + "' AND Graded='true' AND id_course ='"+student.getCourse_id()+"'";      
      ResultSet rsGraded=stmt.executeQuery(sqlQueryString);
      if (rsGraded.next()) return true;
    }catch (Exception e){
      return false;
    }
    return false; 
  }

  String studentEssays(Student student){
    StringBuffer buf=new StringBuffer();
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString="Select * from Essays WHERE StudentIDNumber='";
      sqlQueryString+=student.getIDNumber()+"' AND Graded='true' AND id_course ='"+student.getCourse_id()+"'";
      ResultSet rsEssays=stmt.executeQuery(sqlQueryString);
      if (!rsEssays.next()){
        return res.getString("str_no_essays");
      }
      buf.append("<table>");
      do{
        
        ResultSet rsQuestion=stmt2.executeQuery("Select * from " + rsEssays.getString("TestType") + "Questions WHERE QuestionID='" + rsEssays.getString("QuestionID") + "'");
        if (rsQuestion.next()){
          buf.append("<tr><td valign=top><b>" + res.getString("str_q") 
          + "</b></td><td width=70%>" + rsQuestion.getString("QuestionText") + "</td></tr>");
        }
        buf.append("<tr><td><b>" + res.getString("str_score") + ":</b></td><td width=70%>"
        + rsEssays.getString("Score")+"</td></tr>");
        buf.append("<tr><td valign=top><b>" + res.getString("str_answer") 
        + "</b></td><td width=70%>" + rsEssays.getString("Answer")+"</td></tr>\n");

        buf.append("<tr><td height=15 colspan=2><hr></td></tr>");
      }while (rsEssays.next());

      buf.append("\n</table>");
      buf.append("<div align=center><FORM METHOD=POST>"
      + "<INPUT TYPE=HIDDEN NAME=UserRequest Value=main><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_ret_scores") + "'></FORM></div>");
    }catch(Exception e){
      return e.getMessage();
    } 
    return buf.toString();
  }

}
