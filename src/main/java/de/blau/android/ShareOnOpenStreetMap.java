package de.blau.android;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import de.blau.android.contract.Urls;
import de.blau.android.util.GeoUriData;

/**
 * Take a geo intent and open the location on OSM
 */
public class ShareOnOpenStreetMap extends IntentDataActivity {

    @Override
    protected void process(@NonNull Uri data) {
        GeoUriData geoUrlData = GeoUriData.parse(data.getSchemeSpecificPart());
        if (geoUrlData != null) {
            double lat = geoUrlData.getLat();
            double lon = geoUrlData.getLon();
            String url = Urls.OSM + "/?mlat=" + lat + "&mlon=" + lon + "#map=" + (geoUrlData.hasZoom() ? geoUrlData.getZoom() : 18) + "/" + lat + "/" + lon;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        finish();
    }
}
