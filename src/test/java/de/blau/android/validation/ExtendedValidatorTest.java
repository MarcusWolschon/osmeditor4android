package de.blau.android.validation;

import static de.blau.android.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class ExtendedValidatorTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Robolectric.buildActivity(Main.class).create().resume();
    }

    /**
     * Test untagged node validation
     */
    @Test
    public void untaggedNodeTest() {
        Validator v = new ExtendedValidator(ApplicationProvider.getApplicationContext(), App.getDefaultValidator(ApplicationProvider.getApplicationContext()));
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n = factory.createNodeWithNewId(toE7(51.476), toE7(0.007));
        d.insertElementSafe(n);
        int result = v.validate(n);
        assertEquals(Validator.UNTAGGED, result);

        Way w = factory.createWayWithNewId();
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.008));
        d.addNodeToWay(n, w);
        d.addNodeToWay(n2, w);
        d.insertElementSafe(n2);
        d.insertElementSafe(w);
        n.resetHasProblem();
        result = v.validate(n);
        assertEquals(Validator.OK, result);
    }

    /**
     * Detect missing required roles
     */
    @Test
    public void missingRoleTest() {
        Validator v = new ExtendedValidator(ApplicationProvider.getApplicationContext(), App.getDefaultValidator(ApplicationProvider.getApplicationContext()));
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();

        Relation r = factory.createRelationWithNewId();
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON);
        d.setTags(r, tags);
        Way w = factory.createWayWithNewId();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.007));
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.008));
        d.addNodeToWay(n1, w);
        d.addNodeToWay(n2, w);
        d.insertElementSafe(n1);
        d.insertElementSafe(n2);
        d.insertElementSafe(w);

        RelationMember member = new RelationMember("", w);
        d.addMemberToRelation(member, r);

        int result = v.validate(r);
        assertEquals(Validator.MISSING_ROLE, result);

        member.setRole(Tags.ROLE_OUTER);
        r.resetHasProblem();
        result = v.validate(r);
        assertEquals(Validator.OK, result);
    }

    /**
     * Detect looping relations
     */
    @Test
    public void relationLoopTest() {
        Validator v = new ExtendedValidator(ApplicationProvider.getApplicationContext(), App.getDefaultValidator(ApplicationProvider.getApplicationContext()));
        StorageDelegator d = App.getDelegator();
        OsmElementFactory factory = d.getFactory();

        Relation r = factory.createRelationWithNewId();
        d.insertElementSafe(r);
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, Tags.VALUE_ROUTE);
        tags.put(Tags.VALUE_ROUTE, "bicycle");
        tags.put(Tags.KEY_NAME, "name");
        tags.put("network", "lcn");
        d.setTags(r, tags);
        Way w = factory.createWayWithNewId();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.007));
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.008));
        d.addNodeToWay(n1, w);
        d.addNodeToWay(n2, w);
        d.insertElementSafe(n1);
        d.insertElementSafe(n2);
        d.insertElementSafe(w);

        RelationMember m1 = new RelationMember("", w);
        d.addMemberToRelation(m1, r);

        Relation r2 = factory.createRelationWithNewId();
        d.insertElementSafe(r2);
        RelationMember m2 = new RelationMember("", r2);
        d.addMemberToRelation(m2, r);

        int result = v.validate(r);
        assertEquals(Validator.OK, result);

        // add original relation as member to r2
        RelationMember m3 = new RelationMember("", r);
        d.addMemberToRelation(m3, r2);
        r.resetHasProblem();
        result = v.validate(r);
        assertEquals(Validator.RELATION_LOOP, result);
    }
}