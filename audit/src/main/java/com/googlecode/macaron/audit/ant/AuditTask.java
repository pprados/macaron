/**
 * BSD-style license; for more info see http://xradar.sourceforge.net/license.html
 */
package com.googlecode.macaron.audit.ant;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.googlecode.macaron.audit.Audit;
import com.googlecode.macaron.audit.ParamsConfig;

/**
 * @author Romain PELISSE <belaran@gmail.com>
 *
 */
public class AuditTask extends Task {

	private ParamsConfig config = new ParamsConfig();
	private Audit audit;
	
	/*
	 * Validation of task attributes 
	 */
	private void validate() {
		
	}
	
	/**
	 * <p>Execute AuditTask:
	 * 	<ol>
	 * 	<li>Check task attributes and elements provided;</li>
	 *  <li>Create an Audit instance ;</li>
	 *  <li>Invoke the doAll method.</li>
	 *  <ol>
	 * </p>
	 */
	@Override
	public void execute() {
		validate();
		audit = new Audit(config);
		File[] files = null;
		try {
			audit.doAll(files);
		} catch (IOException e) {
			new BuildException(e);
		}
	}
	
}
