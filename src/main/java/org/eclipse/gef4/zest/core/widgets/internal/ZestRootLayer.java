/*******************************************************************************
 * Copyright (c) 2005-2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation Chisel Group,
 * University of Victoria - Adapted for XY Scaled Graphics
 ******************************************************************************/
package org.eclipse.gef4.zest.core.widgets.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.IFigure;

/**
 * The root figure for Zest. The figure is broken up into following segments:
 * <ol>
 * <li>The Connections</li>
 * <li>The Subgraphs</li>
 * <li>The Nodes</li>
 * <li>The Highlighted Connections</li>
 * <li>The Highlighted Nodes</li>
 * </ol>
 * 
 */
public class ZestRootLayer extends FreeformLayer {

	/**
	 * Bottom layer, holding connection figures.
	 */
	public static final int CONNECTIONS_LAYER = 0;

	/**
	 * Layer holding subgraph figures, above connections.
	 */
	public static final int SUBGRAPHS_LAYER = 1;

	/**
	 * Layer holding node figures, above subgraphs.
	 */
	public static final int NODES_LAYER = 2;

	/**
	 * Layer holding highlighted connection figures, above nodes.
	 */
	public static final int CONNECTIONS_HIGHLIGHTED_LAYER = 3;

	/**
	 * Layer holding highlighted node figures, above highlighted connections.
	 */
	public static final int NODES_HIGHLIGHTED_LAYER = 4;

	/**
	 * Topmost layer, holding figures added without an explicit layer.
	 */
	public static final int TOP_LAYER = 5;

	/**
	 * Number of layers in this figure.
	 */
	public static final int NUMBER_OF_LAYERS = 6;

	private final int[] itemsInLayer = new int[NUMBER_OF_LAYERS];

	/**
	 * Set of all figures that are decorations for other figures. A decoration
	 * figure is always put one position (or more if there's more than one
	 * decoration for the same figure) after the decorated figure in children
	 * list.
	 */
	private HashSet decoratingFigures = new HashSet();

	/**
	 * If true, it indicates that a figure is added using a proper method and
	 * its layer is known. Otherwise, Figure#add() method was used and layer
	 * must be guessed
	 */
	private boolean isLayerKnown = false;

	/**
	 * Adds a node to the ZestRootLayer
	 * 
	 * @param nodeFigure
	 *            The figure representing the node
	 */
	public void addNode(IFigure nodeFigure) {
		addFigure(nodeFigure, NODES_LAYER);
	}

	/**
	 * Adds a connection to the ZestRootLayer
	 * 
	 * @param connectionFigure
	 *            The figure representing the connection
	 */
	public void addConnection(IFigure connectionFigure) {
		addFigure(connectionFigure, CONNECTIONS_LAYER);
	}

	/**
	 * Adds a subgraph to the ZestRootLayer
	 * 
	 * @param subgraphFigrue
	 *            The figure representing the subgraph
	 */
	public void addSubgraph(IFigure subgraphFigrue) {
		addFigure(subgraphFigrue, SUBGRAPHS_LAYER);
	}

	/**
	 * Moves a node figure to the highlighted nodes layer, above all other
	 * nodes.
	 * 
	 * @param nodeFigure
	 *            The figure representing the node
	 */
	public void highlightNode(IFigure nodeFigure) {
		changeFigureLayer(nodeFigure, NODES_HIGHLIGHTED_LAYER);
	}

	/**
	 * Moves a connection figure to the highlighted connections layer, above
	 * nodes.
	 * 
	 * @param connectionFigure
	 *            The figure representing the connection
	 */
	public void highlightConnection(IFigure connectionFigure) {
		changeFigureLayer(connectionFigure, CONNECTIONS_HIGHLIGHTED_LAYER);
	}

	/**
	 * Moves a node figure back to the regular nodes layer.
	 * 
	 * @param nodeFigure
	 *            The figure representing the node
	 */
	public void unHighlightNode(IFigure nodeFigure) {
		changeFigureLayer(nodeFigure, NODES_LAYER);
	}

	/**
	 * Moves a connection figure back to the regular connections layer.
	 * 
	 * @param connectionFigure
	 *            The figure representing the connection
	 */
	public void unHighlightConnection(IFigure connectionFigure) {
		changeFigureLayer(connectionFigure, CONNECTIONS_LAYER);
	}

	private void changeFigureLayer(IFigure figure, int newLayer) {
		ArrayList decorations = getDecorations(figure);
		Object constraint = null;
		if (figure instanceof Connection)
		{
			ConnectionRouter router = ((Connection)figure).getConnectionRouter();
			if (router != null)
				constraint = router.getConstraint((Connection)figure);
		}
		remove(figure);

		addFigure(figure, newLayer);
		for (Iterator iterator = decorations.iterator(); iterator.hasNext();) {
			addDecoration(figure, (IFigure) iterator.next());
		}
		if (constraint != null)
			((Connection)figure).getConnectionRouter().setConstraint((Connection)figure, constraint);

		this.invalidate();
		this.repaint();
	}

	private ArrayList getDecorations(IFigure figure) {
		ArrayList result = new ArrayList();
		int index = getChildren().indexOf(figure);
		if (index == -1) {
			return result;
		}
		for (index++; index < getChildren().size(); index++) {
			Object nextFigure = getChildren().get(index);
			if (decoratingFigures.contains(nextFigure)) {
				result.add(nextFigure);
			} else {
				break;
			}
		}
		return result;
	}

	/**
	 * 
	 * @param layer
	 * @return position after the last element in given layer
	 */
	private int getPosition(int layer) {
		int result = 0;
		for (int i = 0; i <= layer; i++) {
			result += itemsInLayer[i];
		}
		return result;
	}

	/**
	 * 
	 * @param position
	 * @return number of layer containing element at given position
	 */
	private int getLayer(int position) {
		int layer = 0;
		int positionInLayer = itemsInLayer[0];
		while (layer < NUMBER_OF_LAYERS - 1 && positionInLayer <= position) {
			layer++;
			positionInLayer += itemsInLayer[layer];
		}
		return layer;
	}

	/**
	 * Adds a figure at the end of the given layer.
	 * 
	 * @param figure
	 *            figure to add
	 * @param layer
	 *            one of the *_LAYER constants of this class
	 */
	public void addFigure(IFigure figure, int layer) {
		int position = getPosition(layer);
		itemsInLayer[layer]++;
		isLayerKnown = true;
		add(figure, position);
	}

	public void add(IFigure child, Object constraint, int index) {
		super.add(child, constraint, index);
		if (!isLayerKnown) {
			int layer = 0, positionInLayer = itemsInLayer[0];
			while (positionInLayer < index) {
				layer++;
				positionInLayer += itemsInLayer[layer];
			}
			if (index == -1) {
				layer = NUMBER_OF_LAYERS - 1;
			}
			itemsInLayer[layer]++;
		}
		isLayerKnown = false;
	}

	public void remove(IFigure child) {
		int position = this.getChildren().indexOf(child);
		if (position == -1) {
			return;
		}
		itemsInLayer[getLayer(position)]--;
		if (decoratingFigures.contains(child)) {
			decoratingFigures.remove(child);
			super.remove(child);
		} else {
			ArrayList decorations = getDecorations(child);
			super.remove(child);
			for (Iterator iterator = decorations.iterator(); iterator.hasNext();) {
				remove((IFigure) iterator.next());
			}
		}
	}

	/**
	 * Adds a decoration figure right after the figure it decorates, in the same
	 * layer. The decoration is removed together with the decorated figure.
	 * 
	 * @param decorated
	 *            figure already on this layer
	 * @param decorating
	 *            figure drawn on top of the decorated one
	 */
	public void addDecoration(IFigure decorated, IFigure decorating) {
		int position = this.getChildren().indexOf(decorated);
		if (position == -1) {
			throw new RuntimeException(
					"Can't add decoration for a figuer that is not on this ZestRootLayer");
		}
		itemsInLayer[getLayer(position)]++;
		isLayerKnown = true;
		do {
			position++;
		} while (position < getChildren().size()
				&& decoratingFigures.contains(getChildren().get(position)));
		decoratingFigures.add(decorating);
		add(decorating, position);
	}
}
