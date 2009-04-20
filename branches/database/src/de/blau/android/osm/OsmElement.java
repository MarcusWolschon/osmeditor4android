package de.blau.android.osm;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public abstract class OsmElement implements Serializable {

	private static final long serialVersionUID = 7711945069147743666L;
	
	public static enum State {
		UNCHANGED, CREATED, MODIFIED, DELETED
	}
	
	public static enum Type {
		NODE, WAY, RELATION 
	}
	
	protected long osmId;

	protected TreeMap<String, String> tags;

	protected State state;

	OsmElement(final long osmId, final State state) {
		this.osmId = osmId;
		this.tags = new TreeMap<String, String>();
		this.state = state;
	}

	public long getOsmId() {
		return osmId;
	}

	void setOsmId(final long osmId) {
		this.osmId = osmId;
	}

	public SortedMap<String, String> getTags() {
		return Collections.unmodifiableSortedMap(tags);
	}

	public Set<Map.Entry<String, String>> getTagSet() {
		return Collections.unmodifiableSet(tags.entrySet());
	}

	public State getState() {
		return state;
	}

	abstract public String getName();
	
	abstract public Type getType();

	void setState(final State newState) {
		this.state = newState;
	}

	void addOrUpdateTag(final String key, final String value) {
		tags.put(key, value);
	}

	void addTags(final Map<String, String> tags) {
		this.tags.putAll(tags);
	}

	void setTags(final Map<String, String> tags) {
		if (tags == null) {
			this.tags.clear();
		} else {
			this.tags = new TreeMap<String, String>(tags);
		}
	}

	public boolean hasTag(final String key, final String value) {
		return tags.get(key).equals(value);
	}

	public String getTagWithKey(final String key) {
		return tags.get(key);
	}

	public boolean hasTagKey(final String key) {
		return tags.containsKey(key);
	}

	@Override
	public String toString() {
		return getName() + " #" + Long.toString(getOsmId());
	}

	abstract public String toXml();

	public String tagsToXml() {
		StringBuilder xml = new StringBuilder();
		for (Entry<String, String> tag : tags.entrySet()) {
			xml.append("  <tag k=\"" + tag.getKey() + "\" v=\""
					+ tag.getValue() + "\"/>\n");
		}
		return xml.toString();
	}

	public boolean isUnchanged() {
		return state == State.UNCHANGED;
	}

	public String getDescription() {
		String name = getTagWithKey("name");
		if (name != null && name.length() > 0)
			return name;
		return toString();
	}

}
