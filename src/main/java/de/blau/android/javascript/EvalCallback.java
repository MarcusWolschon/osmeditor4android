package de.blau.android.javascript;

import android.support.annotation.Nullable;

/**
 * Callback interface for evaluating JS
 * 
 * @author simon
 *
 */
public interface EvalCallback {
    /**
     * Evaluate JS given as input
     * 
     * @param input the input JS
     * @return the result or null
     */
    @Nullable
    String eval(String input);
}
