/*
 * Created on May 29, 2006 4:23:01 PM
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
 */
package com.aelitis.azureus.ui.skin;

import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Constants;

/**
 * Implementation of SkinProperties using a java.util.Properties loaded from
 * hard coded paths.
 * <P>
 * Three level lookup of keys:
 * <li>(plugin) skin property file
 * <li>defaults property file
 * <li>Azureus MessageText class
 * <br>
 * Additionally, checks each for platform specific keys.
 * <p><br>
 * Values containing "{*}" are replaced with a lookup of *  
 * 
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class SkinPropertiesImpl implements SkinProperties
{
	private static final LogIDs LOGID = LogIDs.UI3;

	private static final String PATH_SKIN_DEFS = "com/aelitis/azureus/ui/skin/";

	private static final String FILE_SKIN_DEFS = "skin3.properties";

	private static final String MESSAGES = "com.aelitis.azureus.ui.skin.messages3";

	private static final String LOCATION_SKIN = "skin/display.properties";

	private static final Pattern PAT_PARAM_ALPHA = Pattern.compile("\\{([^0-9].+?)\\}");

	private static final Pattern PAT_PARAM_NUM = Pattern.compile("\\{([0-9]+?)\\}");

	private Properties properties;

	public SkinPropertiesImpl() {
		properties = new Properties();
		InputStream is;
		ClassLoader classLoader = SkinPropertiesImpl.class.getClassLoader();

		is = classLoader.getResourceAsStream(PATH_SKIN_DEFS + FILE_SKIN_DEFS);
		if (is != null) {
			try {
				properties.load(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Logger.log(new LogEvent(LOGID, PATH_SKIN_DEFS + FILE_SKIN_DEFS
					+ " not found"));
		}

		is = classLoader.getResourceAsStream(LOCATION_SKIN);
		if (is != null) {
			try {
				properties.load(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String sFiles = (String) properties.getProperty("skin.include");
		if (sFiles != null) {
			String[] sFilesArray = sFiles.split(",");
			for (int i = 0; i < sFilesArray.length; i++) {
				String sFile = PATH_SKIN_DEFS + sFilesArray[i] + ".properties";
				try {
					is = classLoader.getResourceAsStream(sFile);
					if (is != null) {
						properties.load(is);
					} else {
						System.err.println("No Skin " + sFile + " found");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		MessageText.integratePluginMessages(MESSAGES,
				this.getClass().getClassLoader());

	}

	public Properties getProperties() {
		return properties;
	}

	public void addProperty(String name, String value) {
		properties.put(name, value);
	}

	private String getValue(String name, String[] params) {
		String value = null;
		String osName = null;

		if (name == null) {
			return null;
		}

		if (Constants.isOSX) {
			osName = name + "._mac";
		} else if (Constants.isUnix) {
			osName = name + "._unix";
		} else if (Constants.isFreeBSD) {
			osName = name + "._freebsd";
		} else if (Constants.isLinux) {
			osName = name + "._linux";
		} else if (Constants.isSolaris) {
			osName = name + "._solaris";
		} else if (Constants.isWindows) {
			osName = name + "._windows";
		}

		if (osName != null) {
			value = properties.getProperty(osName);
		}

		if (value == null) {
			value = properties.getProperty(name);
		}

		if (value != null && value.indexOf('}') > 0) {
			Matcher matcher;

			if (params != null) {
				matcher = PAT_PARAM_NUM.matcher(value);
				while (matcher.find()) {
					String key = matcher.group(1);
					try {
						int i = Integer.parseInt(key);

						if (i < params.length) {
							value = value.replaceAll("\\Q{" + key + "}\\E", params[i]);
						}
					} catch (Exception e) {
					}
				}
			}

			matcher = PAT_PARAM_ALPHA.matcher(value);
			while (matcher.find()) {
				String key = matcher.group(1);
				String text = getValue(key, params);
				if (text == null) {
					text = MessageText.getString(key);
				}
				value = value.replaceAll("\\Q{" + key + "}\\E", text);
			}
		}

		return value;
	}

	public int getIntValue(String name, int def) {
		String value = getValue(name, null);
		if (value == null) {
			return def;
		}

		int result = def;
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return result;
	}

	public int[] getColorValue(String name) {
		int[] colors = new int[3];
		String value = getValue(name, null);

		if (value == null || value.length() == 0) {
			colors[0] = colors[1] = colors[2] = -1;
			return colors;
		}

		try {
			if (value.charAt(0) == '#') {
				// hex color string
				long l = Long.parseLong(value.substring(1), 16);
				colors[0] = (int) ((l >> 16) & 255);
				colors[1] = (int) ((l >> 8) & 255);
				colors[2] = (int) (l & 255);
			} else {
				StringTokenizer st = new StringTokenizer(value, ",");
				colors[0] = Integer.parseInt(st.nextToken());
				colors[1] = Integer.parseInt(st.nextToken());
				colors[2] = Integer.parseInt(st.nextToken());
			}
		} catch (Exception e) {
			e.printStackTrace();
			colors[0] = colors[1] = colors[2] = -1;
		}

		return colors;
	}

	public String getStringValue(String name) {
		return getStringValue(name, (String[]) null);
	}

	public String getStringValue(String name, String def) {
		return getStringValue(name, (String[]) null, def);
	}

	public String[] getStringArray(String name) {
		return getStringArray(name, (String[]) null);
	}

	public String[] getStringArray(String name, String[] params) {
		String s = getValue(name, params);
		if (s == null) {
			return null;
		}

		String[] values = s.split("\\s*,\\s*");
		if (values == null) {
			return new String[] { s
			};
		}

		return values;
	}

	public String getStringValue(String name, String[] params) {
		return getValue(name, params);
	}

	public String getStringValue(String name, String[] params, String def) {
		String s = getValue(name, params);
		return (s == null) ? def : s;
	}
}
