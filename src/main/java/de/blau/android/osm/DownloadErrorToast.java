package de.blau.android.osm;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.Snack;

public class DownloadErrorToast implements Runnable {
    final int     code;
    final String  message;
    final Context context;

    /**
     * 
     * 
     * @param context an Android Context, if null of not an Activity nothing will happen
     * @param code the error code
     * @param message the message to display
     */
    DownloadErrorToast(@Nullable Context context, int code, @NonNull String message) {
        this.code = code;
        this.message = message;
        this.context = context;
    }

    @Override
    public void run() {
        if (context instanceof Activity) {
            try {
                Snack.barError((Activity) context, context.getResources().getString(R.string.toast_download_failed, code, message));
            } catch (Exception ex) {
                // do nothing ... this is stop bugs in the Android format parsing crashing the app, report the error
                // because it is likely caused by a translation error
                ACRAHelper.nocrashReport(ex, ex.getMessage());
            }
        }
    }
}
