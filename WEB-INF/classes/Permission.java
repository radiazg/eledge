/******************************************************************************
 * This object is the fundamental object for permissions, and is extended
 * by TAPermsions.java
 * The basic "job" of this object is to store/manipulate permission information
 * Author: Robert Zeigler
 * Started: 7/22/03
 * Last Modified: 8/1/03
 ******************************************************************************/
package Eledge;

import java.text.MessageFormat;
import java.util.Vector;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class Permission {
  private String name; //the "name" of this permission. Corresponds to the column name in TAPermissions table.
  private MessageFormat sql=null; //this is a sql string used to lookup "permission" information for a student/teacher/etc.
  private int numArgs=0; //number of arguments to supply to the sql string.
  private Vector args=null; //arguments to supply to sql string. These are actually Strings, and represent 
               //the name of form inputs to lookup.
  private String denyMsg=""; // this is the key for the resource bundle item containing
                             // the message to be displayed when permission to the request is denied.
  private String editMsg=""; // this is the Message displayed when a teacher goes to edit a TA's permissions.
                  // it explains what this permission is for/what it does/limits/etc.
                  // more accurately, this is the key for the resource bundle item containing this message.
  private String servlet=""; // servlet to which the permisison "belongs"
  private String request=""; // request to which this permission corresponds.
  private RBStore res = EledgeResources.getPermissionBundle();
  private Logger log = new Logger();
  private boolean requireTeacherStatus=false; //whether or not to temporarily
                              //set a student's status to instructor
                              //if have a given permission.
  public Permission() {
    this.name="";
  }

  public Permission(String n) {
      this.name = n;
  }

  public Permission(String n, String s) {
    this.name = n;
    this.sql= new MessageFormat(s);
  }

  public Permission(String n, String s, String[] a, String dM, String eM, String serv, String r, boolean setTeach) {
    this.name = n;
    this.sql = new MessageFormat(s);
    setArgs(a);
    this.numArgs=args.size();
    this.denyMsg=dM;
    this.editMsg=eM;
    this.servlet=serv;
    this.request=r;
    this.requireTeacherStatus=setTeach;
  }

  /*** Get Methods ***/
  public String getName() {
    return this.name;
  }

  public String getSqlAsString() {
    if (this.sql != null)
      return this.sql.toPattern();
    return "";
  }

  public MessageFormat getSql() {
    return this.sql;
  }

  public int getNumArgs() {
    return this.numArgs;
  }

  public String getArg(int i) {
    if (this.args == null)
      return "";
    return (String)this.args.elementAt(i);
  }

  public String[] getArgs() {
    if (this.args==null) {
      log.paranoid("Null Args!! Returning null!","Permission:getArgs");
      return null;
    }

    String[] ret = new String[this.args.size()];
    int i=0;
    for (Enumeration e = this.args.elements(); e.hasMoreElements(); i++ ) {
      ret[i]=(String)e.nextElement();
      log.paranoid("Added: " + ret[i] + " to return array","Permissions:getArgs");
    }
    return ret;
  }

  public String getDenyMsg() {
    try {
      return res.getString(this.denyMsg);
    } catch (Exception e) {
      return res.getString("str_resource_not_found") + ": " + this.denyMsg;
    }
  }

  public String getEditMsg() {
    try {
      return res.getString(this.editMsg);
    } catch (Exception e) {
      return res.getString("str_resource_not_found") + ": " + this.editMsg;
    }
  }
  
  public String getServlet() {
    return servlet;
  }

  public String getRequest() {
    return request;
  }

  public boolean getRequireTeacherStatus() {
    return requireTeacherStatus;
  }

  /*** Set Methods ***/
  public void setName(String n) {
    this.name = n;
  }

  public void setSql(String s) {
    if (this.sql==null)
      this.sql = new MessageFormat(s);
    else
      this.sql.applyPattern(s);
  }

  public void setEditMsg(String eM) {
    this.editMsg=eM;
  }

  public void setDenyMsg(String dM) {
    this.denyMsg=dM;
  }

  public void setServlet(String s) {
    this.servlet=s;
  }

  public void setRequest(String r) {
    this.request=r;
  }

  public void setArgs(String[] a) {
    if (this.args==null)
      this.args = new Vector();
    else
      this.args.clear();
    for (int i=0; i<a.length;i++) {
      log.paranoid("Adding: " + a[i] + " to argument vector for: " + this.name,"Permission:setArts");
      this.args.addElement(a[i]);
    }
    this.numArgs=args.size();
  }

  public void addArg(String a) {
    if (this.args == null)
      this.args = new Vector();
    this.args.addElement(a);
    this.numArgs=args.size();
  }

  public void addArg(String a, int i) {
    if (this.args == null)
      this.args = new Vector();
    this.args.add(i,a);
    this.numArgs=args.size();
  }

  public void setRequireTeacherStatus(boolean t) {
    this.requireTeacherStatus=t;
  }
}
