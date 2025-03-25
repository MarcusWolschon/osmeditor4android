package io.vespucci.validation;

import androidx.annotation.NonNull;

public interface FormValidation {

    /**
     * Validate whatever
     */
    void validate();

    /**
     * Get the error text from the validation
     * 
     * @return a String with the error
     */
    @NonNull
    String getErrorText();
}
