package de.blau.android.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import ch.poole.osm.josmfilterparser.ElementState;
import ch.poole.osm.josmfilterparser.Type;
import ch.poole.osm.josmfilterparser.Version;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.ShadowWorkManager;
import de.blau.android.osm.Node;
import de.blau.android.osm.PbfTest;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.util.Util;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class })
@LargeTest
public class WrapperTest {

    Wrapper          wrapper;
    StorageDelegator delegator;

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        Robolectric.buildActivity(Main.class).create().resume();
        delegator = App.getDelegator();
        delegator.reset(true);
        delegator.setCurrentStorage(PbfTest.read());
        Logic logic = App.getLogic();
        logic.setMap(new de.blau.android.Map(ApplicationProvider.getApplicationContext()), false);
        logic.getViewBox().fitToBoundingBox(logic.getMap(), delegator.getLastBox());
        wrapper = new Wrapper(ApplicationProvider.getApplicationContext());
    }

    /**
     * Check getUser (not implemented)
     */
    @Test
    public void getUserTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        try {
            wrapper.getUser();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iaex) {
            // expected
        }
    }

    /**
     * Check getId
     */
    @Test
    public void getIdTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        assertEquals(300852915L, wrapper.getId());
    }

    /**
     * Check getVersion
     */
    @Test
    public void getVersionTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        assertEquals(3, wrapper.getVersion());
    }

    /**
     * Check getChangeset (not implemented)
     */
    @Test
    public void getChangesetTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        try {
            wrapper.getChangeset();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iaex) {
            // expected
        }
    }

    /**
     * Check getTimestamp
     */
    @Test
    public void getTimestampTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        assertEquals(1321093164L, wrapper.getTimestamp());
    }

    /**
     * Check getState
     */
    @Test
    public void getStateTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertEquals(ElementState.State.UNCHANGED, wrapper.getState());
        Map<String, String> tags = new HashMap<>();
        tags.put("something", "new");
        delegator.setTags(way, tags);
        assertEquals(ElementState.State.MODIFIED, wrapper.getState());
    }

    /**
     * Check isClosed
     */
    @Test
    public void isClosedTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertFalse(wrapper.isClosed());

        way = (Way) delegator.getOsmElement(Way.NAME, 364548693L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertTrue(wrapper.isClosed());
    }

    /**
     * Check getNodeCount
     */
    @Test
    public void getNodeCountTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertEquals(11, wrapper.getNodeCount());

        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertEquals(14, wrapper.getNodeCount());
    }

    /**
     * Check getWayCount
     */
    @Test
    public void getWayCountTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertEquals(39, wrapper.getWayCount());

        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        assertEquals(2, wrapper.getWayCount());
    }

    /**
     * Check getAreaSize (not implemented)
     */
    @Test
    public void getAreaSizeTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        try {
            wrapper.getAreaSize();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iaex) {
            // expected
        }
    }

    /**
     * Check getWayLength
     */
    @Test
    public void getWayLengthTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertEquals(322, wrapper.getWayLength());
    }

    /**
     * Check getRoles
     */
    @Test
    public void getRolesTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 1378576419L);
        assertNotNull(node);
        wrapper.setElement(node);
        Set<String> roles = new HashSet<>(wrapper.getRoles());
        assertEquals(2, roles.size());
        assertTrue(roles.contains("platform"));
        assertTrue(roles.contains("platform_entry_only"));
    }

    /**
     * Check isSelected
     */
    @Test
    public void isSelectedTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        assertFalse(wrapper.isSelected());
        App.getLogic().setSelectedNode(node);
        assertTrue(wrapper.isSelected());
        App.getLogic().setSelectedNode(null);

        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertFalse(wrapper.isSelected());
        App.getLogic().setSelectedWay(way);
        assertTrue(wrapper.isSelected());
        App.getLogic().setSelectedWay(null);

        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertFalse(wrapper.isSelected());
        App.getLogic().setSelectedRelation(r);
        assertTrue(wrapper.isSelected());
        App.getLogic().setSelectedRelation(null);

    }

    /**
     * Check hasRole
     */
    @Test
    public void hasRoleTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.hasRole("stop"));
        assertFalse(wrapper.hasRole("test"));
    }

    /**
     * Check getPreset
     */
    @Test
    public void getPresetTest() {
        Object secondary = wrapper.getPreset("Highways|Streets|Secondary");
        assertTrue(secondary instanceof PresetItem);
        assertEquals("Secondary", ((PresetItem) secondary).getName());

        Object streets = wrapper.getPreset("Highways|Streets|*");
        assertTrue(streets instanceof PresetGroup);
        assertEquals("Streets", ((PresetGroup) streets).getName());
    }

    /**
     * getPreset should fail if no Context is supplied
     */
    @Test
    public void getPresetTes2t() {
        Wrapper wrapper2 = new Wrapper();
        try {
            wrapper2.getPreset("Highways|Streets|Secondary");
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException iaex) {
            // expected
        }
    }

    /**
     * Check matchesPreset
     */
    @Test
    public void matchesPresetTest() {
        Object secondary = wrapper.getPreset("Highways|Streets|Secondary");
        assertTrue(secondary instanceof PresetItem);
        Way way = (Way) delegator.getOsmElement(Way.NAME, 571087535L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertTrue(wrapper.matchesPreset(secondary));
        Object primary = wrapper.getPreset("Highways|Streets|Primary");
        assertTrue(primary instanceof PresetItem);
        assertFalse(wrapper.matchesPreset(primary));

        Object streets = wrapper.getPreset("Highways|Streets|*");
        assertTrue(streets instanceof PresetGroup);
        assertTrue(wrapper.matchesPreset(streets));
        assertTrue(wrapper.matchesPreset(streets));
    }

    /**
     * Check isIncomplete
     */
    @Test
    public void isIncompleteTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.isIncomplete());

        r = (Relation) delegator.getOsmElement(Relation.NAME, 7301251L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertFalse(wrapper.isIncomplete());
    }

    /**
     * Check isInDownloadedArea
     */
    @Test
    public void isInDownloadedAreaTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 303432L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.isInDownloadedArea());
    }

    /**
     * Check isAllInDownloadedArea
     */
    @Test
    public void isAllInDownloadedAreaTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 303432L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertFalse(wrapper.isAllInDownloadedArea());

        r = (Relation) delegator.getOsmElement(Relation.NAME, 7301251L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.isAllInDownloadedArea());
    }

    /**
     * Check is Inview
     */
    @Test
    public void isInviewTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 303432L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.isInview());
    }

    /**
     * Check isAllInview
     */
    @Test
    public void isAllInviewTest() {
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 303432L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertFalse(wrapper.isAllInview());
    }

    /**
     * Check isChild
     */
    @Test
    public void isChildTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        wrapper.setElement(node);
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        assertTrue(wrapper.isChild(Type.NODE, wrapper, Util.wrapInList(way)));
        Way way2 = (Way) delegator.getOsmElement(Way.NAME, 111762730L);
        assertNotNull(way2);
        assertFalse(wrapper.isChild(Type.NODE, wrapper, Util.wrapInList(way2)));

        wrapper.setElement(way);
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        assertTrue(wrapper.isChild(Type.WAY, wrapper, Util.wrapInList(r)));

        wrapper.setElement(way2);
        assertFalse(wrapper.isChild(Type.WAY, wrapper, Util.wrapInList(r)));
    }

    /**
     * Check isParent
     */
    @Test
    public void isParentTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        wrapper.setElement(way);
        assertTrue(wrapper.isParent(Type.NODE, wrapper, Util.wrapInList(node)));

        Way way2 = (Way) delegator.getOsmElement(Way.NAME, 111762730L);
        assertNotNull(way2);
        wrapper.setElement(way2);
        assertFalse(wrapper.isParent(Type.NODE, wrapper, Util.wrapInList(node)));

        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        wrapper.setElement(r);
        assertTrue(wrapper.isParent(Type.WAY, wrapper, Util.wrapInList(way)));
        assertFalse(wrapper.isParent(Type.WAY, wrapper, Util.wrapInList(way2)));
    }

    /**
     * Check getMatchingElements
     */
    @Test
    public void getMatchingElementsTest() {
        Way way = (Way) delegator.getOsmElement(Way.NAME, 111762730L);
        assertNotNull(way);
        wrapper.setElement(way);
        List<Object> objects = wrapper.getMatchingElements(new Version(8));
        assertEquals(441, objects.size());
        assertEquals(way, wrapper.getElement());
    }

    /**
     * Check toJosmFilterType
     */
    @Test
    public void toJosmFilterTypeTest() {
        Node node = (Node) delegator.getOsmElement(Node.NAME, 300852915L);
        assertNotNull(node);
        assertEquals(Type.NODE, Wrapper.toJosmFilterType(node));
        Way way = (Way) delegator.getOsmElement(Way.NAME, 75786133L);
        assertNotNull(way);
        assertEquals(Type.WAY, Wrapper.toJosmFilterType(way));
        Relation r = (Relation) delegator.getOsmElement(Relation.NAME, 8134573L);
        assertNotNull(r);
        assertEquals(Type.RELATION, Wrapper.toJosmFilterType(r));
    }
}