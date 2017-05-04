package de.blau.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.MapTile;

/**
 * Note these tests are not mocked
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TileLayerServerTest {
	
	Main main = null;
	View v = null;
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);
    
    @Before
    public void setup() {
    	main = mActivityRule.getActivity();
    }
    
    @Test
	public void buildurl() {
    	Map map = main.getMap();
    	MapTile mapTile = new MapTile("",20,1111,2222);
		Preferences prefs = new Preferences(main);
		
		prefs.setBackGroundLayer("BING"); 
		main.getMap().setPrefs(main, prefs);
		
    	TileLayerServer t = map.getOpenStreetMapTilesOverlay().getRendererInfo();
    	System.out.println(t.toString());
    	
    	String s = t.getTileURLString(mapTile); // note this could fail if the metainfo cannot be retrieved
  
    	System.out.println("Parameters replaced " + s);
    	System.out.println("Quadkey " + t.quadTree(mapTile));
    	Assert.assertTrue(s.contains(t.quadTree(mapTile)));
    	
		prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK); 
		main.getMap().setPrefs(main, prefs);
		t = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		
		System.out.println(t.toString());
		
    	s = t.getTileURLString(mapTile);
 
    	System.out.println("Parameters replaced " + s);
    	
    	Assert.assertTrue(s.contains("1111"));
    	Assert.assertTrue(s.contains("2222"));
    	Assert.assertTrue(s.contains("20"));

	}
    
    @Test
 	public void sort() {
    	ClassLoader loader = Thread.currentThread().getContextClassLoader();
    	InputStream is = loader.getResourceAsStream("imagery_test.json");
    	try {
			TileLayerServer.parseImageryFile(main, is, false);
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
    	List<String> names = Arrays.asList(TileLayerServer.getNames(null, false));
    	
    	int iA = names.indexOf("A imagery");
    	Assert.assertNotEquals(-1, iA);
    	int iAnoDate = names.indexOf("A no date imagery");
    	Assert.assertNotEquals(-1, iAnoDate);
    	int iB = names.indexOf("B imagery");
    	Assert.assertNotEquals(-1, iB);
    	int iBnoDate = names.indexOf("B no date imagery");
    	Assert.assertNotEquals(-1, iBnoDate);
    	int iC = names.indexOf("C imagery");
    	Assert.assertNotEquals(-1, iC);
    	
    	Assert.assertTrue(iAnoDate < iBnoDate); // alphabetic
    	
    	TileLayerServer a = TileLayerServer.get(main, "A", false);
    	TileLayerServer b = TileLayerServer.get(main, "B", false);
    	Assert.assertTrue(a.getEndDate() < b.getEndDate());
    	Assert.assertTrue(iA > iB); // date
    	
    	
    	
    	
    	Assert.assertTrue(iA > iC && iB > iC); // preference
    }
}