package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;

public class Syllabus extends HttpServlet {
  boolean isPublicDocument = true; // controls whether syllabus can be viewed by all or only authenticated students
  private RBStore res = EledgeResources.getSyllabusBundle();
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

    if (student.isAuthenticated() || isPublicDocument ) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Instrucciones</em><br><br>"+syllabusPage(student.getIsInstructor(),Integer.parseInt(student.getCourse_id())),student));    
    }
    else {
      response.sendRedirect(Course.secureLoginURL + "Syllabus");
      return;
    }
    // from here on, user is assumed to be an authenticated student or instructor
  }

  String syllabusPage(boolean isInstructor, int courseID){
    String webPage = new Page().webPage("Syllabus",isInstructor, courseID);
    if (webPage==null) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        createDefaultSyllabusPage(stmt, Integer.toString(courseID));
      }
      catch (Exception e) {
        return e.getMessage();
      }
      webPage = new Page().webPage("Syllabus",isInstructor,courseID);
    }
    return webPage;
  }

  void createDefaultSyllabusPage(Statement stmt, String courseID) {
    
    
    
    try {
     StringBuffer buf = new StringBuffer();
     
     buf.append("<h2>" + res.getString("str_el_syll") + "</h2>");
     buf.append("<strong>"+res.getString("str_intro_mathcad")+"</strong><p>");
     buf.append("<strong>"+res.getString("str_instructor") + "</strong><p> Chuck Wight <br>" + res.getString("str_office") + " 2424 HEB<br>");
     buf.append(res.getString("str_email") + " <A href=mailto:Chuck.Wight@utah.edu>Chuck.Wight@utah.edu</A><br>");
     buf.append(res.getString("str_phone_number") + " 801-581-8796<br>");
     buf.append(res.getString("str_office_hours"));
     buf.append("<p><strong>"+res.getString("str_text") + "</strong><p> P. A. Rock, <i>Chemical Thermodynamics</i>, (University Science Books,"
      + "Sausalito, CA, 1998)");
     buf.append("<p><strong>" + res.getString("str_outcomes") + "</strong><p>"); 
     buf.append(res.getString("str_outcome1"));
     buf.append("<ul><li>" + res.getString("str_outcome2"));
     buf.append("<li>" + res.getString("str_outcome3"));
     buf.append("<li>" + res.getString("str_outcome4"));
     buf.append("<li>" + res.getString("str_outcome5") + "</ul>");
     buf.append("<p><strong>"+res.getString("str_grading") + "</strong><p>" + res.getString("str_grade1"));
     
     //paragraph 0..
     stmt.executeUpdate("INSERT INTO WebPages VALUES (0,'Syllabus','','"+buf.toString()+"','"+courseID+"')");
     
     /*
     
     
     stmt.executeUpdate("INSERT INTO WebPages VALUES (1,'Syllabus',"
      + "'<h2>" + res.getString("str_el_syll") + "</h2>','','All')");

     stmt.executeUpdate("INSERT INTO WebPages VALUES (2,'Syllabus','"
      + res.getString("str_intro_mathcad") + "','','All')");

     stmt.executeUpdate("INSERT INTO WebPages VALUES (3,'Syllabus','" 
      + res.getString("str_instructor") + "','"
      + "Chuck Wight<br>" + res.getString("str_office") + " 2424 HEB<br>"
      + res.getString("str_email") + " <A href=mailto:Chuck.Wight@utah.edu>Chuck.Wight@utah.edu</A><br>"
      + res.getString("str_phone_number") + " 801-581-8796<br>"
      + res.getString("str_office_hours") + "','All')");

     stmt.executeUpdate("INSERT INTO WebPages VALUES (4,'Syllabus','" + res.getString("str_text") + "','"
      + "P. A. Rock, <i>Chemical Thermodynamics</i>, (University Science Books,"
      + "Sausalito, CA, 1998)','All')");

     stmt.executeUpdate("INSERT INTO WebPages VALUES (5,'Syllabus','" + res.getString("str_outcomes") + "','"
      + res.getString("str_outcome1") + "<ul>"
      + "<li>" + res.getString("str_outcome2")
      + "<li>" + res.getString("str_outcome3")
      + "<li>" + res.getString("str_outcome4")
      + "<li>" + res.getString("str_outcome5")
      + "</ul>','All')");

     stmt.executeUpdate("INSERT INTO WebPages VALUES (6,'Syllabus','" + res.getString("str_grading") + "','"
      + res.getString("str_grade1") + "','All')");
    */
    
    }
    catch (SQLException e) {
      log.sparse("Caught: " + e.getMessage(),"Syllabus:createDefaultSyllabusPage");
    }
    
  }
}
