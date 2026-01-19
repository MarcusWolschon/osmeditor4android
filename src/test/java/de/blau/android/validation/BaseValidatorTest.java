package de.blau.android.validation;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.ShadowWorkManager;
import de.blau.android.osm.DelegatorUtil;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.DataStyleManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk = 33)
@LargeTest
public class BaseValidatorTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Robolectric.buildActivity(Main.class).create().resume();
    }

    /**
     * Test relation validation
     */
    @Test
    public void relationTest() {
        Validator v = App.getDefaultValidator(ApplicationProvider.getApplicationContext());
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Relation r = factory.createRelationWithNewId();
        int result = v.validate(r);
        assertEquals(Validator.NO_TYPE, result & Validator.NO_TYPE);
        assertEquals(Validator.EMPTY_RELATION, result & Validator.EMPTY_RELATION);
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, "test");
        d.setTags(r, tags);
        result = v.validate(r);
        assertEquals(0, result & Validator.NO_TYPE);
        Node n = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.addMemberToRelation(new RelationMember("test", n), r);
        result = v.validate(r);
        assertEquals(0, result & Validator.EMPTY_RELATION);
    }

    /**
     * Test noname, noref etc
     */
    @Test
    public void suppressedMissingTest() {
        Logic logic = App.newLogic();
        // this needs a lot of setup as highway validation relies on a valid map object
        DataStyleManager styles = App.getDataStyleManager(ApplicationProvider.getApplicationContext());
        styles.getStylesFromFiles(ApplicationProvider.getApplicationContext());
        de.blau.android.Map map = new de.blau.android.Map(ApplicationProvider.getApplicationContext());
        logic.setMap(map, false);
        map.setPrefs(ApplicationProvider.getApplicationContext(), new Preferences(ApplicationProvider.getApplicationContext()));
        //
        Validator v = App.getDefaultValidator(ApplicationProvider.getApplicationContext());
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();
        Way w = factory.createWayWithNewId();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.007));
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.008));
        d.addNodeToWay(n1, w);
        d.addNodeToWay(n2, w);
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_HIGHWAY, "primary");
        d.setTags(w, tags);
        int result = v.validate(w);
        assertEquals(Validator.MISSING_TAG, result & Validator.MISSING_TAG);
        tags.put(Tags.KEY_NONAME, Tags.VALUE_YES);
        d.setTags(w, tags);
        assertEquals(Validator.NOT_VALIDATED, w.getCachedProblems() & Validator.NOT_VALIDATED);
        result = v.validate(w);
        assertEquals(Validator.OK, result);
        //
        Node n = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        tags.clear();
        tags.put("tourism", "information");
        tags.put("information", "terminal");
        d.setTags(n, tags);
        result = v.validate(n);
        assertEquals(Validator.MISSING_TAG, result & Validator.MISSING_TAG);
        tags.put(Tags.KEY_NOREF, Tags.VALUE_YES);
        d.setTags(n, tags);
        assertEquals(Validator.NOT_VALIDATED, w.getCachedProblems() & Validator.NOT_VALIDATED);
        result = v.validate(n);
        assertEquals(Validator.OK, result);
    }

    /**
     * Test non-standard type
     */
    @Test
    public void nonStandardTypeTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        Validator v = App.getDefaultValidator(ctx);
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n = factory.createNodeWithNewId(0, 0);
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_LANDUSE, "greenhouse_horticulture");
        tags.put(Tags.KEY_WHEELCHAIR, Tags.VALUE_NO);
        d.setTags(n, tags);
        int result = v.validate(n);
        assertEquals(Validator.WRONG_ELEMENT_TYPE, result & Validator.WRONG_ELEMENT_TYPE);
        List<String> warnings = Arrays.asList(v.describeProblem(ctx, n));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains(ctx.getString(R.string.element_type_node)));
    }

    /**
     * Test correct area determination
     */
    @Test
    public void nonStandardTypeTest2() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        Validator v = App.getDefaultValidator(ctx);
        StorageDelegator d = new StorageDelegator();
        Way w = DelegatorUtil.addWayToStorage(d, true);
        Map<String, String> tags = new HashMap<>();
        tags.put("golf", "bunker");
        tags.put(Tags.KEY_NATURAL, "sand");
        d.setTags(w, tags);
        assertTrue(App.getDataStyleManager(ctx).switchTo(Preferences.DEFAULT_MAP_STYLE));
        FeatureStyle style = App.getDataStyleManager(ctx).matchStyle(w);  
        assertTrue(style.isArea());
        PresetItem pi = Preset.findBestMatch(App.getCurrentPresets(ctx), tags, null, null);
        assertFalse(pi.appliesTo().contains(ElementType.AREA));
        assertEquals(Validator.OK, v.validate(w));
    }
}