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
package soot.jimple.infoflow.android;

import org.k33nteam.JadeCfg;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Analyzes the classes in the APK file to find custom implementations of the
 * well-known Android callback and handler interfaces.
 * 
 * @author Steven Arzt
 *
 */
public class AnalyzeJimpleClass {

	private final Set<String> entryPointClasses;
	private final Set<String> androidCallbacks;
	
	private final Map<String, Set<SootMethodAndClass>> callbackMethods =
			new HashMap<String, Set<SootMethodAndClass>>();
	private final Map<String, Set<SootMethodAndClass>> callbackWorklist =
			new HashMap<String, Set<SootMethodAndClass>>();
	private final Map<String, Set<Integer>> layoutClasses =
			new HashMap<String, Set<Integer>>();
	private final Set<String> dynamicManifestComponents =
			new HashSet<>();

	public AnalyzeJimpleClass(Set<String> entryPointClasses) throws IOException {
		this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = loadAndroidCallbacks();
	}

	public AnalyzeJimpleClass(Set<String> entryPointClasses,
			Set<String> androidCallbacks) {
		this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = new HashSet<String>();
	}

	/**
	 * Loads the set of interfaces that are used to implement Android callback
	 * handlers from a file on disk
	 * @return A set containing the names of the interfaces that are used to
	 * implement Android callback handlers
	 */
	private Set<String> loadAndroidCallbacks() throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		BufferedReader rdr = null;
		try {
			String fileName = JadeCfg.getCallback_file();
			if (!new File(fileName).exists()) {
				fileName = "../soot-infoflow-android/AndroidCallbacks.txt";
				if (!new File(fileName).exists())
					throw new RuntimeException("Callback definition file not found");
			}
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

	/**
	 * Collects the callback methods for all Android default handlers
	 * implemented in the source code.
	 * Note that this operation runs inside Soot, so this method only registers
	 * a new phase that will be executed when Soot is next run
	 */
	public void collectCallbackMethods() {
		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Find the mappings between classes and layouts
				findClassLayoutMappings();

				// Process the callback classes directly reachable from the
				// entry points
				for (String className : entryPointClasses) {
					SootClass sc = Scene.v().getSootClass(className);
					List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>();
					methods.addAll(sc.getMethods());
					
					// Check for callbacks registered in the code
					analyzeRechableMethods(sc, methods);

					// Check for method overrides
					analyzeMethodOverrideCallbacks(sc);
				}
				System.out.println("Callback analysis done.");
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}
	
	/**
	 * Incrementally collects the callback methods for all Android default
	 * handlers implemented in the source code. This just processes the contents
	 * of the worklist.
	 * Note that this operation runs inside Soot, so this method only registers
	 * a new phase that will be executed when Soot is next run
	 */
	public void collectCallbackMethodsIncremental() {
		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Process the worklist from last time
				System.out.println("Running incremental callback analysis for " + callbackWorklist.size()
						+ " components...");
				MultiMap<String, SootMethodAndClass> workListCopy =
						new HashMultiMap<String, SootMethodAndClass>(callbackWorklist);
				for (String className : workListCopy.keySet()) {
					List<MethodOrMethodContext> entryClasses = new LinkedList<MethodOrMethodContext>();
					for (SootMethodAndClass am : workListCopy.get(className))
						entryClasses.add(Scene.v().getMethod(am.getSignature()));
					analyzeRechableMethods(Scene.v().getSootClass(className), entryClasses);
					callbackWorklist.remove(className);
				}
				System.out.println("Incremental callback analysis done.");
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}

	private void analyzeRechableMethods(SootClass lifecycleElement, List<MethodOrMethodContext> methods) {
		ReachableMethods rm = new ReachableMethods(Scene.v().getCallGraph(), methods);
		rm.update();

		// Scan for listeners in the class hierarchy
		Iterator<MethodOrMethodContext> reachableMethods = rm.listener();
		while (reachableMethods.hasNext()) {
			SootMethod method = reachableMethods.next().method();
			analyzeMethodForCallbackRegistrations(lifecycleElement, method);
			analyzeMethodForDynamicBroadcastReceiver(method);
		}
	}

	/**
	 * Analyzes the given method and looks for callback registrations
	 * @param lifecycleElement The lifecycle element (activity, etc.) with which
	 * to associate the found callbacks
	 * @param method The method in which to look for callbacks
	 */
	private void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method) {
		// Do not analyze system classes
		if (method.getDeclaringClass().getName().startsWith("android.")
				|| method.getDeclaringClass().getName().startsWith("java."))
			return;
		if (!method.isConcrete())
			return;
		
		ExceptionalUnitGraph graph = new ExceptionalUnitGraph(method.retrieveActiveBody());
		SmartLocalDefs smd = new SmartLocalDefs(graph, new SimpleLiveLocals(graph));

		// Iterate over all statement and find callback registration methods
		Set<SootClass> callbackClasses = new HashSet<SootClass>();
		for (Unit u : method.retrieveActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			// Callback registrations are always instance invoke expressions
			if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				
				String[] parameters = SootMethodRepresentationParser.v().getParameterTypesFromSubSignature(
						iinv.getMethodRef().getSubSignature().getString());
				for (int i = 0; i < parameters.length; i++) {
					String param = parameters[i];
					if (androidCallbacks.contains(param)) {
						Value arg = iinv.getArg(i);
						
						// We have a formal parameter type that corresponds to one of the Android
						// callback interfaces. Look for definitions of the parameter to estimate
						// the actual type.
						if (arg.getType() instanceof RefType && arg instanceof Local)
							for (Unit def : smd.getDefsOfAt((Local) arg, u)) {
								assert def instanceof DefinitionStmt; 
								Type tp = ((DefinitionStmt) def).getRightOp().getType();
								if (tp instanceof RefType) {
									SootClass callbackClass = ((RefType) tp).getSootClass();
									if (callbackClass.isInterface())
										for (SootClass impl : Scene.v().getActiveHierarchy().getImplementersOf(callbackClass))
											for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(impl))
												callbackClasses.add(c);
									else
										for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(callbackClass))
											callbackClasses.add(c);
								}
							}
					}
				}
			}
		}
		
		// Analyze all found callback classes
		for (SootClass callbackClass : callbackClasses)
			analyzeClass(callbackClass, lifecycleElement);
	}
	
	/**
	 * Checks whether the given method dynamically registers a new broadcast
	 * receiver
	 * @param method The method to check
	 */
	private void analyzeMethodForDynamicBroadcastReceiver(SootMethod method) {
		if (!method.isConcrete() || !method.hasActiveBody())
			return;
		// stmt.getInvokeExpr().getMethod().getDeclaringClass().getName().equals("android.content.Context")
		
		for (Unit u : method.getActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			if (stmt.containsInvokeExpr()) {
				if (stmt.getInvokeExpr().getMethod().getName().equals("registerReceiver")
						&& stmt.getInvokeExpr().getArgCount() > 0
						&& isInheritedMethod(stmt, "android.content.ContextWrapper",
								"android.content.Context")) {
					Value br = stmt.getInvokeExpr().getArg(0);
					if (br.getType() instanceof RefType) {
						RefType rt = (RefType) br.getType();
						dynamicManifestComponents.add(rt.getClassName());
					}
				}
			}
		}
	}

	private boolean isInheritedMethod(Stmt stmt, String... classNames) {
		Iterator<Edge> edgeIt = Scene.v().getCallGraph().edgesOutOf(stmt);
		while (edgeIt.hasNext()) {
			Edge edge = edgeIt.next();
			String targetClass = edge.getTgt().method().getDeclaringClass().getName();
			for (String className : classNames)
				if (className.equals(targetClass))
					return true;
		}
		return false;
	}

	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
		Iterator<MethodOrMethodContext> rmIterator = Scene.v().getReachableMethods().listener();
		while (rmIterator.hasNext()) {
			SootMethod sm = rmIterator.next().method();
			if (!sm.isConcrete())
				continue;
			
			for (Unit u : sm.retrieveActiveBody().getUnits())
				if (u instanceof Stmt) {
					Stmt stmt = (Stmt) u;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr inv = stmt.getInvokeExpr();
						if (invokesSetContentView(inv)) {
							for (Value val : inv.getArgs())
								if (val instanceof IntConstant) {
									IntConstant constVal = (IntConstant) val;
									Set<Integer> layoutIDs = this.layoutClasses.get(sm.getDeclaringClass().getName());
									if (layoutIDs == null) {
										layoutIDs = new HashSet<Integer>();
										this.layoutClasses.put(sm.getDeclaringClass().getName(), layoutIDs);
									}
									layoutIDs.add(constVal.value);
								}
						}
					}
				}
		}
	}
	
	/**
	 * Checks whether this invocation calls Android's Activity.setContentView
	 * method
	 * @param inv The invocaton to check
	 * @return True if this invocation calls setContentView, otherwise false
	 */
	private boolean invokesSetContentView(InvokeExpr inv) {
		String methodName = SootMethodRepresentationParser.v().getMethodNameFromSubSignature(
				inv.getMethodRef().getSubSignature().getString());
		if (!methodName.equals("setContentView"))
			return false;
		
		// In some cases, the bytecode points the invocation to the current
		// class even though it does not implement setContentView, instead
		// of using the superclass signature
		SootClass curClass = inv.getMethod().getDeclaringClass();
		while (curClass != null) {
			if (curClass.getName().equals("android.app.Activity")
					|| curClass.getName().equals("android.support.v7.app.ActionBarActivity"))
				return true;
			if (curClass.declaresMethod("void setContentView(int)"))
				return false;
			curClass = curClass.hasSuperclass() ? curClass.getSuperclass() : null;
		}
		return false;
	}

	/**
	 * Analyzes the given class to find callback methods
	 * @param sootClass The class to analyze
	 * @param lifecycleElement The lifecycle element (activity, service, etc.)
	 * to which the callback methods belong
	 */
	private void analyzeClass(SootClass sootClass, SootClass lifecycleElement) {
		// Do not analyze system classes
		if (sootClass.getName().startsWith("android.")
				|| sootClass.getName().startsWith("java."))
			return;
		
		// Check for callback handlers implemented via interfaces
		analyzeClassInterfaceCallbacks(sootClass, sootClass, lifecycleElement);
	}
	
	private void analyzeMethodOverrideCallbacks(SootClass sootClass) {
		if (!sootClass.isConcrete())
			return;
		if (sootClass.isInterface())
			return;
		
		// Do not start the search in system classes
		if (sootClass.getName().startsWith("android."))
			return;
		
		// There are also some classes that implement interesting callback methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class, we treat it as a potential callback.
		Set<String> systemMethods = new HashSet<String>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
			if (parentClass.getName().startsWith("android."))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.add(sm.getSubSignature());
		}
		
		// Iterate over all user-implemented methods. If they are inherited
		// from a system class, they are callback candidates.
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass)) {
			if (parentClass.getName().startsWith("android."))
				continue;
			for (SootMethod method : parentClass.getMethods()) {
				if (!systemMethods.contains(method.getSubSignature()))
					continue;

				// This is a real callback method
				checkAndAddMethod(method, sootClass);
			}
		}
	}
	
	private SootMethod getMethodFromHierarchyEx(SootClass c, String methodSignature) {
		if (c.declaresMethod(methodSignature))
			return c.getMethod(methodSignature);
		if (c.hasSuperclass())
			return getMethodFromHierarchyEx(c.getSuperclass(), methodSignature);
		throw new RuntimeException("Could not find method");
	}

	private void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass,
			SootClass lifecycleElement) {
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (!baseClass.isConcrete())
			return;
		
		// For a first take, we consider all classes in the android.* packages
		// to be part of the operating system
		if (baseClass.getName().startsWith("android."))
			return;
		
		// If we are a class, one of our superclasses might implement an Android
		// interface
		if (sootClass.hasSuperclass())
			analyzeClassInterfaceCallbacks(baseClass, sootClass.getSuperclass(), lifecycleElement);
		
		// Do we implement one of the well-known interfaces?
		for (SootClass i : collectAllInterfaces(sootClass)) {
			if (androidCallbacks.contains(i.getName()))
				for (SootMethod sm : i.getMethods())
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass,
							sm.getSubSignature()), lifecycleElement);
		}
	}

	/**
	 * Checks whether the given Soot method comes from a system class. If not,
	 * it is added to the list of callback methods.
	 * @param method The method to check and add
	 * @param baseClass The base class (activity, service, etc.) to which this
	 * callback method belongs
	 */
	private void checkAndAddMethod(SootMethod method, SootClass baseClass) {
		AndroidMethod am = new AndroidMethod(method);
		
		// Do not call system methods
		if (am.getClassName().startsWith("android.")
				|| am.getClassName().startsWith("java."))
			return;

		// Skip empty methods
		if (method.isConcrete() && isEmpty(method.retrieveActiveBody()))
			return;
			
		boolean isNew;
		if (this.callbackMethods.containsKey(baseClass.getName()))
			isNew = this.callbackMethods.get(baseClass.getName()).add(am);
		else {
			Set<SootMethodAndClass> methods = new HashSet<SootMethodAndClass>();
			isNew = methods.add(am);
			this.callbackMethods.put(baseClass.getName(), methods);
		}
			
		if (isNew)
			if (this.callbackWorklist.containsKey(baseClass.getName()))
					this.callbackWorklist.get(baseClass.getName()).add(am);
			else {
				Set<SootMethodAndClass> methods = new HashSet<SootMethodAndClass>();
				isNew = methods.add(am);
				this.callbackWorklist.put(baseClass.getName(), methods);
			}
	}

	private boolean isEmpty(Body activeBody) {
		for (Unit u : activeBody.getUnits())
			if (!(u instanceof IdentityStmt || u instanceof ReturnVoidStmt))
				return false;
		return true;
	}

	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}
	
	public Map<String, Set<SootMethodAndClass>> getCallbackMethods() {
		return this.callbackMethods;
	}
	
	public Map<String, Set<Integer>> getLayoutClasses() {
		return this.layoutClasses;
	}
	
	public Set<String> getDynamicManifestComponents() {
		return this.dynamicManifestComponents;
	}
		
}
