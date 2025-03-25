package io.vespucci.presets;

import java.util.Locale;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum MatchType {
    NONE, KEY, KEY_NEG, KEY_VALUE, KEY_VALUE_NEG;

    /**
     * Get a MatchType corresponding to the input String
     * 
     * @param matchString the MatchType as a String
     * @return the MatchType or null if unknown
     */
    @Nullable
    static MatchType fromString(@NonNull String matchString) {
        MatchType type = null;
        switch (matchString) {
        case "none":
            type = MatchType.NONE;
            break;
        case "key":
            type = MatchType.KEY;
            break;
        case "key!":
            type = MatchType.KEY_NEG;
            break;
        case "keyvalue":
            type = MatchType.KEY_VALUE;
            break;
        case "keyvalue!":
            type = MatchType.KEY_VALUE_NEG;
            break;
        default:
            Log.e(MatchType.class.getSimpleName(), "Unknown match type string " + matchString);
        }
        if (type != null) {
            return type;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.US);
    }
}
