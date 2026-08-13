package de.blau.android.presets;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.XmlConfigurationLoader;

/**
 * This is just a convenient way of generating the default preset dump
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class TaginfoDump { // NOSONAR

    private static final String TEST_MANUAL_PROPERTY = "test.manual";
    private static final String TARGET_FILE          = "taginfo.json";

    private static final String[] ADDITIONAL_PRESETS = { "https://github.com/simonpoole/military-preset/releases/latest/download/military-josm.zip" };

    /**
     * Write out the current preset in taginfo format
     * 
     * If the system property test.manual is set to true it will generate the dumpfile in the toplevel repository
     * directory and potentially add additional presets.
     */
    @Test
    public void dump() { // NOSONAR
        boolean manual = System.getProperty(TEST_MANUAL_PROPERTY).equals("true");
        final Context ctx = ApplicationProvider.getApplicationContext();
        File target = manual ? new File(TARGET_FILE) : new File(ctx.getFilesDir(), TARGET_FILE);
        Preset[] presets = App.getCurrentPresets(ctx);
        if (manual) {
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
                for (String url : ADDITIONAL_PRESETS) {
                    final File presetDir = new File(ctx.getFilesDir(), Uri.parse(url).getLastPathSegment());
                    presetDir.mkdirs();
                    XmlConfigurationLoader.download(url, presetDir, Preset.PRESETXML);
                    db.addPreset(presetDir.getName(), presetDir.getName(), url, true);
                }
                presets = db.getCurrentPresetObject();
            }
        }
        assertTrue(Preset.generateTaginfoJson(ctx, presets, target));
    }
}
