package com.aelitis.azureus.vivaldi.ver2.stats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import edu.harvard.syrah.nc.VivaldiClient;

public class V1Serializer implements StatsSerializer {
	private static V1Serializer instance;

	public static synchronized V1Serializer getInstance() {
		if (instance == null) {
			instance = new V1Serializer();
		}
		return instance;
	}

	private V1Serializer() {
	}

	public byte getSerializedVersion() {
		return StatsSerializer.VER_01;
	}

	public void toSerialized(DataOutputStream os, VivaldiClient vc) throws IOException {
		V1Statistics.toSerialized(os, vc);
	}

	public VivaldiStatistics fromSerialized(DataInputStream is) throws IOException {
		return new V1Statistics(is);
	}
}
