package org.ddialliance.ddieditor.spss.wizard;

import java.util.List;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.util.swtdesigner.SWTResourceManager;
import org.ddialliance.ddieditor.ui.view.Messages;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ImportSpssWizard extends Wizard {
	private List<DDIResourceType> resources = null;

	public DDIResourceType selectedResource = null;
	public String spssFile = null;

	// variable
	public boolean variable = false;// null;
	public boolean frequency = false;// null;

	// variablerec
	public boolean variableRec = false;

	// variabledatafile
	public boolean variableDataFile = false;

	Button createMetaDataButton;

	Button createFrequenciesButton;

	Button variablerecButton;

	Button variabledatafileButton;

	public String dataFile = null;

	DataFilePage dataFilePage = null;
	ReportPage reportPage = null;

	@Override
	public void addPages() {
		addPage(new ResourcePage());

		// TODO DdiElementsPage is ok, fix in spss file
		// and uncomment setters
		// addPage(new DdiElementsPage());
		variable = true;
		variableRec = true;
		variableDataFile = true;

		dataFilePage = new DataFilePage();
		addPage(dataFilePage);
		reportPage = new ReportPage();
		addPage(reportPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.getName().equals(DdiElementsPage.PAGE_NAME)) {
			if (frequency || variableDataFile) {
				return dataFilePage;
			} else {
				setReport();
				return reportPage;
			}
		} else if (page.getName().equals(ReportPage.PAGE_NAME)) {
			setReport();
		}
		return super.getNextPage(page);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {

		return super.getPreviousPage(page);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public boolean performCancel() {
		unInitialize();
		return true;
	}

	private void unInitialize() {
		selectedResource = null;
		spssFile = null;
		dataFile = null;

		// meta data
		variable = false;
		frequency = false;
		variableRec = false;
		variableDataFile = false;
	}

	class ResourcePage extends WizardPage {
		public static final String PAGE_NAME = "ref";

		public ResourcePage() {
			super(PAGE_NAME, Translator.trans("spss.wizard.refpage.title"),
					null);
		}

		void pageComplete() {
			if (spssFile != null && selectedResource != null) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			Editor editor = new Editor();
			Group group = editor.createGroup(parent,
					Translator.trans("spss.wizard.refpage.title"));

			// spss file
			editor.createLabel(group,
					Translator.trans("spss.filechooser.title"));
			final Text pathText = editor.createText(group, "");
			Button pathBrowse = editor.createButton(group,
					Translator.trans("spss.filechooser.browse"));
			pathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fileChooser = new FileDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					fileChooser.setText(Translator
							.trans("spss.filechooser.title"));
					fileChooser.setFilterExtensions(new String[] { "*.sav" });
					fileChooser.setFilterNames(new String[] { Translator
							.trans("spss.filechooser.filternames") });
					spssFile = fileChooser.open();

					pathText.setText(spssFile);
					pageComplete();
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
						.getActiveShell(), Messages.getString("ErrorTitle"),
						e.getMessage());
			}

			String[] options = new String[resources.size()];
			int count = 0;
			for (DDIResourceType resource : resources) {
				options[count] = resource.getOrgName();
				count++;
			}
			editor.createLabel(group, Translator.trans("spss.resource.select"));
			Combo combo = editor.createCombo(group, options);
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

			// finalize
			setControl(group);
			setPageComplete(false);
		}
	}

	class DdiElementsPage extends WizardPage {
		public static final String PAGE_NAME = "elm";

		public DdiElementsPage() {
			super(PAGE_NAME, Translator.trans("spss.wizard.ddipage.title"),
					null);
		}

		void pageComplete() {
			if (variable || frequency || variableRec || variableDataFile) {
				setPageComplete(true);
			} else {
				setPageComplete(false);
			}
		}

		@Override
		public void createControl(Composite parent) {
			Editor editor = new Editor();
			Group group = editor.createGroup(parent,
					Translator.trans("spss.wizard.ddipage.title"));

			// variable
			createMetaDataButton = editor.createCheckBox(group,
					Translator.trans("spss.ddipage.variable"),
					Translator.trans("spss.ddipage.variableimport"));
			createMetaDataButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					variable = ((Button) e.widget).getSelection();
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {// do
																		// nothing
				}
			});

			// variable frequencies
			createFrequenciesButton = editor.createCheckBox(group,
					Translator.trans(""),
					Translator.trans("spss.ddipage.variablefeqimport"));

			createFrequenciesButton
					.addSelectionListener(new SelectionListener() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							frequency = ((Button) e.widget).getSelection();
							pageComplete();
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
							// do nothing
						}
					});

			// record layout spssFile.getDDI3PhysicalDataProduct
			variablerecButton = editor.createCheckBox(group,
					Translator.trans("spss.ddipage.variablerec"),
					Translator.trans("spss.ddipage.variablerecimport"));
			variablerecButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					variableRec = ((Button) e.widget).getSelection();
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// data file association spssFile.getDDI3PhysicalInstance
			variabledatafileButton = editor.createCheckBox(group,
					Translator.trans("spss.ddipage.variabledatafile"),
					Translator.trans("spss.ddipage.variabledatafileimport"));
			variabledatafileButton
					.addSelectionListener(new SelectionListener() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							variableDataFile = ((Button) e.widget)
									.getSelection();
							pageComplete();
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
							// do nothing
						}
					});

			// finalize
			setControl(group);
			setPageComplete(false);
		}
	}

	class DataFilePage extends WizardPage {
		public static final String PAGE_NAME = "daf";

		public DataFilePage() {
			super(PAGE_NAME,
					Translator.trans("spss.wizard.datafilepage.title"), null);
		}

		void pageComplete() {
			if (dataFile != null) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			Editor editor = new Editor();
			Group group = editor.createGroup(parent,
					Translator.trans("spss.wizard.datafilepage.title"));

			editor.createLabel(group,
					Translator.trans("spss.wizard.datafilepage.exportdir"));
			final Text pathText = editor.createText(group, "");
			Button pathBrowse = editor.createButton(group,
					Translator.trans("spss.filechooser.browse"));
			pathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					DirectoryDialog dirChooser = new DirectoryDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					dirChooser.setText(Translator
							.trans("spss.wizard.datafilepage.exportdir"));

					dataFile = dirChooser.open();

					pathText.setText(dataFile);
					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// finalize
			setControl(group);
			setPageComplete(false);
		}
	}

	class ReportPage extends WizardPage {
		public static final String PAGE_NAME = "rep";
		Group group;

		public ReportPage() {
			super(PAGE_NAME, Translator.trans("spss.wizard.reportpage.title"),
					null);
		}

		@Override
		public void createControl(Composite parent) {
			Editor editor = new Editor();
			group = editor.createGroup(parent,
					Translator.trans("spss.wizard.reportpage.title"));

			// resources
			resourceLabel = editor.createLabel(group, "resourceLabel");
			enhance(resourceLabel);

			// stat file
			editor.createLabel(group, "");
			statFileLabel = editor.createLabel(group, "statFileLabel");
			enhance(statFileLabel);

			// create ddi elements
			editor.createLabel(group, "");
			ddiElements = editor.createLabel(group, "ddiElements");
			enhance(ddiElements);

			// create data file
			editor.createLabel(group, "");
			dataFileImport = editor.createLabel(group, "dataFileImport");
			enhance(dataFileImport);

			// finalize
			setControl(group);
			setPageComplete(true);
		}

		void enhance(Label label) {
			label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false,
					1, 1));
		}
	}

	Label resourceLabel, statFileLabel, ddiElements, dataFileImport;

	void setReport() {
		// resources
		resourceLabel.setText(Translator
				.trans("spss.wizard.reportpage.resource")
				+ ": "
				+ selectedResource.getOrgName());

		// stat file
		statFileLabel.setText(Translator
				.trans("spss.wizard.reportpage.spssfile") + ": " + spssFile);

		// create ddi elements
		StringBuilder d = new StringBuilder();

		// Button
		if (variable) {
			d.append(Translator.trans("spss.wizard.reportpage.createMetaData"));
		}
		if (frequency) {
			appendComma(d);
			d.append(Translator
					.trans("spss.wizard.reportpage.createFrequencies"));
		}
		if (variableRec) {
			appendComma(d);
			d.append(Translator.trans("spss.wizard.reportpage.variablerec"));
		}
		if (variableDataFile) {
			appendComma(d);
			d.append(Translator
					.trans("spss.wizard.reportpage.variabledatafile"));
		} else {
			dataFilePage.setPageComplete(true);
		}

		ddiElements.setText(Translator.trans("spss.wizard.reportpage.import")
				+ ": " + d.toString());

		// create data file
		if (dataFile != null) {
			dataFileImport.setText(Translator
					.trans("spss.wizard.reportpage.datafiledir")
					+ ": "
					+ dataFile);
		}
		// finalize
		reportPage.group.layout(true);
	}

	void appendComma(StringBuilder sb) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
	}
}