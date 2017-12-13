package de.blau.android.util.mapbox.turf;

import java.util.HashMap;
import java.util.Map;

/**
 * This {@code TurfHelpers} class is made up of methods that take in an object, convert it, and then return the object
 * in the desired units or object.
 *
 * @see <a href="http://turfjs.org/docs/">Turfjs documentation</a>
 * @since 1.2.0
 */
class TurfHelpers {

    private static final Map<String, Double> factors;

    static {
        factors = new HashMap<>();
        factors.put(TurfConstants.UNIT_MILES, 3960d);
        factors.put(TurfConstants.UNIT_NAUTICAL_MILES, 3441.145d);
        factors.put(TurfConstants.UNIT_DEGREES, 57.2957795d);
        factors.put(TurfConstants.UNIT_RADIANS, 1d);
        factors.put(TurfConstants.UNIT_INCHES, 250905600d);
        factors.put(TurfConstants.UNIT_YARDS, 6969600d);
        factors.put(TurfConstants.UNIT_METERS, 637300d);
        factors.put(TurfConstants.UNIT_KILOMETERS, 6373d);

        // Also supported
        factors.put("metres", 637300d);
        factors.put("kilometres", 6373d);
    }

    /**
     * Convert radians to distance. The units used here equals the default.
     *
     * @param radians Double representing a radian value.
     * @return Converted radian to distance value.
     * @throws TurfException Signals that a Turf exception of some sort has occurred.
     * @since 1.2.0
     */
    public static double radiansToDistance(double radians) throws TurfException {
        return radiansToDistance(radians, TurfConstants.UNIT_DEFAULT);
    }

    /**
     * Convert radians to distance.
     *
     * @param radians Double representing a radian value.
     * @param units Pass in the units you'd like to use.
     * @return Converted radian to distance value.
     * @throws TurfException Signals that a Turf exception of some sort has occurred.
     * @since 1.2.0
     */
    public static double radiansToDistance(double radians, String units) throws TurfException {
        Double factor = factors.get(units);
        if (factor == null) {
            throw new TurfException("Invalid unit.");
        }

        return radians * factor;
    }

    /**
     * Convert distance to radians. The units used here equals the default.
     *
     * @param distance Double representing a distance value.
     * @return Converted distance to radians value.
     * @throws TurfException Signals that a Turf exception of some sort has occurred.
     * @since 1.2.0
     */
    public static double distanceToRadians(double distance) throws TurfException {
        return distanceToRadians(distance, TurfConstants.UNIT_DEFAULT);
    }

    /**
     * Convert distance to radians.
     *
     * @param distance Double representing a distance value.
     * @param units Pass in the units you'd like to use.
     * @return Converted distance to radians value.
     * @throws TurfException Signals that a Turf exception of some sort has occurred.
     * @since 1.2.0
     */
    public static double distanceToRadians(double distance, String units) throws TurfException {
        Double factor = factors.get(units);
        if (factor == null) {
            throw new TurfException("Invalid unit.");
        }
        return distance / factor;
    }
}
