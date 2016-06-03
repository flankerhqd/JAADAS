package soot.jimple.infoflow.cfg;

import java.util.Collections;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;

/**
 * Class for patching OS libraries such as java.lang.Thread so that we get
 * complete callgraphs
 * 
 * @author Steven Arzt
 *
 */
public class LibraryClassPatcher {
	
	public LibraryClassPatcher() {
		
	}
	
	/**
	 * Patches all supported system libraries
	 */
	public void patchLibraries() {
        // Patch the java.lang.Thread implementation
		patchHandlerImplementation();
		
		// Patch the android.os.Handler implementation
		patchThreadImplementation();
	}
	
	/**
	 * Creates a synthetic minimal implementation of the java.lang.Thread class
	 * to model threading correctly even if we don't have a real implementation.
	 */
	private void patchThreadImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("java.lang.Thread");
		if (sc == null)
			return;
		
		SootMethod smRun = sc.getMethodUnsafe("void run()");
		if (smRun == null || smRun.hasActiveBody())
			return;
		
		SootMethod smCons = sc.getMethodUnsafe("void <init>(java.lang.Runnable)");
		if (smCons == null || smCons.hasActiveBody())
			return;
		
		SootClass runnable = Scene.v().getSootClassUnsafe("java.lang.Runnable");
		if (runnable == null)
			return;
		
		// Create a field for storing the runnable
		int fieldIdx = 0;
		SootField fldTarget = null;
		while ((fldTarget = sc.getFieldByNameUnsafe("target" + fieldIdx)) != null)
			fieldIdx++;
		fldTarget = new SootField("target" + fieldIdx, runnable.getType());
		sc.addField(fldTarget);
		
		// Create a new constructor
		patchThreadConstructor(smCons, runnable, fldTarget);
		
		// Create a new Thread.start() method
		patchThreadRunMethod(smRun, runnable, fldTarget);
	}
	
	/**
	 * Creates a synthetic "java.lang.Thread.run()" method implementation that
	 * calls the target previously passed in when the constructor was called
	 * @param smRun The run() method for which to create a synthetic
	 * implementation
	 * @param runnable The "java.lang.Runnable" interface
	 * @param fldTarget The field containing the target object
	 */
	private void patchThreadRunMethod(SootMethod smRun, SootClass runnable,
			SootField fldTarget) {
		SootClass sc = smRun.getDeclaringClass();
		Body b = Jimple.v().newBody(smRun);
		smRun.setActiveBody(b);
		
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(sc.getType())));
		
		Local targetLocal = Jimple.v().newLocal("target", runnable.getType());
		b.getLocals().add(targetLocal);
		b.getUnits().add(Jimple.v().newAssignStmt(targetLocal,
				Jimple.v().newInstanceFieldRef(thisLocal, fldTarget.makeRef())));
		
		Unit retStmt = Jimple.v().newReturnVoidStmt();
		
		// If (this.target == null) return;
		b.getUnits().add(Jimple.v().newIfStmt(Jimple.v().newEqExpr(targetLocal,
				NullConstant.v()), retStmt));
		
		// Invoke target.run()
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(targetLocal,
				runnable.getMethod("void run()").makeRef())));
		
		b.getUnits().add(retStmt);
	}

	/**
	 * Creates a synthetic "<init>(java.lang.Runnable)" method implementation
	 * that stores the given runnable into a field for later use
	 * @param smCons The <init>() method for which to create a synthetic
	 * implementation
	 * @param runnable The "java.lang.Runnable" interface
	 * @param fldTarget The field receiving the Runnable
	 */
	private void patchThreadConstructor(SootMethod smCons, SootClass runnable,
			SootField fldTarget) {
		SootClass sc = smCons.getDeclaringClass();
		Body b = Jimple.v().newBody(smCons);
		smCons.setActiveBody(b);
		
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(sc.getType())));
		
		Local param0Local = Jimple.v().newLocal("p0", runnable.getType());
		b.getLocals().add(param0Local);
		b.getUnits().add(Jimple.v().newIdentityStmt(param0Local,
				Jimple.v().newParameterRef(runnable.getType(), 0)));
		
		b.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal,
				fldTarget.makeRef()), param0Local));
		
		b.getUnits().add(Jimple.v().newReturnVoidStmt());
	}
	
	/**
	 * Creates a dummy implementation of android.os.Handler if we don't have one
	 */
	private void patchHandlerImplementation() {
		SootClass sc = Scene.v().getSootClassUnsafe("android.os.Handler");
		if (sc == null)
			return;
		
		SootClass runnable = Scene.v().getSootClassUnsafe("java.lang.Runnable");
		if (runnable == null)
			return;
		
		SootMethod smPost = sc.getMethodUnsafe(
				"boolean post(java.lang.Runnable)");
		SootMethod smPostAtFrontOfQueue = sc.getMethodUnsafe(
				"boolean postAtFrontOfQueue(java.lang.Runnable)");
		SootMethod smPostAtTimeWithToken = sc.getMethodUnsafe(
				"boolean postAtTime(java.lang.Runnable,java.lang.Object,long)");
		SootMethod smPostAtTime = sc.getMethodUnsafe(
				"boolean postAtTime(java.lang.Runnable,long)");
		SootMethod smPostDelayed = sc.getMethodUnsafe(
				"boolean postDelayed(java.lang.Runnable,long)");
		
		if (smPost != null && !smPost.hasActiveBody())
			patchHandlerPostBody(smPost, runnable);
		if (smPostAtFrontOfQueue != null && !smPostAtFrontOfQueue.hasActiveBody())
			patchHandlerPostBody(smPostAtFrontOfQueue, runnable);
		if (smPostAtTime != null && !smPostAtTime.hasActiveBody())
			patchHandlerPostBody(smPostAtTime, runnable);
		if (smPostAtTimeWithToken != null && !smPostAtTimeWithToken.hasActiveBody())
			patchHandlerPostBody(smPostAtTimeWithToken, runnable);
		if (smPostDelayed != null && !smPostDelayed.hasActiveBody())
			patchHandlerPostBody(smPostDelayed, runnable);
	}
	
	/**
	 * Creates a new body for one of the postXXX methods in android.os.Handler
	 * @param method The method for which to create the implementation
	 * @param runnable The java.lang.Runnable class
	 * @return The newly created body
	 */
	private Body patchHandlerPostBody(SootMethod method, SootClass runnable) {
		SootClass sc = method.getDeclaringClass();
		Body b = Jimple.v().newBody(method);
		method.setActiveBody(b);
		
		Local thisLocal = Jimple.v().newLocal("this", sc.getType());
		b.getLocals().add(thisLocal);
		b.getUnits().add(Jimple.v().newIdentityStmt(thisLocal,
				Jimple.v().newThisRef(sc.getType())));
		
		// Assign the parameters
		Local firstParam = null;
		for (int i = 0; i < method.getParameterCount(); i++)  {
			Local paramLocal = Jimple.v().newLocal("param" + i, method.getParameterType(i));
			b.getLocals().add(paramLocal);
			b.getUnits().add(Jimple.v().newIdentityStmt(paramLocal,
					Jimple.v().newParameterRef(method.getParameterType(i), i)));
			if (i == 0)
				firstParam = paramLocal;
		}
			
		// Invoke p0.run()
		b.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(firstParam,
				Scene.v().makeMethodRef(runnable, "run", Collections.<Type>emptyList(), VoidType.v(), false))));
		
		Unit retStmt = Jimple.v().newReturnStmt(IntConstant.v(1));
		b.getUnits().add(retStmt);
		return b;
	}

}
