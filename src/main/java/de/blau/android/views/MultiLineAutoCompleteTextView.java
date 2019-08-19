package de.blau.android.views;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AutoCompleteTextView;

/**
 * Wrapper to work around multiLineText restrictions imposed by google, in particular this will stop the IME consuming
 * the "action" key
 * 
 * @author simon
 *
 */
public class MultiLineAutoCompleteTextView extends AutoCompleteTextView {

    /**
     * Construct a instance
     * 
     * @param context an Android Context
     */
    public MultiLineAutoCompleteTextView(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a instance
     * 
     * @param context an Android Context
     * @param attrs an AttributeSet
     */
    public MultiLineAutoCompleteTextView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Construct a instance
     * 
     * @param context an Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr attribute id for a reference to a default style
     */
    public MultiLineAutoCompleteTextView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Construct a instance
     * 
     * @param context an Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr attribute id for a reference to a default style
     * @param defStyleRes resource id for a default style if defStyleAttr is 0 or not found
     */
    public MultiLineAutoCompleteTextView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Construct a instance
     * 
     * @param context an Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr attribute id for a reference to a default style
     * @param defStyleRes resource id for a default style if defStyleAttr is 0 or not found
     * @param popupTheme a Theme for inflating the popup or null
     */
    public MultiLineAutoCompleteTextView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, @Nullable Theme popupTheme) {
        super(context, attrs, defStyleAttr, defStyleRes, popupTheme);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return connection;
    }
}
