package soot.jimple.infoflow.data.pathBuilders;

import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;


/**
 * Common interface for all path builder factories
 * 
 * @author Steven Arzt
 */
public interface IPathBuilderFactory {
	
	/**
	 * Creates a new path builder
	 * @param maxThreadNum The maximum number of threads to use
	 * @param icfg The interprocedural CFG to use
	 * @return The newly created path builder
	 */
	public IAbstractionPathBuilder createPathBuilder
			(int maxThreadNum, IInfoflowCFG icfg);
	
	/**
	 * Gets whether the {@link IAbstractionPathBuilder} object created by this
	 * factory supports the reconstruction of the exact paths between source
	 * and sink.
	 * @return True if the {@link IAbstractionPathBuilder} object constructed
	 * by this factory gives the exact propagation path between source and sink,
	 * false if it only reports source-to-sink connections without paths.
	 */
	public boolean supportsPathReconstruction();

}
