package de.blau.android.views.util;

import java.io.IOException;

import androidx.annotation.NonNull;

public interface MapTileProviderCallback {

    /**
     * Called after a tile has been loaded, copies the tile to the in memory cache
     * 
     * @param rendererID the tile renderer id
     * @param zoomLevel the zoom level
     * @param tileX tile x
     * @param tileY tile y
     * @param data tile image data
     * @throws IOException if something goes wrong receiving the tile
     */
    public void mapTileLoaded(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, @NonNull final byte[] data)
            throws IOException;

    /**
     * Called after a tile has failed
     * 
     * @param rendererID the tile renderer id
     * @param zoomLevel the zoom level
     * @param tileX tile x
     * @param tileY tile y
     * @param reason error code
     * @throws IOException if something goes wrong receiving the tile
     */
    public void mapTileFailed(@NonNull final String rendererID, final int zoomLevel, final int tileX, final int tileY, final int reason) throws IOException;
}
