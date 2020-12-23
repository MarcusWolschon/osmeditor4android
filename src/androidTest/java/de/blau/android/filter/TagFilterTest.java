package de.blau.android.filter;

import java.util.ArrayList;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.orhanobut.mockwebserverplus.MockWebServerPlus;

import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import de.blau.android.App;
import de.blau.android.LayerUtils;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.TestUtils;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Note: these test currently only test the filter logic not the UI
 * 
 * @author simon
 *
 */
public class TagFilterTest {

    MockWebServerPlus    mockServer = null;
    Context              context    = null;
    AdvancedPrefDatabase prefDB     = null;
    SQLiteDatabase       db;
    Main                 main       = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        UiDevice device = UiDevice.getInstance(instrumentation);
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        LayerUtils.removeImageryLayers(context);
        db = new TagFilterDatabaseHelper(context).getWritableDatabase();
        main.getMap().setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, context);
    }

    /**
     * Post test teardown
     */
    @After
    public void teardown() {
        db.delete("filterentries", null, null);
        db.close();
    }

    /**
     * Create a barrier node and check which rule it matches against
     */
    @Test
    public void tagFilterNode() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BARRIER, Tags.VALUE_KERB);
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();

            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(main, n1, tags);

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
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(main, w, tags);

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
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);
            logic.setTags(main, w, tags);

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
     * Test against a way tagged as building that is member of a Relation that the way will be included if the relation matches
     */
    @Test
    public void tagFilterWayInRelation() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, Tags.VALUE_YES);
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);

            ArrayList<OsmElement> members = new ArrayList<>();
            members.add(w);
            Relation r = logic.createRelation(main, "", members);
            logic.setTags(main, r, tags);

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
     * Test against a way tagged as building that is member of a Relation that the way will be included with way nodes if the relation matches
     */
    @Test
    public void tagFilterWayInRelationWithNodes() {
        try {
            TreeMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.KEY_BUILDING, "yes");
            Logic logic = App.getLogic();

            logic.performAdd(main, 100.0f, 100.0f);

            Node n1 = logic.getSelectedNode();
            logic.performAdd(main, 1000.0f, 1000.0f);
            Node n2 = logic.getSelectedNode();
            Way w = logic.getSelectedWay();
            logic.setSelectedNode(null);
            logic.setSelectedWay(null);

            ArrayList<OsmElement> members = new ArrayList<OsmElement>();
            members.add(w);
            Relation r = logic.createRelation(main, "", members);
            logic.setTags(main, r, tags);

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
