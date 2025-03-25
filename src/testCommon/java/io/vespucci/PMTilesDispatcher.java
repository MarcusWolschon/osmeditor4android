package io.vespucci;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

public class PMTilesDispatcher extends Dispatcher {
    private static final String DEBUG_TAG = PMTilesDispatcher.class.getSimpleName().substring(0, Math.min(23, PMTilesDispatcher.class.getSimpleName().length()));

    private static final String RANGE_HEADER = "Range";
    private static final String ETAG_HEADER  = "ETag";

    private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=([0-9]+)-([0-9]+)");

    private final FileChannel channel;

    /**
     * Construct a new dispatcher that will return tiles from a PMTiles source
     * 
     * @param context an Android Context
     * @param source the name of the source
     * @throws IOException if the source can't copied out of the assets and opened
     */
    public PMTilesDispatcher(@NonNull Context context, @NonNull String source) throws IOException {
        Log.d(DEBUG_TAG, "creating new dispatcher for " + source);
        try {
            File destinationDir = ContextCompat.getExternalCacheDirs(context)[0];
            File file = new File(destinationDir, source);
            JavaResources.copyFileFromResources(source, null, file);
            if (!file.exists()) {
                throw new IOException(file.getAbsolutePath() + " doesn't exist");
            }
            channel = new FileInputStream(file).getChannel();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "file " + source + " not found " + e.getMessage());
            throw e;
        }
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        Log.d(DEBUG_TAG, "request " + request);
        try (Buffer data = new Buffer()) {
            Matcher matcher = RANGE_PATTERN.matcher(request.getHeader(RANGE_HEADER));
            if (matcher.find()) {
                long start = Long.parseLong(matcher.group(1));
                long end = Long.parseLong(matcher.group(2));
                ByteBuffer buffer = ByteBuffer.allocate((int) (end - start + 1));
                channel.read(buffer, start);
                if (buffer.capacity() != 0) {
                    data.write(buffer.array());
                    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(data);
                }
            }
            Log.e(DEBUG_TAG, "no range header found");
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        } catch (IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "dispatch failed for " + request + " " + e.getMessage());
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    @Override
    public void shutdown() {
        Log.d(DEBUG_TAG, "shutting down dispatcher");
        try {
            channel.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}
