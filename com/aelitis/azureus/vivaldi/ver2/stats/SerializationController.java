package com.aelitis.azureus.vivaldi.ver2.stats;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

import edu.harvard.syrah.nc.VivaldiClient;

public class SerializationController {
	protected final TreeMap<Byte, StatsSerializer> serializers;
	protected final Set<Byte> versions_ro;

	public SerializationController() {
		serializers = new TreeMap<Byte, StatsSerializer>();
		versions_ro = Collections.unmodifiableSet(serializers.keySet());
		
		// add version 1
		StatsSerializer v1_serializer = V1Serializer.getInstance();
		serializers.put(v1_serializer.getSerializedVersion(), v1_serializer);
	}

	public boolean addSerializer(StatsSerializer new_serializer) {
		final byte new_version = new_serializer.getSerializedVersion();
		if (serializers.containsKey(new_version)) {
			return false;
		}

		serializers.put(new_version, new_serializer);
		return true;
	}

	public StatsSerializer removeSerializer(byte version) {
		return serializers.remove(version);
	}
	
	public boolean contains(byte version) {
		return serializers.containsKey(version);
	}

	public Set<Byte> getVersions() {
		return versions_ro;
	}

	/**
	 * Instructs the controller to serialize the given statistics to the output
	 * stream using the provided versioning.
	 * 
	 * @param version
	 * the version of serialization to use
	 * @param os
	 * the stream to write the serialized form to
	 * @param vc
	 * the client containing the statistics to serialize
	 * @return <code>true</code> if the statistics were serialized, or
	 * <code>false</code> if a serializer for the version could not be found
	 * @throws IOException
	 * if a serializer was found but an error occured during serialization
	 */
	public boolean toSerialized(byte version, DataOutputStream os,
			VivaldiClient vc) throws IOException {
		StatsSerializer serializer = serializers.get(version);
		if (serializer == null) {
			return false;
		}

		// write version
		os.writeByte(version);
		// then write stats
		serializer.toSerialized(os, vc);
		return true;
	}

	/**
	 * Instructs the controller to deserialize from the input stream.
	 * 
	 * @param is
	 * the stream to read the serialized form from
	 * @return the deserialized statistics, or <code>null</code> if no
	 * deserializer for the read version could not be found
	 * @throws IOException
	 * if a deserializer was found but an error occured during deserialization
	 */
	public VivaldiStatistics fromSerialized(DataInputStream is)
			throws IOException {
		// read version
		byte version = is.readByte();
		StatsSerializer serializer = serializers.get(version);
		if (serializer == null) {
			return null;
		}

		// then read stats
		return serializer.fromSerialized(is);
	}
}
