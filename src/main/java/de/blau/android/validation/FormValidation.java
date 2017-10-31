package de.blau.android.validation;

import android.support.annotation.NonNull;

public interface FormValidation {

    void validate();

    @NonNull
    String getErrorText();

}
