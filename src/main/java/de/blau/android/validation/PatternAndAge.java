package de.blau.android.validation;

import java.util.regex.Pattern;

import android.support.annotation.Nullable;

/**
 * Container for last edit age configuration
 * 
 * @author simon
 *
 */
public class PatternAndAge {
    private String  value;
    private boolean isRegexp;
    private long    s;       // age in seconds

    private Pattern patternCache = null;

    /**
     * Test if this matches the supplied value
     * 
     * @param v the value
     * @return true if it matches
     */
    public boolean matches(@Nullable String v) {
        if (v == null) {
            return getValue() == null;
        }
        if (isRegexp()) {
            if (patternCache == null) {
                patternCache = Pattern.compile(getValue());
            }
            return patternCache != null && patternCache.matcher(v).matches();
        } else {
            return getValue().equals(v);
        }
    }

    /**
     * THe value we match against
     * 
     * @return the value or null (matches everything)
     */
    @Nullable
    String getValue() {
        return value;
    }

    /**
     * Set the match value
     * 
     * @param value the value or null
     */
    void setValue(@Nullable String value) {
        this.value = value;
    }

    /**
     * Retrieve the time between surveys
     * 
     * @return the time between survey in seconds
     */
    long getAge() {
        return s;
    }

    /**
     * Set the time between re-surveys in seconds
     * 
     * @param s time between re-survey in seconds
     */
    void setAge(long s) {
        this.s = s;
    }

    /**
     * Check if the value should be treated as a regexp
     * 
     * @return true if a regexp
     */
    boolean isRegexp() {
        return isRegexp;
    }

    /**
     * Set the regexp flag
     * 
     * @param isRegexp the boolean value the flag should be set to
     */
    void setIsRegexp(boolean isRegexp) {
        this.isRegexp = isRegexp;
    }
}
