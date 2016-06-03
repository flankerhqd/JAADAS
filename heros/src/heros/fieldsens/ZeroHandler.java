/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.fieldsens;


public interface ZeroHandler<Field> {

	/**
	 * If reading fields on a fact abstraction directly connected to a Zero fact, this handler is consulted
	 * to decide if the field may be read. 
	 * @param accPath The AccessPath consisting of fields already read in addition to a new field to be read.
	 * @return true if the AccessPath can be generated from within the Zero fact, false otherwise.
	 */
	boolean shouldGenerateAccessPath(AccessPath<Field> accPath);
}
