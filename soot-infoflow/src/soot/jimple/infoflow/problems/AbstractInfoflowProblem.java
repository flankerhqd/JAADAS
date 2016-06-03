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
package soot.jimple.infoflow.problems;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.BooleanType;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.nativ.DefaultNativeCallHandler;
import soot.jimple.infoflow.nativ.NativeCallHandler;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * abstract super class which 
 * 	- concentrates functionality used by InfoflowProblem and BackwardsInfoflowProblem
 *  - contains helper functions which should not pollute the naturally large InfofflowProblems
 *
 */
public abstract class AbstractInfoflowProblem extends DefaultJimpleIFDSTabulationProblem<Abstraction,
			BiDiInterproceduralCFG<Unit, SootMethod>> {

	protected final Map<Unit, Set<Abstraction>> initialSeeds = new HashMap<Unit, Set<Abstraction>>();
	protected ITaintPropagationWrapper taintWrapper;
	
	protected final NativeCallHandler ncHandler = new DefaultNativeCallHandler();
	protected final ISourceSinkManager sourceSinkManager;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean enableImplicitFlows = false;
	protected boolean enableStaticFields = true;
	protected boolean enableExceptions = true;
	protected boolean flowSensitiveAliasing = true;
	protected boolean enableTypeChecking = true;
	protected boolean ignoreFlowsInSystemPackages = true;
	
	protected boolean inspectSources = false;
	protected boolean inspectSinks = false;

	private Abstraction zeroValue = null;
	
	protected IInfoflowSolver solver = null;
	
	protected boolean stopAfterFirstFlow = false;
	
	protected Set<TaintPropagationHandler> taintPropagationHandlers = null;

	private MyConcurrentHashMap<Unit, Set<Unit>> activationUnitsToCallSites =
			new MyConcurrentHashMap<Unit, Set<Unit>>();
	
	public AbstractInfoflowProblem(BiDiInterproceduralCFG<Unit, SootMethod> icfg,
			ISourceSinkManager sourceSinkManager) {
		super(icfg);
		this.sourceSinkManager = sourceSinkManager;
	}
	
	protected boolean canCastType(Type destType, Type sourceType) {
		if (!enableTypeChecking)
			return true;
		
		// If we don't have a source type, we generally allow the cast
		if (sourceType == null)
			return true;
		
		// If both types are equal, we allow the cast
		if (sourceType == destType)
			return true;
		
		// If we have a reference type, we use the Soot hierarchy
		if (Scene.v().getFastHierarchy().canStoreType(destType, sourceType) // cast-up, i.e. Object to String
				|| Scene.v().getFastHierarchy().canStoreType(sourceType, destType)) // cast-down, i.e. String to Object
			return true;
		
		// If both types are primitive, they can be cast unless a boolean type
		// is involved
		if (destType instanceof PrimType && sourceType instanceof PrimType)
			if (destType != BooleanType.v() && sourceType != BooleanType.v())
				return true;
			
		return false;
	}
		
	protected boolean hasCompatibleTypesForCall(AccessPath apBase, SootClass dest) {
		if (!enableTypeChecking)
			return true;

		// Cannot invoke a method on a primitive type
		if (apBase.getBaseType() instanceof PrimType)
			return false;
		// Cannot invoke a method on an array
		if (apBase.getBaseType() instanceof ArrayType)
			return dest.getName().equals("java.lang.Object");
		
		return Scene.v().getOrMakeFastHierarchy().canStoreType(apBase.getBaseType(), dest.getType())
				|| Scene.v().getOrMakeFastHierarchy().canStoreType(dest.getType(), apBase.getBaseType());
	}

	public void setSolver(IInfoflowSolver solver) {
		this.solver = solver;
	}
	
	public void setZeroValue(Abstraction zeroValue) {
		this.zeroValue = zeroValue;
	}

	/**
	 * we need this option as we start directly at the sources, but need to go 
	 * backward in the call stack
	 */
	@Override
	public boolean followReturnsPastSeeds(){
		return true;
	}
	
	public void setTaintWrapper(ITaintPropagationWrapper wrapper){
		taintWrapper = wrapper;
	}
		
	/**
	 * Sets whether the information flow analysis shall stop after the first
	 * flow has been found
	 * @param stopAfterFirstFlow True if the analysis shall stop after the
	 * first flow has been found, otherwise false.
	 */
	public void setStopAfterFirstFlow(boolean stopAfterFirstFlow) {
		this.stopAfterFirstFlow = stopAfterFirstFlow;
	}
		
	/**
	 * Sets whether the solver shall consider implicit flows.
	 * @param enableImplicitFlows True if implicit flows shall be considered,
	 * otherwise false.
	 */
	public void setEnableImplicitFlows(boolean enableImplicitFlows) {
		this.enableImplicitFlows = enableImplicitFlows;
	}

	/**
	 * Sets whether the solver shall consider assignments to static fields.
	 * @param enableStaticFields True if assignments to static fields shall be
	 * tracked, otherwise false
	 */
	public void setEnableStaticFieldTracking(boolean enableStaticFields) {
		this.enableStaticFields = enableStaticFields;
	}

	/**
	 * Sets whether the solver shall track taints over exceptions, i.e. throw
	 * new RuntimeException(secretData).
	 * @param enableExceptions True if taints in thrown exception objects shall
	 * be tracked.
	 */
	public void setEnableExceptionTracking(boolean enableExceptions) {
		this.enableExceptions = enableExceptions;
	}

	/**
	 * Sets whether the solver shall use flow sensitive aliasing. This makes
	 * the analysis more precise, but also requires more time.
	 * @param flowSensitiveAliasing True if flow sensitive aliasing shall be
	 * used, otherwise false
	 */
	public void setFlowSensitiveAliasing(boolean flowSensitiveAliasing) {
		this.flowSensitiveAliasing = flowSensitiveAliasing;
		
		// We need to reset the zero value since it depends on the flow
		// sensitivity setting
		this.zeroValue = null;
	}
	
	/**
	 * Sets whether type checking shall be done on casts and method calls
	 * @param enableTypeChecking True if type checking shall be performed,
	 * otherwise false
	 */
	public void setEnableTypeChecking(boolean enableTypeChecking) {
		this.enableTypeChecking = enableTypeChecking;
	}
	
	/**
	 * Sets whether flows starting or ending in system packages such as Android's
	 * support library shall be ignored.
	 * @param ignoreFlowsInSystemPackages True if flows starting or ending in
	 * system packages shall be ignored, otherwise false.
	 */
	public void setIgnoreFlowsInSystemPackages(boolean ignoreFlowsInSystemPackages) {
		this.ignoreFlowsInSystemPackages = ignoreFlowsInSystemPackages;
	}

	/**
	 * Gets whether the given method is an entry point, i.e. one of the initial
	 * seeds belongs to the given method
	 * @param sm The method to check
	 * @return True if the given method is an entry point, otherwise false
	 */
	protected boolean isInitialMethod(SootMethod sm) {
		for (Unit u : this.initialSeeds.keySet())
			if (interproceduralCFG().getMethodOf(u) == sm)
				return true;
		return false;
	}
	
	@Override
	public Map<Unit, Set<Abstraction>> initialSeeds() {
		return initialSeeds;
	}
	
	/**
	 * performance improvement: since we start directly at the sources, we do not 
	 * need to generate additional taints unconditionally
	 */
	@Override
	public boolean autoAddZero() {
		return false;
	}
	
	/**
	 * default: inspectSources is set to true, this means sources are analyzed as well.
	 * If inspectSources is set to false, then the analysis does not propagate values into 
	 * the source method. 
	 * @param inspect boolean that determines the inspectSource option
	 */
	public void setInspectSources(boolean inspect){
		inspectSources = inspect;
	}

	/**
	 * default: inspectSinks is set to true, this means sinks are analyzed as well.
	 * If inspectSinks is set to false, then the analysis does not propagate values into 
	 * the sink method. 
	 * @param inspect boolean that determines the inspectSink option
	 */
	public void setInspectSinks(boolean inspect){
		inspectSinks = inspect;
	}
		
	/**
	 * Checks whether the given base value matches the base of the given
	 * taint abstraction
	 * @param baseValue The value to check
	 * @param source The taint abstraction to check
	 * @return True if the given value has the same base value as the given
	 * taint abstraction, otherwise false
	 */
	protected boolean baseMatches(final Value baseValue, Abstraction source) {
		if (baseValue instanceof Local) {
			if (baseValue.equals(source.getAccessPath().getPlainValue()))
				return true;
		}
		else if (baseValue instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) baseValue;
			if (ifr.getBase().equals(source.getAccessPath().getPlainValue())
					&& source.getAccessPath().firstFieldMatches(ifr.getField()))
				return true;
		}
		else if (baseValue instanceof StaticFieldRef) {
			StaticFieldRef sfr = (StaticFieldRef) baseValue;
			if (source.getAccessPath().firstFieldMatches(sfr.getField()))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks whether the given base value matches the base of the given
	 * taint abstraction and ends there. So a will match a, but not a.x.
	 * Not that this function will still match a to a.*.
	 * @param baseValue The value to check
	 * @param source The taint abstraction to check
	 * @return True if the given value has the same base value as the given
	 * taint abstraction and no further elements, otherwise false
	 */
	protected boolean baseMatchesStrict(final Value baseValue, Abstraction source) {
		if (!baseMatches(baseValue, source))
			return false;
		
		if (baseValue instanceof Local)
			return source.getAccessPath().isLocal();
		else if (baseValue instanceof InstanceFieldRef || baseValue instanceof StaticFieldRef)
			return source.getAccessPath().getFieldCount() == 1;
		
		throw new RuntimeException("Unexpected left side");
	}
	
	protected boolean isCallSiteActivatingTaint(Unit callSite, Unit activationUnit) {
		if (!flowSensitiveAliasing)
			return false;

		if (activationUnit == null)
			return false;
		Set<Unit> callSites = activationUnitsToCallSites.get(activationUnit);
		return (callSites != null && callSites.contains(callSite));
	}
	
	protected boolean registerActivationCallSite(Unit callSite, SootMethod callee, Abstraction activationAbs) {
		if (!flowSensitiveAliasing)
			return false;
		Unit activationUnit = activationAbs.getActivationUnit();
		if (activationUnit == null)
			return false;

		Set<Unit> callSites = activationUnitsToCallSites.putIfAbsentElseGet
				(activationUnit, new ConcurrentHashSet<Unit>());
		if (callSites.contains(callSite))
			return false;
		
		if (!activationAbs.isAbstractionActive())
			if (!callee.getActiveBody().getUnits().contains(activationUnit)) {
				boolean found = false;
				for (Unit au : callSites)
					if (callee.getActiveBody().getUnits().contains(au)) {
						found = true;
						break;
					}
				if (!found)
					return false;
			}

		return callSites.add(callSite);
	}
	
	public void setActivationUnitsToCallSites(AbstractInfoflowProblem other) {
		this.activationUnitsToCallSites = other.activationUnitsToCallSites;
	}

	@Override
	public IInfoflowCFG interproceduralCFG() {
		return (IInfoflowCFG) super.interproceduralCFG();
	}
	
	/**
	 * Adds the given initial seeds to the information flow problem
	 * @param unit The unit to be considered as a seed
	 * @param seeds The abstractions with which to start at the given seed
	 */
	public void addInitialSeeds(Unit unit, Set<Abstraction> seeds) {
		if (this.initialSeeds.containsKey(unit))
			this.initialSeeds.get(unit).addAll(seeds);
		else
			this.initialSeeds.put(unit, new HashSet<Abstraction>(seeds));
	}
	
	/**
	 * Gets whether this information flow problem has initial seeds
	 * @return True if this information flow problem has initial seeds,
	 * otherwise false
	 */
	public boolean hasInitialSeeds() {
		return !this.initialSeeds.isEmpty();
	}

	/**
	 * Gets the initial seeds with which this information flow problem has been
	 * configured
	 * @return The initial seeds with which this information flow problem has
	 * been configured.
	 */
	public Map<Unit, Set<Abstraction>> getInitialSeeds() {
		return this.initialSeeds;
	}
	
	/**
	 * Adds a handler which is invoked whenever a taint is propagated
	 * @param handler The handler to be invoked when propagating taints
	 */
	public void addTaintPropagationHandler(TaintPropagationHandler handler) {
		if (this.taintPropagationHandlers == null)
			this.taintPropagationHandlers = new HashSet<>();
		this.taintPropagationHandlers.add(handler);
	}
	
	/**
	 * Builds a new array of the given type if it is a base type or increments
	 * the dimensions of the given array by 1 otherwise.
	 * @param type The base type or incoming array
	 * @return The resulting array
	 */
	public Type buildArrayOrAddDimension(Type type) {
		if (type instanceof ArrayType) {
			ArrayType array = (ArrayType) type;
			return array.makeArrayType();
		}
		else
			return ArrayType.v(type, 1);
	}
	
	/**
	 * Checks whether the type of the given taint can be cast to the given
	 * target type
	 * @param accessPath The access path of the taint to be cast
	 * @param type The target type to which to cast the taint
	 * @return True if the cast is possible, otherwise false
	 */
	protected boolean checkCast(AccessPath accessPath, Type type) {
		if (accessPath.isStaticFieldRef())
			return canCastType(type, accessPath.getFirstFieldType());
		else
			return canCastType(type, accessPath.getBaseType());
	}
	
	/**
	 * Checks whether the given type is java.lang.Object, java.io.Serializable,
	 * or java.lang.Cloneable.
	 * @param tp The type to check
	 * @return True if the given type is one of the three "object-like" types,
	 * otherwise false
	 */
	protected boolean isObjectLikeType(Type tp) {
		if (!(tp instanceof RefType))
			return false;
		
		RefType rt = (RefType) tp;
		return rt.getSootClass().getName().equals("java.lang.Object")
				|| rt.getSootClass().getName().equals("java.io.Serializable")
				|| rt.getSootClass().getName().equals("java.lang.Cloneable");
	}
	
	@Override
	public Abstraction createZeroValue() {
		if (zeroValue == null)
			zeroValue = Abstraction.getZeroAbstraction(flowSensitiveAliasing);
		return zeroValue;
	}
	
	protected Abstraction getZeroValue() {
		return zeroValue;
	}
	
	/**
	 * Checks whether the given call is a call to Executor.execute() or
	 * AccessController.doPrivileged() and whether the callee matches
	 * the expected method signature
	 * @param ie The invocation expression to check
	 * @param dest The callee of the given invocation expression
	 * @return True if the given invocation expression and callee are a valid
	 * call to Executor.execute() or AccessController.doPrivileged()
	 */
	protected boolean isExecutorExecute(InvokeExpr ie, SootMethod dest) {
		if (ie == null || dest == null)
			return false;
		
		SootMethod ieMethod = ie.getMethod();
		if (!ieMethod.getName().equals("execute") && !ieMethod.getName().equals("doPrivileged"))
			return false;
		
		final String ieSubSig = ieMethod.getSubSignature();
		final String calleeSubSig = dest.getSubSignature();
		
		if (ieSubSig.equals("void execute(java.lang.Runnable)")
				&& calleeSubSig.equals("void run()"))
			return true;
		
		if (calleeSubSig.equals("java.lang.Object run()")) {
			if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedAction)"))
				return true;
			if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedAction,"
					+ "java.security.AccessControlContext)"))
				return true;
			if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction)"))
				return true;
			if (ieSubSig.equals("java.lang.Object doPrivileged(java.security.PrivilegedExceptionAction,"
					+ "java.security.AccessControlContext)"))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks whether the given type is a string
	 * @param tp The type of check
	 * @return True if the given type is a string, otherwise false
	 */
	protected boolean isStringType(Type tp) {
		if (!(tp instanceof RefType))
			return false;
		RefType refType = (RefType) tp;
		return refType.getClassName().equals("java.lang.String");
	}

}
