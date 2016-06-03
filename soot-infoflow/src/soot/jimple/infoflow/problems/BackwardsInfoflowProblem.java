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
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;
import heros.solver.PathEdge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import soot.ArrayType;
import soot.BooleanType;
import soot.IntType;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.MutableTwoElementSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.functions.SolverCallFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverCallToReturnFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverNormalFlowFunction;
import soot.jimple.infoflow.solver.functions.SolverReturnFlowFunction;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.BaseSelector;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * class which contains the flow functions for the backwards solver. This is
 * required for on-demand alias analysis.
 */
public class BackwardsInfoflowProblem extends AbstractInfoflowProblem {
	private IInfoflowSolver fSolver;
	
	public void setTaintWrapper(ITaintPropagationWrapper wrapper) {
		taintWrapper = wrapper;
	}
	
	public BackwardsInfoflowProblem(BiDiInterproceduralCFG<Unit, SootMethod> icfg,
			ISourceSinkManager sourceSinkManager) {
		super(icfg, sourceSinkManager);
	}

	public void setForwardSolver(IInfoflowSolver forwardSolver) {
		fSolver = forwardSolver;
	}
	
	@Override
	public FlowFunctions<Unit, Abstraction, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Abstraction, SootMethod>() {

			/**
			 * Computes the aliases for the given statement
			 * @param def The definition statement from which to extract
			 * the alias information
			 * @param leftValue The left side of def. Passed in to allow for
			 * caching, no need to recompute this for every abstraction being
			 * processed.
			 * @param d1 The abstraction at the method's start node
			 * @param source The source abstraction of the alias search
			 * from before the current statement
			 * @return The set of abstractions after the current statement
			 */
			private Set<Abstraction> computeAliases
					(final DefinitionStmt defStmt,
					Value leftValue,
					Abstraction d1,
					Abstraction source) {
				assert !source.getAccessPath().isEmpty();
								
				// A backward analysis looks for aliases of existing taints and thus
				// cannot create new taints out of thin air
				if (source == getZeroValue())
					return Collections.emptySet();
				
				final Set<Abstraction> res = new MutableTwoElementSet<Abstraction>();
				
				// Check whether the left side of the assignment matches our
				// current taint abstraction
				final boolean leftSideMatches = baseMatches(leftValue, source);
				if (!leftSideMatches)
					res.add(source);
				else {
					// The left side is overwritten completely
					
					// If we have an assignment to the base local of the current taint,
					// all taint propagations must be below that point, so this is the
					// right point to turn around.
					for (Unit u : interproceduralCFG().getPredsOf(defStmt))
						fSolver.processEdge(new PathEdge<Unit, Abstraction>(d1, u, source));
				}
				
				if (defStmt instanceof AssignStmt) {
					// Get the right side of the assignment
					final Value rightValue = BaseSelector.selectBase(defStmt.getRightOp(), false);
	
					// Is the left side overwritten completely?
					if (leftSideMatches) {
						// Termination shortcut: If the right side is a value we do not track,
						// we can stop here.
						if (!(rightValue instanceof Local || rightValue instanceof FieldRef))
							return Collections.emptySet();
					}
					
					// If we assign a constant, there is no need to track the right side
					// any further or do any forward propagation since constants cannot
					// carry taint.
					if (rightValue instanceof Constant)
						return res;
	
					// We only process heap objects. Binary operations can only
					// be performed on primitive objects.
					if (rightValue instanceof BinopExpr)
						return res;
									
					// If we have a = x with the taint "x" being inactive,
					// we must not taint the left side. We can only taint
					// the left side if the tainted value is some "x.y".
					boolean aliasOverwritten = baseMatchesStrict(rightValue, source)
							&& rightValue.getType() instanceof RefType
							&& !source.dependsOnCutAP();
					
					if (!aliasOverwritten) {
						// If the tainted value 'b' is assigned to variable 'a' and 'b'
						// is a heap object, we must also look for aliases of 'a' upwards
						// from the current statement.
						Abstraction newLeftAbs = null;
						if (rightValue instanceof InstanceFieldRef) {
							InstanceFieldRef ref = (InstanceFieldRef) rightValue;
							if (source.getAccessPath().isInstanceFieldRef()
									&& ref.getBase() == source.getAccessPath().getPlainValue()
									&& source.getAccessPath().firstFieldMatches(ref.getField())) {
								newLeftAbs = source.deriveNewAbstraction(leftValue, true,
										defStmt, source.getAccessPath().getFirstFieldType());
							}
						}
						else if (enableStaticFields && rightValue instanceof StaticFieldRef) {
							StaticFieldRef ref = (StaticFieldRef) rightValue;
							if (source.getAccessPath().isStaticFieldRef()
									&& source.getAccessPath().firstFieldMatches(ref.getField())) {
								newLeftAbs = source.deriveNewAbstraction(leftValue, true,
										defStmt, source.getAccessPath().getBaseType());
							}
						}
						else if (rightValue == source.getAccessPath().getPlainValue()) {
							Type newType = source.getAccessPath().getBaseType();
							if (leftValue instanceof ArrayRef)
								newType = buildArrayOrAddDimension(newType);
							else if (defStmt.getRightOp() instanceof ArrayRef)
								newType = ((ArrayType) newType).getElementType();
							
							// If this is an unrealizable typecast, drop the abstraction
							if (defStmt.getRightOp() instanceof CastExpr) {
								CastExpr ce = (CastExpr) defStmt.getRightOp();
								if (!checkCast(source.getAccessPath(), ce.getCastType()))
									return Collections.emptySet();
								
								// If the cast was realizable, we can assume that we had the
								// type to which we cast. Do not loosen types, though.
								if (!Scene.v().getFastHierarchy().canStoreType(newType, ce.getCastType()))
									newType = ce.getCastType();
							}
							// Special type handling for certain operations
							else if (defStmt.getRightOp() instanceof LengthExpr) {
								assert source.getAccessPath().getBaseType() instanceof ArrayType;
								newLeftAbs = source.deriveNewAbstraction(new AccessPath(leftValue, null,
										IntType.v(), (Type[]) null, true), defStmt);
							}
							else if (defStmt.getRightOp() instanceof InstanceOfExpr)
								newLeftAbs = source.deriveNewAbstraction(new AccessPath(leftValue, null,
										BooleanType.v(), (Type[]) null, true), defStmt);
							
							if (newLeftAbs == null)
								newLeftAbs = source.deriveNewAbstraction(source.getAccessPath().copyWithNewValue
										(leftValue, newType, false), defStmt);
						}
						if (newLeftAbs != null) {
							res.add(newLeftAbs);
							
							// Inject the new alias into the forward solver
							for (Unit u : interproceduralCFG().getPredsOf(defStmt))
								fSolver.processEdge(new PathEdge<Unit, Abstraction>(d1, u, newLeftAbs));
						}
					}
					
					// If we have the tainted value on the left side of the assignment,
					// we also have to look or aliases of the value on the right side of
					// the assignment.
					if (rightValue instanceof Local
							|| rightValue instanceof FieldRef) {
						boolean addRightValue = false;
						boolean cutFirstField = false;
						Type targetType = null;
						
						// if both are fields, we have to compare their fieldName via equals and their bases via PTS
						if (leftValue instanceof InstanceFieldRef) {
							InstanceFieldRef leftRef = (InstanceFieldRef) leftValue;
							if (leftRef.getBase() == source.getAccessPath().getPlainValue()) {
								if (source.getAccessPath().isInstanceFieldRef()) {
									if (source.getAccessPath().firstFieldMatches(leftRef.getField())) {
										targetType = source.getAccessPath().getFirstFieldType();
										addRightValue = true;
										cutFirstField = true;
									}
								}
							}
							// indirect taint propagation:
							// if leftValue is local and source is instancefield of this local:
						} else if (leftValue instanceof Local && source.getAccessPath().isInstanceFieldRef()) {
							Local base = source.getAccessPath().getPlainValue(); // ?
							if (leftValue == base) {
								targetType = source.getAccessPath().getBaseType();
								addRightValue = true;
							}
						} else if (leftValue instanceof ArrayRef) {
							Local leftBase = (Local) ((ArrayRef) leftValue).getBase();
							if (leftBase == source.getAccessPath().getPlainValue()) {
								addRightValue = true;
								targetType = source.getAccessPath().getBaseType();
								assert source.getAccessPath().getBaseType() instanceof ArrayType;
							}
							// generic case, is true for Locals, ArrayRefs that are equal etc..
						} else if (leftValue == source.getAccessPath().getPlainValue()) {
							addRightValue = true;
							targetType = source.getAccessPath().getBaseType();
							
							// Check for unrealizable casts. If a = (O) b and a is tainted,
							// but incompatible to the type of b, this cast is impossible
							if (defStmt.getRightOp() instanceof CastExpr) {
								CastExpr ce = (CastExpr) defStmt.getRightOp();
								if (!checkCast(source.getAccessPath(), ce.getOp().getType()))
									return Collections.emptySet();
							}
						}
						
						// if one of them is true -> add rightValue
						if (addRightValue) {
							if (targetType != null) {
								// Special handling for some operations
								if (defStmt.getRightOp() instanceof ArrayRef)
									targetType = buildArrayOrAddDimension(targetType);
								else if (leftValue instanceof ArrayRef) {
									assert source.getAccessPath().getBaseType() instanceof ArrayType;
									targetType = ((ArrayType) targetType).getElementType();
									// If the types do not match, the right side cannot be an alias
									if (!canCastType(rightValue.getType(), targetType))
										addRightValue = false;
									else {
										// If we have a type of java.lang.Object, we try to tighten it
										if (isObjectLikeType(targetType))
											targetType = rightValue.getType();
									}
								}
							}
							
							// Special type handling for certain operations
							if (defStmt.getRightOp() instanceof LengthExpr)
								targetType = null;
							
							// We do not need to handle casts. Casts only make
							// types more imprecise when going backwards.

							// If the right side's type is not compatible with our current type,
							// this cannot be an alias
							if (addRightValue) {
								if (!canCastType(rightValue.getType(), targetType))
									addRightValue = false;
							}
							
							// Make sure to only track static fields if it has been enabled
							if (addRightValue)
								if (!enableStaticFields && rightValue instanceof StaticFieldRef)
									addRightValue = false;

							if (addRightValue) {
								Abstraction newAbs = source.deriveNewAbstraction(rightValue, cutFirstField,
										defStmt, targetType);
								res.add(newAbs);
								
								// Inject the new alias into the forward solver
								for (Unit u : interproceduralCFG().getPredsOf(defStmt))
									fSolver.processEdge(new PathEdge<Unit, Abstraction>(d1, u, newAbs));
							}
						}
					}
				}
				else if (defStmt instanceof IdentityStmt)
					res.add(source);
				
				return res;
			}
			
			@Override
			public FlowFunction<Abstraction> getNormalFlowFunction(final Unit src, final Unit dest) {
				

				if (src instanceof DefinitionStmt) {
					final DefinitionStmt defStmt = (DefinitionStmt) src;
					final Value leftValue = BaseSelector.selectBase(defStmt.getLeftOp(), true);
					
					final DefinitionStmt destDefStmt = dest instanceof DefinitionStmt
							? (DefinitionStmt) dest : null;
					final Value destLeftValue = destDefStmt == null ? null : BaseSelector.selectBase
							(destDefStmt.getLeftOp(), true);

					return new SolverNormalFlowFunction() {

						@Override
						public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
							if (source == getZeroValue())
								return Collections.emptySet();
							assert source.isAbstractionActive() || flowSensitiveAliasing;
														
							Set<Abstraction> res = computeAliases(defStmt, leftValue, d1, source);
							
							if (destDefStmt != null && interproceduralCFG().isExitStmt(destDefStmt))
								for (Abstraction abs : res)
									computeAliases(destDefStmt, destLeftValue, d1, abs);
							
							return res;
						}

					};
				}
				return Identity.v();
			}

			@Override
			public FlowFunction<Abstraction> getCallFlowFunction(final Unit src, final SootMethod dest) {
				if (!dest.isConcrete())
					return KillAll.v();
								
				final Stmt stmt = (Stmt) src;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr())
						? stmt.getInvokeExpr() : null;

				final Value[] paramLocals = new Value[dest.getParameterCount()]; 
				for (int i = 0; i < dest.getParameterCount(); i++)
					paramLocals[i] = dest.getActiveBody().getParameterLocal(i);
				
				final boolean isSource = sourceSinkManager != null
						? sourceSinkManager.getSourceInfo((Stmt) src, interproceduralCFG()) != null : false;
				final boolean isSink = sourceSinkManager != null
						? sourceSinkManager.isSink(stmt, interproceduralCFG(), null) : false;
				
				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = dest.isStatic() ? null : dest.getActiveBody().getThisLocal();	
				
				// Android executor methods are handled specially. getSubSignature()
				// is slow, so we try to avoid it whenever we can
				final boolean isExecutorExecute = isExecutorExecute(ie, dest);
				
				return new SolverCallFlowFunction() {

					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return Collections.emptySet();
						assert source.isAbstractionActive() || flowSensitiveAliasing;
						
						//if we do not have to look into sources or sinks:
						if (!inspectSources && isSource)
							return Collections.emptySet();
						if (!inspectSinks && isSink)
							return Collections.emptySet();
						
						// Do not propagate in inactive taints that will be
						// activated there since they already came out of the
						// callee
						if (isCallSiteActivatingTaint(stmt, source.getActivationUnit()))
							return Collections.emptySet();
						
						// taint is propagated in CallToReturnFunction, so we do not
						// need any taint here if the taint wrapper is exclusive:
						if(taintWrapper != null && taintWrapper.isExclusive(stmt, source))
							return Collections.emptySet();
						
						// Only propagate the taint if the target field is actually read
						if (enableStaticFields && source.getAccessPath().isStaticFieldRef())
							if (!interproceduralCFG().isStaticFieldRead(dest,
									source.getAccessPath().getFirstField()))
								return Collections.emptySet();
						
						Set<Abstraction> res = new HashSet<Abstraction>();
						
						// if the returned value is tainted - taint values from return statements
						if (src instanceof DefinitionStmt) {
							DefinitionStmt defnStmt = (DefinitionStmt) src;
							Value leftOp = defnStmt.getLeftOp();
							if (leftOp == source.getAccessPath().getPlainValue()) {
								// look for returnStmts:
								for (Unit u : dest.getActiveBody().getUnits()) {
									if (u instanceof ReturnStmt) {
										ReturnStmt rStmt = (ReturnStmt) u;
										if (rStmt.getOp() instanceof Local
												|| rStmt.getOp() instanceof FieldRef) {
											Abstraction abs = source.deriveNewAbstraction
													(source.getAccessPath().copyWithNewValue
															(rStmt.getOp(), null, false), (Stmt) src);
											res.add(abs);
										}
									}
								}
							}
						}

						// easy: static
						if (enableStaticFields && source.getAccessPath().isStaticFieldRef())
							res.add(source.deriveNewAbstraction(source.getAccessPath(), stmt));

						// checks: this/fields
						Value sourceBase = source.getAccessPath().getPlainValue();
						if (!isExecutorExecute
								&& !source.getAccessPath().isStaticFieldRef()
								&& !dest.isStatic()) {
							InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
							if (iIExpr.getBase() == sourceBase
									&& (hasCompatibleTypesForCall(source.getAccessPath(), dest.getDeclaringClass()))) {
								boolean param = false;
								// check if it is not one of the params (then we have already fixed it)
								for (int i = 0; i < dest.getParameterCount(); i++) {
									if (stmt.getInvokeExpr().getArg(i) == sourceBase) {
										param = true;
										break;
									}
								}
								if (!param) {
									Abstraction abs = source.deriveNewAbstraction
											(source.getAccessPath().copyWithNewValue(thisLocal), (Stmt) src);
									res.add(abs);
								}
							}
						}
						
						// Map the parameter values into the callee
						if (isExecutorExecute) {
							if (ie.getArg(0) == source.getAccessPath().getPlainValue()) {
								Abstraction abs = source.deriveNewAbstraction
										(source.getAccessPath().copyWithNewValue(thisLocal), stmt);
								res.add(abs);
							}
						}
						else if (ie != null && dest.getParameterCount() > 0) {
							assert dest.getParameterCount() == ie.getArgCount();
							// check if param is tainted:
							for (int i = 0; i < ie.getArgCount(); i++) {
								if (ie.getArg(i) == source.getAccessPath().getPlainValue()) {
									// Primitive types and strings cannot have aliases and thus
									// never need to be propagated back
									if (source.getAccessPath().getBaseType() instanceof PrimType)
										continue;
									if (isStringType(source.getAccessPath().getBaseType()))
										continue;
									
									Abstraction abs = source.deriveNewAbstraction(source.getAccessPath().copyWithNewValue
											(paramLocals[i]), stmt);
									res.add(abs);
								}
							}
						}
						
						return res;
					}
				};
			}

			@Override
			public FlowFunction<Abstraction> getReturnFlowFunction(final Unit callSite, final SootMethod callee,
					final Unit exitStmt, final Unit retSite) {
				final Value[] paramLocals = new Value[callee.getParameterCount()]; 
				for (int i = 0; i < callee.getParameterCount(); i++)
					paramLocals[i] = callee.getActiveBody().getParameterLocal(i);
				
				final Stmt stmt = (Stmt) callSite;
				final InvokeExpr ie = (stmt != null && stmt.containsInvokeExpr())
						? stmt.getInvokeExpr() : null;
				
				// This is not cached by Soot, so accesses are more expensive
				// than one might think
				final Local thisLocal = callee.isStatic() ? null : callee.getActiveBody().getThisLocal();	
				
				// Android executor methods are handled specially. getSubSignature()
				// is slow, so we try to avoid it whenever we can
				final boolean isExecutorExecute = isExecutorExecute(ie, callee);
				
				return new SolverReturnFlowFunction() {
					
					@Override
					public Set<Abstraction> computeTargets(Abstraction source,
							Abstraction d1,
							Collection<Abstraction> callerD1s) {
						if (source == getZeroValue())
							return Collections.emptySet();
						assert source.isAbstractionActive() || flowSensitiveAliasing;
						
						// If we have no caller, we have nowhere to propagate. This
						// can happen when leaving the main method.
						if (callSite == null)
							return Collections.emptySet();
						
						// easy: static
						if (enableStaticFields && source.getAccessPath().isStaticFieldRef()) {
							registerActivationCallSite(callSite, callee, source);
							return Collections.singleton(source);
						}

						final Value sourceBase = source.getAccessPath().getPlainValue();
						Set<Abstraction> res = new HashSet<Abstraction>();
						
						// Since we return from the top of the callee into the
						// caller, return values cannot be propagated here. They
						// don't yet exist at the beginning of the callee.
						
						if (isExecutorExecute) {
							// Map the "this" object to the first argument of the call site
							if (source.getAccessPath().getPlainValue() == thisLocal) {
								Abstraction abs = source.deriveNewAbstraction
										(source.getAccessPath().copyWithNewValue(ie.getArg(0)), (Stmt) exitStmt);
								res.add(abs);
								registerActivationCallSite(callSite, callee, abs);
							}
						}
						else {
							boolean parameterAliases = false;
							
							// check one of the call params are tainted (not if simple type)
							Value originalCallArg = null;
							for (int i = 0; i < paramLocals.length; i++) {
								if (paramLocals[i] == sourceBase) {
									parameterAliases = true;
									if (callSite instanceof Stmt) {
										originalCallArg = ie.getArg(i);
										
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
										
										Abstraction abs = source.deriveNewAbstraction
												(source.getAccessPath().copyWithNewValue(originalCallArg), (Stmt) exitStmt);
										res.add(abs);
										registerActivationCallSite(callSite, callee, abs);
									}
								}
							}
							
							// Map the "this" local
							if (!callee.isStatic()) {
								if (thisLocal == sourceBase && hasCompatibleTypesForCall
										(source.getAccessPath(), callee.getDeclaringClass())) {
									// check if it is not one of the params (then we have already fixed it)
									if (!parameterAliases) {
										if (callSite instanceof Stmt) {
											Stmt stmt = (Stmt) callSite;
											if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
												InstanceInvokeExpr iIExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
												Abstraction abs = source.deriveNewAbstraction
														(source.getAccessPath().copyWithNewValue(iIExpr.getBase()), (Stmt) exitStmt);
												res.add(abs);
												registerActivationCallSite(callSite, callee, abs);
											}
										}
									}
								}
								}
						}
																		
						for (Abstraction abs : res)
							if (abs != source)
								abs.setCorrespondingCallSite((Stmt) callSite);
						
						return res;
					}
				};
			}
			
			@Override
			public FlowFunction<Abstraction> getCallToReturnFlowFunction(final Unit call, final Unit returnSite) {
				final Stmt iStmt = (Stmt) call;
				final InvokeExpr invExpr = iStmt.getInvokeExpr();
				
				final Value[] callArgs = new Value[iStmt.getInvokeExpr().getArgCount()];
				for (int i = 0; i < iStmt.getInvokeExpr().getArgCount(); i++)
					callArgs[i] = iStmt.getInvokeExpr().getArg(i);
				
				final SootMethod callee = invExpr.getMethod();
				
				final DefinitionStmt defStmt = iStmt instanceof DefinitionStmt
						? (DefinitionStmt) iStmt : null;
				
				return new SolverCallToReturnFlowFunction() {
					@Override
					public Set<Abstraction> computeTargets(Abstraction d1, Abstraction source) {
						if (source == getZeroValue())
							return Collections.emptySet();
						assert source.isAbstractionActive() || flowSensitiveAliasing;
						
						// Compute wrapper aliases
						if (taintWrapper != null) {
							Set<Abstraction> wrapperAliases = taintWrapper.getAliasesForMethod(
									iStmt, d1, source);
							if (wrapperAliases != null && !wrapperAliases.isEmpty()) {
								Set<Abstraction> passOnSet = new HashSet<>(wrapperAliases.size());
								for (Abstraction abs : wrapperAliases)
									if (defStmt != null && defStmt.getLeftOp()
											== abs.getAccessPath().getPlainValue()) {
										// Do not pass on this taint, but trigger the forward analysis
										for (Unit u : interproceduralCFG().getPredsOf(defStmt))
											fSolver.processEdge(new PathEdge<Unit, Abstraction>(d1, u, abs));
									}
									else
										passOnSet.add(abs);
								return passOnSet;
							}
						}
						
						// If the callee does not read the given value, we also need to pass it on
						// since we do not propagate it into the callee.
						if (enableStaticFields && source.getAccessPath().isStaticFieldRef()) {
							if (interproceduralCFG().isStaticFieldUsed(callee,
									source.getAccessPath().getFirstField()))
								return Collections.emptySet();
						}
						
						// We may not pass on a taint if it is overwritten by this call
						if (iStmt instanceof DefinitionStmt && ((DefinitionStmt) iStmt).getLeftOp()
								== source.getAccessPath().getPlainValue())
							return Collections.emptySet();
						
						// If the base local of the invocation is tainted, we do not
						// pass on the taint
						if (iStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
							InstanceInvokeExpr iinv = (InstanceInvokeExpr) iStmt.getInvokeExpr();
							if (iinv.getBase() == source.getAccessPath().getPlainValue())
								return Collections.emptySet();
						}
						
						// We do not pass taints on parameters over the call-to-return edge
						for (int i = 0; i < callArgs.length; i++)
							if (callArgs[i] == source.getAccessPath().getPlainValue())
								return Collections.emptySet();
												
						return Collections.singleton(source);
					}
				};
			}
			
		};
	}
	
}
