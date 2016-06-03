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
package soot.jimple.infoflow.entryPointCreators;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.ArrayType;
import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.dava.internal.javaRep.DIntConstant;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.AssignStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

/**
 * Common base class for all entry point creators. Implementors must override the
 * createDummyMainInternal method to provide their entry point implementation.
 */
public abstract class BaseEntryPointCreator implements IEntryPointCreator {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected Map<String, Local> localVarsForClasses = new HashMap<String, Local>();
	private final Set<SootClass> failedClasses = new HashSet<SootClass>();
	private boolean substituteCallParams = false;
	private List<String> substituteClasses;

	private final Set<SootMethod> failedMethods = new HashSet<>();
	
	protected final String DUMMY_CLASS_NAME = "dummyMainClass";
	protected final String DUMMY_METHOD_NAME = "dummyMainMethod";
	
	/**
	 * Returns a copy of all classes that could not be instantiated properly
	 * @return The classes where the constructor could not be generated
	 */
	public Set<SootClass> getFailedClasses(){
		return new HashSet<SootClass>(failedClasses);
	}
	
    /**
     * Returns all methods from from methodsToCall, where no call was possible
     * @return A Set of methods that were not called in the main method
     */
    public Set<SootMethod> getFailedMethods(){
    	return new HashSet<SootMethod>(failedMethods);
    }
    
    public void setSubstituteCallParams(boolean b){
		substituteCallParams = b;
	}
	
	public void setSubstituteClasses(List<String> l){
		substituteClasses = l;
	}

	@Override
	public SootMethod createDummyMain() {
		// Load the substitution classes
		if (substituteCallParams)
			for (String className : substituteClasses)
				Scene.v().forceResolve(className, SootClass.BODIES).setApplicationClass();
		
		return this.createDummyMainInternal();
	}
	
	@Override
	public SootMethod createDummyMain(SootMethod dummyMainMethod) {
		// Load the substitution classes
		if (substituteCallParams)
			for (String className : substituteClasses)
				Scene.v().forceResolve(className, SootClass.BODIES).setApplicationClass();
		
		return this.createDummyMainInternal(dummyMainMethod);
	}

	protected SootMethod createDummyMainInternal() {
		SootMethod emptySootMethod = createEmptyMainMethod(Jimple.v().newBody());
		return createDummyMainInternal(emptySootMethod);
	}
	
	/**
	 * Implementors need to overwrite this method for providing the actual dummy
	 * main method
	 * @return The generated dummy main method
	 */
	protected abstract SootMethod createDummyMainInternal(SootMethod emptySootMethod);
	
	/**
	 * Creates a new, empty main method containing the given body
	 * @param body The body to be used for the new main method
	 * @return The newly generated main method
	 */
	protected SootMethod createEmptyMainMethod(Body body){
		// If we already have a main class, we need to make sure to use a fresh
		// method name
		final SootClass mainClass;
		String methodName = DUMMY_METHOD_NAME;
		if (Scene.v().containsClass(DUMMY_CLASS_NAME)) {
			int methodIndex = 0;
			mainClass = Scene.v().getSootClass(DUMMY_CLASS_NAME);
			while (mainClass.declaresMethodByName(methodName))
				methodName = DUMMY_METHOD_NAME + "_" + methodIndex++;
		}
		else {
			mainClass = new SootClass(DUMMY_CLASS_NAME);
			Scene.v().addClass(mainClass);
		}
		
		SootMethod mainMethod = new SootMethod(methodName, new ArrayList<Type>(), VoidType.v());
		body.setMethod(mainMethod);
		mainMethod.setActiveBody(body);
		mainClass.addMethod(mainMethod);
		// First add class to scene, then make it an application class
		// as addClass contains a call to "setLibraryClass" 
		mainClass.setApplicationClass();
		mainMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
		return mainMethod;
	}
	
	/**
	 * Builds a new invocation statement that invokes the given method
	 * @param methodToCall The method to call
	 * @param body The body in which to create the invocation statement
	 * @param classLocal The local containing an instance of the class on which
	 * to invoke the method
	 * @param gen The local generator to be used for generating locals that hold
	 * any additional values required for the call parameters
	 * @return The newly created invocation statement
	 */
	protected Stmt buildMethodCall(SootMethod methodToCall, Body body,
			Local classLocal, LocalGenerator gen) {
		return buildMethodCall(methodToCall, body, classLocal, gen,
				Collections.<SootClass>emptySet());
	}

	protected Stmt buildMethodCall(SootMethod methodToCall, Body body,
			Local classLocal, LocalGenerator gen,
			Set<SootClass> parentClasses){
		assert methodToCall != null : "Current method was null";
		assert body != null : "Body was null";
		assert gen != null : "Local generator was null";
		
		if (classLocal == null && !methodToCall.isStatic()) {
			logger.warn("Cannot call method {}, because there is no local for base object: {}", 
					methodToCall, methodToCall.getDeclaringClass());
			failedMethods.add(methodToCall);
			return null;
		}
		
		final InvokeExpr invokeExpr;
		List<Value> args = new LinkedList<Value>();
		if(methodToCall.getParameterCount()>0){
			for (Type tp : methodToCall.getParameterTypes())
				args.add(getValueForType(body, gen, tp, new HashSet<SootClass>(), parentClasses));
			
			if(methodToCall.isStatic())
				invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef(), args);
			else {
				assert classLocal != null : "Class local method was null for non-static method call";
				if (methodToCall.isConstructor())
					invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef(),args);
				else
					invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef(),args);
			}
		}else{
			if(methodToCall.isStatic()){
				invokeExpr = Jimple.v().newStaticInvokeExpr(methodToCall.makeRef());
			}else{
				assert classLocal != null : "Class local method was null for non-static method call";
				if (methodToCall.isConstructor())
					invokeExpr = Jimple.v().newSpecialInvokeExpr(classLocal, methodToCall.makeRef());
				else
					invokeExpr = Jimple.v().newVirtualInvokeExpr(classLocal, methodToCall.makeRef());
			}
		}
		 
		Stmt stmt;
		if (!(methodToCall.getReturnType() instanceof VoidType)) {
			Local returnLocal = gen.generateLocal(methodToCall.getReturnType());
			stmt = Jimple.v().newAssignStmt(returnLocal, invokeExpr);
			
		} else {
			stmt = Jimple.v().newInvokeStmt(invokeExpr);
		}
		body.getUnits().add(stmt);
		
		// Clean up
		for (Object val : args)
			if (val instanceof Local && ((Value) val).getType() instanceof RefType)
				body.getUnits().add(Jimple.v().newAssignStmt((Value) val, NullConstant.v()));
		
		return stmt;
	}

	/**
	 * Creates a value of the given type to be used as a substitution in method
	 * invocations or fields
	 * @param body The body in which to create the value
	 * @param gen The local generator
	 * @param tp The type for which to get a value
	 * @param constructionStack The set of classes we're currently constructing.
	 * Attempts to create a parameter of one of these classes will trigger
	 * the constructor loop check and the respective parameter will be
	 * substituted by null.
	 * @param parentClasses If the given type is compatible with one of the types
	 * in this list, the already-created object is used instead of creating a
	 * new one.
	 * @return The generated value, or null if no value could be generated
	 */
	private Value getValueForType(Body body, LocalGenerator gen,
			Type tp, Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
		// Depending on the parameter type, we try to find a suitable
		// concrete substitution
		if (isSimpleType(tp.toString()))
			return getSimpleDefaultValue(tp.toString());
		else if (tp instanceof RefType) {
			SootClass classToType = ((RefType) tp).getSootClass();
			
			if(classToType != null){
				// If we have a parent class compatible with this type, we use
				// it before we check any other option
				for (SootClass parent : parentClasses)
					if (isCompatible(parent, classToType)) {
						Value val = this.localVarsForClasses.get(parent.getName());
						if (val != null)
							return val;
					}

				// Create a new instance to plug in here
				Value val = generateClassConstructor(classToType, body, constructionStack, parentClasses);
				
				// If we cannot create a parameter, we try a null reference.
				// Better than not creating the whole invocation...
				if(val == null)
					return NullConstant.v();
				
				return val;
			}
		}
		else if (tp instanceof ArrayType) {
			Value arrVal = buildArrayOfType(body, gen, (ArrayType) tp, constructionStack, parentClasses);
			if (arrVal == null){
				logger.warn("Array parameter substituted by null");
				return NullConstant.v();
			}
			return arrVal;
		}
		else {
			logger.warn("Unsupported parameter type: {}", tp.toString());
			return null;
		}
		throw new RuntimeException("Should never see me");
    }
	
	/**
	 * Constructs an array of the given type with a single element of this type
	 * in the given method
	 * @param body The body of the method in which to create the array
	 * @param gen The local generator
	 * @param tp The type of which to create the array
	 * @param constructionStack Set of classes currently being built to avoid
	 * constructor loops
	 * @param parentClasses If a requested type is compatible with one of the
	 * types in this list, the already-created object is used instead of
	 * creating a new one.
	 * @return The local referencing the newly created array, or null if the
	 * array generation failed
	 */
	private Value buildArrayOfType(Body body, LocalGenerator gen, ArrayType tp,
			Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
		Local local = gen.generateLocal(tp);

		// Generate a new single-element array
		NewArrayExpr newArrayExpr = Jimple.v().newNewArrayExpr(tp.getElementType(),
				IntConstant.v(1));
		AssignStmt assignArray = Jimple.v().newAssignStmt(local, newArrayExpr);
		body.getUnits().add(assignArray);
		
		// Generate a single element in the array
		AssignStmt assign = Jimple.v().newAssignStmt
				(Jimple.v().newArrayRef(local, IntConstant.v(0)),
				getValueForType(body, gen, tp.getElementType(), constructionStack, parentClasses));
		body.getUnits().add(assign);
		return local;
	}

	/**
	 * Generates code which creates a new instance of the given class.
	 * @param createdClass The class of which to create an instance
	 * @param body The body to which to add the new statements ("new" statement,
	 * constructor call, etc.)
	 * @return The local containing the new object instance if the operation
	 * completed successfully, otherwise null.
	 */
	protected Local generateClassConstructor(SootClass createdClass, Body body) {
		return this.generateClassConstructor(createdClass, body, new HashSet<SootClass>(),
				Collections.<SootClass>emptySet());
	}
	
	/**
	 * Generates code which creates a new instance of the given class.
	 * @param createdClass The class of which to create an instance
	 * @param body The body to which to add the new statements ("new" statement,
	 * constructor call, etc.)
	 * @param parentClasses If a constructor call requires an object of a type
	 * which is compatible with one of the types in this list, the already-created
	 * object is used instead of creating a new one.
	 * @return The local containing the new object instance if the operation
	 * completed successfully, otherwise null.
	 */
	protected Local generateClassConstructor(SootClass createdClass, Body body,
			Set<SootClass> parentClasses) {
		return this.generateClassConstructor(createdClass, body, new HashSet<SootClass>(),
				parentClasses);
	}

	/**
	 * Generates code which creates a new instance of the given class.
	 * @param createdClass The class of which to create an instance
	 * @param body The body to which to add the new statements ("new" statement,
	 * constructor call, etc.)
	 * @param constructionStack The stack of classes currently under construction.
	 * This is used to detect constructor loops. If a constructor requires a
	 * parameter of a type that is already on the stack, this value is substituted
	 * by null.
	 * @param parentClasses If a constructor call requires an object of a type
	 * which is compatible with one of the types in this list, the already-created
	 * object is used instead of creating a new one.
	 * @return The local containing the new object instance if the operation
	 * completed successfully, otherwise null.
	 */
	private Local generateClassConstructor(SootClass createdClass, Body body,
			Set<SootClass> constructionStack, Set<SootClass> parentClasses) {
		if (createdClass == null || this.failedClasses.contains(createdClass))
			return null;
		
		// We cannot create instances of phantom classes as we do not have any
		// constructor information for them
		if (createdClass.isPhantom() || createdClass.isPhantomClass()) {
			logger.warn("Cannot generate constructor for phantom class {}", createdClass.getName());
			failedClasses.add(createdClass);
			return null;
		}

		LocalGenerator generator = new LocalGenerator(body);

		// if sootClass is simpleClass:
		if (isSimpleType(createdClass.toString())) {
			Local varLocal =  generator.generateLocal(getSimpleTypeFromType(createdClass.getType()));
			
			AssignStmt aStmt = Jimple.v().newAssignStmt(varLocal, getSimpleDefaultValue(createdClass.toString()));
			body.getUnits().add(aStmt);
			return varLocal;
		}
		
		boolean isInnerClass = createdClass.getName().contains("$");
		String outerClass = isInnerClass ? createdClass.getName().substring
				(0, createdClass.getName().lastIndexOf("$")) : "";
		
		// Make sure that we don't run into loops
		if (!constructionStack.add(createdClass)) {
			logger.warn("Ran into a constructor generation loop for class " + createdClass
					+ ", substituting with null...");
			Local tempLocal = generator.generateLocal(RefType.v(createdClass));			
			AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, NullConstant.v());
			body.getUnits().add(assignStmt);
			return tempLocal;
		}
		if(createdClass.isInterface() || createdClass.isAbstract()){
			if(substituteCallParams) {
				// Find a matching implementor of the interface
				List<SootClass> classes;
				if (createdClass.isInterface())
					classes = Scene.v().getActiveHierarchy().getImplementersOf(createdClass);
				else
					classes = Scene.v().getActiveHierarchy().getSubclassesOf(createdClass);
				
				// Generate an instance of the substitution class. If we fail,
				// try the next substitution. If we don't find any possible
				// substitution, we're in trouble
				for(SootClass sClass : classes)
					if(substituteClasses.contains(sClass.toString())) {
						Local cons = generateClassConstructor(sClass, body, constructionStack, parentClasses);
						if (cons == null)
							continue;
						return cons;
					}
				logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass", createdClass,
                        (createdClass.isInterface() ? "an interface" :(createdClass.isAbstract() ? "abstract" : "")));
				this.failedClasses.add(createdClass);
				return null;
			}
			else{
                logger.warn("Cannot create valid constructor for {}, because it is {} and cannot substitute with subclass", createdClass,
                        (createdClass.isInterface() ? "an interface" :(createdClass.isAbstract() ? "abstract" : "")));
				this.failedClasses.add(createdClass);
				return null;
			}
		}
		else{			
			// Find a constructor we can invoke. We do this first as we don't want
			// to change anything in our method body if we cannot create a class
			// instance anyway.
			for (SootMethod currentMethod : createdClass.getMethods()) {
				if (currentMethod.isPrivate() || !currentMethod.isConstructor())
					continue;
				
				List<Value> params = new LinkedList<Value>();
				for (Type type : currentMethod.getParameterTypes()) {
					// We need to check whether we have a reference to the
					// outer class. In this case, we do not generate a new
					// instance, but use the one we already have.
					String typeName = type.toString().replaceAll("\\[\\]]", "");
					if (type instanceof RefType
							&& isInnerClass && typeName.equals(outerClass)
							&& this.localVarsForClasses.containsKey(typeName))
						params.add(this.localVarsForClasses.get(typeName));
					else
						params.add(getValueForType(body, generator, type, constructionStack, parentClasses));
				}

				// Build the "new" expression
				NewExpr newExpr = Jimple.v().newNewExpr(RefType.v(createdClass));
				Local tempLocal = generator.generateLocal(RefType.v(createdClass));			
				AssignStmt assignStmt = Jimple.v().newAssignStmt(tempLocal, newExpr);
				body.getUnits().add(assignStmt);		

				// Create the constructor invocation
				InvokeExpr vInvokeExpr;
				if (params.isEmpty() || params.contains(null))
					vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef());
				else
					vInvokeExpr = Jimple.v().newSpecialInvokeExpr(tempLocal, currentMethod.makeRef(), params);

				// Make sure to store return values
				if (!(currentMethod.getReturnType() instanceof VoidType)) { 
					Local possibleReturn = generator.generateLocal(currentMethod.getReturnType());
					AssignStmt assignStmt2 = Jimple.v().newAssignStmt(possibleReturn, vInvokeExpr);
					body.getUnits().add(assignStmt2);
				}
				else
					body.getUnits().add(Jimple.v().newInvokeStmt(vInvokeExpr));
					
				return tempLocal;
			}

			logger.warn("Could not find a suitable constructor for class {}", createdClass.getName());
			this.failedClasses.add(createdClass);
			return null;
		}
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

	/**
	 * Finds a method with the given signature in the given class or one of its
	 * super classes
	 * @param currentClass The current class in which to start the search
	 * @param subsignature The subsignature of the method to find
	 * @return The method with the given signature if it has been found,
	 * otherwise null
	 */
	protected SootMethod findMethod(SootClass currentClass, String subsignature){
		if(currentClass.declaresMethod(subsignature)){
			return currentClass.getMethod(subsignature);
		}
		if(currentClass.hasSuperclass()){
			return findMethod(currentClass.getSuperclass(), subsignature);
		}
		return null;
	}

	/**
	 * Checks whether an object of type "actual" can be inserted where an
	 * object of  type "expected" is required.
	 * @param actual The actual type (the substitution candidate)
	 * @param expected The requested type
	 * @return True if the two types are compatible and "actual" can be used
	 * as a substitute for "expected", otherwise false
	 */
	protected boolean isCompatible(SootClass actual, SootClass expected) {
		SootClass act = actual;
		while (true) {
			// Do we have a direct match?
			if (act.getName().equals(expected.getName()))
				return true;
			
			// If we expect an interface, the current class might implement it
			if (expected.isInterface())
				for (SootClass intf : act.getInterfaces())
					if (intf.getName().equals(expected.getName()))
						return true;
			
			// If we cannot continue our search further up the hierarchy, the
			// two types are incompatible
			if (!act.hasSuperclass())
				return false;
			act = act.getSuperclass();
		}
	}
	
	/**
	 * Eliminates all loops of length 0 (if a goto <if a>)
	 * @param body The body from which to eliminate the self-loops
	 */
	protected void eliminateSelfLoops(Body body) {
		// Get rid of self-loops
		for (Iterator<Unit> unitIt = body.getUnits().iterator(); unitIt.hasNext(); ) {
			Unit u = unitIt.next();
			if (u instanceof IfStmt) {
				IfStmt ifStmt = (IfStmt) u;
				if (ifStmt.getTarget() == ifStmt)
					unitIt.remove();
			}
		}
	}

}
