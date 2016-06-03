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

import heros.InterproceduralCFG;

public interface Debugger<Field, Fact, Stmt, Method, I extends InterproceduralCFG<Stmt, Method>> {

	public abstract void setICFG(I icfg);

	public abstract void initialSeed(Stmt stmt);

	
	public static class NullDebugger <Field, Fact, Stmt, Method, I extends InterproceduralCFG<Stmt, Method>> implements Debugger<Field, Fact, Stmt, Method, I> {

		@Override
		public void setICFG(I icfg) {
			
		}

		@Override
		public void initialSeed(Stmt stmt) {
			
		}
		
	}
}