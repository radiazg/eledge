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

public class Prueba extends HttpServlet {
  Hashtable table=new Hashtable();
  String fileLocation = Course.contentDirectory; //crea la localizacion del directorio
  Logger log = new Logger();
  Template tmplate = new Template(); //crea objeto plantilla
  //crea el objeto para traducir
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
    
    /*
    // from here on, user is assumed to be the instructor
    if (request.getParameter("PageID")!= null) 
      //imprime la pagina de edicion de html (por paragrafos)
      out.println(Page.create(editPage(student,request.getParameter("PageID")),student));
    else
      //imprime la pagina de subir archivos
      out.println(Page.create(addEditPageForm(),student));
    */
    
    if (student.getIsTA()) {
      student.setIsInstructor(false);
    }
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    /*TA ta=null;
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    if (!student.isAuthenticated()) {
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
    }
    // from here on, user is assumed to be the instructor
    //valida si cuenta con permisos para realizar las operaciones
    String userRequest = request.getParameter("UserRequest");
    if (userRequest==null) { // this is an upload request //para subir archivos
      //si es ta diferente a null quiere decir que no es profesor
      //es el asistente del profesor
      if (ta != null) {
        //examina permisos para subir archivos
        if (ta.getPermission("ManageContent_Upload").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)
          || ta.getPermission("ManageContent_Upload").getPermissionLevel().equals(TAPermission.PERM_ALL))
          out.println(Page.create(uploadFile(request,response),student));
        else
          out.println(Page.create(Permissions.getPermission("Prueba").getDenyMsg(),student));
      }
      else
      out.println(Page.create(uploadFile(request,response),student));
       
    }
    else if (userRequest.equals("Delete")) {
      deletePage(request.getParameter("PageID"));
      out.println(Page.create(addEditPageForm(),student));
    } 
    else if (userRequest.equals("Edit")) {
      out.println(Page.create(editPage(student,request.getParameter("PageID")),student));
    } 
    else if ((userRequest.equals("SaveEdits"))||(userRequest.equals("Save"))) {
      out.println(Page.create(saveEdits(request.getParameter("PageID"),request,response, student),student));
    } 
    else if (userRequest.equals("New")) {
      out.println(Page.create(addPage(student, request.getParameter("PageID")),student));
    } 
    else 
      out.println(Page.create(addEditPageForm(),student));    
    if (student.getIsTA())
      student.setIsInstructor(false);
 */ }
/*
  //subir archivos //arreglado y funciona muy bien
  String uploadFile(HttpServletRequest request, HttpServletResponse response)
    throws IOException {
    StringBuffer err = new StringBuffer();
    String serverFilename = "";
            
    int numFiles=0;//this is for a terrible hack for making sure that only "true" errors get reported. ;)
    int contentLength=request.getContentLength();
    String contentType = request.getContentType();
    int ind = contentType.indexOf("boundary=");
    
    if (ind == -1) 
    	return res.getString("str_nofile");
    
    String boundary = contentType.substring(ind+9);
    
    if (boundary == null) 
    	return res.getString("str_nobound");
    
    String boundaryString = "--"+boundary;
    ServletInputStream in = request.getInputStream();
    
    byte[] buffer = new byte[1024];
    
    table.clear();
    
    int result=readLine(in, buffer, 0, buffer.length);
    
    outer: while (true) {
      			if (numFiles > 0) 
      				break;
      			if (result<=0) {
        			err.append(res.getString("str_err") + res.getString("str_err_stream_short"));
        			break;
      			}
    
    String line = new String(buffer, 0, result);
       
    if (!line.startsWith(boundaryString)) {
        err.append(res.getString("str_err") + res.getString("str_err_no_mime_bound"));
        break;
    }
    
    result=readLine(in, buffer, 0, buffer.length);
    
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
    
    String name=token.substring(nameStart+6, nameEnd);
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
        
    FileOutputStream f_out;
    serverFilename = getValue("ServerFilename");
    
    if (serverFilename == null || serverFilename.equals("")) 
    	serverFilename = filename; // if the file is not renamed in the form,
    
    try {  // use the same name as on the client
    	f_out=new FileOutputStream(fileLocation+'/'+serverFilename);
    }catch (Exception e) {
    	err.append(res.getString("str_err") + e.getMessage());
        break;
    }
    
    appendValue(name, filename, true);
    result=readLine(in, buffer, 0, buffer.length);
    
    if (result<=0) {
        err.append(res.getString("str_err") + res.getString("str_err_stream_short") + " 1");
        break;
    }
    
    int size=0;
    try {
    	byte[] tmpbuffer=new byte[buffer.length];
    	int tmpbufferlen=0;
    	boolean isFirst=true;
    	inner: while ((result=readLine(in, buffer, 0, buffer.length))>0) {
    				if (isFirst) { // ignore all proceeding \r\n
              			if (result==2 && buffer[0]=='\r' && buffer[1]== '\n')
                			continue;
            		}
            
            		String tmp=new String(buffer, 0, result);
            		
            		if (tmp.startsWith(boundaryString)) {
              			if (!isFirst) {
                			size+=tmpbufferlen-2;
                    		f_out.write(tmpbuffer, 0, tmpbufferlen-2);
              			}
              		numFiles++;
              		f_out.close(); //añadido 5/agost/2004
              		continue outer;
            		}
            		else{
              			if (!isFirst) {
                			size+=tmpbufferlen; 
                			f_out.write(tmpbuffer, 0, tmpbufferlen);
              			}
            		}
            
            		System.arraycopy(buffer, 0, tmpbuffer, 0, result);
            		tmpbufferlen=result;
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
       	String value = new String(buffer, 0, result-2); // exclude \r\n
        appendValue(name, value, false);
    }
    
    result=readLine(in, buffer, 0, buffer.length); 
}// end of while

table.clear();

    if (err.length()>0) {
    	return err.toString();
    }
    return fileLocation+'/'+serverFilename;
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
  
  //borrar pagina
  void deletePage(String pageID) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM WebPages WHERE PageID='" + pageID + "'");
    }
    catch (Exception e) {
    }  
  }
  
  //editar pagina //pageID es necesario pues busca el nombre en la base
  //de datos de la pagina y asi su contenido
  //aqui convergen muchas paginas que desean editarse
  String editPage(Student student, String pageID) {
    boolean allowOtherSectionEdit=true;
    String[] cSections=null; 
    
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      TAPermission tap = ta.getPermission("ManageContent_Edit");
      if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)
	 || tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT))
	return Permissions.getPermission(tap.getName()).getDenyMsg();
      if (tap.getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
        allowOtherSectionEdit=false;
	cSections = ta.getAssignedSections();
        if (cSections==null || cSections.length==0) {//no assigned sections. They shouldn't be
                                                    //editing -any- content. 
          return Permissions.getPermission(tap.getName()).getDenyMsg();
        }
      }
    }

    MessageFormat mf = new MessageFormat(res.getString("str_title_edit_page"));
    Object[] args = {pageID};
    StringBuffer buf = new StringBuffer("<h3>" + mf.format(args) + "</h3>");
    mf.applyPattern(res.getString("str_explain_create"));
    buf.append(mf.format(args));
            
    buf.append("<FORM METHOD=POST><TABLE BORDER=0>");
    buf.append(tmplate.appendTopItemsEdit(pageID));
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPage = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "' ORDER BY Section");

      int paragraphNumber=0;
      mf.applyPattern(res.getString("str_paragraph"));
      log.paranoid("Printing out edit form for " + pageID,"Prueba:editForm");
      while (rsPage.next()) {
          args[0] = new Integer(paragraphNumber);
          buf.append("<TR><TD>" + tmplate.appendInnerItemsEdit(pageID,paragraphNumber) + "</TD><TD>");
          buf.append("<p><b>" + mf.format(args) + "</b> ");
          String heading=rsPage.getString("Heading");
          String htmlText=rsPage.getString("HTMLText");
          String courseSection=rsPage.getString("CourseSection");
          log.paranoid("Appending heading " + paragraphNumber + ": " + heading,"ManageContet:editForm");
          log.paranoid("Appending htmltext " + paragraphNumber + ": " + htmlText,"Prueba:editForm");
          log.paranoid("Appending coursesection " + paragraphNumber + ": " + courseSection,"Prueba:editForm");
	  if (allowOtherSectionEdit || (cSections !=null && sectionMatches(courseSection,cSections))) {
            buf.append("<INPUT TYPE=TEXT SIZE=50 NAME=" + paragraphNumber + "H"
            + " VALUE=\"" + heading + "\">");
            //if cSection != null, then this person is a TA, and their level is conditional.
            //TA are -not- allowed to change the section of the currently existing courses
            //if their level is conditional. So, we make coursesection a hidden field. 
            if (cSections != null) {
              buf.append("<INPUT TYPE=HIDDEN NAME=" + paragraphNumber + "S VALUE='" + courseSection + "'>");
            } else {
              buf.append(displaySectionInfo(courseSection, paragraphNumber));
            }
            buf.append("<br><TEXTAREA ROWS=10 COLS=70 WRAP=SOFT NAME=" + paragraphNumber + "T>"
            + htmlText + "</TEXTAREA>");
       } else { 
	    buf.append("<INPUT TYPE=HIDDEN NAME=" + paragraphNumber + "H VALUE=\""
	      + CharHider.dquot2html(heading) + "\">");
            buf.append("<INPUT TYPE=HIDDEN NAME=" + paragraphNumber + "T VALUE=\""
              + CharHider.dquot2html(htmlText) 
              + "\"><INPUT TYPE=HIDDEN NAME=" + paragraphNumber + "S VALUE='" + 
              courseSection + "'>");
            buf.append("<h4>" + heading + "</h4>");
            buf.append(htmlText);
          }
          paragraphNumber++;
          buf.append("</TD></TR>");
      }
      
      //imprime las casillas o tablas para editar html o insertar
      //contenido
      buf.append("<TR><TD>" + tmplate.appendInnerItemsEdit(pageID,paragraphNumber) + "</TD><TD>");
      buf.append("<p><b>" + res.getString("str_insert_paragraph"));
      buf.append("<INPUT NAME=NewSection SIZE=2 VALUE='" + paragraphNumber 
      + "'>:</b>&nbsp;<INPUT TYPE=TEXT SIZE=50 NAME=" + paragraphNumber + "H>");
      
      //condicion para mostrar las secciones si las tiene el curso
      if (cSections == null) {
        buf.append(displaySectionInfo(paragraphNumber));
      } else { 
        buf.append(displaySectionInfo(cSections,paragraphNumber));
      }
      
      //caja de texto grande
      buf.append(""+
"<table align='left'  border='0' cellpadding='5' cellspacing='0'><tr>" +
"            <tr valign=top>"+ 
"            <script type='text/javascript' src='http://localhost/moodle/lib/editor/htmlarea.php?id=1'></script> " +
"              <script type='text/javascript' src='http://localhost/moodle/lib/editor/dialog.js'></script> " +
"              <script type='text/javascript' src='http://localhost/moodle/lib/editor/lang/en.php'></script> "+
"              <script type='text/javascript' src='http://localhost/moodle/lib/editor/popupwin.js'></script> "+
"              <textarea id='" + paragraphNumber + "T' name='" + paragraphNumber + "T' rows='20' cols='65' wrap='SOFT'></textarea> "+
"			   <script language='javascript' type='text/javascript' defer='1'>"+
"			   HTMLArea.replace('" + paragraphNumber + "T')</script></tr>"+
"            " +
"          </tr>"+
"        </table>"+
""+
"");
      
      
      //buf.append("<br><TEXTAREA ROWS=10 COLS=70 WRAP=SOFT NAME=" + paragraphNumber + "T></TEXTAREA>");
      
      paragraphNumber++;
      buf.append("</TD></TR></TABLE>");
      //cierra la tabla nueva
      
      buf.append("<INPUT TYPE=HIDDEN NAME=NSections VALUE=" + paragraphNumber + ">");
      buf.append("<INPUT TYPE=HIDDEN NAME=PageID VALUE=" + pageID + ">");
      buf.append("<INPUT TYPE=HIDDEN NAME=UserRequest>");
                      
    } catch (Exception e) {
      return e.getMessage();
    } 
    buf.append(tmplate.appendBotItemsEdit(pageID));
    buf.append("<br><input type=submit name=Save onClick=this.form.elements.UserRequest.value='Save'; value="+res.getString("str_btn_save_now")+">");
    buf.append("<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='SaveEdits'; VALUE='" + res.getString("str_btn_save") 
    + "'><INPUT TYPE=RESET VALUE='" + res.getString("str_btn_reset") + "'>");    
    buf.append("</FORM>");
        
    return buf.toString();
  }
  
  //añadir pagina
  String addPage(Student student, String pageID) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsPages = stmt.executeQuery("SELECT * FROM WebPages WHERE PageID='" + pageID + "'");
      if (rsPages.next()) return res.getString("str_page_exists");
      else {
        stmt.executeUpdate("INSERT INTO WebPages (Section,PageID,Heading,HTMLText,CourseSection) "
        + "VALUES (1,'" + pageID + "','A Heading Goes Here','HTML code goes here.','All')");
        return editPage(student,pageID);
      }
    }
    catch (Exception e) {
      return e.getMessage();
    }
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
  
  void appendValue(String name, String value, boolean isFile) {
    UploadContent data=new UploadContent(name, value, isFile);
    table.put(name, data);
  }
    
  String getValue(String name) {
    UploadContent data=(UploadContent) table.get(name);
    if (data==null)
      return null;
    return data.value;
  }

  //grabar edicion y previsualiza
  String saveEdits(String pageID,HttpServletRequest request,HttpServletResponse response, Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      boolean checkCourseSection=false;
      WebPage[] pages = null;
      String[] cSections=null;
      String[] taSections=null;
      if (student.getIsTA()) {
        TA ta = TAS.getTA(student.getIDNumber());
        if (ta.getPermission("ManageContent_SaveEdits").getPermissionLevel().equals(TAPermission.PERM_CONDITIONAL)) {
          checkCourseSection=true;
          String sql = "SELECT * FROM WebPages WHERE PageID='" 
            + pageID + "' ORDER BY Section";
          log.paranoid("Executing: " + sql,"Prueba:saveEdits");
          ResultSet rsPage = stmt.executeQuery(sql);
          Vector sections = new Vector();
          Vector pgs = new Vector();
          while (rsPage.next()) {
            sections.add(rsPage.getString("CourseSection"));
            pgs.add(new WebPage(rsPage));
          }
          cSections=(String[])sections.toArray(new String[]{});
          pages = (WebPage[])pgs.toArray(new WebPage[]{});
          taSections=ta.getAssignedSections();
        }
      }
      int nSections = 0;
      int newSection = nSections;
      try {
        nSections = Integer.parseInt(request.getParameter("NSections"));
        newSection = Integer.parseInt(request.getParameter("NewSection"));
      }
      catch (Exception e) {
      }
      stmt.executeUpdate("DELETE FROM WebPages WHERE PageID='" + pageID + "'"); 
      for (int i=0;i<nSections;i++) {
        String heading = request.getParameter(i + "H"); if (heading==null) heading="";
        String bodyText = request.getParameter(i + "T"); if (bodyText==null) bodyText="";
        String courseSection = request.getParameter(i + "S");
        log.paranoid("before saving, heading is: " + heading + "\nbodyText is: " + bodyText + "\ncourseSection is: " + courseSection,"Prueba:saveEdits");
        boolean updateOK=true;
        //convoluted ifs:
        //if we're checking the course section, then, the student is a conditional TA.
        //(for saving... if they've gotten as far as this method, we know their permission
        //is greater than student). So, we check for the section.
        //if the section is 1 less than nSections, then, this is the newly added section.
        //if the sections dont "match", then, updateOK is false. 
        //If this is -not- the "new" section, then we need to see if this is an "edit-ok" section.
        //if the cSection[i] doesn't match w/ a ta section, then editok ain't gonna fly, sorry. ;)
        //if cSection matches, buf courseSection isn't the same as cSection[i] (ie, they tried to
        //change the section on us!), we um, hrm, then updateOK is still true, but
        //we resaet courseSection.
        if (checkCourseSection) {
          if (i==nSections-1) {//this is the newly inserted section. Double check to make sure that the section is valid.
            if (!sectionMatches(courseSection,taSections)) {
              updateOK=false;
            }
          } else {
            if (!sectionMatches(cSections[i],taSections)) {
              updateOK=false;
            } else {
              if (courseSection!=cSections[i]) {
                courseSection=cSections[i];
              }
            }
          }
        } 
        log.paranoid("After convoluted if's, updateOK is: " + updateOK,"Prueba:saveEdits");
        String sql;
        //if the heading and bodytext are blank, then, this shouldn't be 
        //inserted.... unless, of course... this person
        //is a TA, who isn't allowed to update.
        if (updateOK) {
          if (!(heading.equals("") && bodyText.equals(""))){  // store it only if at least one of the fields is not blank
            sql = "INSERT INTO WebPages (Section,PageID,Heading,HTMLText,CourseSection) "
            + "VALUES (" + (i<newSection?i:(i==nSections-1?newSection:i+1)) + ",'" + pageID + "','"
            + CharHider.quot2html(CharHider.curlQuote2Html(heading)) + "','" + CharHider.quot2html(CharHider.curlQuote2Html(bodyText)) + "','" + courseSection + "')";
            log.paranoid("Executing: " + sql,"Prueba:saveEdits");
            stmt.executeUpdate(sql);
          }
        } else {
          sql="INSERT INTO WebPages (Section,PageID,Heading,HTMLText,CourseSection) " 
            + "VALUES('" + (i<newSection?i:(i==nSections-1?newSection:i+1))
            + "','" + pages[i].getPageID() + "','" + CharHider.quot2html(CharHider.curlQuote2Html(pages[i].getHeading()))
            + "','" + CharHider.quot2html(CharHider.curlQuote2Html(pages[i].getHTMLText())) + "','"
            + pages[i].getCourseSection() + "')";
          log.paranoid("Executing: " + sql,"Prueba:saveEdits");
          stmt.executeUpdate(sql);
        }
      }
      tmplate.savePageInformation(request,pageID, nSections);
      
      //imprime en pantalla el preview o sigue añadiendo cajas de texto
     if (!request.getParameter("UserRequest").equals("Save"))
          return new Page().webPage(pageID,true,1);
     else
          return (editPage(student,request.getParameter("PageID")));
            
    }
    catch (Exception e) {
      log.sparse("Caught: " + e.toString(),"Prueba:saveEdits",e);
      return res.getString("str_update_failed") + e.getMessage();
    }
  }  

 //codigo de abajo a eliminar
 
  //añadir seccion del curso
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

  //mostrar secciones
  String displaySectionInfo(String[] cSections, int sectionN) {
    StringBuffer buf = new StringBuffer();
    if (cSections.length==1) {
      buf.append("<INPUT TYPE=HIDDEN NAME=" + sectionN + "S VALUE='" + cSections[0] + "'>");
    } else {
      buf.append("<SELECT NAME=" + sectionN + "S>");
      for (int i=0; i<cSections.length;i++) {
        buf.append("<OPTION VALUE='" + cSections[i] + "'>" + cSections[i] + "</OPTION>");
      }
      buf.append("</SELECT>");
    }
    return buf.toString();
  }
  
  //mostrar informacion de secciones
  String displaySectionInfo(int sectionN){
    StringBuffer buf = new StringBuffer();
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
        return "<INPUT TYPE=hidden NAME='" + sectionN + "S' VALUE='All'>";
      int numSections=rsSections.getInt("Value");
      buf.append("<br>Section: <SELECT NAME='" + sectionN + "S'><OPTION VALUE='All'>"
       + res.getString("str_all"));
      for (int i=1; i<=numSections; i++){
        buf.append("<OPTION>" + i);
      }
    }catch(Exception e){
      return "<INPUT TYPE=hidden NAME='" + sectionN + "S' VALUE='All'>";
    }
    buf.append("</SELECT>");
    return buf.toString(); 
  }    

  String displaySectionInfo(String courseSection, int sectionN){
    StringBuffer buf = new StringBuffer("<br>");
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
       return "<INPUT TYPE=hidden NAME='" + sectionN + "S' VALUE='All'>";
      //sections start w/ 1, therefore, i=1... RDZ
      int numSections=rsSections.getInt("Value");
      buf.append("Section: <SELECT NAME='" + sectionN + "S'>");
      for(int i=1; i<=numSections; i++){
        if (courseSection != null && courseSection.equals(String.valueOf(i)))
          buf.append("<OPTION selected>" +i);
        else
          buf.append("<OPTION>" + i);
      }
      rsSections.close();
      stmt.close();
      conn.close();
    }catch (Exception e){
     buf.append("<INPUT TYPE=hidden NAME='"+sectionN+"S' Value='All'>");
     return buf.toString();
    }
    if (courseSection != null && courseSection.equals("All"))
      buf.append("<OPTION selected value='All'>" + res.getString("str_all") + "</SELECT>");
    else
      buf.append("<OPTION value='All'>" + res.getString("str_all") + "</SELECT>");
    return buf.toString();
  }

  boolean sectionMatches(String pageSection, String[] cSections) {
    for (int i=0; i<cSections.length;i++) {
      if (cSections[i].equals(pageSection))
        return true;
    }
    return false;
  }*/
}

/*
class UploadContent {
  String name;
  String value;
  boolean isFile;

  UploadContent(String name, String value, boolean isFile) {
    this.name=name;
    this.value=value;
    this.isFile=isFile;
  }
}*/
