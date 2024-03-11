package de.blau.android.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * @author Simon Poole
 *
 */
public final class GeoJson {
    private static final String DEBUG_TAG = GeoJson.class.getSimpleName().substring(0, Math.min(23, GeoJson.class.getSimpleName().length()));

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
            final OsmElement first = result.get(0);
            if (Tags.isMultiPolygon(first)) {
                tags.putAll(first.getTags());
                first.setTags(tags);
            } else {
                for (OsmElement e : result) {
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
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            Relation mp = null;
            for (List<List<Point>> polygon : polygons) {
                List<OsmElement> p = polygonToOsm(factory, polygon, maxNodes);
                // post process
                if (p.isEmpty()) {
                    Log.e(DEBUG_TAG, "polygonToOsm returned empty");
                } else if (polygons.size() == 1) {
                    result.addAll(p); // 1 simple polygon
                } else {
                    if (mp == null) {
                        mp = createNewMp(factory);
                        result.add(mp);
                    }
                    if (p.size() == 1) {
                        OsmElement e = p.get(0);
                        mp.addMember(new RelationMember(Tags.ROLE_OUTER, e));
                        e.addParentRelation(mp);
                        result.add(e);
                    } else {
                        for (OsmElement e : p) {
                            if (Tags.isMultiPolygon(e)) {
                                for (RelationMember rm : ((Relation) e).getMembers()) {
                                    mp.addMember(rm);
                                    final OsmElement element = rm.getElement();
                                    if (element != null) {
                                        element.clearParentRelations();
                                        element.addParentRelation(mp);
                                        result.add(element);
                                    } else {
                                        Log.e(DEBUG_TAG, "polygonToOsm returned relation member without element");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            List<Geometry> geometries = ((GeometryCollection) g).geometries();
            for (Geometry geometry : geometries) {
                result.addAll(geometryToOsm(factory, geometry, maxNodes));
            }
            break;
        case GeoJSONConstants.MULTILINESTRING:
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                List<Way> ways = pointsToWays(factory, l, maxNodes);
                result.addAll(ways);
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
     * Create a new MP
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
     * If this generated a MP, it will be the 1st element of the result
     * 
     * @param factory an instance of OsmFactory
     * @param rings a list of rings
     * @param maxNodes maximum number of Nodes in an OSM Way
     * @return a List of OsmElements
     */
    @NonNull
    private static List<OsmElement> polygonToOsm(@NonNull OsmElementFactory factory, @NonNull List<List<Point>> rings, int maxNodes) {
        List<List<Way>> temp = new ArrayList<>();
        for (List<Point> l : rings) {
            List<Way> ways = pointsToWays(factory, l, maxNodes);
            temp.add(ways);
        }
        if (temp.size() == 1 && temp.get(0).size() == 1) {
            // simple OSM polygon
            return new ArrayList<>(temp.get(0));
        } else {
            // need to create a MP
            Relation mp = createNewMp(factory);
            List<OsmElement> result = new ArrayList<>();
            result.add(mp);
            // 1st ring is outer
            List<Way> outer = temp.remove(0);
            for (Way w : outer) {
                mp.addMember(new RelationMember(Tags.ROLE_OUTER, w));
                w.addParentRelation(mp);
                result.add(w);
            }
            // the rest are inners
            for (List<Way> inner : temp) {
                for (Way w : inner) {
                    // determine inner / outer from winding
                    mp.addMember(new RelationMember(Tags.ROLE_INNER, w));
                    w.addParentRelation(mp);
                    result.add(w);
                }
            }
            return result;
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
    private static List<Way> pointsToWays(@NonNull OsmElementFactory factory, @NonNull List<Point> coordinates, int maxNodes) {
        List<Way> result = new ArrayList<>();
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
            way.addNode(result.get(0).getFirstNode());
        }
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
