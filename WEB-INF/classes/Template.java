package Eledge;

/*******************************************************************************
 * Name: Template.java                                                         *
 * Purpose: This object sets up and controls the system wide, shared "template"*
 * framework; any objects that require manipulation or use of template items   *
 * will really do so through this class; main component is a static vector of  *
 * template items; most methods act on that vector, rather than on individual  *
 * Items, as most item work is handled by the TemplateItems themselves         *
 * Started: 2/7/03                                                             *
 * Last Revised: 2/14/03                                                       *
 * Author: Robert Zeigler (robertz@scazdl.org)                                 *
 *******************************************************************************/

import java.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Template {

  private static Vector templateItems = new Vector();
  String mySQLDriver = Course.jdbcDriver;
  private static boolean loaded=false;
  Logger log = new Logger();
  private RBStore res = EledgeResources.getTemplateBundle();
  public Template() {
      log.paranoid("Beginning constructor.","Template:Template()");
    if (!this.loaded) {
      log.paranoid("Template not loaded yet. Loading now.","Template:Template()");
      try {
        Class.forName(mySQLDriver).newInstance();
	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
	Statement stmt = conn.createStatement();
	ResultSet rsItems = stmt.executeQuery("SELECT * FROM TemplateItems ORDER BY ID");
	while (rsItems.next()) {
		TemplateItem item = new TemplateItem(rsItems);
		templateItems.add(item);
	}
	rsItems.close();
        this.loaded=true;
      } catch (Exception e) {
        log.sparse("Caught exception: " + e.getMessage() + ". Trying TemplateItem.createTable()","Template:Template()");
	addTemplatePageTable();
	if (TemplateItem.createTable()) {
	  templateItems.clear();//"reset" vector if a problem occurred.
	  reloadTemplateVector();
	  this.loaded=true;
	  return;
	}
	this.loaded=false;
      }
    }
  }

  synchronized public boolean addItem(TemplateItem newItem) {
    log.paranoid("Begin method.","Template:addItem");
    if (newItem.exists()) {
      return false;
    }
    for (int i = 0; i < templateItems.size(); i++) {
      log.paranoid("In for loop.","Template:addItem");
      TemplateItem t = (TemplateItem)this.templateItems.get(i);
      if (t.getIndex() >= newItem.getIndex()) {
	log.paranoid("incrementing index of t.","Template:addItem");
        t.setIndex(t.getIndex()+1);
	this.templateItems.setElementAt(t,i);
      }
    }
    this.templateItems.add(newItem.getIndex(),newItem);
    log.paranoid("Normal termination of method.","Template:addItem");
    refreshItemTable();
    return updateTemplatePageTable(newItem);
  }
  
  synchronized protected boolean addNew(HttpServletRequest request) {
    log.paranoid("Beginning Method.","Template:addNew(request)");
    //ensure that at least something will get loaded for this new item.
    boolean ret = false;
    if (request.getParameter("NewName")==null || request.getParameter("NewName").equals("")) 
      return false;
    log.paranoid("Newname wasn't null, nor was it an empty string.","Template:addNew(request");
    TemplateItem item = new TemplateItem(request);
    //essentially, in this case, can just run insertItem, then reload the
    //vector from the database.
    if (item.exists()) {
      return false;
    }
    item.insertItem();
    reloadTemplateVector();
    log.paranoid("Normal termination of method.","Template:addNew(request)");
    return updateTemplatePageTable(item);
  }
  //resets the templateItems vector from database information.
  //good for when the database has been updated by some other method, and the
  //vector hasn't been reset appropriately. (eg: addNew(HttpServletRequest)
  synchronized private boolean reloadTemplateVector() {
    log.paranoid("Begin method.","Template:reloadTemplateVector()");
    Vector temp = new Vector();
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsItems = stmt.executeQuery("SELECT * from TemplateItems ORDER BY ID");
      while (rsItems.next()) {
	log.paranoid("Loading next item.","Template:reloadTemplateVector()");
        TemplateItem item = new TemplateItem(rsItems);
        temp.add(item);
      }
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:reloadTemplateVector()");
      return false;
    }
    log.paranoid("Removing all elements.","Template:reloadTemplateVector()");
    templateItems.removeAllElements();
    log.paranoid("Adding all elements from collection/vector temp.","Template:reloadTemplateVector()");
    templateItems.addAll(temp);
    return true;
  }
  //completely rebuilds the entries in the TemplateItems table
  //rather dangerous method in that it completely wipes the templateitems
  //table clean; hence, it's private. 
  synchronized private boolean refreshItemTable() {
    log.paranoid("Begin Method.","Template:refreshItemTable()");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      //clear all existing records.
      stmt.executeUpdate("DELETE FROM TemplateItems");
      stmt.close();
      conn.close();
      log.paranoid("Begin for loop...","Tempalte:refreshItemTable()");
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	log.paranoid("Inserting item: " + t.getName() + " into db.","Template:refreshItemTable()");
        t.insertItem();
      }
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:refreshItemTable()");
      return false;
    }
    return true;
  }
//print out a table of the current items, plus a space for a new item.
//Assumes that the html element <FORM> has already been declared by the 
//calling method.
  synchronized protected String displayItems() {
    StringBuffer buf = new StringBuffer("<TABLE BORDER=1 CELLSPACING=0>");
    int i = 0;
    buf.append("<TR><TD>&nbsp;</TD><TH>" + res.getString("str_field_index")
    + "</TH><TH>" + res.getString("str_field_name") + "</TD><TH>"
    + res.getString("str_field_type") + "</TH><TH>"
    + res.getString("str_field_location") + "</TH><TH>"
    + res.getString("str_field_pretext") + "</TH><TH>"
    + res.getString("str_field_default") + "</TH><TH>"
    + res.getString("str_field_posttext") + "</TH>");
    for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
      TemplateItem t = (TemplateItem)e.nextElement();
      buf.append(t.displayItemEdit());
      i++;
    }
    buf.append(TemplateItem.displayItemNew(i)); 
    buf.append("</TABLE>");
    return buf.toString();
  }

 synchronized protected String displaySectionIndex(int location) {
   StringBuffer buf = new StringBuffer();
   buf.append("\n<SELECT NAME='Index'>");
   int i = 0;
   for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
     TemplateItem t = (TemplateItem)e.nextElement();
     if (t.isAtLocation(location)) {
       i=(t.getIndex() + 1);
       buf.append("<OPTION>" + t.getIndex());
     }
   }
   buf.append("<OPTION VALUE='" + i + "'>" + res.getString("str_end"));
   buf.append("</SELECT>");
   return buf.toString();
 }
//Hm. we're going to need to check here if the item type has been changed...
//to/from inter. Hrm. That sucks.
 synchronized protected void updateItems(HttpServletRequest request) {
    log.paranoid("Begin method.","Template:updateItems");
    String sqlDrop="";
    String sqlAdd="";
    String sqlUpdate="";
    for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
      TemplateItem t = (TemplateItem)e.nextElement();
      int oldLoc = t.getLocation();
      boolean execute=false;
      log.paranoid("Updating item: " + t.getName(),"Template:updateItems");
      t.updateItem(request);
      if (t.getLocation() != oldLoc) {
        //uh-oh, they changed tbe location. So, now what?
        //The idea here is that if the -old- location was interparagraph, 
        //and, as we've already checked, the new and old don't match, then,
        //the new location must be something that needs to be added to the
        //TemplatePages table. Otherwise, the change was to go -from- the 
        //TemplatePages table, -to- the TemplateSections table. Hm.
        //This could get sticky. Ah well. ;)

        if (oldLoc==t.getInterParagraph()) {
          execute=true;
          sqlDrop = "AlTER TABLE TemplateSections DROP `" + t.getName() + "`";
          sqlAdd = "ALTER TABLE TemplatePages ADD(`" + t.getName() + "` TEXT)";
          sqlUpdate = "UPDATE TemplatePages SET " + t.getName() + "='"
            + t.getDefaultValue() + "'";
        } else if (t.getLocation() == t.getInterParagraph()) {//new loc is inter
          execute=true;
          sqlDrop = "ALTER TABLE TemplatePages DROP `" + t.getName() + "`";
          sqlAdd = "ALTER TABLE TemplateSections ADD(`" + t.getName() + "` TEXT)";
          sqlUpdate = "UPDATE TemplateSections SET " + t.getName() + "='" 
            + t.getDefaultValue() + "'";
        }
        if (execute) {
          execute(sqlDrop);
          execute(sqlAdd);
          execute(sqlUpdate);
        }
      }
    }
    //call addNew to check to see if a new element was added or not.
    addNew(request);
    reloadTemplateVector();
    return;
  }

  synchronized protected boolean updateTemplatePageTable(TemplateItem item) {
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      if (item.isInterParagraph()) {
        stmt.executeUpdate("ALTER TABLE TemplateSections ADD(`" + CharHider.stripInvalidChars(item.getName()) + "` TEXT)");
        stmt.executeUpdate("UPDATE TemplateSections SET`" + CharHider.stripInvalidChars(item.getName()) + "`='" + item.getDefaultValue() + "'");
      } else {
        stmt.executeUpdate("ALTER TABLE TemplatePages ADD (`" + CharHider.stripInvalidChars(item.getName()) + "` TEXT)");
        stmt.executeUpdate("UPDATE TemplatePages SET `" + CharHider.stripInvalidChars(item.getName()) + "`='" + item.getDefaultValue() + "'");
 stmt.close();
      }
      conn.close();
    } catch (Exception e) {
      log.normal("Caught an exception adding " + item.getName() + "; trying to add table...","Template:updateTemplatePageTable(item)");

      if (addTemplatePageTable() || addTemplateSectionTable()) //try the template page table addition first... if successful, good, everything is good. If not successfully, -probably- failed on the create table TemplatePages portion, which means that the TempaltePages table exists... so... try addTemplateSectionTable by itself... if -that's- false, too, then... problem. ;)
        return updateTemplatePageTable(item);
      else
        log.sparse("Uh-oh: real excetion here: " + e.getMessage(),"updateTemplatePageTable");
      return false;
    }
    return true;
  }

  synchronized private boolean addTemplatePageTable() {
    log.paranoid("Begin method.","Template:addTemplatePageTable");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE TemplatePages (PageID TEXT, PrinterFriendlyLink VARCHAR(5) DEFAULT 'false')");
      stmt.executeUpdate("CREATE TABLE TemplateSections (PageID TEXT, Section int)");

      stmt.close();
      conn.close();
    } catch(Exception e) {
      log.normal("Caught exception: " + e.getMessage(),"Template:addTemplatePageTable");
      return false;
    }
    return true;
  }

  //used to delete an item when passed the index.
  synchronized protected boolean deleteItem(int delIndex) {
    boolean ret=false;
    log.paranoid("Begin method.","Tempalte:deleteItem");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      TemplateItem t = (TemplateItem)templateItems.get(delIndex);
      if (t.getName().equals("PrinterFriendlyLink")) //no deleting this field! 
        return false;
      log.paranoid("Name wasn't pfl but: " + t.getName() + ";deleting it.","Template:deleteItem(delIndex)");
      stmt.executeUpdate("DELETE FROM TemplateItems WHERE ID='" + delIndex + "'");
      //no check here for reordering, because if they're going to reorder,
      //it's either already been done, or it will be done later . . .
      log.paranoid("Dropping table column: " + t.getName(),"Template:deleteItem(delIndex)");
      if (t.isInterParagraph()) {
        stmt.executeUpdate("ALTER TABLE TemplateSections DROP COLUMN `" + t.getName() + "`");
      } else {
        stmt.executeUpdate("ALTER TABLE TemplatePages DROP COLUMN " + t.getName());
      }
      //fix vector elements "Index"
      log.paranoid("Attempting to fix static vector  . . . ","Template:deleteItem(delIndex)");
      for (int i=0; i<templateItems.size(); i++) {
        t = (TemplateItem)templateItems.get(i);
	if (t.getIndex() > delIndex) {
	  log.paranoid("index > delIndex for " + t.getName() + "; decrementing index.","deleteItem(delIndex)");
	  t.setIndex(i-1);
	  templateItems.setElementAt(t,i);
	}
      }
      //remove the errant TemplateItem. ;)
      templateItems.removeElementAt(delIndex);
      ret = refreshItemTable();
    } catch(Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:deleteItem(int)");
      return false;
    }
    return ret;
  }

  //used to delete an item when passed the item itself.
  synchronized protected boolean deleteItem(TemplateItem item) {
    return deleteItem(item.getIndex());
  }

  //this is to delete an item when passed an HttpServletRequest.
  //hm. I'm going to have to think about how this one is done . . .  
  synchronized protected String deleteItem(HttpServletRequest request) {
    log.paranoid("Begin method.","Template:deleteItem(request)");
    int delIndex=-1;
    try {
      delIndex = Integer.parseInt(request.getParameter("Selected")); 
    } catch (Exception e) {
      return res.getString("str_no_delete");
    }
    log.paranoid("About to return deleteItem(delIndex) for index: " + delIndex,"Template:deleteItem(request)"); 
    return (delIndex==-1?(res.getString("str_not_deleted")):(deleteItem(delIndex)?res.getString("str_deleted"):res.getString("str_not_deleted")));
  }
  
  //use this to append the appropriate, page specific + template
  //information to a page being displayed. (for items with
  //location=LOC_TOP
  synchronized protected String appendTopItems(String pageID) {
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsValues = stmt.executeQuery("SELECT * FROM TemplatePages WHERE PageID='" + pageID + "'");
      if (!rsValues.next()) {
        rsValues.close();
        stmt.close();
        conn.close();
        return "";
      }
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isAtTop()) {
	  if (t.isCheckbox()) {
	    if (rsValues.getBoolean(t.getName())) {
              if (t.getName().equals("PrinterFriendlyLink"))
                buf.append(printerFriendlyLink(pageID));
              else
	        buf.append(t.getPreText() + t.getPostText());
	    }
	  }
	  else {
	    if (!rsValues.getString(t.getName()).equals(""))
	      buf.append(t.getPreText() + rsValues.getString(t.getName()) + t.getPostText());
	  }
	}
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      if (addTemplatePageTable())
        return appendTopItems(pageID);
      log.sparse("Caught exception: " + e.getMessage(),"Template:appendTopItems");
      return "";
    }
    return buf.toString();
  }

  //use this to append the appropriate, page specific + template
  //information to a page being displayed. (for items with
  //location=LOC_BOTTOM
  synchronized protected String appendBotItems(String pageID) {
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsValues = stmt.executeQuery("SELECT * FROM TemplatePages WHERE PageID='" + pageID + "'");
      if (!rsValues.next()) {
        rsValues.close();
        stmt.close();
        conn.close();
        return "";
      }
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isAtBottom()) {
	  if (t.getName().equals("PrinterFriendlyLink") && rsValues.getBoolean(t.getName())) {
	    buf.append(printerFriendlyLink(pageID));
	  }
	  else if (t.isCheckbox()) {
	    if (rsValues.getBoolean(t.getName())) {
	      buf.append(t.getPreText() + t.getPostText());
	    }
	  }
	  else {
	    if (!rsValues.getString(t.getName()).equals(""))//blank value means don't use...
	      buf.append(t.getPreText() + rsValues.getString(t.getName()) + t.getPostText());
	  }
	}
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      if (addTemplatePageTable()) 
        return appendBotItems(pageID);
      log.sparse("Caught exception: " + e.getMessage(),"Template:appendBotItems");
      return "";
    }
    return buf.toString();
  }

//use this to append the items with LOC_TOP to the
//top of a page being edited. This method assumes that it is being called in such a way
//that the form elements will be placed within a valid form...
  synchronized protected String appendTopItemsEdit(String pageID) {
    boolean hasRecord=false;
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsValues = stmt.executeQuery("SELECT * FROM TemplatePages WHERE PageID='" + pageID + "'");
      if (rsValues.next()) 
        hasRecord=true; 
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isAtTop()) {
	  if (t.isCheckbox()) {
	    buf.append("<br><INPUT TYPE=CHECKBOX NAME='" + t.getName() + "' VALUE=true"
	    + (hasRecord?(rsValues.getBoolean(t.getName())?" CHECKED":""):(t.getDefaultValue().equals("true")?" CHECKED":""))
	    + ">&nbsp;" + t.getName());
	  } else {
	    buf.append("<br>" + t.getName() + ":&nbsp;<INPUT TYPE=TEXT NAME='" + t.getName() 
	    + "' VALUE='" + (hasRecord?rsValues.getString(t.getName()):t.getDefaultValue()) 
	    + "'>&nbsp;" + res.getString("str_leave_blank"));
	  }
	}
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:appendTopItemsEdit");
      return "";
    }
    return buf.toString();
  }

//use this to append the items with LOC_BOTTOM to the 
//bottom of a page being edited.
  synchronized protected String appendBotItemsEdit(String pageID) {
    boolean hasRecord=false;
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsValues = stmt.executeQuery("SELECT * FROM TemplatePages WHERE PageID='" + pageID + "'");
      if (rsValues.next()) 
        hasRecord=true; 
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isAtBottom()) {
	  if (t.isCheckbox()) {
	    buf.append("<br><INPUT TYPE=CHECKBOX NAME='" + t.getName() + "' VALUE=true"
	    + (hasRecord?(rsValues.getBoolean(t.getName())?" CHECKED":""):(t.getDefaultValue().equals("true")?" CHECKED":""))
	    + ">&nbsp;" + t.getName());
	  } else {
	    buf.append("<br>" + t.getName() + ":&nbsp;<INPUT TYPE=TEXT NAME='" + t.getName() 
	    + "' VALUE='" + (hasRecord?rsValues.getString(t.getName()):t.getDefaultValue()) +"'>");
	  }
	}
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:appendBotItemsEdit");
      return "";
    }
    return buf.toString();
  }

//this method will save the various page specific information in 
//the TemplatePages table. Each page has one record.
  synchronized protected boolean savePageInformation(HttpServletRequest request, String pageID, int nSections) {
    String sqlUpdateString="";
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM TemplatePages WHERE PageID='" + pageID + "'"; 
      ResultSet rsPage = stmt.executeQuery(sqlQueryString);
      if (!rsPage.next()) { //no record for this page yet . . .
	 stmt.executeUpdate("INSERT INTO TemplatePages (PageID) VALUES ('" + pageID + "')");
	 rsPage = stmt.executeQuery(sqlQueryString);
	 if (!rsPage.next()) {//something went wrong, bail out . . . 
	   return false;
	 }
      }
      rsPage.close();
      //ok, we find yet another place for modification. We're going to need
      //to loop through the page sections for the interparagraph crap. *sigh*
      for (Enumeration e=templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	log.paranoid("Attempting updateString for " + t.getName() + " on page: "+ pageID,"Template:savePageInformation");
        if (t.isInterParagraph()) {
          //loop through all of the paragraphs...
          for (int i=0;i<nSections; i++) {
            String query = "SELECT * FROM TemplateSections WHERE PageID='"
              + pageID + "' AND Section='" + i + "'";
            ResultSet rs = query(query);
            if (!rs.next()) {
              execute("INSERT INTO TemplateSections (PageID, Section) VALUES('"
                  + pageID + "','" + i + "')");
            }
            rs.close();
            execute("UPDATE TemplateSections SET " + t.getName() + "='" 
                + request.getParameter((t.getName() + i)) + "' WHERE PageID='"
                + pageID + "' AND Section='" + i + "'");
          }
        } else {
	  sqlUpdateString="UPDATE TemplatePages SET " + t.getName() + "='" 
          + (request.getParameter(t.getName())==null?"false":request.getParameter(t.getName())) + "' WHERE PageID='" + pageID + "'";
	  log.paranoid("Attempting: " + sqlUpdateString,"Template:savePageInformation(...)");
          stmt.executeUpdate(sqlUpdateString);
        }
      }
    } catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage() + " trying to execute: " + sqlUpdateString,"Template:savePageInformation");
      return false;
    }
    return true;
  }

  private String printerFriendlyLink(String pageID) {
    return "<p><a href='" + Course.name + ".Page?ID=" + pageID + "&UserRequest=PrinterFriendlyLink'>" + res.getString("str_pfv") + "</a></p>";
  }

  protected String indexChange(HttpServletRequest request) {
    String ret="";
    int selected = -1;
    try {
       selected = Integer.parseInt(request.getParameter("Selected"));
    } catch(Exception e) {
      return res.getString("str_must_select");
    }
    try {
      TemplateItem t = (TemplateItem)templateItems.get(selected);

      ret = t.updateIndex(request,templateItems.size());
    } catch (Exception e) {
      return ret + "&nbsp;" + res.getString("str_nochange_index");
    } 
    if (reloadTemplateVector()) 
      return ret;
    else
      return ret + ".&nbsp;" + res.getString("str_error_loadvec");
  }

  synchronized public String appendInnerItems(String pageID, int sectionID) {
    StringBuffer buf = new StringBuffer("");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlQueryString = "SELECT * FROM TemplateSections WHERE PageID='" + pageID + "' AND Section='" + sectionID + "'"; 
      log.paranoid("Executing: " + sqlQueryString,"Template:appendInnerItems");
      ResultSet rsValues = stmt.executeQuery(sqlQueryString);
      if (!rsValues.next())
        return "";
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isInterParagraph()) {
	  if (t.isCheckbox()) {
	    if (rsValues.getBoolean(t.getName())) {
	      buf.append(t.getPreText() + t.getPostText());
	    }
	  }
	  else {
	    if (!rsValues.getString(t.getName()).equals(""))//blank value means don't use...
	      buf.append(t.getPreText() + rsValues.getString(t.getName()) + t.getPostText());
	  }
        }
      }
      rsValues.close();
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Template:appendInnerItems"); 
      if (addTemplateSectionTable()) {
        return appendInnerItems(pageID, sectionID);
      }
      log.paranoid("Creating the TemplateSections table didn't seem to fix the problem...","Template:appendInnerItems");
      return "";
    }
    return buf.toString();
  }

  synchronized public String appendInnerItemsEdit(String pageID, int sectionID) {
    boolean hasRecord=false;
    StringBuffer buf = new StringBuffer("<table border=0>");
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsValues = stmt.executeQuery("SELECT * FROM TemplateSections WHERE PageID='" + pageID + "' AND Section='" + sectionID + "'");
      if (rsValues.next()) 
        hasRecord=true; 
      for (Enumeration e = templateItems.elements();e.hasMoreElements();) {
        TemplateItem t = (TemplateItem)e.nextElement();
	if (t.isInterParagraph()) {
          buf.append("\n<TR><TD>");
	  if (t.isCheckbox()) {
	    buf.append("<INPUT TYPE=CHECKBOX NAME='" + t.getName() + sectionID
            + "' VALUE=true" + (hasRecord?(rsValues.getBoolean(t.getName())?
                " CHECKED":""):(t.getDefaultValue().equals("true")?
                  " CHECKED":"")) + ">&nbsp;" + t.getName());
	  } else {
	    buf.append(t.getName() + ":&nbsp;<INPUT TYPE=TEXT NAME='" 
            + t.getName() + sectionID + "' VALUE='" 
            + (hasRecord?rsValues.getString(t.getName()):t.getDefaultValue()) 
            + "'>");
	  }
          buf.append("</TD></TR>");
	}
      }
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught exception: " + e.getMessage(),"Template:appendBotItemsEdit");
      return "";
    }
    buf.append("</TABLE>");
    return buf.toString();
  }

  synchronized public boolean addTemplateSectionTable() {
    try {
      Class.forName(mySQLDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sql= "CREATE TABLE TemplateSections (PageID TEXT, Section int)"; 
      log.paranoid("Executing: " + sql,"Template:addTemplateSectionTable");
      stmt.executeUpdate(sql);
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Template:addTemplateSectionTable");
      return false;
    }
    return true;
  }
 
  synchronized public int execute(String sql) {
    int ret;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Executing: " + sql,"Template:execute");
      ret = stmt.executeUpdate(sql);
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught(e): " + e.getMessage(),"Template:execute");
      ret=-1;
    }
    return ret;
  }

  synchronized public ResultSet query(String sql) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      log.paranoid("Executing; " + sql,"Template:query");
      ResultSet rs = stmt.executeQuery(sql);
      return rs;
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Template:execute");
    }
    return null;
  }

}


