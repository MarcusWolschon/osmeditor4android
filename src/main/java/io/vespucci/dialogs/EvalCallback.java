package io.vespucci.dialogs;

import java.io.Serializable;

import android.content.Context;
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
     * @param context an Android context
     * @param input the input
     * @param flag1 boolean param
     * @param flag2 boolean param
     * 
     * @return the result or null
     */
    @Nullable
    String eval(@Nullable Context context, @NonNull String input, boolean flag1, boolean flag2);
}
