package de.blau.android.osm;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.app.Activity;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.ShadowWorkManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class OsmElementListTest {

    private static final int COUNT = 80000;
    Context                  context;
    Activity                 activity;
    Logic                    logic;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        activity = Robolectric.buildActivity(Main.class).create().resume().get();
        logic = App.getLogic();
    }

    /**
     * Load elements in to storage and then reference them via id
     */
    @Test
    public void fromIds() {
        logic.createCheckpoint(activity, R.string.undo_action_add);
        StorageDelegator delegator = App.getDelegator();
        for (int i = 1; i <= COUNT; i++) {
            delegator.insertElementSafe(OsmElementFactory.createNode((long) i, 1L, System.currentTimeMillis() / 1000, OsmElement.STATE_CREATED, 0, 0));
        }

        OsmElementList<Node> list = new OsmElementList<>();
        long[] ids = new long[COUNT];
        for (int i = 1; i <= COUNT; i++) {
            ids[i - 1] = i;
        }
        long start = System.currentTimeMillis();
        list.fromIds(delegator, Node.NAME, ids);
        System.out.println("Getting elements from ids took " + (System.currentTimeMillis() - start));

        List<Node> nodes = list.getElements();
        for (int i = 1; i <= COUNT; i++) {
            assertEquals(i, (int) nodes.get(i - 1).getOsmId());
        }
    }
}