package com.aelitis.azureus.vivaldi.ver2;

import java.io.DataInputStream;
import java.io.IOException;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.harvard.syrah.nc.Coordinate;

/*
 * The DHTNetworkPosition implementation for a remote node.
 */
class RemotePosition extends SyrahPosition {
	private Coordinate coords;
	private float error;
	private long age;
  
	protected RemotePosition(DataInputStream dis) throws IOException {
		coords = new Coordinate(VivaldiV2PositionProvider.NUM_DIMS, dis);
		error = dis.readFloat();
		age = dis.readLong();
	}

	public Coordinate getCoords() {
		return coords;
	}

       // Because we are currently sending only one coordinate over the wire,
       // only got one to reply with.
       // In future might want to send both and use stable coord for routing table
	public Coordinate getStableCoords() {
		return coords;
	}


	public float getError() {
		return error;
	}

  public long getAge () {
    return age;
  }
  
	public float estimateRTT(DHTNetworkPosition other) {
		SyrahPosition sp = (SyrahPosition) other;
    if (Float.isNaN(error) || Float.isNaN(sp.getError())) return Float.NaN;
    return (float) coords.distanceToNonOriginCoord(sp.getCoords());
	}

	public void update(byte[] other_id, DHTNetworkPosition other, float rtt) {
		// log error
		VivaldiV2PositionProvider.doLog("method update invoked on a remote position");
	}
}
