package de.blau.android.easyedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
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
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.SignalUtils;
import de.blau.android.TestUtils;
import de.blau.android.osm.Way;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ElementInfoDialogTest {

    public static final int       TIMEOUT       = 90;

    Context                       context       = null;
    Main                          main          = null;
    private Instrumentation       instrumentation;
    UiDevice                      device        = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext();
        main = mActivityRule.getActivity();
        LayerUtils.removeImageryLayers(context);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);
        TestUtils.unlock(device);
    }

    /**
     * Select a way, display dialog, check that it starts correctly, exit
     */
    @Test
    public void dialog() {
        final CountDownLatch signal = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test1.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal));
        SignalUtils.signalAwait(signal, TIMEOUT);
        TestUtils.sleep(2000);
        TestUtils.clickAtCoordinates(device, main.getMap(), 8.3855944D, 47.3880326D);
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.actionmode_wayselect), 5000));
        Way w = App.getLogic().getSelectedWay();
        assertNotNull(w);
        assertEquals(49695069, w.getOsmId());
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(main.getString(R.string.menu_information), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true));
        
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.done), true));
        App.getLogic().setTags(main, w, new HashMap<>());
        assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo(main.getString(R.string.menu_information), false);
        assertTrue(TestUtils.clickText(device, false, main.getString(R.string.menu_information), true));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.original), 5000));
        assertTrue(TestUtils.findText(device, false, main.getString(R.string.tag_form_untagged_element), 5000));
    }
}
