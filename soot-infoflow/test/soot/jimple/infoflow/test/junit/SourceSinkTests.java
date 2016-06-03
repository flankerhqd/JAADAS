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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.source.SourceInfo;

/**
 * Test cases for FlowDroid's interaction with non-standard source sink managers
 * 
 * @author Steven Arzt
 */
public class SourceSinkTests extends JUnitTests {

	private static final String sourceGetSecret = "<soot.jimple.infoflow.test.SourceSinkTestCode: soot.jimple.infoflow.test.SourceSinkTestCode$A getSecret()>";
	private static final String sourceGetSecret2 = "<soot.jimple.infoflow.test.SourceSinkTestCode: soot.jimple.infoflow.test.SourceSinkTestCode$B getSecret2()>";
	
	private static final String sinkAP = "<soot.jimple.infoflow.test.SourceSinkTestCode: void doleakSecret2(soot.jimple.infoflow.test.SourceSinkTestCode$A)>";

	private abstract class BaseSourceSinkManager implements ISourceSinkManager {

		@Override
		public boolean isSink(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg,
				AccessPath ap) {
			if (!sCallSite.containsInvokeExpr())
				return false;
			SootMethod target = sCallSite.getInvokeExpr().getMethod();
			if (target.getSignature().equals(sink))
				return true;
			
			if (target.getSignature().equals(sinkAP)
					&& sCallSite.getInvokeExpr().getArgCount() > 0
					&& ap.getPlainValue() == sCallSite.getInvokeExpr().getArg(0))
				return true;
			return false;
		}

	}
	
	private final ISourceSinkManager getSecretSSM = new BaseSourceSinkManager() {

		@Override
		public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
			if (sCallSite.containsInvokeExpr()
					&& sCallSite instanceof DefinitionStmt
					&& sCallSite.getInvokeExpr().getMethod().getName().equals("getSecret")) {
				AccessPath ap = new AccessPath(((DefinitionStmt) sCallSite).getLeftOp(), true);
				return new SourceInfo(ap);
			}
			return null;
		}
		
	};
	
	private final ISourceSinkManager getSecretOrSecret2SSM = new BaseSourceSinkManager() {

		@Override
		public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
			if (sCallSite.containsInvokeExpr()
					&& sCallSite instanceof DefinitionStmt
					&& (sCallSite.getInvokeExpr().getMethod().getName().equals("getSecret")
							|| (sCallSite.getInvokeExpr().getMethod().getName().equals("getSecret2")))) {
				AccessPath ap = new AccessPath(((DefinitionStmt) sCallSite).getLeftOp(), true);
				return new SourceInfo(ap);
			}
			return null;
		}
		
	};
	
	private final ISourceSinkManager noTaintSubFieldsSSM = new BaseSourceSinkManager() {

		@Override
		public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
			if (sCallSite.containsInvokeExpr()
					&& sCallSite instanceof DefinitionStmt
					&& sCallSite.getInvokeExpr().getMethod().getName().equals("getSecret")) {
				AccessPath ap = new AccessPath(((DefinitionStmt) sCallSite).getLeftOp(), false);
				return new SourceInfo(ap);
			}
			return null;
		}
		
	};
	
	@Test(timeout = 300000)
	public void fieldTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testDataObject()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), getSecretSSM);
		Assert.assertTrue(infoflow.isResultAvailable());
		Assert.assertEquals(1, infoflow.getResults().size());
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceGetSecret));
	}

	@Test(timeout = 300000)
	public void negativeFieldTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testDataObject()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), noTaintSubFieldsSSM);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void accessPathTypesTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testAccessPathTypes()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), getSecretOrSecret2SSM);
		Assert.assertTrue(infoflow.isResultAvailable());
		Assert.assertEquals(1, infoflow.getResults().size());
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceGetSecret));
		Assert.assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceGetSecret2));
	}

	@Test(timeout = 300000)
	public void sourceAccessPathTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testSourceAccessPaths()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), noTaintSubFieldsSSM);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void sinkAccessPathTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testSinkAccessPaths()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), noTaintSubFieldsSSM);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout = 300000)
	public void sinkAccessPathTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.SourceSinkTestCode: void testSinkAccessPaths2()>");
		infoflow.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(epoints), noTaintSubFieldsSSM);
		negativeCheckInfoflow(infoflow);
	}

}
