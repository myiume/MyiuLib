package me.myiume.myiulib.util.vector;

import com.google.common.base.Objects;

/**
 * Represents a three-dimensional point.
 *
 * @since 0.4.0
 */
public class Vector3f {

	private float x;
	private float y;
	private float z;

	/**
	 * Creates a new Vector3f.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param z the z-coordinate
	 * @since 0.4.0
	 */
	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Gets the x-coordinate of this vector.
	 *
	 * @return the x-coordinate
	 * @since 0.4.0
	 */
	public float getX() {
		return this.x;
	}

	/**
	 * Gets the y-coordinate of this vector.
	 *
	 * @return the y-coordinate
	 * @since 0.4.0
	 */
	public float getY() {
		return this.y;
	}

	/**
	 * Gets the z-coordinate of this vector.
	 *
	 * @return the z-coordinate
	 * @since 0.4.0
	 */
	public float getZ() {
		return this.z;
	}

	/**
	 * Sets the x-coordinate of this vector.
	 *
	 * @param x he x-coordinate
	 * @since 0.4.0
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * Sets the y-coordinate of this vector.
	 *
	 * @param y the y-coordinate
	 * @since 0.4.0
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * Sets the z-coordinate of this vector.
	 *
	 * @param z he z-coordinate
	 * @since 0.4.0
	 */
	public void setZ(float z) {
		this.z = z;
	}

	@Override
	public boolean equals(Object otherVector) {
		return otherVector instanceof Vector3f &&
				       this.x == ((Vector3f)otherVector).getX() &&
				       this.y == ((Vector3f)otherVector).getY() &&
				       this.z == ((Vector3f)otherVector).getZ();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(x, y, z);
	}

}
