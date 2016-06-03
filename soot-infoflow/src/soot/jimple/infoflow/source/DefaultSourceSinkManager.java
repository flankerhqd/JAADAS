/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.source;

import heros.InterproceduralCFG;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;

/**
 * A {@link ISourceSinkManager} working on lists of source and sink methods
 * 
 * @author Steven Arzt
 */
public class DefaultSourceSinkManager implements ISourceSinkManager  {

	private Collection<String> sources;
	private Collection<String> sinks;

	private Collection<String> returnTaintMethods;
	private Collection<String> parameterTaintMethods;
	
	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources
	 *            The list of methods to be treated as sources
	 * @param sinks
	 *            The list of methods to be treated as sins
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks) {
		this(sources, sinks, null, null);
	}

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources
	 *            The list of methods to be treated as sources
	 * @param sinks
	 *            The list of methods to be treated as sinks
	 * @param parameterTaintMethods
	 *            The list of methods whose parameters shall be regarded as
	 *            sources
	 * @param returnTaintMethods
	 *            The list of methods whose return values shall be regarded as
	 *            sinks
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks, Collection<String> parameterTaintMethods, Collection<String> returnTaintMethods) {
		this.sources = sources;
		this.sinks = sinks;
		this.parameterTaintMethods = (parameterTaintMethods != null) ? parameterTaintMethods : new HashSet<String>();
		this.returnTaintMethods = (returnTaintMethods != null) ? returnTaintMethods : new HashSet<String>();
	}

	/**
	 * Sets the list of methods to be treated as sources
	 * 
	 * @param sources
	 *            The list of methods to be treated as sources
	 */
	public void setSources(List<String> sources) {
		this.sources = sources;
	}

	/**
	 * Sets the list of methods to be treated as sinks
	 * 
	 * @param sinks
	 *            The list of methods to be treated as sinks
	 */
	public void setSinks(List<String> sinks) {
		this.sinks = sinks;
	}
	
	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
		SootMethod callee = sCallSite.containsInvokeExpr() ?
				sCallSite.getInvokeExpr().getMethod() : null;
		
		AccessPath targetAP = null;
		if (callee != null && sources.contains(callee.toString())) {
			if (callee.getReturnType() != null 
					&& sCallSite instanceof DefinitionStmt) {
				// Taint the return value
				Value leftOp = ((DefinitionStmt) sCallSite).getLeftOp();
				targetAP = new AccessPath(leftOp, true);
			}
			else if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
				// Taint the base object
				Value base = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
				targetAP = new AccessPath(base, true);
			}
		}
		// Check whether we need to taint parameters
		else if (sCallSite instanceof IdentityStmt) {
			IdentityStmt istmt = (IdentityStmt) sCallSite;
			if (istmt.getRightOp() instanceof ParameterRef) {
				ParameterRef pref = (ParameterRef) istmt.getRightOp();
				SootMethod currentMethod = cfg.getMethodOf(istmt);
				if (parameterTaintMethods.contains(currentMethod.toString()))
					targetAP = new AccessPath(currentMethod.getActiveBody()
							.getParameterLocal(pref.getIndex()), true);
			}
		}
		
		if (targetAP == null)
			return null;
		
		// Create the source information data structure
		return new SourceInfo(targetAP);
	}

	@Override
	public boolean isSink(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg,
			AccessPath ap) {
		// Check whether values returned by the current method are to be
		// considered as sinks
		if (this.returnTaintMethods != null
				&& sCallSite instanceof ReturnStmt
				&& this.returnTaintMethods.contains(cfg.getMethodOf(sCallSite).getSignature()))
			return true;
		
		// Check whether the callee is a sink
		if (this.sinks != null
				&& sCallSite.containsInvokeExpr()
				&& this.sinks.contains(sCallSite.getInvokeExpr().getMethod().getSignature()))
			return true;
		
		return false;
	}

	/**
	 * Sets the list of methods whose parameters shall be regarded as taint
	 * sources
	 * 
	 * @param parameterTaintMethods
	 *            The list of methods whose parameters shall be regarded as
	 *            taint sources
	 */
	public void setParameterTaintMethods(List<String> parameterTaintMethods) {
		this.parameterTaintMethods = parameterTaintMethods;
	}

	/**
	 * Sets the list of methods whose return values shall be regarded as taint
	 * sinks
	 * 
	 * @param returnTaintMethods
	 *            The list of methods whose return values shall be regarded as
	 *            taint sinks
	 */
	public void setReturnTaintMethods(List<String> returnTaintMethods) {
		this.returnTaintMethods = returnTaintMethods;
	}
	
}
