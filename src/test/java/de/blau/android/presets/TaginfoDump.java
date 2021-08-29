package de.blau.android.presets;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

/**
 * This is just a convenient way of generating the default preset dump
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class TaginfoDump {

    /**
     * Write out the current preset in taginfo format
     */
    @Test
    public void dump() { // NOSONAR
        assertTrue(Preset.generateTaginfoJson(ApplicationProvider.getApplicationContext(), new File("taginfo.json")));
    }
}
