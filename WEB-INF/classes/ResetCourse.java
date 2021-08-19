package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;

public class ResetCourse extends HttpServlet {
  
  RBStore res = EledgeResources.getResetCourseBundle();
  Logger log = new Logger();

  public String getServletInfo() {
    return "This Eledge servlet erases the class database to reset the course.";
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin method.","ResetCourse:doGet");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ResetCourse");
      return;
    }
    if (!student.getIsInstructor() || student.getIsTA()) {
      log.sparse(student.getIDNumber() +" attempted to access the ResetCourse Servlet, but is not an instructor!","ResetCourse:doGet");
      out.println(Page.create(res.getString("str_must_be_instructor")));
      //out.println(Page.create("Sorry, you must be logged in as the instructor to use this function."));
      return;
    }
    // from here on, user is assumed to be the instructor
 	
    out.println(Page.create(resetPage()));    
    log.paranoid("End Method.","ResetCourse:doGet");
  }

  String resetPage(){
    log.paranoid("Begin Method","ResetCourse:resetPage()");
    MessageFormat mf = new MessageFormat(res.getString("str_warning_msg2"));
    Object[] mfArgs = {
	    "<strong>",
	    "</strong>"
    };
    StringBuffer buf = new StringBuffer();

    buf.append("<h3>" + Course.name  + res.getString("str_reset_page") + "</h3>");
    buf.append(res.getString("str_reset_info") + Course.name + res.getString("str_course"));
    buf.append("<h2>" + res.getString("str_warning") + "</h2>");
    buf.append(res.getString("str_warning_msg"));
    buf.append("<p>" + mf.format(mfArgs) + "</p>");
    buf.append("<FORM NAME=EraseAll METHOD=POST><h3>" + res.getString("str_title_erase_all")
    + "</h3><INPUT TYPE=HIDDEN NAME=UserRequest VALUE=EraseAll>"
    + "<INPUT TYPE=BUTTON VALUE='" + res.getString("str_button_erase_all") + "' "
    + "onClick='if (confirm(\"" + res.getString("str_js_erase_all") + "\")) "
    + "EraseAll.submit()'></FORM>");

    buf.append("<FORM NAME=EraseStudents METHOD=POST><h3>" + res.getString("str_title_erase_some") + "</h3>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=EraseStudents>"
    + "<INPUT TYPE=BUTTON VALUE='" + res.getString("str_button_erase_some") + "' "
    + "onClick='if (confirm(\"" + res.getString("str_js_erase_some") + "\")) "
    + "EraseStudents.submit()'></FORM>");
    log.paranoid("End method.","ResetCourse:resetPage");

    buf.append("<FORM NAME=SaveNErase METHOD=POST><h3>" + res.getString("str_title_archive") + "</h3>\n"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=ArchiveErase>"
    + "<INPUT TYPE=BUTTON VAlUE='Archive and Erase Student Records' "
    + "onClick='if (confirm(\"" + res.getString("str_confirm_archive")
    + "\")) document.forms.SaveNErase.submit()'");
    return buf.toString();
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin method.","ResetCourse:doPost");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Gradebook");
      return;
    }
    if (!student.getIsInstructor()) {
      log.sparse(student.getIDNumber() + " tried to reset the course, but is not an instructgor!","ResetCourse:doPost");
      return;
    }

    // from here on, user is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    if (userRequest==null) {
      doGet(request,response);
      return;
    }
    log.paranoid("userRequest was: " + userRequest,"ResetCourse:doPost");
    if (userRequest.equals("EraseAll")) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP DATABASE " + Course.name);
        stmt.executeUpdate("CREATE DATABASE " + Course.name);
        Page.isLoaded = false;
        ManageCourse.isLoaded = false;
        
      }
      catch (Exception e) {
      }
    }
    if (userRequest.equals("EraseStudents")) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DELETE FROM Students WHERE (Status='Current' OR Status='Frozen')");
        stmt.executeUpdate("DELETE FROM DiscussionBoardEntries");
        stmt.executeUpdate("DELETE FROM ViewedMessages");
        stmt.executeUpdate("DELETE FROM JournalEntries");
        stmt.executeUpdate("DROP TABLE Portfolios");
        stmt.executeUpdate("DDROP TABLE PortfolioArtifacts");
        stmt.executeUpdate("DELETE FROM HomeworkTransactions");
        stmt.executeUpdate("DELETE FROM QuizTransactions");
        stmt.executeUpdate("DELETE FROM ReportTransactions");
        stmt.executeUpdate("DELETE FROM ExamTransactions");
        stmt.executeUpdate("DELETE FROM Reviews");
        stmt.executeUpdate("DELETE FROM Scores");
        //These may or may not exist; put them in their own catch statement.
        try {
          stmt.executeUpdate("DELETE FROM ExamAssignedQuestions");
        } catch(Exception e) {
          log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:doPost");
        }
        try {
          stmt.executeUpdate("DELETE FROM QuizAssignedQuestions");
        } catch (Exception e) {
          log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:doPost");
        }
        try {
          stmt.executeUpdate("DELETE FROM HomeworkAssignedQuestions");
        } catch (Exception e) {
          log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:doPost");
        }

      }
      catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:doPost");
      }
    }
    if (userRequest.equals("ArchiveErase")) {
           archiveErase();
    }

    response.sendRedirect(Course.name + ".Home");
  }

  private void archiveErase() {
    SimpleDateFormat df = new SimpleDateFormat("yy_MM_dd");
    Date now = new Date();

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      try {
        stmt.executeUpdate("CREATE TABLE Students" + df.format(now)
        + " Select * from Students");
        stmt.executeUpdate("DELETE FROM Students WHERE (Status='Current' OR Status='Frozen')");
      } catch (Exception e) {
        log.sparse("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE DiscussionBoardEntries" + df.format(now)
        + " SELECT * FROM DiscussionBoardEntries");
        stmt.executeUpdate("DELETE FROM DiscussionBoardEntries");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE ViewedMessages" + df.format(now)
        + " SELECT * FROM ViewedMessages");
        stmt.executeUpdate("DELETE FROM ViewedMessages");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE Table JournalEntries" + df.format(now)
        + " SELECT * FROM JournalEntries");
        stmt.executeUpdate("DELETE FROM JournalEntries");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE Table Portfolios" + df.format(now)
        + " SELECT * FROM Portfolios Entries");
        stmt.executeUpdate("DROP TABLE Portfolios");
      } catch(Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE Table PortfolioArtifacts" + df.format(now)
        + " SELECT * FROM PortfolioArtifacts");
        stmt.executeUpdate("DROP TABLE PortfolioArtifacts");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE HomeworkTransactions" + df.format(now)
        + " SELECT * FROM HomeworkTransactions");
        stmt.executeUpdate("DELETE FROM HomeworkTransactions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE QuizTransactions" + df.format(now)
        + " SELECT * FROM QuizTransactions");
        stmt.executeUpdate("DELETE FROM QuizTransactions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE ReportTransactions" + df.format(now)
        + " SELECT * FROM ReportTransactions");
        stmt.executeUpdate("DELETE FROM ReportTransactions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE ExamTransactions" + df.format(now)
        + " SELECT * FROM ExamTransactions");
        stmt.executeUpdate("DELETE FROM ExamTransactions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE Reviews" + df.format(now)
        + " SELECT * FROM Reviews");
        stmt.executeUpdate("DELETE FROM Reviews");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE Scores" + df.format(now)
        + " SELECT * FROM Scores");
        stmt.executeUpdate("DELETE FROM Scores");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE QuizAssignedQuestions" + df.format(now)
        + " SELECT * FROM QuizAssignedQuestions");
        stmt.executeUpdate("DELETE FROM QuizAssignedQuestions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE ExamAssignedQuestions" + df.format(now)
        + " SELECT * FROM ExamAssignedQuestions");
        stmt.executeUpdate("DELETE FROM ExamAssignedQuestions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
        stmt.executeUpdate("CREATE TABLE HomeworkAssignedQuestions" + df.format(now)
        + " SELECT * FROM HomeworkAssignedQuestions");
        stmt.executeUpdate("DELETE From HomeworkAssignedQuestions");
      } catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      try {
	stmt.executeUpdate("CREATE TABLE Essays" + df.format(now)
	+ " SELECT * FROM Essays");
	stmt.executeUpdate("DELETE FROM Essays");
      } catch (Exception e) {
	log.normal("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"ResetCourse:archiveErase");
    }
  }
}
