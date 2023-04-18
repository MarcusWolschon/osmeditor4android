package de.blau.android.dialogs;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback interface for evaluating a string
 * 
 * @author simon
 *
 */
public interface EvalCallback extends Serializable {
    /**
     * Evaluate the input and potentially return a message or output
     * 
     * @param input the input
     * @param flag1 boolean param
     * @param flag2 boolean param
     * @return the result or null
     */
    @Nullable
    String eval(@NonNull String input, boolean flag1, boolean flag2);
}
