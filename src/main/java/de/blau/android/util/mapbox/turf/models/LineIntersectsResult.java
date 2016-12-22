package de.blau.android.util.mapbox.turf.models;

/**
 * if the lines intersect, the result contains the x and y of the intersection (treating the lines
 * as infinite) and booleans for whether line segment 1 or line segment 2 contain the point
 *
 * @see <a href="http://jsfiddle.net/justin_c_rounds/Gd2S2/light/">Good example of how this class works written in JavaScript</a>
 * @since 1.2.0
 */
public class LineIntersectsResult {

  private Double x;
  private Double y;
  private boolean onLine1 = false;
  private boolean onLine2 = false;

  /**
   * Public constructor
   *
   * @since 1.2.0
   */
  public LineIntersectsResult() {
  }

  /**
   * If the lines intersect, use this method to get the intersecting points {@code X} value
   *
   * @return The {@code X} value where the lines intersect.
   * @since 1.2.0
   */
  public Double getX() {
    return x;
  }

  /**
   * Set the points {@code X} value where the two lines should intersect.
   *
   * @param x Double value representing the intersecting {@code X} coordinate.
   * @since 1.2.0
   */
  public void setX(Double x) {
    this.x = x;
  }

  /**
   * If the lines intersect, use this method to get the intersecting points {@code Y} value
   *
   * @return The {@code Y} value where the lines intersect.
   * @since 1.2.0
   */
  public Double getY() {
    return y;
  }

  /**
   * Set the points {@code Y} value where the two lines should intersect.
   *
   * @param y Double value representing the intersecting {@code Y} coordinate.
   * @since 1.2.0
   */
  public void setY(Double y) {
    this.y = y;
  }

  /**
   * Determine if the intersecting point lands on line 1 or not.
   *
   * @return True if the intersecting point is located on line 1, otherwise false.
   * @since 1.2.0
   */
  public boolean isOnLine1() {
    return onLine1;
  }

  /**
   * Set whether the intersecting point should land on line 1.
   *
   * @param onLine1 True if you want the intersecting point to lie on line 1, otherwise pass in
   *                false.
   * @since 1.2.0
   */
  public void setOnLine1(boolean onLine1) {
    this.onLine1 = onLine1;
  }

  /**
   * Determine if the intersecting point lands on line 2 or not.
   *
   * @return True if the intersecting point is located on line 2, otherwise false.
   * @since 1.2.0
   */
  public boolean isOnLine2() {
    return onLine2;
  }

  /**
   * Set whether the intersecting point should land on line 2.
   *
   * @param onLine2 True if you want the intersecting point to lie on line 2, otherwise pass in
   *                false.
   * @since 1.2.0
   */
  public void setOnLine2(boolean onLine2) {
    this.onLine2 = onLine2;
  }
}
