package de.blau.android.resources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import ch.poole.poparser.Po;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StyleableFeature;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.resources.symbols.Symbols;
import de.blau.android.util.Density;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import de.blau.android.util.Version;
import de.blau.android.util.XmlFileFilter;
import de.blau.android.util.collections.MultiHashMap;

public final class DataStyle extends DefaultHandler {
    private static final String DEBUG_TAG = DataStyle.class.getSimpleName().substring(0, Math.min(23, DataStyle.class.getSimpleName().length()));

    private static final String I18N_DATASTYLE = "i18n/datastyle_";

    private static final Version CURRENT_VERSION = new Version("0.3.0");

    private static final String FILE_PATH_STYLE_SUFFIX = "-profile.xml";

    // constants for the internal profiles
    public static final String GPS_TRACK                     = "gps_track";
    public static final String MVT_DEFAULT                   = "mvt_default";
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
    public static final String NODE_UNTAGGED                 = "node_untagged";
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
    public static final String GPS_POS_STALE                 = "gps_pos_stale";
    public static final String GPS_POS_FOLLOW_STALE          = "gps_pos_follow_stale";
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
    public static final String BOOKMARK_DEFAULT              = "bookmark_default";
    public static final String DONTRENDER_WAY                = "dontrender_way";
    public static final String MIN_HANDLE_LEN                = "min_handle_length";
    public static final String ICON_ZOOM_LIMIT               = "icon_zoom_limit";

    // XML elements for the config files
    private static final String INTERVAL_ELEMENT      = "interval";
    private static final String DASH_ELEMENT          = "dash";
    private static final String PROFILE_ELEMENT       = "profile";
    private static final String CONFIG_ELEMENT        = "config";
    private static final String FEATURE_ELEMENT       = "feature";
    private static final String DONTRENDER_ATTR       = "dontrender";
    private static final String MIN_VISIBLE_ZOOM_ATTR = "minVisibleZoom";
    private static final String UPDATE_WIDTH_ATTR     = "updateWidth";
    private static final String WIDTH_FACTOR_ATTR     = "widthFactor";
    private static final String AREA_ATTR             = "area";
    private static final String CLOSED_ATTR           = "closed";
    private static final String ONEWAY_ATTR           = "oneway";
    private static final String COLOR_ATTR            = "color";
    private static final String STYLE_ATTR            = "style";
    private static final String CAP_ATTR              = "cap";
    private static final String JOIN_ATTR             = "join";
    private static final String STROKEWIDTH_ATTR      = "strokeWidth";
    private static final String TEXTSIZE_ATTR         = "textsize";
    private static final String PHASE_ATTR            = "phase";
    private static final String LENGTH_ATTR           = "length";
    private static final String TYPEFACESTYLE_ATTR    = "typefacestyle";
    private static final String SHADOW_ATTR           = "shadow";
    private static final String PATH_PATTERN_ATTR     = "pathPattern";
    private static final String ARROW_STYLE_ATTR      = "arrowStyle";
    private static final String CASING_STYLE_ATTR     = "casingStyle";
    private static final String VALIDATION            = "validation";
    private static final String CODE_ATTR             = "code";
    private static final String SCALE_ATTR            = "scale";
    private static final String TOUCH_RADIUS_ATTR     = "touchRadius";
    private static final String ZOOM_ATTR             = "zoom";
    private static final String TYPE_ATTR             = "type";
    private static final String FORMAT_ATTR           = "format";
    private static final String NAME_ATTR             = "name";
    private static final String TAGS_ATTR             = "tags";
    private static final String LABEL_KEY_ATTR        = "labelKey";
    private static final String LABEL_ZOOM_LIMIT_ATTR = "labelZoomLimit";
    private static final String ICON_PATH_ATTR        = "iconPath";
    private static final String PRESET                = "preset";
    private static final String OFFSET_ATTR           = "offset";
    private static final String TEXT_COLOR_ATTR       = "textColor";

    private static final int  DEFAULT_MIN_VISIBLE_ZOOM     = 15;
    public static final float DEFAULT_GPX_STROKE_WIDTH     = 4.0f;
    public static final float DEFAULT_GEOJSON_STROKE_WIDTH = 3.0f;

    public class FeatureStyle {

        final Map<String, String> tags;
        private int               minVisibleZoom = DEFAULT_MIN_VISIBLE_ZOOM;
        private boolean           area           = false;
        boolean                   dontrender     = false;
        boolean                   updateWidth    = true;
        final Paint               paint;
        float                     widthFactor;
        float                     offset         = 0f;
        DashPath                  dashPath       = null;
        private FontMetrics       fontMetrics    = null;
        private PathPattern       pathPattern    = null;
        private boolean           pathPatternSupported;
        private FeatureStyle      arrowStyle     = null;
        private FeatureStyle      casingStyle    = null;
        private boolean           oneway         = false;
        private Boolean           closed         = null;
        private String            labelKey       = null;
        private String            iconPath       = null;
        private int               labelZoomLimit = Integer.MAX_VALUE;
        private int               textColor;

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
            textColor = paint.getColor();
            widthFactor = 1.0f;
            pathPatternSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
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
            offset = fp.offset;
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
            closed = fp.closed;
            labelKey = fp.labelKey;
            setLabelZoomLimit(fp.getLabelZoomLimit());
            iconPath = fp.iconPath;
            cascadedStyles = null;
            textColor = fp.textColor;
            pathPatternSupported = fp.pathPatternSupported;
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
         * Set an offset
         * 
         * @param f the value to set
         */
        public void setOffset(float f) {
            offset = f;
        }

        /**
         * Get the offset
         * 
         * @return the offset multiplied with the current stroke width
         */
        public float getOffset() {
            return offset * paint.getStrokeWidth();
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
            } else {
                if (pathPattern != null && pathPatternSupported) {
                    getPaint().setPathEffect(new PathDashPathEffect(pathPattern.draw(newWidth), pathPattern.advance(newWidth), 0f, pathPattern.style()));
                } else {
                    getPaint().setPathEffect(null);
                }
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
            DashPathEffect dp = new DashPathEffect(intervals, dashPath.phase);
            paint.setPathEffect(dp);
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
            this.pathPattern = pathPattern;
            if (pathPattern != null && pathPatternSupported) {
                float width = getPaint().getStrokeWidth();
                getPaint().setPathEffect(new PathDashPathEffect(pathPattern.draw(width), pathPattern.advance(width), 0f, pathPattern.style()));
            } else {
                getPaint().setPathEffect(null);
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
         * Check if we should check a oneway tag This could be done cleaner by using regexps for matching values
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

        /**
         * Set if this style should apply only to closed ways or the opposite
         * 
         * @param closed if true this style will only be used for closed ways
         */
        public void setClosed(boolean closed) {
            this.closed = Boolean.valueOf(closed);
        }

        /**
         * Set the label key
         * 
         * @param key the key
         */
        public void setLabelKey(@Nullable String key) {
            labelKey = key;
        }

        /**
         * Get the label key
         * 
         * @return the key or null if not set
         */
        @Nullable
        public String getLabelKey() {
            return labelKey;
        }

        /**
         * Get the limit from which on we display labels
         * 
         * @return the labelZoomLimit
         */
        public int getLabelZoomLimit() {
            return labelZoomLimit;
        }

        /**
         * Set the limit from which on we display labels
         * 
         * @param labelZoomLimit the labelZoomLimit to set
         */
        public void setLabelZoomLimit(int labelZoomLimit) {
            this.labelZoomLimit = labelZoomLimit;
        }

        /**
         * Check if labelkey is set to "preset"
         * 
         * @return true if the magic value is set
         */
        public boolean usePresetLabel() {
            return PRESET.equals(labelKey);
        }

        /**
         * Check if the iconPath is set to "preset"
         * 
         * @return true if the magic value is set
         */
        public boolean usePresetIcon() {
            return PRESET.equals(iconPath);
        }

        /**
         * Set the icon path
         * 
         * @param path the path
         */
        public void setIconPath(@Nullable String path) {
            iconPath = iconDirPath != null && path != null && !PRESET.equals(path) ? iconDirPath + Paths.DELIMITER + path : path;
        }

        /**
         * Get the icon path
         * 
         * @return the path or null if not set
         */
        @Nullable
        public String getIconPath() {
            return iconPath;
        }

        /**
         * Set a color for text only
         * 
         * @param textColor the color
         */
        public void setTextColor(int textColor) {
            this.textColor = textColor;
        }

        /**
         * Get a color for text only
         * 
         * @return the color
         */
        public int getTextColor() {
            return textColor;
        }

        /**
         * Dump this Style in XML format, not very abstracted and closely tied to the implementation
         * 
         * @param s an XmlSerialzer instance
         * @throws IllegalArgumentException if the serializer encountered an illegal argument
         * @throws IllegalStateException if the serializer detects an illegal state
         * @throws IOException if writing to the serializer fails
         */
        public void toXml(@NonNull final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
            s.startTag("", FEATURE_ELEMENT);
            s.attribute("", DONTRENDER_ATTR, Boolean.toString(dontrender));
            s.attribute("", MIN_VISIBLE_ZOOM_ATTR, Integer.toString(minVisibleZoom));
            s.attribute("", UPDATE_WIDTH_ATTR, Boolean.toString(updateWidth()));
            s.attribute("", WIDTH_FACTOR_ATTR, Float.toString(getWidthFactor()));
            s.attribute("", AREA_ATTR, Boolean.toString(isArea()));
            if (closed != null) {
                s.attribute("", CLOSED_ATTR, Boolean.toString(closed));
            }
            s.attribute("", ONEWAY_ATTR, Boolean.toString(oneway));
            if (labelKey != null) {
                s.attribute("", LABEL_KEY_ATTR, labelKey);
            }
            if (getLabelZoomLimit() < Integer.MAX_VALUE) {
                s.attribute("", LABEL_ZOOM_LIMIT_ATTR, Integer.toString(getLabelZoomLimit()));
            }
            if (iconPath != null) {
                s.attribute("", ICON_PATH_ATTR, iconPath);
            }
            //
            s.attribute("", COLOR_ATTR, Integer.toHexString(getPaint().getColor()));
            // alpha should be contained in color
            s.attribute("", STYLE_ATTR, getPaint().getStyle().toString());
            s.attribute("", CAP_ATTR, getPaint().getStrokeCap().toString());
            s.attribute("", JOIN_ATTR, getPaint().getStrokeJoin().toString());
            if (!updateWidth()) {
                s.attribute("", STROKEWIDTH_ATTR, Float.toString(getPaint().getStrokeWidth()));
            }
            s.attribute("", OFFSET_ATTR, Float.toString(offset));
            Typeface tf = getPaint().getTypeface();
            if (tf != null) {
                s.attribute("", TYPEFACESTYLE_ATTR, Integer.toString(tf.getStyle()));
                s.attribute("", TEXTSIZE_ATTR, Float.toString(getPaint().getTextSize()));
            }
            if (pathPattern != null) {
                s.attribute("", PATH_PATTERN_ATTR, pathPattern.toString());
            }
            DashPath dp = getDashPath();
            if (dp != null) {
                s.startTag("", DASH_ELEMENT);
                s.attribute("", PHASE_ATTR, Float.toString(dp.phase));
                for (int i = 0; i < dp.intervals.length; i++) {
                    s.startTag("", INTERVAL_ELEMENT);
                    s.attribute("", LENGTH_ATTR, Float.toString(dp.intervals[i]));
                    s.endTag("", INTERVAL_ELEMENT);
                }
                s.endTag("", DASH_ELEMENT);
            }
            s.endTag("", FEATURE_ELEMENT);
        }

        @Override
        public String toString() {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
                serializer.setOutput(outputStream, OsmXml.UTF_8);
                serializer.startDocument(OsmXml.UTF_8, null);
                toXml(serializer);
                serializer.endDocument();
                return outputStream.toString();
            } catch (IllegalArgumentException | IllegalStateException | IOException | XmlPullParserException e) {
                return e.getMessage();
            }
        }
    }

    private String                     name;
    private Map<String, FeatureStyle>  internalStyles;
    private Map<Integer, FeatureStyle> validationStyles;
    private FeatureStyle               nodeStyles;
    private FeatureStyle               wayStyles;
    private FeatureStyle               relationStyles;

    private static DataStyle                  currentStyle;
    private static HashMap<String, DataStyle> availableStyles = new HashMap<>();

    public static final float NODE_OVERLAP_TOLERANCE_VALUE = 10f;

    private static final int TOLERANCE_ALPHA   = 40;
    private static final int TOLERANCE_ALPHA_2 = 128;

    /**
     * zoom level from which on we display icons and house numbers
     */
    private static final int DEFAULT_SHOW_ICONS_LIMIT      = 15;
    private static final int DEFAULT_SHOW_ICON_LABEL_LIMIT = DEFAULT_SHOW_ICONS_LIMIT + 5;

    /**
     * GPS arrow
     */
    private Path orientationPath = new Path();

    /**
     * Crosshairs
     */
    private Path crosshairsPath = new Path();

    /**
     * X
     */
    private Path xPath = new Path();

    /**
     * Direction arrow
     */
    private Path directionArrowPath = new Path();

    /**
     * Arrow indicating the direction of one-way streets. Set/updated in updateStrokes
     */
    public static final Path WAY_DIRECTION_PATH = new Path();

    private static final String BUILTIN_STYLE_NAME = "Built-in (minimal)";

    private float nodeToleranceValue;
    private float wayToleranceValue;
    private float largDragToleranceRadius;
    private float minLenForHandle;
    private int   iconZoomLimit;
    private int   iconLabelZoomLimit;

    private final Context ctx;
    private String        iconDirPath;

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
     * @param iconDirPath dir to use for icons
     * @throws ParserConfigurationException other parser issues
     * @throws IOException if reading the file fails
     * @throws SAXException if parsing encounters an issue
     */
    private DataStyle(@NonNull Context ctx, @NonNull InputStream is, @Nullable String iconDirPath)
            throws SAXException, IOException, ParserConfigurationException {
        this.ctx = ctx;
        this.iconDirPath = iconDirPath;
        init(); // defaults for internal styles
        read(is);
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
        iconZoomLimit = DEFAULT_SHOW_ICONS_LIMIT;
        iconLabelZoomLimit = DEFAULT_SHOW_ICON_LABEL_LIMIT;

        createOrientationPath(1.0f);
        createCrosshairsPath(1.0f);
        createXPath(1.0f);
        createDirectionArrowPath(1.0f);
        Symbols.draw(ctx, 1.0f);

        Log.i(DEBUG_TAG, "setting up default profile elements");
        internalStyles = new HashMap<>();
        validationStyles = new HashMap<>();

        Paint standardPath = new Paint();
        standardPath.setStyle(Style.STROKE);
        // As nodes cover the line ends/joins, the line ending styles are irrelevant for most paints
        // However, at least on the software renderer, the default styles (Cap = BUTT, Join = MITER)
        // have slightly better performance than the round styles.

        FeatureStyle baseWayStyle = new FeatureStyle(WAY, standardPath);
        baseWayStyle.setColor(Color.BLACK);

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

        fp = new FeatureStyle(NODE_UNTAGGED);
        fp.setColor(cccRed);
        fp.setWidthFactor(1f);
        internalStyles.put(NODE_UNTAGGED, fp);

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

        fp = new FeatureStyle(MVT_DEFAULT, baseWayStyle);
        fp.setColor(Color.BLUE);
        fp.getPaint().setAlpha(0x7F);
        fp.getPaint().setStrokeCap(Cap.ROUND);
        fp.getPaint().setStrokeJoin(Join.ROUND);
        internalStyles.put(MVT_DEFAULT, fp);

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
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, DEFAULT_GPX_STROKE_WIDTH));
        fp.setUpdateWidth(false);
        internalStyles.put(GPS_POS, fp);

        fp = new FeatureStyle(GPS_POS_FOLLOW, internalStyles.get(GPS_POS));
        fp.getPaint().setStyle(Style.STROKE);
        internalStyles.put(GPS_POS_FOLLOW, fp);

        fp = new FeatureStyle(GPS_POS_STALE, baseWayStyle);
        fp.getPaint().setStyle(Style.FILL);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, DEFAULT_GPX_STROKE_WIDTH));
        fp.setUpdateWidth(false);
        internalStyles.put(GPS_POS_STALE, fp);

        fp = new FeatureStyle(GPS_POS_FOLLOW_STALE, internalStyles.get(GPS_POS_STALE));
        fp.getPaint().setStyle(Style.STROKE);
        internalStyles.put(GPS_POS_FOLLOW_STALE, fp);

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
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, DEFAULT_GEOJSON_STROKE_WIDTH));
        fp.setUpdateWidth(false);
        internalStyles.put(GEOJSON_DEFAULT, fp);

        fp = new FeatureStyle(BOOKMARK_DEFAULT);
        fp.getPaint().setStyle(Style.STROKE);
        fp.setColor(0x9dff0000);
        fp.setWidthFactor(2f);
        fp.getPaint().setStrokeWidth(Density.dpToPx(ctx, 3.0f));
        fp.setUpdateWidth(false);
        internalStyles.put(BOOKMARK_DEFAULT, fp);

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
        nodeStyles = fp;

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
        processCurrentStyle(style -> style.getPaint().setAntiAlias(aa));
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
        if (currentStyle.validationStyles != null) {
            for (FeatureStyle style : currentStyle.validationStyles.values()) {
                if (style != null) {
                    processor.process(style);
                }
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
        processCurrentStyle(style -> {
            if (style.updateWidth) {
                style.setStrokeWidth(newStrokeWidth);
            }
        });

        // hardwired (for now)
        WAY_DIRECTION_PATH.rewind();
        float wayDirectionPathOffset = newStrokeWidth * 2.0f;
        WAY_DIRECTION_PATH.moveTo(-wayDirectionPathOffset, -wayDirectionPathOffset);
        WAY_DIRECTION_PATH.lineTo(0, 0);
        WAY_DIRECTION_PATH.lineTo(-wayDirectionPathOffset, +wayDirectionPathOffset);
    }

    /**
     * Get the radius for the area for node selection
     * 
     * @return the radius (in px ?)
     */
    public float getNodeToleranceValue() {
        return nodeToleranceValue;
    }

    /**
     * Get the half width of the tolerance area around ways
     * 
     * @return the width (in px ?)
     */
    public float getWayToleranceValue() {
        return wayToleranceValue;
    }

    /**
     * Get the radius of the large area for node dragging
     * 
     * @return the radius (in px ?)
     */
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
     * @return the iconLabelZoomLimit
     */
    public int getIconLabelZoomLimit() {
        return iconLabelZoomLimit;
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
     * Get the validation FeatureStyle specified by code from current profile
     * 
     * @param code the validation code for the object
     * @return the style or the default problem style if not found
     */
    @Nullable
    public static FeatureStyle getValidationStyle(int code) {
        FeatureStyle style = currentStyle.validationStyles.get(code);
        if (style == null) {
            return getInternal(DataStyle.PROBLEM_WAY);
        }
        return style;
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
     * @param context an Android Context
     * @return list of available Styles (Default entry first, rest sorted)
     */
    @NonNull
    public static String[] getStyleList(@NonNull Context context) {
        if (availableStyles.size() == 0) { // shouldn't happen
            Log.e(DEBUG_TAG, "getStyleList called before initialized");
            addDefaultStyle(context);
        }
        // creating the default style object will set availableStyles
        String[] res = new String[availableStyles.size()];
        res[0] = BUILTIN_STYLE_NAME;
        Map<String, DataStyle> sortedMap = new TreeMap<>();
        for (Entry<String, DataStyle> entry : availableStyles.entrySet()) {
            final DataStyle value = entry.getValue();
            final String key = entry.getKey();
            if (key != null && value != null) {
                sortedMap.put(key, value);
            } else {
                Log.e(DEBUG_TAG, "Style object missing for style " + key);
            }
        }
        String[] keys = sortedMap.keySet().toArray(new String[0]); // sort the list
        int j = 1;
        for (int i = 0; i < res.length; i++) {
            if (!BUILTIN_STYLE_NAME.equals(keys[i])) {
                res[j] = keys[i];
                j++;
            }
        } // probably silly way of doing this
        return res;
    }

    /**
     * Get the list of available Styles translated
     * 
     * @param context an Android Context
     * @param styleNames the list of style names to translate
     * @return list of available Styles translated (or untranslated if no translation is available)
     */
    @NonNull
    public static String[] getStyleListTranslated(@NonNull Context context, @NonNull String[] styleNames) {
        Locale locale = Locale.getDefault();
        try (InputStream poFileStream = getPoFileStream(context, locale)) {
            Po po = de.blau.android.util.Util.parsePoFile(poFileStream);
            if (po != null) {
                int len = styleNames.length;
                String[] res = new String[len];
                for (int i = 0; i < len; i++) {
                    res[i] = po.t(styleNames[i]);
                }
                return res;
            } else {
                Log.w(DEBUG_TAG, "Error parsing translations for " + locale);
                return styleNames;
            }
        } catch (IOException ioex) {
            Log.w(DEBUG_TAG, "No translations found for " + locale);
            return styleNames;
        }
    }

    /**
     * Get a stream for the translation
     * 
     * @param context an Android Context
     * @param locale the relevant Locale
     * @return an InputStream
     * @throws IOException if no file could be opened or read
     */
    @NonNull
    private static InputStream getPoFileStream(Context context, Locale locale) throws IOException {
        AssetManager assetManager = context.getAssets();
        try {
            return assetManager.open(I18N_DATASTYLE + locale + "." + FileExtensions.PO);
        } catch (IOException ioex) {
            return assetManager.open(I18N_DATASTYLE + locale.getLanguage() + "." + FileExtensions.PO);
        }
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
     * Start parsing a config file
     * 
     * @param in the InputStream
     * @throws SAXException if parsing fails
     * @throws IOException if reading the InputStream fails
     * @throws ParserConfigurationException if something is wrong with the parser
     */
    private void start(@NonNull final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * vars for the XML parser
     */
    private FeatureStyle        tempFeatureStyle;
    String                      type           = null;
    String                      tags           = null;
    int                         validationCode = 0;
    private List<Float>         tempIntervals;
    private float               tempPhase;
    private FeatureStyle        parent         = null;
    private Deque<FeatureStyle> styleStack     = new ArrayDeque<>();

    @Override
    public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
        try {
            if (element.equals(PROFILE_ELEMENT)) {
                name = atts.getValue(NAME_ATTR);
                String format = atts.getValue(FORMAT_ATTR);
                if (format != null) {
                    Version v = new Version(format);
                    if (v.getMajor() == CURRENT_VERSION.getMajor() && v.getMinor() == CURRENT_VERSION.getMinor()) {
                        return; // everything OK
                    }
                }
                Log.e(DEBUG_TAG, "format attribute missing or wrong for " + getName());
                throw new SAXException("format attribute missing or wrong for " + getName());
            } else if (element.equals(CONFIG_ELEMENT)) {
                type = atts.getValue(TYPE_ATTR);
                if (type != null) {
                    switch (type) {
                    case LARGE_DRAG_AREA:
                        // special handling
                        largDragToleranceRadius = Density.dpToPx(ctx, Float.parseFloat(atts.getValue(TOUCH_RADIUS_ATTR)));
                        return;
                    case MARKER_SCALE:
                        float scale = Float.parseFloat(atts.getValue(SCALE_ATTR));
                        createOrientationPath(scale);
                        createCrosshairsPath(scale);
                        createXPath(scale);
                        createDirectionArrowPath(scale);
                        Symbols.draw(ctx, scale);
                        return;
                    case MIN_HANDLE_LEN:
                        String lenStr = atts.getValue(LENGTH_ATTR);
                        if (lenStr != null) {
                            minLenForHandle = Density.dpToPx(ctx, Float.parseFloat(lenStr));
                        }
                        return;
                    case ICON_ZOOM_LIMIT:
                        String zoomStr = atts.getValue(ZOOM_ATTR);
                        if (zoomStr != null) {
                            iconZoomLimit = Integer.parseInt(zoomStr);
                        }
                        String labelZoomLimitString = atts.getValue(LABEL_ZOOM_LIMIT_ATTR);
                        if (labelZoomLimitString != null) {
                            iconLabelZoomLimit = Integer.parseInt(labelZoomLimitString);
                        }
                        return;
                    default:
                        Log.e(DEBUG_TAG, "unknown config type " + type);
                    }
                }
            } else if (element.equals(FEATURE_ELEMENT)) {
                type = atts.getValue(TYPE_ATTR);
                if (tempFeatureStyle != null) { // we already have a style, save it
                    styleStack.push(tempFeatureStyle);
                    parent = tempFeatureStyle;
                }

                tags = atts.getValue(TAGS_ATTR);
                if (Way.NAME.equals(type) || Relation.NAME.equals(type) || Node.NAME.equals(type)) {
                    if (parent != null) {
                        tempFeatureStyle = new FeatureStyle(tags == null ? "" : tags, parent); // inherit
                    } else {
                        tempFeatureStyle = new FeatureStyle(tags == null ? "" : tags);
                    }
                } else {
                    tempFeatureStyle = new FeatureStyle(type);
                }

                String areaString = atts.getValue(AREA_ATTR);
                if (areaString != null) {
                    tempFeatureStyle.setArea(Boolean.parseBoolean(areaString));
                }

                String dontrenderString = atts.getValue(DONTRENDER_ATTR);
                if (dontrenderString != null) {
                    tempFeatureStyle.setDontRender(Boolean.parseBoolean(dontrenderString));
                }

                String updateWidthString = atts.getValue(UPDATE_WIDTH_ATTR);
                if (updateWidthString != null) {
                    tempFeatureStyle.setUpdateWidth(Boolean.parseBoolean(updateWidthString));
                }

                String widthFactorString = atts.getValue(WIDTH_FACTOR_ATTR);
                if (widthFactorString != null) {
                    tempFeatureStyle.setWidthFactor(Float.parseFloat(widthFactorString));
                }

                String colorString = atts.getValue(COLOR_ATTR);
                if (colorString != null) {
                    tempFeatureStyle.setColor((int) Long.parseLong(colorString, 16)); // workaround highest bit
                }

                String styleString = atts.getValue(STYLE_ATTR);
                if (styleString != null) {
                    Style style = Style.valueOf(styleString);
                    tempFeatureStyle.getPaint().setStyle(style);
                }

                String capString = atts.getValue(CAP_ATTR);
                if (capString != null) {
                    tempFeatureStyle.getPaint().setStrokeCap(Cap.valueOf(capString));
                }

                String joinString = atts.getValue(JOIN_ATTR);
                if (joinString != null) {
                    tempFeatureStyle.getPaint().setStrokeJoin(Join.valueOf(joinString));
                }

                if (!tempFeatureStyle.updateWidth()) {
                    String strokeWidthString = atts.getValue(STROKEWIDTH_ATTR);
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

                String offsetString = atts.getValue(OFFSET_ATTR);
                if (offsetString != null) {
                    tempFeatureStyle.setOffset(Float.parseFloat(offsetString));
                }

                if (atts.getValue(TYPEFACESTYLE_ATTR) != null) {
                    tempFeatureStyle.getPaint().setTypeface(Typeface.defaultFromStyle(Integer.parseInt(atts.getValue(TYPEFACESTYLE_ATTR))));
                    tempFeatureStyle.getPaint().setTextSize(Density.dpToPx(ctx, Float.parseFloat(atts.getValue(TEXTSIZE_ATTR))));
                    if (atts.getValue(SHADOW_ATTR) != null) {
                        tempFeatureStyle.getPaint().setShadowLayer(Integer.parseInt(atts.getValue(SHADOW_ATTR)), 0, 0, Color.BLACK);
                    }
                }

                if (atts.getValue(PATH_PATTERN_ATTR) != null) {
                    tempFeatureStyle.setPathPattern(PathPatterns.get(atts.getValue(PATH_PATTERN_ATTR)));
                }

                String minVisibleZoomString = atts.getValue(MIN_VISIBLE_ZOOM_ATTR);
                if (minVisibleZoomString != null) {
                    tempFeatureStyle.setMinVisibleZoom(Integer.parseInt(minVisibleZoomString));
                }

                String arrowStyle = atts.getValue(ARROW_STYLE_ATTR);
                if (arrowStyle != null) {
                    tempFeatureStyle.setArrowStyle(internalStyles.get(arrowStyle));
                }

                String casingStyle = atts.getValue(CASING_STYLE_ATTR);
                if (casingStyle != null) {
                    tempFeatureStyle.setCasingStyle(internalStyles.get(casingStyle));
                }

                tempFeatureStyle.setCheckOneway(atts.getValue(ONEWAY_ATTR) != null);

                String closedString = atts.getValue(CLOSED_ATTR);
                if (closedString != null) {
                    tempFeatureStyle.setClosed(Boolean.parseBoolean(closedString));
                }

                String labelKey = atts.getValue(LABEL_KEY_ATTR);
                if (labelKey != null) {
                    tempFeatureStyle.setLabelKey(labelKey);
                }

                String labelZoomLimitString = atts.getValue(LABEL_ZOOM_LIMIT_ATTR);
                if (labelZoomLimitString != null) {
                    tempFeatureStyle.setLabelZoomLimit(Integer.parseInt(labelZoomLimitString));
                }

                String iconPath = atts.getValue(ICON_PATH_ATTR);
                if (iconPath != null) {
                    tempFeatureStyle.setIconPath(iconPath);
                }

                validationCode = 0; // reset
                String codeString = atts.getValue(CODE_ATTR);
                if (codeString != null) {
                    validationCode = Integer.parseInt(codeString);
                }

                String textColorString = atts.getValue(TEXT_COLOR_ATTR);
                if (textColorString != null) {
                    tempFeatureStyle.setTextColor((int) Long.parseLong(textColorString, 16));
                }
            } else if (element.equals(DASH_ELEMENT)) {
                tempPhase = Float.parseFloat(atts.getValue(PHASE_ATTR));
                tempIntervals = new ArrayList<>();
            } else if (element.equals(INTERVAL_ELEMENT)) {
                tempIntervals.add(Float.parseFloat(atts.getValue(LENGTH_ATTR)));
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
     * Create a path for the "GPS" arrow
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

    /**
     * Create a path for the direction arrow
     * 
     * @param scale scaling factor
     */
    private void createDirectionArrowPath(float scale) {
        directionArrowPath = new Path();
        directionArrowPath.moveTo(Density.dpToPx(ctx, -20) * scale, 0);
        directionArrowPath.lineTo(-10, Density.dpToPx(ctx, +5) * scale);
        directionArrowPath.lineTo(-10, Density.dpToPx(ctx, -5) * scale);
        directionArrowPath.lineTo(Density.dpToPx(ctx, -20) * scale, 0);
    }

    @Override
    public void endElement(final String uri, final String element, final String qName) {
        if (element == null) {
            Log.i(DEBUG_TAG, "element is null");
            return;
        }
        if (element.equals(FEATURE_ELEMENT)) {
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
            case Node.NAME:
                if (tags == null) {
                    nodeStyles = tempFeatureStyle;
                    parent = nodeStyles;
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
            case VALIDATION:
                if (validationCode > 0) {
                    validationStyles.put(validationCode, tempFeatureStyle);
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
                    case Node.NAME:
                        parent = nodeStyles;
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
        } else if (element.equals(DASH_ELEMENT)) {
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
     * @throws ParserConfigurationException if something is wrong with the parser
     * @throws IOException if reading the InputStream fails
     * @throws SAXException if parsing fails
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
    @SuppressLint("NewApi")
    public static void getStylesFromFiles(@NonNull Context ctx) {
        if (availableStyles.size() == 0) {
            Log.i(DEBUG_TAG, "No style files found");
            // no files, need to install a default
            addDefaultStyle(ctx);
        }
        // assets directory
        AssetManager assetManager = ctx.getAssets();
        //
        try {
            String[] fileList = assetManager.list(Paths.DIRECTORY_PATH_STYLES);
            if (fileList != null) {
                for (String fn : fileList) {
                    if (fn.endsWith(Paths.FILE_EXTENSION_XML)) {
                        Log.i(DEBUG_TAG, "Creating style from file in assets directory " + fn);
                        try (InputStream is = assetManager.open(Paths.DIRECTORY_PATH_STYLES + Paths.DELIMITER + fn)) {
                            DataStyle p = new DataStyle(ctx, is, null);
                            availableStyles.put(p.getName(), p);
                        } catch (Exception ex) {
                            // this shouldn't happen with styles included with the APK, so no need to toast
                            Log.e(DEBUG_TAG, "Reading " + fn + " failed");
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Log.i(DEBUG_TAG, ex.toString());
        }
        // old style named files
        try {
            File indir = FileUtil.getPublicDirectory();
            class StyleFilter implements FilenameFilter {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(FILE_PATH_STYLE_SUFFIX);
                }
            }
            readStylesFromFileList(ctx, indir.listFiles(new StyleFilter()));
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Unable to read style files " + ex.getMessage());
        }
        // from public styles directory
        try {
            File indir = new File(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_STYLES);
            readStylesFromFileList(ctx, indir.listFiles(new XmlFileFilter()));
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Unable to read style files from public style dir " + ex.getMessage());
        }
        // from private styles directory
        try {
            File indir = FileUtil.getApplicationDirectory(ctx, Paths.DIRECTORY_PATH_STYLES);
            readStylesFromFileList(ctx, indir.listFiles(new XmlFileFilter()));
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Unable to read style files from private styles dir " + ex.getMessage());
        }
    }

    /**
     * Read styles provided as a list of Files, adding them to the available styles
     * 
     * @param ctx an Android Context
     * @param list the list
     */
    private static void readStylesFromFileList(@NonNull Context ctx, @Nullable File[] list) {
        if (list == null) {
            Log.w(DEBUG_TAG, "Null file list");
            return;
        }
        for (File f : list) {
            Log.i(DEBUG_TAG, "Creating profile from " + f.getName());
            try (InputStream is = new FileInputStream(f)) {
                DataStyle p = new DataStyle(ctx, is, f.getParent());
                // overwrites profile with same name
                availableStyles.put(p.getName(), p);
            } catch (Exception ex) { // never crash
                Log.e(DEBUG_TAG, ex.toString());
                ScreenMessage.toastTopError(ctx, ctx.getString(R.string.toast_invalid_style_file, f.getName(), ex.getMessage()));
            }
        }
    }

    /**
     * Add the builtin minimal style so that we always have something to fall back too
     * 
     * @param ctx an Android Context
     */
    private static void addDefaultStyle(@NonNull Context ctx) {
        DataStyle p = new DataStyle(ctx);
        p.name = BUILTIN_STYLE_NAME;
        currentStyle = p;
        availableStyles.put(p.getName(), p);
    }

    /**
     * Reset contents used for testing only
     */
    public static void reset() {
        availableStyles.clear();
        currentStyle = null;
    }

    /**
     * Get the name of the builtin style
     * 
     * @return the name of the builtin style
     */
    @NonNull
    public static String getBuiltinStyleName() {
        return BUILTIN_STYLE_NAME;
    }

    /**
     * @return the orientation_path
     */
    @NonNull
    public Path getOrientationPath() {
        return orientationPath;
    }

    /**
     * @return the orientation_path
     */
    @NonNull
    public Path getDirectionArrowPath() {
        return directionArrowPath;
    }

    /**
     * Get the Path for a specific Symbol
     * 
     * @param name the name
     * @return a Path, null if not found
     */
    @Nullable
    public Path getSymbol(@NonNull String name) {
        return Symbols.get(name);
    }

    /**
     * @return the crosshairs_path
     */
    @NonNull
    public Path getCrosshairsPath() {
        return crosshairsPath;
    }

    /**
     * @return the x_path
     */
    @NonNull
    public Path getXPath() {
        return xPath;
    }

    /**
     * If a directory is set for custom icons return it
     * 
     * @return the directory path or null
     */
    @Nullable
    public String getIconDirPath() {
        return iconDirPath;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the cached style for the element, or determine the style to use and cache it in the object
     * 
     * @param element the OsmElement we need the style for
     * @param <T> an OsmElement
     * @return the style
     */
    @NonNull
    public static <T extends OsmElement> FeatureStyle matchStyle(@NonNull final T element) {
        final boolean styleable = element instanceof StyleableFeature;
        FeatureStyle style = styleable ? ((StyleableFeature) element).getStyle() : null;
        if (style == null) {
            if (element instanceof Way) {
                style = matchRecursive(currentStyle.wayStyles, element.getTags(), ((Way) element).isClosed());
            } else if (element instanceof Node) {
                style = matchRecursive(currentStyle.nodeStyles, element.getTags(), false);
            } else {
                style = matchRecursive(currentStyle.relationStyles, element.getTags(), false);
            }
            if (styleable) {
                ((StyleableFeature) element).setStyle(style);
            }
        }
        return style;
    }

    /**
     * Recursively traverse the styles and try to find a match
     * 
     * @param style the style
     * @param tags tags from the element we are trying to match
     * @param closed true if the element is a way and closed
     * @return the best matching style
     */
    @NonNull
    private static FeatureStyle matchRecursive(@NonNull FeatureStyle style, @NonNull SortedMap<String, String> tags, boolean closed) {
        FeatureStyle result = style;
        if (style.cascadedStyles != null) {
            for (FeatureStyle s : style.cascadedStyles) {
                if ((s.closed == null || s.closed == closed) && s.match(tags)) {
                    return matchRecursive(s, tags, closed);
                }
            }
        }
        return result;
    }

    /**
     * Generate a taginfo project file for the current style
     * 
     * @param output File to write to
     * @return true if successful
     */
    public static boolean generateTaginfoJson(@NonNull File output) {
        MultiHashMap<String, String> tagMap = new MultiHashMap<>(true);
        addRecursive(tagMap, currentStyle.nodeStyles, "node");
        addRecursive(tagMap, currentStyle.wayStyles, "way");
        addRecursive(tagMap, currentStyle.relationStyles, "relation");

        try (FileOutputStream fout = new FileOutputStream(output); PrintStream outputStream = new PrintStream(new BufferedOutputStream(fout))) {
            Preset.tagInfoHeader(outputStream, "Vespucci map style", "https://raw.githubusercontent.com/MarcusWolschon/osmeditor4android/master/taginfo-style.json",
                    "Default map style for Vespucci. Nodes are rendered with the icons from the matching preset item.");
            outputStream.println("\"tags\":[");
            boolean firstTag = true;
            for (String tag : tagMap.getKeys()) {
                String[] keyValue = tag.split("=");
                final String key = keyValue[0];
                if (Util.isEmpty(key)) {
                    continue;
                }
                if (!firstTag) {
                    outputStream.println(",");
                }
                outputStream.print("{\"description\":\"Data rendering\",");
                outputStream
                        .print("\"key\": \"" + key + "\"" + (keyValue.length == 1 || "*".equals(keyValue[1]) ? "" : ",\"value\": \"" + keyValue[1] + "\""));
                outputStream.print(",\"object_types\": [");
                boolean firstGeometry = true;
                for (String geometry : tagMap.get(tag)) {
                    if (!firstGeometry) {
                        outputStream.print(",");
                    }
                    outputStream.print("\"" + geometry + "\"");
                    firstGeometry = false;
                }
                outputStream.print("]");
                firstTag = false;
                outputStream.print("}");
            }
            outputStream.print("]}");
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Export failed - " + output.getAbsolutePath() + " exception " + e);
            return false;
        }
        return true;
    }

    /**
     * Add tags from a style recursively to a map
     * 
     * @param tagMap the Map
     * @param style the input style
     * @param geometry the geometry value
     */
    private static void addRecursive(@NonNull MultiHashMap<String, String> tagMap, @NonNull FeatureStyle style, @NonNull String geometry) {
        if (style.isArea()) {
            geometry = "area";
        }
        for (Entry<String, String> entry : style.tags.entrySet()) {
            tagMap.add(entry.getKey() + "=" + entry.getValue(), geometry);
        }
        if (style.cascadedStyles != null) {
            for (FeatureStyle subStyle : style.cascadedStyles) {
                addRecursive(tagMap, subStyle, geometry);
            }
        }
    }
}
