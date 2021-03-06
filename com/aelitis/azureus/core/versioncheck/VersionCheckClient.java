/*
 * Created on Dec 20, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.versioncheck;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.transfer.*;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.clientmessageservice.*;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminASNLookup;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.PRUDPReleasablePacketHandler;



/**
 * Client for checking version information from a remote server.
 */
public class VersionCheckClient {
	private static final LogIDs LOGID = LogIDs.CORE;
	
	public static final String	REASON_UPDATE_CHECK_START		= "us";
	public static final String	REASON_UPDATE_CHECK_PERIODIC	= "up";
	public static final String	REASON_CHECK_SWT				= "sw";
	public static final String	REASON_DHT_EXTENDED_ALLOWED		= "dx";
	public static final String	REASON_DHT_ENABLE_ALLOWED		= "de";
	public static final String	REASON_EXTERNAL_IP				= "ip";
	public static final String	REASON_RECOMMENDED_PLUGINS		= "rp";
	public static final String	REASON_SECONDARY_CHECK			= "sc";
	
	
  private static final String 	AZ_MSG_SERVER_ADDRESS 	= "version.aelitis.com";
  private static final int 		AZ_MSG_SERVER_PORT 		= 27001;
  private static final String 	MESSAGE_TYPE_ID 		= "AZVER";
  
  public static final String 	HTTP_SERVER_ADDRESS 	= "version.aelitis.com";
  public static final int 		HTTP_SERVER_PORT 		= 2080;

  public static final String 	TCP_SERVER_ADDRESS 		= "version.aelitis.com";
  public static final int 		TCP_SERVER_PORT 		= 2080;

  public static final String 	UDP_SERVER_ADDRESS 		= "version.aelitis.com";
  public static final int 		UDP_SERVER_PORT 		= 2080;

  
  private static final long		CACHE_PERIOD	= 5*60*1000;
  private static boolean secondary_check_done;
  
  
  static{
	  VersionCheckClientUDPCodecs.registerCodecs();
  }
  
  private static final VersionCheckClient instance = new VersionCheckClient();
  private Map last_check_data = null;
  private final AEMonitor check_mon = new AEMonitor( "versioncheckclient" );
  private long last_check_time = 0; 
  
  
  private VersionCheckClient() {
    /* blank */
  }
  
  
  
  /**
   * Get the singleton instance of the version check client.
   * @return version check client
   */
  public static VersionCheckClient getSingleton() {  return instance;  }
  
  
  
  /**
   * Get the version check reply info.
   * @return reply data, possibly cached, if the server was already checked within the last minute
   */
  public Map getVersionCheckInfo( String reason ) {
	  return( getVersionCheckInfoSupport( reason, false, false ));
  }

 
  protected Map 
  getVersionCheckInfoSupport( 
		  String 	reason, 
		  boolean 	only_if_cached, 
		  boolean 	force )
  {
    try {  check_mon.enter();
    
      long time_diff = SystemTime.getCurrentTime() - last_check_time;
     
      force = force || time_diff > CACHE_PERIOD || time_diff < 0;
      
      if( last_check_data == null || last_check_data.size() == 0 || force ) {
    	  // if we've never checked before then we go ahead even if the "only_if_cached"
    	  // flag is set as its had not chance of being cached yet!
    	if ( only_if_cached && last_check_data != null ){
    		return( new HashMap() );
    	}
        try {
          last_check_data = performVersionCheck( constructVersionCheckMessage( reason ), true, true );
        }
        catch( Throwable t ) {
          t.printStackTrace();
          last_check_data = new HashMap();
        }
      }
      else {
      	Logger.log(new LogEvent(LOGID, "VersionCheckClient is using "
						+ "cached version check info. Using " + last_check_data.size()
						+ " reply keys.")); 
      }
    }
    finally {  check_mon.exit();  }
    
    if( last_check_data == null )  last_check_data = new HashMap();
    
    return last_check_data;
  }
  
  private boolean
  isVersionCheckDataValid()
  {  	
  	return( last_check_data != null && last_check_data.size() > 0 ); 
  }
  
  /**
   * Get the ip address seen by the version check server.
   * NOTE: This information may be cached, see getVersionCheckInfo().
   * @return external ip address, or empty string if no address information found
   */
  public String 
  getExternalIpAddress(
		boolean	only_if_cached )
  {
    Map reply = getVersionCheckInfoSupport( REASON_EXTERNAL_IP, only_if_cached, false );
    
    byte[] address = (byte[])reply.get( "source_ip_address" );
    if( address != null ) {
      return new String( address );
    }
    
    return( null );
  }
  
  
  /**
   * Is the DHT plugin allowed to be enabled.
   * @return true if DHT can be enabled, false if it should not be enabled
   */
  public boolean DHTEnableAllowed() {
    Map reply = getVersionCheckInfo( REASON_DHT_ENABLE_ALLOWED );
    
    boolean	res = false;
    
    byte[] value = (byte[])reply.get( "enable_dht" );
    
    if( value != null ) {
    	
      res = new String( value ).equalsIgnoreCase( "true" );
    }
    
	// we take the view that if the version check failed then we go ahead
	// and enable the DHT (i.e. we're being optimistic)

    if ( !res ){
    	res = !isVersionCheckDataValid();
    }
    
    return res;
  }
  
  
  /**
   * Is the DHT allowed to be used by external plugins.
   * @return true if extended DHT use is allowed, false if not allowed
   */
  public boolean DHTExtendedUseAllowed() {
    Map reply = getVersionCheckInfo( REASON_DHT_EXTENDED_ALLOWED );
    
    boolean	res = false;
    
    byte[] value = (byte[])reply.get( "enable_dht_extended_use" );
    if( value != null ) {
      res = new String( value ).equalsIgnoreCase( "true" );
    }
    
    	// be generous and enable extended use if check failed
    
    if ( !res ){
    	res = !isVersionCheckDataValid();
    }
    
    return res;
  }
  
  public String[]
  getRecommendedPlugins()
  {
	  Map reply = getVersionCheckInfo( REASON_RECOMMENDED_PLUGINS );

	  List	l = (List)reply.get( "recommended_plugins" );
	  
	  if ( l == null ){
		  
		  return( new String[0] );
	  }
	  
	  String[]	res = new String[l.size()];
	  
	  for (int i=0;i<l.size();i++){
		  
		  res[i] = new String((byte[])l.get(i));
	  }
	  
	  return( res );
  }
  
  /**
   * Perform the actual version check by connecting to the version server.
   * @param data_to_send version message
   * @return version reply
   * @throws Exception if the server check connection fails
   */
  private Map 
  performVersionCheck( 
	Map 	data_to_send,
	boolean	use_az_message,
	boolean	use_http ) 
  
  	throws Exception 
  {
	Exception 	error 	= null;
	Map			reply	= null;
	
	if ( use_az_message ){
	
		try{
			reply = executeAZMessage( data_to_send );
			
			reply.put( "protocol_used", "AZMSG" );
			
		}catch( Exception e ){
		
			error = e;
		}
	}
	
	if ( reply == null && use_http ){
		
		try{
			reply = executeHTTP( data_to_send );
			
			reply.put( "protocol_used", "HTTP" );
			
			error = null;
			
		}catch( Exception e ){
		
			error = e;
			
		}
	}
	if ( error != null ){
		
		throw( error );
	}
      
	if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "VersionCheckClient server "
						+ "version check successful. Received " + reply.size()
						+ " reply keys."));

    last_check_time = SystemTime.getCurrentTime();
      
    return reply;
  }
  
  private Map
  executeAZMessage(
	Map	data_to_send )
  
  	throws Exception
  {
	  if (Logger.isEnabled())
		  Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
				  + "version information from " + AZ_MSG_SERVER_ADDRESS + ":" + AZ_MSG_SERVER_PORT)); 

	  ClientMessageService 	msg_service = null;
	  Map 					reply		= null;	

	  try{
		  msg_service = ClientMessageServiceClient.getServerService( AZ_MSG_SERVER_ADDRESS, AZ_MSG_SERVER_PORT, MESSAGE_TYPE_ID );

		  msg_service.sendMessage( data_to_send );  //send our version message

		  reply = msg_service.receiveMessage();  //get the server reply

		  preProcessReply( reply );
		  
	  }finally{

		  if ( msg_service != null ){

			  msg_service.close();
		  }
	  }
	  
	  return( reply );
  }
  
  private Map
  executeHTTP(
	Map	data_to_send )
  
  	throws Exception
  {
	  if (Logger.isEnabled())
		  Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
				  + "version information from " + HTTP_SERVER_ADDRESS + ":" + HTTP_SERVER_PORT + " via HTTP" )); 

	  String	url_str = "http://" + HTTP_SERVER_ADDRESS + (HTTP_SERVER_PORT==80?"":(":" + HTTP_SERVER_PORT)) + "/version?";

	  url_str += URLEncoder.encode( new String( BEncoder.encode( data_to_send ), "ISO-8859-1" ), "ISO-8859-1" );
	  
	  URL	url = new URL( url_str );
	  
	  HttpURLConnection	url_connection = (HttpURLConnection)url.openConnection();
	  
	  url_connection.connect();
	  
	  try{
		  InputStream	is = url_connection.getInputStream();
		  
		  Map	reply = BDecoder.decode( new BufferedInputStream( is ));
		  
		  preProcessReply( reply );

		  return( reply );
		  
	  }finally{
		  
		  url_connection.disconnect();
	  }
  }
  
  public String
  getHTTPGetString(
	boolean	for_proxy )
  {
	  return( getHTTPGetString( new HashMap(), for_proxy));
  }
  
  private String
  getHTTPGetString(
	Map		content,
	boolean	for_proxy )
  {
	  String	get_str = "GET " + (for_proxy?("http://" + HTTP_SERVER_ADDRESS + ":" + HTTP_SERVER_PORT ):"") +"/version?";

	  try{
		  get_str += URLEncoder.encode( new String( BEncoder.encode( content ), "ISO-8859-1" ), "ISO-8859-1" );
		  
	  }catch( Throwable e ){ 
	  }
	  
	  get_str +=" HTTP/1.1" + "\015\012" + "\015\012";
	 
	  return( get_str );
  }
  
  private Map
  executeTCP(
	Map				data_to_send,
	InetAddress		bind_ip,
	int				bind_port )
  
  	throws Exception
  {
	  if (Logger.isEnabled())
		  Logger.log(new LogEvent(LOGID, "VersionCheckClient retrieving "
				  + "version information from " + TCP_SERVER_ADDRESS + ":" + TCP_SERVER_PORT + " via TCP" )); 

	  String	get_str = getHTTPGetString( data_to_send, false );
	  
	  Socket	socket = null;
	  
	  try{
		  socket = new Socket();
		 
		  if ( bind_ip != null ){
			  
			  socket.bind( new InetSocketAddress( bind_ip, bind_port ));
			  
		  }else if ( bind_port != 0 ){
			  
			  socket.bind( new InetSocketAddress( bind_port ));
		  }
		  
		  socket.setSoTimeout( 10000 );
		
		  socket.connect( new InetSocketAddress( TCP_SERVER_ADDRESS, TCP_SERVER_PORT ), 10000 );
		  
		  OutputStream	os = socket.getOutputStream();
		  
		  os.write( get_str.getBytes( "ISO-8859-1" ));
		  
		  os.flush();
		  
		  InputStream	is = socket.getInputStream();
		  
		  ByteArrayOutputStream	baos = new ByteArrayOutputStream();
		  		
		  byte[]	buffer = new byte[1024];

		  int	total_len = 0;
		  
		  while( true ){
			  
			  int	len = is.read( buffer );
			  
			  if ( len <= 0 ){
				  
				  break;
			  }
			  
			  total_len += len;
			  
			  if ( total_len > 16000 ){
				  
				  throw( new IOException( "reply too large" ));
			  }
			  
			  baos.write( buffer, 0, len );
		  }
		  
		  byte[]	reply_bytes = baos.toByteArray();
		  
		  for (int i=3;i<reply_bytes.length;i++){
			  
			  if ( 		reply_bytes[i-3]== (byte)'\015' &&
					  	reply_bytes[i-2]== (byte)'\012' &&
					  	reply_bytes[i-1]== (byte)'\015' &&
					  	reply_bytes[i-0]== (byte)'\012' ){
			  		  
				  Map reply = BDecoder.decode( new BufferedInputStream( new ByteArrayInputStream( reply_bytes, i+1, reply_bytes.length - (i+1 ))));
				  
				  preProcessReply( reply );

				  return( reply );
			  }
		  }
		  
		  throw( new Exception( "Invalid reply: " + new String( reply_bytes )));
		  
	  }finally{
		  
		  if ( socket != null ){
			  
			  try{
				  socket.close();
				  
			  }catch( Throwable e ){
				  
			  }
		  }
	  }
  }
  
  private Map
  executeUDP(
	Map				data_to_send,
	InetAddress		bind_ip,
	int				bind_port )
  
  	throws Exception
  {
	  PRUDPReleasablePacketHandler handler = PRUDPPacketHandlerFactory.getReleasableHandler( bind_port );
	  	  
	  PRUDPPacketHandler	packet_handler = handler.getHandler();
	  
	  long timeout = 5;
	  
	  Random random = new Random();
	  
	  try{
		  Exception	last_error = null;
		  
		  packet_handler.setExplicitBindAddress( bind_ip );	  
		  
		  for (int i=0;i<3;i++){
			  
			  try{
				  	// connection ids for requests must always have their msb set...
				  	// apart from the original darn udp tracker spec....
				  
				  long connection_id = 0x8000000000000000L | random.nextLong();
				  
				  VersionCheckClientUDPRequest	request_packet = new VersionCheckClientUDPRequest( connection_id );
				  
				  request_packet.setPayload( data_to_send );
				  
				  VersionCheckClientUDPReply reply_packet = (VersionCheckClientUDPReply)packet_handler.sendAndReceive( null, request_packet, new InetSocketAddress( UDP_SERVER_ADDRESS, UDP_SERVER_PORT ), timeout );
		
				  Map	reply = reply_packet.getPayload();
				  
				  preProcessReply( reply );
				  
				  return( reply );
				  
			  }catch( Exception e){
				  
				  last_error	= e;
				  
				  timeout = timeout * 2;
			  }
		  }
		  
		  if ( last_error != null ){
			  
			  throw( last_error );
		  }
		  
		  throw( new Exception( "Timeout" ));
		  
	  }finally{
		 
		  packet_handler.setExplicitBindAddress( null );

		  handler.release();
	  }
  }
  
  protected void
  preProcessReply(
	Map		reply )
  {
	  	// two cases where we automatically attempt to resolve the ASN (to minimise load on ASN
	  	// provider)
	  	// 1) new installs
	  	// 2) where we have an asn and public IP has changed outside of prefix range
	 
	 final long ASN_MIN_CHECK = 7*24*60*60*1000L;
		 
	 boolean	check_asn 	= false;
	 
	 long now = SystemTime.getCurrentTime();
	 	 
	 long	asn_check_time = COConfigurationManager.getLongParameter( "ASN Autocheck Performed Time" );
	 		 
	 if ( now < asn_check_time || now - asn_check_time > ASN_MIN_CHECK ){
		 
		 String bgp_prefix	= COConfigurationManager.getStringParameter( "ASN BGP", null );
		 String asn			= COConfigurationManager.getStringParameter( "ASN ASN", null );
		 
		 if ( 	asn == null || asn.length() == 0 ||
				bgp_prefix == null || bgp_prefix.length() == 0 ){
			
			 	// during 2502 introduction we ran DNS based queries as fallback without support
			 	// for reading ASN - pick up blank ASNs now and force recheck
			 
			 check_asn = true;
			 
		 }else{
			 					 
			 try{
				 byte[] address = (byte[])reply.get( "source_ip_address" );
				  
				 InetAddress	ip = InetAddress.getByName( new String( address ));

				 	// if we've got a prefix only recheck if outside existing range
			 
				 if ( !NetworkAdmin.getSingleton().matchesCIDR( bgp_prefix, ip )){
				 
					 check_asn = true;
				 }
				 
			 }catch( Throwable e ){
				 
				 check_asn = true;
				 
				 Debug.printStackTrace(e);
			 }
		}
	 }
	 
	 if ( check_asn ){
		 
		 COConfigurationManager.setParameter( "ASN Autocheck Performed Time", now );

		 try{
			 byte[] address = (byte[])reply.get( "source_ip_address" );
			  
			 InetAddress	ip = InetAddress.getByName( new String( address ));
		 
			 NetworkAdminASNLookup	asn = NetworkAdmin.getSingleton().lookupASN( ip );
			 				 
			 COConfigurationManager.setParameter( "ASN AS", 	asn.getAS());
			 COConfigurationManager.setParameter( "ASN ASN", 	asn.getASName());
			 COConfigurationManager.setParameter( "ASN BGP", 	asn.getBGPPrefix());
			 
			 	// kick off a secondary version check to communicate the new information
			 
			 if ( !secondary_check_done ){
				 
				 secondary_check_done	= true;
				 
				 new AEThread( "Secondary version check", true )
				 {
					 public void
					 runSupport()
					 {
						 getVersionCheckInfoSupport( REASON_SECONDARY_CHECK, false, true );
					 }
				 }.start();
			 }			 
		 }catch( Throwable e ){		
			 
			 Debug.printStackTrace(e);
		 }
	 }
	 
	 Long	as_advice = (Long)reply.get( "as_advice" );
	 
	 if ( as_advice != null ){
		 
	     String	asn = COConfigurationManager.getStringParameter( "ASN ASN", null );

	     if ( asn != null ){
	    	
			 long	advice = as_advice.longValue();
			 
			 if ( advice != 0 ){
			
				 	// require crypto
				 
				 String	done_asn = COConfigurationManager.getStringParameter( "ASN Advice Followed", "" );
				 
				 if ( !done_asn.equals( asn )){
					 
					 COConfigurationManager.setParameter( "ASN Advice Followed", asn );
					 
					 boolean	change 	= advice == 1 || advice == 2;
					 boolean	alert 	= advice == 1 || advice == 3;
					 
					 if ( !COConfigurationManager.getBooleanParameter( "network.transport.encrypted.require" )){
						 
						 if ( change ){
							 
							 COConfigurationManager.setParameter( "network.transport.encrypted.require", true );
						 }
						 
						 if ( alert ){
							 
							 String	msg = 
								 MessageText.getString(
										"crypto.alert.as.warning",
										new String[]{ asn });
							 
							 Logger.log( new LogAlert( false, LogAlert.AT_WARNING, msg ));
						 }
					 }
				 }
			 }
	     }
	 }
  }
  
  public InetAddress
  getExternalIpAddressHTTP()
  
  	throws Exception
  {
	  Map reply = executeHTTP( new HashMap());
	  
	  byte[] address = (byte[])reply.get( "source_ip_address" );
	  
	  return( InetAddress.getByName( new String( address )));
  }
  
  public InetAddress
  getExternalIpAddressTCP(
	InetAddress	 	bind_ip,
	int				bind_port )
  
  	throws Exception
  {
	  Map reply = executeTCP( new HashMap(), bind_ip, bind_port );
	  
	  byte[] address = (byte[])reply.get( "source_ip_address" );
	  
	  return( InetAddress.getByName( new String( address )));
  }
  
  public InetAddress
  getExternalIpAddressUDP(
	InetAddress	 	bind_ip,
	int				bind_port )
  
  	throws Exception
  {
	  Map reply = executeUDP( new HashMap(), bind_ip, bind_port );
	  
	  byte[] address = (byte[])reply.get( "source_ip_address" );
	  
	  return( InetAddress.getByName( new String( address )));
  }
  
  /**
   * Construct the default version check message.
   * @return message to send
   */
  private Map constructVersionCheckMessage( String reason ) {
    Map message = new HashMap();
    
    message.put( "appid", SystemProperties.getApplicationIdentifier());
    message.put( "version", Constants.AZUREUS_VERSION );
    
    String id = COConfigurationManager.getStringParameter( "ID", null );
    boolean send_info = COConfigurationManager.getBooleanParameter( "Send Version Info" );
    
    int last_send_time = COConfigurationManager.getIntParameter( "Send Version Info Last Time", -1 );

    int current_send_time = (int)(SystemTime.getCurrentTime()/1000);
    
    COConfigurationManager.setParameter( "Send Version Info Last Time", current_send_time );
    
    if( id != null && send_info ) {
    	
      message.put( "id", id );
      message.put( "os", Constants.OSName );
      
      message.put( "os_version", System.getProperty( "os.version" ) );
      message.put( "os_arch", System.getProperty( "os.arch" ) );   //see http://lopica.sourceforge.net/os.html
    
      if ( last_send_time != -1 && last_send_time < current_send_time ){
    	  
    	  	// tims since last
    	  
    	  message.put( "tsl", new Long(current_send_time-last_send_time));
      }
      
      message.put( "reason", reason );
      
      String  java_version = System.getProperty( "java.version" );
      if ( java_version == null ){  java_version = "unknown";  }
      message.put( "java", java_version );
      
      
      String  java_vendor = System.getProperty( "java.vm.vendor" );
      if ( java_vendor == null ){   java_vendor = "unknown";  }
      message.put( "javavendor", java_vendor );
      
      
      long  max_mem = Runtime.getRuntime().maxMemory()/(1024*1024);
      message.put( "javamx", new Long( max_mem ) );
      
      OverallStats	stats = StatsFactory.getStats();
      
      if ( stats != null ){
      	
	      //long total_bytes_downloaded 	= stats.getDownloadedBytes();
	      //long total_bytes_uploaded		= stats.getUploadedBytes();
	      long total_uptime 			= stats.getTotalUpTime();
	      
	      //removed due to complaints about anonymous stats collection
	      //message.put( "total_bytes_downloaded", new Long( total_bytes_downloaded ) );
	      //message.put( "total_bytes_uploaded", new Long( total_bytes_uploaded ) );
	      message.put( "total_uptime", new Long( total_uptime ) );
	      //message.put( "dlstats", stats.getDownloadStats());
      }
      
      String	as = COConfigurationManager.getStringParameter( "ASN AS", null );
      
      if ( as != null ){
    	
    	  	// there was borkage with DNS based queries that left leading " on AS
    	  
    	  if ( as.startsWith( "\"" )){
    		
    		  as = as.substring( 1 ).trim();
    		  
    		  COConfigurationManager.setParameter( "ASN AS", as );
    	  }
    	  
    	  message.put( "ip_as", as );
      }
      
      String	asn = COConfigurationManager.getStringParameter( "ASN ASN", null );
      
      if ( asn != null ){
    	
    	  if ( asn.length() > 64 ){
    		  
    		  asn = asn.substring( 0, 64 );
    	  }
    	  
    	  message.put( "ip_asn", asn );
      }
      
      String ui = COConfigurationManager.getStringParameter("ui");
      if (ui.length() > 0) {
      	message.put("ui", ui);
      }
      
      if ( AzureusCoreFactory.isCoreAvailable()){
      	
	      //installed plugin IDs
	      PluginInterface[] plugins = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaces();
	      List pids = new ArrayList();
	      for (int i=0;i<plugins.length;i++){
	        String  pid = plugins[i].getPluginID();
	        
        	String	info = (String)plugins[i].getPluginconfig().getPluginStringParameter( "plugin.info" );
        	
	          // filter out built-in and core ones
	        if ( 	( info != null && info.length() > 0 ) ||
	        		(	!pid.startsWith( "<" ) && 
		        		!pid.startsWith( "azbp" ) &&
		        		!pid.startsWith( "azupdater" ) &&
	        			!pid.startsWith( "azplatform" ) &&
	        			!pids.contains( pid ))){
	        
	        	if ( info != null && info.length() > 0 ){
	        		
	        		if( info.length() < 256 ){
	        			
	        			pid += ":" + info;
	        			
	        		}else{
	        			
	        			Debug.out( "Plugin '" + pid + "' reported excessive info string '" + info + "'" );
	        		}
	        	}
	        	
	        	pids.add( pid );
	        }
	      }
	      message.put( "plugins", pids );
      }
    }
    
    
    //swt stuff
    try {
      Class c = Class.forName( "org.eclipse.swt.SWT" );
      
      String swt_platform = (String)c.getMethod( "getPlatform", new Class[]{} ).invoke( null, new Object[]{} );
      message.put( "swt_platform", swt_platform );
      
      if( send_info ) {
        Integer swt_version = (Integer)c.getMethod( "getVersion", new Class[]{} ).invoke( null, new Object[]{} );
        message.put( "swt_version", new Long( swt_version.longValue() ) );

        c = Class.forName("org.gudy.azureus2.ui.swt.mainwindow.MainWindow");
				if (c != null) {
					c.getMethod("addToVersionCheckMessage", new Class[] { Map.class }).invoke(
							null, new Object[] { message });
				}
      }
    }
    catch( ClassNotFoundException e ) {  /* ignore */ }
    catch( NoClassDefFoundError er ) {  /* ignore */ }
    catch( InvocationTargetException err ) {  /* ignore */ }
    catch( Throwable t ) {  t.printStackTrace();  }
    
    
    boolean using_phe = COConfigurationManager.getBooleanParameter( "network.transport.encrypted.require" );
    message.put( "using_phe", using_phe ? new Long(1) : new Long(0) );
    
    return message;
  }
  
  public static void
  main(
	String[]	args )
  {
	  try{
		  COConfigurationManager.initialise();
		  
		  System.out.println( "UDP:  " + getSingleton().getExternalIpAddressUDP(null,0));
		  System.out.println( "TCP:  " + getSingleton().getExternalIpAddressTCP(null,0));
		  System.out.println( "HTTP: " + getSingleton().getExternalIpAddressHTTP());
		  
	  }catch( Throwable e){
		  e.printStackTrace();
	  }
  }
}
