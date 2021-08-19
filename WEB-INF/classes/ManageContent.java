package Eledge;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.Enumeration;
import javax.servlet.http.*;
import javax.servlet.*;
import com.oreilly.servlet.MultipartRequest;

public class ManageContent extends HttpServlet {
	
  /*Template tmplate = new Template(); //crea objeto plantilla
  //crea el objeto para traducir*/
private RBStore res = EledgeResources.getManageContentBundle();
  
public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    //para conocer sesion del estudiante
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageContent");
      return;
    }
    
    //para validar si es TA y si cuenta con permisos
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageCourse",request,student,err)) {
      	//mensaje de error, para decir que no tiene permisos para ver
      	//la pagina
      	out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+err.toString(),student));
      	return;
      	}
    }
    
    String userRequest = request.getParameter("userRequest");
    //operaciones generales
    if(userRequest != null) {
		if(userRequest.equals("newThemeForm")){
			out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+addThemeForm(request.getParameter("theme"),student.getCourse_id()),student));
		return;
		}
		if(userRequest.equals("Edit")) {
			String Title = getTitleWebPage(request.getParameter("id_sec"),student.getTheme_id(),student.getCourse_id());
			String Body = getBodyWebPage(request.getParameter("id_sec"), student.getTheme_id(),student.getCourse_id()); 
			int id_tc = getID_TCWebPage(request.getParameter("id_sec"),student.getTheme_id(),student.getCourse_id());
			out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+themeContentForm(false,Title,Body,false,id_tc),student));
			return;
		}
		if(userRequest.equals("deletepage")) {
			
			int id_tc = getID_TCWebPage(request.getParameter("id_sec"),student.getTheme_id(),student.getCourse_id());
			deletePage(id_tc);
			response.sendRedirect(Course.name+".Content");
		}
		if(userRequest.equals("edit_content")) {
			int content_id = Integer.parseInt(request.getParameter("content_id"));
			out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+editThemeForm(content_id),student));
		}
		if(userRequest.equals("delete_content")) {
			int content_id = Integer.parseInt(request.getParameter("content_id"));
			int theme = Integer.parseInt(request.getParameter("theme"));
			deleteContent(content_id,theme);
			response.sendRedirect(Course.name+".Content");
		}
	}
	
	if(request.getParameter("PageID") != null){
		
		if(request.getParameter("PageID").equals("Home")){
			String Body = getBodyWebPage(student.getCourse_id(), request.getParameter("PageID")); 
				
		out.print(Page.create_editor("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "
		+"Editar Pagina Home</em><br><br>"+script()+editThemeForm(Body, student.getCourse_id(), request.getParameter("PageID")),student));			
		}
		
		if(request.getParameter("PageID").equals("Syllabus")){
			String Body = getBodyWebPage(student.getCourse_id(), request.getParameter("PageID")); 
				
		out.print(Page.create_editor("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "
		+"<a href='"+Course.name+".Syllabus'>Instrucciones</a> >>Editar Pagina Instrucciones</em><br><br>"+script()+editThemeForm(Body, student.getCourse_id(), request.getParameter("PageID")),student));			
		}
		
		if(request.getParameter("PageID").equals("Help")){
			String Body = getBodyWebPage(student.getCourse_id(), request.getParameter("PageID")); 
				
		out.print(Page.create_editor("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> "
		+"<a href='"+Course.name+".Help'>Ayuda</a> >>Editar Pagina Ayuda</em><br><br>"+script()+editThemeForm(Body, student.getCourse_id(), request.getParameter("PageID")),student));			
		}	
	}
		
}

public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    TA ta=null;
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageContent");
      return;
    }
    
    //aqui se validan los permisos que puede tener el TA (asistente del profesor)
    //tales como Delete, New, Update, etc. Es decir ManagerContent_New
    if (student.getIsTA()) {
      ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageContent",request,student,err)) {
	out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+err.toString(),student));
	return;
      }
    }
    
    //operaciones como salvar en la base de datos la forma de nuevo tema
    String userRequest = request.getParameter("userRequest");
    String format = request.getParameter("formatting");
    String saved = request.getParameter("Saved");
    String add = request.getParameter("Add");
    String body = request.getParameter("ta"); //ta es el nombre del valor del area de texto
    String title = request.getParameter("title");//title de addThemeForm
    String title1 = request.getParameter("title1"); //title themeContentForm
    String isadd = request.getParameter("isAdd");//String del boton oculto isAdd
    String ID_tc = request.getParameter("id_tc"); //String de ID_tc
    String uploadFiles = request.getParameter("uploadfiles");
    
    boolean isAdd = true; //isAdd de tipo booleano
    int id_tc = 0; //id_tc de tipo entero

	try{
	//valor que toma id_tc para poder realizar actualizaciones
    if (ID_tc != null) 
    	id_tc = Integer.parseInt(ID_tc);
    
    if (userRequest != null) {
    	if(userRequest.equals("newTheme")) {
    		student.setTheme_id(Integer.parseInt(request.getParameter("theme"))+1); //le asigna el tema al estudiante
    		//inserta nuevo tema
    		addTheme(student.getTheme_id(),request.getParameter("pageid"),title,request.getParameter("description"),request.getParameter("courseSection"));
    		
    		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+">> Nuevo</em><br><br>"+themeContentForm(false,"","",isAdd,id_tc),student));
    		return;
    	}
    	if(userRequest.equals("update")) {
    		int content_id = Integer.parseInt(request.getParameter("content_id"));
    		title = request.getParameter("title");
    		String description = request.getParameter("description");
    		
    		updateTheme(content_id,title,description);
    		response.sendRedirect(Course.name+".Content");
    		return;
    	}
    	if(userRequest.equals("updateWebPage")) {
    		updateWebPage(request.getParameter("content_id"),request.getParameter("pageID"), body);
    		response.sendRedirect(Course.name+".ManageContent?PageID="+request.getParameter("pageID"));
    		return;
    	}
    }
    
    //envia al usuario ManageFiles, para gestionar los archivos del contenido
    if(uploadFiles != null){
    	response.sendRedirect(Course.name+".ManageFiles");
    }
        
    //para evaluar el campo oculto de themeContentForm(), cuando save y add son null
    if(isadd != null && isadd.equals("false")) {
    	isAdd = false;
    }
    
    if (saved != null && saved.equals("Save")) {
    	addContentTheme(student.getCourse_id(), student.getTheme_id(), title1, body);
    	isAdd = false;
    	id_tc = valIDContentToTheme(student);
    }
    if (saved != null && saved.equals("Update")) {
    	updateContentTheme(id_tc,title1,body);
    }
    
    if (add != null) {
    	title1 = ""; body = "";  
    	isAdd = true;
    }
 
    if (format.equals("Html")) {
    	out.print(Page.create_editor("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+script()+themeContentForm(true,title1,body,isAdd, id_tc),student));
    	return;    	
    }
    if (format.equals("Text")) {
    	out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+themeContentForm(false,title1,body,isAdd, id_tc),student));
    	return;    	
    }
    
    }catch(Exception e){
    	e.printStackTrace();
    }
    
    try {    
   
    File file = new File(Course.tempDirectory+"\\"+student.getCourse_id()+"\\");
    file.mkdir();
    
    MultipartRequest multi = new MultipartRequest(request,Course.tempDirectory+student.getCourse_id(), 5 * 1024 * 1024);
    
    Enumeration files = multi.getFileNames();
    String name = (String)files.nextElement();
    String filename = multi.getFilesystemName(name);
        
    ManageFiles Abstract = new ManageFiles();
    
    //para conocer si el usuario esta agregando nueva página, mediante el multipart
    if(multi.getParameter("Saved").equals("Save"))
		isAdd = true;
	else
		isAdd = false;
		
		    
    if ((multi.getParameter("id_tc")) != null)  {
		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+themeContentForm(false,Abstract.AbstractTitle(filename,student.getCourse_id()),Abstract.AbstractBody(filename,student.getCourse_id()),isAdd,Integer.parseInt(multi.getParameter("id_tc"))),student));
    }
    else {
        out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+"</em><br><br>"+themeContentForm(false,Abstract.AbstractTitle(filename,student.getCourse_id()),Abstract.AbstractBody(filename,student.getCourse_id()),true,0),student));
    }
    
    }catch (Exception e) {
    	
    }
    
  }
     
//imprime la forma de nuevo tema
public String addThemeForm(String theme, String courseSection) {
  	
  StringBuffer buf = new StringBuffer();
  	
  buf.append("<table cellspacing=0>"
  	+"\n<form name='form1' method='post' action='"+Course.name+".ManageContent'>"
  	+"\n<tr>"
          +"\n<td class='titulo-center'>Tema "+(Integer.parseInt(theme)+1)+"</td>"
    +"\n</tr>"
  	+"\n<tr>"
          +"\n<td class='comentario-center'>Va a agregar un nuevo Tema. Llene los datos del formulario, una vez realizado de clic en el boton siguiente</td>"
    +"\n</tr>"
    +"\n<td><table cellspacing='0' class='bodyline'>"
        +"\n<tr>"
          +"\n<td class='row-center'>Titulo del Tema</td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><input class='formfield' type='text' name='title' size='50'></td>"
        +"\n</tr>"
        +"\n<tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
        +"\n</tr>"
          +"\n<td class='row-center'>Breve descripcion del Tema</td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><textarea class='formfield' name='description' rows='10' cols='50'></textarea></td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
          +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><input type='hidden' name='userRequest' value='newTheme'>"
          +"\n<input type='hidden' name='theme' value='"+theme+"'>"
          +"\n<input type='hidden' name='pageid' value='Content'>"
          +"\n<input type='hidden' name='courseSection' value='"+courseSection+"'>"
          +"\n<div align='center'><input class='button' type='submit' name='Submit' value='Siguiente>'></div></td>"
        +"\n</tr>"
    +"\n</table></td>"
    +"\n</tr>"
    +"\n</form>"
  +"\n</table>");
  	
  	return buf.toString(); 	
  	
  	
  }
  
//imprime la forma de editar un tema
public String editThemeForm(int content_id) {
  	
  StringBuffer buf = new StringBuffer();
  String sqlString;
                  
    sqlString = "SELECT content_id,theme,title,description FROM content WHERE content_id ='"+content_id+"'";   
  	
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectPage = stmt.executeQuery(sqlString);
                
        if(rsSelectPage.next()) {
        	
        	 buf.append("<table cellspacing=0>"
  	+"\n<form name='form1' method='post' action='"+Course.name+".ManageContent'>"
  	+"\n<tr>"
          +"\n<td class='titulo-center'>Tema "+rsSelectPage.getString("theme")+"</td>"
    +"\n</tr>"
  	+"\n<tr>"
          +"\n<td class='comentario-center'>Va a editar un Tema. Llene los datos del formulario, una vez realizado de clic en el boton Actualizar</td>"
    +"\n</tr>"
    +"\n<td><table cellspacing='0' class='bodyline'>"
        +"\n<tr>"
          +"\n<td class='row-center'>Titulo del Tema</td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><input class='formfield' type='text' name='title' size='50' value="+rsSelectPage.getString("title")+"></td>"
        +"\n</tr>"
        +"\n<tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
        +"\n</tr>"
          +"\n<td class='row-center'>Breve descripcion del Tema</td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><textarea class='formfield' name='description' rows='10' cols='50'>"+rsSelectPage.getString("description")+"</textarea></td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
          +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><input type='hidden' name='userRequest' value='update'>"
          +"\n<input type='hidden' name='content_id' value='"+content_id+"'>"
          +"\n<div align='center'><input class='button' type='submit' name='Submit' value='>Actualizar<'></div></td>"
        +"\n</tr>"
    +"\n</table></td>"
    +"\n</tr>"
    +"\n</form>"
  +"\n</table>");

        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
  
  	
 
  	
  	return buf.toString(); 	
  	
  	
  }

/*imprime la forma de añadir y editar contenido al tema seleccionado o creado
isHTML, true si se va a utilizar el editor HTML
Title y Body valores de la pagina que se esta creando
isADD, true si se oprimió el boton agregar y lo deshabilita, por defecto true*/
public String themeContentForm(boolean isHTML, String Title, String Body, boolean isADD, int id_tc) {

//valores que debe tomar si es txt o html	
String radioTxt="";  
String radioHtml="";
//habilita el boton Add y da el valor t/f al campo oculto
String btnAddDisabled = "";
String btnHiddenIsAdd = "<input type='hidden' name='isAdd' value='false'>";
//cambia el valor del boton oculto Saved, entre Save y Update
String btnHiddenChangeValueSaved = "<input type='hidden' name='Saved' value='Update'>";
//valor oculto del campo id_tc de la tabla contenttotheme para a ser actualizaciones
String btnHiddenID_tc = "";

if (isHTML) {

	radioTxt = "\n<input type='radio' name='formatting' value='Text' id='text' disabled='disabled'/>";
	radioHtml = "\n<input type='radio' name='formatting' checked='checked' value='Html' id='html'/>";
}
else {
	radioTxt = "\n<input type='radio' name='formatting' value='Text' id='text' checked='checked' onclick='javascript: document.form.setvisual.disabled=true;'  />";	
	radioHtml = "\n<input type='radio' name='formatting' value='Html' id='html'  onclick='javascript: document.form.setvisual.disabled=false;'/>";
}
if (isADD){
	btnHiddenIsAdd = "<input type='hidden' name='isAdd' value='true'>";
	btnAddDisabled = "disabled='disabled'";
	btnHiddenChangeValueSaved = "<input type='hidden' name='Saved' value='Save'>";
}	

if (id_tc != 0) {
	btnHiddenID_tc = "<input type='hidden' name='id_tc' value='"+id_tc+"'>";
}
String Form = (" "
	+"\n<table>"
	+"\n<tr><td><center><strong>Crear o Editar Temas del Contenido</strong></center></td></tr>"
	+"\n<tr><td class='comentario-center'> En esta Forma puede Editar o Agregar nuevas páginas web, para el respectivo tema del contenido del curso."
	+"\n Para los anteriores procesos, lo puede realizar de dos formas. 1. Si ya tiene creado un archivo HTML, lo puede importar dando clic en examinar, buscar el archivo HTML"
	+"\n deseado y luego da clic en Subir. 2. Puede editar o agregar la página web inmediatamente desde el editor HTML. En ambos pasos para salvar"
	+"\n dar clic en guardar y si desea agregar nuevo tema de clic en Agregar Nueva Página. </td></tr>"
	+"\n</table>"
//crea tabla
+"\n<table class='bodyline' cellspacing='0'>"
	+"\n<tr><form name='form2' enctype='multipart/form-data' method='post' action='Eledge.ManageContent'>"
		+"\n<td class='row1'><strong>Pegar desde el archivo:</strong></td>"
		+"\n<td class='row1'><input type='file' name='FileToUpload' class='formfield' size='20' /> <input type='submit' value='Subir' class='button'/><br/ > "
		+btnHiddenID_tc+btnHiddenIsAdd + btnHiddenChangeValueSaved //boton id_tc oculto
        +"\n<small>&middot; Archivos HTML solamente.<br />"
        +"\n&middot; Despues de subir use los campos de abajo para editar el contenido.</small>"
	+"\n</form></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	
	+"<form name='form' method='post' action='Eledge.ManageContent'>"
	
	//botones de las acciones
	+"\n<tr><td colspan='2' class='saved'><strong>Acciones:</strong><input class='button' type='submit' name='Save' value='Guardar'>"
	+btnHiddenIsAdd + btnHiddenID_tc + btnHiddenChangeValueSaved //botones ocultos
	+"<input class='button' type='submit' name='Add' value='Añadir Nueva Página' "+btnAddDisabled+"><input class='button' type='submit' name='uploadfiles' value='Subir archivos al curso'></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	
	/*+"\n<tr><td colspan='2' class='saved'><center><strong>acciones:       <input class='button' type='submit' name='Save' value='Save'>"
	+btnHiddenIsAdd + btnHiddenID_tc + btnHiddenChangeValueSaved //botones ocultos
	+"      <input class='button' type='submit' name='Add' value='Añadir Nueva Página' "+btnAddDisabled+">   <input class='button' type='submit' name='uploadfiles' value='Subir archivos al curso'></strong></center></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"*/
	
	+"\n<tr>"
      +"\n<td colspan='2' class='row1'><strong>Título: </strong>" 
        +"\n<input name='title1' type='text' size='80' value='"+Title+"'></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	+"\n<tr><td colspan='2' class='row1'><strong>Formato: </strong>" 
                    +radioTxt
                    +"\nTexto Plano, "
                    +radioHtml
                    +"\nHTML "
                    +"\n<input type='submit' name='setvisual' value='Activar editor HTML' class='button' disabled='disabled'/></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	+"\n<tr><td colspan='2' class='row1'><strong>Cuerpo del documento:</strong><br><textarea name='ta' rows='20' class='formfield' id='ta' style='width=100%'>"+Body+"</textarea></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	
		//botones de las acciones
	+"\n<tr><td colspan='2' class='saved'><strong>Acciones:</strong><input class='button' type='submit' name='Save' value='Guardar'>"
	+btnHiddenIsAdd + btnHiddenID_tc + btnHiddenChangeValueSaved //botones ocultos
	+"<input class='button' type='submit' name='Add' value='Añadir Nueva Página' "+btnAddDisabled+"><input class='button' type='submit' name='uploadfiles' value='Subir archivos al curso'></td></tr>"
	+"\n<tr><td colspan='2' class='row2'></td></tr>"
	
	+"\n</form>"

+"\n</table>");
	
return Form;
	
	
}

//actualiza el tema  
public void updateTheme(int content_id, String title, String description) {
	String sqlString; 
    
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		
      		sqlString = "UPDATE content SET title='"+title+"',description='"+description+"' WHERE content_id='"+content_id+"'";	
      		stmt.executeUpdate(sqlString);
  
      	}catch (Exception e) { 
    	e.getMessage();
    	}
    
}

//elimina el contenido del tema y el tema por ultimo
public void deleteContent(int content_id, int theme) {
	String sqlString; 
    
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		
      		sqlString = "SELECT coursesection FROM content WHERE content_id='"+content_id+"'";
      		ResultSet rsSection = stmt.executeQuery(sqlString);
      		
      		if (rsSection.next()) {
      			sqlString = "DELETE FROM contenttotheme WHERE coursesection_id='"+rsSection.getString("coursesection")+"' and id_theme='"+theme+"'";
      			stmt.executeUpdate(sqlString);	
      		}
  			
  			sqlString = "DELETE FROM content WHERE content_id='"+content_id+"'";	
      		stmt.executeUpdate(sqlString);
      	
      	}catch (Exception e) { 
    	e.getMessage();
    	}
}

//elimina la pagina web seleccionada
public void deletePage(int id_tc) {
	String sqlString; 
    
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		
  			sqlString = "DELETE FROM contenttotheme WHERE id_tc='"+id_tc+"'";	
      		stmt.executeUpdate(sqlString);
      	
      	}catch (Exception e) { 
    	e.getMessage();
    	}
}

 //inserta en la base de datos, los datos para crear un nuevo tema al curso
public void addTheme(int theme, String pageID, String title, String description, String courseSection) {
    String sqlString; 
              
    if (!evalTheme(theme,pageID,title,description,courseSection))
    {
    	sqlString = "INSERT INTO content (content_id,theme,page_id,title,description,coursesection)" 
      	+ "VALUES ('','"+theme+"','"+pageID+"','"+title+"','"+description+"','"+courseSection+"')"; 
    
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		stmt.executeUpdate(sqlString);
      	}catch (Exception e) { 
    	e.getMessage();
    	}
    }
}

 //inserta en la base de datos, los datos a la tabla addcontenttheme, es decir el contenido(s) del tema
public void addContentTheme(String courseSection, int theme, String title, String body) {
    String sqlString = ""; 
    
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		      		
      		ResultSet rsSec = stmt.executeQuery("SELECT MAX(id_sec) FROM contenttotheme WHERE coursesection_id='"+courseSection+"' and  id_theme='"+theme+"'");
      		
      		//asigna un numero conscutivo con el numero max de los registro del contenido del tema
      		     		
      		try {
      		
      			if (rsSec.next()) {
      				int id_sec = (Integer.parseInt(rsSec.getString("MAX(id_sec)"))+1);
      				sqlString = "INSERT INTO contenttotheme (id_tc,coursesection_id,id_theme,title,body,id_sec)" 
      					   +"VALUES ('','"+courseSection+"','"+theme+"','"+title+"','"+body+"','"+id_sec+"')";
      			}
      		
      		}catch (Exception e) {
      			sqlString = "INSERT INTO contenttotheme (id_tc,coursesection_id,id_theme,title,body,id_sec)" 
      					   +"VALUES ('','"+courseSection+"','"+theme+"','"+title+"','"+body+"','1')";    			
      		}
      		finally {
      			stmt.executeUpdate(sqlString);

      		}
      	      		
      	}catch (Exception e) { 
    		e.getMessage();
    	}
   //}
}

 //actualiza en la base de datos, los datos de la tabla addcontenttheme, es decir el contenido(s) del tema
public void updateContentTheme(int id_tc, String title, String body) {
    String sqlString; 
   
    	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		
      		sqlString = "UPDATE contenttotheme SET title='"+title+"',body='"+body+"' WHERE id_tc='"+id_tc+"'";	
      		stmt.executeUpdate(sqlString);
      		      		
      	}catch (Exception e) { 
    	e.getMessage();
    	}
   
}

//examina si ya se inserto el tema, true si ya se inserto
public boolean evalTheme(int theme, String pageID, String title, String description, String courseSection) {
    String sqlString;
    boolean eval=false; //falso si no se ha creado el tema 
              
    sqlString = "Select * from content where theme='"+theme+"' and page_id='"+pageID+"' and title='"+title+"' and description='"+description+"' and coursesection='"+courseSection+"'";
    
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsAllCourses = stmt.executeQuery(sqlString);
                
        if(rsAllCourses.next()) {
        	eval=true;
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return eval;
 }
 
 //retorna el valor del campo autonumerico de la tabla contenttotheme, de la fila insertada
public int valIDContentToTheme(Student student) {
    String sqlString;
    int id_tc = 0;
                  
    sqlString = "Select MAX(id_tc) from contenttotheme where coursesection_id ='"+student.getCourse_id()+"' and id_theme ='"+student.getTheme_id()+"'";
     
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsID = stmt.executeQuery(sqlString);
                
        if(rsID.next()) {
        	id_tc=rsID.getInt("MAX(id_tc)");
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return id_tc;
 }

//Script para el editor HTML
public String script() {
	
	String spt = ("\n<script type='text/javascript'>"
  +"\n_editor_url = '"+Course.server2+"/editor/';"
  +"\n_editor_lang = 'en';"
+"\n</script>"
+"\n<script type='text/javascript' src='"+Course.server2+"/editor/htmlarea.js'></script>"

+"\n<script type='text/javascript'>"
+"\nvar editor = null;"
+"\nfunction initEditor() {"
  +"\neditor = new HTMLArea('ta');"

  // comment the following two lines to see how customization works
 +"\n editor.generate();"
  +"\nreturn false;"

  +"\nvar cfg = editor.config;" // this is the default configuration
  +"\ncfg.registerButton({"
    +"\nid        : 'my-hilite',"
   +"\n tooltip   : 'Highlight text',"
   +"\n image     : 'ed_custom.gif',"
  +"\n  textMode  : false,"
   +"\n action    : function(editor) {"
  +"\n                editor.surroundHTML('<span class=\\'hilite\\'>', '</span>');"
 +"\n               },"
  +"\n  context   : 'table'"
+"\n  });"

 +"\n cfg.toolbar.push(['linebreak', 'my-hilite']);" // add the new button to the toolbar

  // BEGIN: code that adds a custom button
  // uncomment it to test
 +"\n var cfg = editor.config; // this is the default configuration"

+"\ncfg.registerButton('my-sample', 'Class: sample', 'ed_custom.gif', false,"
 +"\n function(editor) {"
  +"\n  if (HTMLArea.is_ie) {"
  +"\n    editor.insertHTML('<span class=\\'sample\\'>&nbsp;&nbsp;</span>');"
   +"\n   var r = editor._doc.selection.createRange();"
   +"\n   r.move('character', -2);"
   +"\n   r.moveEnd('character', 2);"
   +"\n   r.select();"
  +"\n  } else { // Gecko/W3C compliant"
   +"\n   var n = editor._doc.createElement('span');"
   +"\n   n.className = 'sample';"
   +"\n   editor.insertNodeAtSelection(n);"
   +"\n   var sel = editor._iframe.contentWindow.getSelection();"
   +"\n   sel.removeAllRanges();"
    +"\n  var r = editor._doc.createRange();"
    +"\n  r.setStart(n, 0);"
    +"\n  r.setEnd(n, 0);"
    +"\n  sel.addRange(r);"
  +"\n  }"
 +"\n }"
+"\n);"


  /*
  cfg.registerButton("my-hilite", "Highlight text", "ed_custom.gif", false,
    function(editor) {
      editor.surroundHTML('<span class="hilite">', '</span>');
    }
  );
  */
  
 +"\n editor.generate();"
+"\n}"
+"\nfunction insertHTML() {"
 +"\n var html = prompt('Enter some HTML code here');"
  +"\nif (html) {"
  +"\n  editor.insertHTML(html);"
 +"\n }"
+"\n}"
+"\nfunction highlight() {"
 +"\n editor.surroundHTML('<span style=background-color: yellow>', '</span>');"
+"\n}"
+"\n</script>");
	
	
	return spt.toString();
	
}

//extrae el titulo de la Pagina Web
public String getTitleWebPage(String id_sec, int theme, String courseSection) {
    String sqlString;
    String Title = "";
                  
    sqlString = "SELECT title FROM contenttotheme WHERE  coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_sec='"+id_sec+"'";   
  	
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectPage = stmt.executeQuery(sqlString);
                
        if(rsSelectPage.next()) {
        	Title = rsSelectPage.getString("title");
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return Title;
 }
 
//extrae el body de la Pagina Web
public String getBodyWebPage(String id_sec, int theme, String courseSection) {
    String sqlString;
    String Body = "";
                  
    sqlString = "SELECT body FROM contenttotheme WHERE  coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_sec='"+id_sec+"'";   
  	
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectPage = stmt.executeQuery(sqlString);
                
        if(rsSelectPage.next()) {
        	Body = rsSelectPage.getString("body");
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return Body;
 }
 
 //extrae el id_tc de la Pagina Web
public int getID_TCWebPage(String id_sec, int theme, String courseSection) {
    String sqlString;
    int ID_TC = 0;
                  
    sqlString = "SELECT id_tc FROM contenttotheme WHERE  coursesection_id='"+courseSection+"' and  id_theme='"+theme+"' and id_sec='"+id_sec+"'";   
  	
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectPage = stmt.executeQuery(sqlString);
                
        if(rsSelectPage.next()) {
        	ID_TC = Integer.parseInt(rsSelectPage.getString("id_tc"));
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return ID_TC;
 }

//extrae los datos de la pagina web requerida
//extrae el body de la Pagina Web
public String getBodyWebPage(String courseSection, String pageID) {
    String sqlString;
    String Body = "";
    
    if (courseSection==null) courseSection = "0";
                  
    sqlString = "SELECT HTMLText FROM webpages WHERE  CourseSection='"+courseSection+"' and PageID='"+pageID+"'";   
  	
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectPage = stmt.executeQuery(sqlString);
                
        if(rsSelectPage.next()) {
        	Body = rsSelectPage.getString("HTMLText");
        }
    	      
       }
    catch(Exception e){
    	e.getMessage();
    }
 
 return Body;
 }
 
//imprime la forma de editar una pagina web
public String editThemeForm(String Body, String CourseID, String PageID) {
  	
  StringBuffer buf = new StringBuffer();
  String sqlString;
  
  if (CourseID==null) CourseID = "0";
                   
    buf.append("<table cellspacing=0>"
  	+"\n<form name='form1' method='post' action='"+Course.name+".ManageContent'>"
  	
  	+"\n<tr>"
          +"\n<td class='comentario-center'>Va a editar la Página Web. El Editor HTML lo ayudará a editarla, una vez realizado de clic en el boton Actualizar</td>"
    +"\n</tr>"
    +"\n<td><table cellspacing='0' class='bodyline'>"
        +"\n<tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
        +"\n</tr>"
          +"\n<td class='row-center'>Edición de la Página Web</td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><textarea name='ta' rows='20' class='formfield' id='ta' style='width=100%'>"+Body+"</textarea></td>"
        +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row2'></td>"
          +"\n</tr>"
        +"\n<tr>"
          +"\n<td class='row-center'><input type='hidden' name='userRequest' value='updateWebPage'>"
          +"\n<input type='hidden' name='content_id' value='"+CourseID+"'>"
          +"\n<input type='hidden' name='pageID' value='"+PageID+"'>"
          +"\n<div align='center'><input class='button' type='submit' name='Submit' value='>Actualizar<'></div></td>"
        +"\n</tr>"
    +"\n</table></td>"
    +"\n</tr>"
    +"\n</form>"
  +"\n</table>");
      	
  
  return buf.toString();
  	
  }


//actualiza el tema  
public void updateWebPage(String content_id, String PageID, String body) {
	String sqlString; 
    
      	try {
      		Class.forName(Course.jdbcDriver).newInstance();
      		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      		Statement stmt = conn.createStatement();
      		
      		sqlString = "UPDATE webpages SET HTMLText ='"+body+"' WHERE CourseSection= '"+content_id+"' and PageID = '"+PageID+"' ";	
      		stmt.executeUpdate(sqlString);
  
      	}catch (Exception e) { 
    	e.getMessage();
    	}
    
}

public String getTitleContent(String course_id, int theme){
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	ResultSet rsTitleContent = stmt.executeQuery("SELECT title FROM content WHERE coursesection='"+course_id+"' AND theme='"+theme+"'");
    	
    	if(rsTitleContent.next()){
    		return(rsTitleContent.getString("title"));
    	}
    }catch(Exception e){
    	e.getMessage();
    } 
	return "";
  }
  
  public int getContentId(String course_id, int theme){
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	ResultSet rsContentId = stmt.executeQuery("SELECT content_id FROM content WHERE coursesection='"+course_id+"' AND theme='"+theme+"'");
    	
    	if(rsContentId.next()){
    		return(rsContentId.getInt("content_id"));
    	}
    }catch(Exception e){
    	e.getMessage();
    } 
	return 0;
}

private String getNameCourse (String idCourse){
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

}