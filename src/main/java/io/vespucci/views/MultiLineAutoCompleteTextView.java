package io.vespucci.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

/**
 * Wrapper to work around multiLineText restrictions imposed by google, in particular this will stop the IME consuming
 * the "action" key
 * 
 * @author simon
 *
 */
public class MultiLineAutoCompleteTextView extends AppCompatAutoCompleteTextView {

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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return connection;
    }
}
