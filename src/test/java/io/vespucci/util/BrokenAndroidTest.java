package io.vespucci.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.util.BrokenAndroid;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class BrokenAndroidTest {

    /**
     * TEst that we detect broken deiveces properly
     */
    @Test
    public void brokenTest() {
        BrokenAndroid ba = new BrokenAndroid(ApplicationProvider.getApplicationContext());
        BrokenAndroid.Properties props = ba.getProperties("xiaomi", "daisy_sprout");
        assertNotNull(props);
        assertFalse(props.fullScreen);
        // test wildcard
        props = ba.getProperties("xiaomi", "something");
        assertNotNull(props);
        assertFalse(props.fullScreen);
        // test non existent entry
        props = ba.getProperties("something", "something");
        assertNull(props);
        // test specific entry
        props = ba.getProperties("google", "sargo");
        assertNotNull(props);
        assertFalse(props.fullScreen);
        // test non existent entry
        props = ba.getProperties("google", "something");
        assertNull(props);
        
        assertFalse(ba.isFullScreenBroken()); // whatever is set as device for roboelectric
    }
}