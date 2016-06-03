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

/**
 * Classes implementing this interface provide a range of edge functions used to
 * compute a V-type value for each of the finitely many D-type values reachable
 * in the program.
 * 
 * @param <N>
 *            The type of nodes in the interprocedural control-flow graph.
 *            Typically {@link Unit}.
 * @param <D>
 *            The type of data-flow facts to be computed by the tabulation
 *            problem.
 * @param <M>
 *            The type of objects used to represent methods. Typically
 *            {@link SootMethod}.
 * @param <V>
 *            The type of values to be computed along flow edges.
 */
public interface EdgeFunctions<N, D, M, V> {

	/**
	 * Returns the function that computes how the V-typed value changes when
	 * being propagated from srcNode at statement src to tgtNode at statement
	 * tgt.
	 * 
	 * @param curr
	 *            The statement from which the flow originates.
	 * @param currNode
	 *            The D-type value with which the source value is associated.
	 * @param succ
	 *            The target statement of the flow.
	 * @param succNode
	 *            The D-type value with which the target value will be
	 *            associated.
	 */
	public EdgeFunction<V> getNormalEdgeFunction(N curr, D currNode, N succ, D succNode);

	/**
	 * Returns the function that computes how the V-typed value changes when
	 * being propagated along a method call.
	 * 
	 * @param callStmt
	 *            The call statement from which the flow originates.
	 * @param srcNode
	 *            The D-type value with which the source value is associated.
	 * @param destinationMethod
	 *            A concrete destination method of the call.
	 * @param destNode
	 *            The D-type value with which the target value will be
	 *            associated at the side of the callee.
	 */
	public EdgeFunction<V> getCallEdgeFunction(N callStmt, D srcNode, M destinationMethod, D destNode);

	/**
	 * Returns the function that computes how the V-typed value changes when
	 * being propagated along a method exit (return or throw).
	 * 
	 * @param callSite
	 *            One of all the call sites in the program that called the
	 *            method from which the exitStmt is actually returning. This
	 *            information can be exploited to compute a value that depend on
	 *            information from before the call.
	 *            <b>Note:</b> This value might be <code>null</code> if
	 *            using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
	 *            returning <code>true</code> in a situation where the call graph
	 *            does not contain a caller for the method that is returned from.
	 * @param calleeMethod
	 *            The method from which we are exiting.
	 * @param exitStmt
	 *            The exit statement from which the flow originates.
	 * @param exitNode
	 *            The D-type value with which the source value is associated.
	 * @param returnSite
	 *            One of the possible successor statements of a caller to the
	 *            method we are exiting from.
	 *            <b>Note:</b> This value might be <code>null</code> if
	 *            using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
	 *            returning <code>true</code> in a situation where the call graph
	 *            does not contain a caller for the method that is returned from.
	 * @param tgtNode
	 *            The D-type value with which the target value will be
	 *            associated at the returnSite.
	 */
	public EdgeFunction<V> getReturnEdgeFunction(N callSite, M calleeMethod, N exitStmt, D exitNode, N returnSite, D retNode);

	/**
	 * Returns the function that computes how the V-typed value changes when
	 * being propagated from a method call to one of its intraprocedural
	 * successor.
	 * 
	 * @param callSite
	 *            The call statement from which the flow originates.
	 * @param callNode
	 *            The D-type value with which the source value is associated.
	 * @param returnSite
	 *            One of the possible successor statements of a call statement.
	 * @param returnSideNode
	 *            The D-type value with which the target value will be
	 *            associated at the returnSite.
	 */
	public EdgeFunction<V> getCallToReturnEdgeFunction(N callSite, D callNode, N returnSite, D returnSideNode);

}
