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
package soot.jimple.infoflow.android.data;

import java.util.Comparator;

public class AndroidMethodCategoryComparator implements Comparator<AndroidMethod>{

	@Override
	public int compare(AndroidMethod m1, AndroidMethod m2) {
		if(m1.getCategory() == null && m2.getCategory() == null)
			return 0;
		else if(m1.getCategory() == null)
			return 1;
		else if(m2.getCategory() == null)
			return -1;
		else
			return m1.getCategory().compareTo(m2.getCategory());
	}

}
