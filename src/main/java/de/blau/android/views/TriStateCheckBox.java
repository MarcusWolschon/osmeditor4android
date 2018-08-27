package de.blau.android.views;

import com.buildware.widget.indeterm.IndeterminateCheckBox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
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
    private void init() {
        ColorStateList colorStateList = ContextCompat.getColorStateList(getContext(), R.color.control_checkable_material);
        setSupportButtonTintList(colorStateList);
        setLongClickable(true);
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                setIndeterminate(!isIndeterminate());
                return true;
            }
        });
    }
}
