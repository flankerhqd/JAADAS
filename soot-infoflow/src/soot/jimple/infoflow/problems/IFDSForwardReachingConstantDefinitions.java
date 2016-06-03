/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-2013 Eric Bodden and others
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
package soot.jimple.infoflow.problems;

import heros.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.scalar.Pair;

import java.util.*;

class TaintFact extends Pair<Value, Set<DefinitionStmt>> // first for currently tainting value, second for taint source value. Taint always made in AssignStmt via CallToReturn
{
	public TaintFact(Value value, Set<DefinitionStmt> assignStmt)
	{
		super(value, assignStmt);
	}

	
}
//used to track constant propagation, reaching definition is too memory-consumption, forward propagation on icfg
//currently track StringArrayExtra, use getIntent() as source, propagate through getExtras, getStringExtra
public class IFDSForwardReachingConstantDefinitions extends DefaultJimpleIFDSTabulationProblem<TaintFact,InterproceduralCFG<Unit, SootMethod>> {
	boolean found = false;
	final static boolean DEBUG = true;
	InfoflowCFG infoflowCFG;
	public IFDSForwardReachingConstantDefinitions(InfoflowCFG icfg) {
		super(icfg);
		infoflowCFG = icfg;
	}
	
	private static boolean checkIfIsArrayFunction(SootMethod method, InstanceInvokeExpr instanceInvokeExpr)
	{
		String methodName = method.getName();
		Value base = instanceInvokeExpr.getBase();
		System.out.println(base.getType());
		if (base.getType().toString().equals("android.content.Intent")) {
			if (methodName.startsWith("get") && methodName.contains("Array")) {
				return true;
			}
		}
		return false;
	}
	@Override
	public FlowFunctions<Unit, TaintFact, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, TaintFact, SootMethod>() {

			@Override
			public FlowFunction<TaintFact> getNormalFlowFunction(final Unit curr, final Unit succ) {
				
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;

					return new FlowFunction<TaintFact>() {
						@Override
						public Set<TaintFact> computeTargets(TaintFact source) {
							if(DEBUG) System.out.println("getNormalFlowFunction: " + curr + " in method: " + interproceduralCFG().getMethodOf(curr) + " next inst: " + succ + " source: "+ source);
							if (source != zeroValue()) {
								if(DEBUG) System.out.println(source);
								System.out.println(assignment.getLeftOp());
								if (assignment.getRightOp() instanceof Constant && maybeSameLocation(assignment.getLeftOp(), source.getO1())) { //notice $r0.test may not equivTo $r0.test because $r0 may be assigned at different location
									// we need pointer analysis
									//System.out.println(interproceduralCFG().getMethodOf(assignment));
									//although nearly impossible in our case
									// System.out.println("constant found! killing value: " + assignment.getRightOp() + "found: "+found);
									//found = true;
									if(DEBUG) System.out.println("killing because of constant");
									return Collections.emptySet();
								}
								if (assignment.getRightOp() instanceof FieldRef && maybeSameLocation(assignment.getLeftOp(),  source.getO1()) && !interproceduralCFG().getMethodOf(curr).isConstructor()) {
									FieldRef fieldRef = (FieldRef)assignment.getRightOp();
									if(fieldRef.getField().isFinal())
									{
										//perform kill
										if(DEBUG) System.out.println("killing because of final field and constructor");
										return Collections.emptySet();
									}
								}
								if (source.getO1().equivTo(assignment.getRightOp()) || maybeSameLocation(source.getO1(), assignment.getRightOp())) {//transfer
									System.out.println("transfering from " + source + " to " + assignment.getRightOp());
									Value left = assignment.getLeftOp();
									return Collections.singleton(new TaintFact(left, source.getO2()));
								}
								return Collections.singleton(source);
							} else {
								//zero value? pass
								if(DEBUG) System.out.println("killing because of zero value");
								return Collections.emptySet();
							}
						}
					};
				}
				else {
					return new FlowFunction<TaintFact>() {
						@Override
						public Set<TaintFact> computeTargets(TaintFact source) {
							if (source == zeroValue()) {
								return Collections.emptySet();
							}
							else {
								return Collections.singleton(source);
							}
						}
					};
				}
			}

			@Override
			public FlowFunction<TaintFact> getCallFlowFunction(Unit callStmt,
					final SootMethod destinationMethod) {
				final Stmt stmt = (Stmt) callStmt;

				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				final List<Value> args = invokeExpr.getArgs();

				final List<Local> localArguments = new ArrayList<Local>(args.size());
				for (Value value : args) {
					if (value instanceof Local)
						localArguments.add((Local) value);
					else
						localArguments.add(null);
				}

				return new FlowFunction<TaintFact>() {

					@Override
					public Set<TaintFact> computeTargets(TaintFact source) {
						if(DEBUG) System.out.println("getCallFlowFunction: " + stmt + " source: " + source +" in method: " + interproceduralCFG().getMethodOf(stmt));
						//一般来说，分两种情况:一个是this->invoke, 一个是invoke(param1, param2), param1有记录到field的taint
						//return Collections.singleton(source);
						//当source不为空时，传递进去
						if (source ==  zeroValue()) {
							return Collections.emptySet();
						}
						else {
							LinkedHashSet<TaintFact> values = new LinkedHashSet<>();
							if (source.getO1() instanceof FieldRef) {
								//propagate this, static field ref, params
								
								//TODO: process field taint in param
								//propagate this fields
								if (DEBUG) {
									System.out.println("checking field ref: " + source + " on " + stmt);
								}
								if(DEBUG) {System.out.println("checking this fields... type:" + source.getO1().getClass());}
								if (source.getO1() instanceof InstanceFieldRef && stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
									InstanceFieldRef instanceFieldRef = (InstanceFieldRef)source.getO1();
									Value invokeBase = ((InstanceInvokeExpr)stmt.getInvokeExpr()).getBase();
									if(DEBUG) {System.out.println("comparing local: " + instanceFieldRef.getBase() + " and " + invokeBase);}
									if (mayBaseAtSameLocation((Local)instanceFieldRef.getBase(), (Local)invokeBase)) {
										values.add(source);
									}
								}
								else if(source.getO1() instanceof StaticFieldRef)
								{
									System.out.println("adding staticfield ref: " + source);
									values.add(source);
								}
								else {
									System.out.println("not static ref or this field, ignoring");
								}
								
								if (values.size() == 0) {
									if (DEBUG) {
										System.out.println("killing value: " + source + " in callflowfunction");
									}
									return Collections.emptySet();
								}
								else {
									return values;
								}
							}
							
							//map params into callee context, refer to reachingdefinition
							if (!destinationMethod.getName().equals("<clinit>") && !destinationMethod.getSubSignature().equals("void run()"))
								if(localArguments.contains(source.getO1())) {
									int paramIndex = args.indexOf(source.getO1());
									TaintFact pair = new TaintFact(new EquivalentValue(Jimple.v().newParameterRef(destinationMethod.getParameterType(paramIndex), paramIndex)),source.getO2());
									return Collections.singleton(pair);
								}
					
							return Collections.emptySet();
						}
					}
				};
			}

			@Override
			public FlowFunction<TaintFact> getReturnFlowFunction(final Unit callSite,
					final SootMethod calleeMethod, final Unit exitStmt, final Unit returnSite) {


				return new FlowFunction<TaintFact>() {

					@Override
					public Set<TaintFact> computeTargets(TaintFact source) {
						if(DEBUG) System.out.println("return flow flow :"+callSite);
						//if (!(callSite instanceof AssignStmt))
						//	return Collections.emptySet();

						//if (exitStmt instanceof ReturnVoidStmt)
						//	return Collections.emptySet();
						//TODO: check exitStmt because exitStmt maybe assignStmt
						
						//map back to return value, refer to ReachingDefinition implementation
						if(DEBUG) System.out.println("getReturnFlowFunction: callSite " + callSite + " calleeMethod: " + calleeMethod.getSignature() + " exitStmt: " + exitStmt + " returnSite: " + returnSite +   " source: " + source);
						
						if(exitStmt instanceof ReturnStmt) {
							ReturnStmt returnStmt = (ReturnStmt) exitStmt;
							if (returnStmt.getOp().equivTo(source.getO1())) {
								DefinitionStmt definitionStmt = (DefinitionStmt) callSite;
								TaintFact pair = new TaintFact(definitionStmt.getLeftOp(), source.getO2());
								return Collections.singleton(pair);
							}
						}
						if (source.getO1() instanceof FieldRef) {
							if (DEBUG) {
								System.out.println("passing taint on fieldref in getReturnFlow functions: " + source);
							}
							return Collections.singleton(source);
						}
						//if(exitStmt instanceof ReturnStmt) {
						//	ReturnStmt returnStmt = (ReturnStmt) exitStmt;
						//	if (returnStmt.getOp().equivTo(source)) {
						//		AssignStmt assignStmt = (AssignStmt) callSite;
						//		return Collections.singleton(assignStmt.getLeftOp());
						//	}
						//}
						if (DEBUG) {
							System.out.println("killing value: " + source + " in returnflow function");
						}
						return Collections.emptySet();
					}
				};
			}

			@Override
			public FlowFunction<TaintFact> getCallToReturnFlowFunction(final Unit callSite, final Unit returnSite) {
				// we found taint here.
				final Stmt stmt = (Stmt)callSite;
				if(DEBUG) System.out.println("callflow :"+stmt);

				if(DEBUG) System.out.println("call-to-return flow flow :"+callSite);

				return new FlowFunction<TaintFact>() {

					@Override
					public Set<TaintFact> computeTargets(TaintFact source) {
						//TODO: track and record intent source. 
						//检查source.getO1和当前的invoke base是不是一个就可以了。
						final SootMethod callee = ((Stmt)callSite).getInvokeExpr().getMethod();
						if (callee.getName().equals("getIntent") && callSite instanceof DefinitionStmt) {
							//generate taint here
							if(DEBUG) System.out.println("call-to-return flow flow generating getIntent taint");
							DefinitionStmt definitionStmt = (DefinitionStmt)callSite;
							TaintFact newfact = new TaintFact(definitionStmt.getLeftOp(), Collections.singleton(definitionStmt));

							if (DEBUG) {
								System.out.println("checking two sources: I: " + source + " II: " + newfact);
							}
							//if (newfact.equals(source) || source == zeroValue())
							
							//we should check, if the incoming source is also a intent type, we should not propagate it. 
							//Otherwise we should generate two source
							if (source.getO1().getType().toString().equals("android.content.Intent")) {
								if (DEBUG) {
									System.out.println("facts are same or zero because in getIntent");
								}
								return Collections.singleton(newfact);
							}
							else
							{
								return new TwoElementSet<TaintFact>(newfact, source);
							}
							//else {
							//	if (DEBUG) {
							//		System.out.println("facts are not same, propagating two facts");
							//	}
							//	return new TwoElementSet<TaintFact>(newfact, source);
							//}
						}
						if (callSite instanceof DefinitionStmt && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
							Value base = ((InstanceInvokeExpr)stmt.getInvokeExpr()).getBase();
							//add taint to return value
							//getStringArrayExtra的返回值不会覆盖intent相关的taint，因为他们类型不符，故不考虑这个问题
							if (checkIfIsArrayFunction(callee, (InstanceInvokeExpr) stmt.getInvokeExpr())) {
								if (base.equivTo(source.getO1())) {
									if(DEBUG) System.out.println("call-to-return flow flow generating getExtra taint");
									//generate taint here
									DefinitionStmt definitionStmt = (DefinitionStmt)callSite;
									Set<DefinitionStmt> stmts = new HashSet<>();
									stmts.addAll(source.getO2());
									stmts.add(definitionStmt);
									TaintFact newfact = new TaintFact(definitionStmt.getLeftOp(), stmts);
									if (newfact.equals(source) || source == zeroValue()) {
										return Collections.singleton(newfact);
									}
									else {
										return new TwoElementSet<TaintFact>(newfact, source);
									}
								}
								else {
									//err... get*ArrayExtra without recorded base, just fall through
								}
							}
							
						}
						if(DEBUG) System.out.println("getCallToReturnFlowFunction: callSite " + callSite  + " returnSite: " + returnSite +   " source: " + source);
						
						if (source == zeroValue()) {
							return Collections.emptySet();
						}
						if (source.getO1() instanceof FieldRef) {
							//if this and static, do not propagate
							if (source.getO1() instanceof StaticFieldRef) {
								if (DEBUG) {
									System.out.println("value: " + source + "killed  because of staticfieldref in getCallToReturn");
								}
								if (!infoflowCFG.isStaticFieldUsed(callee,((StaticFieldRef)(source.getO1())).getField()))
											return Collections.singleton(source);
								return Collections.emptySet();
							}
							if (source.getO1() instanceof InstanceFieldRef) {
								if (stmt.containsInvokeExpr()) {
									InvokeExpr invokeExpr = stmt.getInvokeExpr();
									if (invokeExpr instanceof InstanceInvokeExpr) {
										InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr)invokeExpr;
										InstanceFieldRef instanceFieldRef = (InstanceFieldRef) source.getO1();
										if (mayBaseAtSameLocation((Local)(instanceFieldRef.getBase()), (Local)instanceInvokeExpr.getBase())) {
											if (DEBUG) {
												System.out.println("value: " + source + "killed  because of this field in getCallToReturn");
											}
											return Collections.emptySet();
										}
									}
								}
								
							}

							if (DEBUG) {
								System.out.println("value: " + source + "propagated because non-this nor static in calltoreturn");
							}
							return Collections.singleton(source);
						}
						if (callSite instanceof DefinitionStmt) {
							DefinitionStmt assignStmt = (DefinitionStmt)callSite;
							if(source.getO1().equivTo(assignStmt.getLeftOp()) || maybeSameLocation(source.getO1(), assignStmt.getLeftOp())) {
								//now track return stmt
								if (DEBUG) {
									System.out.println("killing value: " + source + " because of return overwrite");
								}
								return Collections.emptySet();
							} else {
								return Collections.singleton(source);
							}
						}
						return Collections.singleton(source);
					}
				};
			}
		};
	}

	public Map<Unit, Set<TaintFact>> initialSeeds() {
		if(DEBUG) System.out.println("initial seeds");
		return DefaultSeeds.make(Collections.singleton(Scene.v().getEntryPoints().get(0).getActiveBody().getUnits().getFirst()), zeroValue());
	}


	public TaintFact createZeroValue() {
		return new TaintFact(new JimpleLocal("<<zero>>", NullType.v()),Collections.<DefinitionStmt> emptySet());
	}

	public static boolean mayBaseAtSameLocation(Local base1, Local base2)
	{
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet pts1 = pta.reachingObjects(base1);
		PointsToSet pts2 = pta.reachingObjects(base2);								
		return pts1.hasNonEmptyIntersection(pts2);
	}
	public static boolean maybeSameLocation(Value v1, Value v2) {
		if(!(v1 instanceof InstanceFieldRef && v2 instanceof InstanceFieldRef) &&
		   !(v1 instanceof ArrayRef && v2 instanceof ArrayRef)) {
			return v1.equivTo(v2);
		}
		if(v1 instanceof InstanceFieldRef && v2 instanceof InstanceFieldRef) {
			InstanceFieldRef ifr1 = (InstanceFieldRef) v1;
			InstanceFieldRef ifr2 = (InstanceFieldRef) v2;
			if(!ifr1.getField().getName().equals(ifr2.getField().getName())) return false;
			
			Local base1 = (Local) ifr1.getBase();
			Local base2 = (Local) ifr2.getBase();
			PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
			PointsToSet pts1 = pta.reachingObjects(base1);
			PointsToSet pts2 = pta.reachingObjects(base2);								
			return pts1.hasNonEmptyIntersection(pts2);
		} else { //v1 instanceof ArrayRef && v2 instanceof ArrayRef
			ArrayRef ar1 = (ArrayRef) v1;
			ArrayRef ar2 = (ArrayRef) v2;

			Local base1 = (Local) ar1.getBase();
			Local base2 = (Local) ar2.getBase();
			PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
			PointsToSet pts1 = pta.reachingObjects(base1);
			PointsToSet pts2 = pta.reachingObjects(base2);								
			return pts1.hasNonEmptyIntersection(pts2);
		}
	}
	
	public static Value getFieldRefWrap(Value value) {
		if (value instanceof FieldRef) {
			return new EquivalentValue((FieldRef) value);
		} else {
			return value;
		}
	}
}
