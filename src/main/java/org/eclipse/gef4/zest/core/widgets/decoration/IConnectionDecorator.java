/*******************************************************************************
 * Copyright 2013, Zoltan Ujhelyi. All rights reserved. This program and the 
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Zoltan Ujhelyi
 ******************************************************************************/
package org.eclipse.gef4.zest.core.widgets.decoration;

import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef4.zest.core.widgets.GraphConnection;

/**
 * Adds decorations, such as arrow heads, to the figure of a graph connection.
 */
public interface IConnectionDecorator {

	/**
	 * Decorates the figure of the given connection.
	 * 
	 * @param connection
	 *            connection whose figure is decorated
	 * @param figure
	 *            figure of the connection to add decorations to
	 */
	public abstract void decorateConnection(GraphConnection connection,
			PolylineConnection figure);

}