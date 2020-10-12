package de.blau.android.easyedit;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.PropertyEditorTest;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RelationTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        TestUtils.removeImageryLayers(context);
        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        logic = App.getLogic();
        logic.deselectAll();
        TestUtils.loadTestData(main, "test2.osm");
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        TestUtils.stopEasyEdit(main);
        TestUtils.zoomToLevel(device, main, 18);
    }

    /**
     * Select, show info dialog, delete, undelete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void selectRelation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Hiking", false, false));
        List<Relation> rels = App.getLogic().getSelectedRelations();
        Assert.assertNotNull(rels);
        Assert.assertEquals(1, rels.size());
        Relation relation = rels.get(0);
        List<RelationMember> origMembers = relation.getMembers();
        Assert.assertEquals(6490362L, relation.getOsmId());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_relationselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_information), true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "hiking"));
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.done), true, false));
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.delete), false, true));
        Assert.assertEquals(OsmElement.STATE_DELETED, relation.getState());
        Assert.assertTrue(TestUtils.clickMenuButton(device, context.getString(R.string.undo), false, false));
        Assert.assertEquals(OsmElement.STATE_UNCHANGED, relation.getState());
        TestUtils.clickText(device, true, context.getString(R.string.okay), true, false); // for the tip alert
        Assert.assertNotNull(relation.getMember(Way.NAME, 104148456L));
        List<RelationMember> members = relation.getMembers();
        Assert.assertEquals(origMembers.size(), members.size());
        for (int i = 0; i < members.size(); i++) {
            Assert.assertEquals(origMembers.get(i), members.get(i));
        }
    }

    /**
     * Select way, create relation, set tag, delete
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void createRelation() {
        map.getDataLayer().setVisible(true);
        TestUtils.unlock(device);
        TestUtils.zoomToLevel(device, main, 21);
        TestUtils.clickAtCoordinates(device, map, 8.3893820, 47.3895626, true);
        Assert.assertTrue(TestUtils.clickText(device, false, "Path", false, false));
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertEquals(104148456L, way.getOsmId());
        //
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        Assert.assertTrue(TestUtils.clickOverflowButton(device));
        TestUtils.scrollTo("Create relation");
        Assert.assertTrue(TestUtils.clickText(device, false, "Create relation", true, false));
        Assert.assertTrue(TestUtils.findText(device, false, "Add member"));
        TestUtils.clickUp(device);
        Assert.assertTrue(TestUtils.findText(device, false, "Relation type"));
        UiObject2 relationType = null;
        try {
            relationType = PropertyEditorTest.getField(device, "Relation type", 1);
        } catch (UiObjectNotFoundException e) {
            Assert.fail();
        }
        Assert.assertNotNull(relationType);
        relationType.click(); // NOSONAR
        relationType.setText(Tags.VALUE_MULTIPOLYGON); // can't find text in drop downs
        TestUtils.clickHome(device, true);
        List<Relation> relations = App.getLogic().getSelectedRelations();
        Assert.assertEquals(1, relations.size());
        Relation relation = relations.get(0);
        Assert.assertTrue(relation.getOsmId() < 0);
        Assert.assertTrue(relation.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON));
        Assert.assertTrue(TestUtils.clickMenuButton(device, "Delete", false, true));
    }
}
