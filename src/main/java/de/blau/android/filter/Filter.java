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
	
	/**
	 * Get all nodes that are currently visible from the cache
	 * @return
	 */
	@NonNull
	public List<Node> getVisibleNodes() {
		List<Node>result = new ArrayList<Node>();
		for (Entry<Node,Include>e:cachedNodes.entrySet()) {
			if (e.getValue() != Include.DONT) {
				result.add(e.getKey());
			}
		}
		return result;
	}
	
	/**
	 * Get all wayss that are currently visible from the cache
	 * @return
	 */
	@NonNull
	public List<Way> getVisibleWays() {
		List<Way>result = new ArrayList<Way>();
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
	 * Call this on element(s) change to update/invalidate the cache.
	 * 
	 * The default implementation simply calls {@link #clear()}
	 * @param pre the element(s) before the change or null
	 * @param post the element(s) after the change or null
	 */
	public void onElementChanged(@Nullable List<OsmElement> pre, @Nullable List<OsmElement> post) {
		clear();
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
