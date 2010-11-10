package org.ddialliance.ddieditor.spss.dialog;

import java.util.List;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.view.Messages;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ImportSpssDialog extends Dialog {
	private List<DDIResourceType> resources = null;
	public DDIResourceType selectedResource = null;
	public String searchTxt;
	public String fileName;
	public boolean createMetaData = false;
	public boolean createFrequencies = false;

	public ImportSpssDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// dialog setup
		this.getShell().setText(Translator.trans("spss.dialog.title"));

		// group
		Editor editor = new Editor();
		Group group = editor.createGroup(parent,
				Translator.trans("spss.dialog.group"));
		group.setLayoutData(new GridData(700, 400));

		// spss file
		editor.createLabel(group, Translator.trans("spss.filechooser.title"));
		final Text pathText = editor.createText(group, "", false);
		Button pathBrowse = editor.createButton(group,
				Translator.trans("spss.filechooser.browse"));
		pathBrowse.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileChooser = new FileDialog(PlatformUI
						.getWorkbench().getDisplay().getActiveShell());
				fileChooser.setText(Translator.trans("spss.filechooser.title"));
				fileChooser.setFilterExtensions(new String[] { "*.sav" });
				fileChooser.setFilterNames(new String[] { Translator
						.trans("spss.filechooser.filternames") });
				fileName = fileChooser.open();
				pathText.setText(fileName);
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
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				// do nothing
			}
		});

		// create meta data
		Button createMetaDataButton = editor.createCheckBox(group,
				Translator.trans("spss.import.head"),
				Translator.trans("spss.import.metadata"));
		createMetaDataButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createMetaData = true;
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {// do nothing
			}
		});

		// create frequencies
		Button createFrequenciesButton = editor.createCheckBox(group,
				Translator.trans(""),
				Translator.trans("spss.import.frequencies"));

		createFrequenciesButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createFrequencies = true;
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {// do nothing
			}
		});

		return null;
	}
}