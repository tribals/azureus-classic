package com.aelitis.azureus.vivaldi.ver2;

import java.io.DataOutputStream;
import java.io.IOException;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

import edu.harvard.syrah.nc.Coordinate;

/*
 * Abstract DHTNetworkPosition implementation for local and remote nodes.
 */
abstract class SyrahPosition implements DHTNetworkPosition {
	protected SyrahPosition() {
	}

	public abstract Coordinate getStableCoords();	

	public abstract Coordinate getCoords();
	
  public abstract float getError();
  
  public abstract long getAge();
	
	public byte getPositionType() {
		return DHTNetworkPosition.POSITION_TYPE_VIVALDI_V2;
	}

	public int getSerialisedSize() {
		// 1 byte version + count dimensions and error, each as a float + age (which is a long)
		return 1 + ( 4 * (VivaldiV2PositionProvider.NUM_DIMS + 1)) + (1 * 8);
	}
	
	public void serialise(DataOutputStream dos) throws IOException {
		getCoords().toSerialized(dos);
		dos.writeFloat(getError());
    dos.writeLong(getAge());
	}
  
	public boolean
	isValid()
	{
		return( (!Float.isNaN( getError())) && getCoords().isValid());
	}
	
  public String toString () {
    return new String (getCoords()+",er="+getError()+",age="+getAge());
  }
  
}
