package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.sql.*;
import java.text.Collator;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class Student {
  int sectionID = 1;
  String lastName = "";
  String firstName = "";
  String email = "";
  String shareInfo = "true";
  String status = "Visitor";
  private String idNumber = "";
  private String password = "";
  private String secretQuestion = "";
  private String secretAnswer = "";
  private int hitCount=0;
  private boolean isAuthentic = false;
  private boolean isRegistered = false;
  private boolean isInstructor = false;
  private boolean isFrozen = false;
  private boolean isVisitor = false;
  private boolean isTA;
  private String course_id;
  private int theme_id;
  private boolean TAtoStudent = false;
  private Logger log = new Logger();
  private RBStore res = EledgeResources.getStudentBundle();

  public String getServletInfo() {
    return "This Eledge class defines the characteristics and methods of a student or instructor.";
  }
  
  public boolean loadStudent(String studID) {
    boolean ret=false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sql = "SELECT * FROM Students WHERE StudentIDNumber='" + studID + "'";
      log.paranoid("Executing: " + sql,"Students:student(String)");
      ResultSet rs = stmt.executeQuery(sql); 
      if (rs.next()) {
        this.idNumber=rs.getString("StudentIDNumber");
        this.lastName=rs.getString("LastName");
        this.firstName=rs.getString("FirstName");
        this.sectionID=rs.getInt("SectionID");
        this.email=rs.getString("Email");
        this.password=rs.getString("Password");
        this.secretQuestion=rs.getString("SecretQuestion");
        this.secretAnswer=rs.getString("SecretAnswer");
        this.status=rs.getString("Status");
        this.shareInfo = rs.getString("ShareInfo");
        if (status.equals("Instructor")) {
          this.isInstructor=true;
        }
        if (status.equals("TA")) {
          this.isTA=true;
        }
        if (status.equals("Visitor")) {
          this.isVisitor=true;
        }
	this.isAuthentic=true;
        ret=true;
      }
      rs.close();
      stmt.close();
      conn.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"Student:loadStudent",e);
      ret = false;
    }
    return ret;

  }

  String getIDNumber() {
    if (isAuthentic) return idNumber;
    return "";
  }
 
  String getIDNumber(boolean isLoaded) {
    if (isLoaded)
      return idNumber;
    return "";
  }

  String getPassword() {
    if (isAuthentic) return password;
    return "";
  }

  String getPassword(String id) {
    String returnValue = null;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlString = "SELECT Password from Students WHERE StudentIDNumber='" + id + "'";
      ResultSet rs = stmt.executeQuery(sqlString);
      
      if (rs.next()) returnValue = rs.getString("Password");
      else returnValue = res.getString("str_id_not_valid") + " " + id;
    } catch (Exception e) {
      return e.toString();
    }
    return returnValue;
  }
  
  String getSecretQuestion() {
    if (isAuthentic) return secretQuestion;
    return "";
  }

  String getSecretQuestion(String id) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlString = "SELECT SecretQuestion from Students WHERE StudentIDNumber='" + id + "'";
      ResultSet rs = stmt.executeQuery(sqlString);
      
      if (rs.next()) return rs.getString("SecretQuestion");
      else return res.getString("str_id_not_valid") + " " + id;
    }
    catch (Exception e) {
     return e.toString();
    }
  }
  
  String getSecretAnswer() {
    if (isAuthentic) return secretAnswer;
    return "";
  }
  
  boolean secretAnswerIsCorrect(String id,String sa) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      String sqlString = "SELECT SecretAnswer from Students WHERE StudentIDNumber='" + id + "'";
      ResultSet rs = stmt.executeQuery(sqlString);
      
     if (rs.next()) {
        Collator compare = Collator.getInstance();
        compare.setStrength(Collator.PRIMARY);
        return compare.equals(sa,rs.getString("SecretAnswer"));
      }
    } catch (Exception e) {
      return false;
    }
    return false;
  }
  
  String getFullName() {
    if (isAuthentic) return firstName + " " + lastName;
    return "";
   }
  
  String getFullName(String studentIDNumber) {
    String name=studentIDNumber;
    if (!isAuthentic) return name;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn=DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsName=stmt.executeQuery("SELECT CONCAT(FirstName, ' ', LastName) AS Name From Students WHERE StudentIDNumber='" + studentIDNumber + "'");
      if (!rsName.next())
        return name;
      name=rsName.getString("Name");
      return name;
    }catch(Exception e){
      return name;
    }
  }

  boolean getIsTA() {
    return isTA;
  }

  boolean getIsInstructor(){
    return isInstructor;
  }

  boolean getIsFrozen(){
    return isFrozen;
  }

  boolean getIsVisitor(){
    return isVisitor;
  }
  
  boolean storeProfile(String id, String ln, String fn, String em, String si, String pw, String cf, String sq, String sa, StringBuffer errorBuf, HttpServletRequest request) {
  //boolean storeProfile(String id, String sc, String ln, String fn, String em, String si, String pw, String cf, String sq, String sa, StringBuffer errorBuf, HttpServletRequest request) {
    boolean newProfile = true;
    if (isAuthentic) newProfile = false;
    
    // validate the entries:
    if (idNumberLooksValid(id)) idNumber = id;
    else {
      errorBuf.append(res.getString("str_id_not_valid") + " " + id);
      return false;
    }

    if (ln.length() > 0) lastName = ln;
    else {
      errorBuf.append(res.getString("str_nolast"));
      return false;
    }
    
    if (fn.length() > 0) firstName = fn;
    else {
      errorBuf.append(res.getString("str_nofirst"));
      return false;
    }
    
    if (em.length() > 0 & em.indexOf('@') > 0) email = em;
    else {
      errorBuf.append(res.getString("str_bad_email"));
      return false;
    }

    if (si != null) {
      if (si.equals("true")) shareInfo = "true";
	  else shareInfo = "false";
    }
    else shareInfo = "false";
	    
    if (pw.length() >= 6 & pw.length() <= 12 & pw.equals(cf)) password = pw;
    else {
      errorBuf.append(res.getString("str_bad_pass"));
      return false;
    }

    if (sq.length() > 0) secretQuestion = sq;
    else {
      errorBuf.append(res.getString("str_no_sq"));
      return false;
    }
    
    if (sa.length() > 0) secretAnswer = sa;
    else {
      errorBuf.append(res.getString("str_no_sa"));
      return false;
    }

    String sqlString; 
    if (newProfile) {      
      sqlString = "INSERT INTO Students "
      + "(StudentIDNumber,LastName,FirstName,Email,ShareInfo,Password,SecretQuestion,SecretAnswer,SectionID,Status) "
      + "VALUES ("
      + "'" + idNumber + "',"
      + "'" + converter(lastName) + "',"
      + "'" + converter(firstName) + "',"
      + "'" + converter(email) + "',"
      + "'" + shareInfo + "',"
      + "'" + converter(password) + "',"
      + "'" + converter(secretQuestion) + "',"
      + "'" + converter(secretAnswer) + "',"
      + "'" + Integer.toString(sectionID) + "',"
      + "'" + status + "')";  // default value is 'Current'
    }
    else { // revise existing profile entry
      sqlString = "UPDATE Students SET "
      + "LastName='" + converter(lastName) + "',"
      + "FirstName='" + converter(firstName) + "',"
      + "Email='" + converter(email) + "',"
      + "ShareInfo='" + shareInfo + "',"
      + "Password='" + converter(password) + "',"
      + "SecretQuestion='" + converter(secretQuestion) + "',"
      + "SecretAnswer='" + converter(secretAnswer) + "' "
      + "WHERE StudentIDNumber='" + idNumber + "' LIMIT 1";
    } 

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      if (stmt.executeUpdate(sqlString) == 1) {
        isAuthentic = true;
        authenticate(idNumber,password);
        HttpSession session = request.getSession(true);
        session.setAttribute(Course.name + "Student",this);
        return true;
      }
      return false;
    } catch (Exception e) {
      if (createStudentsTable()) return true; // first profile creates table with Instructor and Visitor profiles
      if (newProfile) {
        MessageFormat mf = new MessageFormat(res.getString("str_new_stud_failed"));
        Object[] args = {
          "<a href='" + Course.name + ".Login?DestURL=" + Course.name + ".Profile'>",
          "</a>"
        };
        errorBuf.append(mf.format(args));
      } else errorBuf.append(res.getString("str_db_error"));
      return false;
    }
  }

  String converter(String oldString, int fromIndex) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }
  
  String converter(String oldString) {
  // recursive method inserts backslashes before all apostrophes
    int i=0;
    try {
      i = oldString.indexOf('\'',0);
    }
    catch (Exception e) {
      return "";
    }
    return i<0?oldString:converter(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }
  
  boolean idNumberLooksValid(String n) {
    if (n==null) return false;
    if (n.length()==0) return false;
    if (Course.idLength==0) return true;
    if (n.length()!=Course.idLength) return false;
    try {
      int i = Integer.parseInt(n);
      if (i==0) return true;
      if (i<0) return false;
    }
    catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  boolean authenticate(String id, String pass){
    idNumber = id;
    password = pass;
    
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
            
      String sqlQueryString = "SELECT * FROM Students "
      + "WHERE StudentIDNumber='" + idNumber + "' AND (Password='" + converter(password,0) 
      + "' OR Password=SUBSTRING(PASSWORD('" + converter(password,0) + "'),1,12))";
      ResultSet rs = stmt.executeQuery(sqlQueryString);
      
      if (rs.next()) {
        log.paranoid("rs existed for: " + id,"Student:authenticate");
        lastName = rs.getString("LastName");
        firstName = rs.getString("FirstName");
        sectionID = rs.getInt("SectionID");
        email = rs.getString("Email");
        password = rs.getString("Password");
        secretQuestion = rs.getString("SecretQuestion");
        secretAnswer = rs.getString("SecretAnswer");
        status = rs.getString("Status");
        if (status.equals("Frozen")) {
          isFrozen = true;
          return false;
        }
        isAuthentic = true;
        if (status.equals("Instructor")) 
          isInstructor = true;
        if (status.equals("TA")) {
          isTA=true;
        }
        if (status.equals("Visitor"))
          isVisitor = true;
      }
      else isAuthentic = false;
      rs.close();
      stmt.close();
      conn.close();      
    } catch (Exception e) {
    }
    log.paranoid("returning: " + isAuthentic + " for " + id + " id'ed by " + pass,"Student:authenticate");
    return isAuthentic;
  }
  
  boolean isAuthenticated() {  // just checks status and returns true or false
    if (isAuthentic) return true;
    else return false;
  }

  boolean createStudentsTable (){
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("CREATE TABLE Students (StudentIDNumber VARCHAR(50) PRIMARY KEY,Password VARCHAR(12),"
      + "LastName TEXT,FirstName TEXT,Email TEXT,ShareInfo TEXT,SecretQuestion TEXT,SecretAnswer TEXT,"
      + "Status TEXT,SectionID INT)");
      stmt.executeUpdate("INSERT INTO Students VALUES ('0','visitor','Visitor','',"
      + "'visitor@eledge.com','true','Favorite activity?','Catching 4TWinks','Visitor','1')");
      stmt.executeUpdate("INSERT INTO Students VALUES ('" + idNumber + "','" 
      + converter(password) + "','" + converter(lastName) + "','" + converter(firstName) 
      + "','" + converter(email) + "','" + shareInfo + "','" + converter(secretQuestion)
      + "','" + converter(secretAnswer) + "','Instructor','1')");
	  return true;
    }
    catch (Exception e) {
      return false;	
    }
  }

  boolean createStudentsTable(boolean mkInstructProf) {
	  try {
	    Class.forName(Course.jdbcDriver).newInstance();
	    Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate("CREATE TABLE Students (StudentIDNumber VARCHAR(50) PRIMARY KEY,Password VARCHAR(12),"
	    + "LastName TEXT,FirstName TEXT,Email TEXT,ShareInfo TEXT,SecretQuestion TEXT,SecretAnswer TEXT,"
            + "Status TEXT,SectionID INT)");

	    stmt.executeUpdate("INSERT INTO Students VALUES ('0','visitor','Visitor','',"
            + "'visitor@eledge.com','true','Favorite activity?','Catching 4TWinks','Visitor','1')");
	    
	    if (mkInstructProf) {
              stmt.executeUpdate("INSERT INTO Students VALUES('" + idNumber + "','"
		+ converter(password) + "','" + converter(lastName) + "','"
		+ converter(firstName) + "','" + converter(email) + "','"
		+ shareInfo + "','" + converter(secretQuestion) + "','"
		+ converter(secretAnswer) + "','instructor','1'");
	    }
	    stmt.close();
	    conn.close();
	    return true;
	  } catch(Exception e) {}
	  return false;
  }
          
  String resetPassword(String studentPass, String studentID){
    try{
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName, Course.mySQLUser, Course.mySQLPass);
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("UPDATE Students SET Password='" + studentPass + "' WHERE StudentIDNumber='" + studentID + "'");
      stmt.close();
      conn.close();
    }catch(Exception e){
      return e.getMessage();
    }
    return res.getString("str_pword_changed");
  }

  int getHitCount() {
	  return hitCount;
  }

  void updateHitCount() {
	  hitCount++;
  }

  void resetHitCount() {
	  hitCount=0;
  }

  String getFirstName() {
	  return firstName;
  }

  String getLastName() {
	  return lastName;
  }

  void setFirstName(String fn) {
	  firstName=fn;
  }
  void setLastName(String ln) {
	  lastName=ln;
  }

  void setStatusInstructor() {
      status="Instructor";
      isInstructor=true;
  }

  void setIsInstructor(boolean stat) {
    isInstructor=stat;
  }

  void setTAStatus(boolean s) {
    if (s==true) {
      status="TA";
      isTA=true;
    } else {
      status="Current";
      isTA=false;
    }
  }

  public void setAuthentic(boolean auth) {
	  isAuthentic=true;
  }
 
  public String getSectionID() {
    return Integer.toString(sectionID);
  }
 
  public void setIDNumber(String id) {
	  idNumber=id;
  }

  public String getStatus() {
    return status;
  }

  public String getStatusName() {
    String sn = "str_" + status;
    log.paranoid("Attempting to get status name: " + sn + " from ResourceBundle","StudentgetStatusName");
    return res.getString(sn);
  }

  public String getEmail() {
    return this.email;
  }
  
  public String getCursos(String id) {
  	//busca los cursos asignados al estudiante
  	int NCursos = 1;
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	StringBuffer buf = new StringBuffer();
        
    	String consulta = ("SELECT * FROM students, courses, course_to_student WHERE idcourses = courses.sectionid AND idstudent = studentidnumber AND idstudent = '"+id+"' ORDER BY SectionName ASC");
     	ResultSet rsLinks = stmt.executeQuery(consulta);
      
    	while (rsLinks.next()) {
        	buf.append("\n<tr><td><a href= "+Course.name+".Home?curso="+rsLinks.getString("courses.SectionId")+">"+NCursos+". "+rsLinks.getString("courses.SectionName")+"</a></td></tr>");
      		buf.append("\n<tr><td> "+rsLinks.getString("Description")+"</td></tr>");
      		NCursos++;
      		}
    	return buf.toString();
       
       }
    catch(Exception e){
      return e.getMessage();
    }
     	
  }
  
  boolean isRegisteredCourse(String id, int idcurso) {
  	StringBuffer buf = new StringBuffer();
  	//busca los cursos asignados al estudiante
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
    	String consulta = ("SELECT  * FROM students, courses, course_to_student WHERE idcourses = courses.sectionid AND idstudent = studentidnumber AND idstudent =  '"+id+"' and idcourses = "+idcurso+" ORDER  BY SectionName ASC");
     	ResultSet rsLinks = stmt.executeQuery(consulta);
      
    	
    	if (rsLinks.next()) isRegistered = true;
      	else isRegistered = false;
      	
      	   	       
       }
    catch(Exception e){
      
    }
    
    return isRegistered;  	
  }
  
boolean isTeacherRegisteredCourse(String id, int idcurso) {
  	StringBuffer buf = new StringBuffer();
  	//busca los cursos asignados al estudiante
  	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
    	String consulta = ("SELECT  * FROM students, courses, course_to_teacher WHERE idcourses = courses.sectionid AND idstudent = studentidnumber AND idstudent =  '"+id+"' and idcourses = "+idcurso+" ORDER  BY SectionName ASC");
     	ResultSet rsLinks = stmt.executeQuery(consulta);
      
    	
    	if (rsLinks.next()) isRegistered = true;
      	else isRegistered = false;
      	
      	   	       
       }
    catch(Exception e){
      
    }
    
    return isRegistered;  	
  }
  
  public void setTAtoStudent(boolean isStudent) {
  	this.TAtoStudent = isStudent;
  }
  
  boolean getTAtoStudent() {
  	return this.TAtoStudent;
  }
  
  public void setCourse_id(String Course_id) {
  	this.course_id = Course_id;
  }
  
  String getCourse_id() {
  	return this.course_id;
  }
  public void setTheme_id(int Theme_id) {
  	this.theme_id = Theme_id;
  }
  
  int getTheme_id() {
  	return this.theme_id;
  }
  
}
