import java.io.*;
import java.sql.*;
import java.util.*;

public class updateDB {
  //replace the following constants with the appropriate values.
  private static String mySQLUser="root";
  private static String mySQLPass="";
  private static String jdbcDriver="org.gjt.mm.mysql.Driver";
  private static String db = "jdbc:mysql://localhost/";
  private static String eledgeDir="E:\\Archivos de programa\\Apache Group\\Tomcat 4.1\\webapps\\ROOT\\WEB-INF\\classes";
  public static void main(String[] args) {
    File directory = new File(eledgeDir);
    if (!directory.isDirectory()) {
      System.out.println("Error: " + eledgeDir + " is not a valid directory");
      return;
    }
    if (args.length < 1) {//update all courses in directory...
      File[] files = directory.listFiles();
      for (int i=0; i<files.length; i++) {
        if (files[i].isDirectory()) {
          File f = new File(files[i].getAbsolutePath() + "/Course.java");
          if (f.exists()) {
            updateDatabase(files[i]);
          }
        }
      }
    } else {
      for (int i=0; i<args.length; i++) {
        File f = new File(directory.getAbsolutePath() + "/" + args[i]);
        if (f.exists() && f.isDirectory()) {
          File f2 = new File(f.getAbsolutePath() + "/Course.java");
          if (f2.exists()) {
            updateDatabase(f);
          } else
            System.out.println("Error: " + args[i] + "doesn't appear to be a valid course");
        } else {
          System.out.println("Error: " + args[i] + " doesn't appear to be a valid course.");
        }
      }
    }      
  }

  private static void updateDatabase(File f) {
     System.out.println("Updating: " + f.getName());
     try {
       Class.forName(jdbcDriver).newInstance();
       Connection c = DriverManager.getConnection(db + f.getName(),mySQLUser,mySQLPass);
       Statement stmt = c.createStatement();

       try {
         System.out.println("Creating new tables TAPermissions, TAAssignments, Permissions, PermissionArguments (ignore 'table exists' type errors here)");
         stmt.executeUpdate("CREATE TABLE TAPermissions (StudentIDNumber VARCHAR(50) PRIMARY KEY, isDefault VARCHAR(5))");
         stmt.executeUpdate("CREATE TABLE TAAssignments (StudentIDNumber VARCHAR(50), Type VARCHAR(7), Value VARCHAR(50))");
         stmt.executeUpdate("CREATE TABLE Permissions (Name VARCHAR(50) PRIMARY KEY, Servlet VARCHAR(50), Request VARCHAR(50), SqlFormat TEXT, NumArgs INT, DenyMsg VARCHAR(50), EditMsg VARCHAR(50), SetToTeacher VARCHAR(5))");
         stmt.executeUpdate("CREATE TABLE PermissionArguments (Name VARCHAR(50), Argument VARCHAR(50), ArgNum INT)");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         System.out.println("Updating ReviewQuestions table (you can ignore 'table not found' type exceptions here)");
         stmt.executeUpdate("ALTER TABLE ReviewQuestions ADD COLUMN (Section VARCHAR(3) DEFAULT 'All')");
         stmt.executeUpdate("UPDATE ReviewQuestions SET Section='All'");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
//notes about the logic involved here. Each permission set is embedded within it's own try/catch statement.
//For future revisions, wherein table structure is further modified, and we don't know where a user is updating from....
//future additions will also go within their own try/catch structure. For the persmission table updates,
//we perform "ALTER" statements before insert statements. That way, if a permission already exists, the exception is thrown,
//and we don't end up with duplicate entries for the actual permission information within the db.
       System.out.println("Performing permission updates for DiscussionBoard");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard','DiscussionBoard','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_db','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_SwitchActiveForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_SwitchActiveForum','DiscussionBoard','SwitchActiveForum','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbswitchforum','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_SwitchActiveForum','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_SwitchActiveForum','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_MessageBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_MessageBoard','DiscussionBoard','MessageBoard','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbmessageboard','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_MessageBoard','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_MessageBoard','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_StartNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_StartNewThread','DiscussionBoard','StartNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbstartthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_StartNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_StartNewThread','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Read VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Read','DiscussionBoard','Read','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbread','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Read','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Read','AffectedMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_ViewAllMessages VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_ViewAllMessages','DiscussionBoard','ViewAllMessages','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbviewall','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ViewAllMessages','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ViewAllMessages','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_ChangeMessage VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_ChangeMessage','DiscussionBoard','ChangeMessage','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbchangemessage','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ChangeMessage','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_ChangeMessage','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_DeleteMsg VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_DeleteMsg','DiscussionBoard','DeleteMsg','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbdeletemsg','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_DeleteMsg','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_DeleteMsg','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Reply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Reply','DiscussionBoard','Reply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Reply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Reply','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_EditNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_EditNewThread','DiscussionBoard','EditNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbeditthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditNewThread','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PreviewNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PreviewNewThread','DiscussionBoard','PreviewNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpreviewthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewNewThread','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PostNewThread VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PostNewThread','DiscussionBoard','PostNewThread','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpostthread','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostNewThread','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostNewThread','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PreviewReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PreviewReply','DiscussionBoard','PreviewReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpreviewreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PreviewReply','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_PostReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_PostReply','DiscussionBoard','PostReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbpostreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_PostReply','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_EditReply VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_EditReply','DiscussionBoard','EditReply','SELECT DiscussionBoardEntries.ID, DiscussionBoardEntries.StudentIDNumber, TAAssignments.Value, TAAssignments.StudentIDNumber FROM DiscussionBoardEntries LEFT JOIN TAAssignments ON (DiscussionBoardEntries.StudentIDNumber=TAAssignments.Value AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE DiscussionBoardEntries.ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbeditreply','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditReply','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_EditReply','AffectMsgID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (DiscussionBoard_Cancel VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('DiscussionBoard_Cancel','DiscussionBoard','Cancel','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON ((DiscussionBoardForums.CourseSection=\'\'All\'\' OR (DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_dbcancel','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Cancel','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('DiscussionBoard_Cancel','Forum','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Calendar");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar','Calendar','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_cal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_Delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_Delete','Calendar','Delete','SELECT Events.EventID, Events.Section, TAAssignments.Value,TAAssignments.StudentIDNumber FROM Events LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=Events.Section) WHERE Events.EventID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_caldel','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Delete','TASTUDENTID','0');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Delete','EventID','1');");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_Revise VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_Revise','Calendar','Revise','SELECT Events.EventID, Events.Section, TAAssignments.Value,TAAssignments.StudentIDNumber FROM Events LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=Events.Section) WHERE Events.EventID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND Events.Section=\'\'{2}\'\'','3','str_permission_denied','str_edit_calrevise','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','TASTUDENTID','0');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','EventID','1');");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_Revise','Section','2');");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Calendar_New VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Calendar_New','Calendar','New','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Section\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_calnew','false')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_New','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Calendar_New','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Exam");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam','Exam','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_exam','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam_NewExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam_NewExam','Exam','NewExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_examnew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam_NewExam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Exam_GradeExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Exam_GradeExam','Exam','GradeExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_examgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Exam_GradeExam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Homework");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework','Homework','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homework','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework_NewHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework_NewHomework','Homework','NewHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homeworknew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework_NewHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Homework_GradeHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Homework_GradeHomework','Homework','GradeHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_homeworkgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Homework_GradeHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Quiz");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz VARCHAR(15));");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz','Quiz','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quiz','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz_NewQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz_NewQuiz','Quiz','NewQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quiznew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz_NewQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Quiz_GradeQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Quiz_GradeQuiz','Quiz','GradeQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_quizgrade','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Quiz_GradeQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       System.out.println("Updating permissions for ManageExam");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam','ManageExam','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexam','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_Update','ManageExam','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddAnExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddAnExam','ManageExam','AddAnExam','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddAnExam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_DeleteExam VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_DeleteExam','ManageExam','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteExam','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_EditForm','ManageExam','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddQuestionForm','ManageExam','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_manageexamaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_AddQuestion','ManageExam','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_manageexamaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_AddQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_Edit','ManageExam','Edit','SELECT ExamQuestions.QuestionID, ExamQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM ExamQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ExamQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_manageexameditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_Edit','Section','2')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageExam_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageExam_DeleteQuestion','ManageExam','DeleteQuestion','SELECT ExamQuestions.QuestionID, ExamQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM ExamQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ExamQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_manageexamdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageExam_DeleteQuestion','QuestionID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       System.out.println("Updating permissions for ManageHomework");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework','ManageHomework','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomework','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_Update','ManageHomework','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddAHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddAHomework','ManageHomework','AddAHomework','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddAHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_DeleteHomework VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_DeleteHomework','ManageHomework','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteHomework','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_EditForm','ManageHomework','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddQuestionForm','ManageHomework','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managehomeworkaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_AddQuestion','ManageHomework','AddQuestion','SELECT * FROM TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managehomeworkaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_AddQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_Edit','ManageHomework','Edit','SELECT HomeworkQuestions.QuestionID, HomeworkQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM HomeworkQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=HomeworkQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managehomeworkeditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_Edit','Section','2')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageHomework_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageHomework_DeleteQuestion','ManageHomework','DeleteQuestion','SELECT HomeworkQuestions.QuestionID, HomeworkQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM HomeworkQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=HomeworkQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managehomeworkdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageHomework_DeleteQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating perissions for ManageQuiz");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz','ManageQuiz','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequiz','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_Update','ManageQuiz','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddAQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddAQuiz','ManageQuiz','AddAQuiz','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizadd','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddAQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_DeleteQuiz VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_DeleteQuiz','ManageQuiz','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuiz','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_EditForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_EditForm','ManageQuiz','EditForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizef','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_EditForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddQuestionForm','ManageQuiz','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managequizaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_AddQuestion','ManageQuiz','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managequizaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_AddQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_Edit','ManageQuiz','Edit','SELECT QuizQuestions.QuestionID, QuizQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value  FROM QuizQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=QuizQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managequizeditq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_Edit','Section','2')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageQuiz_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageQuiz_DeleteQuestion','ManageQuiz','DeleteQuestion','SELECT QuizQuestions.QuestionID, QuizQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM QuizQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=QuizQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managequizdeleteq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageQuiz_DeleteQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Gradebook");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook','Gradebook','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebook','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_HelpTextFile VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_HelpTextFile','Gradebook','HelpTextFile','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebookhelpfile','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_HelpTextFile','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_CreateTextFile VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_CreateTextFile','Gradebook','CreateTextFile','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_gradebookcreatetabfile','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_CreateTextFile','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteStudent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteStudent','Gradebook','DeleteStudent','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookdelstud','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteStudent','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteStudent','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_UpdateStudent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_UpdateStudent','Gradebook','UpdateStudent','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookupdatestud','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateStudent','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateStudent','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ScoresDetail VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ScoresDetail','Gradebook','ScoresDetail','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookscoredetail','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ScoresDetail','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ScoresDetail','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteScore','Gradebook','DeleteScore','SELECT Scores.StudentIDNumber, Scores.Score, Scores.ScoreID, TAAssignments.Value, TAAssignments.StudentIDNumber FROM Scores LEFT JOIN TAAssignments ON (TAAssignments.value=Scores.StudentIDNumber AND TAAssignments.Type=\'\'Student\'\' AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ScoreID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_gradebookdelscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteScore','ScoreID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_AddScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_AddScore','Gradebook','AddScore','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookaddscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_AddScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_AddScore','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ShowAnswers VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ShowAnswers','Gradebook','ShowAnswers','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookshowanswers','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowAnswers','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowAnswers','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_GradeEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_GradeEssays','Gradebook','GradeEssays','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookgradeessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_GradeEssays','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_GradeEssays','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_UpdateEssay VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_UpdateEssay','Gradebook','UpdateEssay','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookupdateessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateEssay','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_UpdateEssay','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_DeleteEssay VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_DeleteEssay','Gradebook','DeleteEssay','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookdeleteessay','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteEssay','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_DeleteEssay','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ShowGradedEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ShowGradedEssays','Gradebook','ShowGradedEssays','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookshowgradedessays','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowGradedEssays','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ShowGradedEssays','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ResetPassword VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ResetPassword','Gradebook','ResetPassword','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookresetpassword','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetPassword','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetPassword','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Gradebook_ResetStudentPassword VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Gradebook_ResetStudentPassword','Gradebook','ResetStudentPassword','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_gradebookresetstudentpassword','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetStudentPassword','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Gradebook_ResetStudentPassword','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions Journal");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal','Journal','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Review VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Review','Journal','Review','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Review','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Create VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Create','Journal','Create','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalcreate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Create','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Journal_Preview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Preview','Journal','Preview','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalpreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Preview','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN(Journal_Submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Journal_Submit','Journal','Submit','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_journalsubmit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Journal_Submit','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for ManageContent");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent','ManageContent','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontent','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Upload VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Upload','ManageContent','Upload','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentupload','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Upload','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Edit','ManageContent','Edit','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Edit','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_SaveEdits VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_SaveEdits','ManageContent','SaveEdits','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentsaveedits','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_SaveEdits','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       
       //añadido ManageContent_Save
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Save VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Save','ManageContent','Save','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentsaveedits','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Save','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_Delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_Delete','ManageContent','Delete','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_Delete','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       //permisos para crear contenido (añadido)
    try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageContent_New VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageContent_New','ManageContent','New','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecontentnew','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageContent_New','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       
       System.out.println("Updating permissions for ManageCourse");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse','ManageCourse','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecourse','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveParameters VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveParameters','ManageCourse','SaveParameters','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesaveparameters','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveParameters','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveSections VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveSections','ManageCourse','SaveSections','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesavesections','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveSections','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_SaveNavBarLinks VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_SaveNavBarLinks','ManageCourse','SaveNavBarLinks','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecoursesavenavbar','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_SaveNavBarLinks','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageCourse_UpdateLogInfo VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageCourse_UpdateLogInfo','ManageCourse','UpdateLogInfo','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managecourselog','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageCourse_UpdateLogInfo','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for ManageDiscussionBoard");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard','ManageDiscussionBoard','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedb','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_AddForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_AddForum','ManageDiscussionBoard','AddForum','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedbaddf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_AddForum','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_DeleteForum VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_DeleteForum','ManageDiscussionBoard','DeleteForum','SELECT * from DiscussionBoardForums LEFT JOIN TAAssignments ON (((DiscussionBoardForums.CourseSection=TAAssignments.Value AND TAAssignments.Type=\'\'Section\'\')) AND TAAssignments.StudentIDNumber=\'\'{0}\'\') WHERE ID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managedbdelf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_DeleteForum','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_DeleteForum','ID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageDiscussionBoard_UpdateForums VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageDiscussionBoard_UpdateForums','ManageDiscussionBoard','UpdateForums','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managedbupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageDiscussionBoard_UpdateForums','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for ManageJournal");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal','ManageJournal','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managejournal','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Summary VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Summary','ManageJournal','Summary','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managejournalsum','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Summary','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Review VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Review','ManageJournal','Review','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managejournalrev','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Review','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Review','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageJournal_Submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageJournal_Submit','ManageJournal','Submit','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Type=\'\'Student\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managejournalsub','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Submit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageJournal_Submit','StudentIDNumber','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for ManagePeerReview");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview','ManagePeerReview','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_create VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_create','ManagePeerReview','create','SELECT TAAssignments.StudentIDNumber, TA1.Value AS Value1, TA2.Value AS Value2, TA3.Value as Value3 FROM TAAssignments LEFT JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber) LEFT JOIN TAAssignments AS TA3 ON (TAAssignments.StudentIDNumber=TA3.StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TA1.Value LIKE \'\'%{1}\'\' AND TA2.Value LIKE \'\'%{2}\'\' AND TA3.Value LIKE \'\'%{3}\'\'','4','str_permission_denied','str_edit_managepeerreviewupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','ReviewerID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','Author1ID','2')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_create','Author2ID','3')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_clone VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_clone','ManagePeerReview','clone','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\'\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\'\'\') AND (Reviews.Author2ID=\'\'\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewclone','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_clone','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_clone','ReviewID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_delete VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_delete','ManagePeerReview','delete','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\'\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\'\'\') AND (Reviews.Author2ID=\'\'\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewdelete','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_delete','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_delete','ReviewID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_update','ManagePeerReview','update','SELECT * FROM Students WHERE Status=\'\'TA\'\' AND StudentIDNumber=\'\'{0}\'\'','1','str_permission_denied','str_edit_managepeerreviewupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_update','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_UpdateParams VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_UpdateParams','ManagePeerReview','UpdateParams','SELECT * FROM Students WHERE Status=\'\'TA\'\' AND StudentIDNumber=\'\'{0}\'\'','1','str_permission_denied','str_edit_managepeerreviewup','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_UpdateParams','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_EditQuestions VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_EditQuestions','ManagePeerReview','EditQuestions','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerrevieweq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_EditQuestions','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_AddQuestionForm VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_AddQuestionForm','ManagePeerReview','AddQuestionForm','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managepeerreviewaqf','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestionForm','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_AddQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_AddQuestion','ManagePeerReview','AddQuestion','SELECT * from TAAssignments WHERE Type=\'\'Section\'\' AND StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\'','2','str_permission_denied','str_edit_managepeerreviewaddq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_AddQuestion','Section','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_Edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_Edit','ManagePeerReview','Edit','SELECT ReviewQuestions.QuestionID, ReviewQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value, TA2.Value FROM ReviewQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ReviewQuestions.Section) LEFT JOIN TAAssignments AS TA2 ON (TAAssignments.StudentIDNumber=TA2.StudentIDNumber AND TA2.Type=\'\'Section\'\') WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL AND TA2.Value=\'\'{2}\'\'','3','str_permission_denied','str_edit_managepeerreviewedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','QuestionID','1')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_Edit','Section','2')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManagePeerReview_DeleteQuestion VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManagePeerReview_DeleteQuestion','ManagePeerReview','DeleteQuestion','SELECT ReviewQuestions.QuestionID, ReviewQuestions.Section, TAAssignments.Type, TAAssignments.StudentIDNumber, TAAssignments.Value FROM ReviewQuestions LEFT JOIN TAAssignments ON (TAAssignments.StudentIDNumber=\'\'{0}\'\' AND TAAssignments.Type=\'\'Section\'\' AND TAAssignments.Value=ReviewQuestions.Section) WHERE QuestionID=\'\'{1}\'\' AND TAAssignments.StudentIDNumber IS NOT NULL','2','str_permission_denied','str_edit_managepeerreviewdelq','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_DeleteQuestion','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManagePeerReview_DeleteQuestion','QuestionID','1')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for ManageReport");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport','ManageReport','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_Update VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_Update','ManageReport','Update','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportupdate','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_Update','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_AddReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_AddReport','ManageReport','AddReport','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportaddreport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_AddReport','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_DeleteReport VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_DeleteReport','ManageReport','DeleteReport','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_managereportdeletereport','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_DeleteReport','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_ShowFilenames VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_ShowFilenames','ManageReport','ShowFilenames','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\' AND Type=\'\'Student\'\'','2','str_permission_denied','str_edit_managereportshowfilenames','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_ShowFilenames','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_ShowFilenames','StudentIDNumber','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (ManageReport_RecordScore VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('ManageReport_RecordScore','ManageReport','RecordScore','SELECT * FROM TAAssignments WHERE StudentIDNumber=\'\'{0}\'\' AND Value=\'\'{1}\'\' AND Type=\'\'Student\'\'','2','str_permission_denied','str_edit_managereportrecordscore','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_RecordScore','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('ManageReport_RecordScore','StudentIDNumber','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for PeerReview");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview','PeerReview','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_peerreview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_view VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_view','PeerReview','view','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewview','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_view','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_view','ReviewID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_edit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_edit','PeerReview','edit','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewedit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_edit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_edit','ReviewID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (PeerReview_submit VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('PeerReview_submit','PeerReview','submit','SELECT Reviews.ReviewID, Reviews.ReviewerID, Reviews.Author1ID, Reviews.Author2ID, TAAssignments.StudentIDNumber, TA1.Value, TA2.Value, TA3.Value from Reviews LEFT OUTER JOIN TAAssignments ON ((Reviews.ReviewerID=TA1.Value OR Reviews.ReviewerID=\'\') AND (Reviews.Author1ID=TA2.Value OR Reviews.Author1ID=\'\') AND (Reviews.Author2ID=\'\' OR Reviews.Author2ID=TA3.Value)) INNER JOIN TAAssignments AS TA1 ON (TAAssignments.StudentIDNumber=TA1.StudentIDNumber) INNER JOIN TAAssignments AS TA2 USING (StudentIDNumber) INNER JOIN TAAssignments AS TA3 USING (StudentIDNumber) WHERE TAAssignments.StudentIDNumber=\'\'{0}\'\' AND Reviews.ReviewID=\'\'{1}\'\'','2','str_permission_denied','str_edit_peerreviewsubmit','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_submit','TASTUDENTID','0')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('PeerReview_submit','ReviewID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Scores");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Scores VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Scores','Scores','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_scores','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Scores','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Scores_viewGradedEssays VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Scores_viewGradedEssays','Scores','viewGradedEssays','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_scoresviewgradedessays','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Scores_viewGradedEssays','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Report");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report','Report','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report_Upload VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report_Upload','Report','Upload','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report_upload','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report_Upload','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Report_UploadStatus VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Report_UploadStatus','Report','UploadStatus','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_report_status','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Report_UploadStatus','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Email");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Email VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Email','Email','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_email','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Email','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Email_Send VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Email_Send','Email','Send','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_email_send','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Email_Send','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }

       System.out.println("Updating permissions for Content");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Content VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Content','Content','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_content','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Content','TASTUDENTID','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       
       //añadido permisos para Home
       System.out.println("Updating permissions for Home");
       try {
         stmt.executeUpdate("ALTER TABLE TAPermissions ADD COLUMN (Home VARCHAR(15))");
         stmt.executeUpdate("INSERT INTO Permissions VALUES('Home','Home','','SELECT * FROM Students WHERE StudentIDNumber=\'\'{0}\'\' AND Status=\'\'TA\'\'','1','str_permission_denied','str_edit_content','true')");
         stmt.executeUpdate("INSERT INTO PermissionArguments VALUES('Home','Home','0')");
       } catch (Exception e) {
         System.out.println("Caught: " + e.getMessage());
       }
       
   } catch (Exception e) {
       System.out.println("Caught: " + e.getMessage());
       e.printStackTrace(System.out);
    }
  }
}
