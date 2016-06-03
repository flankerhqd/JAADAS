package soot.jimple.infoflow.results;

import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.data.AccessPath;
import soot.tagkit.LineNumberTag;

/**
 * Class for modeling information flowing into a specific source
 * @author Steven Arzt
 */
public class ResultSinkInfo {
	private final AccessPath accessPath;
	private final Stmt sink;
	
	public ResultSinkInfo(AccessPath sink, Stmt context) {
		assert sink != null;

		this.accessPath = sink;
		this.sink = context;
	}
	
	public AccessPath getAccessPath() {
		return this.accessPath;
	}
	
	public Stmt getSink() {
		return this.sink;
	}
	
	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder(sink == null
        		? accessPath.toString() : sink.toString());

        if (sink != null && sink.hasTag("LineNumberTag"))
            sb.append(" on line ").append(((LineNumberTag)sink.getTag("LineNumberTag")).getLineNumber());

		return sb.toString();
	}

	@Override
	public int hashCode() {
		return (Infoflow.getOneResultPerAccessPath() ? 31 * this.accessPath.hashCode() : 0)
				+ 7 * (this.sink == null ? 0 : this.sink.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		if (super.equals(o))
			return true;
		if (o == null || !(o instanceof ResultSinkInfo))
			return false;
		ResultSinkInfo si = (ResultSinkInfo) o;
		
		if (this.sink == null) {
			if (si.sink != null)
				return false;
		}
		else if (!this.sink.equals(si.sink))
			return false;
		
		return !Infoflow.getOneResultPerAccessPath()
				|| this.accessPath.equals(si.accessPath);
	}
}
