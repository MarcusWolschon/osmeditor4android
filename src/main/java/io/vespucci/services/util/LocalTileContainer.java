package io.vespucci.services.util;

import java.io.IOException;

import androidx.annotation.NonNull;

public interface LocalTileContainer {

    /**
     * Returns requested tile
     * 
     * @param aTile the tile meta data
     * @return the contents of the tile or null on failure to retrieve
     * @throws IOException if we had issues reading from the database
     */
    public byte[] getTile(@NonNull MapTile tile) throws IOException;

    /**
     * Close the container
     */
    public void close();

}