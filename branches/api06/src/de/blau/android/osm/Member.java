package de.blau.android.osm;

import java.io.Serializable;

import android.util.Log;

public class Member implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3790515905559349096L;

	private final OsmElement osmElement;

	private String role;

	public static final String NAME = "member";

	public Member(OsmElement osmElement, String role) {
		if (osmElement == null) {
			throw new IllegalArgumentException("Member: osmElement has to be not null!");
		}
		this.osmElement = osmElement;
		this.role = role;
	}

	public OsmElement getOsmElement() {
		return osmElement;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public String toString() {
		return "\t\tMember: osmId=" + osmElement.getOsmId() + " type=" + osmElement.getClass().getName() + " role="
				+ role;
	}

	public String toXml() {
		String xml = "";
		if (osmElement.getOsmId() >= 0) {
			xml = "<member type=\"" + osmElement.getName() + "\" ref=\"" + osmElement.getOsmId() + "\" role=\"" + role
					+ "\">\n";
		} else {
			Log.w(NAME, "Referred OsmElement by member (" + this + ") has no osmId!");
		}
		return xml;
	}
}
