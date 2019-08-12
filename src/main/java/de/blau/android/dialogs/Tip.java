package de.blau.android.dialogs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Simple alert dialog with an OK button that displays a text once
 * 
 * @author simon
 *
 */
public class Tip extends ImmersiveDialogFragment {

    private static final String TAG = "tip";

    private static final String MESSAGE_KEY  = "message";
    private static final String OPTIONAL_KEY = "optional";

    private static final String DEBUG_TAG = Tip.class.getSimpleName();

    private static SharedPreferences prefs;

    private int     messageId;
    private boolean optional;

    /**
     * Display a simple alert dialog with an OK button that displays a text once
     * 
     * This variant allows setting a preference that will turn all optional messages off
     * 
     * @param activity the calling Activity
     * @param prefId res id for the preference key
     * @param messageId res id for the message text
     */
    public static void showOptionalDialog(@NonNull FragmentActivity activity, int prefId, int messageId) {
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(activity.getString(prefId), true) && prefs.getBoolean(activity.getString(R.string.tip_show_key), true)) {
            prefs.edit().putBoolean(activity.getString(prefId), false).commit();
            de.blau.android.dialogs.Util.dismissDialog(activity, TAG);

            FragmentManager fm = activity.getSupportFragmentManager();
            Tip alertDialogFragment = newInstance(messageId, true);
            try {
                alertDialogFragment.show(fm, TAG);
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, "showOptionalDialog", isex);
            }
        }
    }

    /**
     * Display a simple alert dialog with an OK button that displays a text once
     * 
     * @param activity the calling Activity
     * @param prefId res id for the preference key
     * @param messageId res id for the message text
     */
    public static void showDialog(@NonNull FragmentActivity activity, int prefId, int messageId) {
        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(activity.getString(prefId), true)) {
            prefs.edit().putBoolean(activity.getString(prefId), false).commit();
            de.blau.android.dialogs.Util.dismissDialog(activity, TAG);

            FragmentManager fm = activity.getSupportFragmentManager();
            Tip alertDialogFragment = newInstance(messageId, false);
            try {
                alertDialogFragment.show(fm, TAG);
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, "showDialog", isex);
            }
        }
    }

    /**
     * Create a new instance of a Tip dialog
     * 
     * @param messageId the message resource id
     * @param optional if true a check box will be shown that allows turing optional messages off
     * @return a new instance of an ErrorAlert dialog
     */
    @NonNull
    private static Tip newInstance(final int messageId, boolean optional) {
        Tip f = new Tip();

        Bundle args = new Bundle();
        args.putInt(MESSAGE_KEY, messageId);
        args.putBoolean(OPTIONAL_KEY, optional);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageId = getArguments().getInt(MESSAGE_KEY);
        optional = getArguments().getBoolean(OPTIONAL_KEY);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.tip, null);
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.lightbulb_dialog));
        builder.setTitle(R.string.tip_title);
        TextView message = (TextView) layout.findViewById(R.id.tip_message);
        message.setText((Util.fromHtml(getString(messageId))));

        if (optional) {
            CheckBox check = (CheckBox) layout.findViewById(R.id.tip_check);
            check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    prefs.edit().putBoolean(getString(R.string.tip_show_key), isChecked).commit();
                }
            });
        } else {
            View checkContainer = layout.findViewById(R.id.tip_check_container);
            checkContainer.setVisibility(View.GONE);
        }

        builder.setView(layout);

        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.okay, doNothingListener);
        return builder.create();
    }
}
