/*
 * Created on 27 juin 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

/**
 * @author Olivier
 * 
 */
public class TimeFormatter {
  // XXX should be i18n'd
	static final String[] TIME_SUFFIXES = { "s", "m", "h", "d" };

	/**
	 * Format time into two time sections, the first chunk trimmed, the second
	 * with always with 2 digits.  Sections are *d, **h, **m, **s.  Section
	 * will be skipped if 0.   
	 * 
	 * @param time time in ms
	 * @return Formatted time string
	 */
	public static String format(long time) {
		if (time >= Constants.INFINITY_AS_INT)
			return Constants.INFINITY_STRING;

		if (time < 0)
			return "";

		// secs, mins, hours, days
		int[] vals = { (int) time % 60, (int) (time / 60) % 60,
				(int) (time / 3600) % 24, (int) (time / 86400) };

		int end = vals.length - 1;
		while (vals[end] == 0 && end > 0) {
			end--;
		}
		
		String result = vals[end] + TIME_SUFFIXES[end];

		// skip until we have a non-zero time section
		do {
			end--;
		} while (end >= 0 && vals[end] == 0);
		
		if (end >= 0)
			result += " " + twoDigits(vals[end]) + TIME_SUFFIXES[end];

		return result;
	}

    public static String formatColon(long time)
    {
      if (time >= Constants.INFINITY_AS_INT) return Constants.INFINITY_STRING;
      if (time < 0) return "";

      int secs = (int) time % 60;
      int mins = (int) (time / 60) % 60;
      int hours = (int) (time /3600) % 24;
      int days = (int) (time / 86400);
      
      String result = "";
      if (days > 0) result = days + "d ";
      result += twoDigits(hours) + ":" + twoDigits(mins) + ":" + twoDigits(secs);

      return result;
    }
    
    private static String twoDigits(int i) {
      return (i < 10) ? "0" + i : String.valueOf(i);
    }
}
