package edu.harvard.syrah.nc;

/*
 * NCLib - a network coordinate library 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * A vector in the Euclidian space.
 */
public class Vec {
  
	final protected static int CLASS_HASH = Vec.class.hashCode();
	
	final protected double[] direction;
	final protected int num_dims;
  
  /*
	public static Vec add(Vec lhs, Vec rhs) {
		Vec sum = new Vec(lhs);
		sum.add(rhs);
		return sum;
	}
	
	public static Vec subtract(Vec lhs, Vec rhs) {
		Vec diff = new Vec(lhs);
		diff.subtract(rhs);
		return diff;
	}
	*/
  
	public static Vec scale(Vec lhs, double k) {
		Vec scaled = new Vec(lhs);
		scaled.scale(k);
		return scaled;
	}
	
  /*
	public static Vec makeUnit(Vec v) {
		Vec unit = new Vec(v);
		unit.makeUnit();
		return unit;
	}
	*/
  
  /*
  public static Vec makeRandomUnit(int num_dims) {
    final Vec v = makeRandom (num_dims, 1.); 
    v.makeUnit();
    return v;
  }
  */

  public static Vec makeRandom(int num_dims, double axisLength) {
    final Vec v = new Vec(num_dims);
    for (int i = 0; i < num_dims; ++i) {
      double length = VivaldiClient.random.nextDouble() * axisLength;
      if ( (!VivaldiClient.USE_HEIGHT || i < num_dims-1) &&
        (VivaldiClient.random.nextBoolean())) {
        length *= -1.;
      }
      v.direction[i] = length;
    }
    return v;
  }
  
  
	public Vec(int _num_dims) {
		direction = new double[_num_dims];
		if (VivaldiClient.USE_HEIGHT) _num_dims--;
    num_dims = _num_dims;
  }
	
	public Vec(Vec v) {
		this(v.direction, true);
	}

	// TODO isn't this missing the height coordinate?
	public Vec(double[] init_dir, boolean make_copy) {
		if (make_copy) {
			final int num_dims = init_dir.length;
			direction = new double[num_dims];
			System.arraycopy(init_dir, 0, direction, 0, num_dims);
		}
		else {
			direction = init_dir;
		}
    int _num_dims = init_dir.length;
    if (VivaldiClient.USE_HEIGHT) _num_dims--;
    num_dims = _num_dims;
	}
	
	public int getNumDimensions() {
	  // keep num_dimensions internal
    return direction.length;
	}
	
	public double[] getComponents() {
		final double[] dir_copy = new double[direction.length];
		System.arraycopy(direction, 0, dir_copy, 0, direction.length);
		return dir_copy;
	}
	
	//Same regardless of using height
	public void add(Vec v) {
		for (int i = 0; i < direction.length; ++i) {
			direction[i] += v.direction[i];
		}
	}
	  
  /*
	public void subtract(Vec v) {
		for (int i = 0; i < num_dims; ++i) {
			direction[i] -= v.direction[i];
		}
    if (VivaldiClient.USE_HEIGHT) {
      direction[direction.length-1] += v.direction[direction.length-1];
    }
	}
	*/
  
	public void scale(double k) {
		for (int i = 0; i < direction.length; ++i) {
			direction[i] *= k;
		}
	}
	
	public boolean isUnit() {
		return (getLength() == 1.0);
	}
	
  public double getLength() {
    double sum = getPlanarLength();
    if (VivaldiClient.USE_HEIGHT)
      sum += direction[direction.length-1];
    return sum;
  }

  double getPlanarLength() {
    double sum = 0;
    for (int i = 0; i < num_dims; ++i) {
      sum += (direction[i] * direction[i]);
    }
    return Math.sqrt(sum);
  }
  
	public void makeUnit() {
		final double length = getLength();
		if (length != 1.0) {
      scale (1./length);
    }
	}
  
	public Coordinate asCoordinateFromZero(boolean make_copy) {
		return new Coordinate(direction, make_copy);
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Vec) {
			Vec v = (Vec) obj;
			final int num_dims = direction.length;
			for (int i = 0; i < num_dims; ++i) {
				if (direction[i] != v.direction[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		final int num_dims = direction.length;
		int hc = CLASS_HASH;
		for (int i = 0; i < num_dims; ++i) {
			hc ^= new Double(direction[i]).hashCode();
		}
		return hc;
	}
	
	public String toString() {
		final StringBuffer sbuf = new StringBuffer(1024);
		sbuf.append("[");
		final int num_dims = direction.length;
		for (int i = 0; true; ) {
      if (i == num_dims-1 && VivaldiClient.USE_HEIGHT) {
        sbuf.append('h');  
      }
			sbuf.append(VivaldiClient.nf.format(direction[i]));
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
}
