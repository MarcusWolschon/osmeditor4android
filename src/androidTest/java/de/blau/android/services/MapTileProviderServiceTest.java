package de.blau.android.services;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import de.blau.android.TestUtils;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.MapTile;
import de.blau.android.views.util.MapTileProvider;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Directly test functionality of the tile service service.
 * 
 * Note: currently there doesn't seem to be a way to include coverage data from this.
 * 
 * @author Simon Poole
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MapTileProviderServiceTest {

    private static final String RENDERER   = "VESPUCCITEST";
    private static final int    TIMEOUT    = 10;
    private Context             context    = null;
    private MockWebServer       tileServer = null;

    private CountDownLatch signal = new CountDownLatch(1);

    @Rule
    public final ServiceTestRule serviceRule            = new ServiceTestRule();
    @Rule
    public GrantPermissionRule   mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        tileServer = TestUtils.setupTileServer(context, new Preferences(context), "ersatz_background.mbt");
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            tileServer.close();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Check that the service has started correctly and request a tile
     */
    @Test
    public void startService() {
        MapTileProvider provider = new MapTileProvider(context, new TileHandler());
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(provider.connected());
        provider.update();
        MapTile tile = new MapTile(RENDERER, 19, 274337, 183513);
        Assert.assertNull(provider.getMapTile(tile, 123456L));
        signal = new CountDownLatch(1);
        try {
            signal.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(provider.getMapTileFromCache(tile));
        provider.flushCache(RENDERER);
        Assert.assertNull(provider.getMapTileFromCache(tile));
    }

    private class TileHandler extends Handler {

        /**
         * Handle success messages from downloaded tiles
         * 
         */
        public TileHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(final Message msg) {
            if (msg.what == MapTile.MAPTILE_SUCCESS_ID) {
                signal.countDown();
            }
        }
    }
}
