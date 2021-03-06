/*
 * Created by Joseph Bridgewater
 * Created on Feb 24, 2006
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

/**
 * @author MjrTom Feb 24, 2006
 * This is for utilities for calculating custom object hashCode() values
 */
public final class HashCodeUtils
{
    public static final int hashMore(final int hash, final int more)
    {
        int result =hash <<1;
        if (result <0)
            result |=1;
        return result ^more;
    }
    
    public static final int hashMore(final int hash, final long more)
    {
        int result =hashMore(hash, (int)(more >>>32));
        return hashMore(result, (int)(more &0xffff));
    }
    
    public static final int hashMore(final int hash, final boolean[] more)
    {
        int result =hash <<1;
        if (result <0)
            result |=1;
        if (more[0])
            result ^=1;
        for (int i =1; i <more.length; i++)
        {
            result <<=1;
            if (result <0)
                result |=1;
            if (more[i])
                result ^=1;
        }
        return result;
    }
    
}
