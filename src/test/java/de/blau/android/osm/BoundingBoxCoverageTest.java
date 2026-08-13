package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import de.blau.android.UnitTestUtils;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class BoundingBoxCoverageTest {
    
    private final static List<BoundingBox> testCoverage = new ArrayList<>();
    static {
        testCoverage.add(new BoundingBox(83876908,473877297,83881235,473881333));
        testCoverage.add(new BoundingBox(83881235,473876834,83885561,473881333));
        testCoverage.add(new BoundingBox(83878162,473881333,83882661,473885832));
        testCoverage.add(new BoundingBox(83882661,473881333,83886119,473885832));
        testCoverage.add(new BoundingBox(83886119,473882168,83889577,473885832));
        testCoverage.add(new BoundingBox(83880957,473885832,83885456,473890331));
        testCoverage.add(new BoundingBox(83885456,473885832,83888992,473890331));
        testCoverage.add(new BoundingBox(83888992,473885832,83892528,473890331));
        testCoverage.add(new BoundingBox(83885134,473890331,83888907,473894830));
        testCoverage.add(new BoundingBox(83888907,473890331,83892679,473894830));
        testCoverage.add(new BoundingBox(83884759,473894830,83888611,473899329));
        testCoverage.add(new BoundingBox(83888611,473894830,83892463,473899329));
        testCoverage.add(new BoundingBox(83884758,473899329,83889257,473903828));
        testCoverage.add(new BoundingBox(83885669,473903828,83889257,473906432));
        testCoverage.add(new BoundingBox(83887045,473906432,83889257,473909036));
        testCoverage.add(new BoundingBox(83889257,473899329,83893267,473903828));
        testCoverage.add(new BoundingBox(83889257,473903828,83893756,473908327));
        testCoverage.add(new BoundingBox(83889257,473908327,83893756,473910831));
        testCoverage.add(new BoundingBox(83891095,473910831,83893756,473913334));
        testCoverage.add(new BoundingBox(83893756,473904654,83898163,473909153));
        testCoverage.add(new BoundingBox(83893756,473909153,83897166,473911709));
        testCoverage.add(new BoundingBox(83893756,473911709,83897166,473914264));
        testCoverage.add(new BoundingBox(83897166,473909153,83900575,473913077));
    }
   

    /**
     * Test getting bounding box coverage for a way
     */
    @Test
    public void wayBoundingBoxCoverageTest() {
        StorageDelegator d = UnitTestUtils.loadTestData(getClass(), "test2.osm");
        Way w = (Way) d.getOsmElement(Way.NAME, 119104094L);
        List<BoundingBox> coverage = w.getCoverage(20, 20, 50);
        assertFalse(coverage.isEmpty());
        assertEquals(testCoverage, coverage);
    }  
 
}
