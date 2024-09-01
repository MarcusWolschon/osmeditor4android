package de.blau.android.util;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.Selection;
import de.blau.android.exception.OsmException;
import de.blau.android.filter.Filter;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;

/**
 * Save the edit state across pause / resume cycles
 * 
 * @author simon
 *
 */
public class EditState implements Serializable {
    private static final long         serialVersionUID = 29L;
    private final boolean             savedLocked;
    private final Mode                savedMode;
    private final List<Selection.Ids> savedSelection;
    private final String              savedImageFileName;
    private final BoundingBox         savedBox;
    private final List<String>        savedLastComments;
    private final List<String>        savedLastSources;
    private final String              savedCommentDraft;
    private final String              savedSourceCommentDraft;
    private final NotificationCache   savedTaskNotifications;
    private final NotificationCache   savedOsmDataNotifications;
    private final boolean             savedFollowGPS;
    private final Filter              savedFilter;
    private final long                savedChangesetId;
    private final List<String>        savedLastObjectSearches;
    private final String              savedEasyEditActionModeCallback;
    private final float               savedNearbyPoiSplitterPos;

    /**
     * Construct a new EditState instance
     * 
     * @param main Android Context
     * @param logic the current Logic instance
     * @param imageFileName a image filename is any
     * @param box the current BoundingBox
     * @param followGPS true if the map is following the current location
     * @param changesetId the current changeset id (or -1)
     */
    public EditState(@NonNull Main main, @NonNull Logic logic, @Nullable String imageFileName, @NonNull BoundingBox box, boolean followGPS, long changesetId) {
        savedLocked = logic.isLocked();
        savedMode = logic.getMode();
        savedSelection = new ArrayList<>();
        for (Selection selection : logic.getSelectionStack()) {
            savedSelection.add(selection.getIds());
        }
        savedImageFileName = imageFileName;
        savedBox = box;
        savedLastComments = logic.getLastComments();
        savedCommentDraft = logic.getDraftComment();
        savedLastSources = logic.getLastSources();
        savedSourceCommentDraft = logic.getDraftSourceComment();
        savedTaskNotifications = App.getTaskNotifications(main);
        savedOsmDataNotifications = App.getOsmDataNotifications(main);
        savedFollowGPS = followGPS;
        savedFilter = logic.getFilter();
        savedChangesetId = changesetId;
        savedLastObjectSearches = logic.getLastObjectSearches();
        savedEasyEditActionModeCallback = main.getEasyEditManager().getActionModeCallbackName();
        savedNearbyPoiSplitterPos = main.getSplitterPosition();
    }

    /**
     * Create a list of OSM ids from a list of OsmElements
     * 
     * @param <T> OsmElement type
     * @param elements List of OsmElements
     * @return a List of Longs or null
     */
    @Nullable
    private <T extends OsmElement> List<Long> getIdList(@Nullable List<T> elements) {
        if (elements != null) {
            List<Long> result = new ArrayList<>();
            for (OsmElement e : elements) {
                result.add(e.getOsmId());
            }
            return result;
        }
        return null;
    }

    /**
     * Set selected elements and tasks from this EditState instance
     * 
     * @param main the current Main instance
     * @param logic the current Logic instance
     */
    public void setSelected(@NonNull Main main, @NonNull Logic logic) {
        logic.setLocked(savedLocked);
        logic.setMode(main, savedMode);
        Log.d("EditState", "savedMode " + savedMode);
        Deque<Selection> selectionStack = new ArrayDeque<>();
        if (savedSelection != null) {
            final StorageDelegator delegator = App.getDelegator();
            for (Selection.Ids ids : savedSelection) {
                Selection s = new Selection();
                s.fromIds(main, delegator, ids);
                selectionStack.add(s);
            }
            logic.setSelectionStack(selectionStack);
        }
        main.getEasyEditManager().setRestartActionModeCallbackName(savedEasyEditActionModeCallback);
    }

    /**
     * Set misc. state from this EditState instance
     * 
     * @param main the current Main instance
     * @param logic the current Logic instance
     */
    public void setMiscState(@NonNull Main main, @NonNull Logic logic) {
        main.setImageFileName(savedImageFileName);
        main.setSplitterPosition(savedNearbyPoiSplitterPos);
        logic.setLastComments(savedLastComments);
        logic.setDraftComment(savedCommentDraft);
        logic.setLastSources(savedLastSources);
        logic.setDraftSourceComment(savedSourceCommentDraft);
        if (logic.getFilter() == null) { // only restore if we have to
            if (savedFilter != null) { // or else we might overwrite state
                savedFilter.init(main); // in the filter
            }
            logic.setFilter(savedFilter);
        }
        App.setTaskNotifications(main, savedTaskNotifications);
        App.setOsmDataNotifications(main, savedOsmDataNotifications);
        main.setFollowGPS(savedFollowGPS);
        logic.getPrefs().getServer().setOpenChangeset(savedChangesetId);
        logic.setLastObjectSearches(savedLastObjectSearches);
    }

    /**
     * Set the ViewBox from this EditState instance
     * 
     * @param logic the current Logic instance
     * @param map the current Map instance
     */
    public void setViewBox(@NonNull Logic logic, @NonNull Map map) {
        logic.getViewBox().setBorders(map, savedBox);
        try {
            logic.getViewBox().setRatio(map, (float) map.getWidth() / (float) map.getHeight());
        } catch (OsmException e) {
            // shouldn't happen since we would have only stored a legal BB
        }
        map.setViewBox(logic.getViewBox());
        map.getDataStyle().updateStrokes(logic.strokeWidth(logic.getViewBox().getWidth()));
        map.invalidate();
    }
}
