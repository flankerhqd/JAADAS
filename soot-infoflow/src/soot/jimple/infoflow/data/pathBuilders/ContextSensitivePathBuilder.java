package soot.jimple.infoflow.data.pathBuilders;

import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.data.SourceContextAndPath;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Class for reconstructing abstraction paths from sinks to source. This builder
 * is context-sensitive which makes it more precise than the
 * {@link ContextInsensitivePathBuilder}, but also a bit slower.
 * 
 * @author Steven Arzt
 */
public class ContextSensitivePathBuilder extends AbstractAbstractionPathBuilder {
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InfoflowResults results = new InfoflowResults();
	private final CountingThreadPoolExecutor executor;
			
	/**
	 * Creates a new instance of the {@link ContextSensitivePathBuilder} class
	 * @param icfg The interprocedural control flow graph
	 * @param maxThreadNum The maximum number of threads to use
	 * @param reconstructPaths True if the exact propagation path between source
	 * and sink shall be reconstructed.
	 */
	public ContextSensitivePathBuilder(IInfoflowCFG icfg, int maxThreadNum,
			boolean reconstructPaths) {
		super(icfg, reconstructPaths);
        int numThreads = Runtime.getRuntime().availableProcessors();
		this.executor = createExecutor(maxThreadNum == -1 ? numThreads
				: Math.min(maxThreadNum, numThreads));
	}
	
	/**
	 * Creates a new executor object for spawning worker threads
	 * @param numThreads The number of threads to use
	 * @return The generated executor
	 */
	private CountingThreadPoolExecutor createExecutor(int numThreads) {
		return new CountingThreadPoolExecutor
				(numThreads, Integer.MAX_VALUE, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}
	
	/**
	 * Task for tracking back the path from sink to source.
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final Abstraction abstraction;
		
		public SourceFindingTask(Abstraction abstraction) {
			this.abstraction = abstraction;
		}
		
		@Override
		public void run() {
			final Set<SourceContextAndPath> paths = abstraction.getPaths();
			final Abstraction pred = abstraction.getPredecessor();
			
			if (pred != null) {
				for (SourceContextAndPath scap : paths) {
					// Process the predecessor
					if (processPredecessor(scap, pred))
						// Schedule the predecessor
						executor.execute(new SourceFindingTask(pred));
					
					// Process the predecessor's neighbors
					if (pred.getNeighbors() != null)
						for (Abstraction neighbor : pred.getNeighbors())
							if (processPredecessor(scap, neighbor))
								// Schedule the predecessor
								executor.execute(new SourceFindingTask(neighbor));
				}
			}
		}

		private boolean processPredecessor(SourceContextAndPath scap, Abstraction pred) {
			// Shortcut: If this a call-to-return node, we should not enter and
			// immediately leave again for performance reasons.
			if (pred.getCurrentStmt() != null
					&& pred.getCurrentStmt() == pred.getCorrespondingCallSite()) {
				SourceContextAndPath extendedScap = scap.extendPath(pred, reconstructPaths);
				if (extendedScap == null)
					return false;
				
				checkForSource(pred, extendedScap);
				return pred.addPathElement(extendedScap);
			}
			
			// If we enter a method, we put it on the stack
			SourceContextAndPath extendedScap = scap.extendPath(pred, reconstructPaths);
			if (extendedScap == null)
				return false;
			
			// Do we process a method return?
			if (pred.getCurrentStmt() != null 
					&& pred.getCurrentStmt().containsInvokeExpr()) {
				// Pop the top item off the call stack. This gives us the item
				// and the new SCAP without the item we popped off.
				Pair<SourceContextAndPath, Stmt> pathAndItem =
						extendedScap.popTopCallStackItem();
				if (pathAndItem != null) {
					Stmt topCallStackItem = pathAndItem.getO2();
					// Make sure that we don't follow an unrealizable path
					if (topCallStackItem != pred.getCurrentStmt())
						return false;
					
					// We have returned from a function
					extendedScap = pathAndItem.getO1();
				}
			}
			
			// Add the new path
			checkForSource(pred, extendedScap);
			return pred.addPathElement(extendedScap);
		}
		
	}
	
	/**
	 * Checks whether the given abstraction is a source. If so, a result entry
	 * is created.
	 * @param abs The abstraction to check
	 * @param scap The path leading up to the current abstraction
	 * @return True if the current abstraction is a source, otherwise false
	 */
	protected boolean checkForSource(Abstraction abs, SourceContextAndPath scap) {
		if (abs.getPredecessor() != null)
			return false;
		
		// If we have no predecessors, this must be a source
		assert abs.getSourceContext() != null;
		assert abs.getNeighbors() == null;
		
		// Register the source that we have found
		SourceContext sourceContext = abs.getSourceContext();
		results.addResult(scap.getAccessPath(),
				scap.getStmt(),
				sourceContext.getAccessPath(),
				sourceContext.getStmt(),
				sourceContext.getUserData(),
				scap.getPath());
		return true;
	}
	
	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink> res) {
		logger.info("Context-sensitive path reconstructor started");
		runSourceFindingTasks(res);
	}
	
	private void runSourceFindingTasks(final Set<AbstractionAtSink> res) {
		if (res.isEmpty())
			return;
		
		long beforePathTracking = System.nanoTime();
    	logger.info("Obtainted {} connections between sources and sinks", res.size());
    	
    	// Start the propagation tasks
    	int curResIdx = 0;
    	for (final AbstractionAtSink abs : res) {
    		logger.info("Building path " + ++curResIdx);    		
   			buildPathForAbstraction(abs);
   			
   			// Also build paths for the neighbors of our result abstraction
   			if (abs.getAbstraction().getNeighbors() != null)
   				for (Abstraction neighbor : abs.getAbstraction().getNeighbors()) {
   					AbstractionAtSink neighborAtSink = new AbstractionAtSink(neighbor,
   							abs.getSinkStmt());
   		   			buildPathForAbstraction(neighborAtSink);
   				}
    	}

    	try {
			executor.awaitCompletion();
		} catch (InterruptedException ex) {
			logger.error("Could not wait for path executor completion: {0}", ex.getMessage());
			ex.printStackTrace();
		}
    	
    	logger.info("Path processing took {} seconds in total",
    			(System.nanoTime() - beforePathTracking) / 1E9);
	}
	
	/**
	 * Builds the path for the given abstraction that reached a sink
	 * @param abs The abstraction that reached a sink
	 */
	private void buildPathForAbstraction(final AbstractionAtSink abs) {
		SourceContextAndPath scap = new SourceContextAndPath(
				abs.getAbstraction().getAccessPath(), abs.getSinkStmt());
		scap = scap.extendPath(abs.getAbstraction());
		abs.getAbstraction().addPathElement(scap);
		
		if (!checkForSource(abs.getAbstraction(), scap))
			executor.execute(new SourceFindingTask(abs.getAbstraction()));
	}
	
	@Override
	public void shutdown() {
    	executor.shutdown();		
	}

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

}
