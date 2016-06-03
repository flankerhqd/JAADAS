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

import heros.InterproceduralCFG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import soot.RefType;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow.AliasingAlgorithm;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.infoflow.taintWrappers.AbstractTaintWrapper;

/**
 * tests aliasing of heap references
 */
public class HeapTests extends JUnitTests {

	@Test(timeout = 300000)
	public void testForEarlyTermination() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForEarlyTermination()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForLoop() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForLoop()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testForWrapper() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void testForWrapper()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void simpleTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void argumentTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void argumentTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void doubleCallTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void heapTest0() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest0()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test(timeout = 300000)
	public void heapTest0b() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest0b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test(timeout = 300000)
	public void heapTest1() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testExample1() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ForwardBackwardTest: void testMethod()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testReturn() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void methodReturn()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void testTwoLevels() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void twoLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void multiAliasTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test(timeout = 300000)
	public void multiAliasNoRecusiveAPTest() {
		Infoflow infoflow = initInfoflow();
		boolean oldRecAPI = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldRecAPI);
		checkInfoflow(infoflow, 1);
	}
	
	@Test(timeout = 300000)
	public void overwriteAliasTest() {
		boolean oldUseRecAP = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);
		
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldUseRecAP);

		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void arrayAliasTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void arrayAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void functionAliasTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void functionAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiLevelTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void negativeMultiLevelTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeMultiLevelTaint2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void threeLevelTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void threeLevelShortAPTest() {
		Infoflow infoflow = initInfoflow();
		
		int oldAPLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(1);
		
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void threeLevelTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		
		Infoflow.setAccessPathLength(oldAPLength); // this is a global setting!
													// Restore it when we're
													// done
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void recursionTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void recursionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest1() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest3() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest4b() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest4b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void activationUnitTest5() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void activationUnitTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void returnAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void returnAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void callPerformanceTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void callPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasesTest() {
		Infoflow infoflow = initInfoflow();
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(5);
		
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
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);

		infoflow.setTaintWrapper(new AbstractTaintWrapper() {
						
			@Override
			public boolean isExclusiveInternal(Stmt stmt, AccessPath taintedPath) {
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
										.getFieldByName("b1") }, true);
						res.add(ap);
					}
					if (taintedPath.getPlainValue() == iinv.getArg(1)) {
						RefType rt = (RefType) iinv.getBase().getType();
						AccessPath ap = new AccessPath(iinv.getBase(),
								new SootField[] { rt.getSootClass()
										.getFieldByName("b2") }, true);
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
										.getFieldByName("b1") }, true);
						res.add(ap);
					} else if (taintedPath.getPlainValue() == iinv.getBase()) {
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
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(4);
		
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void negativeTestAliases()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
		
		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void aliasPerformanceTest() {
		Infoflow infoflow = initInfoflow();
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);

		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
		
		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void aliasPerformanceTestFIS() {
		Infoflow infoflow = initInfoflow();
		int oldLength = Infoflow.getAccessPathLength();
		Infoflow.setAccessPathLength(3);
		
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		infoflow.setAliasingAlgorithm(AliasingAlgorithm.PtsBased);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasPerformanceTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 3);	// PTS-based alias analysis is not flow-sensitive
		
		Infoflow.setAccessPathLength(oldLength);
	}

	@Test(timeout = 300000)
	public void backwardsParameterTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void backwardsParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void aliasTaintLeakTaintTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasTaintLeakTaintTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void fieldBaseOverwriteTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void fieldBaseOverwriteTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test(timeout = 300000)
	public void doubleAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void doubleAliasTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void doubleAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void tripleAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void tripleAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 3);
	}

	@Test(timeout = 300000)
	public void intAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void intAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticAliasTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAliasTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void staticAliasTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAliasTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void unAliasParameterTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void unAliasParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test(timeout = 300000)
	public void overwriteParameterTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteParameterTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void multiAliasBaseTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void multiAliasBaseTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void innerClassTest() {
		boolean oldUseRecAP = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldUseRecAP);
		
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest2() {
		boolean oldUseRecAP = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldUseRecAP);
		
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void innerClassTest3() {
		boolean oldUseRecAP = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);

		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldUseRecAP);
		
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void innerClassTest4() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void innerClassTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void datastructureTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void datastructureTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void datastructureTest2() {
		boolean oldUseRecAP = Infoflow.getUseRecursiveAccessPaths();
		Infoflow.setUseRecursiveAccessPaths(false);
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void datastructureTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		Infoflow.setUseRecursiveAccessPaths(oldUseRecAP);
		
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void staticAccessPathTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void staticAccessPathTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void separatedTreeTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void separatedTreeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest2() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest3() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest4() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest5() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 2);
	}

	@Test(timeout = 300000)
	public void overwriteAliasedVariableTest6() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void overwriteAliasedVariableTest6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Ignore("fails, looks like a conceptual problem")
	@Test(timeout = 300000)
	public void aliasFlowTest() {
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasFlowTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}
	
	@Test(timeout = 300000)
	public void aliasStrongUpdateTest() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";
		final String sourceMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "soot.jimple.infoflow.test.HeapTestCode$Data getSecretData()>";
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints,
				Collections.singleton(sourceMethod),
				Collections.singleton(sinkMethod));
		
   	 	Assert.assertTrue(infoflow.isResultAvailable());
   	 	InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
		Assert.assertTrue(map.isPathBetweenMethods(sinkMethod, sourceMethod));
	}
	
	@Test(timeout = 300000)
	public void aliasStrongUpdateTest2() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";
		final String sourceMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "soot.jimple.infoflow.test.HeapTestCode$Data getSecretData()>";
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints,
				Collections.singleton(sourceMethod),
				Collections.singleton(sinkMethod));
		
   	 	Assert.assertTrue(infoflow.isResultAvailable());
   	 	InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
		Assert.assertTrue(map.isPathBetweenMethods(sinkMethod, sourceMethod));
	}
	
	@Test(timeout = 300000)
	public void aliasStrongUpdateTest3() {
		final String sinkMethod = "<soot.jimple.infoflow.test.HeapTestCode: "
				+ "void leakData(soot.jimple.infoflow.test.HeapTestCode$Data)>";
		
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSources(false);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(false);
		
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void aliasStrongUpdateTest3()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints),
				new ISourceSinkManager() {
			
			@Override
			public boolean isSink(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg,
					AccessPath ap) {
				return sCallSite.containsInvokeExpr()
						&& sCallSite.getInvokeExpr().getMethod().getSignature().equals(sinkMethod);
			}
			
			@Override
			public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
				if (sCallSite instanceof AssignStmt) {
					AssignStmt assignStmt = (AssignStmt) sCallSite;
					if (assignStmt.getRightOp().toString().contains("taintedBySourceSinkManager"))
						return new SourceInfo(new AccessPath(assignStmt.getLeftOp(), true));
					else
						return null;
				}
				return null;
			}
			
		});
		
   	 	Assert.assertTrue(infoflow.isResultAvailable());
   	 	InfoflowResults map = infoflow.getResults();
		Assert.assertEquals(1, map.size());
		Assert.assertTrue(map.containsSinkMethod(sinkMethod));
	}
	
}
