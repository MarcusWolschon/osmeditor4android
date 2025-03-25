package io.vespucci.util;

public final class GeoJSONConstants {

    /**
     * Private constructor to stop instantiation
     */
    private GeoJSONConstants() {
        // private
    }

    public static final String FEATURE_COLLECTION = "FeatureCollection";
    public static final String FEATURE            = "Feature";
    public static final String FEATURES           = "features";
    public static final String BBOX               = "bbox";
    public static final String POINT              = "Point";
    public static final String MULTIPOINT         = "MultiPoint";
    public static final String LINESTRING         = "LineString";
    public static final String MULTILINESTRING    = "MultiLineString";
    public static final String POLYGON            = "Polygon";
    public static final String MULTIPOLYGON       = "MultiPolygon";
    public static final String GEOMETRYCOLLECTION = "GeometryCollection";
}
