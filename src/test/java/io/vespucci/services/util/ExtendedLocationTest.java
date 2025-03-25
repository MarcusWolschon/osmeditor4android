package io.vespucci.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.location.LocationManager;
import android.os.Parcel;
import androidx.test.filters.LargeTest;
import io.vespucci.services.util.ExtendedLocation;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
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
        loc.setUseBarometricHeight();
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
        assertTrue(restoredLoc.hasAltitude());
        assertEquals(600, restoredLoc.getAltitude(), 0.0);
        assertTrue(restoredLoc.hasBarometricHeight());
        assertEquals(555, restoredLoc.getBarometricHeight(), 0.0);
        assertTrue(restoredLoc.useBarometricHeight());
        assertTrue(restoredLoc.hasGeoidCorrection());
        assertEquals(48, restoredLoc.getGeoidCorrection(), 0.0);
        assertTrue(restoredLoc.hasGeoidHeight());
        assertEquals(552, restoredLoc.getGeoidHeight(), 0.0);
        assertTrue(restoredLoc.hasHdop());
        assertEquals(2.0, restoredLoc.getHdop(), 0.0);
    }
}
