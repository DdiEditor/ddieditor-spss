package org.ddialliance.ddieditor.spss.command;

import java.io.IOException;
import java.util.List;

import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.spss.controler.SpssImportControler;
import org.ddialliance.ddieditor.spss.dialog.ImportSpssDialog;
import org.ddialliance.ddieditor.spss.osgi.Activator;
import org.ddialliance.ddieditor.ui.editor.category.CategorySchemeEditor;
import org.ddialliance.ddieditor.ui.editor.code.CodeSchemeEditor;
import org.ddialliance.ddieditor.ui.editor.variable.VariableEditor;
import org.ddialliance.ddieditor.ui.perspective.InfoPerspective;
import org.ddialliance.ddieditor.ui.preference.PreferenceConstants;
import org.ddialliance.ddieditor.ui.util.DialogUtil;
import org.ddialliance.ddieditor.ui.view.InfoView;
import org.ddialliance.ddieditor.ui.view.Messages;
import org.ddialliance.ddieditor.ui.view.View;
import org.ddialliance.ddieditor.ui.view.ViewManager;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.opendatafoundation.data.Utils;
import org.opendatafoundation.data.spss.ExportOptions;
import org.opendatafoundation.data.spss.SPSSFile;
import org.w3c.dom.Document;

public class ImportSpss extends org.eclipse.core.commands.AbstractHandler {
	private Log log = LogFactory.getLog(LogType.SYSTEM, ImportSpss.class);
	SpssImportControler spssImportControler = null;
	ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(
			new ConfigurationScope(), "ddieditor-ui");

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ImportSpssDialog dialog = new ImportSpssDialog(PlatformUI
				.getWorkbench().getDisplay().getActiveShell());
		int returnCode = dialog.open();
		if (returnCode == Dialog.CANCEL) {
			return null;
		}

		// check prefs
		if (dialog.selectedResource == null && dialog.fileName != null) {
			new MessageDialog(null,
					Translator.trans("jsie.selectedresourcenull.title"), null,
					Translator.trans("jsie.selectedresourcenull.message"),
					MessageDialog.ERROR, new String[] { "Ok" }, 0).open();
			returnCode = dialog.open();
			if (returnCode == Dialog.CANCEL) {
				return null;
			}
		}

		// confirm
		if (MessageDialog
				.openConfirm(
						PlatformUI.getWorkbench().getDisplay().getActiveShell(),
						Messages.getString("spss.confirm.title"),
						Translator.trans("spss.confirm.import",
								new Object[] { dialog.fileName,
										dialog.selectedResource.getOrgName() }))) {

			// import
			SPSSFile spssFile = null;
			try {
				spssFile = new SPSSFile(dialog.fileName);
				if (dialog.createMetaData) {
					spssFile.loadMetadata();
					ExportOptions exportOptions = new ExportOptions();
					exportOptions.createCategories = true;

					Document dom = spssFile.getDDI3LogicalProduct(
							exportOptions, null, preferenceStore
									.getString(PreferenceConstants.DDI_AGENCY));

					DdiManager.getInstance().setWorkingDocument(
							dialog.selectedResource.getOrgName());
					List<LightXmlObjectType> parentList = DdiManager
							.getInstance().checkForParent(
									SPSSFile.DDI3_LOGICAL_PRODUCT_NAMESPACE,
									"LogicalProduct");
					if (!parentList.isEmpty()) {
						DdiManager.getInstance().createElement(
								Utils.nodeToString(dom).toString(),
								parentList.get(0).getId(),
								parentList.get(0).getVersion(),
								parentList.get(0).getElement(), null);
					} else {
						MessageDialog.openConfirm(PlatformUI.getWorkbench()
								.getDisplay().getActiveShell(), Messages
								.getString("spss.confirm.title"), Translator
								.trans("spss.confirm.createdditoimportinto"));
						spssFile.close();
						return null;
					}
				}
				if (dialog.createFrequencies) {
					MessageDialog
							.openConfirm(
									PlatformUI.getWorkbench().getDisplay()
											.getActiveShell(),
									Messages.getString("spss.confirm.title"),
									Translator
											.trans("spss.confirm.createfrequenciesnotimplemented"));
					spssFile.loadData();
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

			// update info view
			// TODO refactor boiler plate code to refresh a
			// view into a rcp command
			final IWorkbenchWindow[] workbenchWindows = PlatformUI
					.getWorkbench().getWorkbenchWindows();

			IWorkbenchPage workbenchPage = workbenchWindows[0].getActivePage();
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						PlatformUI.getWorkbench().showPerspective(
								InfoPerspective.ID, workbenchWindows[0]);
					} catch (WorkbenchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			IViewPart iViewPart = workbenchWindows[0].getActivePage().findView(
					InfoView.ID);
			if (iViewPart == null) {
				try {
					iViewPart = workbenchPage.showView(InfoView.ID);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// refresh views
			String[] updateViewsIds = new String[] { VariableEditor.ID, CodeSchemeEditor.ID, CategorySchemeEditor.ID };
			ViewManager.getInstance().addViewsToRefresh(updateViewsIds);
			ViewManager.getInstance().refesh();
		}
		return null;
	}
}
