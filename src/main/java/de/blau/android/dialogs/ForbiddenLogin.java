package de.blau.android.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Simple alert dialog with an OK button that does nothing
 * 
 * @author simon
 *
 */
public class ForbiddenLogin extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = ForbiddenLogin.class.getSimpleName();

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
        de.blau.android.dialogs.Util.dismissDialog(activity, FRAGMENT_TAG);
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
        message = (String) getArguments().getSerializable(MESSAGE_KEY);
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
        builder.setPositiveButton(R.string.go_to_openstreetmap, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.OSM)));
            }
        });
        return builder.create();
    }
}
