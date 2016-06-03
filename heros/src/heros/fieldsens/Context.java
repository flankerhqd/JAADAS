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

public abstract class Context<Field, Fact, Stmt, Method> {

	public final InterproceduralCFG<Stmt, Method> icfg;
	public final Scheduler scheduler;
	public final Fact zeroValue;
	public final boolean followReturnsPastSeeds;
	public final FactMergeHandler<Fact> factHandler;
	public final ZeroHandler<Field> zeroHandler;
	public final FlowFunctions<Stmt, Field, Fact, Method> flowFunctions;
	
	Context(IFDSTabulationProblem<Stmt, Field, Fact, Method, ? extends InterproceduralCFG<Stmt, Method>> tabulationProblem, 
			Scheduler scheduler, FactMergeHandler<Fact> factHandler) {
		this.icfg = tabulationProblem.interproceduralCFG();
		this.flowFunctions = tabulationProblem.flowFunctions();
		this.scheduler = scheduler;
		this.zeroValue = tabulationProblem.zeroValue();
		this.followReturnsPastSeeds = tabulationProblem.followReturnsPastSeeds();
		this.factHandler = factHandler;
		this.zeroHandler = tabulationProblem.zeroHandler();
	}
	
	public abstract MethodAnalyzer<Field, Fact, Stmt, Method> getAnalyzer(Method method);
}
