package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import de.blau.android.Main;

/**
 * @author mb
 */
class GotoAreaPickerListener implements OnClickListener {

    private final Main caller;

    /**
     * @param caller
     */
    public GotoAreaPickerListener(final Main caller) {
        this.caller = caller;
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        caller.gotoBoxPicker();
    }
}
