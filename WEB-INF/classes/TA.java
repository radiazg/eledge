package Eledge;

/** This java class handles the fundamental data manipulation, etc.of ta's
 *  Up to this point, TA's have had -exactly- the same functionality as 
 *  instructors, which is fine, they will continue to have -some- of the
 *  same functionality, but will no longer be classified exactly as teachers.
 *  Instead, we will make it so that TA's may be assigned to a student,
 *  a set of students, one or more sections, or a combination.
 *  They will be given access to most of the functionality of the gradebook...
 *  but only for the students to whom they have access. 
 *  Other than that...? 
 *  Author: Robert Zeigler
 *  Started: 7/30/03
 *  Last Modified: 8/1/03
 */
import java.util.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.text.MessageFormat;
public class TA {
  private boolean isTA=false;//for quick checks on whether the heck this person is a ta or not.
  private boolean isDefaultProfile=false;//for whether or not this profile is a "default"
  private boolean studentsLoaded=false;                                    //profile. 
  private boolean sectionsLoaded=false;
  private Student stud = null;//this allows us to easily reference the "parent" of this class.
  private HashMap taPerms;
  private String[] assignedSections;
  private Student[] assignedStudents;
  private Logger log = new Logger();
  public static final String ASSIGNMENT_STUDENT="Student";
  public static final String ASSIGNMENT_SECTION="Section";

  public TA(String studentID) {
    stud = new Student();
    if (isDefaultProfile(studentID)) {
      stud.setIDNumber(studentID);
      stud.setAuthentic(true);
      isTA=true;
      isDefaultProfile=true;
    } else {
      if (lookupTA(studentID)) {
        isTA=true;
      }
      stud.loadStudent(studentID);
    }
    setUpTA();
  }

  public TA(String id, boolean isDefault, boolean isNew) {
    stud = new Student();
    if (isDefault) {
        stud.setIDNumber(id);
        isTA=true;
        isDefaultProfile=true;
        stud.setAuthentic(true);
    } else {
      stud.loadStudent(id);
      if (lookupTA(id)) {
        isTA=true;
      }
    }
    if (!isNew) {
      setUpTA();
    }
  }

  public TA(boolean status, Student s) {
    stud = s;
    isTA=status;
    setUpTA();
  }

  private void setUpTA() {
    if (isTA) {
      log.paranoid("isTA==true","TA:setUpTA");
      if (stud != null) {
	log.paranoid("stud not null","TA:setUpTA");
        loadTAPermissions(); 
      }
    }
  }

  public void setIsTA(boolean status) {
    isTA=status;
  }

  public boolean getIsTA() {
    return isTA;
  }


  public TAPermission getPermission(String key) {
    if (taPerms != null) {
      return (TAPermission)taPerms.get(key);
    }
    return null;
  }

  public void setPermission(TAPermission value) {
    if (taPerms == null) {
      taPerms = new HashMap();
    }
    taPerms.put(value.getName(), value);
  }

  public String updatePermissionTable() {
    String[] keys = Permissions.getKeys();
    //not a valid ta for updating.
    if (stud==null || stud.getIDNumber()==null || stud.getIDNumber().equals("") || stud.getIDNumber().equals(""))
      return "";

    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "UPDATE TAPermissions SET ";
      for (int i=0;i<keys.length;i++) {
        TAPermission tap = (TAPermission)taPerms.get(keys[i]);
        sql+=keys[i] + "='" + tap.getPermissionLevel() + "',";
      }
      sql += "isDefault='" + isDefaultProfile + "' WHERE StudentIDNumber='" + stud.getIDNumber() + "'";
      log.paranoid("Executing: " + sql,"Ta:updatePermissionTable");
      stmt.executeUpdate(sql);
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:updatePermissionTable",e);
      return e.getMessage();
    }
    return "";
  }

  public String insertPermissions() {
    String[] keys = Permissions.getKeys();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "INSERT INTO TAPermissions (StudentIDNumber,";
      String sql2= " VALUES('" + stud.getIDNumber() + "','"; 
      log.paranoid("Appended: " + sql2 + " to sql2.","TA:insertPermissions");
      for (int i=0;i<keys.length;i++) {
	log.paranoid("Updating: " + keys[i] + " (index: " + i + ")","TA:insertPermissions");
        TAPermission tap = (TAPermission)taPerms.get(keys[i]);
	log.paranoid("tap.getPermissionLevel() yields: " + tap.getPermissionLevel(),"TA:insertPermissions");
        sql+=keys[i] + ",";
        sql2+=tap.getPermissionLevel() + "','";
      }
      sql2 += isDefaultProfile + "')";
      sql += "isDefault)" + sql2;
      log.paranoid("Executing: " + sql,"TA:insertPermissions");
      stmt.executeUpdate(sql);
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:insertPermissions",e);
      return e.getMessage();
    }
    return "";
  }

  public void setupNewTA() {
    String[] keys = Permissions.getKeys();
    if (taPerms==null)
      taPerms = new HashMap();
    else
      taPerms.clear();
    //if the "isTA" flag is set... this is actually -NOT- a new ta...
    if (isTA) {
      if (loadTAPermissions()) {
        return;
      }
    }

    for (int i=0;i<keys.length;i++) {
      log.paranoid("Attempting to add tap for key: " + keys[i] + " for " + stud.getIDNumber(),"TA:setupNewTA()");
      TAPermission tap = new TAPermission(stud.getIDNumber(),keys[i],TAPermission.PERM_ALL);
      log.paranoid("After creation of tap, perm is: " + tap.getPermissionLevel(),"TA:setupNewTA()");
      taPerms.put(keys[i],tap);
    }
    isDefaultProfile=false;
    isTA=true;
    insertPermissions(); 
    addAssignment("Student",stud.getIDNumber());
  }

  public boolean lookupTA(String id, boolean usePermissionTable) {
    boolean ret = false;
    if (!usePermissionTable) {
      return lookupTA(id);
    }
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT StudentIDNumber FROM TAPermissions WHERE StudentIDNumber='" + id + "'");
      if (rs.next()) {
         ret = true;
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      ret=false;
      log.normal("Caught: " + e.getMessage(),"TA:lookupTA");
    }
    return ret;
  }
 

  public boolean lookupTA(String id) {
    boolean ret = false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT Status FROM Students WHERE StudentIDNumber='" + id + "'");
      if (rs.next()) {
       if (rs.getString("Status").equals("TA"))
         ret = true;
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      ret=false;
      log.normal("Caught: " + e.getMessage(),"TA:lookupTA");
    }
    return ret;
  }
  
  public boolean loadTAPermissions() {
    boolean ret = false;
    if (stud==null) {
      log.paranoid("Student was null!","TA:loadTAPermissions");
      return ret;
    }
    String[] keys = Permissions.getKeys();
    if (taPerms == null)
      taPerms = new HashMap();
    else
      taPerms.clear();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT * FROM TAPermissions WHERE StudentIDNumber='" + stud.getIDNumber() + "'";
      log.paranoid("Executing: " + sql,"TA:loadTAPermissions");
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        for (int i=0;i<keys.length;i++) {
	  log.paranoid("rs.getString(keys[i]) yields: " + rs.getString(keys[i]),"TA:loadTAPermissions");
          TAPermission p = new TAPermission(stud.getIDNumber(),keys[i],rs.getString(keys[i]));
	  log.paranoid("Loading key " + keys[i] + " with value: " + p.getPermissionLevel() + "for " + stud.getIDNumber(),"TA:loadTAPermissions");
          taPerms.put(keys[i],p);
        }
        ret=true;
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:loadTAPermissions");
      ret=false;
      createTATables();
    }
    return ret;
  }

  public String addAssignment(String type, String value) {
    if (stud==null) return "";
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "INSERT INTO TAAssignments (StudentIDNumber, Type, Value) VALUES('"
        + stud.getIDNumber() + "','" + type + "','" + value + "')";
      log.paranoid("Executing: " + sql,"TA:addAssignment");
      stmt.executeUpdate(sql);
      stmt.close();
      con.close();
      if (type.equals("Section")) {
        addSectionStudents(value);
        this.sectionsLoaded=false;
      } else
        this.studentsLoaded=false;
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:addAssignment");
      return e.getMessage();
    }
    return "";
  }

  private void addSectionStudents(String value) {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT StudentIDNumber FROM Students WHERE "
      + "SectionID = '" + value + "'";
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
	String studID = rs.getString("StudentIDNumber");
	Student s = new Student();
	s.loadStudent(studID);
	//only add the students in the section if:
	//1) they aren't the visitor.
	//2) they aren't already assigned.
	//3) They aren't another TA (-OR- if they -are- a TA, but this student is themselves..
        if (!this.isAssigned("Student",studID) 
	    && !studID.equals("0") 
	    && !s.getIsInstructor() 
	    && (!s.getIsTA() || stud.getIDNumber().equals(studID))) {
          addAssignment("Student",rs.getString("StudentIDNumber"));
        }
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:addSectionStudents");
    }
  }

  private void dropSectionStudents(String value) {
    if (stud==null)
      return;
    if (!stud.isAuthenticated())
      stud.setAuthentic(true);
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql="SELECT * FROM Students WHERE SectionID='" + value + "' AND StudentIDNumber<>'" + stud.getIDNumber() + "'";
      log.paranoid("Executing: " + sql,"TA:dropSectionStudents");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
         sql="DELETE FROM TAAssignments WHERE StudentIDNumber='"
          + stud.getIDNumber() + "' AND Type='Student' AND Value='"
          + rs.getString("StudentIDNumber") + "'";
        log.paranoid("Executing: " + sql,"TA:dropSectionStudents");
        stmt.executeUpdate(sql);
      }
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:addSectionStudents");
    }
  }

  public Student[] getAssignedStudents() {
    if (stud==null) 
      return null;
    if (this.studentsLoaded)
      return this.assignedStudents;

    Vector students = new Vector();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT * FROM TAAssignments WHERE StudentIDNumber='"
        + stud.getIDNumber()+ "' AND Type='Student' ORDER BY Value";
      log.paranoid("Exeucting: " + sql,"TA:getStudentAssignments");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        Student student = new Student();
        student.loadStudent(rs.getString("Value"));
        students.add(student);
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:getStudentAssignments");
      return null;
    }
    this.assignedStudents = (Student[])students.toArray(new Student[]{});
    this.studentsLoaded = true;
    return this.assignedStudents;
  }

  public Student[] getUnAssignedStudents() {
    if (stud==null) 
      return null;
    Vector students = new Vector();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT Students.StudentIDNumber, TAAssignments.Value from "
        + "Students LEFT JOIN TAAssignments ON (TAAssignments.Value=Students.StudentIDNumber "
        + "AND TAAssignments.StudentIDNumber='" + stud.getIDNumber() 
        + "') WHERE TAAssignments.Value IS NULL AND Students.StudentIDNumber<>"
        + "'0' AND (Students.Status='Current' OR (Students.Status='TA' AND Students.StudentIDNumber='" 
	+ stud.getIDNumber() + "')) ORDER BY Students.StudentIDNumber";
      log.paranoid("Execucting: " + sql,"TA:getUnassigned");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        Student student = new Student();
        student.loadStudent(rs.getString("StudentIDNumber"));
        students.add(student);
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:getUnassigned");
      return null;
    }
    return (Student[])students.toArray(new Student[]{});
  }

  public String[] getAssignedSections() {
    if (stud==null) return null;
    if (this.sectionsLoaded) 
      return this.assignedSections;
    Vector sections = new Vector();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT * FROM TAAssignments WHERE StudentIDNumber='"
        + stud.getIDNumber()+ "' AND Type='Section' ORDER BY Value";
      log.paranoid("Exeucting: " + sql,"TA:getSectionAssignments");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        sections.add(rs.getString("Value"));
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:getSectionAssignments");
      return null;
    }
    this.sectionsLoaded=true;
    this.assignedSections=(String[])sections.toArray(new String[]{});
    return this.assignedSections;
  }

  public String[] getUnAssignedSections() {
    if (stud==null) return null;
    Vector sections = new Vector();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      //This statement will pull out all SectionID's from CourseSections
      //for which the given ta does not have an appropriate value in
      //the TAAssignments table.
      String sql = "SELECT CourseSections.SectionID, TAAssignments.Value "
        + "FROM CourseSections LEFT JOIN TAAssignments ON "
        + "(CourseSections.SectionID=TAAssignments.Value AND "
        + "TAAssignments.StudentIDNumber='" + stud.getIDNumber() 
        + "') WHERE TAAssignments.Value IS NULL ORDER BY CourseSections.SectionID";
      log.paranoid("Exeucting: " + sql,"TA:getSectionAssignments");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        sections.add(rs.getString("SectionID"));
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:getSectionAssignments");
      return null;
    }
     return (String[])sections.toArray(new String[]{});
  }

  public String removeAssignment(String type, String value) {
    if (stud==null) {
      log.paranoid("Err: student null!","TA:removeAssignment");
      return "";
    }
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "DELETE FROM TAAssignments WHERE StudentIDNumber='" 
        + stud.getIDNumber() + "' AND Type='" + type + "' AND Value='" + value + "'";
      log.paranoid("Executing: " + sql,"TA:removeAssignment");
      stmt.executeUpdate(sql);
      stmt.close();
      con.close();
      if (type.equals("Section")) {
        this.dropSectionStudents(value);
        this.studentsLoaded=false;
        this.sectionsLoaded=false;
      } else
        this.studentsLoaded=false;

    } catch (Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:removeAssignment");
      return e.getMessage();
    }
    return "";
  }

  public boolean isAssigned(String type, String value) {
    if (stud==null) return false;
    boolean ret = false;
    Object[] assignees;
    if (type.equals("Student")) {
      assignees = getAssignedStudents();
    } else {
      assignees = getAssignedSections();
    }

    for (int i=0;i<assignees.length;i++) {
      if (type.equals("Student")) {
        if (((Student)assignees[i]).getIDNumber().equals(value)) {
          ret = true;
        }
      } else {
        if (((String)assignees[i]).equals(value)) {
          ret = true;
        }
      }
    }
    return ret;
  }

  public Object getAssignment(String type, String value) {
    if (stud==null) return null;
    Object ret = null;
    Object[] assignees;
    if (type.equals("Student")) {
      assignees = getAssignedStudents();
    } else {
      assignees = getAssignedSections();
    }

    for (int i=0;i<assignees.length;i++) {
      if (type.equals("Student")) {
        if (((Student)assignees[i]).getIDNumber().equals(value)) {
          ret = assignees[i];
        }
      } else {
        if (((String)assignees[i]).equals(value)) {
          ret = assignees[i];
        }
      }
    }
    return ret;
  }

  private boolean createTATables() {
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      stmt.executeUpdate("CREATE TABLE TAPermissions (StudentIDNumber VARCHAR(50) PRIMARY KEY, isDefault VARCHAR(5))");
      stmt.executeUpdate("CREATE TABLE TAAssignments (StudentIDNumber VARCHAR(50),"
        + "Type VARCHAR(7), Value VARCHAR(50)");
      stmt.close();
      con.close();
    } catch (Exception e) {
      log.normal("Caught: " + e.getMessage(),"TA:createTATables");
    }
    return false;
  }
  
  public boolean isDefaultProfile() {
    return isDefaultProfile;
  }

  public static boolean isDefaultProfile(String id) {
    Logger l = new Logger();
    boolean ret = false;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT isDefault FROM TAPermissions WHERE StudentIDNumber='" + id + "'";
      ResultSet rs = stmt.executeQuery(sql);
      if (rs.next()) {
        ret = rs.getBoolean("isDefault");
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      ret = false;
    }
    return ret;
  }
  
  public void setIsDefaultProfile(boolean flag) {
    isDefaultProfile=flag;
  }

  //we use Student student to ensure that, if something temporarily
  //modifies the student object to have a teacher status,
  //the correct student object will be modified
  public synchronized boolean hasPermission(String servlet, HttpServletRequest request, Student student, StringBuffer error) {
    boolean ret = false;
    String permName;
    Permission p;
    TAPermission tap;
    String ur = request.getParameter("UserRequest");
    log.paranoid("ur: " + ur,"TA:hasPermission");
    //make sure they are not initially set as instructors.
    student.setIsInstructor(false);
    if (ur==null)
      permName=new String(servlet);
    else
      permName = new String(servlet + "_" + ur);
    log.paranoid("perm name: " + permName,"TA:hasPermission");

    p = Permissions.getPermission(permName);
    if (p==null)//default action if no permission found: permission DENIED!!!
      return false;

    tap =  (TAPermission) taPerms.get(permName);
    if (tap == null) {
      error.append(p.getDenyMsg());
      return false;
    }
    log.paranoid("tap permission: " + tap.getPermissionLevel(),"TA:hasPermission");
    if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)) {
      error.append(p.getDenyMsg());
      return false;
    }

    if (tap.getPermissionLevel().equals(TAPermission.PERM_ALL)) {
      student.setIsInstructor(true);
      return true;
    }

    if (tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT)) {
      return true;
    }

    //ok, if the permission level is neither of the above, 
    //it must be "Conditional"... so, let's go. ;)
    MessageFormat sql = p.getSql();
    log.paranoid("sql: " + p.getSqlAsString(),"TA:hasPermission");
    Object[] args = new Object[p.getNumArgs()];
    log.paranoid("num args: " + p.getNumArgs(),"TA:hasPermission");
    String[] argNames = p.getArgs();//these are the "names" of the arguments...
                                   //ie, the request parameters that we need
                                   //to look for.
    log.paranoid("Begin for...'","TA:hasPermission");
    for (int i=0;i<argNames.length;i++) {
        //TASTUDENTIDID is a special keyword for our purposes, which denotes
        //the idNumber of the TA.
      if (argNames[i].equals("TASTUDENTID")) {
        args[i]=student.getIDNumber();
      } else {
        try {
          args[i] = request.getParameter(argNames[i]);
        } catch (Exception e) {
          log.sparse("Caught: " + e.getMessage(),"TA:hasPermission",e);
        }
      }
      log.paranoid("argName was: " + argNames[i],"TA:hasPermission");
      log.paranoid("arg is: " + args[i],"TA:hasPermission");
    }
    //now we get to the fun part... ie, the sql part. Heh. ;)
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      log.paranoid("Executing: " + sql.format(args),"TA:hasPermission");
      ResultSet rs = stmt.executeQuery(sql.format(args));
      if (rs.next()) {
        ret=true;
        if (p.getRequireTeacherStatus()) {
          student.setIsInstructor(true);
        } else
          student.setIsInstructor(false);
      } else {
        ret=false;
        student.setIsInstructor(false);
	error.append(p.getDenyMsg());
      }
      rs.close();
      stmt.close();
      con.close();
    } catch(Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:hasPermission");
      error.append(e.getMessage());
      ret=false;
    }
    log.paranoid("Returning: " + ret,"TA:hasPermission");
    return ret;
  }

  //2nd, "optional" hasPermission.
  public boolean hasPermission(String servlet, String request, Student student, StringBuffer error,HashMap parameters) {
    boolean ret = false;
    String permName;
    Permission p;
    TAPermission tap;
    if (request==null || request.equals(""))
      permName=new String(servlet);
    else
      permName = new String(servlet + "_" + request);
    log.paranoid("perm name: " + permName,"TA:hasPermission");

    p = Permissions.getPermission(permName);
    if (p==null)//default action if no permission found: permission DENIED!!!
      return false;

    tap =  (TAPermission) taPerms.get(permName);
    if (tap == null) {
      error.append(p.getDenyMsg());
      return false;
    }
    log.paranoid("tap permission: " + tap.getPermissionLevel(),"TA:hasPermission");
    if (tap.getPermissionLevel().equals(TAPermission.PERM_NONE)) {
      error.append(p.getDenyMsg());
      return false;
    }

    if (tap.getPermissionLevel().equals(TAPermission.PERM_ALL)) {
      student.setIsInstructor(true);
      return true;
    }

    if (tap.getPermissionLevel().equals(TAPermission.PERM_STUDENT)) {
      return true;
    }

    //ok, if the permission level is neither of the above, 
    //it must be "Conditional"... so, let's go. ;)
    MessageFormat sql = p.getSql();
    log.paranoid("sql: " + p.getSqlAsString(),"TA:hasPermission");
    Object[] args = new Object[p.getNumArgs()];
    log.paranoid("num args: " + p.getNumArgs(),"TA:hasPermission");
    String[] argNames = p.getArgs();//these are the "names" of the arguments...
                                   //ie, the request parameters that we need
                                   //to look for.
    log.paranoid("Begin while...'","TA:hasPermission");
    for (int i=0;i<argNames.length;i++) {
        //TASTUDENTIDID is a special keyword for our purposes, which denotes
        //the idNumber of the TA.
      if (argNames[i].equals("TASTUDENTID")) {
        args[i]=student.getIDNumber();
      } else {
        args[i] = parameters.get(argNames[i]);
      }
      log.paranoid("argName was: " + args[i],"TA:hasPermission");
      log.paranoid("arg is: " + args[i],"TA:hasPermission");
    }
    //now we get to the fun part... ie, the sql part. Heh. ;)
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      log.paranoid("Executing: " + sql.format(args),"TA:hasPermission");
      ResultSet rs = stmt.executeQuery(sql.format(args));
      if (rs.next()) {
        ret=true;
        if (p.getRequireTeacherStatus()) {
          student.setIsInstructor(true);
        } else
          student.setIsInstructor(false);
      } else {
        ret=false;
        student.setIsInstructor(false);
	error.append(p.getDenyMsg());
      }
      rs.close();
      stmt.close();
      con.close();
    } catch(Exception e) {
      log.sparse("Caught: " + e.getMessage(),"TA:hasPermission");
      error.append(e.getMessage());
      ret=false;
    }
    log.paranoid("Returning: " + ret,"TA:hasPermission");
    return ret;
  }

  public static String[] getTAS() {
    Vector studs = new Vector();
    Logger l = new Logger();
    String[] ret;
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT TAPermissions.StudentIDNumber, Students.Status, TAPermissions.isDefault  FROM TAPermissions LEFT JOIN Students USING(StudentIDNumber) WHERE TAPermissions.isDefault='true' OR Students.Status='TA' ORDER BY StudentIDNumber";
      l.paranoid("Executing: " + sql,"TA:getTAS");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        studs.add(rs.getString("StudentIDNumber"));
      }
      rs.close();
      stmt.close();
      con.close();
      ret = (String[])studs.toArray(new String[]{});
    } catch (Exception e) {
      l.sparse("Caught: " + e.getMessage(),"TA:getTAS",e);
      return null;
    }
    return ret;
  }

  String getID() {
    if (stud==null)
      return "";
    return stud.getIDNumber();
  }

  public static String[] getDefaultTAs() {
    Vector studs = new Vector();
    Logger l = new Logger();
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection con = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = con.createStatement();
      String sql = "SELECT StudentIDNumber FROM TAPermissions WHERE isDefault='false' ORDER BY StudentIDNumber";
      l.paranoid("Executing: " + sql,"TA:getDefaultTAs");
      ResultSet rs = stmt.executeQuery(sql);
      while (rs.next()) {
        studs.add(rs.getString("StudentIDNumber"));
      }
      rs.close();
      stmt.close();
      con.close();
    } catch (Exception e) {
      l.sparse("Caught: " + e.getMessage(),"TA:getDefaultTAs",e);
      return null;
    }
    return (String[])studs.toArray(new String[]{});
  }
}
