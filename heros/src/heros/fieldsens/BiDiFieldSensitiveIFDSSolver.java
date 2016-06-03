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


import heros.InterproceduralCFG;
import heros.fieldsens.SourceStmtAnnotatedMethodAnalyzer.Synchronizer;

import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;


public class BiDiFieldSensitiveIFDSSolver<Field, Fact, Stmt, Method, I extends InterproceduralCFG<Stmt, Method>> {

	private FieldSensitiveIFDSSolver<Field, Fact, Stmt, Method, I> forwardSolver;
	private FieldSensitiveIFDSSolver<Field, Fact, Stmt, Method, I> backwardSolver;
	private Scheduler scheduler;
	private SynchronizerImpl<Stmt> forwardSynchronizer;
	private SynchronizerImpl<Stmt> backwardSynchronizer;

	public BiDiFieldSensitiveIFDSSolver(IFDSTabulationProblem<Stmt, Field, Fact, Method, I> forwardProblem,
			IFDSTabulationProblem<Stmt, Field, Fact, Method, I> backwardProblem, 
			FactMergeHandler<Fact> factHandler, 
			Debugger<Field, Fact, Stmt, Method, I> debugger,
			Scheduler scheduler) {
		
		this.scheduler = scheduler;
		
		forwardSynchronizer = new SynchronizerImpl<Stmt>();
		backwardSynchronizer = new SynchronizerImpl<Stmt>();
		forwardSynchronizer.otherSynchronizer = backwardSynchronizer;
		backwardSynchronizer.otherSynchronizer = forwardSynchronizer;
		
		forwardSolver = createSolver(forwardProblem, factHandler, debugger, forwardSynchronizer);
		backwardSolver = createSolver(backwardProblem, factHandler, debugger, backwardSynchronizer);
	}

	private FieldSensitiveIFDSSolver<Field, Fact, Stmt, Method, I> createSolver(IFDSTabulationProblem<Stmt, Field, Fact, Method, I> problem, 
			FactMergeHandler<Fact> factHandler, Debugger<Field, Fact, Stmt, Method, I> debugger, final SynchronizerImpl<Stmt> synchronizer) {
		return new FieldSensitiveIFDSSolver<Field, Fact, Stmt, Method, I>(problem, factHandler, debugger, scheduler) {
			@Override
			protected MethodAnalyzer<Field, Fact, Stmt, Method> createMethodAnalyzer(Method method) {
				return new SourceStmtAnnotatedMethodAnalyzer<Field, Fact, Stmt, Method>(method, context, synchronizer);
			}
		};
	}
	
	private static class SynchronizerImpl<Stmt> implements Synchronizer<Stmt> {
		
		private SynchronizerImpl<Stmt> otherSynchronizer;
		private Set<Stmt> leakedSources = Sets.newHashSet();
		private HashMultimap<Stmt, Runnable> pausedJobs = HashMultimap.create();

		@Override
		public void synchronizeOnStmt(Stmt stmt, Runnable job) {
			leakedSources.add(stmt);
			if(otherSynchronizer.leakedSources.contains(stmt)) {
				job.run();
				for(Runnable runnable : otherSynchronizer.pausedJobs.get(stmt)) {
					runnable.run();
				}
				otherSynchronizer.pausedJobs.removeAll(stmt);
			}
			else {
				pausedJobs.put(stmt, job);
			}
		}
	}
}
