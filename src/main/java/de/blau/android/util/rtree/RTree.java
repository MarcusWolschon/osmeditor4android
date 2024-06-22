package de.blau.android.util.rtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.BoundingBox;

/**
 * 2D R-Tree implementation for Android. Uses algorithms from:
 * http://www.sai.msu.su/~megera/postgres/gist/papers/Rstar3.pdf
 * 
 * @author Colonel32
 * @author cnvandev
 * @author simonpoole
 */
public class RTree<T extends BoundedObject & Serializable> implements Serializable {
    private static final long        serialVersionUID = 1L;
    private Node<T>                  root;
    private int                      maxSize;
    private int                      minSize;
    private QuadraticNodeSplitter<T> splitter;

    private class Node<Q extends BoundedObject & Serializable> implements BoundedObject, Serializable {
        private static final long  serialVersionUID = 1L;
        private Node<Q>            parent;
        private BoundingBox        box;
        private ArrayList<Node<Q>> children;
        private ArrayList<Q>       data;

        /**
         * Construct a new tree Node
         * 
         * @param isLeaf if true the Node is a leaf Node
         */
        public Node(boolean isLeaf) {
            if (isLeaf) {
                data = new ArrayList<>(maxSize + 1);
            } else {
                children = new ArrayList<>(maxSize + 1);
            }
        }

        /**
         * Check if this is a leaf Node
         * 
         * @return true is a leaf Node
         */
        public boolean isLeaf() {
            return data != null;
        }

        /**
         * Compute and set the BoundingBox for this Node and its parents
         */
        public void computeMBR() {
            computeMBR(true);
        }

        /**
         * Compute and set the BoundingBox for this Node
         * 
         * @param doParents if true compute the BoundingBox for the parent
         */
        public void computeMBR(boolean doParents) {
            if (box == null) {
                box = new BoundingBox();
            }
            if (!isLeaf()) {
                if (children.isEmpty()) {
                    return;
                }
                box.set(children.get(0).box);
                final int size = children.size();
                for (int i = 1; i < size; i++) {
                    box.union(children.get(i).box);
                }
            } else {
                if (data.isEmpty()) {
                    return;
                }
                BoundingBox temp = new BoundingBox();
                box.set(data.get(0).getBounds(temp));
                final int size = data.size();
                for (int i = 1; i < size; i++) {
                    BoundingBox box2 = data.get(i).getBounds(temp);
                    if (box2.isEmpty()) {
                        box.union(box2.getLeft(), box2.getTop());
                    } else {
                        box.union(box2);
                    }
                }
            }
            if (doParents && parent != null) {
                parent.computeMBR();
            }
        }

        /**
         * Get the BoundingBox for this Node
         * 
         * @return the BoundingBox
         */
        @Override
        public BoundingBox getBounds() {
            return box;
        }

        /**
         * Get the number of Nodes or BoundedObject this Node contains
         * 
         * @return the size of the Node
         */
        public int size() {
            return isLeaf() ? data.size() : children.size();
        }

        /**
         * Get the depth of this Node in the tree
         * 
         * @return the dept
         */
        public int depth() {
            Node<Q> n = this;
            int d = 0;
            while (n != null) {
                n = n.parent;
                d++;
            }
            return d;
        }

        @Override
        public String toString() {
            return "Depth: " + depth() + ", size: " + size();
        }
    }

    private class QuadraticNodeSplitter<S extends BoundedObject & Serializable> implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Split a node
         * 
         * @param parent2 the node
         */
        public void split(@NonNull Node<S> parent2) {
            if (parent2.size() <= maxSize) {
                return;
            }
            boolean isleaf = parent2.isLeaf();

            // Choose seeds. Would write a function for this, but it requires returning 2 objects
            Object[] list;
            if (isleaf) {
                list = parent2.data.toArray();
            } else {
                list = parent2.children.toArray();
            }

            List<BoundingBox> cachedBox = new ArrayList<>(list.length);
            double[] cachedArea = new double[list.length];

            for (int i = 0; i < list.length; i++) {
                BoundingBox tempBox = ((BoundedObject) list[i]).getBounds();
                cachedBox.add(tempBox);
                double tempArea = area(tempBox);
                cachedArea[i] = tempArea;
            }

            BoundingBox box = new BoundingBox();

            BoundingBox seed1Box = cachedBox.get(0); // Note we need to cater for the degenerate cases
            BoundingBox seed2Box = cachedBox.get(list.length - 1);

            double maxD = -Double.MAX_VALUE;
            for (int i = 0; i < list.length; i++) {
                for (int j = 0; j < list.length; j++) {
                    if (i == j) {
                        continue;
                    }
                    BoundingBox box1 = cachedBox.get(i);
                    BoundingBox box2 = cachedBox.get(j);

                    box.set(box1);
                    double d;
                    if (box2.isEmpty()) {
                        if (box.isEmpty()) {
                            // not sure if this really works
                            box.union(box2.getLeft(), box2.getTop());
                            d = area(box);
                            if (d == 0) {
                                d = (double) box.getRight() - box2.getRight();
                                if (d == 0) {
                                    d = (double) box.getTop() - box2.getTop();
                                }
                                // else ... two nodes in the same place
                            }
                        } else {
                            box.union(box2.getLeft(), box2.getTop());
                            d = area(box) - cachedArea[i];
                        }
                    } else {
                        box.union(box2);
                        d = area(box) - cachedArea[i] - cachedArea[j];
                    }
                    if (d > maxD) {
                        maxD = d;
                        seed1Box = box1;
                        seed2Box = box2;
                    }
                }
            }

            // Distribute
            Node<S> group1 = new Node<>(isleaf);
            group1.box = new BoundingBox(seed1Box);
            Node<S> group2 = new Node<>(isleaf);
            group2.box = new BoundingBox(seed2Box);
            if (isleaf) {
                distributeLeaves(parent2, cachedBox, group1, group2);
            } else {
                distributeBranches(parent2, group1, group2);
            }
            Node<S> parent = parent2.parent;
            if (parent == null) {
                parent = new Node<>(false);
                root = (Node<T>) parent;
            } else {
                parent.children.remove(parent2);
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

        /**
         * Distribute branches from n to two new nodes
         * 
         * @param n the original node
         * @param g1 new node 1
         * @param g2 new node 2
         */
        private void distributeBranches(@NonNull Node<S> n, @NonNull Node<S> g1, @NonNull Node<S> g2) {
            while (!n.children.isEmpty() && g1.children.size() < maxSize - minSize + 1 && g2.children.size() < maxSize - minSize + 1) {
                // Pick next
                long difmax = Long.MIN_VALUE;
                int nmaxIndex = -1;
                long overlap1 = -1;
                long overlap2 = -1;
                final int size = n.children.size();
                for (int i = 0; i < size; i++) {
                    Node<S> node = n.children.get(i);
                    long expansion1 = expansionNeeded(node.box, g1.box);
                    long expansion2 = expansionNeeded(node.box, g2.box);
                    long dif = Math.abs(expansion1 - expansion2);
                    if (dif > difmax) {
                        difmax = dif;
                        nmaxIndex = i;
                        overlap1 = expansion1;
                        overlap2 = expansion2;
                    }
                }

                // Distribute Entry
                Node<S> nmax = n.children.remove(nmaxIndex);
                Node<S> parent = null;

                // ... to the one with the least expansion
                if (overlap1 > overlap2) {
                    parent = g1;
                } else if (overlap2 > overlap1) {
                    parent = g2;
                } else {
                    // Or the one with the lowest area
                    double area1 = area(g1.box);
                    double area2 = area(g2.box);
                    if (area1 > area2) {
                        parent = g2;
                    } else if (area2 > area1) {
                        parent = g1;
                    } else {
                        // Or the one with the least items
                        if (g1.children.size() < g2.children.size()) {
                            parent = g1;
                        } else {
                            parent = g2;
                        }
                    }
                }
                parent.children.add(nmax);
                nmax.parent = parent;
            }

            if (!n.children.isEmpty()) {
                Node<S> parent = null;
                if (g1.children.size() == maxSize - minSize + 1) {
                    parent = g2;
                } else {
                    parent = g1;
                }
                final int size = n.children.size();
                for (int i = 0; i < size; i++) {
                    final RTree<T>.Node<S> child = n.children.get(i);
                    parent.children.add(child);
                    child.parent = parent;
                }
                n.children.clear();
            }
        }

        /**
         * Distribute leaves from n to two new nodes
         * 
         * @param n the original node
         * @param cache cached BoundingBoxes
         * @param g1 new node 1
         * @param g2 new node 2
         */
        private void distributeLeaves(@NonNull Node<S> n, @NonNull List<BoundingBox> cache, @NonNull Node<S> g1, @NonNull Node<S> g2) {
            // Same process as above; just different types.
            while (!n.data.isEmpty() && g1.data.size() < maxSize - minSize + 1 && g2.data.size() < maxSize - minSize + 1) {
                // Pick next
                long difmax = Long.MIN_VALUE;
                int nmaxIndex = -1;
                long overlap1 = -1;
                long overlap2 = -1;
                final int size = n.data.size();
                for (int i = 0; i < size; i++) {
                    BoundingBox b = cache.get(i);
                    long d1 = expansionNeeded(b, g1.box);
                    long d2 = expansionNeeded(b, g2.box);
                    long dif = Math.abs(d1 - d2);
                    if (dif > difmax) {
                        difmax = dif;
                        nmaxIndex = i;
                    }
                }

                // Distribute Entry
                S nmax = n.data.remove(nmaxIndex);

                // ... to the one with the least expansion
                cache.remove(nmaxIndex);

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
     * Creates an R-Tree. Sets the splitting algorithm to quadratic splitting.
     * 
     * @param minChildren Minimum children in a node. {@code 2 <= minChildren <= maxChildren/2}
     * @param maxChildren Maximum children in a node. Node splits at this number + 1
     */
    public RTree(int minChildren, int maxChildren) {
        if (minChildren < 2 || minChildren > maxChildren / 2) {
            throw new IllegalArgumentException("2 <= minChildren <= maxChildren/2");
        }
        splitter = new QuadraticNodeSplitter<>();

        this.minSize = minChildren;
        this.maxSize = maxChildren;
        root = null;
    }

    /**
     * Return all items in the tree
     * 
     * @param results A collection to store the query results
     */
    public void query(@NonNull Collection<T> results) {
        query(results, root);
    }

    /**
     * Return all items for which the bounding box intersects with box
     * 
     * @param results a Collection holding the results
     * @param box the BoundingBox we are querying
     */
    public void query(@NonNull Collection<T> results, @NonNull BoundingBox box) {
        query(results, box, root, new BoundingBox());
    }

    /**
     * Return all items for which the bounding box intersects with box starting at node
     * 
     * @param results a Collection holding the results
     * @param box the BoundingBox we are querying
     * @param node the Node to start at
     * @param tempBox pre-allocated BoundingBox
     */
    private void query(@NonNull Collection<T> results, @NonNull BoundingBox box, @Nullable Node<T> node, @NonNull BoundingBox tempBox) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            final int size = node.data.size();
            for (int i = 0; i < size; i++) {
                T bo = node.data.get(i);
                if (BoundingBox.intersects(bo.getBounds(tempBox), box)) {
                    results.add(bo);
                }
            }
        } else {
            final int size = node.children.size();
            for (int i = 0; i < size; i++) {
                final RTree<T>.Node<T> child = node.children.get(i);
                if (BoundingBox.intersects(child.box, box)) {
                    query(results, box, child, tempBox);
                }
            }
        }
    }

    /**
     * Return all items in the tree below node
     * 
     * @param results a Collection holding the results
     * @param node the Node to start at
     */
    private void query(@NonNull Collection<T> results, @Nullable Node<T> node) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            final int size = node.data.size();
            for (int i = 0; i < size; i++) {
                results.add(node.data.get(i));
            }
        } else {
            final int size = node.children.size();
            for (int i = 0; i < size; i++) {
                final RTree<T>.Node<T> child = node.children.get(i);
                query(results, child);
            }
        }
    }

    /**
     * Returns one item that intersects the query box, or null if nothing intersects the query box.
     * 
     * @param box the BoundingBox we are querying
     * @return a BoundedObject or null ir none found
     */
    @Nullable
    public BoundedObject queryOne(@NonNull BoundingBox box) {
        return queryOne(box, root, new BoundingBox());
    }

    /**
     * Returns one item that intersects the query box, or null if nothing intersects the query box, starting at node
     * 
     * @param box the BoundingBox we are querying
     * @param node Node to start at
     * @param tempBox pre-allocated BoundingBox
     * @return a BoundedObject or null if none found
     */
    @Nullable
    private BoundedObject queryOne(@NonNull BoundingBox box, @Nullable Node<T> node, @NonNull BoundingBox tempBox) {
        if (node == null) {
            return null;
        }
        if (node.isLeaf()) {
            final int size = node.data.size();
            for (int i = 0; i < size; i++) {
                final T data = node.data.get(i);
                if (BoundingBox.intersects(data.getBounds(tempBox), box)) {
                    return data;
                }
            }
            return null;
        }
        final int size = node.children.size();
        for (int i = 0; i < size; i++) {
            final RTree<T>.Node<T> child = node.children.get(i);
            if (BoundingBox.intersects(child.box, box)) {
                BoundedObject result = queryOne(box, child, tempBox);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Returns items whose Rect contains the specified point, start at the root.
     * 
     * @param results A collection to store the query results.
     * @param px Point X coordinate
     * @param py Point Y coordinate
     */
    @Nullable
    public void query(@NonNull Collection<T> results, int px, int py) {
        query(results, px, py, root, new BoundingBox());
    }

    /**
     * Returns items whose Rect contains the specified point, start at node.
     * 
     * @param results A collection to store the query results.
     * @param px Point X coordinate
     * @param py Point Y coordinate
     * @param node the node to start at
     * @param tempBox pre-allocated BoundingBox
     */
    @Nullable
    private void query(@NonNull Collection<T> results, int px, int py, @Nullable Node<T> node, @NonNull BoundingBox tempBox) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            final int size = node.data.size();
            for (int i = 0; i < size; i++) {
                T bo = node.data.get(i);
                BoundingBox b = bo.getBounds(tempBox);
                if (b.isEmpty()) {
                    if (b.getLeft() == px && b.getTop() == py) {
                        results.add(bo);
                    }
                } else {
                    if (b.contains(px, py)) {
                        results.add(bo);
                    }
                }
            }
            return;
        }
        final int size = node.children.size();
        for (int i = 0; i < size; i++) {
            final RTree<T>.Node<T> child = node.children.get(i);
            if (child.box.contains(px, py)) {
                query(results, px, py, child, tempBox);
            }
        }

    }

    /**
     * Returns one item that intersects the query point, or null if no items intersect that point, starting at the root.
     * 
     * @param px Point X coordinate
     * @param py Point Y coordinate
     * @return a found BoundedObject or null if none found
     */
    @Nullable
    public BoundedObject queryOne(int px, int py) {
        return queryOne(px, py, root, new BoundingBox());
    }

    /**
     * Returns one item that intersects the query point, or null if no items intersect that point, starting at node.
     * 
     * @param px Point X coordinate
     * @param py Point Y coordinate
     * @param node the Node to start at
     * @return a found BoundedObject or null if none found
     */
    @Nullable
    private BoundedObject queryOne(int px, int py, @Nullable Node<T> node, @NonNull BoundingBox tempBox) {
        if (node == null) {
            return null;
        }
        if (node.isLeaf()) {
            final int size = node.data.size();
            for (int i = 0; i < size; i++) {
                final T data = node.data.get(i);
                if (data.getBounds(tempBox).contains(px, py)) {
                    return data;
                }
            }
            return null;
        }
        final int size = node.children.size();
        for (int i = 0; i < size; i++) {
            final RTree<T>.Node<T> child = node.children.get(i);
            if (child.box.contains(px, py)) {
                BoundedObject result = queryOne(px, py, child, tempBox);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Removes the specified object if it is in the tree.
     * 
     * @param o object to remove
     * @return true if successful
     */
    public synchronized boolean remove(@NonNull T o) {
        if (root == null) {
            return false; // empty
        }
        boolean result = false;
        Node<T> n = getLeaf(o, o.getBounds(), root, new BoundingBox());
        if (n != null) {
            result = n.data.remove(o);
            n.computeMBR();
        }
        return result;
    }

    /**
     * Check if an object is in the tree.
     * 
     * @param o object to search for
     * @return true if the object is present in the tree, false otherwise
     */
    public synchronized boolean contains(@NonNull T o) {
        if (root == null) {
            return false; // empty
        }
        return getLeaf(o, o.getBounds(), root, new BoundingBox()) != null;
    }

    /**
     * Inserts object o into the tree. Note that if the value of o.getBounds() changes while in the R-tree, the result
     * is undefined.
     * 
     * @param o object to insert
     * @throws NullPointerException if o is null or no node can be found for storage
     */
    public synchronized void insert(@Nullable T o) {
        if (o == null) {
            throw new NullPointerException("Cannot store null object");
        }
        if (root == null) {
            root = new Node<>(true);
        }
        Node<T> n = chooseLeaf(o.getBounds(), root);
        if (n == null) {
            throw new NullPointerException("No node found for object");
        }
        n.data.add(o);
        n.computeMBR();
        splitter.split(n);
    }

    /**
     * Counts the number of items in the tree.
     * 
     * @return the item count
     */
    public int count() {
        if (root == null) {
            return 0;
        }
        return count(root);
    }

    /**
     * Counts the number of items in the sub-tree starting with n.
     * 
     * @param n the Node to start at
     * @return the item count
     */
    private int count(@NonNull Node<T> n) {
        if (n.isLeaf()) {
            return n.data.size();
        } else {
            int sum = 0;
            final int size = n.children.size();
            for (int i = 0; i < size; i++) {
                sum += count(n.children.get(i));
            }
            return sum;
        }
    }

    /**
     * Choose the appropriate leaf for the BoundingBox
     * 
     * @param box the BoundingBox
     * @param n the node we're starting at
     * @return a leaf Node
     */
    @Nullable
    private Node<T> chooseLeaf(@NonNull BoundingBox box, @NonNull Node<T> n) {
        if (n.isLeaf()) {
            return n;
        } else {
            long maxOverlap = Long.MAX_VALUE;
            Node<T> maxnode = null;
            double maxnodeArea = Double.MAX_VALUE;
            final int size = n.children.size();
            for (int i = 0; i < size; i++) {
                Node<T> child = n.children.get(i);
                long overlap = expansionNeeded(child.box, box);
                if ((overlap < maxOverlap) || (overlap == maxOverlap) && area(child.box) < maxnodeArea) {
                    maxOverlap = overlap;
                    maxnode = child;
                    maxnodeArea = area(maxnode.box);
                }
            }

            if (maxnode == null) {// Not sure how this could occur
                return null;
            }
            return chooseLeaf(box, maxnode);
        }
    }

    /**
     * Get the leaf containing bo starting at Node n
     * 
     * @param bo the BoundedObject
     * @param boBounds the BoundingBox of bo
     * @param n the starting node
     * @param tempBox pre-allocated BoundingBox
     * @return the leaf node or null
     */
    @Nullable
    private Node<T> getLeaf(@NonNull BoundedObject bo, @NonNull BoundingBox boBounds, @NonNull Node<T> n, @NonNull BoundingBox tempBox) {
        if (n.isLeaf()) {
            if (n.data.contains(bo)) {
                return n;
            }
            return null;
        } else {
            Node<T> child = null;
            int size = n.children.size();
            for (int i = 0; i < size; i++) {
                child = n.children.get(i);
                if (child.getBounds(tempBox).intersects(boBounds)) {
                    Node<T> n2 = getLeaf(bo, boBounds, child, tempBox);
                    if (n2 != null) {
                        return n2;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns a measure indicating how much expansion that bounding box one will need to be expanded to fit this.
     * 
     * @param one BoundingBox that may need to be expanded
     * @param two BoundingBox that we want to cover
     * @return the measure value
     */
    private static long expansionNeeded(@NonNull BoundingBox one, @NonNull BoundingBox two) {
        long total = 0;

        int twoL = two.getLeft();
        int oneL = one.getLeft();
        if (twoL < oneL) {
            total += (long) oneL - (long) twoL;
        }
        int twoR = two.getRight();
        int oneR = one.getRight();
        if (twoR > oneR) {
            total += (long) twoR - (long) oneR;
        }
        int twoT = two.getTop();
        int oneT = one.getTop();
        if (twoT < oneT) {
            total += (long) oneT - (long) twoT;
        }
        int twoB = two.getBottom();
        int oneB = one.getBottom();
        if (twoB > oneB) {
            total += (long) twoB - (long) oneB;
        }
        return total;
    }

    /**
     * Get the area of a BoundingBox
     * 
     * @param box the BoundingBox
     * @return width"height
     */
    private static double area(@NonNull BoundingBox box) {
        return (double) box.getWidth() * (double) box.getHeight();
    }

    /**
     * Find an object in the tree without using bounding boxes and print debugging to System.out
     * 
     * @param o the object
     */
    public void debug(@NonNull BoundedObject o) {
        System.out.println("debug: target bounding box " + o.getBounds()); // NOSONAR
        debug(o, root, 0);
    }

    /**
     * Find an object in the tree and print debugging to System.out
     * 
     * @param o the object
     * @param node starting node
     * @param level level the node is on
     * @return true if found
     */
    private boolean debug(@NonNull BoundedObject o, @Nullable Node<T> node, int level) { // NOSONAR
        if (node == null) {
            System.out.println(level + " debug: node is null"); // NOSONAR
            return false;
        }
        BoundingBox box = o.getBounds();
        if (node.isLeaf()) {
            for (int i = 0; i < node.data.size(); i++) {
                if (node.data.get(i) == o) {
                    BoundingBox box2 = node.data.get(i).getBounds();
                    System.out.println(level + " debug: found object parent box " + node.box); // NOSONAR
                    System.out.println(level + " debug: would have matched correctly: 1: " + box2.contains(box.getRight(), box.getTop())); // NOSONAR
                    if (!box2.isEmpty()) {
                        System.out.println(level + " debug: would have matched correctly: 2: " + BoundingBox.intersects(box2, box));// NOSONAR
                    } else {
                        System.out.println(level + " debug: would have matched correctly: 3: " + box.contains(box2.getLeft(), box2.getTop()));// NOSONAR
                    }
                    return true;
                }
            }
        } else {
            for (int i = 0; i < node.children.size(); i++) { // this is what should normally happen
                if (BoundingBox.intersects(node.children.get(i).box, box) && debug(o, node.children.get(i), level + 1)) {
                    System.out.println(level + " debug: target box intersects with node box");// NOSONAR
                    return true;
                }
            }
            for (int i = 0; i < node.children.size(); i++) {
                if (!BoundingBox.intersects(node.children.get(i).box, o.getBounds())) {
                    if (node.children.get(i).box.contains(box.getLeft(), box.getBottom())) {
                        if (debug(o, node.children.get(i), level + 1)) {
                            System.out.println(level + " debug: target point contained in node box, didn't intersect with " + node.children.get(i).box);// NOSONAR
                            return true;
                        }
                    } else {
                        if (debug(o, node.children.get(i), level + 1)) {
                            System.out.println(level + " debug: target box doesn't intersect with node box " + node.children.get(i).box);// NOSONAR
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}