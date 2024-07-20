package de.blau.android.filter;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;

/**
 * Note: these test currently only test the filter logic not the UI
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class TagFilterTest {

    Context context = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        Robolectric.buildActivity(Main.class).create().resume();
    }

    /**
     * Post test teardown
     */
    @After
    public void teardown() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            db.delete("filterentries", null, null);
        }
    }

    /**
     * Create a barrier node and check which rule it matches against
     */
    @Test
    public void tagFilterNode() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BARRIER, Tags.VALUE_KERB);
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();

            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(null, n1, tags);

            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", Tags.KEY_BUILDING, null);

            TagFilter f = new TagFilter(context);
            Assert.assertTrue(!f.include(n1, false));
            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", "b.*", null);
            f = new TagFilter(context);
            Assert.assertTrue(f.include(n1, false));
            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "node", null, ".*l");
            f = new TagFilter(context);
            Assert.assertTrue(f.include(n1, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test against a way tagged as building, with filter that doesn't include way nodes
     */
    @Test
    public void tagFilterWay() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(null, w, tags);

            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "way", Tags.KEY_BUILDING, null);

            TagFilter f = new TagFilter(context);
            Assert.assertTrue(f.include(w, false));
            Assert.assertTrue(!f.include(n1, false));
            Assert.assertTrue(!f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test against a way tagged as building, with filter that includes way nodes
     */
    @Test
    public void tagFilterWayWithNodes() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(null, w, tags);

            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "way+", Tags.KEY_BUILDING, null);

            TagFilter f = new TagFilter(context);
            Assert.assertTrue(f.include(w, false));
            Assert.assertTrue(f.include(n1, false));
            Assert.assertTrue(f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test against a way tagged as building that is member of a Relation that the way will be included if the relation
     * matches
     */
    @Test
    public void tagFilterWayInRelation() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, Tags.VALUE_YES);
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);

            ArrayList<OsmElement> members = new ArrayList<>();
            members.add(w);
            Relation r = logic.createRelation(null, "", members);
            logic.setTags(null, r, tags);

            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, Relation.NAME, Tags.KEY_BUILDING, null);

            TagFilter f = new TagFilter(context);
            Assert.assertTrue(f.include(w, false));
            Assert.assertTrue(!f.include(n1, false));
            Assert.assertTrue(!f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test against a way tagged as building that is member of a Relation that the way will be included with way nodes
     * if the relation matches
     */
    @Test
    public void tagFilterWayInRelationWithNodes() {
        try (TagFilterDatabaseHelper helper = new TagFilterDatabaseHelper(context); SQLiteDatabase db = helper.getWritableDatabase()) {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(null, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(null, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);

            ArrayList<OsmElement> members = new ArrayList<OsmElement>();
            members.add(w);
            Relation r = logic.createRelation(null, "", members);
            logic.setTags(null, r, tags);

            insertTagFilterRow(db, TagFilter.DEFAULT_FILTER, true, true, "relation+", Tags.KEY_BUILDING, null);

            TagFilter f = new TagFilter(context);
            Assert.assertTrue(f.include(w, false));
            Assert.assertTrue(f.include(n1, false));
            Assert.assertTrue(f.include(n2, false));
        } catch (OsmIllegalOperationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Insert a row in the tag filter DB
     * 
     * @param db a writable database
     * @param filter the filter name
     * @param active if this entry is active
     * @param include if the object should be included if it matches
     * @param type OSM element type
     * @param key the key
     * @param value the value
     */
    private void insertTagFilterRow(SQLiteDatabase db, String filter, boolean active, boolean include, String type, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("filter", filter);
        values.put("active", active ? 1 : 0);
        values.put("include", include ? 1 : 0);
        values.put("type", type);
        values.put("key", key);
        values.put("value", value);
        db.insert("filterentries", null, values);
    }
}
