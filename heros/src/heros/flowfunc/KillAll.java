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
package heros.flowfunc;

import static java.util.Collections.emptySet;
import heros.FlowFunction;

import java.util.Set;



/**
 * The empty function, i.e. a function which returns an empty set for all points
 * in the definition space.
 *  
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public class KillAll<D> implements FlowFunction<D> {
	
	@SuppressWarnings("rawtypes")
	private final static KillAll instance = new KillAll();
	
	private KillAll(){} //use v() instead

	public Set<D> computeTargets(D source) {
		return emptySet();
	}
	
	@SuppressWarnings("unchecked")
	public static <D> KillAll<D> v() {
		return instance;
	}

}
