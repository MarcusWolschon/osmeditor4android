package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Simple alert dialog with an OK button that does nothing
 * 
 * @author simon
 *
 */
public class ErrorAlert extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ErrorAlert.class.getSimpleName().length());
    private static final String DEBUG_TAG = ErrorAlert.class.getSimpleName().substring(0, TAG_LEN);

    private static final Map<Integer, String>  TAGS     = new HashMap<>();
    private static final Map<Integer, Integer> TITLES   = new HashMap<>();
    private static final Map<Integer, Integer> MESSAGES = new HashMap<>();
    static {
        TAGS.put(ErrorCodes.NO_LOGIN_DATA, "alert_no_login_data");
        TITLES.put(ErrorCodes.NO_LOGIN_DATA, R.string.no_login_data_title);
        MESSAGES.put(ErrorCodes.NO_LOGIN_DATA, R.string.no_login_data_message);
        TAGS.put(ErrorCodes.NO_CONNECTION, "alert_no_connection");
        TITLES.put(ErrorCodes.NO_CONNECTION, R.string.no_connection_title);
        MESSAGES.put(ErrorCodes.NO_CONNECTION, R.string.no_connection_message);
        TAGS.put(ErrorCodes.SSL_HANDSHAKE, "ssl_handshake_failed");
        TITLES.put(ErrorCodes.SSL_HANDSHAKE, R.string.no_connection_title);
        MESSAGES.put(ErrorCodes.SSL_HANDSHAKE, R.string.ssl_handshake_failed);
        TAGS.put(ErrorCodes.UPLOAD_PROBLEM, "alert_upload_problem");
        TITLES.put(ErrorCodes.UPLOAD_PROBLEM, R.string.upload_problem_title);
        MESSAGES.put(ErrorCodes.UPLOAD_PROBLEM, R.string.upload_problem_message);
        TAGS.put(ErrorCodes.BAD_REQUEST, "alert_bad_request");
        TITLES.put(ErrorCodes.BAD_REQUEST, R.string.upload_problem_title);
        MESSAGES.put(ErrorCodes.BAD_REQUEST, R.string.bad_request_message);
        TAGS.put(ErrorCodes.DATA_CONFLICT, "alert_data_conflict");
        TITLES.put(ErrorCodes.DATA_CONFLICT, R.string.data_conflict_title);
        MESSAGES.put(ErrorCodes.DATA_CONFLICT, R.string.data_conflict_message);
        TAGS.put(ErrorCodes.API_OFFLINE, "alert_api_offline");
        TITLES.put(ErrorCodes.API_OFFLINE, R.string.api_offline_title);
        MESSAGES.put(ErrorCodes.API_OFFLINE, R.string.api_offline_message);
        TAGS.put(ErrorCodes.OUT_OF_MEMORY, "alert_out_of_memory");
        TITLES.put(ErrorCodes.OUT_OF_MEMORY, R.string.out_of_memory_title);
        MESSAGES.put(ErrorCodes.OUT_OF_MEMORY, R.string.out_of_memory_message);
        TAGS.put(ErrorCodes.OUT_OF_MEMORY_DIRTY, "alert_out_of_memory_dirty");
        TITLES.put(ErrorCodes.OUT_OF_MEMORY_DIRTY, R.string.out_of_memory_title);
        MESSAGES.put(ErrorCodes.OUT_OF_MEMORY_DIRTY, R.string.out_of_memory_dirty_message);
        TAGS.put(ErrorCodes.INVALID_DATA_RECEIVED, "alert_invalid_data_received");
        TITLES.put(ErrorCodes.INVALID_DATA_RECEIVED, R.string.invalid_data_received_title);
        MESSAGES.put(ErrorCodes.INVALID_DATA_RECEIVED, R.string.invalid_data_received_message);
        TAGS.put(ErrorCodes.INVALID_DATA_READ, "alert_invalid_data_read");
        TITLES.put(ErrorCodes.INVALID_DATA_READ, R.string.invalid_data_read_title);
        MESSAGES.put(ErrorCodes.INVALID_DATA_READ, R.string.invalid_data_read_message);
        TAGS.put(ErrorCodes.FILE_WRITE_FAILED, "alert_file_write_failed");
        TITLES.put(ErrorCodes.FILE_WRITE_FAILED, R.string.file_write_failed_title);
        MESSAGES.put(ErrorCodes.FILE_WRITE_FAILED, R.string.file_write_failed_message);
        TAGS.put(ErrorCodes.NAN, "alert_nan");
        TITLES.put(ErrorCodes.NAN, R.string.location_nan_title);
        MESSAGES.put(ErrorCodes.NAN, R.string.location_nan_message);
        TAGS.put(ErrorCodes.INVALID_BOUNDING_BOX, "invalid_bounding_box");
        TITLES.put(ErrorCodes.INVALID_BOUNDING_BOX, R.string.invalid_bounding_box_title);
        MESSAGES.put(ErrorCodes.INVALID_BOUNDING_BOX, R.string.invalid_bounding_box_message);
        TAGS.put(ErrorCodes.BOUNDING_BOX_TOO_LARGE, "bounding_box_too_large");
        TITLES.put(ErrorCodes.BOUNDING_BOX_TOO_LARGE, R.string.bounding_box_too_large_title);
        MESSAGES.put(ErrorCodes.BOUNDING_BOX_TOO_LARGE, R.string.bounding_box_too_large_message);
        TAGS.put(ErrorCodes.INVALID_LOGIN, "invalid_login");
        TITLES.put(ErrorCodes.INVALID_LOGIN, R.string.wrong_login_data_title);
        MESSAGES.put(ErrorCodes.INVALID_LOGIN, R.string.wrong_login_data_message);
        TAGS.put(ErrorCodes.NOT_FOUND, "not_found");
        TITLES.put(ErrorCodes.NOT_FOUND, R.string.not_found_title);
        MESSAGES.put(ErrorCodes.NOT_FOUND, R.string.not_found_message);
        TAGS.put(ErrorCodes.UNKNOWN_ERROR, "unknown");
        TITLES.put(ErrorCodes.UNKNOWN_ERROR, R.string.unknown_error_title);
        MESSAGES.put(ErrorCodes.UNKNOWN_ERROR, R.string.unknown_error_message);
        TAGS.put(ErrorCodes.NO_DATA, "no_data");
        TITLES.put(ErrorCodes.NO_DATA, R.string.no_data_title);
        MESSAGES.put(ErrorCodes.NO_DATA, R.string.no_data_message);
        TAGS.put(ErrorCodes.REQUIRED_FEATURE_MISSING, "required_feature_missing");
        TITLES.put(ErrorCodes.REQUIRED_FEATURE_MISSING, R.string.required_feature_missing_title);
        MESSAGES.put(ErrorCodes.REQUIRED_FEATURE_MISSING, R.string.required_feature_missing_message);
        TAGS.put(ErrorCodes.APPLYING_OSC_FAILED, "applying_osc_failed");
        TITLES.put(ErrorCodes.APPLYING_OSC_FAILED, R.string.applying_osc_failed_title);
        MESSAGES.put(ErrorCodes.APPLYING_OSC_FAILED, R.string.applying_osc_failed_message);
        TAGS.put(ErrorCodes.CORRUPTED_DATA, "alert_corrupt_data");
        TITLES.put(ErrorCodes.CORRUPTED_DATA, R.string.corrupted_data_title);
        MESSAGES.put(ErrorCodes.CORRUPTED_DATA, R.string.corrupted_data_message);
        TAGS.put(ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED, "download_limit_exceeded");
        TITLES.put(ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED, R.string.download_limit_title);
        MESSAGES.put(ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED, R.string.download_limit_message);
        TAGS.put(ErrorCodes.UPLOAD_LIMIT_EXCEEDED, "upload_limit_exceeded");
        TITLES.put(ErrorCodes.UPLOAD_LIMIT_EXCEEDED, R.string.upload_limit_title);
        MESSAGES.put(ErrorCodes.UPLOAD_LIMIT_EXCEEDED, R.string.upload_limit_message);
        TAGS.put(ErrorCodes.DUPLICATE_TAG_KEY, "alert_duplicate_tag_key");
        TITLES.put(ErrorCodes.DUPLICATE_TAG_KEY, R.string.duplicate_tag_key_title);
        MESSAGES.put(ErrorCodes.DUPLICATE_TAG_KEY, R.string.duplicate_tag_key_message);
        TAGS.put(ErrorCodes.UPLOAD_BOUNDING_BOX_TOO_LARGE, "alert_bounding_box_too_large");
        TITLES.put(ErrorCodes.UPLOAD_BOUNDING_BOX_TOO_LARGE, R.string.upload_bounding_box_too_large_title);
        MESSAGES.put(ErrorCodes.UPLOAD_BOUNDING_BOX_TOO_LARGE, R.string.upload_bounding_box_too_large_message);
        TAGS.put(ErrorCodes.TOO_MANY_WAY_NODES, "alert_too_many_way_nodes");
        TITLES.put(ErrorCodes.TOO_MANY_WAY_NODES, R.string.attempt_to_add_too_many_way_nodes_title);
        MESSAGES.put(ErrorCodes.TOO_MANY_WAY_NODES, R.string.attempt_to_add_too_many_way_nodes_message);
        TAGS.put(ErrorCodes.UPLOAD_WAY_NEEDS_ONE_NODE, "alert_way_needs_one_node");
        TITLES.put(ErrorCodes.UPLOAD_WAY_NEEDS_ONE_NODE, R.string.upload_way_needs_one_node_title);
        MESSAGES.put(ErrorCodes.UPLOAD_WAY_NEEDS_ONE_NODE, R.string.upload_way_needs_one_node_message);
        TAGS.put(ErrorCodes.UNAVAILABLE, "alert_service_unavailable");
        TITLES.put(ErrorCodes.UNAVAILABLE, R.string.upload_unavailable_title);
        MESSAGES.put(ErrorCodes.UNAVAILABLE, R.string.upload_unavailable_message);
    }

    private static final String TITLE            = "title";
    private static final String MESSAGE          = "message";
    private static final String ORIGINAL_MESSAGE = "original_message";

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
     * @param result a ReadAsyncResult instance
     */
    public static void showDialog(@NonNull FragmentActivity activity, AsyncResult result) {
        showDialog(activity, result.getCode(), result.getMessage());
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
            String tag = getTag(errorCode);
            if (alertDialogFragment != null && tag != null) {
                if (fm.findFragmentByTag(tag) != null) {
                    Log.w(DEBUG_TAG, "dialog still being displayed " + tag);
                    return;
                }
                alertDialogFragment.show(fm, tag);
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
        String tag = getTag(errorCode);
        if (tag != null) {
            de.blau.android.dialogs.Util.dismissDialog(activity, tag);
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
        return TAGS.get(errorCode);
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
        if (TAGS.containsKey(errorCode)) {
            return createNewInstance(TITLES.get(errorCode), MESSAGES.get(errorCode), msg);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        titleId = Util.getSerializeable(getArguments(), TITLE, Integer.class);
        messageId = getArguments().getInt(MESSAGE);
        originalMessage = getArguments().getString(ORIGINAL_MESSAGE);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = ThemeUtils.getAlertDialogBuilder(getActivity());
        builder.setIcon(ThemeUtils.getResIdFromAttribute(getActivity(), R.attr.alert_dialog));
        builder.setTitle(titleId);
        if (messageId != 0) {
            String message = getString(messageId);
            if (originalMessage != null) {
                message = message + "<p/><i>" + originalMessage + "</i>";
            }
            builder.setMessage(Util.fromHtml(message));
        }
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.okay, doNothingListener);
        return builder.create();
    }
}
