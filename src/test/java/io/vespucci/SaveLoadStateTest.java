package io.vespucci;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.Main;
import io.vespucci.SignalHandler;
import io.vespucci.osm.Storage;
import io.vespucci.osm.StorageDelegator;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class SaveLoadStateTest {
    Main    main    = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        main = Robolectric.buildActivity(Main.class).create().resume().get();
    }

    /**
     * Save and then restore internal state
     */
    @Test
    public void saveThenLoad() {
        StorageDelegator delegator = App.getDelegator();
        Storage storage = delegator.getCurrentStorage();
        int nodeCount = storage.getNodeCount();
        int wayCount = storage.getWayCount();
        int relCount = storage.getRelationCount();
        App.getLogic().save(main);
        delegator.reset(false);
        storage = delegator.getCurrentStorage();
        Assert.assertEquals(0, storage.getNodeCount());
        Assert.assertEquals(0, storage.getWayCount());
        Assert.assertEquals(0, storage.getRelationCount());
        final CountDownLatch signal = new CountDownLatch(1);
        main.runOnUiThread(() -> App.getLogic().loadStateFromFile(main, new SignalHandler(signal)));

        try {
            signal.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        storage = delegator.getCurrentStorage();
        Assert.assertEquals(nodeCount, storage.getNodeCount());
        Assert.assertEquals(wayCount, storage.getWayCount());
        Assert.assertEquals(relCount, storage.getRelationCount());
    }
}
