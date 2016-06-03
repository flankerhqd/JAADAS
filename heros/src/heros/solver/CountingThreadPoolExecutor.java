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

import heros.util.SootThreadGroup;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ThreadPoolExecutor} which keeps track of the number of spawned
 * tasks to allow clients to await their completion. 
 */
public class CountingThreadPoolExecutor extends ThreadPoolExecutor {
	
    protected static final Logger logger = LoggerFactory.getLogger(IDESolver.class);

    protected final CountLatch numRunningTasks = new CountLatch(0);
	
	protected volatile Throwable exception = null;

	public CountingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(new SootThreadGroup(), r);
			}
		});
	}

	@Override
	public void execute(Runnable command) {
		try {
			numRunningTasks.increment();
			super.execute(command);
		}
		catch (RejectedExecutionException ex) {
			// If we were unable to submit the task, we may not count it!
			numRunningTasks.decrement();
			throw ex;
		}
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if(t!=null) {
			exception = t;
			logger.error("Worker thread execution failed: " + t.getMessage(), t);
			
			shutdownNow();
            numRunningTasks.resetAndInterrupt();
		}
		else {
			numRunningTasks.decrement();
		}
		super.afterExecute(r, t);
	}

	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion() throws InterruptedException {
		numRunningTasks.awaitZero();
	}
	
	/**
	 * Awaits the completion of all spawned tasks.
	 */
	public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
		numRunningTasks.awaitZero(timeout, unit);
	}
	
	/**
	 * Returns the exception thrown during task execution (if any).
	 */
	public Throwable getException() {
		return exception;
	}

}