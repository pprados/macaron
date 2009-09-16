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
package com.googlecode.macaron.audit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Analyse each files and components and generate an XML rapport with all suspicious files.
 * 
 * @author Philippe PRADOS
 */
public class Audit
{
	/** If debug */
	private static boolean debug = true;
	/** The logger. Use to print steps. */
	private static Logger log = Logger.getLogger(Audit.class.getName());
	static
	{
		log.setLevel(Level.SEVERE);
	}
	
	/** Tempo directory. */
	private static final String TEMP;
	static
	{
		final String t=System.getProperty("java.io.tmpdir");
		TEMP=(t==null) ? "." : t;
	}

	/** Default home for XSTL and XSD */
	private static final String home="http://macaron.googlecode.com/svn/trunk/audit/src/main/resources/com/googlecode/macaron/audit/";
	
	/** Default xslt. */
	public static final String DEFAULT_STYLE_SHEET = home + "audit-to-xhtml.xslt";
	/** Default XML Schema. */
	private static final String defaultXSD=home+"audit.xsd";

	/** Context of current file. */
	static class Context
	{
		String idName;
		String baseIdName;
		String path;
		String packageName;
		String name;
		String ext;
		String basename;
		String codebase;
		public int hashCode() // TODO: hash ?
		{
			return idName.hashCode();
		}
		public String toString()
		{
			return idName+"!"+path;
		}
		Context(Context ctx)
		{
			idName=ctx.idName;
			baseIdName=ctx.baseIdName;
			path=ctx.path;
			packageName=ctx.packageName;
			name=ctx.name;
			ext=ctx.ext;
			basename=ctx.basename;
			codebase=ctx.codebase;
		}
		Context()
		{
			
		}
	}
	
	/** Packages to ignore. */
	private HashMap<String,Set<String>> ignorePackageName_=new LinkedHashMap<String,Set<String>>();
	/** Resource to ignore. */
	private HashMap<String,Set<String>> ignoreName_=new LinkedHashMap<String,Set<String>>();
	/** Basenames to ignore. */
	private HashMap<String,Set<String>> ignoreBaseName_=new LinkedHashMap<String,Set<String>>();
	/** Services to ignore. */
	private HashMap<String,Set<String>> ignoreServices_=new LinkedHashMap<String,Set<String>>();
	/** Annotation to detect. */
	private HashMap<String,Set<String>> ignoreAnnotations_=new LinkedHashMap<String,Set<String>>();
	
	private HashMap<String,String> detectAnnotations_=new LinkedHashMap<String,String>();

	/** Contexts with packages. */
	private HashMap<String,Set<Context>> packageName_=new LinkedHashMap<String,Set<Context>>();
	/** Contexts with name. */
	private HashMap<String,Set<Context>> name_=new LinkedHashMap<String,Set<Context>>();
	/** Contexts with basenames. */
	private HashMap<String,Set<Context>> baseName_=new LinkedHashMap<String,Set<Context>>();
	/** Contexts with services. */
	private HashMap<String,Set<Context>> services_=new LinkedHashMap<String,Set<Context>>();
	/** Contexts with annotations. */
	private HashMap<String,Set<Context>> annotations_=new LinkedHashMap<String,Set<Context>>();
	
	/**
		FIXME: should use directly ConfigParams ...
	*/
	/** Use recursive analyse with directory ? */
	private boolean recurs_;
	/** XSLT to inject in the result. */
	private String xslt_;
	/** XSLT output dir. */
	private String dir_;
	/** Writer for the report. */
	private PrintWriter out_;
	/** Filename to exclude. */
	private Pattern excludePattern;
	private ParamsConfig config;

	/**
	 * Constructor.
	 * 
	 * @param output The output filename or "-".
	 * @param xslt The XSLT to inject.
	 * @param ignore Ignore file.
	 * @param recurs Use recursive analyse with directory ?
	 * @throws IOException If IO error.
	 * @throws SAXException If error when parse xml file.
	 */
	private void setup(File output,String xslt,URL ignore,boolean recurs) throws IOException, SAXException
	{
		try
		{
			recurs_=recurs;
			parseIgnore(Audit.class.getResource("ignore.xml"));
			if (ignore!=null) parseIgnore(ignore);
			if (output.getName().equals("-"))
				out_=new PrintWriter(System.out,debug);
			else
				out_=new PrintWriter(new FileWriter(output));
			dir_=output.getAbsoluteFile().getParent();
			assert( dir_!=null);
			xslt_=xslt;
		}
		catch (XPathExpressionException e)
		{
			throw new AssertionError(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param config All configuration.
	 * @throws IOException If IO error.
	 * @throws SAXException If error when parse xml file.
	 */
	public Audit(ParamsConfig config)
	{
		this.config = config;
	}

	public void init() throws IOException, SAXException {
		this.setup(config.getOutput(),config.getXslt(),config.getIgnore(),config.isRecurs());
	}
	

	/**
	 * Exception when command line error.
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
		void doFileName(String idname,JarFile jarFile,ZipEntry entry) throws IOException;
	}
	
	/**
	 * Copy stream.
	 * 
	 * @param in Source.
	 * @param out Destination.
	 * @throws IOException If error.
	 */
	private static void copyStream(InputStream in,OutputStream out) throws IOException
	{
		byte[] tampon=new byte[4096];
	    int len;
        while ((len = in.read(tampon)) > 0) {
            out.write(tampon, 0, len);
        }
	    out.flush();
	}

	/**
	 * Analyse zip file.
	 * @param idName The identity name.
	 * @param baseIdName The base identity name.
	 * @param path The path of component.
	 * @param in The inputstream
	 * @param length The file size
	 * @throws IOException 
	 */
	private void auditFile(String idName,final String baseIdName, final String path,InputStream in,long length) throws IOException
	{
		try
		{
			final Context ctx=new Context();
			if (log.isLoggable(Level.FINEST)) log.fine("analyseZipFile("+idName+","+baseIdName+","+path+")");
			int idx=path.lastIndexOf('/');
			ctx.idName=idName;
			ctx.baseIdName=baseIdName;
			ctx.path=path;
			ctx.packageName=(idx==-1) ? "" : path.substring(0,idx+1);
			ctx.name=(idx==-1) ? path : path.substring(idx+1);
			idx=ctx.name.lastIndexOf('.');
			ctx.basename=ctx.packageName+((idx==-1) ? ctx.name : ctx.name.substring(0,ctx.name.lastIndexOf('.')));
			ctx.ext=(idx==-1) ? "" : ctx.name.substring(idx);
			if (excludePattern.matcher(ctx.name).matches()) return;
	
			Set<Context> contexts;
			
			// Analyze packages
			{
				contexts=packageName_.get(ctx.packageName);
				if (contexts==null)
				{
					contexts=new HashSet<Context>();
					packageName_.put(ctx.packageName, contexts);
				}
				boolean find=false;
				for (Context ctx2:contexts)
					if (ctx2.idName.equals(ctx.idName))
					{
						find=true;
						break;
					}
				if (!find)	contexts.add(ctx);
			}
	
			// Analyze fullnames
			if (!ctx.name.endsWith(".class"))
			{
				contexts=name_.get(ctx.name);
				if (contexts==null)
				{
					contexts=new HashSet<Context>();
					name_.put(ctx.name, contexts);
				}
				contexts.add(ctx);
	
			}
			else
			{
				// Analyze annotations
				final Set<String> annotationsFinded=new HashSet<String>();
	    		ClassFile classFile=new ClassFile();
	    		classFile.setConstantPoolInfo(new ConstantPoolInfo(new CustomAnnotationScanner()
	    		{

	    			public boolean isAnnotation(String value)
	    			{
	    				boolean find=(detectAnnotations_.get(value)!=null);
	    				if (find)
	    				{
	    					annotationsFinded.add(value);
	    				}
	    				return find;
	    			}
	    			
	    		}));
				ReadableByteChannel channel = Channels.newChannel(in);
				assert(channel!=null);
	            if (classFile.containsAnnotation(channel, length))
	            {
		    		for (String anno:annotationsFinded)
		    		{
		    			String normalAnnotation=detectAnnotations_.get(anno);
						contexts=annotations_.get(normalAnnotation);
						if (contexts==null)
						{
							contexts=new HashSet<Context>();
							annotations_.put(normalAnnotation, contexts);
						}
						contexts.add(ctx);
		    		}
	            }
			}
			// Analyze basename
			{
				contexts=baseName_.get(ctx.basename);
				if (contexts==null)
				{
					contexts=new HashSet<Context>();
					baseName_.put(ctx.basename, contexts);
				}
				contexts.add(ctx);
			}
	
			// Analyze services
			if (ctx.packageName.startsWith("META-INF/services/"))
			{
				contexts=services_.get(ctx.path);
				if (contexts==null)
				{
					contexts=new HashSet<Context>();
					services_.put(ctx.path, contexts);
				}
				contexts.add(ctx);
			}
		}
		finally
		{
			in.close();
		}
	}
	
	/**
	 * Audit a component.
	 * 
	 * @param idname The identity name.
	 * @param jar The JAR file.
	 * @param visitor The visitor.
	 * 
	 * @throws IOException If error.
	 */
	private void auditComponent(String idname,File jar,Visitor visitor) throws IOException
	{
		JarFile jarFile=new JarFile(jar);
		try
		{
			for (Enumeration<?> entrie=jarFile.entries();entrie.hasMoreElements();)
			{
				ZipEntry entry=(ZipEntry)entrie.nextElement();
				if (entry.isDirectory()) continue;
	
				visitor.doFileName(idname,jarFile,entry);
			}
		}
		finally
		{
			jarFile.close();
		}
	}
	
	/**
	 * Audit JAR component.
	 * 
	 * @param idname The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doJar(final String idName,final String baseIdName,File jar) throws IOException
	{
		log.info("Audit "+idName);
		auditComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.isDirectory()) return;
				auditFile(idName, baseIdName, entry.getName(),jarFile.getInputStream(entry),entry.getSize());
			}

		});
	}
	
	/**
	 * Audit WAR component.
	 * 
	 * @param idname The identity name.
	 * @param baseIdName The base identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doWar(final String idName,final String baseIdName,File jar) throws IOException
	{
		log.info("Audit "+idName);
		final int len="WEB-INF/lib/".length();
		auditComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().startsWith("WEB-INF/lib/"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(TEMP+File.separator+new File(jarFile.getName()).getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						// Audit de l'archive
						doJar(idName+"!"+entry.getName(),entry.getName().substring(len),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
				else if (entry.getName().startsWith("WEB-INF/classes/"))
				{
					auditFile(idName,baseIdName, entry.getName().substring("WEB-INF/classes/".length()),jarFile.getInputStream(entry),entry.getSize());
				}
			}
		});
	}
	
	/**
	 * Audit EAR component.
	 * 
	 * @param idname The identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doEar(String idName,File jar) throws IOException
	{
		log.info("Audit "+idName);
		auditComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						if (entry.getName().endsWith(".jar"))
							doJar(idName+"!"+entry.getName(),entry.getName(),jarfile);
						else
							doWar(idName+"!"+entry.getName(),entry.getName(),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
			}
		});
	}
	
	/**
	 * Audit RAR component.
	 * 
	 * @param idname The identity name.
	 * @param jar The JAR file.
	 * @throws IOException If error.
	 */
	private void doRar(String idName,File jar) throws IOException
	{
		log.info("Audit "+idName);
		auditComponent(idName,jar, new Visitor()
		{
			public void doFileName(String idName,JarFile jarFile,ZipEntry entry) throws IOException
			{
				if (entry.getName().endsWith(".jar"))
				{
					// Extraire l'archive
					String basename=entry.getName().substring(entry.getName().lastIndexOf('/')+1);
					File jarfile=new File(jarFile.getName()+"-"+basename);
					try
					{
						OutputStream out=new FileOutputStream(jarfile);
						InputStream in=jarFile.getInputStream(entry);
						copyStream(in,out);
						in.close();
						out.close();
						doJar(idName+"!"+entry.getName(),entry.getName(),jarfile);
					}
					finally
					{
						jarfile.delete();
					}
				}
			}
		});
	}

	/**
	 * Analyze all files.
	 * 
	 * @param names All files.
	 * @throws IOException If error.
	 */
	//FIXME: won't a List<File> be more elegant ? 
	//       or even List<String> filenames; which ask less work 
	//		 for user API
	public void doAll(File[] names) throws IOException
	{
		if (names==null) return;
		for (File name:names)
		{
			if (name.isDirectory())
			{
				final File[] alljar= name.listFiles(new AuditFileFilter(this.recurs_));
				if (alljar.length!=0)
				{
					doAll(alljar);
				}

			}
			else if (name.getName().endsWith(".war"))
			{
				doWar(name.toString(),name.getName(),name);
			}
			else if (name.getName().endsWith(".ear"))
			{
				doEar(name.toString(),name);
			}
			else if (name.getName().endsWith(".rar"))
			{
				doRar(name.toString(),name);
			}
			else if (name.getName().endsWith(".jar"))
			{
				doJar(name.toString(),name.getName(),name);
			}
			else if (name.getName().endsWith(".class"))
			{
				InputStream in=new FileInputStream(name);
				auditFile(name.getName(), name.getName(), name.getName(),in,name.length());
			}
			else
			{
				throw new IllegalArgumentException("Unknown file type for "+name.getName());
			}
		}
		
		// Post analyse
		for (String item:new HashSet<String>(baseName_.keySet()))
		{
			Set<Context> contexts=baseName_.get(item);
			
			// Verifie qu'il y a bien des extensions différentes
			String ext=null;
			boolean mixe=false;
			for (Context ctx:contexts)
			{
				if (ext==null) ext=ctx.ext;
				else if (!ext.equals(ctx.ext))
				{
					mixe=true;
					break;
				}
			}
			if (!mixe)
			{
				contexts.clear();	// Si toutes les ext sont identiques, nettoie.
				baseName_.remove(item);
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
		if (!"".equals(xslt_))
		{
			URL u = null;
			try
			{
				u = new URL(xslt_);
			}
			catch (MalformedURLException e)
			{
				u = new File(xslt_).getCanonicalFile().toURI().toURL();
			}
			if (u.toExternalForm().equals(DEFAULT_STYLE_SHEET ))
			{
				u=getClass().getResource("audit-to-xhtml.xslt");
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
	 * Write XML report.
	 * 
	 * @param out The writer.
	 * @throws IOException 
	 */
	public void writeXMLReport(PrintWriter out) throws IOException
	{
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		insertXSLT(out);
		StringBuilder ann=new StringBuilder();
		for (String k:detectAnnotations_.keySet())
		{
			ann.append(detectAnnotations_.get(k));
			ann.append(',');
		}
		ann.setLength(ann.length()-1);
		out.print("<audit \n"+
			"\txmlns=\""+XMLContext.NAMESPACESTR+"\"\n"+
			"\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
			"\txsi:schemaLocation=\"http://macaron.googlecode.com/1.0/ "+defaultXSD+"\"\n"+
			"\tignoreFilenames=\""+excludePattern.toString().replaceAll("\"", "&#x22;")+"\"\n"+
			"\tannotations=\""+ann.toString()+"\"\n"+
			">\n");
		// Analyse des packages dans les archives
		writeXMLPackagesReport(out);

		// Analyse des packages dans les archives
		writeXMLFilenameReport(out);

		// Je recherche des fichiers de même noms placés aux mêmes packages
		writeXMLBasenameReport(out);

		// Je recherche des fichiers de même noms placés aux mêmes packages
		writeServicesReport(out);

		// Je recherche des annotations dans les classes
		writeAnnotationsReport(out);

		out.print("</audit>\n");
	}

	/**
	 * Write XML report.
	 * @throws IOException 
	 */
	public void writeXMLReport() throws IOException
	{
		writeXMLReport(out_);
		out_.flush();
	}
	
	enum Scope { jar, pack, clazz };
	/**
	 * Write XML partial report.
	 * 
	 * @param out The writer.
	 * @param tag The current tag.
	 * @param list The list of context.
	 * @param ignoreList The ignore list.
	 * @param mincontext The minimum number of context to write.
	 * @param all Flag to generate all context or local context.
	 */
	private static void writeXMLPartialReport(
			PrintWriter out,String tag,HashMap<String,Set<Context>> list,HashMap<String,Set<String>> ignoreList,int mincontext,Scope scope)
	{
		boolean empty=true;
		for (String item:list.keySet())
		{
			Set<Context> contexts=new HashSet<Context>(list.get(item));
			
			// Si je n'ai qu'une fois le contexte, tous est bon
			if (contexts.size()>=mincontext)
			{
				// Je vire d'abord les packages à ignorer
				Set<String> ignore=ignoreList.get(item);
				if (ignore!=null)
				{
					if (ignore.size()==0) // Ignore partout
						continue;
					for (String codebaseToIgnore:ignore)
					{
						String packageName=null;
						int idx=codebaseToIgnore.indexOf('!');
						if (idx!=-1)
						{
							packageName=codebaseToIgnore.substring(idx+1);
							codebaseToIgnore=codebaseToIgnore.substring(0,idx);
						}
						for (Context ctx:list.get(item))
						{
							switch (scope)
							{
								case clazz:
									if (ctx.baseIdName.equals(codebaseToIgnore) && (ctx.path.equals(packageName)))
										contexts.remove(ctx);
									break;
								case pack:
									if (ctx.baseIdName.equals(codebaseToIgnore) && (ctx.packageName.equals(packageName)))
										contexts.remove(ctx);
									break;
								default:
									if (ctx.baseIdName.equals(codebaseToIgnore))
										contexts.remove(ctx);
									break;
							}
						}
					}
				}
				// Il en reste ? Alors repart du début
				if (contexts.size()>0)
				{
					contexts=list.get(item);
					if (empty)
					{
						empty=false;
						out.print("\t<"+tag+"s>\n");
					}
					out.print("\t\t<"+tag+" name=\""+item+"\">\n");
					for (Context c:contexts)
					{
						switch (scope)
						{
							case clazz:
								out.print("\t\t\t<context>"+c.baseIdName+"!"+c.path+"</context>\n");
								break;
							case pack:
								out.print("\t\t\t<context>"+c.baseIdName+"!"+c.packageName+"</context>\n");
								break;
							default:
								out.print("\t\t\t<context>"+c.baseIdName+"</context>\n");
							break;
						}
					}
					out.print("\t\t</"+tag+">\n");
				}
			}
		}
		out.print((empty) ? "\t<"+tag+"s/>\n":"\t</"+tag+"s>\n");
	}

	/**
	 * Write XML basename report.
	 * 
	 * @param out The writer.
	 */
	private void writeXMLBasenameReport(PrintWriter out)
	{
		out.println("\n\t<!-- Same basename with different extensions in same package. -->");
		writeXMLPartialReport(out,"basename",baseName_,ignoreBaseName_,2,Scope.clazz);
		
	}

	/**
	 * Write XML filename report.
	 * 
	 * @param out The writer.
	 */
	private void writeXMLFilenameReport(PrintWriter out)
	{
		out.println("\n\t<!-- Same name with different package. -->");
		writeXMLPartialReport(out,"filename",name_,ignoreName_,2,Scope.pack);
	}

	/**
	 * Write XML packages report.
	 * 
	 * @param out The writer.
	 */
	private void writeXMLPackagesReport(PrintWriter out)
	{
		out.println("\n\t<!-- Same package from different jar. -->");
		writeXMLPartialReport(out,"package",packageName_,ignorePackageName_,2,Scope.jar);
	}
	
	/**
	 * Write XML services report.
	 * 
	 * @param out The writer.
	 */
	private void writeServicesReport(PrintWriter out)
	{
		out.println("\n\t<!-- Services in jar. -->");
		writeXMLPartialReport(out,"service",services_,ignoreServices_,1,Scope.jar);
	}
	
	/**
	 * Write Annotation report.
	 * 
	 * @param out The writer.
	 */
	private void writeAnnotationsReport(PrintWriter out)
	{
		out.println("\n\t<!-- Suspicious Annotations. -->");
		writeXMLPartialReport(out,"annotation",annotations_,ignoreAnnotations_,1,Scope.clazz);
	}
	
	/**
	 * Parse Node.
	 * 
	 * @param ignoreParams The XML document.
	 * @param environnement The XPath environnement.
	 * @param xpath The XPath string.
	 * @param dico The result.
	 * @throws XPathExpressionException If error.
	 */
	private void parseNode(Document ignoreParams,XPath environnement,String xpath,HashMap<String,Set<String>> dico) throws XPathExpressionException
	{
		XPathExpression xpathResources = environnement.compile(xpath);
		NodeList result = (NodeList) xpathResources.evaluate(ignoreParams, XPathConstants.NODESET);
		for (int i=0;i<result.getLength();++i)
		{
			final Node packageNode=result.item(i);
			String name= packageNode.getAttributes().getNamedItem("name").getNodeValue();
			final Set<String> set=new HashSet<String>();
			final NodeList childs=packageNode.getChildNodes();
			for (int j=0;j<childs.getLength();++j)
			{
				final Node contexts=childs.item(j);
				if (contexts.getNodeName().equals("context"))
				{
					final String codebase=contexts.getTextContent();
					set.add(codebase);
				}
			}
			dico.put(name, set);

		}
	}
	
	/**
	 * Parse ignore file.
	 * 
	 * @param data The URL.
	 * @throws IOException If error.
	 * @throws SAXException If error.
	 * @throws ParserConfigurationException If error.
	 * @throws XPathExpressionException If error.
	 */
	private void parseIgnore(final URL data) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException
	{
		if (data==null) return;
		final InputStream in = data.openStream();
		try
		{
			final Document ignoreParams = XMLContext.DOC_BUILDER_FACTORY.newDocumentBuilder().parse(in);

			XPath environnement = XMLContext.XPATH_FACTORY.newXPath();
			environnement.setNamespaceContext(XMLContext.NAMESPACE);
			XPathExpression xpathIgnorePattern = environnement.compile("/audit/@ignoreFilenames");
			NodeList result = (NodeList) xpathIgnorePattern.evaluate(ignoreParams, XPathConstants.NODESET);
			if (result.getLength()==1)
			{
				excludePattern=Pattern.compile(result.item(0).getTextContent());
			}
			XPathExpression xpathAnnotations = environnement.compile("/audit/@annotations");
			result = (NodeList) xpathAnnotations.evaluate(ignoreParams, XPathConstants.NODESET);
			if (result.getLength()==1)
			{
				StringTokenizer tok=new StringTokenizer(result.item(0).getTextContent(),", \t");
				while (tok.hasMoreTokens())
				{
					String t=tok.nextToken();
					detectAnnotations_.put(convJavaName(t),t);
				}
			}
			parseNode(ignoreParams,environnement,"/audit/packages/package",ignorePackageName_);
			parseNode(ignoreParams,environnement,"/audit/filenames/filename",ignoreName_);
			parseNode(ignoreParams,environnement,"/audit/basenames/basename",ignoreBaseName_);
			parseNode(ignoreParams,environnement,"/audit/services/service",ignoreServices_);
			parseNode(ignoreParams,environnement,"/audit/annotations/annotation",ignoreAnnotations_);
		}
		finally
		{
			in.close();
		}
		
	}

	private static final String convJavaName(final String javastyle)
	{
		String classstyle=javastyle;
		if (classstyle.charAt(0)=='@')
			classstyle=classstyle.substring(1);
		classstyle="L"+classstyle.replace('.', '/')+";";
		return classstyle;
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
			"Usage: macaron-audit [-R] [-i <file.xml|url>] [-h] \\\n"+
			"                     [--xslt <str>] [-o <-|file.xml>] \\\n"+
			"                     [--loglevel <severe|warning|info|...>] \\\n"+
			"                     <dir>|<files(.jar|.war|.ear|.rar)>)+\n"+
			"(-R|--recursive)             : Audit all component in directory and\n"+
			"                               sub-directory.\n"+
			"(-i|--ignore) <file.xml|url> : The xml result for ignore warning.\n"+
			"(-o|--output) <-|file.xml>   : The xml result file or stdout.\n"+
			"--xslt <str>                 : The xslt to inject.\n"+
			"--loglevel <str>             : Java Log level\n"+
			"                               (severe, warning, info,fine, ...)\n"+
			"(-h|--help)                  : This help\n"+
			"\n"+
			"Audit JavaEE component and jar to detect suspicious files.\n"+
			"The environment variable AUDIT_XSLT may be the default XSLT."
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
	 * Parse arguments.
	 * 
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
		if ("-r".equals(arg) || "-R".equals(arg) || "--recursive".equals(arg))
		{
			params.setRecurs(params.isRecurs() == false);
		}
		else if (("-i".equals(arg) || "--ignore".equals(arg)) && position < args.length - 1)
		{
			String ignore=args[++position];
			try
			{
				params.setIgnore(new URL(ignore));
			}
			catch (MalformedURLException x)
			{
				final int idx = ignore.indexOf('#');
				try
				{
					params.setIgnore( new File((idx != -1) ? ignore.substring(
						0, idx) : ignore).toURI().toURL());
				}
				catch (MalformedURLException e)
				{
					throw new CommandLineException(e.getLocalizedMessage());
				}
			}

		}
		else if (("-o".equals(arg) || "--output".equals(arg)) && position < args.length - 1)
		{
			params.setOutput(new File(args[++position]));
		}
		else if ("--xslt".equals(arg) && position < args.length - 1)
		{
			params.setXslt(args[++position]);
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
			params.getNames().add(new File(arg));
		}
		return position;
	}
	
	/**
	 * Parse command line.
	 * 
	 * @param args The args.
	 * @return The configuration.
	 * 
	 * @throws CommandLineException If syntax error.
	 */
	public static ParamsConfig parseArgs(final String[] args) throws CommandLineException
	{
		final ParamsConfig params = new ParamsConfig();

		if (args.length==0)
		{
			help(System.out);
			return null;
		}
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
		if ( params.getOutput() == null )
			throw new CommandLineException("Missing --output parameter.");
		return params;
	}
	
	/**
	 * Analyse and invoque Audit.
	 * 
	 * @param args The command line arguments.
	 */
	public static int mainRet(final String[] args)
	{
		try
		{
			log.setLevel(Level.SEVERE);
			ParamsConfig config = parseArgs(args);
			if (config==null) return 0;
			Audit audit=new Audit(config);
			audit.init();
			audit.doAll(config.getNames().toArray(new File[config.getNames().size()]));
			audit.writeXMLReport();
			return 0;
		}
		catch (CommandLineException e)
		{
			log.severe(e.getLocalizedMessage());
			return 1;
		}
		catch (IOException e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			return 1;
		}
		catch (Exception e)
		{
			log.severe(e.getLocalizedMessage());
			e.printStackTrace();
			return 1;
		}
	}

	/**
	 * JDK5 log formater.
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
			LogManager.getLogManager().readConfiguration(Audit.class.getResourceAsStream("/META-INF/logging.properties"));
			System.exit(mainRet(args));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
