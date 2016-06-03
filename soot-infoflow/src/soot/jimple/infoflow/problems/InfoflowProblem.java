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

import heros.FlowFunction;
import heros.FlowFunctions;
import heros.TwoElementSet;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import org.k33nteam.JadeCfg;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.aliasing.Aliasing;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.aliasing.ImplicitFlowAliasStrategy;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.handlers.TaintPropagationHandler;
import soot.jimple.infoflow.handlers.TaintPropagationHandler.FlowFunctionType;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG.UnitContainer;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.source.DefaultSourceSinkManager;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.util.*;

public class InfoflowProblem extends AbstractInfoflowProblem {
	private final static boolean DEBUG = false;

	private final Aliasing aliasing;
	private final IAliasingStrategy aliasingStrategy;
	private final IAliasingStrategy implicitFlowAliasingStrategy;

    private final MyConcurrentHashMap<Unit, Set<Abstraction>> implicitTargets =
    		new MyConcurrentHashMap<Unit, Set<Abstraction>>();

	protected final MyConcurrentHashMap<AbstractionAtSink, Abstraction> results =
			new MyConcurrentHashMap<AbstractionAtSink, Abstraction>();

	public InfoflowProblem(ISourceSinkManager sourceSinkManager,
			IAliasingStrategy aliasingStrategy) {
		this(new InfoflowCFG(), sourceSinkManager, aliasingStrategy);
	}

	public InfoflowProblem(InfoflowCFG icfg, List<String> sourceList, List<String> sinkList,
			IAliasingStrategy aliasingStrategy) {
		this(icfg, new DefaultSourceSinkManager(sourceList, sinkList), aliasingStrategy);
	}

	public InfoflowProblem(ISourceSinkManager mySourceSinkManager, Set<Unit> analysisSeeds,
			IAliasingStrategy aliasingStrategy) {
	    this(new InfoflowCFG(), mySourceSinkManager, aliasingStrategy);
	    for (Unit u : analysisSeeds)
	    	this.initialSeeds.put(u, Collections.singleton(getZeroValue()));
    }

	public InfoflowProblem(IInfoflowCFG icfg, ISourceSinkManager sourceSinkManager,
			IAliasingStrategy aliasingStrategy) {
		super(icfg, sourceSinkManager);
		this.aliasingStrategy = aliasingStrategy;
		this.implicitFlowAliasingStrategy = new ImplicitFlowAliasStrategy(icfg);
		this.aliasing = new Aliasing(aliasingStrategy, icfg);
	}

	/**
	 * Computes the taints produced by a taint wrapper object
	 * @param d1 The context (abstraction at the method's start node)
	 * @param iStmt The call statement the taint wrapper shall check for well-
	 * known methods that introduce black-box taint propagation
	 * @param source The taint source
	 * @return The taints computed by the wrapper
	 */
	private Set<Abstraction> computeWrapperTaints
			(Abstraction d1,
			final Stmt iStmt,
			Abstraction source) {
		assert inspectSources || source != getZeroValue();
		
		// If we don't have a taint wrapper, there's nothing we can do here
		if(taintWrapper == null)
			return Collections.emptySet();
		
		// Do not check taints that are not mentioned anywhere in the call
		if (!source.getAccessPath().isStaticFieldRef()
				&& !source.getAccessPath().isEmpty()) {
			boolean found = false;

			// The base object must be tainted
			if (iStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iiExpr = (InstanceInvokeExpr) iStmt.getInvokeExpr();
				found = aliasing.mayAlias(iiExpr.getBase(), source.getAccessPath().getPlainValue());
			}

			// or one of the parameters must be tainted
			if (!found)
				for (int paramIdx = 0; paramIdx < iStmt.getInvokeExpr().getArgCount(); paramIdx++)
					if (aliasing.mayAlias(source.getAccessPath().getPlainValue(),
							iStmt.getInvokeExpr().getArg(paramIdx))) {
						found = true;
						break;
				}

			// If nothing is tainted, we don't have any taints to propagate
			if (!found)
				return Collections.emptySet();
		}
		
		Set<Abstraction> res = taintWrapper.getTaintsForMethod(iStmt, d1, source);
		if(res != null) {
			Set<Abstraction> resWithAliases = new HashSet<>(res);
			for (Abstraction abs : res) {
				// The new abstraction gets activated where it was generated
				if (!abs.equals(source)) {
					// If the taint wrapper creates a new taint, this must be propagated
					// backwards as there might be aliases for the base object
					// Note that we don't only need to check for heap writes such as a.x = y,
					// but also for base object taints ("a" in this case).
					final AccessPath val = abs.getAccessPath();
					boolean taintsObjectValue = val.getBaseType() instanceof RefType
							&& abs.getAccessPath().getBaseType() instanceof RefType
							&& !isStringType(val.getBaseType());
					boolean taintsStaticField = enableStaticFields
							&& abs.getAccessPath().isStaticFieldRef();
						
					// If the tainted value gets overwritten, it cannot have aliases afterwards
					boolean taintedValueOverwritten = (iStmt instanceof DefinitionStmt)
							? baseMatches(((DefinitionStmt) iStmt).getLeftOp(), abs) : false;
					
					if (!taintedValueOverwritten)
						if (taintsStaticField
								|| (taintsObjectValue && abs.getAccessPath().getTaintSubFields())
								|| triggerInaktiveTaintOrReverseFlow(iStmt, val.getPlainValue(), abs))
							computeAliasTaints(d1, iStmt, val.getPlainValue(), resWithAliases,
									interproceduralCFG().getMethodOf(iStmt), abs);
				}
			}
			res = resWithAliases;
		}
		
		return res;
	}

	/**
	 * Computes the taints for the aliases of a given tainted variable
	 * @param d1 The context in which the variable has been tainted
	 * @param src The statement that tainted the variable
	 * @param targetValue The target value which has been tainted
	 * @param taintSet The set to which all generated alias taints shall be
	 * added
	 * @param method The method containing src
	 * @param newAbs The newly generated abstraction for the variable taint
	 * @return The set of immediately available alias abstractions. If no such
	 * abstractions exist, null is returned
	 */
	private void computeAliasTaints
			(final Abstraction d1, final Stmt src,
			final Value targetValue, Set<Abstraction> taintSet,
			SootMethod method, Abstraction newAbs) {
		// If we are not in a conditionally-called method, we run the
		// full alias analysis algorithm. Otherwise, we use a global
		// non-flow-sensitive approximation.
		if (!d1.getAccessPath().isEmpty()) {
			aliasingStrategy.computeAliasTaints(d1,
					src, targetValue, taintSet, method, newAbs);
		}
		else if (targetValue instanceof InstanceFieldRef) {
			assert enableImplicitFlows;
			implicitFlowAliasingStrategy.computeAliasTaints(d1, src,
					targetValue, taintSet, method, newAbs);
		}
	}

	/**
	 * we cannot rely just on "real" heap objects, but must also inspect locals because of Jimple's representation ($r0 =... )
	 * @param val the value which gets tainted
	 * @param source the source from which the taints comes from. Important if not the value, but a field is tainted
	 * @return true if a reverseFlow should be triggered or an inactive taint should be propagated (= resulting object is stored in heap = alias)
	 */
	private boolean triggerInaktiveTaintOrReverseFlow(Stmt stmt, Value val, Abstraction source){
		if (stmt instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			// If the left side is overwritten completely, we do not need to
			// look for aliases. This also covers strings.
			if (defStmt.getLeftOp() instanceof Local
					&& defStmt.getLeftOp() == source.getAccessPath().getPlainValue())
				return false;

			// Arrays are heap objects
			if (val instanceof ArrayRef)
				return true;
			if (val instanceof FieldRef)
				return true;
		}

		// Primitive types or constants do not have aliases
		if (val.getType() instanceof PrimType)
			return false;
		if (val instanceof Constant)
			return false;
		
		// String cannot have aliases
		if (isStringType(val.getType()))
			return false;
		
		return val instanceof FieldRef
				|| (val instanceof Local && ((Local)val).getType() instanceof ArrayType);
	}

	@Override
	public FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Abstraction, SootMethod>() {

			/**
			 * Abstract base class for all normal flow functions. This is to
			 * share code that e.g. notifies the taint handlers between the
			 * various functions.
			 *
			 * @author Steven Arzt
			 */
			abstract class NotifyingNormalFlowFunction extends SolverNormalFlowFunction {

				private final Stmt stmt;

				public NotifyingNormalFlowFunction(Stmt stmt) {
					this.stmt = stmt;
				}

				@Override
				public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
					if (stopAfterFirstFlow && !results.isEmpty())
						return Collections.emptySet();

					// Notify the handler if we have one
					if (taintPropagationHandlers != null)
						for (TaintPropagationHandler tp : taintPropagationHandlers)
							tp.notifyFlowIn(stmt, source, interproceduralCFG(),
									FlowFunctionType.NormalFlowFunction);

					// Compute the new abstractions
					Set<Abstraction> res = computeTargetsInternal(d1, source);
					return notifyOutFlowHandlers(stmt, d1, source, res,
							FlowFunctionType.NormalFlowFunction);
				}

				public abstract Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source);

			}

			/**
			 * Notifies the outbound flow handlers, if any, about the computed
			 * result abstractions for the current flow function
			 * @param d1 The abstraction at the beginning of the method
			 * @param stmt The statement that has just been processed
			 * @param incoming The incoming abstraction from which the outbound
			 * ones were computed
			 * @param outgoing The outbound abstractions to be propagated on
			 * @param functionType The type of flow function that was computed
			 * @return The outbound flow abstracions, potentially changed by the
			 * flow handlers
			 */
			private Set<Abstraction> notifyOutFlowHandlers(Unit stmt,
					Abstraction d1,
					Abstraction incoming,
					Set<Abstraction> outgoing,
					FlowFunctionType functionType) {
				if (taintPropagationHandlers != null
						&& outgoing != null
						&& !outgoing.isEmpty())
					for (TaintPropagationHandler tp : taintPropagationHandlers)
						outgoing = tp.notifyFlowOut(stmt, d1, incoming, outgoing,
								interproceduralCFG(), functionType);
				return outgoing;
			}

			/**
			 * Taints the left side of the given assignment
			 * @param assignStmt The source statement from which the taint originated
			 * @param targetVal The target value that shall now be tainted
			 * @param source The incoming taint abstraction from the source
			 * @param taintSet The taint set to which to add all newly produced
			 * taints
			 */
			private void addTaintViaStmt
					(final Abstraction d1,
					final AssignStmt assignStmt,
					Abstraction source,
					Set<Abstraction> taintSet,
					boolean cutFirstField,
					SootMethod method,
					Type targetType) {
				final Value leftValue = assignStmt.getLeftOp();
				final Value rightValue = assignStmt.getRightOp();

				// Do not taint static fields unless the option is enabled
				if (!enableStaticFields && leftValue instanceof StaticFieldRef)
					return;

				Abstraction newAbs = null;
				if (!source.getAccessPath().isEmpty()) {
					// Special handling for array (de)construction
					if (leftValue instanceof ArrayRef && targetType != null)
						targetType = buildArrayOrAddDimension(targetType);
					else if (assignStmt.getRightOp() instanceof ArrayRef && targetType != null)
						targetType = ((ArrayType) targetType).getElementType();

					// If this is an unrealizable typecast, drop the abstraction
					if (rightValue instanceof CastExpr) {
						// If we cast java.lang.Object to an array type,
						// we must update our typing information
						CastExpr cast = (CastExpr) assignStmt.getRightOp();
						if (cast.getType() instanceof ArrayType && !(targetType instanceof ArrayType)) {
							assert canCastType(targetType, cast.getType());

							// If the cast was realizable, we can assume that we had the
							// type to which we cast.
							targetType = cast.getType();
						}
					}
					// Special type handling for certain operations
					else if (rightValue instanceof InstanceOfExpr)
						newAbs = source.deriveNewAbstraction(new AccessPath(leftValue, null,
								BooleanType.v(), (Type[]) null, true), assignStmt);
				}
				else
					// For implicit taints, we have no type information
					assert targetType == null;

				// also taint the target of the assignment
				if (newAbs == null)
					if (source.getAccessPath().isEmpty())
						newAbs = source.deriveNewAbstraction(new AccessPath(leftValue, true), assignStmt, true);
					else
						newAbs = source.deriveNewAbstraction(leftValue, cutFirstField, assignStmt, targetType);
				taintSet.add(newAbs);

				if (triggerInaktiveTaintOrReverseFlow(assignStmt, leftValue, newAbs))
					computeAliasTaints(d1, assignStmt, leftValue, taintSet,
							method, newAbs);
			}

			/**
			 * Checks whether the given call has at least one valid target,
			 * i.e. a callee with a body.
			 * @param call The call site to check
			 * @return True if there is at least one callee implementation
			 * for the given call, otherwise false
			 */
			private boolean hasValidCallees(Unit call) {
				Collection<SootMethod> callees = interproceduralCFG().getCalleesOfCallAt(call);
				for (SootMethod callee : callees)
					if (callee.isConcrete())
						return true;
				return false;
			}

			@Override
			public FlowFunction<Abstraction> getNormalFlowFunction(final Unit src, final Unit dest) {
				// Get the call site
				if (!(src instanceof Stmt))
					return KillAll.v();
				final Stmt stmt = (Stmt) src;

				final SourceInfo sourceInfo = sourceSinkManager != null
						? sourceSinkManager.getSourceInfo(stmt, interproceduralCFG()) : null;
				
				// If we compute flows on parameters, we create the initial
				// flow fact here
				if (src instanceof IdentityStmt) {
					final IdentityStmt is = (IdentityStmt) src;

					return new NotifyingNormalFlowFunction(is) {

						@Override
						public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
							if(DEBUG){
							synchronized (InfoflowProblem.this) {
								System.out.println("identity " + src);
								System.out.println("current abstraction: d1 " + d1 + " source: " + source);
								System.out.println("source stmt: " + source.getCurrentStmt());
								System.out.println("in method: " + interproceduralCFG().getMethodOf(src));
							}}
							// Check whether we must leave a conditional branch
							if (source.isTopPostdominator(is)) {
								source = source.dropTopPostdominator();
								// Have we dropped the last postdominator for an empty taint?
								if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
									return Collections.emptySet();
							}

							// This may also be a parameter access we regard as a source
							Set<Abstraction> res = new HashSet<Abstraction>();
							if (source == getZeroValue()
									&& sourceInfo != null
									&& !sourceInfo.getAccessPaths().isEmpty()) {
								for (AccessPath ap : sourceInfo.getAccessPaths()) {
									Abstraction abs = new Abstraction(ap,
											is,
											sourceInfo.getUserData(),
											false,
											false);
									res.add(abs);
									
									// Compute the aliases
									if (triggerInaktiveTaintOrReverseFlow(is, is.getLeftOp(), abs))
										computeAliasTaints(d1, is, is.getLeftOp(),
												res, interproceduralCFG().getMethodOf(is), abs);
								}
								return res;
							}

							boolean addOriginal = true;
							//FLANKER FIX: doesn't care about exception now
							if (is.getRightOp() instanceof CaughtExceptionRef) {
								if (source.getExceptionThrown()) {
									res.add(source.deriveNewAbstractionOnCatch(is.getLeftOp()));
									addOriginal = false;
								}
							}

							if (addOriginal)
								if (source != getZeroValue())
									res.add(source);
							return res;
						}
					};

				}

				// taint is propagated with assignStmt
				else if (src instanceof AssignStmt) {
					final AssignStmt assignStmt = (AssignStmt) src;
					final Value right = assignStmt.getRightOp();

					final Value leftValue = assignStmt.getLeftOp();
					final Value[] rightVals = BaseSelector.selectBaseList(right, true);
										
					return new NotifyingNormalFlowFunction(assignStmt) {

						@Override
						public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
							if(DEBUG){
							synchronized (InfoflowProblem.this) {
								System.out.println("assignment " + src);
								System.out.println("current abstraction: d1 " + d1 + " source: " + source);
								System.out.println("source stmt: " + source.getCurrentStmt());
								System.out.println("in method: " + interproceduralCFG().getMethodOf(src));
							}}
							// Make sure nothing all wonky is going on here
							assert source.getAccessPath().isEmpty()
									|| source.getTopPostdominator() == null;
							assert source.getTopPostdominator() == null
									|| interproceduralCFG().getMethodOf(src) == source.getTopPostdominator().getMethod()
									|| interproceduralCFG().getMethodOf(src).getActiveBody().getUnits().contains
											(source.getTopPostdominator().getUnit());

							// Fields can be sources in some cases
                            if (source == getZeroValue()
                            		&& sourceInfo != null
                            		&& !sourceInfo.getAccessPaths().isEmpty()) {
                            	Set<Abstraction> res = new HashSet<Abstraction>();
                            	for (AccessPath ap : sourceInfo.getAccessPaths()) {
	                                final Abstraction abs = new Abstraction(ap,
	                                		assignStmt,
	                                		sourceInfo.getUserData(),
	                                		false,
	                                		false);
	                                res.add(abs);
	                                
	                                // Compute the aliases
									if (triggerInaktiveTaintOrReverseFlow(assignStmt, leftValue, abs))
										computeAliasTaints(d1, assignStmt, leftValue, res,
												interproceduralCFG().getMethodOf(assignStmt), abs);
                            	}
                                return res;
                            }

                            // on NormalFlow taint cannot be created
							if (source == getZeroValue())
								return Collections.emptySet();

							// Check whether we must leave a conditional branch
							if (source.isTopPostdominator(assignStmt)) {
								source = source.dropTopPostdominator();
								// Have we dropped the last postdominator for an empty taint?
								if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
									return Collections.emptySet();
							}

							// Check whether we must activate a taint
							final Abstraction newSource;
							if (!source.isAbstractionActive() && src == source.getActivationUnit())
								newSource = source.getActiveCopy();
							else
								newSource = source;

							// Create the new taints that may be created by this assignment
							Set<Abstraction> res = createNewTaintOnAssignment(src, assignStmt,
									rightVals, d1, newSource);
							if (res != null)
								return res;

							// If we have propagated taint, we have returned from this method by now

							//if leftvalue contains the tainted value -> it is overwritten - remove taint:
							//but not for arrayRefs:
							// x[i] = y --> taint is preserved since we do not distinguish between elements of collections
							//because we do not use a MUST-Alias analysis, we cannot delete aliases of taints
							if (assignStmt.getLeftOp() instanceof ArrayRef)
								return Collections.singleton(newSource);

							if(newSource.getAccessPath().isInstanceFieldRef()) {
								// Data Propagation: x.f = y && x.f tainted --> no taint propagated
								// Alias Propagation: Only kill the alias if we directly overwrite it,
								// otherwise it might just be the creation of yet another alias
								if (leftValue instanceof InstanceFieldRef) {
									InstanceFieldRef leftRef = (InstanceFieldRef) leftValue;
									boolean baseAliases = source.isAbstractionActive()
											&& aliasing.mustAlias((Local) leftRef.getBase(),
													newSource.getAccessPath().getPlainValue(), assignStmt);
									if (baseAliases
											|| leftRef.getBase() == newSource.getAccessPath().getPlainValue()) {
										if (aliasing.mustAlias(leftRef.getField(), newSource.getAccessPath().getFirstField())) {
											return Collections.emptySet();
										}
									}
								}
								// x = y && x.f tainted -> no taint propagated. This must only check the precise
								// variable which gets replaced, but not any potential strong aliases
								else if (leftValue instanceof Local){
									if (leftValue == newSource.getAccessPath().getPlainValue()) {
										return Collections.emptySet();
									}
								}
							}
							//X.f = y && X.f tainted -> no taint propagated. Kills are allowed even if
							// static field tracking is disabled
							else if (newSource.getAccessPath().isStaticFieldRef()){
								if(leftValue instanceof StaticFieldRef
										&& aliasing.mustAlias(((StaticFieldRef)leftValue).getField(),
												newSource.getAccessPath().getFirstField())){
									return Collections.emptySet();
								}

							}
							//when the fields of an object are tainted, but the base object is overwritten
							// then the fields should not be tainted any more
							//x = y && x.f tainted -> no taint propagated
							else if (newSource.getAccessPath().isLocal()
									&& leftValue instanceof Local
									&& leftValue == newSource.getAccessPath().getPlainValue()){
								return Collections.emptySet();
							}

							//nothing applies: z = y && x tainted -> taint is preserved
							return Collections.singleton(newSource);
						}

						private Set<Abstraction> createNewTaintOnAssignment(final Unit src,
								final AssignStmt assignStmt,
								final Value[] rightVals,
								Abstraction d1,
								final Abstraction newSource) {
							final Value leftValue = assignStmt.getLeftOp();
							final Value rightValue = assignStmt.getRightOp();
							boolean addLeftValue = false;
							
							// If we have an implicit flow, but the assigned
							// local is never read outside the condition, we do
							// not need to taint it.
							boolean implicitTaint = newSource.getTopPostdominator() != null
									&& newSource.getTopPostdominator().getUnit() != null;
							implicitTaint |= newSource.getAccessPath().isEmpty();
							
							// If we have a non-empty postdominator stack, we taint
							// every assignment target
							if (implicitTaint) {
								assert enableImplicitFlows;

								// We can skip over all local assignments inside conditionally-
								// called functions since they are not visible in the caller
								// anyway
								if ((d1 == null || d1.getAccessPath().isEmpty())
										&& !(leftValue instanceof FieldRef))
									return Collections.singleton(newSource);
																
								if (newSource.getAccessPath().isEmpty())
									addLeftValue = true;
							}

							// If we have a = x with the taint "x" being inactive,
							// we must not taint the left side. We can only taint
							// the left side if the tainted value is some "x.y".
							boolean aliasOverwritten = !addLeftValue
									&& !newSource.isAbstractionActive()
									&& baseMatchesStrict(rightValue, newSource)
									&& rightValue.getType() instanceof RefType
									&& !newSource.dependsOnCutAP();

							boolean cutFirstField = false;
							AccessPath mappedAP = newSource.getAccessPath();
							Type targetType = null;
							if (!addLeftValue && !aliasOverwritten) {
								for (Value rightVal : rightVals) {
									if (rightValue instanceof FieldRef) {
										// Get the field reference
										FieldRef rightRef = (FieldRef) rightVal;

										// If the right side references a NULL field, we kill the taint
										if (rightRef instanceof InstanceFieldRef
												&& ((InstanceFieldRef) rightRef).getBase().getType() instanceof NullType)
											return null;

										// Check for aliasing
										mappedAP = aliasing.mayAlias(newSource.getAccessPath(), rightRef);
										
										// check if static variable is tainted (same name, same class)
										//y = X.f && X.f tainted --> y, X.f tainted
										if (rightVal instanceof StaticFieldRef) {
											if (enableStaticFields && mappedAP != null) {
												addLeftValue = true;
												cutFirstField = true;
											}
										}
										// check for field references
										//y = x.f && x tainted --> y, x tainted
										//y = x.f && x.f tainted --> y, x tainted
										else if (rightVal instanceof InstanceFieldRef) {
											Local rightBase = (Local) ((InstanceFieldRef) rightRef).getBase();
											Local sourceBase = newSource.getAccessPath().getPlainValue();
											final SootField rightField = rightRef.getField();

											// We need to compare the access path on the right side
											// with the start of the given one
											//x.f tainted
											if (mappedAP != null) {
												addLeftValue = true;
												cutFirstField = (mappedAP.getFieldCount() > 0
														&& mappedAP.getFirstField() == rightField);
											}
											//x tainted but not x.f
											//FLANKER FIX TODO a better approach?
											//else if (aliasing.mayAlias(rightBase, sourceBase)
											//		&& newSource.getAccessPath().getFieldCount() == 0
											//		&& newSource.getAccessPath().getTaintSubFields()) {
											//	addLeftValue = true;
											//	targetType = rightField.getType();
											//}
										}
									}
									// indirect taint propagation:
									// if rightvalue is local and source is instancefield of this local:
									// y = x && x.f tainted --> y.f, x.f tainted
									// y.g = x && x.f tainted --> y.g.f, x.f tainted
									else if (rightValue instanceof Local && newSource.getAccessPath().isInstanceFieldRef()) {
										//FLANKER FIX currently won't track field
										//if you want performance, turn off this option
										if (JadeCfg.field_taint_propagate_enabled()) {
											Local base = newSource.getAccessPath().getPlainValue();
											if (aliasing.mayAlias(rightValue, base)) {
												addLeftValue = true;
												targetType = newSource.getAccessPath().getBaseType();
											}
										}
									}
									//y = x[i] && x tainted -> x, y tainted
									else if (rightVal instanceof ArrayRef) {
										Local rightBase = (Local) ((ArrayRef) rightVal).getBase();
										if (aliasing.mayAlias(rightBase, newSource.getAccessPath().getPlainValue())) {
											addLeftValue = true;

											targetType = newSource.getAccessPath().getBaseType();
											assert targetType instanceof ArrayType;
										}
									}
									// generic case, is true for Locals, ArrayRefs that are equal etc..
									//y = x && x tainted --> y, x tainted
									else if (aliasing.mayAlias(rightVal, newSource.getAccessPath().getPlainValue())) {
										addLeftValue = true;
										targetType = newSource.getAccessPath().getBaseType();
									}

									// One reason to taint the left side is enough
									if (addLeftValue)
										break;
								}
							}

							// If we have nothing to add, we quit
							if (!addLeftValue)
								return null;

							// If the right side is a typecast, it must be compatible,
							// or this path is not realizable
							if (rightValue instanceof CastExpr) {
								CastExpr ce = (CastExpr) rightValue;
								if (!checkCast(newSource.getAccessPath(), ce.getCastType()))
									return Collections.emptySet();
							}
							// Special handling for certain operations
							else if (rightValue instanceof LengthExpr) {
								assert newSource.getAccessPath().isEmpty()
										|| newSource.getAccessPath().getBaseType() instanceof ArrayType;
								assert leftValue instanceof Local;

								Abstraction lenAbs = newSource.deriveNewAbstraction(new AccessPath
										(leftValue, null, IntType.v(), (Type[]) null, true), assignStmt);
								return new TwoElementSet<Abstraction>(newSource, lenAbs);
							}
							
							if (mappedAP == null)
								for (Value val : rightVals)
									aliasing.mayAlias(newSource.getAccessPath(), val);
							
							// If this is a sink, we need to report the finding
							if (sourceSinkManager != null
									&& sourceSinkManager.isSink(stmt, interproceduralCFG(),
											newSource.getAccessPath())
									&& newSource.isAbstractionActive()
									&& newSource.getAccessPath().isEmpty())
								addResult(new AbstractionAtSink(newSource, assignStmt));

							Set<Abstraction> res = new HashSet<Abstraction>();
							Abstraction targetAB = mappedAP.equals(newSource.getAccessPath())
									? newSource : newSource.deriveNewAbstraction(mappedAP, null);
							addTaintViaStmt(d1, assignStmt, targetAB, res, cutFirstField,
									interproceduralCFG().getMethodOf(src), targetType);
							res.add(newSource);
							return res;
						}
					};
				}
				// for unbalanced problems, return statements correspond to
				// normal flows, not return flows, because there is no return
				// site we could jump to
				else if (src instanceof ReturnStmt) {
					final ReturnStmt returnStmt = (ReturnStmt) src;
					return new NotifyingNormalFlowFunction(returnStmt) {

						@Override
						public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
							if(DEBUG){
							synchronized (InfoflowProblem.this) {
								System.out.println("return " + src);
								System.out.println("current abstraction: d1 " + d1 + " source: " + source);
								System.out.println("source stmt: " + source.getCurrentStmt());
								System.out.println("in method: " + interproceduralCFG().getMethodOf(src));
							}}
							// Check whether we must leave a conditional branch
							if (source.isTopPostdominator(returnStmt)) {
								source = source.dropTopPostdominator();
								// Have we dropped the last postdominator for an empty taint?
								if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
									return Collections.emptySet();
							}

							// Check whether we have reached a sink
							if (sourceSinkManager != null
									&& source.isAbstractionActive()
									&& aliasing.mayAlias(returnStmt.getOp(), source.getAccessPath().getPlainValue())
									&& sourceSinkManager.isSink(returnStmt, interproceduralCFG(),
											source.getAccessPath()))
								addResult(new AbstractionAtSink(source, returnStmt));

							return Collections.singleton(source);
						}
					};
				}
				else if (enableExceptions && src instanceof ThrowStmt) {
					final ThrowStmt throwStmt = (ThrowStmt) src;
					return new NotifyingNormalFlowFunction(throwStmt) {

						@Override
						public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
							// Check whether we must leave a conditional branch
							if (source.isTopPostdominator(throwStmt)) {
								source = source.dropTopPostdominator();
								// Have we dropped the last postdominator for an empty taint?
								if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
									return Collections.emptySet();
							}

							if (aliasing.mayAlias(throwStmt.getOp(), source.getAccessPath().getPlainValue()))
								return Collections.singleton(source.deriveNewAbstractionOnThrow(throwStmt));

							return Collections.singleton(source);
						}
					};
				}
				// IF statements can lead to implicit flows or sinks
				else if (src instanceof IfStmt
						|| src instanceof LookupSwitchStmt
						|| src instanceof TableSwitchStmt) {
					final Value condition = src instanceof IfStmt ? ((IfStmt) src).getCondition()
							: src instanceof LookupSwitchStmt ? ((LookupSwitchStmt) src).getKey()
							: ((TableSwitchStmt) src).getKey();
					
					// If implicit flow tracking is not enabled, we only need to
					// check for sinks
					if (!enableImplicitFlows)
						return new NotifyingNormalFlowFunction(stmt) {
							
							@Override
							public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
								// Check for a sink
								if (source.isAbstractionActive()
										&& sourceSinkManager != null
										&& sourceSinkManager.isSink(stmt, interproceduralCFG(),
												source.getAccessPath()))
									for (Value v : BaseSelector.selectBaseList(condition, false))
										if (aliasing.mayAlias(v, source.getAccessPath().getPlainValue())) {
											addResult(new AbstractionAtSink(source, stmt));
											break;
										}
								
								return Collections.singleton(source);
							}
							
						};
					
					// Check for implicit flows
					return new NotifyingNormalFlowFunction(stmt) {

						@Override
						public Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
							if (!source.isAbstractionActive())
								return Collections.singleton(source);
							
							// Check for a sink
							if (sourceSinkManager != null
									&& sourceSinkManager.isSink(stmt, interproceduralCFG(),
											source.getAccessPath()))
								for (Value v : BaseSelector.selectBaseList(condition, false))
									if (aliasing.mayAlias(v, source.getAccessPath().getPlainValue())) {
										addResult(new AbstractionAtSink(source, stmt));
										break;
									}
							
							// Check whether we must leave a conditional branch
							if (source.isTopPostdominator(src)) {
								source = source.dropTopPostdominator();
								// Have we dropped the last postdominator for an empty taint?
								if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
									return Collections.emptySet();
							}

							// If we are in a conditionally-called method, there is no
							// need to care about further conditionals, since all
							// assignment targets will be tainted anyway
							if (source.getAccessPath().isEmpty())
								return Collections.singleton(source);

							Set<Abstraction> res = new HashSet<Abstraction>();
							res.add(source);

							Set<Value> values = new HashSet<Value>();
							if (condition instanceof Local)
								values.add(condition);
							else
								for (ValueBox box : condition.getUseBoxes())
									values.add(box.getValue());

							for (Value val : values)
								if (aliasing.mayAlias(val, source.getAccessPath().getPlainValue())) {
									// ok, we are now in a branch that depends on a secret value.
									// We now need the postdominator to know when we leave the
									// branch again.
									UnitContainer postdom = interproceduralCFG().getPostdominatorOf(src);
									if (!(postdom.getMethod() == null
											&& source.getTopPostdominator() != null
											&& interproceduralCFG().getMethodOf(postdom.getUnit()) == source.getTopPostdominator().getMethod())) {
										Abstraction newAbs = source.deriveConditionalAbstractionEnter(postdom, stmt);
										res.add(newAbs);
										break;
									}
								}

							return res;
						}
					};
				}
				return Identity.v();
			}

			@Override
			public FlowFunction<Abstraction> getCallFlowFunction(final Unit src, final SootMethod dest) {
                if (!dest.isConcrete()){
                    logger.debug("Call skipped because target has no body: {} -> {}", src, dest);
                    return KillAll.v();
                }

				final Stmt stmt = (Stmt) src;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr())
						? stmt.getInvokeExpr() : null;
				
				final Local[] paramLocals = dest.getActiveBody().getParameterLocals().toArray(
						new Local[0]);
				
				final SourceInfo sourceInfo = sourceSinkManager != null
						? sourceSinkManager.getSourceInfo(stmt, interproceduralCFG()) : null;
				final boolean isSink = sourceSinkManager != null
						? sourceSinkManager.isSink(stmt, interproceduralCFG(), null) : false;
				
				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();
				
				return new SolverCallFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if(DEBUG){
						synchronized (InfoflowProblem.this) {
							System.out.println("call " + src);
							System.out.println("current abstraction: d1 " + d1 + " source: " + source);
							System.out.println("source stmt: " + source.getCurrentStmt());
							System.out.println("in method: " + interproceduralCFG().getMethodOf(src));
						}}
						Set<Abstraction> res = computeTargetsInternal(d1, source);
						if (!res.isEmpty())
							for (Abstraction abs : res)
								aliasingStrategy.injectCallingContext(abs, solver, dest, src, source, d1);
						return notifyOutFlowHandlers(stmt, d1, source, res,
								FlowFunctionType.CallFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if (stopAfterFirstFlow && !results.isEmpty())
							return Collections.emptySet();

						//if we do not have to look into sources or sinks:
						if (!inspectSources && sourceInfo != null)
							return Collections.emptySet();
						if (!inspectSinks && isSink)
							return Collections.emptySet();
						if (source == getZeroValue()) {
							assert sourceInfo != null;
							return Collections.singleton(source);
						}

						// Notify the handler if we have one
						if (taintPropagationHandlers != null)
							for (TaintPropagationHandler tp : taintPropagationHandlers)
								tp.notifyFlowIn(stmt, source, interproceduralCFG(),
										FlowFunctionType.CallFlowFunction);

						// If we have an exclusive taint wrapper for the target
						// method, we do not perform an own taint propagation. 
						if(taintWrapper != null && taintWrapper.isExclusive(stmt, source)) {
							//taint is propagated in CallToReturnFunction, so we do not need any taint here:
							return Collections.emptySet();
						}

						// Check whether we must leave a conditional branch
						if (source.isTopPostdominator(stmt)) {
							source = source.dropTopPostdominator();
							// Have we dropped the last postdominator for an empty taint?
							if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
								return Collections.emptySet();
						}

						// If no parameter is tainted, but we are in a conditional, we create a
						// pseudo abstraction. We do not map parameters if we are handling an
						// implicit flow anyway.
						if (source.getAccessPath().isEmpty()) {
							assert enableImplicitFlows;

							// Block the call site for further explicit tracking
							if (d1 != null) {
								Set<Abstraction> callSites = implicitTargets.putIfAbsentElseGet
										(src, new ConcurrentHashSet<Abstraction>());
								callSites.add(d1);
							}

							Abstraction abs = source.deriveConditionalAbstractionCall(src);
							return Collections.singleton(abs);
						}
						else if (source.getTopPostdominator() != null)
							return Collections.emptySet();
						
						// If we have already tracked implicit flows through this method,
						// there is no point in tracking explicit ones afterwards as well.
						if (implicitTargets.containsKey(src) && (d1 == null || implicitTargets.get(src).contains(d1)))
							return Collections.emptySet();

						// Only propagate the taint if the target field is actually read
						if (source.getAccessPath().isStaticFieldRef()) {
							if (!enableStaticFields)
								return Collections.emptySet();
						}
						
						// Map the source access path into the callee
						Set<AccessPath> res = mapAccessPathToCallee(dest, ie, paramLocals,
								thisLocal, source.getAccessPath());
						if (res == null)
							return Collections.emptySet();
						
						// Translate the access paths into abstractions
						Set<Abstraction> resAbs = new HashSet<Abstraction>(res.size());
						for (AccessPath ap : res)
							if (ap.isStaticFieldRef()) {
								// Do not propagate static fields that are not read inside the callee 
								if (interproceduralCFG().isStaticFieldRead(dest, ap.getFirstField()))
									resAbs.add(source.deriveNewAbstraction(ap, stmt));
							}
							// If the variable is never read in the callee, there is no
							// need to propagate it through
							else if (source.isImplicit() || interproceduralCFG().methodReadsValue(dest, ap.getPlainValue()))
								resAbs.add(source.deriveNewAbstraction(ap, stmt));
						
						return resAbs;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getReturnFlowFunction(final Unit callSite,
					final SootMethod callee, final Unit exitStmt, final Unit retSite) {
				// Get the call site
				if (callSite != null && !(callSite instanceof Stmt))
					return KillAll.v();
				final Stmt iCallStmt = (Stmt) callSite;
				
				final ThrowStmt throwStmt = (exitStmt instanceof ThrowStmt) ? (ThrowStmt) exitStmt : null;
				
				final ReturnStmt returnStmt = (exitStmt instanceof ReturnStmt) ? (ReturnStmt) exitStmt : null;
				
				final Local[] paramLocals = callee.getActiveBody().getParameterLocals().toArray(
						new Local[0]);
				
				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();

				return new SolverReturnFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction source, Abstraction d1,
							Collection<Abstraction> callerD1s) {
						Set<Abstraction> res = computeTargetsInternal(source, callerD1s);
						return notifyOutFlowHandlers(exitStmt, d1, source, res,
								FlowFunctionType.ReturnFlowFunction);
					}
					
					private Set<Abstraction> computeTargetsInternal(Abstraction source,
							Collection<Abstraction> callerD1s) {
						if (stopAfterFirstFlow && !results.isEmpty())
							return Collections.emptySet();
						if (source == getZeroValue())
							return Collections.emptySet();

						// Notify the handler if we have one
						if (taintPropagationHandlers != null)
							for (TaintPropagationHandler tp : taintPropagationHandlers)
								tp.notifyFlowIn(exitStmt, source, interproceduralCFG(),
										FlowFunctionType.ReturnFlowFunction);

						boolean callerD1sConditional = false;
						for (Abstraction d1 : callerD1s)
							if (d1.getAccessPath().isEmpty()) {
								callerD1sConditional = true;
								break;
							}

						// Activate taint if necessary
						Abstraction newSource = source;
						if(!source.isAbstractionActive())
							if(callSite != null)
								if (callSite == source.getActivationUnit()
										|| isCallSiteActivatingTaint(callSite, source.getActivationUnit()))
									newSource = source.getActiveCopy();

						// Empty access paths are never propagated over return edges
						if (source.getAccessPath().isEmpty()) {
							// If we return a constant, we must taint it
							if (returnStmt != null && returnStmt.getOp() instanceof Constant)
								if (callSite instanceof DefinitionStmt) {
									DefinitionStmt def = (DefinitionStmt) callSite;
									Abstraction abs = newSource.deriveNewAbstraction
											(newSource.getAccessPath().copyWithNewValue(def.getLeftOp()), (Stmt) exitStmt);

									Set<Abstraction> res = new HashSet<Abstraction>();
									res.add(abs);

									// If we taint a return value because it is implicit,
									// we must trigger an alias analysis
									if(triggerInaktiveTaintOrReverseFlow(def, def.getLeftOp(), abs) && !callerD1sConditional)
										for (Abstraction d1 : callerD1s)
											computeAliasTaints(d1, iCallStmt, def.getLeftOp(), res,
													interproceduralCFG().getMethodOf(callSite), abs);
									return res;
								}

							// Kill the empty abstraction
							return Collections.emptySet();
						}

						// Are we still inside a conditional? We check this before we
						// leave the method since the return value is still assigned
						// inside the method.
						boolean insideConditional = newSource.getTopPostdominator() != null
								|| newSource.getAccessPath().isEmpty();

						// Check whether we must leave a conditional branch
						if (newSource.isTopPostdominator(exitStmt) || newSource.isTopPostdominator(callee)) {
							newSource = newSource.dropTopPostdominator();
							// Have we dropped the last postdominator for an empty taint?
							if (!insideConditional
									&& newSource.getAccessPath().isEmpty()
									&& newSource.getTopPostdominator() == null)
								return Collections.emptySet();
						}
												
						//if abstraction is not active and activeStmt was in this method, it will not get activated = it can be removed:
						if(!newSource.isAbstractionActive() && newSource.getActivationUnit() != null)
							if (interproceduralCFG().getMethodOf(newSource.getActivationUnit()) == callee)
								return Collections.emptySet();
						
						// Static field tracking can be disabled
						if (!enableStaticFields && newSource.getAccessPath().isStaticFieldRef())
							return Collections.emptySet();
												
						// Check whether this return is treated as a sink
						if (returnStmt != null) {
							assert returnStmt.getOp() == null
									|| returnStmt.getOp() instanceof Local
									|| returnStmt.getOp() instanceof Constant;
							
							boolean mustTaintSink = insideConditional;
							mustTaintSink |= returnStmt.getOp() != null
									&& newSource.getAccessPath().isLocal()
									&& aliasing.mayAlias(newSource.getAccessPath().getPlainValue(), returnStmt.getOp());
							if (mustTaintSink
									&& sourceSinkManager != null
									&& sourceSinkManager.isSink(returnStmt, interproceduralCFG(),
											newSource.getAccessPath())
									&& newSource.isAbstractionActive())
								addResult(new AbstractionAtSink(newSource, returnStmt));
						}

						// If we have no caller, we have nowhere to propagate. This
						// can happen when leaving the main method.
						if (callSite == null)
							return Collections.emptySet();
						
						// If we throw an exception with a tainted operand, we need to
						// handle this specially
						if (throwStmt != null)
							if (aliasing.mayAlias(throwStmt.getOp(), source.getAccessPath().getPlainValue()))
								return Collections.singleton(source.deriveNewAbstractionOnThrow(throwStmt));
						
						Set<Abstraction> res = new HashSet<Abstraction>();
						
						// if we have a returnStmt we have to look at the returned value:
						if (returnStmt != null && callSite instanceof DefinitionStmt) {
							Value retLocal = returnStmt.getOp();
							DefinitionStmt defnStmt = (DefinitionStmt) callSite;
							Value leftOp = defnStmt.getLeftOp();

							if ((insideConditional && leftOp instanceof FieldRef)
									|| aliasing.mayAlias(retLocal, newSource.getAccessPath().getPlainValue())) {
								Abstraction abs = newSource.deriveNewAbstraction
										(newSource.getAccessPath().copyWithNewValue(leftOp), (Stmt) exitStmt);
								res.add(abs);

								// Aliases of implicitly tainted variables must be mapped back
								// into the caller's context on return when we leave the last
								// implicitly-called method
								if ((abs.isImplicit()
										&& (abs.getAccessPath().isInstanceFieldRef() || abs.getAccessPath().isStaticFieldRef())
										&& !callerD1sConditional) || aliasingStrategy.requiresAnalysisOnReturn())
									for (Abstraction d1 : callerD1s)
										computeAliasTaints(d1, iCallStmt, leftOp, res,
												interproceduralCFG().getMethodOf(callSite), abs);
							}
						}

						// easy: static
						if (newSource.getAccessPath().isStaticFieldRef()) {
							// Simply pass on the taint
							Abstraction abs = newSource;
							res.add(abs);

							// Aliases of implicitly tainted variables must be mapped back
							// into the caller's context on return when we leave the last
							// implicitly-called method
							if ((abs.isImplicit() && !callerD1sConditional)
									 || aliasingStrategy.requiresAnalysisOnReturn())
								for (Abstraction d1 : callerD1s)
									computeAliasTaints(d1, iCallStmt, null, res,
											interproceduralCFG().getMethodOf(callSite), abs);
						}

						// checks: this/params/fields

						// check one of the call params are tainted (not if simple type)
						Value sourceBase = newSource.getAccessPath().getPlainValue();
						boolean parameterAliases = false;
						{
						Value originalCallArg = null;
						for (int i = 0; i < callee.getParameterCount(); i++) {
							// If this parameter is overwritten, we cannot propagate
							// the "old" taint over. Return value propagation must
							// always happen explicitly.
							if (callSite instanceof DefinitionStmt) {
								DefinitionStmt defnStmt = (DefinitionStmt) callSite;
								Value leftOp = defnStmt.getLeftOp();
								originalCallArg = defnStmt.getInvokeExpr().getArg(i);
								if (originalCallArg == leftOp)
									continue;
							}

							// Propagate over the parameter taint
							if (aliasing.mayAlias(paramLocals[i], sourceBase)) {
								parameterAliases = true;
								originalCallArg = iCallStmt.getInvokeExpr().getArg(i);

								// If this is a constant parameter, we can safely ignore it
								if (!AccessPath.canContainValue(originalCallArg))
									continue;
								if (!checkCast(source.getAccessPath(), originalCallArg.getType()))
									continue;
								
								// Primitive types and strings cannot have aliases and thus
								// never need to be propagated back
								if (source.getAccessPath().getBaseType() instanceof PrimType)
									continue;
								if (isStringType(source.getAccessPath().getBaseType()))
									continue;
								
								Abstraction abs = newSource.deriveNewAbstraction
										(newSource.getAccessPath().copyWithNewValue(originalCallArg), (Stmt) exitStmt);
								res.add(abs);

								// Aliases of implicitly tainted variables must be mapped back
								// into the caller's context on return when we leave the last
								// implicitly-called method
								if ((abs.isImplicit()
										&& implicitFlowAliasingStrategy.hasProcessedMethod(callee)
										&& !callerD1sConditional) || aliasingStrategy.requiresAnalysisOnReturn()) {
									assert originalCallArg.getType() instanceof ArrayType
											|| originalCallArg.getType() instanceof RefType;
									for (Abstraction d1 : callerD1s)
										computeAliasTaints(d1, iCallStmt, originalCallArg, res,
											interproceduralCFG().getMethodOf(callSite), abs);
								}
							}
						}
						}


						{
						if (!callee.isStatic()) {
							if (aliasing.mayAlias(thisLocal, sourceBase)) {
								// check if it is not one of the params (then we have already fixed it)
								if (!parameterAliases) {
									if (iCallStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
										InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) iCallStmt.getInvokeExpr();
										Abstraction abs = newSource.deriveNewAbstraction
												(newSource.getAccessPath().copyWithNewValue(iIExpr.getBase()), (Stmt) exitStmt);
										res.add(abs);

										// Aliases of implicitly tainted variables must be mapped back
										// into the caller's context on return when we leave the last
										// implicitly-called method
										if ((abs.isImplicit()
												&& triggerInaktiveTaintOrReverseFlow(iCallStmt, iIExpr.getBase(), abs)
												&& implicitFlowAliasingStrategy.hasProcessedMethod(callee)
												&& !callerD1sConditional) || aliasingStrategy.requiresAnalysisOnReturn())
											for (Abstraction d1 : callerD1s)
												computeAliasTaints(d1, iCallStmt, iIExpr.getBase(), res,
														interproceduralCFG().getMethodOf(callSite), abs);
									}
								}
							}
							}
						}

						for (Abstraction abs : res)
							if (abs != newSource)
								abs.setCorrespondingCallSite(iCallStmt);

						return res;
					}

				};
			}

			@Override
			public FlowFunction<Abstraction> getCallToReturnFlowFunction(final Unit call, final Unit returnSite) {
				// special treatment for native methods:
				if (!(call instanceof Stmt))
					return KillAll.v();

				final Stmt iCallStmt = (Stmt) call;
				final InvokeExpr invExpr = iCallStmt.getInvokeExpr();

				final Value[] callArgs = new Value[invExpr.getArgCount()];
				for (int i = 0; i < invExpr.getArgCount(); i++)
					callArgs[i] = invExpr.getArg(i);

				final SourceInfo sourceInfo = sourceSinkManager != null
						? sourceSinkManager.getSourceInfo(iCallStmt, interproceduralCFG()) : null;
				final boolean isSink = (sourceSinkManager != null)
						? sourceSinkManager.isSink(iCallStmt, interproceduralCFG(), null) : false;
				
				final SootMethod callee = invExpr.getMethod();
				final boolean hasValidCallees = hasValidCallees(call);
				
				return new SolverCallToReturnFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						Set<Abstraction> res = computeTargetsInternal(d1, source);
						return notifyOutFlowHandlers(call, d1, source, res,
								FlowFunctionType.CallToReturnFlowFunction);
					}

					private Set<Abstraction> computeTargetsInternal(Abstraction d1, Abstraction source) {
						if(DEBUG){
						synchronized (InfoflowProblem.this) {
							System.out.println("calltoreturn: call " + call + " returnSite: " + returnSite);
							System.out.println("current abstraction: d1 " + d1 + " source: " + source);
							System.out.println("source stmt: " + source.getCurrentStmt());
							System.out.println("in method: " + interproceduralCFG().getMethodOf(call));
						}}
						if (stopAfterFirstFlow && !results.isEmpty())
							return Collections.emptySet();

						// Notify the handler if we have one
						if (taintPropagationHandlers != null)
							for (TaintPropagationHandler tp : taintPropagationHandlers)
								tp.notifyFlowIn(call, source, interproceduralCFG(),
										FlowFunctionType.CallToReturnFlowFunction);

						// Check whether we must leave a conditional branch
						if (source.isTopPostdominator(call)) {
							source = source.dropTopPostdominator();
							// Have we dropped the last postdominator for an empty taint?
							if (source.getAccessPath().isEmpty() && source.getTopPostdominator() == null)
								return Collections.emptySet();
						}
						
						// Static field tracking can be disabled
						if (!enableStaticFields && source.getAccessPath().isStaticFieldRef())
							return Collections.emptySet();
						
						Set<Abstraction> res = new HashSet<Abstraction>();

						// Sources can either be assignments like x = getSecret() or
						// instance method calls like constructor invocations
						if (source == getZeroValue()
								&& sourceInfo != null
								&& !sourceInfo.getAccessPaths().isEmpty()) {
							for (AccessPath ap : sourceInfo.getAccessPaths()) {
								final Abstraction abs = new Abstraction(ap,
										iCallStmt,
										sourceInfo.getUserData(),
										false,
										false);
								res.add(abs);
								
								// Compute the aliases
								if (triggerInaktiveTaintOrReverseFlow(iCallStmt, ap.getPlainValue(), abs))
									computeAliasTaints(d1, iCallStmt, ap.getPlainValue(), res,
											interproceduralCFG().getMethodOf(call), abs);
								
								// Set the corresponding call site
								for (Abstraction absRet : res)
									if (absRet != source)
										absRet.setCorrespondingCallSite(iCallStmt);
							}
							return res;
						}
						
						//check inactive elements:
						final Abstraction newSource;
						if (!source.isAbstractionActive() && (call == source.getActivationUnit()
								|| isCallSiteActivatingTaint(call, source.getActivationUnit())))
							newSource = source.getActiveCopy();
						else
							newSource = source;

						// Compute the taint wrapper taints
						boolean passOn = true;
						Collection<Abstraction> wrapperTaints = computeWrapperTaints(d1, iCallStmt, newSource);
						if (wrapperTaints != null) {
							res.addAll(wrapperTaints);
							
							// If the taint wrapper generated an abstraction for
							// the incoming access path, we assume it to be handled
							// and do not pass on the incoming abstraction on our own
							for (Abstraction wrapperAbs : wrapperTaints)
								if (wrapperAbs.getAccessPath().equals(newSource.getAccessPath())) {
									passOn = false;
									break;
								}
						}
						
						// if we have called a sink we have to store the path from the source - in case one of the params is tainted!
						if (sourceSinkManager != null
								&& sourceSinkManager.isSink(iCallStmt, interproceduralCFG(),
										newSource.getAccessPath())) {
							// If we are inside a conditional branch, we consider every sink call a leak
							boolean conditionalCall = enableImplicitFlows 
									&& !interproceduralCFG().getMethodOf(call).isStatic()
									&& aliasing.mayAlias(interproceduralCFG().getMethodOf(call).getActiveBody().getThisLocal(),
											newSource.getAccessPath().getPlainValue())
									&& newSource.getAccessPath().getFirstField() == null;
							boolean taintedParam = (conditionalCall
										|| newSource.getTopPostdominator() != null
										|| newSource.getAccessPath().isEmpty())
									&& newSource.isAbstractionActive();
							
							// If the base object is tainted, we also consider the "code" associated
							// with the object's class as tainted.
							if (!taintedParam) {
								for (int i = 0; i < callArgs.length; i++) {
									if (aliasing.mayAlias(callArgs[i], newSource.getAccessPath().getPlainValue())) {
										taintedParam = true;
										break;
									}
								}
							}
							
							if (taintedParam && newSource.isAbstractionActive())
								addResult(new AbstractionAtSink(newSource, iCallStmt));
							
							// if the base object which executes the method is tainted the sink is reached, too.
							if (invExpr instanceof InstanceInvokeExpr) {
								InstanceInvokeExpr vie = (InstanceInvokeExpr) iCallStmt.getInvokeExpr();
								if (newSource.isAbstractionActive()
										&& aliasing.mayAlias(vie.getBase(), newSource.getAccessPath().getPlainValue()))
                                {
                            //FLANKER FIX: ignoring base for now
                            //		addResult(new AbstractionAtSink(newSource, iCallStmt));
                                }
							}
						}
						
						if (newSource.getTopPostdominator() != null
								&& newSource.getTopPostdominator().getUnit() == null)
							return Collections.singleton(newSource);
						
						// Implicit flows: taint return value
						if (call instanceof DefinitionStmt) {
							// If we have an implicit flow, but the assigned
							// local is never read outside the condition, we do
							// not need to taint it.
							boolean implicitTaint = newSource.getTopPostdominator() != null
									&& newSource.getTopPostdominator().getUnit() != null;							
							implicitTaint |= newSource.getAccessPath().isEmpty();
							
							if (implicitTaint) {
								Value leftVal = ((DefinitionStmt) call).getLeftOp();
								
								// We can skip over all local assignments inside conditionally-
								// called functions since they are not visible in the caller
								// anyway
								if ((d1 == null || d1.getAccessPath().isEmpty())
										&& !(leftVal instanceof FieldRef))
									return Collections.singleton(newSource);
								
								Abstraction abs = newSource.deriveNewAbstraction(new AccessPath(leftVal, true),
										iCallStmt);
								return new TwoElementSet<Abstraction>(newSource, abs);
							}
						}
						
						// If this call overwrites the left side, the taint is never passed on.
						if (passOn) {
							if (newSource.getAccessPath().isStaticFieldRef())
								passOn = false;
							else if (call instanceof DefinitionStmt
									&& aliasing.mayAlias(((DefinitionStmt) call).getLeftOp(),
											newSource.getAccessPath().getPlainValue()))
								passOn = false;
						}
						
						//we only can remove the taint if we step into the call/return edges
						//otherwise we will loose taint - see ArrayTests/arrayCopyTest
						if (passOn
								&& invExpr instanceof InstanceInvokeExpr
								&& newSource.getAccessPath().isInstanceFieldRef()
								&& (inspectSinks || !isSink)
								&& (hasValidCallees
									|| (taintWrapper != null && taintWrapper.isExclusive(
											iCallStmt, newSource)))) {
							// If one of the callers does not read the value, we must pass it on
							// in any case
							boolean allCalleesRead = true;
							outer : for (SootMethod callee : interproceduralCFG().getCalleesOfCallAt(call)) {
								if (callee.isConcrete() && callee.hasActiveBody()) {
									Set<AccessPath> calleeAPs = mapAccessPathToCallee(callee,
											invExpr, null, null, source.getAccessPath());
									if (calleeAPs != null)
										for (AccessPath ap : calleeAPs)
											if (!interproceduralCFG().methodReadsValue(callee, ap.getPlainValue())) {
												allCalleesRead = false;
												break outer;
											}
										}
							}
							
							if (allCalleesRead) {
								if (aliasing.mayAlias(((InstanceInvokeExpr) invExpr).getBase(),
										newSource.getAccessPath().getPlainValue())) {
									passOn = false;
								}
								if (passOn)
									for (int i = 0; i < callArgs.length; i++)
										if (aliasing.mayAlias(callArgs[i], newSource.getAccessPath().getPlainValue())) {
											passOn = false;
											break;
										}
								//static variables are always propagated if they are not overwritten. So if we have at least one call/return edge pair,
								//we can be sure that the value does not get "lost" if we do not pass it on:
								if(newSource.getAccessPath().isStaticFieldRef())
									passOn = false;
							}
						}
						
						// If the callee does not read the given value, we also need to pass it on
						// since we do not propagate it into the callee.
						if (source.getAccessPath().isStaticFieldRef()) {
							if (!interproceduralCFG().isStaticFieldUsed(callee,
									source.getAccessPath().getFirstField()))
								passOn = true;
						}
												
						// Implicit taints are always passed over conditionally called methods
						passOn |= source.getTopPostdominator() != null || source.getAccessPath().isEmpty();
						if (passOn)
							if (newSource != getZeroValue())
								res.add(newSource);

						if (callee.isNative())
							for (Value callVal : callArgs)
								if (callVal == newSource.getAccessPath().getPlainValue()) {
									// java uses call by value, but fields of complex objects can be changed (and tainted), so use this conservative approach:
									Set<Abstraction> nativeAbs = ncHandler.getTaintedValues(iCallStmt, newSource, callArgs);
									res.addAll(nativeAbs);

									// Compute the aliases
									for (Abstraction abs : nativeAbs)
										if (abs.getAccessPath().isStaticFieldRef()
												|| triggerInaktiveTaintOrReverseFlow(iCallStmt,
														abs.getAccessPath().getPlainValue(), abs))
											computeAliasTaints(d1, iCallStmt,
													abs.getAccessPath().getPlainValue(), res,
													interproceduralCFG().getMethodOf(call), abs);

									// We only call the native code handler once per statement
									break;
								}
						
						for (Abstraction abs : res)
							if (abs != newSource)
								abs.setCorrespondingCallSite(iCallStmt);

						return res;
					}
				};
			}
			
			/**
			 * Maps the given access path into the scope of the callee
			 * @param callee The method that is being called
			 * @param ie The invocation expression for the call
			 * @param paramLocals The list of parameter locals in the callee
			 * @param thisLocal The "this" local in the callee
			 * @param ap The caller-side access path to map
			 * @return The set of callee-side access paths corresponding to the
			 * given caller-side access path
			 */
			private Set<AccessPath> mapAccessPathToCallee(final SootMethod callee, final InvokeExpr ie,
					Value[] paramLocals, Local thisLocal, AccessPath ap) {
				// We do not transfer empty access paths
				if (ap.isEmpty())
					return Collections.emptySet();
				
				// Android executor methods are handled specially. getSubSignature()
				// is slow, so we try to avoid it whenever we can
				final boolean isExecutorExecute = isExecutorExecute(ie, callee);
				
				Set<AccessPath> res = null;
				
				// check if whole object is tainted (happens with strings, for example:)
				if (!isExecutorExecute
						&& !ap.isStaticFieldRef()
						&& !callee.isStatic()) {
					assert ie instanceof InstanceInvokeExpr;
					InstanceInvokeExpr vie = (InstanceInvokeExpr) ie;
					// this might be enough because every call must happen with a local variable which is tainted itself:
					if (aliasing.mayAlias(vie.getBase(), ap.getPlainValue()))
						if (hasCompatibleTypesForCall(ap, callee.getDeclaringClass())) {
							if (res == null) res = new HashSet<AccessPath>();
							
							// Get the "this" local if we don't have it yet
							if (thisLocal == null)
								thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();
							
							res.add(ap.copyWithNewValue(thisLocal));
						}
				}
				// staticfieldRefs must be analyzed even if they are not part of the params:
				else if (ap.isStaticFieldRef()) {
					if (res == null) res = new HashSet<AccessPath>();
					res.add(ap);
				}
				
				//special treatment for clinit methods - no param mapping possible
				if (isExecutorExecute) {
					if (aliasing.mayAlias(ie.getArg(0), ap.getPlainValue())) {
						if (res == null) res = new HashSet<AccessPath>();
						res.add(ap.copyWithNewValue(callee.getActiveBody().getThisLocal()));
					}
				}
				else if (callee.getParameterCount() > 0) {
					assert callee.getParameterCount() == ie.getArgCount();
					// check if param is tainted:
					for (int i = 0; i < ie.getArgCount(); i++) {
						if (aliasing.mayAlias(ie.getArg(i), ap.getPlainValue())) {
							if (res == null) res = new HashSet<AccessPath>();							
							
							// Get the parameter locals if we don't have them yet
							if (paramLocals == null)
								paramLocals = callee.getActiveBody().getParameterLocals().toArray(
										new Local[callee.getParameterCount()]);
							
							res.add(ap.copyWithNewValue(paramLocals[i]));
						}
					}
				}
				return res;
			}
		};
	}

	@Override
	public boolean autoAddZero() {
		return false;
	}

	/**
	 * Adds a new result of the data flow analysis to the collection
	 * @param resultAbs The abstraction at the sink instruction
	 */
	private void addResult(AbstractionAtSink resultAbs) {
		// Check whether we need to filter a result in a system package
		if (ignoreFlowsInSystemPackages && SystemClassHandler.isClassInSystemPackage
				(interproceduralCFG().getMethodOf(resultAbs.getSinkStmt()).getDeclaringClass().getName()))
			return;

		// Make sure that the sink statement also appears inside the
		// abstraction
		resultAbs = new AbstractionAtSink
				(resultAbs.getAbstraction().deriveNewAbstraction
						(resultAbs.getAbstraction().getAccessPath(), resultAbs.getSinkStmt()),
				resultAbs.getSinkStmt());
		resultAbs.getAbstraction().setCorrespondingCallSite(resultAbs.getSinkStmt());

		Abstraction newAbs = this.results.putIfAbsentElseGet
				(resultAbs, resultAbs.getAbstraction());
		if (newAbs != resultAbs.getAbstraction())
			newAbs.addNeighbor(resultAbs.getAbstraction());
	}

	/**
	 * Gets the results of the data flow analysis
	 */
    public Set<AbstractionAtSink> getResults(){
   		return this.results.keySet();
	}

}

