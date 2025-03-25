package io.vespucci;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.util.ScreenMessage;

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
        ScreenMessage.toastTopInfo(context, context.getString(R.string.toast_successfully_written, fileName));
    }

    @Override
    public void onError(@Nullable AsyncResult result) {
        ScreenMessage.toastTopError(context, context.getString(R.string.toast_error_writing, fileName));
    }
}
