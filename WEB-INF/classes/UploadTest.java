package Eledge;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;

public class UploadTest extends HttpServlet{
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) 
			throws ServletException, IOException {
				
				res.setContentType("text/html");
				PrintWriter out = res.getWriter();
				
				try{
					MultipartRequest multi = new MultipartRequest(req,Course.contentDirectory, 5 * 1024 * 1024);
					
					out.print("<body>");
					out.print("<h1>bajo test</h1>");
					out.print("<h3>Params:</h3>");
					out.print("<pre>");
					Enumeration params = multi.getParameterNames();
					while (params.hasMoreElements()){
						String name = (String)params.nextElement();
						String value = multi.getParameter(name);
						out.print(name+ " = " + value);
					}
					out.print("</pre>");
					
					out.print("<h3>files:</h3>");
					out.print("<pre>");
					Enumeration files = multi.getFileNames();
					while(files.hasMoreElements()){
						String name = (String)files.nextElement();
						String filename = multi.getFilesystemName(name);
						String type = multi.getContentType(name);
						File f = multi.getFile(name);
						out.print("name: " + name);
						out.print("filename: " + filename);
						out.print("type: " + type);
						if (f != null) {
							out.print("length: " + f.length());
						}
					}
					
					out.print("</pre>");
				}catch(Exception e) {
					out.print("<pre>");
					e.printStackTrace();
					out.print("</pre>");
				}
							
					out.print("</body>");	
	}
				
}
		
