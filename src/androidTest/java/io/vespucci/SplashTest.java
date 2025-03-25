package io.vespucci;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import io.vespucci.Main;
import io.vespucci.Splash;
import io.vespucci.resources.TileLayerDatabase;
import io.vespucci.resources.TileLayerSource;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SplashTest {

    Instrumentation instrumentation = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class, false, false);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        // run this first to avod ANRs
        try (TileLayerDatabase db = new TileLayerDatabase(context)) {
            TileLayerSource.createOrUpdateFromAssetsSource(context, db.getWritableDatabase(), true, false);
        }
    }

    /**
     * Start splash, check that it in turn starts Main
     */
    @Test
    public void splash() {
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        Splash splash = mActivityRule.launchActivity(intent);
        assertNotNull(splash);
        Main main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 60000); // wait for main
        assertNotNull(main);
    }
}
