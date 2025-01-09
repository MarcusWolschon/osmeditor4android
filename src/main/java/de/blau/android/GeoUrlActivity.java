package de.blau.android;

import android.content.Intent;
import android.net.Uri;
import de.blau.android.util.GeoUriData;

/**
 * Start vespucci with geo: URLs. see http://www.ietf.org/rfc/rfc5870.txt
 */
public class GeoUrlActivity extends UrlActivity {

    public static final String GEODATA = "de.blau.android.GeoUrlActivity";

    @Override
    boolean setIntentExtras(Intent intent, Uri data) {
        GeoUriData geoUrlData = GeoUriData.parse(data.getSchemeSpecificPart());
        if (geoUrlData != null) {
            intent.putExtra(GEODATA, geoUrlData);
            return true;
        }
        return false;
    }
}
