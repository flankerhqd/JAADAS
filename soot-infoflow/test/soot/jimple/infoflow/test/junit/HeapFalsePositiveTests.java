package soot.jimple.infoflow.test.junit;

import org.junit.Test;
import soot.jimple.infoflow.Infoflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hqd on 1/29/15.
 */
public class HeapFalsePositiveTests extends JUnitTests {

	@Test(timeout = 300000)
	public void simpleTest() {
		Infoflow infoflow = initInfoflow();
		List<String> epoints = new ArrayList<String>();
		epoints.add("<soot.jimple.infoflow.test.HeapTestCode: void simpleTest()>");
		infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
		negativeCheckInfoflow(infoflow);
	}

    @Test(timeout = 300000)
    public void simpleTestGithubProblem() {
        Infoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.ProblemTestCode: void static5Test()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        checkInfoflow(infoflow,1);
    }

}
