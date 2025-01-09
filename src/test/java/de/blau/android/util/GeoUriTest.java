package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.Uri;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class GeoUriTest {

    @Test
    public void parse() {
        Uri geo = Uri.parse("geo:47.220104,7.715071?z=18");
        GeoUriData data = GeoUriData.parse(geo.getSchemeSpecificPart());
        assertNotNull(data);
        assertEquals(47.220104, data.getLat(), 0.000001);
        assertEquals(7.715071, data.getLon(), 0.000001);
        assertEquals(18, data.getZoom());
    }

    @Test
    public void parseFail() {
        Uri geo = Uri.parse("geo:47.220104?z=18");
        GeoUriData data = GeoUriData.parse(geo.getSchemeSpecificPart());
        assertNull(data);
    }
}
