package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.annotation.NonNull;
import de.blau.android.Main;

/**
 * @author mb
 */
public class DownloadCurrentListener implements OnClickListener {

    private final Main caller;

    /**
     * @param caller calling instance of Main
     */
    public DownloadCurrentListener(@NonNull final Main caller) {
        this.caller = caller;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        Main.performCurrentViewHttpLoad(caller, false);
    }
}
