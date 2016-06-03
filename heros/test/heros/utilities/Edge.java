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
package heros.utilities;


public abstract class Edge {
	public final ExpectedFlowFunction[] flowFunctions;
	public boolean includeInCfg = true;

	public Edge(ExpectedFlowFunction...flowFunctions) {
		this.flowFunctions = flowFunctions;
		for(ExpectedFlowFunction ff : flowFunctions) {
			ff.edge = this;
		}
	}
	
	public abstract void accept(EdgeVisitor visitor);
	
	
	public static class NormalEdge extends Edge {

		public final Statement unit;
		public final Statement succUnit;

		public NormalEdge(Statement unit, Statement succUnit, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.unit = unit;
			this.succUnit = succUnit;
		}

		@Override
		public String toString() {
			return String.format("%s -normal-> %s", unit, succUnit);
		}

		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class CallEdge extends Edge {

		public final Statement callSite;
		public final TestMethod destinationMethod;

		public CallEdge(Statement callSite, TestMethod destinationMethod, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.destinationMethod = destinationMethod;
		}

		@Override
		public String toString() {
			return String.format("%s -call-> %s", callSite, destinationMethod);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class Call2ReturnEdge extends Edge {
		public final Statement callSite;
		public final Statement returnSite;

		public Call2ReturnEdge(Statement callSite, Statement returnSite, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.returnSite = returnSite;
		}

		@Override
		public String toString() {
			return String.format("%s -call2ret-> %s", callSite, returnSite);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}

	public static class ReturnEdge extends Edge {

		public final Statement exitStmt;
		public final Statement returnSite;
		public final Statement callSite;
		public TestMethod calleeMethod;

		public ReturnEdge(Statement callSite, Statement exitStmt, Statement returnSite, ExpectedFlowFunction...flowFunctions) {
			super(flowFunctions);
			this.callSite = callSite;
			this.exitStmt = exitStmt;
			this.returnSite = returnSite;
			if(callSite == null || returnSite == null)
				includeInCfg = false;
		}

		@Override
		public String toString() {
			return String.format("%s -return-> %s", exitStmt, returnSite);
		}
		
		@Override
		public void accept(EdgeVisitor visitor) {
			visitor.visit(this);
		}
	}
	
	
	public static interface EdgeVisitor {
		void visit(NormalEdge edge);
		void visit(CallEdge edge);
		void visit(Call2ReturnEdge edge);
		void visit(ReturnEdge edge);
	}
}