package soot.jimple.infoflow.solver;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.data.Abstraction;

import java.util.Set;

public interface IInfoflowSolver {
	
	/**
	 * Schedules the given edge for processing in the solver
	 * @param edge The edge to schedule for processing
	 * @return True if the edge was scheduled, otherwise (e.g., if the edge has
	 * already been processed earlier) false
	 */
	public boolean processEdge(PathEdge<Unit, Abstraction> edge);
	
	/**
	 * Gets the end summary of the given method for the given incoming
	 * abstraction
	 * @param m The method for which to get the end summary
	 * @param d3 The incoming fact (context) for which to get the end summary
	 * @return The end summary of the given method for the given incoming
	 * abstraction
	 */
	public Set<Pair<Unit, Abstraction>> endSummary(SootMethod m, Abstraction d3);
	
	public void injectContext(IInfoflowSolver otherSolver, SootMethod callee, Abstraction d3,
			Unit callSite, Abstraction d2, Abstraction d1);
	
	/**
	 * Cleans up some unused memory. Results will still be available afterwards,
	 * but no intermediate computation values.
	 */
	public void cleanup();
	
	/**
	 * Sets a handler that will be called when a followReturnsPastSeeds case
	 * happens, i.e., a taint leaves a method for which we have not seen any
	 * callers
	 * @param handler The handler to be called when a followReturnsPastSeeds
	 * case happens
	 */
	public void setFollowReturnsPastSeedsHandler(IFollowReturnsPastSeedsHandler handler);
	
}