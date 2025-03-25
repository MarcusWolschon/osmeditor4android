package io.vespucci.resources.eli;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import io.vespucci.util.Version;

class Meta implements Serializable {

    private static final long serialVersionUID = 1L;

    Version formatVersion;
    String  generated;

    /**
     * Gson type adapter for parsing Gson to this class.
     *
     * @param gson the built {@link Gson} object
     * @return the TYPE adapter for this class
     */
    public static TypeAdapter<Meta> typeAdapter(Gson gson) {
        return new GsonTypeAdapter(gson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatVersion, generated);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Meta)) {
            return false;
        }
        Meta other = (Meta) obj;
        return Objects.equals(formatVersion, other.formatVersion) && Objects.equals(generated, other.generated);
    }

    /**
     * Adapter to read and write a Meta object.
     *
     */
    @Keep
    static final class GsonTypeAdapter extends TypeAdapter<Meta> {
        private static final int TAG_LEN = Math.min(23, GsonTypeAdapter.class.getSimpleName().length());
        private static final String DEBUG_TAG = GsonTypeAdapter.class.getSimpleName().substring(0, TAG_LEN);

        private static final String NAME_FORMAT_VERSION = "format_version";
        private static final String NAME_GENERATED      = "generated";

        private TypeAdapter<String> stringTypeAdapter;
        private final Gson          gson;

        /**
         * Construct a new TypeAdapter
         * 
         * @param gson the Gson object
         */
        GsonTypeAdapter(@NonNull Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Meta value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name(NAME_GENERATED);
            if (value.generated == null) {
                out.nullValue();
            } else {
                getStringTypeAdapter();
                stringTypeAdapter.write(out, value.generated);
            }
            out.name(NAME_FORMAT_VERSION);
            if (value.formatVersion == null) {
                out.nullValue();
            } else {
                getStringTypeAdapter();
                stringTypeAdapter.write(out, value.formatVersion.toString());
            }
            out.endObject();
        }

        @Override
        public Meta read(JsonReader in) throws IOException {
            Meta meta = new Meta();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    continue;
                }
                switch (name) {
                case NAME_GENERATED:
                case NAME_FORMAT_VERSION:
                    getStringTypeAdapter();
                    if (NAME_GENERATED.equals(name)) {
                        meta.generated = stringTypeAdapter.read(in);
                    } else {

                        meta.formatVersion = new Version(stringTypeAdapter.read(in));
                    }
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown meta field " + name);
                }
            }
            in.endObject();
            return meta;
        }

        /**
         * Get a TypeAdapter for Strings
         * 
         * Sets stringTypeAdapter
         */
        void getStringTypeAdapter() {
            if (stringTypeAdapter == null) {
                stringTypeAdapter = gson.getAdapter(String.class);
            }
        }
    }
}