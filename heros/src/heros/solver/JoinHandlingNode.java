/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.solver;

import java.util.Arrays;


public interface JoinHandlingNode<T> {

	/**
	 * 
	 * @param joiningNode the node abstraction that was propagated to the same target after {@code this} node.
	 * @return true if the join could be handled and no further propagation of the {@code joiningNode} is necessary, otherwise false meaning 
	 * the node should be propagated by the solver.
	 */
	public boolean handleJoin(T joiningNode);
	
	/**
	 * 
	 * @return a JoinKey object used to identify which node abstractions require manual join handling. 
	 * For nodes with {@code equal} JoinKey instances {@link #handleJoin(JoinHandlingNode)} will be called.
	 */
	public JoinKey createJoinKey();
	
	public void setCallingContext(T callingContext);
	
	public static class JoinKey {
		private Object[] elements;

		/**
		 * 
		 * @param elements Passed elements must be immutable with respect to their hashCode and equals implementations.
		 */
		public JoinKey(Object... elements) {
			this.elements = elements;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(elements);
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
			JoinKey other = (JoinKey) obj;
			if (!Arrays.equals(elements, other.elements))
				return false;
			return true;
		}
	}
}
