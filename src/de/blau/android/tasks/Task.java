package de.blau.android.tasks;

import java.io.Serializable;
import java.util.Date;

import de.blau.android.osm.BoundingBox;
import de.blau.android.util.rtree.BoundedObject;

/**
 * Base class for OSM Notes, Osmose Errors and in the future perhaps more
 *
 */
public abstract class Task implements Serializable, BoundedObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6L;

	/** OSB Bug ID. */
	long id;
	/** Latitude *1E7. */
	int lat;
	/** Longitude *1E7. */
	int lon;
	/** Bug state. */
	/**
	 * Enums for modes.
	 */
	public static enum State {
		OPEN,
		CLOSED,
		FALSE_POSITIVE
	}
	State state;
	
	public static int POS_OPEN = 0;
	public static int POS_CLOSED = 1;
	public static int POS_FALSE_POSITIVE = 2;

	/** Has been edited */
	boolean changed = false;

	/**
	 * Get the bug ID.
	 * @return The bug ID.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Get the latitude of the bug.
	 * @return The latitude *1E7.
	 */
	public int getLat() {
		return lat;
	}

	/**
	 * Get the longitude of the bug.
	 * @return The longitude *1E7.
	 */
	public int getLon() {
		return lon;
	}

	/**
	 * Get the bug open/closed state.
	 * @return true if the bug is closed, false if it's still open.
	 */
	public boolean isClosed() {
		return state == State.CLOSED || state == State.FALSE_POSITIVE;
	}
	
	/**
	 * Get the bug open/closed state.
	 * @return true if the bug is closed, false if it's still open.
	 */
	public boolean hasBeenChanged() {
		return changed;
	}

	/**
	 * Close the bug
	 */
	public void close() {
		state = State.CLOSED;
	}
	
	/**
	 * Close the bug
	 */
	public void setFalse() {
		state = State.FALSE_POSITIVE;
	}

	/**
	 * Open the bug
	 */
	public void open() {
		state = State.OPEN;
	}
	
	public BoundingBox getBounds() {
		BoundingBox r = new BoundingBox(lon,lat);
		return r;
	}

	/**
	 * Return true if a newly created bug, only makes sense for Notes
	 * @return
	 */
	public boolean isNew() {
		return id <= 0;
	}
	
	/**
	 * Inidicate that the bug has been modified
	 */
	public void setChanged() {
		changed = true;
	}
	
	abstract public String getDescription();

	abstract public Date getLastUpdate();

	abstract public String bugFilterKey();
}
