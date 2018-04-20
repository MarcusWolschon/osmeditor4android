package de.blau.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import android.support.test.uiautomator.UiDevice;
import android.view.View;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class GeoContextTest {

    Context          context   = null;
    Main             main      = null;
    StorageDelegator delegator = null;
    View             v         = null;
    UiDevice         device    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        delegator = App.getDelegator();
        delegator.reset(false);
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        main.getMap().setPrefs(main, prefs);
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    @Test
    public void UK() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("london.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way)delegator.getOsmElement(Way.NAME, 451984385L);
        Assert.assertNotNull(w);
        Assert.assertTrue(App.getGeoContext(context).imperial(w));
        Assert.assertTrue(App.getGeoContext(context).driveLeft(w));
        Node n = (Node)delegator.getOsmElement(Node.NAME, 1012733590L);
        Assert.assertNotNull(n);
        Assert.assertTrue(App.getGeoContext(context).imperial(n));
        Assert.assertTrue(App.getGeoContext(context).driveLeft(n));
    }

    @Test
    public void DC() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("dc.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way)delegator.getOsmElement(Way.NAME, 485299712L);
        Assert.assertNotNull(w);
        Assert.assertTrue(App.getGeoContext(context).imperial(w));
        Assert.assertFalse(App.getGeoContext(context).driveLeft(w));
        Node n = (Node)delegator.getOsmElement(Node.NAME, 309551133L);
        Assert.assertNotNull(n);
        Assert.assertTrue(App.getGeoContext(context).imperial(n));
        Assert.assertFalse(App.getGeoContext(context).driveLeft(n));
    }
    
    @Test
    public void BD() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way)delegator.getOsmElement(Way.NAME,27009604L);
        Assert.assertNotNull(w);
        Assert.assertFalse(App.getGeoContext(context).imperial(w));
        Assert.assertFalse(App.getGeoContext(context).driveLeft(w));
        Node n = (Node)delegator.getOsmElement(Node.NAME, 101792984L);
        Assert.assertNotNull(n);
        Assert.assertFalse(App.getGeoContext(context).imperial(n));
        Assert.assertFalse(App.getGeoContext(context).driveLeft(n));
    }
}
