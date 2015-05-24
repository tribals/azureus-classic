/*
 * Created on 29-Dec-2004
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

package org.gudy.azureus2.pluginsimpl.local.clientid;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.ThreadPoolTask;
import org.gudy.azureus2.plugins.clientid.ClientIDException;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

/**
 * @author parg
 *
 */

public class 
ClientIDManagerImpl
	implements ClientIDManager
{
	private static final LogIDs LOGID = LogIDs.PLUGIN;
	protected static ClientIDManagerImpl	singleton = new ClientIDManagerImpl();
	
	protected static final char		CR			= '\015';
	protected static final char		FF			= '\012';
	protected static final String	NL			= "\015\012";
	
	public static ClientIDManagerImpl
	getSingleton()
	{
		return( singleton );
	}

	private ClientIDGenerator		generator;
	private boolean					use_filter;
	private boolean					filter_override;
	private ThreadPool				thread_pool;
	
	private int						filter_port;
	
	public void
	setGenerator(
		ClientIDGenerator	_generator,
		boolean				_use_filter )
	{
		generator	= _generator;
		use_filter	= _use_filter;
		
			// we override the filter parameter here if we have a local bind IP set as
			// this is the only simple solution to enforcing the local bind (Sun's
			// HTTPConnection doesn't allow the network interface to be bound)
		

		if ( !use_filter ){
			
				// another reason for NOT doing this is if the user has a defined proxy
				// in this case the assumption is that they know what they're doing and
				// the proxy will be bound correctly to ensure that things work...
			
			String	http_proxy 	= System.getProperty( "http.proxyHost" );
			String	socks_proxy = System.getProperty( "socksProxyHost" );
			
		    InetAddress bindIP = NetworkAdmin.getSingleton().getDefaultBindAddress();
		    
	        if (	( http_proxy == null || http_proxy.trim().length() == 0 ) &&
	        		( socks_proxy == null || socks_proxy.trim().length() == 0 ) &&
	        		bindIP != null ){

	        	int		ips = 0;
	        	
	        		// seeing as this is a bit of a crappy way to enforce binding, add one more check to make
	        		// sure that the machine has multiple ips before going ahead in case user has set it
	        		// incorrectly
	        	
	        	try{
	        		Enumeration nis = NetworkInterface.getNetworkInterfaces();
	        			        		
	        		while( nis.hasMoreElements()){
	        			
	        			NetworkInterface ni = (NetworkInterface)nis.nextElement();
	        			
	        			Enumeration addresses = ni.getInetAddresses();
	        			
	        			while( addresses.hasMoreElements()){
	        				
	        				InetAddress address = (InetAddress)addresses.nextElement();
	        				
	        				if ( !address.isLoopbackAddress()){
	        					
	        					ips++;
	        				}
	        			}        			
	        		}
	        	}catch( Throwable e ){
	        		Logger.log(new LogEvent(LOGID, "", e));
	        	}
	        	
	        	if ( ips > 1 ){
	        		
		        	filter_override	= true;
		        	
		        	use_filter	= true;
		        	
		        	if (Logger.isEnabled())
		        		Logger.log(new LogEvent(LOGID,
		        				"ClientIDManager: overriding filter "
		        				+ "option to support local bind IP"));
	        	}
	        }
		}
		
		if ( use_filter ){
			
			try{
				thread_pool = new ThreadPool( "ClientIDManager", 32 );
				
			  	String	connect_timeout = System.getProperty("sun.net.client.defaultConnectTimeout"); 
			  	String	read_timeout 	= System.getProperty("sun.net.client.defaultReadTimeout"); 
			  			
			  	int	timeout = Integer.parseInt( connect_timeout ) + Integer.parseInt( read_timeout );
				
				thread_pool.setExecutionLimit( timeout );
			
				final ServerSocket ss = new ServerSocket( 0, 1024, InetAddress.getByName("127.0.0.1"));
				
				filter_port	= ss.getLocalPort();
				
				ss.setReuseAddress(true);
								
				Thread accept_thread = 
						new AEThread("ClientIDManager::filterloop")
						{
							public void
							runSupport()
							{
								long	successfull_accepts = 0;
								long	failed_accepts		= 0;

								while(true){
									
									try{				
										Socket socket = ss.accept();
							
										successfull_accepts++;
							
										thread_pool.run( new httpFilter( socket ));
										
									}catch( Throwable e ){
										
										failed_accepts++;
										
                    if (Logger.isEnabled())
                      Logger.log(new LogEvent(LOGID, 
                                              "ClientIDManager: listener failed on port "
                                              + filter_port, e )); 
										
										if ( failed_accepts > 100 && successfull_accepts == 0 ){

												// looks like its not going to work...
												// some kind of socket problem
															
											Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
												LogAlert.AT_ERROR, "Network.alert.acceptfail"),
												new String[] { "" + filter_port, "TCP" });
									
											use_filter	= false;
											
											break;
										}
									}
								}
							}
						};
			
				accept_thread.setDaemon( true );
			
				accept_thread.start();									
			
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"ClientIDManager: listener established on port " + filter_port)); 
				
			}catch( Throwable e){
			
				Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
						LogAlert.AT_ERROR, "Tracker.alert.listenfail"), new String[] { ""
						+ filter_port });
		
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"ClientIDManager: listener failed on port " + filter_port, e)); 
				
				use_filter	= false;
			}		
		}
	}
	
	public ClientIDGenerator
	getGenerator()
	{
		return( generator );
	}
	
	public byte[]
	generatePeerID(
		TOTorrent	torrent,
		boolean		for_tracker )
	
		throws ClientIDException
	{
		return( generator.generatePeerID( new TorrentImpl( torrent ), for_tracker ));
	}
	
	public void
	generateHTTPProperties(
		Properties	properties )
	
		throws ClientIDException
	{
		if ( use_filter ){
		
				// to support SSL here we would need to substitute the https url with an https one
				// and then drive the SSL in the filter appropriately
			
			URL	url = (URL)properties.get( ClientIDGenerator.PR_URL );
			
			if ( !url.getProtocol().toLowerCase().equals( "http" )){
				
				Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
						"ClientIDManager only supports filtering of http, not https"));
				
				return;
			}
			
			try{
				String	url_str = url.toString();
				
				String	target_host = url.getHost();
				int		target_port	= url.getPort();
				
				if ( target_port == -1 ){
					
					target_port = url.getDefaultPort();
				}
				
				int host_pos = url_str.indexOf( target_host );
				
				String	new_url = url_str.substring(0,host_pos) + "127.0.0.1:" + filter_port;
				
				String	rem = url_str.substring( host_pos + target_host.length());
				
				if ( rem.charAt(0) == ':' ){
					
					rem = rem.substring( (""+ target_port ).length() + 1 );
				}
				
				int q_pos = rem.indexOf( '?' );
				
				new_url += rem.substring(0,q_pos+1) + "cid=" + target_host + ":" + target_port + "&" + rem.substring(q_pos+1);
				
				properties.put( ClientIDGenerator.PR_URL, new URL( new_url ));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}else{
			
			generator.generateHTTPProperties( properties );
		}
	}
	
	protected class
	httpFilter
		extends ThreadPoolTask
	{
		private Socket		socket;
		
		protected
		httpFilter(
			Socket		_socket )
		{
			socket	= _socket;
		}
		
		public void
		runSupport()
		{
			String		report_error	= null;
			int			written			= 0;
			
			try{
						
				setTaskState( "reading header" );
										
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				String	header = "";
				
				while(true ){
						
					int	len = is.read(buffer);
						
					if ( len == -1 ){
					
						break;
					}
									
					header += new String( buffer, 0, len, Constants.BYTE_ENCODING );
									
					if ( 	header.endsWith( NL+NL ) ||
							header.indexOf( NL+NL ) != -1 ){
						
						break;
					}
				}
				
				List	lines = new ArrayList();
				
				int	pos = 0;
				
				while( true){
					
					int	p1 = header.indexOf( NL, pos );
					
					String	line;
					
					if ( p1 == -1 ){
						
						line = header.substring(pos);
						
					}else{
											
						line = header.substring( pos, p1 );
					}
					
					line = line.trim();
					
					if ( line.length() > 0 ){
					
						lines.add( line );
					}
				
					if ( p1 == -1 ){
						
						break;
					}
					
					pos = p1+2;
				}
				
				
				String[]	lines_in = new String[ lines.size()];
				
				lines.toArray( lines_in );
				
				String	get = lines_in[0];
				
				int	p1 = get.indexOf( "?cid=" );
				int	p2 = get.indexOf( "&", p1 );
				
				String	cid = get.substring( p1+5, p2 );
				
				int	p3 = cid.indexOf( ":" );
				
				String	target_host	= cid.substring( 0, p3 );
				int		target_port	= Integer.parseInt( cid.substring(p3+1));
				
					// fix up the Host: entry with the target details
				
				for (int i=1;i<lines_in.length;i++){
					
					String	line = lines_in[i];
					
					if ( line.toLowerCase().indexOf( "host:" ) != -1 ){
						
						lines_in[i] = "Host: " + target_host + ":" + target_port;
						
						break;
					}
				}
				
				get = get.substring( 0, p1+1 ) + get.substring( p2+1 );
				
				lines_in[0] = get;
				
				String[]	lines_out;
				
				if ( filter_override ){
					
						// bodge for ip override. we still need to take account of the correct
						// user-agent
					lines_out = lines_in;
					
					Properties p = new Properties();
					
					generator.generateHTTPProperties( p );
						
					String	agent = p.getProperty( ClientIDGenerator.PR_USER_AGENT );
					
					if ( agent != null ){
						
						for (int i=0;i<lines_out.length;i++){
							
							if ( lines_out[i].toLowerCase().startsWith( "user-agent" )){
								
								lines_out[i] = "User-Agent: " + agent;
							}
						}
					}
				}else{
					
					lines_out = generator.filterHTTP( lines_in );
				}
				
				String	header_out = "";
				
				for (int i=0;i<lines_out.length;i++){
					
					header_out += lines_out[i] + NL;
				}
				
				header_out += NL;
				
				Socket	target = new Socket();
				
			    InetAddress bindIP = NetworkAdmin.getSingleton().getDefaultBindAddress();
			    
		        if ( bindIP != null ){
		        	
		        	target.bind( new InetSocketAddress( bindIP, 0 ) );
		        }

		        // System.out.println( "filtering " + target_host + ":" + target_port );
		        
		        target.connect( new InetSocketAddress(  target_host, target_port  ));
		        
				target.getOutputStream().write( header_out.getBytes(Constants.BYTE_ENCODING ));
				
				target.getOutputStream().flush();
				
				InputStream	target_is = target.getInputStream(); 
					
				while( true ){
					
					int	len = target_is.read( buffer );
					
					if ( len == -1 ){
						
						break;
					}
					
					socket.getOutputStream().write( buffer, 0,len );
					
					written += len;
				}	
				
			}catch( ClientIDException e ){
						
				report_error = e.getMessage();
				
			}catch( UnknownHostException e ){
				
				report_error = "Unknown host '" + e.getMessage() + "'";
				
			}catch( Throwable e ){
				
				// Debug.printStackTrace(e);
					
			}finally{
				
				if ( report_error != null && written == 0 ){
					
					Map	failure = new HashMap();
					
					failure.put("failure reason", report_error );
					
					try{
						byte[] x = BEncoder.encode( failure );
					
						socket.getOutputStream().write( x );
						
					}catch( Throwable f ){
						
						Debug.printStackTrace(f);
					}
				}
				
				try{
					socket.getOutputStream().flush();
					
					socket.close();
					
				}catch( Throwable f ){
					
				}			
			}
		}
		
		public void
		interruptTask()
		{
			try{
/*
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "ClientIDManager - interrupting "
							+ "HTTP filter due to timeout"));
*/				
				socket.close();
				
			}catch( Throwable e ){
				
			}
		}
	}
}