package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class Logout extends HttpServlet {
  private Logger log = new Logger();
  private RBStore res = EledgeResources.getLogoutBundle();

  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
    
    Student student = (Student)session.getAttribute(Course.name + "Student"); //devuelve null si el estudiante no ha iniciado sesion
    if (student==null) student = new Student(); //crea el objeto estudiante de Student.class
    
    session.invalidate();
    
	Cookie[] myCookie = request.getCookies();
    if (myCookie!=null) {
      for (int i=0;i<myCookie.length;i++) {
        if (!myCookie[i].getName().equals("wrapText") && !myCookie[i].getName().equals("wrapLength")) {
          log.paranoid("Resetting cookie age for: " + myCookie[i],"Logout:doGet");
          myCookie[i].setMaxAge(0);
          response.addCookie(myCookie[i]);
        } 
      }
    }
    MessageFormat mf = new MessageFormat(res.getString("str_logged_out"));
    Object[] args = {Course.name};
    out.println(Page.create("<meta http-equiv=refresh content='0;URL=../servlet/"+Course.name+".Home'>","<head><title>" + res.getString("str_title_logout") + "</title></head>"
    + "<body><h2>" + res.getString("str_gb") + "</h2>"
    + mf.format(args),student));
    //response.sendRedirect("../servlet/Eledge.Home");
      
  }
}
