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
 ******************************************************************************/
package heros.solver;


import heros.DontSynchronize;
import heros.EdgeFunction;
import heros.EdgeFunctionCache;
import heros.EdgeFunctions;
import heros.FlowFunction;
import heros.FlowFunctionCache;
import heros.FlowFunctions;
import heros.IDETabulationProblem;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.SynchronizedBy;
import heros.ZeroedFlowFunctions;
import heros.edgefunc.EdgeIdentity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;


/**
 * Solves the given {@link IDETabulationProblem} as described in the 1996 paper by Sagiv,
 * Horwitz and Reps. To solve the problem, call {@link #solve()}. Results can then be
 * queried by using {@link #resultAt(Object, Object)} and {@link #resultsAt(Object)}.
 * 
 * Note that this solver and its data structures internally use mostly {@link java.util.LinkedHashSet}s
 * instead of normal {@link HashSet}s to fix the iteration order as much as possible. This
 * is to produce, as much as possible, reproducible benchmarking results. We have found
 * that the iteration order can matter a lot in terms of speed.
 *
 * @param <N> The type of nodes in the interprocedural control-flow graph. 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods.
 * @param <V> The type of values to be computed along flow edges.
 * @param <I> The type of inter-procedural control-flow graph being used.
 */
public class IDESolver<N,D,M,V,I extends InterproceduralCFG<N, M>> {
	
	public static CacheBuilder<Object, Object> DEFAULT_CACHE_BUILDER = CacheBuilder.newBuilder().concurrencyLevel(Runtime.getRuntime().availableProcessors()).initialCapacity(10000).softValues();
	
    protected static final Logger logger = LoggerFactory.getLogger(IDESolver.class);

    //enable with -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
    public static final boolean DEBUG = logger.isDebugEnabled();

	protected CountingThreadPoolExecutor executor;
	
	@DontSynchronize("only used by single thread")
	protected int numThreads;
	
	@SynchronizedBy("thread safe data structure, consistent locking when used")
	protected final JumpFunctions<N,D,V> jumpFn;
	
	@SynchronizedBy("thread safe data structure, only modified internally")
	protected final I icfg;
	
	//stores summaries that were queried before they were computed
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on 'incoming'")
	protected final Table<N,D,Table<N,D,EdgeFunction<V>>> endSummary = HashBasedTable.create();

	//edges going along calls
	//see CC 2010 paper by Naeem, Lhotak and Rodriguez
	@SynchronizedBy("consistent lock on field")
	protected final Table<N,D,Map<N,Set<D>>> incoming = HashBasedTable.create();

	//stores the return sites (inside callers) to which we have unbalanced returns
	//if followReturnPastSeeds is enabled
	@SynchronizedBy("use of ConcurrentHashMap")
	protected final Set<N> unbalancedRetSites;

	@DontSynchronize("stateless")
	protected final FlowFunctions<N, D, M> flowFunctions;

	@DontSynchronize("stateless")
	protected final EdgeFunctions<N,D,M,V> edgeFunctions;

	@DontSynchronize("only used by single thread")
	protected final Map<N,Set<D>> initialSeeds;

	@DontSynchronize("stateless")
	protected final JoinLattice<V> valueLattice;
	
	@DontSynchronize("stateless")
	protected final EdgeFunction<V> allTop;

	@SynchronizedBy("consistent lock on field")
	protected final Table<N,D,V> val = HashBasedTable.create();	
	
	@DontSynchronize("benign races")
	public long flowFunctionApplicationCount;

	@DontSynchronize("benign races")
	public long flowFunctionConstructionCount;
	
	@DontSynchronize("benign races")
	public long propagationCount;
	
	@DontSynchronize("benign races")
	public long durationFlowFunctionConstruction;
	
	@DontSynchronize("benign races")
	public long durationFlowFunctionApplication;

	@DontSynchronize("stateless")
	protected final D zeroValue;
	
	@DontSynchronize("readOnly")
	protected final FlowFunctionCache<N,D,M> ffCache; 

	@DontSynchronize("readOnly")
	protected final EdgeFunctionCache<N,D,M,V> efCache;

	@DontSynchronize("readOnly")
	protected final boolean followReturnsPastSeeds;

	@DontSynchronize("readOnly")
	protected final boolean computeValues;

	/**
	 * Creates a solver for the given problem, which caches flow functions and edge functions.
	 * The solver must then be started by calling {@link #solve()}.
	 */
	public IDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem) {
		this(tabulationProblem, DEFAULT_CACHE_BUILDER, DEFAULT_CACHE_BUILDER);
	}

	/**
	 * Creates a solver for the given problem, constructing caches with the given {@link CacheBuilder}. The solver must then be started by calling
	 * {@link #solve()}.
	 * @param flowFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for flow functions.
	 * @param edgeFunctionCacheBuilder A valid {@link CacheBuilder} or <code>null</code> if no caching is to be used for edge functions.
	 */
	public IDESolver(IDETabulationProblem<N,D,M,V,I> tabulationProblem, @SuppressWarnings("rawtypes") CacheBuilder flowFunctionCacheBuilder, @SuppressWarnings("rawtypes") CacheBuilder edgeFunctionCacheBuilder) {
		if(logger.isDebugEnabled()) {
			if(flowFunctionCacheBuilder != null)
				flowFunctionCacheBuilder = flowFunctionCacheBuilder.recordStats();
			if(edgeFunctionCacheBuilder != null)
				edgeFunctionCacheBuilder = edgeFunctionCacheBuilder.recordStats();
		}
		this.zeroValue = tabulationProblem.zeroValue();
		this.icfg = tabulationProblem.interproceduralCFG();		
		FlowFunctions<N, D, M> flowFunctions = tabulationProblem.autoAddZero() ?
				new ZeroedFlowFunctions<N,D,M>(tabulationProblem.flowFunctions(), tabulationProblem.zeroValue()) : tabulationProblem.flowFunctions(); 
		EdgeFunctions<N, D, M, V> edgeFunctions = tabulationProblem.edgeFunctions();
		if(flowFunctionCacheBuilder!=null) {
			ffCache = new FlowFunctionCache<N,D,M>(flowFunctions, flowFunctionCacheBuilder);
			flowFunctions = ffCache;
		} else {
			ffCache = null;
		}
		if(edgeFunctionCacheBuilder!=null) {
			efCache = new EdgeFunctionCache<N,D,M,V>(edgeFunctions, edgeFunctionCacheBuilder);
			edgeFunctions = efCache;
		} else {
			efCache = null;
		}
		this.flowFunctions = flowFunctions;
		this.edgeFunctions = edgeFunctions;
		this.initialSeeds = tabulationProblem.initialSeeds();
		this.unbalancedRetSites = Collections.newSetFromMap(new ConcurrentHashMap<N, Boolean>());
		this.valueLattice = tabulationProblem.joinLattice();
		this.allTop = tabulationProblem.allTopFunction();
		this.jumpFn = new JumpFunctions<N,D,V>(allTop);
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.numThreads = Math.max(1,tabulationProblem.numThreads());
		this.computeValues = tabulationProblem.computeValues();
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
			for(D val: seed.getValue()) {
				propagate(zeroValue, startPoint, val, EdgeIdentity.<V>v(), null, false);
			}
			jumpFn.addFunction(zeroValue, startPoint, zeroValue, EdgeIdentity.<V>v());
		}
	}

	/**
	 * Awaits the completion of the exploded super graph. When complete, computes result values,
	 * shuts down the executor and returns.
	 */
	protected void awaitCompletionComputeValuesAndShutdown() {
		{
			final long before = System.currentTimeMillis();
			//run executor and await termination of tasks
			runExecutorAndAwaitCompletion();
			durationFlowFunctionConstruction = System.currentTimeMillis() - before;
		}
		if(computeValues) {
			final long before = System.currentTimeMillis();
			computeValues();
			durationFlowFunctionApplication = System.currentTimeMillis() - before;
		}
		if(logger.isDebugEnabled())
			printStats();
		
		//ask executor to shut down;
		//this will cause new submissions to the executor to be rejected,
		//but at this point all tasks should have completed anyway
		executor.shutdown();
		//similarly here: we await termination, but this should happen instantaneously,
		//as all tasks should have completed
		runExecutorAndAwaitCompletion();
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
			throw new RuntimeException("There were exceptions during IDE analysis. Exiting.",exception);
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
     * Dispatch the processing of a given value. It may be executed in a different thread.
     * @param vpt
     */
    private void scheduleValueProcessing(ValuePropagationTask vpt){
    	// If the executor has been killed, there is little point
    	// in submitting new tasks
    	if (executor.isTerminating())
    		return;
    	executor.execute(vpt);
    }
  
    /**
     * Dispatch the computation of a given value. It may be executed in a different thread.
     * @param task
     */
	private void scheduleValueComputationTask(ValueComputationTask task) {
    	// If the executor has been killed, there is little point
    	// in submitting new tasks
    	if (executor.isTerminating())
    		return;
		executor.execute(task);
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
		EdgeFunction<V> f = jumpFunction(edge);
		Collection<N> returnSiteNs = icfg.getReturnSitesOfCallAt(n);
		
		//for each possible callee
		Collection<M> callees = icfg.getCalleesOfCallAt(n);
		for(M sCalledProcN: callees) { //still line 14
			
			//compute the call-flow function
			FlowFunction<D> function = flowFunctions.getCallFlowFunction(n, sCalledProcN);
			flowFunctionConstructionCount++;
			Set<D> res = computeCallFlowFunction(function, d1, d2);
			
			//for each callee's start point(s)
			Collection<N> startPointsOf = icfg.getStartPointsOf(sCalledProcN);
			for(N sP: startPointsOf) {
				//for each result node of the call-flow function
				for(D d3: res) {
					//create initial self-loop
					propagate(d3, sP, d3, EdgeIdentity.<V>v(), n, false); //line 15
	
					//register the fact that <sp,d3> has an incoming edge from <n,d2>
					Set<Cell<N, D, EdgeFunction<V>>> endSumm;
					synchronized (incoming) {
						//line 15.1 of Naeem/Lhotak/Rodriguez
						addIncoming(sP,d3,n,d2);
						//line 15.2, copy to avoid concurrent modification exceptions by other threads
						endSumm = new HashSet<Table.Cell<N,D,EdgeFunction<V>>>(endSummary(sP, d3));
					}
					
					//still line 15.2 of Naeem/Lhotak/Rodriguez
					//for each already-queried exit value <eP,d4> reachable from <sP,d3>,
					//create new caller-side jump functions to the return sites
					//because we have observed a potentially new incoming edge into <sP,d3>
					for(Cell<N, D, EdgeFunction<V>> entry: endSumm) {
						N eP = entry.getRowKey();
						D d4 = entry.getColumnKey();
						EdgeFunction<V> fCalleeSummary = entry.getValue();
						//for each return site
						for(N retSiteN: returnSiteNs) {
							//compute return-flow function
							FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(n, sCalledProcN, eP, retSiteN);
							flowFunctionConstructionCount++;
							//for each target value of the function
							for(D d5: computeReturnFlowFunction(retFunction, d3, d4, n, Collections.singleton(d2))) {
								//update the caller-side summary function
								EdgeFunction<V> f4 = edgeFunctions.getCallEdgeFunction(n, d2, sCalledProcN, d3);
								EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(n, sCalledProcN, eP, d4, retSiteN, d5);
								EdgeFunction<V> fPrime = f4.composeWith(fCalleeSummary).composeWith(f5);					
								D d5_restoredCtx = restoreContextOnReturnedFact(d2, d5);
								propagate(d1, retSiteN, d5_restoredCtx, f.composeWith(fPrime), n, false);
							}
						}
					}
				}		
			}
		}
		//line 17-19 of Naeem/Lhotak/Rodriguez		
		//process intra-procedural flows along call-to-return flow functions
		for (N returnSiteN : returnSiteNs) {
			FlowFunction<D> callToReturnFlowFunction = flowFunctions.getCallToReturnFlowFunction(n, returnSiteN);
			flowFunctionConstructionCount++;
			for(D d3: computeCallToReturnFlowFunction(callToReturnFlowFunction, d1, d2)) {
				EdgeFunction<V> edgeFnE = edgeFunctions.getCallToReturnEdgeFunction(n, d2, returnSiteN, d3);
				propagate(d1, returnSiteN, d3, f.composeWith(edgeFnE), n, false);
			}
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
		EdgeFunction<V> f = jumpFunction(edge);
		M methodThatNeedsSummary = icfg.getMethodOf(n);
		
		final D d1 = edge.factAtSource();
		final D d2 = edge.factAtTarget();
		
		//for each of the method's start points, determine incoming calls
		Collection<N> startPointsOf = icfg.getStartPointsOf(methodThatNeedsSummary);
		Map<N,Set<D>> inc = new HashMap<N,Set<D>>();
		for(N sP: startPointsOf) {
			//line 21.1 of Naeem/Lhotak/Rodriguez
			
			//register end-summary
			synchronized (incoming) {
				addEndSummary(sP, d1, n, d2, f);
				//copy to avoid concurrent modification exceptions by other threads
				for (Entry<N, Set<D>> entry : incoming(d1, sP).entrySet())
					inc.put(entry.getKey(), new HashSet<D>(entry.getValue()));
			}
		}
		
		//for each incoming call edge already processed
		//(see processCall(..))
		for (Entry<N,Set<D>> entry: inc.entrySet()) {
			//line 22
			N c = entry.getKey();
			//for each return site
			for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
				//compute return-flow function
				FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
				flowFunctionConstructionCount++;
				//for each incoming-call value
				for(D d4: entry.getValue()) {
					Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c, entry.getValue());
					//for each target value at the return site
					//line 23
					for(D d5: targets) {
						//compute composed function
						EdgeFunction<V> f4 = edgeFunctions.getCallEdgeFunction(c, d4, icfg.getMethodOf(n), d1);
						EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(c, icfg.getMethodOf(n), n, d2, retSiteC, d5);
						EdgeFunction<V> fPrime = f4.composeWith(f).composeWith(f5);
						//for each jump function coming into the call, propagate to return site using the composed function
						synchronized (jumpFn) { // some other thread might change jumpFn on the way
							for(Map.Entry<D,EdgeFunction<V>> valAndFunc: jumpFn.reverseLookup(c,d4).entrySet()) {
								EdgeFunction<V> f3 = valAndFunc.getValue();
								if(!f3.equalTo(allTop)) {
									D d3 = valAndFunc.getKey();
									D d5_restoredCtx = restoreContextOnReturnedFact(d4, d5);
									propagate(d3, retSiteC, d5_restoredCtx, f3.composeWith(fPrime), c, false);
								}
							}
						}
					}
				}
			}
		}
		
		//handling for unbalanced problems where we return out of a method with a fact for which we have no incoming flow
		//note: we propagate that way only values that originate from ZERO, as conditionally generated values should only
		//be propagated into callers that have an incoming edge for this condition
		if(followReturnsPastSeeds && inc.isEmpty() && d1.equals(zeroValue)) {
			// only propagate up if we 
				Collection<N> callers = icfg.getCallersOf(methodThatNeedsSummary);
				for(N c: callers) {
					for(N retSiteC: icfg.getReturnSitesOfCallAt(c)) {
						FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(c, methodThatNeedsSummary,n,retSiteC);
						flowFunctionConstructionCount++;
						Set<D> targets = computeReturnFlowFunction(retFunction, d1, d2, c, Collections.singleton(zeroValue));
						for(D d5: targets) {
							EdgeFunction<V> f5 = edgeFunctions.getReturnEdgeFunction(c, icfg.getMethodOf(n), n, d2, retSiteC, d5);
							propagateUnbalancedReturnFlow(retSiteC, d5, f.composeWith(f5), c);
							//register for value processing (2nd IDE phase)
							unbalancedRetSites.add(retSiteC);
						}
					}
				}
				//in cases where there are no callers, the return statement would normally not be processed at all;
				//this might be undesirable if the flow function has a side effect such as registering a taint;
				//instead we thus call the return flow function will a null caller
				if(callers.isEmpty()) {
					FlowFunction<D> retFunction = flowFunctions.getReturnFlowFunction(null, methodThatNeedsSummary,n,null);
					flowFunctionConstructionCount++;
					retFunction.computeTargets(d2);
				}
			}
		}
	
	protected void propagateUnbalancedReturnFlow(N retSiteC, D targetVal, EdgeFunction<V> edgeFunction, N relatedCallSite) {		
		propagate(zeroValue, retSiteC, targetVal, edgeFunction, relatedCallSite, true);
	}

	/**
	 * This method will be called for each incoming edge and can be used to
	 * transfer knowledge from the calling edge to the returning edge, without
	 * affecting the summary edges at the callee.
	 * 
	 * @param d4
	 *            Fact stored with the incoming edge, i.e., present at the
	 *            caller side
	 * @param d5
	 *            Fact that originally should be propagated to the caller.
	 * @return Fact that will be propagated to the caller.
	 */
	@SuppressWarnings("unchecked")
	protected D restoreContextOnReturnedFact(D d4, D d5) {
		if (d5 instanceof LinkedNode) {
			((LinkedNode<D>) d5).setCallingContext(d4);
		}
		if(d5 instanceof JoinHandlingNode) {
			((JoinHandlingNode<D>) d5).setCallingContext(d4);
		}			
		return d5;
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
			(FlowFunction<D> retFunction, D d1, D d2, N callSite, Set<D> callerSideDs) {
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
		
		EdgeFunction<V> f = jumpFunction(edge);
		for (N m : icfg.getSuccsOf(n)) {
			FlowFunction<D> flowFunction = flowFunctions.getNormalFlowFunction(n,m);
			flowFunctionConstructionCount++;
			Set<D> res = computeNormalFlowFunction(flowFunction, d1, d2);
			for (D d3 : res) {
				EdgeFunction<V> fprime = f.composeWith(edgeFunctions.getNormalEdgeFunction(n, d2, m, d3));
				propagate(d1, m, d3, fprime, null, false); 
			}
		}
	}
	
	/**
	 * Computes the normal flow function for the given set of start and end
	 * abstractions-
	 * @param flowFunction The normal flow function to compute
	 * @param d1 The abstraction at the method's start node
	 * @param d1 The abstraction at the current node
	 * @return The set of abstractions at the successor node
	 */
	protected Set<D> computeNormalFlowFunction
			(FlowFunction<D> flowFunction, D d1, D d2) {
		return flowFunction.computeTargets(d2);
	}

	/**
	 * Propagates the flow further down the exploded super graph, merging any edge function that might
	 * already have been computed for targetVal at target. 
	 * @param sourceVal the source value of the propagated summary edge
	 * @param target the target statement
	 * @param targetVal the target value at the target statement
	 * @param f the new edge function computed from (s0,sourceVal) to (target,targetVal) 
	 * @param relatedCallSite for call and return flows the related call statement, <code>null</code> otherwise
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IDESolver}) 
	 * @param isUnbalancedReturn <code>true</code> if this edge is propagating an unbalanced return
	 *        (this value is not used within this implementation but may be useful for subclasses of {@link IDESolver}) 
	 */
	protected void propagate(D sourceVal, N target, D targetVal, EdgeFunction<V> f,
		/* deliberately exposed to clients */ N relatedCallSite,
		/* deliberately exposed to clients */ boolean isUnbalancedReturn) {
		EdgeFunction<V> jumpFnE;
		EdgeFunction<V> fPrime;
		boolean newFunction;
		synchronized (jumpFn) {
			jumpFnE = jumpFn.reverseLookup(target, targetVal).get(sourceVal);
			if(jumpFnE==null) jumpFnE = allTop; //JumpFn is initialized to all-top (see line [2] in SRH96 paper)
			fPrime = jumpFnE.joinWith(f);
			newFunction = !fPrime.equalTo(jumpFnE);
			if(newFunction) {
				jumpFn.addFunction(sourceVal, target, targetVal, fPrime);
			}
		}

		if(newFunction) {
			PathEdge<N,D> edge = new PathEdge<N,D>(sourceVal, target, targetVal);
			scheduleEdgeProcessing(edge);

            if(targetVal!=zeroValue) {
                logger.trace("{} - EDGE: <{},{}> -> <{},{}> - {}", getDebugName(), icfg.getMethodOf(target), sourceVal, target, targetVal, fPrime );
            }
		}
	}
	
	/**
	 * Computes the final values for edge functions.
	 */
	private void computeValues() {	
		//Phase II(i)
        logger.debug("Computing the final values for the edge functions");
        //add caller seeds to initial seeds in an unbalanced problem
        Map<N, Set<D>> allSeeds = new HashMap<N, Set<D>>(initialSeeds);
        for(N unbalancedRetSite: unbalancedRetSites) {
        	Set<D> seeds = allSeeds.get(unbalancedRetSite);
        	if(seeds==null) {
        		seeds = new HashSet<D>();
        		allSeeds.put(unbalancedRetSite, seeds);
        	}
        	seeds.add(zeroValue);
        }
		//do processing
		for(Entry<N, Set<D>> seed: allSeeds.entrySet()) {
			N startPoint = seed.getKey();
			for(D val: seed.getValue()) {
				setVal(startPoint, val, valueLattice.bottomElement());
				Pair<N, D> superGraphNode = new Pair<N,D>(startPoint, val); 
				scheduleValueProcessing(new ValuePropagationTask(superGraphNode));
			}
		}
		logger.debug("Computed the final values of the edge functions");
		//await termination of tasks
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Phase II(ii)
		//we create an array of all nodes and then dispatch fractions of this array to multiple threads
		Set<N> allNonCallStartNodes = icfg.allNonCallStartNodes();
		@SuppressWarnings("unchecked")
		N[] nonCallStartNodesArray = (N[]) new Object[allNonCallStartNodes.size()];
		int i=0;
		for (N n : allNonCallStartNodes) {
			nonCallStartNodesArray[i] = n;
			i++;
		}
		//No need to keep track of the number of tasks scheduled here, since we call shutdown
		for(int t=0;t<numThreads; t++) {
			ValueComputationTask task = new ValueComputationTask(nonCallStartNodesArray, t);
			scheduleValueComputationTask(task);
		}
		//await termination of tasks
		try {
			executor.awaitCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void propagateValueAtStart(Pair<N, D> nAndD, N n) {
		D d = nAndD.getO2();		
		M p = icfg.getMethodOf(n);
		for(N c: icfg.getCallsFromWithin(p)) {					
			Set<Entry<D, EdgeFunction<V>>> entries; 
			synchronized (jumpFn) {
				entries = jumpFn.forwardLookup(d,c).entrySet();
				for(Map.Entry<D,EdgeFunction<V>> dPAndFP: entries) {
					D dPrime = dPAndFP.getKey();
					EdgeFunction<V> fPrime = dPAndFP.getValue();
					N sP = n;
					propagateValue(c,dPrime,fPrime.computeTarget(val(sP,d)));
					flowFunctionApplicationCount++;
				}
			}
		}
	}
	
	private void propagateValueAtCall(Pair<N, D> nAndD, N n) {
		D d = nAndD.getO2();
		for(M q: icfg.getCalleesOfCallAt(n)) {
			FlowFunction<D> callFlowFunction = flowFunctions.getCallFlowFunction(n, q);
			flowFunctionConstructionCount++;
			for(D dPrime: callFlowFunction.computeTargets(d)) {
				EdgeFunction<V> edgeFn = edgeFunctions.getCallEdgeFunction(n, d, q, dPrime);
				for(N startPoint: icfg.getStartPointsOf(q)) {
					propagateValue(startPoint,dPrime, edgeFn.computeTarget(val(n,d)));
					flowFunctionApplicationCount++;
				}
			}
		}
	}
	
	private void propagateValue(N nHashN, D nHashD, V v) {
		synchronized (val) {
			V valNHash = val(nHashN, nHashD);
			V vPrime = valueLattice.join(valNHash,v);
			if(!vPrime.equals(valNHash)) {
				setVal(nHashN, nHashD, vPrime);
				scheduleValueProcessing(new ValuePropagationTask(new Pair<N,D>(nHashN,nHashD)));
			}
		}
	}

	private V val(N nHashN, D nHashD){ 
		V l;
		synchronized (val) {
			l = val.get(nHashN, nHashD);
		}
		if(l==null) return valueLattice.topElement(); //implicitly initialized to top; see line [1] of Fig. 7 in SRH96 paper
		else return l;
	}
	
	private void setVal(N nHashN, D nHashD,V l){
		// TOP is the implicit default value which we do not need to store.
		synchronized (val) {
			if (l == valueLattice.topElement())     // do not store top values
				val.remove(nHashN, nHashD);
			else
				val.put(nHashN, nHashD,l);
		}
        logger.debug("VALUE: {} {} {} {}", icfg.getMethodOf(nHashN), nHashN, nHashD, l);
	}

	private EdgeFunction<V> jumpFunction(PathEdge<N,D> edge) {
		synchronized (jumpFn) {
			EdgeFunction<V> function = jumpFn.forwardLookup(edge.factAtSource(), edge.getTarget()).get(edge.factAtTarget());
			if(function==null) return allTop; //JumpFn initialized to all-top, see line [2] in SRH96 paper
			return function;
		}
	}

	protected Set<Cell<N, D, EdgeFunction<V>>> endSummary(N sP, D d3) {
		Table<N, D, EdgeFunction<V>> map = endSummary.get(sP, d3);
		if(map==null) return Collections.emptySet();
		return map.cellSet();
	}

	private void addEndSummary(N sP, D d1, N eP, D d2, EdgeFunction<V> f) {
		Table<N, D, EdgeFunction<V>> summaries = endSummary.get(sP, d1);
		if(summaries==null) {
			summaries = HashBasedTable.create();
			endSummary.put(sP, d1, summaries);
		}
		//note: at this point we don't need to join with a potential previous f
		//because f is a jump function, which is already properly joined
		//within propagate(..)
		summaries.put(eP,d2,f);
	}	
	
	protected Map<N, Set<D>> incoming(D d1, N sP) {
		synchronized (incoming) {
			Map<N, Set<D>> map = incoming.get(sP, d1);
			if(map==null) return Collections.emptyMap();
			return map;
		}
	}
	
	protected void addIncoming(N sP, D d3, N n, D d2) {
		synchronized (incoming) {
			Map<N, Set<D>> summaries = incoming.get(sP, d3);
			if(summaries==null) {
				summaries = new HashMap<N, Set<D>>();
				incoming.put(sP, d3, summaries);
			}
			Set<D> set = summaries.get(n);
			if(set==null) {
				set = new HashSet<D>();
				summaries.put(n,set);
			}
			set.add(d2);
		}
	}	
	
	/**
	 * Returns the V-type result for the given value at the given statement.
	 * TOP values are never returned.
	 */
	public V resultAt(N stmt, D value) {
		//no need to synchronize here as all threads are known to have terminated
		return val.get(stmt, value);
	}
	
	/**
	 * Returns the resulting environment for the given statement.
	 * The artificial zero value is automatically stripped. TOP values are
	 * never returned.
	 */
	public Map<D,V> resultsAt(N stmt) {
		//filter out the artificial zero-value
		//no need to synchronize here as all threads are known to have terminated
		return Maps.filterKeys(val.row(stmt), new Predicate<D>() {

			public boolean apply(D val) {
				return val!=zeroValue;
			}
		});
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
		return "";
	}

	public void printStats() {
		if(logger.isDebugEnabled()) {
			if(ffCache!=null)
				ffCache.printStats();
			if(efCache!=null)
				efCache.printStats();
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
				if(icfg.isExitStmt(edge.getTarget())) {
					processExit(edge);
				}
				if(!icfg.getSuccsOf(edge.getTarget()).isEmpty()) {
					processNormalFlow(edge);
				}
			}
		}
	}
	
	private class ValuePropagationTask implements Runnable {
		private final Pair<N, D> nAndD;

		public ValuePropagationTask(Pair<N,D> nAndD) {
			this.nAndD = nAndD;
		}

		public void run() {
			N n = nAndD.getO1();
			if(icfg.isStartPoint(n) ||
				initialSeeds.containsKey(n) ||			//our initial seeds are not necessarily method-start points but here they should be treated as such
				unbalancedRetSites.contains(n)) { 		//the same also for unbalanced return sites in an unbalanced problem
				propagateValueAtStart(nAndD, n);
			}
			if(icfg.isCallStmt(n)) {
				propagateValueAtCall(nAndD, n);
			}
		}
	}
	
	private class ValueComputationTask implements Runnable {
		private final N[] values;
		final int num;

		public ValueComputationTask(N[] values, int num) {
			this.values = values;
			this.num = num;
		}

		public void run() {
			int sectionSize = (int) Math.floor(values.length / numThreads) + numThreads;
			for(int i = sectionSize * num; i < Math.min(sectionSize * (num+1),values.length); i++) {
				N n = values[i];
				for(N sP: icfg.getStartPointsOf(icfg.getMethodOf(n))) {					
					Set<Cell<D, D, EdgeFunction<V>>> lookupByTarget;
					lookupByTarget = jumpFn.lookupByTarget(n);
					for(Cell<D, D, EdgeFunction<V>> sourceValTargetValAndFunction : lookupByTarget) {
						D dPrime = sourceValTargetValAndFunction.getRowKey();
						D d = sourceValTargetValAndFunction.getColumnKey();
						EdgeFunction<V> fPrime = sourceValTargetValAndFunction.getValue();
						synchronized (val) {
							setVal(n,d,valueLattice.join(val(n,d),fPrime.computeTarget(val(sP,dPrime))));
						}
						flowFunctionApplicationCount++;
					}
				}
			}
		}
	}

}
