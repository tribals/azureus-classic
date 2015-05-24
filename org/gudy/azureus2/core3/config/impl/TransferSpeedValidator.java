package org.gudy.azureus2.core3.config.impl;

/*
 * Created on 13-Feb-2005
 * Created by James Yeh
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

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.global.GlobalManager;

import com.aelitis.azureus.core.AzureusCore;

/**
 * Provides validation for transfer speed settings
 * @version 1.0
 * @since 1.4
 * @author James Yeh
 */
public final class TransferSpeedValidator
{
	public static final String AUTO_UPLOAD_CONFIGKEY 			=  "Auto Upload Speed Enabled";
	public static final String AUTO_UPLOAD_SEEDING_CONFIGKEY 	=  "Auto Upload Speed Seeding Enabled";
    
    public static final String UPLOAD_CONFIGKEY 		=  "Max Upload Speed KBs";
    public static final String UPLOAD_SEEDING_CONFIGKEY =  "Max Upload Speed Seeding KBs";
    public static final String DOWNLOAD_CONFIGKEY 		=  "Max Download Speed KBs";

    public static final String UPLOAD_SEEDING_ENABLED_CONFIGKEY =  "enable.seedingonly.upload.rate";
    
    private final String configKey;
    private final Number configValue;

    private static boolean seeding_upload_enabled;
    
    static{
    	    		
    	COConfigurationManager.addAndFireParameterListener(
    			UPLOAD_SEEDING_ENABLED_CONFIGKEY,
    			new ParameterListener()
        		{
        			public void 
        			parameterChanged(
        				String parameterName)
        			{
        				seeding_upload_enabled = COConfigurationManager.getBooleanParameter( parameterName );
        			}
        		});	
    }
    
    /**
     * Creates a TransferSpeedValidator with the given configuration key and value
     * @param configKey Configuration key; must be "Max Upload Speed KBs" or "Max Download Speed KBs"
     * @param value Configuration value to be validated
     */
    public TransferSpeedValidator(final String configKey, final Number value)
    {
        this.configKey = configKey;
        configValue = value;
    }

    /**
     * Gets the transformed value as an Integer
     */
    private static Object validate(final String configKey, final Number value)
    {
        //assert value instanceof Number;

        int newValue = value.intValue();

        if(newValue < 0)
        {
            newValue = 0;
        }

        if(configKey == UPLOAD_CONFIGKEY)
        {
            final int downValue = COConfigurationManager.getIntParameter(DOWNLOAD_CONFIGKEY);

            if(
                    newValue != 0 &&
                    newValue < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED &&
                    (downValue == 0 || downValue > newValue*2)
            )
            {
                newValue = (downValue + 1)/2;
                //COConfigurationManager.setParameter(DOWNLOAD_CONFIGKEY, newValue * 2);
            }
        }
        else if(configKey == DOWNLOAD_CONFIGKEY)
        {
            final int upValue = COConfigurationManager.getIntParameter(UPLOAD_CONFIGKEY);

            if(
                    upValue != 0 &&
                    upValue < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED
            )
            {
                if(newValue > upValue*2)
                {
                    newValue = upValue*2;
                    //COConfigurationManager.setParameter(UPLOAD_CONFIGKEY, (newValue+1)/2);
                }
                else if(newValue == 0)
                {
                    newValue = upValue*2;
                    //COConfigurationManager.setParameter(UPLOAD_CONFIGKEY, 0);
                }
            }
        }else if ( configKey == UPLOAD_SEEDING_CONFIGKEY ){

        		// nothing to do as this is active only when were not downloading
        		// so we don't really care
        }else
        {
            throw new IllegalArgumentException("Invalid Configuation Key; use key for max upload and max download");
        }

        return new Integer(newValue);
        //return value;
    }

    /**
     * Validates the given configuration key/value pair and returns the validated value
     * @return Modified configuration value that conforms to validation as an Integer
     */
    public Object getValue()
    {
        return validate(configKey, configValue);
    }
    
    public static String
    getActiveUploadParameter(
    	GlobalManager	gm )
    {
       if ( seeding_upload_enabled && gm.isSeedingOnly()){
        	
        	return( TransferSpeedValidator.UPLOAD_SEEDING_CONFIGKEY );
        	
      	}else{
      		
      		return( TransferSpeedValidator.UPLOAD_CONFIGKEY );
      	}
    }
    
    public static String
    getDownloadParameter()
    {
    	return( DOWNLOAD_CONFIGKEY );
    }
    
    public static boolean
    isAutoUploadAvailable(
    	AzureusCore	core )
    {
    	return( core.getSpeedManager().isAvailable());
    }
    
    public static String
    getActiveAutoUploadParameter(
    	GlobalManager	gm )
    {
    		// if downloading+seeding is set then we always use this regardless of
    		// only seeding status
    	
    	if ( COConfigurationManager.getBooleanParameter(TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY)){
    		
    		return( TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY );
    	}
    	
    	if ( gm.isSeedingOnly()){
        	
        	return( TransferSpeedValidator.AUTO_UPLOAD_SEEDING_CONFIGKEY );
        	
      	}else{
      		
      		return( TransferSpeedValidator.AUTO_UPLOAD_CONFIGKEY );
      	}
    }
    
    public static boolean
    isAutoSpeedActive(
    	GlobalManager	gm )
    {
    	return( COConfigurationManager.getBooleanParameter( getActiveAutoUploadParameter( gm )));
    }
}
