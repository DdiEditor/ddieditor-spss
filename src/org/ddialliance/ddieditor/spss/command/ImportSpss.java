package org.ddialliance.ddieditor.spss.command;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.spss.osgi.Activator;
import org.ddialliance.ddieditor.spss.wizard.ImportSpssWizard;
import org.ddialliance.ddieditor.ui.editor.category.CategorySchemeEditor;
import org.ddialliance.ddieditor.ui.editor.code.CodeSchemeEditor;
import org.ddialliance.ddieditor.ui.editor.variable.VariableSchemeEditor;
import org.ddialliance.ddieditor.ui.preference.PreferenceConstants;
import org.ddialliance.ddieditor.ui.util.DialogUtil;
import org.ddialliance.ddieditor.ui.view.Messages;
import org.ddialliance.ddieditor.ui.view.ViewManager;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.opendatafoundation.data.FileFormatInfo;
import org.opendatafoundation.data.FileFormatInfo.ASCIIFormat;
import org.opendatafoundation.data.FileFormatInfo.Format;
import org.opendatafoundation.data.Utils;
import org.opendatafoundation.data.spss.ExportOptions;
import org.opendatafoundation.data.spss.SPSSFile;
import org.w3c.dom.Document;

public class ImportSpss extends org.eclipse.core.commands.AbstractHandler {
	private Log log = LogFactory.getLog(LogType.SYSTEM, ImportSpss.class);

	ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(
			new ConfigurationScope(), "ddieditor-ui");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// open dialog
		ImportSpssWizard importSpssWizard = new ImportSpssWizard();
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench()
				.getDisplay().getActiveShell(), importSpssWizard);

		int returnCode = dialog.open();
		if (returnCode != Window.CANCEL) {
			// import
			SpssImportRunnable longJob = new SpssImportRunnable(
					importSpssWizard);
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(),
					longJob);

			// refresh
			ViewManager.getInstance().addViewsToRefresh(
					new String[] { CodeSchemeEditor.ID,
							CategorySchemeEditor.ID, VariableSchemeEditor.ID });
			ViewManager.getInstance().refesh();
		}
		return null;
	}

	/**
	 * Runnable wrapping spss import
	 */
	class SpssImportRunnable implements Runnable {
		ImportSpssWizard importSpssWizard;

		public SpssImportRunnable(ImportSpssWizard importSpssWizard) {
			this.importSpssWizard = importSpssWizard;
		}

		@Override
		public void run() {
			Document dom;
			String logicalProductID = null;

			// import
			SPSSFile spssFile = null;
			try {
				// study unit
				List<LightXmlObjectType> studList = DdiManager.getInstance()
						.checkForParent(
								SPSSFile.DDI3_LOGICAL_PRODUCT_NAMESPACE,
								"LogicalProduct");
				if (studList.isEmpty()) {
					MessageDialog.openConfirm(PlatformUI.getWorkbench()
							.getDisplay().getActiveShell(), Messages
							.getString("spss.confirm.title"), Translator
							.trans("spss.confirm.createdditoimportinto"));
					return;
				}
				LightXmlObjectType studyUnitLight = studList.get(0);

				// init spss file
				DdiManager.getInstance().setWorkingDocument(
						importSpssWizard.selectedResource.getOrgName());
				spssFile = new SPSSFile(importSpssWizard.spssFile);

				// logical product
				if (importSpssWizard.variable) {
					spssFile.loadMetadata();
					ExportOptions exportOptions = new ExportOptions();
					// TODO Control ExportOption.createCategories by property 
					// Get the Categories from Wiki interface
					exportOptions.createCategories = false;

					dom = spssFile.getDDI3LogicalProduct(exportOptions, null,
							preferenceStore
									.getString(PreferenceConstants.DDI_AGENCY));

					// insert
					DdiManager.getInstance().createElementInto(
							Utils.nodeToString(dom).toString(),
							studyUnitLight.getId(),
							studyUnitLight.getVersion(),
							studyUnitLight.getElement());
					dom = null;
				}

				// physical data product
				if (importSpssWizard.variableRec) {
					if (!spssFile.isMetadataLoaded) {
						spssFile.loadMetadata();
						// TODO add varirefs
					}

					logicalProductID = spssFile.getLogicalProductDdi3Id();
					if (logicalProductID == null) {
						// parent list
						List<LightXmlObjectType> logpList = DdiManager
								.getInstance()
								.getLogicalProductsLight(null, null, null, null)
								.getLightXmlObjectList()
								.getLightXmlObjectList();

						if (logpList.isEmpty()) {
							MessageDialog
									.openConfirm(
											PlatformUI.getWorkbench()
													.getDisplay()
													.getActiveShell(),
											Messages.getString("spss.confirm.title"),
											Translator
													.trans("spss.confirm.createdditoimportinto"));
							spssFile.close();
							return;
						} else {
							// TODO add dialog with list to choose from is more
							// than one ...
							logicalProductID = logpList
									.get(logpList.size() - 1).getId();
						}
					}

					// ascii only
					Format[] format = new Format[] {
					// FileFormatInfo.Format.SPSS,
					FileFormatInfo.Format.ASCII };
					for (int i = 0; i < format.length; i++) {
						dom = spssFile
								.getDDI3PhysicalDataProduct(new FileFormatInfo(
										format[i]), logicalProductID);

						DdiManager.getInstance().createElement(
								dom.getDocumentElement().getLocalName(), 
								Utils.nodeToString(dom).toString(),
								studyUnitLight.getId(),
								studyUnitLight.getVersion(),
								studyUnitLight.getElement(),
								new String[] { "LogicalProduct" },
								new String[] {}, new String[] {});
					}
				}

				// physical instance and dat file
				if (importSpssWizard.variableDataFile) {
					if (!spssFile.isMetadataLoaded) {
						spssFile.loadMetadata();
					}
					if (!spssFile.isDataLoaded) {
						spssFile.loadData();
					}

					// create dat file - dat file location
					String fileName = studyUnitLight.getId() + ".dat";
					FileFormatInfo fileFormatInfo = new FileFormatInfo();
					fileFormatInfo.format = FileFormatInfo.Format.ASCII;
					fileFormatInfo.asciiFormat = ASCIIFormat.FIXED;
					spssFile.exportData(new File(importSpssWizard.dataFile
							+ "/" + fileName), fileFormatInfo);

					// create meta data
					dom = spssFile.getDDI3PhysicalInstance(new URI("file://"
							+ fileName), fileFormatInfo);

					DdiManager.getInstance().createElementInto(
							Utils.nodeToString(dom).toString(),
							studyUnitLight.getId(),
							studyUnitLight.getVersion(),
							studyUnitLight.getElement());
					dom = null;
				}

				// frequencies
				// if (importSpssWizard.frequency) {
				// TODO use phyton spss oms export plus xslt import
				// MessageDialog
				// .openConfirm(
				// PlatformUI.getWorkbench().getDisplay()
				// .getActiveShell(),
				// Messages.getString("spss.confirm.title"),
				// Translator
				// .trans("spss.confirm.createfrequenciesnotimplemented"));
				// }

				// clean up archive - delete - reinsert - strategy
				List<LightXmlObjectType> archList = DdiManager.getInstance()
						.getArchivesLight(null, null, null, null)
						.getLightXmlObjectList().getLightXmlObjectList();
				if (!archList.isEmpty()) {
					// get archive xml objs
					XmlObject[] archs = new XmlObject[archList.size()];
					int count = 0;
					for (LightXmlObjectType archLight : archList) {
						archs[count] = DdiManager.getInstance().getAchive(
								archLight.getId(), archLight.getVersion(),
								archLight.getParentId(),
								archLight.getParentVersion());
						count++;
					}
					for (int i = 0; i < archs.length; i++) {
						// remove
						DdiManager.getInstance().deleteElement(archs[i],
								studyUnitLight.getId(),
								studyUnitLight.getVersion(),
								studyUnitLight.getElement());

						// insert
						DdiManager.getInstance().createElementInto(archs[i],
								studyUnitLight.getId(),
								studyUnitLight.getVersion(),
								studyUnitLight.getElement());
					}
				}
			} catch (Exception e) {
				DialogUtil.errorDialog(PlatformUI.getWorkbench().getDisplay()
						.getActiveShell(), Activator.PLUGIN_ID,
						Translator.trans("spss.errortitle"), e.getMessage(), e);
			} finally {
				if (spssFile != null) {
					try {
						spssFile.close();
					} catch (IOException e) {
						// do nothing
					}
				}
			}
		}
	}
}
