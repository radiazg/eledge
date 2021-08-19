package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class Content extends HttpServlet {
  
  private RBStore res = EledgeResources.getContentBundle();

  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public String getNameCourse (String idCourse){
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	    	
    	ResultSet rsNameCourse = stmt.executeQuery("SELECT SectionName  FROM courses WHERE  SectionID='"+idCourse+"'");
    	if (rsNameCourse.next()) {
    		return (rsNameCourse.getString("SectionName"));
    	}
    }catch (Exception e){
    	e.getMessage();
    }
    
    return "";
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
	
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

	
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    
    //elimina permisos al TA
 	if (student.getIsTA()) {
      	student.setIsInstructor(false); 
    }

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Content");
      return;
    } 
          
    
   // from here on, user is assumed to be an authenticated student or instructor
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Content",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "+getNameCourse(student.getCourse_id())+"</em><br><br>"+err.toString()));
        return;
      }
    }
    
    //le quita los permisos al TA, si entra a un curso que va ver o estudiar
    if (student.getTAtoStudent()) {
    	student.setIsInstructor(false);
    	student.setTAStatus(false);
    }
         
    String userRequest = request.getParameter("userRequest");
    //String id_tc = request.getParameter("btnID_TC");  
    if (userRequest != null){
    	if (userRequest.equals("ContentForm")){
    		if(student.getIsTA() || student.getIsInstructor()) {
    			student.setTheme_id(Integer.parseInt(request.getParameter("theme")));
    			out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> "+getTitleContent(request.getParameter("content_id"))+"</em><br><br>"+contentPage(null,student.getTheme_id(),student.getCourse_id(),false, getTitleContent(request.getParameter("content_id"))),student));
    			return;
    		}
    		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> "+getTitleContent(request.getParameter("content_id"))+"</em><br><br>"+contentPage(null,Integer.parseInt(request.getParameter("theme")),student.getCourse_id(),true, getTitleContent(request.getParameter("content_id"))),student));
    		return;
    	}
    }
  
  //imprime la pagina de contenido
   //out.println(Page.create(contentPage(student.getIsInstructor(),(student.getCourse_id() != null?Integer.parseInt(student.getCourse_id()):0)),student));    
		
	if(student.getIsTA() || student.getIsInstructor()) {
		//imprime la pagina web especial para el admin,profesor
		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "+getNameCourse(student.getCourse_id())+"</em><br><br>"+listThemesCourse(student.getCourse_id(),false),student));	
		return;
	}
	//imprime la pagina web especial para el estudiante
	out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "+getNameCourse(student.getCourse_id())+"</em><br><br>"+listThemesCourse(student.getCourse_id(),true),student));
}


public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
	
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Content");
      return;
    } 
          
    // from here on, user is assumed to be an authenticated student or instructor
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Content",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> "+getTitleContent(request.getParameter("content_id"))+"</em><br><br>"+err.toString()));
        return;
      }
    }
    
    String id_sec = " ";
    int id_Sec = Integer.parseInt(request.getParameter("btnID_SEC"));
    
    if (request.getParameter("next") != null) {
    	//incrementa a uno mas para continuar con el siguiente registro, es decir, siguiente
    	id_sec = Integer.toString(id_Sec+1);
    }
    if (request.getParameter("back") != null) {
    	//dismunuye a uno menos para continuar con el anterior registro, atras
    	id_sec = Integer.toString(id_Sec-1);
    }
    if (request.getParameter("edit") != null || request.getParameter("deletepage") != null) {
    	
    	//actualiza el id de tema al usuario para que lo conozca otra clase cuando se desee
    	//student.setTheme_id(Integer.parseInt(request.getParameter("theme")));
    	
    	if (request.getParameter("edit") != null) {
    		//envia parametros a managecontent de edicion
    		response.sendRedirect(Course.name+".ManageContent?userRequest=Edit&id_sec="+id_Sec);	
    	}else{
    		//envia parametros a managecontent de eliminar
    		response.sendRedirect(Course.name+".ManageContent?userRequest=deletepage&id_sec="+id_Sec);	
    	}
    }
     
  //imprime la pagina de contenido
   //out.println(Page.create(contentPage(student.getIsInstructor(),(student.getCourse_id() != null?Integer.parseInt(student.getCourse_id()):0)),student));    
	if(student.getIsTA() || student.getIsInstructor()) 
		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> "+getTitleContent(request.getParameter("content_id"))+"</em><br><br>"+contentPage(id_sec,Integer.parseInt(request.getParameter("theme")),student.getCourse_id(),false, getTitleContent(request.getParameter("content_id"))),student));	
	else
		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> <a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> "+getTitleContent(request.getParameter("content_id"))+"</em><br><br>"+contentPage(id_sec,Integer.parseInt(request.getParameter("theme")),student.getCourse_id(),true, getTitleContent(request.getParameter("content_id"))),student));	
	//out.print(Page.create(listThemesCourse(student.getCourse_id()),student));
}

//pagina de contenido
  String contentPage(String id_sec,int themeID,String courseSection, boolean isStudent, String titleContent) {
    
    StringBuffer buf = new StringBuffer();
    String sqlString;
    
    //crea la forma de contenido del tema       
    buf.append("<form name='form' method='post'>");
    
    //leyenda cuando muestra el contenido del tema
     buf.append("\n<table>"
	+"\n<tr><td><center><strong>Contenido del tema</strong></center></td></tr>"
	+"\n<tr><td class='comentario-center'> Para navegar por el contenido del tema, use los bótones Atras y Siguiente.");
		
	if(!isStudent) {
		buf.append("\n Para Editar o Agregar una nueva página, de clic en Editar/Añadir Nueva Página."
		+"\n Si desea eliminar la página actual, de clic en Eliminar.");
	}
		
	buf.append("</td></tr>"
	+"\n</table>");
    
    //crea la tabla para el contenido y la barra de navegación de la misma
    buf.append("\n<table class='bodyline' cellspacing='0'>"
        +"\n<tr><td class='row1'><strong>Tema "+themeID+":</strong> "+titleContent+"</td></tr>"
        +"\n<tr><td class='row2'></td></tr>"
        +"\n<tr><td>");
   
   	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	/*Estas condiciones se realizan con el proposito de conocer
    	 *el id del anterior registro y del siguiente registro, como
    	 *el actual, entonces se ordenan por sección, ya que el id
    	 *autonumerico puede ser de diferente orden, pero el id de 
    	 *secuencia no.*/
    	 //si hasta ahora se oprime agregar, el id_sec siempre es null, ya que lo deja en la primera pagina del tema
    	if (id_sec == null) {
    		sqlString = "SELECT id_sec,title,body FROM contenttotheme WHERE  coursesection_id='"+courseSection+"' and  id_theme='"+themeID+"' ORDER BY id_sec ASC";
    		ResultSet rsSelectContent = stmt.executeQuery(sqlString);
    		
    		//examina si el primer registro es el unico, no habilita el boton siguiente
    		if(rsSelectContent.next()) {
    			buf.append("<input type='hidden' name='btnID_SEC' value='"+rsSelectContent.getString("id_sec")+"'>"
        		+"<h2>"+rsSelectContent.getString("title")+"</h2>"
        		+ rsSelectContent.getString("body") +"</td></tr>"
        		+"\n<tr><td class='row2'></td></tr>"
        		+"\n<tr><td class='row1'>"
        		+"<center><input class='button' type='submit' name='back' value='<Atras' disabled='disabled'>");
        		
        		//boton editar y eliminar
        		if (!isStudent) {
        			buf.append("  <input class='button' type='submit' name='edit' value='Editar/Añadir Nueva Página'>");
        			buf.append("  <input class='button' type='submit' name='deletepage' value='Eliminar'>");
        		}
        		       		 
        		//examina si el primer registro es el unico, no habilita el boton siguiente
        		if (!rsSelectContent.isLast())
        			buf.append("  <input class='button' type='submit' name='next' value='Siguiente>'></center></td></tr>");
        		else
        			buf.append("  <input class='button' type='submit' name='next' value='Siguiente>' disabled='disabled'></center></td></tr>");
        	}
        	
    	}
    	else{		
    		
    		int id = Integer.parseInt(id_sec); //valor entero del id_sec para la barra de navegación
    		
    		sqlString = "SELECT id_sec,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+themeID+"' and id_sec='"+(id)+"'";
    		ResultSet rsSelectContent = stmt.executeQuery(sqlString);
    		
    		if(rsSelectContent.next()) {
    			buf.append("<input type='hidden' name='btnID_SEC' value='"+rsSelectContent.getString("id_sec")+"'>"
        		+"<h2>"+rsSelectContent.getString("title")+"</h2>"
        		+ rsSelectContent.getString("body") +"</td></tr>"
        		+"\n<tr><td class='row2'></td></tr>"
        		+"\n<tr><td class='row1'><center>");
        	}	
        	
        	sqlString = "SELECT id_sec,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+themeID+"' and id_sec='"+(id-1)+"'";
    		rsSelectContent = stmt.executeQuery(sqlString);
	
			if(rsSelectContent.next()) {
				buf.append("<input class='button' type='submit' name='back' value='<Atras'>"); 
			}
        	else {
        		buf.append("<input class='button' type='submit' name='back' value='<Atras' disabled='disabled'>");
        	}
        	
        	//boton editar y eliminar
        	if (!isStudent) {
        			buf.append("  <input class='button' type='submit' name='edit' value='Editar/Añadir Nueva Página'>");
        			buf.append("  <input class='button' type='submit' name='deletepage' value='Eliminar'>");
        	}
        	
        	sqlString = "SELECT id_sec,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+themeID+"' and id_sec='"+(id+1)+"'";
    		rsSelectContent = stmt.executeQuery(sqlString);
	
        	if(rsSelectContent.next()) {
        		buf.append("  <input class='button' type='submit' name='next' value='Siguiente>'></td></tr>");
    		}
    		else {
    			
    			buf.append("  <input class='button' type='submit' name='next' value='Siguiente>' disabled='disabled'>");
    		}
    		
    		
    		buf.append("</center></td></tr>");
       	}
    	   
       }
    catch(Exception e){
    	buf.append(e.getMessage());
    }
 
 	buf.append("\n</table></form>");
    
    return buf.toString();
  }
  
//lista los temas del curso
  public String listThemesCourse(String courseSection, boolean isStudent) {
 	
 	int num_theme=0,content_id;
 	StringBuffer buf = new StringBuffer();
 	
 	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsAllThemes = stmt.executeQuery("SELECT * FROM content Where coursesection= '"+courseSection+"' ORDER By theme ASC");
     
     	
     	//leyenda cuando lista los temas del curso
     	buf.append("\n<table>"
		+"\n<tr><td><center><strong>Lista de temas disponibles del curso</strong></center></td></tr>"
		+"\n<tr><td class='comentario-center'> Seleccione el tema del curso que guste, y de clic en Ingresar o sobre el título del tema.");
		
		if(!isStudent) {
		buf.append("\n Para editar el título del tema o la breve descripción del tema, de clic en Editar."
		+"\n Si desea eliminar el tema con sus respectivos contenidos por completo, de clic en Eliminar.");
		}
		
		buf.append("</td></tr>"
		+"\n</table>");
     	
        while (rsAllThemes.next()) {
        	
        	//da valor entero a num_theme para conocer el tema que trata
        	num_theme = rsAllThemes.getInt("theme");
        	content_id = rsAllThemes.getInt("content_id");
        	//envia a bufer, las tablas del curso con informacion de los temas registrados
        	buf.append("<table cellspacing='0' class=tabla>");
  			buf.append("\n<tr><td class=titulo-themecenter>");
  				buf.append("Tema "+num_theme);
  			buf.append("</td></tr>");
  			buf.append("\n<tr><td class=header-themecenter>");
  				//buf.append(rsAllThemes.getString("title")); 
  				buf.append("<a href="+Course.name+".Content?content_id="+content_id+"&theme="+num_theme+"&userRequest=ContentForm class='navbar-over'>"+rsAllThemes.getString("title")+"</a>");
  			buf.append("</td></tr>");
  			buf.append("\n<tr><td class=row-left>");
  				buf.append(rsAllThemes.getString("description"));
  				buf.append("<br><div align='center'><img src='../../Images/icons/ingresar.gif'><a href="+Course.name+".Content?content_id="+content_id+"&theme="+num_theme+"&userRequest=ContentForm class='link-themecenter'>Ingresar</a>");
  				if(!isStudent) {
  					buf.append("  <img src='../../Images/t/edit.gif'><a href="+Course.name+".ManageContent?content_id="+content_id+"&userRequest=edit_content class='link-themecenter'>  Editar </a>"); 
  					buf.append("  <img src='../../Images/t/delete.gif'><a href="+Course.name+".ManageContent?content_id="+content_id+"&theme="+num_theme+"&userRequest=delete_content class='link-themecenter'>  Eliminar </a></div>");
  				
  					//buf.append("<a href="+Course.name+".ManageCursos?curso="+rsCourses.getString("SectionID")+"&userRequest=Edit title="+res.getString("str_edit_botton")+"><img src=../../Images/t/edit.gif border=0></a>");
  				}
  				else {
  					buf.append("</center>");
  				}
  			buf.append("</td></tr>");
			buf.append("\n</table><br>");			
		}
           	//hace que el estudiante no pueda agregar nuevo tema
           	if (isStudent) {
				return buf.toString();
			}
           	
           	//enviar a bufer, una nueva tabla para agregar los temas
        	buf.append("<table cellspacing='0' class=tabla>");
  			buf.append("\n<tr><td class=titulo-themecenter>");
  				buf.append("Tema "+(num_theme+1));
  			buf.append("</td></tr>");
  			buf.append("\n<tr><td class=header-themecenter>");
  				buf.append("Tema sin asignar"); 
  			buf.append("</td></tr>");
  			buf.append("\n<tr><td class=row-left>");
  				buf.append("Para agregar un nuevo tema haga clic en Agregar>>");
  				buf.append("<br><center><img src='../../Images/icons/agregar.gif'><a href="+Course.name+".ManageContent?theme="+num_theme+"&userRequest=newThemeForm class='link-themecenter'>Agregar</a></center>");
  			buf.append("</td></tr>");
			buf.append("\n</table>");
            	      
       }
    catch(Exception e){
    	e.getMessage();
    }
    
    return buf.toString(); 
     	
 }
  
 //extrae de la base de datos la información del titulo y body de la Pagina Web
public String getContentPage(String id_tc, int theme, String courseSection) {
    String sqlString;
    String WebPage = " ";
       
   	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	if (id_tc == null) {
    		sqlString = "SELECT id_tc,title,body FROM contenttotheme WHERE  coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' ORDER BY id_tc ASC";
    		ResultSet rsSelectContent = stmt.executeQuery(sqlString);
    		
    		if(rsSelectContent.next()) {
        		WebPage= "<input type='hidden' name='btnID_TC' value='"+rsSelectContent.getString("id_tc")+"'>"
        		+"<h2>"+rsSelectContent.getString("title")+"</h2>"
        		+ rsSelectContent.getString("body") +"</td></tr>"
        		+"\n<tr><td class='row2'></td></tr>"
        		+"\n<tr><td class='row1'>"
        		+"<input type='submit' name='back' value='Atras' disabled='disabled'>"
        		+"Editar" 
        		+"<input type='submit' name='next' value='Siguiente'></td></tr>";
        	}
    	
    	}
    	else{
    		
    		int id = Integer.parseInt(id_tc); //valor entero del id_tc para la barra de navegación
    		
    		sqlString = "SELECT id_tc,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_tc='"+(id)+"'";
    		ResultSet rsSelectContent = stmt.executeQuery(sqlString);
    		
    		if(rsSelectContent.next()) {
    			WebPage= "<input type='hidden' name='btnID_TC' value='"+rsSelectContent.getString("id_tc")+"'>"
        		+"<h2>"+rsSelectContent.getString("title")+"</h2>"
        		+ rsSelectContent.getString("body") +"</td></tr>"
        		+"\n<tr><td class='row2'></td></tr>"
        		+"\n<tr><td class='row1'>";
        	}	
        	
        	sqlString = "SELECT id_tc,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_tc='"+(id-1)+"'";
    		rsSelectContent = stmt.executeQuery(sqlString);
	
			if(rsSelectContent.next()) {
				WebPage = WebPage + "<input type='submit' name='back' value='Atras'>"; 
			}
        	else {
        		WebPage = WebPage + "<input type='submit' name='back' value='Atras' disabled='disabled'>";
        	}
        	        	
        	WebPage = WebPage + "Editar";
        	
        	sqlString = "SELECT id_tc,title,body FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_tc='"+(id+1)+"'";
    		rsSelectContent = stmt.executeQuery(sqlString);
	
        	if(rsSelectContent.next()) {
        		WebPage = WebPage + "<input type='submit' name='next' value='Siguiente'></td></tr>";
    		}
    		else {
    			WebPage = WebPage + "<input type='submit' name='next' value='Siguiente' disabled='disabled'>";
    		}
    		
    		
    		WebPage = WebPage + "</td></tr>";
    		
    	}
    	   
       }
    catch(Exception e){
    	WebPage = e.getMessage();
    }
 
 return WebPage;
 }
 

public String getTitleContent(String content_id){
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	ResultSet rsTitleContent = stmt.executeQuery("SELECT title FROM content WHERE content_id='"+content_id+"'");
    	
    	if(rsTitleContent.next()){
    		return(rsTitleContent.getString("title"));
    	}
    }catch(Exception e){
    	e.getMessage();
    } 
	return "";
}

}

