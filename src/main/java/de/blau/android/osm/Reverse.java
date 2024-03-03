package de.blau.android.osm;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.JosmFilterParseException;
import ch.poole.osm.josmfilterparser.JosmFilterParser;
import de.blau.android.search.Wrapper;

/**
 * Logic for reversing direction dependent tags, this is one of the more arcane things about the OSM data model
 * 
 * @author simon
 *
 */
final class Reverse {

    private static final String DEBUG_TAG = Reverse.class.getSimpleName();

    private static final String LEFT_INFIX       = ":left:";
    private static final String RIGHT_INFIX      = ":right:";
    private static final String BACKWARD_INFIX   = ":backward:";
    private static final String FORWARD_INFIX    = ":forward:";
    private static final String FORWARD_POSTFIX  = ":forward";
    private static final String BACKWARD_POSTFIX = ":backward";
    private static final String RIGHT_POSTFIX    = ":right";
    private static final String LEFT_POSTFIX     = ":left";
    private static final String PERCENT          = "%";
    private static final String DEGREE           = "°";

    private static Set<String> directionDependentKeys   = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Tags.KEY_ONEWAY, Tags.KEY_INCLINE, Tags.KEY_DIRECTION, Tags.KEY_CONVEYING, Tags.KEY_PRIORITY)));
    private static Set<String> directionDependentValues = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Tags.VALUE_RIGHT, Tags.VALUE_LEFT, Tags.VALUE_FORWARD, Tags.VALUE_BACKWARD)));

    private static Map<String, Condition> reverseExceptions = new HashMap<>();
    static {
        try {
            reverseExceptions.put(Tags.KEY_SIDE, compilePattern("highway=cyclist_waiting_aid -child (type:way highway: (oneway? OR oneway=\"-1\"))"));
        } catch (JosmFilterParseException e) {
            Log.e(DEBUG_TAG, e.getMessage());
        }
    }

    /**
     * Private constructor
     */
    private Reverse() {
        // don't allow instantiating of this class
    }

    /**
     * Return the direction dependent tags and associated values oneway, *:left, *:right, *:backward, *:forward from the
     * element
     * 
     * @param e element that we extract the direction dependent tags from
     * @return map containing the tags
     */
    @NonNull
    public static Map<String, String> getDirectionDependentTags(@NonNull OsmElement e) {
        Map<String, String> result = new TreeMap<>();
        Map<String, String> tags = e.getTags();
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (((Tags.KEY_HIGHWAY.equals(key) && (Tags.VALUE_MOTORWAY.equals(value) || Tags.VALUE_MOTORWAY_LINK.equals(value)))
                    || directionDependentKeys.contains(key) || key.endsWith(LEFT_POSTFIX) || key.endsWith(RIGHT_POSTFIX) || key.endsWith(BACKWARD_POSTFIX)
                    || key.endsWith(FORWARD_POSTFIX) || key.contains(FORWARD_INFIX) || key.contains(BACKWARD_INFIX) || key.contains(RIGHT_INFIX)
                    || key.contains(LEFT_INFIX) || directionDependentValues.contains(value)) && !matchExceptions(e, key)) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Check if a key is in the exceptions map and the object matches the pattern
     * 
     * @param e the OsmElement
     * @param key the key to check
     * @return true if this is an exception we should not reverse
     */
    private static boolean matchExceptions(@NonNull OsmElement e, @NonNull String key) {
        Condition c = reverseExceptions.get(key);
        if (c != null) {
            c.reset();
            Wrapper meta = new Wrapper();
            meta.setElement(e);
            return c.eval(Wrapper.toJosmFilterType(e), meta, e.getTags());
        }
        return false;
    }

    /**
     * Return a list of (route) relations that the element is a member of with a direction dependent role
     * 
     * @param e element for which we need to inspect the parent relations
     * @return List of relations or null if none found
     */
    @NonNull
    public static List<Relation> getRelationsWithDirectionDependentRoles(@NonNull OsmElement e) {
        List<Relation> result = new ArrayList<>();
        List<Relation> parents = e.getParentRelations();
        if (parents != null) {
            for (Relation r : parents) {
                String t = r.getTagWithKey(Tags.KEY_TYPE);
                if (Tags.VALUE_ROUTE.equals(t)) {
                    RelationMember rm = r.getMember(Way.NAME, e.getOsmId());
                    if (rm != null && (Tags.ROLE_FORWARD.equals(rm.getRole()) || Tags.ROLE_BACKWARD.equals(rm.getRole()))) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reverse the role of this element in any relations it is in (currently only relevant for routes)
     * 
     * @param e element
     * @param relations list of the relations in which e needs it role to be reversed
     */
    public static void reverseRoleDirection(@NonNull OsmElement e, @Nullable List<Relation> relations) {
        if (relations != null) {
            for (Relation r : relations) {
                for (RelationMember rm : r.getAllMembers(e)) {
                    if (Tags.ROLE_FORWARD.equals(rm.role)) {
                        rm.setRole(Tags.ROLE_BACKWARD);
                    } else if (Tags.ROLE_BACKWARD.equals(rm.role)) {
                        rm.setRole(Tags.ROLE_FORWARD);
                    }
                }
            }
        }
    }

    /**
     * Reverse a cardinal direction
     * 
     * @param value the value to reverse
     * @return the reversed value
     */
    private static String reverseCardinalDirection(final String value) {
        StringBuilder tmpVal = new StringBuilder("");
        for (int i = 0; i < value.length(); i++) {
            switch (value.toUpperCase(Locale.US).charAt(i)) {
            case Tags.VALUE_NORTH:
                tmpVal.append(Tags.VALUE_SOUTH);
                break;
            case Tags.VALUE_WEST:
                tmpVal.append(Tags.VALUE_EAST);
                break;
            case Tags.VALUE_SOUTH:
                tmpVal.append(Tags.VALUE_NORTH);
                break;
            case Tags.VALUE_EAST:
                tmpVal.append(Tags.VALUE_WEST);
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
        return tmpVal.toString();
    }

    /**
     * REverse the value of a direction tag
     * 
     * @param value the value to reverse
     * @return the reversed value
     */
    private static String reverseDirection(final String value) {
        if (Tags.VALUE_UP.equals(value)) {
            return Tags.VALUE_DOWN;
        }
        if (Tags.VALUE_DOWN.equals(value)) {
            return Tags.VALUE_UP;
        }
        if (Tags.VALUE_FORWARD.equals(value)) {
            return Tags.VALUE_BACKWARD;
        }
        if (Tags.VALUE_BACKWARD.equals(value)) {
            return Tags.VALUE_FORWARD;
        }
        if (value.endsWith(DEGREE)) { // degrees
            try {
                String tmpVal = value.substring(0, value.length() - 1);
                return floatToString(((Float.valueOf(tmpVal) + 180.0f) % 360.0f)) + DEGREE;
            } catch (NumberFormatException nex) {
                // oops put back original values
                return value;
            }
        }
        if (value.matches("-?\\d+(\\.\\d+)?")) { // degrees without degree symbol
            try {
                return floatToString(((Float.valueOf(value) + 180.0f) % 360.0f));
            } catch (NumberFormatException nex) {
                // oops put back original values
                return value;
            }
        }
        // cardinal directions
        try {
            return reverseCardinalDirection(value);
        } catch (IllegalArgumentException fex) {
            return value;
        }
    }

    /**
     * Reverse the value of an incline tag
     * 
     * @param value the value to reverse
     * @return the reversed value
     */
    private static String reverseIncline(@NonNull final String value) {
        String tmpVal;
        if (Tags.VALUE_UP.equals(value)) {
            return Tags.VALUE_DOWN;
        } else if (Tags.VALUE_DOWN.equals(value)) {
            return Tags.VALUE_UP;
        } else {
            try {
                if (value.endsWith(DEGREE)) { // degrees
                    tmpVal = value.substring(0, value.length() - 1);
                    return floatToString((Float.valueOf(tmpVal) * -1)) + DEGREE;
                } else if (value.endsWith(PERCENT)) { // percent
                    tmpVal = value.substring(0, value.length() - 1);
                    return floatToString((Float.valueOf(tmpVal) * -1)) + PERCENT;
                } else {
                    return floatToString((Float.valueOf(value) * -1));
                }
            } catch (NumberFormatException nex) {
                // oops put back original values
                return value;
            }
        }
    }

    /**
     * Reverse the value for a oneway tag
     * 
     * @param value current value of the oneway tag
     * @return reversed value
     */
    private static String reverseOneway(@NonNull final String value) {
        if (Tags.VALUE_YES.equalsIgnoreCase(value) || Tags.VALUE_TRUE.equalsIgnoreCase(value) || Tags.VALUE_ONE.equals(value)) {
            return Tags.VALUE_MINUS_ONE;
        } else if (Tags.VALUE_REVERSE.equalsIgnoreCase(value) || Tags.VALUE_MINUS_ONE.equals(value)) {
            return Tags.VALUE_YES;
        }
        return value;
    }

    /**
     * Reverse the direction dependent tags and save them to tags
     * 
     * Note this code in its original version ran in to complexity limits on Android 2.2 (and probably older).
     * Eliminating if .. else if constructs seems to have resolved this
     * 
     * @param e the OsmElement
     * @param dirTags Map of all direction dependent tags
     * @param reverseOneway if false don't change the value of the oneway tag if present
     */
    public static void reverseDirectionDependentTags(@NonNull OsmElement e, @NonNull Map<String, String> dirTags, boolean reverseOneway) {
        if (e.getTags() == null) {
            return;
        }
        Map<String, String> tags = new TreeMap<>(e.getTags());

        // remove all dir dependent keys first
        for (String key : dirTags.keySet()) {
            tags.remove(key);
        }

        for (Entry<String, String> entry : dirTags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().trim();
            if (!key.equals(Tags.KEY_ONEWAY) || reverseOneway) {
                // special case
                // motorway and motorway_link have implicit oneway properties, if automatically reversing
                // (reverseOneway), add a oneway tag in the opposite direction
                if (reverseOneway && Tags.KEY_HIGHWAY.equals(key) && (Tags.VALUE_MOTORWAY.equals(value) || Tags.VALUE_MOTORWAY_LINK.equals(value))) {
                    tags.put(key, value); // don't have to change anything
                    if (!dirTags.containsKey(Tags.KEY_ONEWAY)) {
                        tags.put(Tags.KEY_ONEWAY, Tags.VALUE_MINUS_ONE);
                    }
                    continue;
                }
                // normal case
                if (Tags.KEY_ONEWAY.equals(key)) {
                    tags.put(key, reverseOneway(value));
                    continue;
                }
                if (Tags.KEY_DIRECTION.equals(key)) {
                    tags.put(key, reverseDirection(value));
                    continue;
                }
                if (Tags.KEY_INCLINE.equals(key)) {
                    tags.put(key, reverseIncline(value));
                    continue;
                }
                if (key.endsWith(LEFT_POSTFIX)) { // this would be more elegant in a loop
                    tags.put(key.substring(0, key.length() - LEFT_POSTFIX.length()) + RIGHT_POSTFIX, value);
                    continue;
                }
                if (key.endsWith(RIGHT_POSTFIX)) {
                    tags.put(key.substring(0, key.length() - RIGHT_POSTFIX.length()) + LEFT_POSTFIX, value);
                    continue;
                }
                if (key.endsWith(BACKWARD_POSTFIX)) {
                    tags.put(key.substring(0, key.length() - BACKWARD_POSTFIX.length()) + FORWARD_POSTFIX, value);
                    continue;
                }
                if (key.endsWith(FORWARD_POSTFIX)) {
                    tags.put(key.substring(0, key.length() - FORWARD_POSTFIX.length()) + BACKWARD_POSTFIX, value);
                    continue;
                }
                if (key.contains(FORWARD_INFIX)) {
                    tags.put(key.replace(FORWARD_INFIX, BACKWARD_INFIX), value);
                    continue;
                }
                if (key.contains(BACKWARD_INFIX)) {
                    tags.put(key.replace(BACKWARD_INFIX, FORWARD_INFIX), value);
                    continue;
                }
                if (key.contains(RIGHT_INFIX)) {
                    tags.put(key.replace(RIGHT_INFIX, LEFT_INFIX), value);
                    continue;
                }
                if (key.contains(LEFT_INFIX)) {
                    tags.put(key.replace(LEFT_INFIX, RIGHT_INFIX), value);
                    continue;
                }
                if (Tags.VALUE_RIGHT.equals(value)) { // doing this for all values is probably dangerous
                    tags.put(key, Tags.VALUE_LEFT);
                    continue;
                }
                if (Tags.VALUE_LEFT.equals(value)) {
                    tags.put(key, Tags.VALUE_RIGHT);
                    continue;
                }
                if (Tags.VALUE_FORWARD.equals(value)) {
                    tags.put(key, Tags.VALUE_BACKWARD);
                    continue;
                }
                if (Tags.VALUE_BACKWARD.equals(value)) {
                    tags.put(key, Tags.VALUE_FORWARD);
                    continue;
                }
                // shouldn't happen
                Log.e(DEBUG_TAG, "unhandled key/value " + key + "/" + value);
                tags.put(key, value);
            } else {
                tags.put(key, value);
            }
        }
        e.setTags(tags);
    }

    /**
     * Format a float in a nice way, dopping the decimal part if possible
     * 
     * @param f the float to format
     * @return a strign representation of the float value
     */
    private static String floatToString(float f) {
        if (f == (int) f) {
            return String.format(Locale.US, "%d", (int) f);
        } else {
            return String.format(Locale.US, "%s", f);
        }
    }

    /**
     * Compile a JosmFilter pattern
     * 
     * @param pattern the pattern to compile
     * @return the compiled pattern in a Condition object
     * @throws JosmFilterParseException if parsing fails
     */
    private static Condition compilePattern(@NonNull String pattern) throws JosmFilterParseException {
        return new JosmFilterParser(new ByteArrayInputStream(pattern.getBytes())).condition(false);
    }
}
