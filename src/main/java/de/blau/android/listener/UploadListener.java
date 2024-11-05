package de.blau.android.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.DialogInterface;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.ReviewAndUpload;
import de.blau.android.osm.Capabilities;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Server;
import de.blau.android.osm.Tags;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.UndoStorage.UndoElement;
import de.blau.android.osm.UndoStorage.UndoNode;
import de.blau.android.osm.UndoStorage.UndoRelation;
import de.blau.android.osm.UndoStorage.UndoWay;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ScreenMessage;
import de.blau.android.validation.FormValidation;
import de.blau.android.validation.NotEmptyValidator;

/**
 * @author mb
 * @author Simon Poole
 */
public class UploadListener implements DialogInterface.OnShowListener, View.OnClickListener {

    // auto summary tags
    public static final String V_DELETED           = "v:deleted";
    public static final String V_MODIFIED_MEMBERS  = "v:modified_members";
    public static final String V_MODIFIED_GEOMETRY = "v:modified_geometry";
    public static final String V_MODIFIED          = "v:modified";
    public static final String V_CREATED           = "v:created";

    private static final long DEBOUNCE_TIME = 1000;

    private final FragmentActivity     caller;
    private final EditText             commentField;
    private final EditText             sourceField;
    private final CheckBox             closeOpenChangeset;
    private final CheckBox             closeChangeset;
    private final CheckBox             requestReview;
    private final List<FormValidation> validations;
    private final List<OsmElement>     elements;
    private Long                       lastClickTime = null;

    private boolean tagsShown = false;

    /**
     * Upload the current changes
     * 
     * @param caller the instance of Main calling this
     * @param commentField an EditText containing the comment tag value
     * @param sourceField an EditText containing the source tag value
     * @param closeOpenChangeset close any open changeset first if true
     * @param closeChangeset close the changeset after upload if true
     * @param requestReview CheckBox for the review_requested tag
     * @param elements List of OsmELement to upload if null all changed elements will be uploaded
     */
    public UploadListener(@NonNull final FragmentActivity caller, @NonNull final EditText commentField, @NonNull final EditText sourceField,
            @Nullable CheckBox closeOpenChangeset, @NonNull final CheckBox closeChangeset, @NonNull CheckBox requestReview,
            @Nullable List<OsmElement> elements) {

        this.caller = caller;
        this.commentField = commentField;
        this.sourceField = sourceField;
        this.closeOpenChangeset = closeOpenChangeset;
        this.closeChangeset = closeChangeset;
        this.requestReview = requestReview;
        this.elements = elements;
        FormValidation commentValidator = new NotEmptyValidator(commentField, caller.getString(R.string.upload_validation_error_empty_comment));
        FormValidation sourceValidator = new NotEmptyValidator(sourceField, caller.getString(R.string.upload_validation_error_empty_source));
        this.validations = Arrays.asList(commentValidator, sourceValidator);
    }

    @Override
    public void onShow(final DialogInterface dialog) {
        Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (lastClickTime != null && (SystemClock.elapsedRealtime() - lastClickTime < DEBOUNCE_TIME)) {
            return;
        }
        final boolean emptyCommentWarning = App.getPreferences(caller).emptyCommentWarning();
        if (emptyCommentWarning) {
            validateFields();
        }
        if (!emptyCommentWarning || tagsShown || ReviewAndUpload.getPage(caller) == ReviewAndUpload.TAGS_PAGE) {
            ReviewAndUpload.dismissDialog(caller);
            upload();
        } else {
            ReviewAndUpload.showPage(caller, ReviewAndUpload.TAGS_PAGE);
            tagsShown = true;
        }
        lastClickTime = SystemClock.elapsedRealtime();
    }

    /**
     * Actually upload
     */
    private void upload() {
        Map<String, String> extraTags = new LinkedHashMap<>();
        if (requestReview.isChecked()) {
            extraTags.put(Tags.KEY_REVIEW_REQUESTED, Tags.VALUE_YES);
        }
        extraTags.putAll(generateAutoSummary(elements != null ? elements : App.getDelegator().listChangedElements()));
        final Logic logic = App.getLogic();
        final Server server = logic.getPrefs().getServer();
        if (!server.isLoginSet()) {
            ErrorAlert.showDialog(caller, ErrorCodes.NO_LOGIN_DATA);
            return;
        }
        boolean hasDataChanges = logic.hasChanges();
        boolean hasBugChanges = !App.getTaskStorage().isEmpty() && App.getTaskStorage().hasChanges();
        if (hasDataChanges || hasBugChanges) {
            if (hasDataChanges) {
                logic.upload(caller, getTrimmedString(commentField), getTrimmedString(sourceField),
                        closeOpenChangeset != null && closeOpenChangeset.isChecked(), closeChangeset.isChecked(), extraTags, elements,
                        () -> logic.checkForMail(caller, server));
            }
            if (hasBugChanges) {
                TransferTasks.upload(caller, server, null);
            }
        } else {
            ScreenMessage.barInfo(caller, R.string.toast_no_changes);
        }
    }

    /**
     * Container for actions, currently simply strings
     */
    private class Actions {
        List<String> created          = new ArrayList<>();
        List<String> modified         = new ArrayList<>();
        List<String> geometryModified = new ArrayList<>();
        List<String> membersModified  = new ArrayList<>();
        List<String> deleted          = new ArrayList<>();
    }

    /**
     * Generate tags that roughly describe what we've done
     * 
     * @param elements list of OsmElements that will be uploaded
     * @return a Map containing the tags
     */
    private Map<String, String> generateAutoSummary(@NonNull List<OsmElement> elements) {
        Map<String, String> result = new HashMap<>();
        UndoStorage undoStorage = App.getDelegator().getUndo();
        Preset[] presets = App.getCurrentPresets(caller);
        List<Node> movedNodes = new ArrayList<>();
        Actions actions = new Actions();
        for (OsmElement current : elements) {
            addActionsForElement(actions, movedNodes, undoStorage, presets, current);
        }
        if (!movedNodes.isEmpty()) {
            Set<Way> modifiedGeometryWays = new HashSet<>();
            for (Node n : movedNodes) {
                modifiedGeometryWays.addAll(App.getLogic().getWaysForNode(n));
            }
            for (Way w : modifiedGeometryWays) {
                PresetItem match = Preset.findBestMatch(caller, presets, w.getTags(), null, w, true);
                actions.geometryModified.add(getPresetName(match));
            }
        }
        putSummary(V_CREATED, actions.created, result);
        putSummary(V_MODIFIED, actions.modified, result);
        putSummary(V_MODIFIED_GEOMETRY, actions.geometryModified, result);
        putSummary(V_MODIFIED_MEMBERS, actions.membersModified, result);
        putSummary(V_DELETED, actions.deleted, result);
        return result;
    }

    /**
     * Given an OsmELement generate a rough description of what changes were made in this session
     * 
     * Note has the side effect of adding changes for ways if way nodes were changed
     * 
     * @param actions container for the list of actions
     * @param movedNodes list of Nodes that have been moved
     * @param undoStorage our current UndoStorage
     * @param presets current presets
     * @param element the OsmElement
     */
    private void addActionsForElement(@NonNull Actions actions, @NonNull List<Node> movedNodes, @NonNull UndoStorage undoStorage, @NonNull Preset[] presets,
            @NonNull OsmElement element) {

        final boolean hasTags = element.hasTags();
        final Map<String, String> currentTags = element.getTags();
        PresetItem matchCurrent = Preset.findBestMatch(caller, presets, currentTags, null, element, true);
        final String currentPresetName = getPresetName(matchCurrent);
        final boolean currentHasMatch = matchCurrent != null;

        if (OsmElement.STATE_CREATED == element.getState()) {
            // created
            if (hasTags) {
                actions.created.add(currentPresetName);
            } else {
                actions.created.add(getUntaggedString(element));
            }
            return;
        }
        // note original can be null if data was loaded from a JOSM OSM file
        UndoElement original = undoStorage.getOriginal(element);
        final Map<String, String> originalTags = original != null ? original.getTags() : new HashMap<>();
        // element type should be the same as current element here
        PresetItem matchOriginal = Preset.findBestMatch(presets, originalTags, null, element.getType(), true, null);
        if (OsmElement.STATE_DELETED == element.getState()) {
            // deleted
            actions.deleted.add(!originalTags.isEmpty() ? getPresetName(matchOriginal) : getUntaggedString(element));
            return;
        }
        addGeometryActions(actions, presets, element, original, movedNodes, hasTags, currentPresetName);
        if (!hasTags) {
            return;
        }
        // tag changes
        final boolean originalHasMatch = matchOriginal != null;
        if ((!originalHasMatch && currentHasMatch) || (originalHasMatch && !matchOriginal.equals(matchCurrent))) {
            actions.modified.add(caller.getString(R.string.changed_preset, getPresetName(matchOriginal), currentPresetName));
        } else if (!originalTags.equals(currentTags)) {
            actions.modified.add(caller.getString(R.string.changed_tags, currentPresetName));
        }
    }

    /**
     * Add geometry changes for element
     * 
     * @param actions container for the list of actions
     * @param presets current presets
     * @param element the current element
     * @param original the elements original state
     * @param movedNodes list of moved nodes
     * @param hasTags true if element has tags
     * @param currentPresetName the name of the preset for element
     */
    private void addGeometryActions(@NonNull Actions actions, @NonNull Preset[] presets, @NonNull OsmElement element, @Nullable UndoElement original,
            List<Node> movedNodes, final boolean hasTags, @NonNull final String currentPresetName) {
        if (original == null) {
            return;
        }
        // geometry changes
        if (element instanceof Node && moved((Node) element, (UndoNode) original)) {
            if (hasTags) {
                actions.geometryModified.add(caller.getString(R.string.moved, currentPresetName));
            }
            movedNodes.add((Node) element);
        }
        if (element instanceof Way && !((UndoWay) original).getNodes().equals(((Way) element).getNodes())) {
            actions.geometryModified.add(currentPresetName);
        }
        if (element instanceof Relation && !((UndoRelation) original).getMembers().equals(((Relation) element).getMembers())) {
            actions.membersModified.add(currentPresetName);
        }
    }

    /**
     * Get a string for an untagged element
     * 
     * @param e an OsmElement
     * @return a suitable string
     */
    @NonNull
    private String getUntaggedString(@NonNull OsmElement e) {
        switch (e.getName()) {
        case Node.NAME:
            return caller.getString(R.string.untagged_node);
        case Way.NAME:
            return caller.getString(R.string.untagged_way);
        case Relation.NAME:
            return caller.getString(R.string.untagged_relation);
        default: // fall through
        }
        throw new IllegalArgumentException(e.getClass().getCanonicalName() + " is unknown");
    }

    /**
     * Check if a node has moved
     * 
     * @param current current Node
     * @param original original UndoNode
     * @return true if the node moved
     */
    private boolean moved(@NonNull Node current, @NonNull UndoNode original) {
        return current.getLat() != original.getLat() || current.getLon() != original.getLon();
    }

    /**
     * Create tags(s) for the summary taking max length in to account
     * 
     * @param key tag key
     * @param actions list of actions we are summarizing
     * @param result Map holding the tags
     */
    private void putSummary(@NonNull String key, @NonNull Collection<String> actions, @NonNull Map<String, String> result) {
        actions = addCounts(actions);
        int summaryCount = 0;
        StringBuilder summary = new StringBuilder();
        for (String s : actions) {
            if (summary.length() + s.length() + 1 > Capabilities.DEFAULT_MAX_STRING_LENGTH) {
                result.put(key + (summaryCount > 0 ? summaryCount : ""), summary.toString());
                summaryCount++;
                summary.setLength(0);
            }
            if (summary.length() != 0) {
                summary.append("\n");
            }
            summary.append(s);
        }
        if (summary.length() > 0) {
            result.put(key + (summaryCount > 0 ? summaryCount : ""), summary.toString());
        }
    }

    /**
     * Generate a List of actions with counts from the original actions
     * 
     * @param actions the actions
     * @return a List of Strings with counts
     */
    @NonNull
    List<String> addCounts(@NonNull Collection<String> actions) {
        List<String> result = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (String action : actions) {
            Integer count = counts.get(action);
            if (count == null) {
                counts.put(action, 1);
                continue;
            }
            counts.put(action, count + 1);
        }
        for (Entry<String, Integer> entry : counts.entrySet()) {
            result.add(Integer.toString(entry.getValue()) + " " + entry.getKey());
        }
        return result;
    }

    /**
     * Get the preset name from a preset item
     * 
     * @param item the PresetItem
     * @return a name
     */
    @NonNull
    private String getPresetName(@Nullable PresetItem item) {
        if (item != null) {
            return item.getName();
        }
        return caller.getString(R.string.unknown_object);
    }

    /**
     * Get the trimmed contents from an EditText
     * 
     * @param editText the EditText
     * @return a String with the EditText contents
     */
    @NonNull
    private String getTrimmedString(@NonNull EditText editText) {
        return editText.getText().toString().trim();
    }

    /**
     * Run all supplied validators
     */
    private void validateFields() {
        for (FormValidation validation : validations) {
            validation.validate();
        }
    }
}
