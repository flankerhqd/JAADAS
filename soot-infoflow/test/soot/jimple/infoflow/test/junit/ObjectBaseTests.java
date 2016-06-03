package soot.jimple.infoflow.test.junit;

import org.junit.Test;
import soot.jimple.infoflow.Infoflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hqd on 3/10/15.
 */
public class ObjectBaseTests extends JUnitTests{
    @Test(timeout=300000)
    public void multiTest1(){
        Infoflow infoflow = initInfoflow();
        List<String> epoints = new ArrayList<String>();
        epoints.add("<soot.jimple.infoflow.test.ObjectBaseTestCode: void simpleTestBaseActivity()>");
        infoflow.computeInfoflow(appPath, libPath, epoints, sources, sinks);
        negativeCheckInfoflow(infoflow);
    }
}
