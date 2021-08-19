package Eledge;

import java.sql.*;

public class WebPage {
  private int section;
  private String pageID;
  private String heading;
  private String htmlText;
  private String courseSection;
  private Logger log = new Logger();

  public WebPage(ResultSet rs) {
    try {
      section=rs.getInt("Section");
      heading=rs.getString("Heading");
      pageID=rs.getString("PageID");
      htmlText=rs.getString("HTMLText");
      courseSection=rs.getString("CourseSection");
    } catch (Exception e) { 
      log.sparse("Caught: " + e.getMessage(),"WebPage:WebPage(rs)");
    }
  }

  public String getPageID() {
    return pageID;
  }

  public int getSection() {
    return section;
  }

  public String getHeading() {
    return heading;
  }

  public String getHTMLText() {
    return htmlText;
  }

  public String getCourseSection() {
    return courseSection;
  }

}
