package de.blau.android.util;

import java.io.Serializable;

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
	private static final long serialVersionUID = 2L;
	Mode savedMode;
	Node savedNode;
	Way	savedWay;
	Relation savedRelation;
	Bug	savedBug;
	double savedLonOffset;
	double savedLatOffset;
	
	public EditState(Mode mode, Node selectedNode, Way selectedWay,
			Relation selectedRelation, Bug selectedBug, double lonOffset, double latOffset) {
		savedMode = mode;
		savedNode = selectedNode;
		savedWay = selectedWay;
		savedRelation = selectedRelation;
		savedBug = selectedBug;
		savedLonOffset = lonOffset;
		savedLatOffset = latOffset;
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
		osmts.setLonOffset(savedLonOffset);
		osmts.setLatOffset(savedLatOffset);
	}
}

