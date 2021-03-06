/*
 * Created on 21 Jun 2006
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

package com.aelitis.azureus.core.networkmanager.impl.tcp;


import java.net.InetAddress;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;

public class 
TCPNetworkManager 
{  
	private static int WRITE_SELECT_LOOP_TIME 	= 25;
	private static int READ_SELECT_LOOP_TIME	= 25;
	
	protected static int tcp_mss_size;
	  
	private static final TCPNetworkManager instance = new TCPNetworkManager();

	public static TCPNetworkManager getSingleton(){ return( instance ); }

	public static boolean TCP_INCOMING_ENABLED;
	public static boolean TCP_OUTGOING_ENABLED;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
				"TCP.Listen.Port.Enable",
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						TCP_INCOMING_ENABLED = TCP_OUTGOING_ENABLED = COConfigurationManager.getBooleanParameter( name );
					}
				});
		
		COConfigurationManager.addAndFireParameterListeners(
				new String[]{ "network.tcp.read.select.time", "network.tcp.write.select.time"  },
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String name )
					{
						WRITE_SELECT_LOOP_TIME 	= COConfigurationManager.getIntParameter(  "network.tcp.write.select.time" );
						READ_SELECT_LOOP_TIME 	= COConfigurationManager.getIntParameter(  "network.tcp.read.select.time" );
					}
				});
	}
	
	 /**
	   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
	   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
	   * connections, or 1452 (1492-40) for PPPOE connections.
	   * @return mss size in bytes
	   */
	
	public static int getTcpMssSize() {  return tcp_mss_size;  }

	public static void
	refreshRates(
		int		min_rate )
	{
		 tcp_mss_size = COConfigurationManager.getIntParameter( "network.tcp.mtu.size" ) - 40; 	        

	    if( tcp_mss_size > min_rate )  tcp_mss_size = min_rate - 1;
	    
	    if( tcp_mss_size < 512 )  tcp_mss_size = 512; 
	}
	
	protected
	TCPNetworkManager()
	{
		   //start read selector processing
	    Thread read_selector_thread = new AEThread( "ReadController:ReadSelector" ) {
	      public void runSupport() {
	        readSelectorLoop();
	      }
	    };
	    read_selector_thread.setDaemon( true );
	    read_selector_thread.setPriority( Thread.MAX_PRIORITY - 2 );
	    read_selector_thread.start();
	    
	    //start write selector processing
	    Thread write_selector_thread = new AEThread( "WriteController:WriteSelector" ) {
	      public void runSupport() {
	        writeSelectorLoop();
	      }
	    };
	    write_selector_thread.setDaemon( true );
	    write_selector_thread.setPriority( Thread.MAX_PRIORITY - 2 );
	    write_selector_thread.start();	    
	}
	
	  private final VirtualChannelSelector read_selector = new VirtualChannelSelector( "TCP network manager", VirtualChannelSelector.OP_READ, true );
	  private final VirtualChannelSelector write_selector = new VirtualChannelSelector( "TCP network manager", VirtualChannelSelector.OP_WRITE, true );


	  private final ConnectDisconnectManager connect_disconnect_manager = new ConnectDisconnectManager();
	  
	  private final IncomingSocketChannelManager incoming_socketchannel_manager = 
		  new IncomingSocketChannelManager( "TCP.Listen.Port", "TCP.Listen.Port.Enable" );	  

	  public void
	  setExplicitBindAddress(
		InetAddress	address )
	  {
		  incoming_socketchannel_manager.setExplicitBindAddress( address );
	  }
	  
	  public void
	  clearExplicitBindAddress()
	  {
		  incoming_socketchannel_manager.clearExplicitBindAddress();
	  }
	  
		public boolean
		isEffectiveBindAddress(
			InetAddress		address )
		{
			return( incoming_socketchannel_manager.isEffectiveBindAddress( address ));
		}
	  /**
	   * Get the socket channel connect / disconnect manager.
	   * @return connect manager
	   */
	  public ConnectDisconnectManager getConnectDisconnectManager() {  return connect_disconnect_manager;  }
	  
	  

	 
	  /**
	   * Get the virtual selector used for socket channel read readiness.
	   * @return read readiness selector
	   */
	  public VirtualChannelSelector getReadSelector() {  return read_selector;  }
	  
	  
	  /**
	   * Get the virtual selector used for socket channel write readiness.
	   * @return write readiness selector
	   */
	  public VirtualChannelSelector getWriteSelector() {  return write_selector;  }
	  
	  
	  public boolean
	  isTCPListenerEnabled()
	  {
		  return( incoming_socketchannel_manager.isEnabled());
	  }
	  
	  /**
	   * Get port that the TCP server socket is listening for incoming connections on.
	   * @return port number
	   */
	  public int getTCPListeningPortNumber() {  return incoming_socketchannel_manager.getTCPListeningPortNumber();  }	  
	  
	  private void readSelectorLoop() {
		    while( true ) {
		      try {
		        read_selector.select( READ_SELECT_LOOP_TIME );
		      }
		      catch( Throwable t ) {
		        Debug.out( "readSelectorLoop() EXCEPTION: ", t );
		      }      
		    }
		  }
		 
	  private void writeSelectorLoop() {
		    while( true ) {
		      try {
		        write_selector.select( WRITE_SELECT_LOOP_TIME );
		      }
		      catch( Throwable t ) {
		        Debug.out( "writeSelectorLoop() EXCEPTION: ", t );
		      }      
		    }
		  }

}
