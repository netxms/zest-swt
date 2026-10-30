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

import org.eclipse.jface.viewers.IStructuredContentProvider;

/**
 * 
 * @author Ian Bull
 * 
 */
public interface IGraphEntityContentProvider extends IStructuredContentProvider {

	/**
	 * Returns the entities (nodes) of the graph for the given input.
	 * 
	 * @param inputElement
	 *            the input model object
	 * @return all entities to show as nodes in the graph
	 */
	public Object[] getElements(Object inputElement);

	/**
	 * Gets the elements this object is connected to
	 * 
	 * @param entity
	 *            the entity whose connected entities are requested
	 * @return entities that the given entity has a connection to
	 */
	public Object[] getConnectedTo(Object entity);

}
