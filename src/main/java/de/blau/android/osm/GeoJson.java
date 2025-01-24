package de.blau.android.osm;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.GeometryCollection;
import com.mapbox.geojson.Point;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.util.GeoJSONConstants;

/**
 * Methods to convert GeoJson objects to OSM elements
 * 
 * The returned lists will be in element order nwr except for GeoJson MPs or Polygons with multiple rings, which will
 * have nwnww...r
 * 
 * Caveats:
 * 
 * - polygons that can be modelled as an OSM simple closed ways will be returned as that (maybe this should be
 * switchable)
 * 
 * - in geometrycollections points that are in the same position as points in the same position as vertexes of
 * linear/area elements are not merged
 * 
 * @author Simon Poole
 *
 */
public final class GeoJson {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, GeoJson.class.getSimpleName().length());
    private static final String DEBUG_TAG = GeoJson.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor to stop instantiation
     */
    private GeoJson() {
        // private
    }

    /**
     * Create a Map of tags from the properties
     * 
     * @param properties the properties of a Feature
     * @return a map holding the tags
     */
    @NonNull
    public static Map<String, String> extractTags(@NonNull JsonObject properties) {
        Map<String, String> tags = new LinkedHashMap<>();
        for (String key : properties.keySet()) {
            JsonElement e = properties.get(key);
            if (!e.isJsonNull() && e.isJsonPrimitive()) {
                tags.put(key, e.getAsString());
            }
        }
        return tags;
    }

    /**
     * Create OSM element(s) from a GeoJson feature
     * 
     * If this generated a MP, it will be the 1st element of the result
     * 
     * @param f the Feature to convert
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of OsmElement
     */
    @Nullable
    public static List<OsmElement> toOsm(@NonNull Feature f, int maxNodes) {
        OsmElementFactory factory = App.getDelegator().getFactory();
        List<OsmElement> result = geometryToOsm(factory, f.geometry(), maxNodes);
        JsonObject properties = f.properties();
        Map<String, String> tags = properties != null ? extractTags(properties) : null;
        if (!result.isEmpty() && tags != null) {
            final OsmElement last = result.get(result.size() - 1);
            if (Tags.isMultiPolygon(last)) {
                tags.putAll(last.getTags());
                tags.put(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON); // setTags overwrites existing tags
                last.setTags(tags);
            } else {
                for (OsmElement e : result) {
                    String geomType = f.geometry().type();
                    if (e instanceof Node && (GeoJSONConstants.LINESTRING.equals(geomType) || GeoJSONConstants.MULTILINESTRING.equals(geomType)
                            || GeoJSONConstants.POLYGON.equals(geomType))) {
                        continue;
                    }
                    e.setTags(tags);
                }
            }
        }
        return result;
    }

    /**
     * Convert a GeoJson geometry to OSM element(s)
     * 
     * If this generated a MP, it will be the 1st element of the result
     * 
     * @param factory an instance of OsmElementFactory
     * @param g GeoJson geometry
     * @param maxNodes maximum number of Nodes in an OSM Way
     * 
     * @return a List of OsmElement
     */
    @SuppressWarnings("unchecked")
    private static List<OsmElement> geometryToOsm(@NonNull OsmElementFactory factory, @NonNull Geometry g, int maxNodes) {
        List<OsmElement> result = new ArrayList<>();
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            result.add(pointToNode(factory, (Point) g));
            break;
        case GeoJSONConstants.MULTIPOINT:
            for (Point q : ((CoordinateContainer<List<Point>>) g).coordinates()) {
                result.add(pointToNode(factory, q));
            }
            break;
        case GeoJSONConstants.LINESTRING:
            result.addAll(pointsToWays(factory, ((CoordinateContainer<List<Point>>) g).coordinates(), maxNodes));
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            result.addAll(multiPolygonsToOsm(factory, ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates(), maxNodes));
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            List<Geometry> geometries = ((GeometryCollection) g).geometries();
            for (Geometry geometry : geometries) {
                result.addAll(geometryToOsm(factory, geometry, maxNodes));
            }
            break;
        case GeoJSONConstants.MULTILINESTRING:
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                List<OsmElement> elements = pointsToWays(factory, l, maxNodes);
                result.addAll(elements);
            }
            break;
        case GeoJSONConstants.POLYGON:
            result.addAll(polygonToOsm(factory, ((CoordinateContainer<List<List<Point>>>) g).coordinates(), maxNodes));
            break;
        default:
            Log.e(DEBUG_TAG, "toOsm unknown GeoJSON geometry " + g.type());
        }
        return result;
    }

    /**
     * Convert a GeoJson multipolygons geometry to OsmElements
     * 
     * @param factory an instance of OsmFactory
     * @param rings a list of list of rings
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of OsmElements
     */
    @NonNull
    private static List<OsmElement> multiPolygonsToOsm(OsmElementFactory factory, List<List<List<Point>>> polygons, int maxNodes) {
        List<OsmElement> result = new ArrayList<>();
        Relation mp = createNewMp(factory);
        for (List<List<Point>> polygon : polygons) {
            List<OsmElement> p = polygonToOsm(factory, polygon, maxNodes);
            // post process
            if (p.isEmpty()) {
                Log.e(DEBUG_TAG, "polygonToOsm returned empty");
                continue;
            }
            OsmElement lastElement = p.get(p.size() - 1);
            if (polygon.size() == 1 && lastElement instanceof Way) {
                for (OsmElement e : p) {
                    addRingMember(mp, Tags.ROLE_OUTER, e);
                    result.add(e);
                }
            } else if (Tags.isMultiPolygon(lastElement)) {
                for (OsmElement e : p) {
                    if (Tags.isMultiPolygon(e)) {
                        continue;
                    }
                    result.add(e);
                    // fixup way relation membership
                    if (e instanceof Way) {
                        RelationMember rm = ((Relation) lastElement).getMember(e);
                        mp.addMember(rm);
                        e.clearParentRelations();
                        e.addParentRelation(mp);
                    }
                }
            } else {
                Log.e(DEBUG_TAG, "Unexpected last element " + lastElement.getName());
            }
        }
        result.add(mp);
        return result;
    }

    /**
     * Create a new MP Relation
     * 
     * @param factory the current OsmElementFactory instance
     * @return a Multi-Polygon
     */
    @NonNull
    private static Relation createNewMp(@NonNull OsmElementFactory factory) {
        Relation mp = factory.createRelationWithNewId();
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON);
        mp.setTags(tags);
        return mp;
    }

    /**
     * Convert a GeoJson polygons geometry to OsmElements
     * 
     * If this generated a MP, it will be the last element of the result, for a single ring polygon it will try to
     * generate an OSM closed way
     * 
     * @param factory an instance of OsmFactory
     * @param rings a list of rings
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of OsmElements
     */
    @NonNull
    private static List<OsmElement> polygonToOsm(@NonNull OsmElementFactory factory, @NonNull List<List<Point>> rings, int maxNodes) {
        List<List<OsmElement>> temp = new ArrayList<>();
        for (List<Point> ring : rings) {
            temp.add(pointsToWays(factory, ring, maxNodes));
        }
        if (temp.size() == 1) { // just one ring
            List<OsmElement> ring = temp.get(0);
            int ringSize = ring.size();
            if (ringSize > 0) {
                OsmElement lastElement = ring.get(ringSize - 1);
                if (lastElement instanceof Way && ((Way) lastElement).isClosed() && ((Way) lastElement).nodeCount() == ringSize) {
                    // simple OSM polygon
                    return temp.get(0);
                }
            }
        }
        // need to create a MP
        Relation mp = createNewMp(factory);
        List<OsmElement> result = new ArrayList<>();
        // 1st ring is outer
        List<OsmElement> outer = temp.remove(0);
        for (OsmElement e : outer) {
            addRingMember(mp, Tags.ROLE_OUTER, e);
            result.add(e);
        }
        // the rest are inners
        for (List<OsmElement> inner : temp) {
            for (OsmElement e : inner) {
                addRingMember(mp, Tags.ROLE_INNER, e);
                result.add(e);
            }
        }
        result.add(mp);
        return result;
    }

    /**
     * Add a way to the multi-polygon with the specified role
     * 
     * @param mp the multi-polygon
     * @param role the rolw
     * @param e the element to potentionall< add
     */
    private static void addRingMember(@NonNull Relation mp, @NonNull String role, @Nullable OsmElement e) {
        if (e instanceof Way) {
            mp.addMember(new RelationMember(role, e));
            e.addParentRelation(mp);
        }
    }

    /**
     * Convert a List of GeoJson points to one or more OSM ways
     * 
     * Handles closing ways and splitting them in to multiple ways if too "long"
     * 
     * @param factory an instance of OsmFactory
     * @param coordinates the GeoJson points
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of Ways
     */
    @NonNull
    private static List<OsmElement> pointsToWays(@NonNull OsmElementFactory factory, @NonNull List<Point> coordinates, int maxNodes) {
        List<OsmElement> result = new ArrayList<>();
        Way way = factory.createWayWithNewId();
        int nodeCount = 0;
        final int lastIndex = coordinates.size() - 1;
        boolean closed = coordinates.get(0).equals(coordinates.get(lastIndex));
        List<Point> points = closed ? coordinates.subList(0, lastIndex) : coordinates;
        Point lastPoint = points.get(points.size() - 1);
        for (Point p : points) {
            Node n = pointToNode(factory, p);
            way.addNode(n);
            nodeCount++;
            if (nodeCount >= maxNodes && !p.equals(lastPoint)) {
                result.add(way);
                way = factory.createWayWithNewId();
                way.addNode(n);
                nodeCount = 1;
            }
        }
        result.add(way);
        if (closed) {
            way.addNode(((Way) result.get(0)).getFirstNode());
        }
        // now add the nodes
        Set<Node> nodes = new HashSet<>();
        for (OsmElement e : result) {
            nodes.addAll(((Way) e).getNodes());
        }
        result.addAll(0, nodes);
        return result;
    }

    /**
     * Convert a GeoJson point to an OSM node
     * 
     * @param factory an instance of OsmFactory
     * @param p the point
     * @return an OSM node
     */
    @NonNull
    private static Node pointToNode(@NonNull OsmElementFactory factory, @NonNull Point p) {
        return factory.createNodeWithNewId((int) ((p.latitude()) * 1E7D), (int) ((p.longitude()) * 1E7D));
    }
}
