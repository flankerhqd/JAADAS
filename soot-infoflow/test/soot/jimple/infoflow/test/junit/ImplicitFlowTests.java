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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

/**
 * Test class for implicit flows
 * @author Steven Arzt 
 */
public class ImplicitFlowTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void simpleTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}
	
	@Test(timeout=300000)
	public void simpleNegativeTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);	
	}

	@Test(timeout=300000)
	public void simpleOverwriteTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void simpleOverwriteTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void switchTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void switchTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void convertTest(){
		long timeBefore = System.nanoTime();
    	System.out.println("Starting convertTest...");
    	Infoflow infoflow = initInfoflow();

    	int oldAPLength = Infoflow.getAccessPathLength();
    	Infoflow.setAccessPathLength(1);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
		infoflow.setEnableStaticFieldTracking(false);
		
	    List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void convertTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	

		Infoflow.setAccessPathLength(oldAPLength);	// this is a global setting! Restore it when we're done
		System.out.println("convertTest took " + (System.nanoTime() - timeBefore) / 1E9 + " seconds");
	}

	@Test(timeout=300000)
	public void sinkTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void sinkTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void returnTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void returnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void callTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void callTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void negativeCallTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void negativeCallTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);	
	}

	@Test(timeout=300000)
	public void recursionTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void recursionTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void recursionTest3(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void recursionTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void exceptionTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void exceptionTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void exceptionTest3(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void exceptionTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void fieldTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void fieldTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void staticFieldTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void staticFieldTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void staticFieldTest3(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void staticFieldTest4(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void staticFieldTest5(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
	    
		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void staticFieldTest4()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void integerClassTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void integerClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void stringClassTest(){
		long timeBefore = System.nanoTime();
    	System.out.println("Starting stringClassTest...");
    	Infoflow infoflow = initInfoflow();
    	
    	int oldAPLength = Infoflow.getAccessPathLength();
    	Infoflow.setAccessPathLength(1);
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
		infoflow.setEnableStaticFieldTracking(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void stringClassTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	

		Infoflow.setAccessPathLength(oldAPLength);	// this is a global setting! Restore it when we're done
		System.out.println("stringClassTest took " + (System.nanoTime() - timeBefore) / 1E9 + " seconds");
	}

	@Test(timeout=300000)
	@Ignore
	public void conditionalExceptionTest(){
		// not yet supported
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalExceptionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void passOverTest(){
		// not yet supported
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void passOverTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void passOverTest2(){
		// not yet supported
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void passOverTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void callToReturnTest() throws IOException{
		// not yet supported
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
		infoflow.setTaintWrapper(new EasyTaintWrapper("EasyTaintWrapperSource.txt"));
    	infoflow.setSootConfig(new IInfoflowConfig() {
			
			@Override
			public void setSootOptions(Options options) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
			}
			
		});

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void callToReturnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void createAliasInFunctionTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void createAliasInFunctionTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void createAliasInFunctionTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void createAliasInFunctionTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void implicitFlowTaintWrapperTest() throws IOException{
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
		infoflow.setTaintWrapper(new EasyTaintWrapper("EasyTaintWrapperSource.txt"));
    	infoflow.setSootConfig(new IInfoflowConfig() {
			
			@Override
			public void setSootOptions(Options options) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
			}
			
		});

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void implicitFlowTaintWrapperTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void implicitFlowTaintWrapperNegativeTest() throws IOException{
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);
		infoflow.setTaintWrapper(new EasyTaintWrapper(Collections.<String, Set<String>>emptyMap()));
    	infoflow.setSootConfig(new IInfoflowConfig() {
			
			@Override
			public void setSootOptions(Options options) {
				options.set_include(Collections.<String>emptyList());
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
			}
			
		});

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void implicitFlowTaintWrapperTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

	@Test(timeout=300000)
	public void hierarchicalCallSetTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void hierarchicalCallSetTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000)
	public void conditionalAliasingTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout=300000)
	public void conditionalAliasingTest2(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);	
	}

	@Test(timeout=300000)
	public void conditionalAliasingTest3(){
		Infoflow infoflow = initInfoflow();
		infoflow.setInspectSinks(false);
		infoflow.setEnableImplicitFlows(true);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalAliasingTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
	}

	@Test(timeout=300000) 
    public void testStringConvert(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);
		infoflow.setEnableStaticFieldTracking(false);

		List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConvert()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }

	@Test(timeout=300000)
	public void afterCallNegativeTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void afterCallNegativeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);	
	}

	@Test(timeout=300000)
	public void ifInCalleeTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void ifInCalleeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout=300000)
	public void activationConditionalTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void activationConditionalTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);	
	}

	@Test(timeout=300000)
	public void classTypeTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void classTypeTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test(timeout=300000)
	public void conditionalReturnTest(){
		Infoflow infoflow = initInfoflow();
		infoflow.setEnableImplicitFlows(true);
		infoflow.setInspectSinks(false);

		List<String> epoints = new ArrayList<String>();
	    epoints.add("<soot.jimple.infoflow.test.ImplicitFlowTestCode: void conditionalReturnTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
