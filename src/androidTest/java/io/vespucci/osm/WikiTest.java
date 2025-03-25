package io.vespucci.osm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Main;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WikiTest {

    public static final int TIMEOUT = 90;

    MockWebServerPlus       mockServer   = null;
    Context                 context      = null;
    AdvancedPrefDatabase    prefDB       = null;
    Main                    main         = null;
    private Instrumentation instrumentation;
    UiDevice                device       = null;
    ActivityScenario<Main>  mainScenario = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();

        mockServer = new MockWebServerPlus();
        HttpUrl mockBaseUrl = mockServer.server().url("");

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        shared.edit().putString(context.getString(R.string.config_osmWiki_key), mockBaseUrl.toString()).commit();

        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        mainScenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        instrumentation.removeMonitor(monitor);

        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
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
    }

    /**
     * Display map feature page for a feature with a page
     */
    @Test
    public void existingPage() {

        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        assertNotNull(n);
        main.performTagEdit(n, null, false, false);
        
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
    
        assertTrue(TestUtils.clickOverflowButton(device));
        
        mockServer.enqueue("wiki_language_failed");
        mockServer.enqueue("wiki_language_successful");
        mockServer.enqueue("dummy_page");
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_menu_mapfeatures), true));
        
        assertTrue(TestUtils.findText(device, false, "Dummy page", 60000));
        
        try {
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertTrue(request.getPath().endsWith("EN:Tag:place=village"));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Display map feature page for a feature without a page
     */
    @Test
    public void missingPage() {

        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 2205498737L);
        assertNotNull(n);
        main.performTagEdit(n, null, false, false);
        
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
        assertTrue(propertyEditor instanceof PropertyEditorActivity);
    
        assertTrue(TestUtils.clickOverflowButton(device));
        
        mockServer.enqueue("wiki_language_successful");
        mockServer.enqueue("dummy_page");
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.tag_menu_mapfeatures), true));
        
        assertTrue(TestUtils.findText(device, false, "Dummy page", 60000));
        
        try {
            mockServer.takeRequest();
            RecordedRequest request = mockServer.takeRequest();
            assertTrue(request.getPath().endsWith("EN-US:Map_Features"));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
