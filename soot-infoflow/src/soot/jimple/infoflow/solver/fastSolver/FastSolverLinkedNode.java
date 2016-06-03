package soot.jimple.infoflow.solver.fastSolver;

import heros.solver.LinkedNode;

/**
 * Special interface of {@link LinkedNode} that allows the FastSolver to reduce
 * the size of the taint graph
 * 
 * @author Steven Arzt
 */
public interface FastSolverLinkedNode<D, N> extends LinkedNode<D> {
	
	/**
	 * Explicitly sets the predecessor of this node.
	 * @param predecessor The predecessor node to set
	 */
	public void setPredecessor(D predecessor);
	
	/**
	 * Gets the predecessor of this node
	 * @return The predecessor of this node is applicable, null for source nodes
	 */
	public D getPredecessor();
	
}
