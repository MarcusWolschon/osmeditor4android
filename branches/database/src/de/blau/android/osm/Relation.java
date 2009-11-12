package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

public class Relation extends OsmElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = -826632525687546912L;

	private final List<Member> members;

	public static final String NAME = "relation";

	Relation(final long osmId, Long version, final byte state) {
		super(osmId, version, state);
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
	public byte getType() {
		return OsmElement.TYPE_RELATION;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public void toXml(XmlSerializer s, long changeSetId) {
		try {
			s.startTag("", "relation");
			s.attribute("", "id", Long.toString(osmId));
			s.attribute("", "changeset", Long.toString(changeSetId));
			s.attribute("", "version", Long.toString(osmVersion));
			// TODO members
			tagsToXml(s);
			s.endTag("", "relation");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
