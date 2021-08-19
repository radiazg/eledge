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

public class Prueba1 extends HttpServlet {
  Hashtable table=new Hashtable();
  String fileLocation = Course.contentDirectory; //crea la localizacion del directorio
  Logger log = new Logger();
  Template tmplate = new Template(); //crea objeto plantilla
  //crea el objeto para traducir
  private RBStore res = EledgeResources.getManageContentBundle();
  private String contenido = "";
  
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
    /*if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Prueba");
      return;
    }
    
    //para validar si es TA y si cuenta con permisos
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("ManageCourse",request,student,err)) {
      	//mensaje de error, para decir que no tiene permisos para ver
      	//la pagina
      	out.println(Page.create(err.toString(),student));
      	return;
      	}
    }
    
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_teach"),student));
      return;
    }
    
    
    // from here on, user is assumed to be the instructor
    if (request.getParameter("PageID")!= null) 
      //imprime la pagina de edicion de html (por paragrafos)
      out.println(Page.create(editPage(student,request.getParameter("PageID")),student));
    else
      //imprime la pagina de subir archivos
      out.println(Page.create(addEditPageForm(),student));
    
    
    if (student.getIsTA()) {
      student.setIsInstructor(false);
    }
    */ 
    //borrar
    out.println(Page.create(addEditPageForm(),student));
    
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    /*HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    TA ta=null;
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    /*if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Prueba");
      return;
    }
    
    //aqui se validan los permisos que puede tener el TA (asistente del profesor)
    //tales como Delete, New, Update, etc. Es decir ManagerContent_New
    if (student.getIsTA()) {
      ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Prueba",request,student,err)) {
	out.println(Page.create(err.toString(),student));
	return;
      }
    }
           
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_must_be_teach"),student));
      return;
    }*/
    // from here on, user is assumed to be the instructor
    //valida si cuenta con permisos para realizar las operaciones
    /*String userRequest = request.getParameter("UserRequest");
    if (userRequest==null) { // this is an upload request //para subir archivos
      //si es ta diferente a null quiere decir que no es profesor
      //es el asistente del profesor
      if (ta != null) {
        //examina permisos para subir archivos
        if (ta.getPermission("ManageContent_Upload").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)
          || ta.getPermission("ManageContent_Upload").getPermissionLevel().equals(TAPermission.PERM_ALL))
          //out.println(Page.create(uploadFile(request,response),student));
        	{uploadFile(request,response);
      		out.print(abstractSelectText("<title>","</title>"));}
        else
          out.println(Page.create(Permissions.getPermission("Prueba").getDenyMsg(),student));
      }
      else {
        //out.println(Page.create(uploadFile(request,response),student));
      	uploadFile(request,response);
      	out.print(abstractSelectText("<title>","</title>"));
      } 
    }
    else 
      out.println(Page.create(addEditPageForm(),student));    
    if (student.getIsTA())
      student.setIsInstructor(false);
  */}

  //subir archivos //arreglado y funciona muy bien
  public void uploadFile(int contentLength, String contentType, ServletInputStream inputStream)
    throws IOException {
    //PrintWriter out = response.getWriter();
    this.contenido = "";
    StringBuffer err = new StringBuffer();
    //String serverFilename = "";
    //archivo contenido = new archivo();
    
    int numFiles=0;//this is for a terrible hack for making sure that only "true" errors get reported. ;)
    //int contentLength=request.getContentLength();
    //int contentLength=contentLength;
    //String contentType = request.getContentType();
    //String contentType = contentType;
    int ind = contentType.indexOf("boundary=");
    
    /*if (ind == -1) 
    	return res.getString("str_nofile");*/
    
    String boundary = contentType.substring(ind+9);
    
    /*if (boundary == null) 
    	return res.getString("str_nobound");*/
    
    String boundaryString = "--"+boundary;
    ServletInputStream in = inputStream;
    //ServletInputStream in = request.getInputStream();
    
    byte[] buffer = new byte[1024];
    
    //table.clear();
    
    int result=readLine(in, buffer, 0, buffer.length);
    
    outer: while (true) {
      			if (numFiles > 0) 
      				break;
      			if (result<=0) {
        			err.append(res.getString("str_err") + res.getString("str_err_stream_short"));
        			break;
      			}
    
    String line = new String(buffer, 0, result);
        
    //error1   
    if (!line.startsWith(boundaryString)) {
        err.append(res.getString("str_err") + res.getString("str_err_no_mime_bound"));
        break;
    }
    
    result=readLine(in, buffer, 0, buffer.length);
        
    //error2
    if (result<=0) {
        err.append(res.getString("str_err") + res.getString("str_err_no_mime_content"));
        break;
    }
    
    line = new String(buffer, 0, result);
    StringTokenizer tokenizer=new StringTokenizer(line, ";\r\n");
    String token=tokenizer.nextToken();
    String upperToken = token.toUpperCase();
        
    if (!upperToken.startsWith("CONTENT-DISPOSITION")) {
        err.append(res.getString("str_err") + res.getString("str_err_format"));
        break;
    }
    
    String disposition = upperToken.substring(21);
    
    if (!disposition.equals("FORM-DATA")) {
        err.append(res.getString("str_err") + res.getString("str_err_no_handle"));
        break;
    }
    
    if (tokenizer.hasMoreElements())
    	token=tokenizer.nextToken();
    else {
    	err.append(res.getString("str_err") + res.getString("str_err_no_name"));
        break;
    }
    
    int nameStart=token.indexOf("name=\"");
    int nameEnd=token.indexOf("\"", nameStart+7);
    
    if (nameStart<0 || nameEnd<0) {
        err.append(res.getString("str_err") + res.getString("str_err_no_name"));
        break;
    }
    
    //String name=token.substring(nameStart+6, nameEnd);
    if (tokenizer.hasMoreElements()) {
        token=tokenizer.nextToken();
        int tokenStart=token.indexOf("filename=\"");
        int tokenEnd=token.indexOf("\"", tokenStart+11);
        
        if (tokenStart<0 || tokenEnd<0) {
        	err.append(res.getString("str_err") + res.getString("str_err_no_filename"));
        	break;
    	}
    
    String filename=token.substring(tokenStart+10, tokenEnd);
    int lastindex=-1;
    
    if ((lastindex=filename.lastIndexOf('/'))<0)
        lastindex=filename.lastIndexOf('\\');
    
    if (lastindex>=0)
    	filename=filename.substring(lastindex+1);
        
    //FileOutputStream f_out;
    //serverFilename = getValue("ServerFilename");
    
    //if (serverFilename == null || serverFilename.equals("")) 
    	//serverFilename = filename; // if the file is not renamed in the form,
    
    /*try {  // use the same name as on the client
    	//f_out=new FileOutputStream(fileLocation+'/'+serverFilename);
    }catch (Exception e) {
    	err.append(res.getString("str_err") + e.getMessage());
        break;
    }*/
    
    //appendValue(name, filename, true);
    result=readLine(in, buffer, 0, buffer.length);
    if (result<=0) {
        err.append(res.getString("str_err") + res.getString("str_err_stream_short") + " 1");
        break;
    }
    
    int size=0;
    try {
    	//byte[] tmpbuffer=new byte[buffer.length];
    	//int tmpbufferlen=0;
    	boolean isFirst=true;
    	inner: while ((result=readLine(in, buffer, 0, buffer.length))>0) {
    				
    				String tmp=new String(buffer, 0, result);
    				if (isFirst) { // ignore all proceeding \r\n
              			if (result==2 && buffer[0]=='\r' && buffer[1]== '\n') 
              				continue;
            		 takeWebPage(tmp);
            		}
                        		
            		if (tmp.startsWith(boundaryString)) {
              			if (!isFirst) {
                			//size+=tmpbufferlen-2;
                    		//f_out.write(tmpbuffer, 0, tmpbufferlen-2);
              			}
              		numFiles++;
              		//out.print(contenido.bye());
              		//f_out.close(); //añadido 5/agost/2004
              		continue outer;
            		}
            		else{
              			if (!isFirst) {
                			//size+=tmpbufferlen; 
                			//f_out.write(tmpbuffer, 0, tmpbufferlen);
                			takeWebPage(tmp);
              			}
              			
            		}
            
            		//System.arraycopy(buffer, 0, tmpbuffer, 0, result);
            		//tmpbufferlen=result;
            		isFirst=false;
          		}		
          		
        }catch (IOException e) {
         	err.append(res.getString("str_err") + e.getMessage());
        }catch (Exception e) {
          err.append(res.getString("str_err") + e.getMessage());
        }finally{
        }
        result=readLine(in, buffer, 0, buffer.length); 
                         
    }
    else { // no more elements
    	result=readLine(in, buffer, 0, buffer.length);
    	
    	if (result<=0) {
    		err.append(res.getString("str_err") + res.getString("str_err_stream_short") + "2");
    		break;
    	}
    	result=readLine(in, buffer, 0, buffer.length);
    	
    	if (result<=0) {
    		err.append(res.getString("str_err") + res.getString("str_err_stream_short") + "3");
    		break;
        }
       	//String value = new String(buffer, 0, result-2); // exclude \r\n
        //appendValue(name, value, false);
    }
    
    result=readLine(in, buffer, 0, buffer.length); 
}// end of while

//table.clear();

    /*if (err.length()>0) {
    	return err.toString();
    }*/
    
    //return (abstractSelectText("<body>","</body>"));
  }
  
  
  //la interfase de administracion de contenido, necesaria para subir
  //archivos
  String addEditPageForm() { 
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_browse")); 
    Object[] args = {
      "<I>",
      "</I>",
      "<BR>"
    };
    
    buf.append("<h2>" + res.getString("str_title_manage_content") + "</h2>"
    + "<h3>" + res.getString("str_title_upload") + "</h3>"
    + res.getString("str_explain_upload"));

    //the encoding type is multipart/form-data for file uploads
    buf.append("<FORM ENCTYPE='multipart/form-data' METHOD=POST>");
    // ServerFilename form element must precede FILE form element or local file name will be used
    buf.append("<FONT SIZE=-1 COLOR=#FF0000>" + mf.format(args) + "</FONT><br>");
    
    buf.append(res.getString("str_field_filename") 
     + "<i>" + Course.server+"/"+Course.name+"/Content/</i><INPUT NAME='ServerFilename'>");
    buf.append("<INPUT TYPE=FILE NAME='FileToUpload' SIZE=50 MAXLENGTH=255>");
    buf.append("<br><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_upload") + "'>");
    buf.append("</FORM>" + "\n");
    
    mf.applyPattern(res.getString("str_explain_dynamic"));
    buf.append("<h3>" + res.getString("str_title_html") + "</h3>" 
    + mf.format(args));
    
    buf.append("<p>" + res.getString("str_enter_pageid"));
    buf.append("<FORM NAME=NewPage METHOD=POST>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest VALUE=New>"
    + "<INPUT SIZE=10 NAME=PageID Value=");
    
// the following section finds the next higher unique PageID of the form PageXXX to use as the default  
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPages = stmt.executeQuery("SELECT PageID FROM WebPages WHERE PageID LIKE 'Page%' ORDER BY PageID");
      if (rsPages.last()) {
        StringBuffer newPage = new StringBuffer("Page" + (Integer.parseInt(rsPages.getString("PageID").substring(4))+1));
        while (newPage.toString().length() < 7) newPage.insert(4,"0");
        buf.append(newPage.toString() + ">");
      }
      else buf.append("Page001>");
    }
    catch (Exception e) {
      buf.append("Page001>");
    }
    buf.append("<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_create_new") 
    + "'></FORM>");

    buf.append(res.getString("str_select_below"));
    buf.append("<FORM NAME=EditPage METHOD=POST>"
    + "<INPUT TYPE=HIDDEN NAME=UserRequest>");
    buf.append("<TABLE CELLSPACING=0 BORDER=1><TR><TD></TD><TH>"
    + res.getString("str_field_pageid") + "</TH><TH>" + res.getString("str_field_url") + "</TH></TR>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPages = stmt.executeQuery("SELECT PageID FROM WebPages GROUP BY PageID ORDER BY PageID");
      while (rsPages.next()) {
        String pageID = rsPages.getString("PageID");
        buf.append("<TR><TD><INPUT TYPE=RADIO NAME=PageID VALUE=" + pageID + "></TD>"
        + "<TD>" + pageID 
        + "</TD><TD><a href='/servlet/" + Course.name + ".Page?ID=" + pageID + "'>" 
        + "/servlet/" + Course.name + ".Page?ID=" + pageID + "</a></TD></TR>");
      }
    }
    catch (Exception e) {
      return e.getMessage();
    }    
    buf.append("</TABLE>");
    buf.append("<INPUT TYPE=BUTTON VALUE='" + res.getString("str_btn_edit_page") 
    + "' onClick=\"document.EditPage.UserRequest.value='Edit'; "
    + "if (!isChecked()) alert('" + res.getString("str_must_select_edit") 
    + "');else document.EditPage.submit();\">");
    buf.append("&nbsp;"); // put a space between buttons
    buf.append("<INPUT TYPE=BUTTON VALUE='" + res.getString("str_btn_del_pg") 
    + "' onClick=\"document.EditPage.UserRequest.value='Delete'; "
    + "if (!isChecked()) alert('" + res.getString("str_must_select_del") 
    + "');else if (confirm('" + res.getString("str_confirm_del") 
    + "')) EditPage.submit();\">");
    buf.append("</FORM>");
    buf.append("<SCRIPT LANGUAGE=Javascript>"
    + "function isChecked() {"
    + "var checked=false;"
    + "if (document.EditPage.PageID!=undefined) {" 
    + "  if (document.EditPage.PageID.length!=undefined) {"
    + "    for (i=0;i<document.EditPage.PageID.length;i++) if (document.EditPage.PageID[i].checked) checked=true;}"
    + "  else if (document.EditPage.PageID.checked) checked=true;"
    + " } return checked;}"
    + "</SCRIPT>");
    return buf.toString();
  }
  
  int readLine(ServletInputStream in, byte[] b, int off, int len)
    throws IOException {
    if (len <= 0) {
      return 0;
    }
    int count = 0, c;
    while ((c = in.read()) != -1) {
      b[off++] = (byte)c;
      count++;
      if ((c == '\n') || (count==len)) {
        break;
      }
    }
    return count > 0 ? count : -1;
  }
  

/*class archivo {
	String contenido="";*/
	
public void takeWebPage(String Contenido){
  	this.contenido = this.contenido + Contenido;
  	}
public String abstractSelectText(String tagINI, String tagFIN) {
  	return abstractText(this.contenido,tagINI, tagFIN);
  	}
public String abstractText(String sTexto, String tagINI, String tagFIN) {

// Cadena de texto sin blancos
int parcial,band=0;
char car;
String tag="";
boolean title=false;
String Text="";
boolean isEndTitle = false;
	
for (int x=0; x < sTexto.length(); x++) {
	car = sTexto.charAt(x);
			
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
	
	if(!tag.equals(tagFIN) && (isEndTitle)) {
		if(tag.equals(tagINI) || (title)){	
			if(car != '>') {
				Text += car;
			}
			else {
				if (!tag.equals(tagINI))
					Text += tag;
			}
			title = true;
		}
		
	}
			
	if(tag.equals(tagFIN)){
		break;
	} 
}

return (Text);
//}



}
  
class UploadContent {
  String name;
  String value;
  boolean isFile;
  String contenido;

  UploadContent(String name, String value, boolean isFile) {
    this.name=name;
    this.value=value;
    this.isFile=isFile;
  }

}


}
