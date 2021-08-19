# Eledge Open Learning Management System version 3.2.0
Copyright (C) 2002, University of Utah
Distributed freely, but with NO WARRANTY whatsoever, under the terms of the
GNU General Public Licence. See http://www.gnu.org/copyleft/gpl.html

## General Description:
Eledge is an open source electronic course management system consisting of about 34
Java servlets backed by a MySQL database.  Eledge provides the functionality required to
create online courses and instructional web sites including student registration, quizzes,
exams, homework grader, email directory, calendar, password management, class gradebook,
file uploads, student peer review, syllabus, content creation tools, and course navigation.

## Requirements:
* Hardware: nearly any type of computer connected to the Internet or local intranet
* Software:
  * Java Compiler/Interpreter - a program that creates Java byte code and converts
    it to instructions that your particular computer understands.
  * Servlet runner - a program that launches a Java servlet in response to a 
    request from a remote machine over the network.
  * MySQL Database - a program that maintains tables of information that can be
    accessed by the Java servlet programs.
  * JDBC Driver - a Java program that makes the connection between the servlet
    and the MySQL database program.

All of the required software is available as free downloads for most common computer architectures at the following locations:
* Java 2 Platform Standard Edition - http://javasoft.com/
* Servlet engine (e.g., Tomcat)    - http://jakarta.apache.org/
* MySQL database                   - http://www.mysql.com/
* JDBC Driver                      - http://www.mysql.com/products/connector-j/index.html

## New/changed in version 3.2.0 - Fork 2004:
  Eledge now supports a multiple courses through the new cursos servlet.  Other improvements have been made updating anothers servlets.

## New/changed in version 3.1.0:
  In working towards a new way of doing things... the newest database updates for version 3.1.0
    are -NOT- done within the servlets themselves. Rather, an updateDB.java is included within the jar
    archive. It is a standalone, command line program. It is meant to be run from 
    $CATALINA_HOME/webapps/ROOT/WEB-INF/classes directory, or, in otherwords, the webapp directory in
    which all of your course directories reside. Running it from there, with no arguments, will
    cause it to attempt an update to every course in the classes directory.  You may optionally
    supply one or more course names to update in the even that you want to update some, but not all,
    of the course databases. (Perform just one update, for instance, to a test-course to see how 
    things go/how you like the new codebase before performing updates to all existing courses).
    Please note that any database updates, creations, etc. that were handled up until 
  Report.java sports several new features, including improved error reporting (attempts at uploading
    empty or non-existant files will now be caught), as well as various changes to the actual upload
    handling code, backported from Yoon Kyung Koo's servlet version 1.1a. Also new is an "upload status"
    window that opens on upload of a report, showing the progress of the upload. (Tested in mozilla
    1.x, IE 5,x, 6,x, and Opera 6.x and 7.x).
  Optimization of i18n related code.
  Calendar servlet throwing an exception on creation of events table bug is fixed
  Various key-name fixes with i18n
  New logging methods added which provide the ability to print stack traces
  Small sql-related buf fixes to ManageReport.java
  Section Specific question support added to PeerReview questions
  New TA system:
     Up until 3.1.0, setting a profile to the status of "TA" has meant, essentially, the same thing
     as setting them to "Instructor". 3.1.0 makes it possible to change that. By default, TA's
     will still be set to Instructor-like status, so if this functionality was desireable, it,
     transparently, still "feels" the same. However, the option is now available to assign
     permission sets to TA's, customizing their permission level for nearly every facet of 
     Eledge. Additionally, TA's can be assigned one or more sections, one or more students,
     or anything in between.  For example, a TA could be assigned a section of students, and
     the permissions set such that they can access the gradebook, but only see their assigned
     students, only perform actions on their assigned students, etc. 
     Some notes... the majority of database updates in 3.1.0 are from this new TA system.
     Even if you do not plan on using the system, you -should- still compile and run
     updateDB.java.  Any questions, comments, etc. regarding the use of the new TA 
     system may either be posted at the eledge help forum, on the eledge-users mailing list,
     or simply mailed directly to robertz@scazdl.org.
  Various other bug-fixes and improvements.


## New/changed in version 3.0.0:
  First, a new naming system. Eledge will now be following a standard naming convention of Major.Minor.Patch.
  Typically, patch updates will not require database changes, minor probably will, and major will usually mean
  that the code has changed radically from a previous release.
  The foremost change between 3.0.0 and 2.0 is i18n. Eledge has been completely gone through and i18n'ed 
  for easier translation into other languages (spanish language pack is also available). 
  DiscussionBoard using too many mysql connections at a time has been fixed.
  "DeleteMsg" bugs in DiscussionBoard are fixed (formerly, problems occurred when a thread-starting message was deleted.)
  An "ArchiveErase" option has been added to ResetCourse allowing instructors to prepare a course for a new semester, without
     completely losing previous semester student information.
  A new "Template" feature has been added. This feature allows an instructor to set up a basic page template to be
     applied to all new pages dynamically created. (For example, an instructor could create a "heading" item, 
     paragraph separator items, etc. for a page).
  PeerReview sql error has been fixed.
  Instructors may now choose to "hide" student names in PeerReview, which keeps students from seeing who wrote 
    the original article and/or the review.
  A text-wrap feature has been added to the ManageJournal servlet, allowing instructors to set a desired
    screen width for themselves when viewing student entries (in the event that students used "this preformatted
    text" and never hit enter, for instance). Note: this feature requries persistent cookies to be enabled.
  A summary of journal entries feature has been added to the ManageJournal servlet, allowing instructors to
    see at a glance which students have unread entries, how many there are, as well as how many read entries exist.
  Error messages have been re-enabled in report.java and in the content uploads feature for instructors.
  Also, the error messages, if any, for a given student report upload are now recorded. The latest message
    for a given report is displayed to the student, and the instructor may see all error messages/upload comments
    via the View Student Reports page (ManageReports->View Student Reports)
  The "title" attribute has been added to the cells of the Gradebook page table, making it so that a 
    "tooltip" will be displayed in Graphical browsers, showing both the student and the assignment name
    corresponding to the given cell/score.
  Many other changes and bugfixes.

## New in version 2.0:
  Eledge now supports threaded discussion groups through the DiscussionBoard servlet.  Other minor improvements
  have been made to the ScoresDetail section of the Gradebook servlet.

## New in version 1.8:
  Committed several minor bug fixes to achieve upward compatibility between servlets and the underlying database.
  Minor changes to support for class sections in Calendar, Quiz, Homework, Exam, and corresponding ManageQuiz, 
  ManageHomework and ManageExam servlets.
  Essay question support has been added to Quiz,Homework and Exam servlets, along with associated changes in 
  ManageXXX servlets and Gradebook.
  Support was added to ManageContent servlet to allow instructors to create section-specific class content pages.
  A feature was added to the Gradebook servlet that allows teachers to reset student passwords.
  The Journal servlet was modified to allow teachers to record a score and optionally email each entry and 
  instructor response to the student.
  The ManageReport servlet was modified to allow the instructor to view uploaded reports, detect the number 
  of upload attempts, and assign scores to reports.

## New in version 1.7:
  Completely rewrote the PeerReview and ManagePeerReview servlets to add functionality for
  automatically recording scores, hiding completed and abandoned reviews, adding questions,
  including essay questions, to the Reviews database.

## New in version 1.6:
  Added new functionality to the Page class to extend the HttpServlet class, so that it can
  display web pages directly to client browsers. Also changed the ManageContent servlet to 
  allow instructors to create content web pages and store them in the class database.  These
  pages can be managed, edited and deleted by the instructor.
  Also added popup windows to the Quiz, Exam and Homework servlets to avoid accidental student
  submission of web form assignments by typing a carriage return in Internet Explorer.
  Fixed some coding errors in the PeerReview servlet.

## New in version 1.5:
  Fixed some minor bugs in the ManageExam, ManageHomework and ManageQuiz servlets.
  Fixed a minor HTML coding error that caused MyProfile to fail with text-only browsers
  Made better use of the CharHider class to deal with apostrophes embedded in quiz questions/answers.

## New in version 1.4:
  Added a new Journal servlet that allows students to maintain an electronic journal in
  the class database.  The associated ManageJournal servlet allows the instructor to
  read and review student journal entries, and to provide comments and feedback.

## New in version 1.3:
  The flexibility of user IDs has been improved.  You can either use an n-digit Student ID
  number (specify the length with the idLength parameter in Course.java), or by setting
  Course.idLength=0, the program will accept characters (usernames or email addresses) as the
  unique identifier for the students.  Several minor bug fixes were included, and the 
  operation of the ManageContent servlet's file upload utility was improved.  The PeerReview
  servlet works, but pages are large and it needs additional management functionality to work
  properly.
  
## New in version 1.2:
  I added a PeerReview servlet (beta release) with associated ManagePeerReviews servlet for
  performing student peer reviews of each other's reports.  Also added a sendmail utility to
  the Email servlet (for instructor use only).  Fixed several minor bugs and started to make
  the user ID system more flexible (but this needs more work).

# Setup instructions

Setup instructions (assumes that Java 2 JDK, servlet engine and MySQL database programs 
are installed and running properly):

(NOTE: If you are running linux, you might want to check out the eledge-utils package
that has just been released. It contains a suite of shell scripts, including a 
"configure.sh" and "install.sh" script for the utils, that greatly simplify the 
process of creating, updating, and deleting courses. If you're using some other
*nix OS, the script(s) may or may not function properly; they will be expanded to
other *nix OS's later. If you're a windows user, I may eventually get around to 
writing some batch files to do the job for you, but, I'm not making any promises.)

Identify the root directory for your servlet runner.  I use Tomcat4, and on my
server I use the root directory /usr/local/tomcat/webapps/ROOT
Create the following new directories to hold static html files and servlet class files.
in this example, suppose that I am creating directories for a course called ART-1010:

  /usr/local/tomcat/webapps/ROOT/ART-1010                    (static html files)
  /usr/local/tomcat/webapps/ROOT/ART-1010/images             (image files for this course)
  /usr/local/tomcat/webapps/ROOT/ART-1010/content            (static instructional content)
  /usr/local/tomcat/webapps/ROOT/ART-1010/uploads            (student file uploads)
  /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/ART-1010    (servlet class files)

Unzip the eledge distribution file and put the index.html file into the ../ROOT/ART-1010
directory.  Edit this file and replace the word Eledge with ART-1010.  This page is only
a refresh page to create a simple URL for the course: http://servername.edu/ART-1010/

Put the servlets into the ART-1010 servlet directory by extracting them from the jar file:
%> jar -xvf eledge-2.0-src.jar /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/ART-1010

You should now see about 50 Java source files in this directory.  Move updateDB.java to 
/usr/local/tomcat/webapps/ROOT/WEB-INF/classes.  Edit every file (except updateDB.java) and 
replace the word Eledge in the first line (package Eledge;) with ART-1010.  This package statement links
all of the servlet class files together and creates unique instances of the servlets for
each course, e.g.,
  http://servername.edu/servlet/ART-1010.Gradebook
  http://servername.edu/servlet/CHEM1220.Gradebook

HINT: to save time editing, create a keyboard macro to do this task.  For example, if you
use the vi editor in unix or Linux, you can open all of the servlet source code files with
  %>vi *.java
Then create a recursive keystroke remapping by typing
  :map ; :s/Eledge/ART-1010^M:wn^M=
  :map = ;
The ^M character (carriage return) is created by typing the sequence CTRL-V CTRL-M.
The single keystroke ";" will then be remapped to a sequence that replaces the string Eledge
with the new string ART-1010 on the first line of the file, then writes the file, then
moves to the next open file, then runs itself again (because the "=" key is mapped to ";"
in the second map command).  When vi runs out of files, the error message causes the 
recursive macro to terminate.  The second remapping is required because vi prevents the
user from using the remapped key as the last character in the keystroke sequence.

The Course.java servlet needs some extra editing. Make certain that you have edited the
lines in this file to indicate the correct course name, webserver name, outgoing mail
server name, mysql server name, mysql username, mysql password, content directory path, 
and uploads directory path. The mysql username and password are used internally by the 
servlets to communicate with the database.  Neither the students nor the instructor will 
know or use these values. If you use SSL (recommended to keep passwords, student data and 
scores secure), edit the secureLoginURL so that it begins with https:// instead of http://

Locate the mm.mysql-2.0.7-bin.jar file (or download the most current stable version, currently 3.0, from mysql.com).
This contains the JDBC Driver class files required to make the connection between your 
Java servlets and the MySQL database server. The Java servlets make use of this Driver class 
at runtime.  Therefore, it is necessary to put this jar file into your classpath where both the
Java runtime environment (JRE) and the servlet runner program (e.g., tomcat) can find it. You
can do this by editing the $CLASSPATH environment variable (yours and the user that runs the
servlet runner, e.g., root).  Alternatively, you can sometimes place the jar file into the
.../jre/lib/ext directory to accomplish this automatically.

Compile all of the servlets by first switching to the classes directory:
%> cd /usr/local/tomcat/webapps/ROOT/WEB-INF/classes
%> javac ART-1010/*.java 

Next, you should create the class database and grant permissions to the servlets to read/write
to it.  If you specified the user/password in Course.java to be Art1010User and h8VanGho, then start the
mysql client program, connect to the MySQL database and type the commands:
mysql> CREATE DATABASE ART-1010;
mysql> GRANT ALL ON ART-1010.* TO Art1010User IDENTIFIED BY 'h8VanGho';

## NEW IN VERSION 3.1.0

in the /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/ directory, compile the updateDB.java file, 
and run java updateDB ART-1010

(You may safely ignore any error messages about the ManagePeerReview table not existing.
 Please report any other error messages to the help forum at http://sourceforge.net/projects/eledge)

At this point you should be ready to start building the course by pointing a browser to
http://servername.edu/ART-1010  or
http://servername.edu/servlet/ART-1010.Home

As you instantiate each servlet, it creates the database tables that it needs and usually puts
some placeholder content in each one to demonstrate typical or intended use.  If you are logged
in as the instructor, then the look and feel of each page is similar to that for students, 
except that each page has a button or link near the bottom of the page labelled 'Instructor Only'.
Pressing this will lead to another page that is used to customize the content for your course.

