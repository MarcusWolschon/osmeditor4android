package de.blau.android.resources;

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
    	MapTile tile = new MapTile("",20,1111,2222);
		Preferences prefs = new Preferences(main);
		
		prefs.setBackGroundLayer("BING"); 
		main.getMap().setPrefs(main, prefs);
		
    	TileLayerServer t = map.getOpenStreetMapTilesOverlay().getRendererInfo();
    	System.out.println(t.toString());
    	
    	String s = t.getTileURLString(tile); // note this could fail if the metainfo cannot be retrieved
  
    	System.out.println("Parameters replaced " + s);
    	System.out.println("Quadkey " + t.quadTree(tile));
    	Assert.assertTrue(s.contains(t.quadTree(tile)));
    	
		prefs.setBackGroundLayer(TileLayerServer.LAYER_MAPNIK); 
		main.getMap().setPrefs(main, prefs);
		t = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		
		System.out.println(t.toString());
		
    	s = t.getTileURLString(tile);
 
    	System.out.println("Parameters replaced " + s);
    	
    	Assert.assertTrue(s.contains("1111"));
    	Assert.assertTrue(s.contains("2222"));
    	Assert.assertTrue(s.contains("20"));
	}
}