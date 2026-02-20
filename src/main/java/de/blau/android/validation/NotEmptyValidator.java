package de.blau.android.validation;

import android.widget.EditText;
import androidx.annotation.NonNull;

public class NotEmptyValidator extends EditTextValidator {

    @NonNull
    private final String errorText;

    /**
     * Construct an EditTextValidator that checks that the EditText isn't empty
     * 
     * @param editText the EditText
     * @param errorText the text to display if it is empty
     */
    public NotEmptyValidator(@NonNull EditText editText, @NonNull String errorText) {
        super(editText);
        this.errorText = errorText;
    }

    @Override
    protected boolean isValid(@NonNull String text) {
        return !text.trim().isEmpty();
    }

    @NonNull
    @Override
    public String getErrorText() {
        return errorText;
    }

}
