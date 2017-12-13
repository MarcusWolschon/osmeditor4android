package de.blau.android.prefs;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Save the string from an EditTextPreference as an int and the same for retrieving From
 * https://stackoverflow.com/questions/3721358/preferenceactivity-save-value-as-integer
 * 
 * @author simon
 *
 */
public class IntEditTextPreference extends EditTextPreference {

    public IntEditTextPreference(Context context) {
        super(context);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
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
