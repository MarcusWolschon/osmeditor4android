package io.vespucci.osm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * Parse and output OSM duration values
 * 
 * Currently doesn't handle the infrequent ISO 8601 values
 * 
 * @author Simon
 *
 */
public final class Duration {

    private static final Pattern HMS = Pattern.compile("^\\s?(\\d+):(\\d{1,2}):(\\d{1,2})\\s?$");
    private static final Pattern HM  = Pattern.compile("^\\s?(\\d+):(\\d{1,2})\\s?$");
    private static final Pattern M   = Pattern.compile("^\\s?(\\d+)\\s?$");

    /**
     * Private constructor to prevent instantiation
     */
    private Duration() {
        // do nothing
    }

    /**
     * Parser a duration value
     * 
     * Assumptions: bare integer -> minutes hh:mm hours and minutes hh:mm:ss hours, minutes and seconds
     * 
     * @param input the input value
     * @return seconds
     */
    public static int parse(@NonNull String input) {
        Matcher matcher = M.matcher(input);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 60;
        }
        matcher = HM.matcher(input);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 3600 + Integer.parseInt(matcher.group(2)) * 60;
        }
        matcher = HMS.matcher(input);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) * 3600 + Integer.parseInt(matcher.group(2)) * 60 + Integer.parseInt(matcher.group(3));
        }
        throw new NumberFormatException(input + " isn't an OSM duration value");
    }

    /**
     * Format a count of minutes as a duration value
     * 
     * @param input the number of seconds
     * @return a formated String
     */
    @NonNull
    public static String toString(int input) {
        int seconds = input % 60;
        int minutes = ((input - seconds) / 60) % 60;
        int hours = (input - seconds - minutes * 60) / 3600;
        if (seconds == 0) {
            if (hours == 0) {
                return Integer.toString(minutes);
            }
            return String.format("%02d:%02d", hours, minutes);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
