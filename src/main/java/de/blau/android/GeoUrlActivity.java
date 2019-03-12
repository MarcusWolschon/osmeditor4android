package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.util.GeoUrlData;

/**
 * Start vespucci with geo: URLs. see http://www.ietf.org/rfc/rfc5870.txt
 */
public class GeoUrlActivity extends Activity {

    private static final String DEBUG_TAG = "GeoUrlActivity";
    public static final String  GEODATA   = "de.blau.android.GeoUrlActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

        GeoUrlData geoUrlData = GeoUrlData.parse(data.getSchemeSpecificPart());
        if (geoUrlData != null) {
            intent.putExtra(GEODATA, geoUrlData);
        }
        startActivity(intent);
        finish();
    }
}
