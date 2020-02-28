package de.blau.android.prefs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Save the string from an EditTextPreference as an int and the same for retrieving From
 * https://stackoverflow.com/questions/3721358/preferenceactivity-save-value-as-integer
 * 
 * @author Simon Poole
 *
 */
public class IntEditTextPreference extends EditTextPreference {

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public IntEditTextPreference(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     */
    public IntEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     * @param defStyle an attribute in the current theme that contains a reference to a style resource that supplies
     *            default values for the view. Can be 0 to not look for defaults.
     */
    public IntEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }
}
