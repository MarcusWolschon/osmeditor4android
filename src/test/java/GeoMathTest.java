import org.junit.Assert;
import org.junit.Test;

import de.blau.android.util.GeoMath;

public class GeoMathTest {
    
    /**
     * Silly test
     */
    @Test
    public void constants() {
        System.out.println("Max lat " + GeoMath.MAX_LAT); 
        System.out.println("Max lat mercator " + GeoMath.MAX_MLAT);
        Assert.assertEquals(180d, GeoMath.MAX_MLAT, 0.000001);
    }
}