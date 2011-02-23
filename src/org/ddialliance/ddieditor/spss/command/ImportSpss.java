package org.ddialliance.ddieditor.spss.command;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.persistenceaccess.maintainablelabel.MaintainableLightLabelQueryResult;
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
			XmlObject insert;
			Document dom;
			String logicalProductID = null;

			// import
			SPSSFile spssFile = null;
			try {
				// check parent
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

				// init spss file
				DdiManager.getInstance().setWorkingDocument(
						importSpssWizard.selectedResource.getOrgName());
				spssFile = new SPSSFile(importSpssWizard.spssFile);

				// logical product
				if (importSpssWizard.variable) {
					spssFile.loadMetadata();
					ExportOptions exportOptions = new ExportOptions();
					exportOptions.createCategories = true;

					dom = spssFile.getDDI3LogicalProduct(exportOptions, null,
							preferenceStore
									.getString(PreferenceConstants.DDI_AGENCY));
					
					// log.debug(Utils.nodeToString(dom).toString());
					DdiManager.getInstance().createElement(
							Utils.nodeToString(dom).toString(),
							studList.get(0).getId(),
							studList.get(0).getVersion(),
							studList.get(0).getElement(),
							new String[] { "ConceptualComponent" });
					
					// insert =
					// XmlObject.Factory.parse(Utils.nodeToString(dom).toString());
					// dom = null;
					// DdiManager.getInstance().createElement(
					// Utils.nodeToString(dom).toString(),
					// //insert,
					// studList.get(0).getId(),
					// studList.get(0).getVersion(),
					// studList.get(0).getElement(),
					// new String[] { "ConceptualComponent" });
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
								Utils.nodeToString(dom).toString(),
								studList.get(0).getId(),
								studList.get(0).getVersion(),
								studList.get(0).getElement(),
								new String[] { "LogicalProduct" });
					}
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

				// dat file and physical instance
				// if (importSpssWizard.variableDataFile) {
				if (false) {
					if (!spssFile.isMetadataLoaded) {
						spssFile.loadMetadata();
					}
					if (!spssFile.isDataLoaded) {
						spssFile.loadData();
					}
					// create dat file - dat file location
					// importSpssWizard.dataFile
					if (spssFile.getRecordLayoutSchemeDdi3Id() == null) {
						MaintainableLightLabelQueryResult m = DdiManager
								.getInstance().getRecordLayoutSchemeLabel(null,
										null, null, null);

						System.out.println(m);
					}

					// String[] fileSplit =
					// importSpssWizard.dataFile.split("/");
					// fileSplit[fileSplit.length - 2]

					// TODO hokusPoku.dat ;- )
					dom = spssFile.getDDI3PhysicalInstance(new URI("file://"
							+ "hokusPoku.dat"),
							new FileFormatInfo(Format.ASCII));
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
