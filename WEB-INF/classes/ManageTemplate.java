package Eledge; //tells how to name the servlet.

import java.util.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class ManageTemplate extends HttpServlet {

  /*eventually, it might be nice to port Eledge to other db's...
  so, we call this dbDriver instead of mySQLDriver, and use
  it in our class loader call to make porting later easer.*/
  RBStore res = EledgeResources.getManageTemplateBundle();
  private String dbDriver = Course.jdbcDriver;
  Template tmplate = new Template(); 
  Logger log = new Logger();
  //the name, and the type on one page, and even location.
  //Then we can put the pre/post/defaults on the next page (with the info
  //from the first page in hidden inputs). That way, the text around pre/post
  //is appropriate, and the default can be an input or a checkbox, dependent on 
  //type. A "preview template layout" button might also be handy, using default
  //values for inputs, and assuming all checkboxes true.
  //Need to handle the wizard, updates (don't need a regular "new" button, 
  //because, like navbarlinks, there will always be one slot available for 
  //quick "addition" of a link, and w/ the "update" btton. Actually, should
  //call the update button SaveTemplateInformation or some such.
  //Also need to handle deletes. That should do it.
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) 
     throws ServletException, IOException {
     log.paranoid("Sending Http request to doPost","ManageTemplate:doGet");
     doPost(request,response);
     log.paranoid("Request sent.","ManageTemplate:doGet");
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student =  (Student)session.getAttribute(Course.name + "Student");
    int affectedMsgID = 0;
    if (student == null) student = new Student();

    if (!student.isAuthenticated()) {
      response.sendRedirect(Course.secureLoginURL + "ManageTemplate");
      return;
    }
    if (student.getIsFrozen()) {
      out.println(Page.create(res.getString("str_frozen")));
      return;
    }
    if (student.getIsTA()) {
      out.println(Page.create(Permissions.getPermission("Content").getDenyMsg()));
      return;
    }
    if (!student.getIsInstructor()) {
      out.println(Page.create(res.getString("str_instructor_only")));
      return;
    }

    //from here on, user id assumed to be valid.
    String userRequest = request.getParameter("UserRequest");
    log.paranoid(student.getIDNumber() + " made a request of " + userRequest,"ManageTemplate:doPost");
    if (userRequest == null || userRequest.equals("Cancel")) {
      out.println(Page.create(listTemplates()));
      return;
    }

    if (userRequest.equals("StartWizard")) {
      out.println(Page.create(startWizard()));
      return;
    }

    if (userRequest.equals("ContinueWizard")) {
      StringTokenizer st = new StringTokenizer(request.getParameter("NewName")," ");
      if (st.countTokens()>1) {
        out.println(res.getString("str_oneword"));
	return;
      }
      out.println(Page.create(continueWizard(request)));
      return;
    }

    if (userRequest.equals("FinishWizard")) {
      out.println(Page.create(finishWizard(request) + listTemplates()));
      return;
    }

    if (userRequest.equals("Update")) {
      tmplate.updateItems(request);
      out.println(Page.create(listTemplates()));
      return;
    }

    if (userRequest.equals("ChangeIndex")) {
      out.println(Page.create(tmplate.indexChange(request) + listTemplates()));
      return;
    }

    if (userRequest.equals("Delete")) {
      out.println(Page.create(tmplate.deleteItem(request) + listTemplates()));
      return;
    }
  }
  
  protected String listTemplates() {
    StringBuffer buf = new StringBuffer("<H2>" + res.getString("str_title_manage") + "</H2><br><FORM METHOD=POST>");
    buf.append(tmplate.displayItems());
    buf.append("<INPUT TYPE=HIDDEN NAME='UserRequest'>"
      + "<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='StartWizard' VALUE='"
      + res.getString("str_btn_wizard") + "'>"
      + "<INPUT TYPE=SUBMIT onClick=this.form.elements.UserRequest.value='Update' VALUE='"
      + res.getString("str_btn_update") + "'>"
      + "<INPUT TYPE=Submit onClick=this.form.elements.UserRequest.value='Delete' VALUE='"
      + res.getString("str_btn_delete") + "'>"
      + "<INPUT TYPE=Submit onClick=this.form.elements.UserRequest.value='ChangeIndex' VALUE='"
      + res.getString("str_btn_index") + "'>"
      + "</FORM>");
    return buf.toString();

  }

  protected String startWizard() {
    StringBuffer buf = new StringBuffer();
    //javascript function for quick input checking.
    buf.append("<SCRIPT Language=\"JavaScript\">\n<!--\nfunction verifyInput()"
      + "\n{\n  if (document.forms[0].elements[\"NewName\"].value==null ||"
      + " document.forms[0].elements[\"NewName\"].value==\"\") {\n"
      + "    alert(\"" + res.getString("str_alert_name") + "\");\n"
      + "    return false;\n  }\n  return true;\n}\n-->\n</SCRIPT>");
    buf.append("<p>" + res.getString("str_explain_wizard") + "</p>");

    buf.append("<FORM METHOD=POST><INPUT TYPE=HIDDEN NAME='UserRequest'>"
      + "\n<TABLE BORDER=1 CELLSPACING=0><TR>"
      + "<TH>" + res.getString("str_field_fieldname") + "</TH><TH>"
      + res.getString("str_field_value") + "</TH><TH>"
      + res.getString("str_field_description") + "</TH></TR>\n<TR>");
    buf.append("<TD>" + res.getString("str_iname") + "</TD><TD><INPUT TYPE=TEXT NAME='NewName'></TD>"
      + "<TD>" + res.getString("str_explain_name") + "</TD></TR>");
    buf.append("\n<TR><TD>" + res.getString("str_itype") + "</TD><TD>" 
      + TemplateItem.displayTypesNew() + "</TD><TD>" + res.getString("str_explain_type") + "</TD></TR>");
    buf.append("\n<TR><TD>" + res.getString("str_iloc") + "</TD><TD>" 
      + TemplateItem.displayLocationsNew() + "</TD><TD>"
      + res.getString("str_explain_loc") + "</TD></TR>");
    buf.append("\n</TABLE><INPUT TYPE=BUTTON VALUE='---->' "
      + "onClick=\"if (verifyInput()){UserRequest.value='ContinueWizard';"
      + "document.forms[0].submit();}\">&nbsp;"
      + "<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_cancel") + "' "
      + "onClick=this.form.elements.UserRequest.value='Cancel'></FORM>"); 
    return buf.toString();
  }

  protected String continueWizard(HttpServletRequest request) {
    StringBuffer buf = new StringBuffer();
    int location = TemplateItem.extractLocationNew(request);
    int type = TemplateItem.extractTypeNew(request);
    buf.append("\n<FORM METHOD=POST><INPUT TYPE=HIDDEN NAME=UserRequest>"
      + " <INPUT TYPE=HIDDEN NAME='ItemType' Value='" + type 
      + "'><INPUT TYPE=HIDDEN NAME='ItemLocation' VALUE='" + location 
      + "'><INPUT TYPE=HIDDEN NAME='ItemName' VALUE='" 
      + request.getParameter("NewName") + "'>");
    buf.append("\n<TABLE BORDER=1 CELLSPACING=0><TR><TH>" 
      + res.getString("str_field_fieldname") + "</TH><TH>"
      + res.getString("str_field_value") + "</TH><TH>"
      + res.getString("str_field_description") + "</TH></TR>");
    buf.append("\n<TR><TD>" + res.getString("str_field_secpos") + "</TD><TD>" 
      + tmplate.displaySectionIndex(location)
      + "</TD><TD>" + res.getString("str_explain_secpos") + "</TD></TR>");
    if (TemplateItem.isCheckbox(type)) {
      buf.append("\n<TR><TD>" + res.getString("str_itext") + "</TD><TD><INPUT TYPE=TEXT "
      + "NAME='PreText'></TD><TD>" + res.getString("str_explain_pretext_cb") 
      + "<INPUT TYPE=HIDDEN NAME=PostText VALUE=''></TD></TR>");
    } else {
      buf.append("\n<TR><TD>" + res.getString("str_ipretext")
      + "</TD><TD><INPUT TYPE=TEXT "
      + "NAME='PreText'></TD><TD>" + res.getString("str_explain_pretext_input")
      + "</TD></TR>");
      buf.append("\n<TR><TD>" + res.getString("str_iposttext") + "</TD><TD><INPUT TYPE=TEXT "
      + "NAME='PostText'></TD><TD>" + res.getString("str_explain_posttext")
      + "</TD></TR>");
    }

    buf.append("\n<TR><TD>" + res.getString("str_idefault") + "</TD><TD>" 
      + TemplateItem.displayDefaultNew(type) + "</TD><TD>"
      + res.getString("str_explain_default1"));
    if (TemplateItem.isCheckbox(type)) 
      buf.append(res.getString("str_explain_default2"));
    else
      buf.append(res.getString("str_explain_default3"));
    buf.append("</TD></TR></TABLE>\n<INPUT TYPE=SUBMIT VALUE='---->' onClick=this.form.elements.UserRequest.value='"
      + "FinishWizard';>&nbsp;<INPUT TYPE=SUBMIT VALUE='" + res.getString("str_btn_cancel") 
      + "' onClick=this.form.elements.UserRequest.value='Cancel';></FORM>");
    return buf.toString();
  }

  protected String finishWizard(HttpServletRequest request) {
    log.paranoid("Beginning method.","ManageTemplate:finishWizard");
    boolean successful = false;
    StringBuffer buf = new StringBuffer("");
    String name = request.getParameter("ItemName");
    String preText = CharHider.squot2dquot(request.getParameter("PreText"));
    String postText = CharHider.squot2dquot(request.getParameter("PostText"));
    log.paranoid("name: " + name + "; preText: " + preText + "; postText: " + postText,"ManageTemplate:finishWizard");
    int type, location, index;
    try {
      type = Integer.parseInt(request.getParameter("ItemType"));
    } catch(Exception e) {
      type = TemplateItem.TYPE_INPUT;
    }
    try {
      location = Integer.parseInt(request.getParameter("ItemLocation"));
    } catch (Exception e) {
      location=TemplateItem.LOC_TOP;
    }
    try {
      index = Integer.parseInt(request.getParameter("Index")); 
    } catch (Exception e) {
      index=0;
    }
    String defaultValue=""; 
    log.paranoid("Creating item with name: " + name + " and type: " + type,"ManageTemplate:finishWizard");
    TemplateItem item = new TemplateItem(name,type);
    //each "set" returns boolean . . . 
    if (item.setLocation(location)) {
      if (item.setPreText(preText)) {
        if (item.setPostText(postText)) {
          if (item.setIndex(index)) {
	    log.paranoid("Location, pretext, posttext, and index successfully set.","ManageTemplate:finishWizard");
            successful=true;
	  } else
	    log.paranoid("setIndex didn't work. Index was: " + index,"ManageTemplate:finishWizard");
	} else
	  log.paranoid("setPostText(" + postText + ") failed.","ManageTemplate:finishWizard");
      } else
        log.paranoid("setPreText(" + preText + ") failed.","ManageTemplate:finishWizard");
    } else
      log.paranoid("setLocation(" + location + ") failed.","ManageTemplate:finishWizard");
    if (!successful)
      return res.getString("str_problem_stage1");
    if (item.isCheckbox()) 
      defaultValue = request.getParameter("NewDefault")==null?"false":"true"; 
    else 
      defaultValue = CharHider.squot2dquot(request.getParameter("NewDefault"));
    successful = item.setDefaultValue(defaultValue);
    if (!successful) {
      log.paranoid("setDefaultValue(" + defaultValue + ") failed.","ManageTemplate:finishWizard");
      return res.getString("str_problem_stage2");
    }
    successful = tmplate.addItem(item);
    if (successful) {
      log.paranoid("Normal termination of method.","ManageTemplate:finishWizard");
      return res.getString("str_add_ok");
    } else return res.getString("str_problem_stage3");
  }
}

