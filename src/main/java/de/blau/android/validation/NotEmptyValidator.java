package de.blau.android.validation;

import android.support.annotation.NonNull;
import android.widget.EditText;

public class NotEmptyValidator extends EditTextValidator {

    @NonNull
    private final String errorText;

    public NotEmptyValidator(@NonNull EditText editText, @NonNull String errorText) {
        super(editText);
        this.errorText = errorText;
    }

    @Override
    protected boolean isValid(@NonNull String text) {
        return !text.isEmpty();
    }

    @NonNull
    @Override
    public String getErrorText() {
        return errorText;
    }

}
