package de.blau.android.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

public final class ProgressDialog {

    /**
     * Private constructor to stop instantiation
     */
    private ProgressDialog() {
        // private
    }

    /**
     * Factory like method to get new Instances of a ProgressDialog
     * 
     * @param ctx Android Context
     * @param dialogType determines title and heading
     * @return an ProgressDialog instance or null
     */
    @Nullable
    public static AlertDialog get(@NonNull Context ctx, int dialogType) {
        return get(ctx, dialogType, null);
    }

    /**
     * Factory like method to get new Instances of a ProgressDialog
     * 
     * @param ctx Android Context
     * @param dialogType determines title and heading
     * @param arg optional argument for the message
     * @return an ProgressDialog instance or null
     */
    @Nullable
    public static AlertDialog get(@NonNull Context ctx, int dialogType, @Nullable String arg) {
        int titleId = 0;
        int messageId = 0;
        switch (dialogType) {
        case Progress.PROGRESS_LOADING:
            titleId = R.string.progress_title;
            messageId = R.string.progress_message;
            break;
        case Progress.PROGRESS_DOWNLOAD:
            titleId = R.string.progress_title;
            messageId = R.string.progress_download_message;
            break;
        case Progress.PROGRESS_DELETING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_deleting_message;
            break;
        case Progress.PROGRESS_SEARCHING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_searching_message;
            break;
        case Progress.PROGRESS_SAVING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_saving_message;
            break;
        case Progress.PROGRESS_OAUTH:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_oauth;
            break;
        case Progress.PROGRESS_UPLOADING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_uploading_message;
            break;
        case Progress.PROGRESS_RESOURCE:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_resource_message;
            break;
        case Progress.PROGRESS_RUNNING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_running_message;
            break;
        case Progress.PROGRESS_BUILDING_IMAGERY_DATABASE:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_building_imagery_database;
            break;
        case Progress.PROGRESS_QUERY_OAM:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_query_oam;
            break;
        case Progress.PROGRESS_PRUNING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_pruning;
            break;
        case Progress.PROGRESS_MIGRATION:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_migration;
            break;
        case Progress.PROGRESS_LOADING_PRESET:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_loading_preset_message;
            break;
        case Progress.PROGRESS_IMPORTING_FILE:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_importing_file_message;
            break;
        case Progress.PROGRESS_DOWNLOAD_TASKS:
            titleId = R.string.progress_title;
            messageId = R.string.progress_download_tasks_message;
            break;
        case Progress.PROGRESS_DOWNLOAD_SEQUENCE:
            titleId = R.string.progress_title;
            messageId = R.string.progress_download_sequence_message;
            break;
        case Progress.PROGRESS_DETERMINING_STATUS:
            titleId = R.string.progress_title;
            messageId = R.string.progress_determining_status_message;
            break;
        case Progress.PROGRESS_UPDATING:
            titleId = R.string.progress_title;
            messageId = R.string.progress_updating_message;
            break;
        default:
            return null;
        }

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

        Builder builder = ThemeUtils.getAlertDialogBuilder(ctx);
        builder.setTitle(titleId);

        View layout = inflater.inflate(R.layout.progress, null);
        TextView message = (TextView) layout.findViewById(R.id.progressMessage);
        message.setText(arg != null ? ctx.getString(messageId, arg) : ctx.getString(messageId));
        ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null) {
            progressBar.getIndeterminateDrawable().setColorFilter(BlendModeColorFilterCompat
                    .createBlendModeColorFilterCompat(ThemeUtils.getStyleAttribColorValue(ctx, R.attr.colorAccent, 0), BlendModeCompat.SRC_IN));
        }
        builder.setView(layout);
        return builder.create();
    }
}
