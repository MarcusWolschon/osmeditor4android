package de.blau.android.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import de.blau.android.R;

public abstract class Inset extends View {

    /**
     * Construct an inset view
     * 
     * @param context Android Context
     */
    protected Inset(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Construct an inset view
     * 
     * @param context Android COntext
     * @param attrs an AttributeSet
     */
    protected Inset(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int color = getInsetColor(context, attrs, R.styleable.Inset, R.styleable.Inset_insetColor);
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, windowInsets) -> {
            // Android 5 returns IME insets even when the soft keyboard is no longer showing
            boolean getImeInset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    | ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).isAcceptingText();
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | (getImeInset ? WindowInsetsCompat.Type.ime() : 0)); // NOSONAR
            v.setLayoutParams(getLayoutParams(v, insets));
            v.setBackgroundColor(color);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * @param v
     * @param insets
     * @return
     */
    protected abstract LayoutParams getLayoutParams(View v, Insets insets);

    /**
     * Get the inset color
     * 
     * @param context an Android Context
     * @param attrs the AttributeSet
     * @param attributesRes the attribute resource ids
     * @param colorRes the color resource id
     * @return the color for the inset
     */
    private static int getInsetColor(@NonNull Context context, @Nullable AttributeSet attrs, @NonNull int[] attributesRes, @NonNull int colorRes) {
        int defaultColor = ContextCompat.getColor(context, R.color.osm_green);
        if (attrs == null) {
            return defaultColor;
        }
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(attrs, attributesRes);
            return a.getColor(colorRes, defaultColor);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }
}