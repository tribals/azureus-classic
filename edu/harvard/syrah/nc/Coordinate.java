package edu.harvard.syrah.nc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.Vector;

/*
 * NCLib - a network coordinate library
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details (
 * see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/**
 * A coordinate in the Euclidian space.
 * 
 * @author Michael Parker, Jonathan Ledlie
 */
public class Coordinate {

	final static protected int CLASS_HASH = Coordinate.class.hashCode();

	final protected double[] coords;
  final protected byte version;
  final protected int num_dims;

  public static double MIN_COORD = 0.1;
  
    public byte getVersion () {
	return version;
    }
  
	/**
	 * Creates a copy of this <code>Coordinate</code> object, such that
	 * updates in this coordinate are not reflected in the returned object.
	 * 
	 * @return a copy of these coordinates
	 */
	public Coordinate makeCopy() {
		return new Coordinate(coords, true);
	}
	
	/**
	 * Creates a new coordinate having a position at the origin.
	 * 
	 * @param num_dimensions
	 * the number of coordinate dimensions
	 */
	public Coordinate(int num_dimensions) {
		coords = new double[num_dimensions];
    version = VivaldiClient.CURRENT_VERSION;
    if (VivaldiClient.USE_HEIGHT) num_dimensions--;
    num_dims = num_dimensions;
	}

	/**
	 * 
	 * @param num_dimensions
	 * @param dis
	 * @throws IOException
	 */


	public Coordinate(int num_dimensions, DataInputStream dis) throws IOException {  
	  coords = new double[num_dimensions];
	  version = dis.readByte();
	  for (int i = 0; i < num_dimensions; i++) {
	    coords[i] = ((double) dis.readFloat());
	  }
    if (VivaldiClient.USE_HEIGHT) num_dimensions--;
    num_dims = num_dimensions;
    
	}

  public void toSerialized(DataOutputStream dos) throws IOException {
    final int num_dims = coords.length;
    dos.writeByte (version);
    for (int i = 0; i < num_dims; ++i) {
      // when writing, cast to float
      dos.writeFloat((float) coords[i]);
    }
    //if (VivaldiClient.USE_HEIGHT) dos.writeFloat((float) coords[num_dims]);
  }
  
	protected Coordinate(Coordinate c) {
		this(c.coords, true);
	}

	/**
	 * Creates a new coordinate having a position specified by the array
	 * <code>init_coords</code>. The number of dimensions is this equal to
	 * the array length.
	 * 
	 * @param init_pos
	 * the position for this coordinate
	 * @param make_copy
	 * whether a copy of the array should be made
	 */
	protected Coordinate(double[] init_pos, boolean make_copy) {
    int _num_dims = init_pos.length;
    if (make_copy) {
			coords = new double[_num_dims];
			System.arraycopy(init_pos, 0, coords, 0, _num_dims);
		}
		else {
			coords = init_pos;
		}
    version = VivaldiClient.CURRENT_VERSION;
    if (VivaldiClient.USE_HEIGHT) _num_dims--;
    num_dims = _num_dims;
	}

	/**
	 * Creates a new coordinate having a position specified by the array
	 * <code>init_coords</code>. The number of dimensions is this equal to
	 * the array length.
	 * 
	 * @param init_pos
	 * the position for this coordinate
	 */
	public Coordinate(float[] init_pos) {
		int _num_dims = init_pos.length;
		coords = new double[_num_dims];
		for (int i = 0; i < _num_dims; ++i) {
			coords[i] = init_pos[i];
		}
    version = VivaldiClient.CURRENT_VERSION;
    if (VivaldiClient.USE_HEIGHT) _num_dims--;
    num_dims = _num_dims;
	}

  public boolean isCompatible (Coordinate _other) {
    if (version == _other.version)
      return true;
    return false;
  }
  
	public void bump () {
	  for (int i = 0; i < coords.length; ++i) {
	    if (Math.abs(coords[i]) < MIN_COORD) {
        double length = VivaldiClient.random.nextDouble()+MIN_COORD;
	      // don't set height to be negative, if we are using it
        if ( (!VivaldiClient.USE_HEIGHT || i < coords.length-1) &&
          (VivaldiClient.random.nextBoolean())) {
          length *= -1.;
	      }
	      coords[i] += length;
	    }
	  }
	}




	/**
	 * Returns the number of dimensions this coordinate has.
	 * 
	 * @return the number of coordinate dimensions.
	 */
	public int getNumDimensions() {
		return coords.length;
	}

	/**
	 * Returns the Euclidian distance to the given coordinate parameter.
	 * 
	 * @param c
	 * the coordinate to find the Euclidian distance to
	 * @return the distance to parameter <code>c</code>
	 */
  public double distanceToNonOriginCoord(Coordinate c) {
    if (atOrigin() || c.atOrigin()) return Double.NaN;
    return distanceTo(c);
  }
  
  public double distanceTo(Coordinate c) {
    //System.err.println("us="+this.toString()+" them="+c.toString());
    if (!isCompatible(c)) return Double.NaN;
    if (!isValid() || !c.isValid()) return Double.NaN;
    
    // used for debugging so we can call distanceTo on something null
    assert ((VivaldiClient.USE_HEIGHT && num_dims == coords.length-1) ||
            (!VivaldiClient.USE_HEIGHT && num_dims == coords.length));

    if (c == null) {
      assert (!VivaldiClient.SIMULATION);
      return -1.;
    }
    double sum = 0.0;
    for (int i = 0; i < num_dims; ++i) {
      final double abs_dist = coords[i] - c.coords[i];
      sum += (abs_dist * abs_dist);
    }
    sum = Math.sqrt(sum);
    if (VivaldiClient.USE_HEIGHT && sum > 0) {
      sum = sum + coords[coords.length-1] + c.coords[coords.length-1];
    }
    return sum;
  }

	// Same regardless of using height
	public void add(Vec v) {
		final int num_dims = coords.length;
		for (int i = 0; i < num_dims; ++i) {
			coords[i] += v.direction[i];
		}
	}

	/*
	 * protected method, do not expose to client
	 */
  protected Vec getDirection(Coordinate c) {
    double length = distanceTo(c);
    if (length == 0) return null;
    final Vec new_vec = new Vec(coords.length);
    for (int i = 0; i < num_dims; ++i) {
      new_vec.direction[i] = (c.coords[i] - coords[i])/length;
    }
    if (VivaldiClient.USE_HEIGHT) {
      new_vec.direction[coords.length-1] = 
        (c.coords[coords.length-1] + coords[coords.length-1])/length;
    }
    return new_vec;
  }

  protected boolean assign (Coordinate c) {
    if (coords.length != c.coords.length) return false;
    for (int i = 0; i < coords.length; ++i) {
      coords[i] = c.coords[i];
    }
    return true;
  }
  
  public void checkHeight () {
    if (!VivaldiClient.USE_HEIGHT) return;
    if (coords[coords.length-1] <= MIN_COORD) {
      coords[coords.length-1] = VivaldiClient.random.nextDouble()+MIN_COORD;
    }
  }
  
  public boolean atOrigin () {
    for (int i = 0; i < coords.length; i++) {
      if (coords[i] != 0)
        return false;
    }
    return true;
  }
  
	public Vec asVectorFromZero(boolean make_copy) {
		return new Vec(coords, make_copy);
	}

	public boolean isValid() {
	  final double NEG_MAX_DIST_FROM_ORIGIN = -1 * VivaldiClient.MAX_DIST_FROM_ORIGIN;
		for (int i = 0; i < coords.length; ++i) {
			if (Double.isNaN(coords[i])) {
        if (VivaldiClient.SIMULATION) System.err.println("coord isNaN i="+i);
				return false;
			}
			if (coords[i] > VivaldiClient.MAX_DIST_FROM_ORIGIN ||
			    coords[i] < NEG_MAX_DIST_FROM_ORIGIN) {
        if (VivaldiClient.SIMULATION) System.err.println("coord too far from origin i="+i+" coord="+coords[i]);
			    return false;
			}
		}
    if (VivaldiClient.USE_HEIGHT && coords[coords.length-1] < 0) return false;
		return true;
	}

	protected void reset() {
		for (int i = 0; i < coords.length; ++i) {
			coords[i] = 0.;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof Coordinate) {
			Coordinate c = (Coordinate) obj;
			final int num_dims = coords.length;
			for (int i = 0; i < num_dims; ++i) {
				if (coords[i] != c.coords[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public int hashCode() {
		final int num_dims = coords.length;
		int hc = CLASS_HASH;
		for (int i = 0; i < num_dims; ++i) {
			hc ^= new Double(coords[i]).hashCode();
		}
		return hc;
	}

	public String toString() {
		final StringBuffer sbuf = new StringBuffer(1024);
		sbuf.append("[");

		final int num_dims = coords.length;
		for (int i = 0; true;) {
      if (i == num_dims-1 && VivaldiClient.USE_HEIGHT) {
        sbuf.append('h');  
      }
      sbuf.append(VivaldiClient.nf.format(coords[i]));
      if (++i < num_dims) {
				sbuf.append(",");
			}
			else {
				break;
			}
		}
		sbuf.append("]");
		return sbuf.toString();
	}

	public String toStringAsVector() {
		final StringBuffer sbuf = new StringBuffer(1024);

		for (int i = 0; i < coords.length; i++) {
      if (i == coords.length - 1 && VivaldiClient.USE_HEIGHT)
        sbuf.append('h');
      sbuf.append(VivaldiClient.nf.format(coords[i]));
      if (i != coords.length - 1)
				sbuf.append(" ");
		}
		return sbuf.toString();
	}

}
