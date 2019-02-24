package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.annotation.NonNull;
import de.blau.android.Main;

/**
 * @author Marcus Wolschon
 */
public class ConfirmUploadListener implements OnClickListener {

    private final Main caller;

    /**
     * Construct a new instance
     * 
     * @param caller the calling instance of Main
     */
    public ConfirmUploadListener(@NonNull final Main caller) {
        this.caller = caller;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        caller.confirmUpload();
    }
}
