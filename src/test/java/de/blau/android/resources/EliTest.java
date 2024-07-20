package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import de.blau.android.osm.OsmXml;
import de.blau.android.resources.eli.EliFeatureCollection;
import de.blau.android.util.FileUtil;
import de.blau.android.util.Version;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class EliTest {

    private static final String DEBUG_TAG = EliTest.class.getSimpleName().substring(0, Math.min(23, EliTest.class.getSimpleName().length()));

    /**
     * Parse a sample ELI geojson config without meta
     */
    @Test
    public void parseNoMeta() {
        EliFeatureCollection fc = EliFeatureCollection.fromJson(stringFromResource("/imagery_test.geojson"));
        assertEquals(5, fc.features().size());
        Version formatVersion = fc.formatVersion();
        assertNull(formatVersion);
    }

    /**
     * Parse a sample ELI geojson config with meta
     */
    @Test
    public void parseWithMeta() {
        EliFeatureCollection fc = EliFeatureCollection.fromJson(stringFromResource("/imagery_test_with_meta.geojson"));
        assertEquals(5, fc.features().size());
        Version formatVersion = fc.formatVersion();
        assertNotNull(formatVersion);
        assertEquals("1.0.0", formatVersion.toString());
        assertEquals("2020-12-06 10:02:10", fc.generated());
    }

    /**
     * Get a resource file as a String
     * 
     * @param path path for the resource
     * @return a resource file as a String
     */
    String stringFromResource(@NonNull String path) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return FileUtil.readToString(new BufferedReader(new InputStreamReader(is, Charset.forName(OsmXml.UTF_8)))); // NOSONAR
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return null;
    }
}
