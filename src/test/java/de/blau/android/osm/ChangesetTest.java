package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class ChangesetTest {
    private static final String TEST_SOURCE = "Test Source";
    private static final String TEST_COMMENT = "Test Comment";

    /**
     */
    @Test
    public void imageryTags1() {     
        Changeset changeset = new Changeset("vespucci Test", TEST_COMMENT, TEST_SOURCE, Arrays.asList("source1","source2"),
                null, App.getPreferences(ApplicationProvider.getApplicationContext()).getServer().getCachedCapabilities()) ;
        
        assertEquals(TEST_COMMENT,changeset.getTags().get(Tags.KEY_COMMENT));
        assertEquals(TEST_SOURCE,changeset.getTags().get(Tags.KEY_SOURCE));
        assertEquals("source1;source2",changeset.getTags().get(Tags.KEY_IMAGERY_USED));
    }
    
    @Test
    public void imageryTags2() {
        Changeset changeset = new Changeset("vespucci Test", TEST_COMMENT, TEST_SOURCE, Arrays.asList(
                "A very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name","source2"),
                null, App.getPreferences(ApplicationProvider.getApplicationContext()).getServer().getCachedCapabilities()) ;
        
        assertEquals(TEST_COMMENT,changeset.getTags().get(Tags.KEY_COMMENT));
        assertEquals(TEST_SOURCE,changeset.getTags().get(Tags.KEY_SOURCE));
        assertEquals("A very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a very long source name, a ver",changeset.getTags().get(Tags.KEY_IMAGERY_USED));
        assertEquals("source2",changeset.getTags().get(Tags.KEY_IMAGERY_USED + ":1"));
    }
    
    @Test
    public void imageryTags3() {
        Changeset changeset = new Changeset("vespucci Test", TEST_COMMENT, TEST_SOURCE, Arrays.asList(
                "A very long source name 1", "A very long source name 2", "A very long source name 3", "A very long source name 4", 
                "A very long source name 5", "A very long source name 6", "A very long source name 7", "A very long source name 8", 
                "A very long source name 6", "A very long source name 7", "A very long source name 8","source2"),
                null, App.getPreferences(ApplicationProvider.getApplicationContext()).getServer().getCachedCapabilities()) ;
        
        assertEquals(TEST_COMMENT,changeset.getTags().get(Tags.KEY_COMMENT));
        assertEquals(TEST_SOURCE,changeset.getTags().get(Tags.KEY_SOURCE));
        assertTrue(changeset.getTags().get(Tags.KEY_IMAGERY_USED).endsWith(";A very long source name 6"));
        assertEquals("A very long source name 7;A very long source name 8;source2",changeset.getTags().get(Tags.KEY_IMAGERY_USED + ":1"));
    }
}
