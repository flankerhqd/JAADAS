/*******************************************************************************
 * Copyright (c) 2013 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.solver;

/**
 * A data-flow fact that can be linked with other equal facts.
 * Equality and hash-code operations must <i>not</i> take the linking data structures into account!
 * 
 * @deprecated Use {@link JoinHandlingNode} instead.
 */
@Deprecated
public interface LinkedNode<D> {
	/**
	 * Links this node to a neighbor node, i.e., to an abstraction that would have been merged
	 * with this one of paths were not being tracked.
	 */
	public void addNeighbor(D originalAbstraction);
	
	public void setCallingContext(D callingContext);
}