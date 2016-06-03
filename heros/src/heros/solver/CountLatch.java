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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A synchronization aid similar to {@link CountDownLatch} but with the ability
 * to also count up. This is useful to wait until a variable number of tasks
 * have completed. {@link #awaitZero()} will block until the count reaches zero.
 */
public class CountLatch {

	@SuppressWarnings("serial")
	private static final class Sync extends AbstractQueuedSynchronizer {

		Sync(int count) {
			setState(count);
		}

		int getCount() {
			return getState();
		}

        void reset() {
            setState(0);
        }

        @Override
		protected int tryAcquireShared(int acquires) {
			return (getState() == 0) ? 1 : -1;
		}

		protected int acquireNonBlocking(int acquires) {
			// increment count
			for (;;) {
				int c = getState();
				int nextc = c + 1;
				if (compareAndSetState(c, nextc))
					return 1;
			}
		}

        @Override
		protected boolean tryReleaseShared(int releases) {
			// Decrement count; signal when transition to zero
			for (;;) {
				int c = getState();
				if (c == 0)
					return false;
				int nextc = c - 1;
				if (compareAndSetState(c, nextc))
					return nextc == 0;
			}
		}
	}

	private final Sync sync;

	public CountLatch(int count) {
		this.sync = new Sync(count);
	}

	public void awaitZero() throws InterruptedException {
		sync.acquireShared(1);
	}

	public boolean awaitZero(long timeout, TimeUnit unit) throws InterruptedException {
		return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
	}

	public void increment() {
		sync.acquireNonBlocking(1);
	}

	public void decrement() {
		sync.releaseShared(1);
	}

    /**
     * Resets the counter to zero. But waiting threads won't be released somehow.
     * So this interrupts the threads so that they escape from their waiting state.
     */
    public void resetAndInterrupt(){
        sync.reset();
        for (int i = 0; i < 3; i++) //Because it is a best effort thing, do it three times and hope for the best.
            for (Thread t : sync.getQueuedThreads())
                t.interrupt();
        sync.reset(); //Just in case a thread would've incremented the counter again.
    }

	public String toString() {
		return super.toString() + "[Count = " + sync.getCount() + "]";
	}

}
