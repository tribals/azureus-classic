package com.aelitis.azureus.vivaldi.ver2;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.harvard.syrah.nc.Coordinate;

/*
 * The DHTNetworkPosition implementation for the local node.
 */
class LocalPosition extends SyrahPosition {
	private final VivaldiV2PositionProvider v2_provider;
	
	protected LocalPosition(VivaldiV2PositionProvider _v2_provider) {
		v2_provider = _v2_provider;
	}

	public Coordinate getCoords() {
		return v2_provider.getCoords();
	}

	public Coordinate getStableCoords() {
		return v2_provider.getStableCoords();
	}

	public float getError() {
		return (float) v2_provider.getError();
	}

	public long getAge () {
		return v2_provider.getAge();
	}
  
	public float estimateRTT(DHTNetworkPosition other) {
		SyrahPosition sp = (SyrahPosition) other;
    if (Float.isNaN(getError()) || Float.isNaN(sp.getError())) return Float.NaN;
		return (float)getCoords().distanceToNonOriginCoord(sp.getCoords());
	}

	public void update(byte[] other_id, DHTNetworkPosition other, float rtt) {
		v2_provider.update(this, new IDWrapper(other_id), (SyrahPosition) other, rtt);
	}
}
