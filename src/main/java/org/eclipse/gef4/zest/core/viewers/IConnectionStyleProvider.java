/*******************************************************************************
 * Copyright 2005-2010, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.gef4.zest.core.viewers;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef4.zest.core.widgets.decoration.IConnectionDecorator;
import org.eclipse.swt.graphics.Color;

/**
 * An extension to label providers, to supply styles for connections based upon
 * relationships, rather than on connected nodes.
 * 
 * @author Del Myers
 * @see IGraphContentProvider
 * @see IEntityStyleProvider
 * 
 */
// @tag bug(151327-Styles) : created to solve this bug
public interface IConnectionStyleProvider {
	/**
	 * Returns the style flags for this connection. Valid flags are those that
	 * begin with CONNECTION in @see org.eclipse.gef4.zest.core.ZestStyles.
	 * Check ZestStyles for legal combinations.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the style flags for this connection.
	 * @see org.eclipse.gef4.zest.core.widgets.ZestStyles
	 */
	public int getConnectionStyle(Object rel);

	/**
	 * Returns the color for the connection. Null for default. Any resources
	 * created by this class must be disposed by this class.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the color.
	 */
	public Color getColor(Object rel);

	/**
	 * Returns the highlighted color for this connection. Null for default. Any
	 * resources created by this class must be disposed by this class.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the highlighted color. Null for default.
	 */
	public Color getHighlightColor(Object rel);

	/**
	 * Returns the line width of the connection. -1 for default.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the line width for the connection. -1 for default.
	 */
	public int getLineWidth(Object rel);

	/**
	 * Returns the tooltip for the connection.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the tooltip for the connection. Null for default.
	 */
	public IFigure getTooltip(Object rel);

	/**
	 * Returns the connection router of the single relation.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the connection router for rel. Null for default.
	 */
	public ConnectionRouter getRouter(Object rel);

	/**
	 * Returns the connection decoration for a single relation.
	 * 
	 * @param rel
	 *            the relationship represented by this connection.
	 * @return the connection router for rel. Null for default.
	 */
	public IConnectionDecorator getConnectionDecorator(Object rel);
}
