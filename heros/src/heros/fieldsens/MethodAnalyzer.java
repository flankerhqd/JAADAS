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

import heros.fieldsens.structs.WrappedFactAtStatement;

public interface MethodAnalyzer<Field,Fact,Stmt,Method>  {

	public void addIncomingEdge(CallEdge<Field, Fact, Stmt, Method> incEdge);
	
	public void addInitialSeed(Stmt startPoint, Fact val);
	
	public void addUnbalancedReturnFlow(WrappedFactAtStatement<Field, Fact, Stmt, Method> target, Stmt callSite);
}
