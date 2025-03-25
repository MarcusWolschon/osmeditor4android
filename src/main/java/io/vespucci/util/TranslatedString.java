package io.vespucci.util;

import android.content.Context;
import androidx.annotation.NonNull;

public interface TranslatedString {

    /**
     * Return a (potentially) translated String representation of this object
     * 
     * @param context an Android Context
     * @return a translated string
     */
    @NonNull
    public String toTranslatedString(@NonNull Context context);
}
