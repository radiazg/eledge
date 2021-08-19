package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.StringTokenizer;
import java.util.zip.*;

public class ContentPacking extends HttpServlet {

private String resourses="\n					<resource identifier='MANIFEST02_ORG1' type='webctproperties'>"
      					+"\n					<file href='resources/data/properties_MANIFEST01_ORG1.xml'/>"
    					+"\n				</resource>";
    					
private String directoryDefault="";
private String pathDir = "";
private String idPackage = "";
private String directoryRandomName = randomCode();
    
 public boolean exportContent(String themeid){
	 	
 	try{
  		
  		this.idPackage = themeid;
  		this.directoryDefault = Course.tempDirectory+"ims"+directoryRandomName+"\\resources\\";
  		pathDir = Course.tempDirectory+"ims"+directoryRandomName;
  		
  		//crea el objeto prueba de tipo File, en la carpeta de exportacion  
   		File prueba = new File(this.directoryDefault);
		prueba.mkdirs();//crea el directorio
		//creal el archivo en blanco xml
		File xml = new File(Course.tempDirectory+"ims"+directoryRandomName+"\\imsmanifest.xml");
		FileWriter salida = new FileWriter(xml);
		
		StringBuffer buf = new StringBuffer();
  		
  		
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    
    	ResultSet rsSelectCourse = stmt.executeQuery("SELECT sectionname FROM courses where sectionid='"+themeid+"'");
        rsSelectCourse.next();
        
        //almacena en el bufferString, los parametros IMS antes de guardarlos en el archivo manifest de xml 
        //para luego realizar la exportacion
        buf.append("\n<organizations>"
+"\n		<organization identifier='MANIFEST01_ORG1'>"
+"\n			<webct:properties identifierref='MANIFEST02_ORG1'/>"
//+"\n			<title>"+rsSelectCourse.getString("sectionname")+"</title>"
			+organizationXML(themeid)
+"\n			</organization>"
+"\n		</organizations>"
 	
+"\n			<resources>"
			+ getResourses()
+"\n			</resources>"
+"\n		</manifest>");
		
        //escribe el archivo xml
		salida.write(headerManifestXML(rsSelectCourse.getString("sectionname"))+buf.toString());
		salida.close();
       	
       	//crea archivo xml, segundo manifiesto 
       	headerManifestXML();       	
       }
    catch(IOException e){
    	//out.print(e.getMessage());
    	return (false);
    }
    catch(Exception e){
    	//out.print(e.getMessage());
    	return (false);
    } 
 //comprime el contenido
 zipFiles (pathDir,themeid);
 return true;
 	
 }
 
 public String getZipFile () {
 	return(Course.server+"/"+Course.name+"/Temp/ims"+directoryRandomName+"/ims"+this.idPackage+".zip");
 }
 
 //imprime organization, para los temas
 private String organizationXML(String themeid){
 StringBuffer buf = new StringBuffer();
 		
 		try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	ResultSet rsSelectContent = stmt.executeQuery("SELECT * FROM content WHERE coursesection='"+themeid+"' ORDER BY theme ASC"); 
    	
    	while(rsSelectContent.next()) {
			
			//envia datos al string resourses, para ir creando el texto de exportacion, 
			//para el contenido de resources
			setResourses(rsSelectContent.getString("content_id"),rsSelectContent.getString("description"), false);
			
			//crea el archivo 
			makeFile(rsSelectContent.getString("title"), rsSelectContent.getString("description"), rsSelectContent.getString("content_id"));
			
			//parametros del manifest, para los temas
			buf.append(""
+"\n                    <item identifier='MANIFEST01_ITEM"+rsSelectContent.getString("content_id")+"' identifierref='MANIFEST01_RESOURCE"+rsSelectContent.getString("content_id")+"'>"
+"\n                        <title>"+rsSelectContent.getString("title")+"</title>"
	                   		+organizationXML1(rsSelectContent.getString("theme")) 
+"\n                    </item>");
			}
    	
    	}catch(Exception e){
    		e.getMessage();
    	}
 
 return(buf.toString());
 }
 
//imprime la segunda parte de organization, para los contenidos de los temas
private String organizationXML1(String idtheme){
StringBuffer buf = new StringBuffer();
 		
 		try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	ResultSet rsSelectContentTHEME = stmt.executeQuery("SELECT * FROM contenttotheme WHERE coursesection_id='1' and id_theme='"+idtheme+"' ORDER BY id_sec ASC"); 

							
							while(rsSelectContentTHEME.next()) {
							
							//envia datos al string resourses, para ir creando el texto de exportacion
							setResourses(rsSelectContentTHEME.getString("id_tc"), rsSelectContentTHEME.getString("body"), true);
							
							//crea el archivo 
							makeFile(rsSelectContentTHEME.getString("title"), rsSelectContentTHEME.getString("body"), "sub"+rsSelectContentTHEME.getString("id_tc"));	
							//parametros de los contenidos de los temas
buf.append("" 		
+"\n                        <item identifier='MANIFEST01_ITEMSUB"+rsSelectContentTHEME.getString("id_tc")+"' identifierref='MANIFEST01_RESOURCESUB"+rsSelectContentTHEME.getString("id_tc")+"'>"
+"\n                                <title>"+rsSelectContentTHEME.getString("title")+"</title>"
+"\n                        </item>");
							}
 		
 		}catch(Exception e){
 			e.getMessage();
 		}
 return(buf.toString());
 }

//parametros de los recursos del curso, como ejemplo, la ruta donde 
//se encuentran registrados los archivos html,  
 private void setResourses(String numberResourse, String htmlBody, boolean isSUB){
 
StringBuffer buf = new StringBuffer();

 				 if(!isSUB)
buf.append("\n					<resource identifier='MANIFEST01_RESOURCE"+numberResourse+"' type='webcontent' href='resources/"+numberResourse+".html'>");
				 else
buf.append("\n					<resource identifier='MANIFEST01_RESOURCESUB"+numberResourse+"' type='webcontent' href='resources/sub"+numberResourse+".html'>");
					
//buf.append("\n					<metadata/>");
				if(!isSUB)
buf.append("\n					<file href='resources/"+numberResourse+".html'/>");
				else
buf.append("\n					<file href='resources/sub"+numberResourse+".html'/>");	

//encuentra los recursos que necesita el documento html y los inserta
//en la variable resources.
buf.append(existInTable(htmlBody));
				
buf.append("\n					</resource>");


 	this.resourses += buf.toString(); 
 }
 
 //retorna lo almacenado por el string resourses, para escribirlo en el archivo
 //xml
 private String getResourses(){
 	return (this.resourses);
 }
 
 //crea los archivos html, de los contenidos y temas del curso
 private void makeFile(String title, String body, String fileName){
 	StringBuffer buf = new StringBuffer();
 	
 	try{
 	
 	File htm = new File(this.directoryDefault+fileName+".html");
	FileWriter salida = new FileWriter(htm);
	
	buf.append(""
	+"<html>"
	+"	<head>"
	+"	<title>"+title+"</title>"
	+"	</head>"
	+"	<body>"
	+bodyForExport(body)
	+"	</body>"
	+"</html>"
	);
		
	salida.write(buf.toString());
	salida.close();
	}catch(IOException e){
		e.getMessage();
	}
		
 }
 
 //cabecera del archivo manifest XML
 private String headerManifestXML(String courseName){
  	
  	return("<?xml version='1.0' encoding='ISO-8859-1'?>"

+"\n<!-- WebCT XML Content generated by WebCT Content Packaging API -->"

+"\n<manifest identifier='resources' version='1.0'"
+"\n  xmlns='http://www.imsproject.org/content'"
+"\n  xmlns:webct='http://www.webct.com/IMS'>"

+"\n  <metadata>"
+"\n    <schema>WebCT Content</schema>"
+"\n    <schemaversion>2.0</schemaversion>"
+"\n    <lom xmlns='http://www.imsproject.org/metadata'>"
+"\n      <general>"
+"\n        <title>"
+"\n          <langstring xml:lang='en-US'>"+courseName+"</langstring>"
+"\n        </title>"
+"\n      </general>"
+"\n      <educational>"
+"\n        <learningresourcetype>"
+"\n          <source>"
+"\n            <langstring xml:lang='x-none'>Eledge</langstring>"
+"\n          </source>"
+"\n          <value>"
+"\n            <langstring xml:lang='x-none'>Content Module</langstring>"
+"\n          </value>"
+"\n        </learningresourcetype>"
+"\n      </educational>"
+"\n    </lom>"
+"\n  </metadata>");
  	
  }
  
//cabecera del archivo manifest02 XML
 private void headerManifestXML(){
 
 try{
 	//crea directorios
 	File dirs = new File(this.directoryDefault+"\\data\\");
	dirs.mkdirs();
	
	//crea archivo xml
	File xml = new File(this.directoryDefault+"\\data\\properties_MANIFEST01_ORG1.xml");
	FileWriter salida = new FileWriter(xml);
		
 	
 String buf = ("<?xml version='1.0' encoding='ISO-8859-1'?>"

+"\n<!--"
+"\n**************************************************"
+"\nWebCT 959112278 Content Module properties"
+"\ngenerated by Eledge Content Packaging"
+"\n**************************************************"
+"\n-->"

+"\n<properties xmlns='http://Eledge.org/'"
+"\n            type='Content Module Properties'>"
+"\n  <actionmenu align='top'>"
+"\n    <annotationsbutton/>"
+"\n    <searchbutton/>"
+"\n    <discussionsbutton/>"
+"\n    <glossarybutton/>"
+"\n  </actionmenu>"
+"\n  <toc numbering='yes' initial='1' isfirstpage='yes'"
+"\n       visibleincontent='no'>"

+"\n  <!--***** Customized Table of Contents (within toc tag) *****-->"
+"\n  <colorset propertyref='content module'/>"

+"\n  </toc>"


+"\n  <!--***** Defaults for all content pages *****-->"
+"\n  <colorset propertyref='content module'/>"
+"\n</properties>");


	salida.write(buf.toString());
	salida.close();
 }
 catch(IOException e){
 	e.getMessage();
 }
 
 
 }
 
 
 //Comprime archivos del ContenPaking
 
 private void zipFiles (String ruta, String idTheme) {
 	
	String tempDirs[]= new String [1500];
	int j = 0, k = 1;
	tempDirs[0] = ruta;
	String dirs = "@";
	String pathFiles [];
			

	//Busca folders de la ruta....		
	do {		
		File tempFiles = new File (tempDirs[j]);
		File [] lista;			
		lista = tempFiles.listFiles();
		
		for (int i=0; i< lista.length; i++){				
			if(lista[i].isDirectory()){
				tempDirs[k++] = lista[i].getAbsolutePath();									
			}		
		}
		j++;				
			 
	}while (tempDirs[j] != null);
	
	//Busca Archivos de los folders encontrados y los almazena en un
	//String
	
	j=0;
	do {	
		File tempFiles = new File (tempDirs[j]);
		File [] lista;			
		lista = tempFiles.listFiles();
		
		for (int i=0; i< lista.length; i++){				
			if(lista[i].isFile()){
				dirs = dirs + lista[i].getAbsolutePath()+"@";									
			}		
		}
		j++;				
			 
	}while (tempDirs[j] != null);
			

	//Calcula numero de archivos.....	
	int numFiles = 0;
	String temp;
	StringTokenizer dirsFiles = new StringTokenizer (dirs);
	
	while (dirsFiles.hasMoreTokens()) {
		numFiles++;
 		temp = dirsFiles.nextToken("@");
	}
	
	//Asigna path de archivos al arreglo String
 	pathFiles = new String [numFiles];
 	j=0;
 	StringTokenizer dirsFiles2 = new StringTokenizer (dirs);
 	while (dirsFiles2.hasMoreTokens()) {			
     	pathFiles[j++] = dirsFiles2.nextToken("@");
    }
 	
 	//Creando Archivo Zip
 	// Crea un buffer para leer los archivos
	byte[] buf = new byte[1024];

	try {
	    // Create the ZIP file
	    String rut="";
	    String outFilename = Course.tempDirectory+"ims"+directoryRandomName+"\\ims"+idTheme+".zip";
	    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
	
	    // Comprime los archivos
	    for (int i=0; i<pathFiles.length; i++) {
	        FileInputStream in = new FileInputStream(pathFiles[i]);
	
	        // Agrega la entrada ZIP al output stream.
	        rut = pathFiles[i];
	        out.putNextEntry(new ZipEntry(rut.substring(tempDirs[0].length()+1)));
	
	        // Transfiere bytes de el archivo al archivo ZIP 
	        int len;
	        while ((len = in.read(buf)) > 0) {
	            out.write(buf, 0, len);
	        }
	
	        // Completa la entrada
	        out.closeEntry();
	        in.close();	        
	    }
	
	    // Completa el Archivo Zip
	    out.close();	    
	} catch (IOException e) {
		e.getMessage();
	}
 }
 
 //realiza un código randomico
 private String randomCode() {
    String choice = "23456789abcdefghijkmnopqrstuvwxyzABCDEFGHIJKLMNPQRSTUVWXYZ";
    int size = 4;
    Random rand = new Random();
    StringBuffer code = new StringBuffer();
    for (int i=0;i<size;i++) {
      code.append(choice.charAt(rand.nextInt(choice.length())));
    }
    return code.toString();
  }
  
//devuelve los recursos, tales como imagenes, u otros que subio para el html
private String existInTable(String htmlBody){

StringBuffer buf = new StringBuffer();

	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	ResultSet rsListTable = stmt.executeQuery("SELECT name FROM files ORDER BY name ASC"); 
    	
    	while(rsListTable.next()) {
    		
			if (rsListTable.isFirst()) {
				File folder = new File (this.directoryDefault+"Eledge\\Content\\"+this.idPackage+"\\");
				folder.mkdirs();    			
    		}
    		
    	
    		if(htmlBody.indexOf(rsListTable.getString("name")) > (-1)) { 
    			buf.append("\n					<file href='resources/Eledge/Content/"+this.idPackage+"/"+rsListTable.getString("name")+"'/>");
    			copyFiles (Course.contentDirectory+this.idPackage+"\\"+rsListTable.getString("name"), this.directoryDefault+"Eledge\\Content\\"+this.idPackage+"\\"+rsListTable.getString("name"));
    		}
    			
    		
    		
    	}
    		
    	
    }catch(Exception e){
    	e.getMessage();
    }
  
  return (buf.toString());
}

private void copyFiles (String src, String dst) throws IOException {
	
	InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
    }
    
    in.close();
    out.close();
}

private String bodyForExport(String htmlBody){
	
StringBuffer buf = new StringBuffer();
	
	try{
  		Class.forName(Course.jdbcDriver).newInstance();
    	Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
    	Statement stmt = conn.createStatement();
    	
    	ResultSet rsListTable = stmt.executeQuery("SELECT path FROM files ORDER BY name ASC"); 
    	
    	buf.append(htmlBody);
    	//buf.append(buf.indexOf(rsListTable.getString("name")));
    	while(rsListTable.next()) {	
    		if(buf.indexOf(rsListTable.getString("path")) > (-1))
    			buf.deleteCharAt(buf.indexOf(rsListTable.getString("path"))); 
    		 		
    		
    	}	
    }catch(Exception e){
    	e.getMessage();
    }
    
return buf.toString();
}

}