package Eledge;

import java.io.*;
import java.util.Date;
import java.util.*;
import java.text.SimpleDateFormat;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class Logger {

//log info
  private final static int LOG_NONE=0;
  private final static int LOG_SPARSE=1;
  private final static int LOG_NORMAL=2;
  private final static int LOG_PARANOID=3;
  //set these to static so one load works for all servlets.
  private static String logFile;
  private static int logLevel;
//no sense in connecting to database everytime. This variable set for loading
//of variables logLevel and logFile
  private static boolean loaded=false;
  private SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
  private RBStore res = EledgeResources.getLoggerBundle();
  public Logger() {
//only do this if this is the first instance of the object.
    if (!loaded) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * From LoggingParameters");
        if (rs.next()) {
          logFile=rs.getString("LogFile");
          logLevel=rs.getInt("LogLevel");
        } else {
          logLevel=LOG_PARANOID;
          logFile="/var/log/Eledge/";
        }

      } catch (Exception e){
        addLoggingTable();
        logLevel=LOG_PARANOID;
        logFile="/var/log/Eledge/";
      }
      loaded=true;
    }
  }


  synchronized protected void paranoid(String logstring, String callingClass) {
     if (logLevel>=LOG_PARANOID) {
       Date now = new Date();
       try {
         FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
         writer.write("\n"+now+":"+Course.name+ ":" + callingClass + ": ");
         writer.write(logstring);
         writer.close();
       } catch (Exception e) {
       }
     }
     return;
  }

  synchronized protected void paranoid(String logstring, String callingClass, Exception ex) {
     if (logLevel>=LOG_PARANOID) {
       Date now = new Date();
       try {
         FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
         writer.write("\n"+now+":"+Course.name+ ":" + callingClass + ": ");
         writer.write(logstring + "\n");
	 PrintWriter pw = new PrintWriter(writer);
	 ex.printStackTrace(pw);
         writer.close();
       } catch (Exception e) {
       }
     }
     return;
  }

  synchronized protected void normal(String logstring, String callingClass) {
    if (logLevel>=LOG_NORMAL) {
      Date now = new Date();
      try {
        FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
        writer.write("\n"+now+":"+Course.name+":" + callingClass +": ");
        writer.write(logstring);
        writer.close();
      } catch(Exception e) {
      }
    }
    return;
  }

  synchronized protected void normal(String logstring, String callingClass, Exception ex) {
    if (logLevel>=LOG_NORMAL) {
      Date now = new Date();
      try {
        FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
        writer.write("\n"+now+":"+Course.name+":" + callingClass +": ");
        writer.write(logstring + "\n");
	PrintWriter pw = new PrintWriter(writer);
	ex.printStackTrace(pw);
        writer.close();
      } catch(Exception e) {
      }
    }
    return;
  }

  synchronized protected void sparse(String logstring, String callingClass) {
    if (logLevel>=LOG_SPARSE) {
      Date now = new Date();
      try {
        FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
        writer.write("\n"+now+":"+Course.name+":" + callingClass +": ");
        writer.write(logstring);
        writer.close();
      }catch (Exception e) {
      }
    }
    return;
  }

  synchronized protected void sparse(String logstring, String callingClass, Exception ex) {
    if (logLevel>=LOG_SPARSE) {
      Date now = new Date();
      try {
        FileWriter writer = new FileWriter(logFile + Course.name + "_" + df.format(now), true);
        writer.write("\n"+now+":"+Course.name+":" + callingClass +": ");
        writer.write(logstring);
	PrintWriter pw = new PrintWriter(writer);
	ex.printStackTrace(pw);
        writer.close();
      }catch (Exception e) {
      }
    }
    return;
  }

  private boolean addLoggingTable() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE LoggingParameters (LogFile VARCHAR(100), LogLevel INT)");
      stmt.executeUpdate("INSERT INTO LoggingParameters VALUES ('/var/log/Eledge/',3)");
    } catch(Exception e) {
      return false;
    }
    return true;
  }
//Creates a table displaying the log information.
//Not static; must have an instantiated logger object to reference it.
//Want that to be the case to make sure that logFile and logLevel are
//correct instantiated.
  protected String displayLogInfo() {
    StringBuffer buf = new StringBuffer("");
    buf.append("<h3>" + Course.name + " Logging Information</h3>");
    buf.append("<p>" + res.getString("str_log_info1") + "<br>"
    + res.getString("str_log_level_head") + "</p>"
    + "<dl><dt>" + res.getString("str_log_none") + "<dd>"
    + res.getString("str_dd_log_none") 
    + "<dt>" + res.getString("str_log_sparse") 
    + "<dd>" + res.getString("str_dd_log_sparse")
    + "<dt>" + res.getString("str_log_normal") 
    + "<dd>" + res.getString("str_dd_log_normal")
    + "<dt>" + res.getString("str_log_paranoid")
    + "<dd>" + res.getString("str_dd_log_paranoid") + "<br>"
    + res.getString("str_warn_log_paranoid") + "</dl>");
    buf.append("<form method=post><input type=hidden name=UserRequest value='UpdateLogInfo'>");
    buf.append("<table border=0><tr><td>" + res.getString("str_field_log_file") + "</td><td>");
    buf.append("<input type=text name=LogFile value='" + logFile + "'></td></tr>");
    buf.append("<tr><td>LogLevel:</td><td><SELECT name=LogLevel>");
    for (int i=LOG_NONE;i<=LOG_PARANOID;i++) {
      buf.append("<OPTION value='" + i + "'" + (logLevel==i?" SELECTED":"") + ">");
      switch (i) {
        case LOG_NONE:
          buf.append(res.getString("str_log_none"));
          break;
        case LOG_SPARSE:
          buf.append(res.getString("str_log_sparse"));
	  break;
        case LOG_NORMAL:
          buf.append(res.getString("str_log_normal"));
          break;
        case LOG_PARANOID:
	  buf.append(res.getString("str_log_paranoid"));
	  break;
        default : buf.append(res.getString("str_log_none"));
      }
    }
    buf.append("</SELECT></td></tr></table>");
    buf.append("<input type=submit value='" + res.getString("str_btn_save_log")
    + "'></form>");
    return buf.toString();
  }
  
  synchronized protected boolean updateLogInfo(HttpServletRequest request){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //set the static variables . . . 
      logFile=request.getParameter("LogFile");
      logLevel=Integer.parseInt(request.getParameter("LogLevel"));
      //update the database . . .
      stmt.executeUpdate("UPDATE LoggingParameters SET LogFile='" + logFile + "', LogLevel=" + logLevel);
      stmt.close();
      conn.close();
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
