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
package heros;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdgeFunctionCache<N, D, M, V> implements EdgeFunctions<N, D, M, V> {
	
	protected final EdgeFunctions<N, D, M, V> delegate;
	
	protected final LoadingCache<NDNDKey, EdgeFunction<V>> normalCache;
	
	protected final LoadingCache<CallKey, EdgeFunction<V>> callCache;

	protected final LoadingCache<ReturnKey, EdgeFunction<V>> returnCache;

	protected final LoadingCache<NDNDKey, EdgeFunction<V>> callToReturnCache;

    Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unchecked")
	public EdgeFunctionCache(final EdgeFunctions<N, D, M, V> delegate, @SuppressWarnings("rawtypes") CacheBuilder builder) {
		this.delegate = delegate;
		
		normalCache = builder.build(new CacheLoader<NDNDKey, EdgeFunction<V>>() {
			public EdgeFunction<V> load(NDNDKey key) throws Exception {
				return delegate.getNormalEdgeFunction(key.getN1(), key.getD1(), key.getN2(), key.getD2());
			}
		});
		
		callCache = builder.build(new CacheLoader<CallKey, EdgeFunction<V>>() {
			public EdgeFunction<V> load(CallKey key) throws Exception {
				return delegate.getCallEdgeFunction(key.getCallSite(), key.getD1(), key.getCalleeMethod(), key.getD2());
			}
		});
		
		returnCache = builder.build(new CacheLoader<ReturnKey, EdgeFunction<V>>() {
			public EdgeFunction<V> load(ReturnKey key) throws Exception {
				return delegate.getReturnEdgeFunction(key.getCallSite(), key.getCalleeMethod(), key.getExitStmt(), key.getD1(), key.getReturnSite(), key.getD2());
			}
		});
		
		callToReturnCache = builder.build(new CacheLoader<NDNDKey, EdgeFunction<V>>() {
			public EdgeFunction<V> load(NDNDKey key) throws Exception {
				return delegate.getCallToReturnEdgeFunction(key.getN1(), key.getD1(), key.getN2(), key.getD2());
			}
		});
	}

	public EdgeFunction<V> getNormalEdgeFunction(N curr, D currNode, N succ, D succNode) {
		return normalCache.getUnchecked(new NDNDKey(curr, currNode, succ, succNode));
	}

	public EdgeFunction<V> getCallEdgeFunction(N callStmt, D srcNode, M destinationMethod, D destNode) {
		return callCache.getUnchecked(new CallKey(callStmt, srcNode, destinationMethod, destNode));
	}

	public EdgeFunction<V> getReturnEdgeFunction(N callSite, M calleeMethod, N exitStmt, D exitNode, N returnSite, D retNode) {
		return returnCache.getUnchecked(new ReturnKey(callSite, calleeMethod, exitStmt, exitNode, returnSite, retNode));
	}

	public EdgeFunction<V> getCallToReturnEdgeFunction(N callSite, D callNode, N returnSite, D returnSideNode) {
		return callToReturnCache.getUnchecked(new NDNDKey(callSite, callNode, returnSite, returnSideNode));
	}


	private class NDNDKey {
		private final N n1, n2;
		private final D d1, d2;

		public NDNDKey(N n1, D d1, N n2, D d2) {
			this.n1 = n1;
			this.n2 = n2;
			this.d1 = d1;
			this.d2 = d2;
		}

		public N getN1() {
			return n1;
		}

		public D getD1() {
			return d1;
		}

		public N getN2() {
			return n2;
		}

		public D getD2() {
			return d2;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((d1 == null) ? 0 : d1.hashCode());
			result = prime * result + ((d2 == null) ? 0 : d2.hashCode());
			result = prime * result + ((n1 == null) ? 0 : n1.hashCode());
			result = prime * result + ((n2 == null) ? 0 : n2.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			NDNDKey other = (NDNDKey) obj;
			if (d1 == null) {
				if (other.d1 != null)
					return false;
			} else if (!d1.equals(other.d1))
				return false;
			if (d2 == null) {
				if (other.d2 != null)
					return false;
			} else if (!d2.equals(other.d2))
				return false;
			if (n1 == null) {
				if (other.n1 != null)
					return false;
			} else if (!n1.equals(other.n1))
				return false;
			if (n2 == null) {
				if (other.n2 != null)
					return false;
			} else if (!n2.equals(other.n2))
				return false;
			return true;
		}
	}
	
	private class CallKey {
		private final N callSite;
		private final M calleeMethod;
		private final D d1, d2;

		public CallKey(N callSite, D d1, M calleeMethod, D d2) {
			this.callSite = callSite;
			this.calleeMethod = calleeMethod;
			this.d1 = d1;
			this.d2 = d2;
		}

		public N getCallSite() {
			return callSite;
		}

		public D getD1() {
			return d1;
		}

		public M getCalleeMethod() {
			return calleeMethod;
		}

		public D getD2() {
			return d2;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((d1 == null) ? 0 : d1.hashCode());
			result = prime * result + ((d2 == null) ? 0 : d2.hashCode());
			result = prime * result + ((callSite == null) ? 0 : callSite.hashCode());
			result = prime * result + ((calleeMethod == null) ? 0 : calleeMethod.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			CallKey other = (CallKey) obj;
			if (d1 == null) {
				if (other.d1 != null)
					return false;
			} else if (!d1.equals(other.d1))
				return false;
			if (d2 == null) {
				if (other.d2 != null)
					return false;
			} else if (!d2.equals(other.d2))
				return false;
			if (callSite == null) {
				if (other.callSite != null)
					return false;
			} else if (!callSite.equals(other.callSite))
				return false;
			if (calleeMethod == null) {
				if (other.calleeMethod != null)
					return false;
			} else if (!calleeMethod.equals(other.calleeMethod))
				return false;
			return true;
		}
	}


	private class ReturnKey extends CallKey {
		
		private final N exitStmt, returnSite;

		public ReturnKey(N callSite, M calleeMethod, N exitStmt, D exitNode, N returnSite, D retNode) {
			super(callSite, exitNode, calleeMethod, retNode);
			this.exitStmt = exitStmt;
			this.returnSite = returnSite;
		}
		
		public N getExitStmt() {
			return exitStmt;
		}
		
		public N getReturnSite() {
			return returnSite;
		}

		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((exitStmt == null) ? 0 : exitStmt.hashCode());
			result = prime * result + ((returnSite == null) ? 0 : returnSite.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			ReturnKey other = (ReturnKey) obj;
			if (exitStmt == null) {
				if (other.exitStmt != null)
					return false;
			} else if (!exitStmt.equals(other.exitStmt))
				return false;
			if (returnSite == null) {
				if (other.returnSite != null)
					return false;
			} else if (!returnSite.equals(other.returnSite))
				return false;
			return true;
		}
	}


	public void printStats() {
        logger.debug("Stats for edge-function cache:\n" +
                     "Normal:         {}\n"+
                     "Call:           {}\n"+
                     "Return:         {}\n"+
                     "Call-to-return: {}\n",
                normalCache.stats(), callCache.stats(),returnCache.stats(),callToReturnCache.stats());
	}

}
