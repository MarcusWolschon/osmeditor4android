package io.vespucci.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.RelationMember;
import io.vespucci.osm.Tags;
import io.vespucci.osm.ViewBox;
import io.vespucci.osm.Way;
import io.vespucci.osm.OsmElement.ElementType;
import io.vespucci.prefs.Preferences;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetItem;
import io.vespucci.util.AreaTags;
import io.vespucci.util.GeoContext;
import io.vespucci.util.GeoMath;
import io.vespucci.util.Geometry;
import io.vespucci.util.KeyValue;
import io.vespucci.util.collections.MultiHashMap;

public class BaseValidator implements Validator {

    public static final int MAX_CONNECTION_TOLERANCE = 10; // maximum tolerance value for non-connected end nodes

    private static final String[] END_NODE_VALIDATION_KEYS = new String[] { Tags.KEY_HIGHWAY, Tags.KEY_WATERWAY };

    private Preset[]   presets;
    private GeoContext geoContext;
    private AreaTags   areaTags;

    /**
     * Tags for objects that should be re-surveyed regularly.
     */
    private MultiHashMap<String, PatternAndAge> resurveyTags;

    /**
     * Tags that should be present on objects (need to be in the preset for the object too
     */
    private Map<String, Boolean> checkTags;

    /**
     * The following values are cached as long as the current view box doesn't change
     */
    private ViewBox cachedViewBox = null;
    private float   tolerance;

    /**
     * Flags that indicate which validation is enabled
     */
    protected Set<String> enabledValidations;
    private boolean       fixmeValidation;
    private boolean       ageValidation;
    private boolean       missingTagValidation;
    private boolean       wrongTypeValidation;
    private boolean       highwayRoadValidation;
    private boolean       noTypeValidation;
    private boolean       imperialUnitsValidation;
    private boolean       invalidObjectValidation;
    protected boolean     untaggedValidation;
    private boolean       unconnectedEndNodeValidation;
    private boolean       degenerateWayValidation;
    private boolean       emptyRelationValidation;

    /**
     * Regex for general tagged issues with the object
     */
    static final Pattern FIXME_PATTERN = Pattern.compile("(?i).*\\b(?:fixme|todo)\\b.*");

    /**
     * Keys that suppress missing keys, this might make more sense as configuration
     */
    private static final MultiHashMap<String, KeyValue> MISSING_KEY_SUPPRESSION = new MultiHashMap<>();
    static {
        MISSING_KEY_SUPPRESSION.add(Tags.KEY_NAME, new KeyValue(Tags.KEY_NONAME, Tags.VALUE_YES));
        MISSING_KEY_SUPPRESSION.add(Tags.KEY_NAME, new KeyValue(Tags.KEY_VALIDATE_NO_NAME, Tags.VALUE_YES));
        MISSING_KEY_SUPPRESSION.add(Tags.KEY_REF, new KeyValue(Tags.KEY_NOREF, Tags.VALUE_YES));
    }

    /**
     * String resources for element types
     */
    private static final Map<ElementType, Integer> ELEMENT_TYPE_NAMES = new EnumMap<>(ElementType.class);
    static {
        ELEMENT_TYPE_NAMES.put(ElementType.NODE, R.string.element_type_node);
        ELEMENT_TYPE_NAMES.put(ElementType.WAY, R.string.element_type_way);
        ELEMENT_TYPE_NAMES.put(ElementType.CLOSEDWAY, R.string.element_type_closedway);
        ELEMENT_TYPE_NAMES.put(ElementType.AREA, R.string.element_type_area);
        ELEMENT_TYPE_NAMES.put(ElementType.RELATION, R.string.element_type_relation);
    }

    /**
     * Construct a new instance
     * 
     * @param ctx Android Context
     */
    public BaseValidator(@NonNull Context ctx) {
        init(ctx);
    }

    @Override
    public void reset(Context context) {
        init(context);
    }

    /**
     * (Re-)initialize everything
     * 
     * @param ctx Android Context
     */
    private void init(@NonNull Context ctx) {
        // !!!! don't store ctx as that will cause a potential memory leak
        presets = App.getCurrentPresets(ctx);
        geoContext = App.getGeoContext(ctx);
        areaTags = App.getAreaTags(ctx);

        // get per validation prefs
        Preferences prefs = new Preferences(ctx); // use our own instance as logics one may be out of sync
        enabledValidations = prefs.getEnabledValidations();
        ageValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_AGE));
        fixmeValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_FIXME));
        missingTagValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_MISSING_TAG));
        highwayRoadValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_HIGHWAY_ROAD));
        noTypeValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_NO_TYPE));
        imperialUnitsValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_IMPERIAL_UNITS));
        invalidObjectValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_INVALID_OBJECT));
        untaggedValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_UNTAGGED));
        unconnectedEndNodeValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_UNCONNECTED_END_NODE));
        degenerateWayValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_DEGENERATE_WAY));
        emptyRelationValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_EMPTY_RELATION));
        wrongTypeValidation = enabledValidations.contains(ctx.getString(R.string.VALIDATION_WRONG_ELEMENT_TYPE));

        try (ValidatorRulesDatabaseHelper vrDb = new ValidatorRulesDatabaseHelper(ctx); SQLiteDatabase db = vrDb.getReadableDatabase()) {
            resurveyTags = ValidatorRulesDatabase.getDefaultResurvey(db);
            checkTags = ValidatorRulesDatabase.getDefaultCheck(db);
        }
        cachedViewBox = null;
    }

    /**
     * Test if the element has any problems by searching all the tags for the words "fixme" or "todo", or if it has a
     * //NOSONAR key in the list of things to regularly re-survey
     * 
     * @param status status before calling this method
     * @param e the OsmElement
     * @param tags the associated tags
     * @param pi a matching PrestItem or null
     * @return the output status
     */
    private int validateElement(int status, @NonNull OsmElement e, @NonNull SortedMap<String, String> tags, @Nullable PresetItem pi) {
        // test for fixme etc // NOSONAR
        if (fixmeValidation) {
            status = validateFixme(status, tags);
        }

        // age check
        if (ageValidation && resurveyTags != null) {
            status = validateResurvey(status, e, tags);
        }

        // find missing keys
        if (missingTagValidation && pi != null && checkTags != null) {
            status = validateMissingTags(status, e, pi);
        }

        // check element type
        if (wrongTypeValidation && pi != null) {
            status = validateWrongType(status, e, pi);
        }
        return status;
    }

    /**
     * Check if the element is not one of the ElementTypes required by the PresetItem
     * 
     * @param status previous validation status
     * @param e the OsmElement
     * @param pi the PresetItem
     * @return new validation status
     */
    public int validateWrongType(int status, @NonNull OsmElement e, @NonNull PresetItem pi) {
        List<ElementType> elementTypes = pi.appliesTo();
        ElementType type = e.getType();
        // presets currently can't model just simple closed ways as areas
        if (e instanceof Way && type == ElementType.AREA && elementTypes.contains(ElementType.CLOSEDWAY) && areaTags.isImpliedArea(e.getTags())) {
            return status;
        }
        if (!elementTypes.contains(type)) {
            status |= Validator.WRONG_ELEMENT_TYPE;
        }
        return status;
    }

    /**
     * Check if a preset required tag is missing
     * 
     * @param status previous validation status
     * @param e the OsmElement
     * @param pi the PresetItem
     * @return new validation status
     */
    public int validateMissingTags(int status, @NonNull OsmElement e, @NonNull PresetItem pi) {
        for (Entry<String, Boolean> entry : checkTags.entrySet()) {
            String[] keys = splitKeys(entry);
            int tempStatus = 0;
            for (String key : keys) {
                key = key.trim();
                if (pi.hasKey(key, entry.getValue())) {
                    if (!e.hasTagKey(key) && reportMissingKey(e, key)) {
                        tempStatus = Validator.MISSING_TAG;
                    } else {
                        tempStatus = 0; // found a key
                        break;
                    }
                }
            }
            status |= tempStatus;
        }
        return status;
    }

    /**
     * Check if the element is out of date and should be resurveyed
     * 
     * @param status previous validation status
     * @param e the OsmElement
     * @param tags the tags
     * @return new validation status
     */
    public int validateResurvey(int status, @NonNull OsmElement e, @NonNull SortedMap<String, String> tags) {
        long now = System.currentTimeMillis() / 1000;
        long timestamp = e.getTimestamp();
        for (String key : resurveyTags.getKeys()) {
            if (!tags.containsKey(key)) {
                continue;
            }
            for (PatternAndAge value : resurveyTags.get(key)) {
                if ((value.getValue() == null || "".equals(value.getValue()) || value.matches(tags.get(key)))) {
                    long age = value.getAge();
                    // timestamp is too old
                    if (timestamp >= 0 && (now - timestamp > age)) {
                        status |= Validator.AGE;
                    } else if (tags.containsKey(Tags.KEY_CHECK_DATE)) {
                        // check_date tag is too old
                        status |= checkAge(tags, now, Tags.KEY_CHECK_DATE, age);
                    } else {
                        // key specific check_date tag is too old
                        final String keyCheckDate = Tags.KEY_CHECK_DATE + ":" + key;
                        if (tags.containsKey(keyCheckDate)) {
                            status |= checkAge(tags, now, keyCheckDate, age);
                        }
                    }
                }
            }
        }
        return status;
    }

    /**
     * Check if the tags contain a fixme tag or similar //NOSONAR
     * 
     * @param status previous validation status
     * @param tags the tags
     * @return new validation status
     */
    public int validateFixme(int status, SortedMap<String, String> tags) {
        for (Entry<String, String> entry : new ArrayList<>(tags.entrySet())) {
            // test key and value against pattern
            if (FIXME_PATTERN.matcher(entry.getKey()).matches() || FIXME_PATTERN.matcher(entry.getValue()).matches()) {
                status |= Validator.FIXME;
            }
        }
        return status;
    }

    /**
     * Split a key from the missing keys data
     * 
     * @param entry the Entry
     * @return an array of individual keys
     */
    @NonNull
    private String[] splitKeys(@NonNull Entry<String, Boolean> entry) {
        return entry.getKey().split("\\|");
    }

    /**
     * Validate a Way with a highway tag
     * 
     * @param w the Way
     * @param highway the value of the highway tag
     * @return the status
     */
    int validateHighway(@NonNull Way w, @NonNull String highway) {
        int result = Validator.NOT_VALIDATED;

        if (highwayRoadValidation && Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
            // unsurveyed road
            result |= Validator.HIGHWAY_ROAD;
        }
        if (imperialUnitsValidation && geoContext != null) {
            boolean imperial = geoContext.imperial(w);
            if (imperial) {
                SortedMap<String, String> tags = w.getTags();
                Set<String> keys = w.getTags().keySet();
                for (String key : keys) {
                    if (Tags.isSpeedKey(key) && !tags.get(key).endsWith(Tags.MPH)) {
                        return result | Validator.IMPERIAL_UNITS;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Check the end nodes of the way for potential mergers
     * 
     * @param w the way we are checking
     * @param key the key of target ways
     */
    private void validateEndNodes(@NonNull Way w, @NonNull String key) {
        Logic logic = App.getLogic();
        int layer = getLayer(w);
        io.vespucci.Map map = logic.getMap();
        if (unconnectedEndNodeValidation && map != null) {
            // we try to cache these (tolerance) fairly expensive to calculate values at least as long as the ViewBox
            // hasn't changed
            final ViewBox viewBox = map.getViewBox();
            if (!viewBox.equals(cachedViewBox) && map.getPrefs() != null) {
                double centerLat = viewBox.getCenterLat();
                double widthInMeters = GeoMath.haversineDistance(viewBox.getLeft() / 1E7D, centerLat, viewBox.getRight() / 1E7D, centerLat);
                tolerance = (float) (map.getPrefs().getConnectedNodeTolerance() / widthInMeters * map.getWidth());
                if (cachedViewBox == null) {
                    cachedViewBox = new ViewBox(viewBox);
                } else {
                    cachedViewBox.set(viewBox);
                }
            }
            try {
                checkNearbyWays(key, w, logic, layer, w.getFirstNode());
                checkNearbyWays(key, w, logic, layer, w.getLastNode());
            } catch (Exception ex) {
                // ignored
            }
        }
    }

    /**
     * Check if the node is too near any ways within the tolerance
     * 
     * The warning is suppressed if the node is connected to the nearby way via a (single) further way
     * 
     * @param tagKey tag key the ways need to have to be candidates
     * @param w the Way we are validating
     * @param logic the current Logic instance
     * @param layer the layer of w
     * @param n the Node we are checking for
     * 
     * @throws OsmException if something goes wrong creating the bounding box
     */
    private void checkNearbyWays(@NonNull String tagKey, @NonNull Way w, @NonNull Logic logic, int layer, @NonNull Node n) throws OsmException {
        final int lat = n.getLat();
        final int lon = n.getLon();
        if (!App.getDelegator().isInDownload(lon, lat)) { // only check for nodes in download
            return;
        }
        BoundingBox box = GeoMath.createBoundingBoxForCoordinates(lat / 1E7D, lon / 1E7D, tolerance);
        List<Way> nearbyWays = App.getDelegator().getCurrentStorage().getWays(box);
        List<Way> connectedWays = new ArrayList<>();
        BoundingBox bb = w.getBounds();
        for (Way maybeConnected : new ArrayList<>(nearbyWays)) {
            if (maybeConnected.equals(w) || !hasTagKey(tagKey, maybeConnected)) {
                nearbyWays.remove(maybeConnected);
                continue;
            }
            if (bb.intersects(maybeConnected.getBounds()) && maybeConnected.hasCommonNode(w)) {
                connectedWays.add(maybeConnected);
                nearbyWays.remove(maybeConnected);
            }
        }
        for (Way nearbyWay : nearbyWays) {
            if (!hasConnection(nearbyWay, connectedWays) && layer == getLayer(nearbyWay)) {
                connectedValidation(logic, tolerance, nearbyWay, n);
                if ((n.getCachedProblems() & Validator.UNCONNECTED_END_NODE) != 0) {
                    break;
                }
            }
        }
    }

    /**
     * Check if the way has a specific key or has a parent MP with that key
     * 
     * @param key the key we are checking for
     * @param way the way to check
     * @return true if the way has a specific key or has a parent MP with that key
     */
    private boolean hasTagKey(@NonNull String key, @NonNull Way way) {
        if (way.hasTagKey(key)) {
            return true;
        }
        // check for MPs
        List<Relation> parents = way.getParentRelations();
        if (parents != null) {
            for (Relation parent : parents) {
                if (parent.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON) && parent.hasTagKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a way has a connection to one of a List of ways
     * 
     * @param way the way
     * @param candidateWays the list of candidates
     * @return true if we have a common node with one of the ways
     */
    private boolean hasConnection(@NonNull Way way, @NonNull List<Way> candidateWays) {
        BoundingBox bb = way.getBounds();
        for (Way c : candidateWays) {
            if (bb.intersects(c.getBounds()) && way.hasCommonNode(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value of the layer tag for a way
     * 
     * @param w the way
     * @return the layer value or 0
     */
    private int getLayer(Way w) {
        try {
            return w.hasTagKey(Tags.KEY_LAYER) ? Integer.parseInt(w.getTagWithKey(Tags.KEY_LAYER)) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Check if a Node is so near a Way that it should be connected
     * 
     * @param logic the current Logic instance
     * @param tolerance how far away the node has to be from the line in screen pixel units
     * @param way the Way
     * @param node the Node
     */
    private void connectedValidation(@NonNull Logic logic, float tolerance, @NonNull Way way, @NonNull Node node) {
        if (!way.hasNode(node)) {
            float jx = logic.lonE7ToX(node.getLon());
            float jy = logic.latE7ToY(node.getLat());
            List<Node> wayNodes = way.getNodes();
            Node firstNode = wayNodes.get(0);
            float node1X = logic.lonE7ToX(firstNode.getLon());
            float node1Y = logic.latE7ToY(firstNode.getLat());
            double nodeDist = Math.hypot(jx - node1X, jy - node1Y); // first node
            if (nodeDist < tolerance) {
                addProblem(node, Validator.UNCONNECTED_END_NODE);
                return;
            }
            for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
                Node node2 = wayNodes.get(i);
                float node2X = logic.lonE7ToX(node2.getLon());
                float node2Y = logic.latE7ToY(node2.getLat());
                if (Geometry.isPositionOnLine(tolerance, jx, jy, node1X, node1Y, node2X, node2Y) >= 0) {
                    addProblem(node, Validator.UNCONNECTED_END_NODE);
                    break;
                }
                node1X = node2X;
                node1Y = node2Y;
            }
            nodeDist = Math.hypot(jx - node1X, jy - node1Y); // last node
            if (nodeDist < tolerance) {
                addProblem(node, Validator.UNCONNECTED_END_NODE);
            }
        }
    }

    /**
     * Set a specific problem bit
     * 
     * @param e the OsmElement
     * @param problem the problem value
     */
    protected void addProblem(@NonNull OsmElement e, int problem) {
        e.setProblem(e.getCachedProblems() | problem);
    }

    /**
     * Delete a specific problem bit
     * 
     * @param e the OsmElement
     * @param problem the problem value
     */
    protected void deleteProblem(@NonNull OsmElement e, int problem) {
        e.setProblem(e.getCachedProblems() & ~problem);
    }

    /**
     * Check that the date value of tag is not more that validFor seconds old
     * 
     * @param tags tags for the element
     * @param now current time in milliseconds
     * @param tag tag to retrieve the date value for
     * @param validFor time the element is valid for in seconds
     * @return Validator.AGE is too old, otherwise Validator.NOT_VALIDATED
     */
    private int checkAge(@NonNull SortedMap<String, String> tags, long now, @NonNull String tag, long validFor) {
        try {
            return now - new SimpleDateFormat(Tags.CHECK_DATE_FORMAT, Locale.US).parse(tags.get(tag)).getTime() / 1000 > validFor ? Validator.AGE
                    : Validator.NOT_VALIDATED;
        } catch (ParseException e) {
            return Validator.AGE;
        }
    }

    /**
     * Return a List of Strings describing the problems detected in calcProblem
     * 
     * @param ctx Android Context
     * @param e OsmElement
     * @param tags the associates tags
     * @param pi a matching PrestItem or null
     * @return a List of Strings describing the problems
     */
    @NonNull
    private List<String> describeProblemElement(@NonNull Context ctx, @NonNull OsmElement e, @NonNull SortedMap<String, String> tags, @Nullable PresetItem pi) {
        List<String> result = new ArrayList<>();
        int cachedProblems = e.getCachedProblems();
        // invalid OSM element
        if ((cachedProblems & Validator.INVALID_OBJECT) != 0) {
            result.add(ctx.getString(R.string.toast_invalid_object));
        }

        if ((cachedProblems & Validator.UNTAGGED) != 0) {
            result.add(ctx.getString(R.string.toast_untagged_element));
        }

        // fixme etc. // NOSONAR
        for (Entry<String, String> entry : tags.entrySet()) {
            // test key and value against pattern
            if (FIXME_PATTERN.matcher(entry.getKey()).matches() || FIXME_PATTERN.matcher(entry.getValue()).matches()) {
                result.add(entry.getKey() + ": " + entry.getValue());
            }
        }

        // resurvey age
        if ((cachedProblems & Validator.AGE) != 0) {
            result.add(ctx.getString(R.string.toast_needs_resurvey));
        }

        // missing tags
        if (pi != null && checkTags != null) {
            for (Entry<String, Boolean> entry : checkTags.entrySet()) {
                String[] keys = splitKeys(entry);
                for (String key : keys) {
                    key = key.trim();
                    if (pi.hasKey(key, entry.getValue()) && !e.hasTagKey(key) && reportMissingKey(e, key)) {
                        String hint = pi.getHint(key);
                        result.add(ctx.getString(R.string.toast_missing_key, hint != null ? hint : key));
                    }
                }
            }
        }

        // wrong element type
        if (pi != null) {
            List<ElementType> elementType = pi.appliesTo();
            final ElementType type = e.getType();
            if (!elementType.contains(type)) {
                result.add(ctx.getString(R.string.toast_wrong_element_type, ctx.getString(ELEMENT_TYPE_NAMES.get(type))));
            }
        }
        return result;
    }

    /**
     * Get the regions the element is located in
     * 
     * @param e the OsmElement
     * @return the regions the element is located in or null
     */
    @Nullable
    List<String> getIsoCodes(@NonNull OsmElement e) {
        return geoContext.getIsoCodes(e);
    }

    /**
     * Check if missing key validation isn't suppressed by other tags
     * 
     * @param e the OsmElement to validate
     * @param key the missing key
     * @return true if we need to report
     */
    private boolean reportMissingKey(@NonNull OsmElement e, @NonNull String key) {
        Set<KeyValue> suppressionTags = MISSING_KEY_SUPPRESSION.get(key);
        for (KeyValue tag : suppressionTags) {
            if (e.hasTagWithValue(tag.getKey(), tag.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return a List of Strings describing the problems for a Way with a highway tag
     * 
     * @param ctx Android Context
     * @param w the Way
     * @param highway the value of the highway tag
     * @return a List containing the problem descriptions
     */
    List<String> describeProblemHighway(@NonNull Context ctx, @NonNull Way w, @NonNull String highway) {
        List<String> wayProblems = new ArrayList<>();
        if (Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
            wayProblems.add(ctx.getString(R.string.toast_unsurveyed_road));
        }
        //
        if ((w.getCachedProblems() & Validator.IMPERIAL_UNITS) != 0) {
            wayProblems.add(ctx.getString(R.string.toast_imperial_units));
        }
        return wayProblems;
    }

    @Override
    public int validate(@NonNull Node node) {
        int status = Validator.NOT_VALIDATED;
        SortedMap<String, String> tags = node.getTags();
        if (!tags.isEmpty()) {
            // tag based checks
            status = validateElement(status, node, tags, Preset.findBestMatch(presets, tags, getIsoCodes(node), null));
        }
        if (status == Validator.NOT_VALIDATED) {
            status = Validator.OK;
        }
        return status;
    }

    @Override
    public int validate(@NonNull Way way) {
        if (invalidObjectValidation && way.getNodes().isEmpty()) {
            return Validator.INVALID_OBJECT;
        }
        // reset status of end nodes
        deleteProblem(way.getFirstNode(), Validator.UNCONNECTED_END_NODE);
        deleteProblem(way.getLastNode(), Validator.UNCONNECTED_END_NODE);

        int status = Validator.NOT_VALIDATED;
        if (degenerateWayValidation && way.nodeCount() == 1) {
            status |= Validator.DEGENERATE_WAY;
        }
        SortedMap<String, String> tags = way.getTags();
        boolean noTags = tags.isEmpty();
        if (untaggedValidation && noTags && !way.hasParentRelations()) {
            status |= Validator.UNTAGGED;
        }
        if (!noTags) {
            // tag based checks
            PresetItem pi = Preset.findBestMatch(presets, tags, getIsoCodes(way), null);
            status = validateElement(status, way, tags, pi);
            String highway = way.getTagWithKey(Tags.KEY_HIGHWAY);
            if (highway != null) {
                status |= validateHighway(way, highway);
            }
            if (!way.isClosed()) {
                for (String key : END_NODE_VALIDATION_KEYS) {
                    if (hasTagKey(key, way)) {
                        validateEndNodes(way, key);
                        break;
                    }
                }
            }
        }
        if (status == Validator.NOT_VALIDATED) {
            status = Validator.OK;
        }
        return status;
    }

    @Override
    public int validate(@NonNull Relation relation) {
        int status = Validator.NOT_VALIDATED;
        SortedMap<String, String> tags = relation.getTags();
        boolean noTags = tags.isEmpty();
        // tag based checks
        if (noTags) {
            if (untaggedValidation) {
                status |= Validator.UNTAGGED | Validator.NO_TYPE;
            }
        } else {
            PresetItem pi = Preset.findBestMatch(presets, tags, getIsoCodes(relation), null);
            status = validateElement(status, relation, tags, pi);
            if (noTypeValidation && noType(relation)) {
                status |= Validator.NO_TYPE;
            }
        }
        List<RelationMember> members = relation.getMembers();
        if (emptyRelationValidation && (members == null || members.isEmpty())) {
            status |= Validator.EMPTY_RELATION;
        }
        if (status == Validator.NOT_VALIDATED) {
            status = Validator.OK;
        }
        return status;
    }

    /**
     * Check if a Relation has a type tag with value
     * 
     * @param r the Relation
     * @return true if no complete type tag
     */
    private boolean noType(Relation r) {
        String type = r.getTagWithKey(Tags.KEY_TYPE);
        return type == null || "".equals(type);
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull Node node) {
        SortedMap<String, String> tags = node.getTags();
        List<String> result = new ArrayList<>();
        result.addAll(describeProblemElement(ctx, node, tags, Preset.findBestMatch(presets, tags, getIsoCodes(node), null)));
        if ((node.getCachedProblems() & Validator.UNCONNECTED_END_NODE) != 0) {
            result.add(ctx.getString(R.string.toast_unconnected_end_node));
        }
        return result.toArray(new String[result.size()]);
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull Way way) {
        SortedMap<String, String> tags = way.getTags();
        List<String> result = new ArrayList<>();
        result.addAll(describeProblemElement(ctx, way, tags, Preset.findBestMatch(presets, tags, getIsoCodes(way), null)));
        if ((way.getCachedProblems() & Validator.DEGENERATE_WAY) != 0) {
            result.add(ctx.getString(R.string.toast_degenerate_way));
        }
        String highway = way.getTagWithKey(Tags.KEY_HIGHWAY);
        if (highway != null) {
            result.addAll(describeProblemHighway(ctx, way, highway));
        }
        return result.toArray(new String[result.size()]);
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull Relation relation) {
        SortedMap<String, String> tags = relation.getTags();
        List<String> result = new ArrayList<>();
        PresetItem pi = Preset.findBestMatch(presets, tags, getIsoCodes(relation), null);
        result.addAll(describeProblemElement(ctx, relation, tags, pi));
        if (noType(relation)) {
            result.add(ctx.getString(R.string.toast_notype));
        }
        if ((relation.getCachedProblems() & Validator.EMPTY_RELATION) != 0) {
            result.add(ctx.getString(R.string.empty_relation_title));
        }
        if ((relation.getCachedProblems() & Validator.MISSING_ROLE) != 0) {
            result.add(ctx.getString(R.string.toast_missing_role));
        }
        if ((relation.getCachedProblems() & Validator.RELATION_LOOP) != 0) {
            result.add(ctx.getString(R.string.toast_relation_loop));
        }
        return result.toArray(new String[result.size()]);
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull OsmElement e) {
        if (e instanceof Node) {
            return describeProblem(ctx, (Node) e);
        }
        if (e instanceof Way) {
            return describeProblem(ctx, (Way) e);
        }
        if (e instanceof Relation) {
            return describeProblem(ctx, (Relation) e);
        }
        return new String[] {};
    }

    /**
     * Get the current presets
     * 
     * @return the presets object
     */
    @NonNull
    Preset[] getPresets() {
        return presets;
    }
}