package com.aelitis.azureus.vivaldi.ver2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.harvard.syrah.nc.Coordinate;

/*
 * The DHTNetworkPosition implementation for deserialized coordinates.
 */
class DeserializedPosition extends SyrahPosition {
	private Coordinate coords;
	private float error;
	private long age;
  
	protected DeserializedPosition(DataInputStream dis) throws IOException {
		coords = new Coordinate(VivaldiV2PositionProvider.NUM_DIMS, dis);
		error = dis.readFloat();
		age = dis.readLong();
  }

	public Coordinate getCoords() {
		return coords;
	}

	public Coordinate getStableCoords() {
		return coords;
	}

  public float getError() {
    return error;
  }

  public long getAge() {
    return age;
  }
  
	public float estimateRTT(DHTNetworkPosition other) {
		SyrahPosition sp = (SyrahPosition) other;
    if (Float.isNaN(getError()) || Float.isNaN(sp.getError())) return Float.NaN;
		return (float) coords.distanceToNonOriginCoord(sp.getCoords());
	}

	public void update(byte[] other_id, DHTNetworkPosition other, float rtt) {
		SyrahPosition sp = (SyrahPosition) other;
		coords = sp.getCoords();
		error = sp.getError();
    age = sp.getAge();
	}

	public void serialise(DataOutputStream dos) throws IOException {
		coords.toSerialized(dos);
		dos.writeFloat(error);
		dos.writeLong(age);
  }
}
