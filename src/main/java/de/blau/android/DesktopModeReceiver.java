package de.blau.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.blau.android.util.Util;

/**
 * 
 * Receive broadcast events when we are inserted/removed from a DeX dock and similar
 * 
 * @author simon
 *
 */
public class DesktopModeReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = "DesktopModeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // if the density has changed the icons will have wrong dimension remove them
        Util.clearIconCaches(context);
    }
}