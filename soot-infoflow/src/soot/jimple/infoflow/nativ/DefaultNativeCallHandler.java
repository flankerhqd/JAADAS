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
package soot.jimple.infoflow.nativ;

import java.util.Collections;
import java.util.Set;

import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

public class DefaultNativeCallHandler extends NativeCallHandler {
	
	@Override
	public Set<Abstraction> getTaintedValues(Stmt call, Abstraction source, Value[] params){
		//check some evaluated methods:
		
		//arraycopy:
		//arraycopy(Object src, int srcPos, Object dest, int destPos, int length)
        //Copies an array from the specified source array, beginning at the specified position,
		//to the specified position of the destination array.
		if(call.getInvokeExpr().getMethod().toString().contains("arraycopy"))
			if(params[0].equals(source.getAccessPath().getPlainValue())) {
				Abstraction abs = source.deriveNewAbstraction(params[2], false, call,
						source.getAccessPath().getBaseType());
				abs.setCorrespondingCallSite(call);
				return Collections.singleton(abs);
			}
		
		return Collections.emptySet();
	}
	
}
