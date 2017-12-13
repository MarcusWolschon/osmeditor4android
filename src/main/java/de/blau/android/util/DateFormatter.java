package de.blau.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.support.annotation.NonNull;

public abstract class DateFormatter {

    /**
     * Parses a date from the given string using the date pattern. Throws a {@code ParseException} on failure.
     */
    public static @NonNull Date getDate(@NonNull final String pattern, @NonNull final String dateString) throws ParseException {
        return getSimpleDateFormat(pattern).parse(dateString);
    }

    /**
     * Formats the current date using the date pattern.
     */
    public static @NonNull String getFormattedString(@NonNull final String pattern) {
        return getFormattedString(pattern, new Date());
    }

    /**
     * Formats the given date using the date pattern.
     */
    public static @NonNull String getFormattedString(@NonNull final String pattern, @NonNull final Date date) {
        return getSimpleDateFormat(pattern).format(date);
    }

    /**
     * Constructs a new {@code SimpleDateFormat} using the given date pattern.
     */
    private static @NonNull SimpleDateFormat getSimpleDateFormat(@NonNull final String pattern) {
        return new SimpleDateFormat(pattern, Locale.US);
    }

}
