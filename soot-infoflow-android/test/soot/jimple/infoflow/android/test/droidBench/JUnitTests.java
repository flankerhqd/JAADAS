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
package soot.jimple.infoflow.android.test.droidBench;

import java.io.File;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class JUnitTests {
	
	/**
	 * Analyzes the given APK file for data flows
	 * @param fileName The full path and file name of the APK file to analyze
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required
	 * file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could
	 * not be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName)
			throws IOException, XmlPullParserException {
		return analyzeAPKFile(fileName, false);
	}
	
	/**
	 * Analyzes the given APK file for data flows
	 * @param fileName The full path and file name of the APK file to analyze
	 * @param enableImplicitFlows True if implicit flows shall be tracked,
	 * otherwise false
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required
	 * file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could
	 * not be read.
	 */
	public InfoflowResults analyzeAPKFile(String fileName, boolean enableImplicitFlows)
			throws IOException, XmlPullParserException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		String droidBenchDir = System.getenv("DROIDBENCH");
		if (droidBenchDir == null)
			droidBenchDir = System.getProperty("DROIDBENCH");
		if (droidBenchDir == null)
			throw new RuntimeException("DroidBench dir not set");		
		System.out.println("Loading DroidBench from " + droidBenchDir);
		
		SetupApplication setupApplication = new SetupApplication(androidJars,
				droidBenchDir + File.separator + fileName);
		setupApplication.setTaintWrapper(new EasyTaintWrapper("EasyTaintWrapperSource.txt"));
		setupApplication.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		setupApplication.setEnableImplicitFlows(enableImplicitFlows);
		return setupApplication.runInfoflow();
	}

}
