package de.blau.android.validation;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

abstract class EditTextValidator implements TextWatcher, FormValidation {

    private static final String EMPTY_TEXT = "";

    @NonNull
    private final EditText editText;

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

    protected abstract boolean isValid(@NonNull String text);

    private void showError(@NonNull String errorText) {
        editText.setError(errorText);
    }

    private void clearError() {
        editText.setError(null);
    }

}
