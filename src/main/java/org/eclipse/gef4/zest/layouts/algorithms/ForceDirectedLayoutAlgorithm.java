/*******************************************************************************
 * Copyright (c) 2026 Raden Solutions. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.gef4.zest.layouts.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.gef4.zest.layouts.LayoutAlgorithm;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentDimension;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentPoint;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentRectangle;
import org.eclipse.gef4.zest.layouts.interfaces.ConnectionLayout;
import org.eclipse.gef4.zest.layouts.interfaces.EntityLayout;
import org.eclipse.gef4.zest.layouts.interfaces.LayoutContext;
import org.eclipse.gef4.zest.layouts.interfaces.NodeLayout;

/**
 * Force-directed (Fruchterman-Reingold) layout that does not add connection
 * crossings. Simulation starts from {@link RadialLayoutAlgorithm radial
 * layout}, where tree connections rarely cross, instead of random placement,
 * so result is the same each time. With undirected connections, simulation is
 * repeated from radial layouts centered on several most central nodes, as many
 * as graph size allows, and the result with the fewest crossings is used. Nodes are moved one at a time, and a move
 * is rejected if connections of the moved node would cross more other
 * connections than before the move. Distances are measured in units of node
 * size, so wide nodes are kept further apart horizontally than vertically.
 * Nodes repel only nodes within limited range, so on large graphs they are not
 * pushed to the edges, and weak gravity keeps disconnected parts together.
 * Result is scaled into layout bounds.
 */
public class ForceDirectedLayoutAlgorithm implements LayoutAlgorithm {

	private static final int MAX_ITERATIONS = 200;
	// Limit of counted elementary operations for whole layout run. Simulation
	// cools down by this counter, not by time, so result does not depend on
	// machine speed
	private static final double WORK_LIMIT = 1.5e8;
	private static final double START_WORK_LIMIT = 1.3e8;
	private static final int MAX_STARTS = 8;
	private static final int MAX_LINK_GRID_SIDE = 64;
	private static final int NODE_SPACING = 10;
	private static final int INSETS = 4;
	private static final double GRAVITY = 0.5;
	private static final double REPULSION_RANGE = 5; // in units of ideal connection length

	private LayoutContext context;
	private RadialLayoutAlgorithm initialLayout = new RadialLayoutAlgorithm();

	private double[] x;
	private double[] y;
	private int[][] links;
	private int[][] nodeLinks;
	private long work;

	// Grid of connections: each connection is in all cells covered by its
	// bounding box extended by maximal distance nodes can move before rebuild
	private int linkGridSide;
	private double linkGridX, linkGridY, linkGridStepX, linkGridStepY;
	private int[] linkCellHead;
	private int[] linkCellNext = new int[0];
	private int[] linkCellItem = new int[0];
	private int[] linkStamp;
	private int currentStamp;

	public void setLayoutContext(LayoutContext context) {
		this.context = context;
		initialLayout.setLayoutContext(context);
	}

	public void applyLayout(boolean clean) {
		if (!clean)
			return;

		EntityLayout[] entities = context.getEntities();
		int count = entities.length;
		double unitWidth = 0, unitHeight = 0;
		for (EntityLayout e : entities) {
			DisplayIndependentDimension size = e.getSize();
			unitWidth = Math.max(unitWidth, size.width);
			unitHeight = Math.max(unitHeight, size.height);
		}
		DisplayIndependentRectangle bounds = context.getBounds();
		double minX = (bounds.x + INSETS + unitWidth / 2)
				/ (unitWidth + NODE_SPACING);
		double maxX = (bounds.x + bounds.width - INSETS - unitWidth / 2)
				/ (unitWidth + NODE_SPACING);
		double minY = (bounds.y + INSETS + unitHeight / 2)
				/ (unitHeight + NODE_SPACING);
		double maxY = (bounds.y + bounds.height - INSETS - unitHeight / 2)
				/ (unitHeight + NODE_SPACING);
		if ((count < 2) || (maxX <= minX) || (maxY <= minY)) {
			initialLayout.setRoot(null);
			initialLayout.applyLayout(true);
			return;
		}
		unitWidth += NODE_SPACING;
		unitHeight += NODE_SPACING;

		Map<Object, Integer> index = new IdentityHashMap<Object, Integer>();
		for (int i = 0; i < count; i++)
			index.put(entities[i], i);
		boolean directed = false;
		List<int[]> linkList = new ArrayList<int[]>();
		for (ConnectionLayout c : context.getConnections()) {
			if (c.isDirected())
				directed = true;
			Integer source = index.get(c.getSource());
			Integer target = index.get(c.getTarget());
			if ((source != null) && (target != null) && !source.equals(target))
				linkList.add(new int[] { source, target });
		}
		links = linkList.toArray(new int[linkList.size()][]);
		int[] linkCount = new int[count];
		for (int[] l : links) {
			linkCount[l[0]]++;
			linkCount[l[1]]++;
		}
		nodeLinks = new int[count][];
		for (int i = 0; i < count; i++)
			nodeLinks[i] = new int[linkCount[i]];
		for (int l = 0; l < links.length; l++) {
			nodeLinks[links[l][0]][--linkCount[links[l][0]]] = l;
			nodeLinks[links[l][1]][--linkCount[links[l][1]]] = l;
		}

		List<NodeLayout> startRoots = new ArrayList<NodeLayout>();
		if (!directed) {
			double iterationWork = (double) links.length * links.length + count;
			int starts = (int) Math.max(1, Math.min(MAX_STARTS, START_WORK_LIMIT
					/ (iterationWork * MAX_ITERATIONS)));
			startRoots.addAll(centralNodes(starts));
		}
		if (startRoots.isEmpty())
			startRoots.add(null);

		linkStamp = new int[links.length];
		x = new double[count];
		y = new double[count];
		double[] bestX = null, bestY = null;
		int bestCrossings = Integer.MAX_VALUE;
		for (NodeLayout root : startRoots) {
			initialLayout.setRoot(root);
			initialLayout.applyLayout(true);
			for (int i = 0; i < count; i++) {
				DisplayIndependentPoint location = entities[i].getLocation();
				x[i] = location.x / unitWidth;
				y[i] = location.y / unitHeight;
			}
			simulate(entities, WORK_LIMIT / startRoots.size(), minX, maxX,
					minY, maxY);
			int crossings = (startRoots.size() > 1) ? countAllCrossings() : 0;
			if (crossings < bestCrossings) {
				bestCrossings = crossings;
				bestX = x.clone();
				bestY = y.clone();
				if (crossings == 0)
					break;
			}
		}
		initialLayout.setRoot(null);

		for (int i = 0; i < count; i++) {
			if (entities[i].isMovable())
				entities[i].setLocation(bestX[i] * unitWidth, bestY[i]
						* unitHeight);
		}

		bounds.x += INSETS;
		bounds.y += INSETS;
		bounds.width -= 2 * INSETS;
		bounds.height -= 2 * INSETS;
		AlgorithmHelper.fitCentersWithinBounds(entities, bounds);
	}

	/**
	 * Get most central nodes of the largest connected part of the graph.
	 */
	private List<NodeLayout> centralNodes(int maxCount) {
		RadialLayoutAlgorithm.UndirectedGraph graph = new RadialLayoutAlgorithm.UndirectedGraph(
				context);
		boolean[] visited = new boolean[graph.size()];
		List<Integer> largest = new ArrayList<Integer>();
		for (int i = 0; i < graph.size(); i++) {
			if (visited[i])
				continue;
			List<Integer> component = graph.component(i, visited);
			if (component.size() > largest.size())
				largest = component;
		}
		List<NodeLayout> result = new ArrayList<NodeLayout>();
		for (int i : graph.centers(largest, new int[graph.size()])) {
			if (result.size() == maxCount)
				break;
			result.add(graph.nodes[i]);
		}
		return result;
	}

	/**
	 * Run force simulation on current positions.
	 */
	private void simulate(EntityLayout[] entities, double workLimit,
			double minX, double maxX, double minY, double maxY) {
		int count = entities.length;
		double k = Math.sqrt((maxX - minX) * (maxY - minY) / count);
		double centerX = (minX + maxX) / 2;
		double centerY = (minY + maxY) / 2;
		double initialTemperature = Math.max(maxX - minX, maxY - minY) / 10;
		double range = REPULSION_RANGE * k;
		int gridColumns = (int) ((maxX - minX) / range) + 1;
		int gridRows = (int) ((maxY - minY) / range) + 1;
		int[] cellHead = new int[gridColumns * gridRows];
		int[] nextInCell = new int[count];
		work = 0;
		for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
			double progress = Math.max((double) iteration / MAX_ITERATIONS,
					work / workLimit);
			if (progress >= 1)
				break;
			double temperature = initialTemperature * (1 - progress);
			buildLinkGrid(temperature);

			// Grid of cells with side equal to repulsion range; nodes repel
			// only nodes in the same or adjacent cells
			for (int c = 0; c < cellHead.length; c++)
				cellHead[c] = -1;
			for (int i = 0; i < count; i++) {
				int c = cellRow(y[i], minY, range, gridRows) * gridColumns
						+ cellColumn(x[i], minX, range, gridColumns);
				nextInCell[i] = cellHead[c];
				cellHead[c] = i;
			}

			for (int i = 0; (i < count) && (work < workLimit); i++) {
				if (!entities[i].isMovable())
					continue;

				double fx = -GRAVITY * k * (x[i] - centerX);
				double fy = -GRAVITY * k * (y[i] - centerY);
				int column = cellColumn(x[i], minX, range, gridColumns);
				int row = cellRow(y[i], minY, range, gridRows);
				for (int r = Math.max(0, row - 1); r <= Math.min(gridRows - 1,
						row + 1); r++) {
					for (int c = Math.max(0, column - 1); c <= Math.min(
							gridColumns - 1, column + 1); c++) {
						for (int j = cellHead[r * gridColumns + c]; j != -1; j = nextInCell[j]) {
							work++;
							if (j == i)
								continue;
							double dx = x[i] - x[j];
							double dy = y[i] - y[j];
							double distance = Math.sqrt(dx * dx + dy * dy);
							if (distance >= range)
								continue;
							if (distance < 0.01) {
								// Separate coinciding nodes in a fixed direction
								// depending on their order
								dx = (i < j) ? -0.01 : 0.01;
								dy = 0;
								distance = 0.01;
							}
							double force = k * k / distance;
							fx += dx / distance * force;
							fy += dy / distance * force;
						}
					}
				}
				for (int l : nodeLinks[i]) {
					int j = (links[l][0] == i) ? links[l][1] : links[l][0];
					double dx = x[i] - x[j];
					double dy = y[i] - y[j];
					double distance = Math.sqrt(dx * dx + dy * dy);
					fx -= dx * distance / k;
					fy -= dy * distance / k;
				}

				double displacement = Math.sqrt(fx * fx + fy * fy);
				if (displacement == 0)
					continue;
				double step = Math.min(displacement, temperature);
				double newX = x[i] + fx / displacement * step;
				double newY = y[i] + fy / displacement * step;
				if ((nodeLinks[i].length == 0)
						|| !addsCrossings(i, newX, newY)) {
					x[i] = newX;
					y[i] = newY;
				}
			}
		}
	}

	private static int cellColumn(double px, double minX, double cellSize,
			int columns) {
		return Math.min(columns - 1, Math.max(0, (int) ((px - minX) / cellSize)));
	}

	private static int cellRow(double py, double minY, double cellSize, int rows) {
		return Math.min(rows - 1, Math.max(0, (int) ((py - minY) / cellSize)));
	}

	/**
	 * Check if moving node to given position makes its connections cross more
	 * other connections.
	 */
	private boolean addsCrossings(int node, double newX, double newY) {
		int current = countCrossings(node, x[node], y[node], Integer.MAX_VALUE);
		return countCrossings(node, newX, newY, current) > current;
	}

	/**
	 * Count all pairs of crossing connections.
	 */
	private int countAllCrossings() {
		buildLinkGrid(0);
		int crossings = 0;
		for (int i = 0; i < x.length; i++)
			crossings += countCrossings(i, x[i], y[i], Integer.MAX_VALUE);
		return crossings / 4; // each pair is counted from each of its four nodes
	}

	/**
	 * Build grid of connections for current node positions.
	 * 
	 * @param margin
	 *            maximal distance nodes can move while grid is in use
	 */
	private void buildLinkGrid(double margin) {
		double left = Double.MAX_VALUE, right = -Double.MAX_VALUE;
		double top = Double.MAX_VALUE, bottom = -Double.MAX_VALUE;
		for (int i = 0; i < x.length; i++) {
			left = Math.min(left, x[i]);
			right = Math.max(right, x[i]);
			top = Math.min(top, y[i]);
			bottom = Math.max(bottom, y[i]);
		}
		linkGridSide = Math.max(1, Math.min(MAX_LINK_GRID_SIDE,
				(int) Math.sqrt(links.length)));
		linkGridX = left;
		linkGridY = top;
		linkGridStepX = Math.max(right - left, 1e-9) / linkGridSide;
		linkGridStepY = Math.max(bottom - top, 1e-9) / linkGridSide;
		int cells = linkGridSide * linkGridSide;
		if ((linkCellHead == null) || (linkCellHead.length != cells))
			linkCellHead = new int[cells];
		Arrays.fill(linkCellHead, -1);

		int used = 0;
		for (int l = 0; l < links.length; l++) {
			double ax = x[links[l][0]], ay = y[links[l][0]];
			double bx = x[links[l][1]], by = y[links[l][1]];
			int firstColumn = linkGridColumn(Math.min(ax, bx) - margin);
			int lastColumn = linkGridColumn(Math.max(ax, bx) + margin);
			int firstRow = linkGridRow(Math.min(ay, by) - margin);
			int lastRow = linkGridRow(Math.max(ay, by) + margin);
			int needed = used + (lastColumn - firstColumn + 1)
					* (lastRow - firstRow + 1);
			if (needed > linkCellItem.length) {
				linkCellItem = Arrays.copyOf(linkCellItem, needed * 2);
				linkCellNext = Arrays.copyOf(linkCellNext, needed * 2);
			}
			for (int r = firstRow; r <= lastRow; r++) {
				for (int c = firstColumn; c <= lastColumn; c++) {
					int cell = r * linkGridSide + c;
					linkCellItem[used] = l;
					linkCellNext[used] = linkCellHead[cell];
					linkCellHead[cell] = used++;
				}
			}
		}
		work += used;
	}

	private int linkGridColumn(double px) {
		return Math.min(linkGridSide - 1,
				Math.max(0, (int) ((px - linkGridX) / linkGridStepX)));
	}

	private int linkGridRow(double py) {
		return Math.min(linkGridSide - 1,
				Math.max(0, (int) ((py - linkGridY) / linkGridStepY)));
	}

	/**
	 * Count crossings between connections of given node, assuming that node is
	 * placed at given position, and all other connections. Connections are
	 * straight lines between node centers; connections sharing a node do not
	 * count as crossing. Counting stops after exceeding given limit.
	 */
	private int countCrossings(int node, double nodeX, double nodeY, int limit) {
		int crossings = 0;
		for (int l : nodeLinks[node]) {
			int other = (links[l][0] == node) ? links[l][1] : links[l][0];
			double ax = nodeX, ay = nodeY, bx = x[other], by = y[other];
			int firstColumn = linkGridColumn(Math.min(ax, bx));
			int lastColumn = linkGridColumn(Math.max(ax, bx));
			int firstRow = linkGridRow(Math.min(ay, by));
			int lastRow = linkGridRow(Math.max(ay, by));
			if ((lastColumn - firstColumn + 1) * (lastRow - firstRow + 1) * 4 > linkGridSide
					* linkGridSide) {
				// Long connection covers too many cells for the grid to help
				for (int candidate = 0; candidate < links.length; candidate++) {
					work++;
					if (crosses(node, other, ax, ay, bx, by, links[candidate])
							&& (++crossings > limit))
						return crossings;
				}
				continue;
			}
			currentStamp++;
			for (int r = firstRow; r <= lastRow; r++) {
				for (int c = firstColumn; c <= lastColumn; c++) {
					for (int e = linkCellHead[r * linkGridSide + c]; e != -1; e = linkCellNext[e]) {
						int candidate = linkCellItem[e];
						work++;
						if (linkStamp[candidate] == currentStamp)
							continue;
						linkStamp[candidate] = currentStamp;
						if (crosses(node, other, ax, ay, bx, by, links[candidate])
								&& (++crossings > limit))
							return crossings;
					}
				}
			}
		}
		return crossings;
	}

	/**
	 * Check if connection between given node at (ax, ay) and other node at
	 * (bx, by) crosses given connection that does not share a node with it.
	 */
	private boolean crosses(int node, int other, double ax, double ay,
			double bx, double by, int[] link) {
		if ((link[0] == node) || (link[1] == node) || (link[0] == other)
				|| (link[1] == other))
			return false;
		double cx = x[link[0]], cy = y[link[0]], dx = x[link[1]], dy = y[link[1]];
		if ((Math.max(cx, dx) < Math.min(ax, bx))
				|| (Math.min(cx, dx) > Math.max(ax, bx))
				|| (Math.max(cy, dy) < Math.min(ay, by))
				|| (Math.min(cy, dy) > Math.max(ay, by)))
			return false;
		return (orientation(ax, ay, bx, by, cx, cy)
				* orientation(ax, ay, bx, by, dx, dy) < 0)
				&& (orientation(cx, cy, dx, dy, ax, ay)
						* orientation(cx, cy, dx, dy, bx, by) < 0);
	}

	/**
	 * Sign of cross product (b - a) x (c - a): which side of line a-b point c
	 * is on.
	 */
	private static double orientation(double ax, double ay, double bx,
			double by, double cx, double cy) {
		return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
	}
}
