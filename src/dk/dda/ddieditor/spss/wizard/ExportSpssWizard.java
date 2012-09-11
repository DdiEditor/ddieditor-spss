package dk.dda.ddieditor.spss.wizard;

import java.io.File;
import java.util.List;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

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

public class ExportSpssWizard extends Wizard {

	private List<DDIResourceType> resources = null;

	public DDIResourceType selectedResource = null;
	public String inDataFile = null, exportPath = null, fileName = null;
	public boolean addDdaMissingValueLabels = true;

	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void addPages() {
		SelectPage rangePage = new SelectPage();
		addPage(rangePage);
	}

	class SelectPage extends WizardPage {
		public static final String PAGE_NAME = "select";

		public SelectPage() {
			super(PAGE_NAME, Translator.trans("spss.exportwizard.title"), null);
		}

		void pageComplete() {
			if (inDataFile != null && selectedResource != null
					&& exportPath != null && fileName != null) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			final Editor editor = new Editor();
			Group group = editor.createGroup(parent,
					Translator.trans("spss.exportwizard.title"));

			//
			// input
			//
			Group inputGroup = editor.createGroup(group,
					Translator.trans("spss.exportwizard.inputGroup"));

			// remove converted studies
			Button addDdaMissingValueLabelsButton = editor
					.createCheckBox(inputGroup, "", Translator
							.trans("spss.exportwizard.addddamissingvaluelabel"));
			addDdaMissingValueLabelsButton.setSelection(true);
			addDdaMissingValueLabelsButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					addDdaMissingValueLabels  = ((Button) e.widget)
							.getSelection();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// loaded resources
			try {
				resources = PersistenceManager.getInstance().getResources();
			} catch (DDIFtpException e) {
				MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
						.getActiveShell(), Translator.trans("ErrorTitle"),
						e.getMessage());
			}

			String[] options = new String[resources.size()];
			int count = 0;
			for (DDIResourceType resource : resources) {
				options[count] = resource.getOrgName();
				count++;
			}
			editor.createLabel(inputGroup,
					Translator.trans("spss.resource.select"));
			Combo combo = editor.createCombo(inputGroup, options);
			if (options.length == 1) {
				combo.select(0);
				selectedResource = resources.get(0);
			} else {
				combo.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						Combo c = (Combo) event.getSource();
						selectedResource = resources.get(c.getSelectionIndex());
						pageComplete();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent event) {
						// do nothing
					}
				});
			}

			// in data file
			editor.createLabel(inputGroup,
					Translator.trans("spss.exportwizard.choosedatafile"));
			final Text pathText = editor.createText(inputGroup, "");
			pathText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// on a CR - check if file exist and read it
					if (e.keyCode == SWT.CR) {
						inDataFile = readFile(pathText);
					}
				}
			});
			pathText.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					// on a TAB - check if file exist and read it
					switch (e.detail) {
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS: {
						inDataFile = readFile(pathText);
						if (inDataFile == null) {
							e.doit = false;
						}
					}
					}
				}
			});
			Button inSpssFilePathBrowse = editor.createButton(inputGroup,
					Translator.trans("spss.filechooser.browse"));
			inSpssFilePathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fileChooser = new FileDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					fileChooser.setText(Translator
							.trans("spss.filechoosertitle.data"));
					fileChooser.setFilterExtensions(new String[] { "*.csv",
							"*.dat", "*.*" });
					fileChooser.setFilterNames(new String[] {
							Translator.trans("spss.filternames.cvsfile"),
							Translator.trans("spss.filternames.datafile"),
							Translator.trans("spss.filternames.anyfile") });

					PreferenceUtil.setPathFilter(fileChooser);
					inDataFile = fileChooser.open();
					PreferenceUtil.setLastBrowsedPath(inDataFile);

					pathText.setText(inDataFile);
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			//
			// spss syntax file
			//
			Group spssSyntaxGroup = editor.createGroup(group,
					Translator.trans("spss.exportwizard.spsssyntaxGroup"));

			// export path
			editor.createLabel(spssSyntaxGroup,
					Translator.trans("ExportDDI3Action.filechooser.title"));
			final Text exportPathText = editor.createText(spssSyntaxGroup, "",
					false);
			exportPathText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					exportPath = ((Text) e.getSource()).getText();
				}
			});
			File lastBrowsedPath = PreferenceUtil.getLastBrowsedPath();
			if (lastBrowsedPath != null) {
				exportPathText.setText(lastBrowsedPath.getAbsolutePath());
				exportPath = lastBrowsedPath.getAbsolutePath();
			}

			Button pathBrowse = editor.createButton(spssSyntaxGroup,
					Translator.trans("ExportDDI3Action.filechooser.browse"));
			pathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dirChooser = new DirectoryDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					dirChooser.setText(Translator
							.trans("ExportDDI3Action.filechooser.title"));
					PreferenceUtil.setPathFilter(dirChooser);
					exportPath = dirChooser.open();
					if (exportPath != null) {
						exportPathText.setText(exportPath);
						PreferenceUtil.setLastBrowsedPath(exportPath);
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// file name
			Text fileNameText = editor.createTextInput(spssSyntaxGroup,
					Translator.trans("ExportDDI3Action.filename"), "", null);
			fileNameText.setData(true);
			if (options.length == 1) {
				fileName = combo.getItem(0);
				int index = fileName.indexOf(".xml");
				if (index > -1) {
					fileName = fileName.substring(0, index);
				}
				fileName += ".sps";

				fileNameText.setData(false);
				fileNameText.setText(fileName);
			}
			fileNameText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					Text text = ((Text) e.getSource());

					// do not change text on resource change selection
					if (!(Boolean) text.getData()) {
						text.setData(true);
						return;
					}
					fileName = text.getText();
					pageComplete();
				}
			});

			// finalize
			setControl(group);
			setPageComplete(false);
		}

		private String readFile(Text pathText) {
			if (!new File(pathText.getText()).exists()) {
				MessageDialog
						.openError(PlatformUI.getWorkbench().getDisplay()
								.getActiveShell(), Translator
								.trans("ErrorTitle"),
								Translator.trans("spss.filenotfound.message",
										pathText.getText()));
				setPageComplete(false);
				return null;
			}
			setPageComplete(true);
			return pathText.getText();
		}
	}
}
