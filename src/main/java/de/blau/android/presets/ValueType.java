package de.blau.android.presets;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum ValueType {
    OPENING_HOURS, OPENING_HOURS_MIXED, DIMENSION_HORIZONTAL, DIMENSION_VERTICAL, INTEGER, WEBSITE, PHONE, WIKIPEDIA, WIKIDATA, CARDINAL_DIRECTION, DATE;

    /**
     * Get a ValueType corresponding to the input String
     * 
     * @param typeString the ValueType as a String
     * @return the ValueType or null if unknown
     */
    @Nullable
    static ValueType fromString(@NonNull String typeString) {
        ValueType type = null;
        switch (typeString) {
        case "opening_hours":
            type = OPENING_HOURS;
            break;
        case "opening_hours_mixed":
            type = OPENING_HOURS_MIXED;
            break;
        case "dimension_horizontal":
            type = DIMENSION_HORIZONTAL;
            break;
        case "dimension_vertical":
            type = DIMENSION_VERTICAL;
            break;
        case "integer":
            type = INTEGER;
            break;
        case "website":
            type = WEBSITE;
            break;
        case "phone":
            type = PHONE;
            break;
        case "wikipedia":
            type = WIKIPEDIA;
            break;
        case "wikidata":
            type = WIKIDATA;
            break;
        case "cardinal_direction":
            type = CARDINAL_DIRECTION;
            break;
        case "date":
            type = DATE;
            break;
        default:
            Log.e(ValueType.class.getSimpleName(), "Unknown value type string " + typeString);
        }
        return type;
    }
}
