/*
   Copyright (C) 2003 MySQL AB

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
package com.mysql.jdbc.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * @author Mark Matthews
 * @version $Id: LRUCache.java,v 1.1.2.1 2003/07/23 01:45:11 mmatthew Exp $
 */
public class LRUCache extends LinkedHashMap {
	protected int maxElements;

	public LRUCache(int maxSize) {
		super(maxSize);
		this.maxElements = maxSize;
	}
	
	/* (non-Javadoc)
	 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
	 */
	protected boolean removeEldestEntry(Entry eldest) {
		return (size() > this.maxElements);
	}
}
