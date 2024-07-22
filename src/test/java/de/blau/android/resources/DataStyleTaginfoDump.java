package de.blau.android.resources;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

/**
 * This is just a convenient way of generating the default preset dump
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class DataStyleTaginfoDump { // NOSONAR

    private static final String TEST_MANUAL_PROPERTY = "test.manual";
    private static final String TARGET_FILE          = "taginfo-style.json";

    /**
     * Write out the current style in taginfo format
     * 
     * If the system property test.manual is set to true it will generate the dumpfile in the toplevel repository
     * directory and potentially add additional presets.
     */
    @Test
    public void dump() { // NOSONAR
        boolean manual = System.getProperty(TEST_MANUAL_PROPERTY).equals("true");
        final Context ctx = ApplicationProvider.getApplicationContext();
        File target = manual ? new File(TARGET_FILE) : new File(ctx.getFilesDir(), TARGET_FILE);
        DataStyle.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        DataStyle.switchTo("Color Round Nodes");
        assertTrue(DataStyle.generateTaginfoJson(target));
    }
}
