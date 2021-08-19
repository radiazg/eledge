package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class Help extends HttpServlet {
  RBStore res = EledgeResources.getHelpBundle();  
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    HttpSession session = request.getSession(true);
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student==null) student = new Student();
	  out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Ayuda</em><br><br>"+helpPage(student.getIsInstructor(),Integer.parseInt(student.getCourse_id()),student.getCourse_id()),student));    
  }

  String helpPage(boolean isInstructor, int contentID, String courseID){
    String webPage = new Page().webPage("Help",isInstructor,contentID);
    if (webPage==null) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        createDefaultHelpPage(stmt,courseID);
      }
      catch (Exception e) {
        return e.getMessage();
      }
      webPage = new Page().webPage("Help",isInstructor, contentID);
    }
    return webPage;
  }

  void createDefaultHelpPage(Statement stmt, String courseID)
    throws SQLException {
    MessageFormat mf = new MessageFormat(res.getString("str_help0_text"));
    Object args[] = { "<i>",
      "</i>",
      "<a href=http://sourceforge.net/tracker/?func=add&group_id=42221&atid=432437>",
      "</a>"
    };
    try {
      StringBuffer buf = new StringBuffer();
      

      //paragraph 0..
      //stmt.executeUpdate("INSERT INTO WebPages VALUES (0,'Help','','"
      buf.append(mf.format(args) + "<hr><h3>" + res.getString("str_title_faq") +"</h3>");
      
      mf.applyPattern(res.getString("str_help1_text"));
      buf.append("<Strong>"+res.getString("str_help1_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help2_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help2_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help3_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help3_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help4_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help4_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help5_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help5_title") + "</Strong><br>" +mf.format(args));
 
 	  mf.applyPattern(res.getString("str_help6_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help6_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help7_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help7_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help8_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help8_title") + "</Strong><br>" +mf.format(args));
      
      mf.applyPattern(res.getString("str_help9_text"));
      buf.append("<br><br><Strong>"+res.getString("str_help9_title") + "</Strong><br>" +mf.format(args));     
         
      //+ "','"+courseID+"')");

     //paragraph 0..
     stmt.executeUpdate("INSERT INTO WebPages VALUES (0,'Help','','"+buf.toString()+"','"+courseID+"')");
    
    }
    catch (SQLException e) {
      throw e;
    }
  }
}

