package de.blau.android.osm;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.GeoMath;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GeometryEditsTest {
	
	Context context = null;
	Main main = null;
	    
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		main = mActivityRule.getActivity();
		Preferences prefs = new Preferences(context);
		prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
		main.getMap().setPrefs(main, prefs);
    	App.getDelegator().reset(false);
		App.getDelegator().setOriginalBox(BoundingBox.getMaxMercatorExtent());		
    }
    
    @After
    public void teardown() {
    }

    @UiThreadTest
    @Test
	public void mergeSplit() {
    	try {
    		// setup some stuff to test relations
    		Logic logic = App.getLogic();
			logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		logic.setSelectedRelation(null);
    		logic.performAdd(main, 100.0f, 100.0f);
    		Assert.assertNotNull(logic.getSelectedNode());
    		System.out.println(logic.getSelectedNode());
    		Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
    		logic.performAdd(main, 150.0f, 150.0f);
    		logic.performAdd(main, 200.0f, 200.0f);
    		logic.performAdd(main, 250.0f, 250.0f);
    		Way w1 = logic.getSelectedWay();
    		Assert.assertNotNull(w1);
    		System.out.println("ApplicationTest created way " + w1.getOsmId());
    		ArrayList<Node> nList1 = (ArrayList<Node>) w1.getNodes();
    		Assert.assertEquals(4, nList1.size());
    		final Node n1 = nList1.get(0);
    		System.out.println("ApplicationTest n1 " + n1.getOsmId());
    		final Node n2 = nList1.get(1);
    		System.out.println("ApplicationTest n2 " + n2.getOsmId());
    		final Node n3 = nList1.get(2);
    		System.out.println("ApplicationTest n3 " + n3.getOsmId());
    		final Node n4 = nList1.get(3);
    		System.out.println("ApplicationTest n4 " + n4.getOsmId());
			logic.performSplit(main, n2);
    	   
    		ArrayList<Way> wList1 = (ArrayList<Way>) logic.getWaysForNode(n3);
    		Assert.assertEquals(1, wList1.size());
    		Way w2 = wList1.get(0);
    		System.out.println("ApplicationTest split created way " + w2.getOsmId());
    		ArrayList<Node> nList2 = (ArrayList<Node>) w2.getNodes();
    		System.out.print("Way 2 contains nodes");
    		for (Node n:nList2) System.out.print(" " + n.getOsmId());
    		System.out.println();
    		Relation r1 = logic.createRestriction(main, w1, n2, w2, "test rest");
    		ArrayList<OsmElement> mList1 = r1.getMemberElements();
    		Assert.assertEquals(3, mList1.size());
    		Assert.assertEquals(w1,mList1.get(0));
    		Assert.assertEquals(n2,mList1.get(1));
    		Assert.assertEquals(w2,mList1.get(2));
    		Assert.assertEquals(w1.getParentRelations().size(),1);
    		Assert.assertEquals(w1.getParentRelations().get(0),r1);
    		Assert.assertEquals(n2.getParentRelations().size(),1);
    		Assert.assertEquals(n2.getParentRelations().get(0),r1);
    		Assert.assertEquals(w2.getParentRelations().size(),1);
    		Assert.assertEquals(w2.getParentRelations().get(0),r1);
    		
    		// split way 2
    	    logic.performSplit(main, n3);
    	    
    		ArrayList<Way> wList2 = (ArrayList<Way>) logic.getWaysForNode(n3);

    		Assert.assertEquals(2, wList2.size());
    		Assert.assertEquals(wList2.get(0),w2);
    		Assert.assertEquals(wList2.get(0).getParentRelations().get(0),r1);
    		Assert.assertEquals(wList2.get(1).getParentRelations(),null); // special case for restrictions

    		// merge former way 2
    		logic.performMerge(main, wList2.get(0), wList2.get(1));
    		Assert.assertEquals(3, w2.getNodes().size());
    		Assert.assertEquals(w2.getParentRelations().get(0),r1);

    		// add w2 to a normal relation and split
    		ArrayList<OsmElement> mList2 = new ArrayList<OsmElement>();
    		mList2.add(w2);
    		logic.createRelation(main, "test", mList2);
    		Assert.assertEquals(2, w2.getParentRelations().size());
       		
    	    logic.performSplit(main, n3);
    	    
    		wList2 = (ArrayList<Way>) logic.getWaysForNode(n3);
    		Assert.assertEquals(2,wList2.get(0).getParentRelations().size()); // should contain both rels
    		Assert.assertEquals(1,wList2.get(1).getParentRelations().size()); // just the 2nd one
    	} catch (Exception igit) {
    		Assert.fail(igit.getMessage());
    	}  
	}
    
    @UiThreadTest
    @Test
    /**
     * This tries to test adding nodes to existing ways taking the tolerance area in to account
     */
    public void addNodeToWay() {
    	try {
    		App.getDelegator().setOriginalBox(new BoundingBox(-1,-1,1,1)); // force ops to be outside box
    		Logic logic = App.getLogic();
    		Map map = main.getMap();
    		logic.setZoom(map, 20);
    		float tolerance = DataStyle.getCurrent().wayToleranceValue;
    		System.out.println("Tolerance " + tolerance);

    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		logic.setSelectedRelation(null);
    		logic.performAdd(main, 1000.0f, 0.0f);
    		Node wn = logic.getSelectedNode();
    		BoundingBox box = new BoundingBox(wn.getLon(), wn.getLat());
    		float wnY = getY(logic, wn);
    		float wnX = getX(logic, wn);
    		System.out.println("WN1 X " + wnX + " Y " + wnY);
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		wn = logic.getSelectedNode();
    		box.union(wn.getLon(), wn.getLat());
    		wnY = getY(logic, wn);
    		wnX = getX(logic, wn);
    		System.out.println("WN2 X " + wnX + " Y " + wnY);
    		Way w1 = logic.getSelectedWay();
    		Assert.assertEquals(2, w1.getNodes().size());
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		// 2nd way tolerance + 1 pixels away

    		float X = 1000.0f + tolerance + 1.0f;
    		logic.performAdd(main, X, -tolerance);
    		wn = logic.getSelectedNode();
    		wnY = getY(logic, wn);
    		wnX = getX(logic, wn);
       		box.union(wn.getLon(), wn.getLat());
       		map.setViewBox(box);
    		System.out.println("WN3 X " + wnX + " Y " + wnY);
    		logic.performAdd(main, X, 1000.0f + tolerance);
    		wn = logic.getSelectedNode();
    		wnY = getY(logic, wn);
    		wnX = getX(logic, wn);
       		box.union(wn.getLon(), wn.getLat());
       		map.setViewBox(box);
    		System.out.println("WN4 X " + wnX + " Y " + wnY);
    		Way w2 = logic.getSelectedWay();
    		Assert.assertEquals(2, w2.getNodes().size());
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);

    		Node tempNode = logic.performAddOnWay(main, null,X, 500.0f, false);
    		Node n1 = logic.getSelectedNode();
       		box.union(n1.getLon(), n1.getLat());
       		map.setViewBox(box);
    		Assert.assertEquals(n1,tempNode);
    		Assert.assertEquals(1, logic.getWaysForNode(n1).size());
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);

    		Assert.assertEquals(2, w1.getNodes().size()); // should be unchanged
    		Assert.assertEquals(3, w2.getNodes().size());

    		// add again, shouldn't change anything
    		float n1Y = getY(logic, n1);
    		float n1X = getX(logic, n1);
    		logic.performAdd(main, n1X, n1Y);
    		Node n3 = logic.getSelectedNode();
    		Assert.assertEquals(n1,n3);
    		Assert.assertEquals(2, w1.getNodes().size()); // should be unchanged
    		Assert.assertEquals(3, w2.getNodes().size());
    	} catch (Exception igit) {
    		Assert.fail(igit.getMessage());
    	}  
    }
    
    @UiThreadTest
    @Test
    public void joinToWay() {
    	try {
    		Logic logic = App.getLogic();
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		logic.setSelectedRelation(null);
    		logic.performAdd(main, 1000.0f, 0.0f);
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Way w1 = logic.getSelectedWay();
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		double lon = GeoMath.xToLonE7(logic.getMap().getWidth(), logic.getViewBox(), 1001.0f)/1E7D;
    		double lat = GeoMath.yToLatE7(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), 500.0f)/1E7D;
    		Node n1 = logic.performAddNode(main, lon, lat);
    		Assert.assertEquals(0, logic.getWaysForNode(n1).size());
    		logic.performJoin(main, w1, n1);
    		Assert.assertTrue(w1.hasNode(n1));
    	} catch (Exception igit) {
    		Assert.fail(igit.getMessage());
    	}  
    }
    
    @UiThreadTest
    @Test
    public void joinToNode() {
    	try {
    		Logic logic = App.getLogic();
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		logic.setSelectedRelation(null);
    		logic.performAdd(main, 1000.0f, 1000.0f);
    		Node n1 = logic.getSelectedNode();
    		logic.setSelectedWay(null);
    		logic.setSelectedNode(null);
    		double lon = GeoMath.xToLonE7(logic.getMap().getWidth(), logic.getViewBox(), 1001.0f)/1E7D;
    		double lat = GeoMath.yToLatE7(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), 1001.0f)/1E7D;
    		Node n2 = logic.performAddNode(main, lon, lat);
    		Assert.assertEquals(2, App.getDelegator().getApiNodeCount());
    		logic.performJoin(main, n1, n2);
    		Assert.assertEquals(1, App.getDelegator().getApiNodeCount());
    	} catch (Exception igit) {
    		Assert.fail(igit.getMessage());
    	}  
    }
    

	private float getX(Logic logic, Node n) {
		return GeoMath.lonE7ToX(logic.getMap().getWidth(), logic.getViewBox(), n.getLon());
	}

	private float getY(Logic logic, Node n) {
		return GeoMath.latE7ToY(logic.getMap().getHeight(), logic.getMap().getWidth(), logic.getViewBox(), n.getLat());
	}
}
