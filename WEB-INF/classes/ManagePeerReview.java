package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Random;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import javax.servlet.http.*;
import javax.servlet.*;
/*****************************************
  database tables used by the ManagePeerReview servlet:
    Reviews: ReviewID,ReportTitle,ReportFileName,Author1ID,Author2ID,ReviewerID,
             Status,DateAssigned,DateSubmitted,Score,Response1,Response2,Response3,...
    ReviewQuestions: (Same structure as QuizQuestions, so that Question class works OK)
    Students: (not modified; used to put names to ID numbers)
******************************************/

public class ManagePeerReview extends HttpServlet {
  Logger log = new Logger();
  RBStore res = EledgeResources.getManagePeerReviewBundle();
  public String getServletInfo() {
    return "This Eledge servlet module allows the instructor to manage student peer reviews.";  
  }

  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin method.","ManagePeerReview:doGet");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManagePeerReview");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManagePeerReview",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_instructor")));
      return;
    }
    // from here on, student is assumed to be the instructor
    log.paranoid("About to print out reviewSummary page.","ManagePeerReview:doGet");
    out.println(Page.create(reviewSummary(false,student)));
    log.paranoid("End Method.","ManagePeerReview:doGet");
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin method.","ManagePeerReview:doPost");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "PeerReview");
      return;
    }
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManagePeerReview",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }
    if (!student.getIsInstructor()) {
      log.normal("Student: " + student.getIDNumber() + " tried to view page.","ManagePeerReview:doPost");
      out.println(Page.create(res.getString("str_must_be_instructor")));
      return;
    }
    // from here on, student is assumed to be the instructor
    String userRequest = request.getParameter("UserRequest");
    log.paranoid("Student: " + student.getIDNumber() + " requested: " + userRequest,"ManagePeerReview:doPost");
    if (userRequest==null) doGet(request,response);
    else if (userRequest.equals("create")) out.println(Page.create(createReview(request,student)));
    else if (userRequest.equals("clone")) out.println(Page.create(cloneReview(request,student)));
    else if (userRequest.equals("delete")) out.println(Page.create(deleteReview(request,student)));
    else if (userRequest.equals("update")) out.println(Page.create(updateSummary(request,student)));
    else if (userRequest.equals("EditQuestions")) out.println(Page.create(editQuestionsForm(0)));
    else if (userRequest.equals("UpdateParams")) out.println(Page.create(updateParams(request,student)));
    else if (userRequest.equals("AddQuestionForm")) out.println(Page.create(addQuestionForm(0)));
    else if (userRequest.equals("AddQuestion")) 
      out.println(Page.create(submitNewQuestion(request) + editQuestionsForm(0)));
    else if (userRequest.equals("DeleteQuestion")) {
      String questionID = request.getParameter("QuestionID");
      if (questionID != null) deleteQuestion(questionID);
      out.println(Page.create(editQuestionsForm(0)));
    }
    else if (userRequest.equals("Edit")) {
      if (questionEditedSuccessfully(request))
        out.println(Page.create(res.getString("str_edit_success") + "<br>" + editQuestionsForm(0)));
    }
    else doGet(request,response);
    log.paranoid("End Method.","ManagePeerReview:doPost");
  }
  
  String createReviewForm(Student student) {
   try {
    Class.forName(Course.jdbcDriver).newInstance();
    Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    Statement stmt = conn.createStatement();
    log.paranoid("Begin method.","createReviewForm");
    StringBuffer buf = new StringBuffer();
    buf.append("<H3>" + res.getString("str_title_review1") + "</H3>"
    + "<FORM METHOD=POST><INPUT TYPE=HIDDEN NAME=UserRequest VALUE=EditQuestions>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_question") + "'></FORM><HR>");
    MessageFormat mf = new MessageFormat(res.getString("str_confirm"));
    Object[] arg = {
	   "' + ChangeForm.ReviewID.value + '"
    };
    buf.append("<h3>" + res.getString("str_title_review2") + "</h3>"
    + res.getString("str_explain1")
    + "<FORM NAME=ChangeForm METHOD=POST><TABLE BORDER=1 CELLSPACING=0>");
    buf.append("<TR><TD>" + res.getString("str_field_id") + "</TD><TD>" 
    + res.getString("str_field_reviewer") + "</TD><TD>"
    + res.getString("str_field_auth1") + "</TD><TD>" 
    + res.getString("str_field_auth2") + "</TD><TD>"
    + res.getString("str_field_title") + "</TD><TD>"
    + res.getString("str_field_filename") +" </TD></TR>\n"
    + "<TR><TD><INPUT SIZE=3 NAME=ReviewID></TD>"
    + "<TD>" + selectStudentForm("ReviewerID","",student,conn) + "</TD>"
    + "<TD>" + selectStudentForm("Author1ID","",student,conn) + "</TD>"
    + "<TD>" + selectStudentForm("Author2ID","",student,conn) + "</TD>"
    + "<TD>" + selectTitleForm(conn) + "</TD>"
    + "<TD><INPUT SIZE=20 NAME=ReportFilename VALUE=" + new Random().nextInt(100000000) + ".pdf></TD></TR>\n"
    + "</TABLE><INPUT TYPE=HIDDEN NAME=UserRequest Value='create'>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_create") +"'>");
    buf.append("&nbsp;<INPUT TYPE=BUTTON VALUE='" 
    + res.getString("str_btn_clone") + "' "
    + "onClick=\"ChangeForm.UserRequest.value='clone'; "
    + "if (ChangeForm.ReviewID.value=='') alert('" + res.getString("str_alert1") + "');"
    + "else ChangeForm.submit();\">");
    buf.append("&nbsp;<INPUT TYPE=BUTTON VALUE='"+ res.getString("str_btn_delete") 
    + "' onClick=\"ChangeForm.UserRequest.value='delete'; "
    + "if (ChangeForm.ReviewID.value=='') alert('" + res.getString("str_alert2") + "');"
    //how the heck to do this one???
    + "else if (confirm('" + mf.format(arg) + "')) "
    + "ChangeForm.submit();\">");    
    buf.append("</FORM>\n");
    buf.append("<HR>");
    log.paranoid("End Method","ManagePeerReview:createReviewForm"); 
    return buf.toString();
   } catch (Exception e) {
     return e.getMessage();
   }
  }
  
  String createReview(HttpServletRequest request,Student student) {
    log.paranoid("Begin method.","ManagePeerReview:createReview");
    String sqlUpdate="";
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("About to execute update...","ManagePeerReview:createReview");
      sqlUpdate = "INSERT INTO Reviews "
      + "(ReviewID,ReviewerID,Author1ID,Author2ID,ReportTitle,ReportFilename,DateAssigned,Status) VALUES ('"
      + request.getParameter("ReviewID") + "','" + request.getParameter("ReviewerID") + "','" 
      + request.getParameter("Author1ID") + "','" + request.getParameter("Author2ID") + "','" 
      + request.getParameter("ReportTitle") + "','" + request.getParameter("ReportFilename") 
      + "',now(),'')";
      log.paranoid("About to execute: " + sqlUpdate,"ManagePeerReview:createReview");
      stmt.executeUpdate(sqlUpdate);
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManagePeerReview:createReview");
      return e.getMessage();
    }
    log.paranoid("End method.","ManagePeerReview:createReview");
    return reviewSummary(false,student);
  }

  String deleteReview(HttpServletRequest request,Student student) {
    log.paranoid("Begin Method.","ManagePeerReview:deleteReview");
    try {
      int reviewID = Integer.parseInt(request.getParameter("ReviewID"));
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Selecting from reviews...","ManagePeerReview:deleteReview");
      ResultSet rsReview = stmt.executeQuery("SELECT * FROM Reviews WHERE ReviewID=" + reviewID);
      if (rsReview.next()) {
        int scoreID = rsReview.getInt("ScoreID");
        if (scoreID > 0) stmt.executeUpdate("DELETE FROM Scores WHERE ScoreID=" + scoreID);
        stmt.executeUpdate("DELETE FROM Reviews WHERE ReviewID=" + reviewID);
      }
    }
    catch (Exception e) {
      log.paranoid("Caught exception: " + e.getMessage(),"ManagePeerReview:deleteReview");
      return res.getString("str_deletefailed")  + e.getMessage() + reviewSummary(false,student);
    }
    log.paranoid("End method.","ManagePeerReview:deleteReview");
    return reviewSummary(false,student);
  }
  
  String cloneReview(HttpServletRequest request,Student student) {
    log.paranoid("Begin Method.","ManagePeerReview:cloneReview");
    try {
      log.paranoid("Attempting to parse out reviewID...'","ManagePeerReview:cloneReview");
      int reviewID = Integer.parseInt(request.getParameter("ReviewID"));
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsReview = stmt.executeQuery("SELECT * FROM Reviews WHERE ReviewID=" + reviewID);
      StringBuffer update = new StringBuffer ("INSERT INTO Reviews "
      + "(ReportTitle,ReportFilename,Author1ID,Author2ID,ReviewerID,DateAssigned,Status) VALUES (");
      if (rsReview.next()) {
        update.append("'" + rsReview.getString("ReportTitle") + "',");
        update.append("'" + rsReview.getString("ReportFilename") + "',");
        update.append("'" + rsReview.getString("Author1ID") + "',");
        update.append("'" + (rsReview.getString("Author2ID").length()==0?"":rsReview.getString("Author2ID")) + "',");
        update.append("'',now(),'')");
      }
      rsReview.close();
      log.paranoid("Executing: " + update,"ManagePeerReview:cloneReview'");
      stmt.executeUpdate(update.toString());
    }
    catch (Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"ManagePeerReview:cloneReview");
      return res.getString("str_clone_failed") + e.getMessage() + reviewSummary(false,student);
    }
    log.paranoid("End method.","ManagePeerReview:cloneReview");
    return reviewSummary(false,student);
  }
  
  String reviewSummary(boolean showAll, Student student) { // main summary page
    log.paranoid("Begin method.","ManagePeerReview:reviewSummary");
    StringBuffer buf = new StringBuffer(reviewParamsForm()); 
    buf.append(createReviewForm(student));
    buf.append("<h3>" + res.getString("str_title_summary") + "</h3>");
    buf.append(new Date());
    String sqlQuery = "SELECT * FROM Reviews LEFT JOIN Scores USING (ScoreID) "
      + (showAll?"":"WHERE (Status!='Completed' AND Status!='Abandoned') ")
      + "ORDER BY ReviewID DESC";
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      if (ta.getPermission("ManagePeerReview").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
          sqlQuery="SELECT Reviews.*, Scores.*, TAAssignments.StudentIDNumber, TAAssignments.Value, "
            + "TA1.Value, TA2.Value from Reviews Left JOIN Scores USING (ScoreID) LEFT OUTER JOIN TAAssignments "
            + "ON ((Reviews.ReviewerID=TAAssignments.Value OR Reviews.ReviewerID='') "
            + "AND (Reviews.Author1ID=TA1.Value OR Reviews.Author1ID='') "
            + "AND (Reviews.Author2ID='' OR Reviews.Author2ID=TA2.Value)) "
            + "INNER JOIN TAAssignments AS TA1 "
            + "USING (StudentIDNumber) INNER JOIN TAAssignments AS TA2 "
            + "USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber='" 
            + student.getIDNumber() + "' " 
            + (showAll?"":"AND Reviews.Status!='Completed' AND Reviews.Status!='Abandoned' ") 
            + "GROUP BY Reviews.ReviewID ORDER BY Reviews.ReviewID";
      }
    }
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("About to execute: " + sqlQuery,"ManagePeerReview:reviewSummary");
      ResultSet rs = stmt.executeQuery(sqlQuery);
      sqlQuery = "";
      buf.append("<FORM METHOD=POST><TABLE BORDER=1 CELLSPACING=0>");
      buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest Value='update'>");
      buf.append("<TR><TD>" + res.getString("str_field_id2") + "</TD><TD>" 
      + res.getString("str_field_reviewer") + "</TD><TD>"
      + res.getString("str_field_auth1") + "</TD><TD>" 
      + res.getString("str_field_auth2") + "</TD><TD>"
      + res.getString("str_field_title") + "</TD><TD>"
      + res.getString("str_field_status") + "</TD><TD>"
      + res.getString("str_field_created") + "</TD><TD>"
      + res.getString("str_field_submitted") + "</TD><TD>"
      + res.getString("str_field_view") + "</TD><TD>"
      + res.getString("str_field_score") + "</TD></TR>\n");
      while (rs.next()) {
	log.paranoid("rs.next() was true... inside while","ManagePeerReview:reviewSummary");
        int i = rs.getInt("ReviewID");
        buf.append("<TR><TD><INPUT TYPE=HIDDEN NAME=" + i + ":ReviewID VALUE=" + i + ">" + i + "</TD>"
        + "<TD><FONT COLOR=#FF0000>" + selectStudentForm(i+":ReviewerID",rs.getString("ReviewerID"),student,conn) + "</FONT></TD>"
        + "<TD>" + selectStudentForm(i+":Author1ID",rs.getString("Author1ID"),student,conn) + "</TD>");
	log.paranoid("About to check for author1id >0 and author2id==0","ManagePeerReview:reviewSummary");
        if (rs.getString("Author1ID").length()>0 && rs.getString("Author2ID").length()==0) {
          log.paranoid("author1id.length()>0 YY auth2id.length()==0...","ManagePeerReview:reviewSummary");
          buf.append("<TD><INPUT TYPE=HIDDEN NAME=" + i + ":Author2ID VALUE=''>&nbsp;</TD>");
        }
        else {
          log.paranoid("Printing out selectStudentForm(" + i + ":Author2ID,rs.getString(Author2ID)","ManagePeerReview:reviewSummary");
          buf.append("<TD>" + selectStudentForm(i+":Author2ID",rs.getString("Author2ID"),student,conn) + "</TD>");
	}
        buf.append("<TD>");  // this cell will contain the report title or a blank text form
        if (rs.getString("ReportTitle").equals("")) //make a blank form
          buf.append("<INPUT SIZE=20 NAME=" + i + ":ReportTitle>");
        else     // write the existing title and create a hyperlink to the file
          buf.append("<a href=/" + Course.name + "/content/" + rs.getString("ReportFilename") 
          + " TARGET=_blank>" + CharHider.quot2html(rs.getString("ReportTitle")) + "</a>"
          + "<INPUT TYPE=HIDDEN NAME=" + i + ":ReportTitle VALUE='" 
          + CharHider.quot2html(rs.getString("ReportTitle")) + "'>");
        buf.append("</TD>");
        buf.append("<TD>" + selectStatusForm(i+":Status",rs.getString("Status")) + "</TD>"
        + "<TD>" + dfDate(rs.getDate("DateAssigned")) + "</TD>"
        + "<TD>" + dfDate(rs.getDate("DateSubmitted")) + "</TD>"
        + "<TD><a href=" + Course.name + ".PeerReview?UserRequest=view&ReviewID=" + i 
        + " TARGET=_blank>view</a></TD><TD>");
        String s = rs.getString("Score");
        buf.append("<INPUT SIZE=3 NAME=" + i + ":Score" 
        + (s==null?">":" VALUE=" + s + ">") + "</TD></TR>\n");
      }
      buf.append("</TABLE><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update") + "'>"
      + "<INPUT TYPE=CHECKBOX NAME=ShowAll VALUE=true>" + res.getString("str_check_showall")
      + "</FORM>");
    } 
    catch (Exception e) {
/* *********************************************************************
      // this section catches exceptions generated by the old (eledge-1.6 and prior) database structure in which 
      // scores were stored in the Reviews table instead of the Scores table
      log.sparse("Caught Exception: " + e.getMessage(),"ManagePeerReview:reviewSummary");
      if (sqlQuery.length()>0) return e.getMessage(); // show error message (probably due to older version of MySQL)
      try {
	log.normal("Trying to alter PeerReview tables...","ManagePeerReview:reviewSummary");
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("ALTER TABLE Reviews ADD ScoreID INT AFTER DateSubmitted");
        ResultSet rsReviews = stmt.executeQuery("SELECT * FROM Reviews");
        while (rsReviews.next() && rsReviews.getString("ReviewerID").length()>0) {
	  log.paranoid("In while loop, trying to insert into scores...","ManagePeerReview:reviewSummary");
          try {
            stmt.executeUpdate("INSERT INTO Scores (StudentIDNumber,Assignment,Score,TestType,Code) "
            + "VALUES ('" + rsReviews.getString("ReviewerID") + "','Review1','"
            + rsReviews.getString("Score") + "','Review','-1'");
            stmt.executeUpdate("UPDATE Reviews SET ScoreID=LAST_INSERT_ID() WHERE ReviewID='" + rsReviews.getInt("ReviewID") + "'");
          }
          catch (Exception e2) {
            log.sparse("Caught exception (e2a): " + e2.getMessage(),"ManagePeerReview:reviewSummary");
          }         
        }
        rsReviews.close();
        stmt.executeUpdate("ALTER TABLE Reviews DROP COLUMN Score");
        return reviewSummary(true, student);
      }
      catch (Exception e2) {
	log.sparse("Caught exception (e2b): " + e2.getMessage(),"ManagePeerReview:reviewSummary");
        return e2.getMessage() + "<p>" + res.getString("str_convert_failed") + "</p>";
      }
**************************************************** */
    }
    log.paranoid("End method","ManagePeerReview:reviewSummary");
    return buf.toString();
  }

  String updateSummary(HttpServletRequest request,Student student) {
    log.paranoid("Begin method.","ManagePeerReview:updateSumary");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);//make this updateable for newer driver...
      TA ta = null;
      String sql = "SELECT * FROM Reviews ORDER BY ReviewerID,ReviewID";
      if (student.getIsTA()) {
        ta = TAS.getTA(student.getIDNumber());
        if (ta.getPermission("ManagePeerReview_update").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
          sql="SELECT Reviews.*, TAAssignments.StudentIDNumber, TAAssignments.Value, "
            + "TA1.Value, TA2.Value from Reviews LEFT OUTER JOIN TAAssignments "
            + "ON ((Reviews.ReviewerID=TAAssignments.Value OR Reviews.ReviewerID='') "
            + "AND (Reviews.Author1ID=TA1.Value OR Reviews.Author1ID='') "
            + "AND (Reviews.Author2ID=TA2.Value OR Reviews.Author2ID='')) "
            + "INNER JOIN TAAssignments AS TA1 "
            + "USING (StudentIDNumber) INNER JOIN TAAssignments AS TA2 "
            + "USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber='" 
            + student.getIDNumber() + "' GROUP BY Reviews.ReviewID ORDER BY Reviews.ReviewID";

        }
      }
      log.paranoid("Executing: " + sql,"ManagePeerReview:updateSummary");
      ResultSet rsReviews = stmt.executeQuery(sql);
      if (!rsReviews.first()) return reviewSummary(false,student);
      int j = 0;
      boolean done=false;
      while (!done) {
        log.paranoid("Beginning of while loop.","ManagePeerReview:updateSummary");
        j = 1;  // counts the reviews for each reviewer
        String reviewerID = rsReviews.getString("ReviewerID");
        log.paranoid("reviewerID: " + reviewerID,"ManagePeerReview:updateSummary");
        while (rsReviews.getString("ReviewerID").equals(reviewerID)) {
          String updateString="UPDATE Reviews SET ";
          String i = rsReviews.getString("ReviewID");
          int k = rsReviews.getInt("ScoreID");
	  log.paranoid("Updating review: " + i,"ManagePeerReview:updateSummary");
          String author1ID=request.getParameter(i+":Author1ID");
          String author2ID=request.getParameter(i+":Author2ID");
          if (ta != null) {//this student is a TA...
            //if reviewerID is "", then, we don't care. ;)
            //otherwise, if it "matches", we don't care. Otherwise,
            //we make it blank. =)
            if (reviewerID!="" && !ta.isAssigned(TA.ASSIGNMENT_STUDENT,reviewerID)) {
              reviewerID="";
            }
            if (author1ID!="" && !ta.isAssigned(TA.ASSIGNMENT_STUDENT,author1ID)) {
              author1ID="";
            }
            if (author2ID!="" && !ta.isAssigned(TA.ASSIGNMENT_STUDENT,author2ID)) {
              author2ID="";
            }
          }
          if (request.getParameter(i + ":ReviewID") != null) {
            updateString+="ReviewerID='" + reviewerID + "',Author1ID='"
              + author1ID + "',Author2ID='" + author2ID + "',ReportTitle='"
              + request.getParameter(i+":ReportTitle") + "',Status='"
              + request.getParameter(i+":Status") + "' WHERE ReviewID='" + i + "'";
            log.paranoid("Executing: " + updateString,"ManagePeerReview:updateSummary");
            stmt.executeUpdate(updateString);
            //following can no longer be used; the TA pulls out a 
            //result set across multiple joins, which disallows
            //concur-updatability. ;) RDZ 8/19/03

/*            rsReviews.updateString("ReviewerID",reviewerID);
            rsReviews.updateString("Author1ID",author1ID);
            rsReviews.updateString("Author2ID",author2ID);
            rsReviews.updateString("ReportTitle",request.getParameter(i+":ReportTitle"));
            rsReviews.updateString("Status",request.getParameter(i+":Status"));
            rsReviews.updateRow();
*/
          }
          if (!rsReviews.getString("ReviewerID").equals("") && k==0) { // no record in Scores table for this review
            log.paranoid("Inserting score for " + i,"ManagePeerReview:updateSummary");
            stmt.executeUpdate("INSERT INTO Scores (StudentIDNumber,Assignment,Score,IPAddress,TestType,Code) "
            + "VALUES ('" + rsReviews.getString("ReviewerID") + "','Review" + j + "','"
            + request.getParameter(i+":Score") + "','" + request.getRemoteAddr() + "','Review','-1')");
            stmt.executeUpdate("UPDATE Reviews SET ScoreID=LAST_INSERT_ID() WHERE ReviewID='" + i + "'");         
          }
          else if (request.getParameter(i + ":ReviewID") != null) {
            log.paranoid("Updating scores for " + i,"ManagePeerReview:updateSummary");
            stmt.executeUpdate("UPDATE Scores Set Assignment='Review" + j + "',Score='"
            + request.getParameter(i+":Score") + "' WHERE ScoreID='" + k + "'");
          }
          j++;
         if (rsReviews.isLast()) {
            log.paranoid("We're done! Last review was: " + i,"ManagePeerReview:updateSummary");
            done=true;
            break;
          }
          rsReviews.next();
        }
      }
    }
    catch (Exception e) {
      log.sparse("Caught exception:" + e.getMessage(),"ManagePeerReview:updateSummary");
    }
    log.paranoid("End method","ManagePeerReview:updateSummary");
    return reviewSummary((request.getParameter("ShowAll")!=null?true:false),student);
  }

    
  String selectStatusForm(String formName,String status) {
    log.paranoid("Begin method.","ManagePeerReview:selectStatusForm");
    StringBuffer form = new StringBuffer();
    if (status==null) status="";
    return "<SELECT NAME=" + formName + "><OPTION>"
    + "<OPTION VALUE=Assigned" + (status.equals("Assigned")?" SELECTED>":">") 
    + res.getString("str_select_assigned")
    + "<OPTION VALUE=Submitted" + (status.equals("Submitted")?" SELECTED>":">") 
    + res.getString("str_select_submitted")
    + "<OPTION VALUE=Completed" + (status.equals("Completed")?" SELECTED>":">") 
    + res.getString("str_select_completed")
    + "<OPTION VALUE=Abandoned" + (status.equals("Abandoned")?" SELECTED>":">") 
    + res.getString("str_select_abandoned")
    + "</SELECT>";
  }
  
  String selectStudentForm(String formName,String id,Student student, Connection conn) {
    log.paranoid("Begin method.","ManagePeerReview:selectStudentForm");
    try {
      Statement stmt = conn.createStatement();
      //give the query a default value...
      String query="SELECT StudentIDNumber,LastName,FirstName FROM Students ORDER BY Status, LastName, FirstName";
;
      if (id.length()==0) {  // this field not assigned; get everyone's names
        if (student.getIsTA()) {
          TA ta = TAS.getTA(student.getIDNumber());
          if (ta.getPermission("ManagePeerReview").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
            query="SELECT Students.StudentIDNumber, Students.LastName, Students.FirstName, "
              + "TAAssignments.Value FROM Students LEFT JOIN TAAssignments ON "
              + "(TAAssignments.Value=Students.StudentIDNumber) WHERE TAAssignments.StudentIDNumber='"
              + student.getIDNumber() + "' ORDER BY Students.LastName,Students.FirstName";
          }
        } 
      } else                 // this field is assigned; get one name
        query = "SELECT LastName,FirstName FROM Students WHERE StudentIDNumber='" + id + "'"; 
      log.paranoid("About to execute: " + query,"ManagePeerReview:selectStudentForm");
      ResultSet rsStudents = stmt.executeQuery(query);
      StringBuffer form = new StringBuffer();
      if (id.length()==0) {  // create a select box for assigning this field
	log.paranoid("id.length==0...","ManagePeerReview:selectStudentForm");
        form.append("<SELECT NAME=" + formName + "><OPTION>");
        while (rsStudents.next()) {
          form.append("<OPTION VALUE=" + rsStudents.getString("StudentIDNumber") 
          + (rsStudents.getString("StudentIDNumber").equals(id)?" SELECTED>":">")
          + rsStudents.getString("LastName") + ", " + rsStudents.getString("FirstName") + "</OPTION>");
        }
        form.append("</SELECT>");
      }
      else { // just write the name with a hidden field identifier
        rsStudents.next();  // go to the first record
        form.append("<INPUT TYPE=HIDDEN NAME=" + formName + " VALUE=" + id + ">" 
        + "<b>" + rsStudents.getString("LastName") + ", " + rsStudents.getString("FirstName") + "</b>");
      }
      log.paranoid("End method","ManagePeerReview:selectStudentForm");
      rsStudents.close();
      stmt.close();
      return form.toString();
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManagePeerReview:selectStudentForm");
      return "&nbsp;";
    }
  }

  String selectTitleForm(Connection conn) {
    log.paranoid("Begin method.","ManagePeerReview:selectTitleForm");
    StringBuffer buf = new StringBuffer("\n<SELECT NAME=ReportTitle><OPTION VALUE=''>Create a new title");
    try {
      Statement stmt = conn.createStatement();
      ResultSet rsTitles = stmt.executeQuery("SELECT ReportTitle FROM Reviews GROUP BY ReportTitle");
      while (rsTitles.next()) {
        String title = rsTitles.getString("ReportTitle");
        buf.append("\n<OPTION VALUE='" + title + "'>" + title);
      }
      rsTitles.close();
      stmt.close();
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(), "ManagePeerReview:selectTitleForm");
    }
    buf.append("\n</SELECT>");
    log.paranoid("End Method","ManagePeerReview:selectTitleForm");
    return buf.toString();
  }
      
  String dfDate(Date d) {
    if (d==null) return "&nbsp;";
    else return new SimpleDateFormat("MM-dd-yy").format(d);
  }
    
  String editQuestionsForm(int assignmentNumber){
    log.paranoid("Begin method.","ManagePeerReview:editQuestionsForm");
    JSmethods jsm = new JSmethods(JSmethods.REVIEW);
    StringBuffer buf = new StringBuffer();
    jsm.appendJSCheckHtml(buf);
    buf.append("<h3>" + res.getString("str_title_prf") + "</h3>");
    buf.append("<TABLE BORDER=0 CELLSPACING=0><TR>"
    + "<TD><FORM METHOD=POST><input type=hidden name=UserRequest value='AddQuestionForm'>"
    + "<input type=hidden name=AssignmentNumber value=0>"
    + "<input type=submit value='" + res.getString("str_btn_addq") + "'></FORM></TD>"
    + "<TD><FORM METHOD=POST><input type=submit value='" 
    + res.getString("str_btn_return") + "'></FORM></TD>"
    + "</TR></TABLE>");

    buf.append("<TABLE BORDER=1 CELLSPACING=0>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM ReviewQuestions ORDER BY SubjectArea,QuestionID";
      ResultSet rsQuestions = stmt.executeQuery(sqlQueryString);

      while (rsQuestions.next()) {
        Question question = new Question();
        question.loadQuestionData(rsQuestions);
        buf.append("\n<TR>");
        buf.append("\n<td><FORM>" + question.print() + "</FORM></td>");
        buf.append("\n<td><FORM METHOD=POST>" + question.edit() + "</FORM></td>");
        buf.append("\n</TR>");
      }
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManagePeerReview:editQuestionsForm");
      return e.getMessage();
    }
    buf.append("\n</TABLE>");
    buf.append("</body></html>");
    log.paranoid("End method.","ManagePeerReview:editQuestionsForm");
    return buf.toString();
  }

  String addQuestionForm(int assignmentNumber) {
    log.paranoid("Begin method","ManagePeerReview:addQuestionForm");
    StringBuffer buf = new StringBuffer();
    JSmethods jsm = new JSmethods(JSmethods.REVIEW);
    jsm.appendJSCheckHtml(buf);
    buf.append("<FORM METHOD=POST><input type=hidden name=UserRequest value=EditQuestions>"
    + "<input type=hidden name=AssignmentNumber value=" + assignmentNumber + ">"
    + "<input type=submit value='" + res.getString("str_btn_return2") + "'></FORM>");

    buf.append("<H2>" + res.getString("str_title_addq") + "</h2>"
    + res.getString("str_explain_addq1") + "<br>"
    + res.getString("str_explain_addq2") + "<br>"
    + "<TABLE>"
    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='MULTIPLE_CHOICE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><H3>" + res.getString("str_title_mc") + "</H3><TABLE BGCOLOR=66FFFF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview")
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea")
    + "<input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_field_select_best") + "</FONT></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='a'>A:</td>"
    + "<td><input type=text size=50 name='ChoiceAText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='b'>B:</td>"
    + "<td><input type=text size=50 name='ChoiceBText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='c'>C:</td>"
    + "<td><input type=text size=50 name='ChoiceCText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='d'>D:</td>"
    + "<td><input type=text size=50 name='ChoiceDText'></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='e'>E:</td>"
    + "<td><input type=text size=50 name='ChoiceEText'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='"
    + res.getString("str_btn_submit_mc") + "' "
    + "onCLick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='TRUE_FALSE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_title_tf") + "</H3><TABLE BGCOLOR=FF66FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview")
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea") + "<input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_field_select_tf")
    + "</FONT></td></tr>"
    + "<tr><td><input type=radio name='Answer' value='true'></td><td>"
    + res.getString("str_true") + "</td></tr>"
    + "<tr><td><input type=radio name='Answer' value='false'></td><td>"
    + res.getString("str_false") + "</td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" + res.getString("str_btn_submit_tf") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr> <FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='SELECT_MULTIPLE'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_title_sm") + "</H3><TABLE BGCOLOR=FFFF66>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview") 
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea")
    + "<input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_select_sm") + "</FONT></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='a'>A:</td>"
    + "<td><input type=text size=50 name='ChoiceAText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='b'>B:</td>"
    + "<td><input type=text size=50 name='ChoiceBText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='c'>C:</td>"
    + "<td><input type=text size=50 name='ChoiceCText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='d'>D:</td>"
    + "<td><input type=text size=50 name='ChoiceDText'></td></tr>"
    + "<tr><td><input type=checkbox name='Answer' value='e'>E:</td>"
    + "<td><input type=text size=50 name='ChoiceEText'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='"
    + res.getString("str_btn_submit_sm") 
    + "' onClick=\"if (parse_for_error(this.form.elements.ChoiceAText.value) && parse_for_error(this.form.elements.ChoiceBText.value) && parse_for_error(this.form.elements.ChoiceCText.value) && parse_for_error(this.form.elements.ChoiceDText.value) && parse_for_error(this.form.elements.ChoiceEText.value) && parse_for_error(this.form.elements.Question.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='FILL_IN_WORD'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_title_fiw") + "</H3><TABLE BGCOLOR=9999FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview") 
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea") + "<input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_fiw") + "</FONT></td></tr>"
    + "<tr><td>" + res.getString("str_field_answer")
    + "</td><td><input type=text size=23 name='CorrectAnswer1'>"
    + res.getString("str_field_or") + "<input type=text size=23 name='CorrectAnswer2'></td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='QuestionTag' ROWS=3 COLS=47 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" 
    + res.getString("str_btn_submit_fiw") 
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value) && parse_for_error(this.form.elements.CorrectAnswer1.value) && parse_for_error(this.form.elements.CorrectAnswer2.value) && parse_for_error(this.form.elements.QuestionTag.value)){this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='NUMERIC'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_title_num") + "</H3><TABLE BGCOLOR=FF9999>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview")
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea") 
    + " <input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_question")
    + "<TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT>"
    + "</TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_num")
    + "</FONT></td></tr>" + "<tr><td>" + res.getString("str_field_answer") 
    + "<input type=text size=20 name='Answer'></td>"
    + "<td>" + res.getString("str_field_units") 
    + "<input type=text size=15 name='Units'></td></tr>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_prec")
    + "<input type=text size=6 name='RequiredPrecision' value='0.5'></td></tr>"
    + "<tr><td COLSPAN=2><input type=button value='" 
    + res.getString("str_btn_submit_num")
    + "' onClick=\"if (parse_for_error(this.form.elements.Question.value)) {this.form.submit();}\"></td></tr>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "<tr><FORM METHOD=POST><INPUT type=hidden name='QuestionType' value='ESSAY'>"
    + "<input type=hidden name=UserRequest value=AddQuestion>"
    + "<td><HR><H3>" + res.getString("str_title_essay") + "</H3><TABLE BGCOLOR=9999FF>"
    + "<tr><td COLSPAN=2>" + res.getString("str_field_peerreview") 
    + "<input type=hidden name='AssignmentNumber' value='" + assignmentNumber + "'>"
    + "&nbsp;" + res.getString("str_field_subjectarea") 
    + "<input type=text size=2 name='SubjectArea' value=0>"
    + "&nbsp;" + displaySectionInfo() + "</td></tr>"
    + "<tr><td>" + res.getString("str_field_question") + "</td>"
    + "<td><TEXTAREA NAME='Question' ROWS=3 COLS=48 WRAP=SOFT></TEXTAREA></td></tr>"
    + "<tr><td COLSPAN=2><FONT SIZE=-2>" + res.getString("str_essay")
    + "</FONT></td></tr><tr><td>" + res.getString("str_field_answer")
    + "</td><td><TEXTAREA NAME=Answer WRAP=SOFT ROWS=6 COLS=47></TEXTAREA>"
    + "<tr><td COLSPAN=2><input type=button value='" 
    + res.getString("str_btn_submit_essay") + "' onClick=\"if (parse_for_error(this.form.elements.Question.value) && parse_for_error(this.form.elements.Answer.value)){this.form.submit();}\"></td></tr>"
    + "</TD></TR>"
    + "</TABLE>"
    + "</td></FORM></tr>"

    + "</TABLE>");
    log.paranoid("End method","ManagePeerReview:addQuestionForm");
    return buf.toString();
  }

  String submitNewQuestion(HttpServletRequest request) {
    log.paranoid("Begin method.","ManagePeerReview:submitNewQuestion");
    String sqlInsertString = "INSERT INTO ReviewQuestions ";
    // get all of the relevant parameters via the POST method:
    String assignmentNumber = request.getParameter("AssignmentNumber");
    String subjectArea = request.getParameter("SubjectArea");
    String section = request.getParameter("Section");
    int pointValue = 1;
    String questionType = request.getParameter("QuestionType");
    String question = CharHider.quot2literal(request.getParameter("Question"));
    int numberOfChoices = 5;
    
    if (questionType.equals("MULTIPLE_CHOICE")) {
        sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "MULTIPLE_CHOICE" + "',";
      sqlInsertString += numberOfChoices + ",'";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceAText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceBText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceCText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceDText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceEText")) + "','";
      sqlInsertString += request.getParameter("Answer") + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else if (questionType.equals("TRUE_FALSE")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,CorrectAnswer1,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "TRUE_FALSE" + "',";
      sqlInsertString += "2" + ",'";
      sqlInsertString += "True" + "','";
      sqlInsertString += "False" + "','";
      sqlInsertString += request.getParameter("Answer") + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else if (questionType.equals("SELECT_MULTIPLE")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,NumberOfChoices,ChoiceAText,ChoiceBText,ChoiceCText,ChoiceDText,ChoiceEText,CorrectAnswer1,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "SELECT_MULTIPLE" + "',";
      sqlInsertString += numberOfChoices + ",'";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceAText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceBText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceCText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceDText")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("ChoiceEText")) + "','";
      String correctAnswers = "";
      if (request.getParameterValues("Answer") != null) {
        String correctAnswer[] = request.getParameterValues("Answer");
        for (int i = 1; i < correctAnswer.length; i++)   // concatenate correct answers into one string
          correctAnswers += correctAnswer[i];
      }
      sqlInsertString += correctAnswers + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else if (questionType.equals("FILL_IN_WORD")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,CorrectAnswer1,CorrectAnswer2,QuestionTag,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "FILL_IN_WORD" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      // the following line inserts NULL into the database if 2nd answer doesn't exist, so that blank answer is not counted as correct:
      sqlInsertString += request.getParameter("CorrectAnswer2").equals("")?"NULL,'":"'" + CharHider.quot2literal(request.getParameter("CorrectAnswer2")) + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("QuestionTag")) + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else if (questionType.equals("NUMERIC")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,CorrectAnswer1,QuestionTag,RequiredPrecision,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "NUMERIC" + "','";
      sqlInsertString += request.getParameter("Answer") + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("Units")) + "','";
      sqlInsertString += request.getParameter("RequiredPrecision") + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else if (questionType.equals("ESSAY")) {
      sqlInsertString += "(AssignmentNumber,SubjectArea,QuestionText,QuestionType,CorrectAnswer1,PointValue,Section) ";
      sqlInsertString += "VALUES (" + assignmentNumber + ",'";
      sqlInsertString += subjectArea + "','";
      sqlInsertString += question + "','";
      sqlInsertString += "ESSAY" + "','";
      sqlInsertString += CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "','";
      sqlInsertString += pointValue + "','" + section + "')";
    }
    else return res.getString("str_unkown_qt");
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("About to execute: " + sqlInsertString,"ManagePeerReview:submitNewQuestion");
      if (stmt.executeUpdate(sqlInsertString) == 1)
        return res.getString("str_q_added") + "<br>";
    }
    catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManagePeerReview:submitNewQuestion");
      return e.getMessage() + "<br>";
    }
    log.paranoid("End method.","ManagePeerReview:submitNewQuestion");
    return "Done.";
  }

    void deleteQuestion(String questionID) {
    log.paranoid("Begin method.","ManagePeerReview:deleteQuestion");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM ReviewQuestions WHERE QuestionID='" + questionID + "'");
    }
    catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"ManagePeerReview:deleteQuestion");
    }
    log.paranoid("End method.","ManagePeerReview:deleteQuestion");
  }

  boolean questionEditedSuccessfully(HttpServletRequest request) {
    log.paranoid("Begin Method.","ManagePeerReview:questionEditedSuccessfully.");
    String sqlUpdateString = "UPDATE ReviewQuestions SET ";
    // get all of the relevant parameters via the POST method:
    String questionType = request.getParameter("QuestionType");

    if (questionType.equals("MULTIPLE_CHOICE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices='" + request.getParameter("NumberOfChoices") + "',";
      sqlUpdateString += "ChoiceAText='" + CharHider.quot2literal(request.getParameter("ChoiceAText")) + "',";
      sqlUpdateString += "ChoiceBText='" + CharHider.quot2literal(request.getParameter("ChoiceBText")) + "',";
      sqlUpdateString += "ChoiceCText='" + CharHider.quot2literal(request.getParameter("ChoiceCText")) + "',";
      sqlUpdateString += "ChoiceDText='" + CharHider.quot2literal(request.getParameter("ChoiceDText")) + "',";
      sqlUpdateString += "ChoiceEText='" + CharHider.quot2literal(request.getParameter("ChoiceEText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + request.getParameter("CorrectAnswer1") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("TRUE_FALSE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices=2,";
      sqlUpdateString += "ChoiceAText='true',";
      sqlUpdateString += "ChoiceBText='false',";
      sqlUpdateString += "CorrectAnswer1='" + request.getParameter("CorrectAnswer1") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("SELECT_MULTIPLE")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "NumberOfChoices='" + request.getParameter("NumberOfChoices") + "',";
      sqlUpdateString += "ChoiceAText='" + CharHider.quot2literal(request.getParameter("ChoiceAText")) + "',";
      sqlUpdateString += "ChoiceBText='" + CharHider.quot2literal(request.getParameter("ChoiceBText")) + "',";
      sqlUpdateString += "ChoiceCText='" + CharHider.quot2literal(request.getParameter("ChoiceCText")) + "',";
      sqlUpdateString += "ChoiceDText='" + CharHider.quot2literal(request.getParameter("ChoiceDText")) + "',";
      sqlUpdateString += "ChoiceEText='" + CharHider.quot2literal(request.getParameter("ChoiceEText")) + "',";
      String correctAnswer[] = request.getParameterValues("CorrectAnswer1");
      if (correctAnswer != null)
        for (int i = 1; i < correctAnswer.length; i++)   // concatenate correct answers into one string
          correctAnswer[0] += correctAnswer[i];
      sqlUpdateString += "CorrectAnswer1='" + correctAnswer[0] + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("FILL_IN_WORD")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "CorrectAnswer2=" // inserts NULL if 2nd answer is blank
      + (request.getParameter("CorrectAnswer2").equals("") ? "NULL," : ("'" + CharHider.quot2literal(request.getParameter("CorrectAnswer2")) + "',"));
      sqlUpdateString += "QuestionTag='" + CharHider.quot2literal(request.getParameter("QuestionTag")) + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("NUMERIC")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "QuestionTag='" + CharHider.quot2literal(request.getParameter("QuestionTag")) + "',"; // to specify units of answer
      sqlUpdateString += "RequiredPrecision='" + request.getParameter("RequiredPrecision") + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else if (questionType.equals("ESSAY")) {
      sqlUpdateString += "AssignmentNumber='" + request.getParameter("AssignmentNumber") + "',";
      sqlUpdateString += "SubjectArea='" + request.getParameter("SubjectArea") + "',";
      sqlUpdateString += "QuestionText='" + CharHider.quot2literal(request.getParameter("QuestionText")) + "',";
      sqlUpdateString += "CorrectAnswer1='" + CharHider.quot2literal(request.getParameter("CorrectAnswer1")) + "',";
      sqlUpdateString += "PointValue=" + request.getParameter("PointValue");
      sqlUpdateString += " WHERE QuestionID=" + request.getParameter("QuestionID");
    }

    else return false;

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("About to execute: " + sqlUpdateString,"ManagePeerREview:questionEditedSuccessfully.");
      stmt.executeUpdate(sqlUpdateString);
    } 
    catch(Exception e) {
      log.sparse("Caught exception:" + e.getMessage(),"ManagePeerReview:questionEditedSuccessfully.");
      return false;
    }
    log.paranoid("End method.","ManagePeerReview:questionEditedSuccessfullyl.");
    return true;
  }

  String reviewParamsForm() {
    StringBuffer buf = new StringBuffer("<h3>" + res.getString("str_title_params") + "</h3>");
     try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT * FROM ReviewParameters");
      if (!rs.next()) return "";
      buf.append("<FORM METHOD=POST><INPUT TYPE=CHECKBOX NAME='HideNames' VALUE='true'"
        + (rs.getBoolean("HideNames")?" CHECKED>":">") + res.getString("str_explain_hide")
        + "<br><INPUT TYPE=HIDDEN NAME=UserRequest VALUE='UpdateParams'>"
        + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_params") + "'></FORM><hr>");
      stmt.close();
      conn.close();
     } catch (Exception e) {
       if (createParamsTable()) {
         return reviewParamsForm();
       } else
         return res.getString("str_db_error") + "&nbsp;" + e.getMessage();
     }
     return buf.toString();
  }

  boolean createParamsTable() {
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

  String updateParams(HttpServletRequest request, Student student) {
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("UPDATE ReviewParameters SET HideNames='"
        + (request.getParameter("HideNames")==null?"false":"true") + "'");
      stmt.close();
      conn.close();
    } catch (Exception e) {
      if (createParamsTable())
        return updateParams(request, student);
      buf.append(e.getMessage());
    }
    return buf.toString() + reviewSummary(false, student);
  }

  String displaySectionInfo() {
    StringBuffer buf = new StringBuffer(res.getString("str_course_section").concat(": <SELECT NAME='Section'><OPTION>" + res.getString("str_all")));
    log.paranoid("Beginning try block.","ManageExam:displaySectionInfo");
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
        return "<INPUT TYPE=HIDDEN Name=Section value='All'>";
      int nSections=rsSections.getInt("Value");
      //sections start with 1, so, so does i.
      for (int i=1; i<=nSections; i++){
        buf.append("<OPTION>"+i);
      }
      buf.append("</SELECT>");
    }catch(Exception e){
      log.normal("Caught exception " + e.getMessage(),"ManageExam:editQuestionsForm");
      return "<INPUT TYPE=HIDDEN NAME=Section value='All'>";
    }
    return buf.toString();
  }
}

