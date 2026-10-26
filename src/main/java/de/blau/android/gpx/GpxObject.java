package de.blau.android.gpx;

import android.content.Context;
import androidx.annotation.NonNull;

public interface GpxObject {
    
    /**
     * Generate a short description suitable for a menu
     * 
     * @param ctx Android Context
     * @return the description
     */
    public String getShortDescription(@NonNull Context ctx);
}
