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

import heros.fieldsens.structs.WrappedFactAtStatement;

import com.google.common.collect.Lists;


class CallEdgeResolver<Field, Fact, Stmt, Method> extends ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>>  {

	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		this(analyzer, null);
	}
	
	public CallEdgeResolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, CallEdgeResolver<Field, Fact, Stmt, Method> parent) {
		super(analyzer, parent);
	}

	@Override
	protected AccessPath<Field> getResolvedAccessPath() {
		return analyzer.getAccessPath();
	}
	
	@Override
	protected AccessPath<Field> getAccessPathOf(CallEdge<Field, Fact, Stmt, Method> inc) {
		return inc.getCalleeSourceFact().getAccessPath();
	}
	
	@Override
	protected void processIncomingGuaranteedPrefix(CallEdge<Field, Fact, Stmt, Method> inc) {
		analyzer.applySummaries(inc);
	}
	
	@Override
	protected void processIncomingPotentialPrefix(CallEdge<Field, Fact, Stmt, Method> inc) {
		lock();
		inc.registerInterestCallback(analyzer);
		unlock();
	}

	@Override
	protected ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>> createNestedResolver(AccessPath<Field> newAccPath) {
		return analyzer.createWithAccessPath(newAccPath).getCallEdgeResolver();
	}
	
	public void applySummaries(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		for(CallEdge<Field, Fact, Stmt, Method> incEdge : Lists.newLinkedList(incomingEdges)) {
			analyzer.applySummary(incEdge, factAtStmt);
		}
	}
	
	@Override
	public String toString() {
		return "";
	}
	
	@Override
	protected void log(String message) {
		analyzer.log(message);
	}

	public boolean hasIncomingEdges() {
		return !incomingEdges.isEmpty();
	}


}