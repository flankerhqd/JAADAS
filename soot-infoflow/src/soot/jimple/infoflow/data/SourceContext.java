package soot.jimple.infoflow.data;

import soot.jimple.Stmt;

/**
 * Class representing a source value together with the statement that created it
 * 
 * @author Steven Arzt
 */
public class SourceContext implements Cloneable {
	private final AccessPath accessPath;
	private final Stmt stmt;
	private final Object userData;
	
	private int hashCode = 0;
	
	public SourceContext(AccessPath accessPath, Stmt stmt) {
		assert accessPath != null;
		
		this.accessPath = accessPath;
		this.stmt = stmt;
		this.userData = null;
	}
	
	public SourceContext(AccessPath accessPath, Stmt stmt, Object userData) {
		assert accessPath != null;

		this.accessPath = accessPath;
		this.stmt = stmt;
		this.userData = userData;
	}
	
	public AccessPath getAccessPath() {
		return this.accessPath;
	}
	
	public Stmt getStmt() {
		return this.stmt;
	}
	
	public Object getUserData() {
		return this.userData;
	}
	
	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
		result = prime * result + ((accessPath == null) ? 0 : accessPath.hashCode());
		result = prime * result + ((userData == null) ? 0 : userData.hashCode());
		hashCode = result;
		
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		SourceContext other = (SourceContext) obj;
		
		if (this.hashCode != 0 && other.hashCode != 0 && this.hashCode != other.hashCode)
			return false;
		
		if (stmt == null) {
			if (other.stmt != null)
				return false;
		} else if (!stmt.equals(other.stmt))
			return false;
		if (accessPath == null) {
			if (other.accessPath != null)
				return false;
		} else if (!accessPath.equals(other.accessPath))
			return false;
		if (userData == null) {
			if (other.userData != null)
				return false;
		} else if (!userData.equals(other.userData))
			return false;
		return true;
	}
	
	@Override
	public SourceContext clone() {
		SourceContext sc = new SourceContext(accessPath, stmt, userData);
		assert sc.equals(this);
		return sc;
	}

	@Override
	public String toString() {
		return accessPath.toString() + (stmt == null ? "" : (" in " + stmt.toString()));
	}
}
