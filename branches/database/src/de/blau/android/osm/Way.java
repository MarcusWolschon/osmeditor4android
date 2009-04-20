package de.blau.android.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import de.blau.android.exception.OsmException;

import android.util.Log;

public class Way extends OsmElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1104911642016294265L;

	private final List<Node> nodes;

	public static final String NAME = "way";

	public static final String NODE = "nd";

	Way(final long osmId, final State state) {
		super(osmId, state);
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
	public Type getType() {
		return Type.WAY;
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
	public String toXml() {
		String xml = "";
		xml += "<way id=\"" + osmId + "\">\n";
		for (int i = 0, size = nodes.size(); i < size; ++i) {
			long nodeId = nodes.get(i).getOsmId();
			if (nodeId > 0) {
				xml += "  <nd ref=\"" + nodeId + "\"/>\n";
			} else {
				Log.e(NAME, "Referred node of way (" + this + ") has no osmId!");
			}
		}
		xml += tagsToXml();
		xml += "</way>";
		return xml;
	}

	public boolean hasNode(final Node node) {
		return nodes.contains(node);
	}

	void removeAllNodes(final Node node) {
		while (nodes.remove(node)) {
			;
		}
	}

	int appendNode(final Node refNode, final Node newNode)
			throws OsmException {
		if (nodes.get(0) == refNode) {
			nodes.add(0, newNode);
			return 0;
		} else if (nodes.get(nodes.size() - 1) == refNode) {
			nodes.add(newNode);
			return nodes.size() - 1;
		}
		throw new OsmException(
				"refNode must be first or last node.");
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
