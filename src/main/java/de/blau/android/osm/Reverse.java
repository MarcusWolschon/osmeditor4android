package de.blau.android.osm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Logic for reversing direction dependent tags, this is one of the more arcane things about the OSM data model
 * 
 * @author simon
 *
 */
class Reverse {
    private static final String DEBUG_TAG = "Reverse";

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

    private Reverse() {
        // don't allow instantiating of this class
    }

    /**
     * Return the direction dependent tags and associated values oneway, *:left, *:right, *:backward, *:forward from the
     * element
     * 
     * Probably we should check for issues with relation membership too
     * 
     * @param e element that we extract the direction dependent tags from
     * @return map containing the tags
     */
    @Nullable
    public static Map<String, String> getDirectionDependentTags(@NonNull OsmElement e) {
        Map<String, String> result = null;
        Map<String, String> tags = e.getTags();
        if (tags != null) {
            for (Entry<String, String> entry : tags.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if ((Tags.KEY_HIGHWAY.equals(key) && (Tags.VALUE_MOTORWAY.equals(value) || Tags.VALUE_MOTORWAY_LINK.equals(value)))
                        || Tags.KEY_ONEWAY.equals(key) || Tags.KEY_INCLINE.equals(key) || Tags.KEY_DIRECTION.equals(key) || key.endsWith(LEFT_POSTFIX)
                        || key.endsWith(RIGHT_POSTFIX) || key.endsWith(BACKWARD_POSTFIX) || key.endsWith(FORWARD_POSTFIX) || key.contains(FORWARD_INFIX)
                        || key.contains(BACKWARD_INFIX) || key.contains(RIGHT_INFIX) || key.contains(LEFT_INFIX) || Tags.VALUE_RIGHT.equals(value)
                        || Tags.VALUE_LEFT.equals(value) || Tags.VALUE_FORWARD.equals(value) || Tags.VALUE_BACKWARD.equals(value)) {
                    if (result == null) {
                        result = new TreeMap<>();
                    }
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Return a list of (route) relations that the element is a member of with a direction dependent role
     * 
     * @param e element for which we need to inspect the parent relations
     * @return List of relations or null if none found
     */
    @Nullable
    public static List<Relation> getRelationsWithDirectionDependentRoles(@NonNull OsmElement e) {
        ArrayList<Relation> result = null;
        List<Relation> parents = e.getParentRelations();
        if (parents != null) {
            for (Relation r : parents) {
                String t = r.getTagWithKey(Tags.KEY_TYPE);
                if (t != null && Tags.VALUE_ROUTE.equals(t)) {
                    RelationMember rm = r.getMember(Way.NAME, e.getOsmId());
                    if (rm != null && (Tags.ROLE_FORWARD.equals(rm.getRole()) || Tags.ROLE_BACKWARD.equals(rm.getRole()))) {
                        if (result == null) {
                            result = new ArrayList<>();
                        }
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
                    if (rm.role != null && Tags.ROLE_FORWARD.equals(rm.role)) {
                        rm.setRole(Tags.ROLE_BACKWARD);
                        continue;
                    }
                    if (rm.role != null && Tags.ROLE_BACKWARD.equals(rm.role)) {
                        rm.setRole(Tags.ROLE_FORWARD);
                        continue;
                    }
                }
            }
        }
    }

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

    private static String reverseDirection(final String value) {
        if (Tags.VALUE_UP.equals(value)) {
            return Tags.VALUE_DOWN;
        } else if (Tags.VALUE_DOWN.equals(value)) {
            return Tags.VALUE_UP;
        } else {
            if (value.endsWith(DEGREE)) { // degrees
                try {
                    String tmpVal = value.substring(0, value.length() - 1);
                    return floatToString(((Float.valueOf(tmpVal) + 180.0f) % 360.0f)) + DEGREE;
                } catch (NumberFormatException nex) {
                    // oops put back original values
                    return value;
                }
            } else if (value.matches("-?\\d+(\\.\\d+)?")) { // degrees without degree symbol
                try {
                    return floatToString(((Float.valueOf(value) + 180.0f) % 360.0f));
                } catch (NumberFormatException nex) {
                    // oops put back original values
                    return value;
                }
            } else { // cardinal directions
                try {
                    return reverseCardinalDirection(value);
                } catch (IllegalArgumentException fex) {
                    return value;
                }
            }
        }
    }

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
        if (Tags.VALUE_YES.equalsIgnoreCase(value) || Tags.VALUE_TRUE.equalsIgnoreCase(value) || "1".equals(value)) {
            return "-1";
        } else if (Tags.VALUE_REVERSE.equalsIgnoreCase(value) || "-1".equals(value)) {
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
     * @param tags Map of all direction dependent tags
     * @param reverseOneway if false don't change the value of the oneway tag if present
     */
    public static void reverseDirectionDependentTags(@NonNull OsmElement e, @NonNull Map<String, String> dirTags, boolean reverseOneway) {
        if (e.getTags() == null) {
            return;
        }
        Map<String, String> tags = new TreeMap<>(e.getTags());

        // remove all dir dependent key first
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
                        tags.put(Tags.KEY_ONEWAY, "-1");
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
                    tags.put(key, "backward");
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

    private static String floatToString(float f) {
        if (f == (int) f)
            return String.format(Locale.US, "%d", (int) f);
        else
            return String.format(Locale.US, "%s", f);
    }
}
