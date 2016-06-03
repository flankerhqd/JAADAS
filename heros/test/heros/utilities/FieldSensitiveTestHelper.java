/*******************************************************************************
 * Copyright (c) 2014 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.utilities;

import static org.junit.Assert.assertTrue;
import heros.InterproceduralCFG;
import heros.utilities.Edge.Call2ReturnEdge;
import heros.utilities.Edge.CallEdge;
import heros.utilities.Edge.EdgeVisitor;
import heros.utilities.Edge.NormalEdge;
import heros.utilities.Edge.ReturnEdge;
import heros.utilities.EdgeBuilder.CallSiteBuilder;
import heros.utilities.EdgeBuilder.ExitStmtBuilder;
import heros.utilities.EdgeBuilder.NormalStmtBuilder;
import heros.fieldsens.AccessPath;
import heros.fieldsens.AccessPathHandler;
import heros.fieldsens.BiDiFieldSensitiveIFDSSolver;
import heros.fieldsens.FactMergeHandler;
import heros.fieldsens.FieldSensitiveIFDSSolver;
import heros.fieldsens.FlowFunction;
import heros.fieldsens.FlowFunctions;
import heros.fieldsens.IFDSTabulationProblem;
import heros.fieldsens.Scheduler;
import heros.fieldsens.ZeroHandler;
import heros.fieldsens.FlowFunction.ConstrainedFact;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

public class FieldSensitiveTestHelper {

	private Multimap<TestMethod, Statement> method2startPoint = HashMultimap.create();
	private List<NormalEdge> normalEdges = Lists.newLinkedList();
	private List<CallEdge> callEdges = Lists.newLinkedList();
	private List<Call2ReturnEdge> call2retEdges = Lists.newLinkedList();
	private List<ReturnEdge> returnEdges = Lists.newLinkedList();
	private Map<Statement, TestMethod> stmt2method = Maps.newHashMap();
	private Multiset<ExpectedFlowFunction> remainingFlowFunctions = HashMultiset.create();
	private TestDebugger<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>> debugger;

	public FieldSensitiveTestHelper(TestDebugger<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>> debugger) {
		this.debugger = debugger;
	}

	public MethodHelper method(String methodName, Statement[] startingPoints, EdgeBuilder... edgeBuilders) {
		MethodHelper methodHelper = new MethodHelper(new TestMethod(methodName));
		methodHelper.startPoints(startingPoints);
		for(EdgeBuilder edgeBuilder : edgeBuilders){
			methodHelper.edges(edgeBuilder.edges());
		}
		return methodHelper;
	}

	public static Statement[] startPoints(String... startingPoints) {
		Statement[] result = new Statement[startingPoints.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new Statement(startingPoints[i]);
		}
		return result;
	}

	public static EdgeBuilder.NormalStmtBuilder normalStmt(String stmt, ExpectedFlowFunction...flowFunctions) {
		return new NormalStmtBuilder(new Statement(stmt), flowFunctions);
	}
	
	public static EdgeBuilder.CallSiteBuilder callSite(String callSite) {
		return new EdgeBuilder.CallSiteBuilder(new Statement(callSite));
	}
	
	public static EdgeBuilder.ExitStmtBuilder exitStmt(String exitStmt) {
		return new EdgeBuilder.ExitStmtBuilder(new Statement(exitStmt));
	}
	
	public static Statement over(String callSite) {
		return new Statement(callSite);
	}
	
	public static Statement to(String returnSite) {
		return new Statement(returnSite);
	}
	
	public static ExpectedFlowFunction<TestFact> kill(String source) {
		return kill(1, source);
	}
	
	public static ExpectedFlowFunction<TestFact> kill(int times, String source) {
		return new ExpectedFlowFunction<TestFact>(times, new TestFact(source)) {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				throw new IllegalStateException();
			}
			
			@Override
			public String transformerString() {
				return "";
			}
		};
	}

	public static AccessPathTransformer readField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				return accPathHandler.read(new String(fieldName)).generate(target);
			}

			@Override
			public String toString() {
				return "read("+fieldName+")";
			}
		};
	}
	
	public static AccessPathTransformer prependField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				return accPathHandler.prepend(new String(fieldName)).generate(target);
			}
			
			@Override
			public String toString() {
				return "prepend("+fieldName+")";
			}
		};
	}
	
	public static AccessPathTransformer overwriteField(final String fieldName) {
		return new AccessPathTransformer() {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				return accPathHandler.overwrite(new String(fieldName)).generate(target);
			}
			
			@Override
			public String toString() {
				return "write("+fieldName+")";
			}
		};
	}
	
	public static ExpectedFlowFunction<TestFact> flow(String source, final AccessPathTransformer transformer, String... targets) {
		return flow(1, source, transformer, targets);
	}
	
	public static ExpectedFlowFunction<TestFact> flow(int times, String source, final AccessPathTransformer transformer, String... targets) {
		TestFact[] targetFacts = new TestFact[targets.length];
		for(int i=0; i<targets.length; i++) {
			targetFacts[i] = new TestFact(targets[i]);
		}
		return new ExpectedFlowFunction<TestFact>(times, new TestFact(source), targetFacts) {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				return transformer.apply(target, accPathHandler);
			}
			
			@Override
			public String transformerString() {
				return transformer.toString();
			}
		};
	}
	
	private static interface AccessPathTransformer {

		ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler); 
		
	}
	
	public static ExpectedFlowFunction<TestFact> flow(String source, String... targets) {
		return flow(1, source, targets);
	}
	
	public static ExpectedFlowFunction<TestFact> flow(int times, String source, String... targets) {
		return flow(times, source, new AccessPathTransformer() {
			@Override
			public ConstrainedFact<String, TestFact, Statement, TestMethod> apply(TestFact target, AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
				return accPathHandler.generate(target);
			}
			
			@Override
			public String toString() {
				return "";
			}
			
		}, targets);
	}
	
	public static int times(int times) {
		return times;
	}

	public InterproceduralCFG<Statement, TestMethod> buildIcfg() {
		return new InterproceduralCFG<Statement, TestMethod>() {

			@Override
			public boolean isStartPoint(Statement stmt) {
				return method2startPoint.values().contains(stmt);
			}

			@Override
			public boolean isFallThroughSuccessor(Statement stmt, Statement succ) {
				throw new IllegalStateException();
			}

			@Override
			public boolean isExitStmt(Statement stmt) {
				for(ReturnEdge edge : returnEdges) {
					if(edge.exitStmt.equals(stmt))
						return true;
				}
				return false;
			}

			@Override
			public boolean isCallStmt(final Statement stmt) {
				return Iterables.any(callEdges, new Predicate<CallEdge>() {
					@Override
					public boolean apply(CallEdge edge) {
						return edge.callSite.equals(stmt);
					}
				});
			}

			@Override
			public boolean isBranchTarget(Statement stmt, Statement succ) {
				throw new IllegalStateException();
			}

			@Override
			public List<Statement> getSuccsOf(Statement n) {
				LinkedList<Statement> result = Lists.newLinkedList();
				for (NormalEdge edge : normalEdges) {
					if (edge.includeInCfg && edge.unit.equals(n))
						result.add(edge.succUnit);
				}
				return result;
			}

			@Override
			public List<Statement> getPredsOf(Statement stmt) {
				LinkedList<Statement> result = Lists.newLinkedList();
				for (NormalEdge edge : normalEdges) {
					if (edge.includeInCfg && edge.succUnit.equals(stmt))
						result.add(edge.unit);
				}
				return result;
			}

			@Override
			public Collection<Statement> getStartPointsOf(TestMethod m) {
				return method2startPoint.get(m);
			}

			@Override
			public Collection<Statement> getReturnSitesOfCallAt(Statement n) {
				Set<Statement> result = Sets.newHashSet();
				for (Call2ReturnEdge edge : call2retEdges) {
					if (edge.includeInCfg && edge.callSite.equals(n))
						result.add(edge.returnSite);
				}
				for(ReturnEdge edge : returnEdges) {
					if(edge.includeInCfg && edge.callSite.equals(n))
						result.add(edge.returnSite);
				}
				return result;
			}

			@Override
			public TestMethod getMethodOf(Statement n) {
				if(stmt2method.containsKey(n))
					return stmt2method.get(n);
				else
					throw new IllegalArgumentException("Statement "+n+" is not defined in any method.");
			}

			@Override
			public Set<Statement> getCallsFromWithin(TestMethod m) {
				throw new IllegalStateException();
			}

			@Override
			public Collection<Statement> getCallersOf(TestMethod m) {
				Set<Statement> result = Sets.newHashSet();
				for (CallEdge edge : callEdges) {
					if (edge.includeInCfg && edge.destinationMethod.equals(m)) {
						result.add(edge.callSite);
					}
				}
				for (ReturnEdge edge : returnEdges) {
					if (edge.includeInCfg && edge.calleeMethod.equals(m)) {
						result.add(edge.callSite);
					}
				}
				return result;
			}

			@Override
			public Collection<TestMethod> getCalleesOfCallAt(Statement n) {
				List<TestMethod> result = Lists.newLinkedList();
				for (CallEdge edge : callEdges) {
					if (edge.includeInCfg && edge.callSite.equals(n)) {
						result.add(edge.destinationMethod);
					}
				}
				return result;
			}

			@Override
			public Set<Statement> allNonCallStartNodes() {
				throw new IllegalStateException();
			}
		};
	}

	public void assertAllFlowFunctionsUsed() {
		assertTrue("These Flow Functions were expected, but never used: \n" + Joiner.on(",\n").join(remainingFlowFunctions),
				remainingFlowFunctions.isEmpty());
	}

	private void addOrVerifyStmt2Method(Statement stmt, TestMethod m) {
		if (stmt2method.containsKey(stmt) && !stmt2method.get(stmt).equals(m)) {
			throw new IllegalArgumentException("Statement " + stmt + " is used in multiple methods: " + m + " and " + stmt2method.get(stmt));
		}
		stmt2method.put(stmt, m);
	}

	public MethodHelper method(TestMethod method) {
		MethodHelper h = new MethodHelper(method);
		return h;
	}

	public class MethodHelper {

		private TestMethod method;

		public MethodHelper(TestMethod method) {
			this.method = method;
		}

		public void edges(Collection<Edge> edges) {
			for(Edge edge : edges) {
				for(ExpectedFlowFunction<TestFact> ff : edge.flowFunctions) {
					if(!remainingFlowFunctions.contains(ff))
						remainingFlowFunctions.add(ff, ff.times);
				}
				
				edge.accept(new EdgeVisitor() {
					@Override
					public void visit(ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.exitStmt, method);
						edge.calleeMethod = method;
						returnEdges.add(edge);
					}
					
					@Override
					public void visit(Call2ReturnEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						addOrVerifyStmt2Method(edge.returnSite, method);
						call2retEdges.add(edge);
					}
					
					@Override
					public void visit(CallEdge edge) {
						addOrVerifyStmt2Method(edge.callSite, method);
						callEdges.add(edge);
					}
					
					@Override
					public void visit(NormalEdge edge) {
						addOrVerifyStmt2Method(edge.unit, method);
						addOrVerifyStmt2Method(edge.succUnit, method);
						normalEdges.add(edge);
					}
				});
			}
		}

		public void startPoints(Statement[] startingPoints) {
			method2startPoint.putAll(method, Lists.newArrayList(startingPoints));
		}
	}
	
	private static String expectedFlowFunctionsToString(ExpectedFlowFunction<TestFact>[] flowFunctions) {
		String result = "";
		for(ExpectedFlowFunction<TestFact> ff : flowFunctions)
			result += ff.source+"->"+Joiner.on(",").join(ff.targets)+ff.transformerString()+", ";
		return result;
	}
	
	private static boolean nullAwareEquals(Object a, Object b) {
		if(a == null)
			return b==null;
		else
			return a.equals(b);
	}

	public FlowFunctions<Statement, String, TestFact, TestMethod> flowFunctions() {
		return new FlowFunctions<Statement, String, TestFact, TestMethod>() {

			@Override
			public FlowFunction<String, TestFact, Statement, TestMethod> getReturnFlowFunction(Statement callSite, TestMethod calleeMethod, Statement exitStmt, Statement returnSite) {
				for (final ReturnEdge edge : returnEdges) {
					if (nullAwareEquals(callSite, edge.callSite) && edge.calleeMethod.equals(calleeMethod)
							&& edge.exitStmt.equals(exitStmt) && nullAwareEquals(edge.returnSite, returnSite)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for return edge %s -> %s (call edge: %s -> %s)", exitStmt,
						returnSite, callSite, calleeMethod));
			}

			@Override
			public FlowFunction<String, TestFact, Statement, TestMethod> getNormalFlowFunction(final Statement curr) {
				for (final NormalEdge edge : normalEdges) {
					if (edge.unit.equals(curr)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for %s", curr));
			}

			@Override
			public FlowFunction<String, TestFact, Statement, TestMethod> getCallToReturnFlowFunction(Statement callSite, Statement returnSite) {
				for (final Call2ReturnEdge edge : call2retEdges) {
					if (edge.callSite.equals(callSite) && edge.returnSite.equals(returnSite)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call to return edge %s -> %s", callSite, returnSite));
			}

			@Override
			public FlowFunction<String, TestFact, Statement, TestMethod> getCallFlowFunction(Statement callStmt, TestMethod destinationMethod) {
				for (final CallEdge edge : callEdges) {
					if (edge.callSite.equals(callStmt) && edge.destinationMethod.equals(destinationMethod)) {
						return createFlowFunction(edge);
					}
				}
				throw new AssertionError(String.format("No Flow Function expected for call %s -> %s", callStmt, destinationMethod));
			}

			private FlowFunction<String, TestFact, Statement, TestMethod> createFlowFunction(final Edge edge) {
				return new FlowFunction<String, TestFact, Statement, TestMethod>() {
					@Override
					public Set<FlowFunction.ConstrainedFact<String, TestFact, Statement, TestMethod>> computeTargets(TestFact source,
							AccessPathHandler<String, TestFact, Statement, TestMethod> accPathHandler) {
						Set<ConstrainedFact<String, TestFact, Statement, TestMethod>> result = Sets.newHashSet();
						boolean found = false;
						for (ExpectedFlowFunction<TestFact> ff : edge.flowFunctions) {
							if (ff.source.equals(source)) {
								if (remainingFlowFunctions.remove(ff)) {
									for(TestFact target : ff.targets) {
										result.add(ff.apply(target, accPathHandler));
									}
									found = true;
								} else {
									throw new AssertionError(String.format("Flow Function '%s' was used multiple times on edge '%s'", ff, edge));
								}
							}
						}
						if(found)
							return result;
						else
							throw new AssertionError(String.format("Fact '%s' was not expected at edge '%s'", source, edge));
					}
				};
			}
		};
	}

	public void runSolver(final boolean followReturnsPastSeeds, final String...initialSeeds) {
		Scheduler scheduler = new Scheduler();
		FieldSensitiveIFDSSolver<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement,TestMethod>> solver = new FieldSensitiveIFDSSolver<String ,TestFact, Statement, TestMethod, InterproceduralCFG<Statement,TestMethod>>(
				createTabulationProblem(followReturnsPastSeeds, initialSeeds), new FactMergeHandler<TestFact>() {
					@Override
					public void merge(TestFact previousFact, TestFact currentFact) {
					}

					@Override
					public void restoreCallingContext(TestFact factAtReturnSite, TestFact factAtCallSite) {
					}
					
				}, debugger, scheduler);
		addExpectationsToDebugger();
		scheduler.runAndAwaitCompletion();
		
		assertAllFlowFunctionsUsed();
	}
	
	private void addExpectationsToDebugger() {
		for(NormalEdge edge : normalEdges) {
			debugger.expectNormalFlow(edge.unit, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(CallEdge edge : callEdges) {
			debugger.expectCallFlow(edge.callSite, edge.destinationMethod, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(Call2ReturnEdge edge : call2retEdges) {
			debugger.expectNormalFlow(edge.callSite, expectedFlowFunctionsToString(edge.flowFunctions));
		}
		for(ReturnEdge edge : returnEdges) {
			debugger.expectReturnFlow(edge.exitStmt, edge.returnSite, expectedFlowFunctionsToString(edge.flowFunctions));
		}
	}

	private IFDSTabulationProblem<Statement, String, TestFact, TestMethod, InterproceduralCFG<Statement, TestMethod>> createTabulationProblem(final boolean followReturnsPastSeeds, final String[] initialSeeds) {
		final InterproceduralCFG<Statement, TestMethod> icfg = buildIcfg();
		final FlowFunctions<Statement, String, TestFact, TestMethod> flowFunctions = flowFunctions();
		
		return new IFDSTabulationProblem<Statement,String,  TestFact, TestMethod, InterproceduralCFG<Statement, TestMethod>>() {

			@Override
			public boolean followReturnsPastSeeds() {
				return followReturnsPastSeeds;
			}

			@Override
			public boolean autoAddZero() {
				return false;
			}

			@Override
			public int numThreads() {
				return 1;
			}

			@Override
			public boolean computeValues() {
				return false;
			}

			@Override
			public FlowFunctions<Statement,String,  TestFact, TestMethod> flowFunctions() {
				return flowFunctions;
			}

			@Override
			public InterproceduralCFG<Statement, TestMethod> interproceduralCFG() {
				return icfg;
			}

			@Override
			public Map<Statement, Set<TestFact>> initialSeeds() {
				Map<Statement, Set<TestFact>> result = Maps.newHashMap();
				for (String stmt : initialSeeds) {
					result.put(new Statement(stmt), Sets.newHashSet(new TestFact("0")));
				}
				return result;
			}

			@Override
			public TestFact zeroValue() {
				return new TestFact("0");
			}
			
			@Override
			public ZeroHandler<String> zeroHandler() {
				return new ZeroHandler<String>() {
					@Override
					public boolean shouldGenerateAccessPath(AccessPath<String> accPath) {
						return true;
					}
				};
			}
		};
	}
	
	public static enum TabulationProblemExchange {AsSpecified, ExchangeForwardAndBackward};
	public void runBiDiSolver(FieldSensitiveTestHelper backwardHelper, TabulationProblemExchange direction, final String...initialSeeds) {
		FactMergeHandler<TestFact> factMergeHandler = new FactMergeHandler<TestFact>() {
			@Override
			public void merge(TestFact previousFact, TestFact currentFact) {
			}

			@Override
			public void restoreCallingContext(TestFact factAtReturnSite, TestFact factAtCallSite) {
			}
			
		};
		Scheduler scheduler = new Scheduler();
		BiDiFieldSensitiveIFDSSolver<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>> solver =
				direction == TabulationProblemExchange.AsSpecified ? 
				new BiDiFieldSensitiveIFDSSolver<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>>(
						createTabulationProblem(true, initialSeeds), 
						backwardHelper.createTabulationProblem(true, initialSeeds),
						factMergeHandler, debugger, scheduler) :
				new BiDiFieldSensitiveIFDSSolver<String, TestFact, Statement, TestMethod, InterproceduralCFG<Statement, TestMethod>>(
						backwardHelper.createTabulationProblem(true, initialSeeds), 
						createTabulationProblem(true, initialSeeds),
						factMergeHandler, debugger, scheduler);
		
		scheduler.runAndAwaitCompletion();
		assertAllFlowFunctionsUsed();
		backwardHelper.assertAllFlowFunctionsUsed();
	}
}
