package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.contract.Files;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerSource;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class MapTest {

    private Preferences prefs;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        prefs = new Preferences(ApplicationProvider.getApplicationContext());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ApplicationProvider.getApplicationContext());
                InputStream is = loader.getResourceAsStream(Files.FILE_NAME_KEYS_V2)) {
            keyDatabase.keysFromStream(null, is);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        DataStyle styles = App.getDataStyle(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
    }

    /**
     * Add some layers and test if they are returned properly
     */
    @Test
    public void imageryNamesTest() {
        TileLayerDatabase db = new TileLayerDatabase(ApplicationProvider.getApplicationContext());
        try {
            SQLiteDatabase writableDatabase = db.getWritableDatabase();
            TileLayerDatabase.addSource(writableDatabase, TileLayerDatabase.SOURCE_ELI);
            TileLayerSource.parseImageryFile(ApplicationProvider.getApplicationContext(), writableDatabase, TileLayerDatabase.SOURCE_ELI,
                    getClass().getResourceAsStream("/test_imagery_vespucci.geojson"), true);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        db.close();
        Map map = new Map(ApplicationProvider.getApplicationContext());
        map.setPrefs(ApplicationProvider.getApplicationContext(), prefs);
        List<String> names = map.getImageryNames();
        assertEquals(1, names.size());
        TileLayerSource mapnik = TileLayerSource.get(ApplicationProvider.getApplicationContext(), TileLayerSource.LAYER_MAPNIK, false);
        assertNotNull(mapnik);
        assertEquals(mapnik.getName(), map.getImageryNames().get(0));
        TileLayerSource mapillary = TileLayerSource.get(ApplicationProvider.getApplicationContext(),
                de.blau.android.layer.mapillary.MapillaryOverlay.MAPILLARY_TILES_ID, false);
        assertNotNull(mapillary);
        de.blau.android.layer.Util.addLayer(ApplicationProvider.getApplicationContext(), LayerType.MAPILLARY);
        map.setUpLayers(ApplicationProvider.getApplicationContext());
        names = map.getImageryNames();
        assertEquals(2, names.size());
        assertEquals(mapillary.getName(), map.getImageryNames().get(0));
        assertEquals(mapnik.getName(), map.getImageryNames().get(1));
    }
}
