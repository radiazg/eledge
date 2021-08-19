package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;


public class Cursos extends HttpServlet {
 RBStore res = EledgeResources.getCursosBundle();
 
public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
    
    Student student = (Student)session.getAttribute(Course.name + "Student"); //devuelve null si el estudiante no ha iniciado sesion
    if (student==null) student = new Student(); //crea el objeto estudiante de Student.class
    
    //usuario no autentiado lo lleva a login
    if(!student.isAuthenticated()) {
    	response.sendRedirect("../servlet/"+Course.name+".Login");
    }
    
    //examina si es admin o profesor, para enviarlo al servlet recomendado para el
    if(!student.getTAtoStudent() && (student.getIsInstructor() || student.getIsTA())) {
    	   	
    	if (request.getParameter("UserRequest") != null) {
    		if (request.getParameter("UserRequest").equals("Student")) {
    			student.setTAtoStudent(true);
    			student.setCourse_id(null);
    			out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>myEledge</a> >> Mis Cursos</em><br><br>"+ManageCursos.vartopTeacher+studentPage(student),student));
    	
    		}	
    	}
    	else if(student.getTAtoStudent()) 
    			response.sendRedirect(Course.name+".Cursos?UserRequest=Student");
    		 else 
    		 	response.sendRedirect(Course.name+".ManageCursos");
    	return;
    }   
    
    //almacena en la base de datos, la asignacion de los cursos        
    if ((request.getParameter("curso") != null) && (!student.isRegisteredCourse(student.getIDNumber(),Integer.parseInt(request.getParameter("curso"))))) {
    	addCourseStudent(Integer.parseInt(request.getParameter("curso")), request.getParameter("id"));
    	response.sendRedirect(Course.name+".Cursos"); //libera la pagina de los parametros insertados por el usuario	
    }
    
    //lista los cursos que tiene asigando o inscrito el alumno
    if(student.getIsTA())
    	out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>myEledge</a> >> Mis Cursos</em><br><br>"+ManageCursos.vartopTeacher+studentPage(student),student));
    else
        out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>myEledge</a> >> Mis Cursos</em><br><br>"+studentPage(student),student));
    
    //elimina permisos al TA
 	if (student.getIsTA()) {
      	student.setIsInstructor(false); 
    }       
}
   
public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
    
    Student student = (Student)session.getAttribute(Course.name + "Student"); //devuelve null si el estudiante no ha iniciado sesion
    if (student==null) student = new Student(); //crea el objeto estudiante de Student.class
    
    if(!student.isAuthenticated()) {
    	response.sendRedirect("../servlet/"+Course.name+".Login");
    }
    
    //lista los cursos que tiene el alumno
    out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>myEledge</a> >> <a href='"+Course.name+".Cursos'>Mis Cursos</a> >> Añadir Cursos</em><br><br>"+addPage(student),student));
        
   }
 
//lista los cursos potenciales a añadir
public String listAddCourses(Student student) {
 	
 	StringBuffer buf = new StringBuffer();
 	int NCursos = 1;
 	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsAllCourses = stmt.executeQuery("SELECT * FROM courses ORDER BY SectionName ASC");
                
        while (rsAllCourses.next()) {
        	if(!student.isRegisteredCourse(student.getIDNumber(),rsAllCourses.getInt("SectionID"))) {
        	   	buf.append("\n<tr><td>"+ NCursos +". "+rsAllCourses.getString("SectionName")+"<a href="+Course.name+".Cursos?curso="+rsAllCourses.getString("SectionID")+"&id="+student.getIDNumber()+" title="+res.getString("str_left_botton")+"><img src=../../Images/t/up.gif border=0>Añadir</a></td></tr>");
        		buf.append("\n<tr><td> "+rsAllCourses.getString("Description")+"</td></tr>");
        		NCursos++;
        	}
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
    
    return buf.toString(); 
     	
 }

//devuelve la página que lista los cursos que han se han asignado al estudiante.
public String studentPage(Student student) {
	
	StringBuffer page = new StringBuffer();
	
	page.append(""
    	+"\n<form name=Cursos method=post>"
    	  	
    	+"\n<table>"
		+"\n<tr><td><center><strong>"+student.firstName+ " " + student.lastName +", sus curso asignados son los siguientes: </strong></center></td></tr>"
		+"\n<tr><td class='comentario-center'> Para ingresar al curso deseado, de clic sobre el enlace respectivo al nombre del curso."
		+"\n Para registrase en nuevos cursos de clic en Añadir Nuevo Curso.</td></tr>" 
		+"\n</table>"
    	
    	+"\n<table>"
    	+"\n<tr><td>&nbsp;</td></tr>"
    	+ student.getCursos(student.getIDNumber())
    	//imprime el boton de agregar
    	+"\n<tr><td>&nbsp;</td></tr>");
    	    	
    	if(!(student.status.equals("Activo"))) {
    		page.append("\n<tr><td><center><input type=submit class=button name=Submit value='"+res.getString("str_btn")+"'></center></td></tr>");
    	}
    	
    	page.append("\n</table>"
    	
    	+ "</form>");
	
	return page.toString();
	
}

//retorna la pagina de añadir 
public String addPage(Student student) {
StringBuffer buf = new StringBuffer();

buf.append(""
+"\n<form name=Cursos method=post>"
+"\n<table>"
+"\n<tr><td><center><strong>Añadir nuevos cursos en linea</strong></center></td></tr>"
+"\n<tr><td class='comentario-center'> Para añadir nuevos cursos en linea seleccione el curso que guste y de clic en Añadir.</td></tr>" 
+"\n</table>");
		
buf.append("<table border=0>"
+"\n<tr><td>&nbsp;</td></tr>"
+ "<tr><td><div align=center><strong>"+res.getString("str_colum1")+"</strong></div></td></tr>"
+student.getCursos(student.getIDNumber())
+ "<tr><td>&nbsp;&nbsp;</td></tr>"
+ "<tr><td><div align=center><strong>"+res.getString("str_colum2")+"</strong></div></td></tr>"
+listAddCourses(student)
+ "</table>");

return buf.toString();
 }

//asigna curso a estudiante en la tabla asignar
public void addCourseStudent(int idCourse, String idStudent) {
    String sqlString; 
          
    sqlString = "INSERT INTO course_to_student (id,idcourses, idstudent)" 
      + "VALUES ('','"+idCourse+"','"+idStudent+"')"; 
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sqlString);
      }
    catch (Exception e) { 
    	e.getMessage();
    }
  }

}