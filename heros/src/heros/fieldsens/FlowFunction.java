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
package heros.fieldsens;

import heros.fieldsens.structs.WrappedFact;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A flow function computes which of the finitely many D-type values are reachable
 * from the current source values. Typically there will be one such function
 * associated with every possible control flow. 
 * 
 * <b>NOTE:</b> To be able to produce <b>deterministic benchmarking results</b>, we have found that
 * it helps to return {@link LinkedHashSet}s from {@link #computeTargets(Object)}. This is
 * because the duration of IDE's fixed point iteration may depend on the iteration order.
 * Within the solver, we have tried to fix this order as much as possible, but the
 * order, in general, does also depend on the order in which the result set
 * of {@link #computeTargets(Object)} is traversed.
 * 
 * <b>NOTE:</b> Methods defined on this type may be called simultaneously by different threads.
 * Hence, classes implementing this interface should synchronize accesses to
 * any mutable shared state.
 * 
 * @param <D> The type of data-flow facts to be computed by the tabulation problem.
 */
public interface FlowFunction<FieldRef, D, Stmt, Method> {

	/**
	 * Returns the target values reachable from the source.
	 */
	Set<ConstrainedFact<FieldRef, D, Stmt, Method>> computeTargets(D source, AccessPathHandler<FieldRef, D, Stmt, Method> accPathHandler);
	
	
	public static class ConstrainedFact<FieldRef, D, Stmt, Method> {
		
		private WrappedFact<FieldRef, D, Stmt, Method> fact;
		private Constraint<FieldRef> constraint;
		
		ConstrainedFact(WrappedFact<FieldRef, D, Stmt, Method> fact) {
			this.fact = fact;
			this.constraint = null;
		}
		
		ConstrainedFact(WrappedFact<FieldRef, D, Stmt, Method> fact, Constraint<FieldRef> constraint) {
			this.fact = fact;
			this.constraint = constraint;
		}
		
		public WrappedFact<FieldRef, D, Stmt, Method> getFact() {
			return fact;
		}
		
		public Constraint<FieldRef> getConstraint() {
			return constraint;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((constraint == null) ? 0 : constraint.hashCode());
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ConstrainedFact))
				return false;
			ConstrainedFact other = (ConstrainedFact) obj;
			if (constraint == null) {
				if (other.constraint != null)
					return false;
			} else if (!constraint.equals(other.constraint))
				return false;
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return fact.toString()+"<"+constraint+">";
		}
	}
	
	public interface Constraint<FieldRef> {
		AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath);
		
		boolean canBeAppliedTo(AccessPath<FieldRef> accPath);
	}
	
	public class WriteFieldConstraint<FieldRef> implements Constraint<FieldRef> {
		private FieldRef fieldRef;

		public WriteFieldConstraint(FieldRef fieldRef) {
			this.fieldRef = fieldRef;
		}

		@Override
		public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath) {
			return accPath.appendExcludedFieldReference(fieldRef);
		}
		
		@Override
		public String toString() {
			return "^"+fieldRef.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fieldRef == null) ? 0 : fieldRef.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof WriteFieldConstraint))
				return false;
			WriteFieldConstraint other = (WriteFieldConstraint) obj;
			if (fieldRef == null) {
				if (other.fieldRef != null)
					return false;
			} else if (!fieldRef.equals(other.fieldRef))
				return false;
			return true;
		}

		@Override
		public boolean canBeAppliedTo(AccessPath<FieldRef> accPath) {
			return true;
		}
	}
	
	public class ReadFieldConstraint<FieldRef> implements Constraint<FieldRef> {

		private FieldRef fieldRef;

		public ReadFieldConstraint(FieldRef fieldRef) {
			this.fieldRef = fieldRef;
		}
		
		@Override
		public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath) {
			return accPath.append(fieldRef);
		}
		
		@Override
		public String toString() {
			return fieldRef.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fieldRef == null) ? 0 : fieldRef.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ReadFieldConstraint))
				return false;
			ReadFieldConstraint other = (ReadFieldConstraint) obj;
			if (fieldRef == null) {
				if (other.fieldRef != null)
					return false;
			} else if (!fieldRef.equals(other.fieldRef))
				return false;
			return true;
		}

		@Override
		public boolean canBeAppliedTo(AccessPath<FieldRef> accPath) {
			return !accPath.isAccessInExclusions(fieldRef);
		}
	}
}
