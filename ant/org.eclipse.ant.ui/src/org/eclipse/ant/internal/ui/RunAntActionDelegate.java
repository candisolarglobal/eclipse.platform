package org.eclipse.ant.internal.ui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.EclipseProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.wizard.WizardDialog;


public class RunAntActionDelegate implements IWorkbenchWindowActionDelegate, IRunnableWithProgress {

	private IFile selection;

	/*
	 * @see IWorkbenchWindowActionDelegate
	 */
	public void dispose() {
	}
	protected EclipseProject extractProject(IFile sourceFile) {
		// create a project and initialize it
		EclipseProject antProject = new EclipseProject();
		antProject.init();
		antProject.setProperty("ant.file",sourceFile.getLocation().toOSString());
		
		try {
			ProjectHelper.configureProject(antProject,new File(sourceFile.getLocation().toOSString()));
		} catch (Exception e) {
			// If the document is not well-formated for example
			IStatus status = new Status(
				IStatus.ERROR,
				AntUIPlugin.PI_ANTUI,
				IStatus.ERROR,
				e.getMessage(),
				e);
			ErrorDialog.openError(
				AntUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell(),
				"Ant script error",
				"A problem occurred parsing the Ant file",
				status);
				
			return null;
		}
		
		return antProject;
	}
	
	/**
	  * Returns the active shell.
	  */
	protected Shell getShell() {
		return AntUIPlugin.getPlugin().getWorkbench().getActiveWorkbenchWindow().getShell();
	}
	/*
	 * @see IWorkbenchWindowActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
	}
	/*
	 * @see IRunnableWithProgress
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		String buildFileName= null;
		buildFileName= selection.getLocation().toOSString();

		String[] args= {"-buildfile", buildFileName};
		monitor.beginTask("Running Ant", IProgressMonitor.UNKNOWN);

		try {
			//TBD: should remove the build listener somehow
			new AntRunner().run(args, new UIBuildListener(monitor, selection));
		} 
		catch (BuildCanceledException e) {
			// build was canceled don't propagate exception
			return;
		}
		catch (Exception e) {
			throw new InvocationTargetException(e);
		}
		finally {
			monitor.done();
		}
	}
	/*
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		EclipseProject project = extractProject(selection);
		if (project == null)
			return;
			
		AntLaunchWizard wizard = new AntLaunchWizard(project,selection);
		wizard.setNeedsProgressMonitor(true);
		WizardDialog dialog = new WizardDialog(getShell(),wizard);
		dialog.create();
		dialog.open();
	}
	/*
	 * @see IWorkbenchActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			if (structuredSelection.size() == 1) {
				Object selectedResource = structuredSelection.getFirstElement();
				if (selectedResource instanceof IFile)
					this.selection = (IFile)selectedResource;
			}
		}
	}
}
