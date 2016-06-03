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
package heros;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unmodifiable set holding exactly two elements. Particularly useful within flow functions.
 *
 * @param <E>
 * @see FlowFunction
 */
public class TwoElementSet<E> extends AbstractSet<E> {
	
	protected final E first, second;
	
	public TwoElementSet(E first, E second) {
		this.first = first;
		this.second = second;
	}	
	
	public static <E> TwoElementSet<E> twoElementSet(E first, E second) {
		return new TwoElementSet<E>(first, second);
	}
	
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {		
			int elementsRead = 0;
			
			@Override
			public boolean hasNext() {
				return elementsRead<2;
			}

			@Override
			public E next() {
				switch(elementsRead) {
				case 0:
					elementsRead++;
					return first;
				case 1:
					elementsRead++;
					return second;
				default:
					throw new NoSuchElementException();	
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size() {
		return 2;
	}	
}
