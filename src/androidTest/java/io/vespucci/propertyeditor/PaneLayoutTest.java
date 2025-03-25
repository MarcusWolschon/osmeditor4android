package io.vespucci.propertyeditor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.TestUtils;
import io.vespucci.osm.Node;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.propertyeditor.PropertyEditorActivity;
import io.vespucci.propertyeditor.PropertyEditorData;
import io.vespucci.propertyeditor.PropertyEditorFragment;

/**
 * Force an edit in pane layout
 * 
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PaneLayoutTest {

    Context                 context      = null;
    AdvancedPrefDatabase    prefDB       = null;
    Main                    main         = null;
    UiDevice                device       = null;
    Map                     map          = null;
    Logic                   logic        = null;
    private Instrumentation instrumentation;
    ActivityScenario<Main>  mainScenario = null;

    @Rule
    public ActivityScenarioRule<Main> activityScenarioRule = new ActivityScenarioRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ActivityMonitor monitor = instrumentation.addMonitor(Main.class.getName(), null, false);
        mainScenario = ActivityScenario.launch(Main.class);
        main = (Main) instrumentation.waitForMonitorWithTimeout(monitor, 30000);

        instrumentation.removeMonitor(monitor);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(() -> main.showSimpleActionsButton());

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToNullIsland(logic, map);
        App.getTaskStorage().reset();
        mainScenario.moveToState(State.DESTROYED);
    }

    /**
     * Edit a tag in the PropertyEditor - restart app - check that edit is still there
     */
    @Test
    public void editTag() {
        ActivityMonitor monitor = instrumentation.addMonitor(PropertyEditorActivity.class.getName(), null, false);
        Node n = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984);
        assertNotNull(n);

        PropertyEditorData[] single = new PropertyEditorData[1];
        single[0] = new PropertyEditorData(n, null);

        final Intent intent = PropertyEditorActivity.buildIntent(main, single, false, false, null, null);
        intent.putExtra(PropertyEditorFragment.PANELAYOUT, true);
        try (ActivityScenario<PropertyEditorActivity> scenario = ActivityScenario.launch(intent)) {
            Activity propertyEditor = instrumentation.waitForMonitorWithTimeout(monitor, 30000);
            assertTrue(propertyEditor instanceof PropertyEditorActivity);
            TestUtils.clickText(device, true, main.getString(R.string.menu_tags), false, false);
            final String original = "Bergdietikon";
            UiObject2 o = device.wait(Until.findObject(By.clickable(true).textStartsWith(original)), 500);
            assertNotNull(o);
            final String edited = "dietikonBerg";
            UiObject editText = device.findObject(new UiSelector().clickable(true).textStartsWith(original));
            try {
                editText.click();
                editText.setText(edited);
                instrumentation.sendCharacterSync(KeyEvent.KEYCODE_TAB); // this seems to be necessary to guarantee
                                                                         // things will be changed
            } catch (UiObjectNotFoundException e) {
                fail(e.getMessage());
            }

            // check that we have the change
            o = device.wait(Until.findObject(By.clickable(true).textStartsWith(edited)), 500);
            assertNotNull(o);
        }
    }
}
