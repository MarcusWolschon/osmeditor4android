package io.vespucci.services.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.database.Cursor;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import androidx.annotation.NonNull;
import io.vespucci.exception.InvalidTileException;
import io.vespucci.services.util.MBTileProviderDataBase;
import io.vespucci.services.util.MapTile;
import io.vespucci.services.util.MapTileProviderDataBase;

@Implements(value = SQLiteStatement.class)
public class ShadowSQLiteStatement extends ShadowSQLiteProgram {

    /**
     * Mock implementation that returns a ParcelFileDescriptor for a sample tile
     * 
     * @return a ParcelFileDescriptor for a sample tile
     */
    @Implementation
    protected ParcelFileDescriptor simpleQueryForBlobFileDescriptor() {
        String rendererId = stringMap.get(1);
        try {
            byte[] data = null;
            if (longMap.get(1) == null) {
                Long zoomLevel = longMap.get(2);
                Long x = longMap.get(3);
                Long y = longMap.get(4);
                MapTile tile = new MapTile(rendererId, zoomLevel.intValue(), x.intValue(), y.intValue());
                data = getTile(tile);
            } else { // MBT file
                Long zoomLevel = longMap.get(1);
                Long x = longMap.get(2);
                Long y = longMap.get(3);
                MapTile tile = new MapTile(null, zoomLevel.intValue(), x.intValue(), y.intValue());
                data = getMBTile(tile);
            }

            try (InputStream is = new ByteArrayInputStream(data);) {
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                AutoCloseOutputStream os = new AutoCloseOutputStream(pipe[1]);
                int len;
                while ((len = is.read()) >= 0) {
                    os.write(len);
                }
                os.flush();
                os.close();
                return pipe[0];
            } catch (IOException ioex) {
                // ignore
            }
        } catch (InvalidTileException e) {
            // simply return null
        }
        return null;
    }

    /**
     * Get a tile from the cache
     * 
     * @param tile the MapTile
     * @return the data for the tile image
     * @throws InvalidTileException if the tile has been marked as invalid
     */
    private byte[] getTile(@NonNull MapTile tile) throws InvalidTileException {
        try (final Cursor c = db.query(MapTileProviderDataBase.T_FSCACHE, new String[] { MapTileProviderDataBase.T_FSCACHE_DATA },
                MapTileProviderDataBase.T_FSCACHE_WHERE_NOT_INVALID, MapTileProviderDataBase.tileToWhereArgs(tile), null, null, null)) {
            if (c.moveToFirst()) {
                byte[] data = c.getBlob(c.getColumnIndexOrThrow(MapTileProviderDataBase.T_FSCACHE_DATA));
                if (data == null) {
                    throw new InvalidTileException(MapTileProviderDataBase.TILE_MARKED_INVALID_IN_DATABASE);
                }
                return data;
            } else {
                throw new SQLiteDoneException("nothing found");
            }
        }
    }

    /**
     * Get a tile from an MBT DB
     * 
     * @param tile the MapTile
     * @return the data for the tile image
     * @throws InvalidTileException if the tile has been marked as invalid
     */
    private byte[] getMBTile(@NonNull MapTile tile) throws InvalidTileException {
        try (final Cursor c = db.query(MBTileProviderDataBase.T_MBTILES, new String[] { MBTileProviderDataBase.T_MBTILES_DATA },
                MBTileProviderDataBase.T_MBTILES_WHERE, new String[] { Integer.toString(tile.zoomLevel), Integer.toString(tile.x), Integer.toString(tile.y) },
                null, null, null)) {
            if (c.moveToFirst()) {
                byte[] data = c.getBlob(c.getColumnIndexOrThrow(MapTileProviderDataBase.T_FSCACHE_DATA));
                if (data == null) {
                    throw new InvalidTileException(MapTileProviderDataBase.TILE_MARKED_INVALID_IN_DATABASE);
                }
                return data;
            } else {
                throw new SQLiteDoneException("nothing found");
            }
        }
    }
}
