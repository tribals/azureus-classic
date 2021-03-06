/*
 * Created on 15-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.util;

import java.util.*;

public class 
CopyOnWriteList 
{
	private volatile List	list = new ArrayList();
	private volatile int	version;
	
	public void
	add(
		Object	obj )
	{
		synchronized( list ){
			
			List	new_list = new ArrayList( list );
			
			new_list.add( obj );
		
			list	= new_list;
			
			version++;
		}
	}
	
	public boolean
	remove(
		Object	obj )
	{
		synchronized( list ){
			
			List	new_list = new ArrayList( list );
			
			boolean result = new_list.remove( obj );
		
			list	= new_list;
			
			version++;
			
			return( result );
		}
	}
	
	public boolean
	contains(
		Object	obj )
	{
		return( list.contains( obj ));
	}
	
	public Iterator
	iterator()
	{
		return( list.iterator());
	}
	
	public List
	getList()
	{
		return( list );
	}
	
	public int
	size()
	{
		return( list.size());
	}
	
	public int
	getVersion()
	{
		return( version );
	}
}
