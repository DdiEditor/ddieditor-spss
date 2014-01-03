package dk.dda.ddieditor.spss.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.XmlObject;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategorySchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategoryType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeRepresentationType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeSchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.VariableDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.VariableSchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.VariableType;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.BaseRecordLayoutType;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.DataItemType;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.PhysicalLocationType;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.RecordLayoutDocument;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.RecordLayoutSchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.physicaldataproduct.RecordLayoutType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.CategoryRelationCodeType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.DateTimeRepresentationType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.IDType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.LabelType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.NumericRepresentationType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.ReferenceType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.TextRepresentationType;
import org.ddialliance.ddieditor.logic.urn.ddi.ReferenceResolution;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.ui.dbxml.category.CategorySchemeDao;
import org.ddialliance.ddieditor.ui.dbxml.code.CodeSchemeDao;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.model.variable.Variable;
import org.ddialliance.ddieditor.ui.util.LanguageUtil;
import org.ddialliance.ddieditor.util.DdiEditorConfig;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.ResourceBundleManager;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.ddialliance.ddiftp.util.xml.XmlBeansUtil;
import org.eclipse.core.runtime.Platform;

import dk.dda.ddieditor.spss.SPSSTime;
import dk.dda.ddieditor.spss.wizard.ExportSpssWizard;

/*
 * Copyright 2012 Danish Data Archive (http://www.dda.dk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 3 of the License, or 
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
 */

/**
 * Create SPSS syntax file based on DDI-L
 * 
 * @author ddajvj
 */
public class SpssExportRunnable implements Runnable {
	private Log log = LogFactory.getLog(LogType.SYSTEM, ExportSpss.class);
	// input
	ExportSpssWizard exportSpssWizard;
	List<DataItemType> dataItems = new ArrayList<DataItemType>();
	Variable vari = null;
	String label = null;

	//
	// spss
	//
	BufferedWriter writer = null;
	StringBuffer spssSyntaxTxt = new StringBuffer();

	// VARIABLE
	List<String> variables = new ArrayList<String>();

	// VARIABLE LABELS
	List<String> variableLabels = new ArrayList<String>();

	// VALUE LABELS
	List<String> valueLabels = new ArrayList<String>();
	Map<String, String> codsVariToMissing = new HashMap<String, String>();
	Map<String, String> scaleVariToMissing = new HashMap<String, String>();

	// MISSING VALUES
	List<String> missingValues = new ArrayList<String>();

	// comment out 20140103
	// int[] unknown = new int[] { 9, 99, 999, 9999, 99999, 999999, 99999999 };
	// int[] irrelevant = new int[] { 10, 100, 1000, 10000, 100000, 1000000,
	// 10000000 };
	// int[] didnotparticipate = new int[] { 11, 101, 1001, 10001, 100001,
	// 1000001, 10000001 };

	Pattern unknownPattern = Pattern.compile("^9+$");
	Pattern irrelevantPattern = Pattern.compile("^10+$");
	Pattern didnotparticipatePattern = Pattern.compile("^1{1}0*1{1}$");

	String[] missingValueLabels = null;

	// VARIABLE LEVEL
	List<String> variableLevels = new ArrayList<String>();

	public SpssExportRunnable(ExportSpssWizard exportSpssWizard) {
		this.exportSpssWizard = exportSpssWizard;

		Locale locale = null;
		locale = new Locale(DdiEditorConfig.get(DdiEditorConfig.DDI_LANGUAGE));
		ResourceBundle rb = ResourceBundle
				.getBundle(ResourceBundleManager.BUNDLE_DIRECTORY
						+ "spss-message", locale);

		missingValueLabels = new String[] {
				rb.getString("spss.export.missingunknown"),
				rb.getString("spss.export.missingirrelevant"),
				rb.getString("spss.export.missingdidnotparticipate") };
	}

	@Override
	public void run() {
		// traverse ddi vars to define spss syntax
		try {
			DdiManager.getInstance().setWorkingDocument(
					exportSpssWizard.selectedResource.getOrgName());

			List<LightXmlObjectType> vars = DdiManager.getInstance()
					.getVariableSchemesLight(null, null, null, null)
					.getLightXmlObjectList().getLightXmlObjectList();

			List<LightXmlObjectType> relsLight = DdiManager.getInstance()
					.getRecordLayoutSchemesLight(null, null, null, null)
					.getLightXmlObjectList().getLightXmlObjectList();

			for (LightXmlObjectType lightXmlObjectType : relsLight) {
				RecordLayoutSchemeDocument rels = DdiManager.getInstance()
						.getRecordLayoutScheme(lightXmlObjectType.getId(),
								lightXmlObjectType.getVersion(),
								lightXmlObjectType.getParentId(),
								lightXmlObjectType.getParentVersion());
				for (BaseRecordLayoutType baseRecordLayout : rels
						.getRecordLayoutScheme().getBaseRecordLayoutList()) {
					RecordLayoutType recordLayout = (RecordLayoutType) baseRecordLayout
							.substitute(RecordLayoutDocument.type
									.getDocumentElementName(),
									RecordLayoutType.type);
					dataItems.addAll(recordLayout.getDataItemList());
				}
			}

			VariableDocument variDoc = null;
			String measure = null;
			List<String> tmpMissing = new ArrayList<String>();
			for (LightXmlObjectType lightXmlObject : vars) {
				VariableSchemeDocument varsDoc = DdiManager.getInstance()
						.getVariableScheme(lightXmlObject.getId(),
								lightXmlObject.getVersion(),
								lightXmlObject.getParentId(),
								lightXmlObject.getParentVersion());

				// TODO handle references
				for (VariableType variType : varsDoc.getVariableScheme()
						.getVariableList()) {
					variDoc = VariableDocument.Factory.newInstance();
					variDoc.setVariable(variType);
					vari = new Variable(variDoc, varsDoc.getVariableScheme()
							.getId(), varsDoc.getVariableScheme().getVersion());
					label = vari.getName().getStringValue();

					// VARIABLE LABELS
					initSpssSyntax(label);
					LabelType labelType = (LabelType) XmlBeansUtil
							.getLangElement(LanguageUtil.getDisplayLanguage(),
									vari.getDocument().getVariable()
											.getLabelList());
					if (label != null) {
						spssSyntaxString();
						spssSyntaxTxt.append(removeNewLines(XmlBeansUtil
								.getTextOnMixedElement(labelType)));
						spssSyntaxString();
						addSpssSyntax(variableLabels);
					}

					// MISSING VALUES
					tmpMissing.clear();
					initSpssSyntax(label);
					if (vari.getValueRepresentation().getMissingValue() != null) {
						for (Object missing : vari.getValueRepresentation()
								.getMissingValue()) {
							tmpMissing.add(missing.toString());
						}

						spssSyntaxTxt.append("(");
						for (Iterator<String> iterator = tmpMissing.iterator(); iterator
								.hasNext();) {
							String missing = iterator.next();
							spssSyntaxTxt.append(missing);
							if (iterator.hasNext()) {
								spssSyntaxTxt.append(",");
							}
						}
						spssSyntaxTxt.append(")");
						addSpssSyntax(missingValues);

						// define missing value labels
						if (!tmpMissing.isEmpty()
								&& exportSpssWizard.addDdaMissingValueLabels) {
							spssMissingValueLabels(tmpMissing);
						}
					}

					// VARIABLE LEVEL
					measure = null;
					initSpssSyntax(label);
					if (vari.getValueRepresentation().getClassificationLevel() != null) {
						CategoryRelationCodeType.Enum classificationLevel = vari
								.getValueRepresentation()
								.getClassificationLevel();
						if (classificationLevel
								.equals(CategoryRelationCodeType.CONTINUOUS)
								|| classificationLevel
										.equals(CategoryRelationCodeType.RATIO)
								|| classificationLevel
										.equals(CategoryRelationCodeType.INTERVAL)) {
							measure = "SCALE";
						} else {
							measure = vari.getValueRepresentation()
									.getClassificationLevel().toString()
									.toUpperCase();
						}
						spssSyntaxTxt.append("(");
						spssSyntaxTxt.append(measure);
						spssSyntaxTxt.append(")");
						addSpssSyntax(variableLevels);
					}

					// GET DATA - VARIABLES
					initSpssSyntax(label);
					PhysicalLocationType physicalLocation = getPhysicalLocation(vari
							.getId());

					// code representation
					if (vari.getValueRepresentation() instanceof CodeRepresentationType) {
						sppsNumericSyntax(physicalLocation);
						addSpssSyntax(variables);

						// VALUE LABELS
						initSpssSyntax(label);
						spssValueLabel(vari
								.getCodeRepresentationCodeSchemeReference());
						continue;
					}
					// numeric representation
					else if (vari.getValueRepresentation() instanceof NumericRepresentationType) {
						// Fn.d Numeric. Specification of the total number of
						// characters (n) and decimals (d) is optional.

						sppsNumericSyntax(physicalLocation);

						// define missing value labels
						spssMissingValueLabelsForNonScaleVarables();
					}
					// text representation
					else if (vari.getValueRepresentation() instanceof TextRepresentationType) {
						// An String (alphanumeric). Specification of the
						// maximum string length (n) is optional.

						spssSyntaxTxt.append("A");
						spssSyntaxTxt.append(physicalLocation.getWidth());

						// define missing value labels
						spssMissingValueLabelsForNonScaleVarables();
					}
					// date time representation
					else if (vari.getValueRepresentation() instanceof DateTimeRepresentationType) {
						try {
							SPSSTime spssTime = SPSSTime
									.getSpssType(((DateTimeRepresentationType) vari
											.getValueRepresentation())
											.getFormat());
							spssSyntaxTxt.append(spssTime.getType());
						} catch (Exception e) {
							// defaults to string type
							spssSyntaxTxt.append("A");

							// TODO expand to support ISO 8601 expressed in SPSS
							// syntax
							// see
							// http://www.uic.edu/depts/accc/software/isodates/datepgm.html#spss
						}

						spssSyntaxTxt.append(physicalLocation.getWidth());
						if (physicalLocation.getDecimalPositions() != null) {
							spssSyntaxTxt.append("."
									+ physicalLocation.getDecimalPositions());
						}

						// define missing value labels
						spssMissingValueLabelsForNonScaleVarables();
					}
					// fall back
					else {
						spssSyntaxTxt.append("NOT DEFINED");
					}
					addSpssSyntax(variables);
				}
			}
		} catch (DDIFtpException e) {
			Editor.showError(e, null);
		} catch (Exception e) {
			Editor.showError(e, null);
		}

		// create and write spss syntax file
		try {
			writeOutSpssSyntax();
		} catch (IOException e) {
			Editor.showError(e, null);
		}

		return;
	}

	PhysicalLocationType getPhysicalLocation(String varId) {
		for (DataItemType dati : dataItems) {
			for (IDType id : dati.getVariableReference().getIDList()) {
				if (id.getStringValue().equals(varId)) {
					return dati.getPhysicalLocation();
				}
			}
		}
		return null;
	}

	/**
	 * Initialize SPSS syntax command param with varlist followed by a single
	 * whitespace
	 * 
	 * @param varName
	 *            variable name
	 */
	void initSpssSyntax(String varName) {
		clearStringBuffer(spssSyntaxTxt);
		spssSyntaxTxt.append(varName);
		spssSyntaxTxt.append(" ");
	}

	/**
	 * Append quote to SPSS syntax to start/ end string
	 */
	void spssSyntaxString() {
		spssSyntaxTxt.append("\"");
	}

	/**
	 * Define SPSS numeric variable definition
	 * 
	 * @param physicalLocation
	 *            DDI-L physical location
	 */
	void sppsNumericSyntax(PhysicalLocationType physicalLocation) {
		spssSyntaxTxt.append("F");
		spssSyntaxTxt.append(physicalLocation.getWidth());
		if (physicalLocation.getDecimalPositions() != null) {
			spssSyntaxTxt.append(".");
			spssSyntaxTxt.append("" + physicalLocation.getDecimalPositions());
		} else {
			spssSyntaxTxt.append(".0");
		}
	}

	/**
	 * Define syntax for SPSS value labels
	 * 
	 * @param codeSchemeReference
	 *            reference to DDI-L code scheme
	 * @throws Exception
	 */
	void spssValueLabel(ReferenceType codeSchemeReference) throws Exception {
		// resolve references
		CodeSchemeDocument codeScheme = CodeSchemeDao
				.getAllCodeSchemeByReference(new ReferenceResolution(
						codeSchemeReference));
		if (codeScheme == null) {
			return;
		}

		ReferenceType catSchemeRef = codeScheme.getCodeScheme()
				.getCategorySchemeReference();
		CategorySchemeDocument catScheme = null;

		if (catSchemeRef != null) {
			catScheme = CategorySchemeDao
					.getAllCategorySchemeByReference(new ReferenceResolution(
							catSchemeRef));
		}

		for (CodeType code : codeScheme.getCodeScheme().getCodeList()) {
			// workaround spss syntax line max lenght 255!
			if (spssSyntaxTxt.length() > 0) {
				spssSyntaxTxt.append("\n");
			}

			// code
			spssSyntaxTxt.append(code.getValue());

			// resolve cat by default cats ref
			if (catScheme != null) {
				for (CategoryType cat : catScheme.getCategoryScheme()
						.getCategoryList()) {
					if (!code.getCategoryReference().getIDList().isEmpty()
							&& cat.getId().equals(
									XmlBeansUtil.getTextOnMixedElement(code
											.getCategoryReference().getIDList()
											.get(0)))) {

						String text = XmlBeansUtil
								.getTextOnMixedElement(((org.ddialliance.ddi3.xml.xmlbeans.reusable.LabelType) XmlBeansUtil
										.getDefaultLangElement(cat
												.getLabelList())));
						// cat text
						spssValueLabelCategory(removeNewLines(text));
						break;
					}
				}
			}
			// resolve cat by code to cat ref
			else {
				if (code.getCategoryReference() != null) {
					ReferenceResolution refResolv = new ReferenceResolution(
							code.getCategoryReference());
					List<LightXmlObjectType> result = DdiManager
							.getInstance()
							.getCategorysLight(refResolv.getId(),
									refResolv.getVersion(), null, null)
							.getLightXmlObjectList().getLightXmlObjectList();
					if (!result.isEmpty()
							&& !result.get(0).getLabelList().isEmpty()) {
						String text = XmlBeansUtil
								.getTextOnMixedElement((XmlObject) XmlBeansUtil
										.getDefaultLangElement(result.get(0)
												.getLabelList()));
						// cat text
						spssValueLabelCategory(removeNewLines(text));
					}
				} else {
					throw new DDIFtpException(
							"spss.category.notfound",
							new String[] { code.getValue(), label, vari.getId() });
				}
			}
		}

		// add missing value labels
		if (codsVariToMissing.get(label) != null) {
			spssSyntaxTxt.append("\n");
			spssSyntaxTxt.append(codsVariToMissing.get(label));
		}

		addSpssSyntax(valueLabels);
	}

	private void spssValueLabelCategory(StringBuffer tmpSpssSyntax, String text) {
		tmpSpssSyntax.append(" ");
		tmpSpssSyntax.append("\"");
		tmpSpssSyntax.append(text);
		tmpSpssSyntax.append("\"");
	}

	private void spssValueLabelCategory(String text) {
		spssValueLabelCategory(spssSyntaxTxt, text);
	}

	/**
	 * Add Danish Data Archive controlled vocabulary for SPSS value label for
	 * missing definitions
	 * 
	 * @param tmpMissing
	 *            list of missing codes
	 * @throws DDIFtpException
	 */
	private void spssMissingValueLabels(List<String> tmpMissing)
			throws DDIFtpException {
		StringBuffer tmpSpssSyntax = new StringBuffer();
		for (Iterator<String> iterator = tmpMissing.iterator(); iterator
				.hasNext();) {
			String missing = iterator.next();
			String tmpMissingTxt;
			int index = missing.indexOf(".");
			if (index > -1) {
				tmpMissingTxt = missing.substring(0, index);
			} else
				tmpMissingTxt = missing;

			// convert to integer
			int missingInt;
			try {
				missingInt = Integer.parseInt(tmpMissingTxt);
			} catch (Exception e) {
				throw new DDIFtpException(Translator.trans(
						"spss.export.missingcodenotinteger", new String[] {
								missing, label, vari.getId() }));
			}

			boolean found = false;
			tmpSpssSyntax.append(missing);

			// unknown
			Matcher m = unknownPattern.matcher(tmpMissingTxt.trim());
			if (m.matches()) {
				spssValueLabelCategory(tmpSpssSyntax, missingValueLabels[0]);
				found = true;
			}
			// comment out 20140103
			// for (int i = 0; i < unknown.length; i++) {
			// if (missingInt == unknown[i]) {
			// spssValueLabelCategory(tmpSpssSyntax, missingValueLabels[0]);
			// found = true;
			// break;
			// }
			// }
			// irrelevant
			if (!found) {
				m = irrelevantPattern.matcher(tmpMissingTxt.trim());
				if (m.matches()) {
					spssValueLabelCategory(tmpSpssSyntax, missingValueLabels[1]);
					found = true;
				}
				// comment out 20140103
				// for (int i = 0; i < irrelevant.length; i++) {
				// if (missingInt == irrelevant[i]) {
				// spssValueLabelCategory(tmpSpssSyntax,
				// missingValueLabels[1]);
				// found = true;
				// break;
				// }
				// }
			}
			// didnotparticipate
			if (!found) {
				m = didnotparticipatePattern.matcher(tmpMissingTxt.trim());
				if (m.matches()) {
					spssValueLabelCategory(tmpSpssSyntax, missingValueLabels[1]);
					found = true;
				}
				// comment out 20140103
				// for (int i = 0; i < didnotparticipate.length; i++) {
				// if (missingInt == didnotparticipate[i]) {
				// spssValueLabelCategory(tmpSpssSyntax,
				// missingValueLabels[2]);
				// found = true;
				// break;
				// }
				// }
			}

			if (!found) {
				throw new DDIFtpException(Translator.trans(
						"spss.export.missingcodenotfound", new String[] {
								missing, label, vari.getId() }));
				// spssValueLabelCategory(tmpSpssSyntax,
				// "Not defined");
			}

			if (iterator.hasNext()) {
				tmpSpssSyntax.append(" ");
			}
		}
		codsVariToMissing.put(label, tmpSpssSyntax.toString());
	}

	void spssMissingValueLabelsForNonScaleVarables() {
		// add missing value labels
		if (exportSpssWizard.addDdaMissingValueLabels
				&& codsVariToMissing.get(label) != null) {
			StringBuffer tmp = new StringBuffer();
			tmp.append(label); // "/" +
			tmp.append("\n");
			tmp.append(codsVariToMissing.get(label));

			valueLabels.add(tmp.toString());
		}
	}

	/**
	 * Add current SPSS syntax to SPPS syntax command list
	 * 
	 * @param list
	 *            of SPSS command
	 */
	void addSpssSyntax(List<String> list) {
		list.add(spssSyntaxTxt.toString());
	}

	void clearStringBuffer(StringBuffer buf) {
		buf.delete(0, buf.length());
	}

	String removeNewLines(String text) {
		return text;
	}

	/**
	 * Write SPSS syntax file to disk
	 * 
	 * @throws IOException
	 */
	void writeOutSpssSyntax() throws IOException {
		try {
			// init output file writer
			writer = createOutFile(exportSpssWizard.exportPath + File.separator
					+ exportSpssWizard.fileName);

			// utf-8 bom
			if (DdiEditorConfig.get(DdiEditorConfig.SPSS_IMPORT_CHARSET)
					.toLowerCase().equals("utf-8")) {
				// add utf-8 BOM
				writer.write('\ufeff');
			}

			// header
			writer.write("* "
					+ Translator.formatIso8601DateTime(System
							.currentTimeMillis()) + ".");
			writer.newLine();
			writer.write("* SPSS syntax file created by DdiEditor-SPSS-"
					+ Platform.getBundle("ddieditor-spss").getHeaders()
							.get("Bundle-Version") + ".");
			writer.newLine();

			// GET DATA
			// TODO other encoding ?
			writer.write("SET UNICODE ON DECIMAL COMMA OLANG=ENGLISH.");
			writer.newLine();
			writer.write("GET DATA");
			writer.newLine();
			writer.write(" /TYPE=TXT");
			writer.newLine();
			writer.write(" /FILE=\"");
			writer.write(exportSpssWizard.exportPath);
			writer.write(File.separator);
			File inDataFile = new File(exportSpssWizard.inDataFile);
			writer.write(inDataFile.getName());
			writer.write("\"");
			writer.newLine();
			writer.write(" /DELCASE=LINE");
			writer.newLine();
			writer.write(" /DELIMITERS=\",\"");
			writer.newLine();
			// SPSS: The text qualifier appears at both the beginning and end of
			// the value, enclosing the entire value.
			writer.write(" /QUALIFIER='\"'");
			writer.newLine();
			writer.write(" /ARRANGEMENT=DELIMITED");
			writer.newLine();
			writer.write(" /FIRSTCASE=2");
			writer.newLine();
			writer.write(" /IMPORTCASE=ALL");
			writer.newLine();
			writer.write(" /VARIABLES=");
			// variables
			for (String variable : variables) {
				writer.newLine();
				writer.write(" " + variable);
			}
			writer.write(".");
			writer.newLine();
			writer.write("CACHE.");
			writer.newLine();
			writer.write("EXECUTE.");
			writer.flush();

			// VARIABLE LABELS
			writeOutCommand("VARIABLE LABELS", false, variableLabels);
			writer.newLine();
			writer.write("EXECUTE.");
			writer.flush();

			// VALUE LABELS
			writeOutCommand("VALUE LABELS", true, valueLabels);
			writer.newLine();
			writer.write("EXECUTE.");

			// MISSING VALUES
			writeOutCommand("MISSING VALUES", true, missingValues);
			writer.newLine();
			writer.write("EXECUTE.");
			writer.flush();

			// VARIABLE LEVEL
			writeOutCommand("VARIABLE LEVEL", true, variableLevels);
			writer.newLine();
			writer.write("EXECUTE.");
			writer.flush();

			// finalize
			// comment out 20130312
			// writer.newLine();
			// writer.newLine();
			// writer.write("SORT VARIABLES BY NAME (A).");
			// writer.newLine();
			// writer.write("EXECUTE.");
			// writer.flush();
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException e) {
				// do nothing
			}
		}
	}

	/**
	 * Create output file
	 * 
	 * @param fileName
	 *            name of file
	 * @return buffered writer
	 * @throws IOException
	 */
	BufferedWriter createOutFile(String fileName) throws IOException {
		File f = new File(fileName);
		if (!f.exists()) {
			if (!f.createNewFile()) {
				log.debug("File '" + fileName + "' overwritten");
			}
		}
		return new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(f),
				DdiEditorConfig.get(DdiEditorConfig.SPSS_IMPORT_CHARSET)));
	}

	/**
	 * Write out a SPSS command with variable list to file on disk
	 * 
	 * @param commandName
	 *            name of SPSS command
	 * @param addForwardSlash
	 *            option -add forward slash for each variable
	 * @param varList
	 *            list of variables with parameters
	 * @throws IOException
	 */
	void writeOutCommand(String commandName, boolean addForwardSlash,
			List<String> varList) throws IOException {
		String forwardSlash = "/";

		writer.newLine();
		writer.newLine();
		writer.write(commandName);

		boolean notFirst = false;
		for (Iterator<String> iterator = varList.iterator(); iterator.hasNext();) {
			String syntaxTxt = iterator.next();
			writer.newLine();
			if (addForwardSlash && notFirst) {
				writer.write(forwardSlash);
			}
			notFirst = true;

			// trim last value
			writer.write(syntaxTxt.toString().trim());
		}
		writer.write(".");
	}
}
