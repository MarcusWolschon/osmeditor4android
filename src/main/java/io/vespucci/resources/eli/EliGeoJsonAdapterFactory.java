/**
 * Adopted from original code from Mapbox licensed under the following terms
 * 
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018 Mapbox
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.vespucci.resources.eli;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.GeometryCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import androidx.annotation.Keep;

/**
 * A GeoJson type adapter factory for convenience for serialization/deserialization.
 *
 */
@Keep
public abstract class EliGeoJsonAdapterFactory implements TypeAdapterFactory { // NOSONAR

    /**
     * Private class to disallow instantiation
     */
    private EliGeoJsonAdapterFactory() {
        // private
    }

    /**
     * Create a new instance of this GeoJson type adapter factory, this is passed into the Gson Builder.
     *
     * @return a new GSON TypeAdapterFactory
     */
    public static TypeAdapterFactory create() {
        return new GeoJsonAdapterFactoryIml();
    }

    /**
     * GeoJsonAdapterFactory implementation.
     *
     */
    public static final class GeoJsonAdapterFactoryIml extends EliGeoJsonAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<?> rawType = type.getRawType();
            if (BoundingBox.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) BoundingBox.typeAdapter(gson);
            } else if (Feature.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) Feature.typeAdapter(gson);
            } else if (EliFeatureCollection.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) EliFeatureCollection.typeAdapter(gson);
            } else if (GeometryCollection.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) GeometryCollection.typeAdapter(gson);
            } else if (LineString.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) LineString.typeAdapter(gson);
            } else if (MultiLineString.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) MultiLineString.typeAdapter(gson);
            } else if (MultiPoint.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) MultiPoint.typeAdapter(gson);
            } else if (MultiPolygon.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) MultiPolygon.typeAdapter(gson);
            } else if (Polygon.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) Polygon.typeAdapter(gson);
            } else if (Point.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) Point.typeAdapter(gson);
            } else if (Meta.class.isAssignableFrom(rawType)) {
                return (TypeAdapter<T>) Meta.typeAdapter(gson);
            }
            return null;
        }
    }
}
