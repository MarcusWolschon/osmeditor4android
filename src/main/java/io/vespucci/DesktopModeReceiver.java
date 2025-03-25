package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import io.vespucci.util.Util;

/**
 * 
 * Receive broadcast events when we are inserted/removed from a DeX dock and similar
 * 
 * @author simon
 *
 */
public class DesktopModeReceiver extends BroadcastReceiver {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DesktopModeReceiver.class.getSimpleName().length());
    private static final String DEBUG_TAG = DesktopModeReceiver.class.getSimpleName().substring(0, TAG_LEN);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Broadcast received");
        // if the density has changed the icons will have wrong dimension remove them
        Util.clearIconCaches(context);
    }
}