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

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;
import heros.fieldsens.FlowFunction.ConstrainedFact;
import heros.fieldsens.structs.FactAtStatement;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;
import heros.utilities.DefaultValueMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

class PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> {

	private static final Logger logger = LoggerFactory.getLogger(PerAccessPathMethodAnalyzer.class);
	private Fact sourceFact;
	private final AccessPath<Field> accessPath;
	private Map<WrappedFactAtStatement<Field,Fact, Stmt, Method>, WrappedFactAtStatement<Field,Fact, Stmt, Method>> reachableStatements = Maps.newHashMap();
	private List<WrappedFactAtStatement<Field, Fact, Stmt, Method>> summaries = Lists.newLinkedList();
	private Context<Field, Fact, Stmt, Method> context;
	private Method method;
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Field, Fact, Stmt, Method>> returnSiteResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ReturnSiteResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected ReturnSiteResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ReturnSiteResolver<Field, Fact, Stmt, Method>(context.factHandler, PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field, Fact, Stmt, Method>> ctrFlowJoinResolvers = new DefaultValueMap<FactAtStatement<Fact, Stmt>, ControlFlowJoinResolver<Field,Fact,Stmt,Method>>() {
		@Override
		protected ControlFlowJoinResolver<Field, Fact, Stmt, Method> createItem(FactAtStatement<Fact, Stmt> key) {
			return new ControlFlowJoinResolver<Field, Fact, Stmt, Method>(context.factHandler, PerAccessPathMethodAnalyzer.this, key.stmt);
		}
	};
	private CallEdgeResolver<Field, Fact, Stmt, Method> callEdgeResolver;
	private PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent;

	public PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context) {
		this(method, sourceFact, context, new AccessPath<Field>(), null);
	}
	
	private PerAccessPathMethodAnalyzer(Method method, Fact sourceFact, Context<Field, Fact, Stmt, Method> context, AccessPath<Field> accPath, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> parent) {
		if(method == null)
			throw new IllegalArgumentException("Method must be not null");
		this.parent = parent;
		this.method = method;
		this.sourceFact = sourceFact;
		this.accessPath = accPath;
		this.context = context;
		if(parent == null) {
			this.callEdgeResolver = isZeroSource() ? new ZeroCallEdgeResolver<Field, Fact, Stmt, Method>(this, context.zeroHandler) : new CallEdgeResolver<Field, Fact, Stmt, Method>(this);
		}
		else {
			this.callEdgeResolver = isZeroSource() ? parent.callEdgeResolver : new CallEdgeResolver<Field, Fact, Stmt, Method>(this, parent.callEdgeResolver);
		}
		log("initialized");
	}
	
	public PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> createWithAccessPath(AccessPath<Field> accPath) {
		return new PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>(method, sourceFact, context, accPath, this);
	}
	
	WrappedFact<Field, Fact, Stmt, Method> wrappedSource() {
		return new WrappedFact<Field, Fact, Stmt, Method>(sourceFact, accessPath, callEdgeResolver);
	}
	
	public AccessPath<Field> getAccessPath() {
		return accessPath;
	}

	private boolean isBootStrapped() {
		return callEdgeResolver.hasIncomingEdges() || !accessPath.isEmpty();
	}

	private void bootstrapAtMethodStartPoints() {
		callEdgeResolver.interest();
		for(Stmt startPoint : context.icfg.getStartPointsOf(method)) {
			WrappedFactAtStatement<Field, Fact, Stmt, Method> target = new WrappedFactAtStatement<Field, Fact, Stmt, Method>(startPoint, wrappedSource());
			if(!reachableStatements.containsKey(target))
				scheduleEdgeTo(target);
		}
	}
	
	public void addInitialSeed(Stmt stmt) {
		scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(stmt, wrappedSource()));
	}
	
	private void scheduleEdgeTo(Collection<Stmt> successors, WrappedFact<Field, Fact, Stmt, Method> fact) {
		for (Stmt stmt : successors) {
			scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(stmt, fact));
		}
	}

	void scheduleEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		assert context.icfg.getMethodOf(factAtStmt.getStatement()).equals(method);
		if (reachableStatements.containsKey(factAtStmt)) {
			log("Merging "+factAtStmt);
			context.factHandler.merge(reachableStatements.get(factAtStmt).getWrappedFact().getFact(), factAtStmt.getWrappedFact().getFact());
		} else {
			log("Edge to "+factAtStmt);
			reachableStatements.put(factAtStmt, factAtStmt);
			context.scheduler.schedule(new Job(factAtStmt));
		}
	}

	void log(String message) {
		logger.trace("[{}; {}{}: "+message+"]", method, sourceFact, accessPath);
	}

	@Override
	public String toString() {
		return method+"; "+sourceFact+accessPath;
	}

	void processCall(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		Collection<Method> calledMethods = context.icfg.getCalleesOfCallAt(factAtStmt.getStatement());
		for (Method calledMethod : calledMethods) {
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getCallFlowFunction(factAtStmt.getStatement(), calledMethod);
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts =  flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				MethodAnalyzer<Field, Fact, Stmt, Method> analyzer = context.getAnalyzer(calledMethod);
				analyzer.addIncomingEdge(new CallEdge<Field, Fact, Stmt, Method>(this,
						factAtStmt, targetFact.getFact()));
			}
		}
		
		processCallToReturnEdge(factAtStmt);
	}

	void processExit(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		log("New Summary: "+factAtStmt);
		if(!summaries.add(factAtStmt))
			throw new AssertionError();

		callEdgeResolver.applySummaries(factAtStmt);

		if(context.followReturnsPastSeeds && isZeroSource()) {
			Collection<Stmt> callSites = context.icfg.getCallersOf(method);
			for(Stmt callSite : callSites) {
				Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(callSite);
				for(Stmt returnSite : returnSites) {
					FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(callSite, method, factAtStmt.getStatement(), returnSite);
					Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
					for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
						//TODO handle constraint
						context.getAnalyzer(context.icfg.getMethodOf(callSite)).addUnbalancedReturnFlow(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()), callSite);
					}
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callSites.isEmpty()) {
				FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(null, method, factAtStmt.getStatement(), null);
				flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
			}
		}
	}
	
	private void processCallToReturnEdge(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();		
		if(numberOfPredecessors > 1 || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getWrappedFact());
		}
		else {
			processNonJoiningCallToReturnFlow(factAtStmt);
		}
	}

	private void processNonJoiningCallToReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(factAtStmt.getStatement());
		for(Stmt returnSite : returnSites) {
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getCallToReturnFlowFunction(factAtStmt.getStatement(), returnSite);
			Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
				//TODO handle constraint
				scheduleEdgeTo(new WrappedFactAtStatement<Field, Fact, Stmt, Method>(returnSite, targetFact.getFact()));
			}
		}
	}

	private void processNormalFlow(WrappedFactAtStatement<Field,Fact, Stmt, Method> factAtStmt) {
		Stmt stmt = factAtStmt.getStatement();
		int numberOfPredecessors = context.icfg.getPredsOf(stmt).size();
		if((numberOfPredecessors > 1 && !context.icfg.isExitStmt(stmt)) || (context.icfg.isStartPoint(stmt) && numberOfPredecessors > 0)) {
			ctrFlowJoinResolvers.getOrCreate(factAtStmt.getAsFactAtStatement()).addIncoming(factAtStmt.getWrappedFact());
		}
		else {
			processNormalNonJoiningFlow(factAtStmt);
		}
	}

	void processFlowFromJoinStmt(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		if(context.icfg.isCallStmt(factAtStmt.getStatement()))
			processNonJoiningCallToReturnFlow(factAtStmt);
		else
			processNormalNonJoiningFlow(factAtStmt);
	}

	private void processNormalNonJoiningFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
		final List<Stmt> successors = context.icfg.getSuccsOf(factAtStmt.getStatement());
		FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getNormalFlowFunction(factAtStmt.getStatement());
		Collection<ConstrainedFact<Field, Fact, Stmt, Method>> targetFacts = flowFunction.computeTargets(factAtStmt.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(factAtStmt.getAccessPath(), factAtStmt.getResolver()));
		for (final ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targetFacts) {
			if(targetFact.getConstraint() == null)
				scheduleEdgeTo(successors, targetFact.getFact());
			else {
				targetFact.getFact().getResolver().resolve(targetFact.getConstraint(), new InterestCallback<Field, Fact, Stmt, Method>() {
					@Override
					public void interest(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer,
							Resolver<Field, Fact, Stmt, Method> resolver) {
						analyzer.scheduleEdgeTo(successors, new WrappedFact<Field, Fact, Stmt, Method>(targetFact.getFact().getFact(), targetFact.getFact().getAccessPath(), resolver));
					}

					@Override
					public void canBeResolvedEmpty() {
						callEdgeResolver.resolve(targetFact.getConstraint(), this);
					}
				});
			}
		}
	}
	
	public void addIncomingEdge(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		if(isBootStrapped()) {
			context.factHandler.merge(sourceFact, incEdge.getCalleeSourceFact().getFact());
		} else 
			bootstrapAtMethodStartPoints();
		callEdgeResolver.addIncoming(incEdge);
	}

	void applySummary(CallEdge<Field, Fact, Stmt, Method> incEdge, WrappedFactAtStatement<Field, Fact, Stmt, Method> exitFact) {
		Collection<Stmt> returnSites = context.icfg.getReturnSitesOfCallAt(incEdge.getCallSite());
		for(Stmt returnSite : returnSites) {
			FlowFunction<Field, Fact, Stmt, Method> flowFunction = context.flowFunctions.getReturnFlowFunction(incEdge.getCallSite(), method, exitFact.getStatement(), returnSite);
			Set<ConstrainedFact<Field, Fact, Stmt, Method>> targets = flowFunction.computeTargets(exitFact.getFact(), new AccessPathHandler<Field, Fact, Stmt, Method>(exitFact.getAccessPath(), exitFact.getResolver()));
			for (ConstrainedFact<Field, Fact, Stmt, Method> targetFact : targets) {
				context.factHandler.restoreCallingContext(targetFact.getFact().getFact(), incEdge.getCallerCallSiteFact().getFact());
				//TODO handle constraint
				scheduleReturnEdge(incEdge, targetFact.getFact(), returnSite);
			}
		}
	}

	public void scheduleUnbalancedReturnEdgeTo(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact) {
		ReturnSiteResolver<Field,Fact,Stmt,Method> resolver = returnSiteResolvers.getOrCreate(fact.getAsFactAtStatement());
		resolver.addIncoming(new WrappedFact<Field, Fact, Stmt, Method>(fact.getWrappedFact().getFact(), fact.getWrappedFact().getAccessPath(), 
				fact.getWrappedFact().getResolver()), null, Delta.<Field>empty());
	}
	
	private void scheduleReturnEdge(CallEdge<Field, Fact, Stmt, Method> incEdge, WrappedFact<Field, Fact, Stmt, Method> fact, Stmt returnSite) {
		Delta<Field> delta = accessPath.getDeltaTo(incEdge.getCalleeSourceFact().getAccessPath());
		ReturnSiteResolver<Field, Fact, Stmt, Method> returnSiteResolver = incEdge.getCallerAnalyzer().returnSiteResolvers.getOrCreate(
				new FactAtStatement<Fact, Stmt>(fact.getFact(), returnSite));
		returnSiteResolver.addIncoming(fact, incEdge.getCalleeSourceFact().getResolver(), delta);
	}

	void applySummaries(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		for(WrappedFactAtStatement<Field, Fact, Stmt, Method> summary : summaries) {
			applySummary(incEdge, summary);
		}
	}
	
	public boolean isZeroSource() {
		return sourceFact.equals(context.zeroValue);
	}

	private class Job implements Runnable {

		private WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt;

		public Job(WrappedFactAtStatement<Field, Fact, Stmt, Method> factAtStmt) {
			this.factAtStmt = factAtStmt;
		}

		@Override
		public void run() {
			if (context.icfg.isCallStmt(factAtStmt.getStatement())) {
				processCall(factAtStmt);
			} else {
				if (context.icfg.isExitStmt(factAtStmt.getStatement())) {
					processExit(factAtStmt);
				}
				if (!context.icfg.getSuccsOf(factAtStmt.getStatement()).isEmpty()) {
					processNormalFlow(factAtStmt);
				}
			}
		}
	}

	public CallEdgeResolver<Field, Fact, Stmt, Method> getCallEdgeResolver() {
		return callEdgeResolver;
	}
	
//	public void debugReachables() {
//		JsonDocument root = new JsonDocument();
//		
//		for(WrappedFactAtStatement<Field, Fact, Stmt, Method> fact : reachableStatements.keySet()) {
//			JsonDocument doc = root.doc(fact.getStatement().toString()).doc(fact.getFact().toString()).doc(fact.getResolver().toString()).doc(String.valueOf(fact.hashCode()));
//			doc.keyValue("fact", String.valueOf(fact.getFact().hashCode()));
//			doc.keyValue("resolver", String.valueOf(fact.getResolver().hashCode()));
//			doc.keyValue("resolver-analyzer", String.valueOf(fact.getResolver().analyzer.hashCode()));
//			doc.keyValue("resolver-class", String.valueOf(fact.getResolver().getClass().toString()));
//		}
//		try {
//			FileWriter writer = new FileWriter("debug/reachables.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public void debugInterest() {
//		JsonDocument root = new JsonDocument();
//		
//		List<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> worklist = Lists.newLinkedList();
//		worklist.add(this);
//		Set<PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> visited = Sets.newHashSet();
//		
//		while(!worklist.isEmpty()) {
//			PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = worklist.remove(0);
//			if(!visited.add(current))
//				continue;
//			
//			JsonDocument currentMethodDoc = root.doc(current.method.toString()+ "___"+current.sourceFact);
//			JsonDocument currentDoc = currentMethodDoc.doc("accPath").doc("_"+current.accessPath.toString());
//			
//			for(CallEdge<Field, Fact, Stmt, Method> incEdge : current.getCallEdgeResolver().incomingEdges) {
//				currentDoc.doc("incoming").doc(incEdge.getCallerAnalyzer().method+"___"+incEdge.getCallerAnalyzer().sourceFact).doc("_"+incEdge.getCallerAnalyzer().accessPath.toString());
//				worklist.add(incEdge.getCallerAnalyzer());
//			}
//		}
//		
//		try {
//			FileWriter writer = new FileWriter("debug/incoming.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void debugNestings() {
//		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current = this;
//		while(current.parent != null)
//			current = current.parent;
//		
//		JsonDocument root = new JsonDocument();
//		debugNestings(current, root);
//		
//		try {
//			FileWriter writer = new FileWriter("debug/nestings.json");
//			StringBuilder builder = new StringBuilder();
//			builder.append("var root=");
//			root.write(builder, 0);
//			writer.write(builder.toString());
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void debugNestings(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> current, JsonDocument parentDoc) {
//		JsonDocument currentDoc = parentDoc.doc(current.accessPath.toString());
//		for(ResolverTemplate<Field, Fact, Stmt, Method, CallEdge<Field, Fact, Stmt, Method>> nestedAnalyzer : current.getCallEdgeResolver().nestedResolvers.values()) {
//			debugNestings(nestedAnalyzer.analyzer, currentDoc);
//		}
//	}
}
