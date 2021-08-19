package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.net.*;
import java.text.SimpleDateFormat;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.MessageFormat;

/**Report.java, a Java upload servlet based on
 * UploadServlet.java
 * Copyright (c) 1998 Yoon Kyung Koo. All rights reserved.
 * contact via yoonforh@moon.daewoo.co.kr
 * HISTORY
 *  first release ver 1.0 1998/07/14
 *    first working version
 *  revision 1.0a 1998/11/06
 *     change Vector with Hashtable to enhance performance
 *  revision 1.0z 1999/03/06
 *    add work around of a bug
 *      in JSDK 2.0's javax.servlet.ServletInputStream.readLine() method
 * NOTE
 *   if you want to get info. about Upload CGI standard, refer to RFC 1867 
 *
 * @version 1.0z 1999/03/06
 * @author Yoon Kyung Koo
 *
 * Used with permission and modified 2001 by Chuck Wight, University of Utah
 * Further modifications made 2003 by Robert Zeigler, Sequoia Choice Arizona Distance Learning
 * Note: The upload servlet was released by Yoon Kyung Koo with an opensource license in june of 2002
 * Note2: version was originally 1.0z, but various pieces of 1.1a have been backported into this servlet
 * as of Aug 2003
 */

public class Report extends HttpServlet {
  // absolute path where file uploaded
  // NOTE:do not use '\', always use '/' as directory discriminator

  //String logLocation;
  String thisServletURI;
  int numberOfSections;
  boolean useSectionDeadlines = false;
  Logger logger = new Logger();
  final static boolean DEBUG=false;
  final static boolean LOGGING=false;
//  Hashtable table=new Hashtable();
  private static Hashtable currentUploads = new Hashtable();
  PrintWriter log = null;
  RBStore res = EledgeResources.getReportBundle();

  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  protected void finalize() throws Throwable {
    closeLog();
  }

  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    thisServletURI = request.getRequestURI();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Report");
      return;
    }
    // from here on, student id is assumed to be valid
    //no check of frozen???
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }

    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Report",request,student,err)) {
	out.println(Page.create(err.toString()));
	return;
      }
    }

    getReportParameters();

    String userRequest = request.getParameter("UserRequest");
    if (userRequest!=null) {
      if (userRequest.equals("UploadStatus")) {
	out.println(uploadStatus(request));
      } else
	out.println(Page.create(reportsPage(student)));
    } else {
      out.println(Page.create(reportsPage(student)));    
    }
  }

  String uploadStatus(HttpServletRequest request) {
    StringBuffer buf = new StringBuffer();
    String key = request.getParameter("ReportName");
    String url = thisServletURI + "?UserRequest=UploadStatus&ReportName=" + key;
    UploadListener ul = null;
    logger.paranoid("Attempting to get ul for key: " + key,"Report:uploadStatus");
    if (currentUploads.containsKey(key)) {
      ul = (UploadListener)currentUploads.get(key);
    }
    buf.append("<html><body onLoad=setTimeout('");
    if (ul != null) {
      logger.paranoid("ul not nul...","Report:uploadStatus");
      if (!ul.finished()) {
        buf.append("this.document.location=\"" + url + "\"',500)>");
      } else {
	buf.append("this.window.close()',2500)>");
	currentUploads.remove(key);
      }
      buf.append(ul.statusBar());
    } else {
      buf.append("this.document.location=\"" + url + "\"',250)>"
        + res.getString("str_wait") + "...");
    }
    buf.append("</body></html>");
    return buf.toString();
  }
 
  //alternate "uploadStatus" method.... 
  //looks really cool (realtime updates...) on mozilla/gecko-based browsers, but,
  //sucks on IE and Opera (They wait until the page is -fully- loaded to render anything...)
  //so, we use the one above. Uncomment and check it out if you'd like...
  //be sure to change the line in doGet from out.println(uploadStatus(request));
  //to uploadStatus(request,out) if you want to try this method out. RDZ
  /*
  void uploadStatus(HttpServletRequest request,PrintWriter out) {
    StringBuffer buf = new StringBuffer();
    String key = request.getParameter("ReportName");
    UploadListener ul = null;
    logger.paranoid("Attempting to get ul for key: " + key,"Report:uploadStatus");
    out.print("<html><body onLoad=setTimeout('this.window.close()',3000);>");
    if (!currentUploads.containsKey(key)) {
      out.print("Waiting for status...");
      out.flush();
      while (ul==null) {
        ul = (UploadListener)currentUploads.get(key);
      }
    }
    out.print("<br>" + ul.getFilename() + "<br><span style=\"font-size: 14pt; font: Times New Roman\">Upload Status: [");
    out.flush();
    int desiredStars=0;
    int printedStars=0;
    int totalStars=30;
    while(!ul.finished()) {
      desiredStars=(int)((float)ul.ratioDone()*(float)totalStars);
      for (; printedStars<desiredStars;printedStars++) {
	out.print("*");
      }
      out.flush();
    }
    out.print("] Complete<BR>");
    out.flush();
    out.print("<br><a href=javascript:this.window.close()>Close Window</a>");
    out.flush();
    currentUploads.remove(key);
  }
*/
  void getReportParameters() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsParams = stmt.executeQuery("SELECT * FROM ReportParameters");
      rsParams.first();
      useSectionDeadlines = rsParams.getBoolean("UseSectionDeadlines");
    }
    catch (Exception e) {
    }  
  }
  
  String reportsPage(Student student){
    MessageFormat mf = new MessageFormat(res.getString("str_bad_filename"));
    Object[] args = {
      "' + serverext + '",
      "' + clientext + '"
    };
    StringBuffer buf = new StringBuffer();
    //js (very) weak doc-type checking...
    buf.append("\n<SCRIPT Language=JavaScript>\n"
    + "<!--\n"
    + "function fileMatches(form) {\n"
    + "  servername = form.elements.ServerFilename.value;\n"
    + "  clientname = form.elements.FileToUpload.value;\n"
    + "  serverext = servername.substring(servername.lastIndexOf('.'));\n"
    + "  clientext = clientname.substring(clientname.lastIndexOf('.'));\n"
    + "  if (serverext != clientext) {\n"
    + "  alert('" + mf.format(args) + "');\n"
    + "    return false;\n"
    + "  }\n"
    + "  return true;\n"
    + "}\n"
    + "-->\n"
    + "</SCRIPT>");
    //"pop-up" the "Upload Status" window.
    buf.append("<SCRIPT LANGUAGE=JavaScript>\n"
    + "<!--\n"
    + "function openWindow(assignment,student) {\n"
    + "  var popW=600, popH=125;\n"
    + "  var w=screen.availWidth;\n"
    + "  var h=screen.availHeight;\n"
    + "  var leftPos=(w-popW)/2, topPos = (h-popH)/2;\n"
    + "  url = '" + thisServletURI + "?UserRequest=UploadStatus&ReportName=' + student + assignment;\n"
    + "  var windowArgs = 'resizable,width=' + popW + ',height=' + popH + ',top=' + topPos + ',left=' + leftPos;\n"
    + "  window.open(url,'UploadStatus',windowArgs);\n"
    + "}\n-->\n</SCRIPT>\n");
    
    mf.applyPattern(res.getString("str_important"));
    args[0] = "<i>";
    args[1] = "</i>";
    SimpleDateFormat dfDateTime = new SimpleDateFormat("MMM'&nbsp;'dd,'&nbsp;'yyyy h:mm a");
    SimpleDateFormat dfDate = new SimpleDateFormat("MMM'&nbsp;'dd,'&nbsp;'yyyy");
    Date now = new Date();
    int sectionID = student.sectionID;
    String sectionName;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + sectionID + "'");
      if (rsSections.next())
        sectionName = rsSections.getString("SectionName");
      else {
        sectionID=1;
        sectionName = "";        
      }

      ResultSet rsNSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsNSections.next()) numberOfSections = rsNSections.getInt("Value");
      if (numberOfSections == 1) useSectionDeadlines = false;
    
      buf.append("<h3>" + res.getString("str_title_report") + "</h3>" + "\n");
      buf.append("<b>" + student.getFullName() + "</b><br>\n");
      if (useSectionDeadlines) buf.append("Section " + sectionName + "<br>");
      else {
        sectionID=1;
      }
      buf.append(dfDateTime.format(now) + "\n");  // print current date and time
    
      buf.append("<p>" + mf.format(args) + "\n");
      mf.applyPattern(res.getString("str_report_instructions"));
      buf.append("<p>" + mf.format(args) + "\n<p>" 
      + res.getString("str_multiple_upload_note"));
    

      buf.append("\n<p><table border=1 cellspacing=0>\n<tr><td><b>"
        + res.getString("str_field_status") + "</b></td><td><b>" 
        + res.getString("str_field_assignment") + "</b></td></tr>\n");
    
//      logLocation = fileLocation + "/upload.log";
    }
    catch (Exception e) {
      buf.append(e.getMessage());
    }

    try {    
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt1 = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rs1;
      try {
        rs1 = stmt1.executeQuery("SELECT AssignmentNumber,Title,Description,FilePrefix,FileExt,"
        + "DateAvailable" + sectionID + " AS DateAvailable,"
        + "DateDue" + sectionID + " AS DateDue FROM ReportInfo ORDER BY DateDue");
      }
      catch (Exception e) {
        rs1 = stmt1.executeQuery("SELECT AssignmentNumber,Title,Description,FilePrefix,FileExt,"
        + "DateAvailable1 AS DateAvailable,DateDue1 AS DateDue FROM ReportInfo ORDER BY DateDue");
      }
      while (rs1.next()) {
      String sqlStr2 = "SELECT * FROM ReportTransactions WHERE StudentIDNumber='"
      + student.getIDNumber() + "' AND AssignmentNumber='" + rs1.getInt("AssignmentNumber")
      + "' ORDER BY Timestamp";
      ResultSet rs2 = stmt2.executeQuery(sqlStr2);
        if (rs2.last()) { // this assignment has been submitted by this student
          buf.append("<tr VALIGN=TOP><td><FONT COLOR=#00FF00><b>" 
          +  res.getString("str_submitted") + "<br>"
          + dfDateTime.format(rs2.getTimestamp("Timestamp")) + "</b><br>"
          + res.getString("str_upload_comments") + rs2.getString("Errors") + "</FONT></td>\n");
        }
        else if (now.before(rs1.getTimestamp("DateAvailable"))) {
          buf.append("<tr VALIGN=TOP><td>" + res.getString("str_due") 
          + "<br>" + dfDate.format(rs1.getDate("DateDue")) + "</td>\n");
        }
        else if (now.after(rs1.getTimestamp("DateAvailable")) && now.before(rs1.getTimestamp("DateDue"))) {
          buf.append("<tr VALIGN=TOP><td>" + res.getString("str_upload_before")
          + "<br>" + dfDateTime.format(rs1.getTimestamp("DateDue")) + "</td>\n");
        }
        else { 
          buf.append("<tr VALIGN=TOP><td><FONT COLOR=#FF0000><b>" 
          + res.getString("str_overdue") + "</b></FONT></td>\n");
        }
        
        buf.append("<td><b>" + rs1.getString("Title") + "</b><br>" + "\n" + rs1.getString("Description") + "\n");
        if (now.after(rs1.getTimestamp("DateAvailable")) && now.before(rs1.getTimestamp("DateDue"))){
          buf.append(uploadForm(rs1.getInt("AssignmentNumber"),rs1.getString("FilePrefix"),rs1.getString("FileExt"),student.getIDNumber()) + "\n");
        }        
        buf.append("</td></tr>" + "\n");
        rs2.close();
      }
      buf.append("</table>" + "\n");  
    } 
    catch (Exception e) {
      if (addErrorsField())
        return reportsPage(student);
      return createReportTables(student);
    }

    if (student.getIsInstructor()) {
      buf.append("<FORM ACTION=" + Course.name + ".ManageReport><b>" 
      + res.getString("str_teach_only") + "&nbsp;</b><INPUT TYPE=SUBMIT VALUE='"
      + res.getString("str_btn_manage_report") + "'></FORM>");
    }
    return buf.toString();
  }
  
  String uploadForm(int assignmentNumber, String filePrefix, String fileExt, String studentIDNumber) {
    StringBuffer buf = new StringBuffer();
    MessageFormat mf = new MessageFormat(res.getString("str_explain_upload"));
    Object[] args = {
      "<I>",
      "</I>"
    };
    //the encoding type is multipart/form-data for file uploads
    buf.append("<FORM ENCTYPE='multipart/form-data' METHOD=POST>");

    // ServerFilename HIDDEN form element MUST precede FILE form element, 
    // else ClientFilename will be used to store the file on the server.

    buf.append("<FONT SIZE=-1 COLOR=#FF0000>" + mf.format(args) + "</FONT><BR>");
    buf.append("<INPUT TYPE=HIDDEN NAME='AssignmentNumber' VALUE=" + assignmentNumber + ">");
    buf.append("<INPUT TYPE=HIDDEN NAME='ServerFilename' VALUE='"
    + filePrefix + studentIDNumber + fileExt + "'>");
    buf.append("<INPUT TYPE=FILE NAME='FileToUpload' SIZE=50 MAXLENGTH=255>");
    buf.append("<br><INPUT TYPE=BUTTON onClick=\"if (fileMatches(this.form)) { openWindow('" 
	+ assignmentNumber + "','" + studentIDNumber + "'); this.form.submit(); }\" VALUE='" 
        + res.getString("str_btn_upload") + "'>");
    buf.append("</FORM>" + "\n");
    
    return buf.toString();
  }
  
  public void doPost (HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    long start = System.currentTimeMillis();
    String fileLocation = Course.uploadsDirectory;
    int lastupdate=0;
    int BUFFER_SIZE=8192;
    String PATH_KEY="path";
    StringBuffer err = new StringBuffer(); 
    String error = res.getString("str_err");
    HttpSession session = request.getSession(true);
    response.setContentType("text/html;charset=UTF8");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    UploadListener ul = null;
    if (student == null) student = new Student();
    
    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Report");
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_act_frozen")));
      return;
    }
    logger.paranoid("Student is: " + student.getIDNumber(),"Report:doPost");
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      if (ta.getPermission("ManageReport_Upload").getPermissionLevel().equals("None")) {
        out.println(Page.create(Permissions.getPermission("ManageReport_Upload").getDenyMsg()));
        return;
      }
    }
    
    getReportParameters();

    //ServletOutputStream out = response.getOutputStream();
    //log = openLog(logLocation);

    response.setContentType("text/html");
    int contentLength=request.getContentLength();
    logger.paranoid("contentLength: " + contentLength,"Report:doPost");
    // RFC 1867 
    String contentType = request.getContentType();
    if (contentType == null) {
      logger.paranoid("Content type is null","Report:doPost");
      return;
    }
    int ind = contentType.indexOf("boundary=");
    if (ind == -1) {
      out.println(res.getString("str_nofile"));
      logger.paranoid("boundary= expected but not found","Report:doPost");
      return;
    }
    String boundary = contentType.substring(ind+9);

    if (boundary == null) {
      out.println(res.getString("str_nobound"));
      logger.paranoid("boundary is null","Report:doPost");
      return;
    }

    String boundaryString = "--"+boundary;

    ServletInputStream in = request.getInputStream();

    byte[] buffer = new byte[BUFFER_SIZE];
    HashMap table = new HashMap();
//    table.clear();

    int result=in.readLine(buffer, 0, BUFFER_SIZE);

outer:  while (true) {

      if (result<=0) {
        err.append(error + res.getString("str_err_stream_short"));
        logger.paranoid("Error. Stream truncated","Report:doPost");
        break;
      }
      String line = new String(buffer, 0, result);
  
      if (!line.startsWith(boundaryString)) {
        err.append(error + res.getString("str_err_no_mime_bound"));
        logger.paranoid("Error. MIME boundary missing.","Report:doPost");
        break;
      }
      if (line.substring(boundaryString.length()).startsWith("--")) {
        logger.paranoid("End of multipart...(? This is new.... aha! Maybe this is like my numFiles... ;)","Report:doPost");
        break;
      }

      result=in.readLine(buffer, 0, BUFFER_SIZE);
      if (result<=0) {
        err.append(error + res.getString("str_err_no_mime_content"));
        break;
      }
      line = new String(buffer, 0, result);
      StringTokenizer tokenizer=new StringTokenizer(line, ";\r\n");
      String token=tokenizer.nextToken();
      String upperToken = token.toUpperCase();
      if (!upperToken.startsWith("CONTENT-DISPOSITION")) {
        err.append(error + res.getString("str_err_format"));
        logger.paranoid("Format error. Content-Disposition expected.","Report:doPost");
        break;
      }
      String disposition = upperToken.substring(21);
      if (!disposition.equals("FORM-DATA")) {
        err.append(error + res.getString("str_err_no_handle"));
        logger.paranoid("I don't know how to handle ["+disposition+"] disposition.","Report:doPost");
        break;
      }
      if (tokenizer.hasMoreElements())
        token=tokenizer.nextToken();
      else {
        err.append(error + res.getString("str_err_no_name"));
        logger.paranoid("Format error. NAME expected.","Report:doPost");
        break;
      }
      int nameStart=token.indexOf("name=\"");
      int nameEnd=token.indexOf("\"", nameStart+7);
      if (nameStart<0 || nameEnd<0) {
        err.append(error + res.getString("str_err_no_name"));
        logger.paranoid("Format error. NAME expected.","Report:doPost");
        break;
      }
      String name=token.substring(nameStart+6, nameEnd);
      if (tokenizer.hasMoreElements()) {
        String filename=null;
        String serverFilename=null;
        File file = null;
        String fileContentType=null;
        FileOutputStream fout = null;
        int size=0;
        int fnStart, fnEnd;
        
        fnStart = line.indexOf("filename=\"");
        if (fnStart < 0) { //filename term missing
          err.append(error + res.getString("str_err_no_filename"));
          logger.paranoid("Format error. FILENAME expected.","Report:doPost");
          result = in.readLine(buffer,0,BUFFER_SIZE);
          continue;
          //break;
        }

        fnEnd = line.indexOf("\"",fnStart + 11);
        if (fnEnd < 0 ) {
          logger.paranoid("FILENAME is NULL","Report:doPost");
          err.append(error + res.getString("str_err_no_filename"));
        } else {
          filename = line.substring(fnStart+10, fnEnd);
          int lastindex = -1;
          if ((lastindex = filename.lastIndexOf('/')) < 0) {
            lastindex = filename.lastIndexOf('\\');
          }
          if (lastindex >= 0) {
            filename = filename.substring(lastindex+1);
          }
          filename=processEscape(filename);
        }
        logger.paranoid("Receiving file named: " + filename,"Report:doPost");
        serverFilename = getValue(table, "ServerFilename");
        if (serverFilename == null || serverFilename.equals("")) {
          if (filename != null) {
            serverFilename = filename;
          }
        } else stripDots(serverFilename,student);

        if (serverFilename != null) { 
          String path = Course.uploadsDirectory;
          file = new File(path);
          if (path != null && file.exists() && file.isDirectory()) {
            file = new File(path, serverFilename);
          }
        }
        
        result=in.readLine(buffer, 0, BUFFER_SIZE);
        if (result<=0) {
          err.append(error + res.getString("str_err_stream_short") + "1");
          logger.paranoid("Error. Stream truncated 1","Report:doPost");
          break;
        }
        fileContentType = new String(buffer,0,result);
        if (fileContentType.toUpperCase().startsWith("CONTENT-TYPE:")) {
          fileContentType = fileContentType.substring(13).trim();
        } else {
          logger.paranoid("What should I read here ??? - result = " + result + ", and read [" + new String(buffer,0,result) + "]","Report:doPost");
        }
        try {
          byte[] tmpbuffer1=buffer;
          byte[] tmpbuffer2=new byte[BUFFER_SIZE];
          byte[] tmpbuffer=tmpbuffer2;
          int tmpbufferlen=0;
          boolean isFirst=true;
          boolean odd=true;
          ul = new UploadListener(filename,getValue(table,"AssignmentNumber"),student.getIDNumber(),contentLength,res);
	  currentUploads.put(ul.getName(),ul);
inner:     
	  //int updateIncrement=2; //update the status bar in increments of 2 percent...
          while ( (result=in.readLine(buffer, 0, BUFFER_SIZE))>0) {
            logger.paranoid("After the readLine, result is: " + result,"Report:doPost");
            if (isFirst) { // ignore all proceeding \r\n
              if (result==2 && buffer[0]=='\r' && buffer[1]== '\n') {
                continue;
              }
              if (file != null) {
                fout = new FileOutputStream(file);
              }
            }

            if (bytesStartsWith(buffer, 0, result, boundaryString)) {
              if (!isFirst) {
                logger.paranoid("Size was: " + size,"Report:doPost");
                size += tmpbufferlen - 2;
                logger.paranoid("After tmpbufferlen-2 addition, size is: " + size,"Report:doPost");
                if (fout != null) {
                  fout.write(tmpbuffer, 0, tmpbufferlen - 2);
		  ul.updateFinishedSize((tmpbufferlen-2));
		  //if(updateStatus(contentLength,size,lastupdate,updateIncrement)) {
		  //  out.print("=");
		  //  out.flush();
		  //  lastupdate=size;
                }
              }
              continue outer;
            } else {
              if (!isFirst) {
                logger.paranoid("Size was: " + size,"Report:doPost");
                size +=tmpbufferlen;
                logger.paranoid("After tmpbufferlen, size is: " + size,"Report:doPost");
                if (fout != null) {
                  logger.paranoid("Writing: " + tmpbuffer.toString(),"Report:doPost");
                  fout.write(tmpbuffer, 0, tmpbufferlen);
		  //if (updateStatus(contentLength,size,lastupdate,updateIncrement)) {
		  //  out.print("=");
		  //  out.flush();
		  //  lastupdate=size;
		  ul.updateFinishedSize(tmpbufferlen);
                }
              }
            }

            if (odd) {
              buffer = tmpbuffer2;
              tmpbuffer=tmpbuffer1;
            } else {
              buffer = tmpbuffer;
              tmpbuffer = tmpbuffer2;
            }
            odd = !odd;
            tmpbufferlen = result;
            isFirst = false;
          }
        } catch (IOException ie) {
          err.append(error + ie.getMessage());
          logger.paranoid("IO Error while writing to file: " + ie.toString(),"Report:doPost",ie);
        } catch (Exception e) {
          err.append(error + e.getMessage());
          logger.paranoid("Error while writing to file: " + e.toString(),"Report:doPost",e);
        } finally {
          logger.paranoid("size: " + size,"Report:doPost");
          if (size == 0 ) {
            err.append(res.getString("str_err_badfile"));
          }
          if (fout != null) {
            fout.close();
          }
          if (size > 0) {
            appendValue(table,name, filename, fileContentType, size);
          }
        }
        result=in.readLine(buffer, 0, BUFFER_SIZE);
        logger.paranoid("What should I read here(2) ??? - result = " + result + ", and read [" + new String(buffer, 0, result) + "]","Report:doPost");
      } else { // no more elements
        result=in.readLine(buffer, 0, buffer.length);
        if (result<=0) {
          err.append(error + res.getString("str_err_stream_short") + "2");
          logger.paranoid("Error. Stream truncated 2","Report:doPost");
          break;
        }
        result=in.readLine(buffer, 0, buffer.length);
        if (result<=0) {
          err.append(error + res.getString("str_err_stream_short") + "3");
          logger.paranoid("Error. Stream truncated 3","Report:doPost");
          break;
        }
        String value = new String(buffer, 0, result-2); // exclude \r\n
        appendValue(table, name, value);
      }
      result=in.readLine(buffer, 0, buffer.length);
    } // end of while
    long end = System.currentTimeMillis();
    logger.paranoid("Upload took: " + (end-start) + "(ms) to complete","Report:doPost");
    if (ul != null) {
      ul.setFinished(true);
    }
   // out.print("] Complete</span>");
    out.println(recordUploadTransaction(student.getIDNumber(),err, table));
    out.println(Page.create(err + "<BR>" + reportsPage(student)));
    out.close();
  }

  private boolean updateStatus(int contentLength, int size, int lastupdate, int percentincrement) {
    double percent=(((double)percentincrement/(double)100)*(double)contentLength);
    int diff=(size-lastupdate);
    logger.paranoid("cL: " + contentLength + "\ns: " + size + "\nlu: " + lastupdate + "\np: " + percent + "\nd: " + diff,"Report:updateStatus");
    if (diff>percent)
      return true;
    return false;
  }

  boolean bytesStartsWith(byte[] bytes, int offset, int length, String toCompare) {
    boolean result = true;
    if (toCompare.length() > length) {
      return false;
    }

    for (int i = toCompare.length() - 1; i >= 0; i--) {
      if (toCompare.charAt(i) != bytes[offset + i]) {
        result = false;
        break;
      }
    }

    return result;
  }
         
  String recordUploadTransaction(String studentIDNumber, StringBuffer err, HashMap map) {
    String error = err.toString();
    if (error.equals(""))
      error = res.getString("str_upload_ok");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlInsertStr = "INSERT INTO ReportTransactions (StudentIDNumber,"
      + "AssignmentNumber,ClientFilename,ServerFilename,Errors) "
      + "VALUES ('" + studentIDNumber + "','" + getValue(map, "AssignmentNumber") + "','"
      + getValue(map, "FileToUpload") + "','" + getValue(map, "ServerFilename") + "','" 
      + error + "')";
     
      if (stmt.executeUpdate(sqlInsertStr) != 1) return (res.getString("str_err_db"));
    }
    catch (Exception e) {
      if (addErrorsField())
        return recordUploadTransaction(studentIDNumber, err, map);
      return e.getMessage();
    }
    
    return "";  
  }

  private boolean addErrorsField() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sql = "ALTER TABLE ReportTransactions ADD (Errors TEXT)";
      logger.paranoid("Executing: " + sql,"Report:addErrorsField");
      stmt.executeUpdate(sql);
      sql = "UPDATE ReportTransactions SET Errors=''";
      stmt.executeUpdate(sql);
      stmt.close();
      conn.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  void appendValue(HashMap map, String name, String value, String contentType, int size) {
    UploadData data=new UploadData(name, value, contentType, size, true);
    map.put(name, data);
  }

  void appendValue(HashMap map, String name, String value) {
    UploadData data=new UploadData(name, value, null, 0, false);
    map.put(name, data);
  }

  String getValue(HashMap map, String name) {
    UploadData data=(UploadData) map.get(name);
    if (data == null) {
      return null;
    }
    return data.value;
  }

  final static int NORMAL = 0;
  final static int AMPERSAND = 1;
  final static int AMPERSHARP = 2;
  /**
  * process html escape characters (&#NNNN;)
  */
  String processEscape(String string) {
    StringBuffer buffer = new StringBuffer(string.length());
    char[] chars = string.toCharArray();
    StringBuffer escaped = new StringBuffer(6);
    int status = NORMAL;

    for (int i = 0; i < string.length(); i++) {
      switch (status) {
      case NORMAL :
        if (chars[i] == '&') {
          status = AMPERSAND;
        } else {
          buffer.append(chars[i]);
        }
        break;

      case AMPERSAND :
        if (chars[i] == '#') {
          status = AMPERSHARP;
        } else {
          status = NORMAL;
          buffer.append('&');
        }
        break;

      case AMPERSHARP :
        if (chars[i] == ';') {
          try {
            buffer.append((char) Integer.parseInt(escaped.toString()));
          } catch (NumberFormatException nfe) {
            // I don't handle other Entities
            buffer.append(escaped);
            buffer.append(';');
          }
          escaped.setLength(0);
          status = NORMAL;
        } else {
          escaped.append(chars[i]);
        }
        break;
      }
    }

    if (escaped.length() > 0) {
      buffer.append(escaped);
    }

    return buffer.toString();
  }

    
   //this only gets called if "debug" is set to true in the 
   //src, so... not going to i18n this. RDZ 7/10/03
  void printResult(PrintWriter out, Map map) throws IOException {
    Iterator itr = map.values().iterator();

    out.println("<HTML><HEAD>");
    out.println("<TITLE>Upload Result</TITLE>");
    out.println("</HEAD><BODY>"); 
    out.println("<H1>Upload Result</H1>");
    out.println("<TABLE>");
    out.println("<TR><TH>NAME</TH><TH>VALUE</TH><TH>CONTENT TYPE</TH><TH>SIZE</TH><TH>FILE</TH></TR>");
    while (itr.hasNext()) {
      UploadData data = (UploadData) itr.next();
      out.println("<TR>");
      out.println("<TD>" + (data.name == null ? "" : data.name) + "</TD>");
      out.println("<TD>" + (data.value == null ? "" : data.value) + "</TD>");
      out.println("<TD>" + (data.contentType == null ? "" : data.contentType) + "</TD>");
      out.println("<TD>" + (data.isFile ? String.valueOf(data.size) : "") + "</TD>");
      out.println("<TD>" + (data.isFile ? "file" : "") + "</TD>");
      out.println("</TR>");
    }
    out.println("</TABLE>");
    out.println("</BODY></HTML>");
  }

  private PrintWriter openLog(String filename) {
    if (!LOGGING)
      return null;
    try {
      return new PrintWriter(
        new BufferedWriter(new FileWriter(filename)));
    }
    catch (IOException ie) {
      System.err.println("Error:"+ie.toString());
      return null;
    }
    catch (Exception e) {
      System.err.println("Error:"+e.toString());
      return null;
    }
  }

  private void writeLog(String string) {
    if (log ==null)
      return;
    // writing operation
    log.println(string); 
    // print() method never throws IOException,
    // so we should check error while printing
    if (log.checkError()) {
      System.err.println("File write error.");
    }
  }

  private void closeLog() {
    if (log !=null)
      log.close();
  }
/*
    //Note: sun fixed the bug in 2.0, so we don't need this anymore.
    //(at least, the 1.1a version of this servlet doesn't use it anymore, so....
    //
    //to fix JSDK 2.0's bug 
    //
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
*/
  String createReportTables(Student student) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE ReportInfo (AssignmentNumber INT PRIMARY KEY AUTO_INCREMENT,"
      + "Title TEXT,Description TEXT,FilePrefix TEXT,FileExt TEXT,DateAvailable1 DATETIME,DateDue1 DATETIME)");      
      stmt.executeUpdate("CREATE TABLE ReportTransactions (StudentIDNumber VARCHAR(50),"
      + "AssignmentNumber INT,ClientFilename TEXT,ServerFilename TEXT,Timestamp TIMESTAMP, Graded VARCHAR(5) DEFAULT 'false',"
      + " Errors TEXT)");
      stmt.executeUpdate("CREATE TABLE ReportParameters (UseSectionDeadlines VARCHAR(5), UploadsURL TEXT)");
      stmt.executeUpdate("INSERT INTO ReportParameters (UseSectionDeadlines,UploadsURL) VALUES ('false','/" + Course.name + "/uploads/')");
     
      return reportsPage(student);
   }
    catch (Exception e) {
      return e.getMessage();
    }
  }

//gets a list of reports for which there is no score, and no submission, for this student. 
//due to the fact that reports aren't graded instantly, we want to check out report transactions, as well, 
//for those reports that don't yet have a score.
  protected static String getUnsubmittedAssignments(String studentIDNumber) {
    StringBuffer buf = new StringBuffer("");
    RBStore r = EledgeResources.getReportBundle();
    boolean hasUnsubmitted=false;
    try {
      buf.append("<table border=1 cellspacing=0><thead><b>" + r.getString("str_report") + "</b></thead>");
      buf.append("<tr><th>" + r.getString("str_field_assignment") 
      + "</th><th>" + r.getString("str_field_status") + "</th></tr>");
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      Statement stmt2 = conn.createStatement();
      ResultSet rsInfo = stmt.executeQuery("SELECT * FROM ReportInfo");
//if there's nothing in the info, there are no assigned reports, so, return empty string. 
      if (!rsInfo.isBeforeFirst())
        return "";
      while (rsInfo.next()) {
        String sqlQueryString = "SELECT * FROM Scores WHERE StudentIDNumber='" + studentIDNumber + "' AND Assignment='" + rsInfo.getString("Title") + "'";
        ResultSet rsAssignment=stmt2.executeQuery(sqlQueryString);
        if (!rsAssignment.next()){
          if (!hasUnsubmitted) hasUnsubmitted=true;
//no score, so check for the transaction.
          ResultSet rsTransaction = stmt2.executeQuery("SELECT * FROM ReportTransactions"
          + " WHERE StudentIDNumber='" + studentIDNumber + "' AND "
          + "AssignmentNumber=" + rsInfo.getString("AssignmentNumber"));
          buf.append("\n<TR><TD>" + rsInfo.getString("Title") + "</TD><TD>"
          + (rsTransaction.next()?r.getString("str_ungraded"):r.getString("str_unsubmitted")) 
          + "</TD></TR>");
        }
      }
    }catch(Exception e){
      return "";
    }
    if (!hasUnsubmitted)
      buf.append("<TR><TD COLSPAN=2>" + r.getString("str_all_reports_done") 
      + "</TD></TR>");
    buf.append("</TABLE>");
    return buf.toString();
  }

//this method resolves some security concerns regarding uploading and/or
////deleting files with ../.. in them . . .
  String stripDots(String filename, Student student) {
    //get the last set of ../
    int index=filename.lastIndexOf("../");
    if (index==-1)
      return filename;
    logger.sparse("WARNING: " + student.getIDNumber() + " tried to upload to " + filename + ". There shouldn't be any ../ in a report upload serverfilename!","Report:stripDots");
    return filename.substring((index + "../".length()));
  }
}

class UploadData {
  String name;
  String value;
  String contentType;
  int size;
  boolean isFile;

  UploadData(String name, String value, String contentType, int size, boolean isFile) {
    this.name=name;
    this.value=value;
    this.contentType=contentType;
    this.size=size;
    this.isFile=isFile;
  }
}

class UploadListener {
  private String assignmentNumber;
  private String student;
  private int totalSize=0;
  private int finishedSize=0; 
  private boolean finished;
  private String filename="";
  private String name;
  private RBStore res;
  UploadListener(String file, String assignment, String studentIDNumber, int tsize, RBStore r) {
    this.assignmentNumber = assignment;
    this.student = studentIDNumber;
    this.name = this.student + this.assignmentNumber; 
    this.totalSize=tsize; 
    this.filename=file;
    this.finished=false;
    this.res=r;
  }

  String getName() {
    return name;
  }

  String getFilename() {
    return filename;
  }

  void updateFinishedSize(int size) {
    finishedSize+=size;
  }

  int percentDone() {
    return (int)((float)100*(float)finishedSize/(float)totalSize);
  }

  float ratioDone() {
    return (float)((float)finishedSize/(float)totalSize);
  }

  String statusBar() {
    int totalBars = 30; //total "bars" you want to appear... 
    char doneBarChar='*'; //type of character representing percent done...
    char notDoneBarChar='_'; //"filler" character for portion not done...
    			     //works best if doneBarChar and notDone BarChar
			     //have the same representative width in whatever
			     //font you choose...
    int barLength=((int)((ratioDone())*(float)totalBars));
    int remainder = totalBars-barLength;
    int i;
    StringBuffer buf = new StringBuffer("<span style=\"font-size: 15pt;\">"
      + this.filename + "<br>" + res.getString("str_upstatus")
      + ": [");
    if (remainder==0 || this.finished) {
      buf.append(res.getString("str_complete"));
    } else {
      for (i=0;i<barLength;i++) {
        buf.append(doneBarChar);
      }
      for (i=0;i<remainder;i++) {
        buf.append(notDoneBarChar);
      }
    }
    buf.append("]</span>");
    return buf.toString();
  }

  boolean finished() {
    if (finishedSize == totalSize || finished)
      return true;
    return false;
  }

  void setFinished(boolean f) {
    this.finished=f;
  }
}
