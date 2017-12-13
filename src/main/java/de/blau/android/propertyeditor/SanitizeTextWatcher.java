package de.blau.android.propertyeditor;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import de.blau.android.util.Util;

public class SanitizeTextWatcher implements TextWatcher {
    final Activity activity;
    final int      maxStringLength;

    SanitizeTextWatcher(Activity activity, int maxStringLength) {
        this.activity = activity;
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
        Util.sanitizeString(activity, s, maxStringLength);
    }
}
