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
package soot.jimple.infoflow.results;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Class for collecting information flow results
 * 
 * @author Steven Arzt
 */
public class InfoflowResults {

    private final Logger logger = LoggerFactory.getLogger(getClass());
		
	private final MyConcurrentHashMap<ResultSinkInfo, Set<ResultSourceInfo>> results =
			new MyConcurrentHashMap<ResultSinkInfo, Set<ResultSourceInfo>>();
	
	public InfoflowResults() {
		
	}
	
	/**
	 * Gets the number of entries in this result object
	 * @return The number of entries in this result object
	 */
	public int size() {
		return this.results == null ? 0 : this.results.size();
	}
	
	/**
	 * Gets the total number of source-to-sink connections. If there are two
	 * connections along different paths between the same source and sink,
	 * size() will return 1, but numConnections() will return 2.
	 * @return The number of source-to-sink connections in this result object
	 */
	public int numConnections() {
		int num = 0;
		if (this.results != null)
			for (Entry<ResultSinkInfo, Set<ResultSourceInfo>> entry : this.results.entrySet())
				num += entry.getValue().size();
		return num;
	}
	
	/**
	 * Gets whether this result object is empty, i.e. contains no information
	 * flows
	 * @return True if this result object is empty, otherwise false.
	 */
	public boolean isEmpty() {
		return this.results == null || this.results.isEmpty();
	}
	
	/**
	 * Checks whether this result object contains a sink that exactly matches the
	 * given value.
	 * @param sink The sink to check for
	 * @return True if this result contains the given value as a sink, otherwise
	 * false.
	 */
	public boolean containsSink(Stmt sink) {
		for (ResultSinkInfo si : this.results.keySet())
			if (si.getSink().equals(sink))
				return true;
		return false;
	}
	
	/**
	 * Checks whether this result object contains a sink with the given method
	 * signature
	 * @param sinkSignature The method signature to check for
	 * @return True if there is a sink with the given method signature in this
	 * result object, otherwise false.
	 */
	public boolean containsSinkMethod(String sinkSignature) {
		return !findSinkByMethodSignature(sinkSignature).isEmpty();
	}

	public void addResult(AccessPath sink, Stmt sinkStmt,
			AccessPath source, Stmt sourceStmt) {
		this.addResult(new ResultSinkInfo(sink, sinkStmt), new ResultSourceInfo(source, sourceStmt));
	}
	
	public void addResult(AccessPath sink, Stmt sinkStmt,
			AccessPath source, Stmt sourceStmt,
			Object userData,
			List<Stmt> propagationPath) {
		this.addResult(new ResultSinkInfo(sink, sinkStmt),
				new ResultSourceInfo(source, sourceStmt, userData, propagationPath));
	}
	
	public void addResult(ResultSinkInfo sink, ResultSourceInfo source) {
		assert sink != null;
		assert source != null;
		
		Set<ResultSourceInfo> sourceInfo = this.results.putIfAbsentElseGet
				(sink, new ConcurrentHashSet<ResultSourceInfo>());
		sourceInfo.add(source);
	}

	/**
	 * Gets all results in this object as a hash map.
	 * @return All results in this object as a hash map.
	 */
	public Map<ResultSinkInfo, Set<ResultSourceInfo>> getResults() {
		return this.results;
	}
	
	/**
	 * Checks whether there is a path between the given source and sink.
	 * @param sink The sink to which there may be a path
	 * @param source The source from which there may be a path
	 * @return True if there is a path between the given source and sink, false
	 * otherwise
	 */
	public boolean isPathBetween(Stmt sink, Stmt source) {
		Set<ResultSourceInfo> sources = null;
		for(ResultSinkInfo sI : this.results.keySet()){
			if(sI.getSink().equals(sink)){
				sources = this.results.get(sI);
				break;
			}
		}
		if (sources == null)
			return false;
		for (ResultSourceInfo src : sources)
			if (src.getAccessPath().equals(source))
				return true;
		return false;
	}
	
	/**
	 * Checks whether there is a path between the given source and sink.
	 * @param sink The sink to which there may be a path
	 * @param source The source from which there may be a path
	 * @return True if there is a path between the given source and sink, false
	 * otherwise
	 */
	public boolean isPathBetween(String sink, String source) {
		for (ResultSinkInfo si : this.results.keySet())
			if (si.getAccessPath().getPlainValue().toString().equals(sink)) {
				Set<ResultSourceInfo> sources = this.results.get(si);
				for (ResultSourceInfo src : sources)
					if (src.getSource().toString().contains(source))
						return true;
		}
		return false;
	}

	/**
	 * Checks whether there is an information flow between the two
	 * given methods (specified by their respective Soot signatures). 
	 * @param sinkSignature The sink to which there may be a path
	 * @param sourceSignature The source from which there may be a path
	 * @return True if there is a path between the given source and sink, false
	 * otherwise
	 */
	public boolean isPathBetweenMethods(String sinkSignature, String sourceSignature) {
		List<ResultSinkInfo> sinkVals = findSinkByMethodSignature(sinkSignature);
		for (ResultSinkInfo si : sinkVals) {
			Set<ResultSourceInfo> sources = this.results.get(si);
			if (sources == null)
				return false;
			for (ResultSourceInfo src : sources)
				if (src.getSource().containsInvokeExpr()) {
					InvokeExpr expr = src.getSource().getInvokeExpr();
					if (expr.getMethod().getSignature().equals(sourceSignature))
						return true;
				}
		}
		return false;
	}

	/**
	 * Finds the entry for a sink method with the given signature
	 * @param sinkSignature The sink's method signature to look for
	 * @return The key of the entry with the given method signature if such an
	 * entry has been found, otherwise null.
	 */
	private List<ResultSinkInfo> findSinkByMethodSignature(String sinkSignature) {
		List<ResultSinkInfo> sinkVals = new ArrayList<ResultSinkInfo>();
		for (ResultSinkInfo si : this.results.keySet())
			if (si.getSink().containsInvokeExpr()) {
				InvokeExpr expr = si.getSink().getInvokeExpr();
				if (expr.getMethod().getSignature().equals(sinkSignature))
					sinkVals.add(si);
			}
		return sinkVals;
	}

	/**
	 * Prints all results stored in this object to the standard output
	 */
	public void printResults() {
		for (ResultSinkInfo sink : this.results.keySet()) {
			logger.info("Found a flow to sink {}, from the following sources:", sink);
			for (ResultSourceInfo source : this.results.get(sink)) {
				logger.info("\t- {}", source.getSource());
				if (source.getPath() != null && !source.getPath().isEmpty())
					logger.info("\t\ton Path {}", source.getPath());
			}
		}
	}

	/**
	 * Prints all results stored in this object to the given writer
	 * @param wr The writer to which to print the results
	 * @throws IOException Thrown when data writing fails
	 */
	public void printResults(Writer wr) throws IOException {
		for (ResultSinkInfo sink : this.results.keySet()) {
			wr.write("Found a flow to sink " + sink + ", from the following sources:\n");
			for (ResultSourceInfo source : this.results.get(sink)) {
				wr.write("\t- " + source.getSource() + "\n");
				if (source.getPath() != null && !source.getPath().isEmpty())
					wr.write("\t\ton Path " + source.getPath() + "\n");
			}
		}
	}
	
	/**
	 * Removes all results from the data structure
	 */
	public void clear() {
		this.results.clear();
	}
	
	@Override
	public String toString() {
		boolean isFirst = true;
		StringBuilder sb = new StringBuilder();
		for (ResultSinkInfo sink : this.results.keySet())
			for (ResultSourceInfo source : this.results.get(sink)) {
				if (!isFirst)
					sb.append(", ");
				isFirst = false;
				
				sb.append(source);
				sb.append(" -> ");
				sb.append(sink);
			}
		return sb.toString();
	}

}
