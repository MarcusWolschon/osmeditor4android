package de.blau.android.osm;

import java.io.Serializable;

public class Tag implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8581024888419601981L;

	private final String k;

	private String v;

	public static final String NAME = "tag";

	public Tag(final String k, final String v) {
		if ((k == null) || (v == null)) {
			throw new IllegalArgumentException("Key and value have to be not null!");
		}
		this.k = k;
		this.v = v;
	}

	void setValue(final String value) {
		v = value;
	}

	public String getK() {
		return k;
	}

	public String getV() {
		return v;
	}

	@Override
	public String toString() {
		return k + " = " + v;
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof Tag) {
			Tag tag = (Tag) o;
			return tag.getK().equals(k) && tag.getV().equals(v);
		} else {
			return false;
		}
	}

	public String toXml() {
		return "  <tag k=\"" + k + "\" v=\"" + v + "\"/>\n";
	}
}
