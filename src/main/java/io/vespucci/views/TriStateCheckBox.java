package io.vespucci.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import ch.poole.android.checkbox.IndeterminateCheckBox;
import io.vespucci.R;
import io.vespucci.dialogs.Tip;

public class TriStateCheckBox extends IndeterminateCheckBox {

    private OnStateChangedListener onStateChangedListener = null;

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

    @Override
    public void setOnStateChangedListener(OnStateChangedListener listener) {
        // we assume that the initial state has been set before this happens
        super.setOnStateChangedListener((IndeterminateCheckBox arg0, Boolean arg1) -> {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                Tip.showOptionalDialog(activity, R.string.tip_tristate_checkbox_key, R.string.tip_tristate_checkbox);
            }
            if (onStateChangedListener != null) {
                onStateChangedListener.onStateChanged(arg0, arg1);
            }
            // the code above only needs to run once, so short cut from now on
            super.setOnStateChangedListener(onStateChangedListener);
        });
        onStateChangedListener = listener;
    }

    /**
     * Find the fragment activity we are being showed on
     * 
     * @return a FragmentActivity or null
     */
    @Nullable
    private FragmentActivity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) {
                return (FragmentActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
