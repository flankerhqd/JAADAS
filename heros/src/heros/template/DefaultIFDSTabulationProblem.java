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
package heros.template;

import heros.FlowFunctions;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;

/**
 * This is a template for {@link IFDSTabulationProblem}s that automatically caches values
 * that ought to be cached. This class uses the Factory Method design pattern.
 * The {@link InterproceduralCFG} is passed into the constructor so that it can be conveniently
 * reused for solving multiple different {@link IFDSTabulationProblem}s.
 * This class is specific to Soot. 
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public abstract class DefaultIFDSTabulationProblem<N,D,M, I extends InterproceduralCFG<N,M>> implements IFDSTabulationProblem<N,D,M,I> {

	private final I icfg;
	private FlowFunctions<N,D,M> flowFunctions;
	private D zeroValue;
	
	public DefaultIFDSTabulationProblem(I icfg) {
		this.icfg = icfg;
	}
	
	protected abstract FlowFunctions<N, D, M> createFlowFunctionsFactory();

	protected abstract D createZeroValue();

	@Override
	public final FlowFunctions<N,D,M> flowFunctions() {
		if(flowFunctions==null) {
			flowFunctions = createFlowFunctionsFactory();
		}
		return flowFunctions;
	}

	@Override
	public I interproceduralCFG() {
		return icfg;
	}

	@Override
	public final D zeroValue() {
		if(zeroValue==null) {
			zeroValue = createZeroValue();
		}
		return zeroValue;
	}
	
	@Override
	public boolean followReturnsPastSeeds() {
		return false;
	}

	@Override
	public boolean autoAddZero() {
		return true;
	}
	
	@Override
	public int numThreads() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	@Override
	public boolean computeValues() {
		return true;
	}
}
