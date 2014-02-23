package de.blau.android.util;

import java.io.Serializable;

import android.util.Log;

import de.blau.android.Logic;
import de.blau.android.Logic.Mode;
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
	private static final long serialVersionUID = 4L;
	Mode savedMode;
	Node savedNode;
	Way	savedWay;
	Relation savedRelation;
	Bug	savedBug;
	String savedTileServerID;
	double savedLonOffset;
	double savedLatOffset;
	
	public EditState(Mode mode, Node selectedNode, Way selectedWay,
			Relation selectedRelation, Bug selectedBug, OpenStreetMapTileServer osmts) {
		savedMode = mode;
		savedNode = selectedNode;
		savedWay = selectedWay;
		savedRelation = selectedRelation;
		savedBug = selectedBug;
		savedTileServerID = osmts.getId();
		savedLonOffset = osmts.getLonOffset();
		savedLatOffset = osmts.getLatOffset();
	}
	
	public void setSelected(Logic logic) {
		logic.setMode(savedMode == null ? Mode.MODE_MOVE : savedMode);
		if (logic.exists(savedNode))
			logic.setSelectedNode(savedNode);
		if (logic.exists(savedWay))
			logic.setSelectedWay(savedWay);
		if (logic.exists(savedRelation))
			logic.setSelectedRelation(savedRelation);
		// ths currently edited bug is not stored anywhere
		logic.setSelectedBug(savedBug);
	}
	
	public void setOffset(OpenStreetMapTileServer osmts) {
		Log.d("EditState","setOffset saved id " + savedTileServerID + " current id " + osmts.getId());
		if (osmts.getId().equals(savedTileServerID)) {
			Log.d("EditState","restoring offset");
			osmts.setLonOffset(savedLonOffset);
			osmts.setLatOffset(savedLatOffset);
		}
	}
}

