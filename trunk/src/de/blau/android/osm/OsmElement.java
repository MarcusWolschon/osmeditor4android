package de.blau.android.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class OsmElement implements Serializable {

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

	protected String user;

	protected Date dateChanged;

	protected List<Tag> tags;

	protected byte state;

	OsmElement(final long osmId, final String user, final Date timestamp, final byte state) {
		this.osmId = osmId;
		this.user = user;
		this.dateChanged = timestamp;
		this.tags = new ArrayList<Tag>();
		this.state = state;
	}

	public long getOsmId() {
		return osmId;
	}

	void setOsmId(final long osmId) {
		this.osmId = osmId;
	}

	public String getUser() {
		return user;
	}

	void setUser(final String user) {
		this.user = user;
	}

	public Date getDateChanged() {
		return dateChanged;
	}

	void setDateChanged(final Date timestamp) {
		this.dateChanged = timestamp;
	}

	public List<Tag> getTags() {
		return Collections.unmodifiableList(tags);
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

	void addTag(final Tag tag) {
		tags.add(tag);
	}

	void addOrUpdateTag(final Tag tag) {
		for (int i = 0, size = tags.size(); i < size; ++i) {
			Tag current = tags.get(i);
			if (current.getK().equals(tag.getK())) {
				current.setValue(tag.getV());
				return;
			}
		}
		//nothing found -> simple add
		tags.add(tag);
	}

	void addTags(final List<Tag> tags) {
		this.tags.addAll(tags);
	}

	void setTags(final List<Tag> tags) {
		if (tags == null) {
			this.tags = new ArrayList<Tag>();
		} else {
			this.tags = tags;
		}
	}

	public boolean hasTag(final String key, final String value) {
		for (int i = 0, size = tags.size(); i < size; ++i) {
			Tag tag = tags.get(i);
			if (tag.getK().equals(key) && tag.getV().equals(value)) {
				return true;
			}
		}
		return false;
	}

	public Tag getTagWithKey(final String key) {
		for (int i = 0, size = tags.size(); i < size; ++i) {
			Tag tag = tags.get(i);
			if (tag.getK().equals(key)) {
				return tag;
			}
		}
		return null;
	}
	
	public boolean hasTagKey(final String key) {
		return getTagWithKey(key) != null;
	}

	@Override
	public String toString() {
		return getName() + " " + osmId;
	}

	abstract public String toXml();

	public String tagsToXml() {
		String xml = "";
		for (int i = 0, size = tags.size(); i < size; ++i) {
			xml += tags.get(i).toXml();
		}
		return xml;
	}

	public boolean isUnchanged() {
		return state == STATE_UNCHANGED;
	}
	
	public String getDescription() {
		Tag tag = getTagWithKey("name");
		if (tag != null) {
			String name = tag.getV();
			if (name != null && name.length() > 0)
				return name;
		}
		return getName() + " #" + Long.toString(getOsmId());
	}
}
