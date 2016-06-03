/*
 * Copyright (c) 2015. All rights reserved. Author flankerhe@keencloudtech.
 */

/* Soot - a J*va Optimization Framework
 * Copyright (C) 2014 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package soot.jimple.toolkits.pointer;

import soot.*;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.internal.JCastExpr;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Conducts a method-local, equality-based may-alias analysis.
 *
 * @author Eric Bodden
 */
public class LocalMayEquivValueAnalysis extends ForwardFlowAnalysis<Unit, Set<Set<Value>>> {

    private Body body;

    public LocalMayEquivValueAnalysis(UnitGraph graph) {
        super(graph);
        body = graph.getBody();
        doAnalysis();
    }

    @Override
    protected void flowThrough(Set<Set<Value>> source, Unit unit, Set<Set<Value>> target) {
        target.addAll(source);
        if(unit instanceof DefinitionStmt) {
            DefinitionStmt def = (DefinitionStmt) unit;
            Value left = wrapValue(def.getLeftOp());
            Value right = wrapValue(def.getRightOp());
            if(right instanceof Constant) {
                //find the sets containing the left
                Set<Value> leftSet = null;
                for(Set<Value> s: source) {
                    if(s.contains(left)) {
                        leftSet = s;
                        break;
                    }
                }
                if(leftSet==null) throw new RuntimeException("internal error");
                //remove left from this set
                target.remove(leftSet);
                HashSet<Value> setWithoutLeft = new HashSet<Value>(leftSet);
                setWithoutLeft.remove(left);
                target.add(setWithoutLeft);
                //add left on its own
                target.add(Collections.singleton(left));
            } else {
                //FLANKER ADD: FIXME
                //the origin source code treat CastExpr and value in CastExpr as diffrent, thus causing
                //inconsistency
                //find the sets containing the left and right hand sides
                Set<Value> leftSet = null, rightSet = null, castSet = null;
                for(Set<Value> s: source) {
                    if(s.contains(left)) {
                        leftSet = s;
                        break;
                    }
                }
                for(Set<Value> s: source) {
                    if(s.contains(right)) {
                        rightSet = s;
                        break;
                    }
                }
                //add castexpr check
                if (right instanceof JCastExpr) {
                    JCastExpr expr = (JCastExpr)right;
                    Value value = expr.getOp();
                    for(Set<Value> s:source)
                    {
                        if (s.contains(value)) {
                            castSet = s;
                            break;
                        }
                    }
                }
                if(leftSet==null || rightSet==null) throw new RuntimeException("internal error");
                //replace the sets by their union
                target.remove(leftSet);
                target.remove(rightSet);
                if (castSet != null) {
                    target.remove(castSet);
                }
                HashSet<Value> union = new HashSet<Value>(leftSet);
                union.addAll(rightSet);
                if (castSet != null) {
                    union.addAll(castSet);
                }
                target.add(union);
            }
        }
    }

    @Override
    protected void copy(Set<Set<Value>> source, Set<Set<Value>> target) {
        target.clear();
        target.addAll(source);
    }

    @Override
    protected Set<Set<Value>> entryInitialFlow() {
        //initially all values only alias themselves
        Set<Set<Value>> res = new HashSet<Set<Value>>();
        for(ValueBox vb: body.getUseAndDefBoxes()) {
            res.add(Collections.singleton(wrapValue(vb.getValue())));
        }
        return res;
    }

    //FLANKER FIX
    private Value wrapValue(Value value)
    {
        if(value instanceof EquivalentValue)
        {
            return value;
        }
        else if(value instanceof FieldRef)
        {
            return new EquivalentValue(value);
        }
        else
        {
            return value;
        }
    }
    @Override
    protected void merge(Set<Set<Value>> source1, Set<Set<Value>> source2, Set<Set<Value>> target) {
        //we could instead also merge all sets that are non-disjoint
        target.clear();
        target.addAll(source1);
        target.addAll(source2);
    }

    @Override
    protected Set<Set<Value>> newInitialFlow() {
        return new HashSet<Set<Value>>();
    }

    /**
     * Returns true if v1 and v2 may alias before u.
     */
    public boolean mayAlias(Value v1, Value v2, Unit u) {
        Set<Set<Value>> res = getFlowBefore(u);
        for (Set<Value> set : res) {
            if(set.contains(v1) && set.contains(v2))
                return true;
        }
        return false;
    }

    /**
     * Returns all values that may-alias with v before u.
     */
    public Set<Value> mayAliases(Value v, Unit u) {
        Set<Value> res = new HashSet<Value>();
        Set<Set<Value>> flow = getFlowBefore(u);
        for (Set<Value> set : flow) {
            if(set.contains(v))
                res.addAll(set);
        }
        return res;
    }

    /**
     * Returns all values that may-alias with v at the end of the procedure.
     */
    public Set<Value> mayAliasesAtExit(Value v) {
        Set<Value> res = new HashSet<Value>();
        for(Unit u: graph.getTails()) {
            Set<Set<Value>> flow = getFlowAfter(u);
            for (Set<Value> set : flow) {
                if(set.contains(v))
                    res.addAll(set);
            }
        }
        return res;
    }
}
