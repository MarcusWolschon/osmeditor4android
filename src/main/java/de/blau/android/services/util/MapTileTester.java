package de.blau.android.services.util;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.layer.tiles.util.MapTileProviderCallback;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.util.ExecutorTask;

public class MapTileTester {
    private static final String DEBUG_TAG = MapTileTester.class.getSimpleName().substring(0, Math.min(23, MapTileTester.class.getSimpleName().length()));

    private final Runnable      testTileLoader;
    private final StringBuilder output    = new StringBuilder();
    private byte[]              tileData;
    private boolean             succeeded = false;
    private TileType            tileType  = TileType.BITMAP;

    public MapTileTester(@NonNull Context ctx, @NonNull MapTile tile) {
        output.append(ctx.getString(R.string.diagnostics_for_tile, tile.rendererID));
        eol();
        output.append(ctx.getString(R.string.tile_spec, tile.zoomLevel, tile.x, tile.y));
        eol();
        eol();

        MapTileProviderCallback callback = new MapTileProviderCallback() {

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] data) throws IOException {
                TileLayerSource renderer = TileLayerSource.get(ctx, rendererID, false);
                if (renderer != null) {
                    tileType = renderer.getTileType();
                }
                output.append(ctx.getString(R.string.tile_data_received, data.length));
                eol();
                succeeded = true;
            }

            @Override
            public void mapTileFailed(String rendererID, int zoomLevel, int tileX, int tileY, int reason, String message) throws IOException {
                TileLayerSource renderer = TileLayerSource.get(ctx, rendererID, false);
                if (renderer != null) {
                    String url = MapTileDownloader.buildURL(renderer, new MapTile(rendererID, zoomLevel, tileX, tileY));
                    output.append(ctx.getString(R.string.tile_input_error, message, url));
                }
                eol();
            }
        };

        MapTileSaver saver = new MapTileSaver() {

            @Override
            public void saveTile(MapTile tile, byte[] data) throws IOException {
                tileData = data;
            }

            @Override
            public void markAsInvalid(MapTile mTile) throws IOException {
                output.append(ctx.getString(R.string.tile_mark_invalid));
                eol();
            }
        };

        MapTileDownloader downloader = new MapTileDownloader(ctx, saver);
        testTileLoader = downloader.getTileLoader(tile, callback);
    }

    /**
     * Append an eol
     */
    private void eol() {
        output.append("\n");
    }

    /**
     * Run the test
     * 
     * @return true if successful
     */
    public boolean run() {

        ExecutorTask<Void, Void, Boolean> task = new ExecutorTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void param) {
                testTileLoader.run();
                return succeeded;
            }
        };

        try {
            Boolean result = task.execute().get(5, TimeUnit.SECONDS);
            return result != null && result;
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, "run execption " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the output of the test, call after run
     * 
     * @return any output the test produced
     */
    @NonNull
    public String getOutput() {
        return output.toString();
    }

    /**
     * Get the retrieved tile data, call after run
     * 
     * @return any tile data received or null
     */
    @Nullable
    public byte[] getTile() {
        return tileData;
    }

    /**
     * @return the tileType
     */
    @NonNull
    public TileType getTileType() {
        return tileType;
    }
}
