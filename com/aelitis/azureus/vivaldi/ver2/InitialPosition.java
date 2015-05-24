package com.aelitis.azureus.vivaldi.ver2;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.harvard.syrah.nc.Coordinate;

/*
 * The initial Vivaldi position of a remote peer.
 */
class InitialPosition extends SyrahPosition {
	final static Coordinate zero_coords = new Coordinate(VivaldiV2PositionProvider.NUM_DIMS);
	final static float error = 1.0f; 
	
	public InitialPosition() {
	}

	public Coordinate getCoords() {
		return zero_coords;
	}

	public Coordinate getStableCoords() {
		return zero_coords;
	}

	public float getError() {
		return 1.0f;
	}

  public long getAge () {
    return 0L;
  }
  
	public float estimateRTT(DHTNetworkPosition other) {
		// log error
		VivaldiV2PositionProvider.doLog("method estimateRTT invoked on initial position");
		
		SyrahPosition sp = (SyrahPosition) other;
		return (float) zero_coords.distanceTo(sp.getCoords());
	}

	public void update(byte[] other_id, DHTNetworkPosition other, float rtt) {
		// log error
		VivaldiV2PositionProvider.doLog("method update invoked on initial position");
	}
}
