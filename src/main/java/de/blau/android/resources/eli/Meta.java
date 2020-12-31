package de.blau.android.resources.eli;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import android.util.Log;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import de.blau.android.util.Version;

class Meta {
    Version formatVersion;
    String    generated;

    /**
     * Gson type adapter for parsing Gson to this class.
     *
     * @param gson the built {@link Gson} object
     * @return the TYPE adapter for this class
     * @since 3.0.0
     */
    public static TypeAdapter<Meta> typeAdapter(Gson gson) {
        return new MetaTypeAdapter(gson);
    }

    /**
     * Adapter to read and write a Meta object.
     *
     */
    @Keep
    static final class MetaTypeAdapter extends TypeAdapter<Meta> {

        private static final String          NAME_FORMAT_VERSION = "format_version";
        private static final String          NAME_GENERATED      = "generated";
        private static final String          DEBUG_TAG           = "MetaTypeAdapter";
        private volatile TypeAdapter<String> stringTypeAdapter;

        private final Gson gson;

        /**
         * Construct a new TypeAdapter
         * 
         * @param gson the Gson object
         */
        MetaTypeAdapter(@NonNull Gson gson) {
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
                TypeAdapter<String> stringTypeAdapter = this.stringTypeAdapter;
                if (stringTypeAdapter == null) {
                    stringTypeAdapter = gson.getAdapter(String.class);
                    this.stringTypeAdapter = stringTypeAdapter;
                }
                stringTypeAdapter.write(out, value.generated);
            }
            out.name(NAME_FORMAT_VERSION);
            if (value.formatVersion == null) {
                out.nullValue();
            } else {
                TypeAdapter<String> stringTypeAdapter = this.stringTypeAdapter;
                if (stringTypeAdapter == null) {
                    stringTypeAdapter = gson.getAdapter(String.class);
                    this.stringTypeAdapter = stringTypeAdapter;
                }
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
                    TypeAdapter<String> strTypeAdapter = this.stringTypeAdapter;
                    if (strTypeAdapter == null) {
                        strTypeAdapter = gson.getAdapter(String.class);
                        this.stringTypeAdapter = strTypeAdapter;
                    }
                    if (NAME_GENERATED.equals(name)) {
                            meta.generated = strTypeAdapter.read(in);
                    } else {
                        meta.formatVersion = new Version(strTypeAdapter.read(in));
                    }
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown meta field " + name);
                }
            }
            in.endObject();
            return meta;
        }
    }
}