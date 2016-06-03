package org.k33nteam.jade.propagation.track;

import org.k33nteam.jade.bean.VulnResult;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

import java.util.List;

/**
 * Created by hqdvista on 5/9/16.
 */
public class StubAPIVulnManager implements IAPIVulnManager {

    @Override
    public Value getVulnParamAsSource(Stmt stmt) {
        if(stmt.containsInvokeExpr() && stmt.getInvokeExpr().getMethod().getName().contains("sink"))
        {
            return stmt.getInvokeExpr().getArg(0);
        }
        return null;
    }

    @Override
    public void isParamVulnAndStore(SootMethod originMethod, Stmt originStmt, Value reachedValue) {
        System.out.println("originamMethod " + originMethod.getSignature() + "originstmt " + originStmt + "reached value " + reachedValue);
    }

    @Override
    public List<VulnResult> getAnalysisResult() {
        return null;
    }
}
