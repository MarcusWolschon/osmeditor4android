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
package de.blau.android.resources.eli;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.GeoJson;
import com.mapbox.geojson.GeometryAdapterFactory;
import com.mapbox.geojson.gson.BoundingBoxTypeAdapter;
import com.mapbox.geojson.gson.GeoJsonAdapterFactory;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.Version;

@Keep
public final class EliFeatureCollection implements GeoJson {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String TYPE = "FeatureCollection";

    private final String type;

    private final Meta meta;

    @JsonAdapter(BoundingBoxTypeAdapter.class)
    private final BoundingBox bbox;

    private final List<Feature> features;

    /**
     * Create a new instance of this class by passing in a formatted valid JSON String.
     * 
     * @param json a formatted valid JSON string defining a GeoJson Feature Collection
     * @return a new instance of this class defined by the values passed inside this static factory method
     */
    public static EliFeatureCollection fromJson(@NonNull String json) {
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create());
        gson.registerTypeAdapterFactory(GeometryAdapterFactory.create());
        return gson.create().fromJson(json, EliFeatureCollection.class);
    }

    /**
     * Create a new FestureCollection
     * 
     * @param type the GeoJson type
     * @param meta the Meta object
     * @param bbox the BoundingBox object
     * @param features a List of Features
     */
    EliFeatureCollection(String type, @Nullable Meta meta, @Nullable BoundingBox bbox, @Nullable List<Feature> features) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }
        this.type = type;
        this.meta = meta;
        this.bbox = bbox;
        this.features = features;
    }

    /**
     * Get the format version of the configuration
     * 
     * @return the format version of the configuration
     */
    @Nullable
    public Version formatVersion() {
        return meta != null ? meta.formatVersion : null;
    }

    /**
     * Get the date and time when the configuration was generated as a String
     * 
     * @return the date and time when the configuration was generated as a String
     */
    @Nullable
    public String generated() {
        return meta != null ? meta.generated : null;
    }

    /**
     * This describes the type of GeoJson this object is, thus this will always return {@link FeatureCollection}.
     *
     * @return a String which describes the TYPE of GeoJson, for this object it will always return
     *         {@code FeatureCollection}
     * @since 1.0.0
     */
    @NonNull
    @Override
    public String type() {
        return type;
    }

    /**
     * Get the Meta object if any
     * 
     * @return the Meta object or null
     */
    @Nullable
    public Meta meta() {
        return meta;
    }

    /**
     * A Feature Collection might have a member named {@code bbox} to include information on the coordinate range for
     * it's {@link Feature}s. The value of the bbox member MUST be a list of size 2*n where n is the number of
     * dimensions represented in the contained feature geometries, with all axes of the most southwesterly point
     * followed by all axes of the more northeasterly point. The axes order of a bbox follows the axes order of
     * geometries.
     *
     * @return a list of double coordinate values describing a bounding box
     * @since 3.0.0
     */
    @Nullable
    @Override
    public BoundingBox bbox() {
        return bbox;
    }

    /**
     * This provides the list of feature making up this Feature Collection. Note that if the FeatureCollection was
     * created through {@link #fromJson(String)} this list could be null. Otherwise, the list can't be null but the size
     * of the list can equal 0.
     *
     * @return a list of {@link Feature}s which make up this Feature Collection
     * @since 1.0.0
     */
    @Nullable
    public List<Feature> features() {
        return features;
    }

    /**
     * This takes the currently defined values found inside this instance and converts it to a GeoJson string.
     *
     * @return a JSON string which represents this Feature Collection
     * @since 1.0.0
     */
    @Override
    public String toJson() {

        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create());
        gson.registerTypeAdapterFactory(GeometryAdapterFactory.create());
        return gson.create().toJson(this);
    }

    /**
     * Gson type adapter for parsing Gson to this class.
     *
     * @param gson the built {@link Gson} object
     * @return the TYPE adapter for this class
     * @since 3.0.0
     */
    public static TypeAdapter<EliFeatureCollection> typeAdapter(Gson gson) {
        return new EliFeatureCollection.GsonTypeAdapter(gson);
    }

    @Override
    public String toString() {
        return "FeatureCollection{" + "type=" + type + ", " + "bbox=" + bbox + ", " + "features=" + features + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EliFeatureCollection) {
            EliFeatureCollection that = (EliFeatureCollection) obj;
            return (this.type.equals(that.type())) && ((this.bbox == null) ? (that.bbox() == null) : this.bbox.equals(that.bbox()))
                    && ((this.features == null) ? (that.features() == null) : this.features.equals(that.features()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode *= 1000003;
        hashCode ^= type.hashCode();
        hashCode *= 1000003;
        hashCode ^= (bbox == null) ? 0 : bbox.hashCode();
        hashCode *= 1000003;
        hashCode ^= (features == null) ? 0 : features.hashCode();
        return hashCode;
    }

    /**
     * TypeAdapter to serialize/deserialize FeatureCollection objects.
     */
    static final class GsonTypeAdapter extends TypeAdapter<EliFeatureCollection> {
        private static final String NAME_FEATURES = "features";
        private static final String NAME_BBOX     = "bbox";
        private static final String NAME_META     = "meta";
        private static final String NAME_TYPE     = "type";

        private volatile TypeAdapter<String>        stringAdapter;
        private volatile TypeAdapter<BoundingBox>   boundingBoxAdapter;
        private volatile TypeAdapter<List<Feature>> listFeatureAdapter;
        private volatile TypeAdapter<Meta>          metaAdapter;
        private final Gson                          gson;

        /**
         * Construct a new TypeAdapter
         * 
         * @param gson the Gson object
         */
        GsonTypeAdapter(@NonNull Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter jsonWriter, EliFeatureCollection object) throws IOException {
            if (object == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.beginObject();
            jsonWriter.name(NAME_TYPE);
            TypeAdapter<String> stringAdapter = this.stringAdapter;
            if (stringAdapter == null) {
                stringAdapter = gson.getAdapter(String.class);
                this.stringAdapter = stringAdapter;
            }
            stringAdapter.write(jsonWriter, object.type());
            jsonWriter.name(NAME_META);
            if (object.meta() == null) {
                jsonWriter.nullValue();
            } else {
                TypeAdapter<Meta> metaAdapter = this.metaAdapter;
                if (metaAdapter == null) {
                    metaAdapter = gson.getAdapter(Meta.class);
                    this.metaAdapter = metaAdapter;
                }
                metaAdapter.write(jsonWriter, object.meta());
            }
            jsonWriter.name(NAME_BBOX);
            if (object.bbox() == null) {
                jsonWriter.nullValue();
            } else {
                TypeAdapter<BoundingBox> boundingBoxTypeAdapter = this.boundingBoxAdapter;
                if (boundingBoxTypeAdapter == null) {
                    boundingBoxTypeAdapter = gson.getAdapter(BoundingBox.class);
                    this.boundingBoxAdapter = boundingBoxTypeAdapter;
                }
                boundingBoxTypeAdapter.write(jsonWriter, object.bbox());
            }
            jsonWriter.name(NAME_FEATURES);
            if (object.features() == null) {
                jsonWriter.nullValue();
            } else {
                TypeAdapter<List<Feature>> listFeatureAdapter = this.listFeatureAdapter;
                if (listFeatureAdapter == null) {
                    TypeToken typeToken = TypeToken.getParameterized(List.class, Feature.class);
                    listFeatureAdapter = (TypeAdapter<List<Feature>>) gson.getAdapter(typeToken);
                    this.listFeatureAdapter = listFeatureAdapter;
                }
                listFeatureAdapter.write(jsonWriter, object.features());
            }
            jsonWriter.endObject();
        }

        @Override
        public EliFeatureCollection read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            jsonReader.beginObject();
            String type = null;
            Meta meta = null;
            BoundingBox bbox = null;
            List<Feature> features = null;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    continue;
                }
                switch (name) {
                case NAME_TYPE:
                    TypeAdapter<String> stringAdapter = this.stringAdapter;
                    if (stringAdapter == null) {
                        stringAdapter = gson.getAdapter(String.class);
                        this.stringAdapter = stringAdapter;
                    }
                    type = stringAdapter.read(jsonReader);
                    break;

                case NAME_META:
                    TypeAdapter<Meta> metaAdapter = this.metaAdapter;
                    if (metaAdapter == null) {
                        metaAdapter = gson.getAdapter(Meta.class);
                        this.metaAdapter = metaAdapter;
                    }
                    meta = metaAdapter.read(jsonReader);
                    break;

                case NAME_BBOX:
                    TypeAdapter<BoundingBox> boundingBoxAdapter = this.boundingBoxAdapter;
                    if (boundingBoxAdapter == null) {
                        boundingBoxAdapter = gson.getAdapter(BoundingBox.class);
                        this.boundingBoxAdapter = boundingBoxAdapter;
                    }
                    bbox = boundingBoxAdapter.read(jsonReader);
                    break;

                case NAME_FEATURES:
                    TypeAdapter<List<Feature>> listFeatureAdapter = this.listFeatureAdapter;
                    if (listFeatureAdapter == null) {
                        TypeToken typeToken = TypeToken.getParameterized(List.class, Feature.class);
                        listFeatureAdapter = (TypeAdapter<List<Feature>>) gson.getAdapter(typeToken);
                        this.listFeatureAdapter = listFeatureAdapter;
                    }
                    features = listFeatureAdapter.read(jsonReader);
                    break;

                default:
                    jsonReader.skipValue();

                }
            }
            jsonReader.endObject();
            return new EliFeatureCollection(type, meta, bbox, features);
        }
    }
}
