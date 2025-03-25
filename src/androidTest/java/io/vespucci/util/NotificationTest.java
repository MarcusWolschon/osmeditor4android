package io.vespucci.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.location.Criteria;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.gpx.GpxTest;
import io.vespucci.osm.Node;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationTest {

    Main     main   = null;
    UiDevice device = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        // this sets the mock location permission
        instrumentation.getUiAutomation().executeShellCommand("appops set de.blau.android android:mock_location allow");
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        main = mActivityRule.getActivity();
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Test notification generation for data issues
     */
    @Test
    public void dataIssue() {
        TestUtils.loadTestData(main, "test3.osm");
        TestUtils.findNotification(device, main.getString(R.string.alert_data_issue));

        Node zumRueden = (Node) App.getDelegator().getOsmElement(Node.NAME, 370530329);
        assertNotNull(zumRueden);

        TestUtils.setupMockLocation(main, Criteria.ACCURACY_FINE);

        TestUtils.injectLocation(main, zumRueden.getLat() / 1E7D, zumRueden.getLon() / 1E7D, 1000, null);
        GpxTest.clickGpsButton(device);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_gps_goto), false, false));

        Map<String, String> tags = new TreeMap<>(zumRueden.getTags());
        tags.put("fixme", "test");
        final Logic logic = App.getLogic();
        logic.setTags(main, zumRueden, tags);
        main.invalidateMap();

        TestUtils.sleep(5000);
        TestUtils.zoomToNullIsland(logic, logic.getMap());
        double[] center = logic.getMap().getViewBox().getCenter();
        assertEquals(0D, center[0], 0.01);
        assertEquals(0D, center[1], 0.01);

        assertTrue(TestUtils.clickNotification(device, main.getString(R.string.alert_data_issue)));

        TestUtils.sleep(5000);

        center = logic.getMap().getViewBox().getCenter();
        assertEquals(zumRueden.getLon() / 1E7D, center[0], 0.01);
        assertEquals(zumRueden.getLat() / 1E7D, center[1], 0.01);
    }
}