package de.blau.android.dialogs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Simple alert dialog with an OK button that does nothing
 * 
 * @author simon
 *
 */
public class ErrorAlert extends DialogFragment {

    private static final String TITLE = "title";

    private static final String MESSAGE = "message";

    private static final String ORIGINAL_MESSAGE = "original_message";

    private static final String DEBUG_TAG = ErrorAlert.class.getSimpleName();

    private int    titleId;
    private int    messageId;
    private String originalMessage;

    /**
     * Display a simple alert dialog with an OK button that does nothing
     * 
     * @param activity the calling Activity
     * @param errorCode the error code
     */
    public static void showDialog(@NonNull FragmentActivity activity, int errorCode) {
        showDialog(activity, errorCode, null);
    }

    /**
     * Display a simple alert dialog with an OK button that does nothing
     * 
     * @param activity the calling Activity
     * @param errorCode the error code
     * @param msg a message
     */
    public static void showDialog(@NonNull FragmentActivity activity, int errorCode, @Nullable String msg) {
        dismissDialog(activity, errorCode);

        FragmentManager fm = activity.getSupportFragmentManager();
        ErrorAlert alertDialogFragment = newInstance(errorCode, msg);
        try {
            if (alertDialogFragment != null) {
                alertDialogFragment.show(fm, getTag(errorCode));
            } else {
                Log.e(DEBUG_TAG, "Unable to create dialog for value " + errorCode);
            }
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     * @param errorCode the errorCode the dialog was for
     */
    private static void dismissDialog(@NonNull FragmentActivity activity, int errorCode) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(getTag(errorCode));
        try {
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     * Map error codes to tags
     * 
     * @param errorCode the error code
     * @return a tag
     */
    @Nullable
    private static String getTag(int errorCode) {
        switch (errorCode) {
        case ErrorCodes.NO_LOGIN_DATA:
            return "alert_no_login_data";
        case ErrorCodes.NO_CONNECTION:
            return "alert_no_connection";
        case ErrorCodes.SSL_HANDSHAKE:
            return "ssl_handshake_failed";
        case ErrorCodes.UPLOAD_PROBLEM:
            return "alert_upload_problem";
        case ErrorCodes.BAD_REQUEST:
            return "alert_bad_request";
        case ErrorCodes.DATA_CONFLICT:
            return "alert_data_conflict";
        case ErrorCodes.API_OFFLINE:
            return "alert_api_offline";
        case ErrorCodes.OUT_OF_MEMORY:
            return "alert_out_of_memory";
        case ErrorCodes.OUT_OF_MEMORY_DIRTY:
            return "alert_out_of_memory_dirty";
        case ErrorCodes.INVALID_DATA_RECEIVED:
            return "alert_invalid_data_received";
        case ErrorCodes.INVALID_DATA_READ:
            return "alert_invalid_data_read";
        case ErrorCodes.FILE_WRITE_FAILED:
            return "alert_file_write_failed";
        case ErrorCodes.NAN:
            return "alert_nan";
        case ErrorCodes.INVALID_BOUNDING_BOX:
            return "invalid_bounding_box";
        case ErrorCodes.BOUNDING_BOX_TOO_LARGE:
            return "bounding_box_too_large";
        case ErrorCodes.INVALID_LOGIN:
            return "invalid_login";
        case ErrorCodes.NOT_FOUND:
            return "not_found";
        case ErrorCodes.UNKNOWN_ERROR:
            return "unknown";
        case ErrorCodes.NO_DATA:
            return "no_data";
        }
        return null;
    }

    /**
     * Get a new instance of an ErrorAlert dialog
     * 
     * @param errorCode the error code
     * @param msg an optional message
     * @return a new instance of an ErrorAlert dialog or null
     */
    @Nullable
    private static ErrorAlert newInstance(int errorCode, @Nullable String msg) {
        switch (errorCode) {
        case ErrorCodes.NO_LOGIN_DATA:
            return createNewInstance(R.string.no_login_data_title, R.string.no_login_data_message, msg);
        case ErrorCodes.NO_CONNECTION:
            return createNewInstance(R.string.no_connection_title, R.string.no_connection_message, msg);
        case ErrorCodes.SSL_HANDSHAKE:
            return createNewInstance(R.string.no_connection_title, R.string.ssl_handshake_failed, msg);
        case ErrorCodes.UPLOAD_PROBLEM:
            return createNewInstance(R.string.upload_problem_title, R.string.upload_problem_message, msg);
        case ErrorCodes.BAD_REQUEST:
            return createNewInstance(R.string.upload_problem_title, R.string.bad_request_message, msg);
        case ErrorCodes.DATA_CONFLICT:
            return createNewInstance(R.string.data_conflict_title, R.string.data_conflict_message, msg);
        case ErrorCodes.API_OFFLINE:
            return createNewInstance(R.string.api_offline_title, R.string.api_offline_message, msg);
        case ErrorCodes.OUT_OF_MEMORY:
            return createNewInstance(R.string.out_of_memory_title, R.string.out_of_memory_message, msg);
        case ErrorCodes.OUT_OF_MEMORY_DIRTY:
            return createNewInstance(R.string.out_of_memory_title, R.string.out_of_memory_dirty_message, msg);
        case ErrorCodes.INVALID_DATA_RECEIVED:
            return createNewInstance(R.string.invalid_data_received_title, R.string.invalid_data_received_message, msg);
        case ErrorCodes.INVALID_DATA_READ:
            return createNewInstance(R.string.invalid_data_read_title, R.string.invalid_data_read_message, msg);
        case ErrorCodes.FILE_WRITE_FAILED:
            return createNewInstance(R.string.file_write_failed_title, R.string.file_write_failed_message, msg);
        case ErrorCodes.NAN:
            return createNewInstance(R.string.location_nan_title, R.string.location_nan_message, msg);
        case ErrorCodes.INVALID_BOUNDING_BOX:
            return createNewInstance(R.string.invalid_bounding_box_title, R.string.invalid_bounding_box_message, msg);
        case ErrorCodes.BOUNDING_BOX_TOO_LARGE:
            return createNewInstance(R.string.bounding_box_too_large_title, R.string.bounding_box_too_large_message, msg);
        case ErrorCodes.INVALID_LOGIN:
            return createNewInstance(R.string.wrong_login_data_title, R.string.wrong_login_data_message, msg);
        case ErrorCodes.NOT_FOUND:
            return createNewInstance(R.string.not_found_title, R.string.not_found_message, msg);
        case ErrorCodes.UNKNOWN_ERROR:
            return createNewInstance(R.string.unknown_error_title, R.string.unknown_error_message, msg);
        case ErrorCodes.NO_DATA:
            return createNewInstance(R.string.no_data_title, R.string.no_data_message, msg);
        }
        return null;
    }

    /**
     * Create a new instance of an ErrorAlert dialog
     * 
     * @param titleId the title resource id
     * @param messageId the message resource id
     * @param msg an optional message
     * @return a new instance of an ErrorAlert dialog
     */
    @NonNull
    private static ErrorAlert createNewInstance(final int titleId, final int messageId, @Nullable String msg) {
        ErrorAlert f = new ErrorAlert();

        Bundle args = new Bundle();
        args.putSerializable(TITLE, titleId);
        args.putInt(MESSAGE, messageId);
        if (msg != null) {
            args.putString(ORIGINAL_MESSAGE, msg);
        }

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        titleId = (Integer) getArguments().getSerializable(TITLE);
        messageId = getArguments().getInt(MESSAGE);
        originalMessage = getArguments().getString(ORIGINAL_MESSAGE);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(titleId);
        if (messageId != 0) {
            String message = getString(messageId);
            if (originalMessage != null) {
                message = message + originalMessage;
            }
            builder.setMessage(Util.fromHtml(message));
        }
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.okay, doNothingListener);
        return builder.create();
    }
}
