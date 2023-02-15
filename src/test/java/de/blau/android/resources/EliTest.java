package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.blau.android.resources.eli.EliFeatureCollection;
import de.blau.android.util.Version;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class EliTest {

    private static final String DEBUG_TAG = EliTest.class.getSimpleName();

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
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
        return sb.toString();
    }
}
