package de.blau.android.layer.mvt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfException;
import com.mapbox.turf.TurfJoins;

import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Map;
import de.blau.android.dialogs.FeatureInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoMath;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class MapOverlay extends MapTilesOverlayLayer<VectorTileDecoder.FeatureIterable> implements ClickableInterface<VectorTileDecoder.Feature> {

    private static final String DEBUG_TAG = "mvt";

    /** Map this is an overlay of. */
    private Map map = null;

    /**
     * Construct a new MVT layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map, @NonNull TileRenderer<VectorTileDecoder.FeatureIterable> aTileRenderer) {
        super(map, /* TileLayerSource.get(aView.getContext()), null, true), null, */ aTileRenderer);
        this.map = map;
    }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        Log.d(DEBUG_TAG, "getClicked");
        List<VectorTileDecoder.Feature> result = new ArrayList<>();
        int z = map.getZoomLevel();

        MapTile mapTile = getTile(z, x, y);
        VectorTileDecoder.FeatureIterable tile = mTileProvider.getMapTileFromCache(mapTile);
        while (tile == null && z > myRendererInfo.getMinZoom()) { // try zooming out
            z--;
            mapTile = getTile(z, x, y);
            tile = mTileProvider.getMapTileFromCache(mapTile);
        }
        if (tile != null) {
            Rect rect = getScreenRectForTile(new Rect(), map.getWidth(), map.getHeight(), map, mapTile.zoomLevel, mapTile.y, mapTile.x, true, 0, 0);
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue() * 256 / rect.width();
            final float scaledX = (x - rect.left) * 256 / rect.width();
            final float scaledY = (y - rect.top) * 256 / rect.height();
            for (VectorTileDecoder.Feature f : tile) {
                Geometry g = f.getGeometry();
                if (g == null) {
                    continue;
                }
                if (geometryClicked(scaledX, scaledY, tolerance, g)) {
                    result.add(f);
                }
            }
        } else {
            Log.e(DEBUG_TAG, "Tile not found");
            mTileProvider.getCacheUsageInfo();
        }
        return result;
    }

    /**
     * Check if a geometry has been clicked
     * 
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @param tolerance the tolerance value to use
     * @param g the Geometry
     * @return true if clicked
     */
    @SuppressWarnings("unchecked")
    private boolean geometryClicked(final float x, final float y, final float tolerance, @NonNull Geometry g) {
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            return inToleranceArea(tolerance, (Point) g, x, y);
        case GeoJSONConstants.MULTIPOINT:
            for (Point q : ((CoordinateContainer<List<Point>>) g).coordinates()) {
                if (inToleranceArea(tolerance, q, x, y)) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.LINESTRING:
            return distanceToLineString(x, y, ((CoordinateContainer<List<Point>>) g).coordinates(), tolerance) >= 0;
        case GeoJSONConstants.MULTILINESTRING:
            for (List<Point> l : ((CoordinateContainer<List<List<Point>>>) g).coordinates()) {
                if (distanceToLineString(x, y, l, tolerance) >= 0) {
                    return true;
                }
            }
            break;
        case GeoJSONConstants.POLYGON:
        case GeoJSONConstants.MULTIPOLYGON:
            try {
                final Point point = Point.fromLngLat(x, y);
                return GeoJSONConstants.POLYGON.equals(g.type()) ? TurfJoins.inside(point, (Polygon) g) : TurfJoins.inside(point, (MultiPolygon) g);
            } catch (TurfException e) {
                Log.e(DEBUG_TAG, "Exception in getClicked " + e);
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unsupported geometry " + g.type());
        }
        return false;
    }

    /**
     * Check if the current touch position is in the tolerance area around a Point
     * 
     * @param viewBox the current screen ViewBox
     * @param tolerance the tolerance value
     * @param p the Position
     * @param x screen x coordinate of touch location
     * @param y screen y coordinate of touch location
     * @return true if touch position is in tolerance
     */
    private boolean inToleranceArea(float tolerance, @NonNull Point p, float x, float y) {
        float differenceX = (float) Math.abs(p.longitude() - x);
        float differenceY = (float) Math.abs(p.latitude() - y);
        return differenceX <= tolerance && differenceY <= tolerance && Math.hypot(differenceX, differenceY) <= tolerance;
    }

    /**
     * Determine if screen coords are within the tolerance for a geojson linestring
     * 
     * @param x x screen coord
     * @param y y screen coord
     * @param map map object
     * @param viewBox the current ViewBox
     * @param vertices the list of lineString vertices
     * @return if the returned value is > 0 then the coords are in the tolerance
     */
    private double distanceToLineString(final float x, final float y, List<Point> vertices, float tolerance) {
        float p1X = Float.MAX_VALUE;
        float p1Y = Float.MAX_VALUE;
        // Iterate over all points, but not the last one.
        for (int k = 0, verticesSize = vertices.size(); k < verticesSize - 1; ++k) {
            Point p1 = vertices.get(k);
            Point p2 = vertices.get(k + 1);
            if (k == 0) {
                p1X = (float) p1.longitude();
                p1Y = (float) p1.latitude();
            }
            float p2X = (float) p2.longitude();
            float p2Y = (float) p2.latitude();
            double distance = de.blau.android.util.Geometry.isPositionOnLine(tolerance / 2, x, y, p1X, p1Y, p2X, p2Y);
            if (distance >= 0) {
                return distance;
            }
            p1X = p2X;
            p1Y = p2Y;
        }
        return -1;
    }

    /**
     * Get the tile
     * 
     * @param z zoom level
     * @param x screen x
     * @param y screen y
     * @return the Tile or null
     */
    @Nullable
    private MapTile getTile(int z, float x, float y) {
        int n = 1 << z;
        int tileX = xTileNumber(GeoMath.xToLonE7(map.getWidth(), map.getViewBox(), x) / 1E7D, n);
        int tileY = yTileNumber((float) Math.toRadians(GeoMath.yToLatE7(map.getHeight(), map.getWidth(), map.getViewBox(), y) / 1E7D), n);
        return new MapTile(myRendererInfo.getId(), z, tileX, tileY);
    }

    @Override
    public void deselectObjects() {
        // Empty
    }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        JsonObject properties = new JsonObject();
        for (Entry<String, Object> e : f.getAttributes().entrySet()) {
            if (e.getValue() instanceof String) {
                properties.add(e.getKey(), new JsonPrimitive((String) e.getValue()));
            }
        }
        com.mapbox.geojson.Feature geojson = com.mapbox.geojson.Feature.fromGeometry(f.getGeometry(), properties);
        FeatureInfo.showDialog(activity, geojson);
    }

    @Override
    public de.blau.android.util.mvt.VectorTileDecoder.Feature getSelected() {
        return null;
    }

    @Override
    public void setSelected(de.blau.android.util.mvt.VectorTileDecoder.Feature o) {
        // Empty
    }

    @Override
    public String getDescription(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return f.getLayerName() + " " + f.getGeometry().type() + " " + f.getId();
    }
}
