package org.k33nteam.jade.solver;


import com.bpodgursky.jbool_expressions.*;
import org.k33nteam.jade.solver.model.IntentSource;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JNeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.Pair;

import java.util.*;

public class NPEDetector {

	private static int processExpression(Expression expression, ForwardIntentTaintAnalysis taintAnalysis, Pair<IntentSource, DataObject> query)
	{
		//System.out.println("entrying ...-----");
		//System.out.println(expression);
		if (expression instanceof Variable || expression instanceof Not) {
			//System.out.println(ret);
			//System.out.println("exiting from variable or Not ...-----");
			return evaluateSingleExpressionMayNPE(expression, taintAnalysis, query);
		}
		else if(expression instanceof Literal)
		{
			int ret = ((Literal)expression).getValue()?1:0;
			//System.out.println("current ret...-----"+ret);
			DataObject object = query.getO2();
			//System.out.println("current status...-----"+object.status);
			if (object.status == 0x2) {
				ret = 0;
			}
			else if (object.status == 0x1) {
				ret = 1;
			}
			//System.out.println("exiting from literal ...-----");
			//System.out.println(ret);
			return ret;
		}
		else if (expression instanceof NExpression) {
			NExpression nExpression = (NExpression)expression;
			if (nExpression instanceof And) {
				boolean tmp = true, modified = false;
				DataObject object = query.getO2();
				if (object.status == 0x2) {
					tmp = false;
				}
				else if (object.status == 0x1) {
					tmp = true;
				}
				for (int i = 0; i < nExpression.expressions.length; i++) {
					int tmpvar = processExpression(((NExpression) expression).expressions[i], taintAnalysis, query);
					if (tmpvar != -1) {
						tmp  = tmp & (tmpvar == 1);
						modified = true;
					}
				}
				//System.out.println("exiting from And exps ...-----");
				return modified?(tmp?1:0):-1;
			}
			else if (nExpression instanceof Or) {
				boolean tmp = false, modified = false;
				DataObject object = query.getO2();
				if (object.status == 0x2) {
					tmp = false;
				}
				else if (object.status == 0x1) {
					tmp = true;
				}
				for (int i = 0; i < nExpression.expressions.length; i++) {
					int tmpvar = processExpression(nExpression.expressions[i], taintAnalysis, query);
					if (tmpvar != -1) {
						tmp  = tmp | (tmpvar == 1);
						modified = true;
					}	
				}
				//System.out.println("exiting from Or exps ...-----");
				return modified?(tmp?1:0):-1;
			}
			else {
				//wtf?
				throw new RuntimeException();
			}
		}
		else {
			//wtf?
			throw new RuntimeException();
		}
	}
	public static List<List<Pair<Stmt,SootMethod>>> checkPotentialNPE(SootMethod method)
	{
		List<Pair<Stmt, SootMethod>> npeList = new ArrayList<>();
		List<Pair<Stmt, SootMethod>> indexOOBList = new ArrayList<>();

		IntentTaintFact.reset();
		Body body = method.getActiveBody();
		BriefUnitGraph unitGraph = new BriefUnitGraph(body);
		ForwardIntentTaintAnalysis taintAnalysis = new ForwardIntentTaintAnalysis(unitGraph, body);
		LocalConstraintFlowAnalysis constraintFlowAnalysis = new LocalConstraintFlowAnalysis(unitGraph, true);

		for(Unit unit: body.getUnits())
		{
			Stmt stmt = (Stmt)unit;
			if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) stmt.getInvokeExpr();
				Value value = instanceInvokeExpr.getBase();
				IntentTaintFact fact = taintAnalysis.getFlowBefore(stmt);
				if (fact.getPairFromValue(value) != null) {// value is from intent
					ConstrainInfo info = constraintFlowAnalysis.getFlowBefore(stmt);
					Pair<IntentSource, DataObject> query = fact.getPairFromValue(value);
					
					Expression expression = info.expression;
					int ret = processExpression(expression, taintAnalysis, query);
					if(ret == 1)
					{
						npeList.add(new Pair<>(stmt, body.getMethod()));
						/*CallGraph callGraph = Scene.v().getCallGraph();
						
						System.out.println("getting calls");
						Iterator<Edge> iterator = callGraph.edgesInto(body.getMethod());
						while (iterator.hasNext()) {
							System.out.println("level 1...");
							Edge edge = iterator.next();
							System.out.println(edge.getSrc());
							Iterator<Edge> iterator1 = callGraph.edgesInto(edge.getSrc());
							while (iterator1.hasNext()) {
								System.out.println("level 2...");
								System.out.println(iterator1.next().getSrc());
							}
						}*/
					}

					//now check for arraylist get
					if(instanceInvokeExpr.getMethod().getName().equals("get") && instanceInvokeExpr.getArgCount() == 1)
					{
						//TODO: constraint check
						indexOOBList.add(new Pair<>(stmt, body.getMethod()));
					}
				}
			}
			//process indexoutofbound

			else if(stmt.containsArrayRef())
			{
				ArrayRef arrayRef = stmt.getArrayRef();
				Value arrayBase = arrayRef.getBase();
				IntentTaintFact fact = taintAnalysis.getFlowBefore(stmt);
				if(fact.getPairFromValue(arrayBase) != null)
				{
					//TODO: constraint check
					indexOOBList.add(new Pair<>(stmt, body.getMethod()));
				}
			}


		}
		return Arrays.asList(npeList,indexOOBList);
	}



	private static int compareDataObj(DataObject src, DataObject dest)//src is current recorded, status, dest is to query
	{
		//if query longer than src, that's "true", may NPE
		
		DataObject lptr = src, rptr = dest;
		while(rptr != null)
		{
			if (lptr == null) {
				return 1;
			}
			if (rptr.status == 0x2) {
				return 0;
			}
			if (Objects.equals(lptr.key, rptr.key) || (lptr.key != null && lptr.key.equals(rptr.key))) {
				if (lptr.status == 0x2) {//not_empty, 
					return 0;
				}
				else if (lptr.status == 0x1) {//empty
					return 1;
				}
				else {
					//??not empty, src longer than dest? src shorter than dest?
					//go ahead
					rptr = rptr.getData();
					lptr = lptr.getData();
				}
			}
			else {
				//dest.key not found, no information
				return 1;
			}
		}
		//rptr end
		return 0;
	}
	public static int evaluateSingleExpressionMayNPE( Expression expression, ForwardIntentTaintAnalysis analysis, Pair<IntentSource, DataObject> currentInst)
	{
		DataObject query = currentInst.getO2();
		if (expression instanceof Variable) {
			Variable variable = (Variable)expression;
			IfStmt stmt = (IfStmt) variable.getValue();
			ConditionExpr expr = (ConditionExpr) stmt.getCondition();
			IntentTaintFact fact = analysis.getFlowBefore(stmt);
			Value mid, left = expr.getOp1(), right = expr.getOp2();
			if (fact.getPairFromValue(left) == null) {
				return -1;
			}
			if (expr.getOp1() == NullConstant.v() || expr.getOp2() == NullConstant.v()) {
				if (expr.getOp1() == NullConstant.v()) {
					mid = right;
					right = left;
					left = mid;
				}
				
				if (fact.getPairFromValue(left) != null) {
					Pair<IntentSource, DataObject> pair = fact.getPairFromValue(left);
					if (pair.getO1().equals(currentInst.getO1())) {
						DataObject accessData = pair.getO2();
						accessData = accessData.clone();
						DataObject root = accessData;
						DataObject tail = DataObject.getRecurTail(accessData);
						if (expr instanceof JNeExpr) {
							// $r1 != null, branch to true, true-> not null
							tail.setStatus(0x2);
						}
						else if (expr instanceof JEqExpr) {//== null , branch true, null
							tail.setStatus(0x1);//clear
						}
						return compareDataObj(root, query);
					}
					
				}
			}
			else if (expr.getOp1() instanceof IntConstant || expr.getOp2() instanceof IntConstant) {
				if (expr.getOp1() instanceof IntConstant) {
					mid = right;
					right = left;
					left = mid;
				}
				
				if (fact.getPairFromValue(left) != null) {
					Pair<IntentSource, DataObject> pair = fact.getPairFromValue(left);
					if (pair.getO1().equals(currentInst.getO1())) {
						DataObject accessData = pair.getO2();
						accessData = accessData.clone();
						DataObject root = accessData;
						accessData = DataObject.getRecurThirdTail(root);
						//FIXME: ju.android NPE exception
						if (accessData == null) {
							return -1;
						}
						
						if (expr instanceof JNeExpr) {
							if (((IntConstant)right).value == 0) {
								//eval to 1, for textutils.isempty -> empty->true->1 => null eval to true, branch true
								accessData.setStatus(0x1);
							}
							else if (((IntConstant)right).value == 1) {
								accessData.setStatus(0x2);
							}
						}
						else if (expr instanceof JEqExpr) {
							if (((IntConstant)right).value == 0) {
								//eval to 1, for textutils.isempty -> empty->true->1 => null eval to false, branch false
								accessData.setStatus(0x2);
							}
							else if (((IntConstant)right).value == 1) {
								accessData.setStatus(0x1);
							}
						}
						return compareDataObj(root, query);
					}
				}
			}
			return -1;
		}
		else if (expression instanceof Not) {
			Not not = (Not)expression;
			Expression expression2 = not.getE();
			if (expression2 instanceof Variable) {
				Variable variable = (Variable)expression2;
				IfStmt stmt = (IfStmt) variable.getValue();
				ConditionExpr expr = (ConditionExpr) stmt.getCondition();
				Value mid, left = expr.getOp1(), right = expr.getOp2();
				IntentTaintFact fact = analysis.getFlowBefore(stmt);
				if (fact.getPairFromValue(left) == null) {
					return -1;
				}
				if (expr.getOp1() == NullConstant.v() || expr.getOp2() == NullConstant.v()) {
					if (expr.getOp1() == NullConstant.v()) {
						mid = right;
						right = left;
						left = mid;
					}
					
					if (fact.getPairFromValue(left) != null) {
						Pair<IntentSource, DataObject> pair = fact.getPairFromValue(left);
						if (pair.getO1().equals(currentInst.getO1())) {
							DataObject accessData = pair.getO2();
							accessData = accessData.clone();

							DataObject root = accessData;
							DataObject tail = DataObject.getRecurTail(accessData);
							if (expr instanceof JNeExpr) {
								// $r1 != null, branch to true, true-> not null
								tail.setStatus(0x1);
							}
							else if (expr instanceof JEqExpr) {//== null , branch true, null, ! means not-null, branch,true=>null, fall,false=>not-null
								tail.setStatus(0x2);
							}
							return compareDataObj(root, query);
						}
					}
				}
				else if (expr.getOp1() instanceof IntConstant || expr.getOp2() instanceof IntConstant) {
					if (expr.getOp1() instanceof IntConstant) {
						mid = right;
						right = left;
						left = mid;
					}
					
					if (fact.getPairFromValue(left) != null) {
						Pair<IntentSource, DataObject> pair = fact.getPairFromValue(left);
						if (pair.getO1().equals(currentInst.getO1())) {
							DataObject accessData = pair.getO2();
							accessData = accessData.clone();
							DataObject root = accessData;
							accessData = DataObject.getRecurThirdTail(root);
							//FIXME: NPE on com.taobao.ju.android
							if (accessData == null) {
								return -1;
							}
							if (expr instanceof JNeExpr) {
								if (((IntConstant)right).value == 0) {
									//eval to 1, for textutils.isempty -> empty->true->1 => null eval to true, branch true
									accessData.setStatus(0x2);
								}
								else if (((IntConstant)right).value == 1) {
									accessData.setStatus(0x1);
								}
							}
							else if (expr instanceof JEqExpr) {
								if (((IntConstant)right).value == 0) {
									//eval to 1, for textutils.isempty -> empty->true->1 => null eval to false, branch false
									accessData.setStatus(0x1);
								}
								else if (((IntConstant)right).value == 1) {
									accessData.setStatus(0x2);
								}
							}
							return compareDataObj(root, query);
						}
					}
				}
			}
			return -1;
		}
		System.out.println(expression.getClass());
		//throw new RuntimeException();//wtf
		return -1;
	}
	
}
