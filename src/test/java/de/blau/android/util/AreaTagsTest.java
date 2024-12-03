package de.blau.android.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.osm.Tags;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class AreaTagsTest {

    /**
     * 
     */
    @Test
    public void buildingTest() {
        AreaTags at = new AreaTags(ApplicationProvider.getApplicationContext());
        assertTrue(at.isImpliedArea(Tags.KEY_BUILDING, "something"));
    }
    
    @Test
    public void highwayTest() {
        AreaTags at = new AreaTags(ApplicationProvider.getApplicationContext());
        assertFalse(at.isImpliedArea(Tags.KEY_HIGHWAY, "residential"));
        assertTrue(at.isImpliedArea(Tags.KEY_HIGHWAY, "rest_area"));
    }
}