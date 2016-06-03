/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.resources;

import soot.SootClass;

/**
 * Data class representing a layout control on the android screen
 * 
 * @author Steven Arzt
 *
 */
public class LayoutControl {
	
	private final int id;
	private final SootClass viewClass;
	private boolean isSensitive;
	
	public LayoutControl(int id, SootClass viewClass) {
		this.id = id;
		this.viewClass = viewClass;
	}
	
	public LayoutControl(int id, SootClass viewClass, boolean isSensitive) {
		this(id, viewClass);
		this.isSensitive = isSensitive;
	}
	
	public int getID() {
		return this.id;
	}
	
	public SootClass getViewClass() {
		return this.viewClass;
	}
	
	public void setIsSensitive(boolean isSensitive) {
		this.isSensitive = isSensitive;
	}
	
	public boolean isSensitive() {
		return this.isSensitive;
	}
	
	@Override
	public String toString() {
		return id + " - " + viewClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + (isSensitive ? 1231 : 1237);
		result = prime * result + ((viewClass == null) ? 0 : viewClass.hashCode());
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
		LayoutControl other = (LayoutControl) obj;
		if (id != other.id)
			return false;
		if (isSensitive != other.isSensitive)
			return false;
		if (viewClass == null) {
			if (other.viewClass != null)
				return false;
		} else if (!viewClass.equals(other.viewClass))
			return false;
		return true;
	}
	
}
