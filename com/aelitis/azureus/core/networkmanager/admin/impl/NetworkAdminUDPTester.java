/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.PluginInterface;



import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;

public class 
NetworkAdminUDPTester 
	implements NetworkAdminProtocolTester
{
	public static final String 	UDP_SERVER_ADDRESS	= Constants.NAT_TEST_SERVER;
	public static final int		UDP_SERVER_PORT		= 2081; // 2084;
	
	static{
		NetworkAdminNATUDPCodecs.registerCodecs();
	}
	
	private AzureusCore		core;
	
	protected
	NetworkAdminUDPTester(
		AzureusCore	_core )
	{
		core	= _core;
	}
	
	public InetAddress
	testOutbound(
		InetAddress		bind_ip,
		int				bind_port )
	
		throws NetworkAdminException
	{
		try{
			return( VersionCheckClient.getSingleton().getExternalIpAddressUDP(bind_ip, bind_port));
			
		}catch( Throwable e ){
		
			throw( new NetworkAdminException( "Outbound check failed", e ));
		}
	}
	
	public InetAddress
	testInbound(			
		InetAddress		bind_ip,
		int				bind_port )
	
		throws NetworkAdminException
	{
		PRUDPReleasablePacketHandler handler = PRUDPPacketHandlerFactory.getReleasableHandler( bind_port );

		PRUDPPacketHandler	packet_handler = handler.getHandler();

		long timeout = 5000;

		HashMap	data_to_send = new HashMap();

		PluginInterface pi_upnp = core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

		String	upnp_str = null;

		if( pi_upnp != null ) {

			UPnPPlugin upnp = (UPnPPlugin)pi_upnp.getPlugin();

			/*
			UPnPMapping mapping = upnp.getMapping( true, port );

			if ( mapping == null ) {

				new_mapping = mapping = upnp.addMapping( "NAT Tester", true, port, true );

				// give UPnP a chance to work

				try {
					Thread.sleep( 500 );

				}
				catch (Throwable e) {

					Debug.printStackTrace( e );
				}
			}
			*/
			
			UPnPPluginService[]	services = upnp.getServices();

			if ( services.length > 0 ){

				upnp_str = "";

				for (int i=0;i<services.length;i++){

					UPnPPluginService service = services[i];

					upnp_str += (i==0?"":",") + service.getInfo();
				}
			}
		}

		if ( upnp_str != null ){

			data_to_send.put( "upnp", upnp_str );
		}

		String	as 	= COConfigurationManager.getStringParameter( "ASN AS", "" );
		String	asn = COConfigurationManager.getStringParameter( "ASN ASN", "" );

		if ( as.length() > 0 ){

			data_to_send.put( "as", as );
			data_to_send.put( "asn", asn );
		}

		data_to_send.put( "locale", MessageText.getCurrentLocale().toString());

		Random 	random = new Random();

		data_to_send.put( "id", new Long( random.nextLong()));		  

		try{
			packet_handler.setExplicitBindAddress( bind_ip );	  

			Throwable last_error = null;

			final int tries = 3;

			try{
				for (int i=0;i<tries;i++){

					data_to_send.put( "seq", new Long(i));

					try{

						// connection ids for requests must always have their msb set...
						// apart from the original darn udp tracker spec....

						long connection_id = 0x8000000000000000L | random.nextLong();

						NetworkAdminNATUDPRequest	request_packet = new NetworkAdminNATUDPRequest( connection_id );

						request_packet.setPayload( data_to_send );

						NetworkAdminNATUDPReply reply_packet = (NetworkAdminNATUDPReply)packet_handler.sendAndReceive( null, request_packet, new InetSocketAddress( UDP_SERVER_ADDRESS, UDP_SERVER_PORT ), timeout );

						Map	reply = reply_packet.getPayload();

						byte[]	ip_bytes = (byte[])reply.get( "ip_address" );

						if ( ip_bytes == null ){

							throw( new NetworkAdminException( "IP address missing in reply" ));
						}

						byte[] reason = (byte[])reply.get( "reason" );

						if ( reason != null ) {

							throw( new NetworkAdminException( new String( reason, "UTF8")));
						}

						return( InetAddress.getByAddress( ip_bytes ));

					}catch( Throwable e){

						last_error	= e;

						timeout = timeout * 2;
					}
				}

				if ( last_error != null ){

					throw( last_error );
				}

				throw( new NetworkAdminException( "Timeout" ));

			}finally{

				try{
					data_to_send.put( "seq", new Long(99));

					long connection_id = 0x8000000000000000L | random.nextLong();

					NetworkAdminNATUDPRequest	request_packet = new NetworkAdminNATUDPRequest( connection_id );

					request_packet.setPayload( data_to_send );

					// fire off one last packet in attempt to inform server of completion

					packet_handler.send( request_packet, new InetSocketAddress( UDP_SERVER_ADDRESS, UDP_SERVER_PORT ));

				}catch( Throwable e){  
				}
			}
		}catch( NetworkAdminException e ){

			throw( e );

		}catch( Throwable e ){

			throw( new NetworkAdminException( "Inbound check failed", e ));

		}finally{

			packet_handler.setExplicitBindAddress( null );

			handler.release();
		}
	}
}
