package Eledge;

import java.sql.*;
import java.util.ResourceBundle;
//This class contains the core bits of TA specific information
//for a given permission
//Author: Robert Zeigler
//Started: 7/30/03
//Last Modified: 7/30/03

public class TAPermission {
  //members
  private String permissionLevel;
  private String name; 
  private Logger log = new Logger();
  private RBStore res = EledgeResources.getTAPermissionBundle();
  //constants.
  public static final String PERM_ALL="all";//all access for the given request. 
  public static final String PERM_NONE="none";//no access for the given request.
  public static final String PERM_STUDENT="student";//student-like access for the given request
  public static final String PERM_CONDITIONAL="conditional";//depends on some condition.
                                                            //ie, are they assigned to this student/section?

  public TAPermission(String studID, String permissionName) {
    name = permissionName;
    loadPermissionLevel(studID);
  }
  
  public TAPermission(String studID, String permissionName, String level) {
    name=permissionName;
    setPermissionLevel(level); 
  }

  public boolean loadPermissionLevel(String studentID) {
    boolean ret=false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT " + name + " FROM TAPermissions WHERE StudentIDNumber='"
        + studentID + "'";
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        permissionLevel=rs.getString(name);
        ret=true;
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      ret=false;
      log.sparse("Caught: " + e.getMessage(),"TAPermission:loadPermissionLevel");
    }
    return ret;
  }

  public String getPermissionLevel() {
    return this.permissionLevel;
  }

  public String getName() {
    return name;
  }

  public void setPermissionLevel(String level) {
    if (level.equals(PERM_NONE) || level.equals(PERM_ALL) || level.equals(PERM_CONDITIONAL) || level.equals(PERM_STUDENT)) {
       log.paranoid("Setting permission level to: " + level,"TAPermission:setPermissionLevel");
       this.permissionLevel = new String(level);
       log.paranoid("After the set, permissionLevel is: " + this.permissionLevel,"TAPermissions:setPermissionLevel");
    } else {
      log.paranoid("Error: Permissions of " + level + " invalid!","TAPermission:setPermissionLevel");
      this.permissionLevel = new String(PERM_NONE);
    }
  }

  public String selectBox() {
    StringBuffer buf = new StringBuffer("<SELECT NAME='" + name + "'>");
    //no permission
    buf.append("<OPTION VALUE='" + PERM_NONE +
      (permissionLevel.equals(PERM_NONE)?"' SELECTED>":"'>")
      + res.getString("str_perm_none"));
    //student-like permissions
    buf.append("<OPTION VALUE='" + PERM_STUDENT +
      (permissionLevel.equals(PERM_STUDENT)?"' SELECTED>":"'>")
      + res.getString("str_perm_student"));
    //conditional permission
    buf.append("<OPTION VALUE='" + PERM_CONDITIONAL + 
      (permissionLevel.equals(PERM_CONDITIONAL)?"' SELECTED>":"'>")
      + res.getString("str_perm_conditional"));
    //all/teacher-like permission
    buf.append("<OPTION VALUE='" + PERM_ALL +
      (permissionLevel.equals(PERM_ALL)?"' SELECTED>":"'>")
      + res.getString("str_perm_all"));
    buf.append("</SELECT>");
    return buf.toString();
  }
}
