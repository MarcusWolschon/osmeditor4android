package de.blau.android;

import android.content.Context;
import androidx.annotation.NonNull;
import de.blau.android.util.Snack;

public class PostFileReadCallback implements PostAsyncActionHandler {
    private final Context context;
    private final String  fileName;

    /**
     * Construct a new callback for use after a file has been read
     * 
     * @param fileName name of the file
     */
    PostFileReadCallback(@NonNull Context context, @NonNull String fileName) {
        this.context = context;
        this.fileName = fileName;
    }

    @Override
    public void onSuccess() {
        Snack.toastTopInfo(context, R.string.toast_read_successfully);
    }

    @Override
    public void onError() {
        Snack.toastTopError(context, context.getString(R.string.toast_error_reading, fileName));
    }
}

