package soot.jimple.infoflow.android.test.xmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;

/**
 * Testing the new xml-parser with the new xml format
 * 
 * @author Jannik Juergens
 *
 */
public class XmlParserTest {

	/**
	 * Compares the new and the old Parser for different xml files
	 * 
	 * @param xmlFile
	 *            in new format
	 * @param oldXmlFile
	 * @throws IOException
	 */
	private void compareParserResults(String xmlFile, String oldXmlFile) throws IOException {
		XMLSourceSinkParser newParser = XMLSourceSinkParser.fromFile(xmlFile);
		PermissionMethodParser oldParser = PermissionMethodParser.fromFile(oldXmlFile);

		if (newParser != null && oldParser != null) {
			Assert.assertEquals(oldParser.getSources(), newParser.getSources());
			Assert.assertEquals(oldParser.getSinks(), newParser.getSinks());
		}
		else
			Assert.fail();
	}

	/**
	 * Test with a empty xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void emptyXmlTest() throws IOException {
		String xmlFile = "testXmlParser/empty.xml";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeXmlTest() throws IOException {
		String xmlFile = "testXmlParser/complete.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a empty txt file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void emptyTxtTest() throws IOException {
		String xmlFile = "testXmlParser/empty.txt";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete txt file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeTxtTest() throws IOException {
		String xmlFile = "testXmlParser/complete.txt";
		String oldXmlFile = "testXmlParser/completeOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void missingPartsXmlTest() throws IOException {
		String xmlFile = "testXmlParser/missingParts.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}
	
	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void notValidXmlTest() throws IOException {
		String xmlFile = "testXmlParser/notValid.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * manual verification of the parser result
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test
	public void verifyParserResultTest() throws IOException, XmlPullParserException {
		// parsing data from xml file
		String xmlFile = "testXmlParser/complete.xml";
		XMLSourceSinkParser newParser = XMLSourceSinkParser.fromFile(xmlFile);
		Set<SourceSinkDefinition> sourceListParser = newParser.getSources();
		Set<SourceSinkDefinition> sinkListParser = newParser.getSinks();

		// create two methods with reference data
		String methodName = "sourceTest";
		String returnType = "java.lang.String";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		methodParameters.add("int");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);

		methodParameters = new ArrayList<String>();
		methodParameters.add("double");
		methodParameters.add("double");
		AndroidMethod am2 = new AndroidMethod("sinkTest", methodParameters, "void",
				"com.example.androidtest.Sinks");
		
		// Check the loaded access paths (sources)
		Assert.assertEquals(1, sourceListParser.size());
		SourceSinkDefinition loadedSource = sourceListParser.iterator().next();
		Assert.assertEquals(am1, loadedSource.getMethod());
		Assert.assertEquals(0, loadedSource.getBaseObjectCount());
		Assert.assertEquals(2, loadedSource.getParameterCount());
		Assert.assertEquals(1, loadedSource.getReturnValueCount());
		
		// Check the loaded access paths (sinks)
		Assert.assertEquals(2, sinkListParser.size());
		for (SourceSinkDefinition def : sinkListParser) {
			Assert.assertTrue(def.getMethod().equals(am1) || def.getMethod().equals(am2));
			if (def.getMethod().equals(am1)) {
				Assert.assertEquals(1, def.getBaseObjectCount());
				Assert.assertEquals(1, def.getParameterCount());
			}
			else if (def.getMethod().equals(am2)) {
				Assert.assertEquals(1, def.getParameterCount());				
			}
			else
				Assert.fail("should never happen");
		}
	}
}
