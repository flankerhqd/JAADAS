package soot.jimple.infoflow.aliasing;

import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.IInfoflowSolver;

/**
 * Generic interface for the different taint aliasing strategies supported by
 * FlowDroid
 * 
 * @author Steven Arzt
 */
public interface IAliasingStrategy {
	
	/**
	 * Computes the taints for the aliases of a given tainted variable
	 * @param d1 The context in which the variable has been tainted
	 * @param src The statement that tainted the variable
	 * @param targetValue The target value which has been tainted
	 * @param taintSet The set to which all generated alias taints shall be
	 * added
	 * @param method The method containing src
	 * @param newAbs The newly generated abstraction for the variable taint
	 */
	public void computeAliasTaints
			(final Abstraction d1, final Stmt src,
			final Value targetValue, Set<Abstraction> taintSet,
			SootMethod method, Abstraction newAbs);
	
	/**
	 * Gets whether this aliasing strategy is interactive, i.e. computes aliases
	 * on demand.
	 * @return True if this is an on-demand aliasing strategy, otherwise false
	 */
	public boolean isInteractive();
	
	/**
	 * Gets whether the two given access path may alias
	 * @param ap1 The first access path
	 * @param ap2 The second access path
	 * @return True if the two access paths can potentially point ot the same
	 * runtime object, otherwise false
	 */
	public boolean mayAlias(AccessPath ap1, AccessPath ap2);
	
	/**
	 * Sets the forward solver. Implementors can use this reference to inject
	 * edges for taint aliases at right position.
	 * @param fSolver The forward solver performing the taint propagation
	 */
	public void setForwardSolver(IInfoflowSolver fSolver);

	/**
	 * Notifies the aliasing strategy that a method has been called in the
	 * taint analysis. This may be helpful for interprocedural alias analyses.
	 * @param abs The abstraction on the callee's start unit
	 * @param fSolver The forward solver propagating the taints
	 * @param callee The callee
	 * @param callSite The call site
	 * @param source The abstraction at the call site
	 * @param d1 The abstraction at the caller method's entry point
	 */
	public void injectCallingContext(Abstraction abs, IInfoflowSolver fSolver,
			SootMethod callee, Unit callSite, Abstraction source, Abstraction d1);
	
	/**
	 * Gets whether this aliasing strategy is flow sensitive
	 * @return True if the aliasing strategy is flow sensitive, otherwise false
	 */
	public boolean isFlowSensitive();
	
	/**
	 * Gets whether this algorithm requires the analysis to be triggered again
	 * when returning from a callee.
	 * @return True if the alias analysis must be triggered again when returning
	 * from a method, otherwise false
	 */
	public boolean requiresAnalysisOnReturn();
	
	/**
	 * Checks whether this aliasing strategy has already computed aliases in the
	 * given method or not. Strategies that do not want to implement this method
	 * should always return true. 
	 * @param method The method to check
	 * @return True if this aliasing strategy has alreay computed aliases in the
	 * given method, otherwise false.
	 */
	public boolean hasProcessedMethod(SootMethod method);

}
