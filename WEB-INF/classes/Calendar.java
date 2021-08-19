package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.MessageFormat;

public class Calendar extends HttpServlet {
	
  private RBStore res = EledgeResources.getCalendarBundle();
  // these parameters may be changed to suit the owner/user:
  protected boolean viewOnly = false; // true to disallow changes (for public viewing only); false to allow users to add/modify/delete events
  protected String titleBar = Course.name + " " +res.getString("str_calendar_title"); // label shown on the top bar of the Calendar
  protected String cTFontsColor = "#000000";  // black
  protected String cTopBarColor = "#B0D8FF";  // Eledge sandstone
  protected String cDayBarColor = "#B0D8FF";  // Eledge sandstone
  protected String cOutDayColor = "#DDDDDD";  // gray
  protected String cTodaysColor = "#D0D0D0";  // lighter gray
  protected String cBGCellColor = "#FFFFFF";  // white
  protected String cDtFontColor = "#000000";  // black
  protected String timeFormat = "HH:mm";  // use "HH:mm" for 24-hr format or "h:mma" for 12-hr AM/PM format
  protected String dateFormat = "M/d/yy"; // or may changed to some other form such as "d-M-yyyy"
  
  // these parameters should not be changed, or things may break:
  
  String thisServletURI;
  String mySqlJdbcDriver = Course.jdbcDriver; // location of jdbc driver in the classpath
  protected SimpleDateFormat dfTimeField = new SimpleDateFormat(timeFormat);
  protected SimpleDateFormat dfDateField = new SimpleDateFormat(dateFormat);
  SimpleDateFormat dfMonthYear = new SimpleDateFormat("MMMM yyyy");  // used for the title bar
  SimpleDateFormat dfMonthName = new SimpleDateFormat("MMMM");       // used for next/last month navigation
  SimpleDateFormat dfMySQLDate = new SimpleDateFormat("yyyy-MM-dd"); // matches format used by MySQL database
  SimpleDateFormat dfMySQLTime = new SimpleDateFormat("HH:mm");       //format used by MySQL database
  SimpleDateFormat dfDayOfMonth = new SimpleDateFormat("d");         // used for writing date numerals in the calendar
  String time0000 = dfTimeField.format(new java.util.Date(25200000));  // 00:00 (beginning of day in local time format)
  String time2359 = dfTimeField.format(new java.util.Date(111540000)); // 23:59 (end of day in local time format)
  
  Logger log = new Logger(); //crea los logs

  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    //crea el objeto estudiante
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
  	
  	//elimina permisos al TA
 	if (student.getIsTA()) {
      	student.setIsInstructor(false); 
    }
    
	//examina que el estudiante este en sesion.
    if (!student.isAuthenticated()) {
    	out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Agenda</em><br><br>"+
    	"<head><title>Sin Autorización</title></head>" +
    	"<body>" +
    	"<p>Lo sentimos, pero no es un usuario registrado y no puede acceder a esta opci&oacute;n.Registrese entrando a My Profiles.</p>" +
    	"<p>Si olvido la contrase&ntilde;a entre a My Profiles. Si no puede contacte a su docente o administrador.</p>" +
    	"<p>Muchas Gracias.</p>"+
    	"</body></html>",student));
      //response.sendRedirect(Course.secureLoginURL + "Calendar");
      return;
    }
    
    //El estudiante es ayudante del profesor
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) { 
    
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      
      if (!ta.hasPermission("Calendar",request,student,err)) {
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Agenda</em><br><br>"+err.toString(),student));
        return;
      }
    }
    
    //le quita los permisos al TA, si entra a un curso que va ver o estudiar
    if (student.getTAtoStudent()) {
    	student.setIsInstructor(false);
    	student.setTAStatus(false);
    }
    
	//almacena en la variable la ruta del servlet es decir: 
	//(/servlet/Eledge.Calendar)	
    thisServletURI = request.getRequestURI();
        
    java.util.Calendar thisMonth = java.util.Calendar.getInstance();
    java.util.Date thisDate = new java.util.Date();
    
    // look for an input date request (default = today)
    //si es visitante, no muestra el calendario
    if (student.getIsVisitor()){
     if (getSections()>1){
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Agenda</em><br><br>"+res.getString("str_no_visitor"),student));
      return;
     }
    } 
	//examina si la variable fecha(date) no es null
    if (request.getParameter("date") != null) {
      
      try {
        thisDate = dfDateField.parse(request.getParameter("date"));
        thisMonth.setTime(thisDate);
      } 
      catch (ParseException e) {
	log.sparse("Caught: " + e.getMessage(),"Calendar:doGet");
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Agenda</em><br><br>"+res.getString("str_error_parse_date"),student));
      }
    }
    
    // set the calendar to the first day of the month
    thisMonth.set(java.util.Calendar.DAY_OF_MONTH,1);
    
    //select the correct type of output and send it:
    //si la forma no tiene nada crea el calendario
    if (request.getParameter("form") == null) {
      out.println(Page.create(htmlHeader(),
        "<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Agenda</em><br><br>"+tableHeader(thisMonth) + tableCells(thisMonth, student) + tableFooter(thisMonth),student));
    
    }
    
    else if (request.getParameter("form").equals("help")) {
      out.println(instructions());
    }
    
    else if (allowEdit(student)) {
      if (request.getParameter("form").equals("new")) {
        out.println(newEventForm(request.getParameter("value"), student));
      }
      else if (request.getParameter("form").equals("revise")) {
        out.println(reviseEventForm(request.getParameter("value"), student));
      }
    }
    else {
      out.println(res.getString("str_admin_configuration"));
    }
    return;
  }
  
  String htmlHeader() {
    return  "<SCRIPT LANGUAGE=JavaScript>"
     + "function PopupWindow(url,form,value)"
     + "{window.open(url+'?form='+form+'&value='+value,'" + res.getString("str_control_panel") + "','width=370,height=370,dependent,resizable');}"
     + "</SCRIPT>";
  }
  
  String instructions() {
    MessageFormat mf = new MessageFormat(res.getString("str_copyright"));
    Object[] args = {
      "<a href=mailto:wight@chem.utah.edu>",
      "</a>"
    };
    StringBuffer rv = new StringBuffer("<HTML><head>");
    rv.append("<SCRIPT LANGUAGE=JavaScript>"
     + "function PopupWindow(url,form,value)"
     + "{window.open(url+'?form='+form+'&value='+value,'" + res.getString("str_control_panel") + "','width=370,height=370,dependent,resizable');}"
     + "</SCRIPT></head><body>"
     + "<h3>" + res.getString("str_title_webcal") + "</h3>"
     + "<FONT SIZE=-1><OL><LI>" + res.getString("str_li1")
     + "<LI>" + res.getString("str_li2")
     + "<LI>" + res.getString("str_li3")
     + "<LI>" + res.getString("str_li4")
     + "</OL><HR>"
     + mf.format(args) + "<BR>");
    mf.applyPattern(res.getString("str_gpl"));
    args[0]="<a href='http://www.gnu.org/copyleft/gpl.html' onClick=javascript:opener.document.location='http://www.gnu.org/copyleft/gpl.html';window.close();>";
    rv.append(mf.format(args) + "<BR></FONT></body></html>");
    return rv.toString();
  }
  
  //tabla cabecera
  String tableHeader(java.util.Calendar thisMonth) {
    java.util.Calendar nextMonth = (java.util.Calendar)thisMonth.clone(); nextMonth.add(java.util.Calendar.MONTH,+1);
    java.util.Calendar lastMonth = (java.util.Calendar)thisMonth.clone(); lastMonth.add(java.util.Calendar.MONTH,-1);
    return "<body>"
     + "<TABLE BORDER=1 CELLSPACING=0 CELLPADDING=0 WIDTH=100%>"
     + "  <TR>"
     + "  <TD BGCOLOR=" + cTopBarColor + " ALIGN=CENTER COLSPAN=7>"
     + "    <TABLE WIDTH=100% BORDER=0 CELLSPACING=0 CELLPADDING=0>"
     + "    <TR ALIGN=CENTER>"
     + "      <TD><A HREF=" + thisServletURI + "?date=" + dfDateField.format(lastMonth.getTime()) + "><FONT SIZE=-1 COLOR=" +
cTFontsColor + "><&#151;&nbsp;" + dfMonthName.format(lastMonth.getTime()) + "</FONT></A></TD>"
     + "      <TD><FONT SIZE=+1 COLOR=" + cTFontsColor + "><B>" + titleBar + dfMonthYear.format(thisMonth.getTime()) +
"</B></FONT></TD>"
     + "      <TD><A HREF=" + thisServletURI + "?date=" + dfDateField.format(nextMonth.getTime()) + "><FONT SIZE=-1 COLOR=" +
cTFontsColor + ">" + dfMonthName.format(nextMonth.getTime()) + "&nbsp;&#151;></FONT></A></TD>"
     + "    </TR>"
     + "    </TABLE>"
     + "  </TD>"
     + "  </TR>"
     + "  <TR ALIGN=CENTER BGCOLOR=" + cDayBarColor + ">"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_sun") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_mon") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_tue") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_wed") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_thu") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_fri") + "</B></FONT></TD>"
     + "  <TD WIDTH=14%><FONT COLOR=" + cTFontsColor + "><B>" + res.getString("str_sat") + "</B></FONT></TD>"
     + "  </TR>";
  }
  
  //tabla central donde estan las fechas y enlaces para añadir evento y crea las celdas
  String tableCells(java.util.Calendar thisMonth, Student student) {
    StringBuffer returnValue = new StringBuffer();
    
    // record the current value of the field Calendar.MONTH
    int iThisMonth = thisMonth.get(java.util.Calendar.MONTH);
    boolean firstCell = true;

    java.util.Calendar today = java.util.Calendar.getInstance(); //used to highlight today's date cell in the calendar
    java.util.Calendar endOfMonth = (java.util.Calendar)thisMonth.clone();
    endOfMonth.set(java.util.Calendar.DATE,endOfMonth.getActualMaximum(java.util.Calendar.DATE));
    endOfMonth.add(java.util.Calendar.DATE,7-endOfMonth.get(java.util.Calendar.DAY_OF_WEEK));

    // set calendar to first day of the first week (may move back 1 month)
    thisMonth.add(java.util.Calendar.DATE,1-thisMonth.get(java.util.Calendar.DAY_OF_WEEK));

    // get a recordset of this month's events, including extra days at beginning and end
    String startDate = dfMySQLDate.format(thisMonth.getTime());
    String endDate = dfMySQLDate.format(endOfMonth.getTime());
    
    // select only events for this calendar display:
    String sqlQueryString = "SELECT * FROM Events "
    + "WHERE TO_DAYS(Sdate) >= TO_DAYS('" + startDate + "')"
    + " AND TO_DAYS(Sdate) <= TO_DAYS('" + endDate + "')"
    + (" AND (Section='"+student.getCourse_id()+"')")
    + " ORDER BY Sdate";

    try {
      boolean hasEvents=false;
      log.paranoid("Begin Try.","Calendar:tableCells");
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Getting result set.","Calendar:tableCells");
      ResultSet rsEvents = stmt.executeQuery(sqlQueryString);
      log.paranoid("moving to first entry of result set.","Calendar:tableCells");
      if (rsEvents.isBeforeFirst()) {
	log.paranoid("Events existed.","Calendar:tableCells");
	hasEvents=true;
        rsEvents.first();
      } 
      do {
	log.paranoid("Begin do.","Calendar:tableCells");
        returnValue.append("<TR BORDER=1 VALIGN=TOP>"); 
        //aqui se introducen las celdas de fechas y el numero de la fecha como el enlace
        for (int i=0; i < 7; i++) {
	  log.paranoid("Begin for.","Calendar:tableCells");
          returnValue.append("<TD HEIGHT=100");
          if (thisMonth.get(java.util.Calendar.MONTH) != iThisMonth) returnValue.append(" BGCOLOR=" + cOutDayColor);  // dark background for out-of-month days
          else if (dfDateField.format(thisMonth.getTime()).equals(dfDateField.format(today.getTime()))) returnValue.append(" BGCOLOR="
+ cTodaysColor); // grey background for today's date
          else returnValue.append(" BGCOLOR=" + cBGCellColor);  // default white background
	  log.paranoid("End bgcolor appends to returnValue StringBuffer.","Calendar:tableCells");
          returnValue.append("><A HREF=# onClick=javascript:PopupWindow('" + thisServletURI + "','new','" +
dfDateField.format(thisMonth.getTime()) + "');><FONT COLOR=" + cDtFontColor + " SIZE=-1><B>" +
dfDayOfMonth.format(thisMonth.getTime()) + "</B></FONT></A>");
	  log.paranoid("End link appending.","Calendar:tableCells");
	  if (hasEvents && !rsEvents.isAfterLast()) {//only append if more rows in result set...
	    log.paranoid("We have eventage!!","Calendar:tableCells");
            returnValue.append(todaysEvents(rsEvents,dfMySQLDate.format(thisMonth.getTime()))); // write out the cell contents
	  }
	  log.paranoid("appended 'todaysEvents'","Calendar:tableCells");
          if (firstCell) {
            returnValue.append("<CENTER><FORM><INPUT TYPE=BUTTON VALUE= "+res.getString("str_help")+" onClick=javascript:PopupWindow('" + thisServletURI +
"','help','yes');></FORM></CENTER>");
            firstCell = false;
          }
          returnValue.append("</TD>");
          thisMonth.add(java.util.Calendar.DATE,1);
	  log.paranoid("End for.","Calendar:tableCells");
        }
        returnValue.append("</TR>");
	log.paranoid("End doWhile","Calendar:tableCells");
      } while(thisMonth.getTime().before(endOfMonth.getTime()));

      rsEvents.close();
      stmt.close();
      conn.close();
      log.paranoid("End try","Calendar:tableCells");
    } catch(Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Calendar:tableCells");
      if (checkForSection()){
         return "<tr><td colspan=7>" + res.getString("str_section_added") + "</td></tr>";
      }
      if (createEventsTable()) return "<tr><td COLSPAN=7>" + res.getString("str_table_made") + "</td></tr>";
      else return res.getString("str_error_db") + e.getMessage();
    }
    // restore date to first of this month:
    thisMonth.add(java.util.Calendar.MONTH,-1);
    thisMonth.set(java.util.Calendar.DATE,1);    
    return returnValue.toString();
  }
  
  
  String todaysEvents(ResultSet rsEvents, String today) { 
    StringBuffer returnValue = new StringBuffer("<font size=-2>");
    log.paranoid("Begin try.","Calendar:todaysEvents");
    try {
      while(dfMySQLDate.format(rsEvents.getDate("Sdate")).equals(today)) {
        log.paranoid("Begin While","Calendar:todaysEvents");        
        java.util.Date start = rsEvents.getTime("Sdate");
        java.util.Date end = rsEvents.getTime("Edate");
        String timeSpan = null;
        if (dfTimeField.format(start).equals(time0000) && dfTimeField.format(end).equals(time0000)) timeSpan = "TODAY:";
        else if (dfTimeField.format(start).equals(time0000) && dfTimeField.format(end).equals(time2359)) timeSpan = "ALL DAY:";
        else timeSpan = dfTimeField.format(rsEvents.getTime("Sdate")) + "-" + dfTimeField.format(rsEvents.getTime("Edate"));

        returnValue.append("<br><A HREF=# onClick=javascript:PopupWindow('" + thisServletURI + "','revise','" +
rsEvents.getString("EventID") + "');><font color=#0000FF>" + timeSpan + "</font></a>&nbsp;");
        
    	if(rsEvents.getBoolean("Flagged")) returnValue.append("<FONT COLOR=#FF0000>"); // write Desription field in red if flagged
    	returnValue.append(rsEvents.getString("Description"));
    	if(rsEvents.getBoolean("Flagged")) returnValue.append("</FONT>"); // returns red font color to default
        if (!rsEvents.next()) return returnValue.toString() + "</font>";;
        log.paranoid("End while","Calendar:todaysEvents");
      }
    } catch (SQLException e) {
      log.sparse("Caught: " + e.getMessage(),"todaysEvents");
    }
    return returnValue.toString() + "</font>"; 
  }
      
  String tableFooter(java.util.Calendar thisMonth) {
    MessageFormat mf = new MessageFormat(res.getString("str_copy2"));
    Object[] args = {
      "<a href=mailto:wight@chem.utah.edu>",
      "</a>",
      "<a href='http://www.gnu.org/copyleft/gpl.html'>"
    };
    java.util.Calendar nextMonth = (java.util.Calendar)thisMonth.clone(); nextMonth.add(java.util.Calendar.MONTH,+1);
    java.util.Calendar lastMonth = (java.util.Calendar)thisMonth.clone(); lastMonth.add(java.util.Calendar.MONTH,-1);

    return "<TR>"
     + "  <TD BGCOLOR=" + cTopBarColor + " ALIGN=CENTER COLSPAN=7>"
     + "    <TABLE WIDTH=100% BORDER=0 CELLSPACING=0 CELLPADDING=0>"
     + "    <TR ALIGN=CENTER>"
     + "      <TD><A HREF=" + thisServletURI + "?date=" + dfDateField.format(lastMonth.getTime()) + "><FONT SIZE=-1 COLOR=" +
cTFontsColor + "><&#151;&nbsp;" + dfMonthName.format(lastMonth.getTime()) + "</FONT></A></TD>"
     + "      <TD><A HREF=" + thisServletURI + "><FONT SIZE=-1 COLOR=" + cTFontsColor + "><B>Go To Current Month</B></FONT></A></TD>"
     + "      <TD><A HREF=" + thisServletURI + "?date=" + dfDateField.format(nextMonth.getTime()) + "><FONT SIZE=-1 COLOR=" +
cTFontsColor + ">" + dfMonthName.format(nextMonth.getTime()) + "&nbsp;&#151;></FONT></A></TD>"
     + "    </TR>"
     + "    </TABLE>"
     + "  </TD>"
     + "</TR>"
     + "<TR>"
     + "</TABLE>"
     + "<FONT SIZE=-2>" + mf.format(args) + "</FONT>";
  }
  
  boolean allowEdit(Student student) {
    return student.getIsInstructor();
  }

  String newEventForm(String date, Student student) {
    StringBuffer returnValue = new StringBuffer();

    // print blank popup form for entering a new event:
    returnValue.append("<body onLoad=window.focus()>"
     + "<H3>" + res.getString("str_title_ctrl_pnl") + "</H3>"
     + "<FORM METHOD=POST>"
     + "<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='New'>"
     + "  <TABLE BORDER=0 CELLSPACING=0>"
     + "  <TR><TD>Date:</TD><TD><INPUT TYPE=TEXT SIZE=8 NAME=EventDate VALUE=" + date + "></TD></TR>"
     + "  <TR><TD ROWSPAN=3 VALIGN=TOP>" + res.getString("str_time") 
     + "<BR><FONT SIZE=-2>(e.g., " + dfTimeField.format(new java.util.Date(4800000)) +
")</FONT></TD>"
     + "  <TD><INPUT TYPE=RADIO NAME=EventType VALUE=REGULAR CHECKED>"
     + "    " + res.getString("str_start") + "<INPUT TYPE=TEXT SIZE=8 NAME=STime>"
     + "    " + res.getString("str_end") + "<INPUT TYPE=TEXT SIZE=8 NAME=ETime></TD></TR>"
     + "  <TR><TD><INPUT TYPE=RADIO NAME=EventType VALUE=ALLDAY>" + res.getString("str_allday")
     + "(" + dfTimeField.format(new java.util.Date(25200000)) + " - "
+ dfTimeField.format(new java.util.Date(111540000)) + ")</TD></TR>"
     + "  <TR><TD><INPUT TYPE=RADIO NAME=EventType VALUE=SPECIAL>" + res.getString("str_notime") + "</TD></TR>"
     + "  <TR><TD>" + res.getString("str_description") 
     + "</TD><TD><INPUT TYPE=TEXT SIZE=34 NAME=Description></TD></TR>"
     + "  <TR><TD></TD><TD><INPUT TYPE=CHECKBOX NAME=Flagged VALUE='true'>" + res.getString("str_dored") + "</TD></TR>"
     + displaySectionInfo(student)
     + "  <TR><TD VALIGN=TOP>" + res.getString("str_notes") + "<br><FONT SIZE=-2>"
     + res.getString("str_view") + "</FONT></TD><TD><TEXTAREA NAME=Notes ROWS=6 COLS=32 WRAP=SOFT></TEXTAREA></TD></TR>"
     + "  </TD></TR><TR><TD COLSPAN=2>"
     + "  <INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_create") + "'>"
     + "  </TD></TR><TR><TD COLSPAN=2>"    
     + "  <FONT SIZE=-2>"+ res.getString("str_refresh") + "</FONT>"
     + "  </TD></TR>"    
     + "  </TABLE>"
     + "</FORM>"
     + "</body></html>");
    
    return returnValue.toString();
  }
  
  String reviseEventForm(String eventID, Student student) {
    String date = null;
    String sTime = null;
    String eTime = null;
    String description = null;
    String notes = null;
    String user = null;
    String section = null;
    boolean flagged = false;
    
    // retrieve the existing data for the selected event
    try {
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM Events WHERE EventID=" + eventID;
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      if (rs.next()) {
        date = dfDateField.format(rs.getDate("Sdate"));
        sTime = dfTimeField.format(rs.getTime("Sdate"));
        eTime = dfTimeField.format(rs.getTime("Edate"));
        description = rs.getString("Description");
        flagged = rs.getBoolean("Flagged");
        notes = rs.getString("Notes");
        user = rs.getString("User");
        section = rs.getString("Section");
      }
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Calendar:reviseEventForm");
      if (checkForSection()){
        return res.getString("str_section_added2");
      }
      return e.getMessage();
    }
    MessageFormat mf = new MessageFormat(res.getString("str_enterer")); 
    Object[] args = {
      user
    };
    // print a pre-loaded form for revising an existing event:
    StringBuffer returnValue = new StringBuffer();    
    returnValue.append("<body onLoad=window.focus()>"
     + "<H3>Control Panel</H3>"
     + "<FORM METHOD=POST>"
     + "<INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='Revise'>"
     + "<INPUT TYPE=HIDDEN NAME='EventID' VALUE=" + eventID + ">"
     + "  <TABLE BORDER=0 CELLSPACING=0>"
     + "  <TR><TD>Date:</TD><TD>"
     + "    <TABLE WIDTH=100% BORDER=0><TR><TD>"
     + "      <INPUT TYPE=TEXT SIZE=8 NAME='EventDate' VALUE='" + date + "'></TD>"
     + "      <TD ALIGN=RIGHT><FONT SIZE=-2>" + mf.format(args) + "</FONT>"
     + "      </TD></TR></TABLE>"
     + "  </TD></TR>");
    returnValue.append("<TR><TD ROWSPAN=3 VALIGN=TOP>" + res.getString("str_time") 
        + "<BR><FONT SIZE=-2>(e.g., " + dfTimeField.format(new java.util.Date(91200000)) 
        + ")</FONT></TD>");
    returnValue.append("<TD><INPUT TYPE=RADIO NAME=EventType VALUE=REGULAR CHECKED>"
     + res.getString("str_start") + "<INPUT TYPE=TEXT SIZE=8 NAME=STime VALUE='" + sTime + "'>"
     + res.getString("str_end") + "<INPUT TYPE=TEXT SIZE=8 NAME=ETime VALUE='" + eTime + "'></TD></TR>"
     + "  <TR><TD><INPUT TYPE=RADIO NAME=EventType VALUE=ALLDAY>"
     + res.getString("str_allday") + "(" + time0000 + " - " + time2359 + ")</TD></TR>"
     + "  <TR><TD><INPUT TYPE=RADIO NAME=EventType VALUE=SPECIAL>"
     + res.getString("str_notime") + "</TD></TR>"
     + "  <TR><TD>" + res.getString("str_description") + "</TD><TD><INPUT TYPE=TEXT SIZE=34 NAME=Description VALUE=\"" + description + "\"><TD></TR>");
    returnValue.append("<TR><TD></TD><TD><INPUT TYPE=CHECKBOX NAME=Flagged VALUE='true'");
    if (flagged) returnValue.append(" CHECKED><FONT COLOR=#FF0000");
    returnValue.append(">" + res.getString("str_dored") + "</FONT></TD></TR>");
    //returnValue.append(displaySectionInfo(student, section));
    returnValue.append("<TR><TD VALIGN=TOP>" + res.getString("str_notes") 
        + "<br><FONT SIZE=-2>" + res.getString("str_view") 
        + "</FONT></TD><TD><TEXTAREA NAME=Notes ROWS=6 COLS=32 WRAP=SOFT>" 
        + notes + "</TEXTAREA></TD></TR>"
     + "  </TD></TR><TR><TD COLSPAN=2>"
     + "  <TABLE BORDER=0 CELLSPACING=0><TR>"
     + "    <TD><INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_modify") + "'></TD></FORM>"
     + "    <TD><FORM METHOD=POST>"
     + "    <INPUT TYPE=HIDDEN NAME='UserRequest' VALUE='Delete'>"
     + "    <INPUT TYPE=HIDDEN NAME='EventDate' VALUE='" + date + "'>"
     + "    <INPUT TYPE=HIDDEN NAME='EventID' VALUE=" + eventID + ">"
     + "    <INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_delete") + "'></TD></FORM></TR>"
     + "  </TABLE>"
     + "  </TD></TR><TR><TD COLSPAN=2>"    
     + "  <FONT SIZE=-2>" + res.getString("str_refresh") + "</FONT>"    
     + "  </TD></TR>"    
     + "  </TABLE>"
     + "</body></html>");
    
    return returnValue.toString();
  }

  // the doPost method is used to make changes to the calendar
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    if (viewOnly) return;  // this servlet is set to disallow changes
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "Calendar");
      return;
    }
    // from here on, student id is assumed to be valid
    if (student.getIsTA()) {
      TA ta = TAS.getTA(student.getIDNumber());
      StringBuffer err = new StringBuffer();
      if (!ta.hasPermission("Calendar",request,student,err)) {
        out.println(err.toString());
        return;
      }
    }
    
    //le quita los permisos al TA, si entra a un curso que va ver o estudiar
    if (student.getTAtoStudent()) {
    	student.setIsInstructor(false);
    	student.setTAStatus(false);
    }
    
    thisServletURI = request.getRequestURI();
    
    out.println("<HTML><head><title>" + res.getString("str_title_ctrl_pnl") + "</title>\n");
    out.println("<SCRIPT Language=JavaScript>\n");
    out.println("<!-- hide script from older browsers\n");
    out.println("function finish(how,date) {\n");
    out.println("  if (how == 'OK') {\n");
    out.println("    opener.document.location = '" + thisServletURI + "?date='+date;\n");
    out.println("    window.close();\n");
    out.println("  } else if (how == 'conflict') {\n");
    out.println("    alert('" + res.getString("str_warn_conflict") + "');\n");
    out.println("    opener.document.location = '" + thisServletURI + "?date='+date;\n");
    out.println("    window.close();\n");
    out.println("  } else if (how == 'dbError') {\n");
    out.println("    alert('" + res.getString("str_error_db2") + "');\n");
    out.println("    opener.document.location = '" + thisServletURI + "?date='+date;\n");
    out.println("    window.back();\n");//note: some browsers don't like this...
    out.println("  } else if (how == 'bad') {\n");
    out.println("    alert('" + res.getString("str_warn_format") + "');\n");
    out.println("    window.back();\n");
    out.println("  }\n");
    out.println("}\n");
    out.println("-->\n");
    out.println("</SCRIPT></head>");
    
    String userRequest = request.getParameter("UserRequest");
    String eventID = request.getParameter("EventID");
    boolean flagged = Boolean.valueOf(request.getParameter("Flagged")).booleanValue();
    String sqlQuery = null;
    String sqlConflict = null;
      
    if (userRequest.equals("Delete"))
      sqlQuery = "DELETE FROM Events WHERE EventID='" + eventID + "'";
    else {
      String sDate = null;
      String eDate = null;
      String description = removeSpecialCharacters(request.getParameter("Description"));
      String notes = removeSpecialCharacters(request.getParameter("Notes"));
      String section = request.getParameter("Section");
      try {
        String eventType = request.getParameter("EventType");
        String date = dfMySQLDate.format(dfDateField.parse(request.getParameter("EventDate")));
        if (eventType.equals("REGULAR")) {
          sDate = date + " " + dfMySQLTime.format(dfTimeField.parse(request.getParameter("STime")));
          String eTime = request.getParameter("ETime");
          if (eTime.equals("")) eDate = sDate; // default ending time is same as starting time
          else eDate = date + " " + dfMySQLTime.format(dfTimeField.parse(request.getParameter("ETime")));
        } else if (eventType.equals("SPECIAL")) {
          sDate = date + " 00:00";
          eDate = date + " 00:00";
        } else if (eventType.equals("ALLDAY")) {
          sDate = date + " 00:00";
          eDate = date + " 23:59";
        }
      }
      catch (Exception e) {
	log.sparse("Caught: " + e.getMessage(),"Calendar:doPost");
        out.println("<body onLoad=finish('bad','" + request.getParameter("EventDate") + "');>");
        out.println("</body></html>");
   	    return;  
      }
      
      if (userRequest.equals("New")) {
      	
      sqlQuery = "INSERT INTO events (Sdate,Edate,User,Description,Notes,Flagged,Section) VALUES ( '"+sDate+"', '"+eDate+"', '"+student.getFullName()+"', '"+description+"', '"+notes+"', '"+(flagged?1:0)+"', '"+section+"')";
      	      	
      sqlConflict = "SELECT * FROM events WHERE Section = '"+student.getCourse_id()+"' AND NOT (Sdate >= '" + eDate + "'OR Edate<='" + sDate + "')";
      }
      else if (userRequest.equals("Revise")) {
        sqlQuery = "UPDATE Events SET "
         + "Sdate='" + sDate + "',Edate='" + eDate + "',Flagged='" + (flagged?1:0) + "',User='" + student.getFullName() +
"',Description='" + description + "',Notes='" + notes + "', Section='" + student.getCourse_id()
         + "' WHERE EventID=" + eventID;
        sqlConflict = "SELECT * FROM Events WHERE Section = '"+student.getCourse_id()+"' AND (NOT (Edate<='" + sDate + "' OR Sdate>='" + eDate + "') AND EventID<>'" + eventID +
"')";
      }
    }
    
    try {
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      if (userRequest.equals("New") || userRequest.equals("Revise")) {
        // check for conflicting events; if any records returned, a conflict exists.
        ResultSet rsConflict = stmt.executeQuery(sqlConflict);
        if (stmt.executeUpdate(sqlQuery) != 1) {
        	out.println(res.getString("str_warn_caution") + stmt.executeUpdate(sqlQuery));
        	}
        if (rsConflict.next()) {// conflict exists
          out.println("<body onLoad=finish('conflict','" + request.getParameter("EventDate") + "');>");
    	 do {
    	   out.println(rsConflict.getString("Description") + "<br>");
    	   } while (rsConflict.next());
    	} 
    	else  // no conflicts; normal termination
          out.println("<body onLoad=finish('OK','" + request.getParameter("EventDate") + "');>");  
      } 
      else {
        if (stmt.executeUpdate(sqlQuery) != 1) out.println(res.getString("str_warn_caution") + stmt.executeUpdate(sqlQuery));
        out.println("<body onLoad=finish('OK','" + request.getParameter("EventDate") + "');>");
      }
      out.println("</body></html>");
      stmt.close();
      conn.close();
    } catch (Exception e) { // SqlExceptions caught here
      log.sparse("Caught; " + e.getMessage(),"Calendar:doPost");
      out.println("<body onLoad=finish('dbError','" + request.getParameter("EventDate") + "');>");
      out.println(e.getMessage());
      if (checkForSection()){
        out.println("\n" + res.getString("str_section_added3"));
      }
      out.println("</body></html>");  
    }
  }
  
  String removeSpecialCharacters(String inString) {
    StringBuffer sb = new StringBuffer(inString);
    int i=0;
    char c = '\\';
    
    while (sb.toString().indexOf('\'',i) >= 0) {
      i = sb.toString().indexOf('\'',i);
      try {
        sb = sb.insert(i,c); 
      } catch (Exception e){
      }
      i += 2;
    }
    return sb.toString();
  }

  boolean createEventsTable() {
    try {
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Events (EventID INT PRIMARY KEY AUTO_INCREMENT,"
      + "Sdate DATETIME,Edate DATETIME,User VARCHAR(50),Description TEXT,Notes TEXT,Flagged INT,Section VARCHAR(3) DEFAULT '1')");
      return true;
    }
    catch (Exception e) {
      log.normal("Caught: " + e.getMessage(),"Calendar:createEventsTable");
      return false;
    }
  }

 //for new events.
  String displaySectionInfo(Student student)
  {
    StringBuffer buf = new StringBuffer("<TR><TD>");

    if (!student.getIsInstructor()){
      buf.append("<INPUT TYPE=hidden NAME='Section' Value='" + student.getCourse_id() + "'></TD></TR>");
      return buf.toString();
    }

    /*try{
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
        return "<TR><TD><INPUT TYPE=hidden NAME='Section' Value='1'></TD></TR>";
      buf.append(res.getString("str_section") + "</TD><TD><SELECT NAME='Section'>");
      //sections start w/ 1, therefore, i=1... RDZ
      int numSections = rsSections.getInt("Value");
      for(int i=1; i<=numSections; i++){
          buf.append("<OPTION>" + i);                                                 }
      rsSections.close();
      stmt.close();
      conn.close();
    }catch (Exception e){
      log.sparse("Caught; " + e.getMessage(),"Calendar:displaySectionInfo");
     buf.append("<INPUT TYPE=hidden NAME='Section' Value='1'></TD></TR>");
     return buf.toString();
    }*/
    
    /*buf.append("<OPTION SELECTED>" + res.getString("str_all") + "</SELECT></TD></TR>");
    return buf.toString();*/
    return "<TR><TD><INPUT TYPE=hidden NAME='Section' Value='"+student.getCourse_id()+"'></TD></TR>";
  }
//overrides above displaySectionInfo for revising events...

  String displaySectionInfo(Student student, String section)
  {
    StringBuffer buf = new StringBuffer("<TR><TD>");
//in case, later on, students are allowed to create events . . .
    if (!student.getIsInstructor()){
      buf.append("<INPUT TYPE=hidden NAME='Section' Value='"+(section.equals("All")?"All":String.valueOf(student.sectionID)));
      buf.append("'></TD></TR>");
      return buf.toString();
    }

    try{
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
       return "<TR><TD><INPUT TYPE=hidden NAME='Section' VALUE='1'></TD></TR>";
      //sections start w/ 1, therefore, i=1... RDZ
      int numSections=rsSections.getInt("Value");
      buf.append(res.getString("str_section") + "</TD><TD><SELECT NAME='Section'>");
      for(int i=1; i<=numSections; i++){
        if (section != null && section.equals(String.valueOf(i)))
          buf.append("<OPTION selected>" +i);
        else
          buf.append("<OPTION>" + i);
      }
      rsSections.close();
      stmt.close();
      conn.close();
    }catch (Exception e){
     log.sparse("Caught: " + e.getMessage(),"Calendar:displaySectionInfo");
     buf.append("<INPUT TYPE=hidden NAME='Section' Value='1'></TD></TR>");
     return buf.toString();
    }
    if (section != null && section.equals("All"))
      buf.append("<OPTION selected>" + res.getString("str_all") + "</SELECT></TD></TR>");
    else
      buf.append("<OPTION>" + res.getString("str_all") + "</SELECT></TD></TR>");
    return buf.toString();
  }

  int getSections(){
    try{
      Class.forName(mySqlJdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections=stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      if (rsSections.next())
        return rsSections.getInt("Value");
    }catch(Exception e){
      log.sparse("Caught: " + e.getMessage(),"Calendar:getSections");
    }
    return 1;
  } 
//for backwards compatability w/ old versions of eledge . . .
//this is called from the tableCells exception handler, before the call to create
//the tables. Logic works like: if the table is there, then the exception is
//going to be that the Section field doesn't exist, so let's insert it.
//However, assuming the table is -not- there, this statement won't fly, it'll
//be caught by the exception, and the correct table will be made, anyway.
  boolean checkForSection(){
    try{
       Class.forName(mySqlJdbcDriver).newInstance();
       Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
       Statement stmt = conn.createStatement();
       stmt.executeUpdate("ALTER TABLE Events ADD (Section VARCHAR(3) DEFAULT '1')");
    }catch(Exception e){
       log.normal("Caught: " + e.getMessage(),"Calendar:checkForSection");
       return false;
    }
    return true;
  }

}

