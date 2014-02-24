package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;

/**
 * Relation represents an OSM relation element which essentially is a collection of other OSM elements.
 * 
 * @author simon
 *
 */
public class Relation extends OsmElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294265L;

	protected final ArrayList<RelationMember> members;

	public static final String NAME = "relation";

	public static final String MEMBER = "member";
	
	Relation(final long osmId, final long osmVersion, final byte status) {
		super(osmId, osmVersion, status);
		members = new ArrayList<RelationMember>();
	}

	public void addMember(final RelationMember member) {
		members.add(member);
	}

	public List<RelationMember> getMembers() {
		return members;
	}
	
	public RelationMember getMember(OsmElement e) {
		for (int i = 0; i < members.size(); i++) {
			RelationMember member = members.get(i);
			if (member.getElement() == e) {
				return member;
			}
		}
		return null;
	}
	
	public RelationMember getMember(String type, long id) {
		for (int i = 0; i < members.size(); i++) {
			RelationMember member = members.get(i);
			if (member.getRef() == id && member.getType().equals(type)) {
				return member;
			}
		}
		return null;
	}

	public int getPosition(RelationMember e) {
		return members.indexOf(e);
	}
	
	/**
	 * 
	 * @return list of members allowing {@link Iterator#remove()}.
	 */
	Iterator<RelationMember> getRemovableMembers() {
		return members.iterator();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String toString() {
//		String res = super.toString();
//		for (Map.Entry<String, String> tag : tags.entrySet()) {
//			res += "\t" + tag.getKey() + "=" + tag.getValue();
//		}
//		for (RelationMember m:members) {
//			res += "\t" + m.toString();
//		}
//		return res;
		return getDescription();
	}

	@Override
	public void toXml(final XmlSerializer s, final Long changeSetId) throws IllegalArgumentException,
			IllegalStateException, IOException {
		s.startTag("", "relation");
		s.attribute("", "id", Long.toString(osmId));
		if (changeSetId != null) s.attribute("", "changeset", Long.toString(changeSetId));
		s.attribute("", "version", Long.toString(osmVersion));

		for (RelationMember member : members) {
			s.startTag("", "member");
			s.attribute("", "type", member.getType());
			s.attribute("", "ref", Long.toString(member.getRef()));
			s.attribute("", "role", member.getRole());
			s.endTag("", "member");
		}

		tagsToXml(s);
		s.endTag("", "relation");
	}

	public boolean hasMember(final RelationMember member) {
		return members.contains(member);
	}

	void removeMember(final RelationMember member) {
		while (members.remove(member)) {
			;
		}
	}

	void appendMember(final RelationMember refMember, final RelationMember newMember) {
		if (members.get(0) == refMember) {
			members.add(0, newMember);
		} else if (members.get(members.size() - 1) == refMember) {
			members.add(newMember);
		}
	}

	void addMemberAfter(final RelationMember memberBefore, final RelationMember newMember) {
		members.add(members.indexOf(memberBefore) + 1, newMember);
	}
	
	void addMember(int pos, final RelationMember newMember) {
		if (pos < 0 || pos > members.size()) {
			pos = members.size(); // append
		}
		members.add(pos, newMember);
	}
	
	/**
	 * Adds multiple elements to the relation in the order in which they appear in the list.
	 * They can be either prepended or appended to the existing nodes.
	 * @param newMembers a list of new members
	 * @param atBeginning if true, nodes are prepended, otherwise, they are appended
	 */
	void addMembers(List<RelationMember> newMembers, boolean atBeginning) {
		if (atBeginning) {
			members.addAll(0, newMembers);
		} else {
			members.addAll(newMembers);
		}
	}
	
	public ArrayList <RelationMember> getMembersWithRole(String role) {
		ArrayList <RelationMember> rl = new ArrayList<RelationMember>();
		for (RelationMember rm : members) {
			Log.d("Relation", "getMembersWithRole " + rm.getRole());
			if (role.equals(rm.getRole())) {
				rl.add(rm);
			}
		}
		return rl;
	}
	
	/**
	 * Replace an existing member in a relation with a different member.
	 * @param existing The existing member to be replaced.
	 * @param newMember The new member.
	 */
	void replaceMember(RelationMember existing, RelationMember newMember) {
		int idx;
		while ((idx = members.indexOf(existing)) != -1) {
			members.set(idx, newMember);
		}
	}

	/**
	 * rough implementation for now
	 */
	@Override
	public String getDescription() {
		
		String description = "";
		String type = getTagWithKey("type");
		if (type != null){
			description = type.equals("") ? "unset relation type" : type;
			if (type.equals("restriction")) {
				String restriction = getTagWithKey("restriction");
				if (restriction != null) {
					description = restriction + " " + description;
				}
			} else if (type.equals("route")) {
				String route = getTagWithKey("route");
				if (route != null) {
					description = route + " " + description ;
				}
			} else if (type.equals("multipolygon")) {
				String b = getTagWithKey("boundary");
				String l = getTagWithKey("landuse");
				if (b != null) {
					description = b + " boundary" + " " + description ;
				} else if (l != null) {
					description = "landuse " + l + " " + description ;
				}
			}
		} else
			description = "unset relation type";
		String name = getTagWithKey("name");
		if (name != null){
			description = description + " " + name;
		} else {
			description = description + " #" + osmId;
		}
		return description;
	}

	
	/**
	 * Test if the relation has a problem.
	 * @return true if the relation has a problem, false if it doesn't.
	 */
	@Override
	protected boolean calcProblem() {
		
		return super.calcProblem();
	}

	@Override
	public ElementType getType() {
		return ElementType.RELATION;
	}

	/**
	 * return a list of the downloaded elements
	 * @return
	 */
	public ArrayList<OsmElement> getMemberElements() {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();
		for (RelationMember rm:getMembers()) {
			if (rm.getElement()!=null)
				result.add(rm.getElement());
		}
		return result;
	}
	
	
}
