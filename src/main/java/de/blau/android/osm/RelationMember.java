package de.blau.android.osm;

import java.io.Serializable;

/**
 * RelationMember stores the necessary information for a relation member, if the element field is null the element itself is not present
 * (not downloaded typically) and only the osm id, type (needed to make the id unique) and role fields are stored.
 * @author simon
 *
 */
public class RelationMember implements Serializable {
	
	private static final long serialVersionUID = 4L;
	String	type = null;
	long	ref = Long.MIN_VALUE;
	String 	role = null;
	OsmElement element = null;
	
	/**
	 * Constructor for members that have not been downloaded
	 */
	public RelationMember(final String t, final long id, final String r)
	{
		type = t;
		ref = id;
		role = r;
	}
	
	/**
	 * Constructor for members that have been downloaded
	 */
	public RelationMember(final String r, final OsmElement e)
	{
		role = r;
		element = e;
	}
	
	/**
	 * Constructor for copying, assumes that only role changes
	 */
	public RelationMember(final RelationMember rm)
	{
		if (rm.element == null) {
			type = rm.type;
			ref = rm.ref;
			role = new String(rm.role);
		} else {
			role = new String(rm.role);
			element = rm.element;
		}
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
	
	public void setRole(final String role) {
		this.role = role;
	}
	
	/**
	 * @return the element if downloaded, null if it isn't
	 */
	public OsmElement getElement() {
		return element;
	}
	
	/**
	 * set the element, used for post processing relations
	 * @param e
	 */
	public void setElement(final OsmElement e) {
		element=e;
	}
	
	@Override
	public String toString() {
		return role + " " + type + " " + ref;
	}
}
