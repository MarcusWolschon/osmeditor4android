package de.blau.android.validation;

import androidx.annotation.NonNull;
import android.widget.EditText;

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
        return !text.isEmpty();
    }

    @NonNull
    @Override
    public String getErrorText() {
        return errorText;
    }

}
