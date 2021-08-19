package Eledge;  // this tells the java compiler and servlet engine how to name each servlet

class Course {
  // these static parameters give the locations of the servlet server and class database server
  static String name = "Eledge"; // same as package name. no spaces or punctuation except hyphen
  static String server = "http://localhost:8080";    // full name of server where the servlets reside APACHE TOMCAT
  static String server2 = "http://localhost"; //full name of server where the HTML editor reside, APACHE
  static String outgoingMailServer = "127.0.0.1"; // outgoing SMTP mail server
  static String dbServer = "localhost";  // name of mysql server; use localhost or full name
  static String jdbcDriver = "org.gjt.mm.mysql.Driver"; // mysql-jdbc Connector class name
  static String mySQLUser = "root";    // mysql user with permissions on course database
  static String mySQLPass = "";    // mysql password
  // on the following two lines, indicate the absolute path to the upload and content directories
  // always use a forward-slash as the directory separator, even on MS Windows servers
  // the content directory is where the instructor uploads files that are accessible to students
  // the uploads directory is there the students upload files that are accessible to the instructor
  // use the Windows style:
  static String contentDirectory = "C:\\Program Files\\Apache Group\\Tomcat 4.1\\webapps\\ROOT\\Eledge\\Content\\";
  static String uploadsDirectory = "C:\\Program Files\\Apache Group\\Tomcat 4.1\\webapps\\ROOT\\Eledge\\Uploads\\";
  static String tempDirectory = "C:\\Program Files\\Apache Group\\Tomcat 4.1\\webapps\\ROOT\\Eledge\\Temp\\";
  // or use the unix style:
  //static String contentDirectory = "/usr/local/tomcat/webapps/ROOT/Eledge/content/";
  //static String uploadsDirectory = "/usr/local/tomcat/webapps/ROOT/Eledge/uploads/";
  // on the following line, use https if SSL is installed on the server; otherwise use http
  static String secureLoginURL = "https://" + server + "/servlet/" + name + ".Login?DestURL=" + name + ".";
  // this is the fully qualified name of the mysql database (normally no changes needed)
  static String dbName = "jdbc:mysql://" + dbServer + "/" + name;
  static int idLength = 0; // required number of digits in student id number
                           //use 0 to allow variable length and/or characters

  public String getServletInfo() {
    return "This Eledge class contains information about the server and database "
    + "configuration that is used by nearly all the other servlet modules in the package.";
  }
}
