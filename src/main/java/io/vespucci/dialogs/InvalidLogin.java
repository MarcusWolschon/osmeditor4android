package io.vespucci.dialogs;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Authorize;
import io.vespucci.listener.DoNothingListener;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ThemeUtils;

/**
 * Display a dialog reporting that the login credentials don't work
 *
 */
public class InvalidLogin extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = InvalidLogin.class.getSimpleName().substring(0, Math.min(23, InvalidLogin.class.getSimpleName().length()));

    private static final String TAG = "fragment_invalid_login";

    /**
     * Display a dialog reporting that the login credentials don't work
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            InvalidLogin invalidLoginFragment = newInstance();
            invalidLoginFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new instance of InvalidLogin dialog
     * 
     * @return a new InvalidLogin dialog instance
     */
    @NonNull
    private static InvalidLogin newInstance() {
        InvalidLogin f = new InvalidLogin();
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.wrong_login_data_title);
        builder.setMessage(R.string.wrong_login_data_message);
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setNegativeButton(R.string.cancel, doNothingListener); // logins in the preferences should no longer be
                                                                       // used
        if (App.getPreferences(getActivity()).getServer().getOAuth()) {
            builder.setPositiveButton(R.string.wrong_login_data_re_authenticate, (dialog, which) -> Authorize.startForResult(getActivity(), null));
        }
        return builder.create();
    }
}
