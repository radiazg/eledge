package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class Inicio extends HttpServlet {

  RBStore res = EledgeResources.getHomeBundle(); //inicializa los textos para cualquier idioma
  public String getServletInfo() {
    return res.getString("str_servlet_info");
  }
  
  public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    response.setContentType("text/html");
    PrintWriter out = response.getWriter(); //crea el objeto out para imprimir en pantalla
    HttpSession session = request.getSession(true); 
    
    out.println(inicio());  
	
}
  
  String inicio(){
 StringBuffer buf = new StringBuffer();
 buf.append("<table width=78% border=0>" 
  +"<tr>"
    +"<td height=344>" 
      +"<table width=100% border=0>"
        +"<tr>"
          +"<td><div align=center><strong><font color=#336699 size=4 face=Arial, Helvetica, sans-serif>Bienvenidos" 
           +   " a la Plataforma de Ense&ntilde;anza Virtual Eledge </font></strong>"
            +"<form name=form method=get action="+ Course.name +".Login>"
			+"<input type=submit value=Acceso>"
			+"</form>"
             +"</div></td>"
          +"<td><img src=../../images/tecla.jpg width=178 height=146></td>"
        +"</tr>"
      +"</table>"
      +"<hr>"
      +"<p align=center><font color=#663366 size=2>Sistema LMS Learning Manager System, realizado" 
       + " por la Universidad de Utah y la Universidad Aut&oacute;noma de Bucaramanga.</font></p>"
      +"</td>"
  +"</tr>"
+"</table>");

return buf.toString();
    
  }


}
