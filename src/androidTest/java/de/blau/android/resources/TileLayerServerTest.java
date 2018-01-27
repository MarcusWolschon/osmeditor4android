package de.blau.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.SignalHandler;
import de.blau.android.Splash;
import de.blau.android.TestUtils;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.MapTile;

/**
 * Note these tests are not mocked
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TileLayerServerTest {

    Main            main            = null;
    View            v               = null;
    Splash          splash          = null;
    ActivityMonitor monitor         = null;
    Instrumentation instrumentation = null;

    @Rule
    public ActivityTestRule<Splash> mActivityRule = new ActivityTestRule<>(Splash.class);

    @Before
    public void setup() {
        splash = mActivityRule.getActivity();
        instrumentation = InstrumentationRegistry.getInstrumentation();
        monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
    }

    @After
    public void teardown() {
        instrumentation.removeMonitor(monitor);
        splash.deleteDatabase(TileLayerDatabase.DATABASE_NAME);
    }

    @Test
    public void buildurl() {
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 20000); // wait for main
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(splash);
        Map map = main.getMap();
        MapTile mapTile = new MapTile("", 20, 1111, 2222);
        Preferences prefs = new Preferences(main);

        prefs.setBackGroundLayer("BING");
        main.getMap().setPrefs(main, prefs);

        final TileLayerServer t = map.getBackgroundLayer().getTileLayerConfiguration();
        Assert.assertNotNull(t);
        if (!t.isMetadataLoaded()) {
            final CountDownLatch signal = new CountDownLatch(1);
            final SignalHandler handler = new SignalHandler(signal);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    for (int i = 0;i < 10;i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        if (t.isMetadataLoaded()) {
                            System.out.println("metadata is loaded");
                            break;
                        }
                    }
                    return null;
                }  
                @Override
                protected void onPostExecute(Void result) {
                    handler.onSuccess();
                }
            }.execute();
            try {
                signal.await(11, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
        }

        Assert.assertTrue(t.isMetadataLoaded());
        String s = t.getTileURLString(mapTile); // note this would fail if the metainfo cannot be retrieved

        System.out.println("Parameters replaced " + s);
        System.out.println("Quadkey " + t.quadTree(mapTile));
        Assert.assertTrue(s.contains(t.quadTree(mapTile)));

        prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK);
        main.getMap().setPrefs(main, prefs);
 
        TileLayerServer t2 = map.getBackgroundLayer().getTileLayerConfiguration();
        System.out.println(t2.toString());

        s = t2.getTileURLString(mapTile);

        System.out.println("Parameters replaced " + s);

        Assert.assertTrue(s.contains("1111"));
        Assert.assertTrue(s.contains("2222"));
        Assert.assertTrue(s.contains("20"));
    }

    @Test
    public void sort() {
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 20000); // wait for main
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(splash);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("imagery_test.geojson");
        try {
            TileLayerDatabase db = new TileLayerDatabase(splash);
            TileLayerServer.parseImageryFile(splash, db.getWritableDatabase(), TileLayerDatabase.SOURCE_ELI, is, false);
            TileLayerServer.getListsLocked(splash, db.getReadableDatabase(), true);
            db.close();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        List<String> names = Arrays.asList(TileLayerServer.getNames(null, false));

        int iA = names.indexOf("A imagery");
        Assert.assertNotEquals(-1, iA);
        int iAnoDate = names.indexOf("A no date imagery");
        Assert.assertNotEquals(-1, iAnoDate);
        int iB = names.indexOf("B imagery");
        Assert.assertNotEquals(-1, iB);
        int iBnoDate = names.indexOf("B no date imagery");
        Assert.assertNotEquals(-1, iBnoDate);
        int iC = names.indexOf("C imagery");
        Assert.assertNotEquals(-1, iC);

        Assert.assertTrue(iAnoDate < iBnoDate); // alphabetic

        TileLayerServer a = TileLayerServer.get(main, "A", false);
        TileLayerServer b = TileLayerServer.get(main, "B", false);
        Assert.assertTrue(a.getEndDate() < b.getEndDate());
        Assert.assertTrue(iA > iB); // date

        Assert.assertTrue(iA > iC && iB > iC); // preference
    }
}