package io.vespucci.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.contract.Urls;
import io.vespucci.listener.DoNothingListener;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;

/**
 * Simple alert dialog with an OK button that does nothing
 * 
 * @author simon
 *
 */
public class ForbiddenLogin extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = ForbiddenLogin.class.getSimpleName().substring(0, Math.min(23, ForbiddenLogin.class.getSimpleName().length()));

    private static final String FRAGMENT_TAG = "forbidden_alert";

    private static final String MESSAGE_KEY = "message";

    private String message;

    /**
     * Display a dialog indicating that login didn't work
     * 
     * @param activity the calling Activity
     * @param message the message
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String message) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        ForbiddenLogin alertDialogFragment = newInstance(message);
        try {
            alertDialogFragment.show(fm, FRAGMENT_TAG);
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
        io.vespucci.dialogs.Util.dismissDialog(activity, FRAGMENT_TAG);
    }

    /**
     * Get a new instance of the ForbiddenLogin dialog
     * 
     * @param message the message
     * @return a new instance of the ForbiddenLogin dialog
     */
    @NonNull
    private static ForbiddenLogin newInstance(@NonNull String message) {
        ForbiddenLogin f = new ForbiddenLogin();

        Bundle args = new Bundle();
        args.putSerializable(MESSAGE_KEY, message);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        message = Util.getSerializeable(getArguments(), MESSAGE_KEY, String.class);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.forbidden_login_title);
        if (message != null) {
            builder.setMessage(message);
        }
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setNegativeButton(R.string.dismiss, doNothingListener);
        builder.setPositiveButton(R.string.go_to_openstreetmap, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.OSM))));

        return builder.create();
    }
}
