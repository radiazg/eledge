package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.http.*;
import javax.servlet.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.text.MessageFormat;

public class ManageJournal extends HttpServlet {

  Logger log = new Logger();
  RBStore res = EledgeResources.getManageJournalBundle();
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    doPost(request,response);    
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "ManageJournal");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_frozen")));
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageJournal",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_not_instructor")));
      return;
    }
    // from here on, student is assumed to be the instructor
    
    String userRequest = request.getParameter("UserRequest");
    if (userRequest==null) userRequest = "Summary";
    String studentIDNumber = request.getParameter("StudentIDNumber");

    if (userRequest.equals("Submit")) {
      try {
        int entryID = Integer.parseInt(request.getParameter("EntryID"));
        String reviewed = request.getParameter("Reviewed");
        if (reviewed==null || !reviewed.equals("true")) reviewed = "false";
        String comments = request.getParameter("Comments");
        if (comments==null) comments = "";
        submitComments(entryID,reviewed,comments);
        int score=0;
        try{
          String scorestr = request.getParameter("Score");
          //if it's blank, don't record the score; this makes scoring optional 
          if (scorestr != null && !scorestr.equals("")){
            score = Integer.parseInt(scorestr);
            recordScore(studentIDNumber,score,request.getRemoteAddr(),out);
          }
        }catch(Exception e){
          out.println(e.getMessage());
        }
        String emailResponse=request.getParameter("EmailResponse");
        if (emailResponse != null && emailResponse.equals("true"))
          sendMessage(student, studentIDNumber, Message.RecipientType.TO, entryID,comments); 
        userRequest = "Review";
      }
      catch (Exception e) {
        out.println(Page.create(e.getMessage()));
        if (student.getIsTA())
          student.setIsInstructor(false);
        return;
      }
    } 
    
    if (userRequest.equals("Summary")) {
      out.println(Page.create(journalSummary(student)));
      if (student.getIsTA())
        student.setIsInstructor(false);
      return;
    }

    if (studentIDNumber==null||studentIDNumber.equals("any")) 
      studentIDNumber = nextEntryStudentID(student);

    if (userRequest.equals("Review")) {
      boolean wrapText = getWrapText(request);
      int wrapLength = getWrapLength(request);
      try {
        int thisEntry = Integer.parseInt(request.getParameter("ThisEntry"));
        out.println(Page.create(reviewNextEntry(studentIDNumber,thisEntry, wrapText, wrapLength)));
      }
      catch (Exception e) {
        out.println(Page.create(reviewNextEntry(studentIDNumber,0, wrapText, wrapLength)));
      }
      if (student.getIsTA())
        student.setIsInstructor(false);
      return;
    }
  }

  String nextEntryStudentID(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQuery = "SELECT StudentIDNumber,Reviewed FROM JournalEntries ORDER BY Date";
      ResultSet rsNextEntry = stmt.executeQuery(sqlQuery);
      rsNextEntry.first();
      while (rsNextEntry.getBoolean("Reviewed") && !rsNextEntry.isLast()) rsNextEntry.next();
      return rsNextEntry.getString("StudentIDNumber");
    }
    catch (Exception e) { // returns Instructor ID in case of error or no journal entries
      return student.getIDNumber(); 
    }
  }
  
  String reviewNextEntry(String studentIDNumber,int thisEntry, boolean wrap, int length) {
    StringBuffer buf = new StringBuffer(appendCheckboxScript());
    MessageFormat mf = new MessageFormat(res.getString("str_jumpto"));
    Object[] args = new Object[2];
    buf.append("\n<h3>" + res.getString("str_title_navigate") + "</h3>");
    buf.append("\n<a href='" + Course.name + ".ManageJournal?UserRequest=Summary'>" 
     + res.getString("str_summary") + "</a>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQuery = "SELECT * FROM JournalEntries LEFT JOIN Students USING (StudentIDNumber) "
      + "WHERE Students.StudentIDNumber='" + studentIDNumber + "' ORDER BY Date";
      ResultSet rsEntries = stmt.executeQuery(sqlQuery);
      rsEntries.last();
      int nEntries = rsEntries.getRow();
      if (nEntries>0 && thisEntry==0) {  // display the oldest unreviewed entry
        rsEntries.first();               // or last entry if all are reviewed
        while (rsEntries.getBoolean("Reviewed") && !rsEntries.isLast()) rsEntries.next();
        thisEntry = rsEntries.getRow();
      }
      else {            // make sure thisEntry is in bounds
        if (thisEntry < 1) thisEntry = 1; 
        if (thisEntry > nEntries) thisEntry = nEntries;
        rsEntries.first();
        while (rsEntries.getRow()<thisEntry) rsEntries.next();
      }
      args[0] = "<INPUT SIZE=2 NAME=ThisEntry VALUE='" + thisEntry + "'>";
      args[1]=new Integer(nEntries);
      buf.append("<TABLE><TR VALIGN=baseline ALIGN=CENTER><TD><b>"
      + res.getString("str_this_stud") + "</b></TD><TD>");
      
      buf.append("<FONT FACE=Arial><b>" + (thisEntry>1?"<a href=" + Course.name 
      + ".ManageJournal?UserRequest=Review&ThisEntry=" + (thisEntry-1)
      + "&StudentIDNumber=" + studentIDNumber + ">" + res.getString("str_prev")
      + "</a>":"<FONT COLOR=#BBBBBB>" + res.getString("str_prev") + "</FONT>" + "</b></FONT></TD>"));
      
      buf.append("\n<TD><FORM METHOD=POST ACTION='" + Course.name + ".ManageJournal'>"
      + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Review>"
      + "<INPUT TYPE=HIDDEN NAME=StudentIDNumber VALUE='" + studentIDNumber + "'>"
      + mf.format(args) + " <INPUT TYPE=SUBMIT VALUE='"
      + res.getString("str_btn_go") + "'></TD>");      
      
      buf.append("<TD><FONT FACE=Arial><b>" + (thisEntry<nEntries?"<a href=" + Course.name 
      + ".ManageJournal?UserRequest=Review&ThisEntry=" + (thisEntry+1) 
      + "&StudentIDNumber=" + studentIDNumber + ">" + res.getString("str_next")
      + "</a>": "<FONT COLOR=#BBBBBB>Next</FONT>" + "</b></FONT>")
      + "</TD></FORM></TR>");
      
      buf.append("<TR VALIGN=baseline ALIGN=CENTER>"
      + "<FORM METHOD=POST ACTION='" + Course.name + ".ManageJournal'>"
      + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Review>"
      + "<TD><b>" + res.getString("str_all_stud") + " </b></TD><TD COLSPAN=3>");
      mf.applyPattern(res.getString("str_next_all"));
      args[0]=classSelectBox();
      buf.append(mf.format(args)+ " <INPUT TYPE=SUBMIT VALUE='"
      + res.getString("str_btn_go") + "'></TD></FORM></TR></TABLE><HR>");
      
      buf.append("<H3>" + res.getString("str_title_review") + "</H3>");
      
      if (nEntries>0) {
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");
        String submitted = df.format(rsEntries.getTimestamp("Date"));
        String title = rsEntries.getString("Title");
        String entryText = rsEntries.getString("Entry");
        boolean preformatted = rsEntries.getBoolean("Preformatted");
        boolean reviewed = rsEntries.getBoolean("Reviewed");
        String comments = rsEntries.getString("Comments");
        int entryID = rsEntries.getInt("EntryID");
        String studentName = rsEntries.getString("FirstName") + " " + rsEntries.getString("LastName");
        buf.append("<TABLE BORDER=1 CELLSPACING=0 width=100%><TR><TD>"
        + showEntry(studentName,submitted,title,entryText,preformatted,reviewed,comments,wrap,length)
        + "</TD></TR></TABLE>");
        buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".ManageJournal'>"
        + "<INPUT TYPE=CHECKBOX VALUE='true' NAME=WrapText "
        + "onChange=\"updateWrapInfo(this);\""+ (wrap?" CHECKED>":">") 
        + res.getString("str_wrap") + "<br>");
        if (rsEntries.getString("Reviewed")==null) reviewed = true;
        buf.append(commentsForm(entryID,studentIDNumber,reviewed,comments));
      }
      else buf.append(res.getString("str_no_entry"));
    }
    catch (Exception e) {
        buf.append(e.getMessage());
    }
    return buf.toString();
  }

  String classSelectBox() {
    try {
      StringBuffer buf = new StringBuffer("<SELECT NAME=StudentIDNumber>");
      buf.append("<OPTION VALUE='any' SELECTED>" + res.getString("str_select_any") + "</OPTION>");
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQuery = "SELECT StudentIDNumber,LastName,FirstName FROM Students ORDER BY Status,LastName,FirstName";
      ResultSet rsStudents = stmt.executeQuery(sqlQuery);
      while (rsStudents.next()) 
        buf.append("<OPTION VALUE='" + rsStudents.getString("StudentIDNumber") 
        + "'>" + rsStudents.getString("FirstName") + " " 
        + rsStudents.getString("LastName") + "</OPTION>");
      buf.append("</SELECT>");
      rsStudents.close();
      stmt.close();
      conn.close();
      return buf.toString();
    }
    catch (Exception e) {
      return e.getMessage();
    }
  }
  
  String commentsForm(int entryID, String studentIDNumber, boolean reviewed, String comments) {
    MessageFormat mf = new MessageFormat(res.getString("str_submit"));
    Object[] args = new Object[2];
    StringBuffer buf = new StringBuffer();
    if (comments==null) comments = "";
    buf.append("<INPUT TYPE=CHECKBOX NAME=Reviewed VALUE='true'" + (reviewed?" CHECKED>":">")
    + res.getString("str_mark_read") + "<br>"
    + "<INPUT TYPE=CHECKBOX NAME=EmailResponse VALUE='true'>"
    + res.getString("str_send_email") + "<br>"
    + res.getString("str_score") + "&nbsp;<INPUT TYPE=TEXT SIZE=3 NAME=Score>"
    + res.getString("str_score_option") + "<br>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Submit>"
    + "<INPUT TYPE=HIDDEN NAME=EntryID VALUE='" + entryID + "'>"
    + "<INPUT TYPE=HIDDEN NAME=StudentIDNumber VALUE='-1'><br>"
    + res.getString("str_comments_here") + "<br><TEXTAREA ROWS=10 COLS=60 NAME=Comments WRAP=HARD>"
    + (reviewed?comments:"") + "</TEXTAREA><br>");
    
    //i18n'ed final submit line. 

    args[0] = "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_this") + "' onClick=StudentIDNumber.value='" + studentIDNumber + "';>";
    args[1] = "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_any") + "' onClick=StudentIDNumber.value='any';>";
    buf.append(mf.format(args) + "</FORM>");
    return buf.toString();
  }
  
  String submitComments(int entryID, String reviewed, String comments) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("UPDATE JournalEntries SET Reviewed='" + reviewed + "',Comments='" + CharHider.quot2literal(comments)
      + "' WHERE EntryID='" + entryID + "'");
      stmt.close();
      conn.close();
      return "";
    }
    catch (Exception e) {
      return e.getMessage();
    }
  }
  
  String showEntry(String studentName, String submitted, String title, String entryText, 
    boolean preformatted, boolean reviewed, String comments, boolean wrap, int wrapLength) {
    StringBuffer buf = new StringBuffer();
    buf.append(studentName + "<br>" + submitted + "<br>");
    if (preformatted) buf.append("<h3>" + title + "</h3><PRE>" + (wrap?wrapText(entryText,wrapLength):entryText) + "</PRE>");
    else buf.append("<h3>" + title + "</h3>" + "<TABLE><TR><TD>" + entryText + "</TD></TR></TABLE>");
    if (reviewed) {
      buf.append("<hr>" + res.getString("str_isread") + "<br>");
      if (comments.length()>0) buf.append("<b>" + res.getString("str_comments")
      + "</b><br><PRE>" + comments + "</PRE>");
    }
    return buf.toString();
  }

  void recordScore(String id, int score, String ipAddress, PrintWriter out){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsReviewedCount = stmt.executeQuery("SELECT COUNT(*) FROM JournalEntries WHERE Reviewed='true' AND StudentIDNumber='" + id + "'");
      int count = 1;
      if (rsReviewedCount.next()){
        count = rsReviewedCount.getInt("COUNT(*)");
      }
      stmt.executeUpdate("INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType) "
      + "VALUES ('"+id+"','Jour"+count+"','"+score+"','"+ipAddress+"','-1','void')");
    }catch(Exception e){
      out.println(e.getMessage());
    }
  }

  String sendMessage(Student student, String id, Message.RecipientType type, int entryID, String text) {
    StringBuffer err = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsEntryText = stmt.executeQuery("SELECT Entry FROM JournalEntries WHERE EntryID='" + entryID + "'");
      String journalText;
      if (rsEntryText.next()){
        journalText = rsEntryText.getString("Entry");
      } else journalText = res.getString("str_entry_na");

      ResultSet rsRecipient=stmt.executeQuery("SELECT Email FROM Students WHERE StudentIDNumber='" + id + "'");
      if (!rsRecipient.next()) return res.getString("str_no_stud");
      Properties props = System.getProperties();
      props.put("mail.smtp.host",Course.outgoingMailServer);
      Session session = Session.getDefaultInstance(props,null);
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(student.email,student.getFullName()));
      message.addRecipients(type, rsRecipient.getString("Email"));
      message.setSubject(res.getString("str_email_subject"));
      message.setText(res.getString("str_orig_entry") + "\n" 
      + journalText + "\n\n" + res.getString("str_teach_response")
      + "\n" + text);
      Transport.send(message);
      return res.getString("str_send_ok");
    }
    catch (Exception e) {
      return res.getString("str_send_bad") + err + e.getMessage();
    }
  }
//prints out a page summarizing all read and unread messages for all students.
  private String journalSummary(Student student) {
    MessageFormat mf = new MessageFormat(res.getString("str_summary_title"));
    Object[] args = {
      Course.name
    };
    StringBuffer buf = new StringBuffer("<h2>" + mf.format(args) +"</h2>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString = "SELECT CONCAT(Students.LastName, \", \", "
        + "Students.FirstName) AS Name, JournalEntries.StudentIDNumber AS ID "
	+ "FROM JournalEntries LEFT OUTER "
	+ "JOIN Students USING (StudentIDNumber) GROUP BY ID ORDER BY Name";
      if (student.getIsTA()) {
        TA ta = TAS.getTA(student.getIDNumber());
        if (ta.getPermission("ManageJournal_Summary").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
           sqlQueryString="SELECT CONCAT(Students.LastName, \", \", Students.FirstName) "
             + "AS Name, JournalEntries.StudentIDNumber AS ID, TAAssignments.StudentIDNumber, "
             + "TAAssignments.Value, TAAssignments.Type From JournalEntries "
             + "LEFT OUTER JOIN Students USING(StudentIDNumber) LEFT JOIN "
             + "TAAssignments ON (TAAssignments.Type='Student' AND TAAssignments.StudentIDNumber='"
             + student.getIDNumber() + "' AND TAAssignments.VAlue=JournalEntries.StudentIDNumber) "
             + "WHERE TAAssignments.StudentIDNumber IS NOT NULL GROUP BY ID ORDER BY Name";
        }
      }
      buf.append("<TABLE BORDER=1 CELLSPACING=0><TR><TH>" + res.getString("str_stud_name") 
      + "</TH><TH>" + res.getString("str_studid") + "</TH><TH>"
      + res.getString("str_unreviewed") + "</TH><TH>"
      + res.getString("str_reviewed") + "</TH></TR>");
      ResultSet rsStudents = stmt.executeQuery(sqlQueryString);
      //Rob, work on this some more when you're more awake. Find a better/more efficient way of doing this. =/
      while (rsStudents.next()) {
	String studentID=rsStudents.getString("ID");
        buf.append("<TR><TD><A HREF=/servlet/" + Course.name 
	  + ".ManageJournal?UserRequest=Review&StudentIDNumber=" + studentID 
	  + "&ThisEntry=0>" + rsStudents.getString("Name") + "</A></TD><TD>" 
	  + studentID + "</TD>");
        ResultSet rsCount = stmt2.executeQuery("SELECT COUNT(*) FROM JournalEntries WHERE StudentIDNumber='" + studentID + "' AND Reviewed IS NULL");
	if (rsCount.next())
	  buf.append("<TD>" + rsCount.getString("COUNT(*)") + "</TD>");
        rsCount = stmt2.executeQuery("SELECT COUNT(*) FROM JournalEntries WHERE StudentIDNumber='" + studentID + "' AND Reviewed IS NOT NULL");
	if (rsCount.next())
	  buf.append("<TD>" + rsCount.getString("COUNT(*)") + "</TD>");
        buf.append("</TR>");
      }
      buf.append("</TABLE>");
      stmt.close();
      stmt2.close();
      conn.close();
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManageJournal:journalSummary()");
      return e.getMessage();
    }
    return buf.toString();
  }

  public String wrapText(String oldString, int limit) {
    StringBuffer buf = new StringBuffer("");
    int lastSpace=0;
    int currLen=0;

    for (int i=0; i<oldString.length(); i++) {
      boolean update=true;
      if (oldString.charAt(i)==' ') {
        lastSpace=i;
      }
      if (oldString.charAt(i)=='\n') {
        update=false;
        currLen=0;
      }
      //the append has to go before the check, because the lastSpace is the current character, waiting to be appended, 
      //and we try a setCharAt(lastSpace,'\n'), we're going to get an index out of bound exception thrown.

      buf.append(oldString.charAt(i));

      if (currLen>=limit) {
        update=false;
        buf.setCharAt(lastSpace,'\n');
        currLen=0;
      }
      if (update) {
        currLen++;
      }
    }
    return buf.toString();
  }

  boolean getWrapText(HttpServletRequest request) {
    log.paranoid("Begin method.","ManageJournal:getWrapText");
    boolean ret=false;
    Cookie[] c = request.getCookies();
    for (int i=0; i<c.length;i++) {
      if (c[i].getName().equals("wrapText")) {
        log.paranoid("Found the cookie!","ManageJournal:getWrapText");
        ret = new Boolean(c[i].getValue()).booleanValue();
        break;
      }
    }
    log.paranoid("End method. Returning: " + ret,"ManageJournal:getWrapText");
    return ret;
  }

  int getWrapLength(HttpServletRequest request) {
    log.paranoid("Begin method.","ManageJournal:getWrapLength");
    int ret=300;
    Cookie[] c = request.getCookies();
    for (int i=0; i<c.length;i++) {
      if (c[i].getName().equals("wrapLength")) {
        log.paranoid("I found the cookie! Yummy!","ManageJournal:getWrapLength"); 
        ret=Integer.parseInt(c[i].getValue());
        break;
      }
    }
    log.paranoid("End method. Returning: " + ret,"ManageJournal:getWrapLength");
    return ret;
  }

  String appendCheckboxScript() {
    StringBuffer buf = new StringBuffer("<SCRIPT>\n");
    buf.append("<!--\n"
    + "function updateWrapInfo(field) {\n"
    + "  var expDate = new Date('December 31, 2023');\n"
    + "  var wrapTCookie = 'wrapText=' + field.checked + ';expires=' + expDate.toGMTString();\n"
    + "  var wrapLCookie = 'wrapLength=';\n"
    + "  var len=300;\n"
    + "  var reload=false;\n"
    + "  if (field.checked) {\n"
    + "    len=prompt('" + res.getString("str_prompt_wrap") + "')\n"
    + "    reload=true;\n"
    + "  }\n"
    + "  wrapLCookie = wrapLCookie + len + ';expires=' + expDate.toGMTString();\n" 
    + "  document.cookie=wrapTCookie;\n"
    + "  document.cookie=wrapLCookie;\n"
    + "  if (reload) {\n"
    + "    window.location.reload();\n"
    + "  }\n"
    + "}\n"
    + "-->\n"
    + "</SCRIPT>\n");
    return buf.toString();
  }
}
