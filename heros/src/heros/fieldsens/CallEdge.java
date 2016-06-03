/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.fieldsens;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;

public class CallEdge<Field, Fact, Stmt, Method> {

	private WrappedFact<Field, Fact, Stmt, Method> calleeSourceFact;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> callerAnalyzer;
	private WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtCallSite;
	
	public CallEdge(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> callerAnalyzer, 
			WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtCallSite,
			WrappedFact<Field, Fact, Stmt, Method> calleeSourceFact) {
		this.callerAnalyzer = callerAnalyzer;
		this.factAtCallSite = factAtCallSite;
		this.calleeSourceFact = calleeSourceFact;
	}
	
	public WrappedFact<Field, Fact, Stmt, Method> getCalleeSourceFact() {
		return calleeSourceFact;
	}
	
	public WrappedFact<Field, Fact, Stmt, Method> getCallerCallSiteFact() {
		return factAtCallSite.getWrappedFact();
	}
	
	public WrappedFact<Field, Fact, Stmt, Method> getCallerSourceFact() {
		return callerAnalyzer.wrappedSource();
	}
	
	public Stmt getCallSite() {
		return factAtCallSite.getStatement();
	}
	
	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> getCallerAnalyzer() {
		return callerAnalyzer;
	}
	
	public void registerInterestCallback(final PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> interestedAnalyzer) {
		final Delta<Field> delta = calleeSourceFact.getAccessPath().getDeltaTo(interestedAnalyzer.getAccessPath());
		
		if(!factAtCallSite.canDeltaBeApplied(delta))
			return;
		
		factAtCallSite.getWrappedFact().getResolver().resolve(new DeltaConstraint<Field>(delta), new InterestCallback<Field, Fact, Stmt, Method>() {
			
			@Override
			public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer, Resolver<Field, Fact, Stmt, Method> resolver) {
				WrappedFact<Field, Fact, Stmt, Method> calleeSourceFactWithDelta = new WrappedFact<Field, Fact, Stmt, Method>(calleeSourceFact.getFact(), delta.applyTo(calleeSourceFact.getAccessPath()), resolver);
				if(interestedAnalyzer.getAccessPath().isPrefixOf(calleeSourceFactWithDelta.getAccessPath()) != PrefixTestResult.GUARANTEED_PREFIX)
					throw new AssertionError();
				interestedAnalyzer.addIncomingEdge(new CallEdge<Field, Fact, Stmt, Method>(analyzer, 
						new WrappedFactAtStatement<Field, Fact, Stmt, Method>(factAtCallSite.getStatement(), 
											new WrappedFact<Field, Fact, Stmt, Method>(factAtCallSite.getWrappedFact().getFact(), 
													delta.applyTo(factAtCallSite.getWrappedFact().getAccessPath()), 
													resolver)), 
						calleeSourceFactWithDelta));
			}
			
			@Override
			public void canBeResolvedEmpty() {
				callerAnalyzer.getCallEdgeResolver().resolve(new DeltaConstraint<Field>(delta), this);
			}
		});
	}
	
	@Override
	public String toString() {
		return "[IncEdge CSite:"+getCallSite()+", Caller-Edge: "+getCallerSourceFact()+"->"+getCallerCallSiteFact()+",  CalleeFact: "+calleeSourceFact+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calleeSourceFact == null) ? 0 : calleeSourceFact.hashCode());
		result = prime * result + ((callerAnalyzer == null) ? 0 : callerAnalyzer.hashCode());
		result = prime * result + ((factAtCallSite == null) ? 0 : factAtCallSite.hashCode());
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
		CallEdge other = (CallEdge) obj;
		if (calleeSourceFact == null) {
			if (other.calleeSourceFact != null)
				return false;
		} else if (!calleeSourceFact.equals(other.calleeSourceFact))
			return false;
		if (callerAnalyzer == null) {
			if (other.callerAnalyzer != null)
				return false;
		} else if (!callerAnalyzer.equals(other.callerAnalyzer))
			return false;
		if (factAtCallSite == null) {
			if (other.factAtCallSite != null)
				return false;
		} else if (!factAtCallSite.equals(other.factAtCallSite))
			return false;
		return true;
	}
	
	
}
