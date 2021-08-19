package Eledge;

//sort of an "overarching" permissions class.
//This class contains a hashmap of all the permissions,
//and is the fundamental "controller" class for 
//permissions (permissions is to template what
//permission is to templateitem).
//Author: Robert Zeigler
//Started: 7/30/03
//Last Modified: 7/30/03

import java.util.*;
import java.sql.*;

public class Permissions {
  private static HashMap permissions; //use hashmap instead of hashtable for improved performance.
  private static Logger log = new Logger();
  private static boolean isLoaded=false;
  private static boolean permissionGroupNamesLoaded=false;
  private static String[] permissionGroupNames=null;
  private static boolean sortedPermissionNamesLoaded=false;
  private static String[] sortedPermissionNames=null;
  private static String PDTable="Permissions"; //table to store the generic permissions.
 
  synchronized public static void loadPermissions() {
    PermissionData pd = new PermissionData();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
      String sql = "SELECT Name FROM " + PDTable + " ORDER BY Name";
      log.paranoid("Executing: " + sql,"PermissionData:loadPermission");
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.next()) {
        rs.close();
        stmt.close();
        con.close();
        permissions=new HashMap();
        return;
      }
      rs.last();
      int numRows = rs.getRow();
      log.paranoid("numRows: " + numRows,"Permissions:loadPermissions");
      rs.beforeFirst();
      if (permissions == null)
        permissions = new HashMap(numRows);
      else
        permissions.clear();
      String[] names = new String[numRows];
      int i = 0;
      while (rs.next()) {
        names[i++] = rs.getString("Name");
      }
      rs.close();
      for (i=0; i<names.length; i++) {
        log.paranoid("Attempting to load permissions for: " + names[i],"Permissions:loadPermissions");
        Permission p = pd.loadPermission(names[i]);
        permissions.put(p.getName(),p);
      }
      rs.close();
      stmt.close();
      con.close();
      isLoaded=true;
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Permissions:loadPermissions",e);
      pd.createPermissionTables();
    }
  }

  public static Permission getPermission(String pName) {
    if (!isLoaded) {
      loadPermissions();
    }
    return (Permission)permissions.get(pName);
  }

  synchronized public static void addPermission(Permission p) {
    if (!isLoaded) {
      loadPermissions();
    }
    permissions.put(p.getName(),p);
  }

  synchronized public static void removePermission(Permission p) {
    if (!isLoaded) {
      loadPermissions();
    }
    permissions.remove(p.getName());
  }

  public static String[] getKeys() {
    if (!isLoaded) {
      loadPermissions();
    }
    return (String[])permissions.keySet().toArray(new String[]{});
  }

  public static String[] getSortedKeys() {
    //to ensure the integrity of sortedPermissionNames. . .
    //we'll go ahead and do an array copy. 
    String[] ret=null;
    if (!sortedPermissionNamesLoaded) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection con = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
        String sql = "SELECT Name FROM " + PDTable + " ORDER BY Name";
        log.paranoid("Executing: " + sql,"PermissionData:loadPermission");
        ResultSet rs = stmt.executeQuery(sql);
        if (!rs.next()) {
          rs.close();
          stmt.close();
          con.close();
          return new String[]{};
        }
        rs.last();
        int numRows = rs.getRow();
        log.paranoid("numRows: " + numRows,"Permissions:loadPermissions");
        rs.beforeFirst();
        sortedPermissionNames = new String[numRows];
        int i = 0;
        while (rs.next()) {
          sortedPermissionNames[i++] = rs.getString("Name");
        }
        rs.close();
        stmt.close();
        con.close();
      } catch (Exception e) {
        log.sparse("Caught: " + e.getMessage(),"Permissions:getSortedKeys");
        return new String[]{};
      }
    }
    sortedPermissionNamesLoaded=true;
    ret = new String[sortedPermissionNames.length];
    System.arraycopy(sortedPermissionNames,0,ret,0,ret.length);
    return ret;
  }

  static public String[] getPermissionGroupNames() {
    String[] ret;
    if (!permissionGroupNamesLoaded) {
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = con.createStatement();
        String sql = "SELECT Servlet FROM Permissions GROUP BY Servlet ORDER BY Servlet";
        log.paranoid("Executing: " + sql,"Permissions:getPermissionGroupNames");
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
          rs.last();
          permissionGroupNames=new String[rs.getRow()];
          rs.first();
          for (int i=0;i<permissionGroupNames.length;i++) {
            permissionGroupNames[i]=rs.getString("Servlet");
            rs.next();
          }
          permissionGroupNamesLoaded=true;
        } else
          permissionGroupNames=new String[]{};
        rs.close();
        stmt.close();
        con.close();
      } catch (Exception e) {
        log.sparse("Caught: " + e.getMessage(),"Permissions:getPermissionGroupNames",e);
        permissionGroupNamesLoaded=false;
      }
    }
    
    ret = new String[permissionGroupNames.length];
    System.arraycopy(permissionGroupNames,0,ret,0,ret.length);
    return ret;
  }
}
