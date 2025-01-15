package de.blau.android.util.mvt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import de.blau.android.util.GeoJSONConstants;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class DecodeTest {

    /**
     * Decode tile and check that it contains what we think it should (very superficially)
     */
    @Test
    public void decodeOpenInfraMapTest() {
        try {
            long start = System.currentTimeMillis();
            VectorTileDecoder.FeatureIterable decodedTile = new VectorTileDecoder().decode(readTile("/openinframap_tile.pbf"));
            System.out.println("Decoding took " + (System.currentTimeMillis()-start) + "ms");
            List<VectorTileDecoder.Feature> list = decodedTile.asList();
            assertEquals(101, list.size());
            Map<String, Integer> counts = new HashMap<>();
            for (VectorTileDecoder.Feature f : list) {
                String geometry = f.getGeometry().type();
                Integer count = counts.get(geometry);
                if (count == null) {
                    count = Integer.valueOf(0);
                }
                count++;
                counts.put(geometry, count);
            }
            assertEquals(50, (int) counts.get(GeoJSONConstants.LINESTRING));
            assertEquals(50, (int) counts.get(GeoJSONConstants.POINT));
            assertEquals(1, (int) counts.get(GeoJSONConstants.POLYGON));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Decode tilemaker tile and check that it contains what we think it should (very superficially)
     */
    @Test
    public void decodeTilemakerTest() {
        try {
            long start = System.currentTimeMillis();
            VectorTileDecoder.FeatureIterable decodedTile = new VectorTileDecoder().decode(readTile("/tilemaker_tile.pbf"));
            System.out.println("Decoding took " + (System.currentTimeMillis()-start) + "ms");
            List<VectorTileDecoder.Feature> list = decodedTile.asList();
            assertEquals(314, list.size());
            Map<String, Integer> counts = new HashMap<>();
            for (VectorTileDecoder.Feature f : list) {
                String geometry = f.getGeometry().type();
                Integer count = counts.get(geometry);
                if (count == null) {
                    count = Integer.valueOf(0);
                }
                count++;
                counts.put(geometry, count);
            }
            assertEquals(147, (int) counts.get(GeoJSONConstants.LINESTRING));
            assertEquals(12, (int) counts.get(GeoJSONConstants.POINT));
            assertEquals(149, (int) counts.get(GeoJSONConstants.POLYGON));
            assertEquals(6, (int) counts.get(GeoJSONConstants.MULTIPOLYGON));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Read a sample tile in to a byte array
     * 
     * @param filename the tile to read
     * 
     * @return a byte array containing the data
     */
    static byte[] readTile(@NonNull String filename) throws IOException {
        InputStream input = DecodeTest.class.getResourceAsStream(filename);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}