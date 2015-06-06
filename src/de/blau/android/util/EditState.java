package de.blau.android.util;

import java.io.Serializable;
import java.util.List;

import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.Logic.Mode;
import de.blau.android.Main;
import de.blau.android.osb.Bug;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * Save the edit state accros pause / resume cycles
 * @author simon
 *
 */
public class EditState implements Serializable {
	private static final long serialVersionUID = 9L;
	final Mode savedMode;
	final List<Node> savedNodes;
	final List<Way> savedWays;
	final List<Relation> savedRelations;
	final Bug	savedBug;
	final String savedTileServerID;
	final Offset[] savedOffsets;
	final int savedMinZoom;
	final boolean savedShowGPS;
	final boolean savedAutoDownload;
	final String savedImageFileName;

	public EditState(Mode mode, List<Node> selectedNodes, List<Way> selectedWays,
			List<Relation> selectedRelations, Bug selectedBug, OpenStreetMapTileServer osmts, 
			boolean showGPS, boolean autoDownload, String imageFileName) {
		savedMode = mode;
		savedNodes = selectedNodes;
		savedWays = selectedWays;
		savedRelations = selectedRelations;
		savedBug = selectedBug;
		savedTileServerID = osmts.getId();
		savedOffsets = osmts.getOffsets();
		savedMinZoom = osmts.getMinZoomLevel();
		savedShowGPS = showGPS;
		savedAutoDownload = autoDownload;
		savedImageFileName = imageFileName;
	}
	
	public void setSelected(Logic logic) {
		logic.setMode(savedMode == null ? Mode.MODE_MOVE : savedMode);
		Log.d("EditState","savedMode " + savedMode);
		if (savedNodes != null) {
			for (Node n:savedNodes) {
				if (logic.exists(n)) {
					logic.addSelectedNode(n);
				}
			}
		}
		if (savedWays != null) {
			for (Way w:savedWays) {
				if (logic.exists(w)) {
					logic.addSelectedWay(w);
				}
			}
		}
		if (savedRelations != null) {
			for (Relation r:savedRelations) {
				if (logic.exists(r)) {
					logic.addSelectedRelation(r);
				}
			}
		}
		// 
		logic.setSelectedBug(savedBug);
	}
	
	public void setMiscState(Main main) {
		main.setShowGPS(savedShowGPS);
		main.setAutoDownload(savedAutoDownload);
		main.setImageFileName(savedImageFileName);
	}
	
	public void setOffset(OpenStreetMapTileServer osmts) {
		Log.d("EditState","setOffset saved id " + savedTileServerID + " current id " + osmts.getId());
		if (osmts.getId().equals(savedTileServerID)) {
			Log.d("EditState","restoring offset");
			if (savedOffsets.length == osmts.getOffsets().length && savedMinZoom == osmts.getMinZoomLevel()) { // check for config change 
				osmts.setOffsets(savedOffsets);
			}
		}
	}
}

