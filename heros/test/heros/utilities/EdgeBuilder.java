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
package heros.utilities;


import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;


public abstract class EdgeBuilder {
	
	protected List<Edge> edges = Lists.newLinkedList();
	public Collection<Edge> edges() {
		if(edges.isEmpty()) {
			throw new IllegalStateException("Not a single edge created on EdgeBuilder: "+toString());
		}
		
		return edges;
	}

	public static class CallSiteBuilder extends EdgeBuilder {

		private Statement callSite;

		public CallSiteBuilder(Statement callSite) {
			this.callSite = callSite;
		}

		public CallSiteBuilder calls(String method, ExpectedFlowFunction...flows) {
			edges.add(new Edge.CallEdge(callSite, new TestMethod(method), flows));
			return this;
		}
		
		public CallSiteBuilder retSite(String returnSite, ExpectedFlowFunction...flows) {
			edges.add(new Edge.Call2ReturnEdge(callSite, new Statement(returnSite), flows));
			return this;
		}
	}
	
	public static class NormalStmtBuilder extends EdgeBuilder {

		private Statement stmt;
		private ExpectedFlowFunction[] flowFunctions;

		public NormalStmtBuilder(Statement stmt, ExpectedFlowFunction[] flowFunctions) {
			this.stmt = stmt;
			this.flowFunctions = flowFunctions;
		}

		public NormalStmtBuilder succ(String succ) {
			edges.add(new Edge.NormalEdge(stmt, new Statement(succ), flowFunctions));
			return this;
		}
	}
	
	public static class ExitStmtBuilder extends EdgeBuilder {

		private Statement exitStmt;

		public ExitStmtBuilder(Statement exitStmt) {
			this.exitStmt = exitStmt;
		}
		
		public ExitStmtBuilder expectArtificalFlow(ExpectedFlowFunction...flows) {
			edges.add(new Edge.ReturnEdge(null, exitStmt, null, flows));
			return this;
		}

		public ExitStmtBuilder returns(Statement callSite, Statement returnSite, ExpectedFlowFunction... flows) {
			edges.add(new Edge.ReturnEdge(callSite, exitStmt, returnSite, flows));
			return this;
		}
		
	}
}
