package soot.jimple.infoflow.android.source.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import soot.jimple.infoflow.data.SootMethodAndClass;

/**
 * A class to handle all access paths of sources and sinks for a certain method.
 * 
 * @author Daniel Magin
 * @author Steven Arzt
 *
 */
public class SourceSinkDefinition {
	
	private final SootMethodAndClass method;
	private Set<AccessPathTuple> baseObjects;
	private Set<AccessPathTuple>[] parameters;
	private Set<AccessPathTuple> returnValues;
	
	/**
	 * Creates a new instance of the MethodSourceSinkDefinition class 
	 */
	public SourceSinkDefinition(SootMethodAndClass am) {
		this(am, null, null, null);
	}
	
	/**
	 * Creates a new instance of the MethodSourceSinkDefinition class
	 * @param am The method for which this object defines sources and sinks
	 * @param baseObjects The source and sink definitions for the base object on
	 * which a method of this class is invoked
	 * @param parameters The source and sink definitions for parameters of
	 * the current method
	 * @param returnValues The source definitions for the return value of the
	 * current method
	 */
	public SourceSinkDefinition(SootMethodAndClass am,
			Set<AccessPathTuple> baseObjects,
			Set<AccessPathTuple>[] parameters,
			Set<AccessPathTuple> returnValues) {
		this.method = am;
		this.baseObjects = baseObjects == null || baseObjects.isEmpty()
				? null : baseObjects;
		this.parameters = parameters;
		this.returnValues = returnValues == null || returnValues.isEmpty()
				? null : returnValues;
	}
	
	/**
	 * Gets the method for which this object defines sources and sinks
	 * @return The method for which this object defines sources and sinks
	 */
	public SootMethodAndClass getMethod() {
		return this.method;
	}
	
	/**
	 * Gets the source and sink definitions for the base object on which a method
	 * of this class is invoked
	 * @return The source and sink definitions for the base object
	 */
	public Set<AccessPathTuple> getBaseObjects() {
		return this.baseObjects;
	}
	
	/**
	 * Gets the number of access paths defined as sources or sinks on base
	 * objects
	 * @return The number of access paths defined as sources or sinks on base
	 * objects
	 */
	public int getBaseObjectCount() {
		return this.baseObjects == null ? 0 : this.baseObjects.size();
	}
	
	/**
	 * Gets the source and sink definitions for parameters of the current method
	 * @return The source and sink definitions for parameters
	 */
	public Set<AccessPathTuple>[] getParameters() {
		return this.parameters;
	}
	
	/**
	 * Gets the number of access paths defined as sources or sinks on parameters
	 * @return The number of access paths defined as sources or sinks on
	 * parameters
	 */
	public int getParameterCount() {
		if (this.parameters == null || this.parameters.length == 0)
			return 0;
		
		int cnt = 0;
		for (Set<AccessPathTuple> apt : this.parameters)
			cnt += apt.size();
		return cnt;
	}

	/**
	 * Gets the source definitions for the return value of the current method
	 * @return The source definitions for the return value
	 */
	public Set<AccessPathTuple> getReturnValues() {
		return this.returnValues;
	}
	
	/**
	 * Gets the number of access paths defined as sources or sinks on return
	 * values
	 * @return The number of access paths defined as sources or sinks on return
	 * values
	 */
	public int getReturnValueCount() {
		return this.returnValues == null ? 0 : this.returnValues.size();
	}

	/**
	 * Checks whether this source/sink definition is empty, i.e., has no
	 * concrete access paths
	 * @return True if this source/sink definition is empty, i.e., has no
	 * concrete access paths, otherwise false
	 */
	public boolean isEmpty() {
		boolean parametersEmpty = true;
		if (parameters != null)
			for (Set<AccessPathTuple> paramSet : this.parameters)
				if (paramSet != null && !paramSet.isEmpty()) {
					parametersEmpty = false;
					break;
				}
		
		return (baseObjects == null || baseObjects.isEmpty())
				&& parametersEmpty
				&& (returnValues == null || returnValues.isEmpty());
	}
	
	@Override
	public String toString() {
		return method.getSignature();
	}
	
	/**
	 * Creates a definition which is a subset of this definition that only
	 * contains the sources
	 * @return The source-only subset of this definition
	 */
	public SourceSinkDefinition getSourceOnlyDefinition() {
		// Collect all base sources
		Set<AccessPathTuple> baseSources = null;
		if (baseObjects != null) {
			baseSources = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.isSource())
					baseSources.add(apt);
		}
		
		// Collect all parameter sources
		@SuppressWarnings("unchecked")
		Set<AccessPathTuple>[] paramSources = new Set[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Set<AccessPathTuple> aptSet = parameters[i];
			Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
			paramSources[i] = thisParam;
			for (AccessPathTuple apt : aptSet)
				if (apt.isSource())
					thisParam.add(apt);
		}
		
		// Collect all return sources
		Set<AccessPathTuple> returnSources = null;
		if (returnValues != null) {
			returnSources = new HashSet<>(returnValues.size());
			for (AccessPathTuple apt : returnValues)
				if (apt.isSource())
					returnSources.add(apt);
		}
		
		return new SourceSinkDefinition(method, baseSources, paramSources, returnSources);
	}
	
	/**
	 * Creates a definition which is a subset of this definition that only
	 * contains the sinks
	 * @return The sink-only subset of this definition
	 */
	public SourceSinkDefinition getSinkOnlyDefinition() {
		// Collect all base sinks
		Set<AccessPathTuple> baseSinks = null;
		if (baseObjects != null) {
			baseSinks = new HashSet<>(baseObjects.size());
			for (AccessPathTuple apt : baseObjects)
				if (apt.isSink())
					baseSinks.add(apt);
		}
		
		// Collect all parameter sinks
		@SuppressWarnings("unchecked")
		Set<AccessPathTuple>[] paramSinks = new Set[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Set<AccessPathTuple> aptSet = parameters[i];
			Set<AccessPathTuple> thisParam = new HashSet<>(aptSet.size());
			paramSinks[i] = thisParam;
			for (AccessPathTuple apt : aptSet)
				if (apt.isSink())
					thisParam.add(apt);
		}
		
		// Collect all return sinks
		Set<AccessPathTuple> returnSinks = null;
		if (returnValues != null) {
			returnSinks = new HashSet<>(returnValues.size());
			for (AccessPathTuple apt : returnValues)
				if (apt.isSink())
					returnSinks.add(apt);
		}
		
		return new SourceSinkDefinition(method, baseSinks, paramSinks, returnSinks);
	}
	
	/**
	 * Merges the source and sink definitions of the given definition object
	 * into this definition object
	 * @param other The definition object to merge
	 */
	@SuppressWarnings("unchecked")
	public void merge(SourceSinkDefinition other) {
		// Merge the base object definitions
		if (other.baseObjects != null && !other.baseObjects.isEmpty()) {
			if (this.baseObjects == null)
				this.baseObjects = new HashSet<>();
			for (AccessPathTuple apt : other.baseObjects)
				this.baseObjects.add(apt);
		}
		
		// Merge the parameter definitions
		if (other.parameters != null && other.parameters.length > 0) {
			if (this.parameters == null)
				this.parameters = new Set[this.method.getParameters().size()];
			for (int i = 0; i < other.parameters.length; i++) {
				this.parameters[i].addAll(other.parameters[i]);
			}
		}
		
		// Merge the return value definitions
		if (other.returnValues != null && !other.returnValues.isEmpty()) {
			if (this.returnValues == null)
				this.returnValues = new HashSet<>();
			for (AccessPathTuple apt : other.returnValues)
				this.returnValues.add(apt);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((baseObjects == null) ? 0 : baseObjects.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result
				+ ((parameters == null) ? 0 : Arrays.hashCode(parameters));
		result = prime * result
				+ ((returnValues == null) ? 0 : returnValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceSinkDefinition other = (SourceSinkDefinition) obj;
		if (baseObjects == null) {
			if (other.baseObjects != null)
				return false;
		} else if (!baseObjects.equals(other.baseObjects))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (parameters == null) {
			if (other.parameters != null)
				return false;
		} else if (!Arrays.equals(parameters, other.parameters))
			return false;
		if (returnValues == null) {
			if (other.returnValues != null)
				return false;
		} else if (!returnValues.equals(other.returnValues))
			return false;
		return true;
	}
	
}
