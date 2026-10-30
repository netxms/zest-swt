/*******************************************************************************
 * Copyright 2005-2010, CHISEL Group, University of Victoria, Victoria, BC,
 * Canada. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: The Chisel Group, University of Victoria 
 *               Mateusz Matela
 *               Zoltan Ujhelyi
 ******************************************************************************/
package org.eclipse.gef4.zest.core.widgets;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef4.zest.core.widgets.decoration.IConnectionDecorator;
import org.eclipse.gef4.zest.core.widgets.internal.LoopAnchor;
import org.eclipse.gef4.zest.core.widgets.internal.PolylineArcConnection;
import org.eclipse.gef4.zest.core.widgets.internal.RoundedChopboxAnchor;
import org.eclipse.gef4.zest.core.widgets.internal.ZestRootLayer;
import org.eclipse.gef4.zest.layouts.interfaces.ConnectionLayout;
import org.eclipse.gef4.zest.layouts.interfaces.NodeLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;

/**
 * This is the graph connection model which stores the source and destination
 * nodes and the properties of this connection (color, line width etc).
 * 
 * @author Chris Callendar
 * 
 * @author Ian Bull
 */
public class GraphConnection extends GraphItem {

	private Font font;
	private GraphNode sourceNode;
	private GraphNode destinationNode;

	private double weight;
	private Color color;
	private Color highlightColor;
	private Color foreground;
	private int lineWidth;
	private int lineStyle;
	private final Graph graph;

	private int connectionStyle;
	private int curveDepth;
	private boolean isDisposed = false;

	private Label connectionLabel = null;
	private PolylineArcConnection connectionFigure = null;
	private PolylineArcConnection cachedConnectionFigure = null;
	private Connection sourceContainerConnectionFigure = null;
	private Connection targetContainerConnectionFigure = null;

	/**
	 * The state of visibility set by the user.
	 */
	private boolean visible;

	private IFigure tooltip;

	private boolean highlighted;
	private boolean hasCustomTooltip;

	private ConnectionRouter router = null;

	private IConnectionDecorator connectionDecorator = null;

	/**
	 * Creates a connection between two nodes and registers it with both nodes
	 * and with the graph. The connection style is the graph's default
	 * connection style combined with the given style bits.
	 * 
	 * @param graphModel
	 *            graph that owns this connection
	 * @param style
	 *            connection style bits (the CONNECTION constants in
	 *            {@link ZestStyles})
	 * @param source
	 *            node the connection starts from
	 * @param destination
	 *            node the connection ends at
	 */
	public GraphConnection(Graph graphModel, int style, GraphNode source,
			GraphNode destination) {
		super(graphModel, style);

		this.connectionStyle |= graphModel.getConnectionStyle();
		this.connectionStyle |= style;
		this.sourceNode = source;
		this.destinationNode = destination;
		this.visible = true;
		this.color = ColorConstants.lightGray;
		this.foreground = ColorConstants.lightGray;
		this.highlightColor = graphModel.DARK_BLUE;
		this.lineWidth = 1;
		this.lineStyle = Graphics.LINE_SOLID;
		setWeight(1.0);
		this.graph = graphModel;
		this.curveDepth = 0;
		this.font = Display.getDefault().getSystemFont();
		registerConnection(source, destination);
	}

	private void registerConnection(GraphNode source, GraphNode destination) {
		if (source.getSourceConnections().contains(this)) {
			source.removeSourceConnection(this);
		}
		if (destination.getTargetConnections().contains(this)) {
			destination.removeTargetConnection(this);
		}
		(source).addSourceConnection(this);
		(destination).addTargetConnection(this);

		if (source.getParent().getItemType() == GraphItem.CONTAINER
				&& destination.getParent().getItemType() == GraphItem.CONTAINER
				&& (source.getParent() == destination.getParent())) {
			// 196189: Edges should not draw on the edge layer if both the src
			// and dest are in the same container
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=196189
			graph.addConnection(this, false);
		} else {
			graph.addConnection(this, true);
		}

		if ((source.getParent()).getItemType() == GraphItem.CONTAINER) {
			// If the container of the source is a container, we need to draw
			// another
			// arc on that arc layer
			sourceContainerConnectionFigure = doCreateFigure();
			((GraphContainer) source.getParent())
					.addConnectionFigure(sourceContainerConnectionFigure);
			this.setVisible(false);
		}

		if ((destination.getParent()).getItemType() == GraphItem.CONTAINER) { // &&
																				// src_destSameContainer
																				// ==
																				// false)
																				// {
			// If the container of the source is a container, we need to draw
			// another
			// arc on that arc layer
			targetContainerConnectionFigure = doCreateFigure();
			((GraphContainer) destination.getParent())
					.addConnectionFigure(targetContainerConnectionFigure);
			this.setVisible(false);
		}
		graph.registerItem(this);
	}

	void removeFigure() {
		if (connectionFigure.getParent() != null) {
			connectionFigure.getParent().remove(connectionFigure);
		}
		connectionFigure = null;
		if (sourceContainerConnectionFigure != null) {
			sourceContainerConnectionFigure.getParent().remove(
					sourceContainerConnectionFigure);
			sourceContainerConnectionFigure = null;
		}
		if (targetContainerConnectionFigure != null) {
			targetContainerConnectionFigure.getParent().remove(
					targetContainerConnectionFigure);
			targetContainerConnectionFigure = null;
		}

	}

	public void dispose() {
		super.dispose();
		this.isDisposed = true;
		(getSource()).removeSourceConnection(this);
		(getDestination()).removeTargetConnection(this);
		graph.removeConnection(this);
		if (sourceContainerConnectionFigure != null) {
			sourceContainerConnectionFigure.getParent().remove(
					sourceContainerConnectionFigure);
		}
		if (targetContainerConnectionFigure != null) {
			targetContainerConnectionFigure.getParent().remove(
					targetContainerConnectionFigure);
		}
	}

	/**
	 * Returns whether {@link #dispose()} has been called on this connection.
	 * 
	 * @return true if this connection has been disposed
	 */
	public boolean isDisposed() {
		return isDisposed;
	}

	/**
	 * Returns the draw2d figure that draws this connection, creating it on
	 * first use.
	 * 
	 * @return the connection figure
	 */
	public Connection getConnectionFigure() {
		if (connectionFigure == null) {
			connectionFigure = doCreateFigure();
		}
		return connectionFigure;
	}

	/**
	 * Gets the external connection object.
	 * 
	 * @return Object
	 * @deprecated Use {@link #getData()} instead
	 */
	@Deprecated
	public Object getExternalConnection() {
		return this.getData();
	}

	/**
	 * Returns a string like 'source -> destination'
	 * 
	 * @return String
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("GraphModelConnection: ");
		buffer.append(sourceNode != null ? sourceNode.getText() : "null");
		buffer.append(isDirected() ? " --> " : " --- ");
		buffer.append(destinationNode != null ? destinationNode.getText()
				: "null");
		buffer.append("  (weight=").append(getWeightInLayout()).append(")");
		return buffer.toString();
	}

	/**
	 * Returns the style of this connection. Valid styles are those that begin
	 * with CONNECTION in ZestStyles.
	 * 
	 * @return the style of this connection.
	 * @see ZestStyles
	 */
	public int getConnectionStyle() {
		return connectionStyle;
	}

	/**
	 * Sets the style of this connection and updates its figure. Valid styles
	 * are those that begin with CONNECTION in ZestStyles.
	 * 
	 * @param style
	 *            the new connection style bits
	 * @see ZestStyles
	 */
	public void setConnectionStyle(int style) {
		this.connectionStyle = style;
		updateFigure(this.connectionFigure);
	}

	/**
	 * Gets the weight of this connection. The weight must be in {-1, [0-1]}. A
	 * weight of -1 means that there is no force/tension between the nodes. A
	 * weight of 0 results in the maximum spring length being used (farthest
	 * apart). A weight of 1 results in the minimum spring length being used
	 * (closest together).
	 *
	 * @return the weight: {-1, [0 - 1]}.
	 */
	public double getWeightInLayout() {
		return weight;
	}

	/**
	 * Gets the font for the label on this connection
	 * 
	 * @return the label font
	 */
	public Font getFont() {
		return this.font;
	}

	/**
	 * Sets the font for the label on this connection.
	 * 
	 * @param f
	 *            the font to use for the connection label
	 */
	public void setFont(Font f) {
		this.font = f;
	}

	/**
	 * Sets the weight for this connection. The weight must be in {-1, [0-1]}. A
	 * weight of -1 means that there is no force/tension between the nodes. A
	 * weight of 0 results in the maximum spring length being used (farthest
	 * apart). A weight of 1 results in the minimum spring length being used
	 * (closest together).
	 * 
	 * @param weight
	 *            the new weight; values below 0 are stored as -1 and values
	 *            above 1 are stored as 1
	 */
	public void setWeight(double weight) {
		if (weight < 0) {
			this.weight = -1;
		} else if (weight > 1) {
			this.weight = 1;
		} else {
			this.weight = weight;
		}
	}

	/**
	 * Returns the color of this connection.
	 * 
	 * @return Color
	 */
	public Color getLineColor() {
		return color;
	}

	/**
	 * Sets the highlight color.
	 * 
	 * @param color
	 *            the color to use for highlighting.
	 */
	public void setHighlightColor(Color color) {
		this.highlightColor = color;
	}

	/**
	 * @return the highlight color
	 */
	public Color getHighlightColor() {
		return highlightColor;
	}

	/**
	 * Perminently sets the color of this line to the given color. This will
	 * become the color of the line when it is not highlighted. If you would
	 * like to temporarily change the color of the line, use changeLineColor.
	 * 
	 * @param color
	 *            the color to be set.
	 * @see #changeLineColor(Color color)
	 */
	public void setLineColor(Color color) {
		this.foreground = color;
		changeLineColor(foreground);
	}

	/**
	 * Sets the connection color.
	 * 
	 * @param color
	 *            the color used to draw the connection line
	 */
	public void changeLineColor(Color color) {
		this.color = color;
		updateFigure(connectionFigure);
	}

	/**
	 * Sets the tooltip on this node. This tooltip will display if the mouse
	 * hovers over the node. Setting the tooltip has no effect if a custom
	 * figure has been set.
	 * 
	 * @param tooltip
	 *            figure shown as the tooltip of this connection
	 */
	public void setTooltip(IFigure tooltip) {
		hasCustomTooltip = true;
		this.tooltip = tooltip;
		updateFigure(connectionFigure);
	}

	/**
	 * Gets the current tooltip for this node. The tooltip returned is
	 * meaningless if a custom figure has been set.
	 * 
	 * @return the tooltip figure, or null if none has been set
	 */
	public IFigure getTooltip() {
		return this.tooltip;
	}

	/**
	 * Returns the connection line width.
	 * 
	 * @return int
	 */
	public int getLineWidth() {
		return lineWidth;
	}

	/**
	 * Sets the connection line width.
	 * 
	 * @param lineWidth
	 *            the line width in pixels
	 */
	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
		updateFigure(connectionFigure);
	}

	/**
	 * Returns the connection line style.
	 * 
	 * @return int
	 */
	public int getLineStyle() {
		return lineStyle;
	}

	/**
	 * Sets the connection line style.
	 * 
	 * @param lineStyle
	 *            one of the SWT LINE_* constants (solid, dash, dot, etc.)
	 */
	public void setLineStyle(int lineStyle) {
		this.lineStyle = lineStyle;
		updateFigure(connectionFigure);
	}

	/**
	 * Gets the source node for this relationship
	 * 
	 * @return GraphModelNode
	 */
	public GraphNode getSource() {
		return this.sourceNode;
	}

	/**
	 * Gets the target node for this relationship
	 * 
	 * @return GraphModelNode
	 */
	public GraphNode getDestination() {
		return this.destinationNode;
	}

	/**
	 * Highlights this node. Uses the default highlight color.
	 */
	public void highlight() {
		if (highlighted) {
			return;
		}
		IFigure parentFigure = connectionFigure.getParent();
		if (parentFigure instanceof ZestRootLayer) {
			((ZestRootLayer) parentFigure)
					.highlightConnection(connectionFigure);
		}
		highlighted = true;
		updateFigure(connectionFigure);
	}

	/**
	 * Unhighlights this node. Uses the default color.
	 */
	public void unhighlight() {
		if (!highlighted) {
			return;
		}
		IFigure parentFigure = connectionFigure.getParent();
		if (parentFigure instanceof ZestRootLayer) {
			((ZestRootLayer) parentFigure)
					.unHighlightConnection(connectionFigure);
		}
		highlighted = false;
		updateFigure(connectionFigure);
	}

	/**
	 * Returns true if this connection is highlighted, false otherwise
	 * 
	 * @return true if this connection is highlighted
	 */
	public boolean isHighlighted() {
		return highlighted;
	}

	/**
	 * Gets the graph model that this connection is in
	 * 
	 * @return The graph model that this connection is contained in
	 */
	public Graph getGraphModel() {
		return this.graph;
	}

	/**
	 * Sets the curve depth of the arc. The curve depth is defined as the
	 * maximum distance from any point on the chord (i.e. a vector normal to the
	 * chord with magnitude d).
	 * 
	 * If 0 is set, a Polyline Connection will be used, otherwise a
	 * PolylineArcConnectoin will be used. Negative depths are also supported.
	 * 
	 * @param depth
	 *            The depth of the curve
	 */
	public void setCurveDepth(int depth) {
		if (this.curveDepth == 0 && depth != 0 || this.curveDepth != 0
				&& depth == 0) {
			// There is currently no curve, so we have to create
			// a curved connection
			this.cachedConnectionFigure = connectionFigure;
			graph.removeConnection(this);
			this.curveDepth = depth;
			this.connectionFigure = doCreateFigure();
			registerConnection(sourceNode, destinationNode);
			updateFigure(this.connectionFigure);
		} else {
			this.curveDepth = depth;
			updateFigure(this.connectionFigure);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.mylar.zest.core.widgets.IGraphItem#getItemType()
	 */
	public int getItemType() {
		return CONNECTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.mylar.zest.core.internal.graphmodel.GraphItem#setVisible(
	 * boolean)
	 */
	public void setVisible(boolean visible) {
		if (getSource().isVisible() && getDestination().isVisible() && visible) {
			this.getFigure().setVisible(visible);
			if (sourceContainerConnectionFigure != null) {
				sourceContainerConnectionFigure.setVisible(visible);
			}
			if (targetContainerConnectionFigure != null) {
				targetContainerConnectionFigure.setVisible(visible);
			}
			this.visible = visible;
		} else {
			this.getFigure().setVisible(false);
			if (sourceContainerConnectionFigure != null) {
				sourceContainerConnectionFigure.setVisible(false);
			}
			if (targetContainerConnectionFigure != null) {
				targetContainerConnectionFigure.setVisible(false);
			}
			this.visible = false;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.mylar.zest.core.widgets.IGraphItem#isVisible()
	 */
	public boolean isVisible() {
		return visible;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Item#setText(java.lang.String)
	 */
	public void setText(String string) {
		super.setText(string);

		if (this.connectionFigure != null) {
			updateFigure(this.connectionFigure);
		}
	}

	/**
	 * Returns whether this connection is directed, i.e. has the
	 * {@link ZestStyles#CONNECTIONS_DIRECTED} style set.
	 * 
	 * @return true if this connection is directed
	 * @since 2.0
	 */
	public boolean isDirected() {
		return ZestStyles.checkStyle(connectionStyle,
				ZestStyles.CONNECTIONS_DIRECTED);
	}

	/**
	 * Sets or clears the {@link ZestStyles#CONNECTIONS_DIRECTED} style on
	 * this connection.
	 * 
	 * @param directed
	 *            true to make this connection directed, false to make it
	 *            undirected
	 * @since 2.0
	 */
	public void setDirected(boolean directed) {
		if (directed) {
			setConnectionStyle(connectionStyle
					| ZestStyles.CONNECTIONS_DIRECTED);
		} else {
			setConnectionStyle(connectionStyle
					& (-1 - ZestStyles.CONNECTIONS_DIRECTED));
		}
	}

	PolylineConnection getSourceContainerConnectionFigure() {
		return (PolylineConnection) sourceContainerConnectionFigure;
	}

	PolylineConnection getTargetContainerConnectionFigure() {
		return (PolylineConnection) targetContainerConnectionFigure;
	}

	private void updateFigure(PolylineArcConnection connection) {
		if (sourceContainerConnectionFigure != null) {
			doUpdateFigure(sourceContainerConnectionFigure);
		}
		if (targetContainerConnectionFigure != null) {
			doUpdateFigure(targetContainerConnectionFigure);
		}
		doUpdateFigure(connection);
	}

	private void doUpdateFigure(Connection connection) {
		if (connection == null || this.isDisposed()) {
			return;
		}
		Shape connectionShape = (Shape) connection;

		connectionShape.setLineStyle(getLineStyle());

		if (this.getText() != null || this.getImage() != null) {
			if (this.getImage() != null) {
				this.connectionLabel.setIcon(this.getImage());
			}
			if (this.getText() != null) {
				this.connectionLabel.setText(this.getText());
			}
			this.connectionLabel.setFont(this.getFont());
		}

		if (highlighted) {
			connectionShape.setForegroundColor(getHighlightColor());
			connectionShape.setLineWidth(getLineWidth() * 2);
		} else {
			connectionShape.setForegroundColor(getLineColor());
			connectionShape.setLineWidth(getLineWidth());
		}
		if (connection instanceof PolylineArcConnection) {
			PolylineArcConnection arcConnection = (PolylineArcConnection) connection;
			arcConnection.setDepth(curveDepth);
		}
		if (connectionFigure != null) {
			applyConnectionRouter(connectionFigure);
		}

		IFigure toolTip;
		if (this.getTooltip() == null && getText() != null
				&& getText().length() > 0 && hasCustomTooltip == false) {
			toolTip = new Label();
			((Label) toolTip).setText(getText());
		} else {
			toolTip = this.getTooltip();
		}
		connection.setToolTip(toolTip);

		connection.setVisible(visible);
	}

	private PolylineArcConnection doCreateFigure() {
		PolylineArcConnection connectionFigure = cachedOrNewConnectionFigure();
		ChopboxAnchor sourceAnchor = null;
		ChopboxAnchor targetAnchor = null;
		this.connectionLabel = new Label();
		Locator labelLocator = null;

		if (getSource() == getDestination()) {
			// If this is a self loop, create a looped arc and put the locator
			// at the top of the connection
			sourceAnchor = new LoopAnchor(getSource().getNodeFigure());
			targetAnchor = new LoopAnchor(getDestination().getNodeFigure());
			labelLocator = new MidpointLocator(connectionFigure, 0) {
				protected Point getReferencePoint() {
					Point p = Point.SINGLETON;
					p.x = getConnection().getPoints().getPoint(getIndex()).x;
					p.y = (int) (getConnection().getPoints().getPoint(
							getIndex()).y - (curveDepth * 1.5));
					getConnection().translateToAbsolute(p);
					return p;
				}
			};
		} else {
			if (curveDepth != 0) {
				connectionFigure.setDepth(this.curveDepth);
			}
			applyConnectionRouter(connectionFigure);
			sourceAnchor = new RoundedChopboxAnchor(
					getSource().getNodeFigure(), 8);
			targetAnchor = new RoundedChopboxAnchor(getDestination()
					.getNodeFigure(), 8);
			labelLocator = new MidpointLocator(connectionFigure, 0);
		}

		connectionFigure.setSourceAnchor(sourceAnchor);
		connectionFigure.setTargetAnchor(targetAnchor);
		connectionFigure.add(this.connectionLabel, labelLocator);

		applyDecoration(connectionFigure);

		doUpdateFigure(connectionFigure);
		return connectionFigure;
	}

	private PolylineArcConnection cachedOrNewConnectionFigure() {
		return cachedConnectionFigure == null ? new PolylineArcConnection()
				: cachedConnectionFigure;
	}

	IFigure getFigure() {
		return this.getConnectionFigure();
	}

	private InternalConnectionLayout layout;

	InternalConnectionLayout getLayout() {
		if (layout == null) {
			layout = new InternalConnectionLayout();
		}
		return layout;
	}

	class InternalConnectionLayout implements ConnectionLayout {
		private boolean visible = GraphConnection.this.isVisible();

		public NodeLayout getSource() {
			return sourceNode.getLayout();
		}

		public NodeLayout getTarget() {
			return destinationNode.getLayout();
		}

		public double getWeight() {
			return GraphConnection.this.getWeightInLayout();
		}

		public boolean isDirected() {
			return GraphConnection.this.isDirected();
		}

		public boolean isVisible() {
			return visible;
		}

		public void setVisible(boolean visible) {
			graph.getLayoutContext().checkChangesAllowed();
			this.visible = visible;
		}

		void applyLayout() {
			// Commented out to fix problem with re-appearing invisible links after setInput
			/*
			if (GraphConnection.this.isVisible() != this.visible) {
				GraphConnection.this.setVisible(this.visible);
			}
			*/
		}
	}

	void applyLayoutChanges() {
		if (layout != null) {
			layout.applyLayout();
		}
	}

	/**
	 * Applies the connection router with a possible fallback to the default
	 * connection router to the graph
	 * 
	 * @param conn
	 * @since 2.0
	 */
	void applyConnectionRouter(Connection conn) {
		if (router != null) {
			conn.setConnectionRouter(router);
		} else if (graph.getDefaultConnectionRouter() != null) {
			conn.setConnectionRouter(graph.getDefaultConnectionRouter());
		}
	}

	void applyDecoration(PolylineArcConnection conn) {
		IConnectionDecorator decorator = connectionDecorator;
		if (decorator == null) {
			decorator = ((connectionStyle & ZestStyles.CONNECTIONS_DIRECTED) > 0) ? getGraphModel()
					.getDefaultDirectedConnectionDecorator() : getGraphModel()
					.getDefaultConnectionDecorator();
		}
		decorator.decorateConnection(this, conn);
	}

	/**
	 * Sets the connection router of the connection
	 * 
	 * @param router
	 *            router used to compute the path of the connection figure
	 * @since 2.0
	 */
	public void setRouter(ConnectionRouter router) {
		this.router = router;
	}

	/**
	 * Sets the decorator that adds arrow heads and other decorations to this
	 * connection's figure. If a figure already exists it is decorated again
	 * immediately.
	 * 
	 * @param connectionDecorator
	 *            the decorator to use, or null to use the graph's default
	 *            decorator
	 */
	public void setConnectionDecoration(IConnectionDecorator connectionDecorator) {
		this.connectionDecorator = connectionDecorator;
		if (connectionFigure != null) {
			applyDecoration(connectionFigure);
			updateFigure(connectionFigure);
		}
	}
}
