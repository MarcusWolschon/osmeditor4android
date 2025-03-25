package io.vespucci;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

/**
 * Start with an Url
 */
public abstract class UrlActivity extends IntentDataActivity {

    @Override
    protected void process(@NonNull Uri data) {
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
