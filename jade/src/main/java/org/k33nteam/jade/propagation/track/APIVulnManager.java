package org.k33nteam.jade.propagation.track;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.control.CompilationFailedException;
import org.k33nteam.jade.bean.VulnResult;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Constant;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class APIVulnManager implements IAPIVulnManager {
	private Map<String, Integer> mappings = new HashMap<>();
	private Map<String, String> descs = new HashMap<>();
	private List<Pair<SootMethod, Pair<Stmt,String>>> results = new ArrayList<>();
	private GroovyObject groovyObject;

	private static final boolean DEBUG = false;
	public APIVulnManager(String groovyPath)
	{
		initFromGroovy(groovyPath);
	}
	
	private void initFromGroovy(String groovyPath) //called only once!
	{
		ClassLoader parent = APIVulnManager.class.getClassLoader();
		  GroovyClassLoader loader = new GroovyClassLoader(parent);
		  Class groovyClass = null;
		try {
			groovyClass = loader.parseClass(
			    new File(groovyPath));
		} catch (CompilationFailedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		try {
			groovyObject = (GroovyObject) 
			    groovyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		  mappings = (Map<String, Integer>) groovyObject.invokeMethod("getMappings", null);
		  descs = (Map<String, String>) groovyObject.invokeMethod("getDescMappings", null);
		  try {
			loader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getDescBySig(String funcsig)
	{
		return descs.get(funcsig);
	}
	public boolean evaluateResult(String sig, String value)
	{
		String[] args = {sig, value};
		return (Boolean) groovyObject.invokeMethod("evaluate", args);
	}
	@Override
	public List<VulnResult> getAnalysisResult() {
		List<VulnResult> ret = new ArrayList<>();
		for(int i=0;i<this.results.size();i++)
		{
			String funcSig = this.results.get(i).getO2().getO1().getInvokeExpr().getMethod().getSignature();
			String value = this.results.get(i).getO2().getO2();
			if (evaluateResult(funcSig, value))
			{
				if(DEBUG){
					System.out.println("sig: " + this.results.get(i).getO1().getSignature());
					System.out.println("descs: " + descs);
				}
				VulnResult result = VulnResult.toConstantAPIVulnResult(this.results.get(i).getO2().getO1(), this.results.get(i).getO1(), descs.get(funcSig),"", 0.3f);
				ret.add(result);
			}
		}
		return ret;
	}

	@Override
	public Value getVulnParamAsSource(Stmt stmt) {
		String name = stmt.getInvokeExpr().getMethod().getSignature();
		if (mappings.containsKey(name)) {
			return stmt.getInvokeExpr().getArg(mappings.get(name));
		}
		return null;
	}

	@Override
	public void isParamVulnAndStore(SootMethod originMethod, Stmt originStmt, Value reachedValue) { //avoid sideeffect
		//constant already guaranteed by caller
		System.out.println(originStmt);
		String funcSig = originStmt.getInvokeExpr().getMethod().getSignature();
		String valueString = reachedValue.toString();
		if (evaluateResult(funcSig, valueString)) {
			if(DEBUG) {
				System.out.println("result found");
				System.out.println("originstmt: " + originStmt + " reachedValue: " + reachedValue);
			}
			this.results.add(new Pair<>(originMethod, new Pair<>(originStmt, valueString)));
		}
		if(DEBUG) {
			if (reachedValue instanceof Constant || reachedValue instanceof StaticFieldRef) {
				System.out.println("originstmt: " + originStmt + " reachedValue: " + reachedValue);
			}
		}
	}
}
