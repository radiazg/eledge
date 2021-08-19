package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.MessageFormat;

public class PortfolioWizard extends HttpServlet {

  boolean debugMode = false;
  Logger log = new Logger();
  RBStore res = EledgeResources.getPortfolioWizardBundle();

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
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "PortfolioWizard");
      return;
    }
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, user is assumed to be an authenticated student or instructor

    String userRequest = request.getParameter("UserRequest");
    log.paranoid("UserRequest: " + userRequest,"PortfolioWizard:doPost");
    int id = 0;
    try {
      id = Integer.parseInt(request.getParameter("PortfolioID"));
    }
    catch (Exception e) {
      log.normal("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:doPost");
    }
    if (userRequest==null) {
      out.println(Page.create(startWizard(student)));
      return;
    }
    if (userRequest.equals("EnterData")) {
      out.println(Page.create(enterData(student,request)));
      return;
    }
    if (userRequest.equals("Update")) {
      out.println(Page.create(update(id,student,request)));
      return;
    }
    if (userRequest.equals("OrderArtifacts")) {
      out.println(Page.create(orderArtifacts(student,request)));
      return;
    }
    if (userRequest.equals("EditArtifact")) {
      out.println(Page.create(editArtifactForm(student,request)));
      return;
    }
    if (userRequest.equals("UpdateArtifact")) {
      out.println(Page.create(updateArtifact(student,request)));
      return;
    }
    if (userRequest.equals("DeleteArtifact")) {
      out.println(Page.create(deleteArtifact(student,request)));
      return;
    }
    if (userRequest.equals("ArtifactManager")) {
      out.println(Page.create(artifactManager(student,id)));
      return;
    }
  }
  
  String startWizard(Student student) {
    log.paranoid("Begin method","PortfolioWizard:startWizard");
    StringBuffer buf = new StringBuffer("<h3>" + res.getString("str_pf_wiz") + "</h3>");
    buf.append("<FORM METHOD=POST ACTION='/servlet/" + Course.name + ".PortfolioWizard'>");
    buf.append("\n" + res.getString("str_explain_wiz1") + "<p>" + res.getString("str_pf_types")
    + "<UL>\n<LI><b>" + res.getString("str_home") + "</b><br>" + res.getString("str_explain_home") 
    + "\n<LI><b>" + res.getString("str_collection") + "</b><br>" + res.getString("str_explain_collection") 
    + "\n<LI><b>" + res.getString("str_resume") + "</b><br>" + res.getString("str_explain_resume") 
    + "\n<LI><b>" + res.getString("str_skills_matrix") + "</b><br>" + res.getString("str_explain_skills") 
    + "</UL>");
    buf.append("\n<b>" + res.getString("str_to_create_new") + "</b><br>"
    + res.getString("str_select_type") 
    + " \n<SELECT NAME=Type><OPTION SELECTED VALUE=0>" + res.getString("str_home") + "</OPTION>"
    + "\n<OPTION VALUE=1>" + res.getString("str_collection") + "</OPTION>\n<OPTION VALUE=2>" 
    + res.getString("str_resume") + "</OPTION>"
    + "\n<OPTION VALUE=3>" + res.getString("str_skills_matrix") + "</OPTION></SELECT><br>"
    + "\n" + res.getString("str_explain_name") + " <INPUT TYPE=TEXT SIZE=10 NAME=NewName>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=EnterData><p>");
    buf.append("\n<b>" + res.getString("str_to_edit_existing") + "</b><br>"
    + res.getString("str_select_pf") + " " + selectPortfolio(student) + "<p>");
    buf.append("\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_proceed") + "'></FORM>");
    
    log.paranoid("End Method","PortfolioWizard:startWizard");
    return buf.toString();
  }
  
  String selectPortfolio(Student student) {
    StringBuffer buf = new StringBuffer();
    buf.append("<SELECT NAME=ExistingName>"
    + "<OPTION VALUE=CreateNew SELECTED>&nbsp;</OPTION>");
    log.paranoid("Beginning of try block.","PortfolioWizard:selectPortfolio");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolios = stmt.executeQuery("\nSELECT * FROM Portfolios WHERE OwnerID='"
      + student.getIDNumber() + "' ORDER BY ID DESC");
      while (rsPortfolios.next()) {
	log.paranoid("Appending select option.","PortfolioWizard:selectPortfolio");
        buf.append("\n<OPTION VALUE='" + rsPortfolios.getString("Name") + "'>"
        + rsPortfolios.getString("Name") + "</OPTION>");
      }
      buf.append("</SELECT>");
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:selectPortfolio");
      return "<br>" + res.getString("str_weird_err") + " " + e.getMessage() + "<br>";
    }
    return buf.toString();
  }
  
  String enterData(Student student,HttpServletRequest request){
    log.paranoid("Begin Method","PortfolioWizard:enterData");
    StringBuffer buf = new StringBuffer();
    String name = request.getParameter("ExistingName");
    int type; // portfolio types are Home(0), Collection(1), Resume(2), Skills(3)
    String typeString;
    int id=0;
    try {
      type = Integer.parseInt(request.getParameter("Type"));
    }
    catch (Exception e){
      type = 0; //default type is Home
    }
    log.paranoid("Type: " + Integer.toString(type),"PortfolioWizard:enterData");
    if (name.equals("CreateNew")) { // create a new portfolio
      log.paranoid("Name equalled CreateNew.","PortfolioWizard:enterData");
      name = request.getParameter("NewName");
      if (name.length()==0)
        return new Portfolio().topNavBar(student) 
        + "<FONT COLOR=#FF0000><B>" + res.getString("str_need_name") + "</B></FONT>" 
        + startWizard(student);
    }
    log.paranoid("Portfolio Name: " + name,"PortfolioWizard:enterData");
    log.paranoid("StudentIDNumber: " + student.getIDNumber(),"PortfolioWizard:enterData");
    log.paranoid("Beginning of try block.","PortfolioWizard:enterData");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='"
      + student.getIDNumber() + "' AND Name='" + name + "'");
      boolean exists = rsPortfolio.next(); // if the portfolio exists, edit it
      if (exists) {
	log.paranoid("Portfolio existed.","PortfolioWizard:enterData");
        id = rsPortfolio.getInt("ID");
        typeString = rsPortfolio.getString("Type");
        type = typeString.equals("Home")?0:
               typeString.equals("Collection")?1:
               typeString.equals("Resume")?2:
               typeString.equals("Skills")?3:4; // note: type 4 does not exist 
						// question: then why set it's
						// type as such? isn't it just
						// going to wreak havoc later?
						// If no valid type/comparison
						// is found, shouldn't we set
						// the type to some default 
						// (eg. Home)?
      }
      MessageFormat mf = new MessageFormat(res.getString("str_pf_info"));
      Object[] args = {
        "<i>" + name + "</i>",
        null,
        null,
        null
      };
      buf.append("<h3>" + mf.format(args) + "</h3>");
      if (exists)
        mf.applyPattern(res.getString("str_chosen_edit"));
      else
        mf.applyPattern(res.getString("str_chosen_new"));
      switch (type) {
        case 0: args[1] = "Home"; break;
        case 1: args[1] = "Collection"; break;
        case 2: args[1] = "Resume"; break;
        case 3: args[1] = "Skills"; break;
        default: args[1] = "Home";
      }
      typeString=(String)args[1];
      buf.append(mf.format(args) + "<p>" + res.getString("str_explain_fields") + "<p>");
      mf.applyPattern(res.getString("str_can_use_html"));
      args[0]="<b>";
      args[1]="</b>";
      args[2]="<i>";
      args[3]="</i>";
      buf.append(mf.format(args));

      // This script defines functions to combine or extract the three lines of each address
      // to(from) a single form(database) field.
      buf.append("\n<SCRIPT LANGUAGE=JavaScript><!--"
      + "\n function combine() {"
      + "\n if (document.forms.Info.Address1A.value.length>0) document.forms.Info.Address1.value += document.forms.Info.Address1A.value;"
      + "\n if (document.forms.Info.Address1B.value.length>0) document.forms.Info.Address1.value += '<br>' + document.forms.Info.Address1B.value;"
      + "\n if (document.forms.Info.Address1C.value.length>0) document.forms.Info.Address1.value += '<br>' + document.forms.Info.Address1C.value;"
      + "\n if (document.forms.Info.Address2A.value.length>0) document.forms.Info.Address2.value += document.forms.Info.Address2A.value;"
      + "\n if (document.forms.Info.Address2B.value.length>0) document.forms.Info.Address2.value += '<br>' + document.forms.Info.Address2B.value;"
      + "\n if (document.forms.Info.Address2C.value.length>0) document.forms.Info.Address2.value += '<br>' + document.forms.Info.Address2C.value;"
      + "\n }"
      + "\n --></SCRIPT>");
      
      buf.append("\n\n <FORM NAME=Info METHOD=POST ACTION='" + Course.name + ".PortfolioWizard'>"
      + "\n<INPUT TYPE=HIDDEN NAME=Name VALUE='" + name + "'>"
      + "\n<INPUT TYPE=HIDDEN NAME=OwnerID VALUE='" + student.getIDNumber() + "'>"
      + "\n<INPUT TYPE=HIDDEN NAME=Type VALUE='" + typeString + "'>"
      + "\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='Update'>"
      + "\n<INPUT TYPE=HIDDEN NAME=PortfolioID VALUE='" + (exists?Integer.toString(id):"-1") + "'>");
      
      // Portfolio owner
      buf.append("\n<h4>" + res.getString("str_about_owner") + "</h4>");
      buf.append("\n<TABLE>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_full_name") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=FullName" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("FullName")) + "'>":">") + "</TD></TR>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_email") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=Email" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Email")) + "'>":">") 
      + res.getString("str_email_example") + "</TD></TR>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_homepage_url") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=HomePageURL" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("HomePageURL")) 
      + "'>":">") + res.getString("str_url_example") + "</TD></TR>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_owner_title") 
      + "</TD><TD><INPUT SIZE=20 NAME=Title1" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Title1")) + "'>":">") + "(" + res.getString("str_line1") + ")</TD></TR>");
      buf.append("<TR><TD ALIGN=RIGHT>" + res.getString("str_owner_title")
      + "</TD><TD><INPUT SIZE=20 NAME=Title2" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Title2")) 
      + "'>":">") + "(" + res.getString("str_line2") + ")</TD></TR>");
      
      String[] address1Array = (exists?extract(CharHider.quot2html(rsPortfolio.getString("Address1"))):extract(""));
      String[] address2Array = (exists?extract(CharHider.quot2html(rsPortfolio.getString("Address2"))):extract(""));
      
      buf.append("<TR><TD ALIGN=RIGHT VALIGN=TOP>" + res.getString("str_address") + "</TD><TD>"
      + "<TABLE>\n<TR><TH>" + res.getString("str_work_school") + "</TH><TH>" + res.getString("str_home") + "</TH></TR>"
      + "<TR><TD><INPUT SIZE=20 NAME=Address1A VALUE='" + address1Array[0] + "'></TD><TD><INPUT SIZE=20 NAME=Address2A VALUE='" + address2Array[0] + "'></TD></TR>"
      + "<TR><TD><INPUT SIZE=20 NAME=Address1B VALUE='" + address1Array[1] + "'></TD><TD><INPUT SIZE=20 NAME=Address2B VALUE='" + address2Array[1] + "'></TD></TR>"
      + "<TR><TD><INPUT SIZE=20 NAME=Address1C VALUE='" + address1Array[2] + "'></TD><TD><INPUT SIZE=20 NAME=Address2C VALUE='" + address2Array[2] + "'></TD></TR>"
      + "</TABLE>\n<INPUT TYPE=HIDDEN NAME=Address1 VALUE=''><INPUT TYPE=HIDDEN NAME=Address2 VALUE=''></TD></TR>");
      
      buf.append("<TR><TD ALIGN=RIGHT VALIGN=TOP>" + res.getString("str_tels") + "</TD><TD>"
      + "\n<TABLE><TR><TH>" + res.getString("str_work_school") + "</TH><TH>" + res.getString("str_home") + "</TH></TR>"
      + "\n<TR><TD><INPUT SIZE=20 NAME=Telephone1" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Telephone1")) + "'>":">") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=Telephone2" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Telephone2")) + "'>":">") + "</TD></TR>"
      + "</TABLE></TD></TR>");
      buf.append("</TABLE>");
     
      // Portfolio description
      buf.append("\n<h4>" + res.getString("str_about_pf") + "</h4>");
      buf.append("\n<TABLE>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_pf_title") 
      + "</TD><TD><INPUT SIZE=20 NAME=Title" + 
      (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("Title")) + "'>":">") + "</TD></TR>");
      mf.applyPattern(res.getString("str_write_desc"));
      args[0]="<BR>";
      buf.append("\n<TR><TD ALIGN=RIGHT VALIGN=TOP>" + mf.format(args)
      + "</TD><TD><TEXTAREA NAME=Description ROWS=6 COLS=40 WRAP=SOFT>"
      + (exists?CharHider.quot2html(rsPortfolio.getString("Description")):"") + "</TEXTAREA></TD></TR>");
      
      buf.append("\n<TR><TD ALIGN=RIGHT VALIGN=TOP>" + res.getString("str_img_url") + "</TD>"
      + "<TD><INPUT SIZE=53 NAME=ImgURL" 
      + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("ImgURL")) + "'>":">") 
      + "<br>" + res.getString("str_or") + " " + selectMyFile(student,"Info.ImgURL")); 
      mf.applyPattern(res.getString("str_click_here"));
      args[0]="<a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Uploads' TARGET='UploadsWindow'>";
      args[1]="</a>";
      buf.append(" " + mf.format(args) + "</TD></TR>");
      log.paranoid("Finished buf.append after selectMyFile call.","PortfolioWizard:enterData"); 
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_alt_text") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=ImgAlt" + (exists?" VALUE='" + CharHider.quot2html(rsPortfolio.getString("ImgAlt")) + "'>":">") 
      + "(" + res.getString("str_for_text_browser") + ")</TD></TR>");
      log.paranoid("Finished Aternative text buf.append","PortfolioWizard:enterData");
      buf.append("</TABLE>");
      buf.append("<INPUT TYPE=BUTTON OnClick='combine();Info.submit()' "
      + "VALUE='" + res.getString("str_proceed") + "'>");
      buf.append("</FORM>");
      log.paranoid("End try block.","PortfolioWizard:enterData");
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:enterData");
      return e.getMessage();
    }
    log.paranoid("Ending method.","PortfolioWizard:enterData");
    return buf.toString();
  }

  String[] extract(String combined) { // separates 3 html lines into String[] elements
    log.paranoid("Beginning method.","PortfolioWizard:extract");
    String[] components = {"","",""};
    String separator = "<br>";
    int i = combined.indexOf(separator);
    if (i>=0) {
      components[0] = combined.substring(0,i);
      int j = combined.indexOf(separator,i+separator.length());
      if (j>=0) {
	log.paranoid("Three lines of address.","PortfolioWizard:extract");
        components[1] = combined.substring(i+separator.length(),j);
        components[2] = combined.substring(j+separator.length());
      }
      else {
	log.paranoid("Two lines of address.","PortfolioWizard:extract");
        components[1] = combined.substring(i+separator.length());
      }
    }
    else {
      log.paranoid("One line of address.","PortfolioWizard:extract");
      components[0] = combined;
    }
    log.paranoid("Ending method.","PortfolioWizard:extract");
    return components;
  }
    
  String selectMyFile(Student student,String formName) {
    log.paranoid("Beginning method","PortfolioWizard:selectMyFile");
    StringBuffer buf = new StringBuffer();
    File myDirectory = new File(Course.uploadsDirectory + "/" + student.getIDNumber() + "/");
    String uploadsURL = "/" + Course.name + "/uploads/" + student.getIDNumber() + "/";
    if (!myDirectory.exists()) {
      log.paranoid("Making directory " + myDirectory.getName(),"PortfolioWizard:selectMyFile");
      myDirectory.mkdir();
      log.paranoid("Directory " + myDirectory.getName() + " made.","PortfolioWizard:selectMyFile");
    }
    File[] myFiles = myDirectory.listFiles();
    if (myFiles.length>0) {
      String fileURL="";
      buf.append("<SELECT onChange='" + formName + ".value=this.value'>"
      + "<OPTION VALUE='' SELECTED>" + res.getString("str_select_file") + "</OPTION>");
      for(int i=0;i<myFiles.length;i++) {
        fileURL = uploadsURL + myFiles[i].getName();
        buf.append("<OPTION VALUE='" + fileURL + "'>" + myFiles[i].getName() + "</OPTION>");
      }
      buf.append("</SELECT>");
      log.paranoid("Ending method.(Files existed)","PortfolioWizard:selectMyFile");
      return buf.toString();
    }
    else {
      log.paranoid("Ending method.(Files didn't exist)","PortfolioWizard:selectMyFile");
      return "";
    }
  }  
    
  String randomCode() {
    log.paranoid("Beginning method.","PortfolioWizard:randomCode");
    String choice = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ";
    int size = 8;
    Random rand = new Random();
    StringBuffer code = new StringBuffer();
    for (int i=0;i<size;i++) {
      code.append(choice.charAt(rand.nextInt(choice.length())));
    }
    log.paranoid("Ending method.","PortfolioWizard:randomCode");
    return code.toString();
  }

  String update(int id,Student student,HttpServletRequest request) {
  // this method updates the values of an existing portfolio, or
  // if it doesn't exist, creates a new portfolio
    log.paranoid("Beginning method.","PortfolioWizard:update");
    if (student.getIsVisitor()) return "Sorry, visitor cannot create or update portfolios.";
    log.paranoid("Beginning of try block.","PortfolioWizard:update");
    log.paranoid("(Student: " + student.getIDNumber() + ")","PortfolioWizard:update");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'"
      + (student.getIsInstructor()?"":" AND OwnerID='" + student.getIDNumber() + "'"));
      if (rsPortfolio.first()) {
	log.paranoid("Portfolio exists.","PortfolioWizard:update");
        ResultSetMetaData rsmdPortfolio = rsPortfolio.getMetaData();
        int n=rsmdPortfolio.getColumnCount();
        for (int i=3;i<=n;i++) {
          String colName = rsmdPortfolio.getColumnName(i);
          String colValue = request.getParameter(colName);
          if (colValue!=null) // update only those values for the current form
            rsPortfolio.updateString(colName,colValue);
        }
        rsPortfolio.updateRow();
      }
      else { // portfolio does not exist; create a new one 
	log.paranoid("Portfolio doesn't exist.","PorfolioWizard:update");
        String name = request.getParameter("Name"); if (name==null) name = "";
        String type = request.getParameter("Type"); if (type==null) type = "Home";
        if (name.length()==0) return res.getString("str_enter_unique");
        ResultSetMetaData rsmdTemplate = stmt.executeQuery("SELECT * FROM Portfolios").getMetaData();
        StringBuffer insertSQL1 = new StringBuffer("INSERT INTO Portfolios (OwnerID,Type,Name,Code");
        StringBuffer insertSQL2 = new StringBuffer(" VALUES ('" + student.getIDNumber() + "','"
        + type + "','" + name + "','" + randomCode() + "'");
        for (int i=6;i<=rsmdTemplate.getColumnCount();i++) {
	  log.paranoid("Running rsmdTemplate.getColumnCount for loop.","PortfolioWizard:update");
          insertSQL1.append("," + rsmdTemplate.getColumnName(i));
          insertSQL2.append(",'" + CharHider.quot2literal(request.getParameter(rsmdTemplate.getColumnName(i))) + "'");
        }
        insertSQL1.append(")");
        insertSQL2.append(")");
	log.paranoid("Executing sqlinsert: " + insertSQL1.toString() + insertSQL2.toString(),"PortfolioWizard:update");
        stmt.executeUpdate(insertSQL1.toString() + insertSQL2.toString());
        log.paranoid("sqlinsert executed.","PortfolioWizard:update");
        // the following gets the ID value of the newly created portfolio (to add artifacts in the next step)
        ResultSet rsTemp = stmt.executeQuery("SELECT LAST_INSERT_ID()");
	rsTemp.next();
        id = rsTemp.getInt("LAST_INSERT_ID()");
      }
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught","PortfolioWizard:update");
      return e.getMessage();
    }  
    log.paranoid("Normal termination of method.","PortfolioWizard:update");
    return res.getString("str_pf_saved") + "<br>" + artifactManager(student,id);
  }
  
  String artifactManager(Student student,int id) {
    log.paranoid("Beginning Method","PortfolioWizard:artifactManager");
    MessageFormat mf = new MessageFormat(res.getString("str_step1"));
    Object[] args = {
      "<a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Uploads' TARGET='UploadsWindow'>",
      "</a>"
    };
    StringBuffer buf = new StringBuffer("<H3>" + res.getString("str_pf_artifacts") + "</H3>");
    buf.append(res.getString("str_explain_pf_artifact") + "<OL><LI>" + mf.format(args)
    + "<LI>" + res.getString("str_step2") + "</OL>");
    mf.applyPattern(res.getString("str_upload_graphic"));
    buf.append(mf.format(args));
    
    buf.append("<hr><h4>" + res.getString("str_edit_existing_af") + "</h4>");
    int i = 1; // index used internally for this page (may not match current ArtifactNumber)
    log.paranoid("Beginning of try block.","PortfolioWizard:artifactManager");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsArtifacts = stmt.executeQuery("SELECT * FROM PortfolioArtifacts WHERE "
      + "PortfolioID='" + id + "' ORDER BY ArtifactNumber");
      if (rsArtifacts.isBeforeFirst()) { // list the existing artifacts for this portfolio
	log.paranoid("Listing existing artifacts for portfolio...","PortfolioWizard:artifactManager");
        buf.append(res.getString("str_table_for_order"));
        buf.append("\n<FORM METHOD=POST "
        + "ACTION='/servlet/" + Course.name + ".PortfolioWizard'>");
        buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='OrderArtifacts'>");
        buf.append("\n<INPUT TYPE=HIDDEN NAME=PortfolioID VALUE='" + id + "'>");
        buf.append("\n<TABLE><TR><TH></TH><TH>" + res.getString("str_num") + "</TH><TH>"
        + res.getString("str_title") + "</TH><TH>" + res.getString("str_af_url") + "</TH></TR>");
        while (rsArtifacts.next()) {
          buf.append("\n<TR><TD><INPUT TYPE=RADIO NAME=Index VALUE=" + i + "></TD>"
          + "<TD><INPUT SIZE=2 NAME='" + i + ":ArtifactNumber' VALUE='" 
          + rsArtifacts.getString("ArtifactNumber") + "'></TD>"
          + "<TD>" + rsArtifacts.getString("Title") + "</TD>"
          + "<TD><a HREF='" + rsArtifacts.getString("ArtifactURL") + "'>" + rsArtifacts.getString("ArtifactURL") + "</a></TD>"
          + "</TR>");
          i++;
        }
        buf.append("\n</TABLE>"
        + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_af") + "'> "
        + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_edit_af") 
        + "' onCLick=this.form.elements.UserRequest.value='EditArtifact';submit()> "
        + "\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_del_selected_af") 
        + "' onCLick=this.form.elements.UserRequest.value='DeleteArtifact';submit()></FORM>");
      log.paranoid("Done listing artifacts.","PortfolioWizard:artifactManager");
      }
      else buf.append(res.getString("str_no_af"));
    }
    catch (Exception e) {
      return e.getMessage();
    }
    
    buf.append("<hr><h4>" + res.getString("str_add_new_af") + "</h4>");
    buf.append(editArtifactForm(student,id,i));
    
    buf.append("<hr><FORM METHOD=GET ACTION='/servlet/" + Course.name + ".Portfolio"
    + "'><INPUT TYPE=HIDDEN NAME=ID VALUE='" + id + "'><INPUT TYPE=SUBMIT VALUE='" 
    + res.getString("str_finish_wiz") + "'></FORM>");
    return buf.toString();
  }
  
  String orderArtifacts(Student student,HttpServletRequest request) {
    StringBuffer debug = new StringBuffer();
    int portfolioID = 0;
    log.paranoid("Beginning of try block.","PortfolioWizard:orderArtifacts");
    try {
      portfolioID = Integer.parseInt(request.getParameter("PortfolioID"));
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE "
      + "ID='" + portfolioID + "' AND OwnerID='" + student.getIDNumber() + "'");
      if (!rsPortfolio.next()) return res.getString("str_not_authed");
      ResultSet rsArtifacts = stmt.executeQuery("SELECT * FROM PortfolioArtifacts WHERE "
      + "PortfolioID='" + portfolioID + "' ORDER BY ArtifactNumber");
      int i = 1;
      log.paranoid("Begin while loop.","PortfolioWizard:orderArtifacts");
      while (rsArtifacts.next()) {
        int n = Integer.parseInt(request.getParameter(i + ":ArtifactNumber"));
        debug.append(i + ":" + n);
        rsArtifacts.updateInt("ArtifactNumber",n);
        rsArtifacts.updateRow();
        i++;
      }
      log.paranoid("End while loop.","PortfolioWizard:orderArtifacts");
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:artifactManager");
      debug.append(e.getMessage());
    }
    log.paranoid("Normal termination of method.","PortfolioWizard:artifactManager");
    return res.getString("str_num_updated") + "<br>" + artifactManager(student,portfolioID);
  }

  String editArtifactForm(Student student,HttpServletRequest request) {
    String portfolioID = request.getParameter("PortfolioID");
    int artifactNumber = 0;
    int id = 0;
    log.paranoid("Begin try block.","PortfolioWizard:editArtifactForm(1)");
    try {
      String i = request.getParameter("Index");  // from ArtifactManager page
      artifactNumber = Integer.parseInt(request.getParameter(i + ":ArtifactNumber"));
      id = Integer.parseInt(portfolioID);
    }
    catch (Exception e) {
      log.normal("Unhandled exception " + e.getMessage() + " caught. id=" + Integer.toString(id),"PortfolioWizard:editArtifactForm(1)");
    }
    log.paranoid("Normal termination of method.","PortfolioWizard:editArtifactForm(1)");
    return editArtifactForm(student,id,artifactNumber);
  }
  
  String editArtifactForm(Student student,int portfolioID,int artifactNumber) { //int portfolioID,int artifactNumber) {
    StringBuffer buf = new StringBuffer();
    log.paranoid("Begin try block.","PortfolioWizard:editArtifactForm(2)");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsArtifact = stmt.executeQuery("SELECT * FROM PortfolioArtifacts WHERE "
      + "PortfolioID='" + portfolioID + "' ORDER BY ArtifactNumber");
      int nextArtifactNumber = 1;
      boolean exists = false;
      log.paranoid("Begin while loop.","PortfolioWizard:editArtifactForm(2)");
      while (rsArtifact.next()) {
        nextArtifactNumber = rsArtifact.getInt("ArtifactNumber") + 1;
        if (rsArtifact.getInt("ArtifactNumber")==artifactNumber) {
          exists = true;
          break;
        }
      }
      log.paranoid("End while loop.","PortfolioWizard:editArtifactForm(2)");
      buf.append("\n<FORM NAME=Artifact METHOD=POST><TABLE>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='UpdateArtifact'>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=ArtifactNumber VALUE='" 
      + (exists?artifactNumber:nextArtifactNumber) + "'>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=PortfolioID VALUE='" + portfolioID + "'>");
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_enter_af_title") 
      + "</TD><TD><INPUT SIZE=20 NAME=Title" + 
      (exists?" VALUE='" + CharHider.quot2html(rsArtifact.getString("Title")) + "'>":">") + "</TD></TR>");
      MessageFormat mf = new MessageFormat(res.getString("str_write_af_desc"));
      Object[] args = { 
        "<BR>",
        null
      };
      buf.append("\n<TR><TD ALIGN=RIGHT VALIGN=TOP>" + mf.format(args)
      + "</TD><TD><TEXTAREA NAME=Description ROWS=6 COLS=40 WRAP=SOFT>"
      + (exists?CharHider.quot2html(rsArtifact.getString("Description")):"") + "</TEXTAREA></TD></TR>");
      mf.applyPattern(res.getString("str_enter_af_url"));
      buf.append("\n<TR><TD ALIGN=RIGHT VALIGN=TOP>" + mf.format(args) + "</TD>"
      + "<TD><INPUT SIZE=53 NAME=ArtifactURL" 
      + (exists?" VALUE='" + CharHider.quot2html(rsArtifact.getString("ArtifactURL")) + "'>":">") 
      + "<br>" + res.getString("str_or") + " ");
      mf.applyPattern(res.getString("str_click_here"));
      args[0] = "<a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Uploads' TARGET='UploadsWindow'>";
      args[1] = "</a>";
      buf.append(selectMyFile(student,"Artifact.ArtifactURL") + " " 
      + mf.format(args) + "</TD></TR>");
      buf.append("\n<TR><TD ALIGN=RIGHT VALIGN=TOP>" + res.getString("str_img_url") + "</TD>"
      + "<TD><INPUT SIZE=53 NAME=ImgURL" 
      + (exists?" VALUE='" + CharHider.quot2html(rsArtifact.getString("ImgURL")) + "'>":">") 
      + "<br>" + res.getString("str_or") + " " + selectMyFile(student,"Artifact.ImgURL") 
      + mf.format(args) + "</TD></TR>");      
      buf.append("\n<TR><TD ALIGN=RIGHT>" + res.getString("str_alt_text") + "</TD>"
      + "<TD><INPUT SIZE=20 NAME=ImgAlt" 
      + (exists?" VALUE='" + CharHider.quot2html(rsArtifact.getString("ImgAlt")) + "'>":">") 
      + "(" + res.getString("str_for_text_browser") + ")</TD></TR>");
      buf.append("</TABLE>");

      buf.append("<INPUT TYPE=SUBMIT VALUE='" + (exists?res.getString("str_update_af"):res.getString("str_create_af")) + "'>"
      + "</FORM>");
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:editArtifactForm(2)");
      return e.getMessage();
    }
    log.paranoid("Normal termination of method.","PortfolioWizard:editArtifactForm(2)");
    return buf.toString();
  }
  
  String updateArtifact(Student student,HttpServletRequest request) {
    // this method updates the values of an existing portfolio artifact, or
    // if it doesn't exist, creates a new one
    StringBuffer debug = new StringBuffer();
    String portfolioID = request.getParameter("PortfolioID");
    String artifactNumber = request.getParameter("ArtifactNumber");
    boolean exists = false;
    log.paranoid("Being try block.","PortfolioWizard:updateArtifact");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      // check to see if the student owns this portfolio:
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='"
      + portfolioID + "' AND OwnerID='" + student.getIDNumber() + "'");
      if (!rsPortfolio.next()) return "You are not authorized to add/edit artifacts for this portfolio.";
      ResultSet rsArtifact = stmt.executeQuery("SELECT * FROM PortfolioArtifacts WHERE "
      + "PortfolioID='" + portfolioID + "' AND ArtifactNumber='" + artifactNumber + "'");
      if (rsArtifact.first()) {
	log.paranoid("Exists is true.","PortfolioWizard:updateArtifact");
        exists = true;
        ResultSetMetaData rsmdArtifact = rsArtifact.getMetaData();
        int n=rsmdArtifact.getColumnCount();
        for (int i=4;i<=n;i++) {
          String colName = rsmdArtifact.getColumnName(i);
          String colValue = request.getParameter(colName);
          if (colValue!=null) { // update only those values for the current form
            //debug.append("<br>Updating " + colName + " to: " + colValue);
            rsArtifact.updateString(colName,colValue);
          }  
        }
        rsArtifact.updateRow();
      }
      else { // create a new artifact
	log.paranoid("Creating new artifact.","PortfolioWizard:updateArtifact");
        StringBuffer insertSQL = new StringBuffer("INSERT INTO PortfolioArtifacts "
        + "(PortfolioID,ArtifactNumber,ArtifactURL,Title,Description,ImgURL,ImgAlt) ");
        insertSQL.append("VALUES ('" + portfolioID + "','" + artifactNumber + "','"
        + CharHider.quot2literal(request.getParameter("ArtifactURL")) + "','"
        + CharHider.quot2literal(request.getParameter("Title")) + "','"
        + CharHider.quot2literal(request.getParameter("Description")) + "','"
        + CharHider.quot2literal(request.getParameter("ImgURL")) + "','"
        + CharHider.quot2literal(request.getParameter("ImgAlt")) + "')"); 
        //if (insertSQL.length()>0) return insertSQL.toString();
        stmt.executeUpdate(insertSQL.toString());     
      }
    }
    catch (Exception e) {
      try {
	log.normal("Catching exception " + e.getMessage() + " with database update.","PortfolioWizard:updateArtifact");
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("ALTER TABLE PortfolioArtifacts ADD COLUMN "
        + "ArtifactID INT PRIMARY KEY AUTO_INCREMENT FIRST");
        return res.getString("str_db_updated");
      }
      catch (Exception e2) {
	log.sparse("Unhandled exception " + e2.getMessage() + " caught.","PortfolioWizard:updateArtifact");
        return res.getString("str_weird_err");
      }
    }
    try {  // display the results of the edited portfolio
      log.paranoid("Attempting to parse id","PortfolioWizard:updateArtifact");
      int id = Integer.parseInt(portfolioID);
      log.paranoid("Normal termination of method","PortfolioWizard:updateArtifact");
      return res.getString("str_af_stored") + "<br>" + artifactManager(student,id);
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:updateArtifact");
      return res.getString("str_display_af") + e.getMessage();
    }
  }
   
  String deleteArtifact(Student student, HttpServletRequest request) {
    int id = 0;
    log.paranoid("Beginning try block.","PortfolioWizard:deleteArtifact");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String portfolioID = request.getParameter("PortfolioID");
      String i = request.getParameter("Index");
      String artifactNumber = request.getParameter(i + ":ArtifactNumber");
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE "
      + "ID='" + portfolioID + "' AND OwnerID='" + student.getIDNumber() + "'");
      if (!rsPortfolio.first()) return res.getString("str_cant_delete");
      stmt.executeUpdate("DELETE FROM PortfolioArtifacts WHERE PortfolioID='" + portfolioID
      + "' AND ArtifactNumber='" + artifactNumber + "'");
      id = Integer.parseInt(portfolioID);
    }
    catch (Exception e) {
      log.sparse("Unhandled exception " + e.getMessage() + " caught.","PortfolioWizard:deleteArtifact");
      return e.getMessage();
    }
    log.paranoid("Normal termination of method","PortfolioManager:deleteArtifact");
    return res.getString("str_af_deleted") + "<br>" + artifactManager(student,id);
  }
  
}
