/**
 * (c) Copyright 2013, Tata Consultancy Services & Ecole Polytechnique de Montreal
 * All rights reserved
 */
package soot.jimple.infoflow.cfg;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.ide.icfg.OnTheFlyJimpleBasedICFG;

/**
 * Default factory for bidirectional interprocedural CFGs
 * 
 * @author Steven Arzt
 * @author Marc-Andre Lavadiere
 */
public class DefaultBiDiICFGFactory implements BiDirICFGFactory {
	
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public IInfoflowCFG buildBiDirICFG(CallgraphAlgorithm callgraphAlgorithm){
    	if (callgraphAlgorithm == CallgraphAlgorithm.OnDemand) {
    		// Load all classes on the classpath to signatures
    		long beforeClassLoading = System.nanoTime();
    		OnTheFlyJimpleBasedICFG.loadAllClassesOnClassPathToSignatures();
    		logger.info("Class loading took {} seconds", (System.nanoTime() - beforeClassLoading) / 1E9);
    		
    		long beforeHierarchy = System.nanoTime();
    		Scene.v().getOrMakeFastHierarchy();
    		assert Scene.v().hasFastHierarchy();
    		logger.info("Hierarchy building took {} seconds", (System.nanoTime() - beforeHierarchy) / 1E9);

    		long beforeCFG = System.nanoTime();
    		IInfoflowCFG cfg = new InfoflowCFG(new OnTheFlyJimpleBasedICFG(Scene.v().getEntryPoints()));
    		logger.info("CFG generation took {} seconds", (System.nanoTime() - beforeCFG) / 1E9);

    		return cfg;
    	}
        return new InfoflowCFG();
    }
}
