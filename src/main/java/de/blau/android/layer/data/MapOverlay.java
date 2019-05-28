package de.blau.android.layer.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Pools.SimplePool;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.filter.Filter;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.APIEditorActivity;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Util;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.validation.Validator;
import de.blau.android.views.IMapView;
import de.blau.android.views.layers.ImageryLayerInfo;

/**
 * OSM data layer
 * 
 * @author mb
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 * @author Simon Poole
 */

public class MapOverlay extends MapViewLayer implements ExtentInterface, ConfigureInterface, LayerInfoInterface {

    private static final String OUTER = "outer";

    private static final String INNER = "inner";

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    public static final int ICON_SIZE_DP = 20;

    private static final int HOUSE_NUMBER_RADIUS = 10;

    /**
     * zoom level from which on we display icons and house numbers
     */
    private static final int SHOW_ICONS_LIMIT = 15;

    public static final int SHOW_LABEL_LIMIT = SHOW_ICONS_LIMIT + 5;

    /** half the width/height of a node icon in px */
    private final int iconRadius;

    private final int iconSelectedBorder;

    private final int houseNumberRadius;

    private final int verticalNumberOffset;

    private Preferences prefs;

    private final StorageDelegator delegator;

    /**
     * show icons for POIs (in a wide sense of the word)
     */
    private boolean showIcons = false;

    /**
     * show icons for POIs tagged on (closed) ways
     */
    private boolean showWayIcons = false;

    /**
     * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
     */
    private final WeakHashMap<Object, Bitmap> iconCache = new WeakHashMap<>();

    /**
     * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags. This stores icons
     * for areas
     */
    private final WeakHashMap<Object, Bitmap> areaIconCache = new WeakHashMap<>();

    /**
     * Stores strings that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
     */
    private final WeakHashMap<Object, String> labelCache = new WeakHashMap<>();

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

    /** Caches the Paint used for node tolerance */
    private Paint nodeTolerancePaint;
    private Paint nodeTolerancePaint2;

    /** Caches the Paint used for way tolerance */
    private Paint wayTolerancePaint;
    private Paint wayTolerancePaint2;

    /** cached zoom level, calculated once per onDraw pass **/
    private int zoomLevel = 0;

    /** Cache the current filter **/
    private Filter tmpFilter = null;

    /** */
    private boolean inNodeIconZoomRange = false;

    /**
     * We just need one path object
     */
    private Path path = new Path();

    private LongHashSet handles;

    private Context context;

    private Validator validator;

    private Paint labelBackground;

    private float[][] coord = null;

    private FloatPrimitiveList points = new FloatPrimitiveList(); // allocate this just once

    private final Map map;

    /**
     * Stuff for multipolygon support Instantiate these objects just once
     */
    List<RelationMember>        waysOnly       = new ArrayList<>();
    List<ArrayList<Node>>       outerRings     = new ArrayList<>();
    List<ArrayList<Node>>       innerRings     = new ArrayList<>();
    List<ArrayList<Node>>       unknownRings   = new ArrayList<>();
    SimplePool<ArrayList<Node>> ringPool       = new SimplePool<>(10);
    List<Node>                  areaNodes      = new ArrayList<>();   // temp for reversing winding and assembling MPs
    Set<Relation>               paintRelations = new HashSet<>();

    @SuppressLint("NewApi")
    public MapOverlay(final Map map) {
        this.map = map;
        context = map.getContext();

        iconRadius = Density.dpToPx(ICON_SIZE_DP / 2);
        houseNumberRadius = Density.dpToPx(HOUSE_NUMBER_RADIUS);
        verticalNumberOffset = Density.dpToPx(HOUSE_NUMBER_RADIUS / 2);
        iconSelectedBorder = Density.dpToPx(2);

        validator = App.getDefaultValidator(context);

        delegator = App.getDelegator();

        updateStyle();
    }

    @Override
    public void onDestroy() {
        synchronized (iconCache) {
            iconCache.clear();
        }
        synchronized (areaIconCache) {
            areaIconCache.clear();
        }
        synchronized (labelCache) {
            labelCache.clear();
        }
        tmpPresets = null;
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
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

        // set in paintOsmData now tmpDrawingInEditRange = Main.logic.isInEditZoomRange();
        final Logic logic = App.getLogic();
        tmpDrawingEditMode = logic.getMode();
        tmpFilter = logic.getFilter();
        tmpDrawingSelectedNodes = logic.getSelectedNodes();
        tmpDrawingSelectedWays = logic.getSelectedWays();
        tmpClickableElements = logic.getClickableElements();
        tmpDrawingSelectedRelationWays = logic.getSelectedRelationWays();
        tmpDrawingSelectedRelationNodes = logic.getSelectedRelationNodes();
        tmpPresets = App.getCurrentPresets(context);
        tmpLocked = logic.isLocked();

        inNodeIconZoomRange = zoomLevel > DataStyle.getCurrent().getIconZoomLimit();

        paintOsmData(canvas);
    }

    /**
     * Paints all OSM data on the given canvas.
     * 
     * @param canvas Canvas, where the data shall be painted on.
     */
    private void paintOsmData(final Canvas canvas) {

        int screenWidth = map.getWidth();
        int screenHeight = map.getHeight();
        ViewBox viewBox = map.getViewBox();

        paintRelations.clear();

        // first find all nodes that we need to display

        List<Node> paintNodes = delegator.getCurrentStorage().getNodes(viewBox);

        // the following should guarantee that if the selected node is off screen but the handle not, the handle gets
        // drawn
        // note this isn't perfect because touch areas of other nodes just outside the screen still won't get drawn
        if (tmpDrawingSelectedNodes != null) {
            for (Node n : tmpDrawingSelectedNodes) {
                if (!paintNodes.contains(n)) {
                    paintNodes.add(n);
                }
            }
        }

        boolean filterMode = tmpFilter != null; // we have an active filter

        //
        tmpDrawingInEditRange = App.getLogic().isInEditZoomRange();

        boolean drawTolerance = tmpDrawingInEditRange // if we are not in editing range none of the further checks are
                                                      // necessary
                && !tmpLocked && tmpDrawingEditMode.elementsSelectable();

        // Paint all ways
        List<Way> ways = delegator.getCurrentStorage().getWays(viewBox);

        // get relations for all nodes and ways
        for (Node n : paintNodes) {
            addRelations(filterMode, n.getParentRelations(), paintRelations);
        }
        for (Way w : ways) {
            addRelations(filterMode, w.getParentRelations(), paintRelations);
        }

        // draw MPs first
        for (Relation rel : paintRelations) {
            String relType = rel.getTagWithKey(Tags.KEY_TYPE);
            if (Tags.VALUE_MULTIPOLYGON.equals(relType) || Tags.VALUE_BOUNDARY.equals(relType)) {
                paintMultiPolygon(canvas, viewBox, rel);
            }
        }

        List<Way> waysToDraw = ways;
        if (filterMode) {
            /*
             * Split the ways in to those that we are going to show and those that we hide, rendering is far simpler for
             * the later
             */
            tmpHiddenWays.clear();
            tmpStyledWays.clear();
            for (Way w : ways) {
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

        boolean displayHandles = tmpDrawingSelectedNodes == null && tmpDrawingSelectedRelationWays == null && tmpDrawingSelectedRelationNodes == null
                && tmpDrawingEditMode.elementsGeomEditiable();
        Collections.sort(waysToDraw, layerComparator);

        // ways now
        for (Way w : waysToDraw) {
            paintWay(canvas, w, displayHandles, drawTolerance);
        }

        // Paint nodes
        Boolean hwAccelarationWorkaround = Map.myIsHardwareAccelerated(canvas) && Build.VERSION.SDK_INT < 19;

        int coordSize = 0;
        float r = wayTolerancePaint.getStrokeWidth() / 2;
        float r2 = r * r;
        if (drawTolerance) {
            if (coord == null || coord.length < paintNodes.size()) {
                coord = new float[paintNodes.size()][2];
            }
        }
        for (Node n : paintNodes) {
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
            paintNode(canvas, n, x, y, hwAccelarationWorkaround,
                    drawTolerance && !noTolerance && (n.getState() != OsmElement.STATE_UNCHANGED || delegator.isInDownload(lon, lat)));
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

    /**
     * For ordering according to layer value and draw lines on top of areas in the same layer
     */
    private Comparator<Way> layerComparator = new Comparator<Way>() {
        @Override
        public int compare(Way w1, Way w2) {
            int layer1 = 0;
            int layer2 = 0;
            String layer1Str = w1.getTagWithKey(Tags.KEY_LAYER);
            if (layer1Str != null) {
                try {
                    layer1 = Integer.parseInt(layer1Str);
                } catch (NumberFormatException e) {
                    // FIXME should validate here
                }
            }
            String layer2Str = w2.getTagWithKey(Tags.KEY_LAYER);
            if (layer2Str != null) {
                try {
                    layer2 = Integer.parseInt(layer2Str);
                } catch (NumberFormatException e) {
                    // FIXME should validate here
                }
            }
            int result = layer2 == layer1 ? 0 : layer2 > layer1 ? +1 : -1;
            if (result == 0) {
                FeatureStyle fs1 = DataStyle.matchStyle(w1);
                Style style1 = fs1.getPaint().getStyle();
                FeatureStyle fs2 = DataStyle.matchStyle(w2);
                Style style2 = fs2.getPaint().getStyle();
                result = style2 == style1 ? 0 : style2 == Style.STROKE ? -1 : +1;
            }
            return result;
        }
    };

    /**
     * Draw a multipolygon
     * 
     * @param canvas the Canvas to draw on
     * @param viewBox the current ViewBox
     * @param rel the Relation for the multipolygon
     */
    private void paintMultiPolygon(@NonNull Canvas canvas, @NonNull ViewBox viewBox, @NonNull Relation rel) {
        FeatureStyle style;
        if (rel.hasProblem(context, validator) != Validator.OK) {
            style = DataStyle.getInternal(DataStyle.PROBLEM_WAY);
        } else {
            style = DataStyle.matchStyle(rel);
        }

        if (zoomLevel < style.getMinVisibleZoom() || style.dontRender()) {
            return;
        }

        // remove any non-Way non-downloaded members
        waysOnly.clear();

        for (RelationMember m : rel.getMembers()) {
            if (m.downloaded() && Way.NAME.equals(m.getType())) {
                waysOnly.add(m);
            }
        }
        List<RelationMember> members = Util.sortRelationMembers(waysOnly);
        outerRings.clear();
        innerRings.clear();
        unknownRings.clear();
        ArrayList<Node> ring = getNewRing();

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
            // a bit of a hack stop this way from being rendered as a way if it doesn't have any tags
            if (!currentWay.hasTags() && !"".equals(ringRole)) {
                currentWay.setStyle(DataStyle.getInternal(DataStyle.DONTRENDER_WAY));
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

            RelationMember next = members.get((i + 1) % ms);
            Way nextWay = (Way) next.getElement();
            Node lastRingNode = ring.get(ring.size() - 1);
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
        if (!ring.isEmpty()) {
            addRing(ringRole, ring);
        }

        path.reset();

        Paint paint = style.getPaint();
        boolean closeRings = paint.getStyle() != Paint.Style.STROKE;

        outerRings.addAll(innerRings);
        outerRings.addAll(unknownRings);

        for (ArrayList<Node> r : outerRings) {
            map.pointListToLinePointsArray(points, r);
            float[] linePoints = points.getArray();
            int pointsSize = points.size();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            if (closeRings) {
                path.close();
            }
            ringPool.release(r);
        }

        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, paint);
    }

    /**
     * Get a new ring from the pool or instantiate one if the pool is empty
     * 
     * @return an ArrayList<Node>
     */
    @NonNull
    private ArrayList<Node> getNewRing() {
        ArrayList<Node> ring = ringPool.acquire();
        if (ring == null) {
            ring = new ArrayList<>();
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
    private void addRing(@NonNull String role, @NonNull ArrayList<Node> ring) {
        switch (role) {
        case OUTER:
            if (!clockwise(ring)) {
                Collections.reverse(ring);
            }
            outerRings.add(ring);
            break;
        case INNER:
            if (clockwise(ring)) {
                Collections.reverse(ring);
            }
            innerRings.add(ring);
            break;
        default:
            unknownRings.add(ring);
        }
    }

    /**
     * Determine winding of a List of Nodes
     * 
     * @param nodes the List of Nodes
     * @return true if the winding is clockwise
     */
    private boolean clockwise(@NonNull List<Node> nodes) {
        long area = 0;
        int s = nodes.size();
        Node n1 = nodes.get(0);
        int lat1 = n1.getLat();
        int lon1 = n1.getLon();
        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            Node n2 = nodes.get((i + 1) % s);
            int lat2 = n2.getLat();
            int lon2 = n2.getLon();
            area = area + (long) (lat2 - lat1) * (long) (lon2 + lon1);
            lat1 = lat2;
            lon1 = lon2;
        }
        return area < 0;
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
                if (from != null && from.getElement() != null && from.getType().equals(Way.NAME)) {
                    Way fromWay = (Way) from.getElement();
                    int size = fromWay.getNodes().size();
                    if (size > 1) {
                        String type = restriction.getTagWithKey(Tags.VALUE_RESTRICTION);
                        int arrowDirection = 0;
                        if (type != null) {
                            switch (type) {
                            case Tags.VALUE_NO_RIGHT_TURN:
                            case Tags.VALUE_ONLY_RIGHT_TURN:
                                arrowDirection = 90;
                                y -= 2 * ICON_SIZE_DP;
                                x += 2 * ICON_SIZE_DP;
                                break;
                            case Tags.VALUE_NO_LEFT_TURN:
                            case Tags.VALUE_ONLY_LEFT_TURN:
                                arrowDirection = -90;
                                y += 2 * ICON_SIZE_DP;
                                x += 2 * ICON_SIZE_DP;
                                break;
                            case Tags.VALUE_NO_STRAIGHT_ON:
                            case Tags.VALUE_ONLY_STRAIGHT_ON:
                                arrowDirection = 180;
                                y -= 2 * ICON_SIZE_DP;
                                break;
                            default:
                                // ignore
                            }
                        }
                        Node prevNode = fromWay.getNodes().get(1);
                        if (fromWay.getLastNode().equals((Node) v)) {
                            prevNode = fromWay.getNodes().get(size - 2);
                        }
                        long bearing = (GeoMath.bearing(prevNode.getLon() / 1E7D, prevNode.getLat() / 1E7D, lon / 1E7D, lat / 1E7D) + arrowDirection) % 360;
                        canvas.save();
                        canvas.rotate(bearing, x, y);
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
                float[] xy = Logic.centroidXY(screenWidth, screenHeight, viewBox, viaWay);
                List<RelationMember> tos = restriction.getMembersWithRole(Tags.ROLE_TO);
                RelationMember to = tos.isEmpty() ? null : tos.get(0);
                if (to != null && to.getElement() != null && to.getType().equals(Way.NAME)) {
                    Way toWay = (Way) to.getElement();
                    int size = viaWay.getNodes().size();
                    if (size > 1) {
                        int offset = 0;
                        if (!viaWay.hasNode(toWay.getFirstNode())) {
                            offset = 180;
                        }
                        long bearing = (GeoMath.bearing(viaWay.getFirstNode().getLon() / 1E7D, viaWay.getFirstNode().getLat() / 1E7D,
                                viaWay.getLastNode().getLon() / 1E7D, viaWay.getLastNode().getLat() / 1E7D) + offset) % 360;
                        canvas.save();
                        canvas.rotate(bearing, xy[0], xy[1]);
                    } else {
                        to = null;
                    }
                }
                paintNodeIcon(restriction, canvas, xy[0], xy[1], null);
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
     * @param hwAccelarationWorkaround use a workaround for operations that are not supported when HW accelation is used
     * @param drawTolerance draw the touch halo
     */
    private void paintNode(@NonNull final Canvas canvas, @NonNull final Node node, final float x, final float y, final boolean hwAccelarationWorkaround,
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
            if (prefs.isToleranceVisible() && tmpClickableElements == null) {
                drawNodeTolerance(canvas, isTagged, x, y, nodeTolerancePaint);
            } else if (tmpClickableElements != null && tmpClickableElements.contains(node)) {
                drawNodeTolerance(canvas, isTagged, x, y, nodeTolerancePaint2);
            }
        }

        String featureStyle;
        String featureStyleThin;
        String featureStyleTagged;
        String featureStyleFont;
        String featureStyleFontSmall;
        if (isSelected && tmpDrawingInEditRange) {
            // general node style
            featureStyle = DataStyle.SELECTED_NODE;
            // style for house numbers
            featureStyleThin = DataStyle.SELECTED_NODE_THIN;
            // style for tagged nodes or otherwise important
            featureStyleTagged = DataStyle.SELECTED_NODE_TAGGED;
            // style for label text
            featureStyleFont = DataStyle.LABELTEXT_NORMAL_SELECTED;
            // style for small label text
            featureStyleFontSmall = DataStyle.LABELTEXT_SMALL_SELECTED;
            if (tmpDrawingSelectedNodes.size() == 1 && tmpDrawingSelectedWays == null && prefs.largeDragArea() && tmpDrawingEditMode.elementsGeomEditiable()) {
                // don't draw large areas in multi-select mode
                canvas.drawCircle(x, y, DataStyle.getCurrent().getLargDragToleranceRadius(), DataStyle.getInternal(DataStyle.NODE_DRAG_RADIUS).getPaint());
            }
        } else if ((tmpDrawingSelectedRelationNodes != null && tmpDrawingSelectedRelationNodes.contains(node)) && tmpDrawingInEditRange) {
            // general node style
            featureStyle = DataStyle.SELECTED_RELATION_NODE;
            // style for house numbers
            featureStyleThin = DataStyle.SELECTED_RELATION_NODE_THIN;
            // style for tagged nodes or otherwise important
            featureStyleTagged = DataStyle.SELECTED_RELATION_NODE_TAGGED;
            // style for label text
            featureStyleFont = DataStyle.LABELTEXT_NORMAL;
            // style for small label text
            featureStyleFontSmall = DataStyle.LABELTEXT_SMALL;
            isSelected = true;
        } else if (node.hasProblem(context, validator) != Validator.OK) {
            // general node style
            featureStyle = DataStyle.PROBLEM_NODE;
            // style for house numbers
            featureStyleThin = DataStyle.PROBLEM_NODE_THIN;
            // style for tagged nodes or otherwise important
            featureStyleTagged = DataStyle.PROBLEM_NODE_TAGGED;
            // style for label text
            featureStyleFont = DataStyle.LABELTEXT_NORMAL_PROBLEM;
            // style for small label text
            featureStyleFontSmall = DataStyle.LABELTEXT_SMALL_PROBLEM;
            hasProblem = true;
        } else {
            // general node style
            featureStyle = DataStyle.NODE;
            // style for house numbers
            featureStyleThin = DataStyle.NODE_THIN;
            // style for tagged nodes or otherwise important
            featureStyleTagged = DataStyle.NODE_TAGGED;
            // style for label text
            featureStyleFont = DataStyle.LABELTEXT_NORMAL;
            // style for small label text
            featureStyleFontSmall = DataStyle.LABELTEXT_SMALL;
        }

        boolean noIcon = true;
        boolean isTaggedAndInZoomLimit = isTagged && inNodeIconZoomRange;

        if (filterMode && !filteredObject) {
            featureStyle = DataStyle.HIDDEN_NODE;
            featureStyleThin = featureStyle;
            featureStyleTagged = featureStyle;
            isTaggedAndInZoomLimit = false;
        }

        if (isTaggedAndInZoomLimit && showIcons) {
            noIcon = tmpPresets == null || !paintNodeIcon(node, canvas, x, y, isSelected || hasProblem ? featureStyleTagged : null);
            if (noIcon) {
                String houseNumber = node.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
                if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
                    paintHouseNumber(x, y, canvas, featureStyleThin, featureStyleFontSmall, houseNumber);
                    return;
                }
            } else if (zoomLevel > SHOW_LABEL_LIMIT && node.hasTagKey(Tags.KEY_NAME)) {
                Paint p = DataStyle.getInternal(DataStyle.NODE_TAGGED).getPaint();
                paintLabel(x, y, canvas, featureStyleFont, node, p.getStrokeWidth(), true);
            }
        }

        if (noIcon) {
            // draw regular nodes or without icons
            FeatureStyle style = DataStyle.getInternal(isTagged ? featureStyleTagged : featureStyle);
            if (zoomLevel < style.getMinVisibleZoom()) {
                return;
            }
            Paint p = style.getPaint();
            float strokeWidth = p.getStrokeWidth();
            if (hwAccelarationWorkaround) { // FIXME we don't actually know if this is slower than drawPoint
                canvas.drawCircle(x, y, strokeWidth / 2, p);
            } else {
                canvas.drawPoint(x, y, p);
            }
            if (isTaggedAndInZoomLimit) {
                paintLabel(x, y, canvas, featureStyleFont, node, strokeWidth, false);
            }
        }
    }

    /**
     * Draw a circle with center at x,y with the house number in it
     * 
     * @param x screen x
     * @param y screen y
     * @param canvas canvas we are drawing on
     * @param featureKeyThin style to use for the housenumber circle
     * @param featureKeyFont style to use for the housenumber number
     * @param houseNumber the number as a string
     */
    private void paintHouseNumber(final float x, final float y, final Canvas canvas, final String featureKeyThin, final String featureKeyFont,
            final String houseNumber) {
        FeatureStyle fontStyle = DataStyle.getInternal(featureKeyFont);
        Paint fontPaint = fontStyle.getPaint();
        Paint paint = DataStyle.getInternal(featureKeyThin).getPaint();
        canvas.drawCircle(x, y, houseNumberRadius, paint);
        canvas.drawCircle(x, y, houseNumberRadius, labelBackground);
        canvas.drawText(houseNumber, x - fontPaint.measureText(houseNumber) / 2, y + verticalNumberOffset, fontStyle.getPaint());
    }

    /**
     * Paint a label under the node, does not try to do collision avoidance
     * 
     * @param x screen x
     * @param y screen y
     * @param canvas canvas we are drawing on
     * @param featureKeyThin style to use for the label
     * @param e the OsmElement
     * @param strokeWidth current stroke scaling factor
     * @param withIcon offset the label so that we don't overlap an icon
     */
    private void paintLabel(final float x, final float y, final Canvas canvas, final String featureKeyThin, final OsmElement e, final float strokeWidth,
            final boolean withIcon) {
        FeatureStyle fs = DataStyle.getInternal(featureKeyThin);
        Paint paint = fs.getPaint();
        SortedMap<String, String> tags = e.getTags();
        String label = labelCache.get(tags); // may be null!
        if (label == null) {
            if (!labelCache.containsKey(tags)) {
                label = e.getTagWithKey(Tags.KEY_NAME);
                if (label == null && tmpPresets != null) {
                    PresetItem match = Preset.findBestMatch(tmpPresets, e.getTags());
                    if (match != null) {
                        label = match.getTranslatedName();
                    } else {
                        label = e.getPrimaryTag(context);
                        // if label is still null, leave it as is
                    }
                }
                synchronized (labelCache) {
                    labelCache.put(tags, label);
                    if (label == null) {
                        return;
                    }
                }
            } else {
                return;
            }
        }
        float halfTextWidth = paint.measureText(label) / 2;
        FontMetrics fm = fs.getFontMetrics();
        float yOffset = y + strokeWidth + (withIcon ? 2 * iconRadius : iconRadius);
        canvas.drawRect(x - halfTextWidth, yOffset + fm.bottom, x + halfTextWidth, yOffset - paint.getTextSize() + fm.bottom, labelBackground);
        canvas.drawText(label, x - halfTextWidth, yOffset, paint);
    }

    static final Bitmap NOICON = Bitmap.createBitmap(2, 2, Config.ARGB_8888);

    /**
     * Retrieve icon for the element, caching it if it isn't in the cache
     * 
     * @param element element we want to find an icon for
     * @return icon or null if none is found
     */
    private Bitmap getIcon(OsmElement element) {
        SortedMap<String, String> tags = element.getTags();
        boolean isWay = element instanceof Way;
        WeakHashMap<Object, Bitmap> tempCache = isWay ? areaIconCache : iconCache;

        Bitmap icon = tempCache.get(tags); // may be null!
        if (icon == null && tmpPresets != null) {
            if (tempCache.containsKey(tags)) {
                // no point in trying to match
                return null;
            }
            // icon not cached, ask the preset, render to a bitmap and cache result
            PresetItem match = null;
            if (isWay) {
                // don't show building icons, only icons for those with POI tags
                if (Logic.areaHasIcon((Way) element)) {
                    SortedMap<String, String> tempTags = new TreeMap<>(tags);
                    tempTags.remove(Tags.KEY_BUILDING);
                    icon = iconCache.get(tags); // maybe we already cached this for a node
                    if (icon == null) {
                        match = Preset.findBestMatch(tmpPresets, tempTags);
                    }
                }
            } else {
                match = Preset.findBestMatch(tmpPresets, tags);
            }
            if (match != null) {
                Drawable iconDrawable = match.getMapIcon();
                if (iconDrawable != null) {
                    icon = Bitmap.createBitmap(iconRadius * 2, iconRadius * 2, Config.ARGB_8888);
                    // icon.eraseColor(Color.WHITE); // replace nothing with white?
                    iconDrawable.draw(new Canvas(icon));
                }
            } else {
                icon = NOICON;
            }
            synchronized (tempCache) {
                tempCache.put(tags, icon);
            }
        }
        return icon != NOICON ? icon : null;
    }

    /**
     * Remove everything from the iconCache
     */
    public void clearIconCaches() {
        synchronized (iconCache) {
            iconCache.clear();
            areaIconCache.clear();
        }
    }

    /**
     * Paints an icon for an element. tmpPreset needs to be available (i.e. not null).
     * 
     * @param element the element whose icon should be painted
     * @param canvas the canvas on which to draw
     * @param x the x position where the center of the icon goes
     * @param y the y position where the center of the icon goes
     * @param featureKey style key
     * @return true if an icon was found and drawn
     */
    private boolean paintNodeIcon(OsmElement element, Canvas canvas, float x, float y, String featureKey) {
        Bitmap icon = getIcon(element);
        if (icon != null) {
            float w2 = icon.getWidth() / 2f;
            float h2 = icon.getHeight() / 2f;
            if (featureKey != null) { // selected
                RectF r = new RectF(x - w2 - iconSelectedBorder, y - h2 - iconSelectedBorder, x + w2 + iconSelectedBorder, y + h2 + iconSelectedBorder);
                canvas.drawRoundRect(r, iconSelectedBorder, iconSelectedBorder, DataStyle.getInternal(featureKey).getPaint());
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
    private void drawNodeTolerance(final Canvas canvas, final boolean isTagged, final float x, final float y, final Paint paint) {
        canvas.drawCircle(x, y, isTagged ? paint.getStrokeWidth() : wayTolerancePaint.getStrokeWidth() / 2, paint);
    }

    /**
     * Paints the given way on the canvas.
     * 
     * @param canvas Canvas, where the node shall be painted on.
     * @param way way which shall be painted.
     * @param displayHandles draw geometry improvement handles
     * @param drawTolerance if true draw the halo
     */
    private void paintWay(final Canvas canvas, final Way way, final boolean displayHandles, boolean drawTolerance) {

        FeatureStyle style;
        if (way.hasProblem(context, validator) != Validator.OK) {
            style = DataStyle.getInternal(DataStyle.PROBLEM_WAY);
        } else {
            style = DataStyle.matchStyle(way);
        }

        if (zoomLevel < style.getMinVisibleZoom()) {
            return;
        }

        boolean isSelected = tmpDrawingInEditRange // if we are not in editing range don't show selected way ... may be
                                                   // a better idea to do so
                && tmpDrawingSelectedWays != null && tmpDrawingSelectedWays.contains(way);
        boolean isMemberOfSelectedRelation = tmpDrawingInEditRange && tmpDrawingSelectedRelationWays != null && tmpDrawingSelectedRelationWays.contains(way);

        if (style.dontRender() && !(isSelected || isMemberOfSelectedRelation)) {
            return; // the way has already been rendered by something else
        }

        List<Node> nodes = way.getNodes();
        if (style.isArea() && !clockwise(nodes)) {
            areaNodes.clear();
            areaNodes.addAll(nodes);
            Collections.reverse(areaNodes);
            map.pointListToLinePointsArray(points, areaNodes);
        } else {
            map.pointListToLinePointsArray(points, nodes);
        }

        float[] linePoints = points.getArray();
        int pointsSize = points.size();
        Paint paint;
        String labelFontStyle = DataStyle.LABELTEXT_NORMAL;
        String labelFontStyleSmall = DataStyle.LABELTEXT_SMALL;

        // draw way tolerance
        if (drawTolerance) {
            if (prefs.isToleranceVisible() && tmpClickableElements == null) {
                canvas.drawLines(linePoints, 0, pointsSize, wayTolerancePaint);
            } else if (tmpClickableElements != null && tmpClickableElements.contains(way)) {
                canvas.drawLines(linePoints, 0, pointsSize, wayTolerancePaint2);
            }
        }

        // draw selectedWay highlighting
        if (isSelected) {
            FeatureStyle selectedStyle = DataStyle.getInternal(DataStyle.SELECTED_WAY);
            paint = selectedStyle.getPaint();
            paint.setStrokeWidth(style.getPaint().getStrokeWidth() * selectedStyle.getWidthFactor());
            canvas.drawLines(linePoints, 0, pointsSize, paint);
            paint = DataStyle.getInternal(DataStyle.WAY_DIRECTION).getPaint();
            drawWayArrows(canvas, linePoints, pointsSize, false, paint, displayHandles && tmpDrawingSelectedWays.size() == 1);
            labelFontStyle = DataStyle.LABELTEXT_NORMAL_SELECTED;
            labelFontStyleSmall = DataStyle.LABELTEXT_SMALL_SELECTED;
        } else if (isMemberOfSelectedRelation) {
            FeatureStyle relationSelectedStyle = DataStyle.getInternal(DataStyle.SELECTED_RELATION_WAY);
            paint = relationSelectedStyle.getPaint();
            paint.setStrokeWidth(style.getPaint().getStrokeWidth() * relationSelectedStyle.getWidthFactor());
            canvas.drawLines(linePoints, 0, pointsSize, paint);
        }

        FeatureStyle arrowStyle = style.getArrowStyle();
        if (arrowStyle != null) {
            if (arrowStyle.checkOneway()) {
                int onewayCode = way.getOneway();
                if (onewayCode != 0) {
                    drawWayArrows(canvas, linePoints, pointsSize, (onewayCode == -1), arrowStyle.getPaint(), false);
                }
            } else {
                drawWayArrows(canvas, linePoints, pointsSize, false, arrowStyle.getPaint(), false);
            }
        }

        // draw the way itself
        if (pointsSize > 2) {
            path.reset();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            FeatureStyle casingStyle = style.getCasingStyle();
            if (casingStyle != null) {
                canvas.drawPath(path, casingStyle.getPaint());
            }
            canvas.drawPath(path, style.getPaint());
        }

        // display icons on closed ways
        if (showIcons && showWayIcons && zoomLevel > SHOW_ICONS_LIMIT && way.isClosed()) {
            int vs = pointsSize;
            if (vs < nodes.size() * 2) {
                return;
            }
            // calc centroid
            double A = 0;
            double Y = 0;
            double X = 0;
            double x1 = linePoints[0];
            double y1 = linePoints[1];
            for (int i = 0; i < vs; i = i + 2) {
                double x2 = linePoints[(i + 2) % vs];
                double y2 = linePoints[(i + 3) % vs];
                double d = x1 * y2 - x2 * y1;
                A = A + d;
                X = X + (x1 + x2) * d;
                Y = Y + (y1 + y2) * d;
                x1 = x2;
                y1 = y2;
            }
            if (Util.notZero(A)) {
                Y = Y / (3 * A); // NOSONAR nonZero tests for zero
                X = X / (3 * A); // NOSONAR nonZero tests for zero
                boolean iconDrawn = false;
                if (tmpPresets != null) {
                    iconDrawn = paintNodeIcon(way, canvas, (float) X, (float) Y, isSelected ? DataStyle.SELECTED_NODE_TAGGED : null);
                    boolean doLabel = false;
                    if (!iconDrawn) {
                        String houseNumber = way.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
                        if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
                            paintHouseNumber((float) X, (float) Y, canvas, isSelected ? DataStyle.SELECTED_NODE_THIN : DataStyle.NODE_THIN, labelFontStyleSmall,
                                    houseNumber);
                        } else {
                            doLabel = way.hasTagKey(Tags.KEY_NAME);
                        }
                    } else {
                        doLabel = zoomLevel > SHOW_LABEL_LIMIT && way.hasTagKey(Tags.KEY_NAME);
                    }
                    if (doLabel) {
                        Paint p = DataStyle.getInternal(DataStyle.SELECTED_NODE_TAGGED).getPaint();
                        paintLabel((float) X, (float) Y, canvas, labelFontStyle, way, iconDrawn ? p.getStrokeWidth() : 0, iconDrawn);
                    }
                }
            }
        }
    }

    /**
     * Paints the given way on the canvas with the "hidden" style.
     * 
     * @param canvas Canvas, where the node shall be painted on.
     * @param way way which shall be painted.
     */
    private void paintHiddenWay(@NonNull final Canvas canvas, @NonNull final Way way) {
        map.pointListToLinePointsArray(points, way.getNodes());
        float[] linePoints = points.getArray();
        int pointsSize = points.size();

        //
        FeatureStyle style = DataStyle.getInternal(DataStyle.HIDDEN_WAY);
        if (zoomLevel < style.getMinVisibleZoom()) {
            return;
        }

        // draw the way itself
        // canvas.drawLines(linePoints, fp.getPaint()); doesn't work properly with HW acceleration
        if (pointsSize > 2) {
            path.reset();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            canvas.drawPath(path, style.getPaint());
        }
    }

    /**
     * Draw geometry improvement handles
     * 
     * @param canvas the Canvas we are drawing on
     */
    private void paintHandles(@NonNull Canvas canvas) {
        if (handles != null && handles.size() > 0) {
            canvas.save();
            float lastX = 0;
            float lastY = 0;
            for (long l : handles.values()) {
                // draw handle
                float X = Float.intBitsToFloat((int) (l >>> 32));
                float Y = Float.intBitsToFloat((int) (l));
                canvas.translate(X - lastX, Y - lastY);
                lastX = X;
                lastY = Y;
                canvas.drawPath(DataStyle.getCurrent().getXPath(), DataStyle.getInternal(DataStyle.HANDLE).getPaint());
            }
            canvas.restore();
            handles.clear(); // this is hopefully faster than allocating a new set
        }
    }

    /**
     * Draws directional arrows for a way
     * 
     * @param canvas the canvas on which to draw
     * @param linePoints line segment array in the format returned by {@link #pointListToLinePointsArray(Iterable)}.
     * @param linePointsSize number of valid entries in linePoints
     * @param reverse if true, the arrows will be painted in the reverse direction
     * @param paint the paint to use for drawing the arrows
     * @param addHandles if true draw arrows at 1/4 and 3/4 of the length and save the middle pos. for drawing a handle
     */
    private void drawWayArrows(Canvas canvas, float[] linePoints, int linePointsSize, boolean reverse, Paint paint, boolean addHandles) {
        double minLen = DataStyle.getCurrent().getMinLenForHandle();
        int ptr = 0;
        while (ptr < linePointsSize) {

            float x1 = linePoints[ptr++];
            float y1 = linePoints[ptr++];
            float x2 = linePoints[ptr++];
            float y2 = linePoints[ptr++];

            float xDelta = x2 - x1;
            float yDelta = y2 - y1;

            boolean secondArrow = false;
            if (addHandles) {
                double len = Math.hypot(xDelta, yDelta);
                if (len > minLen) {
                    if (handles == null) {
                        handles = new LongHashSet();
                    }
                    handles.put(((long) (Float.floatToRawIntBits(x1 + xDelta / 2)) << 32) + (long) Float.floatToRawIntBits(y1 + yDelta / 2));
                    xDelta = xDelta / 4;
                    yDelta = yDelta / 4;
                    secondArrow = true;
                } else {
                    xDelta = xDelta / 2;
                    yDelta = yDelta / 2;
                }
            } else {
                xDelta = xDelta / 2;
                yDelta = yDelta / 2;
            }

            float x = x1 + xDelta;
            float y = y1 + yDelta;
            float angle = (float) (Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(reverse ? angle - 180 : angle);
            canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
            canvas.restore();

            if (secondArrow) {
                canvas.save();
                canvas.translate(x + 2 * xDelta, y + 2 * yDelta);
                canvas.rotate(reverse ? angle - 180 : angle);
                canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
                canvas.restore();
            }
        }
    }

    /**
     * @param aSelectedNodes the currently selected nodes to edit.
     */
    public void setSelectedNodes(final List<Node> aSelectedNodes) {
        tmpDrawingSelectedNodes = aSelectedNodes;
    }

    /**
     * 
     * @param aSelectedWays the currently selected ways to edit.
     */
    public void setSelectedWays(final List<Way> aSelectedWays) {
        tmpDrawingSelectedWays = aSelectedWays;
    }

    /**
     * Set the current Preferences object for this Map and any thing that needs changing
     * 
     * @param ctx Android Context
     * @param prefs the new Preferences
     */
    public void setPrefs(Context ctx, @NonNull final Preferences prefs) {
        this.prefs = prefs;
        showIcons = prefs.getShowIcons();
        showWayIcons = prefs.getShowWayIcons();
        iconCache.clear();
        areaIconCache.clear();
    }

    /**
     * Update cached Paint objects
     */
    public void updateStyle() {
        // changes when style changes
        nodeTolerancePaint = DataStyle.getInternal(DataStyle.NODE_TOLERANCE).getPaint();
        nodeTolerancePaint2 = DataStyle.getInternal(DataStyle.NODE_TOLERANCE_2).getPaint();
        wayTolerancePaint = DataStyle.getInternal(DataStyle.WAY_TOLERANCE).getPaint();
        wayTolerancePaint2 = DataStyle.getInternal(DataStyle.WAY_TOLERANCE_2).getPaint();
        labelBackground = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
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
        String apiName = prefs.getApiName();
        return apiName != null ? map.getContext().getString(R.string.layer_data_name, apiName) : map.getContext().getString(R.string.layer_data);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        List<BoundingBox> boxes = delegator.getCurrentStorage().getBoundingBoxes();
        if (boxes != null) {
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
}
