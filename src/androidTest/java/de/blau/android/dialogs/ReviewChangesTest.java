package de.blau.android.dialogs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.osm.Node;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ReviewChangesTest {

    Context                 context = null;
    Main                    main    = null;
    UiDevice                device  = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main.getMap().setPrefs(main, prefs);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        final CountDownLatch signal = new CountDownLatch(1);
        App.getLogic().readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, 90);

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Display changes to review
     */
    @Test
    public void reviewChanges() {
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_review), true, false)); // menu
                                                                                                                    // item
        assertTrue(TestUtils.findText(device, false, "address 777 Schulstrasse"));
        assertTrue(TestUtils.clickText(device, false, "address 777 Schulstrasse", true, false));
        assertTrue(TestUtils.findText(device, false, "#210461089"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true, false));

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(0); // dialog close button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
    }
    
    /**
     * Display changes to review
     */
    @Test
    public void reviewChanges2() {
        Logic logic = App.getLogic();
        Node bd = (Node) App.getDelegator().getOsmElement(Node.NAME, 101792984L);
        assertNotNull(bd);
        Map<String,String> tags = new HashMap<>(bd.getTags());
        tags.put(Tags.KEY_NAME, "Dietikonberg");
        tags.put(Tags.KEY_WIKIPEDIA, "en:Bergdietikon");
        
        logic.setTags(main, bd, tags);
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_review), true, false));
        assertTrue(TestUtils.findText(device, false, "Dietikonberg"));
        assertTrue(TestUtils.clickText(device, false, "Dietikonberg", true, false));
        assertTrue(TestUtils.findText(device, false, "#101792984"));
          
        assertTrue(TestUtils.findText(device, false, "Bergdietikon"));
        TestUtils.scrollTo("de:Bergdietikon", true);
        
// Scrolling horizontally would be nice here        
//        int middleV = device.getDisplayHeight() / 2;
//        TestUtils.drag(device, device.getDisplayWidth() - 50f, middleV, 50f, middleV, 100);   
//        assertTrue(TestUtils.findText(device, false, "en:Bergdietikon"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.Done), true, false));

        UiSelector uiSelector = new UiSelector().className("android.widget.Button").instance(0); // dialog close button
        UiObject button = device.findObject(uiSelector);
        try {
            button.click();
        } catch (UiObjectNotFoundException e1) {
            fail(e1.getMessage());
        }
    }
    
    /**
     * Goto element
     */
    @Test
    public void gotoChange() {
        assertTrue(TestUtils.clickMenuButton(device, main.getString(R.string.menu_transfer), false, true));

        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_transfer_review), true, false)); 
        assertTrue(TestUtils.findText(device, false, "address 777 Schulstrasse"));
        assertTrue(TestUtils.clickText(device, false, "address 777 Schulstrasse", true, false));
        assertTrue(TestUtils.findText(device, false, "#210461089"));
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.goto_element), true, false));

        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_wayselect)));
    }
}
