package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.*;
import com.oreilly.servlet.MultipartRequest;


public class ManageFiles extends HttpServlet {

SimpleDateFormat dfMySQLDate = new SimpleDateFormat("yyyy-MM-dd");

//metodo para abstraer las etiquetas de un archivo HTML
public String etiquetas(String Filename, String sectionCourse, String tagSTART, String tagEND){
  	
  	//int parcial,band=0;
	//char car;
	//boolean title=false;
	
	int parcial,band=0;
	String prueba="";
	char car;
	String tag="";
	String Text = "";
	boolean title=false;
	boolean isEndTitle = false;
	
	//StringBuffer bufText = new StringBuffer();
	
	try {
		File file = new File(Course.tempDirectory+"\\"+sectionCourse+"\\"+Filename);
				
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(file);

			while((parcial=fis.read())!=-1)
			{
					car = (char)parcial;
									
					if(car=='<'){
						tag = "";
						band = 1;
						isEndTitle = false;
					}
	
					if(car=='>'){
						band=0;
						tag += car;
						isEndTitle = true;
					}
	
					if(band==1){
						tag += car;
					}
					 
	
					if(!tag.toLowerCase().equals(tagEND) && (isEndTitle)) {
						if(tag.toLowerCase().equals(tagSTART) || (title)){	
							if(car != '>') {
								Text += car;
							}
							else {
								if (!tag.toLowerCase().equals(tagSTART))
									Text += tag;
							}
							title = true;
						}
		
					}
			
					if(tag.toLowerCase().equals(tagEND)){
						break;
					} 
			}
		}
		
	}catch(IOException e) {
		//buf.append(e.getMessage());
	}
  	
  	return Text;
  	
  }
 
//abstrae todo lo que contenga Title de HTML
public String AbstractTitle(String Filename, String sectionCourse){
 		return(etiquetas(Filename,sectionCourse,"<title>","</title>"));	
 }
 
//abstrae todo lo que contenga Body de HTML
public String AbstractBody(String Filename, String sectionCourse){
 		return(etiquetas(Filename,sectionCourse,"<body>","</body>"));	
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
    
    if(request.getParameter("UserRequest") != null){
    	if(request.getParameter("UserRequest").equals("deleteFile")){
    		out.print(request.getParameter("nameFile"));
    		deleteFile(Course.contentDirectory+request.getParameter("nameDirectory")+"\\"+request.getParameter("nameFile"), request.getParameter("nameFile"));
    		response.sendRedirect(Course.name+".ManageFiles");
    	}
    }
    
    out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+">> Subir Archivos</em><br><br>"+manageFilesForm(student.getCourse_id()),student));
	
	
}

public void doPost(HttpServletRequest request,HttpServletResponse response)
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
    
    if(request.getParameter("UserRequest") != null){
    	if(request.getParameter("UserRequest").equals("newdir")){
    		out.print("crear");
    		return;
    	}
    }
	
	try{
		//path directory
		String PathDirectory = Course.contentDirectory+"\\"+student.getCourse_id()+"\\";
		String PathDirectoryWeb = "/"+Course.name+"/Content/"+student.getCourse_id()+"/";
		
		//make directory of content course
		File file = new File(PathDirectory);
    	file.mkdir();
    	
    	MultiPart data = new MultiPart(request,5*1024*1024,PathDirectory, PathDirectoryWeb,student);
    	
    	Enumeration files = data.getFileNames();
    	String name = (String)files.nextElement();
    	String filename = data.getFilesystemName(name);
    	File filesize = data.getFile(name);
    	
    	
    	//eval if file was registered  	
    	if(!data.getisFileRegistered()){
    	
    	if (filesize != null) {
    	
    	   	Date date = new Date(filesize.lastModified());
    	   	uploadTable(filename,filesize.length(),PathDirectoryWeb+filename,date,student.getCourse_id());	
    	}
    	
    	}
    	else {
    		//out.print(confirmFileOverWriter(data.getFilePath()));
    		out.print(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> " 
      	+"<a href='"+Course.name+".Content'>"+getNameCourse(student.getCourse_id())+"</a> >> " 
      	+"<a href='"+Course.name+".Content?content_id="+getContentId(student.getCourse_id(), student.getTheme_id())+"&theme="+student.getTheme_id()+"&userRequest=ContentForm'>"+getTitleContent(student.getCourse_id(), student.getTheme_id())+"</a> "
      	+">> Subir Archivos</em><br><br>"+confirmFileOverWriter(filename, PathDirectoryWeb),student));
    		return;
    	}
    	  	
    	response.sendRedirect(Course.name+".ManageFiles");
	}catch(Exception e) {
    	out.print(e.getMessage());
    }
    
    
}

public String manageFilesForm(String idCourse){
	StringBuffer buf = new StringBuffer();
	//javascript de eliminar archivo
	buf.append("<script LANGUAGE='JavaScript'> "
    +"function confirmSubmitEst(){ var agree=confirm('Ud está seguro de Borrar el archivo del curso?'); "
    +"if (agree) return true ; "
    +"else return false ;} "
    +"</script>");
	
	buf.append(""
	+"<table class='bodyline' cellspacing='0'>\n"
	+"<tr><td colspan='6'><center><strong> Administrador de Archivos </strong></center></td></tr>"
	+"<tr><td colspan='6'><center><small>Usted puede crear carpetas o subir archivos, con esta herramienta y gestionarlos para personalizar el curso </small></center>"
	+"</td></tr>\n"
	+"<tr><td colspan='6' class='row2'></td></tr>\n"
	
	/*+"<form name='form_dir' method='post' action='"+Course.name+".ManageFiles'>\n"
	+"<tr><td colspan='6' class='row1'><br><center><label>Nombre: </label><input type='text' name='dir' size='40'>\n" 
	+"<input class='button' type='submit' name='newdir' value=' Crear Nueva Carpeta'></center>\n"
	+"<input type='hidden' name='UserRequest' value='newdir'><br></td></tr>\n"
	+"<tr><td colspan='6' class='row2'></td></tr></form>\n"*/
	
	+"<form name='form_up' enctype='multipart/form-data' method='post' action='"+Course.name+".ManageFiles'>\n"
	+"<tr><td colspan='6' class='row1'><br><center><label>Nombre: </label><input type='file' name='FileToUpload' size='30'/>\n" 
	+"<input type='submit' value='Upload' class='button'/></center>\n"
	+"<input type='hidden' name='UserRequest' value='upload'></td></tr>\n" 
	+"<tr><td colspan='6' class='row2'></td></tr></form>\n"
	
	+"<tr><td><strong>Tipo</strong></td><td><strong>Nombre</strong></td><td><strong>Tamaño</strong></td><td><strong>Fecha</strong></td><td><strong>Ruta</strong></td><td><strong>Acción</strong></td></tr>"
	+ listFiles(idCourse)+"</table>\n");
return(buf.toString());
}

public String listFiles(String idCourse){

StringBuffer buf = new StringBuffer();
String sqlString = "SELECT * FROM files WHERE idcourse='"+idCourse+"' ORDER BY name";

try{

	Class.forName(Course.jdbcDriver).newInstance();
	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
	Statement stmt = conn.createStatement();
	ResultSet rsAllFiles = stmt.executeQuery(sqlString);
	
	while(rsAllFiles.next()){
	
	String NameFile = rsAllFiles.getString("name");
	NameFile = NameFile.replaceAll(" ","%20");
	
	buf.append("<tr><td colspan='6' class='row2'></td></tr></form>\n"
	+"<tr><td><center><img src=../../Images/t/file.gif border=0></center></td><td>"+rsAllFiles.getString("name")+"</td><td>"+rsAllFiles.getString("size")+"</td><td>"+rsAllFiles.getString("date")+"</td><td>"+rsAllFiles.getString("path")+"</td><td><a href= "+Course.name+".ManageFiles?nameDirectory="+idCourse+"&nameFile="+NameFile+"&UserRequest=deleteFile title='delete' onClick='return confirmSubmitEst()'><center><img src=../../Images/t/delete.gif border=0></center></a></td></tr>\n");	

	}
}catch(Exception e){
}	

return (buf.toString());
}

//inserta en la bd's los archivos subidos al servidor
public void uploadTable(String nameFile, long sizeFile, String pathFile, Date date, String idCourse){
String startDate = dfMySQLDate.format(date);
String sqlString = "INSERT INTO files (filesid,name,size,date,path,idcourse)" 
	      	+ "VALUES ('','"+nameFile+"','"+sizeFile+"','"+startDate+"','"+pathFile+"','"+idCourse+"')"; 
    
	try {
		Class.forName(Course.jdbcDriver).newInstance();
		Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sqlString);
	}
	catch (Exception e) { 
		//e.getMessage();
	}	
    		
}
 
public String warningMessage(){
	    
    return("<form name='alert' method='post' action='"+Course.name+".ManageFiles'>"
    +"<table class='bodyline' cellspacing='0'>\n"
	+"<tr><td><center><strong> Administrador de Archivos </strong></center></td></tr>"
	+"<tr><td><center><small>Ya existe un archivo con en mismo nombre. ¿ Desea reemplazarlo ? </small></center>"
	+"</td></tr>\n"
	+"<tr><td class='row2'></td></tr>\n"
	+"<tr><td class='row1'><INPUT TYPE='Submit' NAME='acept' VALUE='Aceptar'><INPUT TYPE='Submit' NAME='cancel' VALUE='Cancelar'></td></tr>\n"	
	+"</table></form>");
}

//Confirmación de archivo repetido
public String confirmFileOverWriter(String filename, String PathDirectoryWeb){
	StringBuffer buf = new StringBuffer();
	
	
	buf.append("<form name='confirm' method='get' action="+Course.name+".ManageFiles><table border='0' align='center'>\n"
	+"<tr><td>\n"
	+"<div align='center'><strong>El archivo <font color='#FF0000'>"+filename+" </font> que desea subir al\n"
	+" servidor, ya se encuentra en registrado con el mismo nombre en <font color='#FF0000'>"+PathDirectoryWeb+filename+" </font>. Si quiere actualizar el\n"
	+" archivo, busque el archivo por orden alfabetico y luego en acciones de clic\n"
	+" en eliminar, por ultimo vuelva a intentar subir el archivo requerido. </strong></div>\n"
	+"<br><center><input type=submit class='button' value=Volver></center>"
	+"</td></tr></table></form>\n");

return(buf.toString());
}

//Elimina el archivo desde la carpeta del servidor y la base de datos
public void deleteFile(String pathFile, String nameFile){
		
String sqlString = "DELETE FROM files WHERE name='"+nameFile+"'";

try {
	Class.forName(Course.jdbcDriver).newInstance();
	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
	Statement stmt = conn.createStatement();

	stmt.executeUpdate(sqlString);
	
	File file = new File(pathFile);
	file.delete();
		
	

}catch (Exception e) { 
//e.getMessage();
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