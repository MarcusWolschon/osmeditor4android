package de.blau.android.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
	private static final long serialVersionUID = 6L;
	
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
	transient HashMap<Node,Include> cachedNodes = new HashMap<>(100);
	transient HashMap<Way,Include> cachedWays = new HashMap<>(100);
	transient HashMap<Relation,Include> cachedRelations = new HashMap<>(100);

	private transient Logic logic = App.getLogic();

	private Filter savedFilter = null;
	
	Filter() {
		logic.setAttachedObjectWarning(true); // set this to true when we create a new filter 
	}

	/**
	 * This is for any serialisation that might have to happen post deseriaisation
	 * 
	 * @param context	Android Context
	 */
	public void init(Context context) {	
	}
	
	/**
	 * If true include this element
	 * 
	 * @param node		Node element
	 * @param selected	true if element is selected
	 * @return true if the element should be included
	 */
	public abstract boolean include(Node node, boolean selected);
	
	/**
	 * If true include this element
	 * 
	 * @param way		Way element
	 * @param selected	true if element is selected
	 * @return true if the element should be included
	 */
	public abstract boolean include(Way way, boolean selected);
	
	/**
	 * If true include this element
	 * 
	 * @param relation	Relation element
	 * @param selected	true if element is selected
	 * @return true if the element should be included
	 */
	public abstract boolean include(Relation relation, boolean selected);
	
	/**
	 * Calls the element specific include methods
	 * @param e			OsmElement
	 * @param selected	true if element is selected
	 * @return true if the element should be included
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
	 * 
	 * @param layout	the layout the controls should be added to
	 * @param update	call to update anything necessary when controls are used
	 */
	public void addControls(ViewGroup layout, final Update update) {		
	}
	
	/**
	 * Remove the controls from layout
	 */
	public void removeControls() {		
	}
	
	/**
	 * Show the controls if any 
	 */
	public void showControls() {		
	}
	
	/**
	 * Hide the controls if any 
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
	
	/**
	 * Get all nodes that are currently visible from the cache
	 * 
	 * @return List of visible Nodes
	 */
	@NonNull
	public List<Node> getVisibleNodes() {
		List<Node>result = new ArrayList<>();
		for (Entry<Node,Include>e:cachedNodes.entrySet()) {
			if (e.getValue() != Include.DONT) {
				result.add(e.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Get all ways that are currently visible from the cache
	 * 
	 * @return List of visible Ways
	 */
	@NonNull
	public List<Way> getVisibleWays() {
		List<Way>result = new ArrayList<>();
		for (Entry<Way,Include>e:cachedWays.entrySet()) {
			if (e.getValue() != Include.DONT) {
				result.add(e.getKey());
			}
		}
		return result;
	}
	
	public void saveState() {
	}
	
	/**
	 * Call this on element(s) changing to update/invalidate the cache.
	 * 
	 * The default implementation simply calls {@link #clear()}
	 * @param pre	the element(s) before the change or null
	 * @param post	the element(s) after the change or null
	 */
	public void onElementChanged(@Nullable List<OsmElement> pre, @Nullable List<OsmElement> post) {
		clear();
	}
	
	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		// Normal deserialization will not initialize transient objects, need to do it here
		cachedNodes = new HashMap<>(100);
		cachedWays = new HashMap<>(100);
		cachedRelations = new HashMap<>(100);
	}
}
