package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import android.content.res.Resources;

import de.blau.android.R;

public abstract class OsmElement implements Serializable, XmlSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7711945069147743666L;
	
	/**
	 * An array of tags considered 'important' and distinctive enough to be shown as part of
	 * the elements description.
	 */
	private static final String[] importantTags;

	public static final long NEW_OSM_ID = -1;

	public static final byte STATE_UNCHANGED = 0;

	public static final byte STATE_CREATED = 1;

	public static final byte STATE_MODIFIED = 2;

	public static final byte STATE_DELETED = 3;

	protected long osmId;

	protected long osmVersion;

	protected SortedMap<String, String> tags;

	protected byte state;
	
	/**
	 * hasProblem() is an expensive test, so the results are cached.
	 */
	private boolean cachedHasProblem;
	
	/**
	 * flag to determine if the cached result for hasProblem() is valid and can be used.
	 */
	protected boolean cachedHasProblemValid;
	
	static {
		// Create the array of important tags. Tags are listed from most important to least.
		importantTags = "highway,barrier,waterway,railway,aeroway,aerialway,power,man_made,building,leisure,amenity,office,shop,craft,emergency,tourism,historic,landuse,military,natural,boundary".split(",");
	}

	OsmElement(final long osmId, final long osmVersion, final byte state) {
		this.osmId = osmId;
		this.osmVersion = osmVersion;
		this.tags = new TreeMap<String, String>();
		this.state = state;
		cachedHasProblemValid = false;
	}

	public long getOsmId() {
		return osmId;
	}

	void setOsmId(final long osmId) {
		this.osmId = osmId;
	}

	public SortedMap<String,String> getTags() {
		return Collections.unmodifiableSortedMap(tags);
	}

	public byte getState() {
		return state;
	}

	/** gives a string description of the element type (e.g. 'node' or 'way') - see also {@link #getType()} */
	abstract public String getName();

	/**
	 * Does not set the state if it's on CREATED, but if new state is DELETED.
	 * 
	 * @param newState
	 */
	void updateState(final byte newState) {
		if (state != STATE_CREATED || newState == STATE_DELETED) {
			state = newState;
		}
	}

	void setState(final byte newState) {
		state = newState;
	}

	void addOrUpdateTag(final String tag, final String value) {
		tags.put(tag, value);
		cachedHasProblemValid = false;
	}

	/**
	 * Add the tags of the element, replacing any existing tags.
	 * @param tags New tags to add or to replace existing tags.
	 */
	void addTags(final Map<String, String> tags) {
		this.tags.putAll(tags);
		cachedHasProblemValid = false;
	}

	/**
	 * Set the tags of the element, replacing all existing tags.
	 * @param tags New tags to replace existing tags.
	 * @return Flag indicating if the tags have actually changed.
	 */
	boolean setTags(final Map<String, String> tags) {
		if ((tags == null) ? !this.tags.isEmpty() : !this.tags.equals(tags)) {
			this.tags.clear();
			if (tags != null) {
				this.tags.putAll(tags);
			}
			cachedHasProblemValid = false;
			return true;
		}
		return false;
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @param value the value to search for (case sensitive)
	 * @return true if the element has a tag with this key and value.
	 */
	public boolean hasTag(final String key, final String value) {
		String keyValue = tags.get(key);
		return keyValue != null && keyValue.equals(value);
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @return the value of this key.
	 */
	public String getTagWithKey(final String key) {
		return tags.get(key);
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @return true if the element has a tag with this key.
	 */
	public boolean hasTagKey(final String key) {
		return getTagWithKey(key) != null;
	}

	@Override
	public String toString() {
		return getName() + " " + osmId;
	}

	public void tagsToXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		for (Entry<String, String> tag : tags.entrySet()) {
			s.startTag("", "tag");
			s.attribute("", "k", tag.getKey());
			s.attribute("", "v", tag.getValue());
			s.endTag("", "tag");
		}
	}

	public boolean isUnchanged() {
		return state == STATE_UNCHANGED;
	}

	/**
	 * Generate a human-readable description/summary of the element.
	 * @return A description of the element.
	 */
	public String getDescription() {
		// Use the name if it exists
		String name = getTagWithKey("name");
		if (name != null && name.length() > 0) {
			return name;
		}
		// Then the house number
		String housenb = getTagWithKey("addr:housenumber");
		if (housenb != null && housenb.length() > 0) {
			return "house " + housenb;
		}
		// Then the value of the most 'important' tag the element has
		for (String tag : importantTags) {
			String value = getTagWithKey(tag);
			if (value != null && value.length() > 0) {
				return getName() + " " + tag + ":" + value;
			}
		}
		// Failing the above, the OSM ID
		return getName() + " #" + Long.toString(getOsmId());
	}
	
	/**
	 * Generate a description of the element that also includes state information.
	 * @param aResources Application resources.
	 * @return A human readable description of the element that includes state information.
	 */
	public String getStateDescription(final Resources aResources) {
		int resid;
		switch (getState()) {
		case STATE_CREATED:
			resid = R.string.changes_created;
			break;
		case STATE_MODIFIED:
			resid = R.string.changes_changed;
			break;
		case STATE_DELETED:
			resid = R.string.changes_deleted;
			break;
		default:
			resid = 0;
			break;
		}
		String result = getDescription();
		if (resid != 0) {
			result = aResources.getString(resid, result);
		}
		return result;
	}
	
	/**
	 * Test if the element has any problems by searching all the tags for the words
	 * "fixme" or "todo".
	 * @return true if the element has any noted problems, false otherwise.
	 */
	protected boolean calcProblem() {
		final String pattern = "(?i).*\\b(?:fixme|todo)\\b.*";
		for (String key : tags.keySet()) {
			// test key and value against pattern
			if (key.matches(pattern) || tags.get(key).matches(pattern)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Test if the element has a noted problem. A noted problem is where someone has
	 * tagged the element with a "fixme" or "todo" key/value.
	 * @return true if the element has a noted problem, false if it doesn't.
	 */
	public boolean hasProblem() {
		// This implementation assumes that calcProblem() may be expensive, and
		// caches the calculation.
		if (!cachedHasProblemValid) {
			cachedHasProblem = calcProblem();
			cachedHasProblemValid = true;
		}
		return cachedHasProblem;
	}
	
	
	/** (see also {@link #getName()} - this returns the full type, differentiating between open and closed ways) 
	 * @return the {@link ElementType} of the element */
	public abstract ElementType getType();
	
	/** Enum for element types (Node, Way, Closedway) */
	public enum ElementType {
		NODE,
		WAY,
		CLOSEDWAY
	}
}
