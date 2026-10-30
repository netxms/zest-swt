/*******************************************************************************
 * Copyright (c) 2009-2010 Mateusz Matela and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Mateusz Matela - initial API and implementation
 *               Ian Bull
 ******************************************************************************/
package org.eclipse.gef4.zest.layouts.algorithms;

/**
 * Layout algorithm that places all elements in one column or one row, depending
 * on set orientation.
 */
public class BoxLayoutAlgorithm extends GridLayoutAlgorithm {

	/** Orientation that places all elements in one row. */
	public static final int HORIZONTAL = 1;

	/** Orientation that places all elements in one column. */
	public static final int VERTICAL = 2;

	private int orientation = HORIZONTAL;

	/**
	 * Creates a horizontal box layout.
	 */
	public BoxLayoutAlgorithm() {
	}

	/**
	 * Creates a box layout with the given orientation.
	 * 
	 * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
	 */
	public BoxLayoutAlgorithm(int orientation) {
		setOrientation(orientation);
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
	 * Sets orientation of this layout.
	 * 
	 * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
	 * @throws RuntimeException if orientation is neither of these values
	 */
	public void setOrientation(int orientation) {
		if (orientation == HORIZONTAL || orientation == VERTICAL)
			this.orientation = orientation;
		else
			throw new RuntimeException("Invalid orientation: " + orientation);
	}

	/**
	 * Returns a single row or a single column, depending on orientation.
	 * 
	 * @see GridLayoutAlgorithm#calculateNumberOfRowsAndCols(int, double,
	 *      double, double, double)
	 */
	protected int[] calculateNumberOfRowsAndCols(int numChildren,
			double boundX, double boundY, double boundWidth, double boundHeight) {
		if (orientation == HORIZONTAL)
			return new int[] { numChildren, 1 };
		else
			return new int[] { 1, numChildren };
	}
}
