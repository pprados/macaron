/**
 * BSD-style license; for more info see http://xradar.sourceforge.net/license.html
 */
package com.googlecode.macaron.audit.ant;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.googlecode.macaron.audit.Audit;
import com.googlecode.macaron.audit.ParamsConfig;

/**
 * 
 * <code>
 * 		<audit ignore="URL" output="build/audit.xml">
 * 				<target name="src/main/file/to/analyse"/>
 *              <target name="src/other/file/to/analyse"/>
 * 		</audit>
 * </code>
 * @author Romain PELISSE <belaran@gmail.com>
 *
 */
public class AuditTask extends Task {

	private ParamsConfig config = new ParamsConfig();
	private Audit audit;
	
	private String ignore;
	private String output;
	private List<Target> targets;
	private boolean recursive;
	private String xslt;
	/*
	 * Validation of task attributes 
	 */
	private void validate() {
		convertIgnoreToURL();
		buildFilesList();
		this.config.setOutput(new File(output));
		this.config.setRecurs(recursive);
		this.config.setXslt(xslt);
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
		File[] files = new File[this.config.getNames().size()];
		int nbFile = 0;
		for ( File file : this.config.getNames() ) {
			files[nbFile++] = file;
		}
		
		try {
			audit.doAll(files);
		} catch (IOException e) {
			new BuildException(e);
		}
	}
	
	public void addTarget(Target target) {
		if ( this.targets == null )
			this.targets = new ArrayList<Target>();
		this.targets.add(target);
	}
	
	private void buildFilesList() {
		List<File> files = new ArrayList<File>(this.targets.size());
		for ( Target target : targets ){
			// Add exist() & readable() test ?
			files.add(new File(target.getName()));
		}
		config.setNames(files);
	}
	
	private void convertIgnoreToURL() {
		if ( this.ignore != null && ! "".equals(this.ignore)) {
			try {
				this.config.setIgnore(new URL(this.ignore));
			} catch (MalformedURLException e) {
				throw new BuildException("'ignore' attribute must be a valid URL.");
			}
		}
		
	}


	/**
	 * @return the ignore
	 */
	public String getIgnore() {
		return ignore;
	}

	/**
	 * @param ignore the ignore to set
	 */
	public void setIgnore(String ignore) {
		this.ignore = ignore;
	}

	/**
	 * @return the output
	 */
	public String getOutput() {
		return output;
	}

	/**
	 * @param output the output to set
	 */
	public void setOutput(String output) {
		this.output = output;
	}

	/**
	 * @return the targets
	 */
	public List<Target> getTargets() {
		return targets;
	}

	/**
	 * @param targets the targets to set
	 */
	public void setTargets(List<Target> targets) {
		this.targets = targets;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive() {
		return recursive;
	}

	/**
	 * @param recursive the recursive to set
	 */
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	/**
	 * @return the xslt
	 */
	public String getXslt() {
		return xslt;
	}

	/**
	 * @param xslt the xslt to set
	 */
	public void setXslt(String xslt) {
		this.xslt = xslt;
	}
}
