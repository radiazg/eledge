package Eledge; //this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageDiscussionBoard extends HttpServlet {
  
  RBStore res = EledgeResources.getManageDiscussionBoardBundle();
  private Logger log = new Logger();
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageDiscussionBoard");
      return;
    }
    
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageDiscussionBoard",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+err.toString(),student));
        return;
      }
    }
    
    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+res.getString("str_must_be_instructor"),student));
      return;
    }
    //from here on, user is assumed to be the instructor.
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+displayForumInfo(student.getCourse_id()),student));
    return;  
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    TA ta=null;
    Student student =  (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageContent");
      return;
    }

    if (student.getIsTA()) {
      ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageDiscussionBoard",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+err.toString(),student));
        return;
      }
    }
      
    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+res.getString("str_must_be_instructor"),student));
      return;
    }
    String userRequest = request.getParameter("UserRequest");

    if (userRequest == null){
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+displayForumInfo(student.getCourse_id()),student));
      return;
    }
    
    if (userRequest.equals("UpdateForums")) {
      StringBuffer error = new StringBuffer("");
      updateForumInfo(request,error,ta,student.getCourse_id());
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+error + displayForumInfo(student.getCourse_id()),student));
      return;
    }
    
    else if (userRequest.equals("AddForum"))
      addForum(ta, student.getCourse_id());
    
    else if (userRequest.equals("DeleteForum")){
      String forumID = request.getParameter("ID");
      if (forumID==null || forumID.equals("")) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+res.getString("str_must_select_delete"),student));
        return;
      }
      
      deleteForum(forumID);
    }
    
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> Administración de Foros de Debate</em><br><br>"+displayForumInfo(student.getCourse_id()),student));
    return;
  }

  void addForum(TA ta, String getCourse_ID){
    String section=getCourse_ID;
    
    /*if (ta!=null) {
      
      if (ta.getPermission("DiscussionBoard_AddForum").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
      	String[] sections = ta.getAssignedSections();
      	if (sections==null || sections.length==0)
        	return;
      	section=sections[0];
      }
      
    }*/

    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      stmt.executeUpdate("INSERT INTO DiscussionBoardForums (Name,AllowStudentThreads,AllowStudentDeletes,CourseSection) VALUES ('New Forum','true','false','" + section + "')");
      
      stmt.close();
      conn.close();
    }catch(Exception e) {
    }
    return;
  }
  
  void updateForumInfo(HttpServletRequest request, StringBuffer error, TA ta, String course_ID){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //ResultSet rsSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      
      //int numSections=rsSections.next()?rsSections.getInt("Value"):1;
      
      ResultSet rsForums=stmt.executeQuery("SELECT * FROM DiscussionBoardForums WHERE CourseSection = '"+course_ID+"' ORDER BY ID");
      
      int i=0;
      while (rsForums.next()) {
        StringBuffer courseSection= new StringBuffer(request.getParameter("CourseSection:" + Integer.toString(i)));
        
        if (updateOk(rsForums, ta)) {
          
          
          rsForums.updateString("Name",request.getParameter("Name:" + Integer.toString(i)));
          rsForums.updateString("AllowStudentThreads",(request.getParameter("AllowThread:" + Integer.toString(i))==null?"false":"true"));
          rsForums.updateString("AllowStudentDeletes",request.getParameter("AllowDelete:" + Integer.toString(i))==null?"false":"true");
          
          /*if (numSections>1){
            rsForums.updateString("CourseSection",courseSection.toString());
          }*/
          
          rsForums.updateRow();
          i++;
        }
      }   
    }catch(Exception e) {
      log.paranoid("Caught: " + e.toString(),"ManageDiscussionBoard:updateForums",e);
      error.append(e.getMessage());
    }
    return; 
  }

  String displayForumInfo(String course_ID){
    StringBuffer buf = new StringBuffer("<H3>" + res.getString("str_title_managedb") + "</H3>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //ResultSet rsSections = stmt.executeQuery("SELECT Value From CourseParameters WHERE Name='NumberOfSections'");
      
      //int numSections = (rsSections.next()?rsSections.getInt("Value"):1);
      
      //rsSections.close();
      
      ResultSet rsForums=stmt.executeQuery("SELECT * FROM DiscussionBoardForums WHERE CourseSection='"+course_ID+"' ORDER BY ID");
      
      buf.append("<FORM METHOD=POST><TABLE BORDER=1 CELLSPACING=0>" 
      + "<TR><TD>&nbsp;</TD><TD align=center><b>" + res.getString("str_field_forum_name") 
      + "</b></TD><TD><b>" + res.getString("str_allow_student_threads") + "</b></TD>" 
      + "<TD><b>" + res.getString("str_allow_student_deletes") + "</b></TD></TR>");
      
      int i=0;
      
      while (rsForums.next()) {
        buf.append("<TR><TD><INPUT TYPE=RADIO NAME=ID VALUE='" 
        + rsForums.getString("ID") + "'></TD><TD><INPUT TYPE=TEXT Name='Name:"
        + i + "' VALUE='" + rsForums.getString("Name") + "'>"
        + "</TD><TD align=center><INPUT TYPE=CHECKBOX VALUE=true NAME='AllowThread:" + i + "' "
        + (rsForums.getBoolean("AllowStudentThreads")?"CHECKED":"") + "></TD>"
        + "<TD align=center><INPUT TYPE=CHECKBOX VALUE=true NAME='AllowDelete:" + i + "' "
        + (rsForums.getBoolean("AllowStudentDeletes")?"CHECKED":"") + "></TD>"
        + displaySectionInfo(rsForums,i) + "</TR>");
        i++;
      }
      buf.append("</TABLE><INPUT TYPE=HIDDEN NAME=UserRequest>"
      + "<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='AddForum'; VALUE='" 
      + res.getString("str_btn_add_forum") 
      + "'>&nbsp;<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='UpdateForums'; VALUE='" 
      + res.getString("str_btn_update_forum") + "'>&nbsp;<INPUT TYPE=BUTTON "
      + "onClick=\"if (confirm('" + res.getString("str_confirm_delete")
      + "')) { UserRequest.value='DeleteForum';this.form.submit(); }\" VALUE='"
      + res.getString("str_btn_delete_forum") + "'>&nbsp;");
    }catch(Exception e){
      return e.getMessage();
    }
    return buf.toString();
  }

  String displaySectionInfo(ResultSet rsForum, int i) {
    StringBuffer buf = new StringBuffer();
    
    try {
      //if (numSections<=1) {
        buf.append("<INPUT TYPE=Hidden NAME='CourseSection:" + i 
          + "' VALUE='" + rsForum.getString("CourseSection") + "'>");
	
	//return buf.toString();
    //}
    
    /*
      buf.append("<TD><SELECT NAME='CourseSection:" + i + "'>");
      buf.append("<OPTION" 
      + (rsForum.getString("CourseSection").equals("All")?" SELECTED":"")
      + " VALUE='All'>" + res.getString("str_select_all"));
      for (int j=1;j<=numSections;j++) {
        buf.append("<OPTION" + (rsForum.getString("CourseSection").equals(Integer.toString(j))?" SELECTED":"")
        + " VALUE='" + j + "'>" + j);
      }
      buf.append("</SELECT></TD>");*/
      
    }catch(Exception e){
      return "";
    } 
    return buf.toString();
  }

  void deleteForum(String forumID){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      stmt.executeUpdate("DELETE FROM DiscussionBoardForums WHERE ID='" + forumID + "'");
      //get rid of the messages and viewed messages too, to keep things cleaner.
      
      ResultSet rsMessages=stmt.executeQuery("SELECT ID FROM DiscussionBoardEntries"
      + " WHERE ForumID='" + forumID + "'");
      
      while (rsMessages.next()) {
        stmt.executeUpdate("DELETE FROM ViewedMessages WHERE ID=" 
        + rsMessages.getInt("ID"));
      }
      
      rsMessages.close();
      stmt.executeUpdate("DELETE FROM DiscussionBoardEntries WHERE ForumID='" + forumID + "'");
      stmt.close();
      conn.close();
    }catch(Exception e){
    }
    return;
  }

  boolean updateOk(ResultSet rsForum, TA ta) {
    if (ta==null)//not a ta
      return true;
    //if their permission was none or student, they wouldn't have gotten this far.
    //If their permission was is all, they can do whatever.
    //So... ;)
    if (ta.getPermission("ManageDiscussionBoard_UpdateForums").getPermissionLevel().equals(TAPermission.PERM_ALL))
      return true;
    /*try {
      String section = rsForum.getString("CourseSection");
      if (section.equals("All"))
        return false;
      String[] s = ta.getAssignedSections();
      
      for (int i=0;i<s.length;i++) {
        if (s[i].equals(rsForum.getString("CourseSection"))) {
          if (!newSection.toString().equals(s[i])) {
            newSection.delete(0,newSection.length());
            newSection.append(rsForum.getString("CourseSection"));
          }
          return true;
        }
      }
    } catch(Exception e) {
      log.sparse("Caught: " + e.getMessage(),"ManageDiscussionBoard:updateOk",e);
    }*/
    return false;
  } 
  
}
