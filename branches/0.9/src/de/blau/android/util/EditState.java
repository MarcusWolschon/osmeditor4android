package de.blau.android.util;

import java.io.Serializable;

import de.blau.android.Logic;
import de.blau.android.Logic.Mode;
import de.blau.android.osb.Bug;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

/**
 * Save the edite state accros pause / resume cycles
 * @author simon
 *
 */
public class EditState implements Serializable {
	private static final long serialVersionUID = 1L;
	Mode savedMode;
	Node savedNode;
	Way	savedWay;
	Relation savedRelation;
	Bug	savedBug;
	
	public EditState(Mode mode, Node selectedNode, Way selectedWay,
			Relation selectedRelation, Bug selectedBug) {
		savedMode = mode;
		savedNode = selectedNode;
		savedWay = selectedWay;
		savedRelation = selectedRelation;
		savedBug = selectedBug;
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
}

