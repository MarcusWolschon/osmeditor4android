package io.vespucci;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.vespucci.services.util.MBTileProviderDataBase;
import io.vespucci.services.util.MapTile;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class TileDispatcher extends Dispatcher {
    private static final String  DEBUG_TAG = "Dispatcher";
    final MBTileProviderDataBase tileDb;

    /**
     * Construct a new dispatcher that will return tiles from a mapbox tile source
     * 
     * @param context an Android Context
     * @param mbtSource the name of the source
     * @throws IOException if the source can't copied out of the assets and opened
     */
    public TileDispatcher(@NonNull Context context, @NonNull String mbtSource) throws IOException {
        try {
            File destinationDir = ContextCompat.getExternalCacheDirs(context)[0];
            File mbtFile = new File(destinationDir, mbtSource);
            JavaResources.copyFileFromResources(mbtSource, null, mbtFile);
            if (!mbtFile.exists()) {
                throw new IOException(mbtFile.getAbsolutePath() + " doesn't exist");
            }
            tileDb = new MBTileProviderDataBase(context, Uri.fromFile(mbtFile), 1);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "mbt file " + mbtSource + " not found " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get the configured MTB source
     * 
     * @return a MBTileProviderDataBase instance
     */
    @NonNull
    public MBTileProviderDataBase getSource() {
        return tileDb;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        Log.i(DEBUG_TAG, "request " + request);
        try (Buffer data = new Buffer()) {
            int x = Integer.parseInt(request.getRequestUrl().pathSegments().get(1));
            int y = Integer.parseInt(request.getRequestUrl().pathSegments().get(2));
            int z = Integer.parseInt(request.getRequestUrl().pathSegments().get(0));
            MapTile tile = new MapTile("", z, x, y);
            byte[] bytes = tileDb.getTile(tile);
            if (bytes != null) {
                data.write(bytes);
                return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(data);
            }
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "dispatch failed for " + request + " " + e.getMessage());
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }
}
