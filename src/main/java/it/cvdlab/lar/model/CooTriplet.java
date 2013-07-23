package it.cvdlab.lar.model;

final class CooTriplet implements Comparable<CooTriplet> {
	private int x;
	private int y;
	private float val;

	public CooTriplet(int x, int y, float val) {
		super();
		this.x = x;
		this.y = y;
		this.val = val;
	}	
	
	@Override
	public int compareTo(CooTriplet o) {
		if (this.getX() != o.getX()) {
			return this.getX() - o.getX();
		}
		
		return this.getY() - o.getY();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(val);
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CooTriplet other = (CooTriplet) obj;
		if (Float.floatToIntBits(val) != Float.floatToIntBits(other.val))
			return false;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}


	public int getX() {
		return x;
	}


	public int getY() {
		return y;
	}


	public float getVal() {
		return val;
	}
}
