package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.MessageFormat;

public class Login extends HttpServlet {
  private boolean allowCookieLogin = true;
  private RBStore res = EledgeResources.getLoginBundle();
  private Logger log = new Logger();
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    Student student = new Student();
    
    String destURL = new String();
    if (request.getParameter("DestURL")==null)
      destURL = "/servlet/" + Course.name + ".Home";
    else
      destURL = request.getParameter("DestURL");

    if (allowCookieLogin) {  // automatic login section
      Cookie[] myCookie = request.getCookies();
      try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
        for (int i=0;i<myCookie.length;i++){
          String sqlQueryString = "SELECT StudentIDNumber,Password FROM Students "
          + "WHERE PASSWORD(CONCAT(StudentIDNumber,Password))='" + myCookie[i].getValue() + "'";
          ResultSet rs = stmt.executeQuery(sqlQueryString);
          if (rs.next())
            if (student.authenticate(rs.getString("StudentIDNumber"),rs.getString("Password"))){
              session.setAttribute(Course.name + "Student",student);
              myCookie[i].setMaxAge(30*24*3600);  // reset to expire in 30 days
              response.addCookie(myCookie[i]);
              response.sendRedirect(destURL);
              return;
            }
          rs.close();
        }
      }
      catch (Exception e){
      }
    }
    // end of cookie section
    MessageFormat mf = new MessageFormat(res.getString("str_in_as"));
    Object[] args = {null,"<p>"};

    if (request.getParameter("StudentIDNumber")!=null && request.getParameter("Password")!=null)
      if (student.authenticate(request.getParameter("StudentIDNumber"),request.getParameter("Password"))){
          args[0] = "<b>" + student.getFullName() + "</b>";
          session.setAttribute(Course.name + "Student",student);
          out.println(Page.create("<meta http-equiv=refresh content='1;URL=" + destURL + "'>",
          mf.format(args),student)); 
          return;
      }
    out.println(Page.create(loginPage(destURL),student));
 }
  
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    Student student = new Student();
    String destURL = new String();
    if (request.getParameter("DestURL")==null)
      destURL = "/servlet/" + Course.name + ".Home";
    else
      destURL = request.getParameter("DestURL");
      
    String id = request.getParameter("StudentIDNumber");
    String pass = request.getParameter("Password");
    if ((student.authenticate(id,pass))){ //second entry: process 
     log.paranoid("id/pass combo authenticated by normal eledge standards.","Login:doPost");
      session.setAttribute(Course.name + "Student",student);
      if (allowCookieLogin & request.getParameter("GetCookie")!=null & !id.equals("0")) {
        try {
          Class.forName(Course.jdbcDriver).newInstance();
          Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
          Statement stmt = conn.createStatement();
          String sqlCodeGen = "SELECT PASSWORD(CONCAT('" + student.getIDNumber() + "','"
          + student.getPassword() + "')) AS Code";
          ResultSet rs = stmt.executeQuery(sqlCodeGen);
          if (rs.next()) {
            Cookie myCookie = new Cookie(Course.name,rs.getString("Code"));
            myCookie.setMaxAge(30*24*3600);  // valid for 1 month
            response.addCookie(myCookie);
          }
        }
        catch (Exception e) {
          log.sparse("Caught: " + e.getMessage(),"Login:doPost",e);
        }
      }
       log.paranoid("Attempting to redirect to: " + destURL,"Login:doPost");
       response.sendRedirect(destURL);
    }
    else if (student.getIsFrozen()) {
      log.paranoid("Student was frozen, and therefore failed the eledge authentication routine.","Login:doPost");
      out.println(Page.create("<h3>" + res.getString("str_title_act_frozen") + "</h3>"
      + res.getString("str_act_frozen"),student));
    }
    else {
      log.paranoid("Um, login failed for whatever reason.(id: " + id + ")","Login:doPost");
      out.println(Page.create(res.getString("str_login_failed"),student));
   }
  }
  
  String loginPage(String destURL) {
    MessageFormat mf = new MessageFormat(res.getString("str_idlength"));
    Object[] args = { new Integer(Course.idLength),"</a>" };
    
    StringBuffer buf = new StringBuffer("<head><title>" + res.getString("str_title_login_page") + "</title></head>");
    buf.append("<body><h2>" + res.getString("str_pls_login") + "</h2>"
    + res.getString("str_registered_login")
    + "<FORM NAME=LoginForm METHOD=POST>");
    
    if (Course.idLength > 0) {
      buf.append (res.getString("str_field_sid") + "&nbsp;<INPUT SIZE=" + Course.idLength
      + " NAME='StudentIDNumber'>&nbsp;(" + mf.format(args) + ")<br>");
    }
    else {
      buf.append(res.getString("str_field_username") 
      + "&nbsp;<INPUT SIZE=12 NAME='StudentIDNumber'>&nbsp;("
      + res.getString("str_case") + ")<br>");
    }
    buf.append(res.getString("str_field_pass") + "&nbsp;<INPUT TYPE=PASSWORD SIZE=12 NAME='Password'>"
    + "<input type=hidden name=DestURL value='" + destURL + "'>&nbsp;(" + res.getString("str_case") 
    + ")<br><input type=checkbox name=GetCookie value=True>&nbsp;"
    + res.getString("str_cookie_login") + "<sup><font color='#FF0000' size='4'> *</font></sup><br>"
    + "<input type=submit value='" + res.getString("str_btn_login") 
    + "'><p><sup><font color='#FF0000' size='4'> *</font></sup>" + res.getString("str_explain_cookie")
    + "</FORM>");

    mf.applyPattern(res.getString("str_forgot_pword"));
    args[0]="<a href=" + Course.name + ".Profile>";
    buf.append("<hr>" + mf.format(args));

    mf.applyPattern(res.getString("str_visitor_account"));
    args[0]="<a href='" + Course.name + ".Login?StudentIDNumber=0&Password=visitor&DestURL=" + destURL + "'>";
    buf.append("<hr>" + mf.format(args) + "<br>" + res.getString("str_visitor_nowork"));
    
    mf.applyPattern(res.getString("str_administrador"));
    args[0]="<a href=" + Course.name + ".Login?StudentIDNumber=admin&Password=123456&DestURL=" + destURL + "'>";
    buf.append("<hr>" + mf.format(args) + "<br>");
    
    mf.applyPattern(res.getString("str_tomas"));
    args[0]="<a href=" + Course.name + ".Login?StudentIDNumber=tomas&Password=123456&DestURL=" + destURL + "'>";
    buf.append("<hr>" + mf.format(args) + "<br>");
    
    mf.applyPattern(res.getString("str_olafo"));
    args[0]="<a href=" + Course.name + ".Login?StudentIDNumber=olafo&Password=123456&DestURL=" + destURL + "'>";
    buf.append("<hr>" + mf.format(args) + "<br>" + "</body></html>");
    
    
    return buf.toString();
  }
}
