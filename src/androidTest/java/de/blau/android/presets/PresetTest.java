package de.blau.android.presets;


import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.TagFormFragment;

/**
 * This is just a convenient way of generating the default preset dump
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PresetTest {
	
    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);
    
    @Before
    public void setup() {
    }
    
    @Test
	public void matchAndSplit() {
    	Main main = (Main)mActivityRule.getActivity(); 
    	// 
    	Preset[] presets = App.getCurrentPresets(main);
    	HashMap<String,String> tags = new HashMap<String,String>();
    	tags.put("amenity", "restaurant");
    	PresetItem restaurant = Preset.findBestMatch(presets, tags);
    	Assert.assertEquals("Restaurant", restaurant.getName());
    	// Splitting
    	ArrayList<String>values = new ArrayList<String>();
    	values.add("chinese;fondue");
    	values.add("japenese,steak");
    	ArrayList<String>result = Preset.splitValues(values, restaurant, "cuisine");
    	Assert.assertEquals(3,result.size());
    	Assert.assertTrue(result.contains("chinese"));
    	Assert.assertTrue(result.contains("fondue"));
    	Assert.assertTrue(result.contains("japenese,steak"));
    	Assert.assertNull(Preset.splitValues(null, restaurant, "cuisine"));
    	values.add(null);
    	Assert.assertEquals(3,Preset.splitValues(values, restaurant, "cuisine").size());
	}
}
