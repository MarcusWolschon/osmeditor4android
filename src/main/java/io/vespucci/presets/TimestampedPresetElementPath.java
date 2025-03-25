package io.vespucci.presets;

import androidx.annotation.NonNull;

/**
 * Extension of PresetElementPath with a simple timestamp added
 * 
 * This ignores the timestamp in equals and hashCode on purpose
 * 
 * @author simon
 *
 */
public class TimestampedPresetElementPath extends PresetElementPath { // NOSONAR

    private static final long serialVersionUID = 1L;

    private long timestamp;

    /**
     * Construct a new instance
     * 
     * @param path an existing path
     */
    TimestampedPresetElementPath(@NonNull PresetElementPath path) {
        super(path);
        timestamp = System.currentTimeMillis();
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
}
