package de.blau.android.util.mvt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import androidx.annotation.NonNull;

public class DecodeTest {

    /**
     * Decode tile and check that it contains what we think it should
     */
    @Test
    public void decodeOpenInfraMapTest() {
        try {
            VectorTileDecoder.FeatureIterable decodedTile = new VectorTileDecoder().decode(readTile("/openinframap_tile.pbf"));
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
            for (Entry<String, Integer> entry : counts.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Decode tile and check that it contains what we think it should
     */
    @Test
    public void decodeMapboxTest() {
        try {
            VectorTileDecoder.FeatureIterable decodedTile = new VectorTileDecoder().decode(readTile("/mapbox_tile.pbf"));
            List<VectorTileDecoder.Feature> list = decodedTile.asList();
            // assertEquals(101, list.size());
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
            for (Entry<String, Integer> entry : counts.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void decodeBenchmarkTest() {
        try {
            byte[] data = readTile("/openinframap_tile.pbf");
            long start = System.currentTimeMillis();
            VectorTileDecoder.FeatureIterable decodedTile = null;
            List<VectorTileDecoder.Feature> list = null;
            for (int i = 0; i < 1000; i++) {
                decodedTile = new VectorTileDecoder().decode(data);
                list = decodedTile.asList();
            }
            System.out.println("Time for 1000 decodes: " + (System.currentTimeMillis() - start));
            System.out.println("Decoded tile size :" + decodedTile.asList().size());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /*
     * Read a sample tile in to a byte array
     * 
     * @param filename the tile to read
     * @return a byte array containing the data
     */
    private static byte[] readTile(@NonNull String filename) throws IOException {
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