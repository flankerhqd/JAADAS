/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros;

/**
 * This class defines a lattice in terms of its top and bottom elements
 * and a join operation. 
 *
 * @param <V> The domain type for this lattice.
 */
public interface JoinLattice<V> {
	
	V topElement();
	
	V bottomElement();
	
	V join(V left, V right);

}
