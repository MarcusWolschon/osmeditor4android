package de.blau.android.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.util.IndexSearchResult;
import de.blau.android.util.SearchIndexUtils;

/**
 * Test the synonym facility
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class SynonymsTest {

    /**
     * Do a general search in the preset system that should return a hit from the synonyms
     */
    @Test
    public void search() {
        Robolectric.buildActivity(Main.class).create().resume();
        Locale locale = Locale.getDefault();
        assertEquals(Locale.US.getCountry(), locale.getCountry());
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "raptor", ElementType.NODE, 2, 10, null);
        assertTrue(result.size() > 0);
        assertEquals("Animal shelter", result.get(0).getName());
    }

    /**
     * Check that locale variant get read
     */
    @Test
    public void localeTest() {
        Locale.setDefault(new Locale("en", "AU"));
        Synonyms s = new Synonyms(ApplicationProvider.getApplicationContext());
        List<IndexSearchResult> results = s.search(ApplicationProvider.getApplicationContext(), "petrol station", null, 2);
        for (IndexSearchResult isr : results) {
            if (isr.getItem().hasKeyValue(Tags.KEY_AMENITY, "fuel")) {
                assertEquals(-SearchIndexUtils.OFFSET_EXACT_MATCH_WITHOUT_ACCENTS, isr.getWeight());
                return;
            }
        }
        fail("petrol station not found");
    }
}
