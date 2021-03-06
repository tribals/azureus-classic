/*
 * File    : PluginConfigImpl.java
 * Created : 10 nov. 2003
 * By      : epall
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.pluginsimpl.local;

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.pluginsimpl.local.config.*;

import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class 
PluginConfigImpl
	implements PluginConfig 
{

	protected static Map	external_to_internal_key_map = new HashMap();
	
	static{
		
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, 		CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC, 		"Max Upload Speed Seeding KBs" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, 	CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, 				"Max.Peer.Connections.Total" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, 			"Max.Peer.Connections.Per.Torrent" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOADS, 						"max downloads" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE, 							"max active torrents" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE_SEEDING, 							"StartStopManager_iMaxActiveTorrentsWhenSeeding" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOADS, "Max Uploads");
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOADS_SEEDING, "Max Uploads Seeding");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING, "enable.seedingonly.upload.rate");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING, "StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, "Auto Upload Speed Enabled");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_SOCKS_PROXY_NO_INWARD_CONNECTION, 	"Proxy.Data.SOCKS.inform" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP, 			CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP );
		external_to_internal_key_map.put( CORE_PARAM_STRING_LOCAL_BIND_IP, 						"Bind IP" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_FRIENDLY_HASH_CHECKING, 			"diskmanager.friendly.hashchecking" );
		
		// Note: Not in PluginConfig.java because it's an UI option and
		//       not applicable to all UIs
		// TODO: Add a smarter way
		
		// Following parameters can be set directly (we don't have an alias for these values).
		String[] passthrough_params = new String[] {
				"Open MyTorrents", "IconBar.enabled", "Wizard Completed",
				"welcome.version.lastshown", "Set Completion Flag For Completed Downloads On Start",
		};
		
		for (int i=0; i<passthrough_params.length; i++) {
			external_to_internal_key_map.put(passthrough_params[i], passthrough_params[i]);
		}
	}

	private PluginInterface	plugin_interface;
	private String 			key;
  
	public 
	PluginConfigImpl(
		PluginInterface		_plugin_interface,
		String			 	_key ) 
	{
		plugin_interface	= _plugin_interface;
		
		key = _key + ".";
	}

	public boolean
	isNewInstall()
	{
		return( COConfigurationManager.isNewInstall());
	}
	
	public String
	getPluginConfigKeyPrefix()
	{
		return( key );
	}
	
	public void setPluginConfigKeyPrefix(String _key) {
		if (_key.length() > 0 || plugin_interface.isBuiltIn()) {
			key = _key;
		} else {
			throw (new RuntimeException("Can't set Plugin Config Key Prefix to '"
					+ _key + "'"));
		}
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getStringParameter(java.lang.String)
	 */
	public String getStringParameter(String name) {
		return COConfigurationManager.getStringParameter(mapKeyName(name, false));
	}

    public String getStringParameter(String name, String _default )
	{
		return COConfigurationManager.getStringParameter(mapKeyName(name, false), _default);
    }

	public float getFloatParameter(String name) {
		return COConfigurationManager.getFloatParameter(mapKeyName(name, false));
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name) {
		return COConfigurationManager.getIntParameter(mapKeyName(name, false));
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getIntParameter(java.lang.String)
	 */
	public int getIntParameter(String name, int default_value) {
		return COConfigurationManager.getIntParameter(mapKeyName(name, false), default_value);
	}
	
	private String mapKeyName(String key, boolean for_set) {
		String result = (String)external_to_internal_key_map.get(key);
		if (result == null) {
			if (for_set) {
				throw new RuntimeException("No permission to set the value of core parameter: " + key);
			}
			else {
				return key;
			}
		}
		return result;
	}

	public void
	setIntParameter(
	  	String	key, 
		int		value )
	{
		COConfigurationManager.setParameter(mapKeyName(key, true), value );
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getBooleanParameter(java.lang.String)
	 */
	public boolean getBooleanParameter(String name) {
		return COConfigurationManager.getBooleanParameter(mapKeyName(name, false));
	}
	
	public boolean getBooleanParameter(String name, boolean _default) {
		return COConfigurationManager.getBooleanParameter(mapKeyName(name, false), _default);
	}
	
	public void
	setBooleanParameter(
	  	String		key, 
		boolean		value )
	{
		COConfigurationManager.setParameter( mapKeyName(key, true), value );
	}
	
    public byte[] getByteParameter(String name, byte[] _default )
    {
		return COConfigurationManager.getByteParameter(mapKeyName(name, false), _default);
    }

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String)
	 */
	public int getPluginIntParameter(String key)
	{
		return getIntParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginIntParameter(java.lang.String, int)
	 */
	public int getPluginIntParameter(String key, int defaultValue)
	{
	   	COConfigurationManager.setIntDefault( this.key+key, defaultValue );

		return COConfigurationManager.getIntParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String)
	 */
	public String getPluginStringParameter(String key)
	{
		return getStringParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginStringParameter(java.lang.String, int)
	 */
	public String getPluginStringParameter(String key, String defaultValue)
	{
    	COConfigurationManager.setStringDefault( this.key+key, defaultValue );

		return COConfigurationManager.getStringParameter(this.key+key, defaultValue);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String)
	 */
	public boolean getPluginBooleanParameter(String key)
	{
		return getBooleanParameter(this.key+key);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#getPluginBooleanParameter(java.lang.String, int)
	 */
	public boolean getPluginBooleanParameter(String key, boolean defaultValue)
	{
	   	COConfigurationManager.setBooleanDefault( this.key+key, defaultValue );

		return COConfigurationManager.getBooleanParameter(this.key+key, defaultValue);
	}

	public byte[] getPluginByteParameter(String key, byte[] defaultValue )
	{
	   	COConfigurationManager.setByteDefault( this.key+key, defaultValue );

		return COConfigurationManager.getByteParameter(this.key+key, defaultValue);
	}

	 public List
	 getPluginListParameter( String key, List	default_value )
	 {
		return COConfigurationManager.getListParameter(this.key+key, default_value); 
	 }
	 
	 public void
	 setPluginListParameter( String key, List	value )
	 {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }

	 public Map
	 getPluginMapParameter( String key, Map	default_value )
	 {
		return COConfigurationManager.getMapParameter(this.key+key, default_value); 
	 }
	 
	 public void
	 setPluginMapParameter( String key, Map	value )
	 {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }
	 
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, int)
	 */
	public void setPluginParameter(String key, int value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	public void setPluginParameter(String key, int value,boolean global)
	{
		COConfigurationManager.setParameter(this.key+key, value);
		
		if ( global ){
			
			MagnetURIHandler.getSingleton().addInfo( this.key+key, value );
		}
	}
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, java.lang.String)
	 */
	public void setPluginParameter(String key, String value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.PluginConfig#setPluginParameter(java.lang.String, boolean)
	 */
	public void setPluginParameter(String key, boolean value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}
	
	public void setPluginParameter(String key,byte[] value)
	{
		COConfigurationManager.setParameter(this.key+key, value);
	}

	public ConfigParameter
	getParameter(
		String		key )
	{
		return( new ConfigParameterImpl( mapKeyName(key, false)));
	}
	
	public ConfigParameter
	getPluginParameter(
	  	String		key )
	{
		return( new ConfigParameterImpl( this.key+key ));
	}
	
	public boolean removePluginParameter(String key) {
		return COConfigurationManager.removeParameter(this.key + key);
	}
	
	  public boolean
	  getUnsafeBooleanParameter(
		  String		key,
		  boolean		default_value )
	  {
		return( COConfigurationManager.getBooleanParameter( key, default_value ));
	  }

	  public void
	  setUnsafeBooleanParameter(
		  String		key,
		  boolean		value )
	  {
		  COConfigurationManager.setParameter( key, value );
	  }

	  public int
	  getUnsafeIntParameter(
		  String		key,
		  int		default_value )
	  {
			return( COConfigurationManager.getIntParameter( key, default_value ));
	  }

	  public void
	  setUnsafeIntParameter(
		  String		key,
		  int		value )
	  {
		  COConfigurationManager.setParameter( key, value );
	  }

	  public long
	  getUnsafeLongParameter(
		  String		key,
		  long		default_value )
	  {
			return( COConfigurationManager.getLongParameter( key, default_value ));
	  }

	  public void
	  setUnsafeLongParameter(
		  String		key,
		  long		value )
	  {
		  COConfigurationManager.setParameter( key, value );
	  }

	  public float
	  getUnsafeFloatParameter(
		  String		key,
		  float		default_value )
	  {
			return( COConfigurationManager.getFloatParameter( key, default_value ));
	  }

	  public void
	  setUnsafeFloatParameter(
			  String		key,
			  float		value )
	  {
		  COConfigurationManager.setParameter( key, value );
	  }

	  public String
	  getUnsafeStringParameter(
			  String		key,
			  String		default_value )
	  {
			return( COConfigurationManager.getStringParameter( key, default_value ));
	  }

	  public void
	  setUnsafeStringParameter(
			  String		key,
			  String		value )
	  {
		  COConfigurationManager.setParameter( key, value );
	  }

	  public Map
	  getUnsafeParameterList()
	  {
		  Set params = COConfigurationManager.getAllowedParameters();
		  
		  Iterator	it = params.iterator();
		  
		  Map	result = new HashMap();
		  
		  while( it.hasNext()){
			  
			  try{
				  String	name = (String)it.next();
				  
				  Object val = COConfigurationManager.getParameter( name );
				  
				  if ( val instanceof String || val instanceof Long ){
					  
				  }else if ( val instanceof byte[]){
					  
					  val = new String((byte[])val, "UTF-8" );
					  
				  }else if ( val instanceof Integer ){
					  
					  val = new Long(((Integer)val).intValue());
	
				  }else if ( val instanceof List ){
					  
					  val = null;
					  
				  }else if ( val instanceof Map ){
					  
					  val = null;
					  
				  }else if ( val instanceof Boolean ){
					  
					  val = new Long(((Boolean)val).booleanValue()?1:0);
					  
				  }else if ( val instanceof Float || val instanceof Double ){
					  
					  val = val.toString();
				  }
				  
				  if ( val != null ){
					 
					  result.put( name, val );
				  }
			  }catch( Throwable e ){
				  
				  Debug.printStackTrace(e);
			  }
		  }
		  
		  return( result );
	  }
	
	public void
	save()
	{
		COConfigurationManager.save();
	}
		
	public File
	getPluginUserFile(
		String	name )
	{
		
		String	dir = plugin_interface.getUtilities().getAzureusUserDir();
		
		File	file = new File( dir, "plugins" );

		String	p_dir = plugin_interface.getPluginDirectoryName();
		
		if ( p_dir.length() != 0 ){
			
			int	lp = p_dir.lastIndexOf(File.separatorChar);
			
			if ( lp != -1 ){
				
				p_dir = p_dir.substring(lp+1);
			}
			
			file = new File( file, p_dir );
			
		}else{
			
			String	id = plugin_interface.getPluginID();
			
			if ( id.length() > 0 && !id.equals( PluginInitializer.INTERNAL_PLUGIN_ID )){
			
				file = new File( file, id );
				
			}else{
				
				throw( new RuntimeException( "Plugin was not loaded from a directory" ));
			}
		}
	
		
		FileUtil.mkdirs(file);
		
		return( new File( file, name ));
	}
	
	public void
	addListener(
		final PluginConfigListener	l )
	{
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					l.configSaved();
				}
			});
	}
	
	public boolean hasParameter(String param_name) {
		// Don't see any reason why a plugin should care whether it is looking
		// at a system default setting or not, so we'll do an implicit check.
		return COConfigurationManager.hasParameter(param_name, false);
	}
	
	public boolean hasPluginParameter(String param_name) {
		// We should not have default settings for plugins in configuration
		// defaults, so we don't bother doing an implicit check.
		return COConfigurationManager.hasParameter(this.key + param_name, true);
	}
	
}
