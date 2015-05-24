package com.aelitis.azureus.vivaldi.ver2;

import java.util.Arrays;

/*
 * Wrapper for node identifiers so they implement equals, hashCode, and compareTo.
 */
public class IDWrapper implements Comparable<IDWrapper> {
	/**
	 * Translation from a number in the interval [0, 16) to its hexadecimal
	 * representation.
	 */
	public static final char[] hex_char = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private final byte[] raw_id;
	
	public IDWrapper(byte[] _raw_id) {
		raw_id = _raw_id;
	}
	
  public byte[] getRawId () {
    return raw_id;
  }
  
	public boolean equals(Object obj) {
		if (obj instanceof IDWrapper) {
			IDWrapper other = (IDWrapper) obj;
			return Arrays.equals(raw_id, other.raw_id);
		}
		return false;
	}
	
	public int hashCode() {
		return Arrays.hashCode(raw_id);
	}

	public int compareTo(IDWrapper other) {
		if (this == other) {
			return 0;
		}

		// assume all identifiers are of the same length
		for (int i = 0; i < raw_id.length; ++i) {
			if (raw_id[i] != other.raw_id[i]) {
				return ((raw_id[i] & 0xFF) < (other.raw_id[i] & 0xFF)) ? -1 : 1;
			}
		}
		return 0;
	}
	
	public String toString() {
		StringBuffer sbuf = new StringBuffer(64);
		
		sbuf.append("0x");
		for (int i = 0; i < raw_id.length; ++i) {
			sbuf.append(hex_char[(raw_id[i] >> 4) & 0xF]);
			sbuf.append(hex_char[raw_id[i] & 0xF]);
		}
		return sbuf.toString();
	}
}
