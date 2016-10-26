package de.blau.android.util.mapbox.turf;

/**
 * A form of {@code Throwable} that indicates conditions that a reasonable application might want to
 * catch.
 *
 * @see <a href="http://turfjs.org/docs/">Turfjs documentation</a>
 * @since 1.2.0
 */
public class TurfException extends Exception {

  /**
   * A form of {@code Throwable} that indicates conditions that a reasonable application might want
   * to catch.
   *
   * @param message the detail message (which is saved for later retrieval by the
   *                {@link #getMessage()} method).
   */
  public TurfException(String message) {
    super(message);
  }
}
