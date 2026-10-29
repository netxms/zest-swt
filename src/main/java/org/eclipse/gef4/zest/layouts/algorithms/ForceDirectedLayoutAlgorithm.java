/*******************************************************************************
 * Copyright (c) 2026 Raden Solutions. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.gef4.zest.layouts.algorithms;

import java.util.ArrayList;
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

/**
 * Force-directed (Fruchterman-Reingold) layout that does not add connection
 * crossings. Simulation starts from {@link RadialLayoutAlgorithm radial
 * layout}, where tree connections do not cross, instead of random placement,
 * so result is the same each time. Nodes are moved one at a time, and a move
 * is rejected if connections of the moved node would cross more other
 * connections than before the move. Distances are measured in units of node
 * size, so wide nodes are kept further apart horizontally than vertically.
 * Nodes repel only nodes within limited range, so on large graphs they are not
 * pushed to the edges, and weak gravity keeps disconnected parts together.
 * Result is scaled into layout bounds.
 */
public class ForceDirectedLayoutAlgorithm implements LayoutAlgorithm {

	private static final int ITERATIONS = 200;
	private static final long TIMEOUT = 5000; // milliseconds
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

	public void setLayoutContext(LayoutContext context) {
		this.context = context;
		initialLayout.setLayoutContext(context);
	}

	public void applyLayout(boolean clean) {
		if (!clean)
			return;

		initialLayout.applyLayout(true);

		EntityLayout[] entities = context.getEntities();
		int count = entities.length;
		if (count < 2)
			return;

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
		if ((maxX <= minX) || (maxY <= minY))
			return;
		unitWidth += NODE_SPACING;
		unitHeight += NODE_SPACING;

		x = new double[count];
		y = new double[count];
		Map<Object, Integer> index = new IdentityHashMap<Object, Integer>();
		for (int i = 0; i < count; i++) {
			DisplayIndependentPoint location = entities[i].getLocation();
			x[i] = location.x / unitWidth;
			y[i] = location.y / unitHeight;
			index.put(entities[i], i);
		}

		List<int[]> linkList = new ArrayList<int[]>();
		for (ConnectionLayout c : context.getConnections()) {
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

		double k = Math.sqrt((maxX - minX) * (maxY - minY) / count);
		double centerX = (minX + maxX) / 2;
		double centerY = (minY + maxY) / 2;
		double initialTemperature = Math.max(maxX - minX, maxY - minY) / 10;
		double range = REPULSION_RANGE * k;
		int gridColumns = (int) ((maxX - minX) / range) + 1;
		int gridRows = (int) ((maxY - minY) / range) + 1;
		int[] cellHead = new int[gridColumns * gridRows];
		int[] nextInCell = new int[count];
		long startTime = System.currentTimeMillis();
		for (int iteration = 0; iteration < ITERATIONS; iteration++) {
			double progress = Math.max((double) iteration / ITERATIONS,
					(double) (System.currentTimeMillis() - startTime) / TIMEOUT);
			if (progress >= 1)
				break;
			double temperature = initialTemperature * (1 - progress);

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

			for (int i = 0; i < count; i++) {
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
						|| (countCrossings(i, newX, newY) <= countCrossings(i,
								x[i], y[i]))) {
					x[i] = newX;
					y[i] = newY;
				}
			}
		}

		for (int i = 0; i < count; i++) {
			if (entities[i].isMovable())
				entities[i].setLocation(x[i] * unitWidth, y[i] * unitHeight);
		}

		bounds.x += INSETS;
		bounds.y += INSETS;
		bounds.width -= 2 * INSETS;
		bounds.height -= 2 * INSETS;
		AlgorithmHelper.fitWithinBounds(entities, bounds, false);
	}

	private static int cellColumn(double px, double minX, double cellSize,
			int columns) {
		return Math.min(columns - 1, Math.max(0, (int) ((px - minX) / cellSize)));
	}

	private static int cellRow(double py, double minY, double cellSize, int rows) {
		return Math.min(rows - 1, Math.max(0, (int) ((py - minY) / cellSize)));
	}

	/**
	 * Count crossings between connections of given node, assuming that node is
	 * placed at given position, and all other connections. Connections are
	 * straight lines between node centers; connections sharing a node do not
	 * count as crossing.
	 */
	private int countCrossings(int node, double nodeX, double nodeY) {
		int crossings = 0;
		for (int l : nodeLinks[node]) {
			int other = (links[l][0] == node) ? links[l][1] : links[l][0];
			double ax = nodeX, ay = nodeY, bx = x[other], by = y[other];
			double minX = Math.min(ax, bx), maxX = Math.max(ax, bx);
			double minY = Math.min(ay, by), maxY = Math.max(ay, by);
			for (int[] link : links) {
				if ((link[0] == node) || (link[1] == node)
						|| (link[0] == other) || (link[1] == other))
					continue;
				double cx = x[link[0]], cy = y[link[0]], dx = x[link[1]], dy = y[link[1]];
				if ((Math.max(cx, dx) < minX) || (Math.min(cx, dx) > maxX)
						|| (Math.max(cy, dy) < minY) || (Math.min(cy, dy) > maxY))
					continue;
				if ((orientation(ax, ay, bx, by, cx, cy)
						* orientation(ax, ay, bx, by, dx, dy) < 0)
						&& (orientation(cx, cy, dx, dy, ax, ay)
								* orientation(cx, cy, dx, dy, bx, by) < 0))
					crossings++;
			}
		}
		return crossings;
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
