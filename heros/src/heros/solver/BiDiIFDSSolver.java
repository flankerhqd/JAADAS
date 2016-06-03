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
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
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
public class BiDiIFDSSolver<N, D extends JoinHandlingNode<D>, M, I extends InterproceduralCFG<N, M>> extends BiDiIDESolver<N, D, M, BinaryDomain, I> {


	/**
	 * Instantiates a {@link BiDiIFDSSolver} with the associated forward and backward problem.
	 */
	public BiDiIFDSSolver(IFDSTabulationProblem<N,D,M,I> forwardProblem, IFDSTabulationProblem<N,D,M,I> backwardProblem) {
		super(IFDSSolver.createIDETabulationProblem(forwardProblem), IFDSSolver.createIDETabulationProblem(backwardProblem));
	}
	
	public Set<D> fwIFDSResultAt(N stmt) {
		return extractResults(fwSolver.resultsAt(stmt).keySet());
	}
	
	public Set<D> bwIFDSResultAt(N stmt) {
		return extractResults(bwSolver.resultsAt(stmt).keySet());
	}

	private Set<D> extractResults(Set<AbstractionWithSourceStmt> annotatedResults) {
		Set<D> res = new HashSet<D>();		
		for (AbstractionWithSourceStmt abstractionWithSourceStmt : annotatedResults) {
			res.add(abstractionWithSourceStmt.getAbstraction());
		}
		return res;
	}
	
}
