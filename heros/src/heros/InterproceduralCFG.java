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
package heros;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An interprocedural control-flow graph.
 * 
 * @param <N> Nodes in the CFG, typically {@link Unit} or {@link Block}
 * @param <M> Method representation
 */
public interface InterproceduralCFG<N,M>  {
	
	/**
	 * Returns the method containing a node.
	 * @param n The node for which to get the parent method
	 */
	public M getMethodOf(N n);

	public List<N> getPredsOf(N u);
	
	/**
	 * Returns the successor nodes.
	 */
	public List<N> getSuccsOf(N n);

	/**
	 * Returns all callee methods for a given call.
	 */
	public Collection<M> getCalleesOfCallAt(N n);

	/**
	 * Returns all caller statements/nodes of a given method.
	 */
	public Collection<N> getCallersOf(M m);

	/**
	 * Returns all call sites within a given method.
	 */
	public Set<N> getCallsFromWithin(M m);

	/**
	 * Returns all start points of a given method. There may be
	 * more than one start point in case of a backward analysis.
	 */
	public Collection<N> getStartPointsOf(M m);

	/**
	 * Returns all statements to which a call could return.
	 * In the RHS paper, for every call there is just one return site.
	 * We, however, use as return site the successor statements, of which
	 * there can be many in case of exceptional flow.
	 */
	public Collection<N> getReturnSitesOfCallAt(N n);

	/**
	 * Returns <code>true</code> if the given statement is a call site.
	 */
	public boolean isCallStmt(N stmt);

	/**
	 * Returns <code>true</code> if the given statement leads to a method return
	 * (exceptional or not). For backward analyses may also be start statements.
	 */
	public boolean isExitStmt(N stmt);
	
	/**
	 * Returns true is this is a method's start statement. For backward analyses
	 * those may also be return or throws statements.
	 */
	public boolean isStartPoint(N stmt);
	
	/**
	 * Returns the set of all nodes that are neither call nor start nodes.
	 */
	public Set<N> allNonCallStartNodes();
	
	/**
	 * Returns whether succ is the fall-through successor of stmt,
	 * i.e., the unique successor that is be reached when stmt
	 * does not branch.
	 */
	public boolean isFallThroughSuccessor(N stmt, N succ);
	
	/**
	 * Returns whether succ is a branch target of stmt. 
	 */
	public boolean isBranchTarget(N stmt, N succ);

}
