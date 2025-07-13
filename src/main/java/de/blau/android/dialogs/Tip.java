package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import de.blau.android.R;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Simple alert dialog with an OK button that displays a text once
 * 
 * @author simon
 *
 */
public class Tip extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Tip.class.getSimpleName().length());
    private static final String DEBUG_TAG = Tip.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "tip";

    private static final String MESSAGE_IDS_KEY = "messages";
    private static final String OPTIONALS_KEY   = "optionals";
    private static final String PREF_IDS_KEY    = "prefs";

    private List<Integer> messageIds;
    private List<Integer> prefIds;
    private boolean[]     optionals;

    private int currentTip = 0;

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(activity.getString(prefId), true) && prefs.getBoolean(activity.getString(R.string.tip_show_key), true)) {
            de.blau.android.dialogs.Util.dismissDialog(activity, TAG);

            FragmentManager fm = activity.getSupportFragmentManager();
            Tip alertDialogFragment = newInstance(Util.wrapInList(prefId), Util.wrapInList(messageId), new boolean[] { true });
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (prefs.getBoolean(activity.getString(prefId), true)) {
            de.blau.android.dialogs.Util.dismissDialog(activity, TAG);

            FragmentManager fm = activity.getSupportFragmentManager();
            Tip alertDialogFragment = newInstance(Util.wrapInList(prefId), Util.wrapInList(messageId), new boolean[] { false });
            try {
                alertDialogFragment.show(fm, TAG);
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, "showDialog", isex);
            }
        }
    }

    /**
     * Display a simple alert dialog with an OK button that displays a text once
     * 
     * @param activity the calling Activity
     * @param prefIds res ids for the preference keys
     * @param messageIds res ids for the message texts
     */
    public static void showDialog(@NonNull FragmentActivity activity, List<Integer> prefIds, List<Integer> messageIds) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        for (Integer prefId : new ArrayList<>(prefIds)) {
            if (!prefs.getBoolean(activity.getString(prefId), true)) {
                int index = prefIds.indexOf(prefId);
                prefIds.remove(index);
                messageIds.remove(index);
            }
        }
        if (!prefIds.isEmpty()) {
            de.blau.android.dialogs.Util.dismissDialog(activity, TAG);

            FragmentManager fm = activity.getSupportFragmentManager();
            Tip alertDialogFragment = newInstance(prefIds, messageIds, new boolean[prefIds.size()]);
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
     * @param prefIds res ids for the preference keys
     * @param messageIds the message resource ids
     * @param optionals if value is true a check box will be shown that allows turning optional messages off
     * @return a new instance of an ErrorAlert dialog
     */
    @NonNull
    private static Tip newInstance(final List<Integer> prefIds, final List<Integer> messageIds, boolean[] optionals) {
        int count = prefIds.size();
        if (!(count == messageIds.size() && count == optionals.length)) {
            Log.e(DEBUG_TAG, "All arguments must have same size: " + count);
            throw new IllegalArgumentException("All arguments must have same size");
        }
        Tip f = new Tip();
        Bundle args = new Bundle();
        args.putIntegerArrayList(PREF_IDS_KEY, new ArrayList<>(prefIds));
        args.putIntegerArrayList(MESSAGE_IDS_KEY, new ArrayList<>(messageIds));
        args.putBooleanArray(OPTIONALS_KEY, optionals);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            prefIds = savedInstanceState.getIntegerArrayList(PREF_IDS_KEY);
            messageIds = savedInstanceState.getIntegerArrayList(MESSAGE_IDS_KEY);
            optionals = savedInstanceState.getBooleanArray(OPTIONALS_KEY);
            Log.d(DEBUG_TAG, "restoring from saved state");
        } else {
            prefIds = getArguments().getIntegerArrayList(PREF_IDS_KEY);
            messageIds = getArguments().getIntegerArrayList(MESSAGE_IDS_KEY);
            optionals = getArguments().getBooleanArray(OPTIONALS_KEY);
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.tip, null);
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.lightbulb_dialog));
        builder.setTitle(R.string.tip_title);
        builder.setView(layout);
        display(layout);
        builder.setPositiveButton(R.string.okay, null);
        final AlertDialog alertDialog = builder.create();
        if (prefIds.size() > 1) {
            alertDialog.setOnShowListener(dialog -> {
                final Button positive = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                positive.setText(R.string.next);
                positive.setOnClickListener(v -> {
                    currentTip++;
                    display(layout);
                    if (currentTip == prefIds.size() - 1) {
                        positive.setText(R.string.okay);
                        positive.setOnClickListener(v2 -> alertDialog.dismiss());
                    }
                });
            });
        }

        return alertDialog;
    }

    /**
     * Display the content for "currentTip"
     * 
     * @param layout the Layout for the dialog
     */
    private void display(@NonNull final LinearLayout layout) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putBoolean(getActivity().getString(prefIds.get(currentTip)), false).commit();
        TextView message = (TextView) layout.findViewById(R.id.tip_message);
        message.setText((Util.fromHtml(getString(messageIds.get(currentTip)))));
        if (optionals[currentTip]) {
            CheckBox check = (CheckBox) layout.findViewById(R.id.tip_check);
            check.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(getString(R.string.tip_show_key), isChecked).commit());
        } else {
            View checkContainer = layout.findViewById(R.id.tip_check_container);
            checkContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Reset the status of a specific tip
     * 
     * @param cxt
     * @param tipId
     */
    public static void resetTip(@NonNull Context cxt, int tipId) {
        PreferenceManager.getDefaultSharedPreferences(cxt).edit().putBoolean(cxt.getString(tipId), true).commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(PREF_IDS_KEY, new ArrayList<>(prefIds));
        outState.putIntegerArrayList(MESSAGE_IDS_KEY, new ArrayList<>(messageIds));
        outState.putBooleanArray(OPTIONALS_KEY, optionals);
    }
}
