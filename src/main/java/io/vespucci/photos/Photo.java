package io.vespucci.photos;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import io.vespucci.R;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.GeoPoint;
import io.vespucci.util.Util;
import io.vespucci.util.rtree.BoundedObject;

/**
 * a photo somewhere on the device or possibly on the network exif accessing code from
 * http://www.blog.nathanhaze.com/how-to-get-exif-tags-gps-coordinates-time-date-from-a-picture/
 */
public class Photo implements BoundedObject, GeoPoint, Serializable {

    private static final long serialVersionUID = 2L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Photo.class.getSimpleName().length());
    private static final String DEBUG_TAG = Photo.class.getSimpleName().substring(0, TAG_LEN);

    /** a name for display purposes NOTE this ignored for equals and hashCode */
    private final String displayName;
    /** an URI or path to the image */
    private final String ref;
    /** Latitude *1E7. */
    private final int    lat;
    /** Longitude *1E7. */
    private final int    lon;
    /**
     * compass direction
     */
    private int          direction    = 0;
    private String       directionRef = null; // if null direction not present

    private int orientation = ExifInterface.ORIENTATION_UNDEFINED;

    private Long   captureDate = null;
    private String creator     = null;

    /**
     * Construct a Photo object from an Uri
     * 
     * @param context Android context
     * @param uri the Uri
     * @param displayName a name of the image for display purposes
     * @throws IOException If there was a problem parsing the XML.
     * @throws NumberFormatException If there was a problem parsing the XML.
     */
    public Photo(@NonNull Context context, @NonNull Uri uri, @Nullable String displayName) throws IOException, NumberFormatException {
        this(new ExifInterface(openInputStream(context, uri)), uri.toString(), displayName);
    }

    /**
     * Get an InputStream from an uri
     * 
     * @param context Android context
     * @param uri a content or file uri
     * @return an InputStream
     * @throws IOException if anything goes wrong
     */
    @NonNull
    private static InputStream openInputStream(@NonNull Context context, @NonNull Uri uri) throws IOException {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (Exception ex) {
            // other stuff broken ... for example ArrayIndexOutOfBounds
            throw new IOException(ex.getMessage());
        } catch (Error err) { // NOSONAR crashing is not an option
            // other stuff broken ... for example NoSuchMethodError
            throw new IOException(err.getMessage());
        }
    }

    /**
     * Construct a Photo object from a directory and filename of the image
     * 
     * @param directory the directory the image is located in
     * @param imageFile the image file
     * @throws IOException If there was a problem parsing the XML.
     * @throws NumberFormatException If there was a problem parsing the XML.
     */
    public Photo(@NonNull File directory, @NonNull File imageFile) throws IOException, NumberFormatException {
        this(new ExifInterface(imageFile.toString()), imageFile.getAbsolutePath(), imageFile.getName());
    }

    /**
     * Construct a Photo object from the Exif information and a String reference (either a filename or an Uri)
     * 
     * @param exif the Exif information
     * @param ref the Uri to the image of a file path
     * @param displayName a name of the image for display purposes
     * @throws IOException if location information is missing
     */
    private Photo(@NonNull ExifInterface exif, @NonNull String ref, @Nullable String displayName) throws IOException {
        this.ref = ref;
        this.displayName = displayName;
        /**
         * get the attribute. rest of the attributes are the same. will add convertToDegree on the bottom (not required)
         **/
        String lonStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        if (lonStr == null) {
            throw new IOException("No EXIF longitude tag");
        }
        float lonf = convertToDegree(lonStr);

        String lonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        if (lonRef != null && !ExifInterface.LONGITUDE_EAST.equals(lonRef)) { // deal with the negative degrees
            lonf = -lonf;
        }

        float latf = convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));

        String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        if (latRef != null && !ExifInterface.LATITUDE_NORTH.equals(latRef)) {
            latf = -latf;
        }
        if (!(Util.notZero(lonf) && Util.notZero(latf))) {
            throw new IOException("Lat and lon are zero");
        }

        lat = (int) (latf * 1E7d);
        lon = (int) (lonf * 1E7d);

        String orientationStr = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        if (orientationStr != null) {
            try {
                orientation = Integer.parseInt(orientationStr);
            } catch (NumberFormatException nfex) {
                Log.w(DEBUG_TAG, "Unable to parse orientation " + nfex.getMessage());
            }
        }

        captureDate = exif.getDateTime();
        creator = exif.getAttribute(ExifInterface.TAG_ARTIST);

        // don't add anything after this section that needs to be processed in any case
        String dir = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION);
        if (dir != null) {
            String[] r = dir.split("/");
            if (r.length != 2) {
                return;
            }
            try {
                direction = (int) (Double.valueOf(r[0]) / Double.valueOf(r[1]));
                directionRef = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF);
            } catch (NumberFormatException nfex) {
                Log.w(DEBUG_TAG, "Unable to parse cardinal direction " + nfex.getMessage());
            }
        }
    }

    /**
     * Construct a Photo object from coordinates and the path to the file
     * 
     * @param lat latitude in WGS84*1E7 degrees
     * @param lon longitude in WGS84*1E7 degrees
     * @param ref the path of the file
     * @param displayName a short name for the Photo
     * @param orientation image orientation
     */
    public Photo(int lat, int lon, @NonNull String ref, @Nullable String displayName, int orientation) {
        this(lat, lon, 0, ref, displayName, orientation);
        this.directionRef = null;
    }

    /**
     * Construct a Photo object from coordinates, the path to the file and the direction
     * 
     * @param lat latitude in WGS84*1E7 degrees
     * @param lon longitude in WGS84*1E7 degrees
     * @param direction in degrees
     * @param ref the path of the file
     * @param displayName a short name for the Photo
     * @param orientation image orientation
     */
    public Photo(int lat, int lon, int direction, @NonNull String ref, @Nullable String displayName, int orientation) {
        this.lat = lat;
        this.lon = lon;
        this.direction = direction;
        this.directionRef = ExifInterface.GPS_DIRECTION_MAGNETIC;
        this.ref = ref;
        this.displayName = displayName;
        this.orientation = orientation;
    }

    /**
     * Convert the DMS string to degrees
     * 
     * @param stringDMS the DMS string
     * @return degrees
     * @throws NumberFormatException if the String couldn't be parsed for whatever reason
     */
    private Float convertToDegree(@NonNull String stringDMS) throws NumberFormatException {
        try {
            Float result = null;
            String[] dms = stringDMS.split(",", 3);

            String[] stringD = dms[0].split("/", 2);
            Double d0 = Double.valueOf(stringD[0]);
            Double d1 = Double.valueOf(stringD[1]);
            Double d = d0 / d1;

            String[] stringM = dms[1].split("/", 2);
            Double m0 = Double.valueOf(stringM[0]);
            Double m1 = Double.valueOf(stringM[1]);
            Double m = m0 / m1;

            String[] stringS = dms[2].split("/", 2);
            Double s0 = Double.valueOf(stringS[0]);
            Double s1 = Double.valueOf(stringS[1]);
            Double s = s0 / s1;

            result = (float) (d + (m / 60) + (s / 3600));

            return result;
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "couldn't parse >" + stringDMS + "< exception " + ex);
            throw new NumberFormatException("couldn't parse: " + stringDMS);
        }
    }

    /**
     * Get the latitude of the photo.
     * 
     * @return The latitude *1E7.
     */
    @Override
    public int getLat() {
        return lat;
    }

    /**
     * Get the longitude of the photo.
     * 
     * @return The longitude *1E7.
     */
    @Override
    public int getLon() {
        return lon;
    }

    /**
     * Get an content Uri for the photo
     * 
     * @param context Android context
     * @return ref as content Uri
     */
    @Nullable
    public Uri getRefUri(@NonNull Context context) {
        try {
            Log.d(DEBUG_TAG, "getRef ref is " + ref);
            if (ref.startsWith("content:")) {
                return Uri.parse(ref);
            }
            return FileProvider.getUriForFile(context, context.getString(R.string.content_provider), new File(ref));
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "getRef Problem with Uri for ref " + ref + " " + ex);
            return null;
        }
    }

    /**
     * 
     * @return ref as String
     */
    public String getRef() {
        return ref;
    }

    /**
     * Get the display name for this image
     * 
     * @return a name for human consumption
     */
    @Nullable
    public String getDisplayName() {
        return displayName != null && !"".equals(displayName) ? displayName : Uri.parse(ref).getLastPathSegment();
    }

    /**
     * Do we have a direction attribute
     * 
     * @return true if we have a direction attribute
     */
    public boolean hasDirection() {
        return directionRef != null;
    }

    /**
     * get the direction value
     * 
     * @return the direction value
     */
    public int getDirection() {
        return direction;
    }

    /**
     * get the orientation EXIF value
     * 
     * @return the orientation EXIF value
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * @return the capture date
     */
    @Nullable
    public Long getCaptureDate() {
        return captureDate;
    }

    /**
     * @return the creator of the image
     */
    @Nullable
    public String getCreator() {
        return creator;
    }

    /**
     * For the BoundedObject interface
     */
    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(lon, lat);
    }

    @NonNull
    @Override
    public BoundingBox getBounds(@NonNull BoundingBox result) {
        result.resetTo(lon, lat);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon, ref);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Photo)) {
            return false;
        }
        Photo other = (Photo) obj;
        return lat == other.lat && lon == other.lon && Objects.equals(ref, other.ref);
    }
}
