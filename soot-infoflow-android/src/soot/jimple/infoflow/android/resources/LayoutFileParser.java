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
package soot.jimple.infoflow.android.resources;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pxb.android.axml.AxmlVisitor;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;

/**
 * Parser for analyzing the layout XML files inside an android application
 * 
 * @author Steven Arzt
 *
 */
public class LayoutFileParser extends AbstractResourceParser {
	
	private static final boolean DEBUG = true;
	
	private final Map<String, Set<LayoutControl>> userControls = new HashMap<String, Set<LayoutControl>>();
	private final Map<String, Set<String>> callbackMethods = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> includeDependencies = new HashMap<String, Set<String>>();
	private final String packageName;
	private final ARSCFileParser resParser;
	
	private final static int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
	private final static int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
	private final static int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
	private final static int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;
	
	public LayoutFileParser(String packageName, ARSCFileParser resParser) {
		this.packageName = packageName;
		this.resParser = resParser;
	}
	
	private SootClass getLayoutClass(String className) {
		// Cut off some junk returned by the parser
		if (className.startsWith(";"))
			className = className.substring(1);
		
		if (className.contains("(") || className.contains("<") || className.contains("/")) {
			System.err.println("Invalid class name " + className);
			return null;
		}
		
		SootClass sc = Scene.v().forceResolve(className, SootClass.BODIES);
		if ((sc == null || sc.isPhantom()) && !packageName.isEmpty())
			sc = Scene.v().forceResolve(packageName + "." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.view." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.widget." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.webkit." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom()) {
   			System.err.println("Could not find layout class " + className);
   			return null;
		}
		return sc;		
	}
	
	private boolean isLayoutClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
   		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOf(theClass))
   			if (parent.getName().equals("android.view.ViewGroup")) {
   				found = true;
   				break;
   			}
   		return found;
	}
	
	private boolean isViewClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(theClass))
   			if (parent.getName().equals("android.view.View")
   					|| parent.getName().equals("android.webkit.WebView")) {
   				found = true;
   				break;
   			}
   		if (!found) {
   			System.err.println("Layout class " + theClass.getName() + " is not derived from "
   					+ "android.view.View");
   			return false;
   		}
   		return true;
	}
	
	/**
	 * Checks whether the given namespace belongs to the Android operating system
	 * @param ns The namespace to check
	 * @return True if the namespace belongs to Android, otherwise false
	 */
	private boolean isAndroidNamespace(String ns) {
		if (ns == null)
			return false;
		ns = ns.trim();
		if (ns.startsWith("*"))
			ns = ns.substring(1);
		if (!ns.equals("http://schemas.android.com/apk/res/android"))
			return false;
		return true;
	}
	
	private <X,Y> void addToMapSet(Map<X, Set<Y>> target, X layoutFile, Y callback) {
		if (target.containsKey(layoutFile))
			target.get(layoutFile).add(callback);
		else {
			Set<Y> callbackSet = new HashSet<Y>();
			callbackSet.add(callback);
			target.put(layoutFile, callbackSet);
		}
	}

	/**
	 * Adds a callback method found in an XML file to the result set
	 * @param layoutFile The XML file in which the callback has been found
	 * @param callback The callback found in the given XML file
	 */
	private void addCallbackMethod(String layoutFile, String callback) {
		addToMapSet(callbackMethods, layoutFile, callback);
		
		// Recursively process any dependencies we might have collected before
		// we have processed the target
		if (includeDependencies.containsKey(layoutFile))
			for (String target : includeDependencies.get(layoutFile))
				addCallbackMethod(target, callback);
	}	
	
	/**
	 * Parses all layout XML files in the given APK file and loads the IDs of
	 * the user controls in it.
	 * @param fileName The APK file in which to look for user controls
	 */
	public void parseLayoutFile(final String fileName, final Set<String> classes) {
		Transform transform = new Transform("wjtp.lfp", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				handleAndroidResourceFiles(fileName, /*classes,*/ null, new IResourceHandler() {
						
					@Override
					public void handleResourceFile(final String fileName, Set<String> fileNameFilter, InputStream stream) {
						// We only process valid layout XML files
						if (!fileName.startsWith("res/layout"))
							return;
						if (!fileName.endsWith(".xml")) {
							System.err.println("Skipping file " + fileName + " in layout folder...");
							return;
						}
						
						// Get the fully-qualified class name
						String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
						if (!packageName.isEmpty())
							entryClass = packageName + "." + entryClass;
						
						// We are dealing with resource files
						if (!fileName.startsWith("res/layout"))
							return;
						if (fileNameFilter != null) {
							boolean found = false;
							for (String s : fileNameFilter)
								if (s.equalsIgnoreCase(entryClass)) {
									found = true;
									break;
								}
							if (!found)
								return;
						}
						
						try {
							AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
							parseLayoutNode(fileName, handler.getDocument().getRootNode());
							System.out.println("Found " + userControls.size() + " layout controls in file "
									+ fileName);
						}
						catch (Exception ex) {
							System.err.println("Could not read binary XML file: " + ex.getMessage());
							ex.printStackTrace();
						}
					}
				});
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}
	
	/**
	 * Parses the layout file with the given root node
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode The root node from where to start parsing
	 */
	private void parseLayoutNode(String layoutFile, AXmlNode rootNode) {
		if (rootNode.getTag() == null || rootNode.getTag().isEmpty()) {
			System.err.println("Encountered a null or empty node name "
					+ "in file " + layoutFile + ", skipping node...");
			return;
		}
		
		String tname = rootNode.getTag().trim();
		if (tname.equals("dummy")) {
			// dummy root node, ignore it
		}
		// Check for inclusions
		else if (tname.equals("include")) {
			parseIncludeAttributes(layoutFile, rootNode);
		}
		// The "merge" tag merges the next hierarchy level into the current
		// one for flattening hierarchies.
		else if (tname.equals("merge"))  {
			// do not consider any attributes of this elements, just
			// continue with the children
		}
		else if (tname.equals("fragment"))  {
			final AXmlAttribute<?> attr = rootNode.getAttribute("name");
			if (attr == null)
				System.err.println("Fragment without class name detected");
			else {
				if (attr.getType() != AxmlVisitor.TYPE_STRING)
					System.err.println("Invalid targer resource "+attr.getValue()+"for fragment class value");
				getLayoutClass(attr.getValue().toString());
			}
		}
		else {
			final SootClass childClass = getLayoutClass(tname);
			if (childClass != null && (isLayoutClass(childClass) || isViewClass(childClass)))
				parseLayoutAttributes(layoutFile, childClass, rootNode);
		}

		// Parse the child nodes
		for (AXmlNode childNode : rootNode.getChildren())
			parseLayoutNode(layoutFile, childNode);
	}
	
	/**
	 * Parses the attributes required for a layout file inclusion
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode The AXml node containing the attributes
	 */
	private void parseIncludeAttributes(String layoutFile, AXmlNode rootNode) {
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();
			
    		if (attrName.equals("layout")) {
    			if ((attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)
    					&& attr.getValue() instanceof Integer) {
    				// We need to get the target XML file from the binary manifest
    				AbstractResource targetRes = resParser.findResource((Integer) attr.getValue());
    				if (targetRes == null) {
    					System.err.println("Target resource " + attr.getValue() + " for layout include not found");
    					return;
    				}
    				if (!(targetRes instanceof StringResource)) {
    					System.err.println("Invalid target node for include tag in layout XML, was "
    							+ targetRes.getClass().getName());
    					return;
    				}
    				String targetFile = ((StringResource) targetRes).getValue();
    				
    				// If we have already processed the target file, we can
    				// simply copy the callbacks we have found there
        			if (callbackMethods.containsKey(targetFile))
        				for (String callback : callbackMethods.get(targetFile))
        					addCallbackMethod(layoutFile, callback);
        			else {
        				// We need to record a dependency to resolve later
        				addToMapSet(includeDependencies, targetFile, layoutFile);
        			}
    			}
    		}
		}
	}

	/**
	 * Parses the layout attributes in the given AXml node 
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param layoutClass The class for the attributes are parsed
	 * @param rootNode The AXml node containing the attributes
	 */
	private void parseLayoutAttributes(String layoutFile, SootClass layoutClass, AXmlNode rootNode) {
		boolean isSensitive = false;
		int id = -1;
		
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();
			
			// On obfuscated Android malware, the attribute name may be empty
			if (attrName.isEmpty())
				continue;
			
			// Check that we're actually working on an android attribute
			if (!isAndroidNamespace(attr.getNamespace()))
				continue;
			
			// Read out the field data
			if (attrName.equals("id")
					&& (attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX))
				id = (Integer) attr.getValue();
			else if (attrName.equals("password")) {
				if (attr.getType() == AxmlVisitor.TYPE_INT_HEX)
					isSensitive = ((Integer) attr.getValue()) != 0; // -1 for true, 0 for false
				else if (attr.getType() == AxmlVisitor.TYPE_INT_BOOLEAN)
					isSensitive = (Boolean) attr.getValue();
				else
					throw new RuntimeException("Unknown representation of boolean data type");
			}
			else if (!isSensitive && attrName.equals("inputType") && attr.getType() == AxmlVisitor.TYPE_INT_HEX) {
				int tp = (Integer) attr.getValue();
				isSensitive = ((tp & TYPE_NUMBER_VARIATION_PASSWORD) == TYPE_NUMBER_VARIATION_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_PASSWORD) == TYPE_TEXT_VARIATION_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_WEB_PASSWORD) == TYPE_TEXT_VARIATION_WEB_PASSWORD);
			}
			else if (isActionListener(attrName)
					&& attr.getType() == AxmlVisitor.TYPE_STRING
					&& attr.getValue() instanceof String) {
				String strData = ((String) attr.getValue()).trim();
				addCallbackMethod(layoutFile, strData);
			}
			else if (attr.getType() == AxmlVisitor.TYPE_STRING && attrName.equals("text")) {
				// To avoid unrecognized attribute for "text" field
			}
			else if (DEBUG && attr.getType() == AxmlVisitor.TYPE_STRING) {
				System.out.println("Found unrecognized XML attribute:  " + attrName);
			}
		}
		
		// Register the new user control
		addToMapSet(this.userControls, layoutFile, new LayoutControl(id, layoutClass, isSensitive));
	}

	/**
	 * Checks whether this name is the name of a well-known Android listener
	 * attribute. This is a function to allow for future extension.
	 * @param name The attribute name to check. This name is guaranteed to
	 * be in the android namespace.
	 * @return True if the given attribute name corresponds to a listener,
	 * otherwise false.
	 */
	private boolean isActionListener(String name) {
		return name.equals("onClick");
	}

	/**
	 * Gets the user controls found in the layout XML file. The result is a
	 * mapping from the id to the respective layout control.
	 * @return The layout controls found in the XML file.
	 */
	public Map<Integer, LayoutControl> getUserControlsByID() {
		Map<Integer, LayoutControl> res = new HashMap<Integer, LayoutControl>();
		for (Set<LayoutControl> controls : this.userControls.values())
			for (LayoutControl lc : controls)
				res.put(lc.getID(), lc);
		return res;
	}

	/**
	 * Gets the user controls found in the layout XML file. The result is a
	 * mapping from the file name in which the control was found to the
	 * respective layout control.
	 * @return The layout controls found in the XML file.
	 */
	public Map<String, Set<LayoutControl>> getUserControls() {
		return this.userControls;
	}

	/**
	 * Gets the callback methods found in the layout XML file. The result is a
	 * mapping from the file name to the set of found callback methods.
	 * @return The callback methods found in the XML file.
	 */
	public Map<String, Set<String>> getCallbackMethods() {
		return this.callbackMethods;
	}
	
}
