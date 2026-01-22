package de.blau.android.resources;

import static de.blau.android.osm.DelegatorUtil.addWayToStorage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import android.content.ContentResolver;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.contract.Paths;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.StyleConfiguration;
import de.blau.android.prefs.XmlConfigurationLoader;
import de.blau.android.resources.DataStyle.FeatureStyle;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class DataStyleTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        DataStyleManager styles = App.getDataStyleManager(ApplicationProvider.getApplicationContext());
        styles.reset(ApplicationProvider.getApplicationContext(), false);
    }

    /**
     * Test matching for a building with and without a house number
     */
    @Test
    public void buildingTest() {
        DataStyleManager styles = App.getDataStyleManager(ApplicationProvider.getApplicationContext());
        final StorageDelegator delegator = App.getDelegator();
        Way w = addWayToStorage(delegator, true);
        Map<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_BUILDING, Tags.VALUE_YES);
        delegator.setTags(w, tags);
        assertEquals(1, styles.getStyleList(ApplicationProvider.getApplicationContext()).length);
        assertEquals(DataStyleManager.getBuiltinStyleName(), styles.getCurrent().getName());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        assertEquals(5, styles.getStyleList(ApplicationProvider.getApplicationContext()).length);
        styles.switchTo("Color Round Nodes");
        assertEquals("Color Round Nodes", styles.getCurrent().getName());
        FeatureStyle style = styles.matchStyle(w);
        assertEquals(((int) Long.parseLong("ffcc9999", 16)), style.getPaint().getColor());
        assertNull(style.getLabelKey());
        assertFalse(style.usePresetIcon());
        assertFalse(style.isArea());
        tags.put(Tags.KEY_ADDR_HOUSENUMBER, "111");
        delegator.setTags(w, tags);
        style = styles.matchStyle(w);
        assertEquals(Tags.KEY_ADDR_HOUSENUMBER, style.getLabelKey());
    }

    /**
     * Load a custom style besides the standard ones
     */
    @Test
    public void customStyle() {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ApplicationProvider.getApplicationContext())) {
            File style = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test-style.xml", null,
                    "/" + Paths.DIRECTORY_PATH_STYLES);
            final String id = "12345";
            db.addStyle(id, "Test Style", "/" + Paths.DIRECTORY_PATH_STYLES + "/test-style.xml", true, false);
            StyleConfiguration conf = db.getStyle(id);
            assertNotNull(conf);
            final File dir = db.getResourceDirectory(id);
            dir.mkdir();
            assertTrue(dir.isDirectory());
            ContentResolver contentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
            ShadowContentResolver shadowContentResolver = shadowOf(contentResolver);
            Uri inputUri = Uri.parse(conf.url);
            shadowContentResolver.registerInputStream(inputUri, new FileInputStream(style));
            XmlConfigurationLoader.load(ApplicationProvider.getApplicationContext(), inputUri, dir, "style.xml");
        } catch (IOException e) {
            fail(e.getMessage());
        }
        DataStyleManager styles = App.getDataStyleManager(ApplicationProvider.getApplicationContext());
        styles.reset(ApplicationProvider.getApplicationContext(), true);
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        assertEquals(6, styles.getStyleList(ApplicationProvider.getApplicationContext()).length);

        // matching test
        final StorageDelegator delegator = App.getDelegator();
        Node tree = delegator.getFactory().createNodeWithNewId(0, 0);
        delegator.insertElementSafe(tree);
        Map<String, String> tags = new TreeMap<>();
        tags.put(Tags.KEY_NATURAL, "tree");
        delegator.setTags(tree, tags);
        styles.switchTo("Test Style");
        assertEquals("Test Style", styles.getCurrent().getName());
        FeatureStyle style = styles.matchStyle(tree);
        assertTrue(style.usePresetIcon());
        tags.put("height", "1");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_height.png"));
        tags.put("circumference", "2");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_height_circumference.png"));
        tags.put("diameter_crown", "3");
        delegator.setTags(tree, tags);
        style = styles.matchStyle(tree);
        assertTrue(style.getIconPath().endsWith("tree_all.png"));
    }
}
