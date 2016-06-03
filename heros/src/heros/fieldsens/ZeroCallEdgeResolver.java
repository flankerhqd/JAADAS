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
package heros.fieldsens;

import heros.fieldsens.FlowFunction.Constraint;

public class ZeroCallEdgeResolver<Field, Fact, Stmt, Method> extends CallEdgeResolver<Field, Fact, Stmt, Method> {

	private ZeroHandler<Field> zeroHandler;

	public ZeroCallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, ZeroHandler<Field> zeroHandler) {
		super(analyzer);
		this.zeroHandler = zeroHandler;
	}

	@Override
	public void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(zeroHandler.shouldGenerateAccessPath(constraint.applyToAccessPath(new AccessPath<Field>())))
			callback.interest(analyzer, this);
	}
	
	@Override
	public void interest() {
	}
	
	@Override
	protected ZeroCallEdgeResolver<Field, Fact, Stmt, Method> getOrCreateNestedResolver(AccessPath<Field> newAccPath) {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((zeroHandler == null) ? 0 : zeroHandler.hashCode());
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
		ZeroCallEdgeResolver other = (ZeroCallEdgeResolver) obj;
		if (zeroHandler == null) {
			if (other.zeroHandler != null)
				return false;
		} else if (!zeroHandler.equals(other.zeroHandler))
			return false;
		return true;
	}
}
