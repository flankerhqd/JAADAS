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
package heros;

import heros.utilities.TestHelper;

import org.junit.Before;
import org.junit.Test;

import static heros.utilities.TestHelper.*;

public class IFDSSolverTest {

	private TestHelper helper;

	@Before
	public void before() {
		helper = new TestHelper();
	}
	
	@Test
	public void happyPath() {
		helper.method("bar", 
				startPoints("a"),
				normalStmt("a", flow("0", "x")).succ("b"),
				normalStmt("b", flow("x", "x")).succ("c"),
				callSite("c").calls("foo", flow("x", "y")).retSite("f", flow("x", "x")));
		
		helper.method("foo",
				startPoints("d"),
				normalStmt("d", flow("y", "y", "z")).succ("e"),
				exitStmt("e").returns(over("c"), to("f"), flow("z", "u"), flow("y")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummary() {
		helper.method("foo", 
				startPoints("a"),
				callSite("a").calls("bar", flow("0", "x")).retSite("b", flow("0", "y")),
				callSite("b").calls("bar", flow("y", "x")).retSite("c", flow("y")),
				normalStmt("c", flow("w", "0")).succ("c0"));
		
		helper.method("bar",
				startPoints("d"),
				normalStmt("d", flow("x", "z")).succ("e"),
				exitStmt("e").returns(over("a"), to("b"), flow("z", "y"))
							  .returns(over("b"), to("c"), flow("z", "w")));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void reuseSummaryForRecursiveCall() {
		helper.method("foo",
				startPoints("a"),
				callSite("a").calls("bar", flow("0", "1")).retSite("b", flow("0")),
				normalStmt("b", flow("2", "3")).succ("c"));
		
		helper.method("bar",
				startPoints("g"),
				normalStmt("g", flow("1", "1")).succ("i").succ("h"),
				callSite("i").calls("bar", flow("1", "1")).retSite("h", flow("1")),
				exitStmt("h").returns(over("a"), to("b"), flow("1"), flow("2" ,"2"))
							.returns(over("i"), to("h"), flow("1","2"), flow("2", "2")));
				
		helper.runSolver(false, "a");
	}
	
	@Test
	public void branch() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "x")).succ("b2").succ("b1"),
				normalStmt("b1", flow("x", "x", "y")).succ("c"),
				normalStmt("b2", flow("x", "x")).succ("c"),
				normalStmt("c", flow("x", "z"), flow("y", "w")).succ("d"),
				normalStmt("d", flow("z"), flow("w")).succ("e"));
		
		helper.runSolver(false, "a");
	}
	
	@Test
	public void unbalancedReturn() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("x"),  to("y"), flow("1", "1")));
		
		helper.method("bar", 
				startPoints("unused"),
				normalStmt("y", flow("1", "2")).succ("z"));
		
		helper.runSolver(true, "a");
	}
	
	@Test
	public void artificalReturnEdgeForNoCallersCase() {
		helper.method("foo",
				startPoints("a"),
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(null, null, flow("1", "1")));
		
		helper.runSolver(true, "a");
	}
}
