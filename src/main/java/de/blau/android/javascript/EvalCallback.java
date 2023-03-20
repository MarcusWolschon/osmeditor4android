package de.blau.android.javascript;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback interface for evaluating a string
 * 
 * @author simon
 *
 */
public interface EvalCallback {
    /**
     * Evaluate the input and potentially return a message or output
     * 
     * @param input the input
     * @return the result or null
     */
    @Nullable
    String eval(@NonNull String input);
}
