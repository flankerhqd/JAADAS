/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 *     Johannes Lerch, Johannes Spaeth - extension for field sensitivity
 ******************************************************************************/
package heros.fieldsens;


/**
 * Classes implementing this interface provide a factory for a 
 * range of flow functions used to compute which D-type values
 * are reachable along the program's control flow.
 * 
 * @param <Stmt>
 *            The type of nodes in the interprocedural control-flow graph.
 *            Typically {@link Unit}.
 * @param <F>
 *            The type of data-flow facts to be computed by the tabulation
 *            problem.
 * @param <Method>
 *            The type of objects used to represent methods. Typically
 *            {@link SootMethod}.
 */
public interface FlowFunctions<Stmt, FieldRef, F, Method> {

	/**
	 * Returns the flow function that computes the flow for a normal statement,
	 * i.e., a statement that is neither a call nor an exit statement.
	 * 
	 * @param curr
	 *            The current statement.
	 */
	public FlowFunction<FieldRef, F, Stmt, Method> getNormalFlowFunction(Stmt curr);

	
	/**
	 * Returns the flow function that computes the flow for a call statement.
	 * 
	 * @param callStmt
	 *            The statement containing the invoke expression giving rise to
	 *            this call.
	 * @param destinationMethod
	 *            The concrete target method for which the flow is computed.
	 */
	public FlowFunction<FieldRef, F, Stmt, Method> getCallFlowFunction(Stmt callStmt, Method destinationMethod);

	/**
	 * Returns the flow function that computes the flow for a an exit from a
	 * method. An exit can be a return or an exceptional exit.
	 * 
	 * @param callSite
	 *            One of all the call sites in the program that called the
	 *            method from which the exitStmt is actually returning. This
	 *            information can be exploited to compute a value that depends on
	 *            information from before the call.
	 *            <b>Note:</b> This value might be <code>null</code> if
	 *            using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
	 *            returning <code>true</code> in a situation where the call graph
	 *            does not contain a caller for the method that is returned from.
	 * @param calleeMethod
	 *            The method from which exitStmt returns.
	 * @param exitStmt
	 *            The statement exiting the method, typically a return or throw
	 *            statement.
	 * @param returnSite
	 *            One of the successor statements of the callSite. There may be
	 *            multiple successors in case of possible exceptional flow. This
	 *            method will be called for each such successor.
	 *            <b>Note:</b> This value might be <code>null</code> if
	 *            using a tabulation problem with {@link IFDSTabulationProblem#followReturnsPastSeeds()}
	 *            returning <code>true</code> in a situation where the call graph
	 *            does not contain a caller for the method that is returned from.
	 * @return
	 */
	public FlowFunction<FieldRef, F, Stmt, Method> getReturnFlowFunction(Stmt callSite, Method calleeMethod, Stmt exitStmt, Stmt returnSite);

	/**
	 * Returns the flow function that computes the flow from a call site to a
	 * successor statement just after the call. There may be multiple successors
	 * in case of exceptional control flow. In this case this method will be
	 * called for every such successor. Typically, one will propagate into a
	 * method call, using {@link #getCallFlowFunction(Object, Object)}, only
	 * such information that actually concerns the callee method. All other
	 * information, e.g. information that cannot be modified by the call, is
	 * passed along this call-return edge.
	 * 
	 * @param callSite
	 *            The statement containing the invoke expression giving rise to
	 *            this call.
	 * @param returnSite
	 *            The return site to which the information is propagated. For
	 *            exceptional flow, this may actually be the start of an
	 *            exception handler.
	 */
	public FlowFunction<FieldRef, F, Stmt, Method> getCallToReturnFlowFunction(Stmt callSite, Stmt returnSite);

	
	
}
