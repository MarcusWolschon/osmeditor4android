// Created by plusminus on 21:31:36 - 25.09.2008
package de.blau.android.services.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.geo.pmtiles.Constants;
import ch.poole.geo.pmtiles.Reader;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.exception.InvalidTileException;
import de.blau.android.layer.tiles.util.MapTileProvider;
import de.blau.android.layer.tiles.util.MapTileProviderCallback;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Header;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.OkHttpFileChannel;
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

    private static class ReaderCache<K, V> extends LinkedHashMap<K, V> { // NOSONAR
        private static final long serialVersionUID = 1L;

        private static final int DEFAULT_CACHE_SIZE = 5;

        private int cacheSize = DEFAULT_CACHE_SIZE;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (size() > cacheSize) {
                try {
                    ((AutoCloseable) eldest).close();
                    return true;
                } catch (Exception e) {
                    //
                }
            }
            return false;
        }
    }

    private static final String DEBUG_TAG = MapTileDownloader.class.getSimpleName().substring(0, Math.min(23, MapTileDownloader.class.getSimpleName().length()));

    public static final long TIMEOUT = 5000;

    private final Context                     mCtx;
    private final MapTileSaver                mapTileSaver;
    private final NetworkStatus               networkStatus;
    private final OkHttpClient                client;
    private final ReaderCache<String, Reader> pmtilesReaderCache = new ReaderCache<>();
    private final HashSet<String>             disabled           = new HashSet<>();

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

    @Override
    protected Runnable getTileLoader(MapTile aTile, MapTileProviderCallback aCallback) {
        return new TileLoader(aTile, aCallback);
    }

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
            final String sourceId = mTile.rendererID;
            try {
                if (!networkStatus.isConnected()) { // fail immediately
                    mCallback.mapTileFailed(sourceId, mTile.zoomLevel, mTile.x, mTile.y, NONETWORK, null);
                    return;
                }
                TileLayerSource source = TileLayerSource.get(mCtx, sourceId, false);
                if (source != null && !disabled.contains(sourceId)) {
                    try {
                        byte[] data = TileLayerSource.TYPE_PMT_3.equals(source.getType()) ? downloadPMTiles(source, mTile) : downloadTile(source, mTile);
                        mCallback.mapTileLoaded(mTile.rendererID, mTile.zoomLevel, mTile.x, mTile.y, data);
                        mapTileSaver.saveTile(mTile, data);
                    } catch (FileNotFoundException | InvalidTileException ex) {
                        mapTileSaver.markAsInvalid(mTile);
                        mCallback.mapTileFailed(sourceId, mTile.zoomLevel, mTile.x, mTile.y, DOESNOTEXIST, ex.getMessage());
                    } catch (IOException ioe) {
                        // FileNotFound is an expected exception, any other IOException is an error
                        mCallback.mapTileFailed(sourceId, mTile.zoomLevel, mTile.x, mTile.y, IOERR, ioe.getMessage());
                    }
                }
            } catch (NullPointerException | IllegalArgumentException | IOException | UnsupportedOperationException e) {
                Log.e(DEBUG_TAG, "Error for MapTile, disabling source: " + sourceId + " exception: " + e);
                // disable source temporarily to avoid hitting source time and time again
                MapTileFilesystemProvider.displayError(mCtx, disabled, sourceId, R.string.toast_tile_source_issue, e);
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
         * Download a tile from a PMTiles source
         * 
         * @param source the TileLayerSource
         * @param mTile the tile
         * @throws IOException if something goes wrong downloading
         */
        private byte[] downloadPMTiles(@NonNull TileLayerSource source, @NonNull MapTile mTile) throws IOException {
            Reader reader = pmtilesReaderCache.get(mTile.rendererID);
            if (reader == null) {
                synchronized (pmtilesReaderCache) {
                    // re-check to avoid race condition
                    reader = pmtilesReaderCache.get(mTile.rendererID);
                    if (reader == null) {
                        reader = new Reader(new OkHttpFileChannel(client, source));
                        pmtilesReaderCache.put(mTile.rendererID, reader);
                    }
                }
            }
            byte[] data = reader.getTile(mTile.zoomLevel, mTile.x, mTile.y);
            if (data != null) {
                byte method = reader.getTileCompression();
                if (Constants.COMPRESSION_NONE != method) {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
                        byte[] temp = new byte[1024];
                        InputStream is = getDecompressStream(method, bis);
                        int len;
                        while ((len = is.read(temp)) != -1) {
                            bos.write(temp, 0, len);
                        }
                        data = bos.toByteArray();
                    }
                }
                return data;
            }
            throw new FileNotFoundException(mCtx.getString(R.string.empty_tile));
        }

        /**
         * Get a stream that will de-compress the input
         * 
         * @param bis the input
         * @return an appropriate InputStream
         * @throws IOException if we can't create the stream
         */
        @NonNull
        private InputStream getDecompressStream(byte method, @NonNull ByteArrayInputStream bis) throws IOException {
            switch (method) {
            case Constants.COMPRESSION_GZIP:
                return new GZIPInputStream(bis);
            case Constants.COMPRESSION_ZSTD:
                return new InflaterInputStream(bis);
            default:
                throw new UnsupportedOperationException("Unsupported compression " + method);
            }
        }

        /**
         * Download a tile from a tiles/WMS server
         * 
         * @param source the TileLayerSource
         * @param mTile the tile
         * @throws FileNotFoundException tile not found
         * @throws IOException if something goes wrong downloading
         * @throws InvalidTileException invalid tile
         */
        private byte[] downloadTile(@NonNull TileLayerSource source, @NonNull MapTile mTile) throws IOException {
            final String tileURLString = buildURL(source, mTile);
            Builder builder = new Request.Builder().url(tileURLString);
            addCustomHeaders(source, builder);
            Request request = builder.addHeader(HTTP_HEADER_ACCEPT_ENCODING, GZIP).build();
            Call tileCall = client.newCall(request);
            try (Response tileCallResponse = tileCall.execute()) {
                final ResponseBody responseBody = tileCallResponse.body();
                final MediaType format = responseBody.contentType();
                if (tileCallResponse.isSuccessful()) {
                    InputStream inputStream = responseBody.byteStream();
                    String noTileHeader = source.getNoTileHeader();
                    if (noTileHeader != null) {
                        String headerValue = tileCallResponse.header(noTileHeader);
                        if (headerValue != null) {
                            String[] noTileValues = source.getNoTileValues();
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
                                throw new FileNotFoundException(mCtx.getString(R.string.tile_error_message, tileURLString, responseBody.string()));
                            case MimeTypes.APPLICATION_TYPE: // WMS errors, MVT tiles
                                switch (format.subtype().toLowerCase()) {
                                case MimeTypes.WMS_EXCEPTION_XML_SUBTYPE:
                                case MimeTypes.JSON_SUBTYPE:
                                    throw new FileNotFoundException(mCtx.getString(R.string.tile_error_message, tileURLString, responseBody.string()));
                                case MimeTypes.MVT_SUBTYPE:
                                case MimeTypes.X_PROTOBUF_SUBTYPE:
                                    byte[] noTileTile = source.getNoTileTile();
                                    if (noTileTile != null && data.length == noTileTile.length && Arrays.equals(data, noTileTile)) {
                                        throw new FileNotFoundException(mCtx.getString(R.string.no_tile_mvt_tile, tileURLString));
                                    }
                                    break;
                                default:
                                    throw new InvalidTileException(mCtx.getString(R.string.unexpected_tile_format_subtype, format.subtype(), tileURLString));
                                }
                                break;
                            default:
                                throw new InvalidTileException(mCtx.getString(R.string.unexpected_tile_format, format, tileURLString));
                            }
                        }
                        return data;
                    }
                } else {
                    int code = tileCallResponse.code();
                    Charset charset = format != null && format.charset() != null ? format.charset() : Charset.defaultCharset();
                    String message = mCtx.getString(R.string.tile_error, code, new String(MapTileProvider.unGZip(responseBody.bytes()), charset));
                    if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new FileNotFoundException(message);
                    } else {
                        throw new IOException(message);
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

    /**
     * Get the url for a tile
     * 
     * @param renderer a TileLayerServer instance
     * @param tile the tile
     * @return an url as a String
     * @throws IOException if the source metadata is not available
     */
    @NonNull
    public static String buildURL(@NonNull TileLayerSource renderer, @NonNull final MapTile tile) throws IOException {
        if (!renderer.isMetadataLoaded()) {
            throw new IOException("Metadata not loaded");
        }
        return renderer.getTileURLString(tile);
    }

    /**
     * Remove a source from the disabled set
     * 
     * @param sourceId the source to remove, if null all will be removed.
     */
    public void flushDisabled(@Nullable String sourceId) {
        if (sourceId == null) {
            disabled.clear();
            return;
        }
        disabled.remove(sourceId);
    }
}