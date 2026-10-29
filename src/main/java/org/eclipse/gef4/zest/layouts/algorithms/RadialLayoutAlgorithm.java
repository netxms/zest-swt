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
import java.util.List;

import org.eclipse.gef4.zest.layouts.LayoutAlgorithm;
import org.eclipse.gef4.zest.layouts.LayoutStyles;
import org.eclipse.gef4.zest.layouts.algorithms.TreeLayoutObserver.TreeNode;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentRectangle;
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

		TreeLayoutObserver treeObserver = new TreeLayoutObserver(context, null);
		try {
			computeRadialPositions(treeObserver.getSuperRoot());
		} finally {
			treeObserver.stop();
		}

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
	 * Place tree nodes on rings around (0, 0), in units of node size.
	 */
	private void computeRadialPositions(TreeNode superRoot) {
		List<?> roots = superRoot.getChildren();
		if (roots.isEmpty())
			return;

		double range = Math.abs(endDegree - startDegree);
		boolean fullCircle = range >= MAX_DEGREES;
		List<List<RingNode>> rings = new ArrayList<List<RingNode>>();
		int firstRing = (roots.size() > 1) ? 1 : 0;
		int leafOffset = 0;
		for (Object r : roots) {
			TreeNode root = (TreeNode) r;
			collectRingNodes(root, firstRing, leafOffset,
					superRoot.getNumOfLeaves(), range, rings);
			leafOffset += root.getNumOfLeaves();
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
	private void collectRingNodes(TreeNode treeNode, int ringIndex,
			int leafOffset, int totalLeaves, double range,
			List<List<RingNode>> rings) {
		while (rings.size() <= ringIndex)
			rings.add(new ArrayList<RingNode>());
		rings.get(ringIndex).add(
				new RingNode(treeNode.getNode(), startDegree + range
						* (leafOffset + treeNode.getNumOfLeaves() / 2.0)
						/ totalLeaves));

		for (Object c : treeNode.getChildren()) {
			TreeNode child = (TreeNode) c;
			collectRingNodes(child, ringIndex + 1, leafOffset, totalLeaves,
					range, rings);
			leafOffset += child.getNumOfLeaves();
		}
	}

	public void setLayoutContext(LayoutContext context) {
		this.context = context;
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
