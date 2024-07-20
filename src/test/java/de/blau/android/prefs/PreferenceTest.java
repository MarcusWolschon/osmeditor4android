package de.blau.android.prefs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import de.blau.android.R;

/**
 *
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@SmallTest
public class PreferenceTest {

    /**
     * Test if we can set and read a Preference
     */
    @Test
    public void preferences() {
        Preferences prefs = new Preferences(ApplicationProvider.getApplicationContext());
        Assert.assertNull(prefs.getString(R.string.config_gpxPreferredDir_key));
        prefs.putString(R.string.config_gpxPreferredDir_key, "test");
        Assert.assertEquals("test", prefs.getString(R.string.config_gpxPreferredDir_key));
        Assert.assertNull(prefs.getString(-1));
    }
}