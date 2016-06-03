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
import heros.fieldsens.Resolver;
import heros.fieldsens.AccessPath.Delta;


public class WrappedFactAtStatement<Field, Fact, Stmt, Method> {

	private WrappedFact<Field,Fact, Stmt, Method> fact;
	private Stmt stmt;

	public WrappedFactAtStatement(Stmt stmt, WrappedFact<Field, Fact, Stmt, Method> fact) {
		this.stmt = stmt;
		this.fact = fact;
	}

	public WrappedFact<Field,Fact, Stmt, Method> getWrappedFact() {
		return fact;
	}
	
	public Fact getFact() {
		return fact.getFact();
	}
	
	public AccessPath<Field> getAccessPath() {
		return fact.getAccessPath();
	}
	
	public Resolver<Field, Fact, Stmt, Method> getResolver() {
		return fact.getResolver();
	}

	public Stmt getStatement() {
		return stmt;
	}
	
	public FactAtStatement<Fact, Stmt> getAsFactAtStatement() {
		return new FactAtStatement<Fact, Stmt>(fact.getFact(), stmt);
	}
	
	public boolean canDeltaBeApplied(AccessPath.Delta<Field> delta) {
		return delta.canBeAppliedTo(fact.getAccessPath());
	}
	
	@Override
	public String toString() {
		return fact+" @ "+stmt;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fact == null) ? 0 : fact.hashCode());
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
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
		WrappedFactAtStatement other = (WrappedFactAtStatement) obj;
		if (fact == null) {
			if (other.fact != null)
				return false;
		} else if (!fact.equals(other.fact))
			return false;
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		return true;
	}

}
