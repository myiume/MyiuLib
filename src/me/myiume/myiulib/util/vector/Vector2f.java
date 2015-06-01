package me.myiume.myiulib.util.vector;

import com.google.common.base.Objects;

/**
 * Represents a two-dimensional point.
 *
 * @since 0.4.0
 */
public class Vector2f {

	private float x;
	private float y;

	/**
	 * Creates a new Vector2f.
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @since 0.4.0
	 */
	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
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

	@Override
	public boolean equals(Object otherVector) {
		return otherVector instanceof Vector2f &&
				this.x == ((Vector2f)otherVector).getX() &&
				this.y == ((Vector2f)otherVector).getY();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(x, y);
	}

}
