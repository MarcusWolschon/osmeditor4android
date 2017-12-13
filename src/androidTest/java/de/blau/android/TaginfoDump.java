package de.blau.android;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
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

    @Before
    public void setup() {
        TestUtils.grantPermissons();
    }

    @Test
    public void dump() {
        Assert.assertTrue(Preset.generateTaginfoJson(InstrumentationRegistry.getInstrumentation().getTargetContext(), "taginfo.json"));
    }
}
