package de.blau.android.util.rtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.propertyeditor.TagEditorFragment;
import android.graphics.Rect;
import android.util.Log;


/**
 * 2D R-Tree implementation for Android.
 * Uses algorithms from: http://www.sai.msu.su/~megera/postgres/gist/papers/Rstar3.pdf
 * @author Colonel32
 * @author cnvandev
 * @author simonpoole
 */
public class RTree implements Serializable {
	private static final long serialVersionUID = 1L;
	private final static String DEBUG_TAG = RTree.class.getName();
	private Node root;
	private int maxSize;
	private int minSize;
	private QuadraticNodeSplitter splitter;
	
	private class Node implements BoundedObject, Serializable {
		private static final long serialVersionUID = 1L;
		private final String DEBUG_TAG = Node.class.getName();
		Node parent;
		BoundingBox box;
		ArrayList<Node> children;
		ArrayList<BoundedObject> data;
		
		public Node(boolean isLeaf)	{
			if (isLeaf) {
				data = new ArrayList<BoundedObject>(maxSize+1);
			} else {
				children = new ArrayList<Node>(maxSize+1);
			}
		}

		public boolean isLeaf() {
			return data != null;
		}
		
		public boolean isRoot() {
			return parent == null;
		}

		public void addTo(Node parent) {
			assert(parent.children != null);
			parent.children.add(this);
			this.parent = parent;
			computeMBR();
			splitter.split(parent);
		}

		public void computeMBR() {
			computeMBR(true);
		}

		public void computeMBR(boolean doParents) {
			if (box == null) {
				box = new BoundingBox();
			}
			if (!isLeaf()) {
				if (children.isEmpty())
					return;
				
				box.set(children.get(0).box);
				for (int i = 1; i < children.size(); i++) {
					box.union(children.get(i).box);
				}
			} else {
				if (data.isEmpty())
					return;
				
				box.set(data.get(0).getBounds());
				for (int i = 1; i < data.size(); i++) {
					BoundingBox box2 = data.get(i).getBounds();
					if (box2.isEmpty()) {
						box.union(box2.getLeft(), box2.getTop());
					} else {
						box.union(box2);
					}
				}
			}

			if (doParents && parent != null)
				parent.computeMBR();
		}

		public void remove() {
			if (parent == null) {
				assert(root == this);
				root = null;
				return;
			}
			
			parent.children.remove(this);
			
			if (parent.children.isEmpty()) {
				parent.remove();
			} else {
				parent.computeMBR();
			}
		}

		public ArrayList<? extends BoundedObject> getSubItems() {
			return isLeaf() ? data : children;
		}

		public BoundingBox getBounds() {
			Log.d(DEBUG_TAG, "box is " + box);
			return box;
		}
		
		public boolean contains(int px, int py) {
			return box.contains(px, py);
		}

		public int size() {
			return isLeaf() ? data.size() : children.size();
		}
		
		public int depth() {
			Node n = this;
			int d = 0;
			while(n != null) {
				n = n.parent;
				d++;
			}
			return d;
		}

		public String toString() {
			return "Depth: "+depth()+", size: "+size();
		}
	}
	
	private class QuadraticNodeSplitter implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public void split(Node n) {
			if (n.size() <= maxSize) return;
			boolean isleaf = n.isLeaf();

			// Choose seeds. Would write a function for this, but it requires returning 2 objects
			BoundedObject seed1 = null, seed2 = null;
			ArrayList<? extends BoundedObject> list;
			if (isleaf)
				list = n.data;
			else
				list = n.children;

			double maxD = Double.MIN_VALUE;
			BoundingBox box = new BoundingBox();
			for (int i = 0; i < list.size(); i++) {
				for (int j=0; j < list.size(); j++) {
					// Log.d(DEBUG_TAG," i " + i + " j " + j);
					if (i == j) continue;
					BoundedObject n1 = list.get(i), n2 = list.get(j);			
					box.set(n1.getBounds());
					
					BoundingBox box2 = n2.getBounds();
					if (box2.isEmpty()) {
						box.union(box2.getLeft(), box2.getTop());
					} else {
						box.union(box2);
					}
					double d = area(box) - area(n1.getBounds()) - area(n2.getBounds());
					// Log.d(DEBUG_TAG," d " + d );
					if (d > maxD) {
						maxD = d;
						seed1 = n1;
						seed2 = n2;
						// Log.d(DEBUG_TAG,"seed1 " + seed1 + " seed2 " + seed2);
					}
				}
			}
			assert(seed1 != null && seed2 != null);
			// Log.d(DEBUG_TAG,"seed1 " + seed1 + " seed2 " + seed2);
			// Distribute
			Node group1 = new Node(isleaf);
			group1.box = new BoundingBox(seed1.getBounds());
			Node group2 = new Node(isleaf);
			group2.box = new BoundingBox(seed2.getBounds());
			if (isleaf)
				distributeLeaves(n, group1, group2);
			else
				distributeBranches(n, group1, group2);

			Node parent = n.parent;
			if (parent == null) {
				parent = new Node(false);
				root = parent;
			} else {
				parent.children.remove(n);
			}

			group1.parent = parent;
			parent.children.add(group1);
			group1.computeMBR();
			split(parent);

			group2.parent = parent;
			parent.children.add(group2);
			group2.computeMBR();
			split(parent);
		}
		
		private void distributeBranches(Node n, Node g1, Node g2) {
			assert(!(n.isLeaf() || g1.isLeaf() || g2.isLeaf()));

			while(!n.children.isEmpty() && g1.children.size() < maxSize - minSize + 1 && g2.children.size() < maxSize - minSize + 1) {
				// Pick next
				int difmax = Integer.MIN_VALUE;
				int nmax_index = -1;
				for (int i = 0; i < n.children.size(); i++) {
					Node node = n.children.get(i);
					int expansion1 = expansionNeeded(node.box, g1.box);
					int expansion2 = expansionNeeded(node.box, g2.box);
					int dif = Math.abs(expansion1 - expansion2);
					if (dif > difmax) {
						difmax = dif;
						nmax_index = i;
					}
				}
				assert(nmax_index != -1);

				// Distribute Entry
				Node nmax = n.children.remove(nmax_index);
				Node parent = null;

				// ... to the one with the least expansion
				int overlap1 = expansionNeeded(nmax.box, g1.box);
				int overlap2 = expansionNeeded(nmax.box, g2.box);
				if (overlap1 > overlap2) {
					parent = g1;
				} else if (overlap2 > overlap1) {
					parent = g2;
				} else {
					// Or the one with the lowest area
					double area1 = area(g1.box);
					double area2 = area(g2.box);
					if (area1 > area2) parent = g2;
					else if (area2 > area1) parent = g1;
					else {
						// Or the one with the least items
						if (g1.children.size() < g2.children.size()) parent = g1;
						else parent = g2;
					}
				}
				assert(parent != null);
				parent.children.add(nmax);
				nmax.parent = parent;
			}

			if (!n.children.isEmpty()) {
				Node parent = null;
				if (g1.children.size() == maxSize - minSize + 1)
					parent = g2;
				else
					parent = g1;

				for (int i = 0; i < n.children.size(); i++)	{
					parent.children.add(n.children.get(i));
					n.children.get(i).parent = parent;
				}
				n.children.clear();
			}
		}
		
		private void distributeLeaves(Node n, Node g1, Node g2) {
			// Same process as above; just different types.
			assert(n.isLeaf() && g1.isLeaf() && g2.isLeaf());

			while(!n.data.isEmpty() && g1.data.size() < maxSize - minSize + 1 && g2.data.size() < maxSize - minSize + 1) {
				// Pick next
				int difmax = Integer.MIN_VALUE;
				int nmax_index = -1;
				for (int i = 0; i < n.data.size(); i++) {
					BoundedObject node = n.data.get(i);
					int d1 = expansionNeeded(node.getBounds(), g1.box);
					int d2 = expansionNeeded(node.getBounds(), g2.box);
					int dif = Math.abs(d1 - d2);
					if (dif > difmax) {
						difmax = dif;
						nmax_index = i;
					}
				}
				assert(nmax_index != -1);

				// Distribute Entry
				BoundedObject nmax = n.data.remove(nmax_index);

				// ... to the one with the least expansion
				int overlap1 = expansionNeeded(nmax.getBounds(), g1.box);
				int overlap2 = expansionNeeded(nmax.getBounds(), g2.box);
				if (overlap1 > overlap2) {
					g1.data.add(nmax);
				} else if (overlap2 > overlap1) {
					g2.data.add(nmax);
				} else {
					double area1 = area(g1.box);
					double area2 = area(g2.box);
					if (area1 > area2) {
						g2.data.add(nmax);
					} else if (area2 > area1) {
						g1.data.add(nmax);
					} else {
						if (g1.data.size() < g2.data.size()) {
							g1.data.add(nmax);
						} else {
							g2.data.add(nmax);
						}
					}
				}
			}

			if (!n.data.isEmpty()) {
				if (g1.data.size() == maxSize - minSize + 1) {
					g2.data.addAll(n.data);
				} else {
					g1.data.addAll(n.data);
				}
				n.data.clear();
			}
		}
	}

	/**
	 * Default constructor.
	 */
	public RTree() {
		this(2, 12);
	}
	
	/**
	 * Creates an R-Tree. Sets the splitting algorithm to quadratic splitting.
	 * @param minChildren Minimum children in a node.  {@code 2 <= minChildren <= maxChildren/2}
	 * @param maxChildren Maximum children in a node. Node splits at this number + 1
	 */
	public RTree(int minChildren, int maxChildren) {
		if (minChildren < 2 || minChildren > maxChildren/2)
			throw new IllegalArgumentException("2 <= minChildren <= maxChildren/2");
		splitter = new QuadraticNodeSplitter();
				
		this.minSize = minChildren;
		this.maxSize = maxChildren;
		root = null;
	}

	/**
	 * Adds items whose AABB intersects the query AABB to results
	 * @param results A collection to store the query results
	 * @param box The query
	 */
	public void query(Collection<BoundedObject> results) {
		BoundingBox box;
		try {
			box = new BoundingBox(-BoundingBox.MAX_LON_E7, -BoundingBox.MAX_LAT_E7, BoundingBox.MAX_LON_E7, BoundingBox.MAX_LAT_E7);
			query(results, box, root);
		} catch (OsmException e) {
			// shouldn't happen but log anyway
			Log.d(DEBUG_TAG,"new BoundingBox caused " + e);
		}
	}
	public void query(Collection<BoundedObject> results, BoundingBox box) {
		query(results, box, root);
	}
	private void query(Collection<BoundedObject> results, BoundingBox box, Node node) {
		// Log.d(DEBUG_TAG,"query called");
		if (node == null) return;
		if (node.isLeaf()) {
			// Log.d(DEBUG_TAG,"leaf");
			for (int i = 0; i < node.data.size(); i++) {
				BoundedObject bo = node.data.get(i);
				BoundingBox box2 = bo.getBounds();
				if (!box2.isEmpty()) {	
					if (BoundingBox.intersects(box2, box)) {
						results.add(bo);
					}
				} else {
					// Log.d(DEBUG_TAG,"point " + box2.left + " " + box2.top + " " + box);
					if (box.contains(box2.getLeft(),box2.getTop())) {
						results.add(bo);
						// Log.d(DEBUG_TAG,"adding " + bo);
					}
				}
			}
		} else {
			// Log.d(DEBUG_TAG,"not leaf");
			for (int i = 0; i < node.children.size(); i++) {
				if (BoundingBox.intersects(node.children.get(i).box, box)) {
					query(results, box, node.children.get(i));
				}
			}
		}
	}

	/**
	 * Returns one item that intersects the query box, or null if nothing intersects
	 * the query box.
	 */
	public BoundedObject queryOne(BoundingBox box) {
		return queryOne(box,root);
	}
	private BoundedObject queryOne(BoundingBox box, Node node) {
		if (node == null) return null;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				BoundingBox box2 = node.data.get(i).getBounds();
				if (!box2.isEmpty()) {	
					if (BoundingBox.intersects(box2, box)) {
						return node.data.get(i);
					}
				} else {
					if (box.contains(box2.getLeft(),box2.getTop())) {
						return node.data.get(i);
					}
				}
			}
			return null;
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (BoundingBox.intersects(node.children.get(i).box, box)) {
					BoundedObject result = queryOne(box,node.children.get(i));
					if (result != null) return result;
				}
			}
			return null;
		}
	}

	/**
	 * Returns items whose Rect contains the specified point.
	 * @param results A collection to store the query results.
	 * @param px Point X coordinate
	 * @param py Point Y coordinate
	 */
	public void query(Collection<? super BoundedObject> results, int px, int py) {
		query(results, px, py, root);
	}
	private void query(Collection<? super BoundedObject> results, int px, int py, Node node) {
		if (node == null) return;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				BoundingBox b = node.data.get(i).getBounds();
				if (b.isEmpty()) {
					if (b.getLeft() == px && b.getTop() == py) {
						results.add(node.data.get(i));
					}
				} else {
					if (b.contains(px, py)) {
						results.add(node.data.get(i));
					}
				}
			}
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (node.children.get(i).box.contains(px, py)) {
					query(results, px, py, node.children.get(i));
				}
			}
		}
	}

	/**
	 * Returns one item that intersects the query point, or null if no items intersect that point.
	 */
	public BoundedObject queryOne(int px, int py) {
		return queryOne(px, py, root);
	}
	private BoundedObject queryOne(int px, int py, Node node) {
		if (node == null) return null;
		if (node.isLeaf()) {
			for (int i = 0; i < node.data.size(); i++) {
				if (node.data.get(i).getBounds().contains(px, py)) {
					return node.data.get(i);
				}
			}
			return null;
		} else {
			for (int i = 0; i < node.children.size(); i++) {
				if (node.children.get(i).box.contains(px, py)) {
					BoundedObject result = queryOne(px, py, node.children.get(i));
					if (result != null) return result;
				}
			}
			return null;
		}
	}

	/**
	 * Removes the specified object if it is in the tree.
	 * @param o
	 */
	public synchronized void remove(BoundedObject o) {
		Node n = chooseLeaf(o, root);
		assert(n.isLeaf());
		n.data.remove(o);
		n.computeMBR();
	}
	
	/**
	 * Return true if o is in the tree.
	 * @param o
	 */
	public synchronized boolean contains(BoundedObject o) {
		Node n = chooseLeaf(o, root);
		assert(n.isLeaf());
		return n.data.contains(o);
	}

	/**
	 * Inserts object o into the tree. Note that if the value of o.getBounds() changes
	 * while in the R-tree, the result is undefined.
	 * @throws NullPointerException If o == null
	 */
	public synchronized void insert(BoundedObject o) {
		if (o == null) throw new NullPointerException("Cannot store null object");
		if (root == null)
			root = new Node(true);

		Node n = chooseLeaf(o, root);
		assert(n.isLeaf());
		n.data.add(o);
		n.computeMBR();
		splitter.split(n);
	}

	/**
	 * Counts the number of items in the tree.
	 */
	public int count() {
		if (root == null) return 0;
		return count(root);
	}
	private int count(Node n) {
		assert(n != null);
		if (n.isLeaf()) {
			return n.data.size();
		} else {
			int sum = 0;
			for (int i = 0; i < n.children.size(); i++)
				sum += count(n.children.get(i));
			return sum;
		}
	}

	private Node chooseLeaf(BoundedObject o, Node n) {
		assert(n != null);
		if (n.isLeaf()) {
			return n;
		} else {
			BoundingBox box = o.getBounds();

			int maxOverlap = Integer.MAX_VALUE;
			Node maxnode = null;
			for (int i = 0; i < n.children.size(); i++) {
				int overlap = expansionNeeded(n.children.get(i).box, box);
				if ((overlap < maxOverlap) || (overlap == maxOverlap)
						&& area(n.children.get(i).box) < area(maxnode.box)) {
					maxOverlap = overlap;
					maxnode = n.children.get(i);
				}
			}
			
			if (maxnode == null) // Not sure how this could occur
				return null;
			
			return chooseLeaf(o, maxnode);
		}
	}
	
	/**
	 * Returns the amount that other will need to be expanded to fit this.
	 */
	private static int expansionNeeded(BoundingBox one, BoundingBox two) {
		int total = 0;

		if(two.getLeft() < one.getLeft()) total += one.getLeft() - two.getLeft();
		if(two.getRight() > one.getRight()) total += two.getRight() - one.getRight();

		if(two.getTop() < one.getTop()) total += one.getTop() - two.getTop();
		if(two.getBottom() > one.getBottom()) total += two.getBottom() - one.getBottom();

		return total;
	}
	
	private static double area(BoundingBox box) {
		return (double)box.getWidth() * (double)box.getHeight();
	}
}