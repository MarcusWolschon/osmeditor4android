package de.blau.android.validation;

import java.util.regex.Pattern;

import android.support.annotation.Nullable;

public class PatternAndAge {
    private String  value;
    private boolean isRegexp;
    private long    s;       // age in seconds

    private Pattern patternCache = null;

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

    String getValue() {
        return value;
    }

    void setValue(String value) {
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
     * Set the time between resurveys in seconds
     * 
     * @param s time beterrn resurvey in seconds
     */
    void setAge(long s) {
        this.s = s;
    }

    boolean isRegexp() {
        return isRegexp;
    }

    void setIsRegexp(boolean isRegexp) {
        this.isRegexp = isRegexp;
    }
}
