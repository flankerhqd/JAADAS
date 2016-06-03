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
package heros.fieldsens.structs;

import heros.fieldsens.AccessPath;
import heros.fieldsens.FlowFunction;
import heros.fieldsens.Resolver;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.FlowFunction.Constraint;

public class WrappedFact<Field, Fact, Stmt, Method>{

	private final Fact fact;
	private final AccessPath<Field> accessPath;
	private final Resolver<Field, Fact, Stmt, Method> resolver;
	
	public WrappedFact(Fact fact, AccessPath<Field> accessPath, Resolver<Field, Fact, Stmt, Method> resolver) {
		assert fact != null;
		assert accessPath != null;
		assert resolver != null;
		
		this.fact = fact;
		this.accessPath = accessPath;
		this.resolver = resolver;
	}
	
	public Fact getFact() {
		return fact;
	}
	
	public WrappedFact<Field, Fact, Stmt, Method> applyDelta(AccessPath.Delta<Field> delta) {
		return new WrappedFact<Field, Fact, Stmt, Method>(fact, delta.applyTo(accessPath), resolver); //TODO keep resolver?
	}

	public AccessPath<Field> getAccessPath() {
		return accessPath;
	}
	
	public WrappedFact<Field, Fact, Stmt, Method> applyConstraint(Constraint<Field> constraint, Fact zeroValue) {
		if(fact.equals(zeroValue))
			return this;
		else
			return new WrappedFact<Field, Fact, Stmt, Method>(fact, constraint.applyToAccessPath(accessPath), resolver);
	}
	
	@Override
	public String toString() {
		String result = fact.toString()+accessPath;
		if(resolver != null)
			result+=resolver.toString();
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((resolver == null) ? 0 : resolver.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WrappedFact other = (WrappedFact) obj;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		if (resolver == null) {
			if (other.resolver != null)
				return false;
		} else if (!resolver.equals(other.resolver))
			return false;
		return true;
	}

	public Resolver<Field, Fact, Stmt, Method> getResolver() {
		return resolver;
	}
	
	
}
