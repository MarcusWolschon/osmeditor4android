package de.blau.android.util;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Utilities for assisting in the parsing of latitude and longitude strings into Decimals.
 * 
 * Based on CoordinateParseUtils.java Copyright (C) Global Biodiversity Information Facility, Licensed under
 * the Apache License, Version 2.0
 * 
 * Changes wrt the original:
 *  - get rid of some unnecessary dependencies
 *  - allow cardinal direction letters to be in front and behind the coordinate value
 *  - parse degree + minute values correctly
 *  - JavaDoc and other cleanup
 */
public final class CoordinateParser {
    private static final String DMS = "\\s*(\\d{1,3})\\s*(?:°|d|º| |g|o)"  // The degrees
            + "\\s*([0-6]?\\d)\\s*(?:'|m| |´|’|′)" // The minutes
            + "\\s*(?:"                            // Non-capturing group
            + "([0-6]?\\d(?:[,.]\\d+)?)"           // Seconds and optional decimal
            + "\\s*(?:\"|''|s|´´|″)?"
            + ")?\\s*";
    private static final String DM = "\\s*(\\d{1,3})\\s*(?:°|d|º| |g|o)" // The degrees
           + "\\s*(?:"                           // Non-capturing group
           + "([0-6]?\\d(?:[,.]\\d+)?)"          // Minutes and optional decimal
           + "\\s*(?:'|m| |´|’|′)?"
           + ")?\\s*";
    private static final String D = "\\s*(\\d{1,3}(?:[,.]\\d+)?)\\s*(?:°|d|º| |g|o|)\\s*"; // The degrees and optional decimal

    private static final String NSEOW = "([NSEOW])";
    private static final String SEPARATORS = "[ ,;/]?";
    
    private static final Pattern DMS_SINGLE  = Pattern.compile("^" + DMS + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DM_SINGLE   = Pattern.compile("^" + DM + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern D_SINGLE    = Pattern.compile("^" + D + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DMS_COORD   = Pattern.compile("^" + DMS + NSEOW + SEPARATORS + DMS + "([NSEOW])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DMS_COORD_2 = Pattern.compile("^" + NSEOW + DMS + SEPARATORS + NSEOW + DMS + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DM_COORD    = Pattern.compile("^" + DM + NSEOW + SEPARATORS + DM + "([NSEOW])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DM_COORD_2  = Pattern.compile("^" + NSEOW + DM + SEPARATORS + NSEOW + DM + "$", Pattern.CASE_INSENSITIVE);
    // private final static Pattern D_COORD = Pattern.compile("^" + D + "([NSEOW])" + "[ ,;/]?" + D + "([NSEOW])$",
    // Pattern.CASE_INSENSITIVE);
    private static final String POSITIVE = "NEO";

    /**
     * Private default constructor
     */
    private CoordinateParser() {
        throw new UnsupportedOperationException("Can't initialize class");
    }

    /**
     * This parses string representations of latitude and longitude values. 
     *
     * Coordinate precision will be 8 decimals at most, any more precise values will be rounded.
     *
     * Supported standard formats are the following, with dots or optionally a comma as the decimal marker, and
     * variations on the units also accepted e.g. °, d, º, g, o.
     * <ul>
     * <li>43.63871944444445</li>
     * <li>N43°38'19.39"</li>
     * <li>43°38'19.39"N</li>
     * <li>43°38.3232'N</li>
     * <li>43d 38m 19.39s N</li>
     * <li>43 38 19.39</li>
     * <li>433819N</li>
     * </ul>
     *
     * @param latitude The decimal latitude
     * @param longitude The decimal longitude
     *
     * @return The parse result
     * @throws ParseException if parsing fails, note the offset will alyways be 0 
     */
    @Nullable
    public static LatLon parseLatLng(final String latitude, final String longitude) throws ParseException {
        if (isNullOrEmpty(latitude) || isNullOrEmpty(longitude)) {
            throw new ParseException("null or empty coordinates lat " + latitude + " lon " + longitude, 0);
        }
        Double lat = null;
        Double lng = null;
        try {
            lat = Double.parseDouble(latitude);
            lng = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            // ignore
        }
        if (lat == null || lng == null) {
            // try degree minute seconds
            lat = parseDMS(latitude, true);
            lng = parseDMS(longitude, false);
        }

        return validateAndRound(lat, lng);
    }

    /**
     * Check if the string contains something
     * 
     * @param s the String
     * @return true if not null and not empty
     */
    private static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || "".equals(s.trim());
    }

    /**
     * Check if these are valid (WGS84) coordinates
     * 
     * @param lat the latitude
     * @param lon the longitude
     * @return true if in bounds
     */
    private static boolean inRange(double lat, double lon) {
        if (Double.compare(lat, 90) <= 0 && Double.compare(lat, -90) >= 0 && Double.compare(lon, 180) <= 0 && Double.compare(lon, -180) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Check if the cardinal direction indicates that the associated value is a latitude value
     * 
     * @param direction the direction string
     * @return true if the cardinal direction indicates that the associated value is a latitude value
     */
    private static boolean isLat(@NonNull String direction) {
        return "NS".contains(direction.toUpperCase());
    }

    /**
     * Determine the sign for the associated coordinate value from the cardinal direction
     * 
     * @param direction the cardinal direction
     * @return positive or negative 1
     */
    private static int coordSign(@NonNull String direction) {
        return POSITIVE.contains(direction.toUpperCase()) ? 1 : -1;
    }

    /**
     * Try to parse a String containing latitude and longitude values in a number of formats
     * 
     * @param coordinates the coordinate string
     * @return a LatLon object containing the lat and lon values
     * @throws ParseException if parsing fails, note the offset will alyways be 0 
     */
    @NonNull
    public static LatLon parseVerbatimCoordinates(@Nullable final String coordinates) throws ParseException {
        if (isNullOrEmpty(coordinates)) {
            throw new IllegalArgumentException("null or empty coordinates");
        }
        Matcher m = DMS_COORD.matcher(coordinates);
        if (m.find()) {
            final String dir1 = m.group(4);
            final String dir2 = m.group(8);
            // first parse coords regardless whether they are lat or lon
            double c1 = coordFromMatcher(m, 1, 2, 3, dir1);
            double c2 = coordFromMatcher(m, 5, 6, 7, dir2);
            return orderCoordinates(dir1, dir2, c1, c2);
        } else {
            m = DMS_COORD_2.matcher(coordinates);
            if (m.find()) {
                final String dir1 = m.group(1);
                final String dir2 = m.group(5);
                double c1 = coordFromMatcher(m, 2, 3, 4, dir1);
                double c2 = coordFromMatcher(m, 6, 7, 8, dir2);
                return orderCoordinates(dir1, dir2, c1, c2);
            } else {
                m = DM_COORD.matcher(coordinates);
                if (m.find()) {
                    final String dir1 = m.group(3);
                    final String dir2 = m.group(6);
                    double c1 = coordFromMatcher(m, 1, 2, dir1);
                    double c2 = coordFromMatcher(m, 4, 5, dir2);
                    return orderCoordinates(dir1, dir2, c1, c2);
                } else {
                    m = DM_COORD_2.matcher(coordinates);
                    if (m.find()) {
                        final String dir1 = m.group(1);
                        final String dir2 = m.group(4);
                        // first parse coords regardless whether they are lat or lon
                        double c1 = coordFromMatcher(m, 2, 3, dir1);
                        double c2 = coordFromMatcher(m, 5, 6, dir2);
                        return orderCoordinates(dir1, dir2, c1, c2);
                    } else if (coordinates.length() > 4) {
                        // try to split and then use lat/lon parsing
                        for (final char delim : ",;/ ".toCharArray()) {
                            int cnt = countMatches(coordinates, delim);
                            if (cnt == 1) {
                                String[] latlon = coordinates.split(String.valueOf(delim));
                                if (latlon.length == 2) {
                                    return parseLatLng(latlon[0], latlon[1]);
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new ParseException("invalid coordinates " + coordinates, 0);
    }

    /**
     * Try to determine which value is the latitude and which is the longitude
     * 
     * @param dir1 first cardinal direction character
     * @param dir2 second cardinal direction character
     * @param c1 first coordinate value
     * @param c2 second coordinate value
     * @return a LatLon object
     * @throws ParseException if parsing fails, note the offset will alyways be 0 
     */
    @NonNull
    private static LatLon orderCoordinates(@NonNull final String dir1, @NonNull final String dir2, double c1, double c2) throws ParseException {
        // now see what order the coords are in:
        if (isLat(dir1) && !isLat(dir2)) {
            return validateAndRound(c1, c2);
        } else if (!isLat(dir1) && isLat(dir2)) {
            return validateAndRound(c2, c1);
        } else {
            throw new ParseException("invalid coordinates 1: " + c1 + " 2: " + c2, 0);
        }
    }

    /**
     * Count the number of times a character is present in a String
     * 
     * @param s the String
     * @param c the char
     * @return a count of the occurrences
     */
    private static int countMatches(@NonNull String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Validate and round the coordinate values
     * 
     * @param lat the latitude
     * @param lon the longitude
     * @return a LonLon object
     * @throws ParseException if parsing fails, note the offset will alyways be 0 
     */
    @NonNull
    private static LatLon validateAndRound(double lat, double lon) throws ParseException {

        // round to 8 decimals
        lat = roundTo8decimals(lat);
        lon = roundTo8decimals(lon);

        // if everything falls in range
        if (inRange(lat, lon)) {
            return new LatLon(lat, lon);
        }

        // if lat is out of range, but in range of the lng, assume swapped coordinates.
        // note that should we desire to trust the following records, we would need to clear the flag for the records to
        // appear in search results and maps etc. however, this is logic decision, that goes above the capabilities of
        // this method
        if (Double.compare(lat, 90) > 0 || Double.compare(lat, -90) < 0) {
            // try and swap
            if (inRange(lon, lat)) { // NOSONAR
                return new LatLon(lon, lat);
            }
        }

        // then something is out of range
        throw new ParseException("value(s) out of range lat " + lat + " lon " + lon, 0);

    }

    /**
     * Parses a single DMS coordinate
     * 
     * @param coord the coordinate value
     * @param lat if true it is a latitude
     * @return the converted decimal
     * @throws ParseException if parsing fails, note the offset will alyways be 0 
     */
    protected static double parseDMS(@NonNull String coord, boolean lat) throws ParseException {
        final String DIRS = lat ? "NS" : "EOW";
        coord = coord.trim().toUpperCase();

        if (coord.length() > 3) {
            // preparse the direction and remove it from the string to avoid a very complex regex
            char dir = 'n';
            if (DIRS.contains(String.valueOf(coord.charAt(0)))) {
                dir = coord.charAt(0);
                coord = coord.substring(1);
            } else if (DIRS.contains(String.valueOf(coord.charAt(coord.length() - 1)))) {
                dir = coord.charAt(coord.length() - 1);
                coord = coord.substring(0, coord.length() - 1);
            }
            // without the direction chuck it at the regex
            Matcher m = DMS_SINGLE.matcher(coord);
            if (m.find()) {
                return coordFromMatcher(m, 1, 2, 3, String.valueOf(dir));
            } else {
                m = DM_SINGLE.matcher(coord);
                if (m.find()) {
                    return coordFromMatcher(m, 1, 2, String.valueOf(dir));
                } else {
                    m = D_SINGLE.matcher(coord);
                    if (m.find()) {
                        return coordFromMatcher(m, 1, String.valueOf(dir));
                    }
                }
            }
        }
        throw new ParseException("Parsing " + coord + " failed", 0);
    }

    /**
     * Convert the matched values to decimal coordinates
     * 
     * @param m the Matcher
     * @param idx1 index of the first group to use
     * @param idx2 index of the second group to use
     * @param idx3 index of the third group to use
     * @param sign the sign the value should have
     * @return the coordinate as a decimal value
     */
    private static double coordFromMatcher(@NonNull Matcher m, int idx1, int idx2, int idx3, String sign) {
        return roundTo8decimals(
                coordSign(sign) * dmsToDecimal(Double.parseDouble(m.group(idx1)), Double.parseDouble(m.group(idx2)), Double.parseDouble(m.group(idx3))));
    }

    /**
     * Convert the matched values to decimal coordinates
     * 
     * @param m the Matcher
     * @param idx1 index of the first group to use
     * @param idx2 index of the second group to use
     * @param sign the sign the value should have
     * @return the coordinate as a decimal value
     */
    private static double coordFromMatcher(@NonNull Matcher m, int idx1, int idx2, String sign) {
        return roundTo8decimals(coordSign(sign) * dmsToDecimal(Double.parseDouble(m.group(idx1)), Double.parseDouble(m.group(idx2)), 0.0));
    }

    /**
     * Convert the matched values to decimal coordinates
     * 
     * @param m the Matcher
     * @param idx1 index of the first group to use
     * @param sign the sign the value should have
     * @return the coordinate as a decimal value
     */
    private static double coordFromMatcher(@NonNull Matcher m, int idx1, String sign) {
        return roundTo8decimals(coordSign(sign) * dmsToDecimal(Double.parseDouble(m.group(idx1)), 0.0, 0.0));
    }

    /**
     * Convert degree, minutes and seconds value to decimal degrees
     * 
     * @param degree the degree value
     * @param minutes the minutes
     * @param seconds the seconds
     * @return the decimal degree value
     */
    private static double dmsToDecimal(double degree, @Nullable Double minutes, @Nullable Double seconds) {
        minutes = minutes == null ? 0 : minutes;
        seconds = seconds == null ? 0 : seconds;
        return degree + (minutes / 60) + (seconds / 3600);
    }

    /**
     * Round to 8 decimals (better than 1m precision)
     * 
     * @param x the Double value
     * @return the rounded value
     */
    @Nullable
    private static Double roundTo8decimals(@Nullable Double x) {
        return x == null ? null : Math.round(x * Math.pow(10, 8)) / Math.pow(10, 8);
    }
}
