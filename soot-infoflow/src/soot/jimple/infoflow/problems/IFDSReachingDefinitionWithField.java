package soot.jimple.infoflow.problems;

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

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.EquivalentValue;
import soot.Local;
import soot.NullType;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.scalar.Pair;

public class IFDSReachingDefinitionWithField
		extends
		DefaultJimpleIFDSTabulationProblem<Pair<Value, Set<DefinitionStmt>>, InterproceduralCFG<Unit, SootMethod>> {
	public IFDSReachingDefinitionWithField(
			InterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
	}

	public static Value getFieldRefWrap(Value value) {
		if (value instanceof FieldRef) {
			return new EquivalentValue((FieldRef) value);
		} else {
			return value;
		}
	}

	@Override
	public FlowFunctions<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod>() {

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getNormalFlowFunction(
					final Unit curr, Unit succ) {
				System.out.println("getNormalFlowFunction: " + curr);
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;

					return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {
						@Override
						public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(
								Pair<Value, Set<DefinitionStmt>> source) {
							System.out
									.println("in getNormalFlowFunction: computing targets of "
											+ source);
							System.out.println("current statement: " + curr);
							if (source != zeroValue()) {
								if (source.getO1().equivTo(
										assignment.getLeftOp())) { // a = b,
																	// kills a
									return Collections.emptySet();
								}
								return Collections.singleton(source); // same as
																		// Identity
																		// Function
							} else {
								System.out.println("creating new source");
								LinkedHashSet<Pair<Value, Set<DefinitionStmt>>> res = new LinkedHashSet<Pair<Value, Set<DefinitionStmt>>>();
								res.add(new Pair<Value, Set<DefinitionStmt>>(
										getFieldRefWrap(assignment.getLeftOp()),
										Collections
												.<DefinitionStmt> singleton(assignment))); // record
																							// "left"
																							// value
																							// is
																							// defined
																							// in
																							// this
																							// assignment
																							// statement
								return res;
							}
						}
					};
				}

				return Identity.v();
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getCallFlowFunction(
					final Unit callStmt, final SootMethod destinationMethod) {
				System.out.println("destinationMethod " + destinationMethod);
				Stmt stmt = (Stmt) callStmt;
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				final List<Value> args = invokeExpr.getArgs();

				final List<Local> localArguments = new ArrayList<Local>(
						args.size());
				// 在调用者的callsite中获取local arguments
				for (Value value : args) {
					if (value instanceof Local)
						localArguments.add((Local) value);
					else
						localArguments.add(null);
				}

				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(
							Pair<Value, Set<DefinitionStmt>> source) {
						System.out
								.println("in CallFlowFunction: computing targets of "
										+ source);
						System.out.println("call statement: " + callStmt);
						System.out.println("destinationMethod "
								+ destinationMethod);
						if (!destinationMethod.getName().equals("<clinit>")
								&& !destinationMethod.getSubSignature().equals(
										"void run()")) {
							if (localArguments.contains(source.getO1())) { // 调用者参数中包含source的value
								int paramIndex = args.indexOf(source.getO1());
								Pair<Value, Set<DefinitionStmt>> pair = new Pair<Value, Set<DefinitionStmt>>(
										new EquivalentValue(
												Jimple.v()
														.newParameterRef(
																destinationMethod
																		.getParameterType(paramIndex),
																paramIndex)),
										source.getO2());
								return Collections.singleton(pair);
							}

							if (source.getO1() instanceof EquivalentValue) {
								EquivalentValue equivalentValue = (EquivalentValue) source
										.getO1();
								if (equivalentValue.getValue() instanceof FieldRef) {
									// preserve data fact on field transition
									return Collections.singleton(source);
								} else {
									return Collections.emptySet();
								}
							} else {
								return Collections.emptySet();
							}
						}

						return Collections.emptySet();// KILLALL
					}
				};
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getReturnFlowFunction(
					final Unit callSite, final SootMethod calleeMethod,
					final Unit exitStmt, Unit returnSite) {
				// if (!(callSite instanceof
				// DefinitionStmt))//这里单独的datafact是不会被保留的，如果调用的时候没有赋值语句。
				// return KillAll.v();

				// if (exitStmt instanceof ReturnVoidStmt)
				// return KillAll.v();

				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(
							Pair<Value, Set<DefinitionStmt>> source) {
						System.out
								.println("in getReturnFlowFunction: computing targets of "
										+ source);
						System.out.println("call statement: " + callSite);
						System.out.println("destinationMethod " + calleeMethod);
						if (exitStmt instanceof ReturnStmt
								&& callSite instanceof DefinitionStmt) {
							ReturnStmt returnStmt = (ReturnStmt) exitStmt;
							if (returnStmt.getOp().equivTo(source.getO1())) {
								DefinitionStmt definitionStmt = (DefinitionStmt) callSite;
								Pair<Value, Set<DefinitionStmt>> pair = new Pair<Value, Set<DefinitionStmt>>(
										definitionStmt.getLeftOp(),
										source.getO2());// 记录return语句的左值返回值
								return Collections.singleton(pair);
							}
						} else {
							// need to preserve indirect assignments on field,
							// i.e.
							// $r1 = ""
							// a.r = $r1
							// return-void
							if (source.getO1() instanceof EquivalentValue) {
								EquivalentValue equivalentValue = (EquivalentValue) source
										.getO1();
								if (equivalentValue.getValue() instanceof FieldRef) {
									// preserve data fact
									return Collections.singleton(source);
								} else {
									return Collections.emptySet();
								}
							} else {
								return Collections.emptySet();
							}
						}
						return Collections.emptySet();// KILLALL
					}
				};
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getCallToReturnFlowFunction(
					final Unit callSite, Unit returnSite) {
				if (!(callSite instanceof DefinitionStmt))
					return Identity.v();

				final DefinitionStmt definitionStmt = (DefinitionStmt) callSite;
				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(
							Pair<Value, Set<DefinitionStmt>> source) {
						System.out
								.println("in getCallToReturnFlowFunction: computing targets of "
										+ source);
						System.out
								.println("in getCallToReturnFlowFunction: callSite "
										+ callSite);
						if (source.getO1().equivTo(definitionStmt.getLeftOp())) {
							return Collections.emptySet(); // a = b, kills a
						} else {
							return Collections.singleton(source); // Identity
						}
					}
				};
			}
		};
	}

	public Map<Unit, Set<Pair<Value, Set<DefinitionStmt>>>> initialSeeds() {
		return DefaultSeeds.make(
				Collections.singleton(Scene.v().getEntryPoints().get(0).getActiveBody()
						.getUnits().getFirst()), zeroValue());
	}

	public Pair<Value, Set<DefinitionStmt>> createZeroValue() {
		System.err.println("calling createZeroValue");
		return new Pair<Value, Set<DefinitionStmt>>(new JimpleLocal("<<zero>>",
				NullType.v()), Collections.<DefinitionStmt> emptySet());
	}

}
