package io.vespucci.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.SignalHandler;
import io.vespucci.osm.ApiTest;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;
import io.vespucci.util.GeoContext;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class GeoContextTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        App.newLogic();
    }

    /**
     * Test if the US is imperial
     */
    @Test
    public void imperialTest() {
        GeoContext gc = new GeoContext(ApplicationProvider.getApplicationContext());
        assertTrue(gc.imperial(-77.0351535, 38.8894971));
    }

    /**
     * Test for elements in the UK
     */
    @Test
    public void UK() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("london.osm");
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 451984385L);
        Assert.assertNotNull(w);
        GeoContext geoContext = App.getGeoContext(ApplicationProvider.getApplicationContext());
        Assert.assertTrue(geoContext.imperial(w));
        Assert.assertTrue(geoContext.driveLeft(w));
        Assert.assertTrue(geoContext.getIsoCodes(w).contains("GB"));
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 1012733590L);
        Assert.assertNotNull(n);
        Assert.assertTrue(geoContext.imperial(n));
        Assert.assertTrue(geoContext.driveLeft(n));
        Assert.assertTrue(geoContext.getIsoCodes(n).contains("GB"));
    }

    /**
     * Test for elements in Washington DC
     */
    @Test
    public void DC() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("dc.osm");
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 485299712L);
        Assert.assertNotNull(w);
        GeoContext geoContext = App.getGeoContext(ApplicationProvider.getApplicationContext());
        Assert.assertTrue(geoContext.imperial(w));
        Assert.assertFalse(geoContext.driveLeft(w));
        Assert.assertTrue(geoContext.getIsoCodes(w).contains("US"));
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 309551133L);
        Assert.assertNotNull(n);
        Assert.assertTrue(geoContext.imperial(n));
        Assert.assertFalse(geoContext.driveLeft(n));
        Assert.assertTrue(geoContext.getIsoCodes(n).contains("US"));
    }

    /**
     * Test for elements in Bergdietikon, Switzerland
     */
    @Test
    public void BD() {
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(ApplicationProvider.getApplicationContext(), is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        Way w = (Way) App.getDelegator().getOsmElement(Way.NAME, 27009604L);
        Assert.assertNotNull(w);
        GeoContext geoContext = App.getGeoContext(ApplicationProvider.getApplicationContext());
        Assert.assertFalse(geoContext.imperial(w));
        Assert.assertFalse(geoContext.driveLeft(w));
        Assert.assertTrue(geoContext.getIsoCodes(w).contains("CH"));
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        Assert.assertNotNull(n);
        Assert.assertFalse(geoContext.imperial(n));
        Assert.assertFalse(geoContext.driveLeft(n));
        Assert.assertTrue(geoContext.getIsoCodes(n).contains("CH"));
    }
}