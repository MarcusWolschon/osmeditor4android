package de.blau.android.presets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
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
import de.blau.android.util.IndexSearchResult;
import de.blau.android.util.SearchIndexUtils;

/**
 * Test the synonym facility
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class SynonymsTest {
    
    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Robolectric.buildActivity(Main.class).create().resume();
    }
    
    /**
     * Do a general search in the preset system that should return a hit from the synonyms
     */
    @Test
    public void search() {
        Locale locale = Locale.getDefault();
        Assert.assertEquals(Locale.US.getCountry(), locale.getCountry());
        List<PresetElement> result = SearchIndexUtils.searchInPresets(ApplicationProvider.getApplicationContext(), "raptor", ElementType.NODE, 2, 10, null);
        Assert.assertTrue(result.size() > 0);
        Assert.assertEquals("Animal shelter", result.get(0).getName());
    }

    /**
     * Check that 2nd level tag replacements work
     */
    @Test
    public void secondLevelTest() {
        Locale.setDefault(new Locale("en", "AU"));
        Synonyms s = new Synonyms(ApplicationProvider.getApplicationContext());
        List<IndexSearchResult> results = s.search(ApplicationProvider.getApplicationContext(), "petrol", null, 2);
        boolean found = false;
        for (IndexSearchResult isr : results) {
            PresetTagField field = isr.item.getField("vending");

            if (field instanceof PresetFixedField) {
                // we should not have any fixed tags with vending just the regular vending machine preset
                assertEquals("fuel", ((PresetFixedField) field).getValue().getValue());
            }
            if (field instanceof PresetComboField) {
                found = true;
            }
        }
        assertTrue(found);
    }
}
