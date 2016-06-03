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
package heros.fieldsens.structs;

import heros.fieldsens.AccessPath;
import heros.fieldsens.FlowFunction;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.FlowFunction.Constraint;

public class DeltaConstraint<FieldRef> implements Constraint<FieldRef> {

	private Delta<FieldRef> delta;

	public DeltaConstraint(AccessPath<FieldRef> accPathAtCaller, AccessPath<FieldRef> accPathAtCallee) {
		delta = accPathAtCaller.getDeltaTo(accPathAtCallee);
	}

	public DeltaConstraint(Delta<FieldRef> delta) {
		this.delta = delta;
	}
	
	@Override
	public AccessPath<FieldRef> applyToAccessPath(AccessPath<FieldRef> accPath) {
		return delta.applyTo(accPath);
	}

	@Override
	public boolean canBeAppliedTo(AccessPath<FieldRef> accPath) {
		return delta.canBeAppliedTo(accPath);
	}

	@Override
	public String toString() {
		return delta.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delta == null) ? 0 : delta.hashCode());
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
		DeltaConstraint other = (DeltaConstraint) obj;
		if (delta == null) {
			if (other.delta != null)
				return false;
		} else if (!delta.equals(other.delta))
			return false;
		return true;
	}
	
	
}
