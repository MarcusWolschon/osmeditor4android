package de.blau.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.presets.Preset;

/**
 * This is just a convenient way of generating the default preset dump
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TaginfoDump {

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup except this isn't really a test
     */
    @Before
    public void setup() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Main main = mActivityRule.getActivity();
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Write out the current preset in taginfo format
     */
    @Test
    public void dump() {
        Assert.assertTrue(Preset.generateTaginfoJson(InstrumentationRegistry.getInstrumentation().getTargetContext(), "taginfo.json"));
    }
}
