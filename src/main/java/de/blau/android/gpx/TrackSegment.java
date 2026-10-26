package de.blau.android.gpx;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.util.GeoMath.OSM_SCALE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.micycle1.clipper2.core.Path64;
import com.github.micycle1.clipper2.core.Point64;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.BoundingBoxCoverage;
import de.blau.android.osm.GeoPoint;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;

public class TrackSegment implements GpxObject, BoundingBoxCoverage, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TrackSegment.class.getSimpleName().length());
    private static final String DEBUG_TAG = TrackSegment.class.getSimpleName().substring(0, TAG_LEN);

    private final List<TrackPoint> segment;
    private final int              start;
    private final int              end;

    /**
     * Create a new TrackSegment from a track
     * 
     * @param start start index of the segment
     * @param end last index of the segment, if < 0, the end of the segment or track
     * @param track the original GPX track
     */
    public TrackSegment(int start, int end, @NonNull Track track) {
        List<TrackPoint> trackPoints = track.getTrackPoints();
        this.start = start;
        if (end < 0) {
            int size = trackPoints.size();
            end = size;
            for (int i = start + 1; i < size; i++) { // start will have the new segment flag set
                if (trackPoints.get(i).isNewSegment()) {
                    end = i;
                    break;
                }
            }
        } else {
            end = end + 1;
        }
        this.end = end - 1;
        Log.d(DEBUG_TAG, "Creating new track segment from " + start + " to " + this.getEnd());
        segment = new ArrayList<>(trackPoints.subList(start, end));
    }

    /**
     * @return the track segment
     */
    public List<TrackPoint> getSegment() {
        return segment;
    }

    @Override
    @NonNull
    public List<BoundingBox> getCoverage(double bufferMeters, double minDimension, double maxDimension) {
        final int size = segment.size();
        if (size < 2) {
            return new ArrayList<>();
        }
        // create buffer area
        final Path64 path = new Path64();
        for (int i = 0; i < size; i++) {
            GeoPoint p = segment.get(i);
            // lon is already WGS84 E7
            path.add(new Point64(p.getLon(), Math.round(GeoMath.latE7ToMercator(p.getLat()) * OSM_SCALE)));
        }
        return Geometry.coverPath64(path, bufferMeters, minDimension, maxDimension, false);
    }

    /**
     * @return the end
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    @Override
    @NonNull
    public String getShortDescription(@NonNull Context ctx) {
        return ctx.getString(R.string.track_segment_description);
    }

}
