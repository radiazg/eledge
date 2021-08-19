package Eledge;

/*************************************************************
 * This servlet provides the interface for instructors
 * to modify TA Permissions, set up default permission levels,
 * and more. Yay. ;) Anyway.
 * Author: Robert Zeigler
 * Started: 8/1/03
 * Last Modified: 8/6/03
 ************************************************************/

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.sql.*;

public class ManageTAPermissions extends HttpServlet {

  RBStore res = EledgeResources.getManageTAPermissionsBundle();
  Logger log = new Logger();
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    log.paranoid("Sending http request to doPost","ManageTAPermissions:doGet");
    doPost(request, response);
    log.paranoid("Request sent.","ManageTAPermissions:doGet");
  }
  
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageTAPermissions");
    }
    if (student.getIsFrozen()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+res.getString("str_act_frozen"),student));
      return;
    }

    if (!student.getIsInstructor() || student.getIsTA()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+res.getString("str_instructor_only"),student));
      return;
    }

    String userRequest=request.getParameter("UserRequest");
    log.paranoid(student.getIDNumber() + " made a request of " + userRequest,"ManageTAPermissions:doPost");
    //if the userRequest is null, create the "default" page, listing the first ta.
    if (userRequest == null) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+defaultPage(),student));
    }
    ///if the userRequest is SwitchTA, create another page like the "default" page, but this time modifying
    //the selected TA
    else if (userRequest.equals("SwitchTA")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+taPage(request),student));
    }
    //if the userRequest is AssignSection, we assign the section, and display the permissions page again for the ta
    else if (userRequest.equals("AssignSection")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+assignSection(request) + taPage(request),student));
    }
    //if the userRequest is Drop section, we drop the section, and display the permissions page again for the ta
    else if (userRequest.equals("DropSection")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+dropSection(request) + taPage(request),student));
    }
    //if the userRequest is Assign, we assign the student and display the permissions page again for the ta.
    else if (userRequest.equals("Assign")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+assignStudent(request) + taPage(request),student));
    }
    //if the userRequest is Drop, we drop the student and display the permissions page again for the ta.
    else if (userRequest.equals("Drop")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+dropStudent(request) + taPage(request),student));
    }
    //if the userRequest is update, we update the profile permissions for the current TA
    else if (userRequest.equals("Update")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+updateTA(request) + taPage(request),student));
    }
    //if the userRequest is CreateDefault, we take this permission set (minus the students), 
    //and create a "default" profile with the name provided by the "pop-up" box.
    else if (userRequest.equals("CreateDefault")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+createDefaultProfile(request) + taPage(request),student));
    } 
    //if the request is SetDefault, we will set the current ta to the selected "default"
    //permission set
    else if (userRequest.equals("SetDefault")) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home?curso=0'>Home</a> >> Permisos</em><br><br>"+setDefaultProfile(request) + taPage(request),student));
    }
    //if none of those things, we print an error message and return
    else 
      out.println(res.getString("str_err_bad_input"));
    return;
  }

  private String defaultPage() {
    //"creates" the default page. Actually, the page itself is
    //created by the displayTA(TA) page. This just figures out which TA to display by default.
    TA ta=null;
    String permissionSet="";
    try {
      //get a connection to the db.
      Connection con = getDBConnection();
      Statement stmt = con.createStatement();
      //query the TAPermissions table for the TA studentid's, joined with the Students table
      //and sort by Students.LastName, Students.FirstName, order asc, limit 1
      String sql = "SELECT StudentIDNumber FROM TAPermissions ORDER BY StudentIDNumber LIMIT 1"; 
      log.paranoid("Executing: " + sql,"ManageTAPermissions:defaultPage");
      ResultSet rs = stmt.executeQuery(sql);
      //load the selected TA, if any found. If none found, ta=null...
      if (!rs.next()) {
	log.paranoid("Um... didn't find any ta's. . . ","ManageTAPermissions:defaultPage");
        ta=null;
      } else { //pass the TA to "displayTA" page and return the string.
        ta = TAS.getTA(rs.getString("StudentIDNumber"));
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.paranoid("Caught: " + e.getMessage(), "ManageTAPermissions:defaultPage",e);
      return e.getMessage();
    }
    return displayTA(ta, permissionSet);
 }

  private String taPage(HttpServletRequest request) {
    String taID = request.getParameter("TAID");
    String permissionSet=request.getParameter("PermissionSet");
    if (permissionSet==null)
      permissionSet="";
    if (taID!=null) {
      TA ta = TAS.getTA(taID);
      return displayTA(ta,permissionSet);
    }
    else return displayTA(null,permissionSet);
  }

  String assignSection(HttpServletRequest request) {
    String ret="";
    //must get the section to be assigned.
    String newSection = request.getParameter("Section");
    //if null, inform them of their mistake. 
    if (newSection == null) {
      return res.getString("str_must_select");
    }

    //Then we get the TA to assign to.
    String taID = request.getParameter("TAID");
    //Then we create the TA object.
    TA ta = TAS.getTA(taID);
    //Then we assign the section, if it's not already assigned.
    if (!ta.isAssigned("Section",newSection)) {
      ret = ta.addAssignment("Section",newSection);
    }
    //return any errors.
    return ret;
  }

  String dropSection(HttpServletRequest request) {
    //need a return String.
    String ret="";
    //get the taID and the sectionID
    String sectionID = request.getParameter("Section");
    if (sectionID==null) {
      return res.getString("str_must_select");
    }
    String taID = request.getParameter("TAID");
    //create the ta object.
    TA ta = TAS.getTA(taID);
    ret = ta.removeAssignment("Section",sectionID);
    return ret;
  }

  private String assignStudent(HttpServletRequest request) {
    //get taid and student id to assign.
    String ret="";
    String taID = request.getParameter("TAID");
    String[] studentID = request.getParameterValues("Student");
    if (studentID == null) {
      return res.getString("str_must_select");
    }
    //create the ta object and assign the section, if not already assigned
    TA ta = TAS.getTA(taID);
    for (int i=0; i<studentID.length; i++) {
      if (!ta.isAssigned("Student",studentID[i])) {
       ret += ta.addAssignment("Student",studentID[i]);
       ret += "\n<BR>";
      }
    }
    return ret;
  }

  private String dropStudent(HttpServletRequest request) {
    String ret = "";
    String taID = request.getParameter("TAID");
    String studentID[] = request.getParameterValues("Student");
    if (studentID == null) {
      return res.getString("str_must_select");
    }
    TA ta = TAS.getTA(taID);
    for (int i=0; i<studentID.length; i++) {
      ret += ta.removeAssignment("Student",studentID[i]);
      ret +="\n<BR>";
    }
    return ret;
  }

  private String updateTA(HttpServletRequest request) {
    String ret = "";
    //ok, the name of each input on the page is the same as it's
    //keyname, so.... first we get the taID....
    String taID=request.getParameter("TAID");
    //anc create a ta object..
    TA ta = TAS.getTA(taID);
    //then we get the key names...
    String[] keys = Permissions.getKeys();
    //now, we loop through all of the keys.
    for (int i=0; i<keys.length;i++) {
      //for each key, we attempt to pull out the appropriate information.
      String value=request.getParameter(keys[i]);
      if (value != null && !value.equals("")) {
      //then we make a new TAPermission
        TAPermission tap = new TAPermission(taID,keys[i],value);
      //then we update the ta with this new tapermission object...
        ta.setPermission(tap);
      }
    }
    //now we update the permission table for this ta...
    ret = ta.updatePermissionTable();
    //and we're done! =)
    return ret;
  }
//hm. I really don't want this here, I think, because it doesn't
//separate well into MVC if I put it here. But... oh well. ;)
  private String createDefaultProfile(HttpServletRequest request) {
    String ret="";
    try {
      //first, we get the default profile's name. We should check to make sure this doesn't already exist!
      String defaultName = request.getParameter("DefaultProfileName");
      if (defaultName == null) {
        return res.getString("str_must_enter_defaultname");
      }
      //now we load the ta. The first "true" is that it's true it's a default permission,
      //and the second true is that yes, this is a new profile
      TA ta = new TA(defaultName,true,true);
      TA ta2=null;
      String taID = request.getParameter("TAID");
      if (taID==null) {
        taID="";
      } else {
        ta2 = TAS.getTA(taID);
      }
      //check to see if exists... -do- use the TAPermission table (instead of the student table...)

      if (ta.lookupTA(defaultName, true)) {
        return res.getString("str_err_default_exists");
      }

      //get the keys
      String[] keys = Permissions.getKeys();
      for (int i=0; i<keys.length; i++) {
        //for each key, we attempt to pull out the appropriate information.
        String value=request.getParameter(keys[i]);
        //because all of our permissions are split up now, 
        //we have to check for null permission...
        //if the permission is null, and the taID is "", then,
        //this we have -nothing- to go on! ;) so, make the 
        //permission "PERM_STUDENT". Buuuutttt...
        //if taid ain't "" and ta ain't null, 
        //then, we can get the permission from the already loaded ta! haha! nifty. ;)
        if (value == null) {
          if (taID.equals("") || ta2==null) 
            value=TAPermission.PERM_STUDENT;
          else
            value=ta2.getPermission(keys[i]).getPermissionLevel();
        }
        //then we make a new TAPermission
        TAPermission tap = new TAPermission(defaultName,keys[i],value);
        //then we update the ta with this new tapermission object...
        ta.setPermission(tap);
      }
      ret=ta.insertPermissions();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"ManageTAPermissions:createDefaultProfile",e);
      ret += e.getMessage();
    }
    return ret;
  }
 
  private String setDefaultProfile(HttpServletRequest request) {
    String taID = request.getParameter("TAID");
    String defaultProfile = request.getParameter("DefaultTA");
    TA defaultTA = TAS.getTA(taID);
    TA ta = TAS.getTA(defaultProfile);
    String[] keys = Permissions.getKeys();
    for (int i=0;i<keys.length;i++) {
      ta.setPermission(defaultTA.getPermission(keys[i]));
    }
    return ta.updatePermissionTable();
  }

  //gets a connection to the database.
  private Connection getDBConnection() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      return con;
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(), "ManageTAPermissions:getDBConnection");
    }
    return null;
  }
  //displays a single ta.
  private String displayTA(TA ta,String permissionSet) {
    StringBuffer buf = new StringBuffer("<div align=center><h2>" + res.getString("str_manage_ta") + "</h2></div><hr>");
    buf.append(layoutTAS(ta));
    //buf.append("<H3>" + res.getString("str_section_info") + "</h3>");
    //buf.append(layoutSections(ta,permissionSet));
    //buf.append("<H3>" + res.getString("str_student_info") + "</h3>");
    //buf.append(layoutStudents(ta,permissionSet)); 
    buf.append("<h3>" + res.getString("str_permission_info") + "</h3>");
    buf.append(layoutPermissions(ta,permissionSet));
    return buf.toString();
  }

  //spits out the select box for switching between ta's.
  private String layoutTAS(TA ta) {
    String taID;
    if (ta ==null) {
      taID="";
    } else
      taID=ta.getID();

    StringBuffer buf = new StringBuffer("<FORM METHOD=POST ACTION=''>");
    String[] studs = TA.getTAS();
    if (studs.length > 0) {
      buf.append(res.getString("str_current_ta") + "<SELECT NAME='TAID' onChange=\"this.form.submit();\">");
      for (int i=0; i<studs.length;i++) {
        buf.append("<OPTION VALUE='" + studs[i] + "'" 
          + (studs[i].equals(taID)?" SELECTED>":">")
          + studs[i] + "</OPTION>");
      }
      buf.append("</SELECT>");
      buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='SwitchTA'>");
    }
    buf.append("</FORM>");
    return buf.toString();
  }

  private String layoutSections(TA ta,String permissionSet) {
    return layoutExistingSections(ta, permissionSet) + layoutOtherSections(ta,permissionSet);
  }

  private String layoutStudents(TA ta, String permissionSet) {
    return layoutExistingStudents(ta, permissionSet) + "<BR><BR>" + layoutOtherStudents(ta, permissionSet);
  }

  private String layoutPermissions(TA ta, String permissionSet) {
    StringBuffer buf = new StringBuffer();
    String[] keys = Permissions.getSortedKeys();
    String currentServlet;
    if (permissionSet.equals(""))
      currentServlet=keys[0];
    else 
      currentServlet=permissionSet;
    buf.append("<FORM METHOD=POST ACTION=''>");
    buf.append(permissionSelectBox(permissionSet));
    //start the table; include a row of headers.
    buf.append("<blockquote><TABLE BORDER=1 CELLSPACING=1 CELLPADDING=1>\n"
      + "<TR><TH>" + res.getString("str_perm_name") + "</TH><TH>"
      + res.getString("str_perm_level") + "</TH><TH>"
      + res.getString("str_perm_description") + "</TH></TR>");
    for (int i=0; i<keys.length; i++) {
      //if it doesn't start with the "current" servlet, we've started into
      //a new servlet...
      if (keys[i].startsWith(currentServlet)) {
        buf.append("<TR>" + displayPermission(ta,keys[i]) + "</TR>");
      }
    }
    buf.append("</TABLE></BLOCKQUOTE>");
    //add the user request, the update button, and the button for
    //creating default profiles.
    //oh, and, we need an explanation of default profiles,
    //and we'll also put in a "set this ta to such and such
    //default" type of thing. ;)
    buf.append("<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='Update'>");
    if (ta !=null )
      buf.append("<INPUT TYPE=HIDDEN NAME='TAID' VALUE='" + ta.getID() + "'>");
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update")
      + "'><P>" + res.getString("str_explain_default") + "</p>"
      + res.getString("str_identifier") + ": <INPUT TYPE=TEXT NAME='DefaultProfileName'>"
      + "&nbsp;<INPUT TYPE=BUTTON onClick=\"if (this.form.elements.DefaultProfileName.value=='') {"
      + "alert('" + res.getString("str_must_enter_defaultname") 
      + "');} else {this.form.UserRequest.value='CreateDefault';this.form.submit();}\""
      + " VALUE='" + res.getString("str_btn_default") + "'>");
    buf.append("<p>" + res.getString("str_explain_setdefault") + "</p>");
    buf.append(defaultTASelectBox() + "&nbsp;<INPUT TYPE=BUTTON VALUE=\""
      + res.getString("str_btn_setdefault") + "\" onClick=\""
      + "this.form.UserRequest.value='SetDefault';this.form.submit();\">");
    buf.append("</FORM>");
    return buf.toString();
  }
//puts in a form w/ a w/ a select box of currently assigned sections,
//if they exist.
  public String layoutExistingSections(TA ta, String permissionSet) {
    StringBuffer buf = new StringBuffer("<FORM METHOD=POST ACTION=''>");
    buf.append("<INPUT TYPE=HIDDEN NAME=PermissionSet Value='" + permissionSet + "'>");
    if (ta==null)//no sections will exist for this ta...
      return "";
    String[] sections = ta.getAssignedSections();
    if (sections.length > 0) {
      buf.append(res.getString("str_assigned_sections") + ": <SELECT NAME='Section'>");
      for (int i=0; i<sections.length;i++) {
        buf.append("\n<OPTION VALUE='" + sections[i] + "'>" + sections[i] + "</OPTION>");
      }
      buf.append("</SELECT>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='DropSection'>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=TAID VALUE='" + ta.getID() + "'>");
      buf.append("\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_dropsection") + "'>");
    }
    buf.append("</FORM>");
    return buf.toString();
  }
  
  public String layoutOtherSections(TA ta, String permissionSet) {
    StringBuffer buf = new StringBuffer("<FORM METHOD=POST ACTION=''>");
    buf.append("<INPUT TYPE=HIDDEN NAME=PermissionSet Value='" + permissionSet + "'>");
    //we don't need to layout any sections for "null" ;)
    if (ta == null) {
      return "";
    }

    String[] sections = ta.getUnAssignedSections();
    if (sections.length > 0) {
      buf.append(res.getString("str_unassigned_sections") + ": <SELECT NAME='Section'>");
      for (int i=0; i<sections.length;i++) {
        buf.append("\n<OPTION VALUE='" + sections[i] + "'>" + sections[i] + "</OPTION>");
      }
      buf.append("</SELECT>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='AssignSection'>");
      buf.append("\n<INPUT TYPE=HIDDEN NAME=TAID VALUE='" + ta.getID() + "'>");
      buf.append("\n<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_addsection") 
        + "'>");
    }
    buf.append("</FORM>");
    return buf.toString();
  }
//creates a table showing all currently assigned students.
//whee. ;)
  public String layoutExistingStudents(TA ta, String permissionSet) {
    if (ta==null) 
      return "";
    StringBuffer buf = new StringBuffer("<FORM METHOD=POST ACTION=''>");
    buf.append("<INPUT TYPE=HIDDEN NAME='PermissionSet' VALUE='" + permissionSet + "'>");
    buf.append(appendJSStudentCheck());
    Student[] students = ta.getAssignedStudents();
    //only create stuff if there's actually stuff to create. =/ ;)
    if (students.length > 0) {
      //start the table...
      buf.append("<TABLE BORDER=1 CELLSPACING=0 CELLPADDING=0>");
      //give it a heading.
      buf.append("<THEAD>" + res.getString("str_assigned_students") + "</THEAD>");
      //and a row of headers...
      //first one is a blank td for the checkbox row
      buf.append("\n<TR><TD></TD><TH>" + res.getString("str_studID") + "</TH><TH>"
        + res.getString("str_stud_name") + "</TH><TH>"
        + res.getString("str_stud_section") + "</TH><TH>"
        + res.getString("str_stud_status") + "</TH></TR>");
      //now put a row in for each student. We'll use checkboxes here 
      //to allow multiple students to be deleted at once...
      for (int i=0; i<students.length;i++) {
        buf.append("\n<TR><TD><INPUT TYPE=CHECKBOX NAME='Student' VALUE='" 
        + students[i].getIDNumber() + "'></TD><TD>"
	+ students[i].getIDNumber() + "</TD><TD>"
        + students[i].getFullName() + "</TD><TD>"
        + students[i].getSectionID() + "</TD><TD>"
        + students[i].getStatusName() + "</TD></TR>");
      }
      buf.append("</TABLE>");//end table.
      //append user request
      buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='Drop'>");
      //apend the TAID
      buf.append("\n<INPUT TYPE=HIDDEN NAME=TAID VALUE='" + ta.getID() + "'>");
      //append the submit button
      buf.append("\n<INPUT TYPE=BUTTON VALUE='" + res.getString("str_btn_dropstudent") 
        + "' onClick=\"if (no_checks(this.form.elements.Student)) {alert('"
        + res.getString("str_must_select") + "');} else {this.form.submit();}\">");
    }
    buf.append("</FORM>");
    return buf.toString();
  }

  public String appendJSStudentCheck() {
    StringBuffer buf = new StringBuffer();
    buf.append("\n<SCRIPT LANGUAGE=Javascript>\n"
    + "<!--\n"
    + "function no_checks(field) {\n"
    + "  for (i=0;i<field.length;i++) {\n"
    + "    if (field[i].checked==true) {\n"
    + "      return false;\n"
    + "    }\n"
    + "  }\n"
    + "  return true;"
    + "}\n"
    + "-->\n"
    + "</SCRIPT>\n");
    return buf.toString();
  }

//creates a "multiple select" list of unassigned students
  public String layoutOtherStudents(TA ta, String permissionSet) {
    if (ta==null) return "";
    StringBuffer buf = new StringBuffer("<FORM METHOD=POST ACTION=''>");
    buf.append("<INPUT TYPE=HIDDEN NAME=PermissionSet VALUE='" + permissionSet + "'>");
    Student[] students = ta.getUnAssignedStudents();
    //only create stuff if there's actually stuff to create. =/ ;)
    if (students.length > 0) {
      //start the list...
      buf.append("<SELECT NAME='Student' MULTIPLE SIZE='5'>");
      //put an option in for each student in the course...
      for (int i=0; i<students.length;i++) {
        buf.append("\n<OPTION VALUE='" + students[i].getIDNumber()
          + "'>" + students[i].getFullName() + "</OPTION>");
      }
      buf.append("</SELECT>");//end SELECT
      //append user request
      buf.append("\n<INPUT TYPE=HIDDEN NAME=UserRequest VALUE='Assign'>");
      //apend the taid
      buf.append("\n<INPUT TYPE=HIDDEN NAME=TAID VALUE='" + ta.getID() + "'>");
      //append the submit button
      buf.append("\n<BR><INPUT TYPE=BUTTON VALUE='" + res.getString("str_btn_addstudent") 
        + "' onClick=\"if (this.form.elements.Student.value=='') {alert('"
        + res.getString("str_must_select") + "');} else {this.form.submit();}\">");
    }
    buf.append("</FORM>");
    return buf.toString();
  }

  String displayPermission(TA ta, String name) {
    Permission p = Permissions.getPermission(name);
    TAPermission tap;
    if (ta == null ) { 
      log.paranoid("ta was null??","ManageTAPermissions:displayPermission");
      tap = new TAPermission("",name,TAPermission.PERM_NONE);
    } else {
      log.paranoid("ta wasn't null.","ManageTAPermissions:displayPermission");
      tap = ta.getPermission(name);
    }
    return "<TD><b>" + tap.getName() + "</b></TD><TD>" + tap.selectBox() + "</TD><TD>" + p.getEditMsg() + "</TD>";
  }

  public String defaultTASelectBox() {
    StringBuffer buf = new StringBuffer("");
    String[] taIDs = TA.getDefaultTAs();
    if (taIDs.length > 0) {
      buf.append("<SELECT NAME='DefaultTA'>");
      for (int i=0;i<taIDs.length;i++) {
        buf.append("<OPTION VALUE='" + taIDs[i] + "'>" + taIDs[i] + "</option>");
      }
      buf.append("</SELECT>");
    }
    return buf.toString();
  }

  private String permissionSelectBox(String permissionSet) {
    StringBuffer buf = new StringBuffer();
    String[] permissionGroups = Permissions.getPermissionGroupNames();
    buf.append("<SELECT NAME='PermissionSet' onChange=\"this.form.submit()\">");
    for (int i=0;i<permissionGroups.length;i++) {
      buf.append("\n<OPTION VALUE='" + permissionGroups[i] 
        + (permissionSet.equals(permissionGroups[i])?"' SELECTED>":"'>") 
        + permissionGroups[i] + "</option>");
    } 
    buf.append("</SELECT>");
    return buf.toString();
  }
}

