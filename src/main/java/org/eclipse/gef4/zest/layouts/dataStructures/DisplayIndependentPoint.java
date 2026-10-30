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
 * This is a point that isn't dependent on awt, swt, or any other library,
 * except layout.
 * 
 * @author Casey Best
 */
public class DisplayIndependentPoint {
	/** Horizontal coordinate of this point. */
	public double x;
	/** Vertical coordinate of this point. */
	public double y;

	/**
	 * Compares this point with another point by coordinates.
	 * 
	 * @param o point to compare with (must be a
	 *            {@link DisplayIndependentPoint})
	 * @return true if both points have the same x and y coordinates
	 */
	public boolean equals(Object o) {
		DisplayIndependentPoint that = (DisplayIndependentPoint) o;
		if (this.x == that.x && this.y == that.y)
			return true;
		else
			return false;
	}

	/**
	 * Creates a point with the given coordinates.
	 * 
	 * @param x horizontal coordinate
	 * @param y vertical coordinate
	 */
	public DisplayIndependentPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates a copy of the given point.
	 * 
	 * @param point point to copy
	 */
	public DisplayIndependentPoint(DisplayIndependentPoint point) {
		this.x = point.x;
		this.y = point.y;
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	/**
	 * Create a new point based on the current point but in a new coordinate
	 * system
	 * 
	 * @param currentBounds bounds of the coordinate system this point is
	 *            currently expressed in
	 * @param targetBounds bounds of the coordinate system to convert to
	 * @return new point at the same relative position within targetBounds as
	 *         this point has within currentBounds
	 */
	public DisplayIndependentPoint convert(
			DisplayIndependentRectangle currentBounds,
			DisplayIndependentRectangle targetBounds) {
		double currentWidth = currentBounds.width;
		double currentHeight = currentBounds.height;

		double newX = (currentBounds.width == 0) ? 0 : (x / currentWidth)
				* targetBounds.width + targetBounds.x;
		double newY = (currentBounds.height == 0) ? 0 : (y / currentHeight)
				* targetBounds.height + targetBounds.y;
		return new DisplayIndependentPoint(newX, newY);
	}

	/**
	 * Converts this point based on the current x, y values to a percentage of
	 * the specified coordinate system
	 * 
	 * @param bounds bounds of the coordinate system this point is expressed
	 *            in
	 * @return new point whose coordinates are fractions (0 to 1) of the width
	 *         and height of bounds
	 */
	public DisplayIndependentPoint convertToPercent(
			DisplayIndependentRectangle bounds) {
		double newX = (bounds.width == 0) ? 0 : ((double) (x - bounds.x))
				/ bounds.width;
		double newY = (bounds.height == 0) ? 0 : ((double) (y - bounds.y))
				/ bounds.height;
		return new DisplayIndependentPoint(newX, newY);
	}

	/**
	 * Converts this point based on the current x, y values from a percentage of
	 * the specified coordinate system
	 * 
	 * @param bounds bounds of the coordinate system to convert to
	 * @return new point with absolute coordinates within bounds, treating this
	 *         point's coordinates as fractions (0 to 1) of its width and height
	 */
	public DisplayIndependentPoint convertFromPercent(
			DisplayIndependentRectangle bounds) {
		double newX = bounds.x + x * bounds.width;
		double newY = bounds.y + y * bounds.height;
		return new DisplayIndependentPoint(newX, newY);
	}
}
