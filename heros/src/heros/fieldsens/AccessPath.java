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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
public class AccessPath<T> {
	
	public static <T> AccessPath<T> empty() {
		return new AccessPath<T>();
	}
	
	private final T[] accesses;
	private final Set<T> exclusions;
	
	public AccessPath() {
		accesses = (T[]) new Object[0];
		exclusions = Sets.newHashSet();
	}
	
	AccessPath(T[] accesses, Set<T> exclusions) {
		this.accesses = accesses;
		this.exclusions = exclusions;
	}

	public boolean isAccessInExclusions(T fieldReference) {
		return exclusions.contains(fieldReference);
	}
	
	public boolean hasAllExclusionsOf(AccessPath<T> accPath) {
		return exclusions.containsAll(accPath.exclusions);
	}
	
	public AccessPath<T> append(T... fieldReferences) {
		if(fieldReferences.length == 0)
			return this;
		
		if(isAccessInExclusions(fieldReferences[0]))
			throw new IllegalArgumentException("FieldRef "+Arrays.toString(fieldReferences)+" cannot be added to "+toString());

		T[] newAccesses = Arrays.copyOf(accesses, accesses.length+fieldReferences.length);
		System.arraycopy(fieldReferences, 0, newAccesses, accesses.length, fieldReferences.length);
		return new AccessPath<T>(newAccesses, Sets.<T>newHashSet());
	}

	public AccessPath<T> prepend(T fieldRef) {
		T[] newAccesses = (T[]) new Object[accesses.length+1];
		newAccesses[0] = fieldRef;
		System.arraycopy(accesses, 0, newAccesses, 1, accesses.length);
		return new AccessPath<T>(newAccesses, exclusions);
	}

	public AccessPath<T> removeFirst() {
		T[] newAccesses = (T[]) new Object[accesses.length-1];
		System.arraycopy(accesses, 1, newAccesses, 0, accesses.length-1);
		return new AccessPath<T>(newAccesses, exclusions);
	}
	
	public AccessPath<T> appendExcludedFieldReference(Collection<T> fieldReferences) {
		HashSet<T> newExclusions = Sets.newHashSet(fieldReferences);
		newExclusions.addAll(exclusions);
		return new AccessPath<T>(accesses, newExclusions);
	}
	
	public AccessPath<T> appendExcludedFieldReference(T... fieldReferences) {
		HashSet<T> newExclusions = Sets.newHashSet(fieldReferences);
		newExclusions.addAll(exclusions);
		return new AccessPath<T>(accesses, newExclusions);
	}

	public static enum PrefixTestResult {
		GUARANTEED_PREFIX(2), POTENTIAL_PREFIX(1), NO_PREFIX(0);
		
		private int value;

		private PrefixTestResult(int value) {
			this.value = value;
		}
		
		public boolean atLeast(PrefixTestResult minimum) {
			return value >= minimum.value;
		}
	}
	
	public PrefixTestResult isPrefixOf(AccessPath<T> accessPath) {
		if(accesses.length > accessPath.accesses.length)
			return PrefixTestResult.NO_PREFIX;
				
		for(int i=0; i<accesses.length; i++) {
			if(!accesses[i].equals(accessPath.accesses[i]))
				return PrefixTestResult.NO_PREFIX;
		}
		
		if(accesses.length < accessPath.accesses.length) {
			if(exclusions.contains(accessPath.accesses[accesses.length]))
				return PrefixTestResult.NO_PREFIX;
			else
				return PrefixTestResult.GUARANTEED_PREFIX;
		}
		
		if(exclusions.isEmpty())
			return PrefixTestResult.GUARANTEED_PREFIX;
		if(accessPath.exclusions.isEmpty())
			return PrefixTestResult.NO_PREFIX;
		
		boolean intersection = !Sets.intersection(exclusions, accessPath.exclusions).isEmpty();
		boolean containsAll = exclusions.containsAll(accessPath.exclusions);
		boolean oppositeContainsAll = accessPath.exclusions.containsAll(exclusions);
		boolean potentialMatch = oppositeContainsAll || !intersection || (!containsAll && !oppositeContainsAll);
		if(potentialMatch) {
			if(oppositeContainsAll)
				return PrefixTestResult.GUARANTEED_PREFIX;
			else
				return PrefixTestResult.POTENTIAL_PREFIX;
		}
		return PrefixTestResult.NO_PREFIX;
	}

	public Delta<T> getDeltaTo(AccessPath<T> accPath) {
		assert isPrefixOf(accPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX);
		HashSet<T> mergedExclusions = Sets.newHashSet(accPath.exclusions);
		if(accesses.length == accPath.accesses.length)
			mergedExclusions.addAll(exclusions);
		Delta<T> delta = new Delta<T>(Arrays.copyOfRange(accPath.accesses, accesses.length, accPath.accesses.length), mergedExclusions);
		assert (isPrefixOf(accPath).atLeast(PrefixTestResult.POTENTIAL_PREFIX) && accPath.isPrefixOf(delta.applyTo(this)) == PrefixTestResult.GUARANTEED_PREFIX) 
				|| (isPrefixOf(accPath) == PrefixTestResult.GUARANTEED_PREFIX && accPath.equals(delta.applyTo(this)));
		return delta;
	}
	
	public static class Delta<T> {
		final T[] accesses;
		final Set<T> exclusions;

		protected Delta(T[] accesses, Set<T> exclusions) {
			this.accesses = accesses;
			this.exclusions = exclusions;
		}
		
		public boolean canBeAppliedTo(AccessPath<T> accPath) {
			if(accesses.length > 0)
				return !accPath.isAccessInExclusions(accesses[0]);
			else
				return true;
		}
		
		public AccessPath<T> applyTo(AccessPath<T> accPath) {
			return accPath.append(accesses).appendExcludedFieldReference(exclusions);
		}
		
		@Override
		public String toString() {
			String result = accesses.length > 0 ? "."+Joiner.on(".").join(accesses) : "";
			if(!exclusions.isEmpty())
				result += "^" + Joiner.on(",").join(exclusions);
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(accesses);
			result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
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
			Delta other = (Delta) obj;
			if (!Arrays.equals(accesses, other.accesses))
				return false;
			if (exclusions == null) {
				if (other.exclusions != null)
					return false;
			} else if (!exclusions.equals(other.exclusions))
				return false;
			return true;
		}

		public static <T> Delta<T> empty() {
			return new Delta<T>((T[]) new Object[0], Sets.<T>newHashSet());
		}
	}
	
	public AccessPath<T> mergeExcludedFieldReferences(AccessPath<T> accPath) {
		HashSet<T> newExclusions = Sets.newHashSet(exclusions);
		newExclusions.addAll(accPath.exclusions);
		return new AccessPath<T>(accesses, newExclusions);
	}
	
	public boolean canRead(T field) {
		return accesses.length > 0 && accesses[0].equals(field);
	}
	
	public boolean isEmpty() {
		return exclusions.isEmpty() && accesses.length == 0;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(accesses);
		result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
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
		AccessPath other = (AccessPath) obj;
		if (!Arrays.equals(accesses, other.accesses))
			return false;
		if (exclusions == null) {
			if (other.exclusions != null)
				return false;
		} else if (!exclusions.equals(other.exclusions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String result = accesses.length > 0 ? "."+Joiner.on(".").join(accesses) : "";
		if(!exclusions.isEmpty())
			result += "^" + Joiner.on(",").join(exclusions);
		return result;
	}
	
	public AccessPath<T> removeAnyAccess() {
		if(accesses.length > 0)
			return new AccessPath<T>((T[]) new Object[0], exclusions);
		else
			return this;
	}

	public boolean hasEmptyAccessPath() {
		return accesses.length == 0;
	}

	public T getFirstAccess() {
		return accesses[0];
	}
}
