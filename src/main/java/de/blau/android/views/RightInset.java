package de.blau.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;

public class RightInset extends Inset {

    /**
     * Construct an inset view
     * 
     * @param context Android Context
     */
    public RightInset(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct an inset view
     * 
     * @param context Android COntext
     * @param attrs an AttributeSet
     */
    public RightInset(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected LayoutParams getLayoutParams(View v, Insets insets) {
        LayoutParams lp = v.getLayoutParams();
        lp.width = insets.right;
        return lp;
    }
}