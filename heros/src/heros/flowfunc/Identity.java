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

import static java.util.Collections.singleton;
import heros.FlowFunction;

import java.util.Set;



public class Identity<D> implements FlowFunction<D> {
	
	@SuppressWarnings("rawtypes")
	private final static Identity instance = new Identity();
	
	private Identity(){} //use v() instead

	public Set<D> computeTargets(D source) {
		return singleton(source);
	}

	@SuppressWarnings("unchecked")
	public static <D> Identity<D> v() {
		return instance;
	}

}
