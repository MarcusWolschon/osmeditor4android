
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.blau.android.util.Util;

public class UtilTest {

    /**
     * Check that we correctly determine if a double isn't zero
     */
    @Test
    public void notZero() {
        double d = 0D;
        assertFalse(Util.notZero(d));
        d = 0.0000001;
        assertTrue(Util.notZero(d));
    }
    
    /**
     * Test conversion of a list of String values to OSM ";" list representation
     */
    @Test
    public void toOsmList() {
        List<String> test = Arrays.asList("Alpha","Beta","Gamma");
        String result = Util.listToOsmList(test);
        assertEquals("Alpha;Beta;Gamma", result);
    }
}