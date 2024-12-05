package de.blau.android.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.JavaResources;
import de.blau.android.util.FileUtil;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class GpxTest {

    @Test
    public void parsingTest() {
        try {
            File zippedGpxFile = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "2011-06-08_13-21-55 OT.zip", null, "/");
            assertTrue(FileUtil.unpackZip(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), "/").getAbsolutePath(), zippedGpxFile.getName()));
            Track track = new Track(ApplicationProvider.getApplicationContext(), false);
            try (InputStream is = new FileInputStream(
                    new File(FileUtil.getPublicDirectory().getAbsolutePath() + "/2011-06-08_13-21-55 OT/2011-06-08_13-21-55.gpx"));
                    BufferedInputStream in = new BufferedInputStream(is)) {
                track.importFromGPX(in);
                assertEquals(301, track.getTrackPoints().size());
                TrackPoint tp = track.getFirstTrackPoint();
                assertEquals(47.437398, tp.getLatitude(), 0.000001); 
                assertEquals(8.211872, tp.getLongitude(), 0.000001); 
                assertEquals(3, track.getWayPoints().size());
                WayPoint wp = track.getFirstWayPoint();
                assertNotNull(wp.getLinks());
                assertEquals("2011-06-08_13-22-47.3gpp", wp.getLinks().get(0).getUrl());
                assertEquals("2011-06-08_13-22-47.3gpp", wp.getLinks().get(0).getDescription());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
