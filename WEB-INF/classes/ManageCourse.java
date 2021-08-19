package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageCourse extends HttpServlet {
  int numberOfSections = 1;
  static boolean isLoaded = false;
  Logger log = new Logger();  
  RBStore res = EledgeResources.getManageCourseBundle();
  public String getServletInfo() {
    return "This Eledge servlet allows the instructor to edit and configure course parameters.";
  }

  public ManageCourse() {
    super();
    // The following array is used below to ensure that links to all servlets are provided
    // when the system is upgraded. Be sure to add the name here when new servlets are added.
    // The url willl always be static/english, by virtue of having to be the 
    // same as the class name.
    log.paranoid("Being Method.","ManageCourse:ManageCourse");
    String[] servletsURL = {
      "Calendar","Content","DiscussionBoard","Email","Exam","Help",
      "Home","Login","Logout","Profile","Scores","Syllabus"
    };
    //the linkname, however, can be in whatever language
    log.paranoid("initializing servlets array from resource bundle.","ManageCourse:ManageCourse");
    String[] servlets = {
      res.getString("str_link_calendar"),
      res.getString("str_link_content"),
      res.getString("str_link_discussionboard"),
      res.getString("str_link_email"),
      res.getString("str_link_exam"),
      res.getString("str_link_help"),
      res.getString("str_link_home"),
      res.getString("str_link_login"),
      res.getString("str_link_logout"),
      res.getString("str_link_profile"),
      res.getString("str_link_scores"),
      res.getString("str_link_syllabus")
    };

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM NavBarLink WHERE linkid>0 and linkid<=8 ");
      int j = rsCount.first()?(Integer.parseInt(rsCount.getString("Count(*)"))):0;
      rsCount.close();
      for (int i=0;i<servlets.length;i++) {
        ResultSet rsLink = stmt.executeQuery("SELECT * FROM NavBarLink WHERE URL='" 
        + Course.name + "." + servletsURL[i] + "'");
        if (!rsLink.next()) {
          stmt.executeUpdate("INSERT INTO NavBarLink (LinkID,LinkName,URL) VALUES ('"
          + j + "','" + servlets[i] + "','" + Course.name + "." + servletsURL[i] + "')");
          j++;
        }
      }
      this.isLoaded=true;
    }
    catch (Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"ManageCourse:ManageCourse");
    } 
    log.paranoid("End Method.","ManageCourse:ManageCourse");
  }  

  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin Method.","ManageCourse:doGet");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageCourse");
      return;
    }
    
    if (student.getIsTA()) {
		TA ta = TAS.getTA(student.getIDNumber());
		StringBuffer err = new StringBuffer();
		
		if ((!ta.hasPermission("ManageCourse",request,student,err)) || (student.getCourse_id() == null)) {
			out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+"No esta Autorizado para realizar cambios, solo el Administrador lo puede realizar.",student));
			return;
      	}
      
    }

    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+res.getString("str_must_be_instructor"),student));
      return;
    }
    // from here on, user is assumed to be the instructor

    if (!isLoaded) {
      log.normal("Servlet not yet loaded. Loading.","ManageCourse:doGet");
      new ManageCourse();
    }
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+coursePageForm(student.getCourse_id()),student));
    log.paranoid("End Method.","ManageCourse:doGet");
  }
  
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin Method.","ManageCourse:doPost");
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageCourse");
      return;
    }
    
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      
      if (!ta.hasPermission("ManageCourse",request,student,err)) {
			out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+err.toString(),student));
			return;
      }
    }
    
    if (!student.getIsInstructor()) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+res.getString("str_must_be_instructor"),student));
      return;
    }
    // from here on, user is assumed to be the instructor

    String userRequest = request.getParameter("UserRequest");

    log.paranoid("userRequest is: " + userRequest,"ManageCourse:doPost");	
    
    if (userRequest == null) {
    	out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+coursePageForm(student.getCourse_id()),student));
    } 
    
    //almacena los parametros generales de todos los cursos
    else if (userRequest.equals("SaveParameters")) { // save the main course parameters
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        
        Enumeration enum = request.getParameterNames();
        
        while (enum.hasMoreElements()) {
        	String param = (String)enum.nextElement();
          	if (!param.equals("UserRequest"))
            	stmt.executeUpdate("UPDATE CourseParameters SET "
            	+ "Value='" + CharHider.quot2literal(request.getParameter(param)) + "' WHERE Name='" + param + "'");
        }
      }
      catch (Exception e){
        log.normal("Caught Exception: " + e.getMessage(),"ManageCourse:doPost");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+res.getString("str_update_failed") + e.getMessage(),student));
      }
    } 
    
    //almacena los link de la barra de navegacion para cada curso
    else if (userRequest.equals("SaveNavBarLinks")) { // save new navbar links //per course //a cada curso
      
      try {
	    int numberOfLinks = Integer.parseInt(request.getParameter("NumberOfLinks"));
        int newLink = numberOfLinks;
      
      try {
        newLink = Integer.parseInt(request.getParameter("NewLink"));
      } catch (Exception e2){
        log.normal("Caught Exception (e2): " + e2.getMessage(),"ManageCourse:doPost");
      }
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        
        //elimina todos los registros de la tabla navbar_to_course del curso indicado		
        stmt.executeUpdate("DELETE FROM navbar_to_course WHERE id_course = '"+student.getCourse_id()+"'");
        //almacena nuevamente los datos a la tabla siempre y cuando sea show(true)
        for (int i=0;i<numberOfLinks;i++) {
          if (request.getParameter(i+"Show").equals("true")) {
            stmt.executeUpdate("INSERT INTO navbar_to_course (id,id_course,id_navbarlink) VALUES('','"+student.getCourse_id()+"','"+request.getParameter(i+"linkid")+"') ");
          }
        }
      }
      
      catch (Exception e) {
        log.normal("Caught Exception: " + e.getMessage(),"ManageCourse:doPost");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+res.getString("str_update_failed") + e.getMessage(),student));
      }
    } else if (userRequest.equals("UpdateLogInfo")) {
      log.updateLogInfo(request);
    }
    Page.isLoaded = false; // forces reloading of static Page variables
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Administración de curso</em><br><br>"+coursePageForm(student.getCourse_id()),student));
    if (student.getIsTA()) {
      student.setIsInstructor(false);
    }
    log.paranoid("End Method.","ManageCourse:doPost");
  }
  
  String coursePageForm(String id_course){
    log.paranoid("Begin Method.","ManageCourse:coursePageForm");
    
    StringBuffer buf = new StringBuffer();
    
    if (id_course == null) {
    
    buf.append("<h3>" + Course.name + res.getString("str_title_course_parameters") + "</h3>"
    + "<i>" + res.getString("str_explanation_course_parameters") + "</i>"
    + "<FORM METHOD=POST>\n");
    
    buf.append("<br><TABLE><tr><td ALIGN=LEFT><b>" + res.getString("str_field_param_name")
    + "</b></td><td ALIGN=LEFT><b>" + res.getString("str_field_curr_val")
    + "</b></td></tr>\n");
    
    //Forma de parametros del curso
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCourse = stmt.executeQuery("SELECT * FROM CourseParameters");
      
      log.paranoid("Begin Course Parameters section.","ManageCourse:coursePageForm");
      
      while (rsCourse.next()) {
        buf.append("<tr><td>" + rsCourse.getString("Name") + "</td><td>" 
        + "<INPUT SIZE=40 NAME='" + rsCourse.getString("Name") + "' VALUE='" + rsCourse.getString("Value") + "'></td></tr>\n");
        
        /*if (rsCourse.getString("Name").equals("NumberOfSections")) 
        	numberOfSections = rsCourse.getInt("Value");*/
      }
      rsCourse.close();
    }
    catch (Exception e) {
      log.normal("Caught Exception: " + e.getMessage(),"ManageCourse:coursePageForm");
      return e.getMessage();
    }
    
    buf.append("</TABLE>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=SaveParameters>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_save_course_param")
    + "'><INPUT TYPE=RESET VALUE='" + res.getString("str_btn_restore_original")
    + "'></FORM>\n");
    
    log.paranoid("End Course Parameters Section. Beginning Reset Course.","ManageCourse:coursePageForm"); 
    
    //Forma de resetear base de datos del curso
    buf.append("<HR><FORM METHOD=GET ACTION=" + Course.name + ".ResetCourse>"
    + "<h3>" + res.getString("str_title_reset_course") + "</h3>" 
    + res.getString("str_explanation_reset_course") + "<br>"
    + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_reset_course") + "'></FORM>");
    
    log.paranoid("End Reset Course section. Beginning Course Sections","ManageCourse:coursePageForm");
    
        
    }
    
    if (id_course != null) {
    
    buf.append("<hr><h3>" + Course.name + res.getString("str_title_navbar") + "</h3>"
    + "<i>" + res.getString("str_explanation_navbar") + "</i>"
    + "<FORM METHOD=POST>");

    buf.append("<br><TABLE><tr><td><b>" + res.getString("str_show")
    + " | " + res.getString("str_hide") + "</b></td><td><b>"
    + res.getString("str_link_num") + "</b>"
    + "</td><td ALIGN=LEFT><b>" + res.getString("str_field_name")
    + "</b></td><td ALIGN=LEFT><b>" + res.getString("str_field_url")
    + "</b></td></tr>\n");
    
    //forma de barra de navegacion
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //muestra la cantidad total de registros que hay en la tabla
      ResultSet rsLinkCourse = stmt.executeQuery("SELECT COUNT(*) FROM navbar_to_course");
      
      rsLinkCourse.next(); 
      //crea la matriz con la cantidad total de registros 
      int[] NavBar = new int[rsLinkCourse.getInt("COUNT(*)")];
      
      //muestra todos los registros de la tabla navbar_to_course 
      rsLinkCourse = stmt.executeQuery("SELECT * FROM navbar_to_course WHERE id_course ="+id_course);  
      
      int i = 0;
      while(rsLinkCourse.next()) {
      	//llena la matriz con los datos encontrados de la tabla navbar_to_course
      	NavBar[i] = rsLinkCourse.getInt("id_navbarlink");
      	i++;
      }
      
      //muestra todos los datos disponibles de navbarlink
      rsLinkCourse = stmt.executeQuery("SELECT  * FROM navbarlink WHERE ( linkid >=3 AND linkid <9 ) OR ( linkid >=17 AND linkid <19 ) ORDER  BY linkid ASC");
      int nLink = 0;
      boolean showLink;
      while (rsLinkCourse.next()) { // build a table of links from the database
        showLink = false;
        for(i=0; i<NavBar.length; i++ ){
        	if(NavBar[i] == rsLinkCourse.getInt("linkid")){
    			showLink = true;		        		
        	}
        }
        buf.append("<tr><td ALIGN=CENTER>"
        + "<INPUT TYPE=RADIO NAME='" + nLink + "Show' VALUE=true " + (showLink?"CHECKED>":">") 
        + "<INPUT TYPE=RADIO NAME='" + nLink + "Show' VALUE=false " + (showLink?">":"CHECKED>") + "</td>"
        + "<td>" + nLink + "</td>"
        + "<td><LABEL>"+ CharHider.quot2html(rsLinkCourse.getString("name")) +"</LABEL></td>"
        + "<td><LABEL>"+ rsLinkCourse.getString("url") +"</LABEL>"
        + "<INPUT TYPE='hidden' NAME='" + nLink + "linkid' VALUE='"+rsLinkCourse.getString("linkid")+"'></td></tr>\n");
        nLink++;
        
      }
      
      /*buf.append("<tr>"  // add form fields for 1 extra link
      + "<td ALIGN=CENTER><INPUT TYPE=RADIO NAME='" + nLink + "Show' VALUE=true CHECKED>"
      + "<INPUT TYPE=RADIO NAME='" + nLink + "Show' VALUE=false></td>"
      + "<td><INPUT SIZE=2 NAME=NewLink VALUE='" + nLink + "'></td>"
      + "<td><INPUT SIZE=12 NAME='" + nLink + "Name'></td>"
      + "<td><INPUT SIZE=40 NAME='" + nLink + "URL'></td></tr>\n");
      nLink++;*/
      
      buf.append("</TABLE>"
      + "<INPUT TYPE=HIDDEN NAME=NumberOfLinks VALUE='" + nLink + "'>" // record number of links
      + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=SaveNavBarLinks>"
      + "<br><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_links")
      + "'><INPUT TYPE=RESET VALUE='" + res.getString("str_btn_restore_original")
      + "'></FORM>\n");
    }
    catch (Exception e) {
      log.normal("Caught Exception: " + e.getMessage(),"ManageCourse:coursePageForm");
      return e.getMessage();
    }
	
	}
	
	if (id_course == null) {
	//forma de logs
    buf.append("<hr>" + log.displayLogInfo());
    }
    
    return buf.toString();
  }

  /*String createSectionTable() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE CourseSections (SectionID INT PRIMARY KEY AUTO_INCREMENT,SectionName TEXT,Description TEXT)");
      stmt.executeUpdate("INSERT INTO CourseSections (SectionID,SectionName,Description) VALUES (1,'1','Default Section')");
      return coursePageForm();
    }
    catch (Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"ManageCourse:createSectionTable");
      return e.getMessage();
    }
  }*/
}

