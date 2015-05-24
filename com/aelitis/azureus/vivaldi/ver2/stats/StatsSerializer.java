package com.aelitis.azureus.vivaldi.ver2.stats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.harvard.syrah.nc.VivaldiClient;

public interface StatsSerializer {
	public static final byte VER_01 = 0x01;

	/*
	 * Returns the version that the underlying implemnetation serializes and
	 * deserializes.
	 */
	public byte getSerializedVersion();

	/*
	 * Writes the statistics to the given stream. Returns true if written
	 * successfully.
	 */
	public void toSerialized(DataOutputStream os, VivaldiClient vc) throws IOException;

	/*
	 * Reads the statistics from the given stream.
	 */
	public VivaldiStatistics fromSerialized(DataInputStream is) throws IOException;
}
