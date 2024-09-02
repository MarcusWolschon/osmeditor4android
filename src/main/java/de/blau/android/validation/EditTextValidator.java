package de.blau.android.validation;

import android.text.Editable;
import android.widget.EditText;
import androidx.annotation.NonNull;
import de.blau.android.util.AfterTextChangedWatcher;

abstract class EditTextValidator implements AfterTextChangedWatcher, FormValidation {

    private static final String EMPTY_TEXT = "";

    @NonNull
    private final EditText editText;

    /**
     * Construct a validator for a specific EditText
     * 
     * @param editText the EditText
     */
    EditTextValidator(@NonNull EditText editText) {
        this.editText = editText;
        this.editText.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void validate() {
        Editable editable = editText.getText();
        String text = editable == null ? EMPTY_TEXT : editable.toString();
        if (isValid(text)) {
            clearError();
        } else {
            showError(getErrorText());
        }
    }

    /**
     * Test if the text is valid
     * 
     * @param text the text to check
     * @return true if the text is valid
     */
    protected abstract boolean isValid(@NonNull String text);

    /**
     * Show an error text
     * 
     * @param errorText the text to display
     */
    private void showError(@NonNull String errorText) {
        editText.setError(errorText);
    }

    /**
     * Clear any error
     */
    private void clearError() {
        editText.setError(null);
    }
}
