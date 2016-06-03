package org.k33nteam.jade.solver;

import org.k33nteam.jade.solver.model.IntentSource;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


class ForwardIntentTaintAnalysis extends ForwardFlowAnalysis<Unit, IntentTaintFact>
{
	private Body body;
	//new expr? todo
	//getIntent()? -> invoke
	//pass from param? -> initial flow
	//return-call-site? -> invoke
	public ForwardIntentTaintAnalysis(DirectedGraph<Unit> graph, Body body) {
		super(graph);
		this.body = body;
		doAnalysis();
	}

	@Override
	protected void flowThrough(IntentTaintFact in, Unit d, IntentTaintFact out) {

		Stmt stmt = (Stmt)d;
		out.copy(in);
		if (stmt instanceof JAssignStmt) {
			//$r1 = intent.getStringExtra(";;;");
			JAssignStmt assignStmt = (JAssignStmt)stmt;
			Value right = assignStmt.getRightOp();
			Value left = assignStmt.getLeftOp();
			if (right instanceof InvokeExpr) {
				InvokeExpr expr = (InvokeExpr)right;
				//invoke and return-call-site
				//getIntent()?method call?
				if (expr.getMethod().getReturnType().toString().equals("android.content.Intent")) {
					//return-call-site and getIntent
					IntentSource source = in.getIntentSourceFromMethod(expr.getMethod());
					DataObject object = DataObject.newInstance();
					object.setStatus(0x2);
					Pair<IntentSource, DataObject> pair = new Pair<>(source, object);
					out.putPair(getFieldRefWrap(left), pair);
				}
				else if (expr instanceof InstanceInvokeExpr &&
							(
									((InstanceInvokeExpr)expr).getBase().getType().toString().equals("android.content.Intent")
							
									||
									((InstanceInvokeExpr)expr).getBase().getType().toString().equals("android.os.Bundle")
									||
									((InstanceInvokeExpr)expr).getBase().getType().toString().equals("android.net.Uri")
							))
				{
					//call on intent and bundle
					InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr)expr;
					if (instanceInvokeExpr.getArgCount() <= 1) {//getAction, getStringExtra
						//System.out.println(instanceInvokeExpr);
						Pair<IntentSource,DataObject> pair = in.getPairFromValue(instanceInvokeExpr.getBase()); //get associated dataobject and intentsource
						//base may also be a field
						if (pair != null) {
							IntentSource source = pair.getO1();
							DataObject root = in.getPairFromValue(instanceInvokeExpr.getBase()).getO2();
							root = root.clone();
							DataObject object = IntentTaintFact.getRecurTail(root);
							object.setKeyAndData(expr.getMethod().getName(), DataObject.newInstance());
							//object.setStatus(0x2);
							if (instanceInvokeExpr.getArgCount() == 1) {//getStringExtra('aaa')
								Value value = instanceInvokeExpr.getArg(0);
								if (value instanceof StringConstant) {
									object.getData().setKeyAndData(((StringConstant)value).value, DataObject.newInstance());//getstringExtra, *->'key',*
								}
							}
							pair = new Pair<>(source, root);
							out.putPair(getFieldRefWrap(left), pair);//update object taint status, i.e. a = intent.getStringExtra('a'), record a => intent['a']
						}
						else {
							System.err.println("intent source not found. skip");
							System.err.println("method: " + body.getMethod());
						}
						
					}
				}
				//Intent intent = getIntent();
				//Bundle bundle = getIntent().getBundle();
				//bundle.getString
				
				//String string = getStringExtra("")
				else {
					//custom call sites?TextUtils.isEmpty .. should we consider??
					//TextUtils.isEmpty
					if  ((expr.getMethod().getName().equals("isEmpty")
							||
							expr.getMethod().getName().equals("isNullOrNil")
							||
							expr.getMethod().getName().equals("iM")
							||
							expr.getMethod().getName().equals("lm")
							)
							&& expr.getMethod().getParameterCount() == 1) {//getstringExtra, *->'key',* ->isEmpty,*
						Value value = expr.getArg(0);
						if (in.getPairFromValue(value) != null) {
							IntentSource source = in.getPairFromValue(value).getO1();
							DataObject root = in.getPairFromValue(value).getO2();
							root = root.clone();
							DataObject object = IntentTaintFact.getRecurTail(root);//FIXME
							//System.out.println(object);
							//System.out.println(object.getData());
							object.setKeyAndData(expr.getMethod().toString(), DataObject.newInstance());
							out.putPair(getFieldRefWrap(left), new Pair<>(source, root));//record isEmpty data from tainted objects
						}
					}
					else {
						out.killValue(getFieldRefWrap(left));
					}
				}
				
			}
			else if (right instanceof JCastExpr) {
				right = ((JCastExpr)right).getOp();
				out.copyValue(getFieldRefWrap(left), getFieldRefWrap(right));
			}
			else if (right instanceof Constant) {
				out.killValue(getFieldRefWrap(left));
			}
			else{
				//perform
				out.copyValue(getFieldRefWrap(left),getFieldRefWrap(right));
			}
		}
		else {
			//perserve facts?
			//copy value
		}
	}

	@Override
	protected IntentTaintFact newInitialFlow() {
		return new IntentTaintFact();
	}

	@Override
	protected IntentTaintFact entryInitialFlow() {
		IntentTaintFact fact = new IntentTaintFact();
		List<Local> params = body.getParameterLocals();
		int cnt = 0;
		for (Value local : params) {
			if (local == null) {
				continue;
			}
			if (local.getType().toString().equals("android.content.Intent")) {
				IntentSource source = fact.getIntentSourceFromParam(cnt);
				DataObject object = DataObject.newInstance();
				object.setStatus(0x2);
				Pair<IntentSource, DataObject> pair = new Pair<>(source, object);
				fact.putPair(local, pair);
				++cnt;
			}
		}
		
		//check for fields
		Set<SootField> travelled = new HashSet<>(); //FIXME: better option than record using a set?
		for (Unit unit : body.getUnits()) {
			Stmt stmt = (Stmt)unit;
			if (stmt.containsFieldRef()) {
				FieldRef fieldRef = stmt.getFieldRef();
				SootField sootField = fieldRef.getField();
				if (travelled.contains(sootField) || !(fieldRef.getField().getType() instanceof RefType)) {
					continue;
				}
				RefType refType = (RefType) fieldRef.getField().getType();
				if(!refType.getSootClass().getName().equals("android.content.Intent")){//only consider intent
					continue;
				}
				//assume brand new databoject
				IntentSource source = fact.getIntentSourceFromField(sootField);
				DataObject object = DataObject.newInstance();
				object.setStatus(0x2);
				Pair<IntentSource, DataObject> pair = new Pair<>(source, object);
				fact.putPair(getFieldRefWrap(fieldRef), pair);
				++cnt;
				travelled.add(sootField);
			}
		}
		return fact;
	}

	@Override
	protected void merge(IntentTaintFact in1, IntentTaintFact in2,
			IntentTaintFact out) {
		out.clear();
		out.merge(in1);
		out.merge(in2);
	}

	@Override
	protected void copy(IntentTaintFact source, IntentTaintFact dest) {
		dest.copy(source);
	}
	
	private static Value getFieldRefWrap(Value value) {
		if (value instanceof FieldRef) {
			return new EquivalentValue(value);
		} else {
			return value;
		}
	}
	
}