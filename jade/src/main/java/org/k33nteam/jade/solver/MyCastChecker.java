package org.k33nteam.jade.solver;

import soot.jimple.toolkits.pointer.CastCheckEliminator;
import soot.toolkits.graph.BriefUnitGraph;

/**
 * Created by hqd on 12/24/14.
 */
public class MyCastChecker extends CastCheckEliminator {
    public MyCastChecker(BriefUnitGraph cfg) {
        super(cfg);
    }

    @Override
    protected void tagCasts() {
        //nop
    }
}
