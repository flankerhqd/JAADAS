package soot.jimple.infoflow.test.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.options.Options;

/**
 * These tests check whether exceptional control- and data flows are handled
 * correctly.
 * 
 * @author Steven Arzt
 */
public class ExceptionTests extends JUnitTests {
	
	@Test
	public void exceptionControlFlowTest1() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlWrappedFlowTest1() throws IOException {
		Infoflow infoflow = initInfoflow();
		infoflow.setTaintWrapper(new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt")));
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlFlowTestNoJDK1() throws IOException {
		Infoflow infoflow = initInfoflow();
		infoflow.setTaintWrapper(new EasyTaintWrapper(new File("EasyTaintWrapperSource.txt")));
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
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest1()>");
		infoflow.computeInfoflow(appPath, null, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionControlFlowTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test
	public void exceptionControlFlowTest3() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionControlFlowTest3()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}
	
	@Test
	public void exceptionDataFlowTest1() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionDataFlowTest1()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

	@Test
	public void exceptionDataFlowTest2() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.ExceptionTestCode: void exceptionDataFlowTest2()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		checkInfoflow(infoflow, 1);
	}

}
