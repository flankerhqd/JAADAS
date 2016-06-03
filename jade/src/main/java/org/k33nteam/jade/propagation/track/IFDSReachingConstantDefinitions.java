package org.k33nteam.jade.propagation.track;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.collect.MutableTwoElementSet;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//used to track constant propagation, reaching definition is too memory-consumption, backward propagation
//on inverse icfg
public class IFDSReachingConstantDefinitions extends DefaultJimpleIFDSTabulationProblem<TrackFact,InterproceduralCFG<Unit, SootMethod>> {
	final static boolean DEBUG = false;
	final static boolean CONSTANT_DEBUG = false;
	BackwardsInfoflowCFG infoflowCFG;
	IAPIVulnManager manager;
	public IFDSReachingConstantDefinitions(BackwardsInfoflowCFG icfg, IAPIVulnManager imanager) {
		super(icfg);
		infoflowCFG = icfg;
		manager = imanager;
	}
	
	@Override
	public FlowFunctions<Unit, TrackFact, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, TrackFact, SootMethod>() {

			@Override
			public FlowFunction<TrackFact> getNormalFlowFunction(final Unit curr, final Unit succ) {
				
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;

					return new FlowFunction<TrackFact>() {
						@Override
						public Set<TrackFact> computeTargets(TrackFact source) {
							if(DEBUG) System.out.println("getNormalFlowFunction: " + curr + " in method: " + interproceduralCFG().getMethodOf(curr) + " next inst: " + succ + " source: "+ source);
							if (source != zeroValue()) {
								if(DEBUG) { System.out.println(source); System.out.println(assignment.getLeftOp());}
								if ( (assignment.getRightOp() instanceof Constant || assignment.getRightOp() instanceof StaticFieldRef) && maybeSameLocation(assignment.getLeftOp(), source.getO1())) { //notice $r0.test may not equivTo $r0.test because $r0 may be assigned at different location
									// we need pointer analysis
									//HOOK here
									//System.out.println(interproceduralCFG().getMethodOf(assignment));
									if (CONSTANT_DEBUG) {
										System.out.println("constant found! killing value: " + assignment.getRightOp());
									}
									manager.isParamVulnAndStore(interproceduralCFG().getMethodOf(source.getO2()), source.getO2(), assignment.getRightOp());
									return Collections.emptySet();
								}
								if (source.getO1().equivTo(assignment.getLeftOp()) || maybeSameLocation(source.getO1(), assignment.getLeftOp())) {//to save time
									if(DEBUG) System.out.println("transfering from " + source + " to " + assignment.getRightOp());
									Value right = assignment.getRightOp();
									return Collections.singleton(new TrackFact(right, source.getO2()));
								}
								return Collections.singleton(source);
							} else {
								//zero value? pass
								return Collections.emptySet();
							}
						}
					};
				}
				
				if (curr instanceof ReturnStmt) {
					final ReturnStmt returnStmt = (ReturnStmt) curr;

					return new FlowFunction<TrackFact>() {
						@Override
						public Set<TrackFact> computeTargets(TrackFact source) {
							if(DEBUG) System.out.println("getNormalFlowFunction: " + curr + " in method: " + interproceduralCFG().getMethodOf(curr) + " next inst: " + succ + " source: "+ source);
							
							if (source != zeroValue()) {
								if ( maybeSameLocation(returnStmt.getOp(), source.getO1())) {
									if (returnStmt.getOp() instanceof Constant || returnStmt.getOp() instanceof StaticFieldRef) {
										//System.out.println("getNormalFlowFunction: returnstmt " + returnStmt + " source: " + source + " method: " + interproceduralCFG().getMethodOf(returnStmt));
										//HOOK here
										if (CONSTANT_DEBUG) System.out.println("constant found! " + returnStmt.getOp());
										if (CONSTANT_DEBUG) System.out.println("killing " + source );
										manager.isParamVulnAndStore(interproceduralCFG().getMethodOf(source.getO2()), source.getO2(), returnStmt.getOp());
										//returns a constant, now only consider fields.
										return Collections.emptySet();
									}
									if(DEBUG) System.out.println("transfering from " + source + " to " + returnStmt.getOp());
									Value right = returnStmt.getOp();
									return Collections.singleton(new TrackFact(right, source.getO2()));
								}
								return Collections.singleton(source);
							} else {
								//zero value? pass
								return Collections.emptySet();
							}
						}
					};
				}
				else {
					return new FlowFunction<TrackFact>() {

						@Override
						public Set<TrackFact> computeTargets(TrackFact source) {
							if(DEBUG) System.out.println("getNormalFlowFunction: " + curr + " in method: " + interproceduralCFG().getMethodOf(curr) + " next inst: " + succ + " source: "+ source);
							return Collections.singleton(source);
						}
					};
				}
			}

			@Override
			public FlowFunction<TrackFact> getCallFlowFunction(Unit callStmt,
					final SootMethod destinationMethod) {
				final Stmt stmt = (Stmt) callStmt;

				return new FlowFunction<TrackFact>() {

					@Override
					public Set<TrackFact> computeTargets(TrackFact source) {
						if(DEBUG) System.out.println("getCallFlowFunction: " + stmt + " source: " + source +" in method: " + interproceduralCFG().getMethodOf(stmt));
						//if ( destinationMethod.getSignature().startsWith("<javax") || destinationMethod.getSignature().startsWith("<com.sun") || destinationMethod.getSignature().startsWith("<java.security")) {
						//	return Collections.emptySet();
						//}
						//if (!destinationMethod.getName().equals("<clinit>")
						//		&& !destinationMethod.getSubSignature().equals("void run()"))
						//	if(localArguments.contains(source.getO1())) {
						//		int paramIndex = args.indexOf(source.getO1());
						//		Pair<Value, Set<String>> pair = new Pair<Value, Set<String>>(
						//				new EquivalentValue(Jimple.v().newParameterRef(destinationMethod.getParameterType(paramIndex), paramIndex)),
						//				source.getO2());
						//		return Collections.singleton(pair);
						//	}

						//一般来说，分两种情况:一个是this->invoke, 一个是invoke(param1, param2), param1有记录到field的taint
						//return Collections.singleton(source);
						//当source不为空时，传递进去
						if (source ==  zeroValue()) {
							return Collections.emptySet();
						}
						else {
							LinkedHashSet<TrackFact> values = new LinkedHashSet<>();
							if (source.getO1() instanceof FieldRef) {
								//propagate this, static field ref, params
								//TODO: do with param, tainted field in param
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
									if(DEBUG) System.out.println("adding staticfield ref: " + source.getO1());
									values.add(source);
								}
								else {
									if(DEBUG) System.out.println("not static ref or this field, ignoring");
								}
							}
							if (stmt instanceof DefinitionStmt) 
							{
								DefinitionStmt definitionStmt = (DefinitionStmt)stmt;
								if (maybeSameLocation(definitionStmt.getLeftOp(), source.getO1())) {
									//we need to find the return value of this method call, transfer backtrack to it
									//但这个不是应该在call-to-return里处理？也不是，因为call-to-return只处理非call相关的datafact
									for (Unit u : destinationMethod.getActiveBody().getUnits()) {
										if (u instanceof ReturnStmt) {
											ReturnStmt rStmt = (ReturnStmt) u;
											if (rStmt.getOp() instanceof Local
													|| rStmt.getOp() instanceof FieldRef) {
												values.add(new TrackFact(rStmt.getOp(), source.getO2()));
											}
											else if (rStmt.getOp() instanceof Constant || rStmt.getOp() instanceof StaticFieldRef) {
												//directly constant, HOOK here
												manager.isParamVulnAndStore(interproceduralCFG().getMethodOf(source.getO2()), source.getO2(), rStmt.getOp());
											}
										}
									}
								}
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
					}
				};
			}

			@Override
			public FlowFunction<TrackFact> getReturnFlowFunction(final Unit callSite,
					final SootMethod calleeMethod, final Unit exitStmt, final Unit returnSite) {


				return new FlowFunction<TrackFact>() {

					@Override
					public Set<TrackFact> computeTargets(TrackFact source) {
						if(DEBUG) System.out.println("return flow flow :"+callSite);
						//TODO: check exitStmt because exitStmt maybe assignStmt
						
						if(DEBUG) System.out.println("getReturnFlowFunction: callSite " + callSite + " calleeMethod: " + calleeMethod.getSignature() + " exitStmt: " + exitStmt + " returnSite: " + returnSite +   " source: " + source);
						
						if (exitStmt instanceof DefinitionStmt) {
							DefinitionStmt assignStmt = (DefinitionStmt)exitStmt;
							if (source.getO1().equivTo(assignStmt.getLeftOp()) || maybeSameLocation(source.getO1(), assignStmt.getLeftOp())) {
								//HOOK: check value here.
								if (assignStmt.getRightOp() instanceof Constant || assignStmt.getRightOp() instanceof StaticFieldRef) { //save time
									if(CONSTANT_DEBUG) System.out.println("found constant! "+assignStmt.getRightOp() + " killing value");
									manager.isParamVulnAndStore(interproceduralCFG().getMethodOf(source.getO2()), source.getO2(), assignStmt.getRightOp());
									return Collections.emptySet();
								}
								else {
									return Collections.singleton(new TrackFact(assignStmt.getLeftOp(), source.getO2()));
								}
							}
						}
						//FIXME: should also check for this, static?
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
							System.out.println("killing value: " + source);
						}
						return Collections.emptySet();
					}
				};
			}

			@Override
			public FlowFunction<TrackFact> getCallToReturnFlowFunction(final Unit callSite, final Unit returnSite) {
				final Stmt stmt = (Stmt)callSite;
				if(DEBUG) System.out.println("callflow :"+stmt);
				/*
				if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof StaticInvokeExpr && stmt.getInvokeExpr().getMethod().getSignature().equals("<javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String)>") ) {
					System.out.println("callsite: " + callSite + " of method: " + interproceduralCFG().getMethodOf(callSite));
					final StaticInvokeExpr staticExpr = (StaticInvokeExpr) stmt.getInvokeExpr();
					return new FlowFunction<TrackFact>() {
						@Override
						public Set<TrackFact> computeTargets(TrackFact source) {
							if(DEBUG) System.out.println("method: " + interproceduralCFG().getMethodOf(stmt));
							if(DEBUG) System.out.println("getCallToReturnFlowFunction generating source: " + stmt + " source: " + source);
							//TODO: kill taint on field write
							if (source != zeroValue()) {
								if (source.getO1().equivTo(staticExpr.getArg(0)) || maybeSameLocation(source.getO1(), staticExpr.getArg(0))) {
									if(DEBUG) System.out.println("source equal");
									return Collections.singleton(source);
								}
								else {
									return new MutableTwoElementSet<TrackFact>(source, new TrackFact(staticExpr.getArg(0), source.getO2()));
								}
							} else {
								return Collections.singleton(new TrackFact(staticExpr.getArg(0), source.getO2()));
							}
						}
					};
				}*/
				//HOOK: start tracing value
				

				if(DEBUG) System.out.println("call-to-return flow flow :"+callSite);

				return new FlowFunction<TrackFact>() {

					@Override
					public Set<TrackFact> computeTargets(TrackFact source) {
						TrackFact mid = processCallToReturnFact(source);
						return processPotentialGeneration(stmt, mid);
					}
					
					private Set<TrackFact> processPotentialGeneration(Stmt stmt, TrackFact source)
					{
						if (stmt.containsInvokeExpr()) {
							if (DEBUG) {
								System.out.println("callsite: " + callSite + " of method: " + interproceduralCFG().getMethodOf(callSite));
							}
							final Value value= manager.getVulnParamAsSource(stmt);
							if (value != null) { // this is the api we want to track
								if (value instanceof Constant || value instanceof StaticFieldRef) {
									manager.isParamVulnAndStore(interproceduralCFG().getMethodOf(stmt), stmt, value);
								}
								else { //not constant, begin tracking
									if(DEBUG) System.out.println("method: " + interproceduralCFG().getMethodOf(stmt));
									if(DEBUG) System.out.println("getCallToReturnFlowFunction generating source: " + stmt + " source: " + source);
									if (source != zeroValue() && source != null) {
										if (source.getO1().equivTo(value) || maybeSameLocation(source.getO1(), value)) {
											if(DEBUG) System.out.println("source equal");
											return Collections.singleton(source);
										}
										else {
											return new MutableTwoElementSet<TrackFact>(source, new TrackFact(value, stmt));
										}
									} else {
										return Collections.singleton(new TrackFact(value, stmt));
									}
								}
							}
						}
						if (source == null || source == zeroValue()) {
							return Collections.emptySet();
						}
						else {
							return Collections.singleton(source);//pass on source
						}
					}
					private TrackFact processCallToReturnFact(TrackFact source)
					{
						final SootMethod callee = ((Stmt)callSite).getInvokeExpr().getMethod();
						if(DEBUG) System.out.println("getCallToReturnFlowFunction: callSite " + callSite  + " returnSite: " + returnSite +   " source: " + source);
						
						if (source == zeroValue()) {
							return null;
						}
						if (source.getO1() instanceof FieldRef) {
							//if this and static, do not propagate
							if (source.getO1() instanceof StaticFieldRef) {
								if (DEBUG) {
									System.out.println("value: " + source + "killed  because of staticfieldref in getCallToReturn");
								}
								//没有use过得话就propagate，use过得话就返回空, copied from FlowDroid, refer to InfoFlowProblem
								if (!infoflowCFG.isStaticFieldUsed(callee,((StaticFieldRef)source.getO1()).getField()))
											return source;
								return null;
							}
							// do not propagate this->facts
							if (source.getO1() instanceof InstanceFieldRef) {
								if (stmt.containsInvokeExpr()) {
									InvokeExpr invokeExpr = stmt.getInvokeExpr();
									if (invokeExpr instanceof InstanceInvokeExpr) {
										InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr)invokeExpr;

										if (mayBaseAtSameLocation((Local)((InstanceFieldRef)source.getO1()).getBase(), (Local)instanceInvokeExpr.getBase())) {
											if (DEBUG) {
												System.out.println("value: " + source + "killed  because of this field in getCallToReturn");
											}
											return null;
										}
									}
								}
								
							}

							if (DEBUG) {
								System.out.println("value: " + source + "propagated because non-this nor static in calltoreturn");
							}
							return source;
						}
						if (callSite instanceof DefinitionStmt) {
							DefinitionStmt assignStmt = (DefinitionStmt)callSite;
							if(source.getO1().equivTo(assignStmt.getLeftOp()) || maybeSameLocation(source.getO1(), assignStmt.getLeftOp())) {
								//now track return stmt
								if (DEBUG) {
									System.out.println("killing value: " + source + " because of return overwrite");
								}
								return null;
							} else {
								return source;
							}
						}
						return source;
					}
				};
			}
		};
	}

	public Map<Unit, Set<TrackFact>> initialSeeds() {
		if(DEBUG) System.out.println("initial seeds");
		return DefaultSeeds.make(Collections.singleton(Scene.v().getEntryPoints().get(0).getActiveBody().getUnits().getLast()), zeroValue());
	}


	public TrackFact createZeroValue() {
		return new TrackFact(new JimpleLocal("<<zero>>", NullType.v()) , null);
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
			return new EquivalentValue(value);
		} else {
			return value;
		}
	}
}
