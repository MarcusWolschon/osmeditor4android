package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Handler for all kind on files
 */
public class FileHandlerActivity extends Activity {

    private static final String DEBUG_TAG = "FileHandlerAct..";
    public static final String  RCDATA    = "de.blau.android.RemoteControlActivity";

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
        if ("content".equals(data.getScheme())) {
            intent.setData(data);
            startActivity(intent);
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

}
