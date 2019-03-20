package de.blau.android.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import de.blau.android.util.ImmersiveDialogFragment;

/**
 * ProgressDialog can't be styled, this rolls its own.
 * 
 * @author simon
 *
 */
public class Progress extends ImmersiveDialogFragment {

    private static final String TYPE = "type";

    private static final String DEBUG_TAG = Progress.class.getSimpleName();

    public static final int PROGRESS_LOADING = 1;

    public static final int PROGRESS_DOWNLOAD = 2;

    public static final int PROGRESS_DELETING = 3;

    public static final int PROGRESS_SEARCHING = 4;

    public static final int PROGRESS_SAVING = 5;

    public static final int PROGRESS_OAUTH = 6;

    public static final int PROGRESS_UPLOADING = 7;

    public static final int PROGRESS_PRESET = 8;

    public static final int PROGRESS_RUNNING = 9;

    public static final int PROGRESS_BUILDING_IMAGERY_DATABASE = 10;

    public static final int PROGRESS_QUERY_OAM = 11;

    private int dialogType;

    /**
     * Display a progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show
     */
    public static void showDialog(@NonNull FragmentActivity activity, int dialogType) {
        showDialog(activity, dialogType, null);
    }

    /**
     * Display a progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show
     * @param tag a string to differentiate between multiple similar spinners
     */
    public static void showDialog(@NonNull FragmentActivity activity, int dialogType, @Nullable String tag) {
        tag = getTag(dialogType) + (tag != null ? "-" + tag : "");
        dismissDialog(activity, dialogType, tag);

        FragmentManager fm = activity.getSupportFragmentManager();
        Progress progressDialogFragment = newInstance(dialogType);
        try {

            progressDialogFragment.show(fm, tag);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show
     */
    public static void dismissDialog(@NonNull FragmentActivity activity, int dialogType) {
        dismissDialog(activity, dialogType, null);
    }

    /**
     * Dismiss the progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show tag a string to differentiate between multiple similar
     *            spinners
     */
    public static void dismissDialog(@NonNull FragmentActivity activity, int dialogType, @Nullable String tag) {
        tag = getTag(dialogType) + (tag != null ? "-" + tag : "");
        de.blau.android.dialogs.Util.dismissDialog(activity, tag);
    }

    /**
     * Dismiss all possible progress dialogs to stop them being recreated
     * 
     * @param activity the calling FragmentActivity
     */
    public static void dismissAll(FragmentActivity activity) {
        dismissDialog(activity, PROGRESS_LOADING);
        dismissDialog(activity, PROGRESS_DOWNLOAD);
        dismissDialog(activity, PROGRESS_DELETING);
        dismissDialog(activity, PROGRESS_SEARCHING);
        dismissDialog(activity, PROGRESS_SAVING);
        dismissDialog(activity, PROGRESS_OAUTH);
        dismissDialog(activity, PROGRESS_UPLOADING);
        dismissDialog(activity, PROGRESS_PRESET);
        dismissDialog(activity, PROGRESS_RUNNING);
        dismissDialog(activity, PROGRESS_BUILDING_IMAGERY_DATABASE);
        dismissDialog(activity, PROGRESS_QUERY_OAM);
    }

    /**
     * Get the tag for a specific progress spinner version
     * 
     * @param dialogType an int indicating the title
     * @return a String or null
     */
    @Nullable
    private static String getTag(int dialogType) {
        switch (dialogType) {
        case PROGRESS_LOADING:
            return "dialog_progress_loading";
        case PROGRESS_DOWNLOAD:
            return "dialog_progress_download";
        case PROGRESS_DELETING:
            return "dialog_progress_deleting";
        case PROGRESS_SEARCHING:
            return "dialog_progress_searching";
        case PROGRESS_SAVING:
            return "dialog_progress_saving";
        case PROGRESS_OAUTH:
            return "dialog_progress_oauth";
        case PROGRESS_UPLOADING:
            return "dialog_progress_uploading";
        case PROGRESS_PRESET:
            return "dialog_progress_preset";
        case PROGRESS_RUNNING:
            return "dialog_progress_running";
        case PROGRESS_BUILDING_IMAGERY_DATABASE:
            return "dialog_progress_building_imagery_database";
        case PROGRESS_QUERY_OAM:
            return "dialog_progress_query_oam";
        }
        return null;
    }

    /**
     * Get a new instance of a progress spinner
     * 
     * @param dialogType an int indicating the title
     * @return a Progress instance
     */
    @NonNull
    private static Progress newInstance(final int dialogType) {
        Progress f = new Progress();

        Bundle args = new Bundle();
        args.putSerializable(TYPE, dialogType);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogType = (Integer) getArguments().getSerializable(TYPE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return ProgressDialog.get(getActivity(), dialogType);
    }
}
