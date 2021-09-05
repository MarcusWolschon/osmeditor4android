package de.blau.android.voice;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.address.Address;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.tasks.Note;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoMath;
import de.blau.android.util.IntCoordinates;
import de.blau.android.util.OptimalStringAlignment;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

/**
 * Support for simple voice commands, format &lt;location&gt; &lt;number&gt; for an address &lt;location&gt;
 * &lt;object&gt; [&lt;name&gt;] for a POI of some kind- &lt;location&gt; can be one of left, here and right
 * 
 * @author Simon Poole
 *
 */
public class Commands {
    private static final String DEBUG_TAG = Commands.class.getSimpleName();

    public static final String SOURCE_ORIGINAL_TEXT = "source:original_text";

    private Main main;

    /**
     * Construct a new instance
     * 
     * @param main the current Main instance
     */
    public Commands(@NonNull Main main) {
        this.main = main;
    }

    /**
     * Process the result of what the intent returned
     * 
     * @param data the Intent data
     * @param location the current Location
     */
    public void processIntentResult(@NonNull Intent data, @NonNull Location location) {
        // Fill the list view with the strings the recognizer thought it
        // could have heard
        List<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        final Logic logic = App.getLogic();
        // try to find a command it simply stops at the first string that is valid
        for (String text : matches) {
            String[] words = text.split("\\s+", 3);
            if (words.length > 1) {
                String loc = words[0].toLowerCase(Locale.getDefault());
                if (match(R.string.voice_left, loc) || match(R.string.voice_here, loc) || match(R.string.voice_right, loc) || match(R.string.voice_note, loc)) {
                    if (match(R.string.voice_note, loc)) {
                        Note n = createNote(words, location);
                        if (n != null) {
                            Snack.toastTopInfo(main, "Note: " + n.getDescription());
                        }
                        return;
                    }
                    if (!match(R.string.voice_here, loc)) {
                        Snack.toastTopWarning(main, "Sorry currently only the command \"" + main.getString(R.string.voice_here) + "\" is supported");
                    }
                    //
                    String first = words[1].toLowerCase(Locale.getDefault());
                    try {
                        int number = Integer.parseInt(first);
                        // worked if there is a further word(s) simply add it/them
                        String additionalText = words.length == 3 ? words[2] : "";
                        Snack.toastTopInfo(main, loc + " " + number + additionalText);
                        Node node = createNode(loc, location);
                        if (node != null) {
                            setAddressTags(main, logic, number, additionalText, node, text);
                        }
                        return;
                    } catch (NumberFormatException ex) {
                        // ok wasn't a number
                    } catch (OsmIllegalOperationException e) {
                        Log.e(DEBUG_TAG, "processIntentResult got " + e.getMessage());
                        Snack.toastTopError(main, e.getLocalizedMessage());
                    }

                    List<PresetElement> presetItems = SearchIndexUtils.searchInPresets(main, first, ElementType.NODE, 2, 1, null);
                    if (presetItems != null && presetItems.size() == 1) {
                        addNode(main, createNode(loc, location), words.length == 3 ? words[2] : null, (PresetItem) presetItems.get(0), logic, text);
                        return;
                    }

                    // search in names
                    StringBuilder input = new StringBuilder("");
                    for (int i = 1; i < words.length; i++) {
                        input.append(words[i] + (i < words.length ? " " : ""));
                    }
                    NameAndTags nt = SearchIndexUtils.searchInNames(main, input.toString(), 2);
                    if (nt != null) {
                        Map<String, String> map = new HashMap<>();
                        map.putAll(nt.getTags());
                        PresetItem pi = Preset.findBestMatch(App.getCurrentPresets(main), map, null);
                        if (pi != null) {
                            Node node = addNode(main, createNode(loc, location), nt.getName(), pi, logic, text);
                            if (node != null) {
                                // set tags from name suggestions
                                Map<String, String> tags = new TreeMap<>(node.getTags());
                                tags.putAll(map);
                                App.getDelegator().setTags(node, tags); // note doesn't create a new undo checkpoint,
                            }
                            return;
                        }
                    }
                }
            } else if (words.length == 1) {
                if (match(R.string.voice_follow, words[0])) {
                    main.setFollowGPS(true);
                    return;
                } else {
                    Snack.toastTopWarning(main, main.getResources().getString(R.string.toast_unknown_voice_command, words[0]));
                }
            }
        }
    }

    /**
     * Set address tags from a voice command
     * 
     * @param activity calling Activity
     * @param logic current Logic instance
     * @param number parsed number
     * @param additionalText any additional text
     * @param node the Node
     * @param originalText the original text from the voice recording
     */
    public static void setAddressTags(@NonNull Activity activity, @NonNull final Logic logic, int number, @NonNull String additionalText, @NonNull Node node,
            @NonNull String originalText) {
        Map<String, String> tags = new TreeMap<>(node.getTags());
        tags.put(Tags.KEY_ADDR_HOUSENUMBER, Integer.toString(number) + additionalText);
        tags.put(SOURCE_ORIGINAL_TEXT, originalText);
        Map<String, List<String>> map = Address.predictAddressTags(activity, Node.NAME, node.getOsmId(),
                new ElementSearch(new IntCoordinates(node.getLon(), node.getLat()), true), Util.getListMap(tags), Address.NO_HYSTERESIS);
        tags = Address.multiValueToSingle(map);
        logic.setTags(activity, node, tags);
    }

    /**
     * "add" a Node
     * 
     * @param activity the calling Activity
     * @param node the Node
     * @param name the name of the establishment
     * @param pi the PresetItem
     * @param logic the current logic instance
     * @param original the text the voice recognition understood
     * @return the Node or null
     */
    @Nullable
    public static Node addNode(@NonNull Activity activity, @Nullable Node node, @Nullable String name, @NonNull PresetItem pi, @NonNull Logic logic,
            @NonNull String original) {
        if (node != null) {
            Snack.toastTopInfo(activity, pi.getName() + (name != null ? " name: " + name : ""));
            try {
                TreeMap<String, String> tags = new TreeMap<>(node.getTags());
                for (Entry<String, PresetFixedField> tag : pi.getFixedTags().entrySet()) {
                    PresetFixedField field = tag.getValue();
                    tags.put(tag.getKey(), field.getValue().getValue());
                }
                if (name != null) {
                    tags.put(Tags.KEY_NAME, name);
                }
                tags.put(SOURCE_ORIGINAL_TEXT, original);
                logic.setTags(activity, node, tags);
                return node;
            } catch (OsmIllegalOperationException e) {
                Log.e(DEBUG_TAG, "addNode got " + e.getMessage());
                Snack.toastTopError(activity, e.getLocalizedMessage());
            }
        }
        return null;
    }

    /**
     * Create a new node at the current or at a provided GPS pos
     * 
     * @param loc where to put the node, currently only "here"
     * @param location the current location
     * @return the Node or null
     */
    @Nullable
    private Node createNode(String loc, @Nullable Location location) {
        if (location == null) {
            location = getLocation();
        }
        if (location != null) {
            if (main.getString(R.string.voice_here).equals(loc)) {
                double lon = location.getLongitude();
                double lat = location.getLatitude();
                if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_COMPAT_LAT && lat <= GeoMath.MAX_COMPAT_LAT) {
                    final Logic logic = App.getLogic();
                    logic.setSelectedNode(null);
                    Node node = logic.performAddNode(main, lon, lat);
                    logic.setSelectedNode(null);
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Create Note at a Location
     * 
     * @param words the text voice recognition understood
     * @param location the Location the Note should be created at
     * @return the created Note or null
     */
    @Nullable
    private Note createNote(@NonNull String[] words, @Nullable Location location) {
        if (location == null) {
            location = getLocation();
        }
        if (location != null) {
            double lon = location.getLongitude();
            double lat = location.getLatitude();
            if (GeoMath.coordinatesInCompatibleRange(lon, lat)) {
                Note n = new Note((int) (lat * 1E7D), (int) (lon * 1E7D));
                StringBuilder input = new StringBuilder();
                for (int i = 1; i < words.length; i++) {
                    input.append(words[i]);
                    input.append(" ");
                }
                n.addComment(input.toString().trim());
                n.open();
                n.setChanged(true);
                App.getTaskStorage().add(n);
                return n;
            }
        }
        return null;
    }

    /**
     * Get the current Location
     * 
     * @return the Location or null if it could not be determined
     */
    @Nullable
    private Location getLocation() {
        LocationManager locationManager = (LocationManager) main.getSystemService(android.content.Context.LOCATION_SERVICE);
        if (locationManager != null) {
            try {
                return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException sex) {
                // can be safely ignored
                return null;
            }
        }
        return null;
    }

    /**
     * Match a String from voice input with the contents of a resource
     * 
     * @param resId the id of the resource
     * @param input the input String
     * @return true if the input matches
     */
    private boolean match(int resId, @NonNull String input) {
        final int maxDistance = 1;
        int distance = OptimalStringAlignment.editDistance(main.getString(resId), input, maxDistance);
        return distance >= 0 && distance <= maxDistance;
    }
}
