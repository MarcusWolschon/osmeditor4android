package de.blau.android.services.util;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.ExecutorTask;
import de.blau.android.views.util.MapTileProviderCallback;

public class MapTileTester {
    private final Runnable      tester;
    private final StringBuilder output    = new StringBuilder();
    private byte[]              tileData;
    private boolean             succeeded = false;

    public MapTileTester(@NonNull Context ctx, @NonNull MapTile tile) {
        output.append(ctx.getString(R.string.diagnostics_for_tile, tile.rendererID, tile.zoomLevel, tile.x, tile.y));
        eol();

        MapTileProviderCallback callback = new MapTileProviderCallback() {

            @Override
            public void mapTileLoaded(String rendererID, int zoomLevel, int tileX, int tileY, byte[] data) throws IOException {
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
            public void saveFile(MapTile tile, byte[] data) throws IOException {
                tileData = data;
            }

            @Override
            public void markAsInvalid(MapTile mTile) throws IOException {
                output.append(ctx.getString(R.string.tile_mark_invalid));
                eol();
            }
        };

        MapTileDownloader downloader = new MapTileDownloader(ctx, saver);
        tester = downloader.getTileLoader(tile, callback);
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
                tester.run();
                return succeeded;
            }
        }.execute();
        try {
            Boolean result = task.get();
            return result != null && result;
        } catch (InterruptedException | ExecutionException e) { // NOSONAR
            e.printStackTrace();
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
}
