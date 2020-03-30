package de.blau.android.presets;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.util.SearchIndexUtils;

/**
 * Test the synonym facility
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SynonymsTest {
    Main main;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = (Main) mActivityRule.getActivity();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Do a general search in the preset system that should return a hit from the synonyms
     */
    @Test
    public void search() {
        Locale locale = Locale.getDefault();
        Assert.assertEquals(Locale.US.getCountry(), locale.getCountry());
        List<PresetElement> result = SearchIndexUtils.searchInPresets(main, "raptor", ElementType.NODE, 2, 10, null);
        Assert.assertTrue(result.size() > 0);
        Assert.assertEquals("Animal shelter", result.get(0).getName());
    }
}
