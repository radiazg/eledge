package Eledge;  // this tells the java compiler and servlet engine how to name each servlet

class CharHider {

  public String getServletInfo() {
    return "This Eledge servlet module is used to replace single quotation mark characters with "
    + "the string literal equivalent (for entering strings into the database) or with the HTML "
    + "special character equivalent (for including single quotes in web form elements).";  
  }
  
  static Logger log = new Logger();
  static String quot2html(String oldString) {
    if (oldString == null) return "";
  // recursive method replaces single quotes with &#39; for HTML pages
    log.paranoid("oldString: " + oldString,"CharHider:quot2html(string)");
    int i = oldString.indexOf('\'',0);
    return i<0?oldString:quot2html(new StringBuffer(oldString).replace(i,i+1,"&#39;").toString(),i);
  }

  static String quot2html(String oldString,int fromIndex) {
  // recursive method replaces single quotes with &#39; for HTML pages
    int i = oldString.indexOf('\'',fromIndex);
    log.paranoid("oldString: " + oldString,"CharHider:quot2html(string,int)");
    return i<0?oldString:quot2html(new StringBuffer(oldString).replace(i,i+1,"&#39;").toString(),i);
  }

  static String quot2literal(String oldString) {
    if (oldString == null) return "";
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',0);
    return i<0?oldString:quot2literal(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }

  static String quot2literal(String oldString, int fromIndex) {
  // recursive method inserts backslashes before all apostrophes
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:quot2literal(new StringBuffer(oldString).insert(i,'\\').toString(),i+2);
  }

  //following code based off an idea by Ian McFarland
  //original code can be found on: 
  //http://www.purpletech.com/code/src/com/purpletech/util/Utils.java
  public static String curlQuote2Html(String input) {
    if (input==null)
      return "";
    log.paranoid("input: " + input,"CharHider:curlQuote2Html");
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < input.length(); i++) {
      char ch = input.charAt(i);
      int code = (int) ch;
      switch (code) {
	case 147: 
        case 210:
	  buf.append("&ldquo;");
	  break;
	case 148:
	case 211:
          buf.append("&rdquo;");
          break;
        case 145:
	case 212:
          buf.append("&lsquo;");
	  break;
        case 146:
	case 213:
          buf.append("&rsquo;");
	  break;
	default:
          buf.append(ch);
      }
    }
    String ret = buf.toString();
    log.paranoid("Returning: " + ret,"CharHider:curlQuote2Html");
    return ret;
  }

  //single quote(') to double quote(")
  static String squot2dquot(String oldString) {
    if (oldString == null)
      return "";
    int i = oldString.indexOf('\'',0);
    return i<0?oldString:squot2dquot(new StringBuffer(oldString).replace(i,i+1,"\"").toString(),i); 
  }
 
  static String squot2dquot(String oldString,int fromIndex) {
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:squot2dquot(new StringBuffer(oldString).replace(i,i+1,"\"").toString(),i);
  }

  static String dquot2html(String oldString) {
    if (oldString == null)
      return "";
    int i = oldString.indexOf('\"',0);
    return i<0?oldString:squot2dquot(new StringBuffer(oldString).replace(i,i+1,"&#34").toString(),i); 
  }
 
  static String dquot2html(String oldString,int fromIndex) {
    int i = oldString.indexOf('\'',fromIndex);
    return i<0?oldString:squot2dquot(new StringBuffer(oldString).replace(i,i+1,"&#34").toString(),i);
  }
//strips all characters invalid in a column name out of a string.
//This prepares the string for "safe" alterations of tables.
  static String stripInvalidChars(String oldString) {
    StringBuffer buf = new StringBuffer("");
    char c;
    for (int i=0; i<oldString.length(); i++) {
      c = oldString.charAt(i); 
      if ((c>=48 && c<=57) //numbers
          || (c>=65 && c<=90) //A-Z
          || (c>=97 && c<=122) //a-z
          || (c==95)) //underscore
        buf.append(oldString.charAt(i));
    }
    return buf.toString();
  }
}

