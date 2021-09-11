package de.blau.android.validation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.StorageDelegatorTest;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class BaseValidatorTest {

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
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.addMemberToRelation(n, "test", r);
        result = v.validate(r);
        assertEquals(0, result & Validator.EMPTY_RELATION);
    }

    /**
     * Test noname, noref etc
     */
    @Test
    public void suppressedMissingTest() {
        // this needs a lot of setup as highway validation relies on a valid map object
        DataStyle.getStylesFromFiles(ApplicationProvider.getApplicationContext()); 
        Logic logic = App.newLogic();
        de.blau.android.Map map = new de.blau.android.Map(ApplicationProvider.getApplicationContext());
        logic.setMap(map, false);
        map.setPrefs(ApplicationProvider.getApplicationContext(), new Preferences(ApplicationProvider.getApplicationContext()));
        //
        Validator v = App.getDefaultValidator(ApplicationProvider.getApplicationContext());
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();
        Way w = factory.createWayWithNewId();
        Node n1 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.007));
        Node n2 = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.008));
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
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        tags.clear();
        tags.put("tourism", "information");
        tags.put("information", "guidepost");
        d.setTags(n, tags);
        result = v.validate(n);
        assertEquals(Validator.MISSING_TAG, result & Validator.MISSING_TAG);
        tags.put(Tags.KEY_NOREF, Tags.VALUE_YES);
        d.setTags(n, tags);
        assertEquals(Validator.NOT_VALIDATED, w.getCachedProblems() & Validator.NOT_VALIDATED);
        result = v.validate(n);
        assertEquals(Validator.OK, result);
    }
}