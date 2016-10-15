package de.blau.android.osm;

import de.blau.android.Application;

/*
 * RelationMemberDescritption is an extended version of RelationMember that holds a textual description of the element 
 * instead of the element itself
 */
public class RelationMemberDescription extends RelationMember {
	private static final long serialVersionUID = 1104911642016294268L;
	private String description = null;
	private boolean downloaded = false;
	
	public RelationMemberDescription(final RelationMember rm) {
		super(rm.getElement() != null ? rm.getElement().getName() : rm.getType(), rm.getElement() != null ? rm.getElement().getOsmId() : rm.getRef(), rm.getRole());
		OsmElement e = rm.getElement();
		if (e != null) {
			description = e.getDescription(false);
			downloaded = true;
		} else {
			description = "#" + ref;
		}
	}
	
	public RelationMemberDescription(final String t, final long id, final String r, final String d) {
		super(t, id, r);	
		description = d;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean downloaded() {
		return downloaded;
	}
	
	/**
	 * If an downloaded element is present update description and downloaded status
	 */
	public void update() {
		OsmElement e = getElement();
		if (e != null) {
			description = e.getDescription(false);
			downloaded = true;
		}
	}
	
	/**
	 * This returns (if present), the element directly from storage
	 */
	@Override
	public OsmElement getElement() {
		return super.getElement()==null ? Application.getDelegator().getOsmElement(getType(), getRef()):super.getElement();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof RelationMemberDescription 
				&& ref == ((RelationMemberDescription) o).ref 
				&& type.equals(((RelationMemberDescription) o).type) 
				&& ((role == null && ((RelationMemberDescription) o).role == null) || (role != null && role.equals(((RelationMemberDescription) o).role)))) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + (int)(ref ^ (ref >>> 32));
		result = 37 * result + (type == null ? 0 : type.hashCode());
		result = 37 * result + (role == null ? 0 : role.hashCode());
		return result;
	}
}
