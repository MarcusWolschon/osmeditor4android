package de.blau.android.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.GeoContext;
import de.blau.android.util.GeoMath;
import de.blau.android.util.collections.MultiHashMap;

public class BaseValidator implements Validator {
    private static final String DEBUG_TAG = BaseValidator.class.getSimpleName();

    private Preset[] presets;

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
    private double  centerLat;
    private double  widthInMeters;
    private float   tolerance;

    /**
     * Regex for general tagged issues with the object
     */
    static final Pattern FIXME_PATTERN = Pattern.compile("(?i).*\\b(?:fixme|todo)\\b.*");

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
        SQLiteDatabase db = (new ValidatorRulesDatabaseHelper(ctx)).getReadableDatabase();
        resurveyTags = ValidatorRulesDatabase.getDefaultResurvey(db);
        checkTags = ValidatorRulesDatabase.getDefaultCheck(db);
        db.close();
        cachedViewBox = null;
    }

    /**
     * Test if the element has any problems by searching all the tags for the words "fixme" or "todo", or if it has a
     * key in the list of things to regularly re-survey
     * 
     * @param status status before calling this method
     * @param e the OsmElement
     * @param tags the associated tags
     * @return the output status
     */
    int validateElement(int status, OsmElement e, SortedMap<String, String> tags) {
        // test for fixme etc
        for (Entry<String, String> entry : new ArrayList<>(tags.entrySet())) {
            // test key and value against pattern
            if (FIXME_PATTERN.matcher(entry.getKey()).matches() || FIXME_PATTERN.matcher(entry.getValue()).matches()) {
                status = status | Validator.FIXME;
            }
        }

        // age check
        if (resurveyTags != null) {
            for (String key : resurveyTags.getKeys()) {
                Set<PatternAndAge> values = resurveyTags.get(key);
                for (PatternAndAge value : values) {
                    if (tags.containsKey(key) && (value.getValue() == null || "".equals(value.getValue()) || value.matches(tags.get(key)))) {
                        long now = System.currentTimeMillis() / 1000;
                        long timestamp = e.getTimestamp();
                        if (timestamp >= 0 && (now - timestamp > value.getAge())) {
                            status = status | Validator.AGE;
                            break;
                        }
                        if (tags.containsKey(Tags.KEY_CHECK_DATE)) {
                            status = status | checkAge(tags, now, Tags.KEY_CHECK_DATE, value.getAge());
                            break;
                        }
                        if (tags.containsKey(Tags.KEY_CHECK_DATE + ":" + key)) {
                            status = status | checkAge(tags, now, Tags.KEY_CHECK_DATE + ":" + key, value.getAge());
                            break;
                        }
                    }
                }
            }
        }

        // find missing keys
        PresetItem pi = Preset.findBestMatch(presets, tags);
        if (pi != null && checkTags != null) {
            for (Entry<String, Boolean> entry : checkTags.entrySet()) {
                String key = entry.getKey();
                if (pi.hasKey(key, entry.getValue())) {
                    if (!e.hasTagKey(key)) {
                        if (!(Tags.KEY_NAME.equals(key)
                                && (e.hasTagWithValue(Tags.KEY_NONAME, Tags.VALUE_YES) || e.hasTagWithValue(Tags.KEY_VALIDATE_NO_NAME, Tags.VALUE_YES)))) {
                            status = status | Validator.MISSING_TAG;
                        }
                    }
                }
            }
        }
        return status;
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
        Logic logic = App.getLogic();
        List<Way> nearbyWays = App.getDelegator().getCurrentStorage().getWays(w.getBounds());
        int layer = getLayer(w);
        boolean nothingToCheck = true;
        de.blau.android.Map map = logic.getMap();
        if (map != null) {
            // we try to cache these fairly expensive to calculate values at least as long as the ViewBox hasn't changed
            if (!map.getViewBox().equals(cachedViewBox)) {
                centerLat = map.getViewBox().getCenterLat();
                widthInMeters = GeoMath.haversineDistance(map.getViewBox().getLeft() / 1E7D, centerLat, map.getViewBox().getRight() / 1E7D, centerLat);
                tolerance = (float) (map.getPrefs().getConnectedNodeTolerance() / widthInMeters * map.getWidth());
                if (cachedViewBox == null) {
                    cachedViewBox = new ViewBox(map.getViewBox());
                } else {
                    cachedViewBox.set(map.getViewBox());
                }
            }
            for (Way nearbyWay : nearbyWays) {
                if (!w.equals(nearbyWay) && nearbyWay.hasTagKey(Tags.KEY_HIGHWAY) && layer == getLayer(nearbyWay)) {
                    connectedValidation(logic, tolerance, nearbyWay, w.getFirstNode());
                    connectedValidation(logic, tolerance, nearbyWay, w.getLastNode());
                    nothingToCheck = false;
                }
            }
        }
        if (nothingToCheck) {
            w.getFirstNode().setProblem(Validator.OK);
            w.getLastNode().setProblem(Validator.OK);
        }

        if (Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
            // unsurveyed road
            result = result | Validator.HIGHWAY_ROAD;
        }
        GeoContext geoContext = App.getGeoContext();
        if (geoContext != null) {
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
            node.setProblem(Validator.OK);
            float jx = logic.lonE7ToX(node.getLon());
            float jy = logic.latE7ToY(node.getLat());
            List<Node> wayNodes = way.getNodes();
            Node firstNode = wayNodes.get(0);
            float node1X = logic.lonE7ToX(firstNode.getLon());
            float node1Y = logic.latE7ToY(firstNode.getLat());
            for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
                Node node2 = wayNodes.get(i);
                float node2X = logic.lonE7ToX(node2.getLon());
                float node2Y = logic.latE7ToY(node2.getLat());
                if (Logic.isPositionOnLine(tolerance, jx, jy, node1X, node1Y, node2X, node2Y) >= 0) {
                    node.setProblem(Validator.UNCONNECTED_END_NODE);
                }
                node1X = node2X;
                node1Y = node2Y;
            }
        }
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
     * @return a List of Strings describing the problems
     */
    @NonNull
    public List<String> describeProblemElement(@NonNull Context ctx, @NonNull OsmElement e, SortedMap<String, String> tags) {
        List<String> result = new ArrayList<>();

        // invalid OSM element
        if ((e.getCachedProblems() & Validator.INVALID_OBJECT) != 0) {
            result.add(ctx.getString(R.string.toast_invalid_object));
        }

        // fixme etc.
        for (Entry<String, String> entry : tags.entrySet()) {
            // test key and value against pattern
            if (FIXME_PATTERN.matcher(entry.getKey()).matches() || FIXME_PATTERN.matcher(entry.getValue()).matches()) {
                result.add(entry.getKey() + ": " + entry.getValue());
            }
        }

        // resurvey age
        if ((e.getCachedProblems() & Validator.AGE) != 0) {
            result.add(ctx.getString(R.string.toast_needs_resurvey));
        }

        // missing tags
        PresetItem pi = Preset.findBestMatch(presets, tags);
        if (pi != null && checkTags != null) {
            for (Entry<String, Boolean> entry : checkTags.entrySet()) {
                String key = entry.getKey();
                if (pi.hasKey(key, entry.getValue())) {
                    if (!e.hasTagKey(key)) {
                        if (!(Tags.KEY_NAME.equals(key)
                                && (e.hasTagWithValue(Tags.KEY_NONAME, Tags.VALUE_YES) || e.hasTagWithValue(Tags.KEY_VALIDATE_NO_NAME, Tags.VALUE_YES)))) {
                            String hint = pi.getHint(key);
                            result.add(ctx.getString(R.string.toast_missing_key, hint != null ? hint : key));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Return a List of Strings describing the problems for a Way with a highway tag
     * 
     * @param ctx Android Context
     * @param w the Way
     * @param highway the value of the highway tag
     * @return a List containing the problem descriptions
     */
    List<String> describeProblemHighway(Context ctx, Way w, String highway) {
        List<String> wayProblems = new ArrayList<>();
        if (Tags.VALUE_ROAD.equalsIgnoreCase(highway)) {
            wayProblems.add(App.resources().getString(R.string.toast_unsurveyed_road));
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
        // tag based checks
        status = validateElement(status, node, tags);
        if (status == Validator.NOT_VALIDATED) {
            status = Validator.OK;
        }
        return status;
    }

    @Override
    public int validate(@NonNull Way way) {
        if (way.getNodes() == null || way.getNodes().isEmpty()) {
            return Validator.INVALID_OBJECT;
        }
        int status = Validator.NOT_VALIDATED;
        SortedMap<String, String> tags = way.getTags();
        // tag based checks
        status = validateElement(status, way, tags);
        if (tags.isEmpty() && !way.hasParentRelations()) {
            status = status | Validator.UNTAGGED;
        }
        String highway = way.getTagWithKey(Tags.KEY_HIGHWAY);
        if (highway != null) {
            status = status | validateHighway(way, highway);
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
        // tag based checks
        if (tags.isEmpty()) {
            status = status | Validator.UNTAGGED;
        }
        status = validateElement(status, relation, tags);
        if (noType(relation)) {
            status = status | Validator.NO_TYPE;
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
        result.addAll(describeProblemElement(ctx, node, tags));
        if ((node.getCachedProblems() & Validator.UNCONNECTED_END_NODE) != 0) {
            result.add(ctx.getString(R.string.toast_unconnected_end_node));
        }
        String[] resultArray = result.toArray(new String[result.size()]);
        return resultArray;
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull Way way) {
        SortedMap<String, String> tags = way.getTags();
        List<String> result = new ArrayList<>();
        result.addAll(describeProblemElement(ctx, way, tags));
        // invalid OSM element
        if ((way.getCachedProblems() & Validator.UNTAGGED) != 0) {
            result.add(ctx.getString(R.string.toast_untagged_way));
        }
        String highway = way.getTagWithKey(Tags.KEY_HIGHWAY);
        if (highway != null) {
            result.addAll(describeProblemHighway(ctx, way, highway));
        }
        String[] resultArray = result.toArray(new String[result.size()]);
        return resultArray;
    }

    @NonNull
    @Override
    public String[] describeProblem(@NonNull Context ctx, @NonNull Relation relation) {
        SortedMap<String, String> tags = relation.getTags();
        List<String> result = new ArrayList<>();
        result.addAll(describeProblemElement(ctx, relation, tags));
        if ((relation.getCachedProblems() & Validator.UNTAGGED) != 0) {
            result.add(ctx.getString(R.string.toast_untagged_relation));
        }
        if (noType(relation)) {
            result.add(App.resources().getString(R.string.toast_notype));
        }
        String[] resultArray = result.toArray(new String[result.size()]);
        return resultArray;
    }

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
        return null;
    }
}