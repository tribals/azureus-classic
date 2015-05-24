package com.aelitis.azureus.vivaldi.ver2.stats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.text.NumberFormat;

import edu.harvard.syrah.nc.Coordinate;
import edu.harvard.syrah.nc.VivaldiClient;

public class V1Statistics implements VivaldiStatistics {
	public Coordinate sys_coord;
	public Coordinate app_coord;
  public float error, dist_to_nearest_neighbor, age_difference,
  valid_link_count, gravity;
	public float relative_rank_loss, norm_app_latency_penalty,
			relative_app_latency_penalty;
	public float sys_re50, sys_re95, app_re50, app_re95, sys_dd, app_dd;
	public float neighbors_used_per_update, running_relative_diff,
			running_sys_update_frequency, running_app_update_frequency;
	public boolean keepStatistics;

	final static protected NumberFormat nf = NumberFormat.getInstance();
	final static protected int NFDigits = 3;
	static boolean haveSetFormat = false;
  
  // semantic and other validity checks
  public boolean isValid () {
    if ((sys_coord == null) ||
        (app_coord == null) ||
        (error < 0) ||
        (dist_to_nearest_neighbor < 0))
      return false;
  
    if (keepStatistics &&
        ((sys_re50 > sys_re95) ||
          (app_re50 > app_re95) ||
          (running_sys_update_frequency > running_app_update_frequency) ||
          (age_difference < 0) ||
          (valid_link_count < 0)))
      return false;
    return true;
  }
  
	void checkFormat () {
	  if (!haveSetFormat) {
	    if (nf.getMaximumFractionDigits() > NFDigits) {
	      nf.setMaximumFractionDigits(NFDigits);
	    }
	    if (nf.getMinimumFractionDigits() > NFDigits) {
	      nf.setMinimumFractionDigits(NFDigits);
	    }
      nf.setGroupingUsed(false);
	  }
	  haveSetFormat = true;
	}

  public String toString () {
      checkFormat();
      String basic = new String 
	  ("sc "+sys_coord+
	   " ac "+app_coord+
	   " er "+nf.format(error)+
	   " nn "+nf.format(dist_to_nearest_neighbor));

      if (keepStatistics) {
	  String complete = new String
	      (" rrl "+nf.format(relative_rank_loss)+
	       " narl "+nf.format(norm_app_latency_penalty)+
	       " ralp "+nf.format(relative_app_latency_penalty)+
	       " age "+nf.format(age_difference)+
	       " gr "+nf.format(gravity)+
	       " vl "+nf.format(valid_link_count)+
	       " sys_re50 "+nf.format(sys_re50)+
	       " sys_re95 "+nf.format(sys_re95)+
	       " app_re50 "+nf.format(app_re50)+
	       " app_re95 "+nf.format(app_re95)+
	       " sys_dd "+nf.format(sys_dd)+
	       " app_dd "+nf.format(app_dd)+
	       " ne "+nf.format(neighbors_used_per_update)+
	       " rd "+nf.format(running_relative_diff)+
	       " sf "+nf.format(running_sys_update_frequency)+
	       " af "+nf.format(running_app_update_frequency));

	  return new String (basic+complete);
      }
      return basic;
  }
  
    //public V1Statistics(DataInputStream is) throws IOException {
	public V1Statistics(DataInputStream is) {
	    try {
		if (StatsSerializer.VER_01 == is.readByte()) {
		int num_dimensions = is.readInt();
		sys_coord = new Coordinate(num_dimensions, is);
		app_coord = new Coordinate(num_dimensions, is);
		error = is.readFloat();
		dist_to_nearest_neighbor = is.readFloat();

		keepStatistics = is.readBoolean();

		if (keepStatistics) {
			relative_rank_loss = is.readFloat();
			norm_app_latency_penalty = is.readFloat();
			relative_app_latency_penalty = is.readFloat();

			age_difference = is.readFloat();
			valid_link_count = is.readFloat();
			gravity = is.readFloat();
			sys_re50 = is.readFloat();
			sys_re95 = is.readFloat();
			app_re50 = is.readFloat();
			app_re95 = is.readFloat();
			sys_dd = is.readFloat();
			app_dd = is.readFloat();

			neighbors_used_per_update = is.readFloat();
			running_relative_diff = is.readFloat();
			running_sys_update_frequency = is.readFloat();
			running_app_update_frequency = is.readFloat();

		}
		}
	    } catch (IOException ex) {
		// flag that stats did not deserialize and should be ignored
		error = (float)-1.;
	    }
	}

	public static void toSerialized(DataOutputStream os, VivaldiClient<?> vc)
			throws IOException {
	        os.writeByte (StatsSerializer.VER_01);
	        os.writeInt (vc.getSystemCoords().getNumDimensions());
		vc.getSystemCoords().toSerialized (os);
		vc.getApplicationCoords().toSerialized (os);
		Hashtable<String, Double> stats = vc.getStatistics();
		os.writeFloat(new Float(stats.get("er")));
		os.writeFloat(new Float(stats.get("nn")));

		if (VivaldiClient.keepStatistics) {
			os.writeBoolean(true);
			os.writeFloat(new Float(stats.get("rrl")));
			os.writeFloat(new Float(stats.get("narl")));
			os.writeFloat(new Float(stats.get("ralp")));
			os.writeFloat(new Float(stats.get("age")));
      os.writeFloat(new Float(stats.get("vl")));
      os.writeFloat(new Float(stats.get("gr")));

			os.writeFloat(new Float(stats.get("sys_re50")));
			os.writeFloat(new Float(stats.get("sys_re95")));
			os.writeFloat(new Float(stats.get("app_re50")));
			os.writeFloat(new Float(stats.get("app_re95")));

			os.writeFloat(new Float(stats.get("sys_dd")));
			os.writeFloat(new Float(stats.get("app_dd")));

			os.writeFloat(new Float(stats.get("ne")));
			os.writeFloat(new Float(stats.get("rd")));
			os.writeFloat(new Float(stats.get("sf")));
			os.writeFloat(new Float(stats.get("af")));
		}
		else {
			os.writeBoolean(false);
		}
	}

	public byte getSerializedVersion() {
		return StatsSerializer.VER_01;
	}
  
  public long
  getDBValuesStored() {
    return 0;
  }
  
  public long
  getDBKeysBlocked() {
    return 0;
  }
  
    // Router
  
  public long
  getRouterNodes() {
    return 0;
  }
  
  public long
  getRouterLeaves() {
    return 0;
  }
  
  public long
  getRouterContacts() {
    return 0;
  }
  
  public long
  getRouterUptime() {
    return 0;
  }
  
  public int
  getRouterCount() {
    return 0;
  }
  
    // Transport
  
    // totals
  
  public long
  getTotalBytesReceived() {
    return 0;
  }
  
  public long
  getTotalBytesSent() {
    return 0;
  }
  
  public long
  getTotalPacketsReceived() {
    return 0;
  }
  
  public long
  getTotalPacketsSent() {
    return 0;
  }
  
  public long
  getTotalPingsReceived() {
    return 0;
  }
  
  public long
  getTotalFindNodesReceived() {
    return 0;
  }
  
  public long
  getTotalFindValuesReceived() {
    return 0;
  }
  
  public long
  getTotalStoresReceived() {
    return 0;
  }
  
  public long
  getTotalKeyBlocksReceived() {
    return 0;
  }
  
  public long
  getIncomingRequests() {
    return 0;
  }
  
    // averages
  
  public long
  getAverageBytesReceived() {
    return 0;
  }
  
  public long
  getAverageBytesSent() {
    return 0;
  }
  
  public long
  getAveragePacketsReceived() {
    return 0;
  }
  
  public long
  getAveragePacketsSent() {
    return 0;
  }
  
  public String
  getVersion() {
    return new String("");
  }
  
  public String
  getString() {
    return new String ("foo");
  }
  
  
}
