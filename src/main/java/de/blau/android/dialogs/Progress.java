package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.util.CancelableDialogFragment;

/**
 * ProgressDialog can't be styled, this rolls its own.
 * 
 * @author simon
 *
 */
public class Progress extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Progress.class.getSimpleName().length());
    private static final String DEBUG_TAG = Progress.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TYPE    = "type";
    private static final String ARG_KEY = "arg";

    public static final int PROGRESS_LOADING                   = 1;
    public static final int PROGRESS_DOWNLOAD                  = 2;
    public static final int PROGRESS_DELETING                  = 3;
    public static final int PROGRESS_SEARCHING                 = 4;
    public static final int PROGRESS_SAVING                    = 5;
    public static final int PROGRESS_OAUTH                     = 6;
    public static final int PROGRESS_UPLOADING                 = 7;
    public static final int PROGRESS_PRESET                    = 8;
    public static final int PROGRESS_RUNNING                   = 9;
    public static final int PROGRESS_BUILDING_IMAGERY_DATABASE = 10;
    public static final int PROGRESS_QUERY_OAM                 = 11;
    public static final int PROGRESS_PRUNING                   = 12;
    public static final int PROGRESS_MIGRATION                 = 13;
    public static final int PROGRESS_LOADING_PRESET            = 14;
    public static final int PROGRESS_IMPORTING_FILE            = 15;
    public static final int PROGRESS_DOWNLOAD_TASKS            = 16;
    public static final int PROGRESS_DOWNLOAD_SEQUENCE         = 17;
    public static final int PROGRESS_DETERMINING_STATUS        = 18;
    public static final int PROGRESS_UPDATING                  = 19;

    private int    dialogType;
    private String messageArg;

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
        showDialog(activity, dialogType, null, tag);
    }

    /**
     * Display a progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show
     * @param arg optional argument to the message
     * @param tag a string to differentiate between multiple similar spinners
     */
    public static void showDialog(@NonNull FragmentActivity activity, int dialogType, @Nullable String arg, @Nullable String tag) {
        tag = getTag(dialogType) + (tag != null ? "-" + tag : "");
        dismissDialog(activity, dialogType, tag);

        FragmentManager fm = activity.getSupportFragmentManager();
        Progress progressDialogFragment = newInstance(dialogType, arg);
        progressDialogFragment.setCancelable(true);
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
    public static void dismissDialog(@Nullable FragmentActivity activity, int dialogType) {
        dismissDialog(activity, dialogType, null);
    }

    /**
     * Dismiss the progress spinner
     * 
     * @param activity the calling FragmentActivity
     * @param dialogType an int indicating which heading to show tag a string to differentiate between multiple similar
     *            spinners
     * @param tag a String identifying the dialog we want to dismiss or null
     */
    public static void dismissDialog(@Nullable FragmentActivity activity, int dialogType, @Nullable String tag) {
        if (activity == null) {
            Log.e(DEBUG_TAG, "dismissDialog called with null Activity");
            return;
        }
        tag = getTag(dialogType) + (tag != null ? "-" + tag : "");
        de.blau.android.dialogs.Util.dismissDialog(activity, tag);
    }

    /**
     * Dismiss all possible progress dialogs to stop them being recreated
     * 
     * @param activity the calling FragmentActivity
     */
    public static void dismissAll(@NonNull FragmentActivity activity) {
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
        dismissDialog(activity, PROGRESS_PRUNING);
        dismissDialog(activity, PROGRESS_MIGRATION);
        dismissDialog(activity, PROGRESS_LOADING_PRESET);
        dismissDialog(activity, PROGRESS_IMPORTING_FILE);
        dismissDialog(activity, PROGRESS_DOWNLOAD_TASKS);
        dismissDialog(activity, PROGRESS_DOWNLOAD_SEQUENCE);
        dismissDialog(activity, PROGRESS_DETERMINING_STATUS);
        dismissDialog(activity, PROGRESS_UPDATING);
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
        case PROGRESS_PRUNING:
            return "dialog_progress_pruning";
        case PROGRESS_MIGRATION:
            return "dialog_progress_migration";
        case PROGRESS_LOADING_PRESET:
            return "dialog_progress_loading_preset";
        case PROGRESS_IMPORTING_FILE:
            return "dialog_progress_importing_file";
        case PROGRESS_DOWNLOAD_TASKS:
            return "dialog_progress_download_tasks";
        case PROGRESS_DOWNLOAD_SEQUENCE:
            return "dialog_progress_download_sequence";
        case PROGRESS_DETERMINING_STATUS:
            return "dialog_progress_determining_status";
        case PROGRESS_UPDATING:
            return "dialog_progress_updating";
        default:
            Log.w(DEBUG_TAG, "Unknown dialog type " + dialogType);
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
    private static Progress newInstance(final int dialogType, @Nullable String arg) {
        Progress f = new Progress();

        Bundle args = new Bundle();
        args.putSerializable(TYPE, dialogType);
        args.putString(ARG_KEY, arg);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogType = de.blau.android.util.Util.getSerializeable(getArguments(), TYPE, Integer.class);
        messageArg = getArguments().getString(ARG_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return ProgressDialog.get(getActivity(), dialogType, messageArg);
    }
}
