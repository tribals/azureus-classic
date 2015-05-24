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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/*
 * @version $Revision: 1.8 $ on $Date: 2006/09/08 21:25:31 $
 * @since Mar 7, 2006
 */

/**
 * 
 */
public class WindowStatistic {

  // Could be re-written as a circular buffer to avoid
  // memory (re-)alloc

  public static boolean debug = false; 
  
  protected final int max_history;
  //protected final long expiration;
  protected final List<Statistic> samples;
  protected boolean dirty = true;
  
  // short-cut code (getPercentile was taking a significant percentage of time in simulation)
  protected double previousPercentile = -1;
  protected double previousValue = -1;
  
  class Statistic {
    public final double sample;
    public Statistic (double _sample) {
      sample = _sample;
    }
    /*
    public int compareTo (Object _s) {
      Statistic s = (Statistic)_s;
      if (stamp < s.stamp)
        return -1;
      else if (stamp > s.stamp)
        return 1;
      else
        return 0;
    }
    */
    public String toString () {
      //return new String ("[i="+sample+",s="+stamp+"]");
      return new String ("v="+sample);
    }
  }
  
  public WindowStatistic (int _max_history) {
    assert (_max_history >= 0);
    max_history = _max_history;
    samples = new LinkedList<Statistic>();
  }
  
   synchronized public void add (double sample) {
     samples.add(new Statistic(sample));
     dirty = true;
     while (samples.size() > max_history) {
       samples.remove(0);
       if (debug) {
         System.out.println ("tossing sample "+samples.get(0)+", size="+samples.size());
       }
     }
   }
 
   synchronized public int getSize () {
     return samples.size();
   }
 
   synchronized public boolean withinVariance (double pct) {
     if (samples.size() < 2) return true;
     double mean = getMean();
     for (int i = 0; i < samples.size(); i++) {
       double pctDiff = Math.abs(samples.get(i).sample-mean)/mean;
       if (pctDiff > pct) {
         return false;
       }
     }
     return true;
   }
   
   synchronized public double getPercentile (double p) {
     if (!dirty && p == previousPercentile)
       return previousValue; 
     double val = calcPercentile (p);
     previousPercentile = p;
     previousValue = val;
     dirty = false;
     return val;    
   }  
   
   protected double calcPercentile (double p) {     
     if (samples.size() == 0) return 0;
     Double[] samples_copy = new Double[samples.size()];
     for (int i = 0; i < samples.size(); i++) {
       samples_copy[i] = samples.get(i).sample;
       if (debug)
         System.out.println (i+" "+samples_copy[i]);
     }
     Arrays.sort(samples_copy);
     int percentile = (int)(samples_copy.length*p);
     if (percentile < 0) percentile = 0;
     double val = samples_copy[percentile].doubleValue();
     if (debug)
       System.out.println ("p= "+p+" per="+percentile+" val="+val);
     return val;
   }

   protected double getSum () {
     double sampleSum = 0.;
     if (samples.size() > 0) {
       for (int i = 0; i < samples.size(); i++) {
         sampleSum += samples.get(i).sample;
       }
     }
     return sampleSum;
   }

   // can be undefined
   public double getMean () {
     return getSum()/getSize();
   }
     
}
