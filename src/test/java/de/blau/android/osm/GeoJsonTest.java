package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;

import androidx.annotation.NonNull;
import de.blau.android.util.FileUtil;

public class GeoJsonTest {

    /**
     * Convert a GeoJson point to a Node
     */
    @Test
    public void pointTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/point.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(1, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(e instanceof Node);
            assertEquals((int) (30 * 1E7D), ((Node) e).getLon());
            assertEquals((int) (10 * 1E7D), ((Node) e).getLat());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson multipoint to multiple Nodes
     */
    @Test
    public void multiPointTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/multiPoint.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(4, elements.size());
            OsmElement e = elements.get(2);
            assertTrue(e instanceof Node);
            assertEquals((int) (20 * 1E7D), ((Node) e).getLon());
            assertEquals((int) (20 * 1E7D), ((Node) e).getLat());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson point feature to a Node with tags
     */
    @Test
    public void pointFeatureTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/pointFeature.geojson")) {
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromJson(inputStreamToString(input)), 2000);
            assertNotNull(elements);
            assertEquals(1, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(e instanceof Node);
            assertEquals((int) (102.0 * 1E7D), ((Node) e).getLon());
            assertEquals((int) (0.5 * 1E7D), ((Node) e).getLat());
            assertTrue(e.hasTag("prop0", "value0"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson linestring to a Way
     */
    @Test
    public void lineStringTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/lineString.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(1, elements.size());
            OsmElement w = elements.get(0);
            assertTrue(w instanceof Way);
            assertEquals(3, ((Way) w).nodeCount());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson linestring to a Way, pretending it is longer than the max number of Nodes in a Way
     */
    @Test
    public void longLineStringTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/lineString.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2);
            assertNotNull(elements);
            assertEquals(2, elements.size());
            OsmElement w1 = elements.get(0);
            assertTrue(w1 instanceof Way);
            assertEquals(2, ((Way) w1).nodeCount());
            OsmElement w2 = elements.get(1);
            assertTrue(w2 instanceof Way);
            assertEquals(2, ((Way) w2).nodeCount());
            assertEquals(((Way) w1).getLastNode(), ((Way) w2).getFirstNode());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson multilinestring to two Ways
     */
    @Test
    public void multiLineStringTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/multiLineString.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(2, elements.size());
            OsmElement w = elements.get(0);
            assertTrue(w instanceof Way);
            assertEquals(3, ((Way) w).nodeCount());
            OsmElement w2 = elements.get(1);
            assertTrue(w2 instanceof Way);
            assertEquals(4, ((Way) w2).nodeCount());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson polygon to a single closed Way
     */
    @Test
    public void polygonTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/polygon.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(1, elements.size());
            OsmElement w = elements.get(0);
            assertTrue(w instanceof Way);
            assertEquals(5, ((Way) w).nodeCount());
            assertTrue(((Way) w).isClosed());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson polygon feature to a MP with tags
     */
    @Test
    public void polygonFeatureTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/polygonFeature.geojson")) {
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromJson(inputStreamToString(input)), 3);
            assertNotNull(elements);
            assertEquals(3, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(Tags.isMultiPolygon(e));
            OsmElement e2 = elements.get(1);
            OsmElement e3 = elements.get(2);
            assertNotNull(((Relation) e).getMember(e2));
            assertNotNull(((Relation) e).getMember(e3));
            assertTrue(e.hasTag("prop0", "value0"));
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson polygon with a hole to a MP with two closed Ways one outer, one inner
     */
    @Test
    public void holeyPolygonTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/holeyPolygon.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(3, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(Tags.isMultiPolygon(e));
            OsmElement e2 = elements.get(1);
            OsmElement e3 = elements.get(2);
            RelationMember o1 = ((Relation) e).getMember(e2);
            assertNotNull(o1);
            assertEquals(Way.NAME, o1.getType());
            assertEquals(Tags.ROLE_OUTER, o1.getRole());
            assertTrue(((Way) o1.getElement()).isClosed());
            RelationMember o2 = ((Relation) e).getMember(e3);
            assertNotNull(o2);
            assertEquals(Way.NAME, o2.getType());
            assertEquals(Tags.ROLE_INNER, o2.getRole());
            assertTrue(((Way) o2.getElement()).isClosed());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson multipolygon to a MP with two closed Ways as outers
     */
    @Test
    public void multiPolygonTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/multiPolygon.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(3, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(Tags.isMultiPolygon(e));
            OsmElement e2 = elements.get(1);
            OsmElement e3 = elements.get(2);
            RelationMember o1 = ((Relation) e).getMember(e2);
            assertNotNull(o1);
            assertEquals(Way.NAME, o1.getType());
            assertEquals(Tags.ROLE_OUTER, o1.getRole());
            assertTrue(((Way) o1.getElement()).isClosed());
            RelationMember o2 = ((Relation) e).getMember(e3);
            assertNotNull(o2);
            assertEquals(Way.NAME, o2.getType());
            assertEquals(Tags.ROLE_OUTER, o2.getRole());
            assertTrue(((Way) o2.getElement()).isClosed());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson multipolygon to a single closed polygon
     */
    @Test
    public void singleRingMultiPolygonTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/singleRingMultiPolygon.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(1, elements.size());
            OsmElement e = elements.get(0);
            assertEquals(Way.NAME, e.getName());
            assertTrue(((Way) e).isClosed());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson multipolygon with holes to a MP with three closed Ways two outer, one inner
     */
    @Test
    public void holeyMultiPolygonTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/holeyMultiPolygon.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(4, elements.size());
            OsmElement e = elements.get(0);
            assertTrue(Tags.isMultiPolygon(e));
            OsmElement e2 = elements.get(1);
            OsmElement e3 = elements.get(2);
            OsmElement e4 = elements.get(3);
            RelationMember o1 = ((Relation) e).getMember(e2);
            assertNotNull(o1);
            assertEquals(Way.NAME, o1.getType());
            assertEquals(Tags.ROLE_OUTER, o1.getRole());
            assertTrue(((Way) o1.getElement()).isClosed());
            RelationMember o2 = ((Relation) e).getMember(e3);
            assertNotNull(o2);
            assertEquals(Way.NAME, o2.getType());
            assertEquals(Tags.ROLE_OUTER, o2.getRole());
            assertTrue(((Way) o2.getElement()).isClosed());
            RelationMember o3 = ((Relation) e).getMember(e4);
            assertNotNull(o3);
            assertEquals(Way.NAME, o3.getType());
            assertEquals(Tags.ROLE_INNER, o3.getRole());
            assertTrue(((Way) o3.getElement()).isClosed());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Convert a GeoJson geometrycollection to a Node and a Way
     */
    @Test
    public void geometryCollectionTest() {
        try (InputStream input = getClass().getResourceAsStream("/geojson/geometryCollection.geojson")) {
            Geometry g = de.blau.android.util.GeoJson.geometryFromJson(inputStreamToString(input));
            assertNotNull(g.type());
            List<OsmElement> elements = GeoJson.toOsm(Feature.fromGeometry(g), 2000);
            assertNotNull(elements);
            assertEquals(2, elements.size());
            OsmElement w = elements.get(0);
            assertTrue(w instanceof Way);
            assertEquals(5, ((Way) w).nodeCount());
            OsmElement n = elements.get(1);
            assertTrue(n instanceof Node);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Read an InputStream and return contents as a String
     * 
     * @param input the InputStream
     * @return a String
     * @throws IOException if something goes wrong while readins
     */
    @NonNull
    String inputStreamToString(@NonNull InputStream input) throws IOException {
        return FileUtil.readToString(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
    }
}