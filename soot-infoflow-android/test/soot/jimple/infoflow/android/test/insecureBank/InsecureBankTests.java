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
package soot.jimple.infoflow.android.test.insecureBank;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class InsecureBankTests {
	
	private final static String sharedPrefs_putString =
			"<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,java.lang.String)>";

	private final static String activity_startActivity =
			"<android.app.Activity: void startActivity(android.content.Intent)>";
	private final static String activity_findViewById =
			"<android.app.Activity: android.view.View findViewById(int)>";
	private final static String activity_getIntent =
			"<android.app.Activity: android.content.Intent getIntent()>";

	private final static String url_init =
			"<java.net.URL: void <init>(java.lang.String)>";

	private final static String bundle_getString =
			"<android.os.Bundle: java.lang.String getString(java.lang.String)>";

	private final static String log_e =
			"<android.util.Log: int e(java.lang.String,java.lang.String)>";
	private final static String log_d =
			"<android.util.Log: int d(java.lang.String,java.lang.String)>";
	private final static String log_i =
			"<android.util.Log: int i(java.lang.String,java.lang.String)>";
	
	private final static String urlConnection_openConnection =
			"<java.net.URL: java.net.URLConnection openConnection()>";
	private final static String cursor_getString =
			"<android.database.Cursor: java.lang.String getString(int)>";
	
	/**
	 * Analyzes the given APK file for data flows
	 * @param enableImplicitFlows True if implicit flows shall be tracked,
	 * otherwise false
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required
	 * file could not be found
	 * @throws XmlPullParserException Thrown if the Android manifest file could
	 * not be read.
	 */
	private InfoflowResults analyzeAPKFile(boolean enableImplicitFlows) throws IOException, XmlPullParserException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);
		
		SetupApplication setupApplication = new SetupApplication(androidJars,
				"insecureBank" + File.separator + "InsecureBank.apk");
		setupApplication.setTaintWrapper(new EasyTaintWrapper("EasyTaintWrapperSource.txt"));
		setupApplication.setEnableImplicitFlows(enableImplicitFlows);
		setupApplication.setLayoutMatchingMode(LayoutMatchingMode.MatchAll);
		setupApplication.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		return setupApplication.runInfoflow();
	}

	@Test
	public void runTestInsecureBank() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile(false);
		// 7 leaks + 1x inter-component communication (server ip going through an intent)
		Assert.assertEquals(12, res.size());
		
		Assert.assertTrue(res.isPathBetweenMethods(activity_startActivity, activity_findViewById));

		Assert.assertTrue(res.isPathBetweenMethods(log_e, activity_getIntent));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, activity_findViewById));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, bundle_getString));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, urlConnection_openConnection));

		Assert.assertTrue(res.isPathBetweenMethods(log_d, cursor_getString));
		
		Assert.assertTrue(res.isPathBetweenMethods(sharedPrefs_putString, activity_findViewById));
		Assert.assertTrue(res.isPathBetweenMethods(sharedPrefs_putString, activity_findViewById));

		Assert.assertTrue(res.isPathBetweenMethods(log_i, activity_findViewById));
		
		Assert.assertTrue(res.isPathBetweenMethods(url_init, activity_getIntent));
		Assert.assertTrue(res.isPathBetweenMethods(url_init, activity_findViewById));
		Assert.assertTrue(res.isPathBetweenMethods(url_init, bundle_getString));
	}
	
}
