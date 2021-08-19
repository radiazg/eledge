package Eledge;

import java.util.*;
import javax.servlet.*;
//RBStore (ResourceBundleStore) Based off of "ResourBundleStore" class/example
//provided in article found at: 
//http://www.webdevelopersjournal.com/articles/internationalizing_servlets.html
//for optimizing i18n'ed servlet performance. Original article/code written by
//Mark Webber, June 6 2000. Modified for Eledge use by Robert Zeigler,
//September of 2003.

//Modification Details:
//all of our servlets have their own resource bundle. 
//hence, we don't need the strServletName argument.
public class RBStore {
  //private Logger log = new Logger();
  //THIS CLASS CANNOT USE LOGGER B/C LOGGER CALLS THIS CLASS AND YOU GET A CIRCULAR LOOP!
  private String strBaseName;
  private HashMap localeDataHash; //top level hash containing local specific hashes.

  public RBStore() {
    localeDataHash = new HashMap();
  }

  public void setBaseName(String strBaseName) {
    this.strBaseName = strBaseName;
  }
  // If the key is not found in the bundle, this method will return null.
  /*Original Method
  public String getString(String strServletName, String strKey,
   Locale locale) throws ServletException {
   
    // Check to see if the PropertyResourceBundle for this Locale has
    // //been loaded.
    if(!localeDataHash.containsKey(locale)) {
      try {
        loadLocale(locale);
      } catch(MissingResourceException mre) {
        throw new ServletException("The attempt to locate a resource file failed. ",mre);
      }
    }
    return (String)((HashMap)localeDataHash.get(locale)).get(strServletName + "." + strKey);
    
  }
  */ 
  //modified method.
  public String getString(String strKey,
   Locale locale) throws ServletException {
    // Check to see if the PropertyResourceBundle for this Locale has
    // //been loaded.
    if(!localeDataHash.containsKey(locale)) {
      try {
        loadLocale(locale);
      } catch(MissingResourceException mre) {
        throw new ServletException("The attempt to locate a resource file failed. ",mre);
      }
    }
    return (String)((HashMap)localeDataHash.get(locale)).get(strKey);
  }

  //new method for compatability purposes...
  public String getString(String strKey) {
    try {
      return getString(strKey,Locale.getDefault());
    } catch (Exception e) {
      //log.sparse("Caught: " + e.getMessage(),"RBStore:getString(String)",e);
      return "No Resource Found";
    }
  }

  // This will load the resource bundle and add the data it contains
  //to our top-level hash.
  private void loadLocale(Locale locale) throws MissingResourceException {
    ResourceBundle bundle=null;
    //log.paranoid("Loading: " + this.strBaseName + " for locale: " + locale.toString(),"RBStore:loadLocale");
    try {
      bundle = ResourceBundle.getBundle((Course.name + ".i18n." + this.strBaseName), locale);
    } catch (MissingResourceException mre) {
      //log.paranoid("Caught: " + mre.toString(),"RBStore:loadLocale",mre);
      try {
        bundle = ResourceBundle.getBundle(Course.name + "." + this.strBaseName,locale);
      } catch (MissingResourceException mre2) {
        //log.paranoid("Caught(Nested Exception): " + mre2.toString(),"RBStore:loadLocale",mre2);
      }
    }
    HashMap secondaryHash = new HashMap();
    Enumeration enum1 = bundle.getKeys();
    while(enum1.hasMoreElements()) {
      String strKey = (String)enum1.nextElement();
      //log.paranoid("Loading element: " + strKey,"RBStore:loadLocale");
      secondaryHash.put(strKey, bundle.getString(strKey));
    }
    // Now add the newly-created hash to our top-level hash.
    localeDataHash.put(locale, secondaryHash);
  }
}
 
    
