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
package soot.jimple.infoflow.test.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow.AliasingAlgorithm;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.taintWrappers.AbstractTaintWrapper;

/**
 * tests aliasing of heap references
 */
public class HeapTestsPtsAliasing extends JUnitTests {

	@Test(timeout = 300000)
	public void testForEarlyTermination() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForEarlyTermination()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForLoop() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForLoop()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForWrapper() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForWrapper()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void simpleTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// PTS are context-insensitive in Soot
		checkInfoflow(infoflow, 1);
		// negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void argumentTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void argumentTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void doubleCallTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		// In Soot, points-to-sets don't seem to be object sensitive by default
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void heapTest0() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest0()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void heapTest1() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testExample1() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ForwardBackwardTest: void testMethod()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testReturn() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodReturn()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testTwoLevels() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void twoLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void multiAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void arrayAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void threeLevelTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void threeLevelShortAPTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);

		int oldAPLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(1);

		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);

		Infoflow.setAccessPathLength(oldAPLength); // this is a global setting!
													// Restore it when we're
													// done
	}

	@Test(timeout = 300000)
	public void recursionTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void recursionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest1() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest3() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4b() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest5() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void returnAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callPerformanceTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void callPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasesTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);

		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);

		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void wrapperAliasesTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);

		infoflow.setTaintWrapper(new AbstractTaintWrapper() {
			
			@Override
			public boolean isExclusiveInternal(Stmt stmt,
					AccessPath taintedPath) {
				return stmt.containsInvokeExpr()
						&& (stmt.getInvokeExpr().getMethod().getName()
								.equals("foo2") || stmt.getInvokeExpr()
								.getMethod().getName().equals("bar2"));
			}

			@Override
			public Set<AccessPath> getTaintsForMethodInternal(Stmt stmt,
					AccessPath taintedPath) {
				if (!stmt.containsInvokeExpr())
					return Collections.singleton(taintedPath);

				Set<AccessPath> res = new HashSet<AccessPath>();
				res.add(taintedPath);

				// We use a path length of 1, i.e. do not work with member
				// fields,
				// hence the commented-out code
				if (stmt.getInvokeExpr().getMethod().getName().equals("foo2")) {
					InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt
							.getInvokeExpr();
					if (taintedPath.getPlainValue() == iinv.getArg(0)) {
						RefType rt = (RefType) iinv.getBase().getType();
						AccessPath ap = new AccessPath(iinv.getBase(),
								new SootField[] { rt.getSootClass()
										.getFieldByName("b1") /*
															 * , Scene.v().
															 * getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$B"
															 * ).getFieldByName(
															 * "attr"),
															 * Scene.v()
															 * .getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$A"
															 * )
															 * .getFieldByName("b"
															 * )
															 */}, true);
						res.add(ap);
					}
					if (taintedPath.getPlainValue() == iinv.getArg(1)) {
						RefType rt = (RefType) iinv.getBase().getType();
						AccessPath ap = new AccessPath(iinv.getBase(),
								new SootField[] { rt.getSootClass()
										.getFieldByName("b2") /*
															 * , Scene.v().
															 * getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$B"
															 * ).getFieldByName(
															 * "attr"),
															 * Scene.v()
															 * .getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$A"
															 * )
															 * .getFieldByName("b"
															 * )
															 */}, true);
						res.add(ap);
					}
				} else if (stmt.getInvokeExpr().getMethod().getName()
						.equals("bar2")) {
					InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt
							.getInvokeExpr();
					if (taintedPath.getPlainValue() == iinv.getArg(0)) {
						RefType rt = (RefType) iinv.getBase().getType();
						AccessPath ap = new AccessPath(iinv.getBase(),
								new SootField[] { rt.getSootClass()
										.getFieldByName("b1") /*
															 * , Scene.v().
															 * getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$B"
															 * ).getFieldByName(
															 * "attr"),
															 * Scene.v()
															 * .getSootClass(
															 * "soot.jimple.infoflow.test.HeapTestCode$A"
															 * )
															 * .getFieldByName("b"
															 * )
															 */}, true);
						res.add(ap);
					} else if (taintedPath.getPlainValue() == iinv.getBase()
					/*
					 * && taintedPath.getFirstField().getName().equals("b2") //
					 * .attr && taintedPath.getLastField().getName().equals("b")
					 */) {
						DefinitionStmt def = (DefinitionStmt) stmt;
						AccessPath ap = new AccessPath(
								def.getLeftOp(),
								Scene.v()
										.getSootClass(
												"soot.jimple.infoflow.test.HeapTestCode$A")
										.getFieldByName("b"), true);
						res.add(ap);
					}
				}

				return res;
			}

			@Override
			public boolean supportsCallee(SootMethod method) {
				return false;
			}

			@Override
			public boolean supportsCallee(Stmt callSite) {
				return false;
			}

			@Override
			public Set<Abstraction> getAliasesForMethod(Stmt stmt,
					Abstraction d1, Abstraction taintedPath) {
				return null;
			}
			
		});

		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testWrapperAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);

		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void negativeAliasesTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);

		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTestAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void aliasPerformanceTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);

		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 3); // +1 for flow insensitivty

		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void backwardsParameterTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);

		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void backwardsParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
