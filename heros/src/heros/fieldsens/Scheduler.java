/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.fieldsens;

import java.util.LinkedList;
import com.google.common.collect.Lists;

public class Scheduler {

	private LinkedList<Runnable> worklist = Lists.newLinkedList();
	
	public void schedule(Runnable job) {
		worklist.add(job);
	}

	public void runAndAwaitCompletion() {
		while(!worklist.isEmpty()) {
			worklist.removeLast().run();
		}
	}

}
