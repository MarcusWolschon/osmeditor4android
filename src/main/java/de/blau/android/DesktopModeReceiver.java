package de.blau.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import de.blau.android.propertyeditor.PropertyEditor;
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
        Log.d(DEBUG_TAG, "Broadcast received");
        // if the density has changed the icons will have wrong dimension remove them
        Util.clearIconCaches(context);
        if (context instanceof PropertyEditor) {
            ((PropertyEditor)context).recreate();
        }
    }
}