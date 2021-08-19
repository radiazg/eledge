package Eledge; //this tells the java compiler and servlet engine how to name each servlet. Actually, more accurately, it sets up a set of bounds and restrictions, keeping each Eledge course totally seperate from every other eledge course.
//Things to add:
//1) expandable/collapsable threads in forum view.
//2) "settable" color schemes. (ie for new threads, forums with new messages, msg bg colors, etc.)
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class DiscussionBoard extends HttpServlet {

  String mySQLDriver = Course.jdbcDriver;
  String thisServletURI;
  Logger log = new Logger();
  SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
  RBStore res = EledgeResources.getDiscussionBoardBundle();

  private boolean colored=false; //this determines whether or not to color
                         //the background of messages; currently 
			 //only applies to ViewAllMessages/getRepliesWithText
  public String getServletInfo() {
     return res.getString("str_servlet_info");
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("sending httprequest to doPost","DiscussionBoard:doGet");
    doPost(request,response);
    log.paranoid("Request sent.","DiscussionBoard:doGet");
    return;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    thisServletURI = request.getRequestURI();
    PrintWriter out = response.getWriter();
    Student student =  (Student)session.getAttribute(Course.name + "Student");
    
    int affectedMsgID = 0;
    
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "DiscussionBoard");
      return;
    }
    
    if (student.getIsFrozen()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Foro de Debate</em><br><br>"+res.getString("str_act_frozen"),student));
      return;
    }
    
    //from here on, user id assumed to be valid.
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("DiscussionBoard",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Foro de Debate</em><br><br>"+err.toString(),student));
        return;
      }
    }
    
    String userRequest = request.getParameter("UserRequest");
    log.paranoid(student.getIDNumber() + " made a request of " + userRequest,"DiscussionBoard:doPost");
    if (userRequest == null) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Foro de Debate</em><br><br>"+displayBoardContents(student,-1),student));
      if (student.getIsTA()) {
      	//elimina los permisos, para volverlos a asignar
        student.setIsInstructor(false);
      }
      return;
    } 

    int forum = 1;
    try {
      forum = Integer.parseInt(request.getParameter("Forum"));
    } catch(Exception e) {
      log.sparse("Hm. No forum for " + student.getIDNumber() + "?? What happened???","DiscussionBoard:doPost");
      forum = 1;
    }

    if (userRequest.equals("SwitchActiveForum")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+displayBoardContents(student, forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("MessageBoard")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+" >> Mensages</em><br><br>"+displayBoardContents(student,forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("Cancel")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+displayBoardContents(student, forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("StartNewThread")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+" >> Nuevo Tema</em><br><br>"+editNewThread(student, forum,"","",true),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    try {
      affectedMsgID = Integer.parseInt(request.getParameter("AffectedMsgID"));
    } catch(Exception e) {
      log.normal("No AffectedMsgID for " + student.getIDNumber() + ". Setting it to 1. Hope we're not deleting!","DiscussionBoard:doPost");
      affectedMsgID = 1;
    }

    if (userRequest.equals("Read")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+viewMessage(student,forum,affectedMsgID),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("ViewAllMessages")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+" >> Ver todos los mensages</em><br><br>"+viewAllMessages(student,forum,affectedMsgID),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("DeleteMsg")) {
//double check to make sure it's ok for this student to be deleting this message
      if(student.getIsInstructor() || isOkStudentDeletes(student.getIDNumber(),forum,affectedMsgID))
        deleteMsg(affectedMsgID);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+displayBoardContents(student, forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("ChangeMessage")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+viewMessage(student,forum,affectedMsgID),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    } 

    if (userRequest.equals("Reply")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+editReply(student,affectedMsgID,"","",true),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }
    
    boolean useHTML = (request.getParameter("UseHTML")==null?false:(request.getParameter("UseHTML").equals("true")?true:false));
    String body = request.getParameter("Body");
    if (body==null) body="";
    String subject = request.getParameter("Subject");
    if (subject==null) subject="";
    
    if (userRequest.equals("EditNewThread")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+" >> Editar Tema</em><br><br>"+editNewThread(student,forum,subject,body,useHTML),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;       
    }
    if (userRequest.equals("PreviewNewThread")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+previewNewThread(student,forum,subject,body,useHTML),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }
    if (userRequest.equals("PostNewThread")) {
      String result = postNewThread(student.getIDNumber(),forum,subject,body,useHTML);
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+result + displayBoardContents(student, forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }

    if (userRequest.equals("PreviewReply")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> F<a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+previewReply(student,forum,subject,body,useHTML,affectedMsgID),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }
    if (userRequest.equals("PostReply")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+postReply(student.getIDNumber(),forum,subject,body,useHTML,affectedMsgID) + displayBoardContents(student,forum),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }
    if (userRequest.equals("EditReply")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".DiscussionBoard'>Foro de Debate</a> >> "+getNameForum(forum)+"</em><br><br>"+editReply(student,affectedMsgID,subject,body,useHTML),student));
      if (student.getIsTA()) {
        student.setIsInstructor(false);
      }
      return;
    }
    log.paranoid("I don't think it should have gotten this far, but, for " + student.getIDNumber() + ", it did. Oh, and userRequest was " + userRequest,"DiscussionBoard:doPost");
    if (student.getIsTA()) {
      student.setIsInstructor(false);
    }
    return;
  }

  String displayBoardContents(Student student, int forum){
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_unread_msg_pattern"));
//following lines are for i18n.
    double msgLimits[] = {0,1,2};
    double forumLimits[] = {1,2};

    String[] forumStrings = {
      res.getString("str_one_forum"),
      res.getString("str_multiple_forums")
    };

    String[] msgStrings = {
      res.getString("str_no_msgs"),
      res.getString("str_one_msg"),
      res.getString("str_multiple_msgs")
    };

    ChoiceFormat cfForum = new ChoiceFormat(forumLimits,forumStrings);
    ChoiceFormat cfMsg = new ChoiceFormat(msgLimits, msgStrings);
    Format[] formats = {cfForum, cfMsg, NumberFormat.getInstance(), NumberFormat.getInstance()};
    mf.setFormats(formats);
    //first one is for choiceformat for forums, so, # forums.
    //second is for choieformat for msgs, so, #msgs.
    //3rd is #forums.
    //4th is #msgs.
    Object[] mfArgs = { null, null, null, null };
//end i18n additions.
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      //if user request was null, need a "default" board. Normally, this could
      //be one, but, in the event of the original discussion board being 
      //deleted, we need a "backup" discussion board.
      if (forum==-1) {
        ResultSet rsForumId=stmt.executeQuery("SELECT ID FROM DiscussionBoardForums WHERE CourseSection='" + student.getCourse_id() + "' ORDER BY ID LIMIT 1");
        if (rsForumId.next())
          forum=rsForumId.getInt("ID");
        //else forum = 1;
        rsForumId.close();
      }
      
      ResultSet rsForumCount = stmt.executeQuery("SELECT COUNT(*) AS Count FROM DiscussionBoardForums"
      +" WHERE CourseSection='" + student.getCourse_id() + "'");
      
      int forumCount = (rsForumCount.next()?rsForumCount.getInt("Count"):1);
      rsForumCount.close(); 
      buf.append("<h3>" + (forumCount>1?res.getString("str_title_db_plural"):
          res.getString("str_title_db")) + "</h3>");
      
      ResultSet rsMsgCount = stmt.executeQuery("select COUNT(*) AS Count "
      + "FROM DiscussionBoardEntries LEFT OUTER JOIN ViewedMessages "
      + "ON(DiscussionBoardEntries.ID=ViewedMessages.ID AND "
      + "ViewedMessages.StudentIDNumber='" + student.getIDNumber() + "') "
      + "LEFT OUTER JOIN DiscussionBoardForums ON(DiscussionBoardEntries.ForumID=DiscussionBoardForums.ID)"
      + "WHERE ViewedMessages.StudentIDNumber IS NULL"
      + " AND ("
      //+ "DiscussionBoardForums.CourseSection='All' OR "
      + "DiscussionBoardForums.CourseSection='" + student.getCourse_id() + "')");
      
      int unreadMessages = rsMsgCount.next()?rsMsgCount.getInt("Count"):0;
      
      rsMsgCount.close();
      //print a summary of unread messages. This looks a lot cleaner after i18n. ;)
      mfArgs[0]=mfArgs[2]=new Integer(forumCount);
      mfArgs[1]=mfArgs[3]=new Integer(unreadMessages);
      buf.append(mf.format(mfArgs));

      buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><TABLE BORDER=0 CELLSPACING=1 CELLPADDING=1>");
      buf.append("\n<TR><TD HEIGHT=15 VALIGN=BASELINE><b>" + res.getString("str_field_forums") + "</b></TD></TR>"
      + "\n<TR><TD><TABLE><TR>" + getForums(forumCount,forum,student) + "</TR></TABLE></TD></TR><TR><TD ALIGN=LEFT>"
      + "<HR WIDTH=450></TD></TR><TR><TD><UL>");
      //pull out the Main threads and start constructing the thread lists from there. 
      ResultSet rsMainThreads= stmt.executeQuery("SELECT * FROM DiscussionBoardEntries WHERE ForumID='" + forum + "' AND ID=MainThreadID ORDER BY ID");

      //default is all threads expanded.
      //will eventually add ability to expand all/collapse all, as well as 
      //the ability to expand/collapse individual threads.
      while (rsMainThreads.next()){
        int mainThreadID = rsMainThreads.getInt("ID");
        buf.append("\n<LI><A HREF=" + thisServletURI + "?UserRequest=Read"
        + "&Forum=" + forum + "&AffectedMsgID=" + mainThreadID + "><FONT COLOR=#00B000>"
        + rsMainThreads.getString("Subject") + "</FONT></A> " 
        + res.getString("str_posted_by") 
        + student.getFullName(rsMainThreads.getString("StudentIDNumber")));
        ResultSet rsNew=stmt2.executeQuery("SELECT * FROM ViewedMessages WHERE StudentIDNumber='" + student.getIDNumber() + "' AND ID=" + mainThreadID);
        if (!rsNew.next())
          buf.append("&nbsp<i><FONT COLOR=RED>(" + res.getString("str_new_msg")
            + ")</FONT></i>");
        buf.append(getReplies(mainThreadID,student));
	rsNew.close();
      } 
      rsMainThreads.close();
      stmt2.close();
      buf.append("</UL></TD></TR></TABLE>");
      buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest>");
      buf.append("<INPUT TYPE=HIDDEN NAME=Forum VALUE='" + forum + "'>");
      if (isOkStudentThreads(forum)){
        buf.append("<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='StartNewThread'; VALUE='" 
          + res.getString("str_start_new") + "'>");
      } else if (student.getIsInstructor()) {
        buf.append("<br><b>" + res.getString("str_teach_only")
          + "</b>&nbsp;<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='StartNewThread'; VALUE='"
          + res.getString("str_start_new") + "'>");
      }
      buf.append("</FORM>");
      if (student.getIsInstructor())
        buf.append("<BR><FORM METHOD=GET ACTION='" + Course.name + ".ManageDiscussionBoard'>"
        + "<b>" + res.getString("str_teach_only") + "</b>&nbsp;"
        + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_manage_db") + "'>"
        + "</FORM>");
    stmt.close();
    conn.close();
    }catch(Exception e){
      return createBoardTables(student);
    }
    return buf.toString();
  }

  String getForums(int forumCount, int activeForum,Student student) {
    StringBuffer buf = new StringBuffer();
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn  = DriverManager.getConnection(Course.dbName, Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString=null;
      
      /*ResultSet rsSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      int numSections = rsSections.next()?rsSections.getInt("Value"):1;
      rsSections.close();*/
      
      sqlQueryString="SELECT * FROM DiscussionBoardForums"
      + " WHERE CourseSection='" + student.getCourse_id() + "'" + " ORDER BY ID";
      
      ResultSet rsForums=stmt.executeQuery(sqlQueryString);
      while(rsForums.next()){
        buf.append("<TD>");
        if (forumCount > 1){
          if(activeForum == rsForums.getInt("ID"))
             buf.append("[" + rsForums.getString("Name")
             //+ (numSections>1?(student.getIsInstructor()?"{"+rsForums.getString("CourseSection")+"}":""):"")
             + "]</TD>");
          else
            buf.append("<A HREF=" + thisServletURI + "?UserRequest="
            + "SwitchActiveForum&Forum=" + rsForums.getString("ID")
            + "><FONT COLOR=" + getForumColor(rsForums.getInt("ID"),student) + ">[" + rsForums.getString("Name") 
            //+ (numSections>1?(student.getIsInstructor()?"{"+rsForums.getString("CourseSection")+"}":""):"")
            + "]</FONT></A></TD>");
        }
        else
          buf.append("<i>" + rsForums.getString("Name") + "</i></TD>");
      }
    stmt.close();
    conn.close();
    }catch(Exception e){
      return "<TD><i>" + res.getString("str_general_board") + "</i></TD>";
    }
    return buf.toString(); 
  }
           
  //oh, the pretty things teachers want. =/ This method is used to determine if
  //there are any new messages for a given forum, and, if there are, change the
  //forum color accordingly.
  String getForumColor (int forumID, Student student) {
    log.paranoid("Beginning Method.","DiscussionBoard:getForumColor");
    String retColor="#D00000";
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsNew = stmt.executeQuery("SELECT DiscussionBoardEntries.ID AS ID,"
        + "ViewedMessages.StudentIDNumber AS StudentIDNumber FROM "
	+ "DiscussionBoardEntries LEFT OUTER JOIN ViewedMessages ON "
        + "(DiscussionBoardEntries.ID=ViewedMessages.ID AND "
        + "ViewedMessages.StudentIDNumber='" + student.getIDNumber() + "')"
        + " WHERE ViewedMessages.StudentIDNumber IS NULL AND "
        + "DiscussionBoardEntries.ForumID='" + forumID + "' ORDER BY ID LIMIT 1");
      if (rsNew.next()) 
        retColor = "#0F88B0";
      rsNew.close();
      stmt.close();
      conn.close();
      return retColor;
    } catch (Exception e) {
      log.normal("Exception "  + e.getMessage() + " caught.","DiscussionBoard:getForumColor");
    }
      return "#D00000";
  }
  //recursive method builds a "threaded" list of messages. We need to 
  //Keep track of the color to make the messages, as well as the indent level.
  String getRepliesWithText(int id, Student student, int indent) {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString="SELECT * from DiscussionBoardEntries WHERE SubThreadID=" 
        + id + " AND ID<>" + id + " ORDER BY ID";
      ResultSet rsReplies = stmt.executeQuery(sqlQueryString);
      if (!rsReplies.next()) {
	stmt.close();
	stmt2.close();
	conn.close();
        return "";
      } 
      do {
	buf.append("\n<TR" + (colored?" BGCOLOR=#CCCCEE>":">") + "<TD><TABLE><TR><TD WIDTH=" 
	+ indent + ">&nbsp;</TD><TD>" 
	+ displayMessage(rsReplies,rsReplies.getInt("ID"),student) 
	+ "</TD></TR></TABLE></TD></TR>");
	colored=!colored;
        ResultSet rsNew=stmt2.executeQuery("SELECT * FROM ViewedMessages WHERE StudentIDNumber='" + student.getIDNumber() + "' AND ID='" + rsReplies.getString("ID") + "'");
        if (!rsNew.next())
	  stmt.executeUpdate("INSERT INTO ViewedMessages VALUES ('" + student.getIDNumber() + "','" + rsReplies.getString("ID") +"')");
	rsNew.close();

        buf.append(getRepliesWithText(rsReplies.getInt("ID"), student, indent+30));
      } while (rsReplies.next());
      stmt.close();
      stmt2.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return buf.toString();
  }

  String getReplies(int id, Student student) {
    StringBuffer buf = new StringBuffer();
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString="SELECT * from DiscussionBoardEntries WHERE SubThreadID=" + id + " AND ID<>" + id + " ORDER BY ID";
      ResultSet rsReplies = stmt.executeQuery(sqlQueryString);
      if (!rsReplies.next()) {
        stmt.close();
	stmt2.close();
	conn.close();
        return "";
      }
      buf.append("<UL>");
      do {
        buf.append("<LI><A HREF=" + thisServletURI + "?UserRequest=Read"
        + "&Forum=" + rsReplies.getString("ForumID") + "&AffectedMsgID="
        + rsReplies.getString("ID") + ">" + rsReplies.getString("Subject")
        + "</A> " + res.getString("str_posted_by")  
        + student.getFullName(rsReplies.getString("StudentIDNumber")));
        ResultSet rsNew=stmt2.executeQuery("SELECT * FROM ViewedMessages WHERE StudentIDNumber='" + student.getIDNumber() + "' AND ID='" + rsReplies.getString("ID") + "'");
        if (!rsNew.next())
          buf.append("&nbsp;<I><FONT COLOR=RED>(" + res.getString("str_new_msg")
          + ")</FONT></I>"); 
        buf.append(getReplies(rsReplies.getInt("ID"), student));
      } while (rsReplies.next());
      buf.append("</UL>");
      stmt.close();
      stmt2.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return buf.toString();
  }

  String viewAllMessages(Student student, int forum, int msgID) {
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //finally, need a return to forum button.
      //And maybe a "next new message" button. =)
      ResultSet rsMsg = stmt.executeQuery("SELECT * FROM DiscussionBoardEntries WHERE "
      + "ID='" + msgID + "'");
      if (!rsMsg.next())
        return res.getString("str_invalid_msgID");
      //the javascript sets the value of the UserRequest, as well as which
      //message to affect.
      buf.append("<SCRIPT>\n<!--\nfunction setValues(request,ID) {"
      + "\n document.forms[0].UserRequest.value=request;"
      + "\n document.forms[0].AffectedMsgID.value=ID;\n}\n-->\n</SCRIPT>");
      buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><INPUT TYPE=HIDDEN NAME=UserRequest>"
      + "<INPUT TYPE=HIDDEN NAME=Forum VALUE='" + forum + "'>"
      + "<INPUT TYPE=HIDDEN NAME=AffectedMsgID>"
      + "\n<TABLE BORDER=0 WIDTH=100%>\n<TR>");
      
      buf.append("<TD WIDTH=50% ALIGN=LEFT>");
      if (rsMsg.getString("PreviousThreadID") == null
         || rsMsg.getInt("PreviousThreadID")==rsMsg.getInt("MainThreadID"))
        buf.append("&nbsp;");
      else
        buf.append("\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_prev_thread")
        + "' onClick=\"setValues('ViewAllMessages','"
        + rsMsg.getString("PreviousThreadID") + "');\">");
      buf.append("</TD><TD WIDTH=50% ALIGN=RIGHT>");
      if (rsMsg.getString("NextThreadID") == null
          || rsMsg.getInt("NextThreadID") == rsMsg.getInt("MainThreadID"))
	buf.append("&nbsp;");
      else
        buf.append("\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_next_thread")
        + "' onClick=\"setValues('ViewAllMessages','"
        + rsMsg.getString("NextThreadID") + "');\">");
      buf.append("</TD></TR></TABLE>");
      buf.append("\n<TABLE BORDER=1 CELLSPACING=0 WIDTH=100%>\n<TR><TD WIDTH=100% ALIGN=LEFT>");
      buf.append("\n<TABLE><TR><TD>" + displayMessage(rsMsg,msgID,student)
      + "</TD></TR>");
      colored=true;
      buf.append(getRepliesWithText(msgID,student,30) + "</TABLE>");
      //get replies for this thread, send the thread id, student name,
      // and 0 for indent. First set "colored" to true (since we already wrote
      // one message, the next message should have a colored bg.
      buf.append("\n</TD></TR></TABLE>"); 
      buf.append("\n<TABLE WIDTH=100%><TR><TD WIDTH=50% ALIGN=LEFT>"
      + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_return_forum") 
      + "' onClick=this.form.elements.UserRequest.value='MessageBoard'></TD>");
      int newmsgID = getNewMsgID(student,forum,msgID);
      if (newmsgID != -1)
        buf.append("\n<TD WIDTH=50% ALIGN=RIGHT><INPUT TYPE=SUBMIT "
	+ "VALUE='" + res.getString("str_next_new") +"' onClick=\"setValues('ChangeMessage','" 
	+ newmsgID + "');\"></TD>");

      buf.append("</TR></TABLE>");
      rsMsg.close();
      stmt.close();
      conn.close();
    } catch(Exception e) {
      log.normal("Caught exception: " + e.getMessage() + " while " + student.getIDNumber() + " was viewing all messages for " + msgID + ".","DiscussionBoard:viewAllMessages");
      return "";
    }
    return buf.toString();
  }

  String viewMessage(Student student, int forum, int msgID){
    StringBuffer buf = new StringBuffer();
    boolean hasMainThread = false;
    boolean hasSubThread = false;
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      int indent = 0;
      ResultSet rsMsg = stmt.executeQuery("SELECT * FROM DiscussionBoardEntries WHERE "
      + "ID='" + msgID + "'");
      if (!rsMsg.next()) {
        stmt.close();
        stmt2.close();
        conn.close();
        return res.getString("str_invalid_msgID");
      }
      buf.append("<SCRIPT>\n<!--\nfunction setValues(request,ID) {"
      + "\n document.forms[0].UserRequest.value=request;"
      + "\n document.forms[0].AffectedMsgID.value=ID;\n}\n-->\n</SCRIPT>");
      buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><INPUT TYPE=HIDDEN NAME=UserRequest>"
      + "<INPUT TYPE=HIDDEN NAME=Forum VALUE='" + forum + "'>"
      + "<INPUT TYPE=HIDDEN NAME=AffectedMsgID>"
      + "\n<TABLE BORDER=0 WIDTH=100%>\n<TR>");
      
      buf.append("<TD WIDTH=33% ALIGN=LEFT>");
//previous and next thread entries. If null (error checking) or if it's 
//the same as the main thread ID (first/last thread, respectively)
//just put in a space. Avoid visual clutter, and spread buttons out.
      if (rsMsg.getString("PreviousThreadID") == null
         || rsMsg.getInt("PreviousThreadID")==rsMsg.getInt("MainThreadID"))
        buf.append("&nbsp;");
      else
        buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_prev_thread")
        + "' onClick=\"setValues('ChangeMessage','"
        + rsMsg.getString("PreviousThreadID") + "');\">");

      buf.append("</TD><TD WIDTH=34% ALIGN=CENTER><INPUT TYPE=SUBMIT "
      + "VALUE='" + res.getString("str_view_all") + "' onCLICK=\"setValues('ViewAllMessages','" 
      + rsMsg.getString("MainThreadID") + "');\"></TD><TD WIDTH=33% ALIGN=RIGHT>");

      if (rsMsg.getString("NextThreadID") == null 
         || rsMsg.getInt("NextThreadID") == rsMsg.getInt("MainThreadID"))
        buf.append("&nbsp;");
      else
        buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_next_thread") 
        + "' onClick=\"setValues('ChangeMessage','" 
        + rsMsg.getString("NextThreadID") + "');\">");
      buf.append("</TD></TR></TABLE>");
      buf.append("\n<TABLE BORDER=1 CELLSPACING=0 WIDTH=100%>\n<TR><TD WIDTH=100% ALIGN=LEFT>");
      if (rsMsg.getString("MainThreadID") != null && rsMsg.getInt("MainThreadID") != rsMsg.getInt("ID")) {
        ResultSet rsMainThread=stmt2.executeQuery("SELECT * FROM DiscussionBoardEntries"
        + " WHERE ID='" + rsMsg.getString("MainThreadID") + "'");
        if (rsMainThread.next()){
          hasMainThread=true;
          buf.append(displayMessage(rsMainThread, rsMsg.getInt("MainThreadID"),student) + "</TD></TR>\n<TR><TD ALIGN=LEFT BGCOLOR=#D0D0FF>");
          indent+=30;
        }
        rsMainThread.close();
      }
      if (rsMsg.getString("SubThreadID") != null && rsMsg.getInt("SubThreadID") != rsMsg.getInt("ID") && rsMsg.getInt("MainThreadID") != rsMsg.getInt("SubThreadID")) {
        ResultSet rsSubThread=stmt2.executeQuery("SELECT * FROM DiscussionBoardEntries"
        + " WHERE ID='" + rsMsg.getString("SubThreadID") + "'");
        if (rsSubThread.next()) {
          hasSubThread=true;
          if (hasMainThread)
            buf.append("<TABLE BORDER=0>\n<TR><TD WIDTH=" + indent + ">&nbsp</TD><TD ALIGN=LEFT>");
          buf.append(displayMessage(rsSubThread, rsMsg.getInt("SubThreadID"),student)); 
          if (hasMainThread)
            buf.append("</TD></TR></TABLE>");
          buf.append("</TD></TR>\n<TR><TD ALIGN=LEFT BGCOLOR=#FFE0FF>");
          indent+=30;
        }
      }
      if (hasMainThread || hasSubThread) 
        buf.append("<TABLE BORDER=0>\n<TR><TD WIDTH=" + indent + ">&nbsp</TD><TD ALIGN=LEFT>"); 
      buf.append(displayMessage(rsMsg, rsMsg.getInt("ID"),student));
      if (hasMainThread || hasSubThread)
        buf.append("</TD></TR></TABLE>");
      buf.append("</TD></TR></TABLE>\n<TABLE WIDTH=100% BORDER=0>\n<TR><TD ALIGN=LEFT>");
      if (rsMsg.getString("PreviousMessageID") != null 
         && rsMsg.getInt("PreviousMessageID") != msgID)
        buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_prev_msg")
        + "' onClick=\"setValues('ChangeMessage','" 
        + rsMsg.getString("PreviousMessageID") + "');\">");
      if (rsMsg.getString("NextMessageID")!=null
         && rsMsg.getInt("NextMessageID") != msgID)
        buf.append("&nbsp;<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_next_msg")
        + "' onClick=\"setValues('ChangeMessage','" 
        + rsMsg.getString("NextMessageID") + "');\">");
      buf.append("&nbsp;<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_return_forum")
      + "' onClick=this.form.elements.UserRequest.value='MessageBoard';>");
      int newmsgID = getNewMsgID(student,forum,msgID);
      if (newmsgID != -1) 
        buf.append("&nbsp;<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_next_new") 
        + "' onClick=\"setValues('ChangeMessage','" + newmsgID + "');\">");
      buf.append("</TD></TR></TABLE></FORM>");

      if (isOkStudentDeletes(student.getIDNumber(),rsMsg.getInt("ForumID"),msgID) 
        || student.getIsInstructor()) {
        if (student.getIsInstructor() 
          && !isOkStudentDeletes(student.getIDNumber(),rsMsg.getInt("ForumID"),msgID))
          buf.append(res.getString("str_teach_only"));
        buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><INPUT TYPE=HIDDEN NAME=AffectedMsgID VALUE='"
        + msgID + "'><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_delete_msg")
        + "'><INPUT TYPE=HIDDEN NAME=Forum VALUE='" + forum + "'>"
        + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=DeleteMsg></FORM>");
      }
      rsMsg.close();
      stmt.close();
      stmt2.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return buf.toString();
  }

  String editReply(Student student, int msgID, String subject, String body,
    boolean useHTML) {
    StringBuffer buf = new StringBuffer();
    buf.append("<h3" + res.getString("str_title_reply") 
    + "</h3><FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'>");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsMsg=stmt.executeQuery("SELECT * FROM DiscussionBoardEntries "
      + "WHERE ID=" + msgID);
      if (!rsMsg.next()) {
        stmt.close();
        conn.close();
        return res.getString("str_error_nomsg");
      }
      buf.append("<TABLE BORDER=0 WIDTH=100%><TR><TD><H4>" + res.getString("str_replying_msg")
      + "</H4></TR></TD><TR><TD><TABLE BORDER=1 WIDTH=100% CELLSPACING=0><TR><TD>" 
      + displayMessage(rsMsg,-1,student) + "</TD></TR></TABLE></TD></TR>");
      buf.append("<INPUT TYPE=HIDDEN NAME=AffectedMsgID"
      + " VALUE='" + msgID + "'><INPUT TYPE=HIDDEN NAME=UserRequest>"
      + "<INPUT TYPE=HIDDEN NAME=Forum VALUE='" + rsMsg.getString("ForumID")
      + "'><INPUT TYPE=HIDDEN NAME=StudentIDNumber VALUE='" 
      + student.getIDNumber() + "'><TR><TD>");
      buf.append("<TABLE><TR><TD ALIGN=LEFT><b>" + res.getString("str_field_subject")
      + "</b></TD><TD><INPUT TYPE=TEXT NAME=Subject VALUE='" 
      + (subject.equals("")?"Re: " + rsMsg.getString("Subject"):subject)
      + "'></TD></TR>"
      + "<TR><TD COLSPAN=2><INPUT TYPE=CHECKBOX VALUE=true NAME=UseHTML "
      + (useHTML?"CHECKED":"") + ">&nbsp;" + res.getString("str_field_display_html") + "</TD></TR>"
      + "<TR><TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_msg") + "</b>&nbsp;</TD><TD>"
      + "<TEXTAREA ROWS=20 COLS=50 WRAP=SOFT NAME=Body>"+body+"</TEXTAREA></TD></TR>"
      + "<TR><TD COLSPAN=2 ALIGN=LEFT><INPUT TYPE=SUBMIT "
      + "onClick=this.form.elements.UserRequest.value='PreviewReply'; VALUE='" + res.getString("str_btn_preview_msg") 
      +"'>&nbsp;<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='PostReply'; "
      + "VALUE='" + res.getString("str_btn_submit_msg") 
      + "'>&nbsp;<INPUT TYPE=SUBMIT VALUE='" 
      + res.getString("str_btn_cancel_msg") + "' "
      + "onClick=this.form.elements.UserRequest.value='Cancel'></TD></TR></TABLE></TR></TD></TABLE></FORM>");
      rsMsg.close();
      stmt.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    } 
    return buf.toString();
  }

  String displayMessage(ResultSet rsMsg, int replyID, Student student) {
    StringBuffer buf = new StringBuffer();
    try {
      buf.append("\n<TABLE BORDER=0><TR><TD ALIGN=LEFT VALIGN=TOP><b>" 
      + res.getString("str_field_subject") + "</b>&nbsp;</TD>"
      + "<TD ALIGN=LEFT >" + student.getFullName(rsMsg.getString("StudentIDNumber")) + "</TD></TR><TR>"
      + "<TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_subject") + "</b>&nbsp;</TD><TD VALIGN=TOP>"
      + (rsMsg.getBoolean("UseHTML")?rsMsg.getString("Subject"):"<PRE>"
      + rsMsg.getString("Subject") + "</PRE>") + "</TD></TR>"
      + "<TR><TD ALIGN=LEFT><b>" + res.getString("str_field_date") + "</b></TD><TD ALIGN=LEFT>"
      + df.format(rsMsg.getDate("Date")) + "</TD></TR>");
      buf.append("<TR><TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_msg") 
      + "</b>&nbsp;</TD><TD VALIGN=TOP WIDTH=%100>"
      + (rsMsg.getBoolean("UseHTML")?rsMsg.getString("Message"):"<PRE>"
      + rsMsg.getString("Message") + "</PRE>") + "</TD></TR>");
      if (replyID >0) 
        buf.append("<TR><TD COLSPAN=2 ALIGN=LEFT><INPUT TYPE=SUBMIT VALUE='" 
        + res.getString("str_btn_reply") + "' onClick=\"setValues('Reply','" 
        + replyID + "');\"></TD></TR>");
      buf.append("</TABLE>");
      log.paranoid("Message After Retrieval from db: " + rsMsg.getString("Message"),"DiscussionBoard:displayMessage");
    }catch(Exception e){
      return res.getString("str_error_badmsg");
    }
    return buf.toString();
  }

  int getNewMsgID(Student student, int forum, int msgID) {
    int retValue = -1;
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //insert the current message as read so it doesn't come up as the
      //next new message. ;)
      stmt.executeUpdate("INSERT INTO ViewedMessages VALUES ('" 
      + student.getIDNumber() + "','" + msgID + "')");
      //pull out the next msg from the current msg forum that doesn't have
      //an associated entry for this student in the ViewedMessages table.
      String sqlQueryString="SELECT DiscussionBoardEntries.ID AS ID, "
      + "ViewedMessages.StudentIDNumber AS StudentIDNumber FROM "
      + "DiscussionBoardEntries LEFT OUTER JOIN ViewedMessages ON "
      + "(DiscussionBoardEntries.ID=ViewedMessages.ID AND "
      + "ViewedMessages.StudentIDNumber='" + student.getIDNumber() + "')"
      + " WHERE ViewedMessages.StudentIDNumber IS NULL AND "
      + "DiscussionBoardEntries.ForumID='" + forum + "' ORDER BY ID LIMIT 1";
      ResultSet rsNewMsg=stmt.executeQuery(sqlQueryString);
      if (rsNewMsg.next())
        retValue = rsNewMsg.getInt("ID");
      rsNewMsg.close();
      stmt.close();
      conn.close();
      return retValue;
    }catch(Exception e){
    }
    return -1;
  } 

  boolean isOkStudentThreads(int forum){
    boolean isOK=false;
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsIsOk = stmt.executeQuery("SELECT AllowStudentThreads FROM DiscussionBoardForums WHERE ID=" + forum);
      if (rsIsOk.next())
        isOK = rsIsOk.getBoolean("AllowStudentThreads");
      rsIsOk.close();
      stmt.close();
      conn.close();
      return isOK;
    }catch(Exception e){
    }
    return false;
  }

  boolean isOkStudentDeletes(String StudentIDNumber, int forum, int msgID) {
    boolean isOK=false;
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsIsOk = stmt.executeQuery("SELECT StudentIDNumber FROM DiscussionBoardEntries WHERE ID=" + msgID);
      if (!rsIsOk.next()) return false;
      //student's can't delete if it's not their own entry.
      if (!(rsIsOk.getString("StudentIDNumber").equals(StudentIDNumber)))
        return false;
      rsIsOk = stmt.executeQuery("SELECT AllowStudentDeletes FROM DiscussionBoardForums WHERE ID=" + forum);
      if (rsIsOk.next()) 
        isOK = rsIsOk.getBoolean("AllowStudentDeletes");
      rsIsOk.close();
      stmt.close();
      conn.close();
      return isOK;
    }catch(Exception e){
    }
    return false;
  }

  String editNewThread(Student student, int forum, String subject, 
    String body, boolean useHTML) {
    StringBuffer buf = new StringBuffer();
    buf.append("<h3" + res.getString("str_title_new_thread") 
    + "</h3><FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest><INPUT TYPE=HIDDEN "
    + "NAME=StudentIDNumber VALUE='" + student.getIDNumber() + "'>"
    + "<INPUT TYPE=HIDDEN NAME=Forum VALUE=" + forum + ">");
    buf.append("<TABLE BORDER=0><TR><TD ALIGN=LEFT><b>" + res.getString("str_field_subject") + "</b></TD>"
    + "<TD><INPUT TYPE=TEXT NAME=Subject VALUE='" + subject + "'></TD></TR>"
    + "<TR><TD COLSPAN=2><INPUT TYPE=CHECKBOX VALUE=true NAME=UseHTML "
    + (useHTML?"CHECKED":"") + ">&nbsp;" + res.getString("str_field_display_html") + "</TD></TR>"
    + "<TR><TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_msg") + "</b> </TD><TD>"
    + "<TEXTAREA ROWS=20 COLS=50 WRAP=SOFT NAME=Body>"+body+"</TEXTAREA></TD></TR>"
    + "<TR><TD COLSPAN=2 ALIGN=CENTER><INPUT TYPE=SUBMIT "
    + "onClick=this.form.elements.UserRequest.value='PreviewNewThread'; VALUE='" + res.getString("str_btn_preview_msg") 
    + "'>&nbsp;<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='PostNewThread'; "
    + "VALUE='" + res.getString("str_btn_submit_msg") + "'>&nbsp;<INPUT TYPE=SUBMIT VALUE='"
    + res.getString("str_btn_cancel_msg") + "' onClick=this.form.elements.UserRequest.value='Cancel'></TD></TR></TABLE></FORM>");
    return buf.toString();  
  }

  String previewNewThread(Student student, int forum, String subject, 
    String body, boolean useHTML) {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><INPUT TYPE=HIDDEN NAME=StudentIDNumber "
    + "VALUE='" + student.getIDNumber() + "'><INPUT TYPE=HIDDEN NAME=UserRequest>"
    + "<INPUT TYPE=HIDDEN NAME=Forum VALUE=" + forum + "><INPUT TYPE=HIDDEN"
    + " NAME=UseHTML VALUE='" + useHTML + "'><INPUT TYPE=HIDDEN NAME=Body "
    + "VALUE ='" + CharHider.quot2html(body) + "'><INPUT TYPE=HIDDEN NAME=Subject VALUE='"+CharHider.quot2html(subject)+"'>");
    buf.append("<TABLE BORDER=1 CELLSPACING=0><TR><TD><TABLE BORDER=0><TR><TD ALIGN=LEFT VALIGN=TOP><b>"
    + res.getString("str_field_author") + "</b>&nbsp;</TD><TD>" + student.getFullName() 
    + "</TD></TR><TR><TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_subject") + "</b>&nbsp;</TD>" 
    + "<TD>" + (useHTML?subject:"<PRE>" + subject + "</PRE>") + "</TD></TR>"
    + "<TR><TD ALIGN=LEFT VALIGN=TOP><b>" + res.getString("str_field_msg") + "</b>&nbsp;</TD><TD>"
    + (useHTML?body:"<PRE>" + body + "</PRE>") + "</TD></TR></TABLE></TD></TR></TABLE>"
    + "<DIV ALIGN=CENTER><TABLE><TR><TD><INPUT TYPE=SUBMIT "
    + "onClick=this.form.elements.UserRequest.value='EditNewThread'; VALUE='" + res.getString("str_btn_edit_msg") 
    + "'><INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='PostNewThread' "
    + "VALUE='" + res.getString("str_btn_submit_msg") + "'><INPUT TYPE=SUBMIT "
    + "onClick=this.form.elements.UserRequest.value='Cancel' VALUE='" + res.getString("str_btn_cancel_msg") 
    + "'></TD></TR></TABLE></DIV></FORM>");
    log.paranoid("Body: " + body + ";Body after quot2html:" + CharHider.quot2html(body),"DiscussionBoard:previewNewThread");
    return buf.toString();
  }

  String postNewThread(String studentIDNumber, int forum, String subject, 
    String body, boolean useHTML) {
    if (subject.equals(""))
      subject=res.getString("str_no_subj");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2= conn.createStatement();
      String previousDate="";
      int previousThreadID = 0;
      
      String sqlQueryString = "SELECT * FROM DiscussionBoardEntries "
      + "WHERE ID=MainThreadID AND ForumID=" + forum + " ORDER BY ID DESC LIMIT 1";
      
      ResultSet rsLastThread=stmt.executeQuery(sqlQueryString);
      
      if (rsLastThread.next()) 
        previousThreadID = rsLastThread.getInt("ID");
        //otherwise, first thread for this forum.
      
      String sqlInsertString = "INSERT INTO DiscussionBoardEntries "
      + "(StudentIDNumber, Subject, Message, ForumID, UseHTML) VALUES('" 
      + studentIDNumber + "','" + CharHider.quot2html(subject) + "','" 
      + CharHider.curlQuote2Html(CharHider.quot2html(body)) + "'," + forum +",'" + useHTML + "')";
      
      stmt.executeUpdate(sqlInsertString);
      
      stmt.executeUpdate("UPDATE DiscussionBoardEntries SET MainThreadID=ID,"
      + "SubThreadID=ID,PreviousMessageID=ID,NextMessageID=ID,PreviousThreadID="
      + (previousThreadID==0?"ID,":"'" + previousThreadID + "',") 
      + "NextThreadID=ID WHERE MainThreadID IS NULL AND SubThreadID IS NULL AND"
      + " PreviousMessageID IS NULL AND NextMessageID IS NULL AND "
      + "PreviousThreadID IS NULL AND NextThreadID IS NULL AND ForumID=" + forum
      + " AND StudentIDNumber='" + studentIDNumber + "' AND UseHTML='" + useHTML
      + "'");
      
      if (previousThreadID != 0) {
        rsLastThread=stmt.executeQuery(sqlQueryString);
        if (rsLastThread.next()){
          ResultSet rsPreviousThreadMsgs=stmt2.executeQuery("SELECT * FROM DiscussionBoardEntries WHERE MainThreadID=" + previousThreadID);
          while (rsPreviousThreadMsgs.next()) {
	    previousDate = rsPreviousThreadMsgs.getString("Date");
            rsPreviousThreadMsgs.updateInt("NextThreadID", rsLastThread.getInt("ID"));
	    rsPreviousThreadMsgs.updateString("Date",previousDate);
            rsPreviousThreadMsgs.updateRow();
          }
	  rsPreviousThreadMsgs.close();
        }
      }
      rsLastThread.close();
      stmt.close();
      stmt2.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return res.getString("str_msg_added_ok");
  }

  String previewReply(Student student, int forum, String subject,
    String body, boolean useHTML,int msgID) {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".DiscussionBoard'><INPUT TYPE=HIDDEN NAME=StudentIDNumber "
    + "VALUE='" + student.getIDNumber() + "'><INPUT TYPE=HIDDEN NAME=UserRequest>"
    + "<INPUT TYPE=HIDDEN NAME=Forum VALUE=" + forum + "><INPUT TYPE=HIDDEN"
    + " NAME=UseHTML VALUE='" + useHTML + "'><INPUT TYPE=HIDDEN NAME=Body "
    + "VALUE ='" + CharHider.quot2html(body) + "'><INPUT TYPE=HIDDEN NAME=Subject VALUE='"+CharHider.quot2html(subject)+"'>"
    + "<INPUT TYPE=HIDDEN NAME=AffectedMsgID VALUE='" + msgID + "'>");
    buf.append("<TABLE BORDER=1 CELLSPACING=0><TR><TD><TABLE BORDER=0><TR><TD VALIGN=TOP ALIGN=LEFT><b>"
    + res.getString("str_field_author") + "</b>&nbsp;</TD><TD>" + student.getFullName()
    + "</TD></TR><TR><TD VALIGN=TOP ALIGN=LEFT><b>" + res.getString("str_field_subject") 
    + "</b>&nbsp;</TD><TD>" + (useHTML?subject:"<PRE>" + subject + "</PRE>") 
    + "</TD></TR><TR><TD VALIGN=TOP ALIGN=LEFT><b>" + res.getString("str_field_msg") + "</b>&nbsp;</TD><TD>"
    + (useHTML?body:"<PRE>" + body + "</PRE>") + "</TD></TR></TABLE></TD></TR></TABLE>"
    + "<DIV ALIGN=CENTER><TABLE><TR><TD><INPUT TYPE=SUBMIT "
    + "onClick=this.form.elements.UserRequest.value='EditReply'; VALUE='" + res.getString("str_btn_edit_msg") 
    + "'><INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='PostReply' "
    + "VALUE='" + res.getString("str_btn_submit_msg") + "'><INPUT TYPE=SUBMIT "
    + "onClick=this.form.elements.UserRequest.value='Cancel' VALUE='" + res.getString("str_btn_cancel_msg") 
    + "'></TD></TR></TABLE></DIV></FORM>");
    return buf.toString();
  }

  String postReply(String studentIDNumber, int forum, String subject,
    String body, boolean useHTML, int msgID) {
    
    String previousDate="";
    if (subject.equals(""))
      subject=res.getString("str_no_subj");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString = "SELECT * FROM DiscussionBoardEntries WHERE "
      + "ID=" + msgID;
      ResultSet rsMsg=stmt.executeQuery(sqlQueryString);
      if (!rsMsg.next())
        return "Invalid Message or Message no longer exists.";
      int prevMsgId=getLastSubThreadID(rsMsg.getInt("ID"));
      String sqlInsertString = "INSERT INTO DiscussionBoardEntries "
      + "(StudentIDNumber, Subject, Message, MainThreadID, PreviousMessageID,"
      + " SubThreadID, PreviousThreadID, NextThreadID, ForumID, UseHTML) "
      + "VALUES('" + studentIDNumber + "','" + CharHider.quot2html(subject)
      + "','" + CharHider.curlQuote2Html(CharHider.quot2html(body)) + "'," 
      + rsMsg.getString("MainThreadID") + ","
      + prevMsgId + "," + msgID + ","
      + rsMsg.getString("PreviousThreadID") + ","
      + rsMsg.getString("NextThreadID") + "," + forum + ",'" + useHTML + "')";
      stmt.executeUpdate(sqlInsertString);
      //go back and fix related entires (next, previous, etc.)
      int nextMsgID=-1;   
      sqlQueryString="SELECT * FROM DiscussionBoardEntries WHERE "
      + "ID=" + prevMsgId;
      ResultSet rsMsg2=stmt2.executeQuery(sqlQueryString);
      if (rsMsg2.next()) { 
        if (rsMsg2.getInt("NextMessageID") != rsMsg2.getInt("ID"))
          nextMsgID=rsMsg2.getInt("NextMessageID");
        rsMsg = stmt.executeQuery("SELECT * FROM DiscussionBoardEntries WHERE "
        + "MainThreadID=" + rsMsg.getString("MainThreadID") + " AND SubThreadID"
        + "=" + msgID + " AND NextMessageID IS NULL");
        if (rsMsg.next()) {
          int nextMessageNumber = rsMsg.getInt("ID");
	  previousDate=rsMsg.getString("Date");
          rsMsg.updateInt("NextMessageID", (nextMsgID==-1?nextMessageNumber:nextMsgID));
	  rsMsg.updateString("Date",previousDate);
          rsMsg.updateRow();
	  previousDate=rsMsg2.getString("Date");
          rsMsg2.updateInt("NextMessageID",nextMessageNumber);
	  rsMsg2.updateString("Date",previousDate);
          rsMsg2.updateRow();
          if (nextMsgID !=-1)
            stmt.executeUpdate("UPDATE DiscussionBoardEntries SET "
            + "PreviousMessageID=" + nextMessageNumber + ", Date=Date WHERE ID=" 
            + nextMsgID);
        }
      }
      rsMsg.close();
      rsMsg2.close();
      stmt.close();
      stmt2.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return res.getString("str_msg_added_ok");
  }

//recursive method to wind to the end of a thread/sub-thread.
  int getLastSubThreadID(int msgID) {
    int id=-1;
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsThread = stmt.executeQuery("SELECT * FROM  "
      + "DiscussionBoardEntries WHERE SubThreadID=" + msgID + " AND "
      + "ID <> " + msgID + " ORDER BY ID "
      + "DESC LIMIT 1");
      if (rsThread.next()) {
        id=rsThread.getInt("ID");
        rsThread.close();
        stmt.close();
        conn.close();
        return getLastSubThreadID(id);
      } else {
        rsThread.close();
	stmt.close();
	conn.close();
        return msgID; 
      }
    }catch(Exception e) {
    }
    return 1;
  }

  //this method recursively examines the replies to a message (ie, a main thread msg)
  //and makes sure that their main thread is set to the correct main thread.
  void fixReplies(int startID, int threadID) {
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      ResultSet rsThreads = stmt.executeQuery("SELECT * FROM  "
      + "DiscussionBoardEntries WHERE SubThreadID=" + startID + " AND "
      + "ID <> " + startID + " ORDER BY ID "
      + "DESC LIMIT 1");
      while(rsThreads.next()) {
        log.paranoid("In while loop for: " + startID,"DiscussionBoard:fixReplies");
        rsThreads.updateInt("MainThreadID",threadID);
        fixReplies(rsThreads.getInt("ID"),threadID);
        rsThreads.updateRow();
      }
      rsThreads.close();
      stmt.close();
      conn.close();
    }catch(Exception e) {
      log.paranoid("Caught: " + e.getMessage(),"DiscussionBoard:fixReplies");
    }
  }

//this method deletes the message. Due to the linked nature of messages,
//deleting a message will cause all respsonses to that message to not show in
//forum view. To handle this eventuality, all direct responses a message
//being deleted are set as responses to the message that the deleted message was
//responding too. ie, if c is a response to b is a responce to a, and b gets
//deleted, c will be set as a direct response to a.
//In the event that b is a thread starting message, c will be set as a thread 
//starter.
  void deleteMsg(int msgID) {
    int nextThread;
    int previousThread;
    int nextMessage;
    int previousMessage;
    int subThread;
    int mainThread;
    int forum;
    String sql;
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      sql = "SELECT * FROM DiscussionBoardEntries WHERE ID=" + msgID;
      log.paranoid("Executing(1): " + sql,"DiscussionBoard:deleteMsg");
      ResultSet rsMsg=stmt.executeQuery(sql);
      if (!rsMsg.next()) {
        rsMsg.close();
        stmt.close();
        conn.close();
        return; 
      }
      //pull out the values we need.
      nextThread=rsMsg.getInt("NextThreadID");
      previousThread=rsMsg.getInt("PreviousThreadID");
      nextMessage=rsMsg.getInt("NextMessageID");
      previousMessage=rsMsg.getInt("PreviousMessageID");
      subThread=rsMsg.getInt("SubThreadID");
      mainThread=rsMsg.getInt("MainThreadID");
      forum=rsMsg.getInt("ForumID");
      rsMsg.close();
//if it's the last message in the thread, the next message id should be it's own msg id.
//If that's the case, set the previous msg's nextmessageid to it's own id.
//otherwise, to the next message id.
      sql = "UPDATE DiscussionBoardEntries SET NextMessageID="
      + (nextMessage==msgID?"ID":new Integer(nextMessage).toString()) 
      + ", Date=Date WHERE ID=" + previousMessage;
      log.paranoid("Executing(2): " + sql,"DiscussionBoard:deleteMsg");
      stmt.executeUpdate(sql);

//If this message is the -first- message in a thread, then, it's previous message id will be it's own id.
//So, in that case, set the next message's msg id to it's own id.
//Otherwise, set it to the previous msg id.
      sql = "UPDATE DiscussionBoardEntries SET PreviousMessageID="
      + (msgID==previousMessage?"ID":new Integer(previousMessage).toString()) + ", Date=Date WHERE ID=" + nextMessage;
      log.paranoid("Executing(3): " + sql,"DiscussionBoard:deleteMsg");
      stmt.executeUpdate(sql);
//replies to this message should probably still be displayed. Since deleting
//this message causes all replies to it to -not- be shown, set subthreadid
//for all replies to this message to this message's subthreadid. =)
      sql = "UPDATE DiscussionBoardEntries SET SubThreadID="
      + subThread + ",Date=Date WHERE SubThreadID=" + msgID;
      log.paranoid("Exeucting(4): " + sql,"DiscussionBoard:deleteMsg");
      stmt.executeUpdate(sql);
//check for main thread stuff...
//the update statement here works despite the previous subthreadid change
//because this is only for mainthread deletions. Main thread subid, mainthreadid
//and id are all the same. Hence, the previous statement will set all of the
//replies to this main thread to have a subthread id of the mainthread id.
//(basically, the above statement doesn't change diddly for responses to 
//a main thread). Hence, we can still use subthreadid and mainthreadid as 
//the identifiers.
      if (mainThread==msgID){//main thread
        log.paranoid("Msg we're deleting is a main thread.","DiscussionBoard:deleteMsg");
        sql="UPDATE DiscussionBoardEntries SET MainThreadID=ID, SubThreadID=ID, Date=Date WHERE"
        + " MainThreadID=" + msgID + " AND SubThreadID=" + msgID;
        log.paranoid("Executing(5): " + sql,"DiscussionBoard:deleteMsg");
        stmt.executeUpdate(sql);
        
        //reorder the previous/next threads, and fix the mainthread, and previous/next message fields..
        sql = "SELECT * FROM DiscussionBoardEntries WHERE ID=MainThreadID AND ForumID="
          + forum + " AND ID <>" + msgID + " ORDER BY ID"; 
        log.paranoid("Executing(6): " + sql,"DiscussionBoard:deleteMsg");
        ResultSet rsMainThreads=stmt.executeQuery(sql);
        //first, get the date so we can set it and not alter it with our updates.
        //Next, we need to see if this is the first entry, if so, pull the ID as the previous thread id.
        //Now, we deal with everything with this -one- message...
        //so, if this message wasn't a main thread originally, then the ID and the previous Message ID
        //WON'T be the same. What does this mean? This means that we need to set the new main thread's
        //previous message's next messageid to it's own id. We also need to set the new main thread's
        //previous message id to it's own id. We also need to update any subthreads of this new 
        //main thread id so that their main thread is this message's id. 
        //Then, we need to set all subthreads of the main thread to have a correct previous message id
        //and next message id. Phew. Got all that? ;) Then, we need to set the next thread id of the
        //previous thread to this main thread. Or, if it's the last message, it needs to be set to it's own thread. Yay.
        //finally, update the date, and then update the row, and repeat. bleh. ;)
        int previousThreadID=0;
        int mainID=0;
	String previousDate="";
        while (rsMainThreads.next()){
          //pull out the date to begin with.   
          previousDate=rsMainThreads.getString("Date");
          //pull out the id of this message.
          mainID = rsMainThreads.getInt("ID");
          //check if prev. msg id != the message's id, then, 
          //this wasn't originally a "main thread.". So, run the necessary
          //routines to clean things up and convert this to a "real" main thread.
          
          if (rsMainThreads.getInt("PreviousMessageID") != mainID) { 
            log.paranoid("This is a \"new\" main thread.","DiscussionBoard:deleteMsg");
            //update the previous message so that it's next msg id is it's own id.
            sql="UPDATE DiscussionBoardEntries SET Date=Date,"
            + "NextMessageID=ID WHERE NextMessageID='" + mainID + "'";
            log.paranoid("Executing(7): " + sql,"DiscussionBoard:deleteMsg");
            stmt.executeUpdate(sql);
            //update this message's previousmsg id to it's own id...
            rsMainThreads.updateInt("PreviousMessageID", mainID);
            //need to fix all subthreads of this message to have a main thread
            //id of this message. We'll handle this w/ a recursive method...
            log.paranoid("Attempting to fix replies.","DiscussionBoard:deleteMsg");
            fixReplies(mainID,mainID);
            //now that those are fixed, this is like a regular thread, so, continue.
          }
          //if this is the first, then, set the previousthreadid to it's own id.
          //Also, assign the sql in the if statement to be executed after.
          //Because if this is the first thread, we shouldn't be setting the 
          //next thread id to iteself... that would be bad. ;)
          //and then, in both, we have the sql statement to update the previousthreadid
          //of all the msgs that are subthreads of this msg
          if (rsMainThreads.isFirst()) {
            rsMainThreads.updateInt("PreviousThreadID",mainID);
            sql="UPDATE DiscussionBoardEntries SET PreviousThreadID='"
              + mainID + "' WHERE MainThreadID='" + mainID + "' AND ID <> '" 
              + mainID + "'";
          } else {
            rsMainThreads.updateInt("PreviousThreadID",previousThreadID);
            //update the NextThreadID of the previous thread's msgs to this id...
            sql="UPDATE DiscussionBoardEntries SET NextThreadID='" + mainID
            + "' WHERE MainThreadID='" + previousThreadID + "'";
            log.paranoid("Executing(8): " + sql,"DiscussionBoard:deleteMsg");
            stmt.executeUpdate(sql);
            sql="UPDATE DiscussionBoardEntries SET PreviousThreadID='"
              + previousThreadID + "' WHERE MainThreadID='" + mainID + "' AND ID <> '"
              + mainID + "'";
          }
          log.paranoid("Executing(9): " + sql,"DiscussionBoard:deleteMsg");
          stmt.executeUpdate(sql);
          //so, now that the thread is completely fixed,
          //
          //update the previousThreadID variable for the next time around... ;)
          previousThreadID=mainID;

          //if this is the last message, update the nextthreadid to it's own id
          if (rsMainThreads.isLast()) {
            rsMainThreads.updateInt("NextThreadID",mainID);
          }
          rsMainThreads.updateString("Date",previousDate);
          rsMainThreads.updateRow();
        }
       
      }
//delete the message.
      sql="DELETE FROM DiscussionBoardEntries WHERE ID='" + msgID + "'";
      log.paranoid("Executing(10): " + sql,"DiscussionBoard:deleteMsg");
      stmt.executeUpdate(sql);
      //no reason to keep these around if the message itself isn't around.
      //let's keep the db as empty as possible here. ;)
      sql="DELETE FROM ViewedMessages WHERE ID=" + msgID;
      log.paranoid("Exeucting(11):" + sql,"DiscussionBoard:deleteMsg");
      stmt.executeUpdate(sql);
      stmt.close();
      conn.close();
    }catch(Exception e){
      log.sparse("Caught exception: " + e.getMessage(),"DiscussionBoard:deleteMsg");
    }
    return;
  }

  String createBoardTables(Student student) {
    try{
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE DiscussionBoardEntries (ID INT PRIMARY KEY AUTO_INCREMENT, StudentIDNumber VARCHAR(50), Date TIMESTAMP, Subject VARCHAR(50), Message TEXT, MainThreadID INT, PreviousMessageID INT, NextMessageID INT, SubThreadID INT, PreviousThreadID INT, NextThreadID INT, ForumID INT DEFAULT 1, UseHTML VARCHAR(5))");
      stmt.executeUpdate("CREATE TABLE DiscussionBoardForums (ID INT PRIMARY KEY AUTO_INCREMENT, Name VARCHAR(30), AllowStudentThreads VARCHAR(5), AllowStudentDeletes VARCHAR(5), CourseSection CHAR(3))");
      stmt.executeUpdate("CREATE TABLE ViewedMessages (StudentIDNumber VARCHAR(50), ID INT)");      
      stmt.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return displayBoardContents(student,1);
  }
  
  private String getNameForum (int idForum){
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	    	
    	ResultSet rsNameForum = stmt.executeQuery("SELECT Name FROM DiscussionBoardForums WHERE ID='"+idForum+"'");
    	if (rsNameForum.next()) {
    		return (rsNameForum.getString("Name"));
    	}
    }catch (Exception e){
    	e.getMessage();
    }
    
    return "";
  }
  
  //stmt.executeUpdate("INSERT INTO DiscussionBoardForums (Name, AllowStudentThreads, AllowStudentDeletes,CourseSection) VALUES ('General','true','false','1')"); 

}
