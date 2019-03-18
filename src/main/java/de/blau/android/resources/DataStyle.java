package de.blau.android.resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StyleableFeature;
import de.blau.android.osm.Way;
import de.blau.android.util.Density;
import de.blau.android.util.Snack;
import de.blau.android.util.Version;

public final class DataStyle extends DefaultHandler {

    private static final String DEBUG_TAG = "DataStyle";

    private static final Version CURRENT_VERSION = new Version("0.2.0");

    private static final String FILE_PATH_STYLE_SUFFIX = "-profile.xml";

    // constants for the internal profiles
    public static final String GPS_TRACK                     = "gps_track";
    public static final String INFOTEXT                      = "infotext";
    public static final String ATTRIBUTION_TEXT              = "attribution_text";
    public static final String VIEWBOX                       = "viewbox";
    public static final String WAY_TOLERANCE                 = "way_tolerance";
    public static final String WAY_TOLERANCE_2               = "way_tolerance_2";
    public static final String WAY                           = "way";
    public static final String SELECTED_WAY                  = "selected_way";
    public static final String SELECTED_RELATION_WAY         = "selected_relation_way";
    public static final String PROBLEM_WAY                   = "problem_way";
    public static final String HIDDEN_WAY                    = "hidden_way";
    public static final String NODE_TOLERANCE                = "node_tolerance";
    public static final String NODE_TOLERANCE_2              = "node_tolerance_2";
    public static final String NODE                          = "node";
    public static final String NODE_THIN                     = "node_thin";
    public static final String NODE_TAGGED                   = "node_tagged";
    public static final String NODE_DRAG_RADIUS              = "node_drag_radius";
    public static final String PROBLEM_NODE                  = "problem_node";
    public static final String PROBLEM_NODE_THIN             = "problem_node_thin";
    public static final String PROBLEM_NODE_TAGGED           = "problem_node_tagged";
    public static final String SELECTED_NODE                 = "selected_node";
    public static final String SELECTED_NODE_THIN            = "selected_node_thin";
    public static final String SELECTED_NODE_TAGGED          = "selected_node_tagged";
    public static final String SELECTED_RELATION_NODE        = "selected_relation_node";
    public static final String SELECTED_RELATION_NODE_THIN   = "selected_relation_node_thin";
    public static final String SELECTED_RELATION_NODE_TAGGED = "selected_relation_node_tagged";
    public static final String HIDDEN_NODE                   = "hidden_node";
    public static final String WAY_DIRECTION                 = "way_direction";
    public static final String LARGE_DRAG_AREA               = "large_drag_area";
    public static final String MARKER_SCALE                  = "marker_scale";
    public static final String GPS_POS                       = "gps_pos";
    public static final String GPS_POS_FOLLOW                = "gps_pos_follow";
    public static final String GPS_ACCURACY                  = "gps_accuracy";
    public static final String OPEN_NOTE                     = "open_note";
    public static final String CLOSED_NOTE                   = "closed_note";
    public static final String CROSSHAIRS                    = "crosshairs";
    public static final String CROSSHAIRS_HALO               = "crosshairs_halo";
    public static final String HANDLE                        = "handle";
    public static final String LABELTEXT                     = "labeltext";
    public static final String LABELTEXT_NORMAL              = "labeltext_normal";
    public static final String LABELTEXT_SMALL               = "labeltext_small";
    public static final String LABELTEXT_NORMAL_SELECTED     = "labeltext_normal_selected";
    public static final String LABELTEXT_SMALL_SELECTED      = "labeltext_small_selected";
    public static final String LABELTEXT_NORMAL_PROBLEM      = "labeltext_normal_problem";
    public static final String LABELTEXT_SMALL_PROBLEM       = "labeltext_small_problem";
    public static final String LABELTEXT_BACKGROUND          = "labeltext_background";
    public static final String GEOJSON_DEFAULT               = "geojson_default";
    public static final String DONTRENDER_WAY                = "dontrender_way";
    public static final String MIN_HANDLE_LEN                = "min_handle_length";
    public static final String ICON_ZOOM_LIMIT               = "icon_zoom_limit";

    private static final int DEFAULT_MIN_VISIBLE_ZOOM = 15;

    public class FeatureStyle {

        final Map<String, String> tags;
        private int               minVisibleZoom = DEFAULT_MIN_VISIBLE_ZOOM;
        private boolean           area           = false;
        boolean                   dontrender     = false;
        boolean                   updateWidth    = true;
        final Paint               paint;
        float                     widthFactor;
        DashPath                  dashPath       = null;
        private FontMetrics       fontMetrics    = null;
        private PathPattern       pathPattern    = null;
        private FeatureStyle      arrowStyle     = null;
        private FeatureStyle      casingStyle    = null;
        private boolean           oneway         = false;

        List<FeatureStyle> cascadedStyles = null;

        class DashPath {
            float[] intervals;
            float   phase;
        }

        /**
         * Construct a new style from tags and a Paint object
         * 
         * @param tagString a String containing the tags in k=v notation an separated by a |
         * @param p the Paint object, if null a new Paint object will be allocated
         */
        FeatureStyle(@NonNull String tagString, @Nullable Paint p) {
            tags = new HashMap<>();
            String[] tagsList = tagString.split("\\|");
            for (String t : tagsList) {
                String[] kv = t.split("=");
                if (kv.length == 2) {
                    tags.put(kv[0], kv[1]);
                } else if (kv.length == 1) {
                    tags.put(kv[0], "*");
                }
            }
            setArea(false);
            dontrender = false;
            updateWidth = true;
            if (p != null) {
                paint = new Paint(p);
            } else {
                paint = new Paint();
            }
            widthFactor = 1.0f;
        }

        /**
         * Construct a new style from tags, allocates a new Paint object
         * 
         * @param tags a String containing the tags in k=v notation an separated by a |
         */

        FeatureStyle(@NonNull String tags) {
            this(tags, (Paint) null);
        }

        /**
         * Construct a new FestureStyle with tags set to tags and the other values, with the exception of
         * cascadedStyles, set from the provided style
         * 
         * @param tags the tags
         * @param fp the style to copy
         */
        FeatureStyle(@NonNull String tags, @NonNull FeatureStyle fp) {
            this(tags, new Paint(fp.getPaint()));
            minVisibleZoom = fp.minVisibleZoom;
            setArea(fp.isArea());
            dontrender = fp.dontrender;
            updateWidth = fp.updateWidth;
            widthFactor = fp.widthFactor;
            if (fp.dashPath != null) {
                dashPath = new DashPath();
                dashPath.intervals = fp.dashPath.intervals.clone();
                dashPath.phase = fp.dashPath.phase;
            }
            fontMetrics = fp.fontMetrics;
            setPathPattern(fp.pathPattern);
            arrowStyle = fp.arrowStyle;
            casingStyle = fp.casingStyle;
            oneway = fp.oneway;
            cascadedStyles = null;
        }

        /**
         * Add a style to the list of cascaded styles
         * 
         * @param style the style to add
         */
        public void addStyle(@NonNull FeatureStyle style) {
            if (cascadedStyles == null) {
                cascadedStyles = new ArrayList<>();
            }
            cascadedStyles.add(style);
        }

        /**
         * Get the Paint object for this style
         * 
         * @return a Paint object
         */
        @NonNull
        public Paint getPaint() {
            return paint;
        }

        /**
         * Set the colour for this style
         * 
         * @param c the colour to set
         */
        public void setColor(int c) {
            paint.setColor(c);
        }

        /**
         * Check if we should use area rendering
         * 
         * @return true if we should use area rendering
         */
        public boolean isArea() {
            return area;
        }

        /**
         * Set the flag for area rendering
         * 
         * @param area the boolean value we want to use
         */
        public void setArea(boolean area) {
            this.area = area;
        }

        /**
         * Set the relative stroke width
         * 
         * @param f the value to set
         */
        public void setWidthFactor(float f) {
            widthFactor = f;
        }

        /**
         * Get the relative stroke width
         * 
         * @return the relative stroke width
         */
        public float getWidthFactor() {
            return widthFactor;
        }

        /**
         * Set the update width flag
         * 
         * @param update value to set it to
         */
        public void setUpdateWidth(boolean update) {
            updateWidth = update;
        }

        /**
         * Check if we should update the stroke width on zoom level changes
         * 
         * @return true if we should update
         */
        public boolean updateWidth() {
            return updateWidth;
        }

        /**
         * Set the stroke width for this style Updates everything that is dependent on it
         * 
         * @param width the new width to set
         */
        public void setStrokeWidth(float width) {
            float newWidth = width * widthFactor;
            paint.setStrokeWidth(newWidth);
            if (dashPath != null) {
                float[] intervals = dashPath.intervals.clone();
                for (int i = 0; i < intervals.length; i++) {
                    intervals[i] = dashPath.intervals[i] * newWidth;
                }
                DashPathEffect dp = new DashPathEffect(intervals, dashPath.phase);
                paint.setPathEffect(dp);
            } else if (pathPattern != null) {
                getPaint().setPathEffect(new PathDashPathEffect(pathPattern.draw(newWidth), pathPattern.advance(newWidth), 0f, pathPattern.style()));
            }
        }

        /**
         * Get the DashPath for this style
         * 
         * @return a DashPath or null if none set
         */
        @Nullable
        public DashPath getDashPath() {
            return dashPath;
        }

        /**
         * Create DashPath object and set it for this style
         * 
         * @param intervals an array of float containing the lengths of the dashes
         * @param phase a phase value
         */
        public void setDashPath(@NonNull float[] intervals, float phase) {
            dashPath = new DashPath();
            dashPath.intervals = intervals;
            dashPath.phase = phase;
        }

        /**
         * Check if we should render the object
         * 
         * @return true if we shouldn't render
         */
        public boolean dontRender() {
            return dontrender;
        }

        /**
         * Set if we shouldn't render this object
         * 
         * @param dr if true this object will not be rendered
         */
        public void setDontRender(boolean dr) {
            dontrender = dr;
        }

        /**
         * Get the FontMetrics for this style
         * 
         * @return a FontMetrics objecz
         */
        @NonNull
        public FontMetrics getFontMetrics() {
            if (fontMetrics == null) {
                fontMetrics = paint.getFontMetrics();
            }
            return fontMetrics;
        }

        /**
         * Set a pattern to be stamped along the path
         * 
         * @param pathPattern the pattern
         */
        void setPathPattern(@Nullable PathPattern pathPattern) {
            if (pathPattern != null) {
                this.pathPattern = pathPattern;
                float width = getPaint().getStrokeWidth();
                getPaint().setPathEffect(new PathDashPathEffect(pathPattern.draw(width), pathPattern.advance(width), 0f, pathPattern.style()));
            }
        }

        /**
         * Match the provided tags with those in the style
         * 
         * @param elementTags the provided tags
         * @return true if all tags are present in element tags
         */
        public boolean match(@NonNull SortedMap<String, String> elementTags) {
            for (Entry<String, String> tag : tags.entrySet()) {
                String v = elementTags.get(tag.getKey());
                if (v == null) {
                    return false;
                }
                String tagValue = tag.getValue();
                if (!tagValue.equals(v) && !"*".equals(tagValue)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Get the minimum zoom level objects with this style should be visible from on
         * 
         * @return the minimum zoom level
         */
        public int getMinVisibleZoom() {
            return minVisibleZoom;
        }

        /**
         * Set the minimum zoom level objects with this style should be visible from on
         * 
         * @param minVisibleZoom the minimum zoom level to set
         */
        public void setMinVisibleZoom(int minVisibleZoom) {
            this.minVisibleZoom = minVisibleZoom;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Entry<String, String> tag : tags.entrySet()) {
                builder.append(tag.getKey() + "=" + tag.getValue() + "\n");
            }
            builder.append("area: " + area + "\n");
            builder.append("dontrender: " + dontrender + "\n");
            builder.append("updateWidth: " + updateWidth + "\n");
            builder.append("strokeWidth: " + paint.getStrokeWidth() + "\n");
            builder.append("widthFactor: " + widthFactor + "\n");
            return builder.toString();
        }

        /**
         * @return the arrowStyle
         */
        @Nullable
        public FeatureStyle getArrowStyle() {
            return arrowStyle;
        }

        /**
         * @param arrowStyle the arrowStyle to set
         */
        public void setArrowStyle(@Nullable FeatureStyle arrowStyle) {
            this.arrowStyle = arrowStyle;
        }

        /**
         * @return the casingStyle
         */
        @Nullable
        public FeatureStyle getCasingStyle() {
            return casingStyle;
        }

        /**
         * @param casingStyle the casingStyle to set
         */
        public void setCasingStyle(@Nullable FeatureStyle casingStyle) {
            this.casingStyle = casingStyle;
        }

        /**
         * Check if we should check a oneway tag THis could be done cleaner by using regexps for matching values
         * 
         * @return if true the object may has a oneway tag that needs to be checked
         */
        public boolean checkOneway() {
            return oneway;
        }

        /**
         * Set if we should check oneway tags
         * 
         * @param oneway if true we should check oneway tags
         */
        public void setCheckOneway(boolean oneway) {
            this.oneway = oneway;
        }
    }

    private String                    name;
    private Map<String, FeatureStyle> internalStyles;
    private FeatureStyle              wayStyles;
    private FeatureStyle              relationStyles;

    private static DataStyle                  currentStyle;
    private static HashMap<String, DataStyle> availableStyles = new HashMap<>();

    public static final float NODE_OVERLAP_TOLERANCE_VALUE = 10f;

    private static final int TOLERANCE_ALPHA   = 40;
    private static final int TOLERANCE_ALPHA_2 = 128;

    /**
     * GPS arrow
     */
    private Path orientationPath = new Path();

    /**
     * GPS waypoint
     */
    private Path waypointPath = new Path();

    /**
     * Crosshairs
     */
    private Path crosshairsPath = new Path();

    /**
     * X
     */
    private Path xPath = new Path();

    /**
     * Arrow indicating the direction of one-way streets. Set/updated in updateStrokes
     */
    public static final Path WAY_DIRECTION_PATH = new Path();

    private static final String BUILTIN_STYLE_NAME = "Default";

    private float nodeToleranceValue;
    private float wayToleranceValue;
    private float largDragToleranceRadius;
    private float minLenForHandle;
    private int   iconZoomLimit;

    private final Context ctx;

    /**
     * Create minimum default style
     * 
     * @param ctx Android Context
     */
    private DataStyle(@NonNull final Context ctx) {
        this.ctx = ctx;
        init();
    }

    /**
     * Create a profile from an InputStream
     * 
     * @param ctx Android Context
     * @param is the InputStream
     */
    private DataStyle(@NonNull Context ctx, @NonNull InputStream is) {
        this.ctx = ctx;
        init(); // defaults for internal styles
        try {
            read(is);
        } catch (Exception e) { // never crash
            Log.e(DEBUG_TAG, "Reading style configuration failed " + e.getMessage());
        }
    }

    /**
     * initialize the minimum required internal style for a new profile
     * 
     */
    private void init() {
        nodeToleranceValue = Density.dpToPx(ctx, 40f);
        wayToleranceValue = Density.dpToPx(ctx, 40f);
        largDragToleranceRadius = Density.dpToPx(ctx, 100f);
        minLenForHandle = 5 * nodeToleranceValue;
        iconZoomLimit = 15;

        createOrientationPath(1.0f);
        createWayPointPath(1.0f);
        createCrosshairsPath(1.0f);
        createXPath(1.0f);

        Log.i(DEBUG_TAG, "setting up default profile elements");
        internalStyles = new HashMap<>();

        Paint standardPath = new Paint();
        standardPath.setStyle(Style.STROKE);
        // As nodes cover the line ends/joins, the line ending styles are irrelevant for most paints
        // However, at least on the software renderer, the default styles (Cap = BUTT, Join = MITER)
        // have slightly better performance than the round styles.

        FeatureStyle baseWayStyle = new FeatureStyle(WAY, standardPath);
        baseWayStyle.setColor(Color.BLACK);
        wayStyles = baseWayStyle;

        FeatureStyle fp = new FeatureStyle(PROBLEM_WAY, standardPath);
        int problemColor = ContextCompat.getColor(ctx, R.color.problem);
        fp.setColor(problemColor);
        fp.setWidthFactor(1.5f);
        internalStyles.put(PROBLEM_WAY, fp);

        fp = new FeatureStyle(VIEWBOX, standardPath);
        fp.setColor(ContextCompat.getColor(ctx, R.color.grey));
        fp.setUpdateWidth(false);
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setAlpha(125);
        internalStyles.put(VIEWBOX, fp);

        fp = new FeatureStyle(HANDLE);
        int cccRed = ContextCompat.getColor(ctx, R.color.ccc_red);
        fp.setColor(cccRed);
        fp.setWidthFactor(1f);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        internalStyles.put(HANDLE, fp);

        fp = new FeatureStyle(NODE);
        fp.setColor(cccRed);
        fp.setWidthFactor(1f);
        internalStyles.put(NODE, fp);

        fp = new FeatureStyle(NODE_TAGGED);
        fp.setColor(cccRed);
        fp.setWidthFactor(1.5f);
        internalStyles.put(NODE_TAGGED, fp);

        fp = new FeatureStyle(NODE_THIN);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.setColor(cccRed);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(NODE_THIN, fp);

        fp = new FeatureStyle(PROBLEM_NODE);
        fp.setColor(problemColor);
        fp.setWidthFactor(1f);
        internalStyles.put(PROBLEM_NODE, fp);

        fp = new FeatureStyle(PROBLEM_NODE_TAGGED);
        fp.setColor(problemColor);
        fp.setWidthFactor(1.5f);
        internalStyles.put(PROBLEM_NODE_TAGGED, fp);

        fp = new FeatureStyle(PROBLEM_NODE_THIN);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.setColor(problemColor);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(PROBLEM_NODE_THIN, fp);

        fp = new FeatureStyle(HIDDEN_NODE);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.setColor(ContextCompat.getColor(ctx, R.color.light_grey));
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(HIDDEN_NODE, fp);

        fp = new FeatureStyle(GPS_TRACK, baseWayStyle);
        fp.setColor(Color.BLUE);
        fp.getPaint().setStrokeCap(Cap.ROUND);
        fp.getPaint().setStrokeJoin(Join.ROUND);
        internalStyles.put(GPS_TRACK, fp);

        fp = new FeatureStyle(WAY_TOLERANCE, baseWayStyle);
        fp.setColor(ContextCompat.getColor(ctx, R.color.ccc_ocher));
        fp.setUpdateWidth(false);
        fp.getPaint().setAlpha(TOLERANCE_ALPHA);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, wayToleranceValue));
        internalStyles.put(WAY_TOLERANCE, fp);

        fp = new FeatureStyle(WAY_TOLERANCE_2, baseWayStyle);
        fp.setColor(ContextCompat.getColor(ctx, R.color.ccc_ocher));
        fp.setUpdateWidth(false);
        fp.getPaint().setAlpha(TOLERANCE_ALPHA_2);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, wayToleranceValue));
        internalStyles.put(WAY_TOLERANCE_2, fp);

        fp = new FeatureStyle(SELECTED_NODE);
        int cccBeige = ContextCompat.getColor(ctx, R.color.ccc_beige);
        fp.setColor(cccBeige);
        fp.setWidthFactor(1.5f);
        internalStyles.put(SELECTED_NODE, fp);

        fp = new FeatureStyle(SELECTED_RELATION_NODE, internalStyles.get(SELECTED_NODE));
        int colorRelation = ContextCompat.getColor(ctx, R.color.relation);
        fp.setColor(colorRelation);
        internalStyles.put(SELECTED_RELATION_NODE, fp);

        fp = new FeatureStyle(NODE_DRAG_RADIUS);
        fp.setColor(cccBeige);
        fp.setUpdateWidth(false);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setAlpha(150);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 10f));
        internalStyles.put(NODE_DRAG_RADIUS, fp);

        fp = new FeatureStyle(SELECTED_NODE_TAGGED);
        fp.setColor(cccBeige);
        fp.setWidthFactor(2f);
        internalStyles.put(SELECTED_NODE_TAGGED, fp);

        fp = new FeatureStyle(SELECTED_RELATION_NODE_TAGGED, internalStyles.get(SELECTED_NODE_TAGGED));
        fp.setColor(colorRelation);
        internalStyles.put(SELECTED_RELATION_NODE_TAGGED, fp);

        fp = new FeatureStyle(SELECTED_NODE_THIN);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.setColor(cccBeige);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(SELECTED_NODE_THIN, fp);

        fp = new FeatureStyle(SELECTED_RELATION_NODE_THIN, internalStyles.get(SELECTED_NODE_THIN));
        fp.setColor(colorRelation);
        internalStyles.put(SELECTED_RELATION_NODE_THIN, fp);

        fp = new FeatureStyle(GPS_POS, internalStyles.get(GPS_TRACK));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 4.0f));
        fp.setUpdateWidth(false);
        internalStyles.put(GPS_POS, fp);

        fp = new FeatureStyle(GPS_POS_FOLLOW, internalStyles.get(GPS_POS));
        fp.getPaint().setStyle(Style.STROKE);
        internalStyles.put(GPS_POS_FOLLOW, fp);

        fp = new FeatureStyle(GPS_ACCURACY, internalStyles.get(GPS_POS));
        fp.getPaint().setStyle(Style.FILL_AND_STROKE);
        fp.getPaint().setAlpha(TOLERANCE_ALPHA);
        fp.setUpdateWidth(false);
        internalStyles.put(GPS_ACCURACY, fp);

        fp = new FeatureStyle(SELECTED_WAY, baseWayStyle);
        fp.setColor(cccBeige);
        fp.setWidthFactor(2f);
        fp.getPaint().setStrokeCap(Cap.ROUND);
        fp.getPaint().setStrokeJoin(Join.ROUND);
        internalStyles.put(SELECTED_WAY, fp);

        fp = new FeatureStyle(HIDDEN_WAY, baseWayStyle);
        fp.setColor(ContextCompat.getColor(ctx, R.color.light_grey));
        fp.getPaint().setAlpha(TOLERANCE_ALPHA);
        fp.setWidthFactor(0.5f);
        fp.getPaint().setStrokeCap(Cap.ROUND);
        fp.getPaint().setStrokeJoin(Join.ROUND);
        internalStyles.put(HIDDEN_WAY, fp);

        fp = new FeatureStyle(SELECTED_RELATION_WAY, internalStyles.get(SELECTED_WAY));
        fp.setColor(colorRelation);
        internalStyles.put(SELECTED_RELATION_WAY, fp);

        fp = new FeatureStyle(NODE_TOLERANCE);
        fp.setColor(ContextCompat.getColor(ctx, R.color.ccc_ocher));
        fp.setUpdateWidth(false);
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setAlpha(TOLERANCE_ALPHA);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, nodeToleranceValue));
        internalStyles.put(NODE_TOLERANCE, fp);

        fp = new FeatureStyle(NODE_TOLERANCE_2);
        fp.setColor(ContextCompat.getColor(ctx, R.color.ccc_ocher));
        fp.setUpdateWidth(false);
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setAlpha(TOLERANCE_ALPHA_2);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, nodeToleranceValue));
        internalStyles.put(NODE_TOLERANCE_2, fp);

        fp = new FeatureStyle(INFOTEXT);
        fp.setColor(Color.BLACK);
        fp.setUpdateWidth(false);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(INFOTEXT, fp);

        fp = new FeatureStyle(ATTRIBUTION_TEXT);
        fp.setColor(Color.WHITE);
        fp.setUpdateWidth(false);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        fp.getPaint().setShadowLayer(1, 0, 0, Color.BLACK);
        internalStyles.put(ATTRIBUTION_TEXT, fp);

        fp = new FeatureStyle(LABELTEXT);
        fp.setColor(Color.BLACK);
        fp.setUpdateWidth(false);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(LABELTEXT, fp);

        fp = new FeatureStyle(LABELTEXT_NORMAL);
        fp.setColor(Color.BLACK);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 16));
        internalStyles.put(LABELTEXT_NORMAL, fp);

        fp = new FeatureStyle(LABELTEXT_SMALL);
        fp.setColor(Color.BLACK);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(LABELTEXT_SMALL, fp);

        fp = new FeatureStyle(LABELTEXT_NORMAL_SELECTED);
        fp.setColor(cccBeige);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 16));
        internalStyles.put(LABELTEXT_NORMAL_SELECTED, fp);

        fp = new FeatureStyle(LABELTEXT_SMALL_SELECTED);
        fp.setColor(cccBeige);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(LABELTEXT_SMALL_SELECTED, fp);

        fp = new FeatureStyle(LABELTEXT_NORMAL_PROBLEM);
        fp.setColor(Color.BLACK);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 16));
        internalStyles.put(LABELTEXT_NORMAL_PROBLEM, fp);

        fp = new FeatureStyle(LABELTEXT_SMALL_PROBLEM);
        fp.setColor(problemColor);
        fp.setUpdateWidth(false);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setTypeface(Typeface.SANS_SERIF);
        fp.getPaint().setTextSize(Density.dpToPx(ctx, 12));
        internalStyles.put(LABELTEXT_SMALL_PROBLEM, fp);

        fp = new FeatureStyle(LABELTEXT_BACKGROUND);
        fp.setColor(Color.WHITE);
        fp.getPaint().setAlpha(64);
        fp.setUpdateWidth(false);
        fp.getPaint().setStyle(Style.FILL);
        internalStyles.put(LABELTEXT_BACKGROUND, fp);

        fp = new FeatureStyle(WAY_DIRECTION);
        fp.setColor(cccRed);
        fp.setWidthFactor(0.8f);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setStrokeCap(Cap.SQUARE);
        fp.getPaint().setStrokeJoin(Join.MITER);
        internalStyles.put(WAY_DIRECTION, fp);

        fp = new FeatureStyle(OPEN_NOTE);
        fp.setColor(ContextCompat.getColor(ctx, R.color.bug_open));
        fp.getPaint().setAlpha(100);
        internalStyles.put(OPEN_NOTE, fp);

        fp = new FeatureStyle(CLOSED_NOTE);
        fp.setColor(ContextCompat.getColor(ctx, R.color.bug_closed));
        fp.getPaint().setAlpha(100);
        internalStyles.put(CLOSED_NOTE, fp);

        fp = new FeatureStyle(CROSSHAIRS);
        fp.setColor(Color.BLACK);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 1.0f));
        fp.setUpdateWidth(false);
        internalStyles.put(CROSSHAIRS, fp);

        fp = new FeatureStyle(CROSSHAIRS_HALO);
        fp.setColor(Color.WHITE);
        fp.getPaint().setStyle(Style.STROKE);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 3.0f));
        fp.setUpdateWidth(false);
        internalStyles.put(CROSSHAIRS_HALO, fp);

        fp = new FeatureStyle(GEOJSON_DEFAULT);
        fp.getPaint().setStyle(Style.STROKE);
        fp.setColor(0x9d00ff00);
        fp.setWidthFactor(2f);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 3.0f));
        fp.setUpdateWidth(false);
        internalStyles.put(GEOJSON_DEFAULT, fp);

        fp = new FeatureStyle(DONTRENDER_WAY, standardPath);
        fp.setColor(Color.WHITE);
        fp.setWidthFactor(1f);
        fp.setDontRender(true);
        internalStyles.put(DONTRENDER_WAY, fp);

        // dummy styles for ways and relations
        fp = new FeatureStyle("", standardPath);
        fp.setColor(Color.BLACK);
        fp.setWidthFactor(1f);
        wayStyles = fp;

        fp = new FeatureStyle("", standardPath);
        fp.setColor(Color.BLACK);
        fp.setWidthFactor(1f);
        relationStyles = fp;

        Log.i(DEBUG_TAG, "... done");
    }

    /**
     * Set the anti-aliasing flag on all styles
     * 
     * @param aa the boolean value to set
     */
    public static void setAntiAliasing(final boolean aa) {
        processCurrentStyle(new ProcessStyle() {
            @Override
            public void process(FeatureStyle style) {
                style.getPaint().setAntiAlias(aa);
            }
        });
    }

    interface ProcessStyle {
        /**
         * Process a FeatureStyle
         * 
         * @param style the style to work on
         */
        void process(@NonNull FeatureStyle style);
    }

    /**
     * Loop over all styles and apply processor
     * 
     * @param processor the actions to carry out on the styles
     */
    public static void processCurrentStyle(@NonNull ProcessStyle processor) {
        for (FeatureStyle style : currentStyle.internalStyles.values()) {
            if (style != null) {
                processor.process(style);
            }
        }
        if (currentStyle.wayStyles != null) {
            processor.process(currentStyle.wayStyles);
            if (currentStyle.wayStyles.cascadedStyles != null) {
                processRecursive(currentStyle.wayStyles.cascadedStyles, processor);
            }
        }
        if (currentStyle.relationStyles != null) {
            processor.process(currentStyle.relationStyles);
            if (currentStyle.relationStyles.cascadedStyles != null) {
                processRecursive(currentStyle.relationStyles.cascadedStyles, processor);
            }
        }
    }

    /**
     * Traverse recursively all the styles provided
     * 
     * @param styles a List of FeatureStyle
     * @param processor the processor
     */
    private static void processRecursive(@NonNull List<FeatureStyle> styles, @NonNull ProcessStyle processor) {
        for (FeatureStyle style : styles) {
            if (style != null) {
                processor.process(style);
                if (style.cascadedStyles != null) {
                    processRecursive(style.cascadedStyles, processor);
                }
            }
        }
    }

    /**
     * Sets the stroke width of all styles with update enabled corresponding to the width of the viewbox (=zoomfactor).
     * 
     * @param newStrokeWidth the new width to set
     */
    public static void updateStrokes(final float newStrokeWidth) {
        processCurrentStyle(new ProcessStyle() {
            @Override
            public void process(FeatureStyle style) {
                if (style.updateWidth) {
                    style.setStrokeWidth(newStrokeWidth);
                }
            }
        });

        // hardwired (for now)
        WAY_DIRECTION_PATH.rewind();
        float wayDirectionPathOffset = newStrokeWidth * 2.0f;
        WAY_DIRECTION_PATH.moveTo(-wayDirectionPathOffset, -wayDirectionPathOffset);
        WAY_DIRECTION_PATH.lineTo(0, 0);
        WAY_DIRECTION_PATH.lineTo(-wayDirectionPathOffset, +wayDirectionPathOffset);
    }

    public float getNodeToleranceValue() {
        return nodeToleranceValue;
    }

    public float getWayToleranceValue() {
        return wayToleranceValue;
    }

    public float getLargDragToleranceRadius() {
        return largDragToleranceRadius;
    }

    /**
     * Get the minimum zoom from which we show icons
     * 
     * @return the minimum zoom from which we show icons
     */
    public int getIconZoomLimit() {
        return iconZoomLimit;
    }

    /**
     * Get the minimum length of a segment to show a geometry improvement handle on it
     * 
     * @return the minimum length
     */
    public float getMinLenForHandle() {
        return minLenForHandle;
    }

    /**
     * Get the internal FeatureStyle specified by key from current profile
     * 
     * @param key the key for the style
     * @return the style or null if not found
     */
    @Nullable
    public static FeatureStyle getInternal(@NonNull final String key) {
        return currentStyle.internalStyles.get(key);
    }

    /**
     * Get the current DataStyle
     * 
     * @return the current DataStyle
     */
    public static DataStyle getCurrent() {
        return currentStyle;
    }

    /**
     * return specific named profile
     * 
     * @param name name of the profile
     * @return the DataStyle object or null if it couldn't be found
     */
    @Nullable
    public static DataStyle getStyle(@NonNull String name) {
        if (availableStyles == null) {
            return null;
        }
        return availableStyles.get(name);
    }

    /**
     * Get the list of available Styles
     * 
     * @param activity the calling Activity
     * @return list of available Styles (Default entry first, rest sorted)
     */
    @NonNull
    public static String[] getStyleList(@NonNull Activity activity) {
        if (availableStyles.size() == 0) { // shouldn't happen
            Log.e(DEBUG_TAG, "getStyleList called before initialized");
            addDefaultStye(activity);
        }
        // creating the default style object will set availableStyles
        String[] res = new String[availableStyles.size()];

        res[0] = BUILTIN_STYLE_NAME;
        String keys[] = (new TreeMap<>(availableStyles)).keySet().toArray(new String[0]); // sort the list
        int j = 1;
        for (int i = 0; i < res.length; i++) {
            if (!keys[i].equals(BUILTIN_STYLE_NAME)) {
                res[j] = keys[i];
                j++;
            }
        } // probably silly way of doing this
        return res;
    }

    /**
     * switch to Style with name n
     * 
     * @param n name of the style
     * @return true if successful
     */
    public static boolean switchTo(@NonNull String n) {
        DataStyle p = getStyle(n);
        if (p != null) {
            currentStyle = p;
            Log.i(DEBUG_TAG, "Switching to " + n);
            return true;
        }
        return false;
    }

    /**
     * start parsing a config file
     * 
     * @param in the InputStream
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void start(@NonNull final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * vars for the XML parser
     */
    private FeatureStyle             tempFeatureStyle;
    String                           type       = null;
    String                           tags       = null;
    private ArrayList<Float>         tempIntervals;
    private float                    tempPhase;
    private FeatureStyle             parent     = null;
    private ArrayDeque<FeatureStyle> styleStack = new ArrayDeque<>();

    @Override
    public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
        try {
            if (element.equals("profile")) {
                name = atts.getValue("name");
                String format = atts.getValue("format");
                if (format != null) {
                    Version v = new Version(format);
                    if (v.getMajor() == CURRENT_VERSION.getMajor() && v.getMinor() == CURRENT_VERSION.getMinor()) {
                        return; // everything OK
                    }
                }
                Snack.toastTopError(ctx, ctx.getString(R.string.toast_invalid_style_file, name));
                Log.e(DEBUG_TAG, "format attribute missing or wrong for " + name);
                throw new SAXException("format attribute missing or wrong for " + name);
            } else if (element.equals("config")) {
                type = atts.getValue("type");
                if (type != null) {
                    switch (type) {
                    case LARGE_DRAG_AREA:
                        // special handling
                        largDragToleranceRadius = Density.dpToPx(ctx, Float.parseFloat(atts.getValue("touchRadius")));
                        return;
                    case MARKER_SCALE:
                        float scale = Float.parseFloat(atts.getValue("scale"));
                        createOrientationPath(scale);
                        createWayPointPath(scale);
                        createCrosshairsPath(scale);
                        createXPath(scale);
                        return;
                    case MIN_HANDLE_LEN:
                        String lenStr = atts.getValue("length");
                        if (lenStr != null) {
                            minLenForHandle = Density.dpToPx(ctx, Float.parseFloat(lenStr));
                        }
                        return;
                    case ICON_ZOOM_LIMIT:
                        String zoomStr = atts.getValue("zoom");
                        if (zoomStr != null) {
                            iconZoomLimit = Integer.parseInt(zoomStr);
                        }
                        return;
                    default:
                        Log.e(DEBUG_TAG, "unknown config type " + type);
                    }
                }
            } else if (element.equals("feature")) {
                type = atts.getValue("type");
                if (tempFeatureStyle != null) { // we already have a style, save it
                    styleStack.push(tempFeatureStyle);
                    parent = tempFeatureStyle;
                }

                tags = atts.getValue("tags");
                if (Way.NAME.equals(type) || Relation.NAME.equals(type)) {
                    if (parent != null) {
                        tempFeatureStyle = new FeatureStyle(tags == null ? "" : tags, parent); // inherit
                    } else {
                        tempFeatureStyle = new FeatureStyle(tags == null ? "" : tags);
                    }
                } else {
                    tempFeatureStyle = new FeatureStyle(type);
                }

                String areaString = atts.getValue("area");
                if (areaString != null) {
                    tempFeatureStyle.setArea(Boolean.parseBoolean(areaString));
                }

                String dontrenderString = atts.getValue("dontrender");
                if (dontrenderString != null) {
                    tempFeatureStyle.setDontRender(Boolean.parseBoolean(dontrenderString));
                }

                String updateWidthString = atts.getValue("updateWidth");
                if (updateWidthString != null) {
                    tempFeatureStyle.setUpdateWidth(Boolean.parseBoolean(updateWidthString));
                }

                String widthFactorString = atts.getValue("widthFactor");
                if (widthFactorString != null) {
                    tempFeatureStyle.setWidthFactor(Float.parseFloat(widthFactorString));
                }

                String colorString = atts.getValue("color");
                if (colorString != null) {
                    tempFeatureStyle.setColor((int) Long.parseLong(colorString, 16)); // workaround highest bit
                }

                String styleString = atts.getValue("style");
                if (styleString != null) {
                    Style style = Style.valueOf(styleString);
                    tempFeatureStyle.getPaint().setStyle(style);
                }

                String capString = atts.getValue("cap");
                if (capString != null) {
                    tempFeatureStyle.getPaint().setStrokeCap(Cap.valueOf(capString));
                }

                String joinString = atts.getValue("join");
                if (joinString != null) {
                    tempFeatureStyle.getPaint().setStrokeJoin(Join.valueOf(atts.getValue("join")));
                }

                if (!tempFeatureStyle.updateWidth()) {
                    String strokeWidthString = atts.getValue("strokeWidth");
                    if (strokeWidthString != null) {
                        float strokeWidth = Density.dpToPx(ctx, Float.parseFloat(strokeWidthString));
                        tempFeatureStyle.setStrokeWidth(strokeWidth);
                        // special case if we are setting internal tolerance values
                        if (type.equals(NODE_TOLERANCE)) {
                            nodeToleranceValue = strokeWidth;
                        } else if (type.equals(WAY_TOLERANCE)) {
                            wayToleranceValue = strokeWidth;
                        }
                    }
                }

                if (atts.getValue("typefacestyle") != null) {
                    tempFeatureStyle.getPaint().setTypeface(Typeface.defaultFromStyle(Integer.parseInt(atts.getValue("typefacestyle"))));
                    tempFeatureStyle.getPaint().setTextSize(Density.dpToPx(ctx, Float.parseFloat(atts.getValue("textsize"))));
                    if (atts.getValue("shadow") != null) {
                        tempFeatureStyle.getPaint().setShadowLayer(Integer.parseInt(atts.getValue("shadow")), 0, 0, Color.BLACK);
                    }
                }

                if (atts.getValue("pathPattern") != null) {
                    tempFeatureStyle.setPathPattern(PathPatterns.get(atts.getValue("pathPattern")));
                }

                String minVisibleZoomString = atts.getValue("minVisibleZoom");
                if (minVisibleZoomString != null) {
                    tempFeatureStyle.setMinVisibleZoom((int) Integer.parseInt(minVisibleZoomString));
                }

                String arrowStyle = atts.getValue("arrowStyle");
                if (arrowStyle != null) {
                    tempFeatureStyle.setArrowStyle(internalStyles.get(arrowStyle));
                }

                String casingStyle = atts.getValue("casingStyle");
                if (casingStyle != null) {
                    tempFeatureStyle.setCasingStyle(internalStyles.get(casingStyle));
                }

                tempFeatureStyle.setCheckOneway(atts.getValue("oneway") != null);

            } else if (element.equals("dash")) {
                tempPhase = Float.parseFloat(atts.getValue("phase"));
                tempIntervals = new ArrayList<>();
            } else if (element.equals("interval")) {
                tempIntervals.add(Float.parseFloat(atts.getValue("length")));
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Parse Exception", e);
        }
    }

    /**
     * Create a path for the crosshairs
     * 
     * @param scale scaling factor
     */
    private void createCrosshairsPath(float scale) {
        crosshairsPath = new Path();
        int arm = (int) Density.dpToPx(ctx, 10 * scale);
        crosshairsPath.moveTo(0, -arm);
        crosshairsPath.lineTo(0, arm);
        crosshairsPath.moveTo(arm, 0);
        crosshairsPath.lineTo(-arm, 0);
    }

    /**
     * Create a path for the geometry improvement handles
     * 
     * @param scale scaling factor
     */
    private void createXPath(float scale) {
        int arm;
        xPath = new Path();
        arm = (int) Density.dpToPx(ctx, 3 * scale);
        xPath.moveTo(-arm, -arm);
        xPath.lineTo(arm, arm);
        xPath.moveTo(arm, -arm);
        xPath.lineTo(-arm, arm);
    }

    /**
     * Create a path for GPX waypoints
     * 
     * @param scale scaling factor
     */
    private void createWayPointPath(float scale) {
        waypointPath = new Path();
        int side = (int) Density.dpToPx(ctx, 8 * scale);
        waypointPath.moveTo(0, 0);
        waypointPath.lineTo(side, -side * 2f);
        waypointPath.lineTo(-side, -side * 2f);
        waypointPath.lineTo(0, 0);
    }

    /**
     * Create a path for way orientation arrows
     * 
     * @param scale scaling factor
     */
    private void createOrientationPath(float scale) {
        orientationPath = new Path();
        orientationPath.moveTo(0, Density.dpToPx(ctx, -20) * scale);
        orientationPath.lineTo(Density.dpToPx(ctx, 15) * scale, Density.dpToPx(ctx, 20) * scale);
        orientationPath.lineTo(0, Density.dpToPx(ctx, 10) * scale);
        orientationPath.lineTo(Density.dpToPx(ctx, -15) * scale, Density.dpToPx(ctx, 20) * scale);
        orientationPath.lineTo(0, Density.dpToPx(ctx, -20) * scale);
    }

    @Override
    public void endElement(final String uri, final String element, final String qName) {
        if (element == null) {
            Log.i(DEBUG_TAG, "element is null");
            return;
        }
        if (element.equals("feature")) {
            if (tempFeatureStyle == null) {
                Log.i(DEBUG_TAG, "FeatureStyle is null");
                return;
            }
            switch (type) {
            case Way.NAME:
                if (tags == null) {
                    wayStyles = tempFeatureStyle;
                    parent = wayStyles;
                } else {
                    parent.addStyle(tempFeatureStyle);
                }
                break;
            case Relation.NAME:
                if (tags == null) {
                    relationStyles = tempFeatureStyle;
                    parent = relationStyles;
                } else {
                    parent.addStyle(tempFeatureStyle);
                }
                break;
            default:
                // overwrites existing profiles
                internalStyles.put(type, tempFeatureStyle);
            }

            try {
                tempFeatureStyle = styleStack.pop();
                if (!styleStack.isEmpty()) {
                    parent = styleStack.peek();
                } else {
                    switch (type) {
                    case Way.NAME:
                        parent = wayStyles;
                        break;
                    case Relation.NAME:
                        parent = relationStyles;
                        break;
                    default:
                        parent = null;
                    }
                }
            } catch (NoSuchElementException e) {
                tempFeatureStyle = null;
            }
        } else if (element.equals("dash")) {
            float[] tIntervals = new float[tempIntervals.size()];
            for (int i = 0; i < tIntervals.length; i++) {
                tIntervals[i] = tempIntervals.get(i);
            }
            tempFeatureStyle.setDashPath(tIntervals, tempPhase);
        }
    }

    /**
     * Read an InputStream and parse the XML
     * 
     * @param is the InputStream
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private void read(@NonNull InputStream is) throws SAXException, IOException, ParserConfigurationException {
        InputStream inputStream = new BufferedInputStream(is);
        start(inputStream);
    }

    /**
     * searches directories for profile files and creates new profiles from them
     * 
     * @param ctx Android Context
     */
    public static void getStylesFromFiles(@NonNull Context ctx) {
        if (availableStyles.size() == 0) {
            Log.i(DEBUG_TAG, "No style files found");
            // no files, need to install a default
            addDefaultStye(ctx);
        }
        // assets directory
        AssetManager assetManager = ctx.getAssets();
        //
        try {
            String[] fileList = assetManager.list("");
            if (fileList != null) {
                for (String fn : fileList) {
                    if (fn.endsWith(FILE_PATH_STYLE_SUFFIX)) {
                        Log.i(DEBUG_TAG, "Creating profile from file in assets directory " + fn);
                        InputStream is = assetManager.open(fn);
                        DataStyle p = new DataStyle(ctx, is);
                        availableStyles.put(p.name, p);
                    }
                }
            }
        } catch (Exception ex) {
            Log.i(DEBUG_TAG, ex.toString());
        }

        // from sdcard
        File sdcard = Environment.getExternalStorageDirectory();
        File indir = new File(sdcard, Paths.DIRECTORY_PATH_VESPUCCI);
        if (indir != null) {
            class StyleFilter implements FilenameFilter {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(FILE_PATH_STYLE_SUFFIX);
                }
            }
            File[] list = indir.listFiles(new StyleFilter());
            if (list != null) {
                for (File f : list) {
                    Log.i(DEBUG_TAG, "Creating profile from " + f.getName());
                    try {
                        InputStream is = new FileInputStream(f);
                        DataStyle p = new DataStyle(ctx, is);
                        // overwrites profile with same name
                        availableStyles.put(p.name, p);
                    } catch (Exception ex) {
                        Log.i(DEBUG_TAG, ex.toString());
                    }
                }
            }

        }
    }

    private static void addDefaultStye(Context ctx) {
        DataStyle p = new DataStyle(ctx);
        p.name = BUILTIN_STYLE_NAME;
        currentStyle = p;
        availableStyles.put(p.name, p);
    }

    public static String getBuiltinStyleName() {
        return BUILTIN_STYLE_NAME;
    }

    /**
     * @return the orientation_path
     */
    public Path getOrientationPath() {
        return orientationPath;
    }

    /**
     * @return the waypoint_path
     */
    public Path getWaypointPath() {
        return waypointPath;
    }

    /**
     * @return the crosshairs_path
     */
    public Path getCrosshairsPath() {
        return crosshairsPath;
    }

    /**
     * @return the x_path
     */
    public Path getXPath() {
        return xPath;
    }

    /**
     * Determine the style to use for way and cache it in the way object
     * 
     * If the way is untagged or a style can't be determined, we return a style for any relations the way is a member of
     * 
     * @param element way we need the style for
     * @param <T> an OsmElement the implements StyleableFeature
     * @return the style
     */
    public static <T extends OsmElement & StyleableFeature> FeatureStyle matchStyle(@NonNull final T element) {
        FeatureStyle style = element.getStyle();
        if (style == null) {
            style = Way.NAME.equals(element.getName()) ? currentStyle.wayStyles : currentStyle.relationStyles;
            style = matchRecursive(style, element.getTags());
            element.setStyle(style);
        }
        return style;
    }

    /**
     * Recursively traverse the styles and try to find a match
     * 
     * @param style the style
     * @param tags tags from the element we are trying to match
     * @return the best matching style
     */
    @NonNull
    private static FeatureStyle matchRecursive(@NonNull FeatureStyle style, @NonNull SortedMap<String, String> tags) {
        FeatureStyle result = style;
        if (style.cascadedStyles != null) {
            for (FeatureStyle s : style.cascadedStyles) {
                if (s.match(tags)) {
                    return matchRecursive(s, tags);
                }
            }
        }
        return result;
    }
}
