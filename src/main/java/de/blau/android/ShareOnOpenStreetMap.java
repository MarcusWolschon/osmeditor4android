package de.blau.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.blau.android.contract.Urls;
import de.blau.android.util.GeoUrlData;

/**
 * Take a geo intent and open the location on OSM
 */
public class ShareOnOpenStreetMap extends Activity {

    private static final String DEBUG_TAG = "ShareOnOpenStreetMap";

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
        GeoUrlData geoUrlData = GeoUrlData.parse(data.getSchemeSpecificPart());
        if (geoUrlData != null) {
            String url = Urls.OSM + "/?mlat=" + geoUrlData.getLat() + "&mlon=" + geoUrlData.getLon() +  "#map=" + (geoUrlData.hasZoom() ? geoUrlData.getZoom() : 18) + "/" + geoUrlData.getLat() + "/" + geoUrlData.getLon();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        finish();
    }
}
