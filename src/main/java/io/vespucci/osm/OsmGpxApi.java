package io.vespucci.osm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.contract.FileExtensions;
import io.vespucci.contract.MimeTypes;
import io.vespucci.gpx.Track;
import io.vespucci.net.ContentDispositionFileNameParser;
import io.vespucci.services.util.StreamUtils;
import io.vespucci.util.DateFormatter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

/**
 * Bindings to OSM GPX API 0.6
 * 
 * @author simon
 *
 */
public class OsmGpxApi {
    private static final String DEBUG_TAG = OsmGpxApi.class.getSimpleName().substring(0, Math.min(23, OsmGpxApi.class.getSimpleName().length()));

    private static final String CONTENT_DISPOSITION_HEADER = "content-disposition";

    private static final String FILE_KEY        = "file";
    private static final String TAGS_KEY        = "tags";
    private static final String VISIBILITY_KEY  = "visibility";
    private static final String DESCRIPTION_KEY = "description";

    /**
     * Date pattern used for suggesting a file name when uploading GPX tracks.
     */
    private static final String DATE_PATTERN_GPX_TRACK_UPLOAD_SUGGESTED_FILE_NAME_PART = "yyyy-MM-dd'T'HHmmss";

    /**
     * GPS track API visibility/
     */
    public enum Visibility {
        PRIVATE, PUBLIC, TRACKABLE, IDENTIFIABLE
    }

    /**
     * Upload a GPS track in GPX format
     * 
     * @param server the Server object for the API instance to use
     * @param track the track
     * @param description optional description
     * @param tags optional tags
     * @param visibility privacy/visibility setting
     * 
     * @throws IOException on an IO error
     */
    public static void uploadTrack(@NonNull Server server, @NonNull final Track track, @NonNull String description, @NonNull String tags,
            @NonNull Visibility visibility) throws IOException {
        RequestBody gpxBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse(MimeTypes.GPX);
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try {
                    track.exportToGPX(sink.outputStream());
                } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
                    throw new IOException(e);
                }
            }
        };
        String fileNamePart = DateFormatter.getFormattedString(DATE_PATTERN_GPX_TRACK_UPLOAD_SUGGESTED_FILE_NAME_PART);
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(DESCRIPTION_KEY, description)
                .addFormDataPart(TAGS_KEY, tags).addFormDataPart(VISIBILITY_KEY, visibility.name().toLowerCase(Locale.US))
                .addFormDataPart(FILE_KEY, fileNamePart + "." + FileExtensions.GPX, gpxBody).build();
        try (Response response = server.openConnectionForAuthenticatedAccess(getUploadTrackUrl(server), Server.HTTP_POST, requestBody)) {
            if (!response.isSuccessful()) {
                Server.throwOsmServerException(response);
            }
        }
    }

    /**
     * Download a track
     * 
     * We currently force gpx format download which will lead to multiple appended GPX tracks for multi-file archives
     * 
     * @param server the Server object for the API instance to use
     * @param id id of the track to download
     * @param destination director to write the file/dir to
     * @param name file name to use
     * @return a Uri pointing to the file or directory
     */
    @Nullable
    public static Uri downloadTrack(@NonNull Server server, long id, @NonNull String destination, @Nullable String name) {
        try (Response response = server.openConnectionForAuthenticatedAccess(getDownloadTrackUrl(server, id), Server.HTTP_GET, (RequestBody) null)) {
            Server.checkResponseCode(response);
            ResponseBody body = response.body();
            String fileName = name;
            if (fileName == null) {
                String contentDisposition = response.header(CONTENT_DISPOSITION_HEADER);
                if (contentDisposition != null) {
                    try {
                        fileName = ContentDispositionFileNameParser.parse(contentDisposition);
                    } catch (IllegalArgumentException e) {
                        Log.e(DEBUG_TAG, "Problem parsing header ", e);
                    }
                }
                if (fileName == null) {
                    fileName = Long.toString(System.currentTimeMillis());
                }
            }
            if (!fileName.endsWith(FileExtensions.GPX)) { // as we force GPX format, .gpx should be added if missing
                fileName = fileName + "." + FileExtensions.GPX;
            }
            File output = new File(destination, fileName);
            try (OutputStream fileStream = new FileOutputStream(output)) {
                StreamUtils.copy(body.byteStream(), fileStream);
            }
            return Uri.fromFile(output);
        } catch (IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "Problem downloading GPX track ", e);
        }
        return null;
    }

    /**
     * Get a list of GPX tracks for the user
     * 
     * @param server the Server object for the API instance to use
     * @param box if non-null filter the results with this BoundingBox
     * @return a list of GpxFile objects
     */
    @NonNull
    public static List<GpxFile> getUserGpxFiles(@NonNull Server server, @Nullable BoundingBox box) {
        try (Response response = server.openConnectionForAuthenticatedAccess(getUserGpxFilesUrl(server), Server.HTTP_GET, (RequestBody) null)) {
            Server.checkResponseCode(response);
            List<GpxFile> result = GpxFile.parse(response.body().byteStream());
            if (box != null) {
                List<GpxFile> temp = new ArrayList<>();
                for (GpxFile gpxFile : result) {
                    if (box.contains(gpxFile.getLon(), gpxFile.getLat())) {
                        temp.add(gpxFile);
                    }
                }
                result = temp;
            }
            return result;
        } catch (SAXException | ParserConfigurationException | IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "Problem downloading GPX track ", e);
        }
        return new ArrayList<>();
    }

    /**
     * Get the url for uploading a GPS track
     * 
     * @param server the Server object for the API instance to use
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private static URL getUserGpxFilesUrl(@NonNull Server server) throws MalformedURLException {
        return new URL(server.getReadWriteUrl() + "user/gpx_files");
    }

    /**
     * Get the url for uploading a GPS track
     * 
     * @param server the Server object for the API instance to use
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private static URL getUploadTrackUrl(@NonNull Server server) throws MalformedURLException {
        return new URL(server.getReadWriteUrl() + "gpx/create");
    }

    /**
     * Get the url for downloading a GPS track
     * 
     * @param server the Server object for the API instance to use
     * @param id the track id
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private static URL getDownloadTrackUrl(@NonNull Server server, long id) throws MalformedURLException {
        return new URL(server.getReadWriteUrl() + "gpx/" + Long.toString(id) + "/data.gpx");
    }
}
