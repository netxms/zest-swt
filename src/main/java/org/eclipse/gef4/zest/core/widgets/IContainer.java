/*******************************************************************************
 * Copyright 2005-2010, CHISEL Group, University of Victoria, Victoria, BC,
 * Canada. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group, University of Victoria 
 *               Mateusz Matela
 ******************************************************************************/
package org.eclipse.gef4.zest.core.widgets;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef4.zest.layouts.LayoutAlgorithm;
import org.eclipse.gef4.zest.layouts.dataStructures.DisplayIndependentRectangle;
import org.eclipse.swt.widgets.Widget;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * 
 */
public interface IContainer {

	/**
	 * Returns the graph this container belongs to.
	 * 
	 * @return the owning graph, or this if the container is a graph
	 */
	public abstract Graph getGraph();

	/**
	 * Returns the widget representing this container.
	 * 
	 * @return the {@link Graph} or {@link GraphContainer} widget
	 * @since 2.0
	 */
	public Widget getItem();

	/**
	 * Returns nodes lying directly in this container.
	 * 
	 * @return list of {@link GraphNode}s
	 */
	public abstract List getNodes();

	/**
	 * Returns list of connections laying inside this container. Only
	 * connections which both source and target nodes lay directly in this
	 * container are returned.
	 * 
	 * @return list of {@link GraphConnection}s
	 * @since 2.0
	 */
	public abstract List getConnections();

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @param graphNode
	 *            node to add to this container
	 */
	public abstract void addNode(GraphNode graphNode);

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @param figure
	 *            figure representing a subgraph, added to the subgraph layer
	 */
	public abstract void addSubgraphFigure(IFigure figure);

	/**
	 * Returns the type of this container.
	 * 
	 * @return {@link GraphItem#GRAPH} or {@link GraphItem#CONTAINER}
	 */
	public abstract int getItemType();

	/**
	 * Returns the area in which the layout algorithm places entities.
	 * 
	 * @return layout bounds of this container
	 * @since 2.0
	 */
	public abstract DisplayIndependentRectangle getLayoutBounds();

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return layout context of this container
	 */
	public abstract InternalLayoutContext getLayoutContext();

	/**
	 * Runs the layout algorithm on this container.
	 */
	public void applyLayout();

	/**
	 * Sets the layout algorithm used for this container.
	 * 
	 * @param algorithm
	 *            layout algorithm to use
	 * @param apply
	 *            true to run the algorithm immediately
	 */
	public void setLayoutAlgorithm(LayoutAlgorithm algorithm, boolean apply);

	/**
	 * Takes a list of connections and returns only those which source and
	 * target nodes lay directly in this container.
	 * 
	 * @param connections
	 *            list of GraphConnection to filter
	 * @return filtered list
	 */
	// protected List filterConnections(List connections) {
	// List result = new ArrayList();
	// for (Iterator iterator = connections.iterator(); iterator.hasNext();) {
	// GraphConnection connection = (GraphConnection) iterator.next();
	// if (connection.getSource().getParent() == this &&
	// connection.getDestination().getParent() == this)
	// result.add(connection);
	// }
	// return result;
	// }
}
