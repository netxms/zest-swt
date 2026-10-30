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
package org.eclipse.gef4.zest.core.widgets.internal;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

/**
 * Anchor for self-loop connections: its reference point lies on the edge of the
 * owner figure instead of its center, so a loop can leave and enter the figure.
 */
public class LoopAnchor extends ChopboxAnchor {
	/**
	 * Creates an anchor on the given figure.
	 * 
	 * @param owner
	 *            figure the loop connection is attached to
	 */
	public LoopAnchor(IFigure owner) {
		super(owner);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.draw2d.ChopboxAnchor#getReferencePoint()
	 */
	public Point getReferencePoint() {
		// modification to getReferencePoint. Returns
		// a point on the outside of the owners box, rather than the
		// center. Only usefull for self-loops.
		if (getOwner() == null) {
			return null;
		} else {
			Point ref = getOwner().getBounds().getCenter();
			ref.y = getOwner().getBounds().y;
			getOwner().translateToAbsolute(ref);
			return ref;
		}
	}
}
