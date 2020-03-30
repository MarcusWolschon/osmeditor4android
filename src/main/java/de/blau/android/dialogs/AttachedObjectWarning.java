package de.blau.android.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Shows a dialog warning the user that an invisible object has been changed, the user has the option of ignoring the
 * warning, suppressing it and undoing the change.
 *
 */
public class AttachedObjectWarning extends DialogFragment {

    private static final String DEBUG_TAG = AttachedObjectWarning.class.getSimpleName();

    private static final String TAG = "fragment_attached_object_activity";

    private Main main;

    /**
     * Shows a dialog warning the user that an invisible object has been changed, the user has the option of ignoring
     * the warning, suppressing it and undoing the change.
     * 
     * @param activity Activity creating the dialog and starting the intent Activity if confirmed
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            AttachedObjectWarning detachFragment = newInstance();
            detachFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    public static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Construct a new AttachedObjectWarning instance
     * 
     * @return an AttachedObjectWarning dialog
     */
    @NonNull
    private static AttachedObjectWarning newInstance() {
        AttachedObjectWarning f = new AttachedObjectWarning();
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(DEBUG_TAG, "onAttach");
        try {
            main = (Main) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must be class Main");
        }
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(R.string.attached_object_warning_title);
        builder.setMessage(R.string.attached_object_warning_message);
        builder.setPositiveButton(R.string.attached_object_warning_continue, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // simple continue
            }
        });
        builder.setNeutralButton(R.string.attached_object_warning_stop, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                App.getLogic().setAttachedObjectWarning(false);
            }
        });
        builder.setNegativeButton(R.string.undo, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                main.getUndoListener().onClick(null);
            }
        });

        return builder.create();
    }
}
