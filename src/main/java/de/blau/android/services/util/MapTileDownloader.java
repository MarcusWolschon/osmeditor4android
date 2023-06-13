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
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.exception.InvalidTileException;
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

    private static final String DEBUG_TAG = MapTileDownloader.class.getSimpleName();

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

        /**
         * Construct a new TileLoader
         * 
         * @param aTile the tile to download
         * @param aCallback the callback to call when finished
         */
        public TileLoader(@NonNull final MapTile aTile, @NonNull final MapTileProviderCallback aCallback) {
            super(aTile, aCallback);
        }

        @Override
        public void run() {
            try {
                if (!networkStatus.isConnected()) { // fail immediately
                    mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, NONETWORK, null);
                    return;
                }
                TileLayerSource renderer = TileLayerSource.get(mCtx, mTile.rendererID, false);
                if (renderer != null) {
                    final String tileURLString = buildURL(renderer, mTile);
                    if (tileURLString.length() > 0) {
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
                                                        throw new FileNotFoundException(mCtx.getString(R.string.no_tile_header, v));
                                                    }
                                                }
                                            } else {
                                                throw new FileNotFoundException(mCtx.getString(R.string.no_tile_header, headerValue));
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
                                            throw new FileNotFoundException(mCtx.getString(R.string.empty_tile));
                                        }
                                        // check format
                                        if (format != null) {
                                            String formatType = format.type().toLowerCase(Locale.US);
                                            switch (formatType) {
                                            case MimeTypes.IMAGE_TYPE:
                                                if (MimeTypes.BMP_SUBTYPE.equalsIgnoreCase(format.subtype())) {
                                                    // if tile is in BMP format, compress
                                                    data = compressBitmap(CompressFormat.PNG, dataStream, data);
                                                }
                                                break;
                                            case MimeTypes.TEXT_TYPE:
                                                // this can't be a tile and is likely an error message
                                                throw new FileNotFoundException(
                                                        mCtx.getString(R.string.tile_error_message, tileURLString, responseBody.string()));
                                            case MimeTypes.APPLICATION_TYPE: // WMS errors, MVT tiles
                                                switch (format.subtype().toLowerCase()) {
                                                case MimeTypes.WMS_EXCEPTION_XML_SUBTYPE:
                                                case MimeTypes.JSON_SUBTYPE:
                                                    throw new FileNotFoundException(
                                                            mCtx.getString(R.string.tile_error_message, tileURLString, responseBody.string()));
                                                case MimeTypes.MVT_SUBTYPE:
                                                case MimeTypes.X_PROTOBUF_SUBTYPE:
                                                    byte[] noTileTile = renderer.getNoTileTile();
                                                    if (noTileTile != null && data.length == noTileTile.length && Arrays.equals(data, noTileTile)) {
                                                        throw new FileNotFoundException(mCtx.getString(R.string.no_tile_mvt_tile, tileURLString));
                                                    }
                                                    break;
                                                default:
                                                    throw new InvalidTileException(
                                                            mCtx.getString(R.string.unexpected_tile_format_subtype, format.subtype(), tileURLString));
                                                }
                                                break;
                                            default:
                                                throw new InvalidTileException(mCtx.getString(R.string.unexpected_tile_format, format, tileURLString));
                                            }
                                        }
                                        mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                                        mapTileSaver.saveFile(mTile, data);
                                    }
                                } else {
                                    int code = tileCallResponse.code();
                                    String message = mCtx.getString(R.string.tile_error, code, tileCallResponse.body().string());
                                    if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                                        throw new FileNotFoundException(message);
                                    } else {
                                        throw new IOException(message);
                                    }
                                }
                            }
                        } catch (FileNotFoundException | InvalidTileException ex) {
                            mapTileSaver.markAsInvalid(mTile);
                            mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, DOESNOTEXIST, ex.getMessage());
                        } catch (IOException ioe) {
                            // FileNotFound is an expected exception, any other IOException is an error
                            mCallback.mapTileFailed(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, IOERR, ioe.getMessage());
                        }
                    }
                }
            } catch (NullPointerException | IllegalArgumentException | IOException e) {
                Log.e(DEBUG_TAG, "Error calling callbacks for MapTile. Exception: " + e);
            } finally {
                /*
                 * What to do when downloading tile caused an error? Also remove it from the mPending? Not doing so
                 * blocks it for the whole existence of this TileDownloader. -> we remove it and the application has to
                 * re-request it.
                 */
                finished();
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

    /**
     * Get the url for a tile
     * 
     * @param renderer a TileLayerServer instance
     * @param tile the tile
     * @return an url as a String
     */
    @NonNull
    public static String buildURL(@NonNull TileLayerSource renderer, @NonNull final MapTile tile) {
        return renderer.isMetadataLoaded() ? renderer.getTileURLString(tile) : "";
    }
}