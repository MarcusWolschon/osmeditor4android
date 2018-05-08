package de.blau.android.taginfo;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

import android.support.annotation.NonNull;

abstract class ResultReader {
    abstract Object read(@NonNull JsonReader reader) throws IOException;
}
