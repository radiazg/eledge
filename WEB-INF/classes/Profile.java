package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;

public class Profile extends HttpServlet {
  boolean allowNewProfiles = false;
  boolean allowProfileEditing = true;//default is true...
  RBStore res = EledgeResources.getProfileBundle(); //crea el objeto para la traduccion
  Logger log = new Logger(); //crea el objeto Logger de Logger.class para almacenar log de informacion
  //int numberOfSections = 0;
    
   public String getServletInfo() {
    return "This Eledge servlet allows the students to register and maintain a class profile.";
  }
  
 public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    log.paranoid("Begin Method.","Profile:doGet"); //realiza el log de tipo paranoid (excesivo)
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    StringBuffer buf = new StringBuffer();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      //para sacar el valor falso/verdadero de asignar nuevos perfiles o editar perfiles
      ResultSet rs = stmt.executeQuery("SELECT * FROM CourseParameters WHERE Name='AllowNewProfiles' OR Name='AllowProfileEditing' ORDER BY Name");
      rs.next();
      allowNewProfiles = rs.getBoolean("Value");
      if (rs.next()) {
        allowProfileEditing=rs.getBoolean("Value"); //el if examina si existe el valor AllowProfileEditing sino lo crea
      } else {
	stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AllowProfileEditing','true')");
      }
        rs.close();
        
   	stmt.close();
	conn.close();
    }
    catch (Exception e) {
      out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_access_error"),student));
      return;
    }
    out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+profileForm(student),student));
  }

  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {
    
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    Student student = (Student)session.getAttribute(Course.name + "Student");
    if (student == null) student = new Student();
    StringBuffer buf = new StringBuffer();
    /*try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      
      ResultSet rs = stmt.executeQuery("SELECT * FROM CourseParameters WHERE Name='NumberOfSections'");
      rs.next();
      numberOfSections = rs.getInt("Value");
      rs.close();
      
      rs = stmt.executeQuery("SELECT * FROM CourseParameters WHERE Name='AllowNewProfiles'");
      rs.next();
      allowNewProfiles = rs.getBoolean("Value");
      rs.close();
    }
    catch (Exception e) {
      out.println(Page.create(res.getString("str_access_error")));
      return;
    }*/
    
    //captura la variable introducida por el usuario UserRequest
    String userRequest = request.getParameter("UserRequest");
    
    // password recovery routine
    if (userRequest.equals("Recover")) {
    	//extrae la variable
      String id = request.getParameter("StudentIDNumber");
      
      //muestra en pantalla la forma secreta de recuperacion de password
      //se dirige hacia el metodo secretForm, para desarrollarla 
      if (student.idNumberLooksValid(id)) {
      	  out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+secretForm(student,request.getParameter("StudentIDNumber")),student));
      }
      //muestra en pantalla, que no se digito un nombre de usuario
      else {
		  out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_err_back") 
		  + (Course.idLength==0?res.getString("str_user"):res.getString("str_sid")),student));
      } 
      return;
    }
    
    //para recuperar password con la respuesta secreta (forma2)
    if (userRequest.equals("Secret")) {
      
      String id = request.getParameter("StudentIDNumber");
      //examina si la respuesta es correcta, si lo es inicia sesion
      if (student.secretAnswerIsCorrect(id,request.getParameter("SecretAnswer"))) {
        student.authenticate(id,student.getPassword(id));
        session.setAttribute(Course.name + "Student",student);
        out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_pwd_is") + ": <strong>" +student.getPassword(id) + "</strong>"
        + "<br>" + res.getString("str_logged_in"),student));
      }
      //mensaje de error si no tuvo la respuesta correcta
      else out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_answer_incorrect"),student));
    }
    
    //sirve para verificar que los datos que se van a almacenar
    //se encuentren validos
    // process student profile request
    if (userRequest.equals("Profile")) {
      StringBuffer errorBuf = new StringBuffer();
      
      //solo para estudiante logeados
      if (student.isAuthenticated()) {
        
        //si es visitante no puede cambiar valores
        if (student.getIsVisitor()) {
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_err_visitor"),student));
          return;
        }
        
        //si esta congelado muestra mensaje de ello
        if (student.getIsFrozen()) {
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_err_frozen"),student));
          return;
        }
		
		//si no es ninguno de los dos anteriores
		//si tiene permisos de editar o si es instructor
		//salva el perfil revisado
		if (allowProfileEditing || student.getIsInstructor()) {
          if (student.storeProfile(     // store revised profile
            student.getIDNumber(),
            //Integer.toString(student.sectionID),
            request.getParameter("LastName"),
            request.getParameter("FirstName"),
            request.getParameter("Email"),
            request.getParameter("ShareInfo"),
            request.getParameter("Password"),
            request.getParameter("Confirm"),
            request.getParameter("SecretQuestion"),
            request.getParameter("SecretAnswer"),
            errorBuf,request)) {
            out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+"<h3>" + res.getString("str_thanks") + "</h3>"
            + res.getString("str_profile_changed"),student));
          }
          //mensaje de error de datos invalidados
          else {
            out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_info_missing") + errorBuf
            + "<br>" + res.getString("str_try_again") + "<p>",student));
          }
        } 
          //mensaje en pantalla, que dice que no se puede editar
          //porque no tiene permisos 
          else {
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+res.getString("str_no_editing"),student));
	}
      }
      //para usuarios no logeados
      //salva el perfil revisado de un nuevo estudiante
      else {                         // student not logged in
        if (student.storeProfile(    // store a new student profile
          request.getParameter("StudentIDNumber"),
          //request.getParameter("SectionID"),
          request.getParameter("LastName"),
          request.getParameter("FirstName"),
          request.getParameter("Email"),
          request.getParameter("ShareInfo"),
          request.getParameter("Password"),
          request.getParameter("Confirm"),
          request.getParameter("SecretQuestion"),
          request.getParameter("SecretAnswer"),
          errorBuf,request)) {
          
          //si los datos estan bien inicia sesion automaticamente
          if (student.authenticate(request.getParameter("StudentIDNumber"),request.getParameter("Password"))){
          		session.setAttribute(Course.name+"Student",student);
          }
          //muestra en pantalla mensaje de bienvenida  
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+
          "<h3>" + res.getString("str_welcome") + Course.name + res.getString("str_welcome2") + "</h3>"
	  + res.getString("str_profile_created") + student.getFullName() + ".<br>"
          + res.getString("str_click_links"),student));
        }
        //muestra en pantalla los datos invalidados
        else {
          out.println(Page.create("<em class='bar-body'><a href='"+Course.name+".Home'>Home</a> >> Mi Cuenta</em><br><br>"+"<h3>" + res.getString("str_err") + "</h3>"
          + res.getString("str_info_missing") + errorBuf
          + "<br>" + res.getString("str_try_again") + "<p>"
          + res.getString("str_must_login"),student));
        }
      }
    }
  }
  
  //forma para recuperar password. hace la consulta en el metodo del objeto estudiante
  String secretForm(Student student, String id) {
    StringBuffer buf = new StringBuffer();
   buf.append("<h2>" + res.getString("str_title_pwd_recover") + "</h2>"
    + res.getString("str_enter_question") + "<br>"
    + "<b>" + student.getSecretQuestion(id) + "</b><br>"
    + "<FORM METHOD=POST>"
    + "<input size=40 name=SecretAnswer><br>"
    + "<input type=hidden name=StudentIDNumber value='" + id + "'>"
    + "<input type=hidden name=UserRequest value=Secret>"
    + "<input type=submit value='" + res.getString("str_btn_reveal") + "'>"
    + "</FORM>");
    return buf.toString();
  }
 
  String profileForm(Student student) {
    StringBuffer buf = new StringBuffer();
    
    //examina si el usuario esta autenticado, no muestra en pantalla recuperación de password
    if (student.isAuthenticated()) { 
      if (allowProfileEditing || student.getIsInstructor()) {
        buf.append("<h2>" + res.getString("str_title_edit") + "</h2>"
        + res.getString("str_explain_edit"));
       } else {
         return res.getString("str_no_editing");
       }
    }
    //muestra en pantalla lo necesario para recuperar el password
    else {
      //mensaje de la forma de recuperación de password
      buf.append("<h3" + res.getString("str_title_lost") + "</h3>"
      + res.getString("str_explain_lost") 
      + (Course.idLength>0?res.getString("str_sid"):res.getString("str_user"))
      + res.getString("str_explain_lost2") 
      //forma de recuperación de password
      + "<FORM METHOD=POST>");
      int length = (Course.idLength==0?12:Course.idLength);
      buf.append ("<input size=" + length + " name=StudentIDNumber>&nbsp;"
      +   "<input type=hidden name=UserRequest value=Recover>"
      +   "<input type=submit value='" + res.getString("str_btn_recover") + "'>"
      + "</FORM><hr>");
      
      //para editar el perfil de un usuario existente, hace un enlace hacia login
      buf.append("<h3>" + res.getString("str_title_edit_existing") + "</h3>"
      + res.getString("str_click1") + "&nbsp;<a href=" + "/servlet/" + Course.name + ".Login>" 
      + res.getString("str_click2") + "</a>&nbsp;" 
      + res.getString("str_click3") + "<hr>");
      
      //muestra mensaje en pantalla de la forma de crear usuario
      if (allowNewProfiles) { 
        buf.append("<h3>" + res.getString("str_title_create") + "</h3>"
	+ res.getString("str_explain_create1") + "<p>"
        + res.getString("str_explain_create2"));
      }
      //muestra mensaje en pantalla y no permite ingresar datos, cuando el valor de crear nuevos usuarios es false 
      else { // new profiles not permitted
        buf.append("<h3>" + res.getString("str_title_no_new") + "</h3>"
        + res.getString("str_explain_no_new"));
        return buf.toString();
      }
   }  
    // create profile data table
    buf.append("<FORM METHOD=POST><table border=0 cellspacing=1>\n"
    + "  <tr><td ALIGN=RIGHT>"
    //tiene dos entradas, una para caracteres y otra para digitos
    + (Course.idLength==0?res.getString("str_field_un"):res.getString("str_field_sid"))
    + "</td><td>");
    
    //forma de entrada de datos solo para usuarios autenticados, (muestra es nombre de usuario, y seccion)
    if (student.isAuthenticated()) {
      
      //entra el nombre de usuario o el numero, que ambos son los identifiacadores
      buf.append("<b>" + student.getIDNumber() + "</b></td></tr>\n");
      
      //condicion solo para cuando el numero de secciones es > a 1
      /*if (numberOfSections > 1) { 
        String sectionName = null;
        try {
          Class.forName(Course.jdbcDriver).newInstance();
          Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
          Statement stmt = conn.createStatement();
          ResultSet rsSections = stmt.executeQuery("SELECT * FROM CourseSections WHERE SectionID='" + student.sectionID + "'");
          rsSections.next();
          sectionName = rsSections.getString("SectionName");
        }
        catch (Exception e) {
          sectionName = "";
        }
        //almacena en buffer el numero de la seccion 
        buf.append("<tr><td ALIGN=RIGHT>" + res.getString("str_field_section")
	+ "</td><td><b>" + sectionName + "</b></td></tr>");
      }*/
    }
    
    //forma para usuarios no autenticados
    else {  // this is a new profile
      buf.append("<input size=" + (Course.idLength==0?20:Course.idLength) 
      + " name=StudentIDNumber><font color='#FF0000' size='4'> *</font></td></tr>\n");
      //buf.append(sectionSelectBox()); //se dirige hacia el metodo sectionSelectBox de esta clase
    }
    
    //muestra en pantalla es resto de entradas para complementar
    //la forma de usuario autenticado y no autenticado    
    buf.append("<tr><td ALIGN=RIGHT>" + res.getString("str_field_fn")
    + "</td><td><input name=FirstName "
    + "value=\"" + student.firstName + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "<tr><td ALIGN=RIGHT>" + res.getString("str_field_ln") 
    + "</td><td><input name=LastName "
    + "value=\"" + student.lastName + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "<tr><td ALIGN=RIGHT>" + res.getString("str_field_email")
    + "</td><td><input name=Email "
    + "value=\"" + student.email + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "<tr><td ALIGN=RIGHT><input type=checkbox name=ShareInfo value='true' "
    + (student.shareInfo.equals("true")?"CHECKED":"") + "></td>"
    + "<td>" + res.getString("str_list_me") + "</td></tr>"
    + "<tr><td ALIGN=RIGHT>" + res.getString("str_field_pwd")
    + "</td><td><input type=password size=12 name=Password "
    + "value=\"" + student.getPassword() + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "<tr><td ALIGN=RIGHT>" + res.getString("str_field_pwd2")
    + "</td><td><input type=password size=12 name=Confirm "
    + "value=\"" + student.getPassword() + "\"><font color='#FF0000' size='4'> *</font></td></tr>"
    + "</table>\n"
    + "<table border=0>"
    + "<tr><td>" + res.getString("str_field_question") + "<br>"
    + "<input size=40 name=SecretQuestion value=\"" + student.getSecretQuestion() + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "<tr><td>" + res.getString("str_field_answer") + "<br>"
    + "<input size=40 name=SecretAnswer value=\"" + student.getSecretAnswer() + "\"><font color='#FF0000' size='4'> *</font></td></tr>\n"
    + "</table>"
    + "<input type=submit value='" + res.getString("str_btn_submit") + "'>"
    + "<input type=hidden name=UserRequest value=Profile>"
    + "</FORM>");
    return buf.toString();
  }

  /*String sectionSelectBox() { //crea la caja de seleccion de seccion, con las secciones disponibles
    StringBuffer buf = new StringBuffer("<tr><td ALIGN=RIGHT>" 
    + res.getString("str_field_section") + "</td><td><SELECT NAME=SectionID>");
    //int numberOfSections = 1;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT Value FROM CourseParameters WHERE Name='NumberOfSections'");
      
      if (rsSections.next()) {
      	numberOfSections = rsSections.getInt("Value");
      }
      if (numberOfSections == 1) {
      	return "\n<INPUT TYPE=HIDDEN NAME=SectionID VALUE=1>";
      } 
	  else {      
        String sqlStr = "SELECT * FROM CourseSections ORDER BY SectionID LIMIT " + numberOfSections;
        ResultSet rs = stmt.executeQuery(sqlStr);
        buf.append("<OPTION VALUE='1'>" + res.getString("str_opt_choose") + "</OPTION>");
        while (rs.next())
          buf.append("<OPTION VALUE='" + rs.getString("SectionID") + "'>" + rs.getString("SectionName") 
          + " - " + rs.getString("Description") + "</OPTION>");
        buf.append("</SELECT></td></tr>");
      }
    }
    catch (Exception e) {
      return e.getMessage();
    }
    return buf.toString();
  }*/
}
