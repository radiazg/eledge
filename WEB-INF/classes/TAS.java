/************************************** TAS.java *******************************
 * Author: Robert Zeigler
 * Started: 8/12/03
 * Last Modified: 8/12/03
 * About: This class is to TA's what Permissions.java is to Permission.java
 * Keeps a running list of active TA's. Because there are 122 permissions
 * (at the time of starting this class. More may show up later as/if I expand
 * the permission set), and because, everytime we go to ManageTAPermissions, or
 * every time someone loads a page who's a ta, there's a -ton- of db processing
 * and hashing going on. To avoid that, this class has a static hashmap of
 * TA information. The "add" method is, necessarily, synchronized.
 * ****************************************************************************/
package Eledge;

import java.sql.*;
import java.util.*;

public class TAS {
  private static HashMap taList=null;

  public static TA getTA(String taName) {
    if (taList==null)
      taList = new HashMap();
    TA ta = (TA) taList.get(taName);
    if (ta==null) {
      ta = new TA(taName);
      add(taName,ta);
    }
    return ta;
  }

  private static synchronized void add(String taName, TA ta) {
    if (taList==null)
      taList=new HashMap();
    if (!ta.getID().equals("") && ta.getIsTA()) {
        taList.put(taName, ta);
    }
  }
}
