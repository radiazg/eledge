package Eledge;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.Part;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.FilePart;

public class MultiPart {
	
protected Hashtable parameters = new Hashtable();
protected Hashtable files = new Hashtable();  
private String filename;
private boolean isRegistered = false;
private String filePath = null;
	
public MultiPart(HttpServletRequest request, int maxPostSize, String PathDirectory, String PathDirectoryWeb, Student student) throws IOException{
	
	// Save the dir
    File dir = new File(PathDirectory);
	
	MultipartParser parser = new MultipartParser(request, maxPostSize);
    
    Part part;
    while ((part = parser.readNextPart()) != null) {
      String name = part.getName();
      if (part.isParam()) {
        // It's a parameter part, add it to the vector of values
        ParamPart paramPart = (ParamPart) part;
        String value = paramPart.getStringValue();
        Vector existingValues = (Vector)parameters.get(name);
        if (existingValues == null) {
          existingValues = new Vector();
          parameters.put(name, existingValues);
        }
        existingValues.addElement(value);
      }
      else if (part.isFile()) {
        // It's a file part
        FilePart filePart = (FilePart) part;
        String fileName = filePart.getFileName();
        
        setFilePath(filePart.getFilePath());
        
        setFileName(filePart.getFileName());
        if (fileName != null) {
           // The part actually contained a file
          if(!isFileRegistered(fileName,PathDirectoryWeb)){
          //write file to directory
          filePart.writeTo(dir);
          }
          files.put(name, new UploadedFile(dir.toString(), 
                      						fileName, 
                      						filePart.getContentType()));
                      
          
        }
      }
    }	
}

public String getParameter(String name) {
    try {
      Vector values = (Vector)parameters.get(name);
      if (values == null || values.size() == 0) {
        return null;
      }
      String value = (String)values.elementAt(values.size() - 1);
      return value;
    }
    catch (Exception e) {
      return null;
    }
  }
  
public void setFileName(String fileName){
	this.filename = fileName;
}

public String getFileName(){
	return(this.filename);
}
  
public Enumeration getFileNames() {
    return files.keys();
  }

public File getFile(String name) {
    try {
      UploadedFile file = (UploadedFile)files.get(name);
      return file.getFile();  // may be null
    }
    catch (Exception e) {
      return null;
    }
  }
  
public String getFilesystemName(String name) {
    try {
      UploadedFile file = (UploadedFile)files.get(name);
      return file.getFilesystemName();  // may be null
    }
    catch (Exception e) {
      return null;
    }
  }

public boolean isFileRegistered(String filename, String path) {

try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
        
        ResultSet rsFile = stmt.executeQuery("SELECT * FROM files WHERE path='"+path+filename+"'");
        
        this.isRegistered = rsFile.next();
                
    }catch (Exception e) { 
    	//buf.append(e.getMessage());
    	
    }
 			
return this.isRegistered; 		
}

public void setisFileRegistered(boolean Registered) {
	this.isRegistered = isRegistered;
}

public boolean getisFileRegistered(){
	return (this.isRegistered);
}

public String getFilePath(){
	return this.filePath;
}

public void setFilePath(String Path){
	this.filePath = Path;
}

}


// A class to hold information about an uploaded file.
//
class UploadedFile {

  private String dir;
  private String filename;
  private String type;

  UploadedFile(String dir, String filename, String type) {
    this.dir = dir;
    this.filename = filename;
    this.type = type;
    
  }

  public String getContentType() {
    return type;
  }

  public String getFilesystemName() {
    return filename;
  }

  public File getFile() {
    if (dir == null || filename == null) {
      return null;
    }
    else {
      return new File(dir + File.separator + filename);
    }
  }
}


