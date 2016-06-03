package soot.jimple.infoflow.aliasing;

import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public abstract class AbstractBulkAliasStrategy extends AbstractAliasStrategy {

	public AbstractBulkAliasStrategy(IInfoflowCFG cfg) {
		super(cfg);
	}

	@Override
	public boolean isInteractive() {
		return false;
	}
	
	@Override
	public boolean mayAlias(AccessPath ap1, AccessPath ap2) {
		return false;
	}
	
}
