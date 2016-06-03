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

import static heros.solver.IFDSSolver.BinaryDomain.BOTTOM;
import static heros.solver.IFDSSolver.BinaryDomain.TOP;
import heros.EdgeFunction;
import heros.EdgeFunctions;
import heros.FlowFunctions;
import heros.IDETabulationProblem;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.JoinLattice;
import heros.edgefunc.AllBottom;
import heros.edgefunc.AllTop;
import heros.edgefunc.EdgeIdentity;

import java.util.Map;
import java.util.Set;

/**
 * A solver for an {@link IFDSTabulationProblem}. This solver in effect uses the {@link IDESolver}
 * to solve the problem, as any IFDS problem can be intepreted as a special case of an IDE problem.
 * See Section 5.4.1 of the SRH96 paper. In effect, the IFDS problem is solved by solving an IDE
 * problem in which the environments (D to N mappings) represent the set's characteristic function.
 * 
 * @param <N> The type of nodes in the interprocedural control-flow graph. Typically {@link Unit}.
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 * @param <M> The type of objects used to represent methods. Typically {@link SootMethod}.
 * @param <I> The type of inter-procedural control-flow graph being used.
 * @see IFDSTabulationProblem
 */
public class IFDSSolver<N,D,M,I extends InterproceduralCFG<N, M>> extends IDESolver<N,D,M,IFDSSolver.BinaryDomain,I> {

	protected static enum BinaryDomain { TOP,BOTTOM } 
	
	private final static EdgeFunction<BinaryDomain> ALL_BOTTOM = new AllBottom<BinaryDomain>(BOTTOM);
	
	/**
	 * Creates a solver for the given problem. The solver must then be started by calling
	 * {@link #solve()}.
	 */
	public IFDSSolver(final IFDSTabulationProblem<N,D,M,I> ifdsProblem) {
		super(createIDETabulationProblem(ifdsProblem));
	}

	static <N, D, M, I extends InterproceduralCFG<N, M>> IDETabulationProblem<N, D, M, BinaryDomain, I> createIDETabulationProblem(
			final IFDSTabulationProblem<N, D, M, I> ifdsProblem) {
		return new IDETabulationProblem<N,D,M,BinaryDomain,I>() {

			public FlowFunctions<N,D,M> flowFunctions() {
				return ifdsProblem.flowFunctions();
			}

			public I interproceduralCFG() {
				return ifdsProblem.interproceduralCFG();
			}

			public Map<N,Set<D>> initialSeeds() {
				return ifdsProblem.initialSeeds();
			}

			public D zeroValue() {
				return ifdsProblem.zeroValue();
			}

			public EdgeFunctions<N,D,M,BinaryDomain> edgeFunctions() {
				return new IFDSEdgeFunctions();
			}

			public JoinLattice<BinaryDomain> joinLattice() {
				return new JoinLattice<BinaryDomain>() {

					public BinaryDomain topElement() {
						return BinaryDomain.TOP;
					}

					public BinaryDomain bottomElement() {
						return BinaryDomain.BOTTOM;
					}

					public BinaryDomain join(BinaryDomain left, BinaryDomain right) {
						if(left==TOP && right==TOP) {
							return TOP;
						} else {
							return BOTTOM;
						}
					}
				};
			}

			@Override
			public EdgeFunction<BinaryDomain> allTopFunction() {
				return new AllTop<BinaryDomain>(TOP);
			}
			
			@Override
			public boolean followReturnsPastSeeds() {
				return ifdsProblem.followReturnsPastSeeds();
			}
			
			@Override
			public boolean autoAddZero() {
				return ifdsProblem.autoAddZero();
			}
			
			@Override
			public int numThreads() {
				return ifdsProblem.numThreads();
			}
			
			@Override
			public boolean computeValues() {
				return ifdsProblem.computeValues();
			}
			
			class IFDSEdgeFunctions implements EdgeFunctions<N,D,M,BinaryDomain> {
		
				public EdgeFunction<BinaryDomain> getNormalEdgeFunction(N src,D srcNode,N tgt,D tgtNode) {
					if(srcNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getCallEdgeFunction(N callStmt,D srcNode,M destinationMethod,D destNode) {
					if(srcNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getReturnEdgeFunction(N callSite, M calleeMethod,N exitStmt,D exitNode,N returnSite,D retNode) {
					if(exitNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
		
				public EdgeFunction<BinaryDomain> getCallToReturnEdgeFunction(N callStmt,D callNode,N returnSite,D returnSideNode) {
					if(callNode==ifdsProblem.zeroValue()) return ALL_BOTTOM;
					return EdgeIdentity.v(); 
				}
			}

			};
	}
	
	/**
	 * Returns the set of facts that hold at the given statement.
	 */
	public Set<D> ifdsResultsAt(N statement) {
		return resultsAt(statement).keySet();
	}

}
