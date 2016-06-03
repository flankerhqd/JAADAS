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
package heros.solver;

//copied from soot.toolkits.scalar
public class Pair<T, U> {
	protected T o1;
	protected U o2;
	
	protected int hashCode = 0;

	public Pair() {
		o1 = null;
		o2 = null;
	}

	public Pair(T o1, U o2) {
		this.o1 = o1;
		this.o2 = o2;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
		result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
		hashCode = result;
		
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Pair other = (Pair) obj;
		if (o1 == null) {
			if (other.o1 != null)
				return false;
		} else if (!o1.equals(other.o1))
			return false;
		if (o2 == null) {
			if (other.o2 != null)
				return false;
		} else if (!o2.equals(other.o2))
			return false;
		return true;
	}

	public String toString() {
		return "Pair " + o1 + "," + o2;
	}

	public T getO1() {
		return o1;
	}

	public U getO2() {
		return o2;
	}

	public void setO1(T no1) {
		o1 = no1;
		hashCode = 0;
	}

	public void setO2(U no2) {
		o2 = no2;
		hashCode = 0;
	}

	public void setPair(T no1, U no2) {
		o1 = no1;
		o2 = no2;
		hashCode = 0;
	}

}
