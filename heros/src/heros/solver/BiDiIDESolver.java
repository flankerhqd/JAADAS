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

import heros.EdgeFunction;
import heros.EdgeFunctions;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.IDETabulationProblem;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.solver.IFDSSolver.BinaryDomain;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

/**
 * This is a special IFDS solver that solves the analysis problem inside out, i.e., from further down the call stack to
 * further up the call stack. This can be useful, for instance, for taint analysis problems that track flows in two directions.
 * 
 * The solver is instantiated with two analyses, one to be computed forward and one to be computed backward. Both analysis problems
 * must be unbalanced, i.e., must return <code>true</code> for {@link IFDSTabulationProblem#followReturnsPastSeeds()}.
 * The solver then executes both analyses in lockstep, i.e., when one of the analyses reaches an unbalanced return edge (signified
 * by a ZERO source value) then the solver pauses this analysis until the other analysis reaches the same unbalanced return (if ever).
 * The result is that the analyses will never diverge, i.e., will ultimately always only propagate into contexts in which both their
 * computed paths are realizable at the same time.
 * 
 * This solver requires data-flow abstractions that implement the {@link LinkedNode} interface such that data-flow values can be linked to form
 * reportable paths.  
 *
 * @param <N> see {@link IFDSSolver}
 * @param <D> A data-flow abstraction that must implement the {@link LinkedNode} interface such that data-flow values can be linked to form
 * 				reportable paths.
 * @param <M> see {@link IFDSSolver}
 * @param <I> see {@link IFDSSolver}
 */
public class BiDiIDESolver<N, D, M, V, I extends InterproceduralCFG<N, M>> {

	private final IDETabulationProblem<N, AbstractionWithSourceStmt, M,V, I> forwardProblem;
	private final IDETabulationProblem<N, AbstractionWithSourceStmt, M,V, I> backwardProblem;
	private final CountingThreadPoolExecutor sharedExecutor;
	protected SingleDirectionSolver fwSolver;
	protected SingleDirectionSolver bwSolver;

	/**
	 * Instantiates a {@link BiDiIDESolver} with the associated forward and backward problem.
	 */
	public BiDiIDESolver(IDETabulationProblem<N,D,M,V,I> forwardProblem, IDETabulationProblem<N,D,M,V,I> backwardProblem) {
		if(!forwardProblem.followReturnsPastSeeds() || !backwardProblem.followReturnsPastSeeds()) {
			throw new IllegalArgumentException("This solver is only meant for bottom-up problems, so followReturnsPastSeeds() should return true."); 
		}
		this.forwardProblem = new AugmentedTabulationProblem(forwardProblem);
		this.backwardProblem = new AugmentedTabulationProblem(backwardProblem);
		this.sharedExecutor = new CountingThreadPoolExecutor(1, Math.max(1,forwardProblem.numThreads()), 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}
	
	public void solve() {		
		fwSolver = createSingleDirectionSolver(forwardProblem, "FW");
		bwSolver = createSingleDirectionSolver(backwardProblem, "BW");
		fwSolver.otherSolver = bwSolver;
		bwSolver.otherSolver = fwSolver;
		
		//start the bw solver
		bwSolver.submitInitialSeeds();
		
		//start the fw solver and block until both solvers have completed
		//(note that they both share the same executor, see below)
		//note to self: the order of the two should not matter
		fwSolver.solve();
	}
	
	/**
	 * Creates a solver to be used for each single analysis direction.
	 */
	protected SingleDirectionSolver createSingleDirectionSolver(IDETabulationProblem<N, AbstractionWithSourceStmt, M,V, I> problem, String debugName) {
		return new SingleDirectionSolver(problem, debugName);
	}
	
	private class PausedEdge {
		private N retSiteC;
		private AbstractionWithSourceStmt targetVal;
		private EdgeFunction<V> edgeFunction;
		private N relatedCallSite;
		
		public PausedEdge(N retSiteC, AbstractionWithSourceStmt targetVal, EdgeFunction<V> edgeFunction, N relatedCallSite) {
			this.retSiteC = retSiteC;
			this.targetVal = targetVal;
			this.edgeFunction = edgeFunction;
			this.relatedCallSite = relatedCallSite;
		}
	}

	/**
	 *  Data structure used to identify which edges can be unpaused by a {@link SingleDirectionSolver}. Each {@link SingleDirectionSolver} stores 
	 *  its leaks using this structure. A leak always requires a flow from some {@link #sourceStmt} (this is either the statement used as initial seed
	 *  or a call site of an unbalanced return) to a return site. This return site is always different for the forward and backward solvers,
	 *  but, the related call site of these return sites must be the same, if two entangled flows exist. 
	 *  Moreover, this structure represents the pair of such a {@link #sourceStmt} and the {@link #relatedCallSite}.
	 *
	 */
	private static class LeakKey<N> {
		private N sourceStmt;
		private N relatedCallSite;
		
		public LeakKey(N sourceStmt, N relatedCallSite) {
			this.sourceStmt = sourceStmt;
			this.relatedCallSite = relatedCallSite;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((relatedCallSite == null) ? 0 : relatedCallSite.hashCode());
			result = prime * result + ((sourceStmt == null) ? 0 : sourceStmt.hashCode());
			return result;
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof LeakKey))
				return false;
			LeakKey other = (LeakKey) obj;
			if (relatedCallSite == null) {
				if (other.relatedCallSite != null)
					return false;
			} else if (!relatedCallSite.equals(other.relatedCallSite))
				return false;
			if (sourceStmt == null) {
				if (other.sourceStmt != null)
					return false;
			} else if (!sourceStmt.equals(other.sourceStmt))
				return false;
			return true;
		}
	}
	
	/**
	 * This is a modified IFDS solver that is capable of pausing and unpausing return-flow edges.
	 */
	protected class SingleDirectionSolver extends IDESolver<N,AbstractionWithSourceStmt,M,V,I> {
		private final String debugName;
		private SingleDirectionSolver otherSolver;
		private Set<LeakKey<N>> leakedSources = Collections.newSetFromMap(Maps.<LeakKey<N>, Boolean>newConcurrentMap());
		private ConcurrentMap<LeakKey<N>,Set<PausedEdge>> pausedPathEdges =
				Maps.newConcurrentMap();

		public SingleDirectionSolver(IDETabulationProblem<N, AbstractionWithSourceStmt, M,V, I> ifdsProblem, String debugName) {
			super(ifdsProblem);
			this.debugName = debugName;
		}
		
		@Override
		protected void propagateUnbalancedReturnFlow(N retSiteC, AbstractionWithSourceStmt targetVal,
				EdgeFunction<V> edgeFunction, N relatedCallSite) {
			//if an edge is originating from ZERO then to us this signifies an unbalanced return edge
			N sourceStmt = targetVal.getSourceStmt();
			//we mark the fact that this solver would like to "leak" this edge to the caller
			LeakKey<N> leakKey = new LeakKey<N>(sourceStmt, relatedCallSite);
			leakedSources.add(leakKey);
			if(otherSolver.hasLeaked(leakKey)) {
				//if the other solver has leaked already then unpause its edges and continue
				otherSolver.unpausePathEdgesForSource(leakKey);
				super.propagateUnbalancedReturnFlow(retSiteC, targetVal, edgeFunction, relatedCallSite);
			} else {
				//otherwise we pause this solver's edge and don't continue
				Set<PausedEdge> newPausedEdges = 
						Collections.newSetFromMap(Maps.<PausedEdge, Boolean>newConcurrentMap()); 
				Set<PausedEdge> existingPausedEdges = pausedPathEdges.putIfAbsent(leakKey, newPausedEdges);
				if(existingPausedEdges==null)
					existingPausedEdges=newPausedEdges;
				
				PausedEdge edge = new PausedEdge(retSiteC, targetVal, edgeFunction, relatedCallSite);
				existingPausedEdges.add(edge);
				
				//if the other solver has leaked in the meantime, we have to make sure that the paused edge is unpaused
				if(otherSolver.hasLeaked(leakKey) && existingPausedEdges.remove(edge)) {
					super.propagateUnbalancedReturnFlow(retSiteC, targetVal, edgeFunction, relatedCallSite);
				}
						
                logger.debug(" ++ PAUSE {}: {}", debugName, edge);
			}
		}
		
		@Override
		protected void propagate(AbstractionWithSourceStmt sourceVal, N target, AbstractionWithSourceStmt targetVal, EdgeFunction<V> f, N relatedCallSite, boolean isUnbalancedReturn) {
			//the follwing branch will be taken only on an unbalanced return
			if(isUnbalancedReturn) {
				assert sourceVal.getSourceStmt()==null : "source value should have no statement attached";
				
				//attach target statement as new "source" statement to track
				targetVal = new AbstractionWithSourceStmt(targetVal.getAbstraction(), relatedCallSite);
				
				super.propagate(sourceVal, target, targetVal, f, relatedCallSite, isUnbalancedReturn);
			} else { 
				super.propagate(sourceVal, target, targetVal, f, relatedCallSite, isUnbalancedReturn);
			}
		}
		
		@Override
		protected AbstractionWithSourceStmt restoreContextOnReturnedFact(AbstractionWithSourceStmt d4, AbstractionWithSourceStmt d5) {
			return new AbstractionWithSourceStmt(d5.getAbstraction(), d4.getSourceStmt());
		}
		
		/**
		 * Returns <code>true</code> if this solver has tried to leak an edge originating from the given source
		 * to its caller.
		 */
		private boolean hasLeaked(LeakKey<N> leakKey) {
			return leakedSources.contains(leakKey);
		}
		
		/**
		 * Unpauses all edges associated with the given source statement.
		 */
		private void unpausePathEdgesForSource(LeakKey<N> leakKey) {
			Set<PausedEdge> pausedEdges = pausedPathEdges.get(leakKey);
			if(pausedEdges!=null) {
				for(PausedEdge edge: pausedEdges) {
					if(pausedEdges.remove(edge)) {
						if(DEBUG)
							logger.debug("-- UNPAUSE {}: {}",debugName, edge);
						super.propagateUnbalancedReturnFlow(edge.retSiteC, edge.targetVal, edge.edgeFunction, edge.relatedCallSite);
					}
				}
			}
		}
		
		/* we share the same executor; this will cause the call to solve() above to block
		 * until both solvers have finished
		 */ 
		protected CountingThreadPoolExecutor getExecutor() {
			return sharedExecutor;
		}
		
		protected String getDebugName() {
			return debugName;
		}
	}

	/**
	 * This is an augmented abstraction propagated by the {@link SingleDirectionSolver}. It associates with the
	 * abstraction the source statement from which this fact originated. 
	 */
	public class AbstractionWithSourceStmt {

		protected final D abstraction;
		protected final N source;
		
		private AbstractionWithSourceStmt(D abstraction, N source) {
			this.abstraction = abstraction;
			this.source = source;
		}

		public D getAbstraction() {
			return abstraction;
		}
		
		public N getSourceStmt() {
			return source;
		}	
		
		@Override
		public String toString() {
			if(source!=null)
				return ""+abstraction+"-@-"+source+"";
			else
				return abstraction.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
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
			@SuppressWarnings("unchecked")
			AbstractionWithSourceStmt other = (AbstractionWithSourceStmt) obj;
			if (abstraction == null) {
				if (other.abstraction != null)
					return false;
			} else if (!abstraction.equals(other.abstraction))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}
	}
	
	/**
	 * This tabulation problem simply propagates augmented abstractions where the normal problem would propagate normal abstractions.
	 */
	private class AugmentedTabulationProblem implements IDETabulationProblem<N, AbstractionWithSourceStmt,M,V,I> {

		private final IDETabulationProblem<N,D,M,V,I> delegate;
		private final AbstractionWithSourceStmt ZERO;
		private final FlowFunctions<N, D, M> originalFunctions;
		
		public AugmentedTabulationProblem(IDETabulationProblem<N, D, M,V, I> delegate) {
			this.delegate = delegate;
			originalFunctions = this.delegate.flowFunctions();
			ZERO = new AbstractionWithSourceStmt(delegate.zeroValue(), null);
		}

		@Override
		public FlowFunctions<N, AbstractionWithSourceStmt, M> flowFunctions() {
			return new FlowFunctions<N, AbstractionWithSourceStmt, M>() {

				@Override
				public FlowFunction<AbstractionWithSourceStmt> getNormalFlowFunction(final N curr, final N succ) {
					return new FlowFunction<AbstractionWithSourceStmt>() {
						@Override
						public Set<AbstractionWithSourceStmt> computeTargets(AbstractionWithSourceStmt source) {
							return copyOverSourceStmts(source, originalFunctions.getNormalFlowFunction(curr, succ));
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt> getCallFlowFunction(final N callStmt, final M destinationMethod) {
					return new FlowFunction<AbstractionWithSourceStmt>() {
						@Override
						public Set<AbstractionWithSourceStmt> computeTargets(AbstractionWithSourceStmt source) {
							Set<D> origTargets = originalFunctions.getCallFlowFunction(callStmt, destinationMethod).computeTargets(
									source.getAbstraction());

							Set<AbstractionWithSourceStmt> res = new HashSet<AbstractionWithSourceStmt>();
							for (D d : origTargets) {
								res.add(new AbstractionWithSourceStmt(d, null));
							}
							return res;
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt> getReturnFlowFunction(final N callSite, final M calleeMethod, final N exitStmt, final N returnSite) {
					return new FlowFunction<AbstractionWithSourceStmt>() {
						@Override
						public Set<AbstractionWithSourceStmt> computeTargets(AbstractionWithSourceStmt source) {
							return copyOverSourceStmts(source, originalFunctions.getReturnFlowFunction(callSite, calleeMethod, exitStmt, returnSite));
						}
					};
				}

				@Override
				public FlowFunction<AbstractionWithSourceStmt> getCallToReturnFlowFunction(final N callSite, final N returnSite) {
					return new FlowFunction<AbstractionWithSourceStmt>() {
						@Override
						public Set<AbstractionWithSourceStmt> computeTargets(AbstractionWithSourceStmt source) {
							return copyOverSourceStmts(source, originalFunctions.getCallToReturnFlowFunction(callSite, returnSite));
						}
					};
				}
				
				private Set<AbstractionWithSourceStmt> copyOverSourceStmts(AbstractionWithSourceStmt source, FlowFunction<D> originalFunction) {
					D originalAbstraction = source.getAbstraction();
					Set<D> origTargets = originalFunction.computeTargets(originalAbstraction);
					
					Set<AbstractionWithSourceStmt> res = new HashSet<AbstractionWithSourceStmt>();
					for(D d: origTargets) {
						res.add(new AbstractionWithSourceStmt(d,source.getSourceStmt()));
					}
					return res;
				}
			};
		}
		
		//delegate methods follow

		public boolean followReturnsPastSeeds() {
			return delegate.followReturnsPastSeeds();
		}

		public boolean autoAddZero() {
			return delegate.autoAddZero();
		}

		public int numThreads() {
			return delegate.numThreads();
		}

		public boolean computeValues() {
			return delegate.computeValues();
		}

		public I interproceduralCFG() {
			return delegate.interproceduralCFG();
		}

		/* attaches the original seed statement to the abstraction
		 */
		public Map<N,Set<AbstractionWithSourceStmt>> initialSeeds() {
			Map<N, Set<D>> originalSeeds = delegate.initialSeeds();
			Map<N,Set<AbstractionWithSourceStmt>> res = new HashMap<N, Set<AbstractionWithSourceStmt>>();
			for(Entry<N, Set<D>> entry: originalSeeds.entrySet()) {
				N stmt = entry.getKey();
				Set<D> seeds = entry.getValue();
				Set<AbstractionWithSourceStmt> resSet = new HashSet<AbstractionWithSourceStmt>();
				for (D d : seeds) {
					//attach source stmt to abstraction
					resSet.add(new AbstractionWithSourceStmt(d, stmt));
				}
				res.put(stmt, resSet);
			}			
			return res;
		}

		public AbstractionWithSourceStmt zeroValue() {
			return ZERO;
		}

		@Override
		public EdgeFunctions<N, AbstractionWithSourceStmt, M, V> edgeFunctions() {
			return new EdgeFunctions<N, AbstractionWithSourceStmt, M, V>() {

				@Override
				public EdgeFunction<V> getNormalEdgeFunction(N curr, AbstractionWithSourceStmt currNode, N succ,
						AbstractionWithSourceStmt succNode) {
					return delegate.edgeFunctions().getNormalEdgeFunction(curr, currNode.getAbstraction(), succ, succNode.getAbstraction());
				}

				@Override
				public EdgeFunction<V> getCallEdgeFunction(N callStmt,AbstractionWithSourceStmt srcNode,
						M destinationMethod, AbstractionWithSourceStmt destNode) {
					return delegate.edgeFunctions().getCallEdgeFunction(callStmt, srcNode.getAbstraction(), destinationMethod, destNode.getAbstraction());
				}

				@Override
				public EdgeFunction<V> getReturnEdgeFunction(N callSite, M calleeMethod, N exitStmt,
						AbstractionWithSourceStmt exitNode, N returnSite,
						AbstractionWithSourceStmt retNode) {
					return delegate.edgeFunctions().getReturnEdgeFunction(callSite, calleeMethod, exitStmt, exitNode.getAbstraction(), returnSite, retNode.getAbstraction());
				}

				@Override
				public EdgeFunction<V> getCallToReturnEdgeFunction(N callSite, AbstractionWithSourceStmt callNode,
						N returnSite, AbstractionWithSourceStmt returnSideNode) {
					return delegate.edgeFunctions().getCallToReturnEdgeFunction(callSite, callNode.getAbstraction(), returnSite, returnSideNode.getAbstraction());
				}
			};
		}

		@Override
		public JoinLattice<V> joinLattice() {
			return delegate.joinLattice();
		}

		@Override
		public EdgeFunction<V> allTopFunction() {
			return delegate.allTopFunction();
		}

	}
	
}
