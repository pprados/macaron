/**
 * BSD-style license; for more info see http://xradar.sourceforge.net/license.html
 */
package com.googlecode.macaron.audit;

import java.io.File;
import java.io.FileFilter;

/**
 * @author Romain PELISSE <belaran@gmail.com>
 *
 */
public class AuditFileFilter implements FileFilter {
	
	private boolean recurs_;

	public AuditFileFilter(boolean recursive) {
		this.recurs_ = recursive;
	}
	
	public boolean accept(File pathname)
	{
		return (pathname.getName().endsWith(".jar")
				|| pathname.getName().endsWith(".war")
				|| pathname.getName().endsWith(".ear")
				|| pathname.getName().endsWith(".rar")
				|| pathname.getName().endsWith(".class")
				|| (recurs_ && pathname.isDirectory()));
	}

	/**
	 * @return the recurs_
	 */
	public boolean isRecurs_() {
		return recurs_;
	}

	/**
	 * @param recurs_ the recurs_ to set
	 */
	public void setRecurs_(boolean recurs_) {
		this.recurs_ = recurs_;
	}
}
