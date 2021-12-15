package de.blau.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.Snack;

public class PostFileWriteCallback implements PostAsyncActionHandler {
    private final Context context;
    private final String  fileName;

    /**
     * Construct a new callback for use after a file has been written
     * 
     * @param context an Android Context
     * @param fileName name of the file
     */
    PostFileWriteCallback(@NonNull Context context, @NonNull String fileName) {
        this.context = context;
        this.fileName = fileName;
    }

    @Override
    public void onSuccess() {
        Snack.toastTopInfo(context, R.string.toast_successfully_written);
    }

    @Override
    public void onError(@Nullable AsyncResult result) {
        Snack.toastTopError(context, context.getString(R.string.toast_error_writing, fileName));
    }
}
