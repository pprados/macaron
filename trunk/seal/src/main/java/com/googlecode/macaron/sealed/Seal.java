/*
 * Copyright 2009 Philippe Prados.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 * $Log$
 */
package com.googlecode.macaron.sealed;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Seal all packages in components.
 * 
 * @author Philippe Prados
 */
public class Seal
{
	/** The logger. Use to explain steps. */
	private static Logger log=Logger.getLogger(Seal.class.getName());

	/** If debug. */
	private static boolean debug=true;
	/** The base for XSLT and XSD. */
	private static final String home="http://macaron.googlecode.com/svn/trunk/audit/src/main/resources/com/googlecode/macaron/audit/";
	/** The default XSLT. */
	private static final String defaultStylesheet=home+"sealed-to-xhtml.xslt";
	/** The default XSD. */
	private static final String defaultXSD=home+"audit.xsd";

	/** The TEMP directory. */
	private static final String TEMP;
	static
	{
		String t=System.getProperty("java.io.tmpdir");
		if (t==null) t=".";
		TEMP=t;
	}
	
	/** Update the file in place ? */
	private final boolean inPlace_;
	/** Keep a copy of original file ? */
	private final boolean keepOriginal_;
	/** Calculate SHA1 ? */
	private final boolean sha1_;
	/** Recursive in directory ? */
	private final boolean recurs_;
	/** Audit sealed status ? */
	private final boolean audit_;
	/** Current xslt. */
	private final String xslt_;
	/** XSLT output dir. */
	private final String dir_;
	
	/** Flag if update something. */
	private boolean update_;
	/** Ignore packages. */
	private Set<String> ignorePackageName_=new HashSet<String>();
	/** Writer for audit. */
	private PrintWriter auditStream_;
	/** Output file or directory */
	private File output_;
	/** Hexa chars. */
	private static final String HEX_CHAR_TABLE ="0123456789abcdef";

	/**
	 * Convert byte array to hexa string.
	 * 
	 * @param raw The raw.
	 * @return The hexadecimal string.
	 */
	private static String getHexString(byte[] raw)
	{
		char[] hex = new char[raw.length<<1];
		int index = -1;

		for (byte b : raw)
		{
			int v = b & 0xFF;
			hex[++index] = HEX_CHAR_TABLE.charAt(v >>> 4);
			hex[++index] = HEX_CHAR_TABLE.charAt(v & 0xF);
		}
		return new String(hex);
	}

	/**
	 * Seal engine.
	 * 
	 * @param config Configuration.
	 * @throws IOException
	 * @throws SAXException
	 */
	public Seal(ParamsConfig config) throws IOException, SAXException
	{
		try
		{
			audit_=config.audit;
			inPlace_=(audit_) ? false : config.inPlace;
			keepOriginal_=config.keepOriginal;
			sha1_=config.sha1;
			recurs_=config.recurs;
			xslt_=config.xslt;
			output_=config.output;
			if (audit_)
			{
				if (config.output.getName().equals("-"))
					auditStream_=new PrintWriter(System.out,debug);
				else
					auditStream_=new PrintWriter(new FileWriter(config.output));
			}
			dir_=(config.output!=null) ? config.output.getAbsoluteFile().getParent() : null;
			if (config.ignore!=null) parseIgnore(config.ignore);
		}
		catch (XPathException e)
		{
			throw new AssertionError(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * Finest log.
	 * 
	 * @param msg Message.
	 */
	private void finest(String msg)
	{
		log.finest(msg);
	}
	
	/**
	 * Finer log.
	 * @param msg Message.
	 */
	private void finer(String msg)
	{
		log.finer(msg);
	}
	
	/**
	 * Fine log.
	 * @param msg Message.
	 */
	private void fine(String msg)
	{
		log.fine(msg);
	}
	
	/**
	 * Warning.
	 * @param msg Message.
	 */
	private void warn(String msg)
	{
		log.warning(msg);
	}
	
	/**
	 * Error
	 * @param e Exception.
	 */
	private void error(Throwable e)
	{
		log.severe(e.getLocalizedMessage());
	}
	
	/**
	 * Information.
	 * @param msg Message.
	 */
	private void info(String msg)
	{
		log.info(msg);
	}
	
	/**
	 * Exception if allready sealed.
	 */
	public static class AllreadySealed extends Exception
	{
		private static final long serialVersionUID = 1L;

		public AllreadySealed(String msg)
		{
			super(msg);
		}
	}
	
	/**
	 * Command line exception.
	 */
	public static class CommandLineException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public CommandLineException(String s)
		{
			super(s);
		}
	}
	
	/**
	 * Visitor of component.
	 */
	private interface Visitor
	{
		boolean doFileName(String idname,JarFile jarFile,ZipEntry entry,JarOutputStream zos) 
			throws IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed;
	}
	/**
	 * Copy stream.
	 * @param in Input.
	 * @param out Output.
	 * @throws IOException If error.
	 */
	private static void copyStream(InputStream in,OutputStream out) throws IOException
	{
		byte[] tampon=new byte[4096];
	    int len;
        while ((len = in.read(tampon)) > 0) 
        {
            out.write(tampon, 0, len);
        }
	    out.flush();
	}
	
	/**
	 * Copy a jar.
	 * 
	 * @param idname The identity name.
	 * @param jar The jar to copy.
	 * @param visitor The visitor.
	 * @param keeporiginal Keep a copy of original ?
	 * @param inPlace Update in place ?
	 * @return SHA1.
	 * @throws ZipException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException If SHA1 is not disponible.
	 * @throws SignatureException If component is signed.
	 * @throws AllreadySealed If component is allready sealed.
	 */
	private String copyJar(String idname,File jar,Visitor visitor,boolean keeporiginal,final boolean inPlace) throws IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed
	{
		JarFile jarFile=new JarFile(jar);
		for (Enumeration<?> entrie=jarFile.entries();entrie.hasMoreElements();)
		{
			ZipEntry entry=(ZipEntry)entrie.nextElement();
			if (entry.isDirectory()) continue;
			final String path=entry.getName();
			if (path.startsWith("META-INF/") &&
			    ((path.endsWith(".SF") || path.endsWith(".DSA"))))
			{
				throw new SignatureException("Jar "+jar+" is signed. Impossible to update.");
			}
		}
		
		File absoluteFile=jar.getAbsoluteFile();
		final String name=absoluteFile.getName();
		final int idx=name.lastIndexOf('.');
		File newFile=null;
		newFile=(!audit_ && output_!=null) ? output_ : new File(absoluteFile.getParent()+File.separator+name.substring(0,idx)+"-sealed"+name.substring(idx));
		if (newFile.exists()) newFile.delete();
	    DigestOutputStream fos = new DigestOutputStream(new FileOutputStream(newFile),MessageDigest.getInstance("SHA1"));

		JarOutputStream zos = new JarOutputStream(fos);
		for (Enumeration<?> entrie=jarFile.entries();entrie.hasMoreElements();)
		{
			ZipEntry entry=(ZipEntry)entrie.nextElement();
			InputStream in=jarFile.getInputStream(entry);

			// TODO : Manage INDEX.LIST
			finer("Copy "+entry.getName());
			if ("META-INF/INDEX.LIST".equals(entry.getName()))
			{
				continue;
			}
			if (!visitor.doFileName(idname,jarFile,entry,zos))
			{
				ZipEntry entr=entry;
				entr.setCompressedSize(-1);				
				try
				{
					zos.putNextEntry(entr);
				}
				catch (ZipException e)
				{
					if (e.getMessage().startsWith("duplicate entry")) 
					{
						warn(e.getMessage());
						continue;
					}
					throw (ZipException)e.fillInStackTrace();
				}
				copyStream(in,zos);
			}
			zos.closeEntry();
		}
		zos.close();
		jarFile.close();
		if (!update_)
		{
			newFile.delete();
			return null;
		}
		else
		{
			if (inPlace)
			{
				final File original=new File(absoluteFile+".original");
				absoluteFile.renameTo(new File(absoluteFile+".original"));
				newFile.renameTo(absoluteFile);
				if (!keeporiginal) original.delete();
			}
			else
			{
				if (audit_)
					newFile.delete();
			}
			return getHexString(fos.getMessageDigest().digest());
		}
	}

	
	/**
	 * Sealed WAR componant.
	 */
	private void doWar(String idName,File jar,final Collection<String> packages,boolean keeporiginal,final boolean inPlace) throws ZipException, IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed
	{
		info(((audit_) ? "Audit ": "Seal ")+idName);
		copyJar(idName,jar, new Visitor()
		{
			public boolean doFileName(String idName,JarFile jarFile,ZipEntry entry,JarOutputStream zos) throws IOException, NoSuchAlgorithmException,AllreadySealed
			{
				if (!entry.getName().equals("WEB-INF/lib/") && entry.getName().startsWith("WEB-INF/lib/"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					String basejarname=jarFile.getName().substring(jarFile.getName().lastIndexOf('/')+1);
					File jarfile=new File(TEMP+File.separator+basejarname+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						try
						{
							doJar(idName+"!"+entry.getName(),entry.getName(),jarfile,packages,false,true);
							ZipEntry entrylib=new ZipEntry(entry.getName());
							entrylib.setMethod(entry.getMethod());
							zos.putNextEntry(entrylib);
							in=new FileInputStream(jarfile);
							copyStream(in,zos);
							in.close();
						}
						catch (SignatureException e)
						{
							warn(e.getMessage());
						}
					}
					finally
					{
						jarfile.delete();
					}
					return true;
				}
				return false;
			}
		},keeporiginal,inPlace);
		
	}
	
	/**
	 * Sealed EAR componant.
	 */
	private void doEar(String idName,File jar,final Collection<String> packages,boolean keeporiginal,final boolean inPlace) throws ZipException, IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed
	{
		info(((audit_) ? "Audit ": "Seal ")+idName);
		copyJar(idName,jar, new Visitor()
		{
			public boolean doFileName(String idName,JarFile jarFile,ZipEntry entry,JarOutputStream zos) throws IOException, NoSuchAlgorithmException, AllreadySealed
			{
				if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(TEMP+File.separator+jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						try
						{
							if (entry.getName().endsWith(".jar"))
								doJar(idName+"!"+entry.getName(),entry.getName(),jarfile,packages,false,true);
							else
								doWar(idName+"!"+entry.getName(),jarfile,packages,false,true);
							ZipEntry entrylib=new ZipEntry(entry.getName());
							entrylib.setMethod(entry.getMethod());
							zos.putNextEntry(entrylib);
							in=new FileInputStream(jarfile);
							copyStream(in,zos);
							in.close();
						}
						catch (SignatureException e)
						{
							warn(e.getMessage());
						}
					}
					finally
					{
						jarfile.delete();
					}
					return true;
				}
				return false;
			}
		},keeporiginal,inPlace);
	}

	/**
	 * Sealed RAR componant.
	 */
	private void doRar(String idName,File jar,final Collection<String> packages,boolean keeporiginal,final boolean inPlace) throws ZipException, IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed
	{
		info(((audit_) ? "Audit ": "Seal ")+idName);
		copyJar(idName,jar, new Visitor()
		{
			public boolean doFileName(String idName,JarFile jarFile,ZipEntry entry,JarOutputStream zos) throws IOException, NoSuchAlgorithmException, AllreadySealed
			{
				if (entry.getName().endsWith(".jar"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(TEMP+File.separator+jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						try
						{
							doJar(idName+"!"+entry.getName(),entry.getName(),jarfile,packages,false,true);
							ZipEntry entrylib=new ZipEntry(entry.getName());
							entrylib.setMethod(entry.getMethod());
							zos.putNextEntry(entrylib);
							in=new FileInputStream(jarfile);
							copyStream(in,zos);
							in.close();
						}
						catch (SignatureException e)
						{
							warn(e.getMessage());
						}
					}
					finally
					{
						jarfile.delete();
					}
					return true;
				}
				return false;
			}
		},keeporiginal,inPlace);
	}
	
	/**
	 * Sealed JAR componant.
	 */
	private void doJar(final String idName,final String shortIdName,File jar,final Collection<String> packages,boolean keeporiginal,boolean inPlace) throws ZipException, IOException, NoSuchAlgorithmException, SignatureException, AllreadySealed
	{
		info(((audit_) ? "Audit ": "Seal ")+idName);
		String sha1=copyJar(idName,jar, new Visitor()
		{
			public boolean doFileName(String idName,JarFile jarFile,ZipEntry entry,JarOutputStream zos) throws IOException, NoSuchAlgorithmException, AllreadySealed
			{
				if (entry.getName().equals("META-INF/MANIFEST.MF"))
				{
					byte[] manifest=createManifest(idName,shortIdName,packages,jarFile);
					ZipEntry entrymanifest=new ZipEntry(entry.getName());
					entrymanifest.setSize(manifest.length);
					entrymanifest.setMethod(entry.getMethod());
					if (entry.getMethod()==ZipEntry.STORED)
					{
						CRC32 crc=new CRC32();
						crc.update(manifest);
						entrymanifest.setCrc(crc.getValue());
						entrymanifest.setCompressedSize(manifest.length);
					}
					zos.putNextEntry(entrymanifest);
					zos.write(manifest);
					return true;
				}
				return false;
			}
		},keeporiginal,inPlace);
		if ((inPlace) && (sha1_) && (sha1!=null))
		{
			File shaFile=new File(jar.getPath()+".sha1");
			if (shaFile.exists())
			{
				BufferedReader reader=new BufferedReader(new FileReader(shaFile));
				String line=reader.readLine();
				reader.close();
				
				PrintWriter writer=new PrintWriter(new FileWriter(shaFile));
				int idx=line.indexOf(' ');
				line=(idx>=0) ? line=line.substring(idx) : "";
				writer.println(sha1+line);
				writer.close();
			}
		}
	}

	/**
	 * Create manifest file.
	 * 
	 * @param idName The identity name.
	 * @param shortIdName The short identity name.
	 * @param sealedPackages The sealed packages.
	 * @param jarFile The jar file.
	 * @return The manifest file.
	 * @throws IOException If error.
	 * @throws AllreadySealed If component is allready sealed.
	 */
	private byte[] createManifest(final String idName,final String shortIdName,final Collection<String> sealedPackages,JarFile jarFile) throws IOException, AllreadySealed
	{

		ByteArrayOutputStream cawriter=new ByteArrayOutputStream();
		final PrintWriter newManifest=new PrintWriter(new OutputStreamWriter(cawriter,"UTF-8"));
		
		// Analyse la structure du jar pour extraire les packages.
		// Il faut trouver uniquement les répertoires avec des fichiers.
		final Collection<String> packages=new HashSet<String>();
		for (Enumeration<?> entrie=jarFile.entries();entrie.hasMoreElements();)
		{
			ZipEntry entry=(ZipEntry)entrie.nextElement();
			if (entry.isDirectory()) continue;
			String path=entry.getName();
			path=path.substring(0,path.lastIndexOf('/')+1);
			
			final boolean ignore=ignorePackageName_.contains(path);
			if ((path.trim().length()!=0) &&
			    entry.getName().endsWith(".class") &&
			    !ignore)
			{
				packages.add(path);
			}
		}
		
		// Read manifest
		BufferedReader in=new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarFile.getEntry("META-INF/MANIFEST.MF")),"UTF-8"));
		boolean main=true;
		boolean sealed=false;
		boolean allsealed=false;
		String packagename=null;
		StringBuffer bufline=new StringBuffer(in.readLine());
		int lignecnt=0;
		for (String line=in.readLine();line!=null;line=in.readLine())
		{
			++lignecnt;
			if ((line.length()>0) && ((line.charAt(0)==' ') || (line.charAt(0)=='\t')))
			{
				bufline.append(line.substring(1));
				continue;
			}
			else
			{
				String fixeline=bufline.toString();
				if (fixeline.trim().length()==0)
				{
					if (main)
					{
						main=false;
					}
					else if ((!sealed) && (packagename!=null) && packagename.charAt(packagename.length()-1)=='/')
					{
						update_=true;
						if (!allsealed)
						{
							finer(idName+": Add sealed for package "+packagename.replace('/', '.').substring(0,packagename.length()-1));
							finest("Sealed: true");
							newManifest.println("Sealed: true");
							if (sealedPackages.contains(packagename))
							{
								if (audit_)
								{
									auditStream_.println("\t\t<package name=\""+packagename.replaceAll("\"", "&#x22;")+"\" sealed=\"true\">");
									auditStream_.println("\t\t\t<context>"+shortIdName+"</context>");
									auditStream_.println("\t\t</package>");
								}
								else
									throw new AllreadySealed(idName+": Package "+packagename+" allready sealed !");
							}
						}
						sealedPackages.add(packagename);
					}
					packages.remove(packagename);
					packagename=null;
				}
				else 
				{
					final int idx=fixeline.indexOf(':');
					final String key=fixeline.substring(0,idx);
					final String value=fixeline.substring(idx+1).trim();
					if (!main)
					{
						if (key.equalsIgnoreCase("name"))
						{
							packagename=value;
							if (packagename.indexOf('.')!=-1)
							{
								warn(shortIdName+"!META-INF/MANIFEST.MF line "+lignecnt+" is not conform to specification! Do not use a dot package name.");
								packagename=packagename.replace('.', '/');
								if (packagename.charAt(packagename.length()-1)!='/')
									packagename=packagename+'/';
								fixeline=key+": "+packagename;
							}
							if (packagename.charAt(packagename.length()-1)!='/')
							{
								warn(shortIdName+"!META-INF/MANIFEST.MF line "+lignecnt+" is not conform to specification! Package name must final with '/'.");
								packagename=packagename+'/';
								fixeline=key+": "+packagename;
							}
						}
						if (key.equalsIgnoreCase("sealed"))
						{
							sealed=true; // Même si value="false";
							if (audit_)
							{
								auditStream_.println("\t\t<package name=\""+packagename.replaceAll("\"", "&#x22;")+"\" sealed=\"true\">");
								auditStream_.println("\t\t\t<context>"+shortIdName+"</context>");
								auditStream_.println("\t\t</package>");
							}
							else
								fine(idName+": Package "+packagename+" allready sealed !");
							packages.remove(packagename);
							sealedPackages.add(packagename);
						}
					}
					else
					{
						if (key.equalsIgnoreCase("name"))
						{
							warn(shortIdName+"!META-INF/MANIFEST.MF line "+lignecnt+" is not conform to specification! Name: in header.");
//							fixeline="#"+fixeline;								
							bufline.setLength(0);
							bufline.append(line);
							continue;
						}
						if (key.equalsIgnoreCase("sealed") && "true".equals(value))
						{
							allsealed=true;
						}
					}
				}
				int MAX=69;
				if (fixeline.length()<=MAX) 
				{
					finest(fixeline);
					newManifest.println(fixeline);
				}
				else
				{
					String substring=fixeline.substring(0,MAX+1);
					finest(substring);
					newManifest.println(substring);
					for (int i=MAX+1;i<fixeline.length();i+=MAX)
					{
						substring=" "+fixeline.substring(i,Math.min(i+MAX,fixeline.length()));
						finest(substring);
						newManifest.println(substring);
					}
				}
				bufline.setLength(0);
				bufline.append(line);
			}
		}
		if ((packagename!=null) /* && !sealed && !allsealed*/)
		{
			finest("Sealed: true");
			newManifest.println("Sealed: true");
			packages.remove(packagename);
		}
		
		// Ajout des packages absents
		if (!allsealed)
		{
			for (String pac:packages)
			{
				newManifest.println();
				update_=true;
				finer(idName+": Add sealed for package "+pac.replace('/', '.').substring(0,pac.length()-1));
				finest("Name: "+pac);
				finest("Sealed: true");
				newManifest.println("Name: "+pac);
				newManifest.println("Sealed: true");
			}
		}
		newManifest.close();
		fine(new String(cawriter.toByteArray()));	
		return cawriter.toByteArray();
	}
	
	/**
	 * Seal or audit all files.
	 * @param names All files.
	 * @param keeporiginal Keep a copy of original ?
	 * @param inPlace Update in place ?
	 * @throws IOException If error.
	 * @throws NoSuchAlgorithmException If SHA1 is not disponible.
	 * @throws AllreadySealed If a component is allready sealed.
	 */
	private void doAll(File[] names,boolean keeporiginal,boolean inPlace) throws IOException, NoSuchAlgorithmException, AllreadySealed
	{
		if (names==null) return;
		for (File name:names)
		{
			try
			{
				if (name.isDirectory())
				{
					final File[] alljar=name.listFiles(new FileFilter()
					{
						public boolean accept(File pathname)
						{
							return (pathname.getName().endsWith(".jar")
									|| pathname.getName().endsWith(".war")
									|| pathname.getName().endsWith(".ear")
									|| pathname.getName().endsWith(".rar")
									|| (recurs_ && pathname.isDirectory()));
						}
					});
					doAll(alljar,keeporiginal,inPlace);
	
				}
				else if (name.getName().endsWith(".war"))
				{
					update_=false;
					doWar(name.toString(),name,new HashSet<String>(), keeporiginal,inPlace);
				}
				else if (name.getName().endsWith(".ear"))
				{
					update_=false;
					doEar(name.toString(),name,new HashSet<String>(),keeporiginal,inPlace);
				}
				else if (name.getName().endsWith(".rar"))
				{
					update_=false;
					doRar(name.toString(),name,new HashSet<String>(),keeporiginal,inPlace);
				}
				else if (name.getName().endsWith(".jar"))
				{
					update_=false;
					doJar(name.toString(),name.getName(),name,new HashSet<String>(),keeporiginal,inPlace);
					
				}
				else
				{
					throw new IllegalArgumentException("Unknown file type for "+name.getName());
				}
			}
			catch (SignatureException e)
			{
				warn(e.getMessage());
			}
		}
		
	}
	
	/**
	 * Inject XSLT file.
	 * @param out
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private void insertXSLT(PrintWriter out) throws MalformedURLException, IOException
	{
		if (!"".equals(xslt_) && dir_!=null)
		{
			URL u=null;
			try
			{
				u=new URL(xslt_);
			}
			catch (MalformedURLException e)
			{
				u=new File(xslt_).getCanonicalFile().toURI().toURL();
			}
			if (u.toExternalForm().equals(defaultStylesheet))
			{
				u=getClass().getResource("sealed-to-xhtml.xslt");
			}
			String path=u.getPath();
			path=path.substring(path.lastIndexOf('/')+1);
			File outputxslt=new File(dir_+File.separator+path).getCanonicalFile();
			if (!outputxslt.exists())
			{
				InputStream i=u.openStream();
				OutputStream o=new BufferedOutputStream(new FileOutputStream(dir_+File.separator+path));
				copyStream(i,o);
				i.close();
				o.close();
			}
			out.println("<?xml-stylesheet href=\""+path+"\" type=\"text/xsl\"?>");
		}

	}
	
	/**
	 * Seal or audit all files.
	 * 
	 * @param names All files.
	 */
	public void doAll(File[] names)
	{
		try
		{
			if (audit_)
			{
				auditStream_.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
				insertXSLT(auditStream_);
				auditStream_.print("<audit \n"+
					"\txmlns=\""+XMLContext.NAMESPACESTR+"\"\n"+
					"\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
					"\txsi:schemaLocation=\"http://macaron.googlecode.com/1.0/ "+defaultXSD+"\"\n"+
					"\tignoreFilenames=\"\"\n"+
					">\n");
				auditStream_.println("\t<packages>");
			}
			doAll(names,keepOriginal_,inPlace_);
			if (audit_)
			{
				auditStream_.println("\t</packages>");
				auditStream_.println("</audit>");
				auditStream_.close();
			}
		}
		catch (ZipException e)
		{
			error(e);
		}
		catch (NoSuchAlgorithmException e)
		{
			error(e);
		}
		catch (IOException e)
		{
			error(e);
		}
		catch (AllreadySealed e)
		{
			error(e);
		}
	}
	
	/**
	 * Parse an XML node.
	 *  
	 * @param document The XML document.
	 * @param environnement XPath environement.
	 * @param xpath The XPath.
	 * @param dico The result.
	 * @throws XPathExpressionException If error.
	 */
	private void parseNode(Document document,XPath environnement,String xpath,Set<String> dico) throws XPathExpressionException
	{
		XPathExpression xpathResources = environnement.compile(xpath);
		NodeList result = (NodeList) xpathResources.evaluate(document, XPathConstants.NODESET);
		for (int i=0;i<result.getLength();++i)
		{
			final Node packageNode=result.item(i);
			final String name= packageNode.getAttributes().getNamedItem("name").getNodeValue();
			warn("Ignore \""+name+'"');
			dico.add(name);
		}
	}
	
	/**
	 * Parse ignore file.
	 * @param data The file.
	 * @throws IOException If error.
	 * @throws SAXException If error.
	 * @throws ParserConfigurationException If error.
	 * @throws XPathExpressionException If error.
	 */
	private void parseIgnore(final URL data) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException
	{
		final InputStream in = ("-".equals(data.getHost())) ? System.in : data.openStream();
		try
		{
			final Document ignoreParams = XMLContext.DOC_BUILDER_FACTORY.newDocumentBuilder().parse(in);

			XPath environnement = XMLContext.XPATH_FACTORY.newXPath();
			environnement.setNamespaceContext(XMLContext.NAMESPACE);
			parseNode(ignoreParams,environnement,"//packages/package",ignorePackageName_);
		}
		finally
		{
			if (data!=null) in.close();
		}
		
	}
	
	/**
	 * Print help screen.
	 * 
	 * @param out Stream to use.
	 */
	private static void help(final PrintStream out)
	{
		out.println(
		//             1         2         3        4         5         6         7
		//   0123456789012345678901234567890123467890123456789012345678901234567890123456789
			"Usage: macaron-seal [-i] [-k] [-s] [-R] [-a] [-l <int>] \\\n"+
			"                    [-I <-|file|url>] [-o <file>] [-h] \\\n"+
			"                    [--xslt <str>] \\\n"+
			"                    [--loglevel <severe|warning|info|...>] \\\n"+
			"                    <dir>|<files(.jar|.war|.ear|.rar)>)+\n"+
			"(-o|--output <file>)       : The output jar.\n"+
			"(-i|--in-place)            : Update the archive in place.\n"+
			"(-I|--ignore) <-|file|url> : Update the archive in place.\n"+
			"(-k|--keep-original)       : Keep a copy of original in <name>.original.\n"+
			"(-s|--sha1)                : If present, update the <name>.sha file.\n"+
			"(-R|--recursive)           : Seale all component in directory and \n"+
			"                             sub-directory.\n"+
			"(-a|--audit)               : Audit the component.\n"+
			"--xslt <str>               : The xslt to inject in xml result.\n"+
			"(-l|--loglevel) <str>      : Java Log level (severe, warning, info, fine, ...)\n"+
			"(-h|--help)                : This help\n"+
			"\n"+
			"Add \"Sealed: true\" attribute in each META-INF/MANIFEST.MF in jar or\n"+
			"inner jar."
		);
	}
	
	/**
	 * Error in command line.
	 */
	private static final int ERROR = -1;

	/**
	 * Help in command line.
	 */
	private static final int ASK_HELP = -2;

	/**
	 * All configuration.
	 */
	private static class ParamsConfig
	{
		boolean inPlace=false;
		URL ignore=Seal.class.getResource("ignore.xml");
		boolean keepOriginal=false;
		boolean sha1=false;
		boolean recurs=false;
		File output;
		boolean audit=false;
		List<File> names=new ArrayList<File>();
		String xslt=defaultStylesheet;
	}
	
	/**
	 * Parse command line.
	 * @param params The configuration.
	 * @param args The args.
	 * @param pos The current position.
	 * @return The current position.
	 * 
	 * @throws CommandLineException If syntax error.
	 */
	public static int parseArg(final ParamsConfig params, final String[] args, final int pos) throws CommandLineException
	{
		int position = pos;
		final String arg = args[position];
		if ("-i".equals(arg) || "--in-place".equals(arg))
		{
			params.inPlace = (params.inPlace==false);
		}
		else if ("-k".equals(arg) || "--keep-original".equals(arg))
		{
			params.keepOriginal=(params.keepOriginal==false);
		}
		else if ("-s".equals(arg) || "--sha1".equals(arg))
		{
			params.sha1=(params.sha1==false);
		}
		else if (("-o".equals(arg) || "--output".equals(arg)) && position < args.length - 1)
		{
			params.output=new File(args[++position]);
		}
		else if ("-a".equals(arg) || "--audit".equals(arg))
		{
			params.audit=true;
		}
		else if ("-r".equals(arg) || "-R".equals(arg) || "--recursive".equals(arg))
		{
			params.recurs=(params.recurs==false);
		}
		else if ("--xslt".equals(arg) && position < args.length - 1)
		{
			params.xslt=args[++position];
		}
		else if (("-I".equals(arg) || "--ignore".equals(arg)) && position < args.length - 1)
		{
			String ignore=args[++position];
			if ("-".equals(ignore))
			{
				ignore="http://-";
			}
			try
			{
				params.ignore=new URL(ignore);
			}
			catch (MalformedURLException x)
			{
				final int idx = ignore.indexOf('#');
				try
				{
					params.ignore = new File((idx != -1) ? ignore.substring(
						0, idx) : ignore).toURI().toURL();
				}
				catch (MalformedURLException e)
				{
					throw new CommandLineException(e.getLocalizedMessage());
				}
			}
		}
		else if (("-l".equals(arg) || "--loglevel".equals(arg)) && position < args.length - 1)
		{
			log.setLevel(Level.parse(args[++position].toUpperCase()));
		}
		else if ("-h".equals(arg) || "--help".equals(arg))
		{
			help(System.out);
			return ASK_HELP;
		}
		else
		{
			if (arg.charAt(0)=='-')
				return ERROR;
			params.names.add(new File(arg));
		}
		return position;
	}
	
	/**
	 * Parse command line.
	 * 
	 * @param args The args.
	 * @return The parameters.
	 * 
	 * @throws CommandLineException If syntax error.
	 */
	public static ParamsConfig parseArgs(final String[] args) throws CommandLineException
	{
		final ParamsConfig params = new ParamsConfig();

		for (int i = 0, j; i < args.length; i = ++j)
		{
			j = parseArg(params, args, i);
			if (j < 0)
			{
				if (j == ASK_HELP)
					return null;
				throw new CommandLineException("Unknow parameters " + args[i]);
			}
		}
		if (params.audit && params.output==null)
			throw new CommandLineException("--output must be set.");
		if (params.names==null)
		{
			help(System.out);
			throw new CommandLineException("input file must be set.");
		}
		return params;
	}

	/**
	 * Analyse argument and seal or audit components.
	 * @param args Args.
	 */
	public static int mainRet(final String[] args)
	{
		final PrintWriter err=new PrintWriter(System.err,true);
		try
		{
			ParamsConfig config=parseArgs(args);
			if (config==null) return 0;
			new Seal(config).doAll(config.names.toArray(new File[config.names.size()]));
			err.flush();
			return 0;
		}
		catch (CommandLineException e)
		{
			err.println(e.getLocalizedMessage());
			return 1;
		}
		catch (IOException e)
		{
			err.println(e.getLocalizedMessage());
			return 1;
		}
		catch (SAXException e)
		{
			err.println(e.getLocalizedMessage());
			return 1;
		}
	}

	/**
	 * JDK5 log formater.
	 * @author pprados
	 *
	 */
	public static class TinyFormatter extends Formatter 
	{
		private static final int max(int a,int b)
		{
			return (a>b) ? a : b;
		}
		private static final String format_; 
		static
		{
			int w=max(Level.SEVERE.getLocalizedName().length(),Level.WARNING.getLocalizedName().length());
			w=max(w,Level.INFO.getLocalizedName().length());
			w=max(w,Level.CONFIG.getLocalizedName().length());
			w=max(w,Level.FINE.getLocalizedName().length());
			w=max(w,Level.FINER.getLocalizedName().length());
			w=max(w,Level.FINEST.getLocalizedName().length());
			format_="%1$"+w+"s: %2$s%n";
		}
	    /**
	     * Format the given LogRecord.
	     * @param record the log record to be formatted.
	     * @return a formatted log record
	     */
	    public synchronized String format(LogRecord record) 
	    {
			final StringBuffer sb = new StringBuffer();
			final java.util.Formatter formatter = new java.util.Formatter(sb, Locale.US);
	    	formatter.format(format_,new Object[]{record.getLevel().getLocalizedName(),formatMessage(record)});
			return sb.toString();
	    }
	}

	
	/**
	 * Command line main.
	 * @param args
	 */
	public static void main(final String[] args)
	{
		try
		{
			LogManager.getLogManager().readConfiguration(Seal.class.getResourceAsStream("/META-INF/logging.properties"));
			System.exit(mainRet(args));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
