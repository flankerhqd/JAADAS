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
package heros.edgefunc;

import heros.EdgeFunction;

/**
 * The identity function on graph edges
 * @param <V> The type of values to be computed along flow edges.
 */
public class EdgeIdentity<V> implements EdgeFunction<V> {
	
	@SuppressWarnings("rawtypes")
	private final static EdgeIdentity instance = new EdgeIdentity();
	
	private EdgeIdentity(){} //use v() instead

	public V computeTarget(V source) {
		return source;
	}

	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		return secondFunction;
	}

	public EdgeFunction<V> joinWith(EdgeFunction<V> otherFunction) {
		if(otherFunction == this || otherFunction.equalTo(this)) return this;
		if(otherFunction instanceof AllBottom) {
			return otherFunction;
		}
		if(otherFunction instanceof AllTop) {
			return this;
		}
		//do not know how to join; hence ask other function to decide on this
		return otherFunction.joinWith(this);
	}
	
	public boolean equalTo(EdgeFunction<V> other) {
		//singleton
		return other==this;
	}

	@SuppressWarnings("unchecked")
	public static <A> EdgeIdentity<A> v() {
		return instance;
	}

	public String toString() {
		return "id";
	}


}
