package io.vespucci.services.util;

import java.io.IOException;

import androidx.annotation.NonNull;

public interface MapTileSaver {
    
    /**
     * Save the image data for a tile to the database, making space if necessary
     * 
     * @param tile tile meta-data
     * @param data the tile image data
     * @throws IOException if saving the file goes wrong
     */
    public void saveTile(final MapTile tile, final byte[] data) throws IOException;
    
    /**
     * Mark a tile as invalid (really doesn't exist)
     * 
     * @param mTile tile meta-data
     * @throws IOException if writing to the database fails
     */
    public void markAsInvalid(@NonNull MapTile mTile) throws IOException;
}
