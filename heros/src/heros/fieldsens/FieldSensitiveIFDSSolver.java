/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch, Johannes Spaeth.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch, Johannes Spaeth - initial API and implementation
 ******************************************************************************/

package heros.fieldsens;

import heros.InterproceduralCFG;
import heros.utilities.DefaultValueMap;

import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldSensitiveIFDSSolver<FieldRef, D, N, M, I extends InterproceduralCFG<N, M>> {

	protected static final Logger logger = LoggerFactory.getLogger(FieldSensitiveIFDSSolver.class);
	
	private DefaultValueMap<M, MethodAnalyzer<FieldRef, D, N, M>> methodAnalyzers = new DefaultValueMap<M, MethodAnalyzer<FieldRef, D,N, M>>() {
		@Override
		protected MethodAnalyzer<FieldRef, D, N, M> createItem(M key) {
			return createMethodAnalyzer(key);
		}
	};

	private IFDSTabulationProblem<N, FieldRef, D, M, I> tabulationProblem;
	protected Context<FieldRef, D, N,M> context;
	private Debugger<FieldRef, D, N, M, I> debugger;
	private Scheduler scheduler;

	public FieldSensitiveIFDSSolver(IFDSTabulationProblem<N,FieldRef,D,M,I> tabulationProblem, FactMergeHandler<D> factHandler, Debugger<FieldRef, D, N, M, I> debugger, Scheduler scheduler) {
		this.tabulationProblem = tabulationProblem;
		this.scheduler = scheduler;
		this.debugger = debugger == null ? new Debugger.NullDebugger<FieldRef, D, N, M, I>() : debugger;
		this.debugger.setICFG(tabulationProblem.interproceduralCFG());
		context = initContext(tabulationProblem, factHandler);
		submitInitialSeeds();
	}

	private Context<FieldRef, D, N, M> initContext(IFDSTabulationProblem<N, FieldRef, D, M, I> tabulationProblem, FactMergeHandler<D> factHandler) {
		 return new Context<FieldRef, D, N, M>(tabulationProblem, scheduler, factHandler) {
			@Override
			public MethodAnalyzer<FieldRef, D, N, M> getAnalyzer(M method) {
				if(method == null)
					throw new IllegalArgumentException("Method must be not null");
				return methodAnalyzers.getOrCreate(method);
			}
		};
	}
	
	protected MethodAnalyzer<FieldRef, D, N, M> createMethodAnalyzer(M method) {
		return new MethodAnalyzerImpl<FieldRef, D, N, M>(method, context);
	}

	/**
	 * Schedules the processing of initial seeds, initiating the analysis.
	 */
	private void submitInitialSeeds() {
		for(Entry<N, Set<D>> seed: tabulationProblem.initialSeeds().entrySet()) {
			N startPoint = seed.getKey();
			MethodAnalyzer<FieldRef, D,N,M> analyzer = methodAnalyzers.getOrCreate(tabulationProblem.interproceduralCFG().getMethodOf(startPoint));
			for(D val: seed.getValue()) {
				analyzer.addInitialSeed(startPoint, val);
				debugger.initialSeed(startPoint);
			}
		}
	}
}
