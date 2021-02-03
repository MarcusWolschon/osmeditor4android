package de.blau.android.propertyeditor;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.Nullable;
import de.blau.android.util.Util;

public class SanitizeTextWatcher implements TextWatcher {
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
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // unused
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // unused
    }

    @Override
    public void afterTextChanged(Editable s) {
        Util.sanitizeString(context, s, maxStringLength);
    }
}
