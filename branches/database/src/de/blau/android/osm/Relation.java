package de.blau.android.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Relation extends OsmElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = -826632525687546912L;

	private final List<Member> members;

	public static final String NAME = "relation";

	Relation(final long osmId, final String user, final Date dateChanged, final State state) {
		super(osmId, state);
		this.members = new ArrayList<Member>();
	}

	void addMember(final Member member) {
		members.add(member);
	}

	public List<Member> getMembers() {
		return Collections.unmodifiableList(members);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Type getType() {
		return Type.RELATION;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public String toXml() {
		String xml = "";
		xml += "<relation id=\"" + osmId + "\">\n";
		for (int i = 0, size = members.size(); i < size; ++i) {
			xml += members.get(i).toXml();
		}
		xml += tagsToXml();
		xml += "</relation>\n";
		return xml;
	}

}
