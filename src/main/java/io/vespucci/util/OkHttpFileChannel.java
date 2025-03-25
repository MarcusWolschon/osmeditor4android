package io.vespucci.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import ch.poole.geo.pmtiles.SourceChangedException;
import ch.poole.geo.pmtiles.UrlFileChannel;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.resources.TileLayerSource.Header;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OkHttp based read-only FileChannel
 * 
 * @author simon
 *
 */
public class OkHttpFileChannel extends UrlFileChannel {

    private static final String DEBUG_TAG = OkHttpFileChannel.class.getSimpleName().substring(0, Math.min(23, OkHttpFileChannel.class.getSimpleName().length()));

    private final URL          url;
    private final OkHttpClient client;
    private final List<Header> headers;
    private String             savedETag = null;

    /**
     * Construct a (more or less fake) FileChannel for connecting to a remote tile source
     * 
     * @param client an OkHttpClient instant
     * @param tileLayerSource the source
     * @throws MalformedURLException if the URL couldn't be parsed
     */
    public OkHttpFileChannel(@NonNull OkHttpClient client, @NonNull TileLayerSource tileLayerSource) throws MalformedURLException {
        Log.d(DEBUG_TAG, "Creating channel for " + tileLayerSource.getName());
        this.client = client;
        this.url = new URL(tileLayerSource.getTileUrl());
        headers = tileLayerSource.getHeaders();
    }

    /**
     * Construct a (more or less fake) FileChannel for connecting to a remote tile source
     * 
     * @param client an OkHttpClient instant
     * @param url the URL
     * @throws MalformedURLException if the URL couldn't be parsed
     */
    public OkHttpFileChannel(@NonNull OkHttpClient client, @NonNull String url) throws MalformedURLException {
        this.client = client;
        this.url = new URL(url);
        headers = null;
    }

    @Override
    public int read(ByteBuffer dst, long pos) throws IOException {
        Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            for (Header h : headers) {
                builder.addHeader(h.getName(), h.getValue());
            }
        }
        final int capacity = dst.capacity();
        Request request = builder.addHeader(RANGE_HEADER, "bytes=" + pos + "-" + (pos + capacity - 1)).build();
        Call tileCall = client.newCall(request);
        try (Response tileCallResponse = tileCall.execute()) {
            if (tileCallResponse.isSuccessful()) {
                String eTag = tileCallResponse.header(ETAG_HEADER);
                if (eTag != null) {
                    if (savedETag != null && !eTag.equals(savedETag)) {
                        savedETag = eTag;
                        throw new SourceChangedException();
                    }
                    savedETag = eTag;
                }
                ResponseBody responseBody = tileCallResponse.body();
                try (final InputStream is = responseBody.byteStream()) {
                    dst.rewind();
                    int offset = 0;
                    int count = 0;
                    int remaining = capacity;
                    while (remaining > 0 && (count = is.read(dst.array(), offset, remaining)) != -1) {
                        remaining -= count;
                        offset += count;
                    }
                    return capacity - remaining;
                }
            }
            throw new IOException("Error reading from connection " + tileCallResponse.code());
        }
    }
}
