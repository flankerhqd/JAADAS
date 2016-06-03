package soot.jimple.infoflow.source;

import java.util.Collections;
import java.util.Set;

import soot.jimple.infoflow.data.AccessPath;

/**
 * Class containing additional information about a source. Users of FlowDroid
 * can derive from this class when implementing their own SourceSinkManager
 * to associate additional information with a source.
 * 
 * @author Steven Arzt, Daniel Magin
 */
public class SourceInfo {
	
	private final Object userData;
	private final Set<AccessPath> accessPaths;
	
	/**
	 * Creates a new instance of the {@link SourceInfo} class. This is a
	 * convenience constructor to allow for the simple use of a single access
	 * path.
	 * @param ap The single access path that shall be tainted at this source
	 */
	public SourceInfo(AccessPath ap){
		this(Collections.singleton(ap), null);
	}
	
	/**
	 * Creates a new instance of the {@link SourceInfo} class. This is a
	 * convenience constructor to allow for the simple use of a single access
	 * path.
	 * @param ap The single access path that shall be tainted at this source
	 * @param userData Additional user data to be propagated with the source
	 */
	public SourceInfo(AccessPath ap, Object userData){
		this(Collections.singleton(ap), userData);
	}
	
	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 * @param bundle Information about access paths tainted by this source
	 */
	public SourceInfo(Set<AccessPath> bundle){
		this(bundle, null);
	}
	
	/**
	 * Creates a new instance of the {@link SourceInfo} class
	 * @param bundle Information about access paths tainted by this source
	 * @param userData Additional user data to be propagated with the source
	 */
	public SourceInfo(Set<AccessPath> bundle, Object userData){
		this.userData = userData;
		this.accessPaths = bundle;
	}
	
	@Override
	public int hashCode() {
		return 31 * (this.userData == null ? 0 : this.userData.hashCode())
				+ 31 * (this.accessPaths == null ? 0 : this.accessPaths.hashCode());
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof SourceInfo))
			return false;
		SourceInfo otherInfo = (SourceInfo) other;
		if (this.userData == null) {
			if (otherInfo.userData != null)
				return false;
		}
		if(this.accessPaths == null){
			if(otherInfo.userData != null){
				return false;
			}
		}
		else if (!this.userData.equals(otherInfo.userData))
			return false;
		else if(!this.accessPaths.equals(otherInfo.accessPaths))
			return false;
		return true;
	}
	
	/**
	 * Gets the user data to be tracked together with this source
	 * @return The user data to be tracked together with this source
	 */
	public Object getUserData() {
		return this.userData;
	}

	/**
	 * Returns all access paths which are tainted by this source
	 * @return All access paths tainted by this source
	 */
	public Set<AccessPath> getAccessPaths(){
		return this.accessPaths;
	}
	
}
