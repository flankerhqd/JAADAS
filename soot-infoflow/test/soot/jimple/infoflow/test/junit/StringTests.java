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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;
/**
 * covers taint propagation for Strings, String functions such as concat, toUpperCase() or substring, but also StringBuilder and concatenation via '+' operato
 * @author Christian
 *
 */
public class StringTests extends JUnitTests {
	
	@Test(timeout=600000)
    public void multipleSourcesTest(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void multipleSources()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
	
	@Test(timeout=600000)
    public void substringTest(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodSubstring()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
	
	@Test(timeout=600000)
    public void lowerCaseTest(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringLowerCase()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
	
	@Test(timeout=600000)
    public void upperCaseTest(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringUpperCase()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }

	@Test(timeout=600000)
    public void concatTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void concatTest1b(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1b()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void concatTest1c(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat1c(java.lang.String)>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void concatTest2(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void stringConcatTest(){
	   Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
   
	@Test(timeout=600000)
	public void stringConcatTestSmall1(){
		Infoflow infoflow = initInfoflow();
   		List<String> epoints = new ArrayList<String>();
   		epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTestSmall1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
   }
   
	@Test(timeout=600000)
	public void stringConcatTestSmall2(){
		Infoflow infoflow = initInfoflow();
   		List<String> epoints = new ArrayList<String>();
   		epoints.add("<soot.jimple.infoflow.test.StringTestCode: void stringConcatTestSmall2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
    
	@Test(timeout=600000)
    public void concatTestNegative(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatNegative()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
    }
    
	@Test(timeout=600000)
    public void concatPlusTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatPlus1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void concatPlusTest2(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcatPlus2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }
    
	@Test(timeout=600000)
    public void valueOfTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodValueOf()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void toStringTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodtoString()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void stringBufferTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuffer1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void stringBufferTest2(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuffer2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
   }
    
	@Test(timeout=600000)
    public void stringBuilderTest1(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void stringBuilderTest2(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);		
    }
    
	@Test(timeout=600000)
    public void stringBuilderTest2_NoJDK(){
    	Infoflow infoflow = initInfoflow(true);
    	infoflow.setSootConfig(new IInfoflowConfig() {
			
			@Override
			public void setSootOptions(Options options) {
				List<String> excludeList = new ArrayList<String>();
				excludeList.add("java.");
				excludeList.add("javax.");
				options.set_exclude(excludeList);
				options.set_prepend_classpath(false);
			}
			
		});
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);		
    }

	@Test(timeout=600000)
    public void stringBuilderTest3(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }
    
	@Test(timeout=600000)
    public void stringBuilderTest4(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder4(java.lang.String)>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }
    
	@Test(timeout=600000)
    public void stringBuilderTest5(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder5()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }

	@Test(timeout=600000)
    public void stringBuilderTest6(){
    	Infoflow infoflow = initInfoflow(true);
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringBuilder6()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);	
    }
    
	@Test(timeout=600000)
    public void testcharArray(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void getChars()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
    }
    
	@Test(timeout=600000)
    public void testStringConcat(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConcat()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
		assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourcePwd));
		assertTrue(infoflow.getResults().isPathBetweenMethods(sink, sourceDeviceId));
    }

	@Test(timeout=600000)
    public void testStringConstructor(){
    	Infoflow infoflow = initInfoflow();
    	List<String> epoints = new ArrayList<String>();
    	epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodStringConstructor()>");
    	
    	List<String> sources = new LinkedList<String>();
    	sources.add("<java.lang.String: void <init>(java.lang.String)>");
    	
    	List<String> sinks = new LinkedList<String>();
    	sinks.add("<java.lang.Runtime: java.lang.Process exec(java.lang.String)>");

		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
    	assertNotNull(infoflow.getResults());
		assertTrue(infoflow.getResults().isPathBetweenMethods(sinks.get(0), sources.get(0)));
    }

    @Test
    public void methodToCharArrayTest(){
        Infoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.StringTestCode: void methodToCharArray()>");
        infoflow.computeInfoflow(appPath, libPath, epoints,sources, sinks);
        checkInfoflow(infoflow, 1);
    }

}
