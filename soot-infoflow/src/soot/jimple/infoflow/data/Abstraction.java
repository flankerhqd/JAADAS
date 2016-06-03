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
package soot.jimple.infoflow.data;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import soot.NullType;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.collect.AtomicBitSet;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.internal.JimpleLocal;

import com.google.common.collect.Sets;

/**
 * The abstraction class contains all information that is necessary to track the taint.
 * 
 * @author Steven Arzt
 * @author Christian Fritz
 */
public class Abstraction implements Cloneable, FastSolverLinkedNode<Abstraction, Unit> {
	
	private static boolean flowSensitiveAliasing = true;
	
	/**
	 * the access path contains the currently tainted variable or field
	 */
	private final AccessPath accessPath;
	
	private Abstraction predecessor = null;
	private Set<Abstraction> neighbors = null;
	private Stmt currentStmt = null;
	private Stmt correspondingCallSite = null;
	
	private SourceContext sourceContext = null;

	// only used in path generation
	private Set<SourceContextAndPath> pathCache = null;
	
	/**
	 * Unit/Stmt which activates the taint when the abstraction passes it
	 */
	private Unit activationUnit = null;
	/**
	 * taint is thrown by an exception (is set to false when it reaches the catch-Stmt)
	 */
	private boolean exceptionThrown = false;
	private int hashCode = 0;

	/**
	 * The postdominators we need to pass in order to leave the current conditional
	 * branch. Do not use the synchronized Stack class here to avoid deadlocks.
	 */
	private List<UnitContainer> postdominators = null;
	private boolean isImplicit = false;
	
	/**
	 * Only valid for inactive abstractions. Specifies whether an access paths
	 * has been cut during alias analysis.
	 */
	private boolean dependsOnCutAP = false;
	
	private AtomicBitSet pathFlags = null;
	
	public Abstraction(AccessPath sourceVal,
			Stmt sourceStmt,
			Object userData,
			boolean exceptionThrown,
			boolean isImplicit){
		this(sourceVal,
				new SourceContext(sourceVal, sourceStmt, userData),
				exceptionThrown, isImplicit);
	}

	protected Abstraction(AccessPath apToTaint,
			SourceContext sourceContext,
			boolean exceptionThrown,
			boolean isImplicit){
		this.sourceContext = sourceContext;
		this.accessPath = apToTaint;
		this.activationUnit = null;
		this.exceptionThrown = exceptionThrown;
		
		this.neighbors = null;
		this.isImplicit = isImplicit;
		this.currentStmt = sourceContext == null ? null : sourceContext.getStmt();
	}

	/**
	 * Creates an abstraction as a copy of an existing abstraction,
	 * only exchanging the access path. -> only used by AbstractionWithPath
	 * @param p The access path for the new abstraction
	 * @param original The original abstraction to copy
	 */
	protected Abstraction(AccessPath p, Abstraction original){
		if (original == null) {
			sourceContext = null;
			exceptionThrown = false;
			activationUnit = null;
			isImplicit = false;
		}
		else {
			sourceContext = original.sourceContext;
			exceptionThrown = original.exceptionThrown;
			activationUnit = original.activationUnit;
			assert activationUnit == null || flowSensitiveAliasing;
			
			postdominators = original.postdominators == null ? null
					: new ArrayList<UnitContainer>(original.postdominators);
			
			dependsOnCutAP = original.dependsOnCutAP;
			isImplicit = original.isImplicit;
		}
		accessPath = p;
		neighbors = null;
		currentStmt = null;
	}
	
	public final Abstraction deriveInactiveAbstraction(Unit activationUnit){
		if (!flowSensitiveAliasing) {
			assert this.isAbstractionActive();
			return this;
		}
		
		// If this abstraction is already inactive, we keep it
		if (!this.isAbstractionActive())
			return this;

		Abstraction a = deriveNewAbstractionMutable(accessPath, null);
		a.postdominators = null;
		a.activationUnit = activationUnit;
		a.dependsOnCutAP |= a.getAccessPath().isCutOffApproximation();
		return a;
	}

	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt){
		return deriveNewAbstraction(p, currentStmt, isImplicit);
	}
	
	public Abstraction deriveNewAbstraction(AccessPath p, Stmt currentStmt,
			boolean isImplicit){
		// If the new abstraction looks exactly like the current one, there is
		// no need to create a new object
		if (this.accessPath.equals(p) && this.currentStmt == currentStmt
				&& this.isImplicit == isImplicit)
			return this;
		
		Abstraction abs = deriveNewAbstractionMutable(p, currentStmt);
		abs.isImplicit = isImplicit;
		return abs;
	}
	
	private Abstraction deriveNewAbstractionMutable(AccessPath p, Stmt currentStmt){
		if (this.accessPath.equals(p) && this.currentStmt == currentStmt) {
			Abstraction abs = clone();
			abs.currentStmt = currentStmt;
			return abs;
		}
		
		Abstraction abs = new Abstraction(p, this);
		abs.predecessor = this;
		abs.currentStmt = currentStmt;
		
		if (!abs.getAccessPath().isEmpty())
			abs.postdominators = null;
		if (!abs.isAbstractionActive())
			abs.dependsOnCutAP = abs.dependsOnCutAP || p.isCutOffApproximation();
		
		abs.sourceContext = null;
		return abs;
	}
	
	public final Abstraction deriveNewAbstraction(Value taint, boolean cutFirstField, Stmt currentStmt,
			Type baseType){
		assert !this.getAccessPath().isEmpty();
		
		AccessPath newAP = accessPath.copyWithNewValue(taint, baseType, cutFirstField);
		if (this.getAccessPath().equals(newAP) && this.currentStmt == currentStmt)
			return this;
		return deriveNewAbstractionMutable(newAP, currentStmt);
	}

	/**
	 * Derives a new abstraction that models the current local being thrown as
	 * an exception
	 * @param throwStmt The statement at which the exception was thrown
	 * @return The newly derived abstraction
	 */
	public final Abstraction deriveNewAbstractionOnThrow(Stmt throwStmt){
		assert !this.exceptionThrown;
		Abstraction abs = clone();
		
		abs.currentStmt = throwStmt;
		abs.sourceContext = null;
		abs.exceptionThrown = true;
		return abs;
	}
	
	/**
	 * Derives a new abstraction that models the current local being caught as
	 * an exception
	 * @param taint The value in which the tainted exception is stored
	 * @return The newly derived abstraction
	 */
	public final Abstraction deriveNewAbstractionOnCatch(Value taint){
		assert this.exceptionThrown;
		Abstraction abs = deriveNewAbstractionMutable(new AccessPath(taint, true), null);
		abs.exceptionThrown = false;
		return abs;
	}
		
	/**
	 * Gets the path of statements from the source to the current statement
	 * with which this abstraction is associated. If this path is ambiguous,
	 * a single path is selected randomly.
	 * @return The path from the source to the current statement
	 */
	public Set<SourceContextAndPath> getPaths() {
		return pathCache == null ? null : Collections.unmodifiableSet(pathCache);
	}
	
	public Set<SourceContextAndPath> getOrMakePathCache() {
		// We're optimistic about having a path cache. If we definitely have one,
		// we return it. Otherwise, we need to lock and create one.
		if (this.pathCache == null)
			synchronized (this) {
				if (this.pathCache == null)
					this.pathCache = new ConcurrentHashSet<SourceContextAndPath>();
			}
		return Collections.unmodifiableSet(pathCache);
	}
	
	public boolean addPathElement(SourceContextAndPath scap) {
		if (this.pathCache == null) {
			synchronized (this) {
				if (this.pathCache == null) {
					this.pathCache = new ConcurrentHashSet<SourceContextAndPath>();
				}
			}
		}
		return this.pathCache.add(scap);
	}
	
	public void clearPathCache() {
		this.pathCache = null;
	}
	
	public boolean isAbstractionActive() {
		return activationUnit == null;
	}
	
	public boolean isImplicit() {
		return isImplicit;
	}
	
	@Override
	public String toString(){
		return (isAbstractionActive()?"":"_")+accessPath.toString() + " | "+(activationUnit==null?"":activationUnit.toString()) + ">>";
	}
	
	public AccessPath getAccessPath(){
		return accessPath;
	}
	
	public Unit getActivationUnit(){
		return this.activationUnit;
	}
	
	public Abstraction getActiveCopy(){
		assert !this.isAbstractionActive();
		
		Abstraction a = clone();
		a.sourceContext = null;
		a.activationUnit = null;
		return a;
	}
	
	/**
	 * Gets whether this value has been thrown as an exception
	 * @return True if this value has been thrown as an exception, otherwise
	 * false
	 */
	public boolean getExceptionThrown() {
		return this.exceptionThrown;
	}
	
	public final Abstraction deriveConditionalAbstractionEnter(UnitContainer postdom,
			Stmt conditionalUnit) {
		assert this.isAbstractionActive();
		
		if (postdominators != null && postdominators.contains(postdom))
			return this;
		
		Abstraction abs = deriveNewAbstractionMutable
				(AccessPath.getEmptyAccessPath(), conditionalUnit);
		if (abs.postdominators == null)
			abs.postdominators = Collections.singletonList(postdom);
		else
			abs.postdominators.add(0, postdom);
		return abs;
	}
	
	public final Abstraction deriveConditionalAbstractionCall(Unit conditionalCallSite) {
		assert this.isAbstractionActive();
		assert conditionalCallSite != null;
		
		Abstraction abs = deriveNewAbstractionMutable
				(AccessPath.getEmptyAccessPath(), (Stmt) conditionalCallSite);
		
		// Postdominators are only kept intraprocedurally in order to not
		// mess up the summary functions with caller-side information
		abs.postdominators = null;

		return abs;
	}
	
	public final Abstraction dropTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return this;
		
		Abstraction abs = clone();
		abs.sourceContext = null;
		abs.postdominators.remove(0);
		return abs;
	}
	
	public UnitContainer getTopPostdominator() {
		if (postdominators == null || postdominators.isEmpty())
			return null;
		return this.postdominators.get(0);
	}
	
	public boolean isTopPostdominator(Unit u) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getUnit() == u;
	}

	public boolean isTopPostdominator(SootMethod sm) {
		UnitContainer uc = getTopPostdominator();
		if (uc == null)
			return false;
		return uc.getMethod() == sm;
	}
	
	@Override
	public Abstraction clone() {
		Abstraction abs = new Abstraction(accessPath, this);
		abs.predecessor = this;
		abs.neighbors = null;
		abs.currentStmt = null;
		
		assert abs.equals(this);
		return abs;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Abstraction other = (Abstraction) obj;
		
		// If we have already computed hash codes, we can use them for
		// comparison
		if (this.hashCode != 0
				&& other.hashCode != 0
				&& this.hashCode != other.hashCode)
			return false;
		
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		
		return localEquals(other);
	}
	
	/**
	 * Checks whether this object locally equals the given object, i.e. the both
	 * are equal modulo the access path
	 * @param other The object to compare this object with
	 * @return True if this object is locally equal to the given one, otherwise
	 * false
	 */
	private boolean localEquals(Abstraction other) {
		// deliberately ignore prevAbs
		if (sourceContext == null) {
			if (other.sourceContext != null)
				return false;
		} else if (!sourceContext.equals(other.sourceContext))
			return false;
		if (activationUnit == null) {
			if (other.activationUnit != null)
				return false;
		} else if (!activationUnit.equals(other.activationUnit))
			return false;
		if (this.exceptionThrown != other.exceptionThrown)
			return false;
		if (postdominators == null) {
			if (other.postdominators != null)
				return false;
		} else if (!postdominators.equals(other.postdominators))
			return false;
		if(this.dependsOnCutAP != other.dependsOnCutAP)
			return false;
		if(this.isImplicit != other.isImplicit)
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		if (this.hashCode != 0)
			return hashCode;

		final int prime = 31;
		int result = 1;
	
		// deliberately ignore prevAbs
		result = prime * result + ((sourceContext == null) ? 0 : sourceContext.hashCode());
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((activationUnit == null) ? 0 : activationUnit.hashCode());
		result = prime * result + (exceptionThrown ? 1231 : 1237);
		result = prime * result + ((postdominators == null) ? 0 : postdominators.hashCode());
		result = prime * result + (dependsOnCutAP ? 1231 : 1237);
		result = prime * result + (isImplicit ? 1231 : 1237);
		this.hashCode = result;
		
		return this.hashCode;
	}
	
	/**
	 * Checks whether this abstraction entails the given abstraction, i.e. this
	 * taint also taints everything that is tainted by the given taint.
	 * @param other The other taint abstraction
	 * @return True if this object at least taints everything that is also tainted
	 * by the given object
	 */
	public boolean entails(Abstraction other) {
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.entails(other.accessPath))
			return false;
		return localEquals(other);
	}

	/**
	 * Gets the context of the taint, i.e. the statement and value of the source
	 * @return The statement and value of the source
	 */
	public SourceContext getSourceContext() {
		return sourceContext;
	}
	
	public boolean dependsOnCutAP() {
		return dependsOnCutAP;
	}
	
	@Override
	public Abstraction getPredecessor() {
		return this.predecessor;
	}
	
	public Set<Abstraction> getNeighbors() {
		return this.neighbors;
	}
	
	public Stmt getCurrentStmt() {
		return this.currentStmt;
	}
	
	@Override
	public void addNeighbor(Abstraction originalAbstraction) {
		// We should not register ourselves as a neighbor
		if (originalAbstraction == this)
			return;
		
		// We should not add identical nodes as neighbors
		if (this.predecessor == originalAbstraction.predecessor
				&& this.currentStmt == originalAbstraction.currentStmt
				&& this.predecessor == originalAbstraction.predecessor)
			return;
		
		synchronized (this) {
			if (neighbors == null)
				neighbors = Sets.newIdentityHashSet();
			else if (Infoflow.getMergeNeighbors()) {
				// Check if we already have an identical neighbor
				for (Abstraction nb : neighbors) {
					if (nb == originalAbstraction)
						return;
					if (originalAbstraction.predecessor == nb.predecessor
							&& originalAbstraction.currentStmt == nb.currentStmt
							&& originalAbstraction.predecessor == nb.predecessor) {
						return;
					}
				}
			}
			this.neighbors.add(originalAbstraction);
		}
	}
	
	public void setCorrespondingCallSite(Stmt callSite) {
		this.correspondingCallSite = callSite;
	}
	
	public Stmt getCorrespondingCallSite() {
		return this.correspondingCallSite;
	}
		
	public static Abstraction getZeroAbstraction(boolean flowSensitiveAliasing) {
		Abstraction zeroValue = new Abstraction(
				new AccessPath(new JimpleLocal("zero", NullType.v()), false),
				null,
				false,
				false);
		Abstraction.flowSensitiveAliasing = flowSensitiveAliasing;
		return zeroValue;
	}

	@Override
	public void setPredecessor(Abstraction predecessor) {
		this.predecessor = predecessor;
	}

	@Override
	public void setCallingContext(Abstraction callingContext) {
	}
	
	/**
	 * Only use this method if you really need to fake a source context and know
	 * what you are doing.
	 * @param sourceContext The new source context
	 */
	public void setSourceContext(SourceContext sourceContext) {
		this.sourceContext = sourceContext;
	}
	
	/**
	 * Registers that a worker thread with the given ID has already processed
	 * this abstraction
	 * @param id The ID of the worker thread
	 * @return True if the worker thread with the given ID has not been
	 * registered before, otherwise false
	 */
	public boolean registerPathFlag(int id, int maxSize) {
		if (pathFlags != null && id < pathFlags.size() && pathFlags.get(id))
			return false;
		
		if (pathFlags == null) {
			synchronized (this) {
				if (pathFlags == null) {
					// Make sure that the field is set only after the constructor
					// is done and the object is fully usable
					AtomicBitSet pf = new AtomicBitSet(maxSize);
					pathFlags = pf;
				}
			}
		}
		return pathFlags.set(id);
	}
	
	public Abstraction injectSourceContext(SourceContext sourceContext) {
		if (this.sourceContext != null && this.sourceContext.equals(sourceContext))
			return this;
		
		Abstraction abs = clone();
		abs.predecessor = null;
		abs.neighbors = null;
		abs.sourceContext = sourceContext;
		abs.currentStmt = this.currentStmt;
		return abs;
	}
		
}
