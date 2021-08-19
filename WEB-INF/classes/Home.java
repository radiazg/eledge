package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class Home extends HttpServlet {

  RBStore res = EledgeResources.getHomeBundle(); //inicializa los textos para cualquier idioma
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
        
    Student student = (Student)session.getAttribute(Course.name + "Student"); //devuelve null si el estudiante no ha iniciado sesion
    if (student==null) student = new Student(); //crea el objeto estudiante de Student.class
    
    // from here on, user is assumed to be an authenticated student or instructor
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
                  
    if (!ta.hasPermission("Home",request,student,err)) {
        out.println(Page.create(err.toString()));
        return;
      }
    }
           
    //le quita los permisos al TA, si entra a un curso que va ver o estudiar
    if (student.getTAtoStudent()) {
    	student.setIsInstructor(false);
    	student.setTAStatus(false);
    }
    
    //examina si esta autenticado, para dejarlo seguir e imprimirle la barra de navegacion
    //ideal, para el respectivo curso
    if (student.isAuthenticated()) {
    String id_course = request.getParameter("curso"); //conoce la id del curso
    if (id_course != null) {
    	try{
    	
    	if (id_course.equals("0")){
    		student.setCourse_id(null);
    	}
    	
    	else if(student.isRegisteredCourse(student.getIDNumber(),Integer.parseInt(id_course)) || (student.getIsInstructor()))
    			student.setCourse_id(id_course);
    	}
    	catch(Exception e){
    		student.setCourse_id(null);
    	}
    
    }
    }
    
    boolean isAdmin = student.getIsInstructor();
    
    if(student.getCourse_id() == null && student.isAuthenticated()) {
      	if ((student.getIsInstructor()) && (student.getIsTA()))
    		isAdmin = false;
    	if ((student.getIsInstructor()) && (!student.getIsTA()))
    		isAdmin = true;
    }
    
    
    out.println(Page.create(homePage(isAdmin,(student.getCourse_id() != null?Integer.parseInt(student.getCourse_id()):0)),student));//imprime en la web el contenido de homepage     
	
	//elimina permisos al TA
 	if (student.getIsTA()) {
      	student.setIsInstructor(false); 
    }
  }
  
  String homePage(boolean isInstructor, int courseID){
    MessageFormat mf = new MessageFormat(res.getString("str_step1"));
    Object[] args = {
      "<a href=http://www.mysql.com>",
      "</a>",
      "<br>",
      "<i>",
      "</i>",
      "<BR>mysql> GRANT ALL ON Chem5720;<BR>"
    }; //para referenciar el enlace hacia la pagina de mysql cuando la base de datos no esta activa
    
    //if (courseID == 0) isInstructor = false;
    
    String webPage = new Page().webPage("Home",isInstructor,courseID);
        
    if (webPage == null) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass); //establece la conexion a la base de datos
        Statement stmt = conn.createStatement();
        createDefaultHomePage(stmt, Integer.toString(courseID));
        webPage = new Page().webPage("Home",isInstructor,courseID);
        return webPage;
      }
      catch (Exception e) {
        
        StringBuffer buf = new StringBuffer(e.getMessage() + "<p>");
        
        buf.append("<h4>" + res.getString("str_title_home") + "</h4>"
        + res.getString("str_err_report")
        + "<h4>" + res.getString("str_title_setup") + "</h4>"
        + res.getString("str_instructions")
        + "\n<ol>\n<li>" + mf.format(args));

        mf.applyPattern(res.getString("str_step2"));
        buf.append("\n<li>" + mf.format(args));

        mf.applyPattern(res.getString("str_step3"));
        args[5] = "<BR>mysql> GRANT ALL ON Chem5720.* TO <i>Chem5720User</i> IDENTIFIED BY '<i>mySecurePassword</i>';<BR>";
        buf.append("\n<li>" + mf.format(args));

        mf.applyPattern(res.getString("str_step4"));
        buf.append("\n<li>" + mf.format(args));

        mf.applyPattern(res.getString("str_step5"));
        buf.append("\n<li>" + mf.format(args)
        + "<li>" + res.getString("str_step6"));

        mf.applyPattern(res.getString("str_step7"));
        buf.append("<li>" + mf.format(args) + "</ol" + res.getString("str_instructions_end"));
        return buf.toString();
      }
    }
    StringBuffer buf = new StringBuffer(webPage);
    /*if (isInstructor)
      buf.append("<p><FORM ACTION=" + Course.name + ".ManageCourse>"
      + "<b>"+res.getString("str_home2_title")+":</b><INPUT TYPE=SUBMIT VALUE='Manage Course'></FORM>");*/
    return buf.toString();
  }

  void createDefaultHomePage(Statement stmt, String courseID) {
    MessageFormat mf = new MessageFormat(res.getString("str_home1_text"));
    Object[] args = { 
      "<i>",
      "</i>"
    };
    try {
     StringBuffer buf = new StringBuffer();
     
     buf.append("<strong>"+res.getString("str_home0_title")+"</strong><p>" 
     + res.getString("str_home0_texta") + "<p>" 
     + res.getString("str_home0_textb") + "<p>" );
     
     buf.append("<strong>"+res.getString("str_home1_title")+"</strong><p>"+ mf.format(args) + "<p>");
     
     mf.applyPattern(res.getString("str_home2_text"));
     
     buf.append("<strong>"+res.getString("str_home2_title")+"</strong><p>"+ mf.format(args) + "<p>");
     
     stmt.executeUpdate("INSERT INTO WebPages VALUES (0,'Home','', '"+buf.toString()+"', '"+courseID+"')");
     
     /*return "<h3>" + res.getString("str_db_created_ok") + "<br>"
      + res.getString("str_click_refresh") + "</h3>";*/
      
     
     
     /*stmt.executeUpdate("INSERT INTO WebPages VALUES (0,'Home','"
      + res.getString("str_home0_title") + "','"
      + res.getString("str_home0_texta") + "<p>" 
      + res.getString("str_home0_textb") + "','All')");

      stmt.executeUpdate("INSERT INTO WebPages VALUES (1,'Home','"
      + res.getString("str_home1_title") + "','" + mf.format(args) + "','All')");

      mf.applyPattern(res.getString("str_home2_text"));
      stmt.executeUpdate("INSERT INTO WebPages VALUES (2,'Home','"
      + res.getString("str_home2_title") + "','"
      + mf.format(args) + "','All')");
      return "<h3>" + res.getString("str_db_created_ok") + "<br>"
      + res.getString("str_click_refresh") + "</h3>";*/
    }
    catch (Exception e) {
      Page.isLoaded = false;
      //return res.getString("str_db_created_bad");
    }
  }
 
}
