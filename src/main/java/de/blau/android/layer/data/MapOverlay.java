package de.blau.android.layer.data;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.util.Winding.CLOCKWISE;
import static de.blau.android.util.Winding.COUNTERCLOCKWISE;
import static de.blau.android.util.Winding.winding;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pools.SimplePool;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.R;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.filter.Filter;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.Downloader;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.NonSerializeableLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.layer.UpdateInterface;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.PostMergeHandler;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.Server;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.APIEditorActivity;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetIconManager;
import de.blau.android.presets.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.Coordinates;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.Util;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.collections.LinkedList;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.LowAllocArrayList;
import de.blau.android.validation.Validator;
import de.blau.android.views.IMapView;

/**
 * OSM data layer
 * 
 * @author mb
 * @author Marcus Wolschon &lt;Marcus@Wolschon.biz&gt;
 * @author Simon Poole
 */

public class MapOverlay<O extends OsmElement> extends NonSerializeableLayer
        implements ExtentInterface, ConfigureInterface, LayerInfoInterface, PruneableInterface, UpdateInterface<O> {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final List<Integer> PAUSE_AUTO_DOWNLOAD = Collections.unmodifiableList(Arrays.asList(ErrorCodes.CORRUPTED_DATA, ErrorCodes.DATA_CONFLICT,
            ErrorCodes.OUT_OF_MEMORY, ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED, ErrorCodes.UNAVAILABLE));

    private static final int ICON_THREAD_POOL_SIZE = 2;
    public static final int  DATA_THREAD_POOL_SIZE = 3;

    public static final int  ICON_SIZE_DP         = 20;
    private static final int HOUSE_NUMBER_RADIUS  = 10;
    private static final int ICON_SELECTED_BORDER = 2;
    private static final int LABEL_EXTRA          = 40;

    private static final long AUTOPRUNE_MIN_INTERVAL       = 10000; // milli-seconds between
                                                                    // autoprunes
    public static final int   DEFAULT_AUTOPRUNE_NODE_LIMIT = 5000;
    public static final int   DEFAULT_DOWNLOADBOX_LIMIT    = 100;
    public static final int   DEFAULT_AUTO_DOWNLOAD_RADIUS = 50;
    public static final int   PAN_AND_ZOOM_LIMIT           = 17;
    private static final int  MP_SIZE_LIMIT                = 1000;  // max size of MP to render as

    /** half the width/height of a node icon in px */
    private final int iconRadius;
    private final int iconSelectedBorder;
    private final int houseNumberRadius;
    private final int verticalNumberOffset;
    private float     maxDownloadSpeed;
    private boolean   autoPruneEnabled     = false;
    private int       autoPruneNodeLimit   = DEFAULT_AUTOPRUNE_NODE_LIMIT; // node count for autoprune
    private int       autoDownloadBoxLimit = DEFAULT_DOWNLOADBOX_LIMIT;
    private int       panAndZoomLimit      = PAN_AND_ZOOM_LIMIT;

    private boolean wayNodeDragging;

    private final StorageDelegator delegator;
    private Context                context;
    private final Validator        validator;
    private final DataStyle        styles;

    /**
     * Preference related fields
     */
    private Preferences prefs;

    /**
     * show icons for POIs (in a wide sense of the word)
     */
    private boolean showIcons = false;

    /**
     * show icons for POIs tagged on (closed) ways
     */
    private boolean showWayIcons = false;

    /**
     * show tolerance area
     */
    private boolean showTolerance = true;

    /**
     * Download on pan and zoom
     */
    private boolean panAndZoomDownLoad = false;

    /**
     * Minimum side length for auto-download boxes
     */
    private int minDownloadSize = DEFAULT_AUTO_DOWNLOAD_RADIUS * 2;

    /**
     * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
     */
    private final WeakHashMap<java.util.Map<String, String>, Bitmap> iconCache = new WeakHashMap<>();

    /**
     * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags. This stores icons
     * for areas
     */
    private final WeakHashMap<java.util.Map<String, String>, Bitmap> areaIconCache = new WeakHashMap<>();

    /**
     * Stores strings that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
     */
    private final WeakHashMap<java.util.Map<String, String>, String> labelCache = new WeakHashMap<>();

    /**
     * Store direction values in degrees
     */
    private final WeakHashMap<java.util.Map<String, String>, Float> directionCache = new WeakHashMap<>();

    /**
     * Cache for any preset matching during one draw pass
     */
    private final HashMap<java.util.Map<String, String>, PresetItem> matchCache = new HashMap<>();

    /**
     * Stores custom icons
     */
    private final HashMap<String, BitmapDrawable> customIconCache = new HashMap<>();

    /** Caches if the map is zoomed into edit range during one onDraw pass */
    private boolean tmpDrawingInEditRange;

    /** Caches the edit mode during one onDraw pass */
    private Mode tmpDrawingEditMode;

    /** Caches the currently selected nodes during one onDraw pass */
    private List<Node> tmpDrawingSelectedNodes;

    /** Caches the currently selected ways during one onDraw pass */
    private List<Way> tmpDrawingSelectedWays;

    /** Caches the current "clickable elements" set during one onDraw pass */
    private Set<OsmElement> tmpClickableElements;

    /** used for highlighting relation members */
    private List<Way>  tmpDrawingSelectedRelationWays;
    private List<Node> tmpDrawingSelectedRelationNodes;

    /**
     * style for this drawing pass
     * 
     */
    private DataStyle currentStyle;

    /**
     * Locked or not
     */
    private boolean tmpLocked;

    /**
     * 
     */
    private ArrayList<Way> tmpStyledWays = new ArrayList<>();
    private ArrayList<Way> tmpHiddenWays = new ArrayList<>();

    /** Caches the preset during one onDraw pass */
    private Preset[] tmpPresets;

    /** Last validation color that we used */
    int nodeValidationColor = 0;

    /** Caches the Paint used for node tolerance */
    private Paint nodeTolerancePaint;
    private Paint nodeTolerancePaint2;

    /** Caches the Paint used for way tolerance */
    private Paint wayTolerancePaint;
    private Paint wayTolerancePaint2;

    private Paint nodeDragRadiusPaint;

    /** Cached Node FeatureStyles */
    private FeatureStyle nodeFeatureStyle;
    private FeatureStyle nodeFeatureStyleThin;
    private FeatureStyle nodeFeatureStyleTagged;
    private FeatureStyle nodeFeatureStyleSelected;
    private FeatureStyle nodeFeatureStyleThinSelected;
    private FeatureStyle nodeFeatureStyleTaggedSelected;
    private FeatureStyle nodeFeatureStyleRelation;
    private FeatureStyle nodeFeatureStyleThinRelation;
    private FeatureStyle nodeFeatureStyleTaggedRelation;
    private FeatureStyle nodeFeatureStyleFontRelation;
    private FeatureStyle nodeFeatureStyleFontSmallRelation;
    private FeatureStyle nodeFeatureStyleProblem;
    private FeatureStyle nodeFeatureStyleThinProblem;
    private FeatureStyle nodeFeatureStyleTaggedProblem;
    private FeatureStyle nodeFeatureStyleFontProblem;
    private FeatureStyle nodeFeatureStyleFontSmallProblem;
    private FeatureStyle nodeFeatureStyleHidden;

    private float nodeToleranceRadius;

    /** Cached Way FeatureStyles and Paints */
    private FeatureStyle selectedWayStyle;
    private Paint        wayDirectionPaint;
    private FeatureStyle wayFeatureStyleHidden;
    private FeatureStyle wayFeatureStyleRelation;
    private Paint        handlePaint;
    private FeatureStyle dontRenderWay;

    /** Cached label FeatureStyles */
    private FeatureStyle labelTextStyleNormal;
    private FeatureStyle labelTextStyleSmall;
    private FeatureStyle labelTextStyleNormalSelected;
    private FeatureStyle labelTextStyleSmallSelected;

    // other styling params
    private int showIconsLimit;
    private int showIconLabelZoomLimit;

    /** cached zoom level, calculated once per onDraw pass **/
    private int zoomLevel = 0;

    /** Cache the current filter **/
    private Filter tmpFilter = null;

    /** */
    private boolean inNodeIconZoomRange = false;

    /**
     * We just need one path object
     */
    private final Path        path       = new Path();
    private final Path        casingPath = new Path();
    private final PathMeasure pm         = new PathMeasure();

    private final LongHashSet handles = new LongHashSet();

    private Paint labelBackground;

    private float[][] coord = null;

    // allocate these just once
    private final FloatPrimitiveList points            = new FloatPrimitiveList(FloatPrimitiveList.MEDIUM_DEFAULT);
    private float[]                  offsettedCasing   = new float[100];
    private final List<Node>         nodesResult       = new LowAllocArrayList<>(1000);
    private final List<Way>          waysResult        = new LowAllocArrayList<>(1000);
    private final List<BoundingBox>  boundingBoxResult = new LowAllocArrayList<>();
    private final List<BoundingBox>  downloadedBoxes   = new LowAllocArrayList<>();
    private final ViewBox            viewBox           = new ViewBox();
    private final Coordinates        centroid          = new Coordinates(0, 0);

    /**
     * Stuff for multipolygon support Instantiate these objects just once
     */
    private final List<RelationMember>       waysOnly             = new LowAllocArrayList<>(100);
    private final LinkedList<RelationMember> tenpMultiPolygonSort = new LinkedList<>();
    private final List<List<Node>>           outerRings           = new LowAllocArrayList<>();
    private final List<List<Node>>           innerRings           = new LowAllocArrayList<>();
    private final List<List<Node>>           unknownRings         = new LowAllocArrayList<>();
    private final SimplePool<List<Node>>     ringPool             = new SimplePool<>(100);
    private final List<Node>                 areaNodes            = new LowAllocArrayList<>();   // reversing winding
                                                                                                 // and
    // assembling
    private final Set<Relation> paintRelations = new HashSet<>();

    private OnUpdateListener<O> onUpdateListener;

    /**
     * Runnable for downloading data
     */
    private final Downloader download;

    private final ThreadPoolExecutor dataThreadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(DATA_THREAD_POOL_SIZE);
    private final ThreadPoolExecutor iconThreadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(ICON_THREAD_POOL_SIZE);

    /**
     * Construct a new OSM data layer
     * 
     * @param map the current Map instance
     */
    @SuppressLint("NewApi")
    public MapOverlay(@NonNull final Map map) {
        Log.i(DEBUG_TAG, "creating data layer");
        this.map = map;
        context = map.getContext();
        prefs = map.getPrefs();
        validator = App.getDefaultValidator(context);
        download = new DataDownloader(context, prefs.getServer(), validator);

        styles = App.getDataStyle(context);

        iconRadius = Density.dpToPx(context, ICON_SIZE_DP / 2);
        houseNumberRadius = Density.dpToPx(context, HOUSE_NUMBER_RADIUS);
        verticalNumberOffset = Density.dpToPx(context, HOUSE_NUMBER_RADIUS / 2);
        iconSelectedBorder = Density.dpToPx(context, ICON_SELECTED_BORDER);

        delegator = App.getDelegator();

        updateStyle();
    }

    @Override
    public void onDestroy() {
        Util.shutDownThreadPool(dataThreadPoolExecutor);
        Util.shutDownThreadPool(iconThreadPoolExecutor);
        clearCaches();
        tmpPresets = null;
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    private class DataDownloader extends Downloader {

        final PostMergeHandler postMerge;

        /**
         * Construct a new instance
         * 
         * @param context an Android Context
         * @param server the current Server object
         * @param validator a Validator instance
         */
        public DataDownloader(@NonNull Context context, @NonNull Server server, @NonNull Validator validator) {
            super(server.getCachedCapabilities().getMaxArea());
            postMerge = (OsmElement e) -> e.hasProblem(context, validator);
        }

        @Override
        protected void download() {
            dataThreadPoolExecutor.execute(this::queueDownloadTasks);
            if (autoPruneEnabled && (System.currentTimeMillis() - lastAutoPrune) > AUTOPRUNE_MIN_INTERVAL) {
                try {
                    dataThreadPoolExecutor.execute(() -> {
                        if (delegator.reachedPruneLimits(autoPruneNodeLimit, autoDownloadBoxLimit)) {
                            MapOverlay.this.prune();
                            lastAutoPrune = System.currentTimeMillis();
                        }
                    });
                } catch (RejectedExecutionException rjee) {
                    Log.e(DEBUG_TAG, "Prune execution rejected " + rjee.getMessage());
                }
            }
        }

        /**
         * Determine the bounding boxes to download and queue the download tasks
         */
        private void queueDownloadTasks() {
            Context ctx = map.getContext();
            List<BoundingBox> bbList = new ArrayList<>(delegator.getBoundingBoxes());
            box.scale(1.2); // make sides 20% larger
            box.ensureMinumumSize(minDownloadSize); // enforce a minimum size
            List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, box);
            final Logic logic = App.getLogic();
            for (BoundingBox b : bboxes) {
                try {
                    dataThreadPoolExecutor.execute(() -> {
                        delegator.addBoundingBox(b);
                        AsyncResult result = logic.download(ctx, prefs.getServer(), b, postMerge, () -> {
                            logic.reselectRelationMembers();
                            map.postInvalidate();
                        }, true, true);
                        final int code = result.getCode();
                        if (PAUSE_AUTO_DOWNLOAD.contains(code)) {
                            prefs.setPanAndZoomAutoDownload(false);
                            setPrefs(prefs);
                            if (ctx instanceof FragmentActivity) {
                                new Handler(ctx.getMainLooper()).post(
                                        () -> ErrorAlert.showDialog(((FragmentActivity) ctx), code, ctx.getString(R.string.autodownload_has_been_paused)));
                            }
                        }
                    });
                } catch (RejectedExecutionException rjee) {
                    Log.e(DEBUG_TAG, "Download execution rejected " + rjee.getMessage());
                    logic.removeBoundingBox(b);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (!isVisible) {
            return;
        }
        zoomLevel = map.getZoomLevel();

        final Logic logic = App.getLogic();
        tmpDrawingEditMode = logic.getMode();
        tmpFilter = logic.getFilter();
        try {
            if (logic.tryLock()) {
                tmpDrawingSelectedNodes = logic.getSelectedNodes();
                tmpDrawingSelectedWays = logic.getSelectedWays();
                tmpClickableElements = logic.getClickableElements();
                tmpDrawingSelectedRelationWays = logic.getSelectedRelationWays();
                tmpDrawingSelectedRelationNodes = logic.getSelectedRelationNodes();
            }
        } finally {
            logic.unlock();
        }
        tmpPresets = App.getCurrentPresets(context);
        tmpLocked = logic.isUiLocked();

        currentStyle = styles.getCurrent();

        inNodeIconZoomRange = zoomLevel > currentStyle.getIconZoomLimit();

        viewBox.set(map.getViewBox());
        try {
            if (delegator.tryLock()) {
                delegator.getCurrentStorage().getBoundingBoxes(boundingBoxResult);
            } else {
                Log.w(DEBUG_TAG, "BoundingBoxes already locked, reusing existing data");
            }
        } finally {
            delegator.unlock();
        }
        downloadedBoxes.clear();
        for (BoundingBox box : boundingBoxResult) {
            if (box.intersects(viewBox)) {
                downloadedBoxes.add(box);
            }
        }

        Location location = map.getLocation();
        if (zoomLevel >= panAndZoomLimit && panAndZoomDownLoad && (location == null || location.getSpeed() < maxDownloadSpeed)) {
            map.getRootView().removeCallbacks(download);
            download.setBox(viewBox);
            map.getRootView().postDelayed(download, 100);
        }
        matchCache.clear();
        paintOsmData(canvas);
    }

    /**
     * Paints all OSM data on the given canvas.
     * 
     * @param canvas Canvas, where the data shall be painted on.
     */
    @SuppressWarnings("unchecked")
    private void paintOsmData(@NonNull final Canvas canvas) {

        boolean hwAccelerated = canvas.isHardwareAccelerated();

        int screenWidth = map.getWidth();
        int screenHeight = map.getHeight();

        // first find all nodes and ways that we need to display
        // if the delegator is locked re-render what we already have
        try {
            if (delegator.tryLock()) {
                nodesResult.clear();
                waysResult.clear();
                final Storage currentStorage = delegator.getCurrentStorage();
                currentStorage.getNodes(viewBox, nodesResult);
                currentStorage.getWays(viewBox, waysResult);
            } else {
                Log.w(DEBUG_TAG, "Delegator already locked, rerendering existing data");
            }
        } finally {
            delegator.unlock();
        }

        // the following should guarantee that if the selected node is off screen but the handle not, the handle gets
        // drawn, this isn't perfect because touch areas of other nodes just outside the screen still won't get drawn
        if (tmpDrawingSelectedNodes != null) {
            for (Node n : tmpDrawingSelectedNodes) {
                if (!nodesResult.contains(n)) {
                    nodesResult.add(n);
                }
            }
        }

        boolean filterMode = tmpFilter != null; // we have an active filter

        //
        tmpDrawingInEditRange = App.getLogic().isInEditZoomRange();

        boolean drawTolerance = tmpDrawingInEditRange // if we are not in editing range none of the further checks are
                                                      // necessary
                && !tmpLocked && (showTolerance || tmpDrawingEditMode.elementsSelectable());

        // Paint all ways

        List<Way> waysToDraw = waysResult;
        if (filterMode) {
            // initial filtering need to happen before relations are processed
            for (Node n : nodesResult) {
                tmpFilter.include(n, false);
            }
            /*
             * Split the ways in to those that we are going to show and those that we hide, rendering is far simpler for
             * the later
             */
            tmpHiddenWays.clear();
            tmpStyledWays.clear();
            for (Way w : waysResult) {
                if (tmpFilter.include(w, tmpDrawingInEditRange && tmpDrawingSelectedWays != null && tmpDrawingSelectedWays.contains(w))) {
                    tmpStyledWays.add(w);
                } else {
                    tmpHiddenWays.add(w);
                }
            }
            // draw hidden ways first
            for (Way w : tmpHiddenWays) {
                paintHiddenWay(canvas, w);
            }
            waysToDraw = tmpStyledWays;
        }

        // get relations for all nodes and ways
        paintRelations.clear();
        for (Node n : nodesResult) {
            addRelations(filterMode, n.getParentRelations(), paintRelations);
        }
        for (Way w : waysResult) {
            addRelations(filterMode, w.getParentRelations(), paintRelations);
        }

        // draw MPs first
        for (Relation rel : paintRelations) {
            String relType = rel.getTagWithKey(Tags.KEY_TYPE);
            if (Tags.VALUE_MULTIPOLYGON.equals(relType) || Tags.VALUE_BOUNDARY.equals(relType)) {
                paintMultiPolygon(canvas, rel);
            }
        }

        boolean displayHandles = tmpDrawingSelectedRelationWays == null && tmpDrawingSelectedRelationNodes == null && tmpDrawingEditMode.elementsGeomEditable();
        handles.clear();
        Collections.sort(waysToDraw, layerComparator);

        // ways now
        for (Way w : waysToDraw) {
            paintWay(canvas, w, screenWidth, screenHeight, displayHandles, drawTolerance);
        }

        // Paint nodes
        int coordSize = 0;
        float r = wayTolerancePaint.getStrokeWidth() / 2;
        float r2 = r * r;
        if (drawTolerance && (coord == null || coord.length < nodesResult.size())) {
            coord = new float[nodesResult.size()][2];
        }
        for (Node n : nodesResult) {
            boolean noTolerance = false;
            int lat = n.getLat();
            float y = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, lat);
            int lon = n.getLon();
            float x = GeoMath.lonE7ToX(screenWidth, viewBox, lon);
            if (drawTolerance) {
                // this reduces the number of tolerance fields drawn
                // while it rather expensive traversing the array is
                // still reasonably cheap
                if (coordSize != 0) {
                    for (int i = 0; i < coordSize; i++) {
                        float x1 = coord[i][0];
                        float y1 = coord[i][1];
                        float d2 = (x1 - x) * (x1 - x) + (y1 - y) * (y1 - y);
                        if (d2 < r2) {
                            noTolerance = true;
                            break;
                        }
                    }
                }
                if (!noTolerance) {
                    coord[coordSize][0] = x;
                    coord[coordSize][1] = y;
                    coordSize++;
                }
            }
            paintNode(canvas, n, x, y, hwAccelerated, drawTolerance && !noTolerance && (n.getState() != OsmElement.STATE_UNCHANGED || isInDownload(lon, lat)));
        }
        // turn restrictions
        if (inNodeIconZoomRange && showIcons) {
            for (Relation rel : paintRelations) {
                if (Tags.VALUE_RESTRICTION.equals(rel.getTagWithKey(Tags.KEY_TYPE))) {
                    paintRestriction(canvas, screenWidth, screenHeight, viewBox, rel);
                }
            }
        }

        paintHandles(canvas);

        if (onUpdateListener != null) {
            onUpdateListener.onUpdate((Collection<O>) nodesResult, (Collection<O>) waysToDraw, (Collection<O>) paintRelations);
        }
    }

    /**
     * Replacement for the method in StorageDelegator for performance reasons
     * 
     * @param lonE7 WGS84*19E7 longitude
     * @param latE7 WGS84*19E7 latitude
     * @return true if the coordinates are in one of the downloaded areas
     */
    private boolean isInDownload(int lonE7, int latE7) {
        for (BoundingBox bb : downloadedBoxes) {
            if (bb.isIn(lonE7, latE7)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add relations to list of relations to paint
     * 
     * @param filterMode if we need to use a filter
     * @param rels the new relations
     * @param toPaint List of relations to paint
     */
    private void addRelations(final boolean filterMode, @Nullable final List<Relation> rels, final @NonNull Set<Relation> toPaint) {
        if (rels != null) {
            if (!filterMode) {
                toPaint.addAll(rels);
            } else {
                for (Relation rel : rels) {
                    if (tmpFilter.include(rel, false)) {
                        toPaint.add(rel);
                    }
                }
            }
        }
    }

    static class LayerComparator implements Comparator<Way> {

        private BoundingBox box1 = new BoundingBox();
        private BoundingBox box2 = new BoundingBox();

        @Override
        public int compare(Way w1, Way w2) {
            int layer1 = 0;
            int layer2 = 0;
            String layer1Str = w1.getTagWithKey(Tags.KEY_LAYER);
            if (layer1Str != null) {
                try {
                    layer1 = Integer.parseInt(layer1Str);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            String layer2Str = w2.getTagWithKey(Tags.KEY_LAYER);
            if (layer2Str != null) {
                try {
                    layer2 = Integer.parseInt(layer2Str);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            if (layer2 != layer1) {
                return layer2 > layer1 ? -1 : +1;
            }
            boolean w2closed = w2.isClosed();
            if (w1.isClosed() != w2closed) {
                return w2closed ? 1 : -1;
            }
            if (!w2closed) {
                // both not closed
                return 0;
            }
            // both closed
            // note that testing for intersection will not work here as it will result in inconsistent ordering
            return Long.compare(w2.getBounds(box2).approxArea(), w1.getBounds(box1).approxArea());
        }
    }

    private LayerComparator layerComparator = new LayerComparator();

    /**
     * Draw a multipolygon
     * 
     * @param canvas the Canvas to draw on
     * @param rel the Relation for the multipolygon
     */
    private void paintMultiPolygon(@NonNull Canvas canvas, @NonNull Relation rel) {
        FeatureStyle style;
        if (rel.hasProblem(context, validator) != Validator.OK) {
            style = styles.getInternal(DataStyle.PROBLEM_WAY);
        } else {
            style = styles.matchStyle(rel);
        }

        if (zoomLevel < style.getMinVisibleZoom() || style.dontRender()) {
            return;
        }

        waysOnly.clear();
        tenpMultiPolygonSort.clear();

        // remove any non-Way non-downloaded members
        for (RelationMember m : rel.getMembers()) {
            if (m.downloaded() && Way.NAME.equals(m.getType())) {
                waysOnly.add(m);
            }
        }
        if (waysOnly.size() > MP_SIZE_LIMIT) { // protect against very large MPs
            return;
        }
        List<RelationMember> members = RelationUtils.sortRelationMembers(waysOnly, tenpMultiPolygonSort, RelationUtils::haveEndConnection);
        outerRings.clear();
        innerRings.clear();
        unknownRings.clear();
        List<Node> ring = getNewRing();

        int ms = members.size();
        String ringRole = "";
        for (int i = 0; i < ms; i++) {
            ringRole = "";
            RelationMember current = members.get(i);
            Way currentWay = (Way) current.getElement();
            String currentRole = current.getRole();
            if (currentRole != null && !"".equals(currentRole) && "".equals(ringRole)) {
                ringRole = currentRole;
            }
            if (currentWay != null) {
                // a bit of a hack to stop this way from being rendered as a way if it doesn't have any tags
                if (currentWay.getStyle() == null && !currentWay.hasTags() && !"".equals(ringRole)) {
                    currentWay.setStyle(dontRenderWay);
                }
                areaNodes.clear();
                areaNodes.addAll(currentWay.getNodes());
                int rs = ring.size();
                int ns = areaNodes.size();
                if (ring.isEmpty()) {
                    ring.addAll(areaNodes);
                } else if (ring.get(rs - 1).equals(areaNodes.get(0))) {
                    ring.addAll(areaNodes.subList(1, ns));
                } else if (ring.get(rs - 1).equals(areaNodes.get(ns - 1))) {
                    Collections.reverse(areaNodes);
                    ring.addAll(areaNodes.subList(1, ns));
                }
            }

            RelationMember next = members.get((i + 1) % ms);
            Way nextWay = (Way) next.getElement();
            Node lastRingNode = ring.get(ring.size() - 1);
            if (nextWay != null) {
                List<Node> nextNodes = nextWay.getNodes();
                int ns1 = nextNodes.size() - 1;
                if (!nextNodes.get(0).equals(lastRingNode) && !nextNodes.get(ns1).equals(lastRingNode)) {
                    Node firstRingNode = ring.get(0);
                    if (nextNodes.get(0).equals(firstRingNode) || nextNodes.get(ns1).equals(firstRingNode)) {
                        Collections.reverse(ring);
                        continue;
                    }
                    addRing(ringRole, ring);
                    ring = getNewRing();
                }
            }
        }
        if (!ring.isEmpty()) {
            addRing(ringRole, ring);
        } else {
            ringPool.release(ring);
        }

        Paint paint = style.getPaint();
        boolean closeRings = paint.getStyle() != Paint.Style.STROKE;

        outerRings.addAll(innerRings);
        outerRings.addAll(unknownRings);

        path.rewind();
        for (List<Node> r : outerRings) {
            map.pointListToLinePointsArray(points, r);
            float[] linePoints = points.getArray();
            int pointsSize = points.size();
            if (style.getOffset() != 0f) {
                Geometry.offset(linePoints, linePoints, pointsSize, r.get(0) == r.get(r.size() - 1), -style.getOffset());
            }
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i += 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            if (closeRings) {
                path.close();
            }
            ringPool.release(r);
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        if (tmpClickableElements != null && tmpClickableElements.contains(rel)) {
            canvas.drawPath(path, wayTolerancePaint2);
        }
        canvas.drawPath(path, paint);
    }

    /**
     * Get a new ring from the pool or instantiate one if the pool is empty
     * 
     * @return a List&lt;Node&gt;
     */
    @NonNull
    private List<Node> getNewRing() {
        List<Node> ring = ringPool.acquire();
        if (ring == null) {
            ring = new LowAllocArrayList<>();
        } else {
            ring.clear();
        }
        return ring;
    }

    /**
     * Add rings to the list depending on their role If the winding is wrong reverse the List
     * 
     * @param role the role of the the ring
     * @param ring the ring
     */
    private void addRing(@NonNull String role, @NonNull List<Node> ring) {
        final int winding = winding(ring);
        switch (role) {
        case Tags.ROLE_OUTER:
            if (winding == COUNTERCLOCKWISE) {
                Collections.reverse(ring);
            }
            outerRings.add(ring);
            break;
        case Tags.ROLE_INNER:
            if (winding == CLOCKWISE) {
                Collections.reverse(ring);
            }
            innerRings.add(ring);
            break;
        default:
            unknownRings.add(ring);
        }
    }

    /**
     * Draw an icon for a turn restriction
     * 
     * @param canvas the Canvas we are drawing on
     * @param screenWidth screen width in pixels
     * @param screenHeight screen height in pixels
     * @param viewBox the current ViewBox
     * @param restriction the Relation describing the restriction
     */
    private void paintRestriction(@NonNull final Canvas canvas, int screenWidth, int screenHeight, @NonNull ViewBox viewBox, @NonNull Relation restriction) {
        List<RelationMember> vias = restriction.getMembersWithRole(Tags.ROLE_VIA);
        for (RelationMember via : vias) {
            OsmElement v = via.getElement();
            if (v instanceof Node) {
                int lat = ((Node) v).getLat();
                int lon = ((Node) v).getLon();
                float y = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, lat);
                float x = GeoMath.lonE7ToX(screenWidth, viewBox, lon);
                List<RelationMember> froms = restriction.getMembersWithRole(Tags.ROLE_TO);
                RelationMember from = froms.isEmpty() ? null : froms.get(0);
                if (from != null) {
                    Way fromWay = (Way) from.getElement();
                    if (fromWay != null && from.getType().equals(Way.NAME)) {
                        int size = fromWay.getNodes().size();
                        if (size > 1) {
                            String type = restriction.getTagWithKey(Tags.VALUE_RESTRICTION);
                            int arrowDirection = 0;
                            if (type != null) {
                                final int offset = 2 * ICON_SIZE_DP;
                                switch (type) {
                                case Tags.VALUE_NO_RIGHT_TURN:
                                case Tags.VALUE_ONLY_RIGHT_TURN:
                                    arrowDirection = 90;
                                    y -= offset;
                                    x += offset;
                                    break;
                                case Tags.VALUE_NO_LEFT_TURN:
                                case Tags.VALUE_ONLY_LEFT_TURN:
                                    arrowDirection = -90;
                                    y += offset;
                                    x += offset;
                                    break;
                                case Tags.VALUE_NO_STRAIGHT_ON:
                                case Tags.VALUE_ONLY_STRAIGHT_ON:
                                    arrowDirection = 180;
                                    y -= offset;
                                    break;
                                default:
                                    // ignore
                                }
                            }
                            Node prevNode = fromWay.getNodes().get(1);
                            if (fromWay.getLastNode().equals(v)) {
                                prevNode = fromWay.getNodes().get(size - 2);
                            }
                            long bearing = (GeoMath.bearing(prevNode.getLon() / 1E7D, prevNode.getLat() / 1E7D, lon / 1E7D, lat / 1E7D) + arrowDirection) % 360;
                            canvas.save();
                            canvas.rotate(bearing, x, y);
                        } else {
                            from = null;
                        }
                    } else {
                        from = null;
                    }
                }
                paintNodeIcon(restriction, canvas, x, y, null);
                if (from != null) {
                    canvas.restore();
                }
            } else if (v instanceof Way) {
                Way viaWay = (Way) v;
                map.pointListToLinePointsArray(points, viaWay.getNodes());
                Coordinates m = Geometry.midpointFromPointlist(points.getArray(), points.size(), centroid);
                List<RelationMember> tos = restriction.getMembersWithRole(Tags.ROLE_TO);
                RelationMember to = tos.isEmpty() ? null : tos.get(0);
                if (to != null) {
                    Way toWay = (Way) to.getElement();
                    if (toWay != null && Way.NAME.equals(to.getType())) {
                        int size = viaWay.getNodes().size();
                        if (size > 1) {
                            int offset = 0;
                            if (!viaWay.hasNode(toWay.getFirstNode())) {
                                offset = 180;
                            }
                            long bearing = (GeoMath.bearing(viaWay.getFirstNode().getLon() / 1E7D, viaWay.getFirstNode().getLat() / 1E7D,
                                    viaWay.getLastNode().getLon() / 1E7D, viaWay.getLastNode().getLat() / 1E7D) + offset) % 360;
                            canvas.save();
                            canvas.rotate(bearing, (float) m.x, (float) m.y);
                        } else {
                            to = null;
                        }
                    } else {
                        to = null;
                    }
                }
                paintNodeIcon(restriction, canvas, (float) m.x, (float) m.y, null);
                if (to != null) {
                    canvas.restore();
                }
            }
        }
    }

    /**
     * Paints the given node on the canvas.
     * 
     * @param canvas Canvas, where the node shall be painted on.
     * @param node Node to be painted.
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param hwAccelerated use a workaround for unsupported operations when HW acceleration is used
     * @param drawTolerance draw the touch halo
     */
    private void paintNode(@NonNull final Canvas canvas, @NonNull final Node node, final float x, final float y, final boolean hwAccelerated,
            final boolean drawTolerance) {

        boolean isSelected = tmpDrawingSelectedNodes != null && tmpDrawingSelectedNodes.contains(node);

        boolean isTagged = node.isTagged();
        boolean hasProblem = false;

        boolean filteredObject = false;
        boolean filterMode = tmpFilter != null; // we have an active filter
        if (filterMode) {
            filteredObject = tmpFilter.include(node, isSelected);
        }

        // draw tolerance
        if (drawTolerance && (!filterMode || (filterMode && filteredObject))) {
            if (showTolerance && tmpClickableElements == null) {
                drawNodeTolerance(canvas, isTagged, x, y, nodeTolerancePaint);
            } else if (tmpClickableElements != null && tmpClickableElements.contains(node)) {
                drawNodeTolerance(canvas, isTagged, x, y, nodeTolerancePaint2);
            }
        }

        // general node style
        FeatureStyle featureStyle = nodeFeatureStyle;
        // style for house numbers
        FeatureStyle featureStyleThin = nodeFeatureStyleThin;
        // style for tagged nodes or otherwise important
        FeatureStyle featureStyleTagged = nodeFeatureStyleTagged;
        // style for label text
        FeatureStyle featureStyleFont = labelTextStyleNormal;
        // style for small label text
        FeatureStyle featureStyleFontSmall = labelTextStyleSmall;

        // node is selected
        if (tmpDrawingInEditRange && isSelected) {
            featureStyle = nodeFeatureStyleSelected;
            featureStyleThin = nodeFeatureStyleThinSelected;
            featureStyleTagged = nodeFeatureStyleTaggedSelected;
            featureStyleFont = labelTextStyleNormalSelected;
            featureStyleFontSmall = labelTextStyleSmallSelected;
            if (tmpDrawingSelectedNodes.size() == 1 && Util.isEmpty(tmpDrawingSelectedWays) && prefs.largeDragArea()
                    && tmpDrawingEditMode.elementsGeomEditable()) {
                // don't draw large areas in multi-select mode
                canvas.drawCircle(x, y, currentStyle.getLargDragToleranceRadius(), nodeDragRadiusPaint);
            } else {
                canvas.drawCircle(x, y, currentStyle.getNodeToleranceValue(), nodeDragRadiusPaint);
            }
        }

        if (node.hasProblem(context, validator) != Validator.OK) {
            featureStyle = nodeFeatureStyleProblem;
            featureStyleThin = nodeFeatureStyleThinProblem;
            featureStyleTagged = nodeFeatureStyleTaggedProblem;
            featureStyleFont = nodeFeatureStyleFontProblem;
            featureStyleFontSmall = nodeFeatureStyleFontSmallProblem;
            int validationColor = styles.getValidationStyle(node.getCachedProblems()).getPaint().getColor();
            if (validationColor != nodeValidationColor) {
                // this is a bit of an improvement over always setting the color
                featureStyle.setColor(validationColor);
                featureStyleThin.setColor(validationColor);
                featureStyleThin.setColor(validationColor);
                featureStyleTagged.setColor(validationColor);
                featureStyleFont.setColor(validationColor);
                featureStyleFontSmall.setColor(validationColor);
                nodeValidationColor = validationColor;
            }
            hasProblem = true;
        }

        // relation member highlighting needs to overrule validation
        if (tmpDrawingInEditRange && tmpDrawingSelectedRelationNodes != null && tmpDrawingSelectedRelationNodes.contains(node)) {
            featureStyle = nodeFeatureStyleRelation;
            featureStyleThin = nodeFeatureStyleThinRelation;
            featureStyleTagged = nodeFeatureStyleTaggedRelation;
            featureStyleFont = nodeFeatureStyleFontRelation;
            featureStyleFontSmall = nodeFeatureStyleFontSmallRelation;
            isSelected = true;
        }

        if (filterMode && !filteredObject) {
            featureStyle = nodeFeatureStyleHidden;
            featureStyleThin = featureStyle;
            featureStyleTagged = featureStyle;
            isTagged = false;
        }

        if (!isTagged) {
            // this bit of code duplication makes sense
            if (zoomLevel < featureStyle.getMinVisibleZoom()) {
                return;
            }
            Paint paint = featureStyle.getPaint();
            if (hwAccelerated) { // we don't actually know if this is slower than drawPoint
                canvas.drawCircle(x, y, paint.getStrokeWidth() / 2, paint);
            } else {
                canvas.drawPoint(x, y, paint);
            }
            return;
        }

        boolean noIcon = true;
        if (inNodeIconZoomRange && showIcons) {
            Float direction = getDirection(node);
            if (direction != null && !direction.equals(Float.NaN)) {
                canvas.save();
                canvas.rotate(direction + 90, x, y);
                canvas.translate(x - iconRadius, y);
                canvas.drawPath(currentStyle.getDirectionArrowPath(), nodeFeatureStyle.getPaint());
                canvas.restore();
            }

            noIcon = tmpPresets == null || !paintNodeIcon(node, canvas, x, y, isSelected || hasProblem ? featureStyleTagged : null);
            if (noIcon) {
                String houseNumber = node.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
                if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
                    paintHouseNumber(x, y, canvas, featureStyleThin, featureStyleFontSmall, houseNumber);
                    return;
                }
            } else if (zoomLevel > showIconLabelZoomLimit) {
                paintLabel(x, y, canvas, featureStyleFont, node, nodeFeatureStyleTagged.getPaint().getStrokeWidth(), true);
            }
        }

        if (noIcon) {
            // draw regular nodes or without icons
            if (zoomLevel < featureStyleTagged.getMinVisibleZoom()) {
                return;
            }
            Paint paint = featureStyleTagged.getPaint();
            float strokeWidth = paint.getStrokeWidth();
            if (hwAccelerated) {
                canvas.drawCircle(x, y, strokeWidth / 2, paint);
            } else {
                canvas.drawPoint(x, y, paint);
            }
            if (inNodeIconZoomRange) {
                paintLabel(x, y, canvas, featureStyleFont, node, strokeWidth, false);
            }
        }
    }

    /**
     * Get the value of any direction (like) tag
     * 
     * @param node the Node
     * @return the direction as a float if found, otherwise Float.NaN
     */
    @NonNull
    private Float getDirection(@NonNull final Node node) {
        Float direction = node.getFromCache(directionCache);
        if (direction == null) {
            direction = Float.NaN;
            PresetItem match = getMatch(node);
            if (match != null) {
                String key = Tags.getDirectionKey(match, node);
                if (key != null) {
                    direction = Tags.parseDirection(node.getTagWithKey(key));
                }
            }
            synchronized (directionCache) {
                node.addToCache(directionCache, direction);
            }
        }
        return direction;
    }

    /**
     * Get the best preset match for an element and cache it during this draw pass
     * 
     * @param e the OsmElement
     * @return a PresetItem or null
     */
    @Nullable
    private PresetItem getMatch(@NonNull OsmElement e) {
        java.util.Map<String, String> tags = e.getTags();
        PresetItem match = matchCache.get(tags);
        if (match == null && !matchCache.containsKey(tags)) {
            match = Preset.findBestMatch(context, tmpPresets, e.getTags(), null, e, false);
            matchCache.put(tags, match);
        }
        return match;
    }

    /**
     * Draw a circle with center at x,y with the house number in it
     * 
     * @param x screen x
     * @param y screen y
     * @param canvas canvas we are drawing on
     * @param featureStyleThin style to use for the housenumber circle
     * @param featureStyleFont style to use for the housenumber number
     * @param houseNumber the number as a string
     */
    private void paintHouseNumber(final float x, final float y, @NonNull final Canvas canvas, @NonNull final FeatureStyle featureStyleThin,
            @NonNull final FeatureStyle featureStyleFont, final String houseNumber) {
        Paint fontPaint = featureStyleFont.getPaint();
        canvas.drawCircle(x, y, houseNumberRadius, featureStyleThin.getPaint());
        canvas.drawCircle(x, y, houseNumberRadius, labelBackground);
        canvas.drawText(houseNumber, x - fontPaint.measureText(houseNumber) / 2, y + verticalNumberOffset, fontPaint);
    }

    /**
     * Paint a label under the node, does not try to do collision avoidance
     * 
     * @param x screen x
     * @param y screen y
     * @param canvas canvas we are drawing on
     * @param labelStyle style to use for the label
     * @param e the OsmElement
     * @param strokeWidth current stroke scaling factor
     * @param withIcon offset the label so that we don't overlap an icon
     */
    private void paintLabel(final float x, final float y, @NonNull final Canvas canvas, @NonNull final FeatureStyle labelStyle, @NonNull final OsmElement e,
            final float strokeWidth, final boolean withIcon) {
        String label = e.getFromCache(labelCache); // may be null!
        if (label == null) {
            if (e.isInCache(labelCache)) {
                return;
            }
            FeatureStyle style = styles.matchStyle(e);
            if (style.usePresetLabel() && tmpPresets != null) {
                PresetItem match = getMatch(e);
                if (match != null) {
                    String template = e.nameFromTemplate(context, match);
                    label = template != null ? template : match.getTranslatedName();
                } else {
                    label = e.getPrimaryTag(context);
                }
            } else {
                String labelKey = style.getLabelKey();
                label = labelKey != null ? e.getTagWithKey(labelKey) : null;
            }
            synchronized (labelCache) {
                e.addToCache(labelCache, label);
                if (label == null) {
                    return;
                }
            }
        }
        // draw the label
        Paint paint = labelStyle.getPaint();
        float halfTextWidth = paint.measureText(label) / 2;
        FontMetrics fm = labelStyle.getFontMetrics();
        float yOffset = y + strokeWidth + (withIcon ? 2 * iconRadius : iconRadius);
        canvas.drawRect(x - halfTextWidth, yOffset + fm.bottom, x + halfTextWidth, yOffset - paint.getTextSize() + fm.bottom, labelBackground);
        canvas.drawText(label, x - halfTextWidth, yOffset, paint);
    }

    /**
     * Dummy bitmap for the cache
     */
    public static final Bitmap NOICON = Bitmap.createBitmap(2, 2, Config.ARGB_8888);

    /**
     * Get icon for the element
     * 
     * Asynchronously read if it isn't in the cache
     *
     * @param element element we want to find an icon for
     * @return icon or null if none is found
     */
    @Nullable
    public Bitmap getIcon(@NonNull OsmElement element) {
        boolean isWay = element instanceof Way;
        WeakHashMap<java.util.Map<String, String>, Bitmap> tempCache = isWay ? areaIconCache : iconCache;
        Bitmap icon = element.getFromCache(tempCache); // may be null!
        if (icon == null) {
            try {
                iconThreadPoolExecutor.execute(() -> retrieveIcon(element, isWay, tempCache));
            } catch (RejectedExecutionException rjee) {
                Log.e(DEBUG_TAG, "Icon download execution rejected " + rjee.getMessage());
            }
        }
        return icon != NOICON ? icon : null;
    }

    /**
     * Retrieve an icon and put it in the cache for the specific element
     * 
     * @param element the OsmElement
     * @param isWay if the element is a Way
     * @param cache the relevant cache
     */
    @TargetApi(26)
    private void retrieveIcon(@NonNull OsmElement element, boolean isWay, @NonNull WeakHashMap<java.util.Map<String, String>, Bitmap> cache) {
        BitmapDrawable iconDrawable = null;

        // icon not cached, ask the preset/style, render to a bitmap and cache result
        FeatureStyle style = styles.matchStyle(element);
        String iconPath = style.getIconPath();
        boolean usePresetIcon = style.usePresetIcon();

        if (iconPath != null && !usePresetIcon) {
            if (!"".equals(iconPath)) { // empty icon path suppresses icons
                iconDrawable = retrieveCustomIcon(iconPath);
            }
        } else if (tmpPresets != null) {
            PresetItem match = !isWay || usePresetIcon ? getMatch(element) : null;
            if (match != null) {
                iconDrawable = match.getMapIcon(context);
            }
        }
        Bitmap icon = NOICON;
        if (iconDrawable != null) {
            icon = Bitmap.createBitmap(iconRadius * 2, iconRadius * 2, Bitmap.Config.ARGB_8888);
            iconDrawable.draw(new Canvas(icon));
            if (map.isHardwareLayerType()) {
                Bitmap temp = icon;
                icon = temp.copy(Bitmap.Config.HARDWARE, false);
                temp.recycle();
            }
        }
        element.addToCache(cache, icon);
        map.postInvalidate();
    }

    /**
     * Get a custom icon
     * 
     * @param iconPath configured icon path
     */
    @Nullable
    public BitmapDrawable retrieveCustomIcon(String iconPath) {
        BitmapDrawable iconDrawable = customIconCache.get(iconPath); // NOSONAR computeIfAbsent doesn't exist prior to
                                                                     // Android 7
        if (iconDrawable != null || customIconCache.containsKey(iconPath)) {
            return iconDrawable;
        }
        String iconDirPath = currentStyle.getIconDirPath();
        try (InputStream stream = (iconDirPath != null ? new FileInputStream(iconPath) : context.getAssets().open(iconPath))) {
            iconDrawable = PresetIconManager.bitmapDrawableFromStream(context, ICON_SIZE_DP, stream, PresetIconManager.isSvg(iconPath));
            customIconCache.put(iconPath, iconDrawable);
            return iconDrawable;
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "Icon " + iconPath + " not found or can't be parsed " + e.getMessage());
        }

        // search in all presets
        for (Preset preset : App.getCurrentPresets(context)) {
            if (preset != null) {
                PresetIconManager iconManager = preset.getIconManager(context);
                iconDrawable = iconManager.getDrawable(iconPath, ICON_SIZE_DP);
                if (iconDrawable != null) {
                    customIconCache.put(iconPath, iconDrawable);
                    break;
                }
            }
        }
        if (iconDrawable == null) {
            Log.w(DEBUG_TAG, "Icon " + iconPath + " not found");
            customIconCache.put(iconPath, null);
        }
        return iconDrawable;
    }

    /**
     * Remove everything from all caches
     */
    public void clearCaches() {
        synchronized (iconCache) {
            iconCache.clear();
        }
        synchronized (areaIconCache) {
            areaIconCache.clear();
        }
        synchronized (labelCache) {
            labelCache.clear();
        }
        synchronized (customIconCache) {
            customIconCache.clear();
        }
        synchronized (directionCache) {
            directionCache.clear();
        }
    }

    /**
     * Paints an icon for an element. tmpPreset needs to be available (i.e. not null).
     * 
     * @param element the element whose icon should be painted
     * @param canvas the canvas on which to draw
     * @param x the x position where the center of the icon goes
     * @param y the y position where the center of the icon goes
     * @param highlightStyle highlight style key or null
     * @return true if an icon was found and drawn
     */
    private boolean paintNodeIcon(@NonNull OsmElement element, @NonNull Canvas canvas, float x, float y, @Nullable FeatureStyle highlightStyle) {
        Bitmap icon = getIcon(element);
        if (icon != null) {
            float w2 = icon.getWidth() / 2f;
            float h2 = icon.getHeight() / 2f;
            if (highlightStyle != null) { // selected or error
                RectF r = new RectF(x - w2 - iconSelectedBorder, y - h2 - iconSelectedBorder, x + w2 + iconSelectedBorder, y + h2 + iconSelectedBorder);
                canvas.drawRoundRect(r, iconSelectedBorder, iconSelectedBorder, highlightStyle.getPaint());
            }
            // we have an icon! draw it.
            canvas.drawBitmap(icon, x - w2, y - h2, null);
            return true;
        }
        return false;
    }

    /**
     * Paint the tolerance halo for a node
     * 
     * @param canvas the canvas we are drawing on
     * @param isTagged true if the node has any tags
     * @param x screen x
     * @param y screen y
     * @param paint the parameters to use for the colour
     */
    private void drawNodeTolerance(@NonNull final Canvas canvas, final boolean isTagged, final float x, final float y, @NonNull final Paint paint) {
        canvas.drawCircle(x, y, isTagged ? paint.getStrokeWidth() : nodeToleranceRadius, paint);
    }

    /**
     * Paints the given way on the canvas.
     * 
     * @param canvas Canvas, where the node shall be painted on.
     * @param way way which shall be painted.
     * @param screenWidth the width of the screen in pixels
     * @param screenHeinght the height of the screen in pixels
     * @param displayHandles draw geometry improvement handles
     * @param drawTolerance if true draw the halo
     */
    private void paintWay(@NonNull final Canvas canvas, @NonNull final Way way, int screenWidth, int screenHeight, final boolean displayHandles,
            boolean drawTolerance) {

        FeatureStyle style = styles.matchStyle(way);

        boolean isSelected = tmpDrawingInEditRange // if we are not in editing range don't show selected way ... may be
                                                   // a better idea to do so
                && tmpDrawingSelectedWays != null && tmpDrawingSelectedWays.contains(way);
        boolean isMemberOfSelectedRelation = tmpDrawingInEditRange && tmpDrawingSelectedRelationWays != null && tmpDrawingSelectedRelationWays.contains(way);
        boolean isClickable = tmpClickableElements != null && tmpClickableElements.contains(way);

        if (zoomLevel < style.getMinVisibleZoom() || (style.dontRender() && !(isSelected || isMemberOfSelectedRelation || isClickable))) {
            return;
        }

        final boolean closed = way.isClosed();
        List<Node> nodes = way.getNodes();
        boolean reversed = false; // way arrows need to be drawn reversed if we reverse the direction of the way
        if (style.isArea() && winding(nodes) == COUNTERCLOCKWISE) {
            areaNodes.clear();
            areaNodes.addAll(nodes);
            Collections.reverse(areaNodes);
            map.pointListToLinePointsArray(points, areaNodes);
            reversed = true;
        } else {
            map.pointListToLinePointsArray(points, nodes);
        }

        float[] linePoints = points.getArray();
        int pointsSize = points.size();

        if (style.getOffset() != 0f) {
            Geometry.offset(linePoints, linePoints, pointsSize, closed, -style.getOffset());
        }

        Paint paint;
        FeatureStyle labelFontStyle = labelTextStyleNormal;
        FeatureStyle labelFontStyleSmall = labelTextStyleSmall;

        // draw way tolerance
        if (drawTolerance) {
            if (showTolerance && tmpClickableElements == null) {
                canvas.drawLines(linePoints, 0, pointsSize, wayTolerancePaint);
            } else if (isClickable) {
                canvas.drawLines(linePoints, 0, pointsSize, wayTolerancePaint2);
            }
        }

        // draw selectedWay highlighting
        if (isSelected) {
            paint = selectedWayStyle.getPaint();
            paint.setStrokeWidth(style.getPaint().getStrokeWidth() * selectedWayStyle.getWidthFactor());
            canvas.drawLines(linePoints, 0, pointsSize, paint);
            drawWayArrows(canvas, linePoints, pointsSize, screenWidth, screenHeight, reversed, wayDirectionPaint,
                    displayHandles && !tmpDrawingSelectedWays.isEmpty());
            labelFontStyle = labelTextStyleNormalSelected;
            labelFontStyleSmall = labelTextStyleSmallSelected;
            // visual feedback if way nodes are dragable
            if (wayNodeDragging && context instanceof Main && ((Main) context).getEasyEditManager().inWaySelectedMode()) {
                for (int i = 0; i < pointsSize; i += 4) {
                    canvas.drawCircle(linePoints[i], linePoints[i + 1], nodeToleranceRadius, nodeDragRadiusPaint);
                }
                if (!closed) {
                    canvas.drawCircle(linePoints[pointsSize - 2], linePoints[pointsSize - 1], nodeToleranceRadius, nodeDragRadiusPaint);
                }
            }
        } else if (isMemberOfSelectedRelation) {
            paint = wayFeatureStyleRelation.getPaint();
            paint.setStrokeWidth(style.getPaint().getStrokeWidth() * wayFeatureStyleRelation.getWidthFactor());
            canvas.drawLines(linePoints, 0, pointsSize, paint);
        } else if (way.hasProblem(context, validator) != Validator.OK) {
            FeatureStyle problemStyle = styles.getValidationStyle(way.getCachedProblems());
            FeatureStyle casingStyle = style.getCasingStyle();
            float strokeWidth = (casingStyle != null && casingStyle.getOffset() == 0 ? casingStyle.getPaint().getStrokeWidth()
                    : style.getPaint().getStrokeWidth()) * problemStyle.getWidthFactor();
            problemStyle.setStrokeWidth(strokeWidth);
            canvas.drawLines(linePoints, 0, pointsSize, problemStyle.getPaint());
        }

        FeatureStyle arrowStyle = style.getArrowStyle();
        if (arrowStyle != null) {
            if (arrowStyle.checkOneway()) {
                int onewayCode = way.getOneway();
                if (onewayCode != 0) {
                    drawWayArrows(canvas, linePoints, pointsSize, screenWidth, screenHeight, (onewayCode == -1), arrowStyle.getPaint(), false);
                }
            } else {
                drawWayArrows(canvas, linePoints, pointsSize, screenWidth, screenHeight, false, arrowStyle.getPaint(), false);
            }
        }

        // draw the way itself
        if (pointsSize > 2) {
            setupPath(linePoints, pointsSize, path);
            FeatureStyle casingStyle = style.getCasingStyle();
            if (casingStyle != null) {
                if (casingStyle.getOffset() != 0f) {
                    if (offsettedCasing.length < pointsSize) {
                        offsettedCasing = new float[pointsSize * 2];
                    }
                    Geometry.offset(linePoints, offsettedCasing, pointsSize, closed, -casingStyle.getOffset());
                    setupPath(offsettedCasing, pointsSize, casingPath);
                    canvas.drawPath(casingPath, casingStyle.getPaint());
                } else {
                    canvas.drawPath(path, casingStyle.getPaint());
                }
            }
            canvas.drawPath(path, style.getPaint());
        }

        if (closed) {
            // display icons on closed ways
            if (showIcons && showWayIcons && zoomLevel > showIconsLimit) {
                if (pointsSize < nodes.size() * 2) {
                    return;
                }
                Coordinates c = Geometry.centroidFromPointlist(linePoints, pointsSize, centroid);
                if (c != null) {
                    boolean iconDrawn = false;
                    final float x = (float) c.x;
                    final float y = (float) c.y;
                    if (tmpPresets != null) {
                        iconDrawn = paintNodeIcon(way, canvas, x, y, isSelected ? nodeFeatureStyleTaggedSelected : null);
                        if (!iconDrawn) {
                            String houseNumber = way.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
                            if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
                                paintHouseNumber(x, y, canvas, isSelected ? nodeFeatureStyleThinSelected : nodeFeatureStyleThin, labelFontStyleSmall,
                                        houseNumber);
                                return;
                            }
                        }
                    }
                    if (zoomLevel >= showIconLabelZoomLimit && style.getLabelKey() != null) {
                        Paint p = nodeFeatureStyleTaggedSelected.getPaint();
                        paintLabel(x, y, canvas, labelFontStyle, way, iconDrawn ? p.getStrokeWidth() : 0, iconDrawn);
                    }
                }
            }
        } else if (zoomLevel >= style.getLabelZoomLimit()) {
            String label = getLabel(way, style);
            if (label != null) {
                for (int i = 0; i < pointsSize; i = i + 4) {
                    path.rewind();
                    final float startX = linePoints[i];
                    final float endX = linePoints[i + 2];
                    if (startX > endX) {
                        path.moveTo(endX, linePoints[i + 3]);
                        path.lineTo(startX, linePoints[i + 1]);
                    } else {
                        path.moveTo(startX, linePoints[i + 1]);
                        path.lineTo(endX, linePoints[i + 3]);
                    }
                    float labelWidth = labelFontStyle.getPaint().measureText(label);
                    pm.setPath(path, false); // path is still correct here
                    int repeat = Math.round(pm.getLength() / (2 * (labelWidth + LABEL_EXTRA)));
                    if (repeat > 0) {
                        FontMetrics fm = labelFontStyle.getPaint().getFontMetrics();
                        float offset = (fm.top + fm.bottom) / 2;
                        float hInc = pm.getLength() / repeat;
                        float hOffset = (hInc - labelWidth) / 2;
                        for (int j = 0; j < repeat; j++) {
                            canvas.drawTextOnPath(label, path, hOffset, -offset, labelFontStyle.getPaint());
                            hOffset += hInc;
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup a Path from a float array in drawLine format
     * 
     * @param linePoints the array
     * @param pointsSize the number of elements in use
     * @param path the Path
     * 
     */
    private void setupPath(@NonNull float[] linePoints, int pointsSize, @NonNull Path path) {
        path.rewind();
        path.moveTo(linePoints[0], linePoints[1]);
        for (int i = 0; i < pointsSize; i += 4) {
            path.lineTo(linePoints[i + 2], linePoints[i + 3]);
        }
    }

    /**
     * Get a label for a way
     * 
     * @param way the Way
     * @param style the FeatureStyle
     * @return the label or null
     */
    @Nullable
    private String getLabel(@NonNull final Way way, @NonNull FeatureStyle style) {
        String labelKey = style.getLabelKey() != null ? style.getLabelKey() : Tags.KEY_NAME;
        return way.getTagWithKey(labelKey);
    }

    /**
     * Paints the given way on the canvas with the "hidden" style.
     * 
     * @param canvas Canvas, where the node shall be painted on.
     * @param way way which shall be painted.
     */
    private void paintHiddenWay(@NonNull final Canvas canvas, @NonNull final Way way) {
        //
        if (zoomLevel < wayFeatureStyleHidden.getMinVisibleZoom()) {
            return;
        }

        map.pointListToLinePointsArray(points, way.getNodes());
        float[] linePoints = points.getArray();
        int pointsSize = points.size();

        // draw the way itself
        // this doesn't work properly with HW acceleration: canvas.drawLines(linePoints, fp.getPaint()); NOSONAR
        if (pointsSize > 2) {
            path.rewind();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            canvas.drawPath(path, wayFeatureStyleHidden.getPaint());
        }
    }

    /**
     * Draw geometry improvement handles
     * 
     * @param canvas the Canvas we are drawing on
     */
    private void paintHandles(@NonNull Canvas canvas) {
        if (!handles.isEmpty()) {
            final Path xPath = currentStyle.getXPath();
            canvas.save();
            float lastX = 0;
            float lastY = 0;
            for (long l : handles.values()) {
                // draw handle
                float x = Float.intBitsToFloat((int) (l >>> 32));
                float y = Float.intBitsToFloat((int) (l));
                canvas.translate(x - lastX, y - lastY);
                lastX = x;
                lastY = y;
                canvas.drawPath(xPath, handlePaint);
            }
            canvas.restore();
        }
    }

    /**
     * Draws directional arrows for a way
     * 
     * @param canvas the canvas on which to draw
     * @param linePoints line segment array in the format returned by {@link #pointListToLinePointsArray(Iterable)}.
     * @param linePointsSize number of valid entries in linePoints
     * @param screenWidth the width of the screen in pixels
     * @param screenHeinght the height of the screen in pixels
     * @param reverse if true, the arrows will be painted in the reverse direction
     * @param paint the paint to use for drawing the arrows
     * @param addHandles if true draw arrows at 1/4 and 3/4 of the length and save the middle pos. for drawing a handle
     */
    private void drawWayArrows(@NonNull Canvas canvas, float[] linePoints, int linePointsSize, int screenWidth, int screenHeight, boolean reverse,
            @NonNull Paint paint, boolean addHandles) {
        double minLen = currentStyle.getMinLenForHandle();
        final float nodeToleranceValue = currentStyle.getNodeToleranceValue();
        int ptr = 0;
        while (ptr < linePointsSize) {

            float x1 = linePoints[ptr++];
            float y1 = linePoints[ptr++];
            float x2 = linePoints[ptr++];
            float y2 = linePoints[ptr++];

            float xDelta = x2 - x1;
            float yDelta = y2 - y1;

            float xDelta2 = xDelta / 2;
            float yDelta2 = yDelta / 2;

            float x = x1 + xDelta2;
            float y = y1 + yDelta2;

            boolean inViewbox = x > 0 && x < screenWidth && y > 0 && y < screenHeight;

            boolean secondArrow = false;
            if (inViewbox) {
                if (addHandles) {
                    double len = Math.hypot(xDelta, yDelta);
                    if (len > minLen) {
                        handles.put(((long) (Float.floatToRawIntBits(x)) << 32) + Float.floatToRawIntBits(y));
                        // add arrows at 1/4 and 3/4 along the segment
                        secondArrow = true;
                        xDelta2 = xDelta2 / 2;
                        yDelta2 = yDelta2 / 2;
                    }
                }
            } else {
                // clip to box and draw arrow on remaining part
                // extracting this in to a method would require allocating an array,
                // doesn't make sense
                float a = yDelta / xDelta;
                float m = y1 - x1 * a;
                if (y1 < 0) {
                    x1 = -m / a;
                    y1 = 0;
                } else if (y1 > screenHeight) {
                    y1 = screenHeight;
                    x1 = (y1 - m) / a;
                }
                if (y2 < 0) {
                    x2 = -m / a;
                    y2 = 0;
                } else if (y2 > screenHeight) {
                    y2 = screenHeight;
                    x2 = (y2 - m) / a;
                }
                if (x1 < 0) {
                    y1 = m;
                    x1 = 0;
                } else if (x1 > screenWidth) {
                    x1 = screenWidth;
                    y1 = m + a * x1;
                }
                if (x2 < 0) {
                    y2 = m;
                    x2 = 0;
                } else if (x2 > screenWidth) {
                    x2 = screenWidth;
                    y2 = m + a * x2;
                }
                //
                xDelta = x2 - x1;
                yDelta = y2 - y1;
                double len = Math.hypot(xDelta, yDelta);
                if (len < nodeToleranceValue) {
                    continue;
                }
                xDelta2 = xDelta / 2;
                yDelta2 = yDelta / 2;
            }
            x = x1 + xDelta2;
            y = y1 + yDelta2;

            float angle = (float) (Math.atan2(yDelta, xDelta) * 180 / Math.PI);
            angle = reverse ? angle - 180 : angle;
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(angle);
            canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
            canvas.restore();
            if (secondArrow) {
                canvas.save();
                canvas.translate(x + 2 * xDelta2, y + 2 * yDelta2);
                canvas.rotate(angle);
                canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
                canvas.restore();
            }
        }
    }

    /**
     * @param aSelectedNodes the currently selected nodes to edit.
     */
    public void setSelectedNodes(@Nullable final List<Node> aSelectedNodes) {
        tmpDrawingSelectedNodes = aSelectedNodes;
    }

    /**
     * 
     * @param aSelectedWays the currently selected ways to edit.
     */
    public void setSelectedWays(@Nullable final List<Way> aSelectedWays) {
        tmpDrawingSelectedWays = aSelectedWays;
    }

    @Override
    public void setPrefs(@NonNull final Preferences prefs) {
        this.prefs = prefs;
        showIcons = prefs.getShowIcons();
        showWayIcons = prefs.getShowWayIcons();
        showTolerance = prefs.isToleranceVisible();
        panAndZoomDownLoad = prefs.getPanAndZoomAutoDownload();
        minDownloadSize = prefs.getDownloadRadius() * 2;
        maxDownloadSpeed = prefs.getMaxBugDownloadSpeed() / 3.6f;
        autoPruneEnabled = prefs.autoPrune();
        autoPruneNodeLimit = prefs.getAutoPruneNodeLimit();
        autoDownloadBoxLimit = prefs.getAutoPruneBoundingBoxLimit();
        panAndZoomLimit = prefs.getPanAndZoomLimit();
        wayNodeDragging = prefs.isWayNodeDraggingEnabled();
        iconCache.clear();
        areaIconCache.clear();
        context = map.getContext(); // assuer that we have the correct context
    }

    /**
     * Update cached Paint and FeatureStyle objects
     */
    public void updateStyle() {
        nodeValidationColor = 0;
        // changes when style changes
        nodeTolerancePaint = styles.getInternal(DataStyle.NODE_TOLERANCE).getPaint();
        nodeTolerancePaint2 = styles.getInternal(DataStyle.NODE_TOLERANCE_2).getPaint();
        wayTolerancePaint = styles.getInternal(DataStyle.WAY_TOLERANCE).getPaint();
        nodeToleranceRadius = wayTolerancePaint.getStrokeWidth() / 2;
        wayTolerancePaint2 = styles.getInternal(DataStyle.WAY_TOLERANCE_2).getPaint();
        labelBackground = styles.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();

        // general node style
        nodeFeatureStyle = styles.getInternal(DataStyle.NODE_UNTAGGED);
        // style for house numbers
        nodeFeatureStyleThin = styles.getInternal(DataStyle.NODE_THIN);
        // style for tagged nodes or otherwise important
        nodeFeatureStyleTagged = styles.getInternal(DataStyle.NODE_TAGGED);
        // style for label text
        labelTextStyleNormal = styles.getInternal(DataStyle.LABELTEXT_NORMAL);
        // style for small label text
        labelTextStyleSmall = styles.getInternal(DataStyle.LABELTEXT_SMALL);

        // selected
        nodeFeatureStyleSelected = styles.getInternal(DataStyle.SELECTED_NODE);
        nodeFeatureStyleThinSelected = styles.getInternal(DataStyle.SELECTED_NODE_THIN);
        nodeFeatureStyleTaggedSelected = styles.getInternal(DataStyle.SELECTED_NODE_TAGGED);
        labelTextStyleNormalSelected = styles.getInternal(DataStyle.LABELTEXT_NORMAL_SELECTED);
        labelTextStyleSmallSelected = styles.getInternal(DataStyle.LABELTEXT_SMALL_SELECTED);

        // selected as member of a relation
        nodeFeatureStyleRelation = styles.getInternal(DataStyle.SELECTED_RELATION_NODE);
        nodeFeatureStyleThinRelation = styles.getInternal(DataStyle.SELECTED_RELATION_NODE_THIN);
        nodeFeatureStyleTaggedRelation = styles.getInternal(DataStyle.SELECTED_RELATION_NODE_TAGGED);
        nodeFeatureStyleFontRelation = styles.getInternal(DataStyle.LABELTEXT_NORMAL);
        nodeFeatureStyleFontSmallRelation = styles.getInternal(DataStyle.LABELTEXT_SMALL);

        // problem variant
        nodeFeatureStyleProblem = styles.getInternal(DataStyle.PROBLEM_NODE);
        nodeFeatureStyleThinProblem = styles.getInternal(DataStyle.PROBLEM_NODE_THIN);
        nodeFeatureStyleTaggedProblem = styles.getInternal(DataStyle.PROBLEM_NODE_TAGGED);
        nodeFeatureStyleFontProblem = styles.getInternal(DataStyle.LABELTEXT_NORMAL_PROBLEM);
        nodeFeatureStyleFontSmallProblem = styles.getInternal(DataStyle.LABELTEXT_SMALL_PROBLEM);

        // hiden node
        nodeFeatureStyleHidden = styles.getInternal(DataStyle.HIDDEN_NODE);

        nodeDragRadiusPaint = styles.getInternal(DataStyle.NODE_DRAG_RADIUS).getPaint();

        // way stuff
        selectedWayStyle = styles.getInternal(DataStyle.SELECTED_WAY);
        wayDirectionPaint = styles.getInternal(DataStyle.WAY_DIRECTION).getPaint();
        wayFeatureStyleHidden = styles.getInternal(DataStyle.HIDDEN_WAY);
        wayFeatureStyleRelation = styles.getInternal(DataStyle.SELECTED_RELATION_WAY);
        handlePaint = styles.getInternal(DataStyle.HANDLE).getPaint();
        dontRenderWay = styles.getInternal(DataStyle.DONTRENDER_WAY);

        showIconsLimit = styles.getCurrent().getIconZoomLimit();
        showIconLabelZoomLimit = styles.getCurrent().getIconLabelZoomLimit();
    }

    /**
     * @return the iconRadius
     */
    public int getIconRadius() {
        return iconRadius;
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // unused
    }

    @Override
    public String getName() {
        String apiName = prefs != null ? prefs.getApiName() : null;
        return apiName != null ? map.getContext().getString(R.string.layer_data_name, apiName) : map.getContext().getString(R.string.layer_data);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        List<BoundingBox> boxes = delegator.getCurrentStorage().getBoundingBoxes();
        if (!boxes.isEmpty()) {
            return BoundingBox.union(new ArrayList<>(boxes));
        } else if (prefs.getServer().hasMapSplitSource()) {
            return prefs.getServer().getMapSplitSource().getBounds();
        }
        return null;
    }

    @Override
    public boolean enableConfiguration() {
        return true;
    }

    @Override
    public void configure(FragmentActivity activity) {
        APIEditorActivity.start(activity);
    }

    @Override
    public void showInfo(FragmentActivity activity) {
        LayerInfo f = new ApiLayerInfo();
        f.setShowsDialog(true);
        LayerInfo.showDialog(activity, f);
    }

    @Override
    public void prune() {
        ViewBox pruneBox = new ViewBox(viewBox);
        pruneBox.scale(1.6);
        delegator.prune(pruneBox);
    }

    @Override
    public LayerType getType() {
        return LayerType.OSMDATA;
    }

    @Override
    public void setOnUpdateListener(OnUpdateListener<O> listener) {
        onUpdateListener = listener;
    }

    /**
     * @return the downloadedBoxes
     */
    public List<BoundingBox> getDownloadedBoxes() {
        return downloadedBoxes;
    }
}
