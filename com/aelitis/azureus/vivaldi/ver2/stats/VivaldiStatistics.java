package com.aelitis.azureus.vivaldi.ver2.stats;

import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;

/*
 * A marker interface for any Vivaldi statistics.
 */
public interface VivaldiStatistics {
	/*
	 * Returns the version that the underlying implemnetation serializes and deserializes.
	 */
	public byte getSerializedVersion();
  
  public boolean isValid();
  
}
