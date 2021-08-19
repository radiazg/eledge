
package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;

public class ManageNavBar extends HttpServlet {
  int numberOfSections = 1;
  static boolean isLoaded = false;
  Logger log = new Logger();  
  RBStore res = EledgeResources.getManageCourseBundle();
  public String getServletInfo() {
    return "This Eledge servlet allows the instructor to edit and configure course parameters.";
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
		
		if (!ta.hasPermission("ManageCourse",request,student,err)) {
			out.println(Page.create(err.toString(),student));
			return;
      	}
    }

    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_instructor"),student));
      return;
    }
    // from here on, user is assumed to be the instructor

    if (!isLoaded) {
      log.normal("Servlet not yet loaded. Loading.","ManageCourse:doGet");
      new ManageCourse();
    }
    out.println(Page.create(coursePageForm(student.getCourse_id()),student));
    log.paranoid("End Method.","ManageCourse:doGet");
  }
  
  
 String coursePageForm(String id_course){
	StringBuffer buf = new StringBuffer();
	
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
      rsLinkCourse = stmt.executeQuery("SELECT * FROM navbarlink WHERE name!='home' and name!='my profile' and name!='logout' and name!='login' and name!='my courses' ORDER BY LinkID");
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
      //+ "<INPUT TYPE=HIDDEN NAME=NumberOfLinks VALUE='" + nLink + "'>" // record number of links
      + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=SaveNavBarLinks>"
      + "<br><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_update_links")
      + "'><INPUT TYPE=RESET VALUE='" + res.getString("str_btn_restore_original")
      + "'></FORM>\n");
    }
    catch (Exception e) {
      log.normal("Caught Exception: " + e.getMessage(),"ManageCourse:coursePageForm");
      return e.getMessage();
    }
    
return buf.toString();
}

}