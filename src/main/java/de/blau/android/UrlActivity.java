package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Start vespucci with an intent
 */
public abstract class UrlActivity extends Activity {

    private static final String DEBUG_TAG = "UrlActivity";

    @Override
    protected void onStart() {
        super.onStart();
        Uri data = getIntent().getData();
        if (data == null) {
            Log.d(DEBUG_TAG, "Called with null data, aborting");
            finish();
            return;
        }
        Log.d(DEBUG_TAG, data.toString());
        Intent intent = new Intent(this, Main.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (setIntentExtras(intent, data)) {
            startActivity(intent);
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    /**
     * Set the extras on the intent from the uri
     * 
     * @param intent the Intent
     * @param data the Uri
     * @return true if the extras could be set successfully, false otherwise
     */
    abstract boolean setIntentExtras(@NonNull Intent intent, @NonNull Uri data);
}
