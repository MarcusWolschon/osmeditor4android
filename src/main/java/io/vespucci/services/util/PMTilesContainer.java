package io.vespucci.services.util;

import java.io.File;
import java.io.IOException;

import android.util.Log;
import androidx.annotation.NonNull;
import ch.poole.geo.pmtiles.Reader;

public class PMTilesContainer implements LocalTileContainer {

    private static final String DEBUG_TAG = PMTilesContainer.class.getSimpleName().substring(0, Math.min(23, PMTilesContainer.class.getSimpleName().length()));

    private final Reader reader;

    public PMTilesContainer(@NonNull File file) throws IOException {
        reader = new Reader(file);
    }

    @Override
    public byte[] getTile(MapTile tile) throws IOException {
        return reader.getTile(tile.zoomLevel, tile.x, tile.y);
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Closing failed " + e.getMessage());
        }
    }
}