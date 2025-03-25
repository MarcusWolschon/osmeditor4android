package io.vespucci.presets;

import androidx.annotation.NonNull;

public enum UseLastAsDefaultType {
    TRUE, FALSE, FORCE;

    /**
     * Get an UseLastAsDefault value from a String
     * 
     * @param value the input string
     * @return the appropriate value of FALSE if is can't be determined
     */
    @NonNull
    public static UseLastAsDefaultType fromString(@NonNull String value) {
        switch (value) {
        case "true":
            return TRUE;
        case "force":
            return FORCE;
        case "false":
        default:
            return FALSE;
        }
    }
}
