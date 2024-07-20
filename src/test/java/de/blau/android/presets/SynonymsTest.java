package de.blau.android.presets;

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
}
