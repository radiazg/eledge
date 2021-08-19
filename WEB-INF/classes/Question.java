package Eledge;  // this tells the java compiler and servlet engine how to name each servlet
import java.io.*;
import java.sql.*;
import java.text.Collator;
import java.util.ResourceBundle;

class Question {
  String id;
  String text;
  String tag;
  String type;
  String section="All";
  int assignmentNumber;
  int subjectArea;
  int nChoices;
  String[] choices;
  double requiredPrecision;
  int pointValue;
  String[] answers;
  String correctAnswer1;
  String correctAnswer2;
  int columnNumber = 1;  // column number where the answer choices begin
//added new value for keeping track of grading...
  boolean graded = false;
  RBStore res=EledgeResources.getQuestionBundle(); //i18n

  public String getServletInfo() {
    return "This Eledge class provides the structure and methods for quiz/exam/homework/review questions.";
  }
  
  void loadQuestionData(ResultSet rs) {
    try {
      assignmentNumber = rs.getInt("AssignmentNumber");
      subjectArea = rs.getInt("SubjectArea");
      id = rs.getString("QuestionID");
      text = rs.getString("QuestionText");
      tag = rs.getString("QuestionTag");
      type = rs.getString("QuestionType");
      requiredPrecision = rs.getDouble("RequiredPrecision");
      nChoices = rs.getInt("NumberOfChoices");
      choices = new String[nChoices];
      ResultSetMetaData rsmd = rs.getMetaData();
      while (!rsmd.getColumnName(columnNumber).equals("ChoiceAText")) columnNumber++;
      for (int i=0; i < nChoices; i++) {
        choices[i] = rs.getString(columnNumber+i);
      }
      pointValue = rs.getInt("PointValue");
      correctAnswer1 = rs.getString("CorrectAnswer1");
      correctAnswer2 = rs.getString("CorrectAnswer2");
      section = rs.getString("Section");
    } catch (Exception e) {
    }
    return;
  }

  String print() {
    StringBuffer buf = new StringBuffer();
    char choice = 'a';
    switch (getQuestionType(type)) {
      case 1: // Multiple Choice
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_choice")).concat("</FONT><br>"));
        buf.append("<UL>");
        for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=radio name="+id+" value="+choice+">" + choices[i] + "<br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 2: // True/False
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_boolean")).concat("</FONT><br>"));
        buf.append("<UL>");
        buf.append("<input type=radio name="+id+" value='true'> True<br>");
        buf.append("<input type=radio name="+id+" value='false'> False<br>");
        buf.append("</UL>");
        break;
      case 3: // Select Multiple
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_select")).concat("</FONT><br>"));
        buf.append("<UL>");
        for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=checkbox name="+id+" value="+choice+">" + choices[i] + "<br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 4: // Fill-in-the-Word
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_fill_in")).concat("</FONT><br>"));
        buf.append("<input type=text name=" + id + ">");
        buf.append("<b>" + tag + "</b><br>");
        break;
      case 5: // Numeric Answer
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_numeric1")));
        buf.append(requiredPrecision + res.getString("str_help_numeric2") + "</FONT><br>");
        buf.append("<input type=text name=" + id + ">");
        buf.append("<b>" + tag + "</b><br>");
        break;        
      case 6: // Essay
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_essay")).concat("</FONT><br>"));
        buf.append("<TEXTAREA ROWS=10 COLS=50 WRAP=SOFT NAME=" + id + "></TEXTAREA><br>");
        break;
    }
    return buf.toString();
  }

  String printEdit(String myResponse) {
    StringBuffer buf = new StringBuffer();
    char choice = 'a';
    if (myResponse==null) myResponse = "";
    switch (getQuestionType(type)) {
      case 1: // Multiple Choice
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_choice")).concat("</FONT><br>"));
        buf.append("<UL>");
        for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=radio name=" + id + " value=" + choice);
	  if (myResponse.indexOf(choice) >= 0)
	    buf.append(" CHECKED");
          buf.append(">" + choices[i] + "<br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 2: // True/False
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_boolean")).concat("</FONT><br>"));
        buf.append("<UL>");
        buf.append("<input type=radio name="+id+" value='true'"
         + (myResponse.equals("true")?" CHECKED>":">") + "True<br>");
        buf.append("<input type=radio name="+id+" value='false'"
         + (myResponse.equals("false")?" CHECKED>":">") + "False<br>");
        buf.append("</UL>");
        break;
      case 3: // Select Multiple
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_select")).concat("</FONT><br>")); buf.append("<UL>");
	for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=checkbox name=" + id + " value=" + choice
           + (myResponse.indexOf(choice)>=0?" CHECKED>":">") + choices[i] + "<br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 4: // Fill-in-the-Word
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_fill_in")).concat("</FONT><br>"));
        buf.append("<input type=text name=" + id + " value=\"" + myResponse + "\">");
        buf.append("<b>" + tag + "</b><br>");
        break;
      case 5: // Numeric Answer
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_numeric1")));
        buf.append(requiredPrecision + res.getString("str_help_numeric2") + "</FONT><br>");
        buf.append("<input type=text name=" + id + " value=\"" + myResponse + "\">");
        buf.append("<b>" + tag + "</b><br>");
        break;        
      case 6: // Essay
        buf.append("<b>" + text + "</b><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_essay")).concat("</FONT><br>"));
        buf.append("<TEXTAREA ROWS=10 COLS=80 WRAP=SOFT NAME=" + id + ">" + myResponse 
        + "</TEXTAREA><br>");
        break;
    }
    return buf.toString();
  }

  String printCorrection(String myResponse) {
    String str=printEdit(myResponse);
    if ((getQuestionType(type))==6) return str; //Essay
    StringBuffer buf = new StringBuffer(str);
    if (isCorrect(myResponse)) 
	buf.append("<br><i><FONT COLOR=0000FF>".concat(res.getString("str_answered_correctly")).concat("</FONT></i><br><br>"));
    else
	buf.append("<br><i><FONT COLOR=FF0000>".concat(res.getString("str_answered_incorrectly")).concat(getCorrectAnswer()).concat("</FONT></i><br><br>"));
    return buf.toString();
  }


  String printView(String myResponse) {
    StringBuffer buf = new StringBuffer();
    char choice = 'a';
    if (myResponse==null) myResponse = "null";
    switch (getQuestionType(type)) {
      case 1: // Multiple Choice
        buf.append("<b>" + text + "</b><br><UL>");
        if (myResponse.equals("null")) return buf.toString() + "(no response)</UL>";
        for (int i=0;i<nChoices;i++) {
          if (myResponse.indexOf(choice)>=0) buf.append("<li>" + choices[i]);
          choice++;
        }
        buf.append("</UL>");
        break;
      case 2: // True/False
        buf.append("<b>" + text + "</b><br><UL>");
        if (myResponse.equals("null")) return buf.toString() + "(no response)</UL>";
        buf.append("<li>" + (myResponse.equals("true")?"True":"False") + "</UL>");
        break;
      case 3: // Select Multiple
        buf.append("<b>" + text + "</b><br><UL>");
        if (myResponse.equals("null")) return buf.toString() + "(no response)</UL>";
        for (int i=0;i<nChoices;i++) {
          if (myResponse.indexOf(choice)>=0) buf.append("<li>" + choices[i]);
          choice++;
        }
        buf.append("</UL>");
        break;
      case 4: // Fill-in-the-Word
        if (myResponse.equals("null") || myResponse.equals("")) myResponse = "(no response)";
        buf.append("<b>" + text + "</b>");
        buf.append("<UL><PRE>" + myResponse + "</PRE></UL>");
        buf.append("<b>" + tag + "</b><br>");
        break;
      case 5: // Numeric Answer
        if (myResponse.equals("null") || myResponse.equals("")) myResponse = "(no response)";
        buf.append("<b>" + text + "</b> ");
        buf.append("<UL>" + myResponse + "</UL>");
        buf.append(" <b>" + tag + "</b>");
        break;        
      case 6: // Essay
        buf.append("<b>" + text + "</b><br>");
        if (myResponse.equals("null") || myResponse.equals(""))
          return buf.toString() + "<UL>(no response)</UL>";
        buf.append("<UL><PRE>" + myResponse + "</PRE></UL>");
        break;
    }
    return buf.toString();
  }

  String edit() {
    String[] choiceNames = {"ChoiceAText","ChoiceBText","ChoiceCText","ChoiceDText","ChoiceEText"};
    StringBuffer buf = new StringBuffer();
    char choice = 'a';
    /*buf.append("\nAssignment:<input size=3 name=AssignmentNumber value=" + assignmentNumber + ">&nbsp;"
    + "Subject Area:<input size=3 name=SubjectArea value=" + subjectArea + ">"
    + "<br>" + displaySectionInfo() + "<br>"*/
    buf.append("\n<input type='hidden' name=AssignmentNumber value=" + assignmentNumber + ">"
    + "<input type=hidden name=QuestionID value=" + id + ">"
    + "<input type=hidden name=QuestionType value=" + type + ">"
    + "<input type=hidden name=UserRequest value=Edit>"
    + "<input type=hidden name=PointValue value=" + pointValue + ">");
    switch (getQuestionType(type)) {
      case 1: // Multiple Choice
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_choice")).concat("</FONT>").concat("<br>Number of choices (max=5):<input size=1 name=NumberOfChoices value=").concat(String.valueOf(nChoices)).concat("><br>"));
        buf.append("<UL>");
        for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=radio name=CorrectAnswer1 value=" + choice);
          if (correctAnswer1.indexOf(choice) >= 0) buf.append(" CHECKED");
          buf.append("><input size=30 name=" + choiceNames[i] + " value='" 
          + CharHider.quot2html(choices[i]) + "'><br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 2: // True/False
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_boolean")).concat("</FONT><br>"));
        buf.append("<UL>");
        buf.append("<input type=radio name=CorrectAnswer1 value='true'");
        if (correctAnswer1.equals("true")) buf.append(" CHECKED");
        buf.append("> True<br>");
        buf.append("<input type=radio name=CorrectAnswer1 value='false'");
        if (correctAnswer1.equals("false")) buf.append(" CHECKED");
        buf.append("> False<br>");
        buf.append("</UL>");
        break;
      case 3: // Select Multiple
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_multiple_select")).concat("</FONT>")
        + "<br>Number of choices (max=5):<input size=1 name=NumberOfChoices value=" + nChoices + "><br>");
        buf.append("<UL>");
        for (int i = 0; i < nChoices; i++) {
          buf.append("<input type=checkbox name=CorrectAnswer1 value=" + choice);
          if (correctAnswer1.indexOf(choice) >= 0) buf.append(" CHECKED");
          buf.append("><input size=30 name=" + choiceNames[i] + " value='" + choices[i] + "'><br>");
          choice++;
        }
        buf.append("</UL>");
        break;
      case 4: // Fill-in-the-Word
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_fill_in")).concat("</FONT><br>"));
        buf.append("<input type=text name=CorrectAnswer1 value=\"" + correctAnswer1 + "\">&nbsp;or&nbsp;");
        buf.append("<input type=text name=CorrectAnswer2 value=\"" + (correctAnswer2==null?"":correctAnswer2) + "\"><br>");
        buf.append("<TEXTAREA name=QuestionTag rows=5 cols=50 wrap=soft>" + tag + "</TEXTAREA><br>");
        break;
      case 5: // Numeric Answer
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_numeric1")));
        buf.append("<input size=4 name=RequiredPrecision value=" + requiredPrecision + ">");
        buf.append(res.getString("str_help_numeric2").concat("</FONT><br>"));
        buf.append("<input type=text size=30 name=CorrectAnswer1 value='" + correctAnswer1 + "'><br>");
        buf.append("<TEXTAREA name=QuestionTag rows=5 cols=50 wrap=soft>" + tag + "</TEXTAREA><br>");
        break;
      case 6: // Essay
        buf.append("<TEXTAREA name=QuestionText rows=5 cols=50 wrap=soft>" + text + "</TEXTAREA><br>");
        buf.append("<FONT SIZE=-2 COLOR=FF0000>".concat(res.getString("str_help_essay")).concat("</FONT><br>"));
        buf.append("<TEXTAREA name=CorrectAnswer1 rows=10 cols=50 wrap=soft>" + correctAnswer1 + "</TEXTAREA><br>");
        break;
    }
//    buf.append("<input type=submit value='Update This Question'>");
    buf.append("<input type=button value='Update This Question' ");
    appendOnClick(buf);
    buf.append("><input type=button value='Delete This Question' "
    + "onClick=javascript:UserRequest.value='DeleteQuestion';submit();>");
    return buf.toString();
  }

  void appendOnClick(StringBuffer buf) {
    String[] choiceNames = {"ChoiceAText","ChoiceBText","ChoiceCText","ChoiceDText","ChoiceEText"};
    buf.append("onClick=\"if (");
    switch(getQuestionType(type)) {
      case 1: //multiplechoice
      case 3: //select multiple...
        for (int i = 0; i < nChoices; i++) {
          buf.append("parse_for_error(this.form.elements." 
	  + choiceNames[i] + ".value) && ");
	}
	break;
      case 4://fill-in-blank... runs through to case 6 for correct answer1...
	buf.append("parse_for_error(this.form.elements.CorrectAnswer2.value) && ");
	buf.append("parse_for_error(this.form.elements.QuestionTag.value) && ");
      case 6:
	buf.append("parse_for_error(this.form.elements.CorrectAnswer1.value) && ");
	break;
      case 2://true/false... nothing to do...
      case 5://numeric... nothing to do...
      default:
	break;
    }
    buf.append("parse_for_error(this.form.elements.QuestionText.value))");
    buf.append(" { this.form.submit(); }\"");
  }

  int getQuestionType(String questionType) {
    if (questionType.equals("MULTIPLE_CHOICE")) return 1;
    if (questionType.equals("TRUE_FALSE")) return 2;
    if (questionType.equals("SELECT_MULTIPLE")) return 3;
    if (questionType.equals("FILL_IN_WORD")) return 4;
    if (questionType.equals("NUMERIC")) return 5;
    if (questionType.equals("ESSAY")) return 6;
    return 0;
  }
//new getQuestionType method is public for access in test classes.
//returns -1 for invalid or null type, otherwise, it returns the number.  
  public int getQuestionType() {
    if (type.equals("") || type==null)
      return -1;
    if (type.equals("MULTIPLE_CHOICE"))
      return 1;
    if (type.equals("TRUE_FALSE"))
      return 2; 
    if (type.equals("SELECT_MULTIPLE"))
       return 3;
    if (type.equals("FILL_IN_WORD"))
       return 4;
    if (type.equals("NUMERIC"))
       return 5;
    if (type.equals("ESSAY"))
       return 6;
    return -1;
  }
  public String getID() {
    return id;
  }

  public boolean isCorrect(String answer){
    graded=true;
    switch (getQuestionType(type)) {
      case 4:  // Fill-in-the-word
        Collator compare = Collator.getInstance();
        compare.setStrength(Collator.PRIMARY);
        return correctAnswer2==null?compare.equals(answer,correctAnswer1):compare.equals(answer,correctAnswer1)||compare.equals(answer,correctAnswer2);
      case 5: // Numeric Answer
        try {
          double dAnswer = Double.parseDouble(answer);
          double dCorrectAnswer1 = Double.parseDouble(correctAnswer1);
          return Math.abs((dAnswer-dCorrectAnswer1)/dCorrectAnswer1)*100 <= requiredPrecision?true:false;
        }
        catch (Exception e) {
          return false;
        }
      default:
        return correctAnswer1.equals(answer);
    }
  }

  public int getPointValue(){
    return pointValue;
  }
  
  //new method added 9/4/02 by RDZ
  public boolean getQuestionGraded(){
    return graded;
  }
  //new method added 9/6/02 by RDZ
  public String getCorrectAnswer(){
    StringBuffer buf = new StringBuffer();
    if (correctAnswer1 != null && correctAnswer2 != null ){
      buf.append(correctAnswer1 + " or " + correctAnswer2);
      return buf.toString(); 
    }
    return correctAnswer1; 
  }

  //new method added 10/28/02 by RDZ
  String displaySectionInfo(){
    StringBuffer buf = new StringBuffer("CourseSection: <SELECT NAME=Section>");
    try {
      Class.forName(Course.jdbcDriver).newInstance();
      Connection conn = DriverManager.getConnection(Course.dbName,Course.mySQLUser,Course.mySQLPass);
      Statement stmt = conn.createStatement();
      ResultSet rsSections = stmt.executeQuery("SELECT * from CourseParameters WHERE Name='NumberOfSections'");
      if (!rsSections.next() || rsSections.getInt("Value")==1)
       return "<INPUT TYPE=hidden NAME='Section' VALUE='All'>";
      //sections start w/ 1, therefore, i=1... RDZ
      if (section != null && section.equals("All"))
        buf.append("<OPTION selected>All");
      else
        buf.append("<OPTION>All");
      int numSections=rsSections.getInt("Value");
      for(int i=1; i<=numSections; i++){
        if (section != null && section.equals(String.valueOf(i)))
          buf.append("<OPTION selected>" +i);
        else
          buf.append("<OPTION>" + i);
      }
      buf.append("</SELECT>");
      rsSections.close();
      stmt.close();
      conn.close();
    }catch (Exception e){
     return "<INPUT TYPE=hidden NAME='Section' Value='1'></TD></TR>";
    }
    return buf.toString();
  }
 
}
