package de.blau.android.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Authorize;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Util;

/**
 * Display a dialog displaying information on a new version and offering to display the release notes.
 *
 */
public class NewVersion extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = NewVersion.class.getSimpleName();

    private static final String TAG = "fragment_newversion";

    /**
     * Display a dialog displaying information on a new version and offering to display the release notes
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            NewVersion newVersionFragment = newInstance();
            newVersionFragment.show(fm, TAG);
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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new NewVersion dialog instance
     * 
     * @return a new NewVersion instance
     */
    @NonNull
    private static NewVersion newInstance() {
        NewVersion f = new NewVersion();
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.upgrade_title);
        // shoe-horned OAuth 2 migration
        Preferences prefs = App.getPreferences(getContext());
        Server server = prefs.getServer();
        String message = getString(R.string.upgrade_message);
        if (server.getAuthentication() != Auth.OAUTH2 && Urls.DEFAULT_API_NAME.equals(server.getApiName())) {
            // migration necessary
            builder.setPositiveButton(R.string.migrate_now, null);
            message += getString(R.string.upgrade_message_oauth2);
        }
        builder.setMessage(Util.fromHtml(message));
        builder.setNegativeButton(R.string.skip, (d, which) -> dismiss());
        builder.setNeutralButton(R.string.read_upgrade, (d, which) -> {
            dismiss();
            HelpViewer.start(activity, R.string.help_upgrade);
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface d) -> {
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener((View v) -> {
                try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                    API api = db.getCurrentAPI();
                    db.setAPIDescriptors(api.id, api.name, api.url, api.readonlyurl, api.notesurl, Auth.OAUTH2);
                }
                Authorize.startForResult(activity, null);
                positive.setEnabled(false);
            });
        });
        return dialog;
    }
}
