/*
 * Created on 28-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.util;

import java.util.WeakHashMap;


/**
 * @author parg
 *
 */

public abstract class 
AEThread 
	extends Thread
{
	private static WeakHashMap	our_thread_map = new WeakHashMap();
		
	public
	AEThread(
		String	name )
	{
		super(name);
		
		setDaemon( false );
	}
	
	public
	AEThread(
		String	name,
		boolean	daemon )
	{
		super(name);
		
		setDaemon( daemon );
	}
	
	public void
	run()
	{
		try{
			/*
			if ( !isDaemon()){
				
				System.out.println( "non-daemon thread:" + this );
			}
			*/
			
			runSupport();
			
		}catch( Throwable e ){
			
			DebugLight.printStackTrace(e);
		}
	}
	
	public abstract void
	runSupport();
	
	public static boolean
	isOurThread(
		Thread	thread )
	{
		if ( thread instanceof AEThread ){
			
			return( true );
		}
		
		synchronized( our_thread_map ){
			
			return( our_thread_map.get( thread ) != null );
		}
	}
	
	public static void
	setOurThread()
	{
		setOurThread( Thread.currentThread());
	}
	
	public static void
	setOurThread(
		Thread	thread )
	{
		if ( thread instanceof AEThread ){
			
			return;
		}
				
		synchronized( our_thread_map ){
			
			our_thread_map.put( thread, "" );
		}
	}
}
