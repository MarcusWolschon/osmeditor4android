package de.blau.android.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import android.content.Context;
import android.view.ViewGroup;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public abstract class Filter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5L;
	
	/**
	 * Filter action for elements
	 */
	enum Include {
		DONT,
		INCLUDE,
		INCLUDE_WITH_WAYNODES
	}
		
	/**
	 * cache for element filter actions
	 */
	transient HashMap<Node,Include> cachedNodes = new HashMap<Node,Include>(100);
	transient HashMap<Way,Include> cachedWays = new HashMap<Way,Include>(100);
	transient HashMap<Relation,Include> cachedRelations = new HashMap<Relation,Include>(100);

	private transient Logic logic = App.getLogic();

	private Filter savedFilter = null;
	
	Filter() {
		logic.setAttachedObjectWarning(true); // set this to true when we create a new filter 
	}

	/**
	 * This is for any serialisation that might have to happen post deseriaisation
	 * @param context
	 */
	public void init(Context context) {	
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
	 * @param way
	 * @param selected
	 * @return
	 */
	public abstract boolean include(Way way, boolean selected);
	
	/**
	 * If true include this element
	 * @param relation
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
	
	public void saveFilter(Filter filter) {
		savedFilter = filter;
	}
	
	public Filter getSavedFilter() {
		return savedFilter;
	}
	
	
	public interface Update {
		void execute();
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
	
	public void saveState() {
	}
	
	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		// Normal deserialization will not initialize transient objects, need to do it here
		cachedNodes = new HashMap<Node,Include>(100);
		cachedWays = new HashMap<Way,Include>(100);
		cachedRelations = new HashMap<Relation,Include>(100);
	}
}
