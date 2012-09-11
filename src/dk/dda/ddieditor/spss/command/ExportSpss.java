package dk.dda.ddieditor.spss.command;

import java.lang.reflect.InvocationTargetException;

import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

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
 * PDE wrapper for export SPSS syntax file based on DDI-L
 * 
 * @author ddajvj
 */
public class ExportSpss extends org.eclipse.core.commands.AbstractHandler {
	// input
	ExportSpssWizard exportSpssWizard;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// selection
		exportSpssWizard = new ExportSpssWizard();
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench()
				.getDisplay().getActiveShell(), exportSpssWizard);

		int returnCode = dialog.open();
		if (returnCode == Window.CANCEL) {
			return null;
		} else {
			// export
			final SpssExportRunnable longJob = new SpssExportRunnable(
					exportSpssWizard);
			try {
				PlatformUI.getWorkbench().getProgressService()
						.busyCursorWhile(new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								monitor.beginTask(Translator
										.trans("spss.exportwizard.title"), 1);
								PlatformUI.getWorkbench().getDisplay()
										.asyncExec(longJob);
								monitor.worked(1);
							}
						});
			} catch (Exception e) {
				throw new ExecutionException(
						Translator.trans("spss.errortitle"), e.getCause());
			}
		}

		return null;
	}
}
