package de.blau.android.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.Logic.Mode;
import de.blau.android.Application;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Task;

/**
 * Save the edit state accros pause / resume cycles
 * @author simon
 *
 */
public class EditState implements Serializable {
	private static final long serialVersionUID = 12L;
	final Mode savedMode;
	final List<Node> savedNodes;
	final List<Way> savedWays;
	final List<Relation> savedRelations;
	final Task	savedBug;
	final String savedTileServerID;
	final Offset[] savedOffsets;
	final int savedMinZoom;
	final boolean savedShowGPS;
	final boolean savedAutoDownload;
	final boolean savedBugAutoDownload;
	final String savedImageFileName;
	final BoundingBox savedBox;
	final ArrayList<String> savedLastComments;
	final ArrayList<String> savedLastSources;

	public EditState(Logic logic,  TileLayerServer osmts, 
			boolean showGPS, boolean autoDownload, boolean bugAutoDownload, String imageFileName, BoundingBox box) {
		savedMode = logic.getMode();
		savedNodes = logic.getSelectedNodes();
		savedWays = logic.getSelectedWays();
		savedRelations = logic.getSelectedRelations();
		savedBug = logic.getSelectedBug();
		savedTileServerID = osmts.getId();
		savedOffsets = osmts.getOffsets();
		savedMinZoom = osmts.getMinZoomLevel();
		savedShowGPS = showGPS;
		savedAutoDownload = autoDownload;
		savedBugAutoDownload = bugAutoDownload;
		savedImageFileName = imageFileName;
		savedBox = box;
		savedLastComments = logic.getLastComments();
		savedLastSources = logic.getLastSources();
	}
	
	public void setSelected(Logic logic) {
		logic.setMode(savedMode == null ? Mode.MODE_MOVE : savedMode);
		Log.d("EditState","savedMode " + savedMode);
		if (savedNodes != null) {
			for (Node n:savedNodes) {
				Node nodeInStorage = (Node) Application.getDelegator().getOsmElement(Node.NAME,n.getOsmId());
				if (nodeInStorage != null) {
					logic.addSelectedNode(nodeInStorage);
				}
			}
		}
		if (savedWays != null) {
			for (Way w:savedWays) {
				Way wayInStorage = (Way) Application.getDelegator().getOsmElement(Way.NAME,w.getOsmId());
				if (wayInStorage != null) {
					logic.addSelectedWay(wayInStorage);
				}
			}
		}
		if (savedRelations != null) {
			for (Relation r:savedRelations) {
				Relation relationInStorage = (Relation) Application.getDelegator().getOsmElement(Relation.NAME,r.getOsmId());
				if (relationInStorage != null) {
					logic.addSelectedRelation(relationInStorage);
				}
			}
		}
		// 
		logic.setSelectedBug(savedBug);
	}
	
	public void setMiscState(Main main, Logic logic) {
		main.setShowGPS(savedShowGPS);
		main.setAutoDownload(savedAutoDownload);
		main.setBugAutoDownload(savedBugAutoDownload);
		main.setImageFileName(savedImageFileName);
		logic.setLastComments(savedLastComments);
		logic.setLastSources(savedLastSources);
	}
	
	public void setViewBox(Logic logic, Map map) {
		logic.getViewBox().setBorders(savedBox);
		try {
			logic.getViewBox().setRatio((float)map.getWidth() / (float)map.getHeight());
		} catch (OsmException e) {
			// shouldn't happen since we would have only stored a legal BB
		}
		map.setViewBox(logic.getViewBox());
		DataStyle.updateStrokes(Logic.STROKE_FACTOR / logic.getViewBox().getWidth());
		map.invalidate();
	}
	
	public void setOffset(TileLayerServer osmts) {
		Log.d("EditState","setOffset saved id " + savedTileServerID + " current id " + osmts.getId());
		if (osmts.getId().equals(savedTileServerID)) {
			Log.d("EditState","restoring offset");
			if (savedOffsets.length == osmts.getOffsets().length && savedMinZoom == osmts.getMinZoomLevel()) { // check for config change 
				osmts.setOffsets(savedOffsets);
			}
		}
	}
}

