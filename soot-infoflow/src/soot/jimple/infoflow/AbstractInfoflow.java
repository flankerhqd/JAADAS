package soot.jimple.infoflow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.entryPointCreators.DefaultEntryPointCreator;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.source.DefaultSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * Abstract base class for all data/information flow analyses in FlowDroid
 * @author Steven Arzt
 *
 */
public abstract class AbstractInfoflow implements IInfoflow {
	
	protected ITaintPropagationWrapper taintWrapper;

	protected boolean stopAfterFirstFlow = false;
	protected boolean enableImplicitFlows = false;
	protected boolean enableStaticFields = true;
	protected boolean enableExceptions = true;
	protected boolean flowSensitiveAliasing = true;
	protected boolean enableTypeChecking = true;
	protected boolean ignoreFlowsInSystemPackages = true;
	
	protected boolean inspectSources = false;
	protected boolean inspectSinks = false;
	
	protected final BiDirICFGFactory icfgFactory;
	protected int maxThreadNum = -1;

	protected CallgraphAlgorithm callgraphAlgorithm = /*CallgraphAlgorithm.OnDemand;*/ CallgraphAlgorithm.AutomaticSelection;
	protected AliasingAlgorithm aliasingAlgorithm = AliasingAlgorithm.FlowSensitive;
	
	protected Collection<PreAnalysisHandler> preProcessors = Collections.emptyList();
	
	protected CodeEliminationMode codeEliminationMode = CodeEliminationMode.PropagateConstants;
    
    /**
     * Creates a new instance of the abstract info flow problem
     */
    public AbstractInfoflow() {
    	this(null);
    }

    /**
     * Creates a new instance of the abstract info flow problem
     * @param icfgFactory The interprocedural CFG to be used by the InfoFlowProblem
     */
    public AbstractInfoflow(BiDirICFGFactory icfgFactory) {
    	if (icfgFactory == null)
    		this.icfgFactory = new DefaultBiDiICFGFactory();
    	else
    		this.icfgFactory = icfgFactory;
    }

    @Override
	public void setTaintWrapper(ITaintPropagationWrapper wrapper) {
		taintWrapper = wrapper;
	}
    
    @Override
    public ITaintPropagationWrapper getTaintWrapper() {
    	return taintWrapper;
    }

	@Override
	public void setStopAfterFirstFlow(boolean stopAfterFirstFlow) {
		this.stopAfterFirstFlow = stopAfterFirstFlow;
	}
	
	@Override
	public void setPreProcessors(Collection<PreAnalysisHandler> preprocessors) {
        this.preProcessors = preprocessors;
	}

	@Override
	public void computeInfoflow(String appPath, String libPath,
			IEntryPointCreator entryPointCreator,
			List<String> sources, List<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPointCreator,
				new DefaultSourceSinkManager(sources, sinks));
	}

	@Override
	public void computeInfoflow(String appPath, String libPath,
			Collection<String> entryPoints, 
			Collection<String> sources,
			Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, new DefaultEntryPointCreator(entryPoints),
				new DefaultSourceSinkManager(sources, sinks));
	}

	@Override
	public void computeInfoflow(String libPath, String appPath,
			String entryPoint, Collection<String> sources, Collection<String> sinks) {
		this.computeInfoflow(appPath, libPath, entryPoint, new DefaultSourceSinkManager(sources, sinks));
	}
	
	@Override
	public void setInspectSources(boolean inspect) {
		inspectSources = inspect;
	}

	@Override
	public void setInspectSinks(boolean inspect) {
		inspectSinks = inspect;
	}

	@Override
	public void setEnableImplicitFlows(boolean enableImplicitFlows) {
		this.enableImplicitFlows = enableImplicitFlows;
	}

	@Override
	public void setEnableStaticFieldTracking(boolean enableStaticFields) {
		this.enableStaticFields = enableStaticFields;
	}
	
	@Override
	public void setFlowSensitiveAliasing(boolean flowSensitiveAliasing) {
		this.flowSensitiveAliasing = flowSensitiveAliasing;
	}
		
	@Override
	public void setEnableExceptionTracking(boolean enableExceptions) {
		this.enableExceptions = enableExceptions;
	}

	@Override
	public void setCallgraphAlgorithm(CallgraphAlgorithm algorithm) {
    	this.callgraphAlgorithm = algorithm;
	}

	@Override
	public void setAliasingAlgorithm(AliasingAlgorithm algorithm) {
    	this.aliasingAlgorithm = algorithm;
	}
	
	@Override
	public void setMaxThreadNum(int threadNum) {
		this.maxThreadNum = threadNum;
	}

	@Override
	public void setEnableTypeChecking(boolean enableTypeChecking) {
		this.enableTypeChecking = enableTypeChecking;
	}
	
	@Override
	public void setIgnoreFlowsInSystemPackages(boolean ignoreFlowsInSystemPackages) {
		this.ignoreFlowsInSystemPackages = ignoreFlowsInSystemPackages;
	}
	
	@Override
	public void setCodeEliminationMode(CodeEliminationMode mode) {
		this.codeEliminationMode = mode;
	}
	
}
