package de.blau.android.propertyeditor;

import android.content.Context;
import android.text.Editable;
import androidx.annotation.Nullable;
import de.blau.android.util.AfterTextChangedWatcher;
import de.blau.android.util.Util;

public class SanitizeTextWatcher implements AfterTextChangedWatcher {
    final Context context;
    final int     maxStringLength;

    /**
     * Construct a TextWatched that sanitizes the text and displays a message if necessary
     * 
     * @param context an Android Context if null no messages will be displayed
     * @param maxStringLength the maximum length of the text
     */
    public SanitizeTextWatcher(@Nullable Context context, int maxStringLength) {
        this.context = context;
        this.maxStringLength = maxStringLength;
    }

    @Override
    public void afterTextChanged(Editable s) {
        Util.sanitizeString(context, s, maxStringLength);
    }
}
