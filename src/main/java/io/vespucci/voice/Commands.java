package io.vespucci.voice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.address.Address;
import io.vespucci.exception.OsmIllegalOperationException;
import io.vespucci.nsi.Names.NameAndTags;
import io.vespucci.osm.Node;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.osm.Tags;
import io.vespucci.osm.OsmElement.ElementType;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetElement;
import io.vespucci.presets.PresetFixedField;
import io.vespucci.presets.PresetItem;
import io.vespucci.tasks.Note;
import io.vespucci.util.ActivityResultHandler;
import io.vespucci.util.ElementSearch;
import io.vespucci.util.IntCoordinates;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.SearchIndexUtils;
import io.vespucci.util.Util;

/**
 * Support for simple voice commands
 * 
 * @author Simon Poole
 *
 */
public final class Commands {
    private static final String DEBUG_TAG = Commands.class.getSimpleName().substring(0, Math.min(23, Commands.class.getSimpleName().length()));

    public static final String SOURCE_ORIGINAL_TEXT = "source:original_text";

    /**
     * Private constructor
     */
    private Commands() {
        // empty
    }

    /**
     * Process the result of the voice recognition
     *
     * @param activity the calling Activity
     * @param requestCode the Intent request code
     * @param resultCode the Intent result code
     * @param data any Intent data
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     */
    public static void processIntentResult(Activity activity, final int requestCode, final int resultCode, final Intent data, int lonE7, int latE7) {
        Logic logic = App.getLogic();
        if (requestCode == Main.VOICE_RECOGNITION_REQUEST_CODE) {
            List<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //
            StorageDelegator storageDelegator = App.getDelegator();
            for (String text : matches) {
                String[] words = text.split("\\s+", 2);
                if (words.length > 0) {
                    //
                    String first = words[0];
                    try {
                        int number = Integer.parseInt(first);
                        // worked if there is a further word(s) simply add it/them
                        String additionalText = words.length == 2 ? words[1] : "";
                        ScreenMessage.barInfoShort(activity, +number + additionalText);
                        Node node = logic.performAddNode(activity, lonE7, latE7);
                        if (node != null) {
                            Commands.setAddressTags(activity, logic, number, additionalText, node, text);
                            edit(activity, node);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        // ok wasn't a number, just ignore
                    } catch (OsmIllegalOperationException e) {
                        Log.e(DEBUG_TAG, "handleActivityResult got exception " + e.getMessage());
                    }

                    List<PresetElement> presetItems = SearchIndexUtils.searchInPresets(activity, first, ElementType.NODE, 2, 1, null);
                    if (presetItems != null && presetItems.size() == 1) {
                        Node node = Commands.addNode(activity, logic.performAddNode(activity, lonE7, latE7), words.length == 2 ? words[1] : null,
                                (PresetItem) presetItems.get(0), logic, text);
                        if (node != null) {
                            edit(activity, node);
                            return;
                        }
                    }

                    // search in names
                    NameAndTags nt = SearchIndexUtils.searchInNames(activity, text, 2);
                    if (nt != null) {
                        Map<String, String> map = new HashMap<>();
                        map.putAll(nt.getTags());
                        PresetItem pi = Preset.findBestMatch(App.getCurrentPresets(activity), map, null, null);
                        if (pi != null) {
                            Node node = Commands.addNode(activity, logic.performAddNode(activity, lonE7, latE7), nt.getName(), pi, logic, text);
                            if (node != null) {
                                // set tags from name suggestions
                                Map<String, String> tags = new TreeMap<>(node.getTags());
                                tags.putAll(map);
                                storageDelegator.setTags(node, tags); // note doesn't create a new undo checkpoint,
                                edit(activity, node);
                                return;
                            }
                        }
                    }
                }
            }
        } else if (requestCode == Main.VOICE_RECOGNITION_NOTE_REQUEST_CODE) {
            List<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Note n = createNote(matches, lonE7, latE7);
            if (n != null) {
                ScreenMessage.toastTopInfo(activity, n.getDescription());
            }
            if (activity instanceof Main) {
                ((Main) activity).getEasyEditManager().finish();
            }
        }
    }

    /**
     * Start editing a node
     * 
     * @param activity an instance of Main, if something else nothing will happen
     * @param node the Node
     */
    private static void edit(@Nullable Activity activity, @NonNull Node node) {
        if (activity instanceof Main) {
            ((Main) activity).edit(node);
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
            ScreenMessage.toastTopInfo(activity, pi.getName() + (name != null ? " name: " + name : ""));
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
                ScreenMessage.toastTopError(activity, e.getLocalizedMessage());
            }
        }
        return null;
    }

    /**
     * Create Note at a Location
     * 
     * @param words the text voice recognition understood
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @return the created Note or null
     */
    @NonNull
    private static Note createNote(@NonNull List<String> words, final int lonE7, final int latE7) {

        Note n = new Note(latE7, lonE7);
        StringBuilder input = new StringBuilder();
        for (String word : words) {
            input.append(word);
            input.append(" ");
        }
        n.addComment(input.toString().trim());
        n.open();
        n.setChanged(true);
        App.getTaskStorage().add(n);
        return n;
    }

    /**
     * Start voice recognition
     * 
     * @param activity current Activity
     * @param requestCode the code to identify this request
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @return true if started successfully
     */
    public static boolean startVoiceRecognition(@NonNull Activity activity, final int requestCode, final int lonE7, final int latE7) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        if (activity instanceof ActivityResultHandler) {
            ((ActivityResultHandler) activity).setResultListener(requestCode,
                    (int resultCode, Intent result) -> processIntentResult(activity, requestCode, resultCode, result, lonE7, latE7));
        }
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception ex) {
            ScreenMessage.barError(activity, R.string.toast_no_voice);
            return false;
        }
        return true;
    }
}
