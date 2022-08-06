package de.blau.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.util.Util;

@Config(shadows = { ShadowWorkManager.class })
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class LogicTest {

    private Main main;

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        main = Robolectric.setupActivity(Main.class);
        App.getDelegator().reset(true);
    }

    /**
     * Test if node gets merged to the correct location
     * 
     * @see https://github.com/MarcusWolschon/osmeditor4android/issues/1714
     */
    @Test
    public void panhandle() {
        UnitTestUtils.loadTestData(getClass(), "panhandle.osm");
        Way pan = (Way) App.getDelegator().getOsmElement(Way.NAME, -1);
        assertNotNull(pan);
        main.zoomTo(pan);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, -8);
        App.getLogic().performJoinNodeToWays(null, Util.wrapInList(pan), n);
        assertEquals(5, pan.getNodes().indexOf(n));
    }
}
