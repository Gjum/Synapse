package gjum.minecraft.civ.synapse.common;

import com.google.gson.annotations.Expose;

public class Pos implements Comparable<Pos> {
	public static final Pos ORIGIN = new Pos(0, 0, 0);
	@Expose
	public final int x;
	@Expose
	public final int y;
	@Expose
	public final int z;

	public Pos(int xIn, int yIn, int zIn) {
		this.x = xIn;
		this.y = yIn;
		this.z = zIn;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof Pos)) {
			return false;
		} else {
			Pos otherPos = (Pos) other;
			if (this.x != otherPos.x) {
				return false;
			} else if (this.y != otherPos.y) {
				return false;
			} else {
				return this.z == otherPos.z;
			}
		}
	}

	@Override
	public int hashCode() {
		return (this.y + this.z * 31) * 31 + this.x;
	}

	@Override
	public int compareTo(Pos p_compareTo_1_) {
		if (this.y == p_compareTo_1_.y) {
			return this.z == p_compareTo_1_.z ? this.x - p_compareTo_1_.x : this.z - p_compareTo_1_.z;
		} else {
			return this.y - p_compareTo_1_.y;
		}
	}

	public Pos crossProduct(Pos vec) {
		return new Pos(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
	}

	public Pos subtract(Pos other) {
		return new Pos(x - other.x, y - other.y, z - other.z);
	}

	public double distance(double xIn, double yIn, double zIn) {
		double d0 = (double) this.x - xIn;
		double d1 = (double) this.y - yIn;
		double d2 = (double) this.z - zIn;
		return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
	}

	public double distance(Pos to) {
		return this.distance(to.x, to.y, to.z);
	}

	public double distanceSq(double toX, double toY, double toZ) {
		double d0 = (double) this.x - toX;
		double d1 = (double) this.y - toY;
		double d2 = (double) this.z - toZ;
		return d0 * d0 + d1 * d1 + d2 * d2;
	}

	public double distanceSq(Pos to) {
		return this.distanceSq(to.x, to.y, to.z);
	}

	@Override
	public String toString() {
		return "[" + x + " " + y + " " + z + "]";
	}
}
