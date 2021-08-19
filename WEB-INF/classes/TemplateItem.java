package Eledge; 

/*************************************************************************
 * TemplateItem.java                                                     *
 * Started: 1/31/03                                                      *
 * Last Revision: 2/19/03
 * Purpose: The TemplateItem class defines the fields/members for a      *
 * given item within a template. Additionally, it defines the methods to *
 * access and manipulate those members.                                  *
 *************************************************************************/

import java.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.MessageFormat;

public class TemplateItem {
  private static RBStore res = EledgeResources.getTemplateItemBundle();
  static Logger log = new Logger();
  /***fields.***/
  private int index; //# in which it appears on the page; more appropriately,
                     //the order in which it appears in it's section. IE,
		     //if 1 and 3 are location top, and 2 and 4 are loc.
		     //bottom, then, order goes 1,3, content, 2,4.
  private int type;  //type: checkbox or input
  private String preText; //text, html code, etc. that goes before a page specific value
  private String postText;//text, html code, etc., that goes after a page specific value
  private String defaultValue;//default value to plug between preText and postText
  private String name;//name of this particular element.
  private int location;//location on the page: Top or bottom. Eventually, there will/may be a "between paragraphs" ability, as well.
  //constants for location.
  final static protected int LOC_TOP=1;
  final static protected int LOC_INTER_PARAGRAPH=2;//unused for now.
  final static protected int LOC_BOTTOM=3;
  //constants for type.
  final static protected int TYPE_CHECKBOX=1;
  final static protected int TYPE_INPUT=2;
  String mySQLDriver = Course.jdbcDriver;
  /*** Constructors ***/
  //no parameters.
  protected TemplateItem() {
    log.paranoid("Creating generic templateitem","TemplateItem:TemplateItem()");
    index=0;
    type=TYPE_INPUT;
    preText="<p>";
    postText="</p>";
    defaultValue=res.getString("str_default_text");
    name="NewTemplateItem";
    location=LOC_TOP;
  }
  //construct an item w/ the given name.
  protected TemplateItem(String newName) {
    log.paranoid("Constructing template with name: " + newName,"TemplateItem:TemplateItem(newName)");
    index=0;
    type=TYPE_INPUT;
    preText="";
    postText="";
    defaultValue="";
    name=CharHider.stripInvalidChars(newName);
    location=LOC_TOP;
  }
  //construct an item w/ the given name and type
  protected TemplateItem(String newName, int newType) {
    log.paranoid("Begin constructor","TemplateItem:TemplateItem(newName,newType)");
    index=0;
    type=newType;
    preText="";
    postText="";
    defaultValue="";
    name=CharHider.stripInvalidChars(newName);
    location=LOC_TOP;
  }
  //build an instance from a database record.
  protected TemplateItem(ResultSet rsItem) {
    log.paranoid("Begin constructor.","TemplateItem:TemplateItem(rsItem)");
    try {
      index=rsItem.getInt("ID");
      type=rsItem.getInt("Type");
      preText=rsItem.getString("PreText");
      postText=rsItem.getString("PostText");
      defaultValue=rsItem.getString("DefaultValue");
      name=CharHider.stripInvalidChars(rsItem.getString("Name"));
      location=rsItem.getInt("Location");
      if (location<LOC_TOP || location>LOC_BOTTOM) 
        location=LOC_TOP;
      if (type<TYPE_CHECKBOX || type > TYPE_INPUT)
        type=TYPE_INPUT;
    } catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"TemplateItem:TemplateItem(ResultSet)");
    }
  }
  //build an instance from a servlet request. This more or less works in tandem w/ the displayItemNew method.
  //essentially, another object could TemplateItem.displayItemNew(index), displaying it a web page.
  //the other object isn't going to know how to interpret the request, so it could do something like
  //TemplateItem newItem = new TemplateItem(request), allowing the item to fill in details.
  protected TemplateItem(HttpServletRequest request) {
    log.paranoid("Begin constructor","TemplateItem:TemplateItem(request");
    try{
      index = Integer.parseInt(request.getParameter("NewIndex"));
    } catch(Exception e) {
      log.normal("Caught exception parsing NewIndex.","TemplateItem:TemplateItem(request)");
      index = 0;
    }
    try {
      location = Integer.parseInt(request.getParameter("NewLocation"));
    } catch(Exception e) {
      log.normal("Caught exception parsing NewLocation.","TemplateItem:TemplateItem(request)");
      location=LOC_TOP;
    }
    try {
      type = Integer.parseInt(request.getParameter("NewType"));
    } catch(Exception e) {
      log.normal("Caught exception parsing NewType","TemplateItem:TemplateItem(request)");
      type=TYPE_INPUT;
    }
    name = CharHider.stripInvalidChars(request.getParameter("NewName"));
    preText = request.getParameter("NewPreText");
    postText = request.getParameter("NewPostText");
    defaultValue = request.getParameter("NewDefault");
    if (name == null || name.equals("")) 
      name="NewItem";
    if (preText == null || preText.equals(""))
      preText="";
    if (postText == null || postText.equals(""))
      postText="";
    if (defaultValue == null || defaultValue.equals("")) {
      if (type==TYPE_INPUT)
        defaultValue = "";
      else
        defaultValue = "false";
    }
  }

  /*** gets and sets ***/

  protected String getName() {
    if (this.name == null)
      return CharHider.stripInvalidChars(res.getString("str_noname"));
    else return this.name;
  }

  protected boolean setName(String newName) {
    log.paranoid("Setting name of " + this.name + " to " + newName,"TI:setName()");
    if (newName == null || newName.equals(""))
      return false;
    this.name=CharHider.stripInvalidChars(newName);
    return true;
  }


  protected String getPreText() {
    if (this.preText == null)
      return "";
    else return this.preText;
  }

  protected boolean setPreText(String newPreText) {
    if (newPreText == null )
      return false;
    this.preText = newPreText;
    return true;
  }

  protected String getDefaultValue() {
    if (this.defaultValue == null)
      return "";
    else return this.defaultValue;
  }

  protected boolean setDefaultValue(String newDefault) {
    if (newDefault == null )
      return false;
    this.defaultValue=newDefault;
    return true;
  }

  protected String getPostText() {
    if (this.postText == null)
      return "";
    else return this.postText;
  }

  protected boolean setPostText(String newPostText) {
    if (newPostText==null) 
      return false;
    this.postText = newPostText;
    return true;
 }

  protected int getIndex() {
      return this.index;
  }

  protected boolean setIndex(int newIndex) {
    if (newIndex<0)
      return false;
    this.index = newIndex;
    return true;
  }

  protected boolean setType(int newType) {
    if (newType < TYPE_CHECKBOX || newType > TYPE_INPUT)
      return false;
    this.type = newType;
    return true;
  }

  protected boolean isCheckbox() {
    return (this.type == TYPE_CHECKBOX?true:false);
  }

  //this method, as well as the isAtLocation
  //are methods designed to allow other objects to use template items
  //(eg. get and set, etc.) and manipulate them appropriately, even if they
  //haven't actually loaded a new item, while still maintaining overall 
  //"ignorance" as to the internal workings of the templateitems.
  static protected boolean isCheckbox(int itemType) {
    return (itemType==TYPE_CHECKBOX?true:false);
  }

  protected boolean isInput() {
    return (this.type == TYPE_INPUT?true:false);
  }

  protected boolean setLocation(int newLocation) {
    if (newLocation < LOC_TOP || newLocation > LOC_BOTTOM)
      return false;
    location = newLocation;
    return true;
  }

  protected int getLocation() {
    return this.location;
  }

  protected boolean isAtTop() {
    if (this.location==LOC_TOP)
      return true;
    else return false;
  }

  protected boolean isAtBottom() {
    if (this.location==LOC_BOTTOM)
      return true;
    else return false;
  }

  protected boolean isAtLocation(int itemLocation) {
    if (this.location == itemLocation)
      return true;
    else return false;
  }

  protected int getBottom() {
    return LOC_BOTTOM;
  }

  protected int getInterParagraph() {
    return LOC_INTER_PARAGRAPH;
  }

  protected int getTop() {
    return LOC_TOP;
  }

  protected boolean isInterParagraph() {
    if (this.location==LOC_INTER_PARAGRAPH) 
      return true;
    else return false;
  }

  /*** Meat and potatoes methods ***/

  //This spits out one row of a table, displaying the various variables of the object.

  protected String displayItemEdit() {
    log.paranoid("Begin method.","TI:displayItemEdit()");
    StringBuffer buf = new StringBuffer("");
    buf.append("\n<TR><TD><INPUT TYPE=RADIO NAME='Selected' VALUE='" + this.index + "'><TD><INPUT TYPE=TEXT NAME='Index" + this.index + "' VALUE='" 
      + this.index + "' SIZE=3></TD>\n");
    if (this.name.equals("PrinterFriendlyLink")) {
      log.paranoid("Item was PFL.","displayItemEdit");
      buf.append("<TD COLSPAN=2><INPUT TYPE=HIDDEN NAME='Name" + this.index
      + "' VALUE='PrinterFriendlyLink'>" 
      + res.getString("str_pfl") + "</TD>"
      + "<TD>" + displayLocations(true) + "</TD><TD COLSPAN=3>&nbsp;</TD></TR>");
      return buf.toString();
    }
    log.paranoid("Displaying: " + this.name + " for editing.","TI:displayItemEdit()");
    buf.append("<TD><INPUT TYPE=HIDDEN NAME='Name" + this.index 
      + "' VALUE='" + this.name + "'>" + this.name + "</TD>\n<TD>" + displayTypes() + "</TD>\n<TD>" 
      + displayLocations(false) + "</TD>\n<TD><INPUT TYPE=TEXT SIZE=10 NAME='PreText" + this.index 
      + "' VALUE='" + this.preText + "'></TD>\n<TD>" + displayDefault() + "</TD>"
      + "\n<TD><INPUT TYPE=TEXT SIZE=10 NAME='PostText" + this.index + "' VALUE='" 
      + this.postText + "'></TD></TR>");
    return buf.toString();
  }

  //This spits out one row of a table, displaying a set of fields for a blank item. . . ie a new item.
  //static for easy reference; since it doesn't know how many other items exist, something else needs
  //to tell it.
  static protected String displayItemNew(int newIndex) {
    StringBuffer buf = new StringBuffer("");
    buf.append("<TR><TD COLSPAN=2 ALIGN=RIGHT><INPUT TYPE=TEXT NAME='NewIndex' VALUE='" + newIndex 
      + "' SIZE=3></TD>\n<TD><INPUT TYPE=TEXT NAME='NewName"  
      + "' SIZE=10></TD>\n<TD>" + displayTypesNew() + "</TD>\n<TD>" 
      + displayLocationsNew() + "</TD>\n<TD><INPUT TYPE=TEXT SIZE=10 NAME='NewPreText'"
      + "></TD>\n<TD><INPUT TYPE=TEXT NAME='NewDefault' SIZE=10></TD>"
      + "\n<TD><INPUT TYPE=TEXT NAME='NewPostText' SIZE=10></TD></TR>");
    return buf.toString();
  }
//sets up a a select for the item types, and sets this item's type to selected.
  protected String displayTypes() {
    StringBuffer buf = new StringBuffer("");
    buf.append("<SELECT NAME='Type" + this.index + "'><OPTION VALUE='" 
    + Integer.toString(TYPE_CHECKBOX) + (this.type==TYPE_CHECKBOX?"' SELECTED>":"'>")
    + res.getString("str_type_cb")
    + "\n<OPTION VALUE='" + Integer.toString(TYPE_INPUT) 
    + (this.type==TYPE_INPUT?"' SELECTED>":"'>") 
    + res.getString("str_type_input") + "</SELECT>");
    return buf.toString();
  }
//sets up a select for item types, with no "selected" value.
  static protected String displayTypesNew() {
    StringBuffer buf = new StringBuffer("");
    buf.append("<SELECT NAME='NewType'><OPTION VALUE='" 
    + Integer.toString(TYPE_CHECKBOX) + "'>" + res.getString("str_type_cb") 
    + "\n<OPTION VALUE='" + Integer.toString(TYPE_INPUT) + "'>" 
    + res.getString("str_type_input") + "</SELECT>");
    return buf.toString();
  }

  static protected int extractTypeNew(HttpServletRequest request) {
    int ret;
    try {
      ret = Integer.parseInt(request.getParameter("NewType"));
    } catch (Exception e) {
      ret = TYPE_INPUT;
    }
    return ret;
  }

  protected String displayLocations(boolean isPF) {
    StringBuffer buf = new StringBuffer("");
    buf.append("<SELECT NAME='Location" + this.index + "'>"
    //top.... (followed by middle... and lastly bottom...
    + "<OPTION VALUE='" + Integer.toString(LOC_TOP) 
    + (this.location==LOC_TOP?"' SELECTED>":"'>") + res.getString("str_loc_top")
    + "</OPTION>" + (isPF?"":("<OPTION VALUE='" + LOC_INTER_PARAGRAPH 
      + (this.location==LOC_INTER_PARAGRAPH?"' SELECTED>":"'>") 
      + res.getString("str_loc_inter")))
    //bottom...
    + "<OPTION VALUE='" + Integer.toString(LOC_BOTTOM) 
    + (this.location==LOC_BOTTOM?"' SELECTED>":"'>") 
    + res.getString("str_loc_bot") + "</SELECT>");
    return buf.toString();
  }

  static protected String displayLocationsNew() {
    StringBuffer buf = new StringBuffer("");
    buf.append("<SELECT NAME='NewLocation'><OPTION VALUE='" 
    + Integer.toString(LOC_TOP) + "'>" + res.getString("str_loc_top")
    + "<OPTION VALUE='" + LOC_INTER_PARAGRAPH + "'>" 
    + res.getString("str_loc_inter") + "<OPTION VALUE='" 
    + Integer.toString(LOC_BOTTOM) + "'>" 
    + res.getString("str_loc_bot") + "</SELECT>");
    return buf.toString();
  }

  static protected int extractLocationNew(HttpServletRequest request) {
    int ret;
    try {
      ret = Integer.parseInt(request.getParameter("NewLocation"));
    } catch (Exception e) {
      ret = LOC_TOP;
    }
    return ret;
  }

  protected String displayDefault() {
    if (this.type==TYPE_CHECKBOX) {
      return "<DIV ALIGN=CENTER><INPUT NAME='Default" + this.index + "' TYPE=CHECKBOX"
      + " VALUE=true" + (this.defaultValue.equals("true")?" CHECKED>":">")
      + "</DIV>";
    }
    else
      return "<INPUT NAME='Default" + this.index + "' TYPE=INPUT VALUE='"
      + this.defaultValue + "' SIZE=10>";
  }

  static protected String displayDefaultNew(int itemType) {
    if (itemType==TYPE_CHECKBOX) {
      return "<DIV ALIGN=CENTER><INPUT NAME='NewDefault' TYPE=CHECKBOX "
      + "VALUE=true></DIV>";
    }
    else
      return "<INPUT NAME='NewDefault' TYPE=TEXT>";
  } 
  static protected String extractDefaultNew(HttpServletRequest request) {
    return request.getParameter("NewDefault");
  }

/*  protected boolean updateItem(boolean isSafe) {
    String sqlUpdateString="";
    try {
      if (!isSafe)
        return this.updateItem();
      }
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      sqlUpdateString = "U
    } catch(Exception e) {
      log.sparse("Caught Exception: " + e.getMessage(),"TemplateItem:updateItem(boolean)");
      return false;
    }
    return true;
  }
*/
  //this method makes the assumption that all the values have been set previously, and we're now updating the item.
  synchronized protected String updateIndex(HttpServletRequest request,int limit) {
    int newIndex;
    MessageFormat mf = new MessageFormat(res.getString("str_nochange_index"));
    Object[] args = new Object[2];
    try {
      newIndex = Integer.parseInt(request.getParameter("Index" + Integer.toString(this.index)));
    } catch (Exception e) {
      log.sparse("Caught exception parsing for index for item " + name + " index of " + this.index,"TI:updateIndex(request)");
      return res.getString("str_unable_perform");
    }
    if (newIndex >= limit) {
      args[0] = new Integer(limit);
      args[1] = new Integer((limit-1));
      return mf.format(args);
    }
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //check to see if an item already occupies this position . . .
      ResultSet rs = stmt.executeQuery(("SELECT * FROM TemplateItems WHERE ID=" + newIndex));
      if (rs.next()) {
        if (this.index<newIndex) {
          stmt.executeUpdate("UPDATE TemplateItems SET ID=(ID-1) WHERE ID>" + this.index + " AND ID<=" + newIndex); 
        } else {
          stmt.executeUpdate("UPDATE TemplateItems SET ID=(ID+1) WHERE ID>=" + newIndex + " AND ID<" + this.index);
        }
      }
      stmt.executeUpdate("UPDATE TemplateItems SET ID=" + newIndex + " WHERE Name='" + this.name + "'"); 
      this.index=newIndex;
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"TI:updateIndex(request)");
      return e.getMessage();
    }

    return "";
  }

  synchronized protected boolean updateItem() {
    String sqlUpdateString="";
    log.paranoid("Begin method.","TemplateItem:updateItem()");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet exists = stmt.executeQuery("SELECT * FROM TemplateItems WHERE "
      + "Name='" + this.name + "'");//after initial creation, the name cannot be changed . . . too many things depend on its remaining constant . . . like this sql statement. ;) (more importantly, like the column names in the TemplatePages table.
      log.paranoid("Name: " + this.name,"TemplateItem:updateItem()");
      if (!exists.next()) {
	log.paranoid(this.name + "didn't exist yet, so, inserting instead of updating.","TI:updateItem()");
        return insertItem();
      }
      log.paranoid(this.name + " existed.","TemplateItem:updateItem()");
      exists.close();
      
      sqlUpdateString="UPDATE TemplateItems SET Name='" + this.name + "', "
      + "Type='" + this.type + "', Location='" + this.location + "', PreText='"
      + CharHider.squot2dquot(this.preText) + "', PostText='" + CharHider.squot2dquot(this.postText) + "', DefaultValue='"
      + CharHider.squot2dquot(this.defaultValue) + "' WHERE ID='" + this.index + "'";
      
      log.paranoid("About to execute: " + sqlUpdateString,"TemplateItem:updateItem()");

      stmt.executeUpdate(sqlUpdateString);
    } catch(Exception e) {
      if (createTable())
        return updateItem();
      log.sparse("Caught exception " + e.getMessage() + ";update: " + sqlUpdateString,"TemplateItem:updateItem()");
      return false;
    }
    return true;
  }

  synchronized private boolean updateItem(String newName, int newType, 
    int newLocation, String newDefault, String newPreText, String newPostText) {

    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();

      String sqlUpdateString = "UPDATE TemplateItems SET Name='" + newName 
      + "', Type='" + newType + "', Location='" + newLocation + "',"
      + "PreText='" + CharHider.squot2dquot(newPreText) + "', DefaultValue='"
      + CharHider.squot2dquot(newDefault) + "', PostText='"
      + CharHider.squot2dquot(newPostText) + "' WHERE ID='" + this.index + "'";
      log.paranoid("About to execute: " + sqlUpdateString,"TemplateItem:updateItem(String,...)"); 
      stmt.executeUpdate(sqlUpdateString);
      log.paranoid("resetting this item.","TI:updateItem(String,...");
      this.name=newName;
      this.type=newType;
      this.preText=newPreText;
      this.postText=newPostText;
      this.defaultValue=newDefault;
      this.location=newLocation;
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TemplateItem(String,...):updateItem");
      return false;
    }
    return true;
  }

  protected boolean updateItem(HttpServletRequest request) {
    log.paranoid("Begin method","TI:updateItem(request)");
    int newType, newLocation;
    String newPreText, newPostText, newDefault, newName;

    newName = request.getParameter("Name" + Integer.toString(this.index));

    try {
      newLocation = Integer.parseInt(request.getParameter("Location" + Integer.toString(this.index)));
    } catch (Exception e) {
      log.normal("Caught exception while parsing for Location" + Integer.toString(this.index),"TemplateItem:updateItem(request)");
      newLocation = this.location;
    }
   //these are the only updatable things for printerfriendly link. 
    if (newName.equals("PrinterFriendlyLink")) {
      log.paranoid("Name was PFL.","TI:updateItem(request)");
      this.location = newLocation;
      return updateItem();
    }

    try {
      newType = Integer.parseInt(request.getParameter("Type" + Integer.toString(this.index)));
    } catch (Exception e) {
      log.normal("Caught exception while parsing for Type" + Integer.toString(this.index),"TemplateItem:updateItem(request)");
      newType = this.type;
    }

    newPreText = request.getParameter("PreText" + Integer.toString(this.index));
    newPostText = request.getParameter("PostText" + Integer.toString(this.index));
    newDefault = request.getParameter("Default" + Integer.toString(this.index));
    if (newPreText == null)
      newPreText = this.preText;
    if (newPostText == null)
      newPostText = this.postText;
    if (newName == null || newName.equals(""))
      newName = this.name;
    if (newDefault == null) {
      if (this.type == TYPE_CHECKBOX) 
        newDefault = "false";
      else newDefault = this.defaultValue;
    } 
    return updateItem(newName, newType, newLocation, newDefault, newPreText, newPostText);
  }

  synchronized protected boolean insertItem() {
    log.paranoid("Begin Method","TI:insertItem()");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM TemplateItems");
      rsCount.next();
      int count = rsCount.getInt("COUNT(*)");
      rsCount.close();
      stmt.executeUpdate("UPDATE TemplateItems SET ID=ID+1 WHERE ID>=" + this.index + " AND ID<" + count);
      String sqlQueryString = "INSERT INTO TemplateItems (ID, Name, Type, "
      + "Location, PreText, DefaultValue, PostText) VALUES('" + (this.index<=count?this.index:count)
      + "','" + this.name + "','" + this.type + "','" + this.location + "','"
      + CharHider.squot2dquot(this.preText) + "','" + CharHider.squot2dquot(this.defaultValue) + "','" + CharHider.squot2dquot(this.postText) + "')";
      log.paranoid("Executing: " + sqlQueryString,"TI:insertItem()");
      stmt.executeUpdate(sqlQueryString); 
      stmt.close();
      conn.close();
    } catch(Exception e) {
      if (createTable()) 
        return insertItem();
      log.sparse("Caught exception " + e.getMessage(),"TemplateItem:insertItem");
      return false;
    }
    return true;
  }

  static boolean createTable() {
    log.paranoid("Begin Method.","TI:createTable()");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Creating table.","TI:createTable()");
      stmt.executeUpdate("Create Table TemplateItems (ID INT, Name VARCHAR(50), Type INT DEFAULT 2, Location INT DEFAULT 1, PreText TEXT, PostText TEXT, DefaultValue TEXT)");
      //the printer friendly link is a special case due to the fact that
      //it takes special hard code to make it run. So, we'll insert it here.
      //and test for the name elsewhere.
      log.paranoid("Inserting PFL into TemplateItems.","TI:createTable()");
      stmt.executeUpdate("INSERT INTO TemplateItems VALUES (0,'PrinterFriendlyLink',1,3,'','','false')");
      stmt.close();
      conn.close();
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"TemplateItem:createTable()");
      return false;
    }
    return true;
  }

  public boolean exists() {
    boolean ret = false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection c=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement s = c.createStatement();
      ResultSet rs = s.executeQuery("SELECT * FROM TemplateItems WHERE Name='"
        + this.name + "'");
      ret = rs.next();
      rs.close();
      s.close();
      c.close();
    } catch (Exception e) {
      log.sparse("Caugght: " + e.getMessage(),"TemplateItem:exists");
      ret=false;
    }
    return ret;
  }
}

