package io.vespucci.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.AsyncResult;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.ShadowWorkManager;
import io.vespucci.SignalUtils;
import io.vespucci.exception.OsmException;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.osm.Node;
import io.vespucci.osm.Way;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.Util;
import io.vespucci.validation.Validator;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class OverpassApiTest {

    public static final int TIMEOUT = 10;

    private MockWebServerPlus mockServer = null;
    private Main              main       = null;
    private Preferences       prefs      = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("/");
        main = Robolectric.buildActivity(Main.class).create().resume().get();

        System.out.println("mock overpass api url " + mockBaseUrl.toString()); // NOSONAR
        Logic logic = App.getLogic();
        prefs = new Preferences(main);
        prefs.setOverpassServer(mockBaseUrl.toString());
        logic.setPrefs(prefs);
        logic.getMap().setPrefs(main, prefs);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        try {
            mockServer.server().shutdown();
        } catch (IOException ioex) {
            System.out.println("Stopping mock webserver exception " + ioex); // NOSONAR
        }
        prefs.close();
    }

    /**
     * Simple overpass query
     */
    @Test
    public void overpassQuery() {
        mockServer.enqueue("overpass");
        String query = "[out:xml][timeout:90];" + "(" + "node[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);"
                + "way[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);"
                + "relation[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);" + ");" + "(._;>;);" + "out meta;";
        io.vespucci.overpass.Server.query(main, query, false, false);
        runLooper();
        Way way = (Way) App.getDelegator().getOsmElement(Way.NAME, 47977728L);
        assertNotNull(way);
        assertEquals(12, way.getOsmVersion());
    }
    
    /**
     * Simple overpass query with selection of results
     */
    @Test
    public void overpassQueryWithSelect() {
        mockServer.enqueue("overpass");
        String query = "[out:xml][timeout:90];" + "(" + "node[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);"
                + "way[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);"
                + "relation[highway=residential](47.3795692,8.378648,47.3822631,8.3813708);" + ");" + "(._;>;);" + "out meta;";
        io.vespucci.overpass.Server.query(main, query, false, true);
        runLooper();
        Way way = (Way) App.getDelegator().getOsmElement(Way.NAME, 47977728L);
        assertNotNull(way);
        assertTrue(App.getLogic().getSelectedElements().contains(way));
        Node wayNode = (Node) App.getDelegator().getOsmElement(Node.NAME,289981009L);
        assertNotNull(wayNode);
        assertFalse(App.getLogic().getSelectedElements().contains(wayNode));
    }

    /**
     * Super ugly hack to get the looper to run
     */
    private void runLooper() {
        try {
            Thread.sleep(3000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
        shadowOf(Looper.getMainLooper()).idle();
    }

    // 47.3795692,8.378648,47.3822631,8.3813708
    @Test
    public void replacePlaceholders() {
        String query = "[out:xml][timeout:90];" + "(" + "node[highway=residential]({{bbox}});" + "way[highway=residential]({{bbox}});"
                + "relation[highway=residential]({{bbox}});" + ");" + "(._;>;);" + "out meta;";

        App.getLogic().getMap().getViewBox().set(83786480, 473795692, 83813708, 473822631);
        assertTrue(io.vespucci.overpass.Server.replacePlaceholders(main, query).contains("47.3795692,8.378648,47.3822631,8.3813708"));
    }
}
