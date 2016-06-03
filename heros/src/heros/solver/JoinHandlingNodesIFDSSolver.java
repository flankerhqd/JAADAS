/*******************************************************************************
 * Copyright (c) 2013Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.solver;

import heros.EdgeFunction;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.JoinHandlingNode.JoinKey;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * An {@link IFDSSolver} that tracks paths for reporting. To do so, it requires that data-flow abstractions implement the LinkedNode interface.
 * The solver implements a cache of data-flow facts for each statement and source value. If for the same statement and source value the same
 * target value is seen again (as determined through a cache hit), then the solver propagates the cached value but at the same time links
 * both target values with one another.
 *  
 * @author Johannes Lerch
 */
public class JoinHandlingNodesIFDSSolver<N, D extends JoinHandlingNode<D>, M, I extends InterproceduralCFG<N, M>> extends IFDSSolver<N, D, M, I> {

	public JoinHandlingNodesIFDSSolver(IFDSTabulationProblem<N, D, M, I> ifdsProblem) {
		super(ifdsProblem);
	}

	protected final Map<CacheEntry, JoinHandlingNode<D>> cache = Maps.newHashMap();
	
	@Override
	protected void propagate(D sourceVal, N target, D targetVal, EdgeFunction<IFDSSolver.BinaryDomain> f, N relatedCallSite, boolean isUnbalancedReturn) {
		CacheEntry currentCacheEntry = new CacheEntry(target, sourceVal.createJoinKey(), targetVal.createJoinKey());

		boolean propagate = false;
		synchronized (this) {
			if (cache.containsKey(currentCacheEntry)) {
				JoinHandlingNode<D> existingTargetVal = cache.get(currentCacheEntry);
				if(!existingTargetVal.handleJoin(targetVal)) {
					propagate = true;
				}
			} else {
				cache.put(currentCacheEntry, targetVal);
				propagate = true;
			}
		}

		if (propagate)
			super.propagate(sourceVal, target, targetVal, f, relatedCallSite, isUnbalancedReturn);
		
	};
	
	
	private class CacheEntry {
		private N n;
		private JoinKey sourceKey;
		private JoinKey targetKey;

		public CacheEntry(N n, JoinKey sourceKey, JoinKey targetKey) {
			super();
			this.n = n;
			this.sourceKey = sourceKey;
			this.targetKey = targetKey;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sourceKey == null) ? 0 : sourceKey.hashCode());
			result = prime * result + ((targetKey == null) ? 0 : targetKey.hashCode());
			result = prime * result + ((n == null) ? 0 : n.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings({ "unchecked" })
			CacheEntry other = (CacheEntry) obj;
			if (sourceKey == null) {
				if (other.sourceKey != null)
					return false;
			} else if (!sourceKey.equals(other.sourceKey))
				return false;
			if (targetKey == null) {
				if (other.targetKey != null)
					return false;
			} else if (!targetKey.equals(other.targetKey))
				return false;
			if (n == null) {
				if (other.n != null)
					return false;
			} else if (!n.equals(other.n))
				return false;
			return true;
		}
	}	
	


}
