/*******************************************************************************
 * Copyright (c) 2005-2010 The Chisel Group and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group - initial API and implementation
 *               Mateusz Matela 
 *               Ian Bull
 ******************************************************************************/
package org.eclipse.gef4.zest.layouts.algorithms;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.gef4.zest.layouts.LayoutAlgorithm;
import org.eclipse.gef4.zest.layouts.interfaces.ConnectionLayout;
import org.eclipse.gef4.zest.layouts.interfaces.EntityLayout;
import org.eclipse.gef4.zest.layouts.interfaces.LayoutContext;
import org.eclipse.gef4.zest.layouts.interfaces.NodeLayout;
import org.eclipse.gef4.zest.layouts.interfaces.SubgraphLayout;

/**
 * Layout algorithm that arranges nodes in layers according to connection
 * direction, using the Draw2D {@link DirectedGraphLayout}.
 */
public class DirectedGraphLayoutAlgorithm implements LayoutAlgorithm {

	class ExtendedDirectedGraphLayout extends DirectedGraphLayout {

		public void visit(DirectedGraph graph) {
			Field field;
			try {
				field = DirectedGraphLayout.class.getDeclaredField("steps");
				field.setAccessible(true);
				Object object = field.get(this);
				List steps = (List) object;
				// steps.remove(10);
				// steps.remove(9);
				// steps.remove(8);
				// steps.remove(2);
				field.setAccessible(false);
				super.visit(graph);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	/** Orientation that places layers side by side, from left to right. */
	public static final int HORIZONTAL = 1;

	/** Orientation that places layers one below another, from top to bottom. */
	public static final int VERTICAL = 2;

	private int orientation = VERTICAL;

	private LayoutContext context;

	/**
	 * Creates a vertical directed graph layout.
	 */
	public DirectedGraphLayoutAlgorithm() {
	}

	/**
	 * Creates a directed graph layout with the given orientation.
	 * 
	 * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}; any other
	 *            value leaves the default (vertical) orientation
	 */
	public DirectedGraphLayoutAlgorithm(int orientation) {
		if (orientation == VERTICAL)
			this.orientation = orientation;
	}

	/**
	 * Returns orientation of this layout.
	 * 
	 * @return {@link #HORIZONTAL} or {@link #VERTICAL}
	 */
	public int getOrientation() {
		return orientation;
	}

	/**
	 * Sets orientation of this layout. Values other than {@link #HORIZONTAL}
	 * and {@link #VERTICAL} are ignored.
	 * 
	 * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
	 */
	public void setOrientation(int orientation) {
		if (orientation == HORIZONTAL || orientation == VERTICAL)
			this.orientation = orientation;
	}

	public void applyLayout(boolean clean) {
		if (!clean)
			return;
		HashMap mapping = new HashMap();
		DirectedGraph graph = new DirectedGraph();
		EntityLayout[] entities = context.getEntities();
		for (int i = 0; i < entities.length; i++) {
			Node node = new Node(entities[i]);
			node.setSize(new Dimension(10, 10));
			mapping.put(entities[i], node);
			graph.nodes.add(node);
		}
		ConnectionLayout[] connections = context.getConnections();
		for (int i = 0; i < connections.length; i++) {
			Node source = (Node) mapping.get(getEntity(connections[i]
					.getSource()));
			Node dest = (Node) mapping
					.get(getEntity(connections[i].getTarget()));
			if (source != null && dest != null) {
				Edge edge = new Edge(connections[i], source, dest);
				graph.edges.add(edge);
			}
		}
		DirectedGraphLayout directedGraphLayout = new ExtendedDirectedGraphLayout();
		directedGraphLayout.visit(graph);

		for (Iterator iterator = graph.nodes.iterator(); iterator.hasNext();) {
			Node node = (Node) iterator.next();
			EntityLayout entity = (EntityLayout) node.data;
			if (orientation == VERTICAL) {
				entity.setLocation(node.x, node.y);
			} else {
				entity.setLocation(node.y, node.x);
			}
		}
	}

	private EntityLayout getEntity(NodeLayout node) {
		if (!node.isPruned())
			return node;
		SubgraphLayout subgraph = node.getSubgraph();
		if (subgraph.isGraphEntity())
			return subgraph;
		return null;
	}

	public void setLayoutContext(LayoutContext context) {
		this.context = context;
	}

}
