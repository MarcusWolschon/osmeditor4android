package de.blau.android;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import android.util.Log;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.FileUtil;
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
            TestUtils.copyFileFromResources(mbtSource, "/");
            tileDb = new MBTileProviderDataBase(context, Uri.fromFile(new File(FileUtil.getPublicDirectory(), mbtSource)), 1);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "mbt file not found " + e.getMessage());
            throw e;
        }
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        try (Buffer data = new Buffer()) {
            int x = Integer.parseInt(request.getRequestUrl().pathSegments().get(1));
            int y = Integer.parseInt(request.getRequestUrl().pathSegments().get(2));
            int z = Integer.parseInt(request.getRequestUrl().pathSegments().get(0));
            MapTile tile = new MapTile(null, z, x, y);
            byte[] bytes = tileDb.getTile(tile);
            if (bytes != null) {
                data.write(bytes);
                return new MockResponse().setResponseCode(200).setBody(data);
            }
            return new MockResponse().setResponseCode(400);
        } catch (IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "dispatch failed for " + request + " " + e.getMessage());
            return new MockResponse().setResponseCode(500);
        }
    }
}
