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
package heros.fieldsens;

import heros.InterproceduralCFG;
import heros.utilities.FieldSensitiveTestHelper;
import heros.utilities.Statement;
import heros.utilities.TestDebugger;
import heros.utilities.TestFact;
import heros.utilities.TestMethod;
import heros.utilities.FieldSensitiveTestHelper.TabulationProblemExchange;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

import static heros.utilities.FieldSensitiveTestHelper.*;


@RunWith(Parameterized.class)
public class BiDiFieldSensitiveIFDSSolverTest {

	private FieldSensitiveTestHelper forwardHelper;
	private FieldSensitiveTestHelper backwardHelper;
	private TabulationProblemExchange exchange;
	private TestDebugger<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>> debugger;
	
	public BiDiFieldSensitiveIFDSSolverTest(TabulationProblemExchange exchange) {
		this.exchange = exchange;
		debugger = new TestDebugger<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>>();
		forwardHelper = new FieldSensitiveTestHelper(debugger);
		backwardHelper = new FieldSensitiveTestHelper(debugger);
	}

	@Parameters(name="{0}")
	public static Collection<Object[]> parameters() {
		LinkedList<Object[]> result = Lists.newLinkedList();
		result.add(new Object[] {TabulationProblemExchange.AsSpecified});
		result.add(new Object[] {TabulationProblemExchange.ExchangeForwardAndBackward});
		return result;
	}
	
	@Test
	public void happyPath() {
		forwardHelper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b"),
				normalStmt("b", flow("0", "1")).succ("c"),
				exitStmt("c").expectArtificalFlow(flow("1")));
		
		backwardHelper.method("foo",
				startPoints("c"),
				normalStmt("c").succ("b"),
				normalStmt("b", flow("0", "2")).succ("a"),
				exitStmt("a").expectArtificalFlow(flow("2")));
		
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "b");
	}
	
	@Test
	public void unbalancedReturnsInBothDirections() {
		forwardHelper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b"),
				normalStmt("b", flow("0", "1")).succ("c"),
				exitStmt("c").returns(over("y"), to("z"), flow("1", "2")));
		
		forwardHelper.method("bar",
				startPoints("y"),
				normalStmt("y").succ("z"),
				exitStmt("z").expectArtificalFlow(kill("2")));
		
		backwardHelper.method("foo",
				startPoints("c"),
				normalStmt("c").succ("b"),
				normalStmt("b", flow("0", "2")).succ("a"),
				exitStmt("a").returns(over("y"), to("x"), flow("2", "3")));
		
		backwardHelper.method("bar",
				startPoints("y"),
				normalStmt("y").succ("x"),
				exitStmt("x").expectArtificalFlow(kill("3")));
		
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "b");
	}
	
	@Test
	public void unbalancedReturnsNonMatchingCallSites() {
		forwardHelper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b"),
				normalStmt("b", flow("0", "1")).succ("c"),
				exitStmt("c").returns(over("y1"), to("z"), flow("1", "2")));
		
		forwardHelper.method("bar",
				startPoints(),
				normalStmt("y1").succ("z"),
				exitStmt("z").expectArtificalFlow(/*none*/));
		
		backwardHelper.method("foo",
				startPoints("c"),
				normalStmt("c").succ("b"),
				normalStmt("b", flow("0", "2")).succ("a"),
				exitStmt("a").returns(over("y2"), to("x"), flow("2", "3")));
		
		backwardHelper.method("bar",
				startPoints(),
				normalStmt("y2").succ("x"),
				exitStmt("x").expectArtificalFlow(/*none*/));
		
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "b");
	}
	
	@Test
	public void returnsOnlyOneDirectionAndStops() {
		forwardHelper.method("foo",
				startPoints("a"),
				normalStmt("a").succ("b"),
				normalStmt("b", flow("0", "1")).succ("c"),
				exitStmt("c").returns(over("y"), to("z"), flow("1", "2")));
		
		forwardHelper.method("bar",
				startPoints(),
				normalStmt("y").succ("z"),
				exitStmt("z").expectArtificalFlow(/*none*/));
		
		backwardHelper.method("foo",
				startPoints("c"),
				normalStmt("c").succ("b"),
				normalStmt("b", kill("0")).succ("a"),
				exitStmt("a").returns(over("y"), to("x") /*none*/));
		
		backwardHelper.method("bar",
				startPoints(),
				normalStmt("y").succ("x"),
				exitStmt("x").expectArtificalFlow(/*none*/));
		
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "b");
	}
	
	@Test
	public void reuseSummary() {
		forwardHelper.method("foo",
				startPoints(),
				normalStmt("a", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow("1", "2")).retSite("c", kill("1")),
				callSite("c").calls("bar", flow("1", "2")).retSite("d", kill("1")),
				exitStmt("d").expectArtificalFlow(kill("1")));
		
		forwardHelper.method("bar",
				startPoints("x"),
				normalStmt("x", flow("2", "2")).succ("y"),
				exitStmt("y").returns(over("b"), to("c"), flow("2", "1"))
							.returns(over("c"), to("d"), flow("2", "1")));
		
		backwardHelper.method("foo",
				startPoints(),
				exitStmt("a").expectArtificalFlow(kill("0")));
					
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "a");
	}
	
	@Test
	public void multipleSeedsPreventReusingSummary() {
		forwardHelper.method("foo",
				startPoints(),
				normalStmt("a1", flow("0", "1")).succ("b"),
				normalStmt("a2", flow("0", "1")).succ("b"),
				callSite("b").calls("bar", flow(times(2), "1", "2")).retSite("c", kill(times(2), "1")),
				callSite("c").calls("bar", flow(times(2), "1", "2")).retSite("d", kill(times(2), "1")),
				exitStmt("d").expectArtificalFlow(kill(times(2), "1")));
		
		forwardHelper.method("bar",
				startPoints("x"),
				normalStmt("x", flow("2", "2")).succ("y"),
				exitStmt("y").returns(over("b"), to("c"), flow(times(2), "2", "1"))
							 .returns(over("c"), to("d"), flow(times(2), "2", "1")));
		
		backwardHelper.method("foo",
				startPoints(),
				exitStmt("a1").expectArtificalFlow(kill("0")),
				exitStmt("a2").expectArtificalFlow(kill("0")));
					
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "a1", "a2");
	}

	@Test
	public void dontResumeIfReturnFlowIsKilled() {
		forwardHelper.method("foo",
				startPoints(), 
				normalStmt("a", flow("0", "1")).succ("b"),
				exitStmt("b").returns(over("cs"), to("y"), kill("1")));
		
		forwardHelper.method("bar",
				startPoints(),
				normalStmt("y").succ("z" /* none */));
		
		backwardHelper.method("foo",
				startPoints(),
				normalStmt("a", flow("0", "1")).succ("c"),
				exitStmt("c").returns(over("cs"), to("x"), flow("1", "2")));
		
		backwardHelper.method("bar",
				startPoints(),
				normalStmt("cs").succ("x"),
				normalStmt("x").succ("z" /*none*/));
		
		forwardHelper.runBiDiSolver(backwardHelper, exchange, "a");
	}
}
