package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;

public class Install extends HttpServlet {

public String getServletInfo() {
    return "This Eledge class composes a uniform page look, feel and navigation for servlets.";
  }
  
  
public void doGet(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
     try {
	 	Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
           
        stmt.executeUpdate("DROP DATABASE Eledge");
        stmt.executeUpdate("CREATE DATABASE Eledge");
        stmt.executeUpdate("CREATE TABLE WebPages (Section INT,PageID TEXT,Heading TEXT,HTMLText TEXT,CourseSection VARCHAR(3))");
        stmt.executeUpdate("CREATE TABLE CourseParameters (Name TEXT,Value TEXT)");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('Title','Ing.Sistemas')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AllowNewProfiles','true')");
	stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AllowProfileEditing','true')");
        //stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NumberOfSections','1')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgLink','http://www.unab.edu.co')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgSrc','http://localhost:8080/unab.gif')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('ImgAlt','Universidad Autónoma de Bucaramanga')");
        //stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AnchorTop','<a href=http://www.unab.edu.co>Universidad Autónoma de Bucaramanga</a>')");
        //stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AnchorBottom','<a href=http://www.utah.edu/disclaimer/>Disclaimer</a>')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NavCellColor','#FFCC33')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('BgColor','#FFFFFF')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('AlinkColor','#FF0000')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('VlinkColor','#800080')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('LinkColor','#0000FF')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('NavBgImg', 'none')");
        stmt.executeUpdate("INSERT INTO CourseParameters VALUES ('Basefont', 'Arial')");
        //stmt.executeUpdate("CREATE TABLE CourseSections (SectionID INT PRIMARY KEY AUTO_INCREMENT,SectionName TEXT,Description TEXT)");
        //stmt.executeUpdate("INSERT INTO CourseSections (SectionID,SectionName,Description) VALUES (1,'1','Default Section')");
        stmt.executeUpdate("CREATE TABLE navbarlink (linkid int(11) NOT NULL auto_increment,"
        + "name varchar(150) NOT NULL default '', url varchar(50) NOT NULL default '',"
        + "iconurl varchar(150) NOT NULL default '', PRIMARY KEY  (linkid),"
        + "UNIQUE KEY name (name,url)) TYPE=MyISAM AUTO_INCREMENT=28 ;");
        
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (1, 'myEledge', 'Eledge.Home?curso=0', '');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (2, 'Mi Perfil', 'Eledge.Profile', '<img src=../Images/icons/profile.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (3, 'Agenda', 'Eledge.Calendar', '<img src=../Images/icons/calendario.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (4, 'Contenido', 'Eledge.Content', '<img src=../Images/icons/contenido.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (5, 'Foro', 'Eledge.DiscussionBoard', '<img src=../Images/icons/foro.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (6, 'Correo', 'Eledge.Email', '<img src=../Images/icons/mail.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (7, 'Examen', 'Eledge.Exam', '<img src=../Images/icons/exam.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (8, 'Ayuda', 'Eledge.Help', '<img src=../Images/icons/help.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (14, 'Login', 'Eledge.Login', '<img src=../Images/icons/login.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (19, 'Logout', 'Eledge.Logout', '<img src=../Images/icons/logout.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (17, 'Puntuaciones', 'Eledge.Scores', '<img src=../Images/icons/scores.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (18, 'Instrucciones', 'Eledge.Syllabus', '<img src=../Images/icons/syllabus.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (16, 'Mis Cursos', 'Eledge.Cursos', '<img src=../Images/icons/cursos.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (20, 'Inicio', 'Eledge.Home', '<img src=../Images/icons/home.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (21, 'Preferencias', 'Eledge.ManageCourse', '<img src=../Images/icons/preferencias.gif align=absmiddle>');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (22, 'Permisos', 'Eledge.ManageTAPermissions', '<img src=../Images/icons/permisos.gif align=absmiddle >');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (23, 'UNAB', 'http://www.unab.edu.co', '');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (24, 'UNAB Virtual', 'http://www.unabvirtual.edu.co', '');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (25, 'Portal del Estudiante', 'http://estudiantes.unab.edu.co', '');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (26, 'Correo', 'http://bumanga.unab.edu.co', '');");
		stmt.executeUpdate("INSERT INTO navbarlink VALUES (27, 'Usuarios', 'Eledge.Usuarios', '<img src=../Images/icons/profesores.gif align=absmiddle >');");
    
        //mas tablas
        
  stmt.executeUpdate("CREATE TABLE content ( content_id int(11) NOT NULL auto_increment, theme int(11) NOT NULL default '0',"
  + "page_id varchar(50) NOT NULL default '', title text NOT NULL, description text NOT NULL, coursesection varchar(50) NOT NULL default '',"
  + "PRIMARY KEY  (content_id)) TYPE=MyISAM"); 

  stmt.executeUpdate("CREATE TABLE contenttotheme (id_tc int(11) NOT NULL auto_increment, coursesection_id varchar(50) NOT NULL default '',"
  + "id_theme int(11) NOT NULL default '0', title text NOT NULL, body text NOT NULL, id_sec int(11) NOT NULL default '0', PRIMARY KEY  (id_tc) ) TYPE=MyISAM");
  
  stmt.executeUpdate("CREATE TABLE course_to_student (id int(11) NOT NULL auto_increment, idcourses int(11) NOT NULL default '0',"
  + "idstudent varchar(50) NOT NULL default '', PRIMARY KEY  (id) ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE course_to_teacher ( id int(11) NOT NULL auto_increment, idcourses int(11) NOT NULL default '0',"
  + "idstudent varchar(50) NOT NULL default '', PRIMARY KEY  (id) ) TYPE=MyISAM");
  
  stmt.executeUpdate("CREATE TABLE courses ( SectionID int(11) NOT NULL auto_increment, SectionName text, Description text,"
  + "PRIMARY KEY  (SectionID) ) TYPE=MyISAM");
  
  stmt.executeUpdate("CREATE TABLE discussionboardentries (ID int(11) NOT NULL auto_increment, StudentIDNumber varchar(50) default NULL,Date timestamp(14) NOT NULL,"
  + "Subject varchar(50) default NULL, Message text, MainThreadID int(11) default NULL, PreviousMessageID int(11) default NULL,"
  + "NextMessageID int(11) default NULL, SubThreadID int(11) default NULL, PreviousThreadID int(11) default NULL,"
  + "NextThreadID int(11) default NULL, ForumID int(11) default '1', UseHTML varchar(5) default NULL,"
  + "PRIMARY KEY  (ID) ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE discussionboardforums ( ID int(11) NOT NULL auto_increment, Name varchar(30) default NULL,"
  + " AllowStudentThreads varchar(5) default NULL, AllowStudentDeletes varchar(5) default NULL, CourseSection char(3) default NULL,"
  + " PRIMARY KEY  (ID) ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE essays ( Code int(11) default NULL, QuestionID int(11) default NULL, Graded varchar(5) default NULL,"
  + " Answer text, StudentIDNumber varchar(50) default NULL, TestType varchar(8) default NULL, Score int(11) default NULL,"
  + " id_course int(11) default NULL ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE events ( EventID int(11) NOT NULL auto_increment, Sdate datetime default NULL, Edate datetime default NULL,"
  + " User varchar(50) default NULL, Description text, Notes text, Flagged int(11) default NULL, Section char(3) default '1',"
  + " PRIMARY KEY (EventID) ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE examassignedquestions ( Code int(11) default NULL, QuestionID int(11) default NULL, Graded varchar(5) default NULL,"
  + " StudentAnswer text ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE examinfo ( AssignmentNumber int(11) NOT NULL auto_increment, id_course int(11) NOT NULL default '0',"
  + "Title text, SubjectAreas int(11) default NULL, NQuestionsPerSubjectArea int(11) default NULL, Deadline1 datetime default NULL,"
  + "Available1 datetime default NULL, PRIMARY KEY  (AssignmentNumber) ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE examparameters ( id_course int(11) NOT NULL default '0',"
  + "TimeLimit int(11) default NULL, WaitForNewDownload int(11) default NULL, EnforceDeadlines varchar(5) default NULL, AllowMultipleTries varchar(5) default NULL,"
  + "ScrambleQuestions varchar(5) default NULL, AllowWorkAhead varchar(5) default NULL, ShowMissedQuestions varchar(5) default NULL,"
  + "UseSectionDeadlines varchar(5) default NULL, TrackAnswers varchar(5) default NULL ) TYPE=MyISAM;");
  
  stmt.executeUpdate("CREATE TABLE examquestions ( QuestionID int(11) NOT NULL auto_increment, AssignmentNumber int(11) default NULL,"
  + "SubjectArea int(11) default NULL, QuestionText text, QuestionType text, NumberOfChoices int(11) default NULL,"
  + "ChoiceAText text, ChoiceBText text, ChoiceCText text, ChoiceDText text, ChoiceEText text, RequiredPrecision double default NULL,"
  + "CorrectAnswer1 text, CorrectAnswer2 text, QuestionTag text, PointValue int(11) default NULL, Section char(3) default 'All',"
  + "PRIMARY KEY  (QuestionID) ) TYPE=MyISAM");

stmt.executeUpdate("CREATE TABLE examtransactions ( StudentIDNumber varchar(50) default NULL, LastName text,"
+ "FirstName text, AssignmentNumber int(11) default NULL, Date timestamp(14) NOT NULL, Code int(11) default NULL,"
+ "StudentScore int(11) default NULL, PossibleScore int(11) default NULL, ElapsedMinutes int(11) default NULL,"
+ "IPNumber varchar(15) default NULL ) TYPE=MyISAM;");

stmt.executeUpdate("CREATE TABLE files ( filesid int(11) NOT NULL auto_increment, name varchar(50) NOT NULL default '',"
+ "size bigint(11) NOT NULL default '0', date date NOT NULL default '0000-00-00', path text NOT NULL,"
+ "idcourse varchar(10) NOT NULL default '', PRIMARY KEY  (filesid) ) TYPE=MyISAM COMMENT='Administra los archivos';");

stmt.executeUpdate("CREATE TABLE loggingparameters ( LogFile varchar(100) default NULL,"
+ "LogLevel int(11) default NULL ) TYPE=MyISAM;");

stmt.executeUpdate("CREATE TABLE navbar_to_course ( id int(11) NOT NULL auto_increment,"
+ "id_course int(11) NOT NULL default '0', id_navbarlink int(11) NOT NULL default '0',"
+ "PRIMARY KEY  (id) ) TYPE=MyISAM;");

stmt.executeUpdate("CREATE TABLE scores ( ScoreID int(11) NOT NULL auto_increment, StudentIDNumber varchar(50) default NULL,"
+ "Assignment text, Score int(11) default NULL, Timestamp timestamp(14) NOT NULL, IPAddress varchar(15) default NULL,"
+ "Code int(11) default NULL, TestType varchar(8) default NULL, id_course int(11) default NULL, PRIMARY KEY  (ScoreID) ) TYPE=MyISAM;");

stmt.executeUpdate("CREATE TABLE students ( StudentIDNumber varchar(50) NOT NULL default '', Password varchar(12) default NULL,"
+ "LastName text, FirstName text, Email text, ShareInfo text, SecretQuestion text, SecretAnswer text,"
+ "Status text, SectionID int(11) default NULL, PRIMARY KEY  (StudentIDNumber) ) TYPE=MyISAM;");

stmt.executeUpdate("INSERT INTO `students` VALUES ('admin', '123456', 'admin', 'admin', 'admin@127.0.0.1', 'true', '1', '2', 'Instructor', 1);");

/*examinar y analizar esta tabla */

stmt.executeUpdate("CREATE TABLE templateitems ( ID int(11) default NULL, Name varchar(50) default NULL,"
+ "Type int(11) default '2', Location int(11) default '1', PreText text, PostText text, DefaultValue text ) TYPE=MyISAM;");

stmt.executeUpdate("INSERT INTO templateitems VALUES (0, 'PrinterFriendlyLink', 1, 3, '', '', 'false');");

/*examinar y analizar esta tabla */
stmt.executeUpdate("CREATE TABLE templatepages ( PageID text, PrinterFriendlyLink varchar(5) default 'false' ) TYPE=MyISAM;");

stmt.executeUpdate("INSERT INTO templatepages VALUES ('Home', 'false');");

stmt.executeUpdate("CREATE TABLE templatesections ( PageID text, Section int(11) default NULL ) TYPE=MyISAM;");

stmt.executeUpdate("CREATE TABLE viewedmessages ( StudentIDNumber varchar(50) default NULL, ID int(11) default NULL ) TYPE=MyISAM;");
        
   } catch (Exception e) {
       out.println("Caught: " + e.getMessage());
       e.printStackTrace(System.out);
    }
       
    out.println(PaginaPermisos());

  }
  
public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws ServletException, IOException {

    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    String userRequest = request.getParameter("UserRequest");
    
    if (userRequest.equals("permisos"))
    {
    	out.println(permisos());
    }
    else {
    	out.println("vamos mal");
    }
    }
  
private static String permisos(){
  	
  	StringBuffer buf = new StringBuffer();
        
        try {
        Class.forName(Course.jdbcDriver).newInstance();
        Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
        Statement stmt = conn.createStatement();
      
        
        try {
         buf.append("Creating new tables TAPermissions, TAAssignments, Permissions, PermissionArguments (ignore 'table exists' type errors here)");
         stmt.executeUpdate("CREATE TABLE TAPermissions (StudentIDNumber VARCHAR(50) PRIMARY KEY, isDefault VARCHAR(5))");
         stmt.executeUpdate("CREATE TABLE TAAssignments (StudentIDNumber VARCHAR(50), Type VARCHAR(7), Value VARCHAR(50))");
         stmt.executeUpdate("CREATE TABLE Permissions (Name VARCHAR(50) PRIMARY KEY, Servlet VARCHAR(50), Request VARCHAR(50), SqlFormat TEXT, NumArgs INT, DenyMsg VARCHAR(50), EditMsg VARCHAR(50), SetToTeacher VARCHAR(5))");
         stmt.executeUpdate("CREATE TABLE PermissionArguments (Name VARCHAR(50), Argument VARCHAR(50), ArgNum INT)");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         buf.append("Updating ReviewQuestions table (you can ignore 'table not found' type exceptions here)");
         stmt.executeUpdate("ALTER TABLE ReviewQuestions ADD COLUMN (Section VARCHAR(3) DEFAULT 'All')");
         stmt.executeUpdate("UPDATE ReviewQuestions SET Section='All'");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
//notes about the logic involved here. Each permission set is embedded within it's own try/catch statement.
//For future revisions, wherein table structure is further modified, and we don't know where a user is updating from....
//future additions will also go within their own try/catch structure. For the persmission table updates,
//we perform "ALTER" statements before insert statements. That way, if a permission already exists, the exception is thrown,
//and we don't end up with duplicate entries for the actual permission information within the db.
       buf.append("Performing permission updates for DiscussionBoard");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard','DiscussionBoard','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_db','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_SwitchActiveForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_SwitchActiveForum','DiscussionBoard','SwitchActiveForum','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbswitchforum','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_SwitchActiveForum','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_SwitchActiveForum','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_MessageBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_MessageBoard','DiscussionBoard','MessageBoard','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbmessageboard','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_MessageBoard','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_MessageBoard','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_StartNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_StartNewThread','DiscussionBoard','StartNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbstartthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_StartNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_StartNewThread','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Read VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Read','DiscussionBoard','Read','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbread','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Read','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Read','AffectedMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_ViewAllMessages VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_ViewAllMessages','DiscussionBoard','ViewAllMessages','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbviewall','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ViewAllMessages','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ViewAllMessages','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_ChangeMessage VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_ChangeMessage','DiscussionBoard','ChangeMessage','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbchangemessage','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ChangeMessage','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ChangeMessage','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_DeleteMsg VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_DeleteMsg','DiscussionBoard','DeleteMsg','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbdeletemsg','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_DeleteMsg','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_DeleteMsg','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Reply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Reply','DiscussionBoard','Reply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Reply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Reply','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_EditNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_EditNewThread','DiscussionBoard','EditNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbeditthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditNewThread','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PreviewNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PreviewNewThread','DiscussionBoard','PreviewNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpreviewthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewNewThread','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PostNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PostNewThread','DiscussionBoard','PostNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpostthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostNewThread','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PreviewReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PreviewReply','DiscussionBoard','PreviewReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpreviewreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewReply','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PostReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PostReply','DiscussionBoard','PostReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpostreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostReply','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_EditReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_EditReply','DiscussionBoard','EditReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbeditreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditReply','AffectMsgID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Cancel VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Cancel','DiscussionBoard','Cancel','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbcancel','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Cancel','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Cancel','Forum','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Calendar");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar','Calendar','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_cal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_Delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_Delete','Calendar','Delete','SELECT Events.EventID, Events.Section, TAAssignments.Value,TAAssignments.StudentIDNumber FROM Events LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=Events.Section) WHERE Events.EventID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_caldel','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Delete','TASTUDENTID','0');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Delete','EventID','1');");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_Revise VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_Revise','Calendar','Revise','SELECT Events.EventID, Events.Section, TAAssignments.Value,TAAssignments.StudentIDNumber FROM Events LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=Events.Section) WHERE Events.EventID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND Events.Section=\'\'{2}\'\'','3','str_permission_denied','str_edit_calrevise','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','TASTUDENTID','0');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','EventID','1');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','Section','2');");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_New VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_New','Calendar','New','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Section\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_calnew','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_New','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_New','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Exam");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam','Exam','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_exam','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam_NewExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam_NewExam','Exam','NewExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_examnew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam_NewExam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam_GradeExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam_GradeExam','Exam','GradeExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_examgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam_GradeExam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Homework");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework','Homework','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homework','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework_NewHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework_NewHomework','Homework','NewHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homeworknew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework_NewHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework_GradeHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework_GradeHomework','Homework','GradeHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homeworkgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework_GradeHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Quiz");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz','Quiz','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quiz','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz_NewQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz_NewQuiz','Quiz','NewQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quiznew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz_NewQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz_GradeQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz_GradeQuiz','Quiz','GradeQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quizgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz_GradeQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       buf.append("Updating permissions for ManageExam");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam','ManageExam','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexam','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_Update','ManageExam','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddAnExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddAnExam','ManageExam','AddAnExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddAnExam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_DeleteExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_DeleteExam','ManageExam','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteExam','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_EditForm','ManageExam','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddQuestionForm','ManageExam','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddQuestion','ManageExam','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_manageexamaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_Edit','ManageExam','Edit','SELECT ExamQuestions.QuestionID, ExamQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM ExamQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ExamQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_manageexameditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','Section','2')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_DeleteQuestion','ManageExam','DeleteQuestion','SELECT ExamQuestions.QuestionID, ExamQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM ExamQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ExamQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_manageexamdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteQuestion','QuestionID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       buf.append("Updating permissions for ManageHomework");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework','ManageHomework','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomework','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_Update','ManageHomework','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddAHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddAHomework','ManageHomework','AddAHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddAHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_DeleteHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_DeleteHomework','ManageHomework','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_EditForm','ManageHomework','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddQuestionForm','ManageHomework','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddQuestion','ManageHomework','AddQuestion','SELECT * FROM TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managehomeworkaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_Edit','ManageHomework','Edit','SELECT HomeworkQuestions.QuestionID, HomeworkQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM HomeworkQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=HomeworkQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managehomeworkeditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','Section','2')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_DeleteQuestion','ManageHomework','DeleteQuestion','SELECT HomeworkQuestions.QuestionID, HomeworkQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM HomeworkQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=HomeworkQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managehomeworkdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating perissions for ManageQuiz");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz','ManageQuiz','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequiz','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_Update','ManageQuiz','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddAQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddAQuiz','ManageQuiz','AddAQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddAQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_DeleteQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_DeleteQuiz','ManageQuiz','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_EditForm','ManageQuiz','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddQuestionForm','ManageQuiz','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddQuestion','ManageQuiz','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managequizaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_Edit','ManageQuiz','Edit','SELECT QuizQuestions.QuestionID, QuizQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value  FROM QuizQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=QuizQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managequizeditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','Section','2')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_DeleteQuestion','ManageQuiz','DeleteQuestion','SELECT QuizQuestions.QuestionID, QuizQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM QuizQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=QuizQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managequizdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Gradebook");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook','Gradebook','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebook','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_HelpTextFile VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_HelpTextFile','Gradebook','HelpTextFile','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebookhelpfile','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_HelpTextFile','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_CreateTextFile VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_CreateTextFile','Gradebook','CreateTextFile','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebookcreatetabfile','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_CreateTextFile','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteStudent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteStudent','Gradebook','DeleteStudent','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookdelstud','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteStudent','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteStudent','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_UpdateStudent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_UpdateStudent','Gradebook','UpdateStudent','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookupdatestud','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateStudent','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateStudent','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ScoresDetail VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ScoresDetail','Gradebook','ScoresDetail','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookscoredetail','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ScoresDetail','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ScoresDetail','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteScore','Gradebook','DeleteScore','SELECT Scores.StudentIDNumber, Scores.Score, Scores.ScoreID, TAAssignments.Value, TAAssignments.StudentIDNumber FROM Scores LEFT JOIN TAAssignments ON (TAAssignments.value=Scores.StudentIDNumber AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ScoreID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_gradebookdelscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteScore','ScoreID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_AddScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_AddScore','Gradebook','AddScore','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookaddscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_AddScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_AddScore','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ShowAnswers VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ShowAnswers','Gradebook','ShowAnswers','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookshowanswers','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowAnswers','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowAnswers','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_GradeEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_GradeEssays','Gradebook','GradeEssays','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookgradeessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_GradeEssays','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_GradeEssays','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_UpdateEssay VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_UpdateEssay','Gradebook','UpdateEssay','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookupdateessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateEssay','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateEssay','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteEssay VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteEssay','Gradebook','DeleteEssay','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookdeleteessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteEssay','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteEssay','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ShowGradedEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ShowGradedEssays','Gradebook','ShowGradedEssays','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookshowgradedessays','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowGradedEssays','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowGradedEssays','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ResetPassword VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ResetPassword','Gradebook','ResetPassword','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookresetpassword','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetPassword','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetPassword','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ResetStudentPassword VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ResetStudentPassword','Gradebook','ResetStudentPassword','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookresetstudentpassword','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetStudentPassword','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetStudentPassword','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions Journal");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal','Journal','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Review VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Review','Journal','Review','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Review','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Create VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Create','Journal','Create','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalcreate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Create','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Preview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Preview','Journal','Preview','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalpreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Preview','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN(Journal_Submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Submit','Journal','Submit','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalsubmit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Submit','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for ManageContent");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent','ManageContent','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontent','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Upload VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Upload','ManageContent','Upload','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentupload','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Upload','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Edit','ManageContent','Edit','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Edit','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_SaveEdits VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_SaveEdits','ManageContent','SaveEdits','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentsaveedits','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_SaveEdits','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       
       //añadido ManageContent_Save
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Save VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Save','ManageContent','Save','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentsaveedits','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Save','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Delete','ManageContent','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Delete','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
    
    
    
    try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_New VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_New','ManageContent','New','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentnew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_New','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       
       buf.append("Updating permissions for ManageCourse");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse','ManageCourse','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecourse','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveParameters VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveParameters','ManageCourse','SaveParameters','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesaveparameters','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveParameters','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveSections VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveSections','ManageCourse','SaveSections','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesavesections','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveSections','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveNavBarLinks VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveNavBarLinks','ManageCourse','SaveNavBarLinks','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesavenavbar','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveNavBarLinks','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_UpdateLogInfo VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_UpdateLogInfo','ManageCourse','UpdateLogInfo','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecourselog','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_UpdateLogInfo','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for ManageDiscussionBoard");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard','ManageDiscussionBoard','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedb','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_AddForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_AddForum','ManageDiscussionBoard','AddForum','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedbaddf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_AddForum','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_DeleteForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_DeleteForum','ManageDiscussionBoard','DeleteForum','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON (((DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managedbdelf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_DeleteForum','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_DeleteForum','ID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_UpdateForums VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_UpdateForums','ManageDiscussionBoard','UpdateForums','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedbupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_UpdateForums','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for ManageJournal");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal','ManageJournal','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managejournal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Summary VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Summary','ManageJournal','Summary','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managejournalsum','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Summary','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Review VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Review','ManageJournal','Review','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managejournalrev','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Review','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Review','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Submit','ManageJournal','Submit','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managejournalsub','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Submit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Submit','StudentIDNumber','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for ManagePeerReview");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview','ManagePeerReview','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_create VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_create','ManagePeerReview','create','SELECT TAAssignments.StudentIDNumber, TA1.Value AS Value1, TA2.Value AS Value2, TA3.Value as Value3 FROM TAAssignments LEFT JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber) LEFT JOIN TAAssignments AS TA3 ON (TAAssignments.StudentIDNumber=TA3.StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TA1.Value LIKE \'\'%{1}\'\' AND TA2.Value LIKE \'\'%{2}\'\' AND TA3.Value LIKE \'\'%{3}\'\'','4','str_permission_denied','str_edit_managepeerreviewupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','ReviewerID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','Author1ID','2')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','Author2ID','3')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_clone VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_clone','ManagePeerReview','clone','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\'\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\'\'\') AND (Reviews.Author2ID=\'\'\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewclone','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_clone','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_clone','ReviewID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_delete','ManagePeerReview','delete','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\'\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\'\'\') AND (Reviews.Author2ID=\'\'\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_delete','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_delete','ReviewID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_update','ManagePeerReview','update','SELECT * FROM Students WHERE Status=\'\'TA\'\' AND StudentIDNumber=\'\'{0}\'\'','1','str_permission_denied','str_edit_managepeerreviewupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_update','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_UpdateParams VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_UpdateParams','ManagePeerReview','UpdateParams','SELECT * FROM Students WHERE Status=\'\'TA\'\' AND StudentIDNumber=\'\'{0}\'\'','1','str_permission_denied','str_edit_managepeerreviewup','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_UpdateParams','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_EditQuestions VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_EditQuestions','ManagePeerReview','EditQuestions','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerrevieweq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_EditQuestions','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_AddQuestionForm','ManagePeerReview','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerreviewaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_AddQuestion','ManagePeerReview','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestion','Section','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_Edit','ManagePeerReview','Edit','SELECT ReviewQuestions.QuestionID, ReviewQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM ReviewQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ReviewQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managepeerreviewedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','Section','2')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_DeleteQuestion','ManagePeerReview','DeleteQuestion','SELECT ReviewQuestions.QuestionID, ReviewQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM ReviewQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ReviewQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managepeerreviewdelq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_DeleteQuestion','QuestionID','1')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for ManageReport");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport','ManageReport','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_Update','ManageReport','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_AddReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_AddReport','ManageReport','AddReport','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportaddreport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_AddReport','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_DeleteReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_DeleteReport','ManageReport','DeleteReport','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportdeletereport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_DeleteReport','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_ShowFilenames VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_ShowFilenames','ManageReport','ShowFilenames','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\' AND Type=\'\'Student\'\'','2','str_permission_denied','str_edit_managereportshowfilenames','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_ShowFilenames','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_ShowFilenames','StudentIDNumber','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_RecordScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_RecordScore','ManageReport','RecordScore','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\' AND Type=\'\'Student\'\'','2','str_permission_denied','str_edit_managereportrecordscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_RecordScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_RecordScore','StudentIDNumber','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for PeerReview");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview','PeerReview','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_peerreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_view VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_view','PeerReview','view','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_view','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_view','ReviewID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_edit','PeerReview','edit','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_edit','ReviewID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_submit','PeerReview','submit','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewsubmit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_submit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_submit','ReviewID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Scores");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Scores VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Scores','Scores','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_scores','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Scores','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Scores_viewGradedEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Scores_viewGradedEssays','Scores','viewGradedEssays','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_scoresviewgradedessays','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Scores_viewGradedEssays','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Report");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report','Report','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report_Upload VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report_Upload','Report','Upload','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report_upload','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report_Upload','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report_UploadStatus VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report_UploadStatus','Report','UploadStatus','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report_status','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report_UploadStatus','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Email");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Email VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Email','Email','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_email','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Email','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Email_Send VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Email_Send','Email','Send','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_email_send','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Email_Send','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }

       buf.append("Updating permissions for Content");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Content VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Content','Content','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_content','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Content','TASTUDENTID','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       
       //añadido permisos para Home
       buf.append("Updating permissions for Home");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Home VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Home','Home','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_content','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Home','Home','0')");
       } catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
       
  }
  catch (Exception e) {
         buf.append("Caught: " + e.getMessage());
       }
   
   return buf.toString();
}


String PaginaPermisos() {
	StringBuffer buf = new StringBuffer();
	
	buf.append("<form name=form1 method=post>" +
	"<p>Se crear&oacute;n los parametros, de clic en Continuar</p>" +
	"<p><input type=submit name=Submit value=Continuar>" +
	"<p><input type=hidden name=UserRequest value=permisos>" +
    "</p></form>");
    
    return buf.toString();
}



}
