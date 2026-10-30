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
package org.eclipse.gef4.zest.layouts.dataStructures;

/**
 * This is a rectangle that isn't dependent on awt, swt, or any other library,
 * except layout.
 * 
 * @author Casey Best
 */
public class DisplayIndependentRectangle {

	/** Horizontal coordinate of the top-left corner. */
	public double x;
	/** Vertical coordinate of the top-left corner. */
	public double y;
	/** Width of this rectangle. */
	public double width;
	/** Height of this rectangle. */
	public double height;

	/**
	 * Creates an empty rectangle located at the origin.
	 */
	public DisplayIndependentRectangle() {
		this.x = 0;
		this.y = 0;
		this.width = 0;
		this.height = 0;
	}

	/**
	 * Creates a rectangle with the given location and size.
	 * 
	 * @param x horizontal coordinate of the top-left corner
	 * @param y vertical coordinate of the top-left corner
	 * @param width width of the new rectangle
	 * @param height height of the new rectangle
	 */
	public DisplayIndependentRectangle(double x, double y, double width,
			double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/**
	 * Creates a copy of the given rectangle.
	 * 
	 * @param rect rectangle to copy
	 */
	public DisplayIndependentRectangle(DisplayIndependentRectangle rect) {
		this.x = rect.x;
		this.y = rect.y;
		this.width = rect.width;
		this.height = rect.height;
	}

	public String toString() {
		return "(" + x + ", " + y + ", " + width + ", " + height + ")";
	}

	/**
	 * Checks whether this rectangle overlaps the given rectangle.
	 * 
	 * @param rect rectangle to test against
	 * @return true if the two rectangles share a region of non-zero area
	 */
	public boolean intersects(DisplayIndependentRectangle rect) {
		return rect.x < x + width && rect.y < y + height
				&& rect.x + rect.width > x && rect.y + rect.height > y;
	}
}
