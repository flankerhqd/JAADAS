package org.k33nteam.jade.propagation.track;

import java.util.List;

import org.k33nteam.jade.bean.VulnResult;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public interface IAPIVulnManager {

	/**
	 * return null if we don't want to track this api.
	 * @param stmt
	 * @return
	 */
	public Value getVulnParamAsSource(Stmt stmt);
	//shouldStartTrackAPIVuln may call isParamVulnAndStore
	/**
	 * return false if we should not continue tracking, although in most cases, constant/field-ref found already means we don't need to continue tracking
	 * @param originStmt
	 * @param reachedValue
	 * @return
	 */
	public void isParamVulnAndStore(SootMethod originMethod, Stmt originStmt, Value reachedValue);
	public List<VulnResult> getAnalysisResult();
	
}
