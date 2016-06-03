/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * Copyright (c) 2013 Tata Consultancy Services & Ecole Polytechnique de Montreal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 *     Marc-Andre Laverdiere-Papineau - Fixed race condition
 *     Steven Arzt - Created FastSolver implementation
 ******************************************************************************/
package soot.jimple.infoflow.solver.fastSolver;


import heros.DontSynchronize;
import heros.FlowFunction;
import heros.FlowFunctionCache;
import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.SynchronizedBy;
import heros.ZeroedFlowFunctions;
import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;
import heros.solver.PathEdge;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import com.google.common.cache.CacheBuilder;


/**
 * A solver for an {@link IFDSTabulationProblem}. This solver is not based on the IDESolver
 * implementation in Heros for performance reasons.
 * 
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods. Typically {@link SootMethod}.
 * @param <I> The type of inter-procedural control-flow graph being used.
 * @see IFDSTabulationProblem
 */
public class IFDSSolver<N,D extends FastSolverLinkedNode<D, N>,M,I extends BiDiInterproceduralCFG<N, M>> {
	
	public static CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER = CacheBuilder.newBuilder().concurrencyLevel
			(Runtime.getRuntime().availableProcessors()).initialCapacity(10000).softValues();
	
    protected static final Logger logger = LoggerFactory.getLogger(IFDSSolver.class);

    //enable with -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
    public static final boolean DEBUG = logger.isDebugEnabled();

	protected CountingThreadPoolExecutor executor;
	
	@DontSynchronize("only used by single thread")
	protected int numThreads;
	
	@SynchronizedBy("thread safe data structure, consistent locking when used")
	protected final JumpFunctions<N,D> jumpFn;
	
	@SynchronizedBy("thread safe data structure, only modified internally")
	protected final I icfg;
	
	//stores summaries that were queried before they were computed
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on 'incoming'")
	protected final MyConcurrentHashMap<Pair<M,D>,Set<Pair<N,D>>> endSummary =
			new MyConcurrentHashMap<Pair<M,D>, Set<Pair<N,D>>>();
	
	//edges going along calls
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final MyConcurrentHashMap<Pair<M,D>,MyConcurrentHashMap<N,Map<D, D>>> incoming =
			new MyConcurrentHashMap<Pair<M,D>,MyConcurrentHashMap<N,Map<D, D>>>();
	
	@DontSynchronize("stateless")
	protected final FlowFunctions<N, D, M> flowFunctions;
	
	@DontSynchronize("only used by single thread")
	protected final Map<N,Set<D>> initialSeeds;
	
	@DontSynchronize("benign races")
	public long propagationCount;
	
	@DontSynchronize("stateless")
	protected final D zeroValue;
	
	@DontSynchronize("readOnly")
	protected final FlowFunctionCache<N,D,M> ffCache; 
	
	@DontSynchronize("readOnly")
	protected final boolean followReturnsPastSeeds;
	
	@DontSynchronize("readOnly")
	private boolean setJumpPredecessors = false;
	
	@DontSynchronize("readOnly")
	private boolean enableMergePointChecking = false;
	
	/**
	 * Creates a solver for the given problem, which caches flow functions and edge functions.
	 * The solver must then be started by calling {@link #solve()}.
	 */
	public IFDSSolver(IFDSTabulationProblem<N,D,M,I> tabulationProblem) {
		this(tabulationProblem, DEFAULT_CACHE_BUILDER);
	}

	/**
	 * Creates a solver for the given problem, constructing caches with the
	 * given {@link CacheBuilder}. The solver must then be started by calling
	 * {@link #solve()}.
	 * @param tabulationProblem The tabulation problem to solve
	 * @param flowFunctionCacheBuilder A valid {@link CacheBuilder} or
	 * <code>null</code> if no caching is to be used for flow functions.
	 */
	public IFDSSolver(IFDSTabulationProblem<N,D,M,I> tabulationProblem,
			@SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder) {
		if(logger.isDebugEnabled())
			flowFunctionCacheBuilder = flowFunctionCacheBuilder.recordStats();
		this.zeroValue = tabulationProblem.zeroValue();
		this.icfg = tabulationProblem.interproceduralCFG();		
		FlowFunctions<N, D, M> flowFunctions = tabulationProblem.autoAddZero() ?
				new ZeroedFlowFunctions<N,D,M>(tabulationProblem.flowFunctions(), zeroValue) : tabulationProblem.flowFunctions(); 
		if(flowFunctionCacheBuilder!=null) {
			ffCache = new FlowFunctionCache<N,D,M>(flowFunctions, flowFunctionCacheBuilder);
			flowFunctions = ffCache;
		} else {
			ffCache = null;
		}
		this.flowFunctions = flowFunctions;
		this.initialSeeds = tabulationProblem.initialSeeds();
		this.jumpFn = new JumpFunctions<N,D>();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.numThreads = Math.max(1,tabulationProblem.numThreads());
		this.executor = getExecutor();
	}
	
	/**
	 * Runs the solver on the configured problem. This can take some time.
	 */
	public void solve() {		
		submitInitialSeeds();
		awaitCompletionComputeValuesAndShutdown();
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis.
	 * Clients should only call this methods if performing synchronization on
	 * their own. Normally, {@link #solve()} should be called instead.
	 */
	protected void submitInitialSeeds() {
		for(Entry<N, Set<D>> seed: initialSeeds.entrySet()) {
			N startPoint = seed.getKey();
			for(D val: seed.getValue())
				propagate(zeroValue, startPoint, val, null, false);
			jumpFn.addFunction(new PathEdge<N, D>(zeroValue, startPoint, zeroValue));
		}
	}

	/**
	 * Awaits the completion of the exploded super graph. When complete, computes result values,
	 * shuts down the executor and returns.
	 */
	protected void awaitCompletionComputeValuesAndShutdown() {
		{
			//run executor and await termination of tasks
			runExecutorAndAwaitCompletion();
		}
		if(logger.isDebugEnabled())
			printStats();

		//ask executor to shut down;
		//this will cause new submissions to the executor to be rejected,
		//but at this point all tasks should have completed anyway
		executor.shutdown();
		
		// Wait for the executor to be really gone
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Runs execution, re-throwing exceptions that might be thrown during its execution.
	 */
	private void runExecutorAndAwaitCompletion() {
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Throwable exception = executor.getException();
		if(exception!=null) {
			throw new RuntimeException("There were exceptions during IFDS analysis. Exiting.",exception);
		}
	}

    /**
     * Dispatch the processing of a given edge. It may be executed in a different thread.
     * @param edge the edge to process
     */
    protected void scheduleEdgeProcessing(PathEdge<N,D> edge){
    	// If the executor has been killed, there is little point
    	// in submitting new tasks
    	if (executor.isTerminating())
    		return;
    	executor.execute(new PathEdgeProcessingTask(edge));
    	propagationCount++;
    }
	
	/**
	 * Lines 13-20 of the algorithm; processing a call site in the caller's context.
	 * 
	 * For each possible callee, registers incoming call edges.
	 * Also propagates call-to-return flows and summarized callee flows within the caller. 
	 * 
	 * @param edge an edge whose target node resembles a method call
	 */
	private void processCall(PathEdge<N,D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); // a call node; line 14...

        logger.trace("Processing call to {}", n);

		final D d2 = edge.factAtTarget();
		assert d2 != null;
		Collection<N> returnSiteNs = icfg.getReturnSitesOfCallAt(n);
		
		//for each possible callee
		Collection<M> callees = icfg.getCalleesOfCallAt(n);
		for(M sCalledProcN: callees) { //still line 14
			//compute the call-flow function
			FlowFunction<D> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
			Set<D> res = computeCallFlowFunction(function, d1, d2);
			
			Collection<N> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
			//for each result node of the call-flow function
			for(D d3: res) {
				compactAbstractionChain(d3, d2);
				
				//for each callee's start point(s)
				for(N sP: startPointsOf) {
					//create initial self-loop
					propagate(d3, sP, d3, n, false); //line 15
				}
				
				//register the fact that <sp,d3> has an incoming edge from <n,d2>
				//line 15.1 of Naeem/Lhotak/Rodriguez
				if (!addIncoming(sCalledProcN,d3,n,d1,d2))
					continue;
				
				//line 15.2
				Set<Pair<N, D>> endSumm = endSummary(sCalledProcN, d3);
					
				//still line 15.2 of Naeem/Lhotak/Rodriguez
				//for each already-queried exit value <eP,d4> reachable from <sP,d3>,
				//create new caller-side jump functions to the return sites
				//because we have observed a potentially new incoming edge into <sP,d3>
				if (endSumm != null)
					for(Pair<N, D> entry: endSumm) {
						N eP = entry.getO1();
						D d4 = entry.getO2();
						//for each return site
						for(N retSiteN: returnSiteNs) {
							//compute return-flow function
							FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
							//for each target value of the function
							for(D d5: computeReturnFlowFunction(retFunction, d3, d4, n, Collections.singleton(d1))) {
								// If we have not changed anything in the callee, we do not need the facts
								// from there. Even if we change something: If we don't need the concrete
								// path, we can skip the callee in the predecessor chain
								D d5p = d5;
								if (d5.equals(d2))
									d5p = d2;
								else if (setJumpPredecessors)
									d5p.setPredecessor(d3);
								propagate(d1, retSiteN, d5p, n, false);
							}
						}
					}
			}
		}
		//line 17-19 of Naeem/Lhotak/Rodriguez		
		//process intra-procedural flows along call-to-return flow functions
		for (N returnSiteN : returnSiteNs) {
			FlowFunction<D> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, returnSiteN);
			for(D d3: computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2)) {
				compactAbstractionChain(d3, d2);
				propagate(d1, returnSiteN, d3, n, false);
			}
		}
	}
	
	/**
	 * Checks whether the new abstraction is the direct successor of the
	 * original abstraction given to the flow function. If not, the chain
	 * of abstractions is compacted to length 1.
	 * @param d3 The new abstraction generated by the flow function 
	 * @param d2 The predecessor based on which the new abstraction was
	 * generated
	 */
	private void compactAbstractionChain(D d3, D d2) {
		// If the flow function gave us a chain of abstractions, we can
		// compact it
		if (d3 != d2) {
			D pred = d3.getPredecessor();
			if (pred != null && pred != d2)
				d3.setPredecessor(d2);
		}
	}

	/**
	 * Computes the call flow function for the given call-site abstraction
	 * @param callFlowFunction The call flow function to compute
	 * @param d1 The abstraction at the current method's start node.
	 * @param d2 The abstraction at the call site
	 * @return The set of caller-side abstractions at the callee's start node
	 */
	protected Set<D> computeCallFlowFunction
			(FlowFunction<D> callFlowFunction, D d1, D d2) {
		return callFlowFunction.computeTargets(d2);
	}

	/**
	 * Computes the call-to-return flow function for the given call-site
	 * abstraction
	 * @param callToReturnFlowFunction The call-to-return flow function to
	 * compute
	 * @param d1 The abstraction at the current method's start node.
	 * @param d2 The abstraction at the call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<D> computeCallToReturnFlowFunction
			(FlowFunction<D> callToReturnFlowFunction, D d1, D d2) {
		return callToReturnFlowFunction.computeTargets(d2);
	}
	
	/**
	 * Lines 21-32 of the algorithm.
	 * 
	 * Stores callee-side summaries.
	 * Also, at the side of the caller, propagates intra-procedural flows to return sites
	 * using those newly computed summaries.
	 * 
	 * @param edge an edge whose target node resembles a method exits
	 */
	protected void processExit(PathEdge<N,D> edge) {
		final N n = edge.getTarget(); // an exit node; line 21...
		M methodThatNeedsSummary = icfg.getMethodOf(n);
		
		final D d1 = edge.factAtSource();
		final D d2 = edge.factAtTarget();
		
		//for each of the method's start points, determine incoming calls
		
		//line 21.1 of Naeem/Lhotak/Rodriguez
		//register end-summary
		if (!addEndSummary(methodThatNeedsSummary, d1, n, d2))
			return;
		Map<N,Map<D, D>> inc = incoming(d1, methodThatNeedsSummary);
		
		//for each incoming call edge already processed
		//(see processCall(..))
		if (inc != null)
			for (Entry<N,Map<D, D>> entry: inc.entrySet()) {
				//line 22
				N c = entry.getKey();
				Set<D> callerSideDs = entry.getValue().keySet();
				//for each return site
				for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
					//compute return-flow function
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
					Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c, callerSideDs);
					//for each incoming-call value
					for(Entry<D, D> d1d2entry : entry.getValue().entrySet()) {
						final D d4 = d1d2entry.getKey();
						final D predVal = d1d2entry.getValue();
						
						for(D d5: targets) {
							compactAbstractionChain(d5, d2);
							
							// If we have not changed anything in the callee, we do not need the facts
							// from there. Even if we change something: If we don't need the concrete
							// path, we can skip the callee in the predecessor chain
							D d5p = d5;
							if (d5.equals(predVal))
								d5p = predVal;
							else if (setJumpPredecessors)
								d5p.setPredecessor(d1);
							propagate(d4, retSiteC, d5p, c, false);
						}
					}
				}
			}
		
		//handling for unbalanced problems where we return out of a method with a fact for which we have no incoming flow
		//note: we propagate that way only values that originate from ZERO, as conditionally generated values should only
		//be propagated into callers that have an incoming edge for this condition
		if(followReturnsPastSeeds && d1 == zeroValue && (inc == null || inc.isEmpty())) {
			Collection<N> callers = icfg.getCallersOf(methodThatNeedsSummary);
			for(N c: callers) {
				for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
					Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c, Collections.singleton(zeroValue));
					for(D d5: targets) {
						compactAbstractionChain(d5, d2);
						propagate(zeroValue, retSiteC, d5, c, true);
					}
				}
			}
			//in cases where there are no callers, the return statement would normally not be processed at all;
			//this might be undesirable if the flow function has a side effect such as registering a taint;
			//instead we thus call the return flow function will a null caller
			if(callers.isEmpty()) {
				FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary,n,null);
				retFunction.computeTargets(d2);
			}
		}
	}
	
	/**
	 * Computes the return flow function for the given set of caller-side
	 * abstractions.
	 * @param retFunction The return flow function to compute
	 * @param d1 The abstraction at the beginning of the callee
	 * @param d2 The abstraction at the exit node in the callee
	 * @param callSite The call site
	 * @param callerSideDs The abstractions at the call site
	 * @return The set of caller-side abstractions at the return site
	 */
	protected Set<D> computeReturnFlowFunction
			(FlowFunction<D> retFunction, D d1, D d2, N callSite, Collection<D> callerSideDs) {
		return retFunction.computeTargets(d2);
	}

	/**
	 * Lines 33-37 of the algorithm.
	 * Simply propagate normal, intra-procedural flows.
	 * @param edge
	 */
	private void processNormalFlow(PathEdge<N,D> edge) {
		final D d1 = edge.factAtSource();
		final N n = edge.getTarget(); 
		final D d2 = edge.factAtTarget();
		
		for (N m : icfg.getSuccsOf(n)) {
			FlowFunction<D> flowFunction = flowFunctions.getNormalFlowFunction(n,m);
			Set<D> res = computeNormalFlowFunction(flowFunction, d1, d2);
			for (D d3 : res) {
				compactAbstractionChain(d3, d2);
				propagate(d1, m, d3, null, false);
			}
		}
	}
	
	/**
	 * Computes the normal flow function for the given set of start and end
	 * abstractions.
	 * @param flowFunction The normal flow function to compute
	 * @param d1 The abstraction at the method's start node
	 * @param d2 The abstraction at the current node
	 * @return The set of abstractions at the successor node
	 */
	protected Set<D> computeNormalFlowFunction
			(FlowFunction<D> flowFunction, D d1, D d2) {
		return flowFunction.computeTargets(d2);
	}
	
	/**
	 * Propagates the flow further down the exploded super graph. 
	 * @param sourceVal the source value of the propagated summary edge
	 * @param target the target statement
	 * @param targetVal the target value at the target statement
	 * @param relatedCallSite for call and return flows the related call statement, <code>null</code> otherwise
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver}) 
	 * @param isUnbalancedReturn <code>true</code> if this edge is propagating an unbalanced return
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver}) 
	 */
	protected void propagate(D sourceVal, N target, D targetVal,
		/* deliberately exposed to clients */ N relatedCallSite,
		/* deliberately exposed to clients */ boolean isUnbalancedReturn) {
		propagate(sourceVal, target, targetVal, relatedCallSite, isUnbalancedReturn, false);
	}
	
	/**
	 * Propagates the flow further down the exploded super graph. 
	 * @param sourceVal the source value of the propagated summary edge
	 * @param target the target statement
	 * @param targetVal the target value at the target statement
	 * @param relatedCallSite for call and return flows the related call statement, <code>null</code> otherwise
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver}) 
	 * @param isUnbalancedReturn <code>true</code> if this edge is propagating an unbalanced return
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IFDSSolver})
	 * @param forceRegister True if the jump function must always be registered with jumpFn .
	 * 		  This can happen when externally injecting edges that don't come out of this
	 * 		  solver.
	 */
	protected void propagate(D sourceVal, N target, D targetVal,
			/* deliberately exposed to clients */ N relatedCallSite,
			/* deliberately exposed to clients */ boolean isUnbalancedReturn,
			boolean forceRegister) {
		final PathEdge<N,D> edge = new PathEdge<N,D>(sourceVal, target, targetVal);
		final D existingVal = (forceRegister || !enableMergePointChecking || isMergePoint(target)) ?
				jumpFn.addFunction(edge) : null;
		if (existingVal != null) {
			if (existingVal != targetVal)
				existingVal.addNeighbor(targetVal);
		}
		else {
			scheduleEdgeProcessing(edge);
			if(targetVal!=zeroValue)
				logger.trace("EDGE: <{},{}> -> <{},{}>", icfg.getMethodOf(target), sourceVal, target, targetVal);
		}
	}
	
	/**
	 * Gets whether the given unit is a merge point in the ICFG
	 * @param target The unit to check
	 * @return True if the given unit is a merge point in the ICFG, otherwise
	 * false
	 */
	private boolean isMergePoint(N target) {
		if (icfg.isStartPoint(target))
			return true;
		
		List<N> preds = icfg.getPredsOf(target);
		int size = preds.size();
		if (size > 1)
			return true;
		if (size > 0)
			for (N pred : preds)
				if (icfg.isCallStmt(pred))
					return true;
		
		return false;
	}

	protected Set<Pair<N, D>> endSummary(M m, D d3) {
		Set<Pair<N, D>> map = endSummary.get(new Pair<M, D>(m, d3));
		return map;
	}

	private boolean addEndSummary(M m, D d1, N eP, D d2) {
		if (d1 == zeroValue)
			return true;
		
		Set<Pair<N, D>> summaries = endSummary.putIfAbsentElseGet
				(new Pair<M, D>(m, d1), new ConcurrentHashSet<Pair<N, D>>());
		return summaries.add(new Pair<N, D>(eP, d2));
	}	
	
	protected Map<N, Map<D, D>> incoming(D d1, M m) {
		Map<N, Map<D, D>> map = incoming.get(new Pair<M, D>(m, d1));
		return map;
	}
	
	protected boolean addIncoming(M m, D d3, N n, D d1, D d2) {
		MyConcurrentHashMap<N, Map<D, D>> summaries = incoming.putIfAbsentElseGet
				(new Pair<M, D>(m, d3), new MyConcurrentHashMap<N, Map<D, D>>());
		Map<D, D> set = summaries.putIfAbsentElseGet(n, new ConcurrentHashMap<D, D>());
		return set.put(d1, d2) == null;
	}
	
	/**
	 * Factory method for this solver's thread-pool executor.
	 */
	protected CountingThreadPoolExecutor getExecutor() {
		return new CountingThreadPoolExecutor(1, this.numThreads, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	/**
	 * Returns a String used to identify the output of this solver in debug mode.
	 * Subclasses can overwrite this string to distinguish the output from different solvers.
	 */
	protected String getDebugName() {
		return "FAST IFDS SOLVER";
	}

	public void printStats() {
		if(logger.isDebugEnabled()) {
			if(ffCache!=null)
				ffCache.printStats();
		} else {
			logger.info("No statistics were collected, as DEBUG is disabled.");
		}
	}
	
	private class PathEdgeProcessingTask implements Runnable {
		private final PathEdge<N,D> edge;

		public PathEdgeProcessingTask(PathEdge<N,D> edge) {
			this.edge = edge;
		}

		public void run() {
			if(icfg.isCallStmt(edge.getTarget())) {
				processCall(edge);
			} else {
				//note that some statements, such as "throw" may be
				//both an exit statement and a "normal" statement
				if(icfg.isExitStmt(edge.getTarget()))
					processExit(edge);
				if(!icfg.getSuccsOf(edge.getTarget()).isEmpty())
					processNormalFlow(edge);
			}
		}
	}
	
	/**
	 * Sets whether abstractions on method returns shall be connected to the
	 * respective call abstractions to shortcut paths.
	 * @param setJumpPredecessors True if return abstractions shall be connected
	 * to call abstractions as predecessors, otherwise false.
	 */
	public void setJumpPredecessors(boolean setJumpPredecessors) {
		this.setJumpPredecessors = setJumpPredecessors;
	}
	
	/**
	 * Sets whether only abstractions at merge points shall be recorded to jumpFn.
	 * @param enableMergePointChecking True if only abstractions at merge points
	 * shall be recorded to jumpFn, otherwise false.
	 */
	public void setEnableMergePointChecking(boolean enableMergePointChecking) {
		this.enableMergePointChecking = enableMergePointChecking;
	}

}
