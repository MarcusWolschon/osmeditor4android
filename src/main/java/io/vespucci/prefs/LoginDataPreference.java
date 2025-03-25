package io.vespucci.prefs;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import io.vespucci.R;

/**
 * A dialog preference that allows the user to set username and password in one dialog. Changes get saved to the
 * {@link AdvancedPrefDatabase}
 */
public class LoginDataPreference extends DialogPreference {

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     */
    public LoginDataPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     * @param attrs the attributes of the XML tag that is inflating the view. This value may be null.
     * @param defStyle an attribute in the current theme that contains a reference to a style resource that supplies
     *            default values for the view. Can be 0 to not look for defaults.
     */
    public LoginDataPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Actually initialize the preference
     */
    private void init() {
        super.setPersistent(false);
        super.setDialogLayoutResource(R.layout.login_edit);
    }
}
