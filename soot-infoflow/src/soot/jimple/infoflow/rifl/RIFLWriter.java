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
package soot.jimple.infoflow.rifl;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soot.jimple.infoflow.rifl.RIFLDocument.BottomDomain;
import soot.jimple.infoflow.rifl.RIFLDocument.Category;
import soot.jimple.infoflow.rifl.RIFLDocument.DomPair;
import soot.jimple.infoflow.rifl.RIFLDocument.DomainSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.FlowPair;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaFieldSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.JavaParameterSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkDomPair;
import soot.jimple.infoflow.rifl.RIFLDocument.SourceSinkSpec;
import soot.jimple.infoflow.rifl.RIFLDocument.TopDomain;

/**
 * Class for writing out RIFL-compliant data flow policies
 *
 * @author Steven Arzt
 */
public class RIFLWriter {

	private final RIFLDocument document;
	
	/**
	 * Creates a new instance of the {@link RIFLWriter} class
	 * @param document The document to write out
	 */
	public RIFLWriter(RIFLDocument document) {
		this.document = document;
	}
	
	public String write() {
		try {
			// Create a new document
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			
			Document document = documentBuilder.newDocument();
			Element rootElement = document.createElement("riflspec");
			document.appendChild(rootElement);
			
			writeAttackerIO(document, rootElement);
			writeDomains(document, rootElement);
			writeDomainAssignment(document, rootElement);
			writeDomainHierarchy(document, rootElement);
			writeFlowPolicy(document, rootElement);
			
			// Write it out
			StringWriter stringWriter = new StringWriter();
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stringWriter);
			transformer.transform(source, result);
			return stringWriter.toString();
		}
		catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex);
		}
		catch (TransformerConfigurationException ex) {
			throw new RuntimeException(ex);
		} catch (TransformerException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Writes out the attackerIO component of the RIFL document
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeAttackerIO(Document document, Element rootElement) {
		Element attackerIO = document.createElement("attackerio");
		rootElement.appendChild(attackerIO);

		Element sources = document.createElement("sources");
		attackerIO.appendChild(sources);
		for (SourceSinkSpec spec : this.document.getAttackerIO().getSources()) {
			Element source = document.createElement("source");
			sources.appendChild(source);
			writeSourceSinkSpec(spec, document, source);
		}
		
		Element sinks = document.createElement("sinks");
		attackerIO.appendChild(sinks);
		for (SourceSinkSpec spec : this.document.getAttackerIO().getSinks()) {
			Element sink = document.createElement("sink");
			sinks.appendChild(sink);
			writeSourceSinkSpec(spec, document, sink);
		}
	}

	/**
	 * Writes out a source/sink specification object
	 * @param spec The source/sink specification to write out
	 * @param document The document in which to write the source/sink specification
	 * @param parentElement The parent element in the DOM tree. This must be
	 * <source> or <sink>
	 */
	private void writeSourceSinkSpec(SourceSinkSpec spec, Document document,
			Element parentElement) {
		if (spec instanceof JavaParameterSpec)
			writeJavaParameterSpec((JavaParameterSpec) spec, document, parentElement);
		else if (spec instanceof JavaFieldSpec)
			writeJavaFieldSpec((JavaFieldSpec) spec, document, parentElement);
		else
			throw new RuntimeException("Unsupported source or sink specification type");
	}

	/**
	 * Writes out a source/sink specification object for Java method parameters
	 * @param spec The source/sink specification to write out
	 * @param document The document in which to write the source/sink specification
	 * @param parentElement The parent element in the DOM tree. This must be
	 * <source> or <sink>
	 */
	private void writeJavaParameterSpec(JavaParameterSpec spec,
			Document document, Element parentElement) {
		Element parameter = document.createElement("parameter");
		parentElement.appendChild(parameter);
		
		parameter.setAttribute("package", spec.getPackageName());
		parameter.setAttribute("class", spec.getClassName());
		parameter.setAttribute("method", spec.getHalfSignature());
		parameter.setAttribute("parameter", Integer.toString(spec.getParamIdx()));
	}

	/**
	 * Writes out a source/sink specification object for Java static fields
	 * @param spec The source/sink specification to write out
	 * @param document The document in which to write the source/sink specification
	 * @param parentElement The parent element in the DOM tree. This must be
	 * <source> or <sink>
	 */
	private void writeJavaFieldSpec(JavaFieldSpec spec,
			Document document, Element parentElement) {
		Element parameter = document.createElement("parameter");
		parameter.appendChild(parentElement);
		
		parameter.setAttribute("package", spec.getPackageName());
		parameter.setAttribute("class", spec.getClassName());
		parameter.setAttribute("field", spec.getFieldName());
	}

	/**
	 * Writes out the domains component of the RIFL document
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeDomains(Document document, Element rootElement) {
		Element domains = document.createElement("domains");
		rootElement.appendChild(domains);

		for (DomainSpec spec : this.document.getDomains())
			writeDomainSpec(spec, document, domains);
	}

	/**
	 * Writes out a domain specification object
	 * @param spec The domain specification to write out
	 * @param document The document in which to write the domain specification
	 * @param parentElement The parent element in the DOM tree.
	 */
	private void writeDomainSpec(DomainSpec spec, Document document, Element parentElement) {
		if (spec instanceof TopDomain) {
			Element topDomain = document.createElement("top");
			parentElement.appendChild(topDomain);
		}
		else if (spec instanceof BottomDomain) {
			Element bottomDomain = document.createElement("bottom");
			parentElement.appendChild(bottomDomain);
		}
		else if (spec instanceof Category) {
			Element categoryDomain = document.createElement("category");
			parentElement.appendChild(categoryDomain);

			Category cat = (Category) spec;
			categoryDomain.setAttribute("value", cat.getValue());
		}
		else
			throw new RuntimeException("Unsupported source or sink specification type");
	}

	/**
	 * Writes out the domains assignments section of the RIFL document
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeDomainAssignment(Document document, Element rootElement) {
		Element domainAssignment = document.createElement("domainassignment");
		rootElement.appendChild(domainAssignment);

		for (SourceSinkDomPair spec : this.document.getDomainAssignment())
			writeSourceSinkDomPair(spec, document, domainAssignment);
	}

	/**
	 * Writes out a source or sink domain pair
	 * @param pair The domain assignment to write out
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeSourceSinkDomPair(SourceSinkDomPair pair,
			Document document, Element rootElement) {
		final Element pairElement;
		final Element sourceSinkElement;
		switch (pair.getType()) {
			case SourceDomPair:
				pairElement = document.createElement("sourcedompair");
				sourceSinkElement = document.createElement("source");
				break;
			case SinkDomPair:
				pairElement = document.createElement("sinkdompair");
				sourceSinkElement = document.createElement("sink");
				break;
			default:
				throw new RuntimeException("Invalid source/sink domain pair type");
		}
		rootElement.appendChild(pairElement);
		
		pairElement.appendChild(sourceSinkElement);
		writeSourceSinkSpec(pair.getSourceOrSink(), document, sourceSinkElement);
		writeDomainSpec(pair.getDomain(), document, pairElement);
	}

	/**
	 * Writes out the domain hierarchy component of the RIFL document
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeDomainHierarchy(Document document, Element rootElement) {
		Element domainHierarchy = document.createElement("domainhierarchy");
		rootElement.appendChild(domainHierarchy);

		for (DomPair pair : this.document.getDomainHierarchy())
			writeDomainPair(pair, document, domainHierarchy);
	}

	/**
	 * Writes out a domain pair object for the use inside the domain hierarchy
	 * @param pair The domain pair to write out
	 * @param document The document in which to write the domain pair
	 * @param parentElement The parent element in the DOM tree
	 */
	private void writeDomainPair(DomPair pair, Document document, Element parentElement) {
		Element domPair = document.createElement("dompair");
		parentElement.appendChild(domPair);
		
		writeDomainSpec(pair.getFirstDomain(), document, domPair);
		writeDomainSpec(pair.getSecondDomain(), document, domPair);
	}
	
	/**
	 * Writes out the flow policy component of the RIFL document
	 * @param document The XML document in which to write
	 * @param rootElement The root element of the document
	 */
	private void writeFlowPolicy(Document document, Element rootElement) {
		Element flowPolicy = document.createElement("flowpolicy");
		rootElement.appendChild(flowPolicy);

		for (FlowPair pair : this.document.getFlowPolicy())
			writeFlowPair(pair, document, flowPolicy);
	}

	/**
	 * Writes out a flow pair object for the use inside the flow policy
	 * @param pair The flow pair to write out
	 * @param document The document in which to write the flow pair
	 * @param parentElement The parent element in the DOM tree
	 */
	private void writeFlowPair(FlowPair pair, Document document, Element parentElement) {
		Element flowPair = document.createElement("flowpair");
		parentElement.appendChild(flowPair);
		
		writeDomainSpec(pair.getFirstDomain(), document, flowPair);
		writeDomainSpec(pair.getSecondDomain(), document, flowPair);
	}

	/**
	 * Gets the document associated with this writer
	 * @return The document associated with this writer
	 */
	public RIFLDocument getDocument() {
		return this.document;
	}
}
