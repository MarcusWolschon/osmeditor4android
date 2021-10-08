package de.blau.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import androidx.core.content.ContextCompat;
import ch.poole.android.checkbox.IndeterminateCheckBox;
import de.blau.android.R;

public class TriStateCheckBox extends IndeterminateCheckBox {

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     */
    public TriStateCheckBox(Context context) {
        super(context);
        init();
    }

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public TriStateCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Standard View constructor
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr a Style resource id
     */
    public TriStateCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * Setup the ColorTintList and the OnLongClickListener
     */
    @SuppressLint("RestrictedApi")
    private void init() {
        ColorStateList colorStateList = ContextCompat.getColorStateList(getContext(), R.color.control_checkable_material);
        setSupportButtonTintList(colorStateList);
        setLongClickable(true);
        setOnLongClickListener(unused -> {
            setIndeterminate(!isIndeterminate());
            return true;
        });
    }
}
