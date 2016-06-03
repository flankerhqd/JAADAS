package soot.jimple.infoflow.util;

/**
 * Utility class for checking whether methods belong to system classes
 * 
 * @author Steven Arzt
 */
public class SystemClassHandler {
	
	/**
	 * Checks whether the given class name belongs to a system package
	 * @param className The class name to check
	 * @return True if the given class name belongs to a system package,
	 * otherwise false
	 */
	public static boolean isClassInSystemPackage(String className) {
		return className.startsWith("android.")
				|| className.startsWith("java.")
				|| className.startsWith("sun.");
	}

}
