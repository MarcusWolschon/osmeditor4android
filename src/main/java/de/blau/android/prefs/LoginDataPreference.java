package de.blau.android.prefs;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import de.blau.android.R;

/**
 * A dialog preference that allows the user to set username and password in one dialog. Changes get saved to the
 * {@link AdvancedPrefDatabase}
 */
public class LoginDataPreference extends DialogPreference {

    public LoginDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoginDataPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        super.setPersistent(false);
        super.setDialogLayoutResource(R.layout.login_edit);
    }
}
