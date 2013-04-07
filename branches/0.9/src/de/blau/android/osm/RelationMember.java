package de.blau.android.osm;

import java.io.Serializable;

public class RelationMember implements Serializable {
	
	private static final long serialVersionUID = 1104911642016294266L;
	String	type = null;
	long	ref;
	String 	role = null;
	OsmElement element = null;
	
	/**
	 * Constructor for members that have not been downloaded
	 */
	RelationMember(String t, long id, String r)
	{
		type = t;
		ref = id;
		role = r;
	}
	
	/**
	 * Constructor for members that have not been downloaded
	 */
	RelationMember(String r, OsmElement e)
	{
		role = r;
		element = e;
	}
	
	public String getType() {
		if (element != null)
		{
			return element.getName();
		}
		return type;
	}
	
	public long getRef() {
		if (element != null)
		{
			return element.getOsmId();
		}
		return ref;
	}
	
	public String getRole() {
		return role;
	}
	
	public OsmElement getElement() {
		return element;
	}
	
	/**
	 * set the element, used for post processing relations
	 * @param e
	 */
	public void setElement(OsmElement e) {
		element=e;
	}
}