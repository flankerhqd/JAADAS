package org.k33nteam;

import soot.*;
import soot.dava.internal.javaRep.DIntConstant;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.Pair;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConnectCallbackTransformer extends BodyTransformer{
	private final Set<String> androidCallbacks;
	public ConnectCallbackTransformer() throws IOException
	{
		androidCallbacks = loadAndroidCallbacks();
	}
	private Set<String> loadAndroidCallbacks() throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		BufferedReader rdr = null;
		try {
			String fileName = JadeCfg.getCallback_file();
			rdr = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = rdr.readLine()) != null)
				if (!line.isEmpty())
					androidCallbacks.add(line);
		}
		finally {
			if (rdr != null)
				rdr.close();
		}
		return androidCallbacks;
	}
	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}
	@Override
	protected void internalTransform(Body b, String phaseName,
			Map<String, String> options) {
		//System.out.println("fucking transform");
		SootMethod method = b.getMethod();
		//System.out.println(method);
		// Do not analyze system classes
		if (method.getDeclaringClass().getName().startsWith("android.")
				|| method.getDeclaringClass().getName().startsWith("java."))
			return;
		if (!method.isConcrete())
			return;
		
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(method.retrieveActiveBody());
		SmartLocalDefs smd = new SmartLocalDefs(graph, new SimpleLiveLocals(graph));
		List<Pair<Pair<Local,Local>,Pair<SootClass, Stmt>>> worklist = new ArrayList<>();

		// Iterate over all statement and find callback registration methods, i.e. setOnClickListener
		for (Unit u : method.retrieveActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			// Callback registrations are always instance invoke expressions
			if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				for (int i = 0; i < iinv.getArgCount(); i++) {
					Value arg = iinv.getArg(i);//each arg is a "listener"
					Type argType = iinv.getArg(i).getType();
					Type paramType = iinv.getMethod().getParameterType(i);
					if (paramType instanceof RefType && argType instanceof RefType) {
						if (androidCallbacks.contains(((RefType) paramType).getSootClass().getName())) {
							// We have a formal parameter type that corresponds to one of the Android
							// callback interfaces. Look for definitions of the parameter to estimate
							// the actual type.
							if (arg instanceof Local)
								for (Unit def : smd.getDefsOfAt((Local) arg, u)) {
									assert def instanceof DefinitionStmt; 
									Type tp = ((DefinitionStmt) def).getRightOp().getType();
									if (tp instanceof RefType) {
										SootClass callbackClass = ((RefType) tp).getSootClass();
										if (callbackClass.isInterface())
											for (SootClass impl : Scene.v().getActiveHierarchy().getImplementersOf(callbackClass))
												for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(impl))
												{
													worklist.add(new Pair<>(new Pair<>((Local) arg, (Local)iinv.getBase()),new Pair<>(c, stmt)));//record all callback interfaces impl
												}
										else
											for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(callbackClass))
											{
												worklist.add(new Pair<>(new Pair<>((Local) arg, (Local)iinv.getBase()),new Pair<>(c, stmt)));
											}
									}
								}
						}
					}
				}
			}
		}
		

		
		//attention: some callback methods have arguments, some not, we have to infer arguments. forexample: in aliexpresshd:
		//now body b: <com.alibaba.aliexpresshd.ProductListActivity: void onActivityResult(int,int,android.content.Intent)>
		//        specialinvoke $r2.<com.alibaba.aliexpresshd.ProductListActivity$3: void <init>(com.alibaba.aliexpresshd.ProductListActivity,java.lang.String,java.lang.String)>($r0, $r3, $r4);

		//virtualinvoke $r5.<android.os.MessageQueue: void addIdleHandler(android.os.MessageQueue$IdleHandler)>($r2);

		//virtualinvoke $r2.<com.alibaba.aliexpresshd.ProductListActivity$3: boolean queueIdle()>($r5); => should not have arg.


		// Analyze all found callback classes
		for (Pair<Pair<Local,Local>,   Pair<SootClass, Stmt>> pair: worklist) {
			SootClass sootClass = pair.getO2().getO1();
			if (sootClass.getName().startsWith("android.")
					|| sootClass.getName().startsWith("java."))
				continue;
			for (SootClass i : collectAllInterfaces(pair.getO2().getO1())) {
				
				if (androidCallbacks.contains(i.getName()))
					for (SootMethod sm : i.getMethods()){
						try{
							System.out.println("checking sm: " + sm);
							SootMethod realmethod = getMethodFromHierarchyEx(pair.getO2().getO1(), sm.getSubSignature());
							InvokeExpr invokeExpr;
							if(realmethod.getParameterCount() == 1)
							{
								//add invoke stmt.
								//pair.getO2 => setOnclickListener($r2) => $r2.onClick()
								invokeExpr = Jimple.v().newVirtualInvokeExpr(pair.getO1().getO1(), realmethod.makeRef(),Collections.<Value>singletonList(pair.getO1().getO2()));
							}
							else if(realmethod.getParameterCount() == 0)
							{
								invokeExpr = Jimple.v().newVirtualInvokeExpr(pair.getO1().getO1(), realmethod.makeRef(), Collections.<Value>emptyList());
							}
							else
							{
								//debug purpose, argument >= 2, add all other arguments with null
								List<Value> args = new ArrayList<>();
								args.add(pair.getO1().getO2());
								for(int j=1;j<realmethod.getParameterCount(); j++)
								{
									if(isSimpleType(realmethod.getParameterType(j).toString()))
									{
										args.add(getSimpleDefaultValue(realmethod.getParameterType(j).toString()));
									}
									else
									{
										args.add(NullConstant.v());
									}
								}
								//System.out.println("adding callback: " + realmethod);
								invokeExpr = Jimple.v().newVirtualInvokeExpr(pair.getO1().getO1(), realmethod.makeRef(), args);
							}
							Stmt stmt = Jimple.v().newInvokeStmt(invokeExpr);
							//System.out.println("inserting stmt: " + stmt);
							b.getUnits().insertAfter(stmt, pair.getO2().getO2());
						}
						catch (RuntimeException e)
						{
							//refer to issue #11, sometimes we could not found the actual impl of the callback method, so ignore it
							System.err.println("runtimeException - callback method not found?");
							e.printStackTrace();
						}
					}
			}
		}
		//System.out.println("now body b: " + b.getMethod());
		//System.out.println(b);
	}


	private Type getSimpleTypeFromType(Type type) {
		if (type.toString().equals("java.lang.String")) {
			assert type instanceof RefType;
			return RefType.v(((RefType) type).getSootClass());
		}
		if (type.toString().equals("void"))
			return soot.VoidType.v();
		if (type.toString().equals("char"))
			return soot.CharType.v();
		if (type.toString().equals("byte"))
			return soot.ByteType.v();
		if (type.toString().equals("short"))
			return soot.ShortType.v();
		if (type.toString().equals("int"))
			return soot.IntType.v();
		if (type.toString().equals("float"))
			return soot.FloatType.v();
		if (type.toString().equals("long"))
			return soot.LongType.v();
		if (type.toString().equals("double"))
			return soot.DoubleType.v();
		if (type.toString().equals("boolean"))
			return soot.BooleanType.v();
		throw new RuntimeException("Unknown simple type: " + type);
	}

	protected static boolean isSimpleType(String t) {
		if (t.equals("java.lang.String")
				|| t.equals("void")
				|| t.equals("char")
				|| t.equals("byte")
				|| t.equals("short")
				|| t.equals("int")
				|| t.equals("float")
				|| t.equals("long")
				|| t.equals("double")
				|| t.equals("boolean")) {
			return true;
		} else {
			return false;
		}
	}

	protected Value getSimpleDefaultValue(String t) {
		if (t.equals("java.lang.String"))
			return StringConstant.v("");
		if (t.equals("char"))
			return DIntConstant.v(0, CharType.v());
		if (t.equals("byte"))
			return DIntConstant.v(0, ByteType.v());
		if (t.equals("short"))
			return DIntConstant.v(0, ShortType.v());
		if (t.equals("int"))
			return IntConstant.v(0);
		if (t.equals("float"))
			return FloatConstant.v(0);
		if (t.equals("long"))
			return LongConstant.v(0);
		if (t.equals("double"))
			return DoubleConstant.v(0);
		if (t.equals("boolean"))
			return DIntConstant.v(0, BooleanType.v());

		//also for arrays etc.
		return G.v().soot_jimple_NullConstant();
	}

	private SootMethod getMethodFromHierarchyEx(SootClass c, String methodSignature) {
		if (c.declaresMethod(methodSignature))
			return c.getMethod(methodSignature);
		if (c.hasSuperclass())
			return getMethodFromHierarchyEx(c.getSuperclass(), methodSignature);
		throw new RuntimeException("Could not find method");
	}
}
