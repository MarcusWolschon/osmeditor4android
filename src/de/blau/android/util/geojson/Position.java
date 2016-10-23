package de.blau.android.util.geojson;

/**
 * Represents a position defined by a longitude, latitude, and optionally, an altitude.
 *
 * @since 1.0.0
 */
public class Position {

  private final double longitude;
  private final double latitude;
  private final double altitude;

  /**
   * Private Constructor
   *
   * @param longitude double value with position's longitude.
   * @param latitude  double value with position's latitude.
   * @param altitude  double value with position's altitude.
   * @since 1.0.0
   */
  private Position(double longitude, double latitude, double altitude) {
    this.longitude = longitude;
    this.latitude = latitude;
    this.altitude = altitude;
  }

  /**
   * Gets the position's longitude.
   *
   * @return double value with positions longitude.
   * @since 1.0.0
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Gets the position's latitude.
   *
   * @return double value with positions latitude.
   * @since 1.0.0
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Gets the position's altitude.
   *
   * @return double value with positions altitude.
   * @since 1.0.0
   */
  public double getAltitude() {
    return altitude;
  }

  /**
   * Gets the position coordinates in a double array. Altitude will be included in the array
   * if a value is present.
   *
   * @return double[] array with longitude, latitude, and altitude (if present).
   * @since 1.0.0
   */
  public double[] getCoordinates() {
    if (hasAltitude()) {
      return new double[]{getLongitude(), getLatitude(), getAltitude()};
    } else {
      return new double[]{getLongitude(), getLatitude()};
    }
  }

  /**
   * Builds a {@link Position} from a double longitude, latitude and an altitude.
   *
   * @param longitude double longitude value.
   * @param latitude  double latitude value.
   * @param altitude  double altitude value.
   * @return {@link Position}.
   * @since 1.0.0
   */
  public static Position fromCoordinates(double longitude, double latitude, double altitude) {
    return new Position(longitude, latitude, altitude);
  }

  /**
   * Builds a {@link Position} from a double longitude and latitude.
   *
   * @param longitude double longitude value.
   * @param latitude  double latitude value.
   * @return {@link Position}.
   * @since 1.0.0
   */
  public static Position fromCoordinates(double longitude, double latitude) {
    return new Position(longitude, latitude, Double.NaN);
  }

  public static Position fromCoordinates(double[] coordinates) {
    if (coordinates.length == 3) {
      return Position.fromCoordinates(coordinates[0], coordinates[1], coordinates[2]);
    } else {
      return Position.fromCoordinates(coordinates[0], coordinates[1]);
    }
  }

  /**
   * Checks if a position has an altitude value.
   *
   * @return true if position contains altitude data.
   * @since 1.0.0
   */
  public boolean hasAltitude() {
    return !Double.isNaN(altitude);
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param obj An object.
   * @return true if the position matches the object, false otherwise.
   * @since 1.3.0
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Position)) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    Position position = (Position) obj;
    if (position.getLatitude() != latitude) {
      // They need to have the same latitude
      return false;
    } else if (position.getLongitude() != longitude) {
      // They need to have the same longitude
      return false;
    } else if (Double.isNaN(position.getAltitude()) != Double.isNaN(altitude)) {
      // They need to have the same altitude NaN state
      return false;
    } else if (!Double.isNaN(altitude) && position.getAltitude() != altitude) {
      // They need to have the same latitude (if different from NaN)
      return false;
    }

    return true;
  }

  /**
   * Use to print out the longitude, latitude and altitude values.
   *
   * @return String containing the longitude, latitude, and altitude.
   * @since 1.3.0
   */
  @Override
  public String toString() {
    return "Position [longitude=" + longitude + ", latitude=" + latitude + ", altitude=" + altitude + "]";
  }
}
