package de.blau.android.prefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import ch.poole.openinghoursfragment.templates.TemplateDatabaseHelper;
import de.blau.android.R;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.API.AuthParams;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class ImportExportConfigurationTest {

    /**
     */
    @Test
    public void exportTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            API current = db.getCurrentAPI();
            db.addAPI("test_1", "test_1", current.url, null, null, new AuthParams(current.auth, null, null, null, null), false);
            db.addAPI("test_2", "test_2", current.url, null, null, new AuthParams(Auth.OAUTH1A, null, null, null, null), false);
            db.selectAPI("test_2");
            db.setAPIAccessToken("12345", "67890");
        }
        Preferences prefs = new Preferences(ctx);
        prefs.setAutolockDelay(10);
        prefs.setGeoJsonStrokeWidth(1.5f);
        File file;
        try {
            file = File.createTempFile("test.config", ".xml");
            file.deleteOnExit();
            try (FileOutputStream os = new FileOutputStream(file)) {
                ImportExportConfiguration.exportConfig(ctx, os);
            }
            String read = Files.readAllLines(file.toPath()).get(0);
            assertTrue(read.contains("test_2"));
            assertFalse(read.contains("12345"));
            assertFalse(read.contains("67890"));
            assertTrue(read.contains(ctx.getString(R.string.config_autoLockDelay_key)));
            assertTrue(read.contains(ctx.getString(R.string.config_geojson_stroke_width_key)));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     */
    @Test
    public void importTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = loader.getResourceAsStream("config.xml")) {
            ImportExportConfiguration.importConfig(ctx, is);
            Preferences prefs = new Preferences(ctx);
            assertTrue(prefs.poiKeys().contains(Tags.KEY_BUILDING));
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
                assertEquals("https://github.com/simonpoole/militarypreset/releases/latest/download/military.zip",
                        db.getPreset("aab1d149-6183-4df8-8e28-2fc805e8d12f").url);
            }
            // this checks if the mechanism to call the database helper works if the db doesn't exist yet
            try (TemplateDatabaseHelper t = new TemplateDatabaseHelper(ctx);
                Cursor c = t.getReadableDatabase().rawQuery("select * from templates where name='Wekdays with lunch break and late shopping'", null);) {
                assertTrue(c.moveToFirst());
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
