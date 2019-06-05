package de.blau.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import de.blau.android.prefs.Preferences;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PreferenceTest {

    Main main = null;
    View v    = null;
    Context context = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = mActivityRule.getActivity();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(context);
    }

    /**
     * Test if we can set and read a Preference (this is rather silly on multiple counts)
     */
    @Test
    public void preferences() {
        Preferences prefs = new Preferences(context);
        Assert.assertNull(prefs.getString(R.string.config_gpxPreferredDir_key));
        prefs.putString(R.string.config_gpxPreferredDir_key, "test");
        Assert.assertEquals("test", prefs.getString(R.string.config_gpxPreferredDir_key));
        Assert.assertNull(prefs.getString(-1));
    }
}