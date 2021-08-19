package Eledge;

import java.io.*;
import java.util.*;

public class JSmethods {

  public static final String QUIZ = "quiz";
  public static final String EXAM = "exam";
  public static final String HW = "homework";
  public static final String REPORT = "report";
  public static final String REVIEW = "review";
  public static final String SURVEY = "survey";
  public static final String ASSIGNMENT = "assignment";
  RBStore res = EledgeResources.getJSmethodsBundle();
  private String testType;

  public JSmethods() {
	  testType=res.getString("str_assignment");
  }
  
  public JSmethods(String tt) {
          if (tt == this.QUIZ || tt == this.EXAM || tt == this.HW || tt == this.REPORT) {
	    testType=res.getString("str_"+tt);
	  } else testType=res.getString("str_assignment");//something good and generic. ;)
  }

  public String getTestType() {
	  return testType;
  }

  public void setTestType(String tt) {
	  if (tt == this.QUIZ || tt == this.EXAM || tt == this.HW || tt == this.REPORT) {
		  testType=res.getString("str_"+tt);
	  }
  }

  public void appendJSCheckHtml(StringBuffer buf){
    buf.append("<SCRIPT language=\"javascript\">\n<!--\nfunction parse_for_error(myString) {\n");
    buf.append("var i;\nvar j;\nvar k;\nvar sub;\ni=j=k=0;\nsub=myString;\n");
    buf.append("while (sub.indexOf('<') != -1) {\n");
    buf.append("if (sub.charAt(sub.indexOf('<')+1) !=' ')\n  i++;\n");
    buf.append("sub=sub.substring((sub.indexOf('<')+1),sub.length);\n");
    buf.append("}\nsub=myString;\nwhile (sub.indexOf('>') != -1) {\n");
    buf.append("j++;\nsub=sub.substring((sub.indexOf('>')+1),sub.length);\n");
    buf.append("}\nif (j<i) {\nalert('".concat(res.getString("str_missing_gt_error"))
     + "');\nreturn false;\n}\n");
    buf.append("sub=myString\nwhile (sub.indexOf('\"') != -1) {\n");
    buf.append("k++;\nsub=sub.substring((sub.indexOf('\"')+1),sub.length);\n");
    buf.append("}\nif ((k%2) != 0) {\nalert('".concat(res.getString("str_missing_sl_error")) + "');");
    buf.append("\nreturn false;\n}\n return true;\n}\n-->\n</SCRIPT>\n");
  }

  public void appendJSCheckInvalidChar(StringBuffer buf) {
    buf.append("<SCRIPT language=\"javascript\">\n<!--\n"
    + "function parse_for_spaces(myString) {\n"//spaces are the most common mistake made. Next would be #. So, we'll check for those here. The rest of the possibly funky characters are stripped out via stripInvalidChars in CharHider
    + "  if (myString.indexOf(' ')==-1 || myString.indexOf('#')==-1) {\n"
    + "    return false;\n"
    + "  } else {\n"
    + "    return true;\n"
    + "  }\n"
    + "}\n-->\n</SCRIPT>");
  }
}
