package de.blau.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.support.annotation.NonNull;

public final class DateFormatter {

    private static TimeZone timeZone = TimeZone.getTimeZone("UTC");

    /**
     * Parses a date from the given string using the date pattern. Throws a {@code ParseException} on failure.
     * 
     * @param pattern the pattern
     * @param dateString the date string
     * @return a Date object
     * @throws ParseException if dateString can't be parsed
     */
    public static @NonNull Date getDate(@NonNull final String pattern, @NonNull final String dateString) throws ParseException {
        return getUtcFormat(pattern).parse(dateString);
    }

    /**
     * Formats the current date (in UTC) using the date pattern.
     * 
     * 
     * @param pattern the pattern
     * @return a formatted String
     */
    public static @NonNull String getFormattedString(@NonNull final String pattern) {
        return getFormattedString(pattern, new Date());
    }

    /**
     * Formats the given date using the date pattern.
     */
    /**
     * 
     * @param pattern the pattern
     * @param date the date we want to format
     * @return a formatted String
     */
    public static @NonNull String getFormattedString(@NonNull final String pattern, @NonNull final Date date) {
        return getUtcFormat(pattern).format(date);
    }

    /**
     * Get a SimpleDateFormat with the correct TZ set
     * 
     * @param format the format String
     * @return a SimpleDateFormat
     */
    public static SimpleDateFormat getUtcFormat(@NonNull String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.US);
        formatter.setTimeZone(timeZone);
        return formatter;
    }
}
