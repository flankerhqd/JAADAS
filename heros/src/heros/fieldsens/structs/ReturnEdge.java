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
import heros.fieldsens.AccessPath.PrefixTestResult;

public class ReturnEdge<Field, Fact, Stmt, Method> {

	public final Fact incFact;
	public final Resolver<Field, Fact, Stmt, Method> resolverAtCaller;
	public final Delta<Field> callDelta;
	public final AccessPath<Field> incAccessPath;
	public final Resolver<Field, Fact, Stmt, Method> incResolver;
	public final Delta<Field> usedAccessPathOfIncResolver;

	public ReturnEdge(WrappedFact<Field, Fact, Stmt, Method> fact, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller,
			Delta<Field> callDelta) {
		this(fact.getFact(), fact.getAccessPath(), fact.getResolver(), resolverAtCaller, callDelta, Delta.<Field>empty());
	}
	
	private ReturnEdge(Fact incFact, 
			AccessPath<Field> incAccessPath, 
			Resolver<Field, Fact, Stmt, Method> incResolver, 
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, 
			Delta<Field> callDelta, 
			Delta<Field> usedAccessPathOfIncResolver) {
		this.incFact = incFact;
		this.incAccessPath = incAccessPath;
		this.incResolver = incResolver;
		this.resolverAtCaller = resolverAtCaller;
		this.callDelta = callDelta;
		this.usedAccessPathOfIncResolver = usedAccessPathOfIncResolver;
	}
	
	public ReturnEdge<Field, Fact, Stmt, Method> copyWithIncomingResolver(
			Resolver<Field, Fact, Stmt, Method> incResolver, Delta<Field> usedAccessPathOfIncResolver) {
		return new ReturnEdge<Field, Fact, Stmt, Method>(incFact, incAccessPath, incResolver, resolverAtCaller, callDelta, usedAccessPathOfIncResolver);
	}
	
	public ReturnEdge<Field, Fact, Stmt, Method> copyWithResolverAtCaller(
			Resolver<Field, Fact, Stmt, Method> resolverAtCaller, Delta<Field> usedAccessPathOfIncResolver) {
		return new ReturnEdge<Field, Fact, Stmt, Method>(incFact, incAccessPath, null, resolverAtCaller, callDelta, usedAccessPathOfIncResolver);
	}
	
	@Override
	public String toString() {
		return String.format("IncFact: %s%s, Delta: %s, IncResolver: <%s:%s>, ResolverAtCallSite: %s", incFact, incAccessPath, callDelta, usedAccessPathOfIncResolver, incResolver, resolverAtCaller);
	}
	
	
}