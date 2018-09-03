// Created by plusminus on 21:31:36 - 25.09.2008
package de.blau.android.services.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.IMapTileProviderCallback;
import de.blau.android.util.NetworkStatus;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The OpenStreetMapTileDownloader loads tiles from a server and passes them to a
 * OpenStreetMapTileFilesystemProvider.<br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz
 * @author Manuel Stahl
 * @author Simon Poole
 *
 */
public class MapTileDownloader extends MapAsyncTileProvider {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final String DEBUGTAG = "OSM_DOWNLOADER";

    public static final long TIMEOUT = 5000;

    // ===========================================================
    // Fields
    // ===========================================================

    private final Context                   mCtx;
    private final MapTileFilesystemProvider mMapTileFSProvider;
    private final NetworkStatus             networkStatus;
    private final OkHttpClient              client;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Construct a new MapTileDownloader
     * 
     * @param ctx Android Context
     * @param aMapTileFSProvider a MapTileFilesystemProvider instance
     */
    public MapTileDownloader(@NonNull final Context ctx, @NonNull final MapTileFilesystemProvider aMapTileFSProvider) {
        mCtx = ctx;
        mMapTileFSProvider = aMapTileFSProvider;
        networkStatus = new NetworkStatus(ctx);
        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool((new Preferences(ctx)).getMaxTileDownloadThreads());
        client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).build();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected Runnable getTileLoader(MapTile aTile, IMapTileProviderCallback aCallback) {
        return new TileLoader(aTile, aCallback);
    }

    // ===========================================================
    // Methods org.andnav.osm.services
    // ===========================================================

    /**
     * Get the url for a tile
     * 
     * @param renderer a TileLayerServer instance
     * @param tile the tile
     * @return an url as a String
     */
    @NonNull
    private String buildURL(@NonNull TileLayerServer renderer, @NonNull final MapTile tile) {
        return renderer.isMetadataLoaded() ? renderer.getTileURLString(tile) : "";
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class TileLoader extends MapAsyncTileProvider.TileLoader {

        private static final String TILE_NOT_AVAILABLE = "tile not available";

        /**
         * Construct a new TileLoader
         * 
         * @param aTile the tile to download
         * @param aCallback the callback to call when finished
         */
        public TileLoader(@NonNull final MapTile aTile, @NonNull final IMapTileProviderCallback aCallback) {
            super(aTile, aCallback);
        }

        @Override
        public void run() {

            if (!networkStatus.isConnected()) { // fail immediately
                try {
                    Log.e(DEBUGTAG, "No network");
                    mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, NONETWORK);
                } catch (RemoteException re) {
                    Log.e(DEBUGTAG, "Error calling mapTileLoaded for MapTile. Exception: " + re);
                }
                return;
            }

            InputStream in = null;
            OutputStream out = null;
            ResponseBody responseBody = null;
            MediaType format = null;
            InputStream inputStream = null;
            Response tileCallResponse = null;
            TileLayerServer renderer = TileLayerServer.get(mCtx, mTile.rendererID, false);
            if (renderer != null) {
                final String tileURLString = buildURL(renderer, mTile);
                try {
                    if (tileURLString.length() > 0) {
                        if (Log.isLoggable(DEBUGTAG, Log.DEBUG)) {
                            Log.d(DEBUGTAG, "Downloading Maptile from url: " + tileURLString);
                        }

                        Request request = new Request.Builder().url(tileURLString).build();
                        Call tileCall = client.newCall(request);
                        tileCallResponse = tileCall.execute();
                        if (tileCallResponse.isSuccessful()) {
                            responseBody = tileCallResponse.body();
                            inputStream = responseBody.byteStream();
                            format = responseBody.contentType();
                        } else {
                            int code = tileCallResponse.code();
                            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                                throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                            } else {
                                throw new IOException("Code: " + code + " message: " + tileCallResponse.body().string());
                            }
                        }
                        String noTileHeader = renderer.getNoTileHeader();
                        if (noTileHeader != null) {
                            String headerValue = tileCallResponse.header(noTileHeader);
                            if (headerValue != null) {
                                String[] noTileValues = renderer.getNoTileValues();
                                if (noTileValues != null) {
                                    for (String v : noTileValues) {
                                        if (headerValue.equals(v)) {
                                            throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                        }
                                    }
                                } else {
                                    throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                }
                            }
                        }
                        in = new BufferedInputStream(inputStream, StreamUtils.IO_BUFFER_SIZE);
                        final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                        out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
                        StreamUtils.copy(in, out);
                        out.flush();

                        byte[] data = dataStream.toByteArray();

                        if (data.length == 0) {
                            throw new IOException("no tile data");
                        }
                        // if tile is in BMP format, compress
                        if (format != null && "BMP".equalsIgnoreCase(format.subtype())) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                            dataStream.reset();
                            bitmap.compress(CompressFormat.PNG, 100, dataStream);
                            data = dataStream.toByteArray();
                        }
                        mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                        MapTileDownloader.this.mMapTileFSProvider.saveFile(mTile, data);
                    }
                } catch (IOException ioe) {
                    try {
                        int reason = ioe instanceof FileNotFoundException ? DOESNOTEXIST : IOERR;
                        if (reason == DOESNOTEXIST) {
                            MapTileDownloader.this.mMapTileFSProvider.markAsInvalid(mTile);
                        }
                        mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, reason);
                    } catch (RemoteException re) {
                        Log.e(DEBUGTAG,
                                "Error calling mCallback for MapTile. Exception: " + ioe.getClass().getSimpleName() + " further mapTileFailed failed " + re,
                                ioe);
                    } catch (NullPointerException npe) {
                        Log.e(DEBUGTAG,
                                "Error calling mCallback for MapTile. Exception: " + ioe.getClass().getSimpleName() + " further mapTileFailed failed " + npe,
                                ioe);
                    } catch (IOException ioe2) {
                        Log.e(DEBUGTAG,
                                "Error calling mCallback for MapTile. Exception: " + ioe.getClass().getSimpleName() + " further mapTileFailed failed " + ioe2,
                                ioe);
                    }
                    if (!(ioe instanceof FileNotFoundException)) {
                        // FileNotFound is an expected exception, any other IOException should be logged
                        if (Log.isLoggable(DEBUGTAG, Log.ERROR)) {
                            Log.e(DEBUGTAG,
                                    "Error Downloading MapTile. Exception: " + ioe.getClass().getSimpleName() + " " + tileURLString + " " + ioe.getMessage());
                        }
                    }
                    /*
                     * TODO What to do when downloading tile caused an error? Also remove it from the mPending? Doing
                     * not blocks it for the whole existence of this TileDownloader. -> we remove it and the application
                     * has to re-request it.
                     */
                } catch (RemoteException | NullPointerException | IllegalArgumentException e) {
                    Log.e(DEBUGTAG, "Error in TileLoader. Url " + tileURLString + " Exception: " + e);
                } finally {
                    StreamUtils.closeStream(in);
                    StreamUtils.closeStream(out);
                    if (tileCallResponse != null) {
                        tileCallResponse.close();
                    }
                    finished();
                }
            }
        }
    }
}