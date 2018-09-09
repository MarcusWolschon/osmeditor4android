package de.blau.android.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.exception.OsmException;
import de.blau.android.filter.Filter;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.Task;

/**
 * Save the edit state across pause / resume cycles
 * 
 * @author simon
 *
 */
public class EditState implements Serializable {
    private static final long       serialVersionUID = 20L;
    private final boolean           savedLocked;
    private final Mode              savedMode;
    private final List<Node>        savedNodes;
    private final List<Way>         savedWays;
    private final List<Relation>    savedRelations;
    private final Task              savedBug;
    private final String            savedImageFileName;
    private final BoundingBox       savedBox;
    private final ArrayList<String> savedLastComments;
    private final ArrayList<String> savedLastSources;
    private final NotificationCache savedTaskNotifications;
    private final NotificationCache savedOsmDataNotifications;
    private final boolean           savedFollowGPS;
    private final Filter            savedFilter;

    /**
     * Construct a new EditState instance
     * 
     * @param context Android Context
     * @param logic the current Logic instance
     * @param imageFileName a image filename is any
     * @param box the current BoundingBox
     * @param followGPS true if the map is following the current location
     */
    public EditState(@NonNull Context context, @NonNull Logic logic, @Nullable String imageFileName, @NonNull BoundingBox box, boolean followGPS) {
        savedLocked = logic.isLocked();
        savedMode = logic.getMode();
        savedNodes = logic.getSelectedNodes();
        savedWays = logic.getSelectedWays();
        savedRelations = logic.getSelectedRelations();
        savedBug = logic.getSelectedBug();
        savedImageFileName = imageFileName;
        savedBox = box;
        savedLastComments = logic.getLastComments();
        savedLastSources = logic.getLastSources();
        savedTaskNotifications = App.getTaskNotifications(context);
        savedOsmDataNotifications = App.getOsmDataNotifications(context);
        savedFollowGPS = followGPS;
        savedFilter = logic.getFilter();
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
        if (savedNodes != null) {
            for (Node n : savedNodes) {
                Node nodeInStorage = (Node) App.getDelegator().getOsmElement(Node.NAME, n.getOsmId());
                if (nodeInStorage != null) {
                    logic.addSelectedNode(nodeInStorage);
                }
            }
        }
        if (savedWays != null) {
            for (Way w : savedWays) {
                Way wayInStorage = (Way) App.getDelegator().getOsmElement(Way.NAME, w.getOsmId());
                if (wayInStorage != null) {
                    logic.addSelectedWay(wayInStorage);
                }
            }
        }
        if (savedRelations != null) {
            for (Relation r : savedRelations) {
                Relation relationInStorage = (Relation) App.getDelegator().getOsmElement(Relation.NAME, r.getOsmId());
                if (relationInStorage != null) {
                    logic.addSelectedRelation(relationInStorage);
                }
            }
        }
        //
        logic.setSelectedBug(savedBug);
    }

    /**
     * Set misc. state from this EditState instance
     * 
     * @param main the current Main instance
     * @param logic the current Logic instance
     */
    public void setMiscState(@NonNull Main main, @NonNull  Logic logic) {
        main.setImageFileName(savedImageFileName);
        logic.setLastComments(savedLastComments);
        logic.setLastSources(savedLastSources);
        if (logic.getFilter() == null) { // only restore if we have to
            if (savedFilter != null) { // or else we might overwrite state
                savedFilter.init(main); // in the filter
            }
            logic.setFilter(savedFilter);
        }
        App.setTaskNotifications(main, savedTaskNotifications);
        App.setOsmDataNotifications(main, savedOsmDataNotifications);
        main.setFollowGPS(savedFollowGPS);
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
        DataStyle.updateStrokes(logic.strokeWidth(logic.getViewBox().getWidth()));
        map.invalidate();
    }
}
