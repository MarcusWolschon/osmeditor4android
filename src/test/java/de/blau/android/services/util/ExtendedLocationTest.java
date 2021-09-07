package de.blau.android.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.location.LocationManager;
import android.os.Parcel;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class ExtendedLocationTest {

    /**
     * Test writing the location to a a Parcel and restoring it from it
     */
    @Test
    public void saveRestore() {
        ExtendedLocation loc = new ExtendedLocation(LocationManager.GPS_PROVIDER);
        final double lat = 47.3978982D;
        final double lon = 8.3762937D;
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setAltitude(600);
        loc.setBarometricHeight(555);
        loc.setGeoidCorrection(48);
        loc.setGeoidHeight(552);
        loc.setHdop(2.0);
        //
        Parcel p = Parcel.obtain();
        loc.writeToParcel(p, 0);
        p.setDataPosition(0);
        ExtendedLocation restoredLoc = ExtendedLocation.CREATOR.createFromParcel(p);
        assertEquals(lat, restoredLoc.getLatitude(), 0.0);
        assertEquals(lon, restoredLoc.getLongitude(), 0.0);
        assertTrue(loc.hasAltitude());
        assertEquals(600, restoredLoc.getAltitude(), 0.0);
        assertTrue(loc.hasBarometricHeight());
        assertEquals(555, restoredLoc.getBarometricHeight(), 0.0);
        assertTrue(loc.hasGeoidCorrection());
        assertEquals(48, restoredLoc.getGeoidCorrection(), 0.0);
        assertTrue(loc.hasGeoidHeight());
        assertEquals(552, restoredLoc.getGeoidHeight(), 0.0);
        assertTrue(loc.hasHdop());
        assertEquals(2.0, restoredLoc.getHdop(), 0.0);
    }
}
