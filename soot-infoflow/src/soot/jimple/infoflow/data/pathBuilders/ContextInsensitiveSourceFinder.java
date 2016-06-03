package soot.jimple.infoflow.data.pathBuilders;

import heros.solver.CountingThreadPoolExecutor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

/**
 * Class for reconstructing abstraction paths from sinks to source
 * 
 * @author Steven Arzt
 */
public class ContextInsensitiveSourceFinder extends AbstractAbstractionPathBuilder {
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InfoflowResults results = new InfoflowResults();
	private final CountingThreadPoolExecutor executor;
	
	private static int lastTaskId = 0;
	private int numTasks = 0;
	
	/**
	 * Creates a new instance of the {@link ContextInsensitiveSourceFinder} class
	 * @param icfg The interprocedural control flow graph
	 * @param maxThreadNum The maximum number of threads to use
	 */
	public ContextInsensitiveSourceFinder(IInfoflowCFG icfg, int maxThreadNum) {
		super(icfg, false);
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
	 * Task for only finding sources, not the paths towards them
	 * 
	 * @author Steven Arzt
	 */
	private class SourceFindingTask implements Runnable {
		private final int taskId;
		private final AbstractionAtSink flagAbs;
		private final List<Abstraction> abstractionQueue = new LinkedList<Abstraction>();
		
		public SourceFindingTask(int taskId, AbstractionAtSink flagAbs, Abstraction abstraction) {
			this.taskId = taskId;
			this.flagAbs = flagAbs;
			this.abstractionQueue.add(abstraction);
		}
		
		@Override
		public void run() {
			while (!abstractionQueue.isEmpty()) {
				Abstraction abstraction = abstractionQueue.remove(0);
				
				if (abstraction.getSourceContext() != null) {
					// Register the result
					results.addResult(flagAbs.getAbstraction().getAccessPath(),
							flagAbs.getSinkStmt(),
							abstraction.getSourceContext().getAccessPath(),
							abstraction.getSourceContext().getStmt(),
							abstraction.getSourceContext().getUserData(),
							Collections.<Stmt>emptyList());
					
					// Sources may not have predecessors
					assert abstraction.getPredecessor() == null;
				}
				else
					if (abstraction.getPredecessor().registerPathFlag(taskId, numTasks))
						abstractionQueue.add(abstraction.getPredecessor());
				
				if (abstraction.getNeighbors() != null)
					for (Abstraction nb : abstraction.getNeighbors())
						if (nb.registerPathFlag(taskId, numTasks))
							abstractionQueue.add(nb);
			}
		}
	}
	
	@Override
	public void computeTaintPaths(final Set<AbstractionAtSink> res) {
		if (res.isEmpty())
			return;
		
		long beforePathTracking = System.nanoTime();
    	logger.info("Obtainted {} connections between sources and sinks", res.size());
    	
    	// Start the propagation tasks
    	int curResIdx = 0;
    	numTasks = res.size() + 1;
    	for (final AbstractionAtSink abs : res) {
    		logger.info("Building path " + ++curResIdx);
    		executor.execute(new SourceFindingTask(lastTaskId++, abs, abs.getAbstraction()));
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
	
	@Override
	public void shutdown() {
    	executor.shutdown();		
	}

	@Override
	public InfoflowResults getResults() {
		return this.results;
	}

}
