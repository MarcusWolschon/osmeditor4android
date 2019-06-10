package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.widget.TextView;

public final class TextSize {

    /**
     * Wrapper for flavor compatibility
     * 
     * @param v the TextView we want to set the size on
     */
    static void setIconTextSize(@NonNull TextView v) {
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
    }
}
