/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ****************************************************************/
package de.blau.android.util.mvt;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.GeometryCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import vector_tile.VectorTile;
import vector_tile.VectorTile.Tile.GeomType;
import vector_tile.VectorTile.Tile.Layer;

public class VectorTileDecoder {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, VectorTileDecoder.class.getSimpleName().length());
    private static final String DEBUG_TAG = VectorTileDecoder.class.getSimpleName().substring(0, TAG_LEN);

    private boolean autoScale = true;

    /**
     * Get the autoScale setting.
     *
     * @return autoScale
     */
    public boolean isAutoScale() {
        return autoScale;
    }

    /**
     * Set the autoScale setting.
     *
     * @param autoScale when true, the decoder automatically scale and return all coordinates in the 0..255 range. when
     *            false, the decoder returns all coordinates in the 0..extent-1 range as they are encoded.
     *
     */
    public void setAutoScale(boolean autoScale) {
        this.autoScale = autoScale;
    }

    /**
     * Decode all layers in data to a FeatureIterable
     * 
     * @param data the PBF data
     * @return a FeatureIterable
     * @throws IOException if the PBF parser fails
     */
    public FeatureIterable decode(@NonNull byte[] data) throws IOException {
        return decode(data, Filter.ALL);
    }

    /**
     * Decode a a single layers in data to a FeatureIterable
     * 
     * @param data the PBF data
     * @param layerName the name of the layer to decode
     * @return a FeatureIterable
     * @throws IOException if the PBF parser fails
     */
    public FeatureIterable decode(@NonNull byte[] data, @NonNull String layerName) throws IOException {
        return decode(data, new Filter.Single(layerName));
    }

    /**
     * Decode multiple layers in data to a FeatureIterable
     * 
     * @param data the PBF data
     * @param layerNames a Set of the layer names to decode
     * @return a FeatureIterable
     * @throws IOException if the PBF parser fails
     */
    public FeatureIterable decode(@NonNull byte[] data, @NonNull Set<String> layerNames) throws IOException {
        return decode(data, new Filter.Any(layerNames));
    }

    /**
     * Decode all layers in data to a FeatureIterable useing a supplied layer filter
     * 
     * @param data the PBF data
     * @param filter the layer Filter
     * @return a FeatureIterable
     * @throws IOException if the PBF parser fails
     */
    public FeatureIterable decode(@NonNull byte[] data, Filter filter) throws IOException {
        VectorTile.Tile tile = VectorTile.Tile.parseFrom(data);
        return new FeatureIterable(tile, filter, autoScale);
    }

    /**
     * 
     * @param n the value to decode
     * @return the int value
     */
    static int zigZagDecode(int n) {
        return ((n >> 1) ^ (-(n & 1)));
    }

    /**
     * Decode geometry
     * 
     * Currently this decodes to GeoJson, this is not very efficient for linear features and rings and should be
     * replaced
     * 
     * @param geomType target GeomType
     * @param commands MVT commands
     * @param scale scaling factor
     * @return a GeoJson geometry
     */
    @NonNull
    static Geometry decodeGeometry(@NonNull GeomType geomType, @NonNull List<Integer> commands, double scale) {
        int x = 0;
        int y = 0;

        List<List<Point>> coordsList = new ArrayList<>();
        List<Point> coords = null;

        int geometryCount = commands.size();
        int length = 0;
        int command = 0;
        int i = 0;
        while (i < geometryCount) {

            if (length <= 0) {
                length = commands.get(i++).intValue();
                command = length & ((1 << 3) - 1);
                length = length >> 3;
            }

            if (length > 0) {
                if (command == Command.MOVE_TO) {
                    coords = new ArrayList<>();
                    coordsList.add(coords);
                }
                length--;

                if (coords == null) {
                    Log.e(DEBUG_TAG, "Command " + command + " without preceeding MOVE_TO");
                    continue;
                }
                if (command == Command.CLOSE_PATH) {
                    if (geomType != VectorTile.Tile.GeomType.POINT && !coords.isEmpty()) {
                        coords.add(coords.get(0));
                    }
                } else {
                    // Command.LINE_TO must have been proceeded by a MOVE_TO
                    int dx = commands.get(i++).intValue();
                    int dy = commands.get(i++).intValue();

                    dx = zigZagDecode(dx);
                    dy = zigZagDecode(dy);

                    x = x + dx;
                    y = y + dy;

                    Point coord = Point.fromLngLat(x / scale, y / scale);
                    coords.add(coord);
                }
            }
        }

        Geometry geometry = null;

        switch (geomType) {
        case LINESTRING:
            List<LineString> lineStrings = new ArrayList<>();
            for (List<Point> cs : coordsList) {
                if (cs.size() <= 1) {
                    continue;
                }
                lineStrings.add(LineString.fromLngLats(cs));
            }
            if (lineStrings.size() == 1) {
                geometry = lineStrings.get(0);
            } else if (lineStrings.size() > 1) {
                geometry = MultiLineString.fromLineStrings(lineStrings);
            }
            break;
        case POINT:
            List<Point> allCoords = new ArrayList<>();
            for (List<Point> cs : coordsList) {
                allCoords.addAll(cs);
            }
            if (allCoords.size() == 1) {
                geometry = allCoords.get(0);
            } else if (allCoords.size() > 1) {
                geometry = MultiPoint.fromLngLats(allCoords);
            }
            break;
        case POLYGON:
            List<List<LineString>> polygonRings = new ArrayList<>();
            List<LineString> ringsForCurrentPolygon = new ArrayList<>();
            for (List<Point> cs : coordsList) {
                // skip exterior with too few coordinates
                final boolean lessThan4 = cs.size() < 4;
                if (ringsForCurrentPolygon.isEmpty() && lessThan4) {
                    break;
                }
                // skip hole with too few coordinates
                if (!ringsForCurrentPolygon.isEmpty() && lessThan4) {
                    continue;
                }
                LineString ring = LineString.fromLngLats(cs); // is this closed or not?

                if (winding(ring.coordinates()) == COUNTERCLOCKWISE) {
                    ringsForCurrentPolygon = new ArrayList<>();
                    polygonRings.add(ringsForCurrentPolygon);
                }
                ringsForCurrentPolygon.add(ring);
            }
            List<Polygon> polygons = new ArrayList<>();
            for (List<LineString> rings : polygonRings) {
                LineString shell = rings.get(0);
                List<LineString> holes = rings.subList(1, rings.size());
                polygons.add(Polygon.fromOuterInner(shell, holes));
            }
            if (polygons.size() == 1) {
                geometry = polygons.get(0);
            } else if (polygons.size() > 1) {
                geometry = MultiPolygon.fromPolygons(polygons);
            }
            break;
        case UNKNOWN:
            break;
        default:
            break;
        }

        if (geometry == null) {
            Log.e(DEBUG_TAG, "Empty geometry for " + geomType);
            geometry = GeometryCollection.fromGeometries(new ArrayList<>());
        }
        return geometry;
    }

    /**
     * Calculate the bounding box of a List of Points and sets an existing rect to the values
     * 
     * @param rect the Rect for the result
     * @param points the List of Points
     */
    private static void rectFromPoints(Rect rect, List<Point> points) {
        Point first = points.get(0);
        int start = 0;
        if (rect.isEmpty()) {
            rect.set((int) first.longitude(), (int) first.latitude(), (int) first.longitude(), (int) first.latitude());
            start = 1;
        }
        for (int i = start; i < points.size(); i++) {
            Point p = points.get(i);
            rect.union((int) p.longitude(), (int) p.latitude());
        }
    }

    /**
     * Get a bounding box for a Geometry
     * 
     * @param rect pre-allocated Rect
     * @param g the Geometry
     * @return the REct set to the bounding box
     */
    @NonNull
    private static Rect getBoundingBox(@NonNull final Rect rect, @NonNull Geometry g) {
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            rect.union((int) ((Point) g).longitude(), (int) ((Point) g).latitude());
            break;
        case GeoJSONConstants.MULTIPOINT:
            @SuppressWarnings("unchecked")
            List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
            rectFromPoints(rect, pointList);
            break;
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            rectFromPoints(rect, line);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            for (List<Point> l : lines) {
                rectFromPoints(rect, l);
            }
            break;
        case GeoJSONConstants.POLYGON:
            @SuppressWarnings("unchecked")
            List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            for (List<Point> ring : rings) {
                rectFromPoints(rect, ring);
            }
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            @SuppressWarnings("unchecked")
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            for (List<List<Point>> polygon : polygons) {
                for (List<Point> ring : polygon) {
                    rectFromPoints(rect, ring);
                }
            }
            break;
        case GeoJSONConstants.GEOMETRYCOLLECTION:
            List<Geometry> geometries = ((GeometryCollection) g).geometries();
            for (Geometry geometry : geometries) {
                getBoundingBox(rect, geometry);
            }
            break;
        default:
            Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.type());
        }
        return rect;
    }

    public static final int COLINEAR         = 0;
    public static final int CLOCKWISE        = -1;
    public static final int COUNTERCLOCKWISE = 1;

    /**
     * Determine winding of a List of Nodes
     * 
     * @param points the List of Points (must contain at least one)
     * @return an int indicating winding direction
     */
    private static int winding(List<Point> points) {
        double area = 0;
        int s = points.size();
        Point n1 = points.get(0);
        double lat1 = n1.latitude();
        double lon1 = n1.longitude();
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Point n2 = points.get((i + 1) % s);
            double lat2 = n2.latitude();
            double lon2 = n2.longitude();
            area = area + (lat2 - lat1) * (lon2 + lon1);
            lat1 = lat2;
            lon1 = lon2;
        }
        return area < 0 ? CLOCKWISE : area > 0 ? COUNTERCLOCKWISE : COLINEAR;
    }

    /**
     * 
     *
     */
    public static final class FeatureIterable implements Iterable<Feature> {

        private final VectorTile.Tile tile;
        private final Filter          filter;
        private boolean               autoScale;

        /**
         * Construct a new FeatureIterable for a tile
         * 
         * @param tile the tile
         * @param filter a filter
         * @param autoScale if true autoscale
         */
        public FeatureIterable(@NonNull VectorTile.Tile tile, @NonNull Filter filter, boolean autoScale) {
            this.tile = tile;
            this.filter = filter;
            this.autoScale = autoScale;
        }

        /**
         * Return a new Iterator
         * 
         * @return an Iterator returning Features
         */
        public Iterator<Feature> iterator() {
            return new FeatureIterator(tile, filter, autoScale);
        }

        /**
         * Get all features as a single list
         * 
         * @return a List with all features
         */
        @NonNull
        public List<Feature> asList() {
            List<Feature> features = new ArrayList<>();
            for (Feature feature : this) {
                features.add(feature);
            }
            return features;
        }

        /**
         * Get all features as a per layer list
         * 
         * @return a Map with the features per layer
         */
        @NonNull
        public Map<String, List<Feature>> asMap() {
            Map<String, List<Feature>> features = new HashMap<>();
            for (Feature feature : this) {
                List<Feature> list = features.get(feature.layerName);
                if (list == null) {
                    list = new ArrayList<>();
                    features.put(feature.layerName, list);
                }
                list.add(feature);
            }
            return features;
        }

        /**
         * Get all layer names from this tile
         * 
         * @return a Collection of layer names
         */
        public Collection<String> getLayerNames() {
            Set<String> layerNames = new HashSet<>();
            for (VectorTile.Tile.Layer layer : tile.getLayersList()) {
                layerNames.add(layer.getName());
            }
            return Collections.unmodifiableSet(layerNames);
        }
    }

    private static final class FeatureIterator implements Iterator<Feature> {

        private final Filter filter;

        private final Iterator<VectorTile.Tile.Layer> layerIterator;

        private Iterator<VectorTile.Tile.Feature> featureIterator;

        private int     extent;
        private String  layerName;
        private double  scale;
        private boolean autoScale;

        private final List<String> keys   = new ArrayList<>();
        private final List<Object> values = new ArrayList<>();

        private Feature next;

        /**
         * Construct a new FeatureIterator for a tile
         * 
         * @param tile the tile
         * @param filter a filter
         * @param autoScale if true autoscale
         */
        public FeatureIterator(@NonNull VectorTile.Tile tile, @NonNull Filter filter, boolean autoScale) {
            layerIterator = tile.getLayersList().iterator();
            this.filter = filter;
            this.autoScale = autoScale;
        }

        /**
         * Check if there is a further Feature
         * 
         * @return true if a further Feature can be retrieved
         */
        public boolean hasNext() {
            findNext();
            return next != null;
        }

        /**
         * Get the next Features
         * 
         * @return the Feature
         */
        public Feature next() {
            findNext();
            if (next == null) {
                throw new NoSuchElementException();
            }
            Feature n = next;
            next = null;
            return n;
        }

        /**
         * Set the next Feature
         */
        private void findNext() {

            if (next != null) {
                return;
            }

            while (true) {
                if (featureIterator == null || !featureIterator.hasNext()) {
                    if (!layerIterator.hasNext()) {
                        next = null;
                        break;
                    }

                    Layer layer = layerIterator.next();
                    if (filter.include(layer.getName())) {
                        parseLayer(layer);
                    }
                    continue;
                }

                next = parseFeature(featureIterator.next());
                break;
            }
        }

        /**
         * Parse a MVT layer
         * 
         * @param layer the layer
         */
        private void parseLayer(@NonNull VectorTile.Tile.Layer layer) {

            layerName = layer.getName();
            extent = layer.getExtent();
            scale = autoScale ? extent / 256.0 : 1.0;

            keys.clear();
            keys.addAll(layer.getKeysList());
            values.clear();

            for (VectorTile.Tile.Value value : layer.getValuesList()) {
                if (value.hasBoolValue()) {
                    values.add(value.getBoolValue());
                } else if (value.hasDoubleValue()) {
                    values.add(value.getDoubleValue());
                } else if (value.hasFloatValue()) {
                    values.add(value.getFloatValue());
                } else if (value.hasIntValue()) {
                    values.add(value.getIntValue());
                } else if (value.hasSintValue()) {
                    values.add(value.getSintValue());
                } else if (value.hasUintValue()) {
                    values.add(value.getUintValue());
                } else if (value.hasStringValue()) {
                    values.add(value.getStringValue());
                } else {
                    values.add(null);
                }
            }

            featureIterator = layer.getFeaturesList().iterator();
        }

        /**
         * Parser a MVT feature
         * 
         * @param feature the feature
         * @return a Feature
         */
        private Feature parseFeature(@NonNull VectorTile.Tile.Feature feature) {
            int tagsCount = feature.getTagsCount();
            Map<String, Object> attributes = new HashMap<>(tagsCount / 2);
            int tagIdx = 0;
            while (tagIdx < tagsCount) {
                String key = keys.get(feature.getTags(tagIdx++));
                Object value = values.get(feature.getTags(tagIdx++));
                attributes.put(key, value);
            }
            GeomType geomType = feature.getType();
            Geometry geometry = decodeGeometry(geomType, feature.getGeometryList(), scale);
            Feature f = new Feature(layerName, extent, geometry, Collections.unmodifiableMap(attributes), feature.getId());
            if (GeomType.POINT != geomType) {
                Rect rect = new Rect();
                getBoundingBox(rect, geometry);
                f.setBox(rect);
            }
            return f;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Class holding MVT features
     * 
     * Uses a GeoJSON Geometry object, note that this does not actually contain valid GeoJSON coordinates
     *
     */
    public static final class Feature {

        private final String              layerName;
        private final int                 extent;
        private final long                id;
        private final Geometry            geometry;
        private final Map<String, Object> attributes;
        private Rect                      box;
        private Object                    cachedLabel;
        private Bitmap                    cachedBitmap;

        /**
         * Construct a new MVT Feature
         * 
         * @param layerName the layer name
         * @param extent tile size (one side)
         * @param geometry a Geometry object
         * @param attributes attributes for the feature
         * @param id optional id
         */
        public Feature(@NonNull String layerName, int extent, @NonNull Geometry geometry, @NonNull Map<String, Object> attributes, long id) {
            this.layerName = layerName;
            this.extent = extent;
            this.geometry = geometry;
            this.attributes = attributes;
            this.id = id;
        }

        /**
         * Get the layer name
         * 
         * @return the layer name
         */
        @NonNull
        public String getLayerName() {
            return layerName;
        }

        /**
         * Get the feature id
         * 
         * @return the id, 0 if not set
         */
        public long getId() {
            return id;
        }

        /**
         * Size of the tile (one side)
         * 
         * @return the tile size
         */
        public int getExtent() {
            return extent;
        }

        /**
         * The geometry for this feature
         * 
         * @return a Geometry object
         */
        @NonNull
        public Geometry getGeometry() {
            return geometry;
        }

        /**
         * Any attributes for this feature
         * 
         * @return a Map containing the attributes
         */
        @NonNull
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        /**
         * @return the box
         */
        @Nullable
        public Rect getBox() {
            return box;
        }

        /**
         * @param box the box to set
         */
        public void setBox(@Nullable Rect box) {
            this.box = box;
        }

        /**
         * @return the cachedLabel
         */
        public Object getCachedLabel() {
            return cachedLabel;
        }

        /**
         * @param cachedLabel the cachedLabel to set
         */
        public void setCachedLabel(Object cachedLabel) {
            this.cachedLabel = cachedLabel;
        }

        /**
         * @return the cachedBitmap
         */
        public Bitmap getCachedBitmap() {
            return cachedBitmap;
        }

        /**
         * @param cachedBitmap the cachedBitmap to set
         */
        public void setCachedBitmap(Bitmap cachedBitmap) {
            this.cachedBitmap = cachedBitmap;
        }
    }
}
