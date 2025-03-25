package io.vespucci.util;

import android.text.TextWatcher;

public interface AfterTextChangedWatcher extends TextWatcher {

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // required, but not used
    }

    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {
        // required, but not used
    }
}
