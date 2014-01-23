package org.opendatafoundation.data;

/*
 * Author(s): Pascal Heus (pheus@opendatafoundation.org)
 *  
 * This product has been developed with the financial and 
 * technical support of the UK Data Archive Data Exchange Tools 
 * project (http://www.data-archive.ac.uk/dext/) and the 
 * Open Data Foundation (http://www.opendatafoundation.org) 
 * 
 * Copyright 2007 University of Essex (http://www.esds.ac.uk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 * The full text of the license is also available on the Internet at 
 * http://www.gnu.org/copyleft/lesser.html
 * 
 */

import java.io.File;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ddialliance.ddi3.xml.xmlbeans.reusable.ReferenceType;
import org.ddialliance.ddieditor.ui.model.ElementType;
import org.ddialliance.ddieditor.util.DdiEditorConfig;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.xml.XmlBeansUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.opendatafoundation.data.spss.SPSSFile;
import org.opendatafoundation.data.spss.SPSSFileException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dk.dda.ddieditor.spss.osgi.Activator;
import dk.dda.ddieditor.spss.view.ProblemView;
import dk.dda.ddieditor.spss.view.IdentifierMarkerField;
import dk.dda.ddieditor.spss.view.StateMarkerField;
import dk.dda.ddieditor.spss.view.TypeMarkerField;

/**
 * Collection of utility functions
 * 
 * @author Pascal Heus (pheus@opendatafoundation.org)
 * 
 */
public class Utils {

	/**
	 * Converts an w3c.dom.Node into a String
	 * 
	 * @param node
	 * @return The DOM as a String
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws TransformerConfigurationException
	 */
	public static String DOM2String(Node node) throws TransformerException,
			TransformerFactoryConfigurationError {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		StringWriter writer = new StringWriter();
		transFactory.newTransformer().transform(new DOMSource(node),
				new StreamResult(writer));
		String result = writer.getBuffer().toString();
		return (result);
	}

	/**
	 * Converts an w3c.dom.Node into a String with intention and leaving XML
	 * declaration out
	 * 
	 * @param node
	 *            to transform
	 * @return string writer
	 * @throws TransformerException
	 * @throws TransformerFactoryConfigurationError
	 */
	public static StringWriter nodeToString(Node node)
			throws TransformerException, TransformerFactoryConfigurationError {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(node), new StreamResult(writer));
		return writer;
	}

	/**
	 * Converts an w3c.dom.Node into a color syntax HTML
	 * 
	 * @param node
	 * @return The HTML as a String
	 * @throws TransformerException
	 */
	public static String DOM2HTML(Node node) throws TransformerException {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		StringWriter writer = new StringWriter();
		Source xsltSource = new StreamSource(
				Utils.class.getResourceAsStream("xml2html.xslt")); // use
																	// this.getClass()
																	// instead
																	// for non
																	// static
																	// methods
		Transformer transformer = transFactory.newTransformer(xsltSource);
		StreamResult result = new StreamResult(System.out);
		transformer.transform(new DOMSource(node), result);
		return (writer.getBuffer().toString());
	}

	/**
	 * Pads a string left with spaces
	 * 
	 * @param str
	 * @return the padded string
	 */
	public static String leftPad(String str, int length) {
		return (leftPad(str, length, ' '));
	}

	/**
	 * Pads a string left with specified character
	 * 
	 * @param str
	 * @return the padded string
	 */
	public static String leftPad(String str, int length, char ch) {
		int padding = length - str.length();
		if (padding > 0) {
			char[] buf = new char[padding];
			for (int i = 0; i < padding; i++)
				buf[i] = ch;
			return (new String(buf) + str);
		} else
			return (str);
	}
	
	/**
	 * Format CSV output for String values
	 * 
	 * @param byteValue
	 * @param dataFormat
	 * @return
	 * @throws DDIFtpException 
	 */
	public static String formatCsvStringOutput(byte[] byteValue,
			FileFormatInfo dataFormat) throws DDIFtpException {
		String strValue = new String(byteValue).trim();
		return (formatCsvStringOutput(strValue, dataFormat));
	}
	
	/**
	 * Format CSV output for String values
	 * 
	 * @param byteValue
	 * @param dataFormat
	 * @return
	 * @throws DDIFtpException 
	 */
	public static String formatCsvStringOutput(String strValue,
			FileFormatInfo dataFormat) throws DDIFtpException {
		if (dataFormat.format == FileFormatInfo.Format.ASCII) {
			if (dataFormat.asciiFormat == FileFormatInfo.ASCIIFormat.FIXED) { // padding
				// strValue += Utils.leftPad("", fixedLength -
				// strValue.length());
				throw new DDIFtpException("Fixed Ascii format not support");
			} else if (dataFormat.asciiFormat == FileFormatInfo.ASCIIFormat.CSV) {
				// see http://en.wikipedia.org/wiki/Comma-separated_values
				// double the double-quote
				if (strValue.contains("\"")) {
					strValue = strValue.replaceAll("\"", "\"\"");
				}
				// surround by double-quote if contains comma, double-quote,
				// line break
				if (strValue.contains(",") || strValue.contains("\"")
						|| strValue.contains("\n")) {
					strValue = "\"" + strValue + "\"";
				}
			}
		}
		return strValue;
	}

	/**
	 * Writes and org.w3c.dom.Document to a file
	 * 
	 * @param doc
	 * @param filename
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	public static void writeXmlFile(Document doc, String filename)
			throws TransformerFactoryConfigurationError, TransformerException {
		// Prepare the DOM document for writing
		Source source = new DOMSource(doc);

		// Prepare the output file
		File file = new File(filename);
		Result result = new StreamResult(file);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.setOutputProperty(OutputKeys.INDENT, "yes");
		xformer.transform(source, result);
	}

	/**
	 * Writes and org.w3c.dom.Document to a file in a colored syntax HTML file
	 * 
	 * @param doc
	 * @param filename
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	public static void writeXmlFileAsHtml(Document doc, String filename)
			throws TransformerFactoryConfigurationError, TransformerException {
		// Prepare the DOM document for writing
		Source source = new DOMSource(doc);

		// Prepare the output file
		File file = new File(filename);
		Result result = new StreamResult(file);

		// Get transform
		Source xsltSource = new StreamSource(
				Utils.class.getResourceAsStream("xml2html.xslt")); // use
																	// this.getClass()
																	// instead
																	// for non
																	// static
																	// methods

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer(
				xsltSource);
		xformer.transform(source, result);
	}

	/**
	 * Adds id related attributes to DDI Identifiable element
	 * 
	 * @param e
	 * @param id
	 */
	public static void setDDIIdentifiableId(Element e, String id) {
		e.setAttribute("id", id);
	}

	/**
	 * Adds id related attributes to DDI Maintainable element
	 * 
	 * @param e
	 * @param id
	 */
	public static void setDDIMaintainableId(Element e, String id) {
		e.setAttribute("id", id);
		e.setAttribute("version", "1.0.0");
		e.setAttribute("agency", getAgency());
	}

	/**
	 * Adds id related attributes to DDI Versionable element
	 * 
	 * @param e
	 * @param id
	 */
	public static void setDDIVersionableId(Element e, String id) {
		e.setAttribute("id", id);
		e.setAttribute("version", "1.0.0");
	}

	public static ReferenceType getReference(Element e) {
		ReferenceType ref = ReferenceType.Factory.newInstance();
		// id
		ref.addNewID().setStringValue(e.getAttribute("id"));
		// version
		if (e.getAttribute("version") != null
				|| !e.getAttribute("version").equals("")) {
			ref.addNewVersion().setStringValue(e.getAttribute("version"));
		}
		// agency
		if (e.getAttribute("agency") != null
				|| !e.getAttribute("agency").equals("")) {
			ref.addNewVersion().setStringValue(e.getAttribute("agency"));
		}
		return ref;
	}

	/**
	 * Set reference
	 * 
	 * @param doc
	 *            of type reference type to set reference on
	 * @param ref
	 *            reference to set
	 */
	public static void setReference(Element elem, ReferenceType ref,
			Document doc) {
		String id = XmlBeansUtil.getTextOnMixedElement(ref.getIDList().get(0));

		Element idElem = (Element) elem.appendChild(doc.createElementNS(
				SPSSFile.DDI3_REUSABLE_NAMESPACE, "ID"));
		idElem.setTextContent(id);
		addVersionAndAgency(elem, doc);
	}

	public static void addVersionAndAgency(Element reference, Document doc) {
		// agency
		Element elem = (Element) reference.appendChild(doc.createElementNS(
				SPSSFile.DDI3_REUSABLE_NAMESPACE, "IdentifyingAgency"));
		elem.setTextContent(getAgency());
		// version
		elem = (Element) reference.appendChild(doc.createElementNS(
				SPSSFile.DDI3_REUSABLE_NAMESPACE, "Version"));
		elem.setTextContent("1.0.0");
	}

	static String agency;

	static String getAgency() {
		if (agency == null) {
			agency = DdiEditorConfig.get(DdiEditorConfig.DDI_AGENCY);
		}
		return agency;
	}

	/**
	 * Report non printable characters:
	 * - add error report to reportList
	 * - add error marker to Problem View 
	 * @param corrected
	 * @param input
	 * @param b
	 * @param id
	 * @param type
	 * @param reportList
	 * @throws DDIFtpException
	 */
	private static void reportNonPrintableError(boolean corrected, byte[] input, byte b,
			String id, String type, List<ValidationReportElement> reportList)
			throws DDIFtpException {
		if (reportList != null) {
			reportList.add(new ValidationReportElement(id, type, Translator
					.trans("spss.error.nonprintchar1", new Object[] { b })));
		}

		createMarker(corrected, type, id, "Non printable character '" + b
				+ "' replaced by white space in <" + new String(input) + ">");
	}

	/**
	 * Replace non printable characters - if requested
	 * @param correctError
	 * @param bytes
	 * @param id
	 * @param type
	 * @param reportList
	 * @return
	 * @throws DDIFtpException
	 */
	private static byte[] replaceNonPrintableBytes(boolean correctError,
			byte[] bytes, String id, String type,
			List<ValidationReportElement> reportList) throws DDIFtpException {
		for (int i = 0; i < bytes.length; i++) {
			// check single byte
			// check for non printable characters
			if ((bytes[i] > 0 && bytes[i] < ' ') || bytes[i] == '\n'
					|| bytes[i] == '\r' || bytes[i] == 127 /* DEL */) {
				// replace with white space
				// report replacement
				reportNonPrintableError(correctError, bytes, bytes[i], id, type, reportList);
				if (correctError) {
					bytes[i] = ' ';
				}
			}
		}
		return bytes;
	}

	/**
	 * Report UTF -8 error:
	 * - add error report to reportList
	 * - add error marker to Problem View 
	 * 
	 * @param corrected
	 * @param input
	 * @param b
	 * @param type
	 * @param id
	 * @param reportList
	 * @throws DDIFtpException
	 */
	private static void reportUtf8Error(boolean corrected, byte[] input, byte b, String type,
			String id, List<ValidationReportElement> reportList)
			throws DDIFtpException {
		if (reportList != null) {
			reportList.add(new ValidationReportElement(id, type, Translator
					.trans("spss.error.utf8error", new Object[] { b })));
		}

		createMarker(corrected, type, id, "Invalid UTF-8 character '" + b
				+ "' in  <" + new String(input) + ">");
	}

	/**
	 * Check input for invalid UTF-8 and non printable characters. If error
	 * detected marker is generated. If requested the error is corrected.
	 * 
	 * @param correctError
	 * @param input
	 * @param id
	 * @param type
	 * @throws DDIFtpException
	 */
	public static byte[] checkUTF8NonPrintable(boolean correctError,
			byte[] input, String id, String type,
			List<ValidationReportElement> reportList) throws DDIFtpException {
		// test - invalid:
		// byte[] input = { (byte) 0xc3, (byte) 0x50, (byte) 0x41, (byte) 0x42 };

		input = replaceNonPrintableBytes(correctError, input, id, type, reportList);

		CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
		ByteBuffer buf = ByteBuffer.wrap(input);

		try {
			decoder.decode(buf);
		} catch (CharacterCodingException e) {
			for (int i = 0; i < input.length; i++) {
				try {
					// check for utf-8 errors
					decoder.decode(ByteBuffer.wrap(input, i, 1));
				} catch (CharacterCodingException e1) {
					try {
						// if not last byte
						if (i + 1 < input.length) {
							// - check multiple byte
							decoder.decode(ByteBuffer.wrap(input, i, 2));
							i++;
						} else {
							reportUtf8Error(correctError, input, input[i], type, id,
									reportList);
							if (correctError) {
								// replace with white space
								input[i] = ' ';
							}
						}
					} catch (CharacterCodingException e2) {
						reportUtf8Error(correctError, input, input[i], type, id, reportList);
						if (correctError) {
							// replace with white space
							input[i] = ' ';
						}
					}
					continue;
				}
			}
		}
		return input;
	}

	/**
	 * Check label for UTF-8 error and non printable characters
	 * 
	 * @param string
	 * @throws SPSSFileException
	 */
	public static String validateLabel(boolean validateString,
			boolean correctString, String string, String id,
			List<ValidationReportElement> reportList) throws DDIFtpException {
		if (string.length() > 0 && validateString) {
			// check for invalid UTF-8 and non printable characters
			checkUTF8NonPrintable(correctString, string.getBytes(), id,
					ElementType.getElementType("Variable").getElementName()
							+ " - Label", reportList);
		}
		return string;
	}

	public static void createMarker(boolean corrected, String elementName,
			String identifier, String msg) throws DDIFtpException {
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IResource resource = workspace.getRoot();

			IMarker marker = (IMarker) resource
					.createMarker(ProblemView.MARKER_ID);
			if (corrected) {
				marker.setAttribute(StateMarkerField.DDI_STATE, "Corrected");
			} else {
				marker.setAttribute(StateMarkerField.DDI_STATE, "Error");
			}
			if (elementName != null) {
				marker.setAttribute(TypeMarkerField.DDI_TYPE, elementName);
			}
			if (identifier != null) {
				marker.setAttribute(IdentifierMarkerField.DDI_REFERENCE,
						identifier);
			}
			marker.setAttribute(IMarker.SOURCE_ID, Activator.PLUGIN_ID);
			marker.setAttribute(IMarker.MESSAGE, msg);
		} catch (CoreException e) {
			throw new DDIFtpException(e.getMessage(), e);
		}
	}

	public static void cleanMarkers() throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IMarker[] markers = root.findMarkers(ProblemView.MARKER_ID, false,
				IResource.DEPTH_ZERO);
		for (int i = 0; i < markers.length; i++) {
			String message = (String) markers[i]
					.getAttribute(IMarker.SOURCE_ID);
			if (message != null && message.equals(Activator.PLUGIN_ID)) {
				markers[i].delete();
			}
		}
	}

}
