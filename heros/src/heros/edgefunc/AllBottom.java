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


public class AllBottom<V> implements EdgeFunction<V> {
	
	private final V bottomElement;

	public AllBottom(V bottomElement){
		this.bottomElement = bottomElement;
	} 

	public V computeTarget(V source) {
		return bottomElement;
	}

	public EdgeFunction<V> composeWith(EdgeFunction<V> secondFunction) {
		if (secondFunction instanceof EdgeIdentity)
			return this;
		return secondFunction;
	}

	public EdgeFunction<V> joinWith(EdgeFunction<V> otherFunction) {
		if(otherFunction == this || otherFunction.equalTo(this)) return this;
		if(otherFunction instanceof AllTop) {
			return this;
		}
		if(otherFunction instanceof EdgeIdentity) {
			return this;
		}
		throw new IllegalStateException("unexpected edge function: "+otherFunction);
	}

	public boolean equalTo(EdgeFunction<V> other) {
		if(other instanceof AllBottom) {
			@SuppressWarnings("rawtypes")
			AllBottom allBottom = (AllBottom) other;
			return allBottom.bottomElement.equals(bottomElement);
		}		
		return false;
	}
	
	public String toString() {
		return "allbottom";
	}

}
