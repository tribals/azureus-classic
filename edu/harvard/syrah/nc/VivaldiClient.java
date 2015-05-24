package edu.harvard.syrah.nc;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A class that is responsible for updating the local Vivaldi coordinates, both
 * at the system and application level, and also maintaining the state of remote
 * hosts that support Vivaldi.
 * 
 * @author Michael Parker, Jonathan Ledlie
 * 
 * @param <T>
 * the type of the unique identifier of a host
 */
public class VivaldiClient<T> {

  //protected static edu.harvard.syrah.prp.Log crawler_log =
	//new edu.harvard.syrah.prp.Log(VivaldiClient.class);
  protected static Logger crawler_log = Logger.getLogger(VivaldiClient.class.getName()); 
  
  public static final boolean SIMULATION = false;
  public static boolean debug = false;
  public static boolean debugCrawler = false;
  // include even good events in output, not just problems
  public static boolean debugGood = false;

  // Old version
  public static final byte VERSION_02 = 0x02;
  // Height added
  public static final byte VERSION_03 = 0x03;
  // Changed filter percentile from 0.125 to 0.5
  // and added ping timer at 10sec
  // c_error = 0.10, c_control = 0.25
  public static final byte VERSION_04 = 0x04;
  public static final byte CURRENT_VERSION = VERSION_04;

  
	public static double COORD_ERROR = 0.10; // c_e parameter
	public static double COORD_CONTROL = 0.25; // c_c parameter

  // The last element of the coordinate is a "height" away from the Euclidean space
  public static boolean USE_HEIGHT = true;
  
  // Try to minimize our error between up to MAX_NEIGHBORS guys at once
  
    final public static int MAX_NEIGHBORS = 512;
    final protected static int WINDOW_SIZE = 64;

  // Toss remote state of nodes if we haven't heard from them for three days
  // This allows us to keep around a list of RTTs for the node even if
  // we currently aren't using its coordinate for update
    public static long RS_EXPIRATION = 3 * 24 * 60 * 60 * 1000;
    final public static long MAINTENANCE_PERIOD = 10 * 60 * 1000; // ten minutes
    // target max number of remote states kept
    // set to be larger than MAX_NEIGHBORS
    public final static int MAX_RS_MAP_SIZE = 32 * 1024;
    private long lastMaintenanceStamp = 0;

  
	public static Random random = new Random();

	// Do an update if we've moved a third of the way to our nearest known
	// neighbor
	// Lowering this value leads to more frequent updates
	// Should be less than 0.5
	public static final double APP_UPDATE_THRESHOLD = 0.1;

	// completely ignore any RTT larger than ten seconds
	public static final double OUTRAGEOUSLY_LARGE_RTT = 20000.0;

	// range from origin where pull of gravity is 1
	public static double GRAVITY_DIAMETER = 512.;
	
	// We reject remote coords with any component larger than this value
	// This is to prevent serialization problems from leaking into coords
	// we're actually going to use
	// Gravity should keep everybody within this ball
	public static double MAX_DIST_FROM_ORIGIN = 60000.;
	

	final static protected NumberFormat nf = NumberFormat.getInstance();
	final static protected int NFDigits = 3;
	static boolean haveSetFormat = false;
  
	protected final int num_dims;

	protected Coordinate app_coord;

  protected Coordinate sys_coord;
  
  // error should always be less than or equal to MAX_ERROR
  // and greater than 0
  protected double error;
  public static final double MAX_ERROR = 1.;
  
	public static boolean keepStatistics = true;
	// keeping an EWMA of some of these things gives skewed results
	// so we use a filter
	final public static int RUNNING_STAT_HISTORY = 1024;
	// 30 minutes
	//final public static long STAT_EXPIRE_TIME = 30 * 60 * 1000;
	protected WindowStatistic running_sys_error;
	protected WindowStatistic running_app_error;
	protected EWMAStatistic running_sys_dd;
	protected EWMAStatistic running_app_dd;
	protected EWMAStatistic running_neighbors_used;
	protected EWMAStatistic running_relative_diff;
	protected EWMAStatistic running_sys_update_frequency;
	protected EWMAStatistic running_app_update_frequency;
  protected EWMAStatistic running_age;
  protected EWMAStatistic running_gravity;
	protected long time_of_last_app_update = -1;

  // keep the list of neighbors around for computing statistics
	protected final List<RemoteState<T>> neighbors;
  
  // note: not just part of statistics
  // this is returned to querier of our coords so he knows how stale they are
  protected long time_of_last_sys_update = -1;

	protected final ObserverList obs_list;

	protected final HashMap<T, RemoteState<T>> rs_map;

	protected final Set<T> hosts;

	protected Coordinate start_centroid;

	protected boolean updated_app_coord_at_least_once = false;

	protected final List<Coordinate> start_coords;

	protected final List<Coordinate> current_coords;

	protected Coordinate nearest_neighbor;
	protected T local_addr;
  
  
	/**
	 * Creates a new instance. Typically an application should only have one
	 * instance of this class, as it only needs one set of Vivaldi coordinates.
	 * 
	 * @param _num_dims
	 * the number of Euclidian dimensions coordinates should have
	 */
	public VivaldiClient(int _num_dims) {
		num_dims = _num_dims;

		app_coord = new Coordinate(num_dims);
    sys_coord = new Coordinate(num_dims);

		error = MAX_ERROR;
    neighbors = new ArrayList<RemoteState<T>>();
    
		obs_list = new ObserverList();
		rs_map = new HashMap<T, RemoteState<T>>();
		hosts = Collections.unmodifiableSet(rs_map.keySet());
		start_coords = new LinkedList<Coordinate>();
		current_coords = new LinkedList<Coordinate>();
    nearest_neighbor = null;
    
    //bootstrapCoordinates ();
    
    if (keepStatistics) {
			running_sys_update_frequency = new EWMAStatistic();
			running_app_update_frequency = new EWMAStatistic();
			running_sys_error = new WindowStatistic(RUNNING_STAT_HISTORY);
			running_app_error = new WindowStatistic(RUNNING_STAT_HISTORY);
			running_sys_dd = new EWMAStatistic();
			running_app_dd = new EWMAStatistic();
			running_neighbors_used = new EWMAStatistic();
			running_relative_diff = new EWMAStatistic();
      running_age = new EWMAStatistic();
      running_gravity = new EWMAStatistic();
		}
		
		if (!haveSetFormat) {
		  if (nf.getMaximumFractionDigits() > NFDigits) {
		    nf.setMaximumFractionDigits(NFDigits);
		  }
		  if (nf.getMinimumFractionDigits() > NFDigits) {
		    nf.setMinimumFractionDigits(NFDigits);
		  }
		  nf.setGroupingUsed(false);
		  haveSetFormat = true;
		}
	}

  // for debugging simulations
  public void setLocalID (T _local_addr) {
    local_addr = _local_addr;
  }
  
	// See Lua IMC 2005, Pietzuch WORLDS 2005, Ledlie ICDCS 2006
	// for description of these statistics
	protected ApplicationStatistics computeApplicationStatistics() {

		ApplicationStatistics appStats = new ApplicationStatistics();

		if (sys_coord.atOrigin() || neighbors == null ||
      neighbors.size() == 0) return appStats;    

		int rrl_wrong = 0;
		int rrl_count = 0;
		double narl_loss = 0;
		double narl_sum = 0;
		double ralp_loss = 0;
		double ralp_sum = 0;

    // TODO might want to use the app coord here so as to get the average location
    
		for (Iterator<RemoteState<T>> i = neighbors.iterator(); i.hasNext();) {
			RemoteState<T> A_rs = i.next();

			double A_rtt = A_rs.getSample();
			double A_metric = sys_coord.distanceTo(A_rs.getLastCoordinate());
			if (A_rtt > 0 && A_metric > 0) {

				for (Iterator<RemoteState<T>> j = neighbors.iterator(); j
						.hasNext();) {
					RemoteState<T> B_rs = j.next();

					if (!A_rs.addr.equals(B_rs.addr)) {

						double B_rtt = B_rs.getSample();
						double B_metric = sys_coord.distanceTo(B_rs
								.getLastCoordinate());
						if (B_rtt > 0 && B_metric > 0) {

							double rtt_diff = Math.abs(A_rtt - B_rtt);
							rrl_count++;
							narl_sum += rtt_diff;

							if ((A_rtt > B_rtt && A_metric < B_metric)
									|| (B_rtt > A_rtt && B_metric < A_metric)) {
								// oops coordinates have incorrectly ranked
								// these two guys
								rrl_wrong++;
								narl_loss += rtt_diff;
							}

							// relative latency penalty for using A,
							// which the metric says is closer,
							// when A is actually further away
							if (A_rtt > B_rtt && A_metric < B_metric) {
								ralp_loss += rtt_diff;
								ralp_sum += A_rtt;
							}
							if (B_rtt > A_rtt && B_metric < A_metric) {
								ralp_loss += rtt_diff;
								ralp_sum += B_rtt;
							}
						}
					}
				}
			}
		}

		appStats.validLinkCount = rrl_count;
		if (rrl_count > 0)
			appStats.rrl = rrl_wrong / (double) rrl_count;
		if (narl_sum > 0)
			appStats.narl = narl_loss / narl_sum;
		if (ralp_sum > 0)
			appStats.ralp = ralp_loss / ralp_sum;
		return appStats;
	}

	// poor man's public struct
	class ApplicationStatistics {
		double rrl = 0;
		double narl = 0;
		double ralp = 0;
		int validLinkCount = 0;

		public ApplicationStatistics() {
		};
	}

	synchronized public String toString() {
		if (keepStatistics) {
			ApplicationStatistics appStats = computeApplicationStatistics();

			return new String("[sc=" + sys_coord + ",ac=" + app_coord + ",er="
					+ nf.format(error) + ",sys_re50="
					+ nf.format(running_sys_error.getPercentile(.5))
					+ ",sys_re95="
					+ nf.format(running_sys_error.getPercentile(.95))
					+ ",app_re50="
					+ nf.format(running_app_error.getPercentile(.5))
					+ ",app_re95="
					+ nf.format(running_app_error.getPercentile(.95))
					+ ",sys_dd=" + nf.format(running_sys_dd.get()) + ",app_dd="
					+ nf.format(running_app_dd.get()) + ",ne="
					+ nf.format(running_neighbors_used.get()) + ",rd="
					+ nf.format(running_relative_diff.get()) + ",sf="
					+ nf.format(running_sys_update_frequency.get()) + ",af="
					+ nf.format(running_app_update_frequency.get()) + ",rrl="
					+ nf.format(appStats.rrl) + ",narl="
					+ nf.format(appStats.narl) + ",ralp="
					+ nf.format(appStats.ralp) + ",age="
					+ getAge(System.currentTimeMillis()) + ",vl="
					+ nf.format(appStats.validLinkCount) +",gr=" 
          + nf.format(running_gravity.get())+",nn="
					+ nf.format(sys_coord.distanceTo(nearest_neighbor)) + "]");
		}
		else {
			return new String("[sc=" + sys_coord + ",ac=" + app_coord + ",er="
					+ nf.format(error) + ",nn="
					+ nf.format(sys_coord.distanceTo(nearest_neighbor)) + "]");
		}
	}

	synchronized public Hashtable<String, Double> getStatistics() {
		Hashtable<String, Double> stats = new Hashtable<String, Double>();
		for (int i = 0; i < num_dims; i++) {
			stats.put("sys_coord_" + i, sys_coord.coords[i]);
			stats.put("app_coord_" + i, sys_coord.coords[i]);
		}
		stats.put("er", error);
		stats.put("nn", sys_coord.distanceTo(nearest_neighbor));

		if (keepStatistics) {
			ApplicationStatistics appStats = computeApplicationStatistics();

			stats.put("rrl", appStats.rrl);
			stats.put("narl", appStats.narl);
			stats.put("ralp", appStats.ralp);
			stats.put("age", new Double (getAge(System.currentTimeMillis())));
			stats.put("vl", new Double(appStats.validLinkCount));
			stats.put("gr", running_gravity.get());
      
			stats.put("sys_re50", running_sys_error.getPercentile(.5));
			stats.put("sys_re95", running_sys_error.getPercentile(.95));
			stats.put("app_re50", running_app_error.getPercentile(.5));
			stats.put("app_re95", running_app_error.getPercentile(.95));

			stats.put("sys_dd", running_sys_dd.get());
			stats.put("app_dd", running_app_dd.get());

			stats.put("ne", running_neighbors_used.get());
			stats.put("rd", running_relative_diff.get());
			stats.put("sf", running_sys_update_frequency.get());
			stats.put("af", running_app_update_frequency.get());
		}
		return stats;
	}

	synchronized public void reset() {
		sys_coord.reset();
		app_coord.reset();
		error = MAX_ERROR;
		rs_map.clear();
		start_coords.clear();
		current_coords.clear();
		nearest_neighbor = null;		
	}

	/**
	 * Returns whether the application level coordinates have been updated at
	 * least once.
	 * 
	 * @return <code>true</code> if updated, <code>false</code> otherwise
	 */
  synchronized public boolean updatedYet() {
		return updated_app_coord_at_least_once;
	}

	/**
	 * Returns the dimension of the Euclidian space coordinates are embedded in.
	 * 
	 * @return the coordinate space dimension
	 */
  synchronized public int getNumDimensions() {
		return num_dims;
	}

	/**
	 * Returns the application-level Vivaldi coordinates.
	 * 
	 * @return the application-level coordinates
	 */
  synchronized public Coordinate getApplicationCoords() {
		return new Coordinate(sys_coord);
	}

	/**
	 * Returns the system-level Vivaldi coordinates. These coordinates change
	 * more frequently than the application-level coordinates.
	 * 
	 * @return the system-level coordinates
	 */
  synchronized public Coordinate getSystemCoords() {
		return new Coordinate(sys_coord);
	}

  /**
   * Returns the system-level error, which denotes the accuracy of the
   * system-level coordinates.
   * 
   * @return the system-level error
   */
  synchronized public double getSystemError() {
    return error;
  }

  /**
   * Returns the age of our coordinate
   * Note that this does not require clock-synchronization
   * because it is relative to our coordinate
   * 
   * @return relative age of our coordinate since we last updated it
   */
  synchronized public long getAge(long curr_time) {
    if (curr_time < time_of_last_sys_update) return 0;
    return curr_time - time_of_last_sys_update;
  }

  
	/**
	 * Returns the list of observers, to which observers for the
	 * application-level coordinate can be added, removed, and so forth.
	 * 
	 * @return the list of observers for the application-level coordinate
	 */
  synchronized public ObserverList getObserverList() {
		return obs_list;
	}

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that
	 * supports Vivaldi has joined the system. State associated with the new
	 * host is created. This method succeeds and returns <code>true</code>
	 * only if the host is not already registered with this
	 * <code>VivaldiClient</code> object.
	 * 
	 * @param addr
	 * the address of the joining host
	 * @return <code>true</code> if <code>addr</code> is registered and its
	 * associated state created, <code>false</code> otherwise
	 */
  synchronized public boolean addHost(T addr) {
		if (rs_map.containsKey(addr)) {
			return false;
		}

		RemoteState<T> rs = new RemoteState<T>(addr);
		rs_map.put(addr, rs);
		return true;
	}

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that
	 * supports Vivaldi and has the provided coordinates and error has joined
	 * the system. State associated with the new host is created. This method
	 * succeeds and returns <code>true</code> only if the host is not already
	 * registered with this <code>VivaldiClient</code> object.
	 * 
	 * @param addr
	 * the address of the joining host
	 * @param _r_coord
	 * the app-level coordinates of the remote host
	 * @param r_error
	 * the system-level error of the remote host
	 * @param sample_rtt
	 * the RTT sample to the remote host
	 * @param curr_time
	 * the current time, in milliseconds
	 * @param can_update
	 * <code>true</code> if this method can update a host already present
	 * @return <code>true</code> if <code>addr</code> is registered and its
	 * associated state created, <code>false</code> otherwise
	 */

  synchronized public boolean addHost(T addr, Coordinate _r_coord, double r_error,
			long curr_time, boolean can_update) {
		RemoteState<T> rs = null;
		if (rs_map.containsKey(addr)) {
			if (!can_update) {
				return false;
			}
			rs = rs_map.get(addr);
		}
		else {
			rs = new RemoteState<T>(addr);
			rs_map.put(addr, rs);
		}

		Coordinate r_coord = _r_coord.makeCopy();
		rs.assign(r_coord, r_error, curr_time);
		return true;
	}

	/**
	 * Notifies this <code>VivaldiClient</code> object that a host that
	 * supports Vivaldi has left the system. 
   * However, the state (i.e. short list of RTT values) is kept because
   * it will be useful if and when the node returns into the system
	 * @param addr
	 * the address of the departing host
	 * @return <code>true</code> if <code>addr</code> was a known node
	 */

  synchronized public boolean removeHost(T addr) {
    if (rs_map.containsKey(addr)) {
      return true;
    }
    return false;
  }

	/**
	 * Returns whether the given host has been registered with this
	 * <code>VivaldiClient</code> object.
	 * 
	 * @param addr
	 * the address to query as registered
	 * @return <code>true</code> if registered, <code>false</code> otherwise
	 */
  synchronized public boolean containsHost(T addr) {
		return rs_map.containsKey(addr);
	}

	/**
	 * Returns all hosts that support Vivaldi and have been registered with this
	 * <code>VivaldiClient</code> object. The returned set is backed by the
	 * true set of registered hosts, but cannot be modified.
	 * 
	 * @return the set of registered Vivaldi-supporting hosts
	 */
  synchronized public Set<T> getHosts() {
		return hosts;
	}

	/**
	 * This method is invoked when a new RTT sample is made to a host that
	 * supports Vivaldi. This method succeeds and returns <code>true</code>
	 * only if the host is already registered with this
	 * <code>VivaldiClient</code> object, and the RTT sample is valid.
	 * 
	 * @param addr
	 * the address of the host
	 * @param _r_coord
	 * the system-level coordinates of the remote host
	 * @param r_error
	 * the system-level error of the remote host
	 * @param sample_rtt
	 * the RTT sample to the remote host
	 * @param curr_time
	 * the current time, in milliseconds
	 * @param can_add
	 * <code>true</code> if this method can add a host not already present
	 * @return <code>true</code> if <code>addr</code> is registered and the
	 * sample is processed, <code>false</code> otherwise
	 */

  synchronized public boolean processSample(T addr, Coordinate _r_coord, double r_error,
			double sample_rtt, long sample_age, long curr_time, boolean can_add) {

    //if (debugCrawler) crawler_log.info("sample addr="+addr+" rtt="+sample_rtt);
    int id = getIdFromAddr (addr);
    if (debugCrawler && debugGood) crawler_log.info(id+" START");
    
    assert (_r_coord != sys_coord);
		assert (_r_coord != null);
		assert (sys_coord != null);
   
    if (!sys_coord.isCompatible(_r_coord)) {
      if (debugCrawler && debug) crawler_log.info("INVALID "+id+" s "+sample_rtt+" NOT_COMPAT "+_r_coord.getVersion());
      return false;
    }
    
		// There is a major problem with the coord.
		// However, if this is happening, it will probably
		// happen again and again.
		// Note that error is checked and fixed in updateError()
		if (!sys_coord.isValid() || Double.isNaN(error)) {
			//System.err.println("Warning: resetting Vivaldi coordinate");
      if (debugCrawler || SIMULATION) crawler_log.info(id + " RESET, USE_HEIGHT="+USE_HEIGHT);
			reset();
		}
   
    if (r_error <= 0. || r_error > MAX_ERROR || Double.isNaN(r_error) || !_r_coord.isValid()) {
      if (debugCrawler && debug) crawler_log.info(id+" BUSTED his coord is busted: r_error "+r_error+" r_coord "+_r_coord);  
      return false;
    }

		if (sample_rtt > OUTRAGEOUSLY_LARGE_RTT) {
			if (debug)
			  System.err.println("Warning: skipping huge RTT of "
						+ nf.format(sample_rtt) + " from " + addr);
      if (debugCrawler) crawler_log.info(id+ " HUGE "+sample_rtt);
			return false;
		}

		RemoteState<T> addr_rs = rs_map.get(addr);
		if (addr_rs == null) {
			if (!can_add) {
        if (debugCrawler) crawler_log.info(id+ " NO_ADD");  
				return false;
			}
			addHost(addr);
			addr_rs = rs_map.get(addr);
		}
		Coordinate r_coord = _r_coord.makeCopy();
    
		// add sample to history, then get smoothed rtt based on percentile
		addr_rs.addSample(sample_rtt, sample_age, r_coord, r_error, curr_time);

    // even if we aren't going to use him this time around, we remember this RTT
    
    if (sys_coord.atOrigin()) {
	sys_coord.bump();
    }
    
    boolean didUpdate = false;
    int sample_size = addr_rs.getSampleSize();
    double smoothed_rtt = addr_rs.getSample();
    
    if (addr_rs.isValid(curr_time)) {
      addNeighbor(addr_rs);
      // first update our error
      updateError(addr, r_coord, r_error, smoothed_rtt, sample_rtt, sample_age, sample_size, curr_time);
      // next, update our system-level coordinate
      updateSystemCoordinate(curr_time);  
      // last, try to update our application-level coordinate
      tryUpdateAppCoordinate(curr_time);
      didUpdate = true;

    } else {
      if (debugCrawler && debug) {
        String reason;
        if (addr_rs.getSampleSize() < RemoteState.MIN_SAMPLE_SIZE) {
          reason = "TOO_FEW";
        } else if (addr_rs.getSample() <= 0) {
          reason = "sample is "+addr_rs.getSample();
        } else if (addr_rs.getLastError() <= 0.) {
          reason = "error is "+addr_rs.getLastError();
        } else if (addr_rs.getLastUpdateTime() <= 0) {
          reason = "last update "+addr_rs.getLastUpdateTime();
        } else if (addr_rs.getLastCoordinate().atOrigin()) {
          reason = "AT_ORIGIN";
        } else {
          reason = "UNKNOWN";
        }
        crawler_log.info("INVALID "+id+" s "+sample_rtt+" ss "+smoothed_rtt+" c "+sample_size+
          " "+reason);

      } 
    }

    //System.out.println ("maint?");
    if (lastMaintenanceStamp < curr_time - MAINTENANCE_PERIOD) {
	performMaintenance (curr_time);
	lastMaintenanceStamp = curr_time;
    }
    
		return didUpdate;
	}
  
  private Map<T,Integer> addr2id = new HashMap<T,Integer>();
  private int idCounter = 0;

  // If remote nodes are already represented by ints, just use them
  // otherwise, translate into more easily read-able ID
  private int getIdFromAddr (T addr) {
	if ( debugCrawler || SIMULATION ){
		
	    if (addr instanceof Integer) {
	      return ((Integer)addr).intValue();
	    }
	      
	    if (!addr2id.containsKey(addr)) {
	      addr2id.put(addr,idCounter);  
	      idCounter++;
	    }
	    return addr2id.get(addr);
	}
	
	return(0);
  }
  
  protected void updateError(T addr, Coordinate r_coord, double r_error,
    double smoothed_rtt, double sample_rtt, long sample_age, int sample_size, long curr_time) {
		// get the coordinate distance
    double sys_distance = sys_coord.distanceTo(r_coord);
    double app_distance = 0;

    if (app_coord != null)
      app_distance = app_coord.distanceTo(r_coord);
    if (app_distance == 0. || sys_distance == 0.) {
      if (debugCrawler) crawler_log.info("bad distance sys "+sys_distance+" app "+app_distance);      
      return;
    }

    
    // get sample error in terms of coordinate distance and sample rtt
    // Note that smoothed_rtt must be greater than zero
    // or we wouldn't have entered this function

    // Note that app_sample_error is only giving us a limited amount of info
    // because his app coord is not going over the wire
    
    assert (smoothed_rtt > 0.); 
    double sys_sample_error = Math.abs(sys_distance - smoothed_rtt) / smoothed_rtt;
    double app_sample_error = Math.abs(app_distance - smoothed_rtt) / smoothed_rtt;


    if (debugCrawler) {
        int remote_id = getIdFromAddr (addr);
        String info = 
        //"lID "+local_addr+" rID "+
        "UPDATE "+remote_id+
        " re "+nf.format(sys_sample_error)+
        " rtt "+nf.format(smoothed_rtt)+
        " raw "+nf.format(sample_rtt)+
        " age "+sample_age+
        " dist "+nf.format(sys_distance)+
        " ssize "+sample_size+
        " lE "+nf.format(error)+
        " rE "+nf.format(r_error)+
        " rV "+r_coord.getVersion()+
        " lc "+sys_coord+
        " rc "+r_coord;
        
      crawler_log.info(info);
    }
    
    if (sys_sample_error < 0) {
      sys_sample_error = 0;
    }
    if (sys_sample_error > MAX_ERROR) {
      sys_sample_error = MAX_ERROR;
    }

    // EWMA on error
    double alpha = error / (error + r_error) * COORD_ERROR;
    error = (sys_sample_error*alpha)+((1-alpha)*error);
    
		if (keepStatistics) {
			running_sys_error.add(sys_sample_error);
			running_app_error.add(app_sample_error);
		}
	}

  

  protected boolean addNeighbor (RemoteState<T> guy) {
    boolean added = false;
    if (!neighbors.contains(guy)) {
      neighbors.add(guy);
      added = true;
    }
    if (neighbors.size() > MAX_NEIGHBORS) {
      neighbors.remove(0);
    }
    return added;
  }

    protected boolean removeNeighbor (RemoteState<T> guy) {
	if (neighbors.contains(guy)) {
	    neighbors.remove(guy);
	    return true;
	}
	return false;
    }

  synchronized public T getNeighborToPing (long curr_time) {
    final long NEIGHBOR_PING_EXPIRE_TIME = 10 * 60 * 1000; // 10 minutes
    long expire_time = curr_time - NEIGHBOR_PING_EXPIRE_TIME;
    List<RemoteState<T>> recentNeighbors = new ArrayList<RemoteState<T>>();
    for (RemoteState<T> neighbor : neighbors) {
      if (neighbor.getLastUpdateTime() > expire_time) {
        recentNeighbors.add(neighbor);
        //if (debugCrawler) {
          //int id = getIdFromAddr (neighbor.getAddress());
          //crawler_log.info ("considering "+id);
        //}
      }
    }
    if (recentNeighbors.size() > 0) {
      Collections.shuffle(recentNeighbors);
      RemoteState<T> neighbor = recentNeighbors.get(0);
      if (debugCrawler && debugGood) {
        int id = getIdFromAddr (neighbor.getAddress());
        crawler_log.info ("pinging neighbor "+id);
      }
      return recentNeighbors.get(0).getAddress();
    }
    return null;
  }

  
  protected void updateSystemCoordinate(long curr_time) {
    
    // figure out the oldest sample we are going to use
    // and recalculate the nearest neighbor
    // as a side effect
    long oldestSample = curr_time;
    double nearest_neighbor_distance = Double.MAX_VALUE;

    Collections.shuffle(neighbors);
    
    for (RemoteState<T> neighbor : neighbors) {
      double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
      if (distance < nearest_neighbor_distance) { 
        nearest_neighbor_distance = distance;
        nearest_neighbor = neighbor.getLastCoordinate();
      }
      if (oldestSample > neighbor.getLastUpdateTime()) {
        oldestSample = neighbor.getLastUpdateTime();
      }
    } 
    
    double sampleWeightSum = 0.;
    for (RemoteState<T> neighbor : neighbors) {
      double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
      sampleWeightSum += neighbor.getLastUpdateTime()-oldestSample;
    } 
    assert (sampleWeightSum >= 0.);
    
    Vec force = new Vec (sys_coord.getNumDimensions());

    for (RemoteState<T> neighbor : neighbors) {
      double distance = sys_coord.distanceTo(neighbor.getLastCoordinate());      
      while (distance == 0.) {
        sys_coord.bump();
        distance = sys_coord.distanceTo(neighbor.getLastCoordinate());
      }
      // cannot return null b/c distance is not 0
      Vec unitVector = sys_coord.getDirection(neighbor.getLastCoordinate());
      double latency = neighbor.getSample();
      double weight = error / (neighbor.getLastError()+error);
      if (weight == 0.) continue;
      
      // error of sample
      double sampleError = distance - latency;
      double sampleWeight = 1.;
      if (sampleWeightSum > 0) {
        sampleWeight = (neighbor.getLastUpdateTime()-oldestSample)/sampleWeightSum;
      }
      
      if (debugCrawler && debugGood) {
        int id = getIdFromAddr (neighbor.getAddress());
        crawler_log.info ("f "+id+ " age "+Math.round((curr_time-neighbor.getLastUpdateTime())/1000.)+
            " er "+sampleError+" sw "+sampleWeight+" comb " +
            (sampleError*sampleWeight));
      }
      
      unitVector.scale(sampleError*sampleWeight);
      force.add(unitVector);
    }
    
    if (USE_HEIGHT) {
      force.direction[force.direction.length-1] = -1.*force.direction[force.direction.length-1];
    }
    force.scale(COORD_CONTROL);

    if (debugCrawler && debugGood) {
      crawler_log.info ("t "+force.getLength()+" "+force);
    }
    
    // TODO add in gravity if necessary
    // Might not be with all of the churn
    // Check whether scaling should happen before or after addition of gravity

    /*
    if (GRAVITY_DIAMETER > 0) {
      // include "gravity" to keep coordinates centered on the origin
      Vec gravity = sys_coord.asVectorFromZero(true);
      if (gravity.getLength() > 0) {
        
        // scale gravity s.t. it increases polynomially with distance
        double force_of_gravity = Math.pow(gravity.getLength()
          / GRAVITY_DIAMETER, 2.);
        gravity.makeUnit();
        gravity.scale(force_of_gravity);
        
        // add to total force
        force.subtract(gravity);
        
        if (keepStatistics) {
          running_gravity.add(force_of_gravity);
        }
      }
    }
    */
    
    sys_coord.add(force);
    sys_coord.checkHeight ();
    double distance_delta = force.getLength();

    if (keepStatistics) {
      running_sys_dd.add(distance_delta);
      if (neighbors != null) {
        running_neighbors_used.add(neighbors.size());
      }

      if (time_of_last_sys_update > 0) {
        long since_last_sys_update = curr_time - time_of_last_sys_update;
        running_sys_update_frequency.add(since_last_sys_update);
      }
    }
    time_of_last_sys_update = curr_time;
    
  }

    /*
     * Periodically walk the entire rs_map and toss anybody who has expired.
     * If the map has grown beyond the preferred size (MAX_RS_MAP_SIZE),
     * shrink the max age that a guy can be before he gets kicked out.
    */

    protected void performMaintenance (long curr_time) {
	if (debugCrawler && debugGood) crawler_log.info ("performing maintenance");

	if (rs_map.size() > MAX_RS_MAP_SIZE) {
	    RS_EXPIRATION = (long)(.9 * RS_EXPIRATION);
	    if (debugCrawler && debugGood) crawler_log.info ("lowered RS_EXPIRATION to "+RS_EXPIRATION+ " size "+rs_map.size());
	}

	final long expirationStamp = curr_time - RS_EXPIRATION;
	Set<Map.Entry<T,RemoteState<T>>> states = rs_map.entrySet();

	for (Iterator<Map.Entry<T,RemoteState<T>>> stateIter = states.iterator(); stateIter.hasNext(); ) {
	    Map.Entry<T,RemoteState<T>> entry = stateIter.next();
	    if (entry.getValue().getLastUpdateTime() < expirationStamp) {
		if (debugCrawler && debugGood) crawler_log.info ("tossing "+entry.getValue().getAddress());
		removeNeighbor(entry.getValue());
		stateIter.remove();
	    }
	}
    }

	protected void tryUpdateAppCoordinate(long curr_time) {
		final double scale_factor = 1.0 / ((double) WINDOW_SIZE);

		// Make sure app coord always has a value.

		// calculate centroid of starting coordinates by averaging vectors

		if (start_coords.size() < WINDOW_SIZE) {
			Vec start_vec = new Vec(num_dims);
			start_coords.add(sys_coord);
			for (Iterator<Coordinate> i = start_coords.iterator(); i.hasNext();) {
				Coordinate next_coord = i.next();
				start_vec.add(next_coord.asVectorFromZero(false));
			}
			start_vec.scale(scale_factor);
			start_centroid = start_vec.asCoordinateFromZero(false);
		}

		current_coords.add(sys_coord);
		if (current_coords.size() > WINDOW_SIZE) {
			current_coords.remove(0);
		}

		// calculate centroid of current coordinates by averaging vectors
		Vec curr_vec = new Vec(num_dims);
		for (Iterator<Coordinate> i = current_coords.iterator(); i.hasNext();) {
			Coordinate next_coord = i.next();
			curr_vec.add(next_coord.asVectorFromZero(false));
		}
		curr_vec.scale(scale_factor);

		// create centroids
		Coordinate curr_centroid = curr_vec.asCoordinateFromZero(false);

		// get distances of centroids from nearest neighbor
		double start_dist = start_centroid.distanceTo(nearest_neighbor);
		double curr_dist = curr_centroid.distanceTo(nearest_neighbor);

		// fraction of space moved through, relative to distance to our NN
		double relative_diff = Math.abs((start_dist - curr_dist) / start_dist);

		if (keepStatistics) {
			running_relative_diff.add(relative_diff);
		}

		if (relative_diff > APP_UPDATE_THRESHOLD) {
			// exceed threshold, update application-level coordinate
			updated_app_coord_at_least_once = true;
			// clear coordinate windows
			start_coords.clear();
			current_coords.clear();
		}

		// This will keep updating the observers as we get rolling
		// until we've had one time when the coord windows differ

		boolean did_update = false;
		if (relative_diff > APP_UPDATE_THRESHOLD
				|| !updated_app_coord_at_least_once) {
			if (keepStatistics) {
				double app_dd = app_coord.distanceTo(curr_centroid);
				running_app_dd.add(app_dd);
			}
			app_coord = curr_centroid;
			did_update = true;

			// If we've gotten ourselves into a situation where the coord
			// very accurate, stop always updating the app.
			// Currently can only use this if keep track of statistics

			if (keepStatistics) {
				final double MIN_SYS_ERROR_SIZE = (RUNNING_STAT_HISTORY / 8.);
				if (!updated_app_coord_at_least_once
						&& running_sys_error.getSize() > MIN_SYS_ERROR_SIZE
						&& running_sys_error.getPercentile(.5) < 0.20) {
					updated_app_coord_at_least_once = true;
				}
			}

			// notify observers of new application-level coordinate
			for (Iterator<ApplicationObserver> i = obs_list.iterator(); i
					.hasNext();) {
				ApplicationObserver obs = i.next();
				obs.coordinatesUpdated(app_coord);
			}

			if (keepStatistics) {
				if (time_of_last_app_update > 0) {
					long since_last_app_update = curr_time
							- time_of_last_app_update;
					running_app_update_frequency.add(since_last_app_update);
				}
				time_of_last_app_update = curr_time;
			}
		}

		if (debug && debugCrawler) {
			crawler_log.info("app_coord update: done " + did_update
					+ " rolling " + updated_app_coord_at_least_once + " start "
					+ start_coords.size() + " current " + current_coords.size()
					+ " diff " + nf.format(relative_diff));
		}
	}

  public static void setRandomSeed (long seed) {
    random = new Random (seed);
  }

  public void startUp(DataInputStream is) throws IOException {
    
    if (debugCrawler) crawler_log.info("startUp");
        
    boolean valid = false;

    	// when starting up with no previously stored coord we get zero length input stream
    
    if ( is.available() > 0 ){    	
    
	    int	version = 1;
	    
	    	// migration 2501 when version added...
	
	    if ( is.available() != 25 ){
	    
	    	version = is.readInt();
	     }
	    
	    try {
	      sys_coord = new Coordinate (num_dims, is);
	      error = ((double) is.readFloat());
	      if (sys_coord.isValid() && !(Double.isNaN(error))) {
	         
	          valid = true;
	      }else{
	    	  //System.err.println("Invalid coordinate.");
	      }
	      
	    } catch (IOException ex) {
	    }
    }
    
    if (!valid) {
      //System.err.println("Error deserializing coordinate during startup.  Starting afresh.");
      sys_coord = new Coordinate (num_dims);
      error = MAX_ERROR;
    } else {
     // System.err.println("Deserialized coordinate OK during startup "+sys_coord+ " er "+error);
    }
    
  }
  
  synchronized public void shutDown(DataOutputStream os) throws IOException {

    if (debugCrawler) crawler_log.info("shutDown");
    
    os.writeInt( 1 );	// version
    
    // could also save a number of neighbors
    // but then when we come back into the system, 
    // it would be tricky to know how to treat them (they'd be old
    // so they would get much weight either).
    
    //System.err.println("Saving coordinates during shutdown "+sys_coord+" er="+error);
    
    sys_coord.toSerialized(os);
    os.writeFloat((float) error);
    
    // save app_coord also?
    
  }
  
}
