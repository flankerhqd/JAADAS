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

public class EmulatorDetectionTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestContentProvider1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/ContentProvider1.apk");
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	@Ignore		// does not work, need to check
	public void runTestIMEI1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/IMEI1.apk");
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestPlayStore1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("EmulatorDetection/PlayStore1.apk");
		Assert.assertEquals(2, res.size());
	}
	
}
