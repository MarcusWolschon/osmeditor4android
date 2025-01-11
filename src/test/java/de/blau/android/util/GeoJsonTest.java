package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.google.gson.JsonElement;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.opencsv.exceptions.CsvException;

import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class GeoJsonTest {

    @Test
    public void fromCSV() {
        try {
            FeatureCollection fc = GeoJson.fromCSV(getClass().getResourceAsStream("/cvs-geojson.csv"));
            assertNotNull(fc);
            List<Feature> features = fc.features();
            assertEquals(2, features.size());
            Feature f = features.get(0);
            assertEquals(8.06783179982675, ((Point) f.geometry()).longitude(), 0.000001);
            assertEquals(47.399875769847, ((Point) f.geometry()).latitude(), 0.000001);
            Map<String, JsonElement> properties = f.properties().asMap();
            assertEquals(11, properties.size());
            assertTrue(properties.containsKey("service:vehicle:painting"));
            assertEquals(properties.get("service:vehicle:painting").getAsString(), "yes");
        } catch (IOException | CsvException | IllegalArgumentException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void fromCSVFail() {
        try {
            FeatureCollection fc = GeoJson.fromCSV(getClass().getResourceAsStream("/test_fail.csv"));
            fail("should have failed");
        } catch (IOException | CsvException | IllegalArgumentException e) {
            // OK
        }
    }
}
