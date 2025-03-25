package io.vespucci;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Take an intent and do something with its data uri
 */
abstract class IntentDataActivity extends Activity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, IntentDataActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = IntentDataActivity.class.getSimpleName().substring(0, TAG_LEN);

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
        process(data);
    }

    /**
     * Process the data Uri
     * 
     * @param data the Uri
     */
    protected abstract void process(@NonNull Uri data);
}
