package de.blau.android.validation;

import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

abstract class EditTextValidator implements TextWatcher, FormValidation {

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
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        // Nothing to do here.
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        // Nothing to do here.
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
