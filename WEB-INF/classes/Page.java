package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;

public class Page extends HttpServlet {

  
  static String header1 = "<HTML><head>"
   + "<link rel=stylesheet href="+ Course.server +"/ccs/estiloazul.css type=text/css>" 
  + "<title>" + Course.name + "</title>"
  + "<META HTTP-EQUIV=pragma CONTENT=no-cache>\n";
  static String body;
  private static String body_editor;
  static String header2;
  static String navigationBar;
  static String trailer;
  static boolean isLoaded = false; //activa nuevamente el constructor
  static Template tmplate = new Template();
  Logger log = new Logger();
  RBStore res = EledgeResources.getPageBundle(); 
  public String getServletInfo() {
    return "This Eledge class composes a uniform page look, feel and navigation for servlets.";
  }
  
  public Page() {
    super();
    if (isLoaded) return;
    String txtAnchorTop = "<a href='http://www.utah.edu'>University of Utah</a>";
    String txtAnchorBottom = "<a href=http://www.utah.edu/disclaimer/>Disclaimer</a>";
    String cNavCellColor = "#FFCC33";   // default is "#FFCC33";
    //image should be about 90 pixels wide (e.g., institutional logo linked to home page)
    String imgAnchor = "<a href=http://www.utah.edu><img src="+Course.server+"/tomcat.gif BORDER=0></a>";
    String cBgColor = "#FFFFFF"; //background color; default is #FFFFFF
    String cAlinkColor = "#FF0000"; //default is html default
    String cVlinkColor = "#800080"; //default is html default 
    String cLinkColor = "#0000FF"; //default is html default 
    String imgNavBg   = "none"; //default is none. 
    String Basefont   = "Times New Roman"; //default is TNR.
    String cTitle = Course.name;  //title to be displayed for the course.
                                  //default is Course.name, but, 
                                  //we'll try to override that with the title
                                  //stored in the class database.
    //se quito por diseño
    //StringBuffer buf = new StringBuffer("<tr><td ALIGN=CENTER>");
    StringBuffer buf = new StringBuffer("");
    StringBuffer debug = new StringBuffer("DEBUG:<br>");
      
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      String imgLink = "<a href='http://www.utah.edu'>";
      debug.append("ImgLink...<br>");
      ResultSet rsImg = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='ImgLink'");
      if (rsImg.next()) imgLink = "<a href='" + rsImg.getString("Value") + "'>";
      String imgSrc = "<img src='http://www.utah.edu/images/uu/uofu.gif' BORDER=0";
      debug.append("ImgSrc...<br>");
      rsImg = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='ImgSrc'");
      if (rsImg.next()) imgSrc = "<img src='" + rsImg.getString("Value") + "' BORDER=0";
      String imgAlt = " ALT='University of Utah'></a>";
      rsImg = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='ImgAlt'");
      if (rsImg.next()) imgAlt = " ALT='" + rsImg.getString("Value") + "'></a>";
      imgAnchor = imgLink + imgSrc + imgAlt;
      
      ResultSet rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='AnchorTop'");
      if (rsText.next()) txtAnchorTop = rsText.getString("Value");
      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='AnchorBottom'");
      if (rsText.next()) txtAnchorBottom = rsText.getString("Value");
      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NavCellColor'");
      if (rsText.next()) cNavCellColor = rsText.getString("Value");
      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='BgColor'");
      if (rsText.next()) cBgColor = rsText.getString("Value");
      else addParam("BgColor", cBgColor);

      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='LinkColor'");
      if (rsText.next()) cLinkColor = rsText.getString("Value");
      else addParam("LinkColor",cLinkColor );

      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='AlinkColor'");
      if (rsText.next()) cAlinkColor = rsText.getString("Value");
      else addParam("AlinkColor", cAlinkColor);

      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE name='VlinkColor'");
      if (rsText.next()) cVlinkColor = rsText.getString("Value");
      else addParam("VlinkColor", cVlinkColor);

      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE name='NavBgImg'");
      if (rsText.next()) imgNavBg = rsText.getString("Value");
      else addParam("NavBgImg", imgNavBg);

      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE name='Basefont'");
      if (rsText.next()) Basefont = rsText.getString("Value");
      else addParam("Basefont", Basefont);
      rsText = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE name='Title'");
      if (rsText.next()) cTitle = rsText.getString("Value");
	  
	  //se quito la barra de navegacion de aqui
    
      //body para todas las paginas
      body = "</head>\n"
      + "<BODY class=normal BGCOLOR=" + cBgColor + " LINK=" + cLinkColor + " ALINK=" + cAlinkColor + " VLINK=" + cVlinkColor + ">";
      //body especial para inicializar el editor HTML
      body_editor = "</head>\n"
      + "<BODY onload='initEditor()' class=normal BGCOLOR=" + cBgColor + " LINK=" + cLinkColor + " ALINK=" + cAlinkColor + " VLINK=" + cVlinkColor + ">";
      
      header2 = " "
      + "<BASEFONT face=\"" + Basefont + "\">"
      + "<FONT face=\"" + Basefont + "\">" //basefont and font both in for better multi-browser support. 
     
      //los td width deben estar del mismo tamaño para que funcionen el la barra de navegacion
      + "<table BORDER=0><tr VALIGN=TOP><td width='90'>\n"
      + "<table BORDER=0 CELLSPACING=0 BGCOLOR=#FFFFFF><tr><td width='90'>" + imgAnchor
      + "<br><font size=-2>" + txtAnchorTop + "</font></center>"
      + "</td></tr><tr><td><center><hr><b>" + cTitle + "</b><hr></center>"
      + "</td></tr>\n";
	  
	  trailer = "\n</td></tr></table>\n"
      + "<hr><font size=-2 face=Arial,Helvetica><center>\n"
      + "<a href=http://eledge.org/>" + res.getString("str_eledge_system") + "</a>. "
      + res.getString("str_eledge_copyright") 
      + "<a href=http://www.utah.edu>" + res.getString("str_eledge_cr_holder") + "</a>.<br>"
      + res.getString("str_distributed") 
      + " <a href=http://sourceforge.net/projects/eledge>"
      + res.getString("str_free_dl") + "</a> " + res.getString("str_terms")
      + " <a href=http://www.gnu.org/copyleft/gpl.html>"
      + res.getString("str_gpl") + "</a>."
      + "</center></font></body></html>";
    
      this.isLoaded = true;
    }
    catch (Exception e) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE TABLE WebPages (Section INT,PageID TEXT,Heading TEXT,HTMLText TEXT,CourseSection VARCHAR(3))");
        stmt.executeUpdate("CREATE TABLE CourseParameters (Name TEXT,Value TEXT)");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('Title','Ing.Sistemas')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AllowNewProfiles','true')");
	stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AllowProfileEditing','true')");
        //stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NumberOfSections','1')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgLink','http://www.utah.edu')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgSrc','http://www.utah.edu/images/uu/uofu.gif')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgAlt','University of Utah')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AnchorTop','<a href=http://www.utah.edu>University of Utah</a>')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AnchorBottom','<a href=http://www.utah.edu/disclaimer/>Disclaimer</a>')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NavCellColor','#FFCC33')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('BgColor','#FFFFFF')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AlinkColor','#FF0000')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('VlinkColor','#800080')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('LinkColor','#0000FF')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NavBgImg', 'none')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('Basefont', 'Arial')");
//        stmt.executeUpdate("CREATE TABLE CourseSections (SectionID INT PRIMARY KEY AUTO_INCREMENT,SectionName TEXT,Description TEXT)");
//        stmt.executeUpdate("INSERT INTO CourseSections (SectionID,SectionName,Description) VALUES (1,'1','Default Section')");
//        stmt.executeUpdate("CREATE TABLE NavBarLinks (LinkID INT,LinkName TEXT,ShowLink TEXT,URL TEXT)");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (1,'Home','true','" + Course.name + ".Home')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (2,'Syllabus','true','" + Course.name + ".Syllabus')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (3,'My Profile','true','" + Course.name + ".Profile')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (4,'Content','true','" + Course.name + ".Content')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (5,'Quiz','true','" + Course.name + ".Quiz')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (6,'Exam','true','" + Course.name + ".Exam')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (7,'Homework','true','" + Course.name + ".Homework')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (8,'Report','true','" + Course.name + ".Report')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (9,'Journal','true','" + Course.name + ".Journal')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (10,'Reviews','true','" + Course.name + ".PeerReview')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (11,'Email','true','" + Course.name + ".Email')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (12,'Calendar','true','" + Course.name + ".Calendar')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (13,'DiscussionBoard','true','" + Course.name + ".DiscussionBoard')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (14,'Scores','true','" + Course.name + ".Scores')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (15,'Login','true','" + Course.name + ".Login')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (16,'Logout','true','" + Course.name + ".Logout')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (17,'Help','true','" + Course.name + ".Help')");
//        stmt.executeUpdate("INSERT INTO NavBarLinks VALUES (18,'Instructor','true','http://eledge.org')");
       }
      catch (Exception e2) {
      }
    }
  }

  static String create(String contents) {
    if (!Page.isLoaded) new Page();
    return header1 + body + header2 + contents + trailer;
  }
  
  static String create(String headerContents,String bodyContents) {
    if (!Page.isLoaded) new Page();
    return header1 + body + headerContents + header2 + bodyContents + trailer;
  }
  
  //modificado para los frames
  static String Frames(Student student) {
  	if (!Page.isLoaded) new Page();
    return header1 + body + header2 + NavBar(student) + "</BODY></HTML>";
  }
  
  //mofificado a student para el body del editor
  static String create_editor(String contents,Student student) {
    if (!Page.isLoaded) new Page();
    return header1 + body_editor + header2 + NavBar(student) + contents + trailer;
  
  }
  
 //modificado a student
  static String create(String contents,Student student) {
    if (!Page.isLoaded) new Page();
    return header1 + body + header2 + NavBar(student) + contents + trailer;
  }
  
  //mofificado a student
  static String create(String headerContents,String bodyContents,Student student) {
    if (!Page.isLoaded) new Page();
    return header1 + body + headerContents + header2 + NavBar(student) + bodyContents + trailer;
  }
  
  static String NavBar(Student student){
  	
  	  StringBuffer buf = new StringBuffer();
  	  
  	  try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
  	  
  	  //*** aqui empieza impresion de barra de navegacion
      //barra de navegacion, encabezado de tabla
      String openRowString = "<tr width=10><td class=barnav-left><a class=navbar-over href=";
      String openRowString2 = "<tr width=10><td class=barnav-left>";
      String closeRowString = "</a></td></tr>\n";
      
      ResultSet rsLinks;
            
      if ((student.getCourse_id() == null) && (!student.getIsTA() && student.getIsInstructor())) {
      	rsLinks = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid = 21 or linkid = 22 or linkid = 27 ORDER BY linkid ASC");
      	buf.append("<tr><td class=barnav-title>Administración</td></tr>");
      	buf.append("<tr><td class=barnav-left-top></td></tr>");
      	while(rsLinks.next()) 
      		buf.append(openRowString2 + rsLinks.getString("iconurl") + "<a class=navbar-over href=" + rsLinks.getString("url") + ">" + rsLinks.getString("name") + closeRowString);
      	
      buf.append("<tr><td class=barnav-left-top></td></tr>");
      }
      
      
      if (student.getCourse_id() != null) {
      
      //titulo menu del curso
      buf.append("<tr><td class=barnav-title>Menú del Curso</td></tr>");
      buf.append("<tr><td class=barnav-left-top></td></tr>");
      
      //imprime solo el link de home 
      rsLinks = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid = 20");
      if(rsLinks.next()){
      	buf.append(openRowString2 + rsLinks.getString("iconurl") + "<a class=navbar-over href=" + rsLinks.getString("url") + ">" + rsLinks.getString("name") + closeRowString);
      }
      
      //barra de navegacion, de menú de inicio
      rsLinks = stmt.executeQuery("SELECT * FROM courses, navbar_to_course, navbarlink WHERE id_course = sectionid AND id_navbarlink = linkid AND id_course = "+student.getCourse_id()+" ORDER BY linkid");
      
      while (rsLinks.next()) {
        //if (rsLinks.getBoolean("ShowLink"))
        buf.append(openRowString2 + rsLinks.getString("iconurl") + "<a class=navbar-over href=" + rsLinks.getString("url") + ">" + rsLinks.getString("name") + closeRowString);
      }
      buf.append("<tr><td class=barnav-left-top></td></tr>");
      }
     //***aqui termina impresion de tabla de navegacion
      
      
      //***
      //nueva tabla de navegacion para instructor(admin)
	  //***
	  if (!student.getIsTA() && student.getIsInstructor() && student.getCourse_id()!=null) {
	  buf.append("<tr><td><p>&nbsp;</p></td></tr>");
	  buf.append("<tr><td class=barnav-title>Administración del Curso</td></tr>");
	  
	  buf.append("<tr><td class=barnav-left-top></td></tr>");
	  
	  buf.append(openRowString2+"<img src='../Images/icons/herramientas.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageCourse>Herramientas"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/gradebook.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".Gradebook>Lista de Notas"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/archivos.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageFiles>Archivos"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/estudiantes.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageCursos?UserRequest=AdminStudent>Estudiantes"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/profesores.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageCursos?UserRequest=AdminInstructor>Profesores"+closeRowString);
      
      buf.append("<tr><td class=barnav-left-top></td></tr>");
      }
      
      //***
      //nueva tabla de navegacion para TA(profesor)
	  //***
	  if (student.getIsTA() && student.getCourse_id()!=null && !student.getTAtoStudent()) {
	  buf.append("<tr><td><p>&nbsp;</p></td></tr>");
	  buf.append("<tr><td class=barnav-title>Administración del Curso</td></tr>");
	  
	  buf.append("<tr><td class=barnav-left-top></td></tr>");
	  
	  buf.append(openRowString2+"<img src='../Images/icons/herramientas.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageCourse>Herramientas"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/gradebook.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".Gradebook>Lista de Notas"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/archivos.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageFiles>Archivos"+closeRowString);
	  buf.append(openRowString2+"<img src='../Images/icons/estudiantes.gif' align='absmiddle'><a class=navbar-over href="+Course.name+".ManageCursos?UserRequest=AdminStudent>Estudiantes"+closeRowString);
	  
	  buf.append("<tr><td class=barnav-left-top></td></tr>");
	  }
	  
	        
      buf.append("<tr><td></td></tr>");
      
      rsLinks = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid >= 23 and linkid <= 26 ORDER BY linkid ASC");
      	buf.append("<tr><td><p>&nbsp;</p></td></tr>");
      	
      	buf.append("<tr><td class=barnav-title>Institucional</td></tr>");
      	
      	buf.append("<tr><td class=barnav-left-top></td></tr>");
      	while(rsLinks.next()) 
      		buf.append(openRowString + rsLinks.getString("url") + ">" + rsLinks.getString("name") + closeRowString);
      
      //*** aqui termina la tabla
      buf.append("<tr><td class=barnav-left-top></td></tr>");
      
      buf.append("</table></td><td>\n");
      //navigationBar = buf.toString();
	  
	  
	  //home y my profiles
      //ResultSet rsLinks; // = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid = 1 or linkid = 2");
      
      //para imprimir el pie de la tabla
      if (!student.isAuthenticated())
    	 //busca solo el login
    	 rsLinks = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid = 1 or linkid = 2 or linkid = 14");
      else
      	//busca solo el logout y mis cursos
         rsLinks = stmt.executeQuery("SELECT * FROM navbarlink WHERE linkid = 1 or linkid = 2 or linkid = 16 or linkid = 19 ORDER BY linkid ASC");
      
      buf.append("<table><tr>");
      buf.append("<object classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000' codebase='http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,29,0' width='620' height='90'>"
  				+"<param name='movie' value='../Images/banner.swf'>"
  				+"<param name='quality' value='high'>"
  				+"<embed src='../Images/banner.swf' quality='high' pluginspage='http://www.macromedia.com/go/getflashplayer' type='application/x-shockwave-flash' width='620' height='90'></embed></object>'");
      
      while (rsLinks.next()) {
        //if (rsLinks.getBoolean("ShowLink"))
        buf.append("<td class=barnav-top>"+rsLinks.getString("iconurl")+" <a class=navbar-over2 href=" + rsLinks.getString("url") + ">" + rsLinks.getString("name") + "</a></td>\n");
      }
      buf.append("</tr>");
      Content content = new Content();
            
      buf.append("<tr><td colspan='4' class=row4><p>"+content.getNameCourse(student.getCourse_id())+"</td></tr></table>\n");
      
      
	  
	  
	  }catch(Exception e) {
	  	
	  }
	return(buf.toString());  
  }
  
  /*public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    String pageID = request.getParameter("ID");
    if (pageID==null) pageID="Home";
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) 
      response.sendRedirect(Course.secureLoginURL + "Page?ID=" + pageID);
    Page page = new Page();
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    // from here on, student id is assumed to be valid  
    String userRequest = request.getParameter("UserRequest");
    if (userRequest == null) {
      out.println(Page.create(webPage(pageID,student.getIsInstructor(),student.sectionID)));
      return;
    }

    if (userRequest.equals("PrinterFriendlyLink")){
      out.println(printerFriendlyPage(pageID,student.getIsInstructor(),student.sectionID));
      return;
    }
    //llama unicamente la barra de navegacion para los frames
    if(userRequest.equals("OnlyNavBar")) {
    	out.println(Page.Frames(student));
    	return;
    }
	   
    out.println(Page.create(webPage(pageID,student.getIsInstructor(),student.sectionID)));
  }*/
  
  //plantilla e impresion de contenido
  String webPage(String pageID, boolean isInstructor, int sectionNumber) {
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
        ResultSet rsPage;
      /*if (isInstructor)
        rsPage = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "' ORDER BY Section");
      else */
        rsPage = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "' AND (CourseSection='"+sectionNumber+"') ORDER BY Section"); 
      //si la pagina no tiene creado tablas entonces retorna null
      if (!rsPage.isBeforeFirst()) return null;
      //tmplate para la plantilla que se crea, aqui la imprime
      buf.append(tmplate.appendTopItems(pageID));
      while (rsPage.next()) {
        buf.append(tmplate.appendInnerItems(pageID,rsPage.getInt("Section")));
        //imprime encabezado 
        //buf.append("<h4>" + rsPage.getString("Heading") + "</h4>");
        //imprime el texto de descripcion o cometario
        buf.append(rsPage.getString("HTMLText"));
      } 
      buf.append(tmplate.appendBotItems(pageID));
    }
    catch (Exception e) {
      if (addCourseSection()){
        //return webPage(pageID, isInstructor, sectionNumber); 
      }
      else if (!webPageTableExists())
        return null;
      else
        return e.getMessage();
    }
    //enlace para editar de inicio paginas, solo para instructores
    if (isInstructor) 
      buf.append("<p><b>"+res.getString("str_instructor")+"</b>&nbsp;<a href=/servlet/" + Course.name + ".ManageContent?PageID=" + pageID + ">"+res.getString("str_instructor_link")+"</a>");
    return buf.toString();
  }
  
  
  String printerFriendlyPage(String pageID, boolean isInstructor, int sectionNumber) {
    StringBuffer buf = new StringBuffer("<HTML><HEAD><TITLE>" + Course.name
        + "</TITLE><META HTTP-EQUIV=pragma CONTENT=no-cache>\n");
    buf.append("</HEAD><BODY bgcolor=#FFFFFF>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPage;
      if (isInstructor)
        rsPage = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "' ORDER BY Section");
      else
        rsPage = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "' AND (CourseSection='"+sectionNumber+"' OR CourseSection='All') ORDER BY Section");
      if (!rsPage.isBeforeFirst()) return null;
      while (rsPage.next()) {
        buf.append("<h4>" + rsPage.getString("Heading") + "</h4>");
        buf.append(rsPage.getString("HTMLText"));
      }
    } catch (Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"Page:PrinterFriendlyPage()");
      return e.getMessage();
    }
    buf.append("<p><a href=/servlet/" + Course.name + ".Page?ID=" + pageID + ">"
        + res.getString("str_normal_page") + "</a></p>");
    buf.append("</BODY></HTML>");
    return buf.toString();
  }

  void addParam(String Name, String Value){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String SQLString = "INSERT INTO CourseParameters Values('"+Name+"','"+Value+"')";
      stmt.executeUpdate(SQLString);
    }catch(Exception e){
    }
    return;
  }

  boolean addCourseSection(){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("ALTER TABLE WebPages ADD (CourseSection VARCHAR(3) DEFAULT 'All')");
    }catch(Exception e){
      return false;
    }
    return true;
  }
//new method for better error handling in web page method. Because Home.java
//relies on the web page method to return null if there's no table, we need
//to check if the exception is due to lack of a table. If it is, return null. 
//otherwise, the exception string will be returned.
  boolean webPageTableExists(){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsWeb=stmt.executeQuery("SELECT * FROM WebPages LIMIT 1");
      rsWeb.close();
    }catch(Exception e){
      return false;
    }
      return true;
  }
  static String ContentPageIndex() {
  	String page = ("\n<html>"
	+"\n<head>"
	+"\n<title>Untitled Document</title>"
	+"\n<meta http-equiv='Content-Type' content='text/html; charset=iso-8859-1'>"
	+"\n</head>"
	+"\n<body>"
	+"\n<p align='center'><strong>Welcome to Eledge Content Page</strong></p>"
	+"\n<p align='center'>This page contains links to other pages such as lecture notes," 
  	+"\nweb presentations, and related sites elsewhere on the Internet. Use the BACK" 
  	+"\nbutton on your browser to return here, or use your Bookmark to the class Home" 
  	+"\nPage.</p>"
	+"\n</body>"
	+"\n</html>");
	
	return page;

  }
}
