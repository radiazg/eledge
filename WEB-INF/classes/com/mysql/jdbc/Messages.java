/*
   Copyright (C) 2002 MySQL AB

      This program is free software; you can redistribute it and/or modify
      it under the terms of the GNU General Public License as published by
      the Free Software Foundation; either version 2 of the License, or
      (at your option) any later version.

      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.

      You should have received a copy of the GNU General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */
package com.mysql.jdbc;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Support for localized messages.
 * 
 * @author Mark Matthews
 * @version $Id: Messages.java,v 1.1.2.1 2003/06/07 00:44:07 mmatthew Exp $
 */
public class Messages {

	private static final String BUNDLE_NAME = "com.mysql.jdbc.Messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	/**
	 * Dis-allow construction ...
	 */
	private Messages() {

		// XXX Auto-generated constructor stub
	}
	
	/**
	 * Returns the localized message for the given message key
	 * @param key the message key
	 * @return The localized message for the key
	 */
	public static String getString(String key) {
		// XXX Auto-generated method stub
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	public static String getString(String key, Object[] args) {
		return MessageFormat.format(getString(key), args);
	}
}
