package de.blau.android.osm;
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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof RelationMemberDescription 
				&& ref == ((RelationMemberDescription) o).ref 
				&& type.equals(((RelationMemberDescription) o).type) 
				&& (role == ((RelationMemberDescription) o).role || (role != null && role.equals(((RelationMemberDescription) o).role)))) {
			return true;
		}
		return false;
	}
}
