package org.k33nteam.jade.solver.model;


import soot.Local;
import soot.SootMethod;
import soot.jimple.Stmt;

public class IntentSource {
	private int kind = IntentSourceType.UNKNOWN;// IntentSourceType.FROM_CALL_RETURN_SITE;
	
	private Stmt new_alloc_stmt;
	private SootMethod call_site_return_stmt;
	private Local param_local;
	private SootMethod context;
}
