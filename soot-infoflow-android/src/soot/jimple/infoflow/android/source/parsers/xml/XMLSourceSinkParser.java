package soot.jimple.infoflow.android.source.parsers.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import soot.jimple.infoflow.android.source.data.AccessPathTuple;
import soot.jimple.infoflow.android.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.android.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.data.SootMethodAndClass;

/**
 * Parses informations from the new Dataformat (XML) with the help of SAX. Returns only a Set of Android Method when
 * calling the function parse. For the AccessPath the class SaxHandler is used. 
 * 
 * @author Anna-Katharina Wickert
 * @author Joern Tillmans
 * @author Steven Arzt
 */

public class XMLSourceSinkParser extends DefaultHandler implements ISourceSinkDefinitionProvider {
		
	// Holding temporary values for handling with SAX
	private String methodSignature;
	private String methodCategory;
	private boolean isSource, isSink;
	private String[] pathElements;
	private String[] pathElementTypes;
	private int paramIndex;
	private List<String> paramTypes = new ArrayList<String>();
	
	private String accessPathParentElement = "";
	
	private Set<AccessPathTuple> baseAPs = new HashSet<>();
	private List<Set<AccessPathTuple>> paramAPs = new ArrayList<>();
	private Set<AccessPathTuple> returnAPs = new HashSet<>();
	
	private Map<SootMethodAndClass, SourceSinkDefinition> sourcesAndSinks;
	private Set<SourceSinkDefinition> sources = new HashSet<>();
	private Set<SourceSinkDefinition> sinks = new HashSet<>();
	
	// XML stuff incl. Verification against XSD
	private static final String XSD_FILE_PATH = "exchangeFormat.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static XMLSourceSinkParser fromFile(String fileName) throws IOException {
		if (!verifyXML(fileName)) {
			throw new RuntimeException("The XML-File isn't valid");
		}
		XMLSourceSinkParser pmp = new XMLSourceSinkParser(fileName);
		return pmp;
	}
	
	@Override
	public Set<SourceSinkDefinition> getSources() {
		return sources;
	}
	
	@Override
	public Set<SourceSinkDefinition> getSinks() {
		return sinks;
	}
	
	/**
	 * Event Handler for the starting element for SAX. Possible start elements
	 * for filling AndroidMethod objects with the new data format: - method:
	 * Setting parsingvalues to false or null and get and set the signature and
	 * category, - accessPath: To get the information whether the AndroidMethod
	 * is a sink or source, - and the other elements doesn't care for creating
	 * the AndroidMethod object. At these element we will look, if calling the
	 * method getAccessPath (using an new SAX Handler)
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {
		case XMLConstants.METHOD_TAG:
			if (attributes != null) {
				methodSignature = attributes.getValue(XMLConstants.SIGNATURE_ATTRIBUTE).trim();
				methodCategory = attributes.getValue(XMLConstants.CATEGORY_ATTRIBUTE).trim();
			}
			break;

		case XMLConstants.ACCESSPATH_TAG:
			if (attributes != null) {
				String tempStr = attributes.getValue(XMLConstants.IS_SOURCE_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty())
					isSource = tempStr.equalsIgnoreCase(XMLConstants.TRUE);
				
				tempStr = attributes.getValue(XMLConstants.IS_SINK_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty())
					isSink = tempStr.equalsIgnoreCase(XMLConstants.TRUE);
				
				tempStr = attributes.getValue(XMLConstants.LENGTH_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty()) {
					pathElements = new String[Integer.parseInt(tempStr)];
					pathElementTypes = new String[Integer.parseInt(tempStr)];
				}
			}
			break;
		
		case XMLConstants.BASE_TAG:
//			if (methodSignature != null && attributes != null)
//				baseType = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE).trim();
			accessPathParentElement = qNameLower;
			break;

		case XMLConstants.RETURN_TAG:
//			if (methodSignature != null && attributes != null)
//				returnType = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE).trim();
			accessPathParentElement = qNameLower;
			break;

		case XMLConstants.PARAM_TAG:
			if (methodSignature != null && attributes != null) {
//				paramType = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE).trim();

				String tempStr = attributes.getValue(XMLConstants.INDEX_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty())
					paramIndex = Integer.parseInt(tempStr);
				
				tempStr = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty())
					paramTypes.add(tempStr.trim());
			}
			accessPathParentElement = qNameLower;
			break;
			
		case XMLConstants.PATHELEMENT_TAG:
			if (methodSignature != null && attributes != null) {
				int pathElementIdx = -1;
				String tempStr = attributes.getValue(XMLConstants.INDEX_ATTRIBUTE);
				if (tempStr != null && !tempStr.isEmpty()) {
					pathElementIdx = Integer.parseInt(tempStr.trim());
					
					tempStr = attributes.getValue(XMLConstants.FIELD_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty()) {
						if (pathElementIdx >= pathElements.length)
							throw new RuntimeException("Path element index out of range");
						pathElements[pathElementIdx] = tempStr;
					}
					
					tempStr = attributes.getValue(XMLConstants.TYPE_ATTRIBUTE);
					if (tempStr != null && !tempStr.isEmpty()) {
						if (pathElementIdx >= pathElementTypes.length)
							throw new RuntimeException("Path element type index out of range");
						pathElementTypes[pathElementIdx] = tempStr;
					}
				}
			}
			break;
		}
	}

	/**
	 * PathElement is the only element having values inside -> nothing to do
	 * here. Doesn't care at the current state of parsing.
	**/
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

	/**
	 * EventHandler for the End of an element. -> Putting the values into the objects. For additional information:
	 * startElement description. Starting with the innerst elements and switching up to the outer elements
	 * 
	 * - pathElement -> means field sensitive, adding SootFields
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {
		
		// Create the new method based on the data we have collected so far
		case XMLConstants.METHOD_TAG:
			if (methodSignature == null)
				break;
			
			AndroidMethod tempMeth = AndroidMethod.createFromSignature(methodSignature);			
			if (methodCategory != null) {
				String methodCategoryUpper = methodCategory.toUpperCase().trim();
				tempMeth.setCategory(CATEGORY.valueOf(methodCategoryUpper));
			}
			tempMeth.setSink(isSink);
			tempMeth.setSource(isSource);
			
			@SuppressWarnings("unchecked")
			SourceSinkDefinition ssd = new SourceSinkDefinition(tempMeth,
					baseAPs, paramAPs.toArray(new Set[paramAPs.size()]), returnAPs);
			if (sourcesAndSinks.containsKey(tempMeth))
				sourcesAndSinks.get(tempMeth).merge(ssd);
			else
				sourcesAndSinks.put(tempMeth, ssd);
			
			// Start a new method and discard our old data
			methodSignature = null;
			methodCategory = null;
			baseAPs = new HashSet<>();
			paramAPs = new ArrayList<>();
			returnAPs = new HashSet<>();
			break;
		
		case XMLConstants.ACCESSPATH_TAG:
			// Record the access path for the current element
			if (isSource || isSink) {
				// Clean up the path elements
				if (pathElements != null
						&& pathElements.length == 0
						&& pathElementTypes != null
						&& pathElementTypes.length == 0) {
					pathElements = null;
					pathElementTypes = null;
				}
				
				AccessPathTuple apt = AccessPathTuple.fromPathElements(
						pathElements, pathElementTypes, isSource, isSink);
				switch (accessPathParentElement) {
					case XMLConstants.BASE_TAG:
						baseAPs.add(apt);
						break;
					case XMLConstants.RETURN_TAG:
						returnAPs.add(apt);
						break;
					case XMLConstants.PARAM_TAG:
						while (paramAPs.size() <= paramIndex)
							paramAPs.add(new HashSet<AccessPathTuple>());
						paramAPs.get(paramIndex).add(apt);
				}
			}
			
			isSource = false;
			isSink = false;
			pathElements = null;
			pathElementTypes = null;
			break;
			
		case XMLConstants.BASE_TAG:
			accessPathParentElement = "";
//			baseType = "";
			break;
			
		case XMLConstants.RETURN_TAG:
			accessPathParentElement = "";
//			returnType = "";
			break;
			
		case XMLConstants.PARAM_TAG:
			accessPathParentElement = "";
			paramIndex = -1;
			paramTypes.clear();
//			paramType = "";
			break;
			
		case XMLConstants.PATHELEMENT_TAG:
			break;
		}
	}
	
	private XMLSourceSinkParser(String fileName) {
		// Parse the data
		sourcesAndSinks = new HashMap<>();
		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			SAXParser parser = pf.newSAXParser();
			parser.parse(fileName, this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Build the source and sink lists
		for (SourceSinkDefinition def : sourcesAndSinks.values()) {
			SourceSinkDefinition sourceDef = def.getSourceOnlyDefinition();
			if (!sourceDef.isEmpty())
				sources.add(sourceDef);
			SourceSinkDefinition sinkDef = def.getSinkOnlyDefinition();
			if (!sinkDef.isEmpty())
				sinks.add(sinkDef);
		}
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data format.
	 * 
	 * @param fileName
	 *            of the XML
	 * @return true = valid XML false = invalid XML
	 */
	private static boolean verifyXML(String fileName) {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		Source xsdFile = new StreamSource(new File(XSD_FILE_PATH));
		Source xmlFile = new StreamSource(new File(fileName));
		boolean validXML = false;
		try {
			Schema schema = sf.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			try {
				validator.validate(xmlFile);
				validXML = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!validXML) {
				new IOException("File isn't  valid against the xsd");
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return validXML;
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sources.size()
				+ sinks.size());
		sourcesSinks.addAll(sources);
		sourcesSinks.addAll(sinks);
		return sourcesSinks;
	}
}
