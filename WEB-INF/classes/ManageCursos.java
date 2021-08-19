package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;


public class ManageCursos extends HttpServlet {
 RBStore res = EledgeResources.getCursosBundle();
 
static String vartopAdmin = "<table>"
+"\n<tr><td><center><strong>Crear Cursos</strong></center></td></tr>"
+"\n<tr><td class='comentario-center'>Para crear un nuevo curso de clic en Crear Curso</td></tr>"
+"\n<tr><td class=row1><img src=../../Images/icons/cursos.gif align='absmiddle' border=0><a class=menizd href="+Course.name+".ManageCursos?UserRequest=New>Crear Curso</a></td></tr></table>";


static String vartopTeacher = "<table><tr><td class=menuizda bgcolor=#CCCCCC><a class=menizd href="+Course.name+".ManageCursos?UserRequest=Instructor>Ver como profesor</a><br></td>\n"
+"<td class=menuizda bgcolor=#CCCCCC><a class=menizd href="+Course.name+".Cursos?UserRequest=Student>Ver como estudiante</a><br></td></tr></table>\n";
 
public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
    
    Student student = (Student)session.getAttribute(Course.name + "Student"); //devuelve null si el estudiante no ha iniciado sesion
    if (student==null) student = new Student(); //crea el objeto estudiante de Student.class
    
    //elimina permisos al TA
 	if (student.getIsTA()) {
      	student.setIsInstructor(false); 
    }
        
    //usuario no autentiado lo lleva a login
    if(!student.isAuthenticated()) {
    	response.sendRedirect("../servlet/"+Course.name+".Login");
    }
    
    //añade y elimina usuarios de curso   
    if(student.getIsTA() || student.getIsInstructor()) {
    	if (request.getParameter("UserRequest") != null) {
    	 	if (request.getParameter("UserRequest").equals("DeleteStudentCourse")) {
    			deleteCourseStundent(Integer.parseInt(request.getParameter("idCourse")), request.getParameter("idStudent"));
    			out.print(Page.create(studensCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			//response.sendRedirect(Course.name+".ManageCursos?UserRequest=AdminStudent");
    			return;
    		}
    		else if (request.getParameter("UserRequest").equals("addStudentCourse")) {
    			addStudentCourse(Integer.parseInt(student.getCourse_id()),request.getParameter("idStudent"));
    			out.print(Page.create(studensCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			//response.sendRedirect(Course.name+".ManageCursos?UserRequest=AdminStudent");
    			return;
    		}
    		else if (request.getParameter("UserRequest").equals("Export")) {
    			ContentPacking exportContentCourse = new ContentPacking();
    			exportContentCourse.exportContent(request.getParameter("id"));
    			response.sendRedirect(exportContentCourse.getZipFile());
    				
    		}
    	}
    }
    
    //si el usuario es instructor(admin)
    if(student.getIsInstructor()){
    	if (request.getParameter("UserRequest") != null) {
    		if (request.getParameter("UserRequest").equals("New")) {
    			out.print(Page.create(NewCoursePage(),student));
    			return;
    		}
    		if (request.getParameter("UserRequest").equals("Edit")){
    			out.print(Page.create(EditCoursePage(request.getParameter("curso")),student));
    			return;
    		}	
    	    if (request.getParameter("UserRequest").equals("Delete"))
    			deleteCourse(Integer.parseInt(request.getParameter("id")));

    	    if (request.getParameter("UserRequest").equals("addTeacherCourse")){
    	    	addTeacherCourse(Integer.parseInt(student.getCourse_id()), request.getParameter("idStudent"));
    			out.print(Page.create(teacherCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			//response.sendRedirect(Course.name+".ManageCursos?UserRequest=AdminInstructor");
    			return;
    		}	
    		if (request.getParameter("UserRequest").equals("deleteTeacherCourse")){
    			deleteTeacherCourse(request.getParameter("idStudent"));
    			out.print(Page.create(teacherCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			//response.sendRedirect(Course.name+".ManageCursos?UserRequest=AdminInstructor");
    			return;
    		}	    			
    		if (request.getParameter("UserRequest").equals("AdminStudent")) {
    			out.print(Page.create(studensCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			return;
    		}
    		if (request.getParameter("UserRequest").equals("AdminInstructor")) {
    			out.print(Page.create(teacherCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			return;
    		}
    			
    	}
    	
    	out.println(Page.create(PageInstructor(request),student));
    	return;
    }
    
    //procesos de usuario profesor
    if(student.getIsTA()) {
       	if (request.getParameter("UserRequest") != null) {
    	  	if (request.getParameter("UserRequest").equals("Instructor")) {
    	  		student.setCourse_id(null);
    	  		student.setTAtoStudent(false);
    	  		out.print(Page.create(listTeacherCourses(student.getIDNumber()),student));
    	  		return;
    	  	}
    	  	if (request.getParameter("UserRequest").equals("AdminStudent")) {
    			out.print(Page.create(studensCourse(student,Integer.parseInt(student.getCourse_id())),student));
    			return;
    		}
    	   	   	
    	}
    	
    	out.print(Page.create(listTeacherCourses(student.getIDNumber()),student));
    	return;
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
    
    //instructor, crear curso
    if(student.getIsInstructor()) {
    	String userRequest = request.getParameter("UserRequest");
    		if (userRequest.equals("New")) {
    				addCourse(request.getParameter("Name"), request.getParameter("Description"));
    		}
    		if (userRequest.equals("Edit")) {
    				editCourse(Integer.parseInt(request.getParameter("Id")), request.getParameter("Name"), request.getParameter("Description"));
    		}
    		out.println(Page.create(PageInstructor(request),student));
       		return;
    }
    
}

//retorna la pagina de crear nuevo curso
public String NewCoursePage() {
StringBuffer buf = new StringBuffer();
	
	buf.append(""
	+"\n<table>"
	+"\n<tr><td><strong><center>Crear nuevo curso</center></strong></td></tr>\n"
	+"\n<tr><td class='comentario-center'> En esta forma escriba el título o nombre de curso, así como su breve descripción, para que los estudiantes lo puedan distinguir de otros cursos similares."
	+"\n</table>");
	
	buf.append(""
	+"<table class='tabla' border='0' cellspacing='0'><form name=Cursos method=post action="+Course.name+".ManageCursos>"
	+"<tr><td class='row1'><center>Título o nombre del Curso </center></td></tr>\n"
	+"<tr><td class='row1'><center><input name=Name type=text size='60'></center></td></tr>\n"
	+"<tr><td class='row1'><center>Descripci&oacute;n del Curso</center></td></tr>\n"
	+"<tr><td class='row1'><center><textarea name=Description cols='50' rows='5'></textarea></center></td></tr>\n"
	+"<tr><td class='row1'><center><input type=submit class='button' value=Crear></center>\n"
	+"<input type=hidden name=UserRequest value=New>\n"
	+"</td></tr>"
	+"</form><table>");
 			
return buf.toString(); 		
}

//retorna la pagina de editar nuevo curso
public String EditCoursePage(String idcurso) {
StringBuffer buf = new StringBuffer();
	
try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
        ResultSet rsCourses = stmt.executeQuery("SELECT * FROM courses WHERE SectionID="+idcurso);
        rsCourses.next();
        
        buf.append("<form name=Cursos method=post action="+Course.name+".ManageCursos><p align='center'><strong>Ventana Modificar</strong></p>"
        +"<div align='center'>"
        +"Nombre<br><input name=Name type=text value='"+rsCourses.getString("SectionName")+"' size=60><br><br>"
        +"Descripcion<br><textarea name=Description cols='50' rows='5'>"+rsCourses.getString("Description")+"</textarea><br>"
        +"<input type=submit value=Modificar>"
        +"<input type=hidden name=UserRequest value=Edit>"
        +"<input type=hidden name=Id value='"+rsCourses.getString("SectionID")+"'>"
        +"</div>"
        +"</form>");
        
    }catch (Exception e) { 
    	buf.append(e.getMessage());
    	
    }
 			
return buf.toString(); 		
}

//elimina curso a estudiante de la tabla asignar  
public void deleteCourseStundent(int idCourse, String idStudent) {
    String sqlString; 
          
    sqlString = ("DELETE FROM course_to_student WHERE idcourses = "+idCourse+" and idstudent = '"+idStudent+"'"); 
   
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
  
//imprime pagina para instructor(admin) 
public String PageInstructor (HttpServletRequest request) {
	
	StringBuffer buf = new StringBuffer();
	
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        buf.append("<script LANGUAGE='JavaScript'> "
        +"function confirmSubmit(){ var agree=confirm('Ud está seguro de borrar el curso?'); "
        +"if (agree) return true ; "
        +"else return false ;}</script>");
        buf.append(vartopAdmin
 		+	"\n<table border='0'>");
        //+	  "<tr>"
        //+	     "<td>");
                	ResultSet rsCourses = stmt.executeQuery("SELECT Value FROM courseparameters Where Name='title'");
                	
                	if(rsCourses.next()) {
                	   	if(!rsCourses.getString("Value").equals(""))
                	   		buf.append("<tr><td><center><strong>Lista de todos los cursos de "+rsCourses.getString("Value")+"</strong></center></td></tr>");
                		else
                			buf.append("<tr><td><center><strong>Lista de todos los cursos</strong></center></td></tr>");
                	}
                	
                	buf.append("<tr><td>&nbsp;</td></tr>");
                	buf.append("<tr><td><strong>Convenciones:</strong></td></tr>");
                	buf.append("<tr><td><img src=../../Images/t/edit.gif align='absmiddle' border=0> Editar título o breve descripción del curso.</td></tr>"
                	+"<tr><td><img src=../../Images/t/delete.gif align='absmiddle' border=0> Eliminar/Borrar curso por completo.</td></tr>"
					+"<tr><td><img src=../../Images/t/package.gif align='absmiddle' border=0> Exportar Contenido del curso.</td></tr>");
                	buf.append("<tr><td>&nbsp;</td></tr>");
                	
                	//select de cursos.
                	rsCourses = stmt.executeQuery("SELECT * FROM courses ORDER BY SectionName ASC");
                	int NCursos = 1;
                	while (rsCourses.next()) {
    				buf.append("\n");
    				//buf.append("<FORM NAME=curso"+NCursos+" METHOD=post");
    				//buf.append(" onSubmit=\"return confirm('" + res.getString("str_warning_one_chance")
       			 	//+ res.getString("str_sure_to_take_exam") + "')\"");
    				buf.append("<tr><td><a href= "+Course.name+".Home?curso="+rsCourses.getString("SectionID")+">"+NCursos+". "+rsCourses.getString("SectionName")+"</a>");
    				buf.append("\n");
    				buf.append(" <a href= "+Course.name+".ManageCursos?curso="+rsCourses.getString("SectionID")+"&UserRequest=Edit title='"+res.getString("str_edit_botton")+"'><img src=../../Images/t/edit.gif align='absmiddle' border=0></a>");   			
        			buf.append("\n");
    				buf.append(" <a href= "+Course.name+".ManageCursos?id="+rsCourses.getString("SectionID")+"&UserRequest=Delete title="+res.getString("str_delete_botton")+" onClick='return confirmSubmit()'><img src=../../Images/t/delete.gif align='absmiddle' border=0></a>");   			
        			buf.append(" <a href= "+Course.name+".ManageCursos?id="+rsCourses.getString("SectionID")+"&UserRequest=Export title='"+res.getString("str_export_botton")+"'><img src=../../Images/t/package.gif align='absmiddle' border=0></a>"); 
        			buf.append("</td></tr>");
        			
        			buf.append("<tr><td> "+rsCourses.getString("Description")+"</td></tr>");
    				//buf.append("</FORM>");
    				NCursos++;
    				}
                    
        buf.append(""
        //buf.append("</td>"
        //+	"</tr>"
        +   "</table>"
        +    "</p></td>"
       	+ "</tr>"
        +"</table>");
 		
    	}
    	catch(Exception e){
      		  }
    return buf.toString();
}

//crea un nuevo curso
public void addCourse(String SectionName, String Description) {
    String sqlString; 
          
    sqlString = "INSERT INTO courses (SectionID,SectionName, Description)" 
      + "VALUES ('','"+SectionName+"','"+Description+"')"; 
    
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

//edita un curso
public void editCourse(int SectionID, String SectionName, String Description) {
    String sqlString; 
    
    sqlString = "UPDATE courses SET SectionName = '"+SectionName+"', Description = '"+Description+"' WHERE SectionID = "+SectionID; 
        
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

//inserta un profesor, potencial al curso
public void addTeacherCourse(int SectionID, String IdStudent) {
    String sqlString; 
    
    sqlString = "INSERT INTO course_to_teacher (id,idcourses, idstudent) VALUES ('','"+SectionID+"','"+IdStudent+"')";    
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //examina si ya existe en la base de datos,para que no se repitan los datos
      if(!existInTable("course_to_teacher", Integer.toString(SectionID), IdStudent))
      	stmt.executeUpdate(sqlString);
      
      }
    catch (Exception e) { 
    	e.getMessage();
    }
  }
  
//inserta un estudiante, potencial al curso
public void addStudentCourse(int SectionID, String IdStudent) {
    String sqlString; 
    
    sqlString = "INSERT INTO course_to_student (id,idcourses, idstudent) VALUES ('','"+SectionID+"','"+IdStudent+"')";    
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //examina si ya existe en la base de datos,para que no se repitan los datos
      if(!existInTable("course_to_student", Integer.toString(SectionID), IdStudent))
      	stmt.executeUpdate(sqlString);
      
      }
    catch (Exception e) { 
    	e.getMessage();
    }
  }

//borra un curso
public void deleteCourse(int SectionID) {
            
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM courses WHERE SectionID = "+SectionID);
      stmt.executeUpdate("DELETE FROM course_to_student WHERE idcourses = "+SectionID);
    }
    catch (Exception e) { 
    	e.getMessage();
    }
  }

//borra un profesor del curso
public void deleteTeacherCourse(String IdStudent) {
            
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM course_to_teacher WHERE idstudent = '"+IdStudent+"'");
    }
    catch (Exception e) { 
    	e.getMessage();
    }
  }

//lista los cursos del profesor
public String listTeacherCourses(String StudentIDNumber) {
	StringBuffer buf = new StringBuffer();
 	
 	buf.append(vartopTeacher);
 	
 	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
    	ResultSet rsAllCourses = stmt.executeQuery("SELECT  * FROM courses, course_to_teacher, students WHERE courses.SectionID = idCourses AND idStudent = StudentIDNumber AND idStudent =  '"+StudentIDNumber+"' ORDER BY SectionName ASC");
        
        buf.append("<center><strong>Lista de cursos asignados</strong></center><br>\n");
        buf.append("<center>Seleccione de la lista de cursos asigandos, el curso al cual desea ingresar y gestionar.</center><br>\n");
          
        while (rsAllCourses.next()) {
        	buf.append("<a href=Eledge.Home?curso="+rsAllCourses.getString("SectionID")+">"+rsAllCourses.getString("SectionName")+"</a><br>\n");
        	buf.append(" "+rsAllCourses.getString("Description")+"<br>\n");
        	}
        }
    	      
       
    catch(Exception e){
    	e.getMessage();
    }
    
    return buf.toString(); 
}

//Busca Estudiantes del curso, para administrarlos
public String studensCourse(Student student, int SectionID) {
	StringBuffer buf = new StringBuffer();
 	int number=1;
 	//buf.append(vartopTeacher);
 	
 	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
    	//busca los estudiantes del curso
    	ResultSet rsAllStudents = stmt.executeQuery("SELECT  * FROM course_to_student, courses, students WHERE courses.sectionid=idcourses and students.studentIDNumber=idstudent and idcourses ='"+SectionID+"' ORDER BY FirstName ASC");
      
        buf.append("<script LANGUAGE='JavaScript'> "
        +"function confirmSubmitEst(){ var agree=confirm('Ud está seguro de Borrar al estudiante del curso?'); "
        +"if (agree) return true ; "
        +"else return false ;} "
        +"function confirmSubmitEstAg(){ var agree=confirm('Ud está seguro de Agregar al estudiante al curso?'); "
        +"if (agree) return true ; "
        +"else return false ;}</script>");
        buf.append("<table><tr><td class='titulo-center'>Lista de estudiantes en el curso</td></tr>\n");
        buf.append("<tr><td class='comentario-center'>Seleccione los cambios deseados para cada estudiante asignado al curso.</td></tr></table>\n");
        
        buf.append("<table border=0 cellspacing=0 class='bodyline'");
        buf.append("<tr><td colspan='6' class='row1'><strong>Usuarios</strong></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        buf.append("<tr><td class='row3'><center>#</center></td><td class='row3'><center>Usuario</center></td><td class='row3'><center>Nombre</center></td><td class='row3'><center>Apellido</center></td><td class='row3'><center>Estatus</center></td><td class='row3'><center>Acciones</center></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
          
        while (rsAllStudents.next()) {
           		buf.append("<tr bgcolor=#FFFFFF><td class='rightline'>"+number+"</td><td class='rightline'>"+rsAllStudents.getString("StudentIDNumber")+"</td><td class='rightline'>"+rsAllStudents.getString("FirstName")+"</td><td class='rightline'>"+rsAllStudents.getString("LastName")+"</td><td class='rightline'>"+rsAllStudents.getString("Status")+"</td><td><a href= "+Course.name+".ManageCursos?idStudent="+rsAllStudents.getString("StudentIDNumber")+"&idCourse="+rsAllStudents.getString("courses.SectionID")+"&UserRequest=DeleteStudentCourse title="+res.getString("str_delete_botton")+" onClick='return confirmSubmitEst()'><img src=../../Images/t/delete.gif border=0>"+res.getString("str_delete_botton")+"</a></td></tr>\n");
        		
        		if(!rsAllStudents.isLast())
        			buf.append("<tr><td colspan='6' class='row2'>");
        		
        		number++;
        }
        
        buf.append("</table><p></p>");
        //***aqui termina
        
        //para asignar estudiantes potenciales al curso  		
    	rsAllStudents = stmt.executeQuery("SELECT * FROM students WHERE Status!='Instructor'");

		buf.append("<table><tr><td class='titulo-center'>Lista de estudiantes potenciales a añadir en el curso</td></tr>\n");
        buf.append("<tr><td class='comentario-center'>Seleccione el estudiante deseado para el curso, oprima agregar para realizar la operación.</td></tr></table>\n");
            	
      	buf.append("<table border=0 cellspacing=0 class='bodyline'");
        buf.append("<tr><td colspan='6' class='row1'><strong>Usuarios</strong></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        buf.append("<tr><td class='row3'><center>#</center></td><td class='row3'><center>Usuario</center></td><td class='row3'><center>Nombre</center></td><td class='row3'><center>Apellido</center></td><td class='row3'><center>Estatus</center></td><td class='row3'><center>Acciones</center></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
         
        number = 1;
        while (rsAllStudents.next()) {
        	if(!student.isRegisteredCourse(rsAllStudents.getString("StudentIDNumber"),SectionID)) {
        	   	buf.append("<tr bgcolor=#FFFFFF><td class='rightline'>"+number+"</td><td class='rightline'>"+rsAllStudents.getString("StudentIDNumber")+"</td><td class='rightline'>"+rsAllStudents.getString("FirstName")+"</td><td class='rightline'>"+rsAllStudents.getString("LastName")+"</td><td class='rightline'>"+rsAllStudents.getString("Status")+"</td><td><a href= "+Course.name+".ManageCursos?idStudent="+rsAllStudents.getString("StudentIDNumber")+"&UserRequest=addStudentCourse title="+res.getString("str_up_botton")+" onClick='return confirmSubmitEstAg()'><img src=../../Images/t/up.gif border=0>"+res.getString("str_up_botton")+"</a></td></tr>\n");
        		
        		if(!rsAllStudents.isLast())
        			buf.append("<tr><td colspan='6' class='row2'>");
        		
        		number++;
        	}
        }
        
        buf.append("</table>");
        //***aqui termina
    
    }catch(Exception e){
    	e.getMessage();
    }
    
    return buf.toString(); 
}

//lista los profesores potenciales a añadir al curso
public String teacherCourse(Student student, int SectionID) {
 	
 	StringBuffer buf = new StringBuffer();
 	int number=1;
 	
 	try{
  		
  		//profesores registrados
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
    	ResultSet rsAllTeachers = stmt.executeQuery("SELECT  * FROM course_to_teacher, courses, students WHERE courses.sectionid=idcourses and students.studentIDNumber=idstudent and idcourses ='"+SectionID+"' ORDER BY FirstName ASC");
        
        buf.append("<script LANGUAGE='JavaScript'> "
        +"function confirmSubmitProf(){ var agree=confirm('Ud está seguro de Borrar al Profesor del curso?'); "
        +"if (agree) return true ; "
        +"else return false ;} "
        +"function confirmSubmitProfAg(){ var agree=confirm('Ud está seguro de Agregar al Profesor al curso?'); "
        +"if (agree) return true ; "
        +"else return false ;}</script>");
        
        buf.append("<table><tr><td class='titulo-center'>Lista de profesores a dictar en el curso</td></tr>\n");
        buf.append("<tr><td class='comentario-center'>Seleccione los cambios deseados para cada profesor asignado al curso.</td></tr></table>\n");
        
        buf.append("<table border=0 cellspacing=0 class='bodyline'");
        buf.append("<tr><td colspan='6' class='row1'><strong>Usuarios</strong></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        buf.append("<tr><td class='row3'><center>#</center></td><td class='row3'><center>Usuario</center></td><td class='row3'><center>Nombre</center></td><td class='row3'><center>Apellido</center></td><td class='row3'><center>E-mail</center></td><td class='row3'><center>Acciones</center></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        while (rsAllTeachers.next()) {
           		buf.append("<tr bgcolor=#FFFFFF><td class='rightline'>"+number+"</td><td class='rightline'>"+rsAllTeachers.getString("StudentIDNumber")+"</td><td class='rightline'>"+rsAllTeachers.getString("FirstName")+"</td><td class='rightline'>"+rsAllTeachers.getString("LastName")+"</td><td  class='rightline'>"+rsAllTeachers.getString("Email")+"</td><td><a href= "+Course.name+".ManageCursos?idStudent="+rsAllTeachers.getString("StudentIDNumber")+"&UserRequest=deleteTeacherCourse title="+res.getString("str_delete_botton")+" onClick='return confirmSubmitProf()'><img src=../../Images/t/delete.gif border=0>"+res.getString("str_delete_botton")+"</a></td></tr>\n");
        		
        		if(!rsAllTeachers.isLast())
        			buf.append("<tr><td colspan='6' class='row2'>");
        			
        		number++;
        }
        
        buf.append("</table><p></p>");
        //***aqui termina
  		
  		//para asignar profesores potenciales al curso  		
    	rsAllTeachers = stmt.executeQuery("SELECT * FROM students WHERE Status='TA'");
                
        buf.append("<table><tr><td class='titulo-center'>Lista de profesores potenciales a dictar el curso</td></tr>\n");
        buf.append("<tr><td class='comentario-center'>Seleccione el profesor deseado para el curso, oprima agregar para realizar la operación</td></tr></table>\n");
        
        buf.append("<table border=0 cellspacing=0 class='bodyline'");
        buf.append("<tr><td colspan='6' class='row1'><strong>Usuarios</strong></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        buf.append("<tr><td class='row3'><center>#</center></td><td class='row3'><center>Usuario</center></td><td class='row3'><center>Nombre</center></td><td class='row3'><center>Apellido</center></td><td class='row3'><center>E-mail</center></td><td class='row3'><center>Acciones</center></td></tr>");
        buf.append("<tr><td colspan='6' class='row2'>");
        
        number = 1;
        while (rsAllTeachers.next()) {
        	if(!student.isTeacherRegisteredCourse(rsAllTeachers.getString("StudentIDNumber"),SectionID)) {
        	   	buf.append("<tr bgcolor=#FFFFFF><td class='rightline'>"+number+"</td><td class='rightline'>"+rsAllTeachers.getString("StudentIDNumber")+"</td><td class='rightline'>"+rsAllTeachers.getString("FirstName")+"</td><td class='rightline'>"+rsAllTeachers.getString("LastName")+"</td><td class='rightline'>"+rsAllTeachers.getString("Email")+"</td><td><a href= "+Course.name+".ManageCursos?idStudent="+rsAllTeachers.getString("StudentIDNumber")+"&UserRequest=addTeacherCourse title="+res.getString("str_up_botton")+" onClick='return confirmSubmitProfAg()'><img src=../../Images/t/up.gif border=0>"+res.getString("str_up_botton")+"</a></td></tr>\n");
        		
        		if(!rsAllTeachers.isLast())
        			buf.append("<tr><td colspan='6' class='row2'>");
        			
        		number++;
        	}
        }
        
        buf.append("</table>");
        //***aqui termina
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
    
    return buf.toString(); 
     	
 }

//devuelve true si existe el dato en la tabla
private boolean existInTable(String table, String idCourse, String idStudent){
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	ResultSet rsListTable = stmt.executeQuery("SELECT id FROM "+table+" WHERE idcourses='"+idCourse+"' AND idstudent='"+idStudent+"'"); 
    	
    	if(rsListTable.next())
    		return true;
    		
    }catch(Exception e){
    	return false;
    }
  
  return false;
}  
}
