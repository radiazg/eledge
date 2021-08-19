package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.MessageFormat;

public class Journal extends HttpServlet {
  
  RBStore res = EledgeResources.getJournalBundle();
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
      response.sendRedirect(Course.secureLoginURL + "Journal");
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid

    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Journal",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
    String userRequest = request.getParameter("UserRequest");
    if (userRequest==null) {
      out.println(Page.create(reviewEntry(student,0)));
      return;
    }

    if (userRequest.equals("Review")) {
      try {
        int thisEntry = Integer.parseInt(request.getParameter("ThisEntry"));
        out.println(Page.create(reviewEntry(student,thisEntry)));
      }
      catch (Exception e) {
        out.println(Page.create(reviewEntry(student,0)));
      }
      return;
    }

    String title = request.getParameter("Title");
    if (title==null) title="";
    String entryText = request.getParameter("EntryText");
    if (entryText==null) entryText="";
    boolean preformatted = false;
    try {
      preformatted = request.getParameter("Preformatted").equals("true")?true:false;
    }
    catch (Exception e) {
      preformatted = false;
    }

    if (userRequest.equals("Create")) {
      out.println(Page.create(createEntry(student,title,entryText,preformatted)));
      return;
    }
    if (userRequest.equals("Preview")) {
      out.println(Page.create(previewEntry(student,title,entryText,preformatted)));
      return;
    }
    if (userRequest.equals("Submit")) {
      out.println(Page.create(submitEntry(student,title,entryText,preformatted) 
      + reviewEntry(student,0)));
      return;
    }
  }

  String reviewEntry(Student student,int thisEntry) {
    StringBuffer buf = new StringBuffer("\n<h3>" + res.getString("str_title_review") + "</h3>");
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm a");
    MessageFormat mf = new MessageFormat(res.getString("str_jump_to"));
    Object[] args = {
      null,
      null
    };
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQuery = "SELECT * FROM JournalEntries WHERE StudentIDNumber='" 
      + student.getIDNumber() + "' ORDER BY Date";
      ResultSet rsEntries = stmt.executeQuery(sqlQuery);
      int nEntries=0;
      if (rsEntries.last()) nEntries=rsEntries.getRow();
      if (thisEntry==0) thisEntry=nEntries;  // display the most recent entry
      if (thisEntry < 1) thisEntry=1;        // make sure thisEntry is in bounds
      if (thisEntry > nEntries) thisEntry=nEntries;
      args[0] = "<INPUT SIZE=2 NAME=ThisEntry VALUE='" + thisEntry + "'>";
      args[1] = new Integer(nEntries); 
      buf.append("\n<FORM METHOD=POST ACTION='" + Course.name + ".Journal'>"
      + mf.format(args) + " <INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_go") 
      + "'></FORM>");
        
      buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".Journal'>");
      
      buf.append("<FONT FACE=Arial><b>" + (thisEntry>1?"<a href=" + Course.name 
      + ".Journal?UserRequest=Review&ThisEntry=" + (thisEntry-1) 
      + ">" + res.getString("str_previous") + "</a>":"<FONT COLOR=#BBBBBB>"
      + res.getString("str_previous") + "</FONT>") + "</b></FONT>");
      
      buf.append("&nbsp;&nbsp;<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='Create'>"
      + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_create") + "'>&nbsp;&nbsp;");
      
      buf.append("<FONT FACE=Arial><b>" + (thisEntry<nEntries?"<a href=" + Course.name 
      + ".Journal?UserRequest=Review&ThisEntry=" + (thisEntry+1) 
      + ">" + res.getString("str_next") + "</a>":"<FONT COLOR=#BBBBBB>"
      + res.getString("str_next") + "</FONT>") + "</b></FONT>");
      buf.append("</FORM>");
      
      if(student.getIsInstructor()) 
        buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".ManageJournal'>"
        + "<b>" + res.getString("str_teach_only") + "</b><INPUT TYPE=SUBMIT VALUE='"
        + res.getString("str_btn_review") + "'></FORM><hr>");
        
      if (nEntries>0) {
        for (int i=nEntries-thisEntry;i>0;i--) rsEntries.previous();
        String submitted = df.format(rsEntries.getTimestamp("Date"));
        String title = rsEntries.getString("Title");
        String entryText = rsEntries.getString("Entry");
        boolean preformatted = rsEntries.getBoolean("Preformatted");
        String reviewed = rsEntries.getString("Reviewed");
        if (reviewed==null) reviewed="false";
        String comments = rsEntries.getString("Comments");
        buf.append(showEntry(student,submitted,title,entryText,preformatted,
          reviewed.equals("true")?true:false,comments));
      }
    }
    catch (Exception e) {
      String err = createJournalTable();
      if (err.length()>0) {
        buf.append(e.getMessage());
        buf.append(err);
      }
      else buf.append(reviewEntry(student,0));
    }
    return buf.toString();
  }
  
  String createEntry(Student student, String title, String entryText, boolean preformatted) {
    StringBuffer buf = new StringBuffer("<h3>" + res.getString("str_title_create") +"</h3>");
    buf.append("<b>" + student.getFullName() + "</b><br>");
    buf.append(new Date());
    buf.append("<FORM METHOD=POST ACTION='" + Course.name + ".Journal'>" 
    + res.getString("str_field_title") + "&nbsp;"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest Value=Preview>"
    + "<INPUT SIZE=60 NAME=Title VALUE='" + CharHider.quot2html(title) + "'><br>"
    + res.getString("str_field_entry") + "&nbsp;<br>"
    + "<TEXTAREA ROWS=20 COLS=60 NAME=EntryText WRAP=SOFT>" + entryText + "</TEXTAREA><br>"
    + "<INPUT TYPE=CHECKBOX NAME=Preformatted VALUE=true" 
    + (preformatted||entryText.length()==0?" CHECKED>":">") + res.getString("str_use_pre") + "<br>");
    buf.append("<br><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_preview") + "'></FORM>");
    return buf.toString();
  }
  
  String previewEntry(Student student, String title, String entryText,boolean preformatted) {
    StringBuffer buf = new StringBuffer("\n<h3>" + res.getString("str_title_preview") + "</h3>");
    buf.append(res.getString("str_explain_preview") + "<br>");
    buf.append("\n<FORM METHOD=POST ACTION='" + Course.name + ".Journal'>"
    + "\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='Create'>"
    + "\n<INPUT TYPE=HIDDEN NAME=Title VALUE='" + CharHider.quot2html(title) + "'>"
    + "\n<INPUT TYPE=HIDDEN NAME=EntryText VALUE='" + CharHider.quot2html(entryText) + "'>"
    + "\n<INPUT TYPE=HIDDEN NAME=Preformatted VALUE='" + preformatted + "'>"
    + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_edit") + "'>&nbsp;"
    + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_save") + "' OnClick=this.form.elements.UserRequest.value='Submit'>&nbsp;"
    + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_cancel") + "' OnClick=this.form.elements.UserRequest.value='Review'>"
    + "\n</FORM><HR>");
    buf.append(showEntry(student,new Date().toString(),CharHider.quot2html(title),
      CharHider.quot2html(entryText),preformatted,false,""));
    return buf.toString();
  }

  String submitEntry(Student student, String title, String entryText, boolean preformatted) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("INSERT INTO JournalEntries (StudentIDNumber,Date,Title,Entry,Preformatted) VALUES "
      + "('" + student.getIDNumber() + "',now(),'" + CharHider.quot2literal(title) 
      + "','" + CharHider.quot2literal(entryText) + "','" + preformatted + "')");
      return "";
    }
    catch (Exception e) {
      return e.getMessage();
    }
  }
  
  String showEntry(Student student, String submitted, String title, String entryText, 
  boolean preformatted, boolean reviewed, String comments) {
    StringBuffer buf = new StringBuffer();
    buf.append(student.getFullName() + "<br>" + submitted + "<br>");
    if (preformatted) buf.append("<h3>" + title + "</h3><PRE>" + entryText + "</PRE>");
    else buf.append("<h3>" + title + "</h3>" + "<TABLE><TR><TD>" + entryText + "</TD></TR></TABLE>");
    if (reviewed) {
      buf.append("<hr>" + res.getString("str_note_read") + "<br>");
      if (comments.length()>0) buf.append("<b>" + res.getString("str_field_comments") 
          + "</b><br><PRE>" + comments + "</PRE>");
    }
    return buf.toString();
  }
  
  String createJournalTable() {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE JournalEntries(EntryID INT PRIMARY KEY AUTO_INCREMENT,"
      + "StudentIDNumber VARCHAR(50),Date DATETIME,Title TEXT,Entry TEXT,Preformatted TEXT,"
      + "Reviewed TEXT,Comments TEXT)");
    }
    catch (Exception e) {
      buf.append(e.getMessage());
    }
    return buf.toString();    
  }
}
