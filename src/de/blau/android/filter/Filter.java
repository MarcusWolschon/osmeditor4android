package de.blau.android.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public abstract class Filter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * cache for way nodes
	 */
	transient HashMap<Node,Boolean> cachedNodes = new HashMap<Node,Boolean>(100);
	transient HashMap<Way,Boolean> cachedWays = new HashMap<Way,Boolean>(100);
	transient HashMap<Relation,Boolean> cachedRelations = new HashMap<Relation,Boolean>(100);;
	
	transient Logic logic = Application.getLogic();

	Filter() {
		logic.setAttachedObjectWarning(true); // set this to true when we create a new filter 
	}

	/**
	 * If true include this element
	 * @param node
	 * @param selected
	 * @return
	 */
	public abstract boolean include(Node node, boolean selected);
	
	/**
	 * If true include this element
	 * @param node
	 * @param selected
	 * @return
	 */
	public abstract boolean include(Way way, boolean selected);
	
	/**
	 * If true include this element
	 * @param node
	 * @param selected
	 * @return
	 */
	public abstract boolean include(Relation relation, boolean selected);
	
	/**
	 * Calls the element specific include methods
	 * @param e
	 * @param selected
	 * @return
	 */
	public boolean include(OsmElement e, boolean selected) {
		if (e instanceof Node) {
			return include((Node)e, selected);
		} else if (e instanceof Way) {
			return include((Way)e, selected);
		} else if (e instanceof Relation) {
			return include((Relation)e, selected);
		}
		return false;
	}
	
	public abstract interface Update {
		abstract void execute();
	}
	
	/**
	 * Add the controls if any to layout
	 * @param layout
	 */
	public void addControls(ViewGroup layout, final Update update) {		
	}
	
	/**
	 * Remove the controls from layout
	 * @param layout
	 */
	public void removeControls() {		
	}
	
	/**
	 * Show the controls if any 
	 */
	public void showControls() {		
	}
	
	/**
	 * Show the controls if any 
	 */
	public void hideControls() {		
	}
	
	/**
	 * Empty the node, way and relation cache
	 */
	public void clear() {
		cachedNodes.clear();
		cachedWays.clear();
		cachedRelations.clear();
	}
	
	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		// Normal deserialization will not initialize transient objects, need to do it here
		cachedNodes = new HashMap<Node,Boolean>(100);
		cachedWays = new HashMap<Way,Boolean>(100);
		cachedRelations = new HashMap<Relation,Boolean>(100);
	}
}
