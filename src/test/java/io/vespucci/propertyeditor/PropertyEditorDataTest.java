package io.vespucci.propertyeditor;

import static io.vespucci.osm.DelegatorUtil.toE7;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElementFactory;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.RelationMemberPosition;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Way;
import io.vespucci.propertyeditor.PropertyEditorData;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class PropertyEditorDataTest {

    /**
     * Test that parent relations are handled properly
     */
    @Test
    public void sameRoleInParents() {
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Node n1 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n1);
        Node n2 = factory.createNodeWithNewId(toE7(51.476), toE7(0.006));
        d.insertElementSafe(n2);

        Relation r = factory.createRelationWithNewId();
        RelationMember member1 = new RelationMember("", n1);
        List<RelationMember> members = new ArrayList<>();
        members.add(member1);
        RelationMember member2 = new RelationMember("test2", n2);
        members.add(member2);
        RelationMember member3 = new RelationMember(Way.NAME, 1234567L, "test3");
        members.add(member3);
        RelationMember member4 = new RelationMember("", n1);
        members.add(member4);
        App.newLogic();
        Logic logic = App.getLogic();
        logic.addRelationMembers(null, r, members);

        PropertyEditorData pd = new PropertyEditorData(n1, null);

        Set<RelationMemberPosition> rmds = pd.parents.get(r.getOsmId());
        assertNotNull(rmds);
        assertEquals(2, rmds.size());
        List<RelationMemberPosition> rmdList = new ArrayList<>(rmds);
        assertEquals(0, rmdList.get(0).getPosition());
        assertEquals(3, rmdList.get(1).getPosition());
    }
}