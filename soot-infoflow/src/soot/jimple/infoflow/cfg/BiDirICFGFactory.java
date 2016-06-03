/**
 * (c) Copyright 2013, Tata Consultancy Services & Ecole Polytechnique de Montreal
 * All rights reserved
 */
package soot.jimple.infoflow.cfg;


import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public interface BiDirICFGFactory {

    public IInfoflowCFG buildBiDirICFG(CallgraphAlgorithm callgraphAlgorithm);

}
