package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

public abstract class OsmElement implements Serializable, XmlSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7711945069147743666L;

	public static final long NEW_OSM_ID = -1;

	public static final byte STATE_UNCHANGED = 0;

	public static final byte STATE_CREATED = 1;

	public static final byte STATE_MODIFIED = 2;

	public static final byte STATE_DELETED = 3;

	protected long osmId;

	protected long osmVersion;

	protected SortedMap<String, String> tags;

	protected byte state;

	OsmElement(final long osmId, final long osmVersion, final byte state) {
		this.osmId = osmId;
		this.osmVersion = osmVersion;
		this.tags = new TreeMap<String, String>();
		this.state = state;
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

	abstract public String getName();

	/**
	 * Does not set the state if it's on CREATED, but if new state is DELETED.
	 * 
	 * @param newState
	 */
	void updateState(final byte newState) {
		if (state != STATE_CREATED || newState == STATE_DELETED) {
			this.state = newState;
		}
	}

	void setState(final byte newState) {
		this.state = newState;
	}

	void addOrUpdateTag(final String tag, final String value) {
		tags.put(tag, value);
	}

	void addTags(final Map<String, String> tags) {
		this.tags.putAll(tags);
	}

	void setTags(final Map<String, String> tags) {
		this.tags.clear();
		if (tags != null) {
			this.tags.putAll(tags);
		}
	}

	public boolean hasTag(final String key, final String value) {
		String keyValue = tags.get(key);
		return keyValue != null && keyValue.equals(value);
	}

	public String getTagWithKey(final String key) {
		return this.tags.get(key);
	}

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

	public String getDescription() {
		String name = getTagWithKey("name");
		if (name != null && name.length() > 0)
			return name;
		return getName() + " #" + Long.toString(getOsmId());
	}
}
