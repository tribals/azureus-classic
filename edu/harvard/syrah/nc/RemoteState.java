package edu.harvard.syrah.nc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/*
 * NCLib - a network coordinate library
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details (
 * see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/**
 * The state kept of a remote node between samples.
 * 
 * @author Michael Parker, Jonathan Ledlie
 * 
 * @param <T>
 * the type of the unique identifier of a host
 */
public class RemoteState<T> {
  // made not final so they can be changed by simulator
	protected static double SAMPLE_PERCENTILE = 0.5;
  // Don't keep more than this many samples
  public static int MAX_SAMPLE_SIZE = 16;
  // Don't use a guy unless we have this many samples
  public static int MIN_SAMPLE_SIZE = 4;
  
	protected final T addr;
	protected final WindowStatistic ping_samples;

	protected Coordinate last_coords;
	protected double last_error;
	protected long last_update_time;

	public RemoteState(T _addr) {
		addr = _addr;
    ping_samples = new WindowStatistic(MAX_SAMPLE_SIZE);

		last_coords = null;
		last_error = 0.0;
		last_update_time = -1L;
	}

  public T getAddress () {
    return addr;
  }
  
	public void assign(Coordinate _last_coords, double _last_error,
	    long _curr_time) {
	  last_coords = _last_coords;
		last_error = _last_error;
		last_update_time = _curr_time;
	}

  public void addSample(double sample_rtt, long sample_age, Coordinate r_coord,
    double r_error, long curr_time) {
		ping_samples.add(sample_rtt);
		last_coords = r_coord;
		last_error = r_error;
    if (sample_age > 0)
      last_update_time = curr_time-sample_age;
    else
      last_update_time = curr_time;
	}
  
  public boolean isValid (long curr_time) {
    if (getLastError() <= 0. || last_update_time <= 0 ||
        last_coords.atOrigin()) {
      return false;
    }
      
    if (getSampleSize() >= MIN_SAMPLE_SIZE && getSample() > 0) {
      return true;
    }

    if (getSampleSize() >= 2 && ping_samples.withinVariance(.1)) {
      return true;
    }
    
    return false;
  }
  
	public double getSample() {
		return ping_samples.getPercentile(SAMPLE_PERCENTILE);
	}

  public int getSampleSize() {
    return ping_samples.getSize();
  }
  /*
  public boolean isLowVariance () {
    return ping_samples.isLowVariance();
  }
  */
  
	public Coordinate getLastCoordinate() {
		return last_coords;
	}

	public double getLastError() {
		return last_error;
	}
/*
	public boolean beenSampled() {
		return (last_update_time >= 0L);
	}
*/
	public long getLastUpdateTime() {
		return last_update_time;
	}
  
	public static void main (String args[]) {
	  System.out.println("Testing Remote State Object");
	  String sampleFile = args[0];
	  RemoteState<String> rs = new RemoteState<String>(sampleFile);
	  BufferedReader sampleReader = null;
	  try {
	    sampleReader = new BufferedReader (new FileReader (new File (sampleFile)));
	  }catch (FileNotFoundException ex) {
	    System.err.println("Cannot open file "+sampleFile+": "+ex);
	    System.exit(-1);
	  } 
	  
	  long sample_age = 0;
	  Coordinate r_coord = null;
	  double r_error = 0;
	  
	  try {
	    String sampleLine = sampleReader.readLine();
	    while (sampleLine != null) {
	      // reads in timestamp in ms and raw rtt
	      StringTokenizer sampleTokenizer = new StringTokenizer (sampleLine);
	      long curr_time = Long.parseLong((String)(sampleTokenizer.nextElement()));
	      int rawRTT = Integer.parseInt((String)(sampleTokenizer.nextElement()));
	      sampleLine = sampleReader.readLine();
	      rs.addSample (rawRTT, sample_age, r_coord, r_error, curr_time);
        double smoothedRTT = rs.getSample();
        System.out.println(curr_time+" raw "+rawRTT+" smooth "+smoothedRTT);
	    }
	  } catch (Exception ex) {
	    System.err.println("Problem parsing "+sampleFile+": "+ex);
	    System.exit(-1);     
	  }
	}
  
}
