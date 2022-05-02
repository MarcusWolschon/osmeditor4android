package de.blau.android.util;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.blau.android.R;
import de.blau.android.osm.StorageDelegator;

public class UploadChecker extends Worker {
    private static final String DEBUG_TAG = UploadChecker.class.getSimpleName();

    public static final String TAG = "UPLOAD_CHECKER";

    private final Context context;

    /**
     * Create a new UploadChecker
     * 
     * @param context an Android COntext
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
            Notifications.warning(context, R.string.upload_checker_title,
                    context.getResources().getQuantityString(R.plurals.upload_checker_message, changes, changes), R.id.upload_reminder);
        } else {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG);
            Log.d(DEBUG_TAG, "No unsaved changes, cancelling job");
        }
        return Result.success();
    }
}
