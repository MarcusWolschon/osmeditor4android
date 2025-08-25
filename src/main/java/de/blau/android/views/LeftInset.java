package de.blau.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;

public class LeftInset extends Inset {

    /**
     * Construct an inset view
     * 
     * @param context Android Context
     */
    public LeftInset(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct an inset view
     * 
     * @param context Android COntext
     * @param attrs an AttributeSet
     */
    public LeftInset(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected LayoutParams getLayoutParams(View v, Insets insets) {
        LayoutParams lp = v.getLayoutParams();
        lp.width = insets.left;
        return lp;
    }
}