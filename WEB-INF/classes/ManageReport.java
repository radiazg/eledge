package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.Enumeration;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageReport extends HttpServlet {
//for the popupWindow function to ensure a correct URI, even if there's an 
//a-typical server/servlet runner setup.
  String thisServletURI;
  Logger log = new Logger();
  RBStore res = EledgeResources.getManageReportBundle();
  // in this servlet, the index i is used to indicate the Assignment Number, and normally ranges from 0 to numberOfAssignments-1
  // conversely, the index j runs over the sections of the course, normally from 1 to numberOfSections.  The difference is due
  // to the fact that the auto_increment feature of mysql starts numering the SectionID variable at 1 not 0.  Therefore, the first
  // and default sectionID number is 1.  
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    thisServletURI = request.getRequestURI();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageReport");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageReport",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }

    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_teach")));
      return;
    }
    // from here on, user is assumed to be the instructor
    String userRequest=request.getParameter("UserRequest");
    if(userRequest != null){
        if (userRequest.equals("ShowFilenames")){
        String id = request.getParameter("StudentIDNumber");
        String assignment = request.getParameter("Assignment");
        if (id == null || id.equals("")){
           out.println(res.getString("str_bad_id"));
           return;
        }
        if (assignment == null || assignment.equals("")){
          out.println(res.getString("str_bad_assign"));
          return;
        } 
        out.println(showFilenames(id, assignment));
        return; 
      }
    }
    else out.println(Page.create(deadlinesForm()));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    thisServletURI=request.getRequestURI();
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageReport");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageReport",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }

    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_teach")));
      return;
    }
    // from here on, user is assumed to be the instructor
    StringBuffer error = new StringBuffer("");

    String userRequest = request.getParameter("UserRequest");
    if (userRequest == null || userRequest.equals("ManageReports")) {
      out.println(Page.create(deadlinesForm()));
      return;
    }

    if (userRequest.equals("Update")) {
      updateDeadlines(request,error);
      out.println(Page.create(error + deadlinesForm()));
      return;
    }
    else if (userRequest.equals("AddReport")) {
      addReport();
      out.println(Page.create(deadlinesForm()));
      return;
    }

    if (userRequest.equals("ClassReports")) {
      out.println(Page.create(classReports(student)));
      return;
    }
   
    String id = request.getParameter("StudentIDNumber");

    int assignmentNumber = 0;
    try {
      assignmentNumber = Integer.parseInt(request.getParameter("AssignmentNumber"));
    }
    catch (Exception e) {
      out.println(Page.create(res.getString("str_must_select")));
      return;
    }

    if (userRequest.equals("DeleteReport")) {
      deleteReport(assignmentNumber);
      out.println(Page.create(deadlinesForm()));
      return;
    }

    if (userRequest.equals("RecordScore")){
      int score = 0;
      try {
        score = Integer.parseInt(request.getParameter("Score"));
      }
      catch (Exception e) {
        out.println(Page.create(res.getString("str_bad_score")));
        return;
      }
      if (student.idNumberLooksValid(id)){
        recordScore(id, assignmentNumber, score, request.getRemoteAddr());
        out.println(Page.create(classReports(student)));
      }
      else out.println(Page.create(res.getString("str_error_score")));
      return;
    }

    out.println(Page.create(deadlinesForm()));
  }

  String deadlinesForm() {
    int numberOfSections = 1;
    boolean useSectionDeadlines = false;
    String[] sectionNames = null;
    StringBuffer buf = new StringBuffer();
    Date now = new Date();
    String uploadsURL="/" + Course.name + "/uploads/";
    boolean useColor1=true;
    String color1="#c1e28c";
    String color2="#ebff89";//a yellow

    buf.append("<h2>" + res.getString("str_title_report_info") + "</h2>"
    + res.getString("str_explain_reports"));
    buf.append("<FORM METHOD=POST>");
    buf.append("<input type=hidden name=UserRequest value=Update>");
    int numberOfAssignments = 0;
      
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      if (numberOfSections == 1) useSectionDeadlines = false;

      ResultSet rsParams = stmt.executeQuery("SELECT * FROM ReportParameters");
      if (rsParams.next()){
        useSectionDeadlines = rsParams.getBoolean("UseSectionDeadlines");
        try{
          uploadsURL= rsParams.getString("UploadsURL");
        }catch(Exception e){
          try{
            stmt.executeUpdate("ALTER TABLE ReportParameters ADD UploadsURL TEXT");
            stmt.executeUpdate("UPDATE ReportParameters SET UploadsURL='"+uploadsURL+"'");
          }catch(Exception e2){
            buf.append(e2.getMessage());
          }
        } 
      }
      sectionNames = new String[numberOfSections+1];
      ResultSet rsSectionNames = stmt.executeQuery("SELECT SectionName FROM CourseSections ORDER BY SectionID");
      for (int j=1;j<=numberOfSections;j++) 
        if (rsSectionNames.next()) sectionNames[j] = rsSectionNames.getString("SectionName");

      if (numberOfSections>1)
        buf.append("<INPUT TYPE=CHECKBOX VALUE=true NAME=UseSectionDeadlines" + (useSectionDeadlines?" CHECKED>":">") 
        + res.getString("str_param_deadlines"));
      numberOfSections = useSectionDeadlines?numberOfSections:1;
      buf.append("\n<br>" + res.getString("str_upload_url") + "&nbsp;<INPUT TYPE=TEXT NAME=UploadsURL VALUE='" + uploadsURL
      + "'>&nbsp;" + res.getString("str_explain_url"));  
      // print a header row to the reports info table
      buf.append("\n<h3>" + res.getString("str_title_deadlines") + "</h3>");
      buf.append("\n<table border=1 cellspacing=0><tr><th>" + res.getString("str_select") 
      + "</th><th>" + res.getString("str_report") + "</th><th>" + res.getString("str_title") 
      + "</th><th>" + res.getString("str_desc") + "</th><th>" + res.getString("str_prefix") 
      + "</th><th>" + res.getString("str_ext") + "</th>");  // print a header row to the table of report deadlines
      for (int j=1;j<=numberOfSections;j++) {
        if (useSectionDeadlines) buf.append("<th>" + res.getString("str_date_avail") + " " 
          + sectionNames[j] + "</th><th>" + res.getString("str_date_due")
          + sectionNames[j] + "</th>");
        else buf.append("<th>" + res.getString("str_date_avail") + "</th><th>"+ res.getString("str_date_due") + "</th>");
      }
      buf.append("</tr>");
      // get the report information and the database table stucture
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ReportInfo ORDER BY AssignmentNumber");
      ResultSetMetaData rsmd = rsInfo.getMetaData();
       
      String assignmentNumber;
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
      int i=0;
      while (rsInfo.next()) {  // print one row for each report
        String iStr = Integer.toString(i);
        assignmentNumber = rsInfo.getString("AssignmentNumber");
        buf.append("\n<tr bgcolor=" + (useColor1?color1:color2) 
        + "><td valign=top><INPUT TYPE=RADIO NAME=AssignmentNumber VALUE='" + assignmentNumber + "'></td>");
        buf.append("<td valign=top><INPUT SIZE=5 NAME=" + iStr + ":AssignmentNumber VALUE='"
        + assignmentNumber + "'></td>"
        + "<td valign=top><INPUT NAME=" + iStr + ":Title VALUE='" + rsInfo.getString("Title") + "'></td>"
        + "<td valign=top><TEXTAREA NAME=" + iStr + ":Description COLS=40 ROWS=4>" + rsInfo.getString("Description") + "</TEXTAREA></td>"
        + "<td valign=top><INPUT SIZE=5 NAME=" + iStr + ":FilePrefix VALUE='" + rsInfo.getString("FilePrefix") + "'></td>"
        + "<td valign=top><INPUT SIZE=5 NAME=" + iStr + ":FileExt VALUE='" + rsInfo.getString("FileExt") + "'></td>");
        // print 2 columns for each section; use values for section 1 if necessary
        for (int j=1;j<=numberOfSections;j++) {
          buf.append("<td valign=top><INPUT NAME=" + iStr + ":DateAvailable" + Integer.toString(j) + " VALUE='");
          try {
           buf.append(rsInfo.getString("DateAvailable" + Integer.toString(j)));
          }
          catch (Exception e) {
            buf.append(rsInfo.getString("DateAvailable1"));
          }
          buf.append("' SIZE=20></td>"); 
          buf.append("<td valign=top><INPUT NAME=" + iStr + ":DateDue" + Integer.toString(j) + " VALUE='");
          try {
            buf.append(rsInfo.getString("DateDue" + Integer.toString(j)));
          }
          catch (Exception e) {
            buf.append(rsInfo.getString("DateDue1"));
          }
          buf.append("' SIZE=20></td>"); 
        }
        buf.append("</tr>");
        i++;
        useColor1=!useColor1;
      }
      numberOfAssignments = i;
    }
    catch (Exception e) {
    }
    buf.append("\n</table><br>");

    buf.append("\n<INPUT TYPE=HIDDEN NAME=NumberOfAssignments VALUE=" + numberOfAssignments + ">");
    buf.append("\n<input type=submit value='" + res.getString("str_btn_update") + "'>&nbsp;");
    buf.append("<input type=reset value='" + res.getString("str_btn_reset") + "'><br>");
    buf.append("<input type=submit value='" + res.getString("str_btn_add") + "' "
    + "onClick=this.form.elements.UserRequest.value='AddReport'>&nbsp;");
    buf.append("<input type=submit value='" + res.getString("str_btn_del") + "' "
    + "onClick=this.form.elements.UserRequest.value='DeleteReport'><br>");
    buf.append("<input type=submit value='" + res.getString("str_btn_view") + "' " 
    + "onClick=this.form.elements.UserRequest.value='ClassReports'>");
    buf.append("</FORM>");

    return buf.toString();
  }

  void updateDeadlines(HttpServletRequest request, StringBuffer error) {
    int numberOfSections = 1;
    boolean useSectionDeadlines = request.getParameter("UseSectionDeadlines")==null?false:true;
    String uploadsURL = request.getParameter("UploadsURL")==null?"/" + Course.name + "/uploads/":request.getParameter("UploadsURL");
    StringBuffer sqlUpdate = new StringBuffer("UPDATE ReportParameters SET "
      + "UseSectionDeadlines=" + (useSectionDeadlines?"'true'":"'false'")
      + ", UploadsURL ='" + uploadsURL + "'" );
   try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      stmt.executeUpdate(sqlUpdate.toString());      
      ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      rsNSections.close();

      int nSectionsInfo =  (stmt.executeQuery("SELECT * FROM ReportInfo ORDER BY AssignmentNumber").getMetaData().getColumnCount() - 5)/2;
      for (int j=nSectionsInfo+1;j<=numberOfSections;j++) {
        stmt.executeUpdate("ALTER TABLE ReportInfo ADD COLUMN (DateAvailable" + Integer.toString(j) 
        + " DATETIME DEFAULT '2002-01-15 23:59:59',DateDue" + Integer.toString(j) + " DATETIME DEFAULT '2002-01-15 23:59:59')");
      }
      
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ReportInfo ORDER BY AssignmentNumber");
  
      int i=0;
      while (rsInfo.next()) {
        rsInfo.updateString("AssignmentNumber",request.getParameter(Integer.toString(i) + ":AssignmentNumber"));
        rsInfo.updateString("Title",CharHider.quot2literal(request.getParameter(Integer.toString(i) + ":Title")));
        rsInfo.updateString("Description",CharHider.quot2literal(request.getParameter(Integer.toString(i) + ":Description")));
        rsInfo.updateString("FilePrefix",request.getParameter(Integer.toString(i) + ":FilePrefix"));
        rsInfo.updateString("FileExt",request.getParameter(Integer.toString(i) + ":FileExt"));
        for (int j=1;j<=numberOfSections;j++) {
          String date = request.getParameter(Integer.toString(i) + ":DateAvailable" + Integer.toString(j));
          if (date != null) rsInfo.updateString("DateAvailable" + Integer.toString(j),date);
          date = request.getParameter(Integer.toString(i) + ":DateDue" + Integer.toString(j));
          if (date != null) rsInfo.updateString("DateDue" + Integer.toString(j),date);
        }
        rsInfo.updateRow();
        i++;
      }
      rsInfo.close();
    }
    catch (Exception e) {
      error.append(e.getMessage());
    }
  }

  void addReport() {
    Date now = new Date();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      StringBuffer sqlInsert = new StringBuffer("INSERT INTO "
      + "ReportInfo (Title,Description,FilePrefix,FileExt,DateAvailable1,DateDue1) "
      + "VALUES ('" + res.getString("str_new_report") + "','" + res.getString("str_desc") + "','report1','.doc',now(),now())");
      stmt.executeUpdate(sqlInsert.toString());
   }
    catch (Exception e) {
    }
  }

  void deleteReport(int assignmentNumber) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM ReportInfo WHERE AssignmentNumber=" + assignmentNumber);
    }
    catch (Exception e) {
    }
  }

  String classReports(Student student) {
    StringBuffer buf = new StringBuffer();
    Date now = new Date();
    SimpleDateFormat df = new SimpleDateFormat("M-d h:mm a");
    int numSections = 1;
    
    buf.append("<h3>" + res.getString("str_title_class_reports") + "</h3>" + now);
    buf.append("<P>" + res.getString("str_dl_report") + "</P>");

    buf.append("<SCRIPT>\n<!--\n"
    + "function openWindow(assignment,id) {"
    + "\n url = '" + thisServletURI + "?UserRequest=ShowFilenames&StudentIDNumber='+id+'&Assignment='+assignment;"
    + "\n window.open(url,'StudentFilenames', 'resizable,width=600,height=400');"
    + "\n}\n-->\n</SCRIPT>\n");

    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      String sqlQueryString;
      String sqlQueryString2;

      ResultSet rsUploadsURL = stmt.executeQuery("SELECT UploadsURL from ReportParameters");
      String uploadsURL = (rsUploadsURL.next()?rsUploadsURL.getString("UploadsURL"):"/" + Course.name + "/uploads/");
      rsUploadsURL.close();
      //make sure there's a / at the end....
      uploadsURL=uploadsURL + (uploadsURL.lastIndexOf('/')+1 == uploadsURL.length()?"":"/");
      
     buf.append("<FORM METHOD=POST NAME='ReportForm'><INPUT TYPE=HIDDEN NAME='UserRequest'>");
      buf.append("<TABLE BORDER=1 CELLSPACING=0><TR><TD></TD><TH ALIGN=CENTER>" + res.getString("str_name") + "</TH>");
      ResultSet rsSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsSections.next()) numSections = rsSections.getInt("Value");
      rsSections.close();

      if (numSections > 1)
        buf.append("<th>" + res.getString("str_section") + "</th>");

      sqlQueryString = "SELECT ReportTransactions.AssignmentNumber AS Assignment, ReportInfo.Title AS Title "
      + "FROM ReportTransactions LEFT OUTER JOIN ReportInfo USING(AssignmentNumber) GROUP BY Assignment ORDER BY Assignment";
      ResultSet rsAssignments = stmt.executeQuery(sqlQueryString);
      Vector assignments = new Vector();
      Vector filePrefix = new Vector();//make sure that the filename is really what we want...
      Vector fileSuffix = new Vector();
      while (rsAssignments.next()){
        buf.append("<th align=center>" + rsAssignments.getString("Title") + "</th>");
        assignments.addElement(rsAssignments.getString("Assignment"));
      }
      buf.append("</tr>");

      sqlQueryString = "SELECT CONCAT(Students.LastName,'&nbsp;',Students.FirstName) AS Name,"
      + " Students.StudentIDNumber AS ID, Students.SectionID AS Section,"
      + " ReportTransactions.AssignmentNumber AS Assignment FROM Students"
      + " LEFT OUTER JOIN ReportTransactions USING(StudentIDNumber)"
      + " GROUP BY Assignment, ID ORDER BY Section, Students.LastName,"
      + " Students.FirstName, Assignment";

      if (student.getIsTA()) {
	TA ta = TAS.getTA(student.getIDNumber());
	TAPermission tap = ta.getPermission("ManageReport_ClassReports");
	if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)
	  || tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT))
	  return Permissions.getPermission("ManageReport_ClassReports").getDenyMsg();
	
	if (tap.getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
	  //this monstrosity pulls out the student information, like the normal sql string, but.....
	  //this one only pulls out students who are assigned to this ta. Yay. =)
	  sqlQueryString = "SELECT CONCAT(Students.Firstname,'&nbsp;',Students.LastName) "
	    + "AS Name, Students.StudentIDNumber AS ID, Students.SectionID AS "
	    + "Section, ReportTransactions.AssignmentNumber  AS Assignment, "
            + "Students.Status AS Status,  "
	    + "TAAssignments.StudentIDNumber AS TAID, TAAssignments.Type, "
	    + "TAAssignments.Value FROM Students LEFT OUTER JOIN ReportTransactions USING(StudentIDNumber) "
            + "LEFT OUTER JOIN TAAssignments ON (TAAssignments.StudentIDNumber='" 
	    + student.getIDNumber() 
	    + "' AND TAAssignments.Type='Student' AND TAAssignments.Value=Students.StudentIDNumber) "
	    + "WHERE TAAssignments.Value IS NOT NULL AND Status='Current' GROUP BY Assignment,ID "
	    + "ORDER BY Status, Section, Students.LastName, Students.FirstName, Assignment";
	}
      }

      ResultSet rsReportData = stmt.executeQuery(sqlQueryString);

      boolean more = rsReportData.next();
      while (more){
        boolean hasURL=true;
        buf.append("\n<tr>");
        String id = rsReportData.getString("ID");
        //this query pulls out one transaction for each student for each
        //assignment that they've attempted, making sure that it's the 
        //very last attempt made.
        sqlQueryString2="SELECT ReportTransactions.*, ReportInfo.Title FROM "
        + "(ReportTransactions LEFT JOIN ReportTransactions AS RT2 ON "
        + "ReportTransactions.AssignmentNumber=RT2.AssignmentNumber AND "
        + "ReportTransactions.StudentIDNumber=RT2.StudentIDNumber AND "
        + "ReportTransactions.Timestamp < RT2.Timestamp) LEFT OUTER JOIN "
        + "ReportInfo ON ReportTransactions.AssignmentNumber = "
        + "ReportInfo.AssignmentNumber WHERE RT2.Timestamp IS NULL AND "
        + "ReportTransactions.StudentIDNumber='" + id + "' ORDER BY AssignmentNumber";
	ResultSet rsReports = stmt2.executeQuery(sqlQueryString2);
        rsReports.first();
        buf.append("<TD ALIGN=CENTER><INPUT TYPE=RADIO NAME=StudentIDNumber Value='" + id + "'></TD>");
        buf.append("<TD ALIGN=CENTER>" + rsReportData.getString("Name") + "</TD>");
        if (numSections > 1)
          buf.append("<TD ALIGN=CENTER>" + rsReportData.getString("Section") + "</TD>");

        if (rsReportData.getString("Assignment") == null){
          for(int i=0; i<assignments.size();i++){
            buf.append("<TD>&nbsp;</TD>");
          }
          more = rsReportData.next();
        }
        else {
          for(int i=0;i<assignments.size();i++){
            if(!more) buf.append("<TD>&nbsp;</TD>");
            else if (rsReportData.getString("Assignment") == null)
             buf.append("<TD>&nbsp;</TD>");
            else {
              if (rsReportData.getString("ID").equals(id)
              & rsReportData.getString("Assignment").equals((String)assignments.get(i))){
	        buf.append("<TD ALIGN=LEFT><A HREF='" + uploadsURL
                  + rsReports.getString("ServerFilename") + "'>" + rsReports.getString("Title")
                  + "</A><BR>" + df.format(rsReports.getTimestamp("Timestamp"))
                  + "<BR><i>" + res.getString("str_graded") + "</i> " 
                  + (rsReports.getBoolean("Graded")?res.getString("str_yes"):res.getString("str_no"))
                  + "<BR><A HREF=javascript:openWindow('"
                  + rsReports.getString("AssignmentNumber") + "','" + id + "');>" + res.getString("str_details") 
                  + "</A></TD>");
		rsReports.next();
                more = rsReportData.next();
              }
              else{
                buf.append("<TD ALIGN=CENTER>&nbsp;</TD>");
              }
            }
          }
        }
        buf.append("</TR>");
      }
      buf.append("</TABLE>");
      MessageFormat mf = new MessageFormat(res.getString("str_add_score"));
      Object[] args = {
        "<INPUT TYPE=TEXT SIZE=3 NAME='Score'>",
        reportSelect()
      };
      buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_ret_report") + "' "
      + "onClick=this.form.elements.UserRequest.value='ManageReports';><br><hr width=100%>\n" + mf.format(args)
      + "<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='RecordScore' Value='" + res.getString("str_btn_record") + "'>");


      buf.append("</FORM>");
    }catch(Exception e){
      if(addGradedField()) {
	log.paranoid("Graded field was missing. Added. Trying classReports again.","ManageReport:classReports()");
        return classReports(student);
      }
      return e.getMessage();
    }
    return buf.toString();
  }

  String reportSelect() {
    StringBuffer buf = new StringBuffer("<SELECT NAME='AssignmentNumber'>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT ReportTransactions.AssignmentNumber AS "
      + "Assignment, ReportInfo.Title AS Title FROM ReportTransactions LEFT "
      + "OUTER JOIN ReportInfo USING(AssignmentNumber) GROUP BY Assignment "
      + "ORDER BY Assignment";
      ResultSet rsReports = stmt.executeQuery(sqlQueryString);
      while (rsReports.next()) {
        buf.append("<OPTION VALUE=" + rsReports.getString("Assignment") 
	  + ">" + rsReports.getString("Title"));
      }
      buf.append("</SELECT>");
    } catch(Exception e) {
      log.sparse(e.getMessage(),"ManageReport:reportSelect");
      return "";
    }
    return buf.toString();
  }

  boolean addGradedField(){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE ReportTransactions ADD Graded VARCHAR(5) DEFAULT 'false'");
      stmt.close();
      conn.close();
    }catch(Exception e){
      return false;
    }
    return true;
  }

  void recordScore(String id, int assignment, int score, String ipNumber){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsTitle = stmt.executeQuery("SELECT Title FROM ReportInfo WHERE AssignmentNumber='" + assignment + "'");
      String assignmentName = (rsTitle.next()?rsTitle.getString("Title"):"Report" + assignment);
      stmt.executeUpdate("INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,Code,TestType) "
      + "VALUES ('" + id + "','" + assignmentName + "','" + score + "','" + ipNumber + "','-1','void')");
      stmt.executeUpdate("UPDATE ReportTransactions SET Graded='true', Timestamp=Timestamp WHERE StudentIDNumber='" + id
      + "' AND AssignmentNumber='" + assignment + "'");
    }catch(Exception e){
    }
    return;
  }

  String showFilenames(String id, String assignment){
    StringBuffer buf = new StringBuffer();
    SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss a");
    try{
      MessageFormat mf = new MessageFormat(res.getString("str_title_attempts"));
      Object[] args = {
        assignment,
        id
      };
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsFilenames = stmt.executeQuery("SELECT ClientFilename, Timestamp, ServerFilename,Errors "
      + "FROM ReportTransactions WHERE StudentIDNumber='" + id + "' AND "
      + "AssignmentNumber='" + assignment + "' ORDER BY Timestamp");
      buf.append("<html><body onLoad=window.focus()><h3>" + mf.format(args) + "</h3><table border=1 cellspacing=0>"
	+ "<tr><td>&nbsp</td><th>" + res.getString("str_stud_file") + "</th><th>" 
        + res.getString("str_server_file") + "</th><th>" + res.getString("str_time") 
        + "</th><th>" + res.getString("str_errors") + "</tr>");
      int count = 0;
      if (!rsFilenames.next())
        return res.getString("str_no_files");
      do{
        buf.append("\n<TR><TD>" + ++count + "</TD>"
        + "<TD>" + rsFilenames.getString("ClientFilename") + "</TD>"
	+ "<TD>" + rsFilenames.getString("ServerFilename") + "</TD>"
        + "<TD>" + df.format(rsFilenames.getTimestamp("Timestamp")) + "</TD>"
        + "<TD>" + rsFilenames.getString("Errors")  + "</TR>");
      }while (rsFilenames.next());
      buf.append("</TABLE></BODY></HTML>");
    }catch(Exception e){
      if (addErrorsField())
        return showFilenames(id,assignment);
      return e.getMessage() + "<br>" + res.getString("str_no_files_for_stud");
    }
    return buf.toString();
  }

  private boolean addErrorsField() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sql = "ALTER TABLE ReportTransactions ADD (Errors TEXT)";
      log.paranoid("Executing: " + sql,"Report:addErrorsField");
      stmt.executeUpdate(sql);
      sql = "UPDATE ReportTransactions SET Errors=''";
      stmt.executeUpdate(sql);
      stmt.close();
      conn.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}

