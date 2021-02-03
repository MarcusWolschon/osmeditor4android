package de.blau.android.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class GeoContextTest {

    /**
     * Test if the US is imperial
     */
    @Test
    public void imperialTest() {
        GeoContext gc = new GeoContext(ApplicationProvider.getApplicationContext());
        assertTrue(gc.imperial(-77.0351535, 38.8894971));
    }
}