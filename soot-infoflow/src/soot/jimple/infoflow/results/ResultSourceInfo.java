package soot.jimple.infoflow.results;

import java.util.List;

import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.data.AccessPath;
import soot.tagkit.LineNumberTag;

/**
 * Class for modeling information flowing out of a specific source
 * 
 * @author Steven Arzt
 */
public class ResultSourceInfo {
	private final AccessPath accessPath;
	private final Stmt source;
	private final Object userData;
	private final List<Stmt> path;
	
	public ResultSourceInfo(AccessPath source, Stmt context) {
		assert source != null;
		
		this.accessPath = source;
		this.source = context;
		this.userData = null;
		this.path = null;
	}
	
	public ResultSourceInfo(AccessPath source, Stmt context, Object userData, List<Stmt> path) {
		assert source != null;

		this.accessPath = source;
		this.source = context;
		this.userData = userData;
		this.path = path;
	}

	public AccessPath getAccessPath() {
		return this.accessPath;
	}
	
	public Stmt getSource() {
		return this.source;
	}
	
	public Object getUserData() {
		return this.userData;
	}
	
	public List<Stmt> getPath() {
		return this.path;
	}

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(source.toString());

        if (source.hasTag("LineNumberTag"))
            sb.append(" on line ").append(((LineNumberTag) source.getTag("LineNumberTag")).getLineNumber());

        return sb.toString();
    }

	@Override
	public int hashCode() {
		return (path != null && !Infoflow.getPathAgnosticResults() ? 31 * this.path.hashCode() : 0)
				+ (Infoflow.getOneResultPerAccessPath() ?
						31 * this.accessPath.hashCode() : 0)
				+ 7 * (this.source == null ? 0 : this.source.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		if (super.equals(o))
			return true;
		if (o == null || !(o instanceof ResultSourceInfo))
			return false;
		ResultSourceInfo si = (ResultSourceInfo) o;
		
		if (!Infoflow.getPathAgnosticResults()) {
			if (path == null && si.path != null)
				return false;
			if (path != null && si.path == null)
				return false;
			if (!path.equals(si.path))
				return false;
		}
		
		if (this.source == null) {
			if (si.source != null)
				return false;
		}
		else if (!this.source.equals(si.source))
			return false;
		
		return !Infoflow.getOneResultPerAccessPath()
				|| this.accessPath.equals(si.accessPath);
	}
}
