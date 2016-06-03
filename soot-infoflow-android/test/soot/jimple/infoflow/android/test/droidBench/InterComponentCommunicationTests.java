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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

@Ignore
public class InterComponentCommunicationTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestActivityCommunication1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/ActivityCommunication1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestIntentSink1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/IntentSink1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestIntentSink2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/IntentSink2.apk");
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestSharedPreferences1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/SharedPreferences1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore		// we do not support interleaved component executions yet
	public void runTestSingletons1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("InterComponentCommunication/Singletons1.apk");
		Assert.assertEquals(1, res.size());
	}

}
