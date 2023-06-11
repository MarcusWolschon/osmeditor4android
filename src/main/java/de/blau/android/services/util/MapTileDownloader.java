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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.contract.MimeTypes;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Header;
import de.blau.android.util.NetworkStatus;
import de.blau.android.views.util.MapTileProvider;
import de.blau.android.views.util.MapTileProviderCallback;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The OpenStreetMapTileDownloader loads tiles from a server and passes them to a OpenStreetMapTileFilesystemProvider.
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.androin OSMEditor.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Manuel Stahl
 * @author Simon Poole
 *
 */
public class MapTileDownloader extends MapAsyncTileProvider {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final String DEBUG_TAG = "MapTileDownloader";

    public static final long TIMEOUT = 5000;

    // ===========================================================
    // Fields
    // ===========================================================

    private final Context       mCtx;
    private final MapTileSaver  mapTileSaver;
    private final NetworkStatus networkStatus;
    private final OkHttpClient  client;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Construct a new MapTileDownloader
     * 
     * @param ctx Android Context
     * @param mapTileSaver a MapTileFilesystemProvider instance
     */
    public MapTileDownloader(@NonNull final Context ctx, @NonNull final MapTileSaver mapTileSaver) {
        mCtx = ctx;
        this.mapTileSaver = mapTileSaver;
        networkStatus = new NetworkStatus(ctx);
        mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(App.getPreferences(ctx).getMaxTileDownloadThreads());
        client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).build();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    protected Runnable getTileLoader(MapTile aTile, MapTileProviderCallback aCallback) {
        return new TileLoader(aTile, aCallback);
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    private class TileLoader extends MapAsyncTileProvider.TileLoader {

        private static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";
        private static final String GZIP                        = "gzip";

        private static final String TILE_NOT_AVAILABLE = "tile not available";

        /**
         * Construct a new TileLoader
         * 
         * @param aTile the tile to download
         * @param aCallback the callback to call when finished
         */
        public TileLoader(@NonNull final MapTile aTile, @NonNull final MapTileProviderCallback aCallback) {
            super(aTile, aCallback);
        }

        /**
         * Get the url for a tile
         * 
         * @param renderer a TileLayerServer instance
         * @param tile the tile
         * @return an url as a String
         */
        @NonNull
        private String buildURL(@NonNull TileLayerSource renderer, @NonNull final MapTile tile) {
            return renderer.isMetadataLoaded() ? renderer.getTileURLString(tile) : "";
        }

        @Override
        public void run() {
            if (!networkStatus.isConnected()) { // fail immediately
                try {
                    Log.e(DEBUG_TAG, "No network");
                    mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, NONETWORK);
                } catch (IOException re) {
                    Log.e(DEBUG_TAG, "Error calling mapTileLoaded for MapTile. Exception: " + re);
                }
                return;
            }
            TileLayerSource renderer = TileLayerSource.get(mCtx, mTile.rendererID, false);
            if (renderer != null) {
                final String tileURLString = buildURL(renderer, mTile);
                if (tileURLString.length() > 0) {
                    if (Log.isLoggable(DEBUG_TAG, Log.DEBUG)) {
                        Log.d(DEBUG_TAG, "Downloading Maptile from url: " + tileURLString);
                    }
                    try {
                        Builder builder = new Request.Builder().url(tileURLString);
                        addCustomHeaders(renderer, builder);
                        Request request = builder.addHeader(HTTP_HEADER_ACCEPT_ENCODING, GZIP).build();
                        Call tileCall = client.newCall(request);
                        try (Response tileCallResponse = tileCall.execute()) {
                            if (tileCallResponse.isSuccessful()) {
                                ResponseBody responseBody = tileCallResponse.body();
                                InputStream inputStream = responseBody.byteStream();
                                MediaType format = responseBody.contentType();
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
                                try (final InputStream in = new BufferedInputStream(inputStream, StreamUtils.IO_BUFFER_SIZE);
                                        final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                                        final OutputStream out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE)) {
                                    StreamUtils.copy(in, out);
                                    out.flush();

                                    byte[] data = dataStream.toByteArray();
                                    if (data.length == 0) {
                                        throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                    }
                                    // check format
                                    if (format != null) {
                                        switch (format.type().toLowerCase(Locale.US)) {
                                        case MimeTypes.IMAGE_TYPE:
                                            if (MimeTypes.BMP_SUBTYPE.equalsIgnoreCase(format.subtype())) {
                                                // if tile is in BMP format, compress
                                                data = compressBitmap(CompressFormat.PNG, dataStream, data);
                                            }
                                            break;
                                        case MimeTypes.TEXT_TYPE:
                                            // this can't be a tile and is likely an error message
                                            Log.e(DEBUG_TAG, responseBody.string());
                                            throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                        case MimeTypes.APPLICATION_TYPE: // WMS errors, MVT tiles
                                            switch (format.subtype().toLowerCase()) {
                                            case MimeTypes.WMS_EXCEPTION_XML_SUBTYPE:
                                            case MimeTypes.JSON_SUBTYPE:
                                                Log.e(DEBUG_TAG, responseBody.string());
                                                throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                            case MimeTypes.MVT_SUBTYPE:
                                            case MimeTypes.X_PROTOBUF_SUBTYPE:
                                                byte[] noTileTile = renderer.getNoTileTile();
                                                if (noTileTile != null && data.length == noTileTile.length && Arrays.equals(data, noTileTile)) {
                                                    Log.e(DEBUG_TAG, "MVT \"no tile\" tile for " + mTile);
                                                    throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                                }
                                                break;
                                            default:
                                                Log.e(DEBUG_TAG, "Application sub type " + format.subtype());
                                            }
                                            break;
                                        default:
                                            Log.e(DEBUG_TAG, "Unexpected response format " + format + " tile url " + tileURLString);
                                            throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                        }
                                    }
                                    mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                                    mapTileSaver.saveFile(mTile, data);
                                }
                            } else {
                                int code = tileCallResponse.code();
                                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                                    throw new FileNotFoundException(TILE_NOT_AVAILABLE);
                                } else {
                                    throw new IOException("Code: " + code + " message: " + tileCallResponse.body().string());
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        try {
                            int reason = ioe instanceof FileNotFoundException ? DOESNOTEXIST : IOERR; // NOSONAR
                            if (reason == DOESNOTEXIST) {
                                mapTileSaver.markAsInvalid(mTile);
                            } else { // FileNotFound is an expected exception, any other IOException should be logged,
                                     // and
                                     // reported a an error
                                Log.e(DEBUG_TAG, "Error Downloading MapTile. Exception: " + ioe.getClass().getSimpleName() + " " + tileURLString + " "
                                        + ioe.getMessage());
                                mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, reason);
                            }
                        } catch (NullPointerException | IOException e) {
                            Log.e(DEBUG_TAG,
                                    "Error calling mCallback for MapTile. Exception: " + ioe.getClass().getSimpleName() + " further mapTileFailed failed " + e,
                                    ioe);
                        }
                    } catch (NullPointerException | IllegalArgumentException e) {
                        Log.e(DEBUG_TAG, "Error in TileLoader. Url " + tileURLString + " Exception: " + e);
                    } finally {
                        /*
                         * What to do when downloading tile caused an error? Also remove it from the mPending? Not doing
                         * so blocks it for the whole existence of this TileDownloader. -> we remove it and the
                         * application has to re-request it.
                         */
                        finished();
                    }
                }
            }
        }

        /**
         * Add custom headers from configuration to the request
         * 
         * @param tileLayerSource source config
         * @param builder the request builder
         */
        private void addCustomHeaders(@NonNull TileLayerSource tileLayerSource, @NonNull Builder builder) {
            List<Header> headers = tileLayerSource.getHeaders();
            if (headers != null) {
                for (Header h : headers) {
                    builder.header(h.getName(), h.getValue());
                }
            }
        }

        /**
         * Compress bitmap
         * 
         * @param compressFormat destination format
         * @param dataStream preallocated datastream for conversion
         * @param data input data
         * @return the compressed data
         */
        private byte[] compressBitmap(@NonNull CompressFormat compressFormat, @NonNull final ByteArrayOutputStream dataStream, @NonNull byte[] data) {
            data = MapTileProvider.unGZip(data); // unzip if compressed
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
            dataStream.reset();
            bitmap.compress(compressFormat, 100, dataStream);
            bitmap.recycle();
            return dataStream.toByteArray();
        }
    }
}