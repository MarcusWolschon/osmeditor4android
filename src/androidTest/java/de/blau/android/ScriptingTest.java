package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
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
import de.blau.android.javascript.Utils;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset.PresetItem;

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
     * Post-test teardown
     */
    @After
    public void teardown() {
    }

    /**
     * Test that the JS environment is properly sandboxed
     */
    @Test
    public void sandbox() {
        // normal scope
        String r = Utils.evalString(context, "sandbox1", "b = new BoundingBox();");
        Assert.assertEquals("(0,0,0,0)", r);
        r = Utils.evalString(context, "sandbox2", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10,false);");
        Assert.assertEquals("(-899,-899,899,899)", r);
        final String importError = "Sandbox should stop further importing";
        final String importStatement = "importClass(Packages.de.blau.android.App);";
        try {
            r = Utils.evalString(context, "sandbox3", importStatement);
            Assert.fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
        // scope for presets
        Map<String, List<String>> tags = new LinkedHashMap<String, List<String>>();
        List<String> v = new ArrayList<String>();
        v.add("value");
        tags.put("key", v);
        r = Utils.evalString(context, "sandbox4", "a = new java.util.ArrayList(); a.add('value1'); tags.put('key1',a);tags", tags, tags, "test",
                new HashMap<String, PresetItem>(), App.getCurrentPresets(context));
        Assert.assertEquals("{key=[value], key1=[value1]}", r);
        Logic logic = App.getLogic();
        try {
            r = Utils.evalString(context, "sandbox4", importStatement, logic);
            Assert.fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
        // scope for general scripting
        r = Utils.evalString(context, "sandbox5", "b = new BoundingBox();", logic);
        Assert.assertEquals("(0,0,0,0)", r);
        r = Utils.evalString(context, "sandbox6", "b = GeoMath.createBoundingBoxForCoordinates(0,0,10,false);", logic);
        Assert.assertEquals("(-899,-899,899,899)", r);
        r = Utils.evalString(context, "sandbox7", "logic.getModifiedNodes().size() + logic.getNodes().size()", logic);
        Assert.assertEquals("0", r);
        try {
            r = Utils.evalString(context, "sandbox8", importStatement, logic);
            Assert.fail(importError);
        } catch (EvaluatorException ex) {
            // carry on
        }
    }
}
