package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.StorageDelegator;

public class UploadChecker extends Worker {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UploadChecker.class.getSimpleName().length());
    private static final String DEBUG_TAG = UploadChecker.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "UPLOAD_CHECKER";

    private final Context context;

    /**
     * Create a new UploadChecker
     * 
     * @param context an Android Context
     * @param params WorkerParams
     */
    public UploadChecker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = ThemeUtils.getThemedContext(context, R.style.Theme_DialogLight, R.style.Theme_DialogDark);
    }

    @Override
    public Result doWork() {
        StorageDelegator delegator = new StorageDelegator();
        delegator.readFromFile(context);
        int changes = delegator.getApiElementCount();
        if (changes > 0) {
            PackageManager manager = context.getPackageManager();
            Intent intent = manager.getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                Notifications.warning(context, R.string.upload_checker_title,
                        context.getResources().getQuantityString(R.plurals.upload_checker_message, changes, changes), R.id.upload_reminder,
                        Notifications.createPendingIntent(context, Main.class, intent));
            } else {
                Log.e(DEBUG_TAG, "Unable to get launch intent");
            }
        } else {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
            Log.d(DEBUG_TAG, "No unsaved changes, cancelling job");
        }
        return Result.success();
    }

    /**
     * Cancel the work and remove any notification
     * 
     * @param context an Android Context
     */
    public static void cancel(@NonNull Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(UploadChecker.TAG);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(R.id.upload_reminder);
    }
}
