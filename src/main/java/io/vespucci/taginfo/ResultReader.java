package io.vespucci.taginfo;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

import androidx.annotation.NonNull;

/**
 * Process the input from a JsonReader
 * 
 * @author Simon Poole
 *
 */
interface ResultReader {
    /**
     * Process the input from a JsonReader
     * 
     * @param reader the JsonReader
     * @return an Object
     * @throws IOException if reading caused an error
     */
    Object read(@NonNull JsonReader reader) throws IOException;
}
