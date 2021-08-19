/*PermissionData.java. This class handles the database-side of the permission objects.
 * Author: Robert Zeigler
 * Started: 7/22/03
 * Last Modified: 7/30/03
 * */

package Eledge;

import java.sql.*;

public class PermissionData {
  private String PDTable="Permissions"; //table to store the generic permissions.
  private String argsTable="PermissionArguments"; //table to store the arguments for permissions.
  private Logger log = new Logger();

  protected boolean createPermissionTables() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "CREATE TABLE " + PDTable + " (Name VARCHAR(50) PRIMARY KEY, "
       + "Servlet VARCHAR(50), Request VARCHAR(50), SqlFormat TEXT, NumArgs INT, "
       + "DenyMsg VARCHAR(50), EditMsg VARCHAR(50), SetToTeacher VARCHAR(5))";
      log.paranoid("Executing: " + sql,"PermissionData:createTables");
      stmt.executeUpdate(sql);
      sql = "CREATE TABLE " + argsTable + " (Name VARCHAR(50), Argument VARCHAR(50), ArgNum INT)";
      log.paranoid("Executing: " + sql,"PermissionData:createTables");
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.normal("Caught: " + e.getMessage(),"PermissionData:createTables");
      return false;
    }
    return true;
  }
//loads a single permission from the permission table.
  public Permission loadPermission(String pName) {
    Permission p;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      String sql = "SELECT * FROM " + PDTable + " WHERE Name='" + pName + "'";
      log.paranoid("Executing: " + sql,"PermissionData:loadPermission");
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.next()) {
        log.paranoid("Result set empty!","PermissionData:loadPermission");
        return null;
      }
      String name, servlet, request, deny, edit, pattern;
      int numArgs=0;
      boolean setToTeacher=false;
      String[] args=null;
      name = rs.getString("Name");
      servlet = rs.getString("Servlet");
      request = rs.getString("Request");
      deny = rs.getString("DenyMsg");
      edit = rs.getString("EditMsg");
      pattern = rs.getString("SqlFormat");
      numArgs = rs.getInt("NumArgs");
      setToTeacher = rs.getBoolean("SetToTeacher");
      rs.close();
      log.paranoid("pattern is: " + pattern,"PermissionData:loadPermission");
      if (numArgs>0) {
        sql = "SELECT * FROM " + argsTable + " WHERE Name='" + pName + "' ORDER BY ArgNum";
        log.paranoid("Executing: " + sql,"PermissionData:loadPermission");
        rs = stmt.executeQuery(sql);
        if (!rs.next()) {
          log.normal("Error: numArgs>0, but no data for " + pName + " in " + argsTable,"PermissionData:loadPermission");
          args=new String[numArgs];
          //initialize the args array to empty strings. This helps avoid nasty "null pointer" errors later on. ;)
          for (int i=0; i<numArgs; i++) {
            args[i]="";
          }
        } else {
          rs.last();
          int num=rs.getRow();
          if (num != numArgs) {
            log.normal("Error: numArgs and amount of args stored are not equal for " + pName,"PermissionData:loadPermission");
            numArgs=num;
          } 
          args = new String[num];
          int i=0;
	  rs.beforeFirst();
          while (rs.next()) {
            args[i] = rs.getString("Argument");
	    log.paranoid("Loaded argument: " + args[i] + " into temp array for " + name + " permission","PermissionData:loadPermission");
            i++;
          }
        }
      }
      p = new Permission(name, pattern, args, deny, edit, servlet, request,setToTeacher);
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"PermissionData:loadPermission",e);
      //no sense in trying to recall this method, because if the table has just been created,
      //we already know this permission isn't going to be in there, and this method will just return
      //null anyway. So, return null.
      createPermissionTables();
      return null;
    }
    return p;
  }

  public boolean savePermission(Permission p) {
    boolean exists;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      String sql = "SELECT * FROM " + PDTable + " WHERE Name='" + p.getName() + "'";
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        rs.updateString("Servlet",p.getRequest());
        rs.updateString("Request",p.getRequest());
        rs.updateString("SqlFormat",p.getSqlAsString());
        rs.updateString("DenyMsg",p.getDenyMsg());
        rs.updateString("EditMsg",p.getEditMsg());
        rs.updateInt("NumArgs",p.getNumArgs());
        rs.updateBoolean("SetToTeacher",p.getRequireTeacherStatus());
        rs.updateRow();
        rs.close();
        sql = "DELETE FROM " + argsTable + " WHERE Name='" + p.getName() + "'";
        log.paranoid("Exeucting: " + sql,"PermissionData:savePermission");
      } else {
        sql = "INSERT INTO " + PDTable + " (Name, Servlet, Request, SqlFormat, NumArgs,"
          + " DenyMsg, EditMsg) VALUES('" + p.getName() + "','" + p.getServlet()
          + "','" + p.getRequest() + "','" + p.getSql() + "','" + p.getNumArgs()
          + "','" + p.getDenyMsg() + "','" + p.getEditMsg() + "')";
        log.paranoid("Executing: " + sql,"PermissionData:savePermission");
      }
      stmt.executeUpdate(sql);
      String[] args = p.getArgs();
      for (int i=0;i<args.length;i++) {
        sql = "INSERT INTO " + argsTable + " (Name, Argument, ArgNum) VALUES('" + 
          p.getName() + "','" + args[i] + "','" + i + "')";
        log.paranoid("Executing: " + sql,"PermissionData:savePermission");
        stmt.executeUpdate(sql);
      }
      stmt.executeUpdate(sql);
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"PermissionData:savePermission");
      if (createPermissionTables()) {
        return savePermission(p);
      }
      return false;
    }
    return true;
  }

}
