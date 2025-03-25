package io.vespucci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.javascript.EvaluatorException;

import android.app.Instrumentation;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import io.vespucci.App;
import io.vespucci.LayerUtils;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.javascript.Utils;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ScriptingTest {

    Context  context = null;
    UiDevice device  = null;
    Main     main    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        device = UiDevice.getInstance(instrumentation);
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        main = mActivityRule.getActivity();
        main.getMap().setPrefs(main, prefs);
        App.getDelegator().reset(false);
        App.getDelegator().setOriginalBox(ViewBox.getMaxMercatorExtent());
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
    }

    /**
     * Test that the JS environment is properly sandboxed
     */
    @Test
    public void sandbox() {
        // normal scope
        String r = Utils.evalString(context, "sandbox1", "b = new BoundingBox();");
        assertEquals("(0,0,0,0)", r);
        r = Utils.evalString(context, "sandbox2", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10);");
        assertEquals("(-899,-899,899,899)", r);
        final String importError = "Sandbox should stop further importing";
        final String importStatement = "importClass(Packages.de.blau.android.App);";
        try {
            Utils.evalString(context, "sandbox3", importStatement);
            fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
        // scope for presets
        Map<String, List<String>> tags = new LinkedHashMap<>();
        List<String> v = new ArrayList<>();
        v.add("value");
        tags.put("key", v);
        r = Utils.evalString(context, "sandbox4", "a = new java.util.ArrayList(); a.add('value1'); tags.put('key1',a);tags", tags, tags, "test",
                new HashMap<>(), App.getCurrentPresets(context));
        assertEquals("{key=[value], key1=[value1]}", r);
        Logic logic = App.getLogic();
        try {
            Utils.evalString(context, "sandbox4", importStatement, logic);
            fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
        // scope for general scripting
        r = Utils.evalString(context, "sandbox5", "b = new BoundingBox();", logic);
        assertEquals("(0,0,0,0)", r);
        r = Utils.evalString(context, "sandbox6", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10);", logic);
        assertEquals("(-899,-899,899,899)", r);
        r = Utils.evalString(context, "sandbox7", "logic.getModifiedNodes().size() + logic.getNodes().size()", logic);
        assertEquals("0", r);
        try {
            Utils.evalString(context, "sandbox8", importStatement, logic);
            fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
    }

    /**
     * Check that we can catch an illegal object written to the tags map
     */
    @Test
    public void catchClassCastException() {
        // scope for presets
        Map<String, List<String>> tags = new LinkedHashMap<>();
        List<String> v = new ArrayList<>();
        v.add("value");
        tags.put("key", v);
        try {
            Utils.evalString(context, "sandbox", "tags.put('Testoutput',['Value TestKey = ' + tags.get('key')[0]])", tags, tags, "test", new HashMap<>(),
                    App.getCurrentPresets(context));
            fail("Should have thrown an exception");
        } catch (ClassCastException cce) {
            // carry on
        }
    }
}
