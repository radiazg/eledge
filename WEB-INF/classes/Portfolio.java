package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.MessageFormat;

public class Portfolio extends HttpServlet {
  boolean debugMode = false;
  Hashtable table=new Hashtable();
  Logger log = new Logger();  
  int id;
  String name;
  String ownerID;
  String type;
  String title;
  String description;
  String imgURL;
  String imgAlt;
  Vector artifacts = new Vector();
  // the following are associated with specific portfolio types:
  String fullName;
  String title1;
  String title2;
  String homePageURL;
  String email;
  String address1;
  String address2;
  String telephone1;
  String telephone2;
  RBStore res = EledgeResources.getPortfolioBundle(); 
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

    String userRequest = request.getParameter("UserRequest");
    String code = (request.getParameter("Code")==null?"":request.getParameter("Code"));
    int id = -1;
    try {
      id = Integer.parseInt(request.getParameter("ID"));
    }
    catch (Exception e) {
      log.paranoid("Caught an exception parsing the integer from 'ID'","Portfolio:doGet");
    }
    if (!student.isAuthenticated() && userRequest==null && id>0) { // visitor request to view a portfolio
      if (okToView(id,code,student)) 
        out.println(Page.create(new Portfolio().display(id),student));
      else out.println(Page.create(res.getString("str_cannot_view_pf"),student));
      return;
    }
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Portfolio");
      return;
    }
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen"),student));
      return;
    }
    // from here on, user is assumed to be an authenticated student or instructor
    if (userRequest==null) { // student request to display a portfolio
      log.paranoid("UserRequest was null.","Portfolio:doGet");
      if (id>=0) { // wants a specific portfolio
        log.paranoid("ID was greater than 0","Portfolio:doGet");
        if (okToView(id,code,student)) 
          out.println(Page.create(topNavBar(student) + new Portfolio().display(id),student));
        else out.println(Page.create(res.getString("str_noview_pf"),student));
      }
      else { //default behavior for authenticated student
        id = getID(student.getIDNumber(),"Home");
        if (id==0 && !student.getIsVisitor()) // first visit; create this student's home portfolio
          out.println(Page.create(create(student,"Home","Home",0,""),student));
        else // go to this student's home portfolio
          out.println(Page.create(topNavBar(student) + new Portfolio().display(id),student));
      }
      return;
    }   
    if (userRequest.equals("Manager")) { // manage my portfolios
      out.println(Page.create(topNavBar(student) + manager(student),student));
      return;
    }
    if (userRequest.equals("Edit")) { // edit the current portfolio
      out.println(Page.create(new Portfolio().edit(id,student),student));
      return;
    }
    if (userRequest.equals("Uploads")) { // view myUploadDirectory
      out.println(Page.create(topNavBar(student) + uploadManager(student),student));
      return;
    }
    if (userRequest.equals("Search")) {
      String ownerID = request.getParameter("OwnerID");
      out.println(Page.create(topNavBar(student) + search(ownerID,student),student));
      return;
    }
    // should never get to this point, but just in case display student's home portfolio
    out.println(Page.create(new Portfolio().display(student.getIDNumber(),"Home"),student));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    String userRequest = request.getParameter("UserRequest");
    String code = (request.getParameter("Code")==null?"":request.getParameter("Code"));
    int id = -1;
    try {
      id = Integer.parseInt(request.getParameter("ID"));
    }
    catch (Exception e) {
    }
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Portfolio");
      return;
    }
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen"),student));
      return;
    }
    // from here on, user is assumed to be an authenticated student or instructor
    
    if (userRequest==null) { // file upload request
      out.println(Page.create(uploadFile(request,response,student) + uploadManager(student),student));
      return;
    }   
    if (userRequest.equals("Manager")) { // manage my portfolios
      out.println(Page.create(manager(student),student));
      return;
    }
    if (userRequest.equals("Uploads")) { // view myUploadDirectory
      out.println(Page.create(uploadManager(student),student));
      return;
    }
    if (userRequest.equals("DeleteUpload")) { // delete a file
      String filename = request.getParameter("UploadFilename");
      out.println(Page.create(deleteFile(filename,student),student));
      return;
    }
    if (userRequest.equals("UpdateCodes")) {
      out.println(Page.create(updateCodes(student,request),student));
      return;
    }
    if (userRequest.equals("Create")) { // create a new portfolio using id as a template
      if (id<0) {
        out.println(Page.create(create(student,request),student)); // Wizard create
        return;
      }
      String type = (request.getParameter("Type")==null?"Home":request.getParameter("Type"));
      String name = (request.getParameter("Name")==null?"Home":request.getParameter("Name"));
      out.println(Page.create(create(student,type,name,id,code),student)); // Template create
      return;
    }
    if (userRequest.equals("Delete")) { // delete a portfolio from the manager screen
      out.println(Page.create(delete(id,student),student));
      return;
    }
    if (userRequest.equals("Edit")) { // edit the current portfolio
      out.println(Page.create(new Portfolio().edit(id,student),student));
      return;
    }
    if (userRequest.equals("Update")) {
      out.println(Page.create(new Portfolio().update(id,student,request),student));
      return;
    }
    if (userRequest.equals("Search")) {
      String ownerID = request.getParameter("OwnerID");
      out.println(Page.create(search(ownerID,student),student));
      return;
    }
    // should never get to this point, but just in case display student's home portfolio
    out.println(Page.create(new Portfolio().display(student.getIDNumber(),"Home"),student));
  }

  boolean okToView(int id,String code,Student student) {
    if (code==null) code = "";
    if (id<0) return false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'");
      rsPortfolio.next();
      if (student.getIsInstructor() || student.getIDNumber().equals(rsPortfolio.getString("OwnerID"))
       || code.equals(rsPortfolio.getString("Code")) || rsPortfolio.getString("Code").equals("NULL"))
       return true;
      else return false;
    }
    catch (Exception e) {
      return false;
    }
  }
  
  boolean load(int id) {
    this.id = id;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'");
      if (rsPortfolio.next()) {
        this.ownerID = rsPortfolio.getString("OwnerID");
        this.name = rsPortfolio.getString("Name");
        this.type = rsPortfolio.getString("Type");
        this.title = rsPortfolio.getString("Title");
        this.description = rsPortfolio.getString("Description");
        this.imgURL = rsPortfolio.getString("ImgURL");
        this.imgAlt = rsPortfolio.getString("ImgAlt");
        this.fullName = rsPortfolio.getString("FullName");
        this.title1 = rsPortfolio.getString("Title1");
        this.title2 = rsPortfolio.getString("Title2");
        this.address1 = rsPortfolio.getString("Address1");
        this.address2 = rsPortfolio.getString("Address2");
        this.email = rsPortfolio.getString("Email");
        this.homePageURL = rsPortfolio.getString("HomePageURL");
        this.telephone1 = rsPortfolio.getString("Telephone1");
        this.telephone2 = rsPortfolio.getString("Telephone2");
      }
      else return false;
      
      ResultSet rsArtifacts = stmt.executeQuery("SELECT * FROM PortfolioArtifacts "
      + "WHERE PortfolioID='" + this.id + "' ORDER BY ArtifactNumber");
      this.artifacts.clear();       // remove any existing artifacts from this portfolio
      while (rsArtifacts.next()) {  // and load new artifacts from the database
        Artifact a = new Artifact();
        a.load(rsArtifacts);
        this.artifacts.add(a);
      }
    }
    catch (Exception e) {
      return false;
    }
    return true;
  }
  
  String display(int id) {
    if (load(id)) {
      if (type.equals("Home")) return homePage();
      if (type.equals("Resume")) return resume();
      if (type.equals("Collection")) return collection();
      if (type.equals("Skills")) return skills();
    }
    return res.getString("str_pf_load_failed");
  }

  int getID(String ownerID,String name) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT ID FROM Portfolios WHERE OwnerID='"
      + ownerID + "' AND Name='" + name + "'");
      if (rsPortfolio.next()) return rsPortfolio.getInt("ID");
    }
    catch (Exception e) {
    }
    return 0;
  }
  
  String display(String ownerID,String name) {
    MessageFormat mf = new MessageFormat(res.getString("str_pf_doesnt_exist"));
    Object[] args = { ownerID + ":" + name };
    int id = getID(ownerID,name);
    if (id>0) return display(id);
    else return mf.format(args);
  }

  String manager (Student student) {
    StringBuffer buf = new StringBuffer();
    buf.append("<H3>" + res.getString("str_title_pf_manager") + "</H3>"
    + res.getString("str_explain_manager"));
    buf.append("<FORM NAME=ManagerForm METHOD=POST ACTION='/servlet/" + Course.name + ".Portfolio'>"
    + "<TABLE BORDER=1 CELLSPACING=0>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsPortfolios = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='"
      + student.getIDNumber() + "'");
      buf.append("<TR><TH></TH><TH>" + res.getString("str_id") + "</TH><TH>"
      + res.getString("str_name") + "</TH><TH>" + "</TH><TH>" + res.getString("str_code") 
      + "</TH><TH>" + res.getString("str_full_url") + "</TH></TR>");
      while (rsPortfolios.next()) {
        int id = rsPortfolios.getInt("ID");
        String code = rsPortfolios.getString("Code"); if (code==null) code = "";
        String url = "" + Course.server + "/servlet/" 
        + Course.name + ".Portfolio?ID=" + id + (code.length()==0?"":"&Code=" + code);
        
        buf.append("<TR><TD><INPUT TYPE=RADIO NAME=ID VALUE='" + id + "'><TD>" + id 
        + "</TD><TD>" + rsPortfolios.getString("Name") + "</TD><TD>" 
        + rsPortfolios.getString("Type") + "</TD><TD>" 
        + "<INPUT SIZE=8 NAME='" + id + ":Code' Value='" + code + "'>" 
        + "</TD><TD><a href='" + url + "'>" + url + "</a></TD></TR>");
      }
    }
    catch (Exception e) {
    }
    buf.append("</TABLE><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update") + "'>&nbsp;");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=UpdateCodes>");
    buf.append("<INPUT TYPE=BUTTON VALUE='" + res.getString("str_btn_del") + "' "
    + "onClick=\"document.ManagerForm.UserRequest.value='Delete'; "
    + "if (!isChecked()) alert('" + res.getString("str_must_select_del") + "');"
    + "else if (confirm('" + res.getString("str_conf_del") + "')) document.ManagerForm.submit();\">");

    buf.append("</FORM>");

    buf.append("<SCRIPT LANGUAGE=Javascript>"
    + "function isChecked() {"
    + "var checked=false;"
    + "if (document.ManagerForm.ID!=undefined) {" 
    + "  if (document.ManagerForm.ID.length!=undefined) {"
    + "    for (i=0;i<document.ManagerForm.ID.length;i++) if (document.ManagerForm.ID[i].checked) checked=true;}"
    + "  else if (document.ManagerForm.ID.checked) checked=true;"
    + " } return checked;}"
    + "</SCRIPT>");
 
    
    buf.append("<H3>" + res.getString("str_title_create_new") + "</H3>"
    + res.getString("str_explain_create1") + "<p>"
    + res.getString("str_explain_create2"));
    buf.append("<FORM NAME=CreateForm METHOD=POST ACTION='/servlet/" + Course.name + ".Portfolio'>"
    + "<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='Create'>"
    + res.getString("str_type") + ":&nbsp;<SELECT NAME=Type>"
    + "<OPTION VALUE=Collection>" + res.getString("str_collection") + "</OPTION>"
    + "<OPTION VALUE=Home>" + res.getString("str_home") + "</OPTION>"
    + "<OPTION VALUE=Resume>" + res.getString("str_resume") + "</OPTION>"
    + "<OPTION VALUE=Skills>" + res.getString("str_skills") + "</OPTION>"
    + "</SELECT><br>");
    buf.append(res.getString("str_name") + ":&nbsp;<INPUT NAME=Name><br>" 
    + res.getString("str_template_id") + ":&nbsp;<INPUT SIZE=4 NAME=ID> "
    + res.getString("str_code") + ":&nbsp;<INPUT SIZE=8 NAME=Code><br>");
    buf.append("<INPUT TYPE=Submit VALUE='" + res.getString("str_btn_new_pf") + "'></FORM>");
    
    buf.append("<TABLE BORDER=1 CELLSPACING=0>"
    + "<TR><TH>" + res.getString("str_id") + "</TH><TH>" + res.getString("str_name") 
    + "</TH><TH>" + res.getString("str_type") + "</TH><TH>" 
    + res.getString("str_full_url") + "</TH></TR>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsPortfolios = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='0'");
      while (rsPortfolios.next()) {
        int id = rsPortfolios.getInt("ID");
        String code = rsPortfolios.getString("Code"); if (code==null) code = "";
        String url = "" + Course.server + "/servlet/" + Course.name + ".Portfolio?ID=" + id 
        + (code.length()>0?"&Code=" + code:"");
        buf.append("<TR><TD>" + id + "</TD><TD>"
        + rsPortfolios.getString("Name") + "</TD><TD>" + rsPortfolios.getString("Type") 
        + "</TD><TD>" + "<a href='" + url + "'>" + url + "</a></TD></TR>");
      }
    }
    catch (Exception e) {
    }
    buf.append("</TABLE>");
    return buf.toString();
  } 

  String randomCode() {
    String choice = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ";
    int size = 8;
    Random rand = new Random();
    StringBuffer code = new StringBuffer();
    for (int i=0;i<size;i++) {
      code.append(choice.charAt(rand.nextInt(choice.length())));
    }
    return code.toString();
  }

  String updateCodes(Student student,HttpServletRequest request) {
    if (student.getIsVisitor()) return res.getString("str_visitor_cant_alter");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsPortfolios = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='"
      + student.getIDNumber() + "'");
      while (rsPortfolios.next()) {
        int id = rsPortfolios.getInt("ID");
        if (request.getParameter(id + ":Code")!=null) {
          rsPortfolios.updateString("Code",request.getParameter(id + ":Code"));
          rsPortfolios.updateRow();
        }
      }
    }
    catch (Exception e) {
    }
    return manager(student);
  }

  String uploadManager(Student student) {
    // this method lists all student uploaded files for portfolios and a form element
    // for uploading additional files and artifacts to the server
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_explain_new_upload1"));
    Object[] args = {
      "<I>",
      "</I>"
    };
    buf.append("<H2>" + res.getString("str_title_upload") + "</H2>"
    + res.getString("str_explain_upload") + "<HR>");
    
    //the encoding type is multipart/form-data for file uploads
    buf.append("<FORM ENCTYPE='multipart/form-data' METHOD=POST ACTION='/servlet/" 
    + Course.name + ".Portfolio'>");
    // ServerFilename form element must precede FILE form element or local file name will be used
    buf.append("<H3>" + res.getString("str_title_upload_new") 
    + "</H3><FONT SIZE=-1 COLOR=#FF0000>" + mf.format(args) + "<br>" 
    + res.getString("str_explain_new_upload2") + "<br>" + res.getString("str_explain_new_upload3")
    + "</FONT><br>");
    String uploadsURL = "" + Course.server + "/" + Course.name + "/uploads/"
    + student.getIDNumber() + "/";
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='UploadFile'>");
    buf.append(res.getString("str_url_full") + " <i>" + uploadsURL + "</i><INPUT NAME='ServerFilename'>");
    buf.append("<br><INPUT TYPE=FILE NAME='FileToUpload' SIZE=50 MAXLENGTH=255>");
    buf.append("<br><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_upload_file") + "'>");
    buf.append("</FORM><HR>" + "\n");
    
    buf.append("<H3>" + res.getString("str_title_existing_uploads") + "</H3>");
    File myDirectory = new File(Course.uploadsDirectory + "/" + student.getIDNumber() + "/");
    if (!myDirectory.exists()) myDirectory.mkdir();
    String listAllMyFiles = fileList(myDirectory,uploadsURL);
    if (listAllMyFiles.length() > 0) {
      buf.append("<FORM METHOD=POST ACTION='/servlet/" + Course.name + ".Portfolio'>"
      + "<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='DeleteUpload'>"
      + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_del_file") + "'><TABLE BORDER=0>"
      + listAllMyFiles
      + "</TABLE><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_del_file") + "'></FORM>");
    }
    else buf.append("(" + res.getString("str_none") + ")");

    return buf.toString();
  }

  String fileList(File myDirectory,String uploadsURL) {
    StringBuffer buf = new StringBuffer();
    File[] myFiles = myDirectory.listFiles();
    for (int i=0;i<myFiles.length;i++) {
      if (myFiles[i].isDirectory()) buf.append(fileList(myFiles[i],uploadsURL+myFiles[i].getName()+"/"));
      else {
        try {
          String fileURL = uploadsURL + myFiles[i].getName();
          buf.append("<TR>"
          + "<TD><INPUT TYPE=RADIO NAME=UploadFilename VALUE='" + myFiles[i].getCanonicalPath() + "'></TD>"
          + "<TD><A HREF='" + fileURL + "'>" + fileURL + "</A></TD></TR>");
        }
        catch (Exception e) {
          buf.append("<TR><TD></TD><TD>" + e.getMessage() + "</TD></TR>");
        }
      }
    }
    return buf.toString();
  }
  
  String create(Student student,HttpServletRequest request) { // create a portfolio using the wizard
    StringBuffer debug = new StringBuffer((debugMode?"Debug mode ON<br>":""));
    String name = request.getParameter("Name"); if (name==null) name = "";
    String type = request.getParameter("Type"); if (type==null) type = "Home";
    if (student.getIsVisitor()) return res.getString("str_visitor_cant_create");
    if (name.length()==0) return res.getString("str_enter_name");
    int newID=0; // used for keeping track of the newly created portfolio ID; id refers to the template
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsExists = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='" 
      + student.getIDNumber() + "' AND Name='" + name + "'");
      if (rsExists.next())
        return "<H2>" + res.getString("str_error") + "</H2>" + res.getString("str_need_unique");
      
      ResultSetMetaData rsmdTemplate = stmt.executeQuery("SELECT * FROM Portfolios").getMetaData();
      StringBuffer insertSQL1 = new StringBuffer("INSERT INTO Portfolios (OwnerID,Type,Name,Code");
      StringBuffer insertSQL2 = new StringBuffer(" VALUES ('" + student.getIDNumber() + "','"
      + type + "','" + name + "','" + randomCode() + "'");
      for (int i=6;i<=rsmdTemplate.getColumnCount();i++) {
        insertSQL1.append("," + rsmdTemplate.getColumnName(i));
        insertSQL2.append(",'" + CharHider.quot2literal(request.getParameter(rsmdTemplate.getColumnName(i))) + "'");
      }
      insertSQL1.append(")");
      insertSQL2.append(")");
      stmt.executeUpdate(insertSQL1.toString() + insertSQL2.toString());
      
      // the following gets the ID value of the newly created portfolio (to display it below)
      ResultSet rsTemp = stmt.executeQuery("SELECT LAST_INSERT_ID()");
      rsTemp.next();
      newID = rsTemp.getInt("LAST_INSERT_ID()");

      // create the associated artifacts
      ResultSetMetaData rsmdArtifacts = stmt.executeQuery("SELECT * FROM PortfolioArtifacts").getMetaData();

      int numberOfArtifacts=0;
      try {
        numberOfArtifacts = Integer.parseInt(request.getParameter("NumberOfArtifacts"));
      }
      catch (Exception e) {
      }       
      for (int j=1;j<=numberOfArtifacts;j++) {
        insertSQL1 = new StringBuffer("INSERT INTO PortfolioArtifacts (PortfolioID");
        insertSQL2 = new StringBuffer(" VALUES ('" + newID + "'");
        for (int i=2;i<=rsmdArtifacts.getColumnCount();i++) {
          insertSQL1.append("," + rsmdArtifacts.getColumnName(i));
          insertSQL2.append(",'" + request.getParameter(j + ":" + rsmdArtifacts.getColumnName(i)));
        }
        insertSQL1.append(")");
        insertSQL2.append(")");
        debug.append(insertSQL1.toString() + insertSQL2.toString());
        stmt.executeUpdate(insertSQL1.toString() + insertSQL2.toString());
      }   
    }
    catch (Exception e) {
      debug.append(e.getMessage());
      return debug.toString();
    }
    return res.getString("str_portfolio_created")
    + "<FORM METHOD=POST ACTION='/servlet/" + Course.name + ".PortfolioWizard'>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='ArtifactManager'>"
    + "<INPUT TYPE=HIDDEN NAME=PortfolioID VALUE='" + newID + "'>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_proceed") + "'></FORM>";  
  }
    
  String create(Student student,String type,String name,int id,String code) { // using a template
    StringBuffer debug = new StringBuffer((debugMode?"Debug mode ON<br>":""));
    if (student.getIsVisitor()) return res.getString("str_visitor_cant_create");
    if (name.length()==0) return res.getString("str_enter_name");
    int newID=0; // used for keeping track of the newly created portfolio ID; id refers to the template
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsExists = stmt.executeQuery("SELECT * FROM Portfolios WHERE OwnerID='" 
      + student.getIDNumber() + "' AND Name='" + name + "'");
      if (rsExists.next())
        return "<H2>" + res.getString("str_error") + "</H2>" + res.getString("str_need_unique");
      debug.append("OK to view " + id + ": "); debug.append(okToView(id,code,student));
      if (okToView(id,code,student)) {
        ResultSet rsTemplate = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'");
        ResultSetMetaData rsmdTemplate = rsTemplate.getMetaData();
        StringBuffer insertSQL1 = new StringBuffer("INSERT INTO Portfolios (OwnerID,Type,Name");
        StringBuffer insertSQL2 = new StringBuffer(" VALUES ('" + student.getIDNumber() + "','"
        + type + "','" + name + "'");
        rsTemplate.first();
        for (int i=5;i<=rsmdTemplate.getColumnCount();i++) {
          insertSQL1.append("," + rsmdTemplate.getColumnName(i));
          insertSQL2.append(",'" + CharHider.quot2literal(rsTemplate.getString(rsmdTemplate.getColumnName(i))) + "'");
        }
        insertSQL1.append(")");
        insertSQL2.append(")");
        stmt.executeUpdate(insertSQL1.toString() + insertSQL2.toString());
        ResultSet rsNewID = stmt.executeQuery("SELECT LAST_INSERT_ID()");
        rsNewID.first();
        newID = rsNewID.getInt("LAST_INSERT_ID()");
        stmt.executeUpdate("UPDATE Portfolios SET Code='" + randomCode() + "' WHERE ID='" + newID + "'");

      // create copies of the associated artifacts
        ResultSet rsArtifacts = stmt.executeQuery("SELECT * FROM PortfolioArtifacts WHERE PortfolioID='" + id + "'");
        ResultSetMetaData rsmdArtifacts = rsArtifacts.getMetaData();
        
        while (rsArtifacts.next()) {
          insertSQL1 = new StringBuffer("INSERT INTO PortfolioArtifacts (PortfolioID");
          insertSQL2 = new StringBuffer(" VALUES ('" + newID + "'");
          for (int i=3;i<=rsmdArtifacts.getColumnCount();i++) {
            insertSQL1.append("," + rsmdArtifacts.getColumnName(i));
            insertSQL2.append(",'" + rsArtifacts.getString(rsmdArtifacts.getColumnName(i)) + "'");
          }
          insertSQL1.append(")");
          insertSQL2.append(")");
          debug.append(insertSQL1.toString() + insertSQL2.toString());
          stmt.executeUpdate(insertSQL1.toString() + insertSQL2.toString());
        }
      }
      else {// create an empty generic portfolio
        stmt.executeUpdate("INSERT INTO Portfolios (OwnerID,Type,Name,Code) VALUES ('" 
        + student.getIDNumber() + "','" + type + "','" + name + "','" + randomCode() + "')");
        ResultSet rsNewID = stmt.executeQuery("SELECT LAST_INSERT_ID()");
        rsNewID.first();
        newID = rsNewID.getInt("LAST_INSERT_ID()");
        debug.append("Empty portfolio " + newID + " successfully created.");
      }     
    }
    catch (Exception e) {
      debug.append(createPortfolioTables());   
      return debugMode?debug.toString():create(student,type,name,id,code);
    }
    return topNavBar(student) + display(newID);
  }

  String delete(int id,Student student) {
    if (student.getIsVisitor()) return res.getString("str_visitor_cant_delete");
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement(); 
      ResultSet rsExists = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'"
      + (student.getIsInstructor()?"":" AND OwnerID='" + student.getIDNumber() + "'"));
      if (rsExists.next()) {
        stmt.executeUpdate("DELETE FROM Portfolios WHERE ID='" + id + "'");
        stmt.executeUpdate("DELETE FROM PortfolioArtifacts WHERE PortfolioID='" + id + "'");
      }
      else
        buf.append(res.getString("str_delete_error") + "<br>"); 
    }
    catch (Exception e) {
      buf.append(e.getMessage() + ": " + res.getString("str_del_failed"));
    }
    return buf.toString() + manager(student);
  }
    
  String edit(int id,Student student) {
    if (load(id) && (this.ownerID.equals(student.getIDNumber()) || student.getIsInstructor())) {
      if (type.equals("Home")) return homePageForm();
      if (type.equals("Resume")) return resumeForm();
      if (type.equals("Collection")) return collectionForm();
      if (type.equals("Skills")) return skillsForm();
    }
    return res.getString("str_pf_na");
  }
  
  String update(int id,Student student,HttpServletRequest request) {
    if (student.getIsVisitor()) return res.getString("str_visitor_cant_update");
    StringBuffer debug = new StringBuffer((debugMode?"Debug mode ON<br>":""));
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios WHERE ID='" + id + "'"
      + (student.getIsInstructor()?"":" AND OwnerID='" + student.getIDNumber() + "'"));
      if (rsPortfolio.first()) {
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

      // update the associated artifacts      
      stmt.executeUpdate("DELETE FROM PortfolioArtifacts WHERE PortfolioID='" + id + "'");
      int nArtifacts;
      try {
        nArtifacts = Integer.parseInt(request.getParameter("NumberOfArtifacts"));
      }
      catch (Exception e2) {
        nArtifacts = 0;
      }
      for (int i=0;i<nArtifacts;i++) {
        Artifact a = new Artifact();
        a.portfolioID = id;
        a.artifactNumber = i;
        a.artifactURL = request.getParameter(i + ":ArtifactURL");
        a.title = request.getParameter(i + ":ArtifactTitle");
        a.description = request.getParameter(i + ":ArtifactDescription");
        a.imgURL = request.getParameter(i + ":ImgURL");
        a.imgAlt = request.getParameter(i + ":ImgAlt");
        if (a.isDefault() || request.getParameter(i + ":DeleteMe")!=null) continue;
        else { // if not default or marked for deletion, insert into the database
          String sql = "INSERT INTO PortfolioArtifacts "
          + "(PortfolioID,ArtifactNumber,ArtifactURL,Title,Description,ImgURL,ImgAlt) "
          + "VALUES ('" + id + "','" + i + "','" + a.artifactURL + "','"
          + CharHider.quot2html(a.title)+ "','" + CharHider.quot2html(a.description) + "','"
          + a.imgURL + "','" + CharHider.quot2html(a.imgAlt) + "')";
          stmt.executeUpdate(sql);
        }
      }
    }
    catch (Exception e) {
      return res.getString("str_main_err") + "<br>" + debug.toString() + e.getMessage();
    }
    return res.getString("str_pf_updated") + "<br><FORM METHOD=POST ACTION='/servlet/" 
    + Course.name + ".PortfolioWizard'>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='ArtifactManager'>"
    + "<INPUT TYPE=HIDDEN NAME=PortfolioID VALUE='" + id + "'>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_artifacts") + "'></FORM>";  

  }
  
  String search(String ownerID,Student student) {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      StringBuffer sqlQuery = new StringBuffer("SELECT * FROM Portfolios LEFT JOIN Students"
      + " ON Students.StudentIDNumber=Portfolios.OwnerID");
      if (ownerID!=null || !student.getIsInstructor()) sqlQuery.append(" WHERE");
      sqlQuery.append(ownerID==null?"":" Portfolios.OwnerID='" + ownerID + "'");
      sqlQuery.append((ownerID!=null && !student.getIsInstructor())?" AND":"");
      if (!student.getIsInstructor())
        sqlQuery.append(" (Portfolios.Code IS NULL OR Portfolios.Code='')");
      sqlQuery.append(" ORDER BY Students.Status,Students.LastName,Students.FirstName,Portfolios.Name");
      ResultSet rsPortfolio = stmt.executeQuery(sqlQuery.toString());
      
      buf.append("<h3>" + res.getString("str_stud_pf") + "</h3>" + res.getString("str_stud_viewable")); 
      buf.append("<TABLE BORDER=1 CELLSPACING=0><TR><TH>" + res.getString("str_stud")  
      + "</TH><TH>" + res.getString("str_pf_name") + "</TH><TH>" + res.getString("str_full_url") + "</TH></TR>");
      while (rsPortfolio.next()) {
        buf.append("<TR><TD>" 
        + rsPortfolio.getString("Students.LastName") + "," 
        + rsPortfolio.getString("Students.FirstName")+ "</TD><TD>" 
        + rsPortfolio.getString("Portfolios.Name") + "</TD><TD>");
        String url = "" + Course.server + "/servlet/" + Course.name + ".Portfolio?"
        + "ID=" + rsPortfolio.getString("Portfolios.ID");
        String code = rsPortfolio.getString("Portfolios.Code"); 
        if (code==null) code = "";
        if (student.getIsInstructor() && code.length()>0) url += "&Code=" + code;
        buf.append("<A HREF=" + url + ">" + url + "</A></TD></TR>");
      }
      buf.append("</TABLE>");
    }
    catch (Exception e) {
      return e.getMessage();
    }
    return buf.toString();
  }

  String topNavBar(Student student) {
    // This bar goes across the top of the portfolio page
    // Choices are MyPortfolios,Search,EditThisPage,CreateNew,Permissions
    return "<TABLE WIDTH=100% BGCOLOR=#DDDDDD><TR ALIGN=CENTER>"
    + "<TD VALIGN=BOTTOM>" + myPortfolios(student) + "</TD>"
    + "<TD><FONT COLOR=#FFFFFF><B><a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Search'>" 
    + res.getString("str_search") + "</a></B></FONT></TD>"
    + "<TD><FONT COLOR=#FFFFFF><B><a href='/servlet/" + Course.name + ".PortfolioWizard'>"
    + res.getString("str_pf_wizard") + "</a></B></FONT></TD>"
    + "<TD><FONT COLOR=#FFFFFF><B><a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Manager')>"
    + res.getString("str_pf_manager") + "</a></B></FONT></TD>"
    + "<TD><FONT COLOR=#FFFFFF><B><a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Uploads')>"
    + res.getString("str_file_uploads") + "</a></B></FONT></TD>"
    + "</TR></TABLE>";
  }
  
  String myPortfolios(Student student) {
    // This method produces a select box of this student's portfolios for the topNavBar
    StringBuffer buf = new StringBuffer("<FORM METHOD=GET ACTION='/servlet/" 
    + Course.name + ".Portfolio'><SELECT NAME=ID OnChange='submit()'>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPortfolio = stmt.executeQuery("SELECT * FROM Portfolios "
      + "WHERE OwnerID='" + student.getIDNumber() + "' ORDER BY Type,Name");
        buf.append("<OPTION>My Portfolios</OPTION>");
      while (rsPortfolio.next()) {
        buf.append("<OPTION VALUE='" + rsPortfolio.getInt("ID") + "'>" + rsPortfolio.getString("Name") + "</OPTION>");
      }
      buf.append("</SELECT></FORM>");
    }
    catch (Exception e) {
    }
    return buf.toString();
  }
  
  String homePage() {
    StringBuffer buf = new StringBuffer();
    
    buf.append("<TABLE BORDER=0><TR><TD>");  

    if (this.imgURL.length()>0 && !this.imgURL.equals("null"))
      buf.append("<TD VALIGN=TOP><IMG WIDTH=150 SRC='" + this.imgURL + "' "
      + "ALT='" + this.imgAlt + "'></TD><TD>");
    else buf.append("<TD COLSPAN=2>");

    buf.append("<H1>" + this.fullName + "</H1>"
      + "<H3>" + this.title1 + "<br>" + this.title2 + "</H3>");
    buf.append("<a href='mailto:" + this.email + "'>" + this.email + "</a><br>");
    buf.append("<a href='" + this.homePageURL + "'>" + this.homePageURL + "</a>");
    buf.append("</TD></TR></TABLE>");

    buf.append("<H3>" + this.title + "</H3>");
    buf.append(this.description);
    
    buf.append("<TABLE>");
    for (Enumeration e = this.artifacts.elements();e.hasMoreElements();) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR><TD COLSPAN=2><HR></TD></TR><TR>" + a.display() + "</TR>");
    }
    buf.append("</TABLE>");
    return buf.toString();
  }
  
  String homePageForm() {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM NAME=UpdateForm METHOD=POST ACTION=/servlet/" + Course.name + ".Portfolio>");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Update>");
    buf.append("<INPUT TYPE=HIDDEN NAME=ID VALUE='" + this.id + "'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Type VALUE='Home'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Name VALUE='" + CharHider.quot2html(this.name) + "'>");
    buf.append("<TABLE BORDER=0><TR><TD>");  
    buf.append("<IMG SRC='" + this.imgURL + "' ALT='" + CharHider.quot2html(this.imgAlt) + "'><br>"
    + res.getString("str_img_url") + "<INPUT NAME=ImgURL VALUE='" + this.imgURL + "'><br>"
    + res.getString("str_alt") + "<INPUT NAME=ImgAlt VALUE='" + CharHider.quot2html(this.imgAlt) + "'></TD>");
    buf.append("<TD><TABLE BORDER=0>"
    + "<TR><TD>" + res.getString("str_name") + ":</TD><TD><INPUT NAME=FullName VALUE='" + CharHider.quot2html(this.fullName) 
    + "'></TD></TR><TR><TD>" + res.getString("str_status") + "</TD><TD><INPUT NAME=Title1 VALUE='" 
    + CharHider.quot2html(this.title1) + "'></TD></TR><TR><TD>" + res.getString("str_subtitle") 
    + "</TD><TD><INPUT NAME=Title2 VALUE='" + CharHider.quot2html(this.title2) + "'></TD></TR><TR><TD>"
    + res.getString("str_email") + "</TD><TD><INPUT NAME=Email VALUE='" + CharHider.quot2html(this.email) 
    + "'></TD></TR><TR><TD>" + res.getString("str_hp") + "</TD><TD><INPUT NAME=HomePageURL VALUE='" 
    + this.homePageURL + "'></TD></TR></TABLE>");
    buf.append("</TD></TR></TABLE><HR>");
    buf.append(res.getString("str_title") + " <INPUT SIZE=30 NAME=Title VALUE='" + CharHider.quot2html(this.title) + "'>");
    buf.append(res.getString("str_desc") + "<br><TEXTAREA NAME=Description ROWS=6 COLS=80>"
    + CharHider.quot2html(this.description) + "</TEXTAREA><P>");
    // start to write out artifact descriptions
    buf.append("<TABLE BORDER=0>");    
    Enumeration e = this.artifacts.elements();
    int i=0;
    while (e.hasMoreElements()) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR>" + a.edit(i) + "</TR>");
      i++;
    }
    buf.append(new Artifact().edit(i));  // write one more blank form to add another artifact
    i++;
    buf.append("</TABLE><HR>");
    buf.append("<INPUT TYPE=HIDDEN NAME=NumberOfArtifacts VALUE='" + i + "'>");
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_page") + "'></FORM>");
    return buf.toString();
  }

  String resume() {
    StringBuffer buf = new StringBuffer();
    buf.append("<TABLE BORDER=0><TR><TD></TD><TD ALIGN=CENTER><H2>" + this.fullName + "</H2></TD><TD ALIGN=RIGHT></TD></TR>");
    buf.append("<TR><TD>" + this.address1 + "</TD><TD ALIGN=CENTER></TD><TD ALIGN=RIGHT>" + this.address2 + "</TD></TR>");
    buf.append("<TR><TD>" + this.telephone1 + "</TD><TD ALIGN=CENTER></TD><TD ALIGN=RIGHT>" + this.telephone2 + "</TD></TR>");
    buf.append("<TR><TD COLSPAN=3><HR></TD></TR>");
    buf.append("<TR><TD COLSPAN=3><H3>" + this.title + "</H3>" + this.description + "<P></TD></TR>");
    buf.append("<TR><TD COLSPAN=3><TABLE WIDTH=100% BORDER=0 CELLSPACING=0 CELLPADDING=2>");
    for (Enumeration e = this.artifacts.elements();e.hasMoreElements();) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR>" + a.display() + "</TR>");
    }
    buf.append("</TABLE></TD></TR>");
    
    buf.append("</TABLE>");
    return buf.toString();
  }
  
  String resumeForm() {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM NAME=UpdateForm METHOD=POST ACTION=" + Course.name + ".Portfolio>");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Update>");
    buf.append("<INPUT TYPE=HIDDEN NAME=ID VALUE='" + this.id + "'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Type VALUE='Resume'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Name VALUE='" + CharHider.quot2html(this.name) + "'>");
    
    buf.append("<TABLE BORDER=0><TR><TD></TD><TD ALIGN=CENTER>"
    + res.getString("str_name") + ":<br><INPUT NAME=FullName VALUE='" + CharHider.quot2html(this.fullName) 
    + "'></TD><TD ALIGN=RIGHT></TD></TR>");
    buf.append("<TR><TD>" + res.getString("str_addy1") + "<br><TEXTAREA WRAP=SOFT NAME=Address1 ROWS=5 COLS=30>"
    + CharHider.quot2html(this.address1) + "</TEXTAREA></TD><TD ALIGN=CENTER></TD><TD ALIGN=RIGHT>"
    + res.getString("str_addy2") + "<br><TEXTAREA WRAP=SOFT NAME=Address2 ROWS=5 COLS=30>"
    + CharHider.quot2html(this.address2) + "</TEXTAREA></TD></TR>");
    buf.append("<TR><TD>" + res.getString("str_tel1") + "<br><TEXTAREA WRAP=SOFT NAME=Telephone1 ROWS=2 COLS=30>"
    + CharHider.quot2html(this.telephone1) + "</TEXTAREA></TD><TD ALIGN=CENTER></TD><TD ALIGN=RIGHT>"
    + res.getString("str_tel2") + "<br><TEXTAREA WRAP=SOFT NAME=Telephone2 ROWS=2 COLS=30>"
    + CharHider.quot2html(this.telephone2) + "</TEXTAREA></TD></TR>");
    buf.append("</TABLE>");
    
    buf.append(res.getString("str_title") + " <INPUT SIZE=30 NAME=Title VALUE='" + CharHider.quot2html(this.title) + "'>");
    buf.append(res.getString("str_desc") + "<br><TEXTAREA NAME=Description ROWS=6 COLS=80>"
    + CharHider.quot2html(this.description) + "</TEXTAREA><BR>");
    // start to write out artifact descriptions
    buf.append("<TABLE>");    
    Enumeration e = this.artifacts.elements();
    int i=0;
    while (e.hasMoreElements()) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR>" + a.edit(i) + "</TR>");
      i++;
    }
    buf.append(new Artifact().edit(i));  // write one more blank form to add another artifact
    i++;
    buf.append("</TABLE><HR>");
    buf.append("<INPUT TYPE=HIDDEN NAME=NumberOfArtifacts VALUE='" + i + "'>");
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_page") + "'></FORM>");
    return buf.toString();
  }

  String collection() {
    StringBuffer buf = new StringBuffer();
    
    buf.append("<TABLE BORDER=0><TR>");  
    if (this.imgURL.length()>0 && !this.imgURL.equals("null"))
      buf.append("<TD VALIGN=TOP><IMG WIDTH=150 SRC='" + this.imgURL + "' "
      + "ALT='" + this.imgAlt + "'></TD><TD>");
    else buf.append("<TD COLSPAN=2>");
    buf.append("<H2>" + this.title + "</H2>" + this.description);

    buf.append("</TD></TR>");
    
    buf.append("<TR><TD COLSPAN=2><TABLE>");
    for (Enumeration e = this.artifacts.elements();e.hasMoreElements();) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR><TD COLSPAN=2><HR></TD></TR><TR>" + a.display() + "</TR>");
    }
    buf.append("</TABLE></TR></TABLE>");
    return buf.toString();
  }
  
  String collectionForm() {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM NAME=UpdateForm METHOD=POST ACTION=" + Course.name + ".Portfolio>");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Update>");
    buf.append("<INPUT TYPE=HIDDEN NAME=ID VALUE='" + this.id + "'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Type VALUE='Collection'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Name VALUE='" + this.name + "'>");
    buf.append("<TABLE BORDER=0><TR><TD VALIGN=TOP>");  
    buf.append("<IMG WIDTH=150 SRC='" + this.imgURL + "' ALT='" + this.imgAlt + "'><br>"
    + res.getString("str_img_url") + "<INPUT NAME=ImgURL VALUE='" + this.imgURL + "'><br>"
    + res.getString("str_alt") + "<INPUT NAME=ImgAlt VALUE='" + this.imgAlt + "'></TD>");
    buf.append("<TD>");
    buf.append(res.getString("str_title") + " <INPUT SIZE=30 NAME=Title VALUE='" + this.title + "'>");
    buf.append(res.getString("str_desc") + "<br><TEXTAREA NAME=Description ROWS=10 COLS=50>"
    + this.description + "</TEXTAREA></TD></TR>");
    // start to write out artifact descriptions
    buf.append("<TR><TD COLSPAN=2><TABLE BORDER=0>");    
    Enumeration e = this.artifacts.elements();
    int i=0;
    while (e.hasMoreElements()) {
      Artifact a = (Artifact)e.nextElement();
      buf.append("<TR>" + a.edit(i) + "</TR>");
      i++;
    }
    buf.append(new Artifact().edit(i));  // write one more blank form to add another artifact
    i++;
    buf.append("</TABLE></TD></TR></TABLE>");
    buf.append("<INPUT TYPE=HIDDEN NAME=NumberOfArtifacts VALUE='" + i + "'>");
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_page") + "'></FORM>");
    return buf.toString();
  }
  
  String skills() {
    StringBuffer buf = new StringBuffer();
    
    buf.append("<TABLE BORDER=0><TR>");  
    if (this.imgURL.length()>0 && !this.imgURL.equals("null"))
      buf.append("<TD VALIGN=TOP><IMG WIDTH=150 SRC='" + this.imgURL + "' "
      + "ALT='" + this.imgAlt + "'></TD><TD>");
    else buf.append("<TD COLSPAN=2>");
    buf.append("<H2>" + this.title + "</H2>" + this.description);

    buf.append("</TD></TR>");
    
    buf.append("<TR><TD COLSPAN=2><TABLE>");
    for (Enumeration e = this.artifacts.elements();e.hasMoreElements();) {
      buf.append("<TR><TD COLSPAN=6><HR></TD></TR><TR>");
      for (int i=0;i<3;i++) {
        buf.append(e.hasMoreElements()?((Artifact)e.nextElement()).display():"<TD></TD>");
      }
      buf.append("</TR>");
    }
    buf.append("</TABLE></TD></TR></TABLE>");
    return buf.toString();
  }
  
  String skillsForm() {
    StringBuffer buf = new StringBuffer();
    buf.append("<FORM NAME=UpdateForm METHOD=POST ACTION=" + Course.name + ".Portfolio>");
    buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=Update>");
    buf.append("<INPUT TYPE=HIDDEN NAME=ID VALUE='" + this.id + "'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Type VALUE='Skills'>");
    buf.append("<INPUT TYPE=HIDDEN NAME=Name VALUE='" + this.name + "'>");
    buf.append("<TABLE BORDER=0><TR><TD VALIGN=TOP>");  
    buf.append("<IMG WIDTH=150 SRC='" + this.imgURL + "' ALT='" + this.imgAlt + "'><br>"
    + res.getString("str_img_url") + "<INPUT NAME=ImgURL VALUE='" + this.imgURL + "'><br>"
    + res.getString("str_alt") + "<INPUT NAME=ImgAlt VALUE='" + this.imgAlt + "'></TD>");
    buf.append("<TD>");
    buf.append(res.getString("str_title") + " <INPUT SIZE=30 NAME=Title VALUE='" + this.title + "'>");
    buf.append(res.getString("str_desc") + "<br><TEXTAREA NAME=Description ROWS=10 COLS=50>"
    + this.description + "</TEXTAREA></TD></TR>");
    // start to write out artifact descriptions
    buf.append("<TR><TD COLSPAN=2><TABLE BORDER=0>");
    int i=0;    
    for (Enumeration e = this.artifacts.elements();e.hasMoreElements();) {
      buf.append("<TR><TD COLSPAN=6><b>Skill " + (i/3+1) + ":</b></TD></TR><TR>");
      for (int j=0;j<3;j++) {
        buf.append(e.hasMoreElements()?((Artifact)e.nextElement()).edit(i):(new Artifact().edit(i)));
        i++; 
      }
      buf.append("</TR>");
    }
    buf.append("<TR><TD COLSPAN=6><b>" + res.getString("str_new_skill") + "</b></TD></TR><TR>");  // write one more row of artifacts
    for (int j=0;j<3;j++) {
      buf.append(new Artifact().edit(i));
      i++;
    }
    buf.append("</TR>");
    buf.append("</TABLE></TD></TR></TABLE>");
    buf.append("<INPUT TYPE=HIDDEN NAME=NumberOfArtifacts VALUE='" + i + "'>");
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_page") + "'></FORM>");
    return buf.toString();
  }
  
  String createPortfolioTables() {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Portfolios (ID INT PRIMARY KEY AUTO_INCREMENT,"
      + "OwnerID VARCHAR(50),Type TEXT NOT NULL,Name TEXT NOT NULL,Code TEXT NOT NULL,"
      + "Title TEXT NOT NULL,Description TEXT NOT NULL,ImgURL TEXT NOT NULL,ImgAlt TEXT NOT NULL,"
      + "FullName TEXT NOT NULL,Title1 TEXT NOT NULL,Title2 TEXT NOT NULL,"
      + "HomePageURL TEXT NOT NULL,Email TEXT NOT NULL,Address1 TEXT NOT NULL,"
      + "Address2 TEXT NOT NULL,Telephone1 TEXT NOT NULL,Telephone2 TEXT NOT NULL)");
      stmt.executeUpdate("INSERT INTO Portfolios VALUES('0','0','Home','Home','','" + res.getString("str_about_me") 
      + "','" + res.getString("str_default_desc")  
      + "','http://eledge.org/images/silhouette.gif','" + res.getString("str_my_pic") + "',"
      + "'A. Student','B.A. in Business','Class of 2005','http://eledge.org','student@eledge.org',"
      + "'School of Business<br>Hometown College<br>Bartlett, PA 20045',"
      + "'125 E. Main St.<br>Salt Lake City, UT 84102','208-555-1212(voice)<br>208-566-1784(FAX)',"
      + "'801-583-2336')");
      stmt.executeUpdate("UPDATE Portfolios Set ID=0 WHERE OwnerID=0 AND Name='Home'");
      stmt.executeUpdate("CREATE TABLE PortfolioArtifacts (ArtifactID INT PRIMARY KEY "
      + "AUTO_INCREMENT,PortfolioID INT,ArtifactNumber INT,"
      + "ArtifactURL TEXT NOT NULL,Title TEXT NOT NULL,Description TEXT NOT NULL,"
      + "ImgURL TEXT NOT NULL,ImgAlt TEXT NOT NULL)");
      buf.append(res.getString("str_tables_created"));
    }
    catch (Exception e) {
      buf.append(e.getMessage());
    }
    return buf.toString();    
  }

  String deleteFile(String filename,Student student) {
    MessageFormat mf = new MessageFormat(res.getString("str_cant_delete"));
    Object[] args = { filename };
    File myFile = new File(filename);
    if (fileAccessIsAllowed(filename,student)) { // operation allowed
      if (myFile.delete())  
        return topNavBar(student) + uploadManager(student);
      else return mf.format(args)  + "<br>" + uploadManager(student);
    }
    else // operation forbidden because file is not in the allowed part of the directory tree
      return res.getString("str_op_bad") + "<br>" + uploadManager(student); 
  }
    
  String uploadFile(HttpServletRequest request, HttpServletResponse response,Student student)
    throws IOException {
    
    PrintWriter out = response.getWriter();
    
    String fileLocation = Course.uploadsDirectory + "/" + student.getIDNumber() + "/";
    int contentLength=request.getContentLength();
    String contentType = request.getContentType();
    int ind = contentType.indexOf("boundary=");
    if (ind == -1) return res.getString("str_nofile_uploaded");
    String boundary = contentType.substring(ind+9);
    
    if (boundary == null) return res.getString("str_nobound");
    String boundaryString = "--"+boundary;

    ServletInputStream in = request.getInputStream();
    byte[] buffer = new byte[1024];
    table.clear();

    int result=readLine(in, buffer, 0, buffer.length);

    outer: while (true) {
      if (result<=0) break;
      String line = new String(buffer, 0, result); 
      
      if (!line.startsWith(boundaryString)) break;
      result=readLine(in, buffer, 0, buffer.length);
      if (result<=0) break;
      line = new String(buffer, 0, result);
      StringTokenizer tokenizer=new StringTokenizer(line, ";\r\n");
      String token=tokenizer.nextToken();
      String upperToken = token.toUpperCase();
      if (!upperToken.startsWith("CONTENT-DISPOSITION")) break;
      String disposition = upperToken.substring(21);
      if (!disposition.equals("FORM-DATA")) break;
      if (tokenizer.hasMoreElements())
        token=tokenizer.nextToken();
      else break;
      
      int nameStart=token.indexOf("name=\"");
      int nameEnd=token.indexOf("\"", nameStart+7);
      
      if (nameStart<0 || nameEnd<0) break;
      String name=token.substring(nameStart+6, nameEnd);
      if (tokenizer.hasMoreElements()) {
        token=tokenizer.nextToken();
        int tokenStart=token.indexOf("filename=\"");
        out.print(token.toString());
        int tokenEnd=token.indexOf("\"", tokenStart+11);
        if (tokenStart<0 || tokenEnd<0) break;
        String filename=token.substring(tokenStart+10, tokenEnd);
        int lastindex=-1;
        if ((lastindex=filename.lastIndexOf('/'))<0)
          lastindex=filename.lastIndexOf('\\');
        if (lastindex>=0)
          filename=filename.substring(lastindex+1);
        FileOutputStream f_out;
        String serverFilename = getValue("ServerFilename");
        if (serverFilename==null || serverFilename.length()==0) 
          serverFilename = filename; // if the file is not renamed in the form, use the same name as on the client
        serverFilename = fileLocation + '/' + serverFilename;
        if (!fileAccessIsAllowed(serverFilename,student)) {
          return res.getString("str_cant_upload") + (new File(serverFilename).getAbsolutePath());
        }
        try {
          f_out=new FileOutputStream(serverFilename);
        } 
        catch (Exception e) {
          break;
        }

        appendValue(name, filename, true);
        result=readLine(in, buffer, 0, buffer.length);
        if (result<=0) break;
        int size=0;
        try {
          byte[] tmpbuffer=new byte[buffer.length];
          int tmpbufferlen=0;
          boolean isFirst=true;
          inner: while ((result=readLine(in, buffer, 0, buffer.length))>0) {
            if (isFirst) { // ignore all proceeding \r\n
              if (result==2 && buffer[0]=='\r' && buffer[1]== '\n')
                continue;
            }
            String tmp=new String(buffer, 0, result);
            if (tmp.startsWith(boundaryString)) {
              if (!isFirst) {
                size+=tmpbufferlen-2;
                f_out.write(tmpbuffer, 0, tmpbufferlen-2);
              }
              continue outer;
            }
            else {
              if (!isFirst) {
                size+=tmpbufferlen;
                f_out.write(tmpbuffer, 0, tmpbufferlen);
              }
            }
            System.arraycopy(buffer, 0, tmpbuffer, 0, result);
            tmpbufferlen=result;
            isFirst=false;
          }
        } catch (IOException e) {
        } catch (Exception e) {
        } finally {
        }
        result=readLine(in, buffer, 0, buffer.length);
      }
      else { // no more elements
        result=readLine(in, buffer, 0, buffer.length);
        if (result<=0) break;
        result=readLine(in, buffer, 0, buffer.length);
        if (result<=0) break;
        String value = new String(buffer, 0, result-2); // exclude \r\n
        appendValue(name, value, false);
      }
      result=readLine(in, buffer, 0, buffer.length);
    } // end of while
    //printResult(out);
    table.clear();
    return res.getString("str_upload_ok") + "<br>";
  }
  
  int readLine(ServletInputStream in, byte[] b, int off, int len)
    throws IOException {
    if (len <= 0) {
      return 0;
    }
    int count = 0, c;
    while ((c = in.read()) != -1) {
      b[off++] = (byte)c;
      count++;
      if ((c == '\n') || (count==len)) {
        break;
      }
    }
    return count > 0 ? count : -1;
  }
  
  void appendValue(String name, String value, boolean isFile) {
    UploadArtifact data=new UploadArtifact(name, value, isFile);
    table.put(name, data);
  }
    
  String getValue(String name) {
    UploadArtifact data=(UploadArtifact) table.get(name);
    if (data==null)
      return null;
    return data.value;
  }

  boolean fileAccessIsAllowed(String filename, Student student) {
    // this method checks to make sure that the target directory in filename is
    // in the portion of the server directory tree permitted for uploads
    boolean debugMode = true;
    try {
      File uploadFile = new File(filename).getCanonicalFile();
      File parentDirectory = uploadFile.getParentFile();
      File allowedDirectory;
      if (student.getIsInstructor()) allowedDirectory = new File(Course.uploadsDirectory).getCanonicalFile();
      else allowedDirectory = new File(Course.uploadsDirectory + student.getIDNumber()).getCanonicalFile();
      if (parentDirectory.equals(allowedDirectory)) return true;
      else {  // recursion checks farther up the tree
        if (fileAccessIsAllowed(parentDirectory.getCanonicalPath(),student)) {
          if (!parentDirectory.exists()) parentDirectory.mkdir();
          return true;
        }
        else return false;
      }
    }
    catch (Exception e) { // exception most likely means that filename is out of bounds
      return false;
    }
  }
}

class UploadArtifact {
  String name;
  String value;
  boolean isFile;

  UploadArtifact(String name, String value, boolean isFile) {
    this.name=name;
    this.value=value;
    this.isFile=isFile;
  }
}

class Artifact {
  RBStore res = EledgeResources.getPortfolioBundle();
  int portfolioID;
  int artifactNumber;
  String artifactURL = res.getString("str_enter_af_url");
  String title = res.getString("str_my_new_af");
  String description = res.getString("str_new_desc");
  String imgURL = "";
  String imgAlt = res.getString("str_new_alt");
  
  void load(ResultSet rsContents) {
    try {
      portfolioID = rsContents.getInt("PortfolioID");
      artifactNumber = rsContents.getInt("ArtifactNumber");
      artifactURL = rsContents.getString("ArtifactURL");
      title = rsContents.getString("Title");
      description = rsContents.getString("Description");
      imgURL = rsContents.getString("ImgURL");
      imgAlt = rsContents.getString("ImgAlt");
    }
    catch (Exception e) {
    }
  }
  
  String display() {
    StringBuffer buf = new StringBuffer();
    if (this.isDefault()) return "<TD></TD>";
    if (this.imgURL.length()>0 && !this.imgURL.equals("null"))
      buf.append("<TD VALIGN=TOP><A HREF='" + this.artifactURL + "'><IMG WIDTH=75 SRC='" 
      + this.imgURL + "' ALT='" + this.imgAlt + "' BORDER=0></A></TD><TD>");
    else buf.append("<TD COLSPAN=2>");
    buf.append("<H3>" + (this.artifactURL.length()>0?"<a href='" + this.artifactURL + "'>" 
    + this.title + "</a>":this.title) + "</H3>" + this.description + "<p></TD>");
    return buf.toString();
  }
  
  String edit(int i) {
    return "<TR><TD COLSPAN=2><HR></TD></TR>"
      + "<TR><TD COLSPAN=2>"
      + (isDefault()?res.getString("str_edit_fields"):
      "<INPUT TYPE=CHECKBOX NAME='" + i + ":DeleteMe' VALUE=true>" + res.getString("str_del_af")) 
      + "</TD></TR>"
      + "<TR><TD><a href='/servlet/" + Course.name + ".Portfolio?UserRequest=Uploads'>"
      + "<IMG WIDTH=75 SRC='" + this.imgURL + "' ALT='" + res.getString("str_click_to_upload") + "'></a><br>"
      + res.getString("str_img_url") + "<INPUT NAME='" + i + ":ImgURL' VALUE='" + this.imgURL + "'><br>"
      + res.getString("str_alt") + "<INPUT NAME='" + i + ":ImgAlt' VALUE='" + CharHider.quot2html(this.imgAlt) + "'></TD>"
      + "<TD>" + res.getString("str_title") + " <INPUT SIZE=50 NAME='" + i + ":ArtifactTitle' VALUE='" 
      + CharHider.quot2html(this.title) + "'><br>"
      + res.getString("str_url") + " <INPUT SIZE=50 NAME='" + i + ":ArtifactURL' VALUE='" + this.artifactURL + "'><br>"
      + "<TEXTAREA NAME='" + i + ":ArtifactDescription' WRAP=SOFT ROWS=4 COLS=50>"
      + CharHider.quot2html(this.description) + "</TEXTAREA></TD></TR>"; 
  }
  
  String selectMyFile(Student student,String fieldName) {
    StringBuffer buf = new StringBuffer();
    File myDirectory = new File(Course.uploadsDirectory + "/" + student.getIDNumber() + "/");
    String uploadsURL = "/" + Course.name + "/uploads/" + student.getIDNumber() + "/";
    if (!myDirectory.exists()) myDirectory.mkdir();
    File[] myFiles = myDirectory.listFiles();
    if (myFiles.length>0) {
      String fileURL="";
      buf.append("<SELECT Name=selectMyFile onChange='" + fieldName + "'>"
      + "<OPTION VALUE='' SELECTED>" + res.getString("str_select_file") + "</OPTION>");
      for(int i=0;i<myFiles.length;i++) {
        fileURL = uploadsURL + myFiles[i].getName();
        buf.append("<OPTION VALUE='" + fileURL + "'>" + myFiles[i].getName() + "</OPTION>");
      }
      buf.append("</SELECT>");
      return buf.toString();
    }
    else return "";
  }
  
  boolean isDefault() {
    Artifact defaultArtifact = new Artifact();
    if (!this.artifactURL.equals(defaultArtifact.artifactURL)) return false;
    if (!this.title.equals(defaultArtifact.title)) return false;
    if (!this.description.equals(defaultArtifact.description)) return false;
    if (!this.imgURL.equals(defaultArtifact.imgURL)) return false;
    if (!this.imgAlt.equals(defaultArtifact.imgAlt)) return false;
    return true;
  }
}
