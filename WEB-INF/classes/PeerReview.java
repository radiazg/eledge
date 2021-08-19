package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import javax.servlet.http.*;
import javax.servlet.*;
/*****************************************
  database tables used by the PeerReview servlet:
    PeerReviews: ReviewID,ReportTitle,ReportFileName,Author1ID,Author2ID,ReviewerID,
    Status,DateAssigned,DateSubmitted,Score,Response1,Response2,Response3,...
    PeerReviewQuestions: (Same structure as QuizQuestions, so that Question class works OK)
    Students: (not modified; used to put names to ID numbers)
******************************************/

public class PeerReview extends HttpServlet {
  
  RBStore res = EledgeResources.getPeerReviewBundle();
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
      response.sendRedirect(Course.secureLoginURL + "PeerReview");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("PeerReview",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
    // from here on, student id is assumed to be valid
    String userRequest = request.getParameter("UserRequest");
    int reviewID=0;
    try {
      reviewID = Integer.parseInt(request.getParameter("ReviewID"));
    }
    catch (Exception e) {
    }
    if (userRequest==null) out.println(Page.create(myReviews(student)));
    else if (userRequest.equals("view")) out.println(Page.create(viewReview(student,reviewID)));
    else if (userRequest.equals("edit")) out.println(Page.create(editReview(student,reviewID)));
    else out.println(Page.create(myReviews(student)));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "PeerReview");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("PeerReview",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
    // from here on, student id is assumed to be valid
    String userRequest = request.getParameter("UserRequest");
    int reviewID=0;
    try {
      reviewID = Integer.parseInt(request.getParameter("ReviewID"));
    }
    catch (Exception e) {
      doGet(request,response);
      return;
    }
    if (userRequest==null) doGet(request,response);
    else if (userRequest.equals("submit"))
      out.println(Page.create(submitReview(student,reviewID,request)));
    else if (userRequest.equals("edit"))
      out.println(res.getString("str_edit"));
  }
  
  String getStatus(String status) {
    if (status == null) return "";

    if (status.equals("Assigned"))
      return res.getString("str_assigned");
    if (status.equals("Abandoned"))
      return res.getString("str_abandoned");
    if (status.equals("Completed"))
      return res.getString("str_completed");
    if (status.equals("Submitted"))
      return res.getString("str_submitted");
    return status;
  }

  String myReviews(Student student) { // main summary page
  /*  There are two sections to this page
      1) Reviews Assigned to Me
      2) Reviews of My Reports  */
    StringBuffer buf = new StringBuffer();
    buf.append("<b>" + student.getFullName() + "</b>");
    Date now = new Date();
    buf.append("<br>" + now);
    buf.append("<h4>" + res.getString("str_my_reviews") + "</h4>");
    String myID = student.getIDNumber();
    String sqlMyReviews = "SELECT * FROM Reviews WHERE (ReviewerID='" + myID + "' AND Status != '') "
    + "ORDER BY ReviewID";
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(sqlMyReviews);
      if (rs.next()) {
        buf.append("<TABLE BORDER=1 CELLSPACING=0>");
        buf.append("<TR><TD><b>" + res.getString("str_rev") + "</b></TD><TD><b>"
        + res.getString("str_rep_title") + "</b></TD><TD><b>" + res.getString("str_status") 
        + "</b></TD><TD></TD><TD></TD></TR>");
        int i = 1;
        do {
          String status = rs.getString("Status");
          if (status==null) status = "";
          if (!status.equals("")) {
            buf.append("<TR><TD>" + i 
            + "</TD><TD><FONT FACE=Arial COLOR=#FF0000><b>" + rs.getString("ReportTitle") 
            + "</b></FONT></TD>"
            + "<TD><FONT SIZE=-1 FACE=Arial><b>" + getStatus(status) + "</b></FONT></TD>");
            if (status.equals("Assigned") || status.equals("Submitted"))
              buf.append("<TD><a href=/" + Course.name + "/content/" 
              + rs.getString("ReportFilename") + ">" + res.getString("str_dl_report") + " </a></TD>"
              + "<TD><a href=" + Course.name + ".PeerReview?UserRequest=edit&ReviewID=" 
              + rs.getString("ReviewID") + ">" + res.getString("str_edit_review") + "</a></TD></TR>");
            else buf.append("<TD></TD><TD></TD></TR>");
          }
          i++;
        } while (rs.next());
        buf.append("</TABLE>");      
      }
      else buf.append("(none)");
    } 
    catch (Exception e) {
      return createReviewTables(student);
    }
    buf.append("<h4>" + res.getString("str_title_my_reports") + "</h4>");
    String sqlMyReports = "SELECT * FROM Reviews WHERE (Author1ID='" + myID + "' OR Author2ID='" + myID
    + "') AND Status='Completed'";
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery(sqlMyReports);
      if (rs.next()) {
        buf.append("<TABLE BORDER=1 CELLSPACING=0>");
        buf.append("<TR><TD><b>" + res.getString("str_rep_title") + "</b></TD><TD></TD></TR>");
        do {
          buf.append("<TR><TD><FONT FACE=Arial COLOR=#FF0000><b>" + rs.getString("ReportTitle") + "</b></FONT></TD>"
          + "<TD><a href=" + Course.name + ".PeerReview?UserRequest=view&ReviewID=" 
          + rs.getString("ReviewID") + ">" + res.getString("str_view") + "</a></TD></TR>");
        } while (rs.next());
        buf.append("</TABLE>");      
      }
      else buf.append("(" + res.getString("str_none") + ")");
    } 
    catch (Exception e) {
      buf.append(e.getMessage());
    }
    if (student.getIsInstructor()) {
      buf.append("<FORM METHOD=GET ACTION=" + Course.name + ".ManagePeerReview><b>" 
      + res.getString("str_teach_only") + "</b><INPUT TYPE=SUBMIT VALUE='"
      + res.getString("str_btn_manage") + "'></FORM>");
    }
    return buf.toString();
  }

  String viewReview(Student student,int reviewID) {
    StringBuffer buf = new StringBuffer();
    String myID = student.getIDNumber();
    String n = "0";  // keeps track of the QuestionID value
    String sqlReview = "SELECT * FROM Reviews WHERE ReviewID='" + reviewID + "'";
    String sqlQuestions = "SELECT * FROM ReviewQuestions ORDER BY SubjectArea,QuestionID";
    boolean authorized = student.getIsInstructor();
    buf.append("<h2>" + res.getString("str_peer_review") + "</h2>");
    try {
      MessageFormat mf = new MessageFormat(res.getString("str_proofread"));
      Object[] args = {
        "<a href=" + Course.name + ".PeerReview?UserRequest=edit&ReviewID=" + reviewID + ">",
        "</a>"
      };
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt1 = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsReview = stmt1.executeQuery(sqlReview);
      ResultSet rsQuestions = stmt2.executeQuery(sqlQuestions);
      if (rsReview.next()) {
        String status = rsReview.getString("Status");
        if ((myID.equals(rsReview.getString("Author1ID")) || myID.equals(rsReview.getString("Author2ID")))
          && status.equals("Completed")) authorized=true;  // authors allowed to view the review now.
        if ((myID.equals(rsReview.getString("ReviewerID")) && status.equals("Submitted"))
          || student.getIsInstructor()) {
          authorized=true;  // review can be edited by reviewer if not completed or instructor anytime.
          buf.append("<FONT COLOR=#FF0000 FACE=Arial><b>" + mf.format(args) + "</b></FONT>");
        }
      }
      else return res.getString("str_no_review");
      if (!authorized) return res.getString("str_not_authed");
      
      buf.append("<h3>" + res.getString("str_title") + " " + rsReview.getString("ReportTitle") + "<br>"
      + res.getString("str_author") + " " + getName(rsReview.getString("Author1ID"),rsReview.getString("Author2ID")) 
      + "</h3><OL>");
      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        n = question.getID();
        buf.append("<li>");        
        buf.append(question.printView(rsReview.getString("Response" + n)));
        buf.append("</li>");
      }
      buf.append("</OL>");
    }
    catch (Exception e) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("ALTER TABLE Reviews ADD Response" + n + " TEXT");
      }
      catch (Exception e2) {
        return buf.toString() + res.getString("str_db_error") + e2.getMessage();
      }
      return viewReview(student,reviewID);
    }
    return buf.toString();
  }
  
  String editReview(Student student,int reviewID) {
    StringBuffer buf = new StringBuffer();
    String myID = student.getIDNumber();
    String n = "0";  // used to keep track of the QuestionID in case a new column has to be added to Reviews table
    String sqlReview = "SELECT * FROM Reviews WHERE ReviewID='" + reviewID + "'";
    if (!student.getIsInstructor()) // student is not instructor; limit access to own reports
      sqlReview += " AND ReviewerID='" + myID + "'";
    String sqlQuestions = "SELECT * FROM ReviewQuestions ORDER BY SubjectArea,QuestionID";
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt1 = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsReview = stmt1.executeQuery(sqlReview);
      ResultSet rsQuestions = stmt2.executeQuery(sqlQuestions);
      if (rsReview.next()) {
        String status = rsReview.getString("Status");
        if (status==null) return res.getString("str_na");
        if (!(status.equals("Assigned") || status.equals("Submitted")) && !student.getIsInstructor())
          return res.getString("str_na");
        buf.append("<h2>" + res.getString("str_peer_review") + "</h2>");
        buf.append("<h3>" + res.getString("str_title") + " " + rsReview.getString("ReportTitle") + "<br>"
        + res.getString("str_author") + " " + getName(rsReview.getString("Author1ID"),rsReview.getString("Author2ID")) + "<br>" 
        + res.getString("str_reviewer") + " " + getName(rsReview.getString("ReviewerID")) + "</h3>"
        + "\n<FORM METHOD=POST ACTION=" + Course.name + ".PeerReview><OL>");
        while (rsQuestions.next()) {
          Question question = new Question();
          question.loadQuestionData(rsQuestions);
          n = question.getID();
          buf.append("\n<li>");
          buf.append(question.printEdit(rsReview.getString("Response" + n)));
          buf.append("</li>");
        }
        buf.append("</OL>\n<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='submit'>"
        + "<INPUT TYPE=HIDDEN NAME='ReviewID' VALUE='" + reviewID + "'>"
        + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_submit") + "'></FORM>");
      }
      else return res.getString("str_no_edit_auth");
    }
    catch (Exception e) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("ALTER TABLE Reviews ADD Response" + n + " TEXT");
      }
      catch (Exception e2) {
        return buf.toString() + res.getString("str_db_error") + e2.getMessage();
      }
      return editReview(student,reviewID);
    }
    return buf.toString();
  }
  
  String submitReview(Student student,int reviewID,HttpServletRequest request) {
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy HH:mm");
    String now = df.format(new Date());
    String sqlUpdate = "UPDATE Reviews Set Status='Submitted',DateSubmitted=now()";
    Enumeration names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String n = (String)names.nextElement();  // Answers are labeled according to the QuestionID
      try {
        Integer.parseInt(n);  // test if the parameter name is a question/response number
        String studentAnswer[] = request.getParameterValues(n);
        String answer = "";
        if (studentAnswer != null) {
          if (studentAnswer.length >= 0)
            for (int i = 0; i < studentAnswer.length; i++)
              answer += studentAnswer[i];
          else answer = CharHider.quot2literal(request.getParameter(n));
          sqlUpdate += ",Response" + n + "='" + CharHider.quot2literal(answer) + "'";
        } 
      }
      catch (Exception e) {   // falls through to here for other parameter names
      }
    }
    sqlUpdate += " WHERE ReviewID=" + reviewID;
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      conn.createStatement().executeUpdate(sqlUpdate);
      return viewReview(student,reviewID);
    }
    catch (Exception e){
      return e.getMessage() + sqlUpdate + viewReview(student,reviewID);    
    }
  }
  
  String getName(String studentIDNumber) {
    // this method gets the full name of the student corresponding to the ID number
    boolean hide=true;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //let's add the option of conforming more w/ FIRPA anonymity criterion, shall we? ;) RDZ
      ResultSet rsAnon = stmt.executeQuery("SELECT HideNames FROM ReviewParameters");
      if (rsAnon.next()) 
        hide = rsAnon.getBoolean("HideNames");
      rsAnon.close(); 
      ResultSet rsStudent = stmt.executeQuery("SELECT * FROM Students WHERE StudentIDNumber='"
      + studentIDNumber + "'");
      if (rsStudent.next()) {
       if (hide)
         return res.getString("str_anon");
       else
         return rsStudent.getString("FirstName") + " " + rsStudent.getString("LastName");
      } else return "";
    }
    catch (Exception e) {
      if (addParamsTable())
        return getName(studentIDNumber);
      return res.getString("str_db_error") + e.getMessage();
    }
  }

  String getName(String student1IDNumber,String student2IDNumber) {
    // this method concatenates two student names from ID Numbers, if applicable
    String name2 = getName(student2IDNumber);
    return getName(student1IDNumber) + (!name2.equals("")?" and ":"") + name2;
  }
    
  String createReviewTables(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Reviews (ReviewID INT PRIMARY KEY AUTO_INCREMENT,"
      + "ReportTitle TEXT,ReportFilename TEXT,Author1ID TEXT,Author2ID TEXT,ReviewerID TEXT,"
      + "Status TEXT,DateAssigned DATETIME,DateSubmitted DATETIME,ScoreID INT,Response1 TEXT,"
      + "Response2 TEXT,Response3 TEXT,Response4 TEXT,Response5 TEXT,Response6 TEXT)");
      stmt.executeUpdate("CREATE TABLE ReviewQuestions (QuestionID INT PRIMARY KEY AUTO_INCREMENT,"
      + "AssignmentNumber INT,SubjectArea INT,QuestionText TEXT,QuestionType TEXT,NumberOfChoices INT,"
      + "ChoiceAText TEXT,ChoiceBText TEXT,ChoiceCText TEXT,ChoiceDText TEXT,ChoiceEText TEXT,"
      + "RequiredPrecision DOUBLE,CorrectAnswer1 TEXT,CorrectAnswer2 TEXT,QuestionTag TEXT,PointValue INT, Section CHAR(3) DEFAULT 'All')");
      stmt.executeUpdate("CREATE TABLE ReviewParameters (HideNames VARCHAR(5))");
      stmt.executeUpdate("INSERT INTO ReviewParameters VALUES('true')");
      return myReviews(student);
    }
    catch (Exception e2) {
      return e2.getMessage();
    }
  }

  boolean addParamsTable() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE ReviewParameters (HideNames VARCHAR(5))");
      stmt.executeUpdate("INSERT INTO ReviewParameters VALUES('true')");
      stmt.close();
      conn.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}

