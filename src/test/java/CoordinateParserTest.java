import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import de.blau.android.util.CoordinateParser;
import de.blau.android.util.LatLon;

public class CoordinateParserTest {

    /**
     * Test using the same values as the OSM website
     */
    @Test
    public void doItAsOsm() {

        try {
            for (String c : new String[] { "50.06773 14.37742", "50.06773, 14.37742", "+50.06773 +14.37742", "+50.06773, +14.37742" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N50.06773 E14.37742", "N50.06773, E14.37742", "50.06773N 14.37742E", "50.06773N, 14.37742E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N50.06773 W14.37742", "N50.06773, W14.37742", "50.06773N 14.37742W", "50.06773N, 14.37742W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S50.06773 E14.37742", "S50.06773, E14.37742", "50.06773S 14.37742E", "50.06773S, 14.37742E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S50.06773 W14.37742", "S50.06773, W14.37742", "50.06773S 14.37742W", "50.06773S, 14.37742W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N 50° 04.064 E 014° 22.645", "N 50° 04.064' E 014° 22.645", "N 50° 04.064', E 014° 22.645'",
                    "N50° 04.064 E14° 22.645", "N 50 04.064 E 014 22.645", "N50 4.064 E14 22.645", "50° 04.064' N, 014° 22.645' E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N 50° 04.064 W 014° 22.645", "N 50° 04.064' W 014° 22.645", "N 50° 04.064', W 014° 22.645'",
                    "N50° 04.064 W14° 22.645", "N 50 04.064 W 014 22.645", "N50 4.064 W14 22.645", "50° 04.064' N, 014° 22.645' W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S 50° 04.064 E 014° 22.645", "S 50° 04.064' E 014° 22.645", "S 50° 04.064', E 014° 22.645'",
                    "S50° 04.064 E14° 22.645", "S 50 04.064 E 014 22.645", "S50 4.064 E14 22.645", "50° 04.064' S, 014° 22.645' E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S 50° 04.064 W 014° 22.645", "S 50° 04.064' W 014° 22.645", "S 50° 04.064', W 014° 22.645'",
                    "S50° 04.064 W14° 22.645", "S 50 04.064 W 014 22.645", "S50 4.064 W14 22.645", "50° 04.064' S, 014° 22.645' W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N 50° 4' 03.828\" E 14° 22' 38.712\"", "N 50° 4' 03.828\", E 14° 22' 38.712\"",
                    "N 50° 4′ 03.828″, E 14° 22′ 38.712″", "N50 4 03.828 E14 22 38.712", "N50 4 03.828, E14 22 38.712", "50°4'3.828\"N 14°22'38.712\"E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "N 50° 4' 03.828\" W 14° 22' 38.712\"", "N 50° 4' 03.828\", W 14° 22' 38.712\"",
                    "N 50° 4′ 03.828″, W 14° 22′ 38.712″", "N50 4 03.828 W14 22 38.712", "N50 4 03.828, W14 22 38.712", "50°4'3.828\"N 14°22'38.712\"W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S 50° 4' 03.828\" E 14° 22' 38.712\"", "S 50° 4' 03.828\", E 14° 22' 38.712\"",
                    "S 50° 4′ 03.828″, E 14° 22′ 38.712″", "S50 4 03.828 E14 22 38.712", "S50 4 03.828, E14 22 38.712", "50°4'3.828\"S 14°22'38.712\"E" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(14.37742, ll.getLon(), 0.000005);
            }

            for (String c : new String[] { "S 50° 4' 03.828\" W 14° 22' 38.712\"", "S 50° 4' 03.828\", W 14° 22' 38.712\"",
                    "S 50° 4′ 03.828″, W 14° 22′ 38.712″", "S50 4 03.828 W14 22 38.712", "S50 4 03.828, W14 22 38.712", "50°4'3.828\"S 14°22'38.712\"W" }) {
                LatLon ll = CoordinateParser.parseVerbatimCoordinates(c);
                Assert.assertEquals(-50.06773, ll.getLat(), 0.000005);
                Assert.assertEquals(-14.37742, ll.getLon(), 0.000005);
            }
        } catch (ParseException e) {
            Assert.fail(e.getMessage());
        }
    }
}