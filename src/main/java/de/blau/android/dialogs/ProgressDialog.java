package de.blau.android.dialogs;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

public class ProgressDialog {

    public static AlertDialog get(Context ctx, int dialogType) {
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
        case Progress.PROGRESS_PRESET:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_preset_message;
            break;
        case Progress.PROGRESS_RUNNING:
            titleId = R.string.progress_general_title;
            messageId = R.string.progress_running_message;
            break;
        case Progress.PROGRESS_BUILDING_IMAGERY_DATABASE:
            titleId = R.string.progress_general_title;
            messageId = R.string.toast_building_imagery_database;
            break;
        default:
            return null;
        }

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

        Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(titleId);

        View layout = inflater.inflate(R.layout.progress, null);
        TextView message = (TextView) layout.findViewById(R.id.progressMessage);
        message.setText(messageId);
        ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null) {
            PorterDuff.Mode mode = android.graphics.PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                mode = android.graphics.PorterDuff.Mode.MULTIPLY; // ugly but at least it animates
            }
            progressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.getStyleAttribColorValue(ctx, R.attr.colorAccent, 0), mode);
        }
        builder.setView(layout);

        return builder.create();
    }
}
