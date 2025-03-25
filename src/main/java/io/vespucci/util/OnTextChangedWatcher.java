package io.vespucci.util;

import android.text.Editable;
import android.text.TextWatcher;

public interface OnTextChangedWatcher extends TextWatcher {

    @Override
    default void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // not used
    }

    @Override
    default void afterTextChanged(Editable arg0) {
        // not used
    }

}
