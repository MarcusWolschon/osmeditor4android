package io.vespucci;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.util.ScreenMessage;

public class PostFileReadCallback implements PostAsyncActionHandler {
    private final Context context;
    private final String  fileName;

    /**
     * Construct a new callback for use after a file has been read
     * 
     * @param context an Android Context
     * @param fileName name of the file
     */
    PostFileReadCallback(@NonNull Context context, @NonNull String fileName) {
        this.context = context;
        this.fileName = fileName;
    }

    @Override
    public void onSuccess() {
        ScreenMessage.toastTopInfo(context, R.string.toast_read_successfully);
    }

    @Override
    public void onError(@Nullable AsyncResult result) {
        ScreenMessage.toastTopError(context, context.getString(R.string.toast_error_reading, fileName));
    }
}

