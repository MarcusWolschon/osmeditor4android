package de.blau.android.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

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

	void addNode(final Node node) {
		nodes.add(node);
	}

	public List<Node> getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String toString() {
		String res = super.toString();
		for (Iterator<Tag> it = tags.iterator(); it.hasNext();) {
			res += "\t" + it.next();
		}
		return res;
	}

	@Override
	public String toXml(long changesetId) {
		String xml = "";
		xml += "<way id=\"" + osmId + "\" changeset=\"" + changesetId + "\" version=\"" + osmVersion + "\">\n";
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

	void appendNode(final Node refNode, final Node newNode) {
		if (nodes.get(0) == refNode) {
			nodes.add(0, newNode);
		} else if (nodes.get(nodes.size() - 1) == refNode) {
			nodes.add(newNode);
		}
	}

	void addNodeAfter(final Node nodeBefore, final Node newNode) {
		nodes.add(nodes.indexOf(nodeBefore) + 1, newNode);
	}

	public boolean isEndNode(final Node node) {
		return nodes.get(0) == node || nodes.get(nodes.size() - 1) == node;
	}
}
