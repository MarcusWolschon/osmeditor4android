package de.blau.android.util;

import java.io.Serializable;

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
	private static final long serialVersionUID = 7L;
	final Mode savedMode;
	final Node savedNode;
	final Way	savedWay;
	final Relation savedRelation;
	final Bug	savedBug;
	final String savedTileServerID;
	final Offset[] savedOffsets;
	final int savedMinZoom;
	final boolean savedShowGPS;
	final boolean savedAutoDownload;

	
	public EditState(Mode mode, Node selectedNode, Way selectedWay,
			Relation selectedRelation, Bug selectedBug, OpenStreetMapTileServer osmts, 
			boolean showGPS, boolean autoDownload) {
		savedMode = mode;
		savedNode = selectedNode;
		savedWay = selectedWay;
		savedRelation = selectedRelation;
		savedBug = selectedBug;
		savedTileServerID = osmts.getId();
		savedOffsets = osmts.getOffsets();
		savedMinZoom = osmts.getMinZoomLevel();
		savedShowGPS = showGPS;
		savedAutoDownload = autoDownload;
	}
	
	public void setSelected(Logic logic) {
		logic.setMode(savedMode == null ? Mode.MODE_MOVE : savedMode);
		Log.d("EditState","savedMode " + savedMode);
		if (logic.exists(savedNode))
			logic.setSelectedNode(savedNode);
		if (logic.exists(savedWay))
			logic.setSelectedWay(savedWay);
		if (logic.exists(savedRelation))
			logic.setSelectedRelation(savedRelation);
		// 
		logic.setSelectedBug(savedBug);
	}
	
	public void setMiscState(Main main) {
		main.setShowGPS(savedShowGPS);
		main.setAutoDownload(savedAutoDownload);
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

