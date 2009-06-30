/**
 * BSD-style license; for more info see http://xradar.sourceforge.net/license.html
 */
package com.googlecode.macaron.audit;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Romain PELISSE <belaran@gmail.com>
 *
 */
public class ParamsConfig {

	private	boolean recurs=false;
	private	URL ignore;
	private List<File> names=new ArrayList<File>();
	private File output=new File("-");
	private final String envXslt=System.getenv("AUDIT_XSLT");
	private String xslt=(envXslt!=null) ? envXslt :	Audit.DEFAULT_STYLE_SHEET;
	/**
	 * @return the recurs
	 */
	public boolean isRecurs() {
		return recurs;
	}
	/**
	 * @param recurs the recurs to set
	 */
	public void setRecurs(boolean recurs) {
		this.recurs = recurs;
	}
	/**
	 * @return the ignore
	 */
	public URL getIgnore() {
		return ignore;
	}
	/**
	 * @param ignore the ignore to set
	 */
	public void setIgnore(URL ignore) {
		this.ignore = ignore;
	}
	/**
	 * @return the names
	 */
	public List<File> getNames() {
		return names;
	}
	/**
	 * @param names the names to set
	 */
	public void setNames(List<File> names) {
		this.names = names;
	}
	/**
	 * @return the output
	 */
	public File getOutput() {
		return output;
	}
	/**
	 * @param output the output to set
	 */
	public void setOutput(File output) {
		this.output = output;
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
	/**
	 * @return the envXslt
	 */
	public String getEnvXslt() {
		return envXslt;
	}

	
}
