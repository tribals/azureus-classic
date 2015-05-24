/*
 * @author Last modified by $Author: ledlie $
 * @version $Revision: 1.3 $ on $Date: 2006/05/19 20:02:33 $
 * @since Mar 7, 2006
 */
package edu.harvard.syrah.nc;

/*
 * NCLib - a network coordinate library 
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

public class EWMAStatistic {

    public static final double GAIN = 0.01;
    protected final double gain;
    protected double value;
    
    public EWMAStatistic (double g) {
      gain = g;
      value = 0;
    }

    public EWMAStatistic () {
      gain = GAIN;
      value = 0;
    }
    
    synchronized public void add (double item) {
       value = (GAIN*item)+((1.-GAIN)*value);
     }
     
    synchronized public double get () {
       return value;
     }
       
}
