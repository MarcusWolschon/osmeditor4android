package de.blau.android.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import de.blau.android.exception.OsmException;

public class Way extends OsmElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294265L;

	private final List<Node> nodes;

	public static final String NAME = "way";

	public static final String NODE = "nd";

	Way(final long osmId, final long osmVersion, final byte status) {
		super(osmId, osmVersion, status);
		this.nodes = new ArrayList<Node>();
	}

	/* package */ int addNode(final Node node) {
		nodes.add(node);
		return nodes.size() - 1;
	}

	public List<Node> getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public byte getType() {
		return OsmElement.TYPE_WAY;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(super.toString());
		for (Entry<String, String> tag : tags.entrySet()) {
			res.append("\t");
			res.append(tag.getKey());
			res.append(" = ");
			res.append(tag.getValue());
		}
		return res.toString();
	}

	@Override
	public void toXml(XmlSerializer s, long changeSetId) {
		try {
			s.startTag("", "way");
			s.attribute("", "id", Long.toString(osmId));
			s.attribute("", "changeset", Long.toString(changeSetId));
			s.attribute("", "version", Long.toString(osmVersion));

			for (Node node : nodes) {
				s.startTag("", "nd");
				s.attribute("", "ref", Long.toString(node.getOsmId()));
				s.endTag("", "nd");
			}

			tagsToXml(s);
			s.endTag("", "way");
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

	public boolean hasNode(final Node node) {
		return nodes.contains(node);
	}

	void removeAllNodes(final Node node) {
		while (nodes.remove(node)) {
			;
		}
	}

	int appendNode(final Node refNode, final Node newNode) throws OsmException {
		if (nodes.get(0) == refNode) {
			nodes.add(0, newNode);
			return 0;
		} else if (nodes.get(nodes.size() - 1) == refNode) {
			nodes.add(newNode);
			return nodes.size() - 1;
		}
		throw new OsmException("refNode must be first or last node.");
	}

	int addNodeAfter(final Node nodeBefore, final Node newNode) {
		int position = nodes.indexOf(nodeBefore) + 1;
		nodes.add(position, newNode);
		return position;
	}

	public boolean isEndNode(final Node node) {
		return nodes.get(0) == node || nodes.get(nodes.size() - 1) == node;
	}
}
