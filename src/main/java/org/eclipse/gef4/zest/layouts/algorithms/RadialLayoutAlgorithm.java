/*******************************************************************************
 * Copyright (c) 2005-2010 The Chisel Group and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group - initial API and implementation
 *               Mateusz Matela 
 *               Ian Bull
 *               Raden Solutions - sector based placement and ring radii
 ******************************************************************************/
package org.eclipse.gef4.zest.layouts.algorithms;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef4.zest.layouts.LayoutAlgorithm;
import org.eclipse.gef4.zest.layouts.LayoutStyles;
import org.eclipse.gef4.zest.layouts.algorithms.TreeLayoutObserver.TreeNode;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentRectangle;
import org.eclipse.gef4.zest.layouts.interfaces.ConnectionLayout;
import org.eclipse.gef4.zest.layouts.interfaces.EntityLayout;
import org.eclipse.gef4.zest.layouts.interfaces.LayoutContext;
import org.eclipse.gef4.zest.layouts.interfaces.NodeLayout;

/**
 * Radial tree layout. Single tree root is placed in the center, several roots
 * share the first ring; each next tree level is placed on the next ring. A
 * node's angle is the middle of the angular sector covered by its subtree's
 * leaves, so subtrees stay within their sectors and tree connections do not
 * cross. Ring radius is large enough to keep the closest pair of neighbours on
 * the ring one node size apart. Result is scaled into layout bounds.
 * <p>
 * When any connection is directed, tree follows connection direction. When
 * all connections are undirected, tree of each connected part of the graph is
 * built by breadth-first search from its center - the node with the smallest
 * number of hops to the farthest node.
 * 
 * @author Casey Best
 * @author Rob Lintern
 */
public class RadialLayoutAlgorithm implements LayoutAlgorithm {

	private static final double MAX_DEGREES = Math.PI * 2;

	// Distances in units of node size
	private static final double NODE_SPACING = 1.0;
	private static final double RING_SPACING = 1.5;

	private double startDegree = 0;
	private double endDegree = MAX_DEGREES;

	private LayoutContext context;
	private boolean resize = false;
	private NodeLayout root = null;

	/**
	 * @deprecated Since Zest 2.0, use {@link #RadialLayoutAlgorithm()}.
	 */
	public RadialLayoutAlgorithm(int style) {
		this();
		setResizing(style != LayoutStyles.NO_LAYOUT_NODE_RESIZING);
	}

	public RadialLayoutAlgorithm() {
	}

	public void applyLayout(boolean clean) {
		if (!clean)
			return;

		computeRadialPositions(buildTree());

		EntityLayout[] entities = context.getEntities();
		if (resize)
			AlgorithmHelper.maximizeSizes(entities);
		DisplayIndependentRectangle bounds = context.getBounds();
		int insets = 4;
		bounds.x += insets;
		bounds.y += insets;
		bounds.width -= 2 * insets;
		bounds.height -= 2 * insets;
		AlgorithmHelper.fitWithinBounds(entities, bounds, resize);
	}

	/**
	 * Build tree (forest) to lay out.
	 * 
	 * @return tree roots
	 */
	private List<Item> buildTree() {
		ConnectionLayout[] connections = context.getConnections();
		for (ConnectionLayout c : connections) {
			if (c.isDirected())
				return buildDirectedTree();
		}

		UndirectedGraph graph = new UndirectedGraph(context);
		List<Item> roots = new ArrayList<Item>();
		boolean[] visited = new boolean[graph.size()];
		int[] distance = new int[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			if (visited[i])
				continue;
			List<Integer> component = graph.component(i, visited);
			Integer fixedRoot = (root != null) ? graph.index(root) : null;
			int r = ((fixedRoot != null) && component.contains(fixedRoot)) ? fixedRoot
					: graph.centers(component, distance).get(0);
			roots.add(graph.spanningTree(r));
		}
		return roots;
	}

	/**
	 * Build tree that follows connection direction.
	 */
	private List<Item> buildDirectedTree() {
		TreeLayoutObserver treeObserver = new TreeLayoutObserver(context, null);
		try {
			List<Item> roots = new ArrayList<Item>();
			for (Object r : treeObserver.getSuperRoot().getChildren())
				roots.add(new Item((TreeNode) r));
			return roots;
		} finally {
			treeObserver.stop();
		}
	}

	/**
	 * Place tree nodes on rings around (0, 0), in units of node size.
	 */
	private void computeRadialPositions(List<Item> roots) {
		if (roots.isEmpty())
			return;

		int totalLeaves = 0;
		for (Item r : roots)
			totalLeaves += r.leaves;

		double range = Math.abs(endDegree - startDegree);
		boolean fullCircle = range >= MAX_DEGREES;
		List<List<RingNode>> rings = new ArrayList<List<RingNode>>();
		int firstRing = (roots.size() > 1) ? 1 : 0;
		int leafOffset = 0;
		for (Item r : roots) {
			collectRingNodes(r, firstRing, leafOffset, totalLeaves, range,
					rings);
			leafOffset += r.leaves;
		}

		double radius = 0;
		for (int i = 0; i < rings.size(); i++) {
			List<RingNode> ring = rings.get(i);
			int count = ring.size();
			if (i > 0) {
				double minRadius = 0;
				if (count > 1) {
					double minGap = fullCircle ? ring.get(0).angle
							+ MAX_DEGREES - ring.get(count - 1).angle
							: Double.MAX_VALUE;
					for (int j = 1; j < count; j++)
						minGap = Math.min(minGap, ring.get(j).angle
								- ring.get(j - 1).angle);
					// Zero gap is only possible with empty angular range
					if (minGap > 0)
						minRadius = NODE_SPACING
								/ (2 * Math.sin(Math.min(minGap, Math.PI) / 2));
				}
				radius = Math.max(radius + RING_SPACING, minRadius);
			}
			for (RingNode n : ring)
				n.node.setLocation(radius * Math.cos(n.angle),
						radius * Math.sin(n.angle));
		}
	}

	/**
	 * Add node and its subtree to rings. Nodes are added to each ring in
	 * ascending angle order.
	 */
	private void collectRingNodes(Item item, int ringIndex, int leafOffset,
			int totalLeaves, double range, List<List<RingNode>> rings) {
		while (rings.size() <= ringIndex)
			rings.add(new ArrayList<RingNode>());
		rings.get(ringIndex).add(
				new RingNode(item.node, startDegree + range
						* (leafOffset + item.leaves / 2.0) / totalLeaves));

		for (Item child : item.children) {
			collectRingNodes(child, ringIndex + 1, leafOffset, totalLeaves,
					range, rings);
			leafOffset += child.leaves;
		}
	}

	public void setLayoutContext(LayoutContext context) {
		this.context = context;
	}

	/**
	 * Set node to place in the center when connections are undirected. Other
	 * connected parts of the graph are centered on their own centers.
	 * 
	 * @param root
	 *            root node or null to use center of the graph
	 */
	void setRoot(NodeLayout root) {
		this.root = root;
	}

	/**
	 * Set range the radial layout will use when applyLayout is called. Both
	 * values must be in radians.
	 * 
	 * @param startDegree
	 * @param endDegree
	 */
	public void setRangeToLayout(double startDegree, double endDegree) {
		this.startDegree = startDegree;
		this.endDegree = endDegree;
	}

	/**
	 * 
	 * @return true if this algorithm is set to resize elements
	 */
	public boolean isResizing() {
		return resize;
	}

	/**
	 * 
	 * @param resizing
	 *            true if this algorithm should resize elements (default is
	 *            false)
	 */
	public void setResizing(boolean resizing) {
		resize = resizing;
	}

	/**
	 * Tree node with number of leaves in its subtree
	 */
	private static class Item {
		final NodeLayout node;
		final List<Item> children = new ArrayList<Item>();
		int leaves;

		Item(NodeLayout node) {
			this.node = node;
		}

		Item(TreeNode treeNode) {
			this(treeNode.getNode());
			for (Object c : treeNode.getChildren())
				children.add(new Item((TreeNode) c));
			countLeaves();
		}

		void countLeaves() {
			leaves = 0;
			for (Item c : children)
				leaves += c.leaves;
			if (leaves == 0)
				leaves = 1;
		}
	}

	/**
	 * Undirected graph of layout context nodes
	 */
	static class UndirectedGraph {
		final NodeLayout[] nodes;
		final int[][] neighbours;
		private final Map<NodeLayout, Integer> indexes = new IdentityHashMap<NodeLayout, Integer>();

		UndirectedGraph(LayoutContext context) {
			nodes = context.getNodes();
			for (int i = 0; i < nodes.length; i++)
				indexes.put(nodes[i], i);
			List<List<Integer>> lists = new ArrayList<List<Integer>>();
			for (int i = 0; i < nodes.length; i++)
				lists.add(new ArrayList<Integer>());
			for (ConnectionLayout c : context.getConnections()) {
				Integer s = indexes.get(c.getSource());
				Integer t = indexes.get(c.getTarget());
				if ((s == null) || (t == null) || s.equals(t)
						|| lists.get(s).contains(t))
					continue;
				lists.get(s).add(t);
				lists.get(t).add(s);
			}
			neighbours = new int[nodes.length][];
			for (int i = 0; i < nodes.length; i++) {
				List<Integer> l = lists.get(i);
				neighbours[i] = new int[l.size()];
				for (int j = 0; j < l.size(); j++)
					neighbours[i][j] = l.get(j);
			}
		}

		int size() {
			return nodes.length;
		}

		Integer index(NodeLayout node) {
			return indexes.get(node);
		}

		/**
		 * Collect connected part of the graph containing given node, in
		 * breadth-first order, and mark its nodes as visited.
		 */
		List<Integer> component(int start, boolean[] visited) {
			List<Integer> component = new ArrayList<Integer>();
			visited[start] = true;
			component.add(start);
			for (int k = 0; k < component.size(); k++) {
				for (int n : neighbours[component.get(k)]) {
					if (!visited[n]) {
						visited[n] = true;
						component.add(n);
					}
				}
			}
			return component;
		}

		/**
		 * Get nodes of a connected part ordered from the most central one:
		 * by number of hops to the farthest node, then by number of
		 * neighbours (more first), then by order in the context.
		 */
		List<Integer> centers(List<Integer> component, int[] distance) {
			final int[] eccentricity = new int[nodes.length];
			for (int v : component) {
				for (int u : component)
					distance[u] = -1;
				distance[v] = 0;
				List<Integer> queue = new ArrayList<Integer>();
				queue.add(v);
				int max = 0;
				for (int k = 0; k < queue.size(); k++) {
					int u = queue.get(k);
					max = distance[u];
					for (int n : neighbours[u]) {
						if (distance[n] < 0) {
							distance[n] = distance[u] + 1;
							queue.add(n);
						}
					}
				}
				eccentricity[v] = max;
			}
			List<Integer> result = new ArrayList<Integer>(component);
			result.sort((a, b) -> {
				if (eccentricity[a] != eccentricity[b])
					return Integer.compare(eccentricity[a], eccentricity[b]);
				if (neighbours[a].length != neighbours[b].length)
					return Integer.compare(neighbours[b].length,
							neighbours[a].length);
				return Integer.compare(a, b);
			});
			return result;
		}

		/**
		 * Build breadth-first spanning tree from given node.
		 */
		Item spanningTree(int root) {
			Item[] items = new Item[nodes.length];
			List<Integer> queue = new ArrayList<Integer>();
			items[root] = new Item(nodes[root]);
			queue.add(root);
			for (int k = 0; k < queue.size(); k++) {
				int u = queue.get(k);
				for (int n : neighbours[u]) {
					if (items[n] == null) {
						items[n] = new Item(nodes[n]);
						items[u].children.add(items[n]);
						queue.add(n);
					}
				}
			}
			for (int k = queue.size() - 1; k >= 0; k--)
				items[queue.get(k)].countLeaves();
			return items[root];
		}
	}

	/**
	 * Node placed on a ring
	 */
	private static class RingNode {
		final NodeLayout node;
		final double angle;

		RingNode(NodeLayout node, double angle) {
			this.node = node;
			this.angle = angle;
		}
	}
}
